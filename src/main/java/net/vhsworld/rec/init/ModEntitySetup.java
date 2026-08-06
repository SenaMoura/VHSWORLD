package net.vhsworld.rec.init;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.AnomalyEntity;
import net.vhsworld.rec.entity.CrawlerVoidEntity;
import net.vhsworld.rec.entity.InvertedSilhouetteEntity;
import net.vhsworld.rec.entity.ListenerEntity;
import net.vhsworld.rec.entity.ShadeSegmentEntity;
import net.vhsworld.rec.entity.StaticWatcherEntity;
import net.vhsworld.rec.entity.StonemanEntity;

/**
 * O que as criaturas precisam alem do registro: atributos e regra de spawn.
 *
 * ⚠️ SpawnPlacements.register NAO e thread-safe — por isso vai dentro do
 * enqueueWork do FMLCommonSetupEvent, e nao solto no metodo.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntitySetup {

    private ModEntitySetup() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.STONEMAN.get(), StonemanEntity.createAttributes().build());
        event.put(ModEntities.ANOMALY.get(), AnomalyEntity.createAttributes().build());
        // O Espelho nao anda, nao ataca e nao apanha — mas TODO `Mob` precisa de mapa de
        // atributos registrado, senao o jogo estoura ao instanciar. Os do Mob cru bastam.
        event.put(ModEntities.MIRROR.get(), Mob.createMobAttributes().build());

        event.put(ModEntities.STATIC_WATCHER.get(), StaticWatcherEntity.createAttributes().build());
        event.put(ModEntities.SHADE_SEGMENT.get(), ShadeSegmentEntity.createAttributes().build());
        event.put(ModEntities.INVERTED_SILHOUETTE.get(),
                InvertedSilhouetteEntity.createAttributes().build());
        event.put(ModEntities.CRAWLER_VOID.get(), CrawlerVoidEntity.createAttributes().build());
        event.put(ModEntities.LISTENER.get(), ListenerEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> SpawnPlacements.register(
                ModEntities.STONEMAN.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules));

        // A anomalia usa regra PROPRIA porque ela nao e um Monster — ela nao ataca,
        // nao persegue e nao pode ser morta, entao herdar a classe so para pegar o
        // checkMonsterSpawnRules seria mentir sobre o que ela e. A regra em si e a
        // mesma de sempre: chao que aceita bicho e escuro o bastante.
        event.enqueueWork(() -> SpawnPlacements.register(
                ModEntities.ANOMALY.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModEntitySetup::anomalySpawnRules));

        // O Observador e a Silhueta nascem em campo ABERTO e a ceu aberto — os dois
        // dependem de ser vistos de longe (um no alto de um morro, outra na linha do
        // horizonte). Nascer em caverna seria desperdicar a criatura inteira: no
        // escuro apertado ninguem nota que a coisa la longe mudou de lugar.
        event.enqueueWork(() -> SpawnPlacements.register(
                ModEntities.STATIC_WATCHER.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModEntitySetup::openSkySpawnRules));

        event.enqueueWork(() -> SpawnPlacements.register(
                ModEntities.INVERTED_SILHOUETTE.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModEntitySetup::openSkySpawnRules));

        // Os outros dois sao de caverna e de canto escuro; a regra de monstro serve.
        event.enqueueWork(() -> SpawnPlacements.register(
                ModEntities.SHADE_SEGMENT.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules));

        event.enqueueWork(() -> SpawnPlacements.register(
                ModEntities.CRAWLER_VOID.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules));

        // ⚠️ O ESCUTADOR USA A REGRA DE MONSTRO (escuro), e nao uma regra propria, embora
        // luz nao signifique nada para ele — ele e cego. O motivo nao e ele, e o jogador:
        // uma criatura que pudesse nascer ao meio-dia num campo aberto seria vista antes de
        // ser ouvida, e a ordem em que se descobre esta criatura E a criatura. Primeiro o
        // estalo no escuro, depois o corpo.
        event.enqueueWork(() -> SpawnPlacements.register(
                ModEntities.LISTENER.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules));
    }

    /**
     * Chao que aceita bicho, a ceu aberto, e so de noite.
     *
     * Nao usa `isDarkEnoughToSpawn` de proposito: aquilo cobra escuro no bloco, e
     * campo aberto de madrugada com lua cheia passa raspando. Aqui o que importa e
     * ser NOITE e haver ceu — o Observador precisa de horizonte, nao de breu.
     */
    private static boolean openSkySpawnRules(EntityType<? extends Mob> type, ServerLevelAccessor level,
                                             MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.canSeeSky(pos)
                && !level.getLevel().isDay()
                && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    private static boolean anomalySpawnRules(EntityType<AnomalyEntity> type, ServerLevelAccessor level,
                                             MobSpawnType reason, BlockPos pos, RandomSource random) {
        return Monster.isDarkEnoughToSpawn(level, pos, random)
                && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }
}
