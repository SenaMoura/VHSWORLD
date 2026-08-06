package net.vhsworld.rec.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.AnomalyEntity;
import net.vhsworld.rec.entity.CrawlerVoidEntity;
import net.vhsworld.rec.entity.InvertedSilhouetteEntity;
import net.vhsworld.rec.entity.ListenerEntity;
import net.vhsworld.rec.entity.MirrorEntity;
import net.vhsworld.rec.entity.ShadeSegmentEntity;
import net.vhsworld.rec.entity.StaticWatcherEntity;
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

    // ================================================================ a leva das quatro

    /**
     * O Observador Estatico.
     *
     * A caixa e 1.25 x 5.88, medida no modelo (tools/measure_entity_models.py), e ele
     * e alto assim mesmo: quase seis blocos. `clientTrackingRange` no maximo porque o
     * bicho e feito para ser visto de LONGE — parado no alto de um morro, a oitenta
     * blocos. Se ele so aparecesse perto, a criatura inteira deixaria de existir.
     *
     * `updateInterval` alto (10) e de graca: ele nao anda. Toda mudanca de posicao e
     * teletransporte, e teletransporte o servidor manda na hora, fora do intervalo.
     */
    public static final RegistryObject<EntityType<StaticWatcherEntity>> STATIC_WATCHER =
            ENTITIES.register("static_watcher", () -> EntityType.Builder
                    .of(StaticWatcherEntity::new, MobCategory.MONSTER)
                    .sized(1.25F, 5.88F)
                    .clientTrackingRange(16)
                    .updateInterval(10)
                    .fireImmune()
                    .build("static_watcher"));

    /**
     * O Anomalo da Sombra.
     *
     * ⚠️ A caixa (1.2 x 2.6) e MAIS ESTREITA que o modelo, que tem 2.21 de largura por
     * causa das costelas. E de proposito: ele vive em caverna, e uma caixa de 2.2 nao
     * passa em corredor de 2. As costelas atravessando a parede nao sao defeito — ele
     * e uma coisa colada na rocha, e isso ate ajuda.
     */
    public static final RegistryObject<EntityType<ShadeSegmentEntity>> SHADE_SEGMENT =
            ENTITIES.register("shade_segment", () -> EntityType.Builder
                    .of(ShadeSegmentEntity::new, MobCategory.MONSTER)
                    .sized(1.2F, 2.6F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .fireImmune()
                    .build("shade_segment"));

    /**
     * A Silhueta Invertida.
     *
     * Caixa de jogador (0.6 x 1.95), nao a do modelo (1.0 x 2.0). Ela tem que caber
     * exatamente onde um jogador caberia: o disfarce e o bicho, e uma silhueta que
     * emperra numa porta que voce atravessa se denuncia sem dizer nada.
     */
    public static final RegistryObject<EntityType<InvertedSilhouetteEntity>> INVERTED_SILHOUETTE =
            ENTITIES.register("inverted_silhouette", () -> EntityType.Builder
                    .of(InvertedSilhouetteEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(16)
                    .updateInterval(2)
                    .fireImmune()
                    .build("inverted_silhouette"));

    /**
     * O Rastreador do Rastejo.
     *
     * ⚠️ O documento pede "altura de apenas 1 bloco, podendo passar por brechas". O
     * modelo que o Pedro fez tem 3.11 de altura por 3.72 de largura — e um bicho
     * grande, de costas arqueadas. A caixa aqui segue o MODELO (2.0 x 1.9), nao o
     * texto: caixa de 1 bloco num modelo de 3 daria um bicho enterrado no chao pela
     * metade. Passar por brecha e a unica coisa do documento que este bicho nao faz.
     */
    public static final RegistryObject<EntityType<CrawlerVoidEntity>> CRAWLER_VOID =
            ENTITIES.register("crawler_void", () -> EntityType.Builder
                    .of(CrawlerVoidEntity::new, MobCategory.MONSTER)
                    .sized(2.0F, 1.9F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .fireImmune()
                    .build("crawler_void"));

    // ================================================================ o verbo novo

    /**
     * O ESCUTADOR — o primeiro do elenco que nao e sobre olhar.
     *
     * ⚠️ A CAIXA E MAIS ESTREITA QUE O BICHO, e e o mesmo argumento do Anomalo da Sombra.
     * Pela referencia (SCP-939) ele e um quadrupede baixo e ABERTO: os bracos dianteiros
     * saem para os lados e o vao deles passa de dois blocos. Caixa do tamanho do desenho
     * daria uma criatura que nao entra em corredor de mina — e mina e o lugar onde o
     * jogador faz o barulho que a chama. Bicho que nao entra na mina nunca encontra
     * ninguem minerando, e a criatura inteira deixaria de acontecer. Braco atravessando a
     * parede de pedra nao e defeito aqui; e uma coisa espremida no tunel.
     *
     * 1.0 x 1.7: baixo e comprido, nao alto. Ele chega rente ao chao.
     *
     * `clientTrackingRange` 12 e menor de proposito, ao contrario do Observador: ele nao e
     * feito para ser visto de longe. O que se percebe de longe dele e o ESTALO, que e som e
     * nao depende de tracking — e chegar perto o bastante para ver o corpo ja e o problema.
     *
     * `updateInterval` 2: ele para e arranca em seco, e essas trocas tem que chegar
     * inteiras no cliente. A postura dele e a informacao — ver ListenerEntity.
     */
    public static final RegistryObject<EntityType<ListenerEntity>> LISTENER =
            ENTITIES.register("listener", () -> EntityType.Builder
                    .of(ListenerEntity::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.7F)
                    .clientTrackingRange(12)
                    .updateInterval(2)
                    .fireImmune()
                    .build("listener"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
