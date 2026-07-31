package net.vhsworld.rec.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.AnomalyEntity;
import net.vhsworld.rec.entity.MirrorEntity;
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

    /**
     * O ESPELHO: a saida de treze das quinze dimensoes.
     *
     * ⚠️ A CAIXA E 3.0 x 8.4, e ela acompanha o desenho de verdade — ao contrario da
     * anomalia, que usa caixa pequena de proposito. Aqui a caixa NAO e so "onde a criatura
     * esta": e o alvo do olhar. A regra inteira desta criatura e o servidor perguntar
     * "para onde este jogador esta olhando?", e essa pergunta e respondida com um raio
     * contra a caixa. Caixa menor que o painel daria um Espelho cujas bordas se pode
     * encarar impunemente, e o jogador aprenderia a olhar de esguelha.
     *
     * `clientTrackingRange` alto pelo mesmo motivo do Homem de Pedra, e mais um: ele tem
     * oito blocos de altura e e a SAIDA. Ver o painel preto de longe, no meio da neblina,
     * e como a dimensao diz onde e — se ele so aparecesse perto, achar a saida viraria
     * sorte em vez de leitura.
     */
    public static final RegistryObject<EntityType<MirrorEntity>> MIRROR =
            ENTITIES.register("mirror", () -> EntityType.Builder
                    .of(MirrorEntity::new, MobCategory.MISC)
                    .sized(3.0F, 8.4F)
                    .clientTrackingRange(16)
                    .updateInterval(20)
                    .fireImmune()
                    .noSummon()
                    .build("mirror"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
