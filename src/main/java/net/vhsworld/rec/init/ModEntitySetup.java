package net.vhsworld.rec.init;

import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.vhsworld.rec.RECMod;
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
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> SpawnPlacements.register(
                ModEntities.STONEMAN.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules));
    }
}
