package net.vhsworld.rec.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.AnomalyEntity;
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

    /**
     * As anomalias 2D.
     *
     * A caixa e pequena de proposito (1.0 x 2.6) e nao acompanha o cartaz: ela nao
     * colide com nada, entao a caixa so serve para o jogo saber onde a criatura
     * esta e se ela cabe no frustum. Caixa do tamanho do desenho faria a aranha, que
     * e larga, sumir da tela quando o centro dela saisse de vista.
     */
    public static final RegistryObject<EntityType<AnomalyEntity>> ANOMALY =
            ENTITIES.register("anomaly", () -> EntityType.Builder
                    .of(AnomalyEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 2.6F)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .fireImmune()
                    .build("anomaly"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
