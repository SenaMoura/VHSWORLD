package net.vhsworld.rec.apocalypse;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.AnomalyEntity;
import net.vhsworld.rec.entity.AnomalyType;
import net.vhsworld.rec.init.ModEntities;

/**
 * O mundo depois do CALLER.
 *
 * O apocalipse nao e uma explosao, e uma MUDANCA DE REGRA: o dia nao volta, a
 * tempestade nao passa, e a coisa da qual voce fugia uma vez por partida passa a
 * estar sempre a caminho. O jogador nao perde o mundo — ele perde o mundo em que
 * estava seguro de dia, que e pior.
 *
 * TUDO AQUI E DO SERVIDOR. O mod nao tem canal de rede proprio, entao nada disto
 * pode depender de avisar o cliente: o que o jogador ve (noite eterna, trovao, a
 * caçadora chegando) sao coisas que o jogo ja sincroniza sozinho. Foi de proposito —
 * inventar pacote so para isto seria abrir uma porta que o mod ainda nao precisa.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID)
public final class ApocalypseHandler {

    /** Meia-noite. O ceu fica onde estava quando o item foi usado. */
    private static final long FIXED_TIME = 18000L;

    /** De quanto em quanto se renova a tempestade (ela expira sozinha). */
    private static final int WEATHER_EVERY = 600;

    /** De quanto em quanto se manda outra caçadora atras de cada jogador. */
    private static final int HUNT_EVERY = 1200;

    /** Nem colada nem longe demais: perto o bastante para ela achar voce. */
    private static final int HUNT_MIN = 24, HUNT_MAX = 44;

    /** Quantas ela mantem por jogador. Duas ja e mais do que da para correr. */
    private static final int HUNTERS_PER_PLAYER = 2;

    private ApocalypseHandler() {}

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;
        if (!ApocalypseState.get(level).isActive()) return;

        // A noite nao passa. Prende-se o relogio em vez de mexer na gamerule: a
        // gamerule e do jogador, e devolve-la depois seria devolver o mundo antigo.
        level.setDayTime(FIXED_TIME);

        long time = level.getGameTime();
        if (time % WEATHER_EVERY == 0) {
            // Renovada de tempos em tempos porque toda tempestade tem prazo; sem
            // isto o ceu limpava sozinho no meio do apocalipse.
            level.setWeatherParameters(0, WEATHER_EVERY * 2, true, true);
        }
        if (time % HUNT_EVERY == 0) {
            for (ServerPlayer player : level.players()) {
                sendHunter(level, player);
            }
        }
    }

    /**
     * Poe mais uma caçadora a caminho, se ja nao houver o bastante por perto.
     *
     * Ela nasce FORA DE VISTA, a algumas dezenas de blocos: aparecer na cara do
     * jogador seria um susto barato e, pior, tiraria dele a unica coisa que essa
     * criatura oferece de bom — os segundos entre ouvir e ver.
     */
    private static void sendHunter(ServerLevel level, ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) return;

        long nearby = level.getEntitiesOfClass(AnomalyEntity.class,
                        player.getBoundingBox().inflate(HUNT_MAX * 2.0))
                .stream().filter(AnomalyEntity::hunts).count();
        if (nearby >= HUNTERS_PER_PLAYER) return;

        RandomSource random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = Mth.lerp(random.nextDouble(), HUNT_MIN, HUNT_MAX);
        int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
        int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);

        // Se o chunk nao estiver carregado, deixa para a proxima rodada: forcar o
        // carregamento aqui faria o apocalipse gerar mundo sozinho a cada minuto.
        if (!level.isLoaded(new BlockPos(x, level.getMinBuildHeight(), z))) return;

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos where = new BlockPos(x, y, z);
        if (!level.getBlockState(where).isAir() || !level.getBlockState(where.above()).isAir()) return;

        AnomalyEntity hunter = ModEntities.ANOMALY.get().create(level);
        if (hunter == null) return;
        hunter.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
        hunter.finalizeSpawn(level, level.getCurrentDifficultyAt(where), MobSpawnType.EVENT, null, null);
        // DEPOIS do finalizeSpawn, e nao antes: e ele quem sorteia o tipo, e o
        // apocalipse e do Cara Cinza — nao de qualquer uma das cinco.
        hunter.setType(AnomalyType.GREYFACE);
        level.addFreshEntity(hunter);
    }
}
