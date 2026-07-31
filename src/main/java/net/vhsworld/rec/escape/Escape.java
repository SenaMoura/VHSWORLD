package net.vhsworld.rec.escape;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.vhsworld.rec.item.DimensionTapeItem;
import net.vhsworld.rec.item.ModSounds;
import net.vhsworld.rec.net.EscapeFxPacket;
import net.vhsworld.rec.net.RECNetwork;
import net.vhsworld.rec.worldgen.dim.DimSpawn;

import java.util.Random;

/**
 * O ato de sair. Um lugar so, para os quatro metodos.
 *
 * ⚠️ POR QUE NAO ESTA DENTRO DE CADA APARELHO. Os quatro blocos de saida chegam ao mesmo
 * ponto — "este jogador vai embora agora" — por caminhos completamente diferentes, e a
 * tentacao e cada um resolver o proprio final. Seriam quatro copias de: achar a marca de
 * volta, tratar a marca ausente, tocar o som, mandar o efeito de tela, teleportar. Cada
 * copia e um lugar onde o quinto metodo (ou a vigesima primeira dimensao) pode ser
 * esquecido, e esquecer aqui nao da erro: da um aparelho que consome o item do jogador e
 * nao o leva a lugar nenhum.
 *
 * O que muda de um metodo para o outro e SO o efeito de tela, e ele entra por parametro.
 */
public final class Escape {

    private Escape() {}

    /**
     * Onde o jogador estava antes de entrar.
     *
     * ⚠️ E A MESMA CHAVE QUE O `DimensionTapeItem` ESCREVE, e tem que continuar sendo. A
     * fita grava a marca na ida; daqui em diante ela nao a apaga mais (ela deixou de ter
     * volta), e quem consome a marca e a saida. Se as duas usassem chaves diferentes, a
     * ida gravaria numa e a saida leria a outra — e o sintoma seria todo mundo voltando
     * para o spawn do mundo em vez de para casa, o que parece "decisao de design" e nao
     * defeito. Por isso o valor mora aqui e o item o importa, e nao ao contrario.
     */
    public static final String RETURN_TAG = "recmod:tape_return";

    // ------------------------------------------------------------------ a saida
    /**
     * Leva o jogador de volta para onde ele entrou.
     *
     * Devolve `false` quando nao havia para onde ir — o chamador nao deve consumir o item
     * nem gastar o aparelho nesse caso.
     */
    public static boolean leave(ServerPlayer player, ExitMethod method) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        // ⚠️ SO SE AINDA ESTIVER NUMA DIMENSAO NOSSA. Parece redundante — quem chama isto
        // sao os aparelhos, que so existem la dentro — e nao e: a PORTA DA LINHA tem DOIS
        // blocos de altura, e o `entityInside` do jogo dispara uma vez por bloco tocado.
        // A primeira chamada teleporta e APAGA a marca de volta; a segunda, no mesmo
        // tique, cairia no ramo de reserva e mandaria o jogador para o spawn do mundo em
        // vez da casa dele. O sintoma seria uma saida que funciona e larga voce no lugar
        // errado — e pareceria decisao de design, nao defeito.
        if (net.vhsworld.rec.worldgen.dim.DimensionProfile.of(player.level()) == null) {
            return false;
        }

        CompoundTag data = player.getPersistentData();
        ServerLevel destination = null;
        Vec3 where = null;

        if (data.contains(RETURN_TAG)) {
            CompoundTag mark = data.getCompound(RETURN_TAG);
            ResourceLocation id = ResourceLocation.tryParse(mark.getString("dimension"));
            if (id != null) {
                destination = server.getLevel(
                        ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id));
            }
            where = new Vec3(mark.getDouble("x"), mark.getDouble("y"), mark.getDouble("z"));
        }

        // Sem marca (mundo antigo, ou entrou por comando): o overworld resolve. Ficar
        // preso seria pior do que chegar no lugar errado — e agora que a fita nao volta
        // mais, este ramo e a UNICA rede de seguranca que sobrou.
        if (destination == null || where == null) {
            destination = server.overworld();
            BlockPos spawn = destination.getSharedSpawnPos();
            where = new Vec3(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        }

        // O efeito de tela sai ANTES do teleporte, e de proposito: ele e o corte da fita,
        // e um corte que aparece depois de o mundo ja ter trocado nao esconde nada.
        RECNetwork.toPlayer(player, new EscapeFxPacket(method));

        player.level().playSound(null, player.blockPosition(), ModSounds.TAPE_PLAYER.get(),
                SoundSource.PLAYERS, 1.0F, 0.8F);

        data.remove(RETURN_TAG);
        player.changeDimension(destination, new DimensionTapeItem.FixedPoint(where));
        return true;
    }

    // ------------------------------------------------------------------ o castigo
    /**
     * O preco de olhar para tras no corredor do espelho.
     *
     * O `fuga.rtf` pede uma "sub-dimensao ainda mais perigosa". Isto NAO e ela, e a
     * decisao foi do Pedro: uma decima sexta dimensao para hospedar um castigo seria uma
     * dimensao inteira que existe so para punir, e o mod ja tem quinze lugares em que se
     * pode estar perdido.
     *
     * O que ficou no lugar cobra a mesma coisa que a sub-dimensao cobraria — TEMPO e a
     * caminhada de volta. O jogador e cuspido a uns milhares de blocos, no escuro, com a
     * sanidade no chao, e o corredor do espelho que ele acabou de achar fica onde estava:
     * agora ele sabe que existe uma saida e sabe que esta longe. Ser jogado para longe da
     * saida conhecida e pior do que ser jogado num lugar novo, porque o lugar novo nao
     * tem nada a perder.
     *
     * ⚠️ O destino sai do `dimensionSpawn` do proprio gerador, e nao de um deslocamento
     * qualquer. Somar 2000 no X poria o jogador dentro da pedra em onze das quinze — e
     * "cuspido para dentro de um bloco" e sufocamento, nao castigo.
     */
    public static void punish(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 far = DimensionTapeItem.spawnOf(level);

        RECNetwork.toPlayer(player, new EscapeFxPacket(EscapeFxPacket.Kind.MIRROR_BROKEN));

        level.playSound(null, player.blockPosition(), ModSounds.DREAD_GLITCH.get(),
                SoundSource.HOSTILE, 1.0F, 0.6F);

        player.teleportTo(level, far.x, far.y, far.z, player.getYRot(), player.getXRot());
        player.displayClientMessage(Component.translatable("message.recmod.mirror_broken"), true);
    }

    // ------------------------------------------------------------------ consulta
    /** O metodo desta dimensao, ou null se ela nao e nossa. */
    public static ExitMethod methodOf(Level level) {
        var profile = net.vhsworld.rec.worldgen.dim.DimensionProfile.of(level);
        return profile == null ? null : profile.exit();
    }

    /**
     * O ponto de saida mais proximo, perguntado ao gerador.
     *
     * Usado pelo bilhete e pelos fragmentos: os dois precisam citar coordenadas, e as
     * coordenadas tem que ser as MESMAS que o mundo desenhou. Perguntar ao gerador e a
     * unica forma de garantir isso — recalcular a conta aqui seria uma segunda copia da
     * regra, e as duas copias so ficam iguais ate alguem mexer numa.
     */
    public static BlockPos siteNear(ServerLevel level, BlockPos from) {
        if (!(level.getChunkSource().getGenerator() instanceof DimSpawn dimension)) return null;
        return net.vhsworld.rec.worldgen.dim.ExitSite.nearest(dimension, from);
    }

    /** Um sorteio qualquer, para o que nao precisa ser determinista. */
    public static final Random DICE = new Random();
}
