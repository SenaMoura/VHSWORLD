package net.vhsworld.rec.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.StonemanEntity;

/** As criaturas do mod. */
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RECMod.MOD_ID);

    /**
     * O Homem de Pedra.
     *
     * Caixa de 0.9 x 2.3: um palmo mais alto que o jogador. clientTrackingRange alto
     * (16 chunks) porque o susto dele depende de ser visto de LONGE parado — se ele
     * so aparecesse perto, nao daria para notar que mudou de lugar.
     */
    public static final RegistryObject<EntityType<StonemanEntity>> STONEMAN =
            ENTITIES.register("stoneman", () -> EntityType.Builder
                    .of(StonemanEntity::new, MobCategory.MONSTER)
                    .sized(0.9F, 2.3F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .build("stoneman"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
