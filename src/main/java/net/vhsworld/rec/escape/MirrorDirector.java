package net.vhsworld.rec.escape;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.MirrorEntity;
import net.vhsworld.rec.init.ModEntities;
import net.vhsworld.rec.item.ModSounds;
import net.vhsworld.rec.net.EscapeFxPacket;
import net.vhsworld.rec.net.RECNetwork;
import net.vhsworld.rec.worldgen.dim.DimSpawn;
import net.vhsworld.rec.worldgen.dim.DimensionProfile;
import net.vhsworld.rec.worldgen.dim.ExitSite;

import java.util.List;

/**
 * PLANTA O ESPELHO e cobra a regra de nao olhar para ele.
 *
 * ============================ AS DUAS COISAS QUE ELE FAZ ============================
 *
 * 1. GARANTE QUE O ESPELHO EXISTE. Ele nao vem do gerador de chunk: um `Mob` nao pode ser
 *    criado durante a geracao (o mundo ainda nao existe para receber entidade), e
 *    tabela de spawn nao serve porque a saida nao pode depender de sorte. Entao ele e
 *    posto aqui, em tempo de jogo, na ancora da regiao, assim que um jogador chega perto o
 *    bastante para o chunk estar carregado.
 *
 * 2. COBRA O OLHAR. Encarar drena sanidade, traz o susto e avisa; insistir devolve o
 *    jogador ao inicio da dimensao.
 *
 * ============================ POR QUE O RELOGIO E DE OLHAR, E NAO DE PERTO ============
 *
 * ⚠️ O contador so anda enquanto o jogador esta OLHANDO, e desce quando ele desvia. A
 * alternativa obvia — punir por proximidade — daria uma criatura que e so uma area de
 * dano, e transformaria a saida num campo minado que se atravessa correndo. Medindo o
 * olhar, o jogador tem uma acao a fazer o tempo todo (desviar), e a tensao vira dele: ele
 * SABE onde esta o espelho e tem que chegar la sem confirmar que ele ainda esta ali.
 *
 * ⚠️ E DESCE MAIS DEVAGAR DO QUE SOBE. Desviar alivia, mas nao apaga: quem encarou por
 * quatro segundos nao zera olhando para o chao por um. Sem isso, daria para "piscar" na
 * direcao do espelho a cada meio segundo e navegar de olho nele impunemente, que e
 * exatamente a jogada que a regra existe para proibir.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID)
public final class MirrorDirector {

    private MirrorDirector() {}

    private static final String TAG = "recmod:stare";

    /** De quantos blocos o Espelho ainda cobra o olhar. */
    private static final double LOOK_RANGE = 56.0D;

    /**
     * Quao centrado o olhar precisa estar para contar como encarar.
     *
     * 0.985 sao uns 10 graus. E APERTADO de proposito: o painel tem oito blocos de altura
     * e enche a tela de longe, entao um cone largo puniria quem so o tem no canto do
     * campo de visao. O que a regra quer cobrar e olhar PARA ele, e nao ve-lo passar.
     */
    private static final double LOOK_CONE = 0.985D;

    /** Tiques de encarada ate o primeiro susto. */
    private static final int WARN_AT = 20;

    /** De quantos em quantos tiques o susto se repete enquanto ele insiste. */
    private static final int SCARE_EVERY = 40;

    /** Tiques de encarada acumulada ate ser devolvido ao inicio da dimensao. */
    private static final int LIMIT = 130;

    /** O quanto o contador desce por tique quando o jogador desvia. Ver a classe. */
    private static final int RELIEF = 1;

    /** So a cada tantos tiques o diretor procura/planta espelho: varrer entidade e caro. */
    private static final int PLANT_EVERY = 40;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long time = event.getServer().getTickCount();

        for (ServerLevel level : event.getServer().getAllLevels()) {
            DimensionProfile profile = DimensionProfile.of(level);
            if (profile == null || profile.exit() != ExitMethod.MIRROR) continue;
            if (!(level.getChunkSource().getGenerator() instanceof DimSpawn generator)) continue;
            if (level.players().isEmpty()) continue;

            for (ServerPlayer player : level.players()) {
                BlockPos site = ExitSite.nearest(generator, player.blockPosition());
                if (site == null) continue;

                if (time % PLANT_EVERY == 0) plant(level, site);
                stare(level, player, site);
            }
        }
    }

    /**
     * Poe o Espelho na ancora, se ele ainda nao estiver la.
     *
     * ⚠️ SO SE O CHUNK ESTIVER CARREGADO. Criar entidade em chunk que nao esta carregado a
     * faz nascer e sumir no mesmo tique — e a proxima passagem tentaria de novo, para
     * sempre. Como o diretor so roda com jogador na dimensao, e a ancora mais proxima dele
     * costuma estar dentro do raio de simulacao, isso resolve sozinho na hora certa.
     */
    private static void plant(ServerLevel level, BlockPos site) {
        if (!level.isLoaded(site)) return;

        AABB around = new AABB(site).inflate(6.0D);
        List<MirrorEntity> already = level.getEntitiesOfClass(MirrorEntity.class, around);
        if (!already.isEmpty()) return;

        MirrorEntity mirror = ModEntities.MIRROR.get().create(level);
        if (mirror == null) return;
        mirror.moveTo(site.getX() + 0.5D, site.getY(), site.getZ() + 0.5D, 0.0F, 0.0F);
        mirror.finalizeSpawn(level, level.getCurrentDifficultyAt(site), MobSpawnType.EVENT, null, null);
        level.addFreshEntity(mirror);
    }

    /** A regra do olhar, para um jogador. */
    private static void stare(ServerLevel level, ServerPlayer player, BlockPos site) {
        MirrorEntity mirror = nearestMirror(level, site);
        if (mirror == null) return;

        int stare = player.getPersistentData().getInt(TAG);

        if (looking(player, mirror)) {
            stare++;

            if (stare == WARN_AT || (stare > WARN_AT && (stare - WARN_AT) % SCARE_EVERY == 0)) {
                // O susto e a sanidade saem os dois deste pacote — ver EscapeFx.play.
                RECNetwork.toPlayer(player, new EscapeFxPacket(EscapeFxPacket.Kind.MIRROR_SCARE));
                // ⚠️ O AVISO E LITERAL E EM INGLES, e e uma citacao e nao um descuido de
                // idioma: e o texto que aparece na tela em creepypasta de fita, e o Pedro
                // pediu essa frase. Traduzi-la mataria a referencia.
                player.displayClientMessage(
                        Component.translatable("message.recmod.mirror_turn_back"), true);
                level.playSound(null, mirror.getX(), mirror.getY(), mirror.getZ(),
                        ModSounds.DREAD_GLITCH.get(), SoundSource.HOSTILE, 1.0F, 0.55F);
            }

            if (stare >= LIMIT) {
                player.getPersistentData().putInt(TAG, 0);
                Escape.punish(player);
                return;
            }
        } else {
            stare = Math.max(0, stare - RELIEF);
        }

        player.getPersistentData().putInt(TAG, stare);
    }

    /** O Espelho desta ancora, se ele ja foi plantado. */
    private static MirrorEntity nearestMirror(ServerLevel level, BlockPos site) {
        List<MirrorEntity> found = level.getEntitiesOfClass(
                MirrorEntity.class, new AABB(site).inflate(LOOK_RANGE));
        MirrorEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (MirrorEntity mirror : found) {
            double distance = mirror.distanceToSqr(site.getX(), site.getY(), site.getZ());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = mirror;
            }
        }
        return best;
    }

    /**
     * Este jogador esta encarando o Espelho?
     *
     * Tres perguntas, e as tres precisam ser sim: esta perto o bastante, o olhar aponta
     * para ele dentro do cone, e nao ha parede no meio.
     *
     * ⚠️ A LINHA DE VISADA E O QUE IMPEDE O DEFEITO MAIS ESTUPIDO POSSIVEL: sem ela, o
     * jogador seria punido por olhar na DIRECAO do espelho estando do outro lado de uma
     * parede de pedra, numa dimensao de corredores onde isso acontece o tempo todo. Ele
     * levaria sustos sem ter visto nada e nao teria como entender por que.
     */
    private static boolean looking(ServerPlayer player, MirrorEntity mirror) {
        Vec3 eye = player.getEyePosition();
        Vec3 target = mirror.position().add(0.0D, mirror.getBbHeight() * 0.5D, 0.0D);
        Vec3 toMirror = target.subtract(eye);

        double distance = toMirror.length();
        if (distance > LOOK_RANGE || distance < 0.01D) return false;
        if (player.getLookAngle().dot(toMirror.scale(1.0D / distance)) < LOOK_CONE) return false;

        HitResult wall = player.level().clip(new ClipContext(
                eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return wall.getType() == HitResult.Type.MISS
                || eye.distanceToSqr(wall.getLocation()) >= toMirror.lengthSqr() - 1.0D;
    }
}
