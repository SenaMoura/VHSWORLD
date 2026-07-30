package net.vhsworld.rec.worldgen.dim;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.item.DimensionTapeItem;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Errar o pulo na PARKOURLAND nao mata: troca de dimensao.
 *
 * "se ele errar ele cai no void e e levado pra alguma dimensao aleatoria menos a DATA."
 *
 * ⚠️ POR QUE ISTO NAO E DANO DE QUEDA E NAO E MORTE. Morrer devolveria o jogador ao
 * nascimento com o inventario largado no vazio, e o vazio nao devolve nada — cair uma vez
 * custaria o inventario inteiro. Trocar de dimensao e o oposto: a queda nao tira nada,
 * ela LEVA para outro lugar, e o castigo e ter que achar o caminho de volta. E tambem
 * cria a unica ligacao direta entre duas dimensoes do mod que nao passa pela fita.
 *
 * ⚠️ MENOS A DATA, e o Pedro tem razao: a DATA e a unica com Diretor e caçadora, e ela
 * nao tem saida a pe. Chegar lá sem ter escolhido ir seria ser posto num labirinto escuro
 * com uma coisa atras de voce e sem a fita para sair. As outras se atravessa.
 *
 * O gatilho e y < -4. O piso da gaiola esta em y=1 e o mundo comeca em y=0, entao passar
 * de -4 e um estado que nao existe de outra forma: ninguem chega ali andando. E fica bem
 * antes dos -64 em que o jogo comeca a machucar quem esta fora do mundo — a troca tem que
 * acontecer enquanto o jogador ainda esta inteiro.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID)
public final class ParkourFall {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Abaixo disto, o jogador esta fora do mundo e nao ha volta por conta propria. */
    private static final int VOID_Y = -4;

    /** De quanto em quanto se confere. Uma queda leva mais de meio segundo. */
    private static final int CHECK_EVERY = 5;

    /** Para onde NAO se cai. Ver o comentario da classe. */
    private static final List<String> FORBIDDEN = List.of("data", "parkourland");

    private ParkourFall() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;
        if (!DimensionProfile.isMold(level, DimensionProfile.Mold.TOWER)) return;
        if (level.getGameTime() % CHECK_EVERY != 0) return;

        MinecraftServer server = level.getServer();
        // Copia da lista: `changeDimension` tira o jogador de `level.players()` no meio
        // do laco, e iterar a lista viva enquanto ela encolhe salta o proximo.
        for (ServerPlayer player : new ArrayList<>(level.players())) {
            if (player.getY() > VOID_Y) continue;
            fall(player, server);
        }
    }

    private static void fall(ServerPlayer player, MinecraftServer server) {
        ServerLevel destination = pick(server);
        if (destination == null) {
            // Nenhuma outra dimensao de pe: devolve para a plataforma de nascimento desta
            // mesma, em vez de deixar o jogador caindo para sempre num lugar sem fundo.
            net.minecraft.world.phys.Vec3 back = DimensionTapeItem.spawnOf(player.serverLevel());
            player.teleportTo(back.x, back.y, back.z);
            player.resetFallDistance();
            return;
        }

        player.resetFallDistance();
        player.displayClientMessage(Component.translatable("message.recmod.parkour_fell"), true);
        player.changeDimension(destination,
                new DimensionTapeItem.FixedPoint(DimensionTapeItem.spawnOf(destination)));
        // Depois do teleporte tambem: `changeDimension` recria o jogador no destino, e a
        // distancia de queda acumulada no vazio viaja com ele — sem isto, o jogador chega
        // na dimensao nova e leva o dano dos 70 blocos que caiu na anterior.
        player.resetFallDistance();
    }

    /**
     * Uma dimensao do mod sorteada, que nao esteja na lista proibida e que esteja de pe.
     *
     * A lista sai do proprio `DimensionProfile`, e nao de um vetor escrito aqui: a
     * dimensao numero dez vai entrar no sorteio sem ninguem ter que lembrar deste arquivo.
     * E isso importa mais do que parece — esquecer aqui nao da erro nenhum, so faz a nova
     * dimensao nunca ser sorteada, o que se descobre depois de cem quedas.
     */
    private static ServerLevel pick(MinecraftServer server) {
        List<ServerLevel> options = new ArrayList<>();
        for (DimensionProfile profile : DimensionProfile.all()) {
            if (FORBIDDEN.contains(profile.id())) continue;
            ServerLevel level = server.getLevel(ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, profile.id())));
            if (level != null) options.add(level);
        }
        if (options.isEmpty()) {
            LOGGER.warn("[parkourland] ninguem para receber a queda: nenhuma dimensao de pe");
            return null;
        }
        return options.get(new java.util.Random().nextInt(options.size()));
    }
}
