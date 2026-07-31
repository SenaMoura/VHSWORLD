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
    }

    private static boolean anomalySpawnRules(EntityType<AnomalyEntity> type, ServerLevelAccessor level,
                                             MobSpawnType reason, BlockPos pos, RandomSource random) {
        return Monster.isDarkEnoughToSpawn(level, pos, random)
                && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }
}
