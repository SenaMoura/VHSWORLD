package net.vhsworld.rec.init;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.item.AnchorItem;
import net.vhsworld.rec.item.BatteryItem;
import net.vhsworld.rec.item.BlankTapeItem;
import net.vhsworld.rec.item.CorruptedBatteryItem;
import net.vhsworld.rec.item.CorruptedCompassItem;
import net.vhsworld.rec.item.FractureArmorItem;
import net.vhsworld.rec.item.FractureAxeItem;
import net.vhsworld.rec.item.FractureItem;
import net.vhsworld.rec.item.FracturePickaxeItem;
import net.vhsworld.rec.item.FractureShovelItem;
import net.vhsworld.rec.item.LureClockItem;
import net.vhsworld.rec.item.ModTiers;
import net.vhsworld.rec.item.OreTrackerItem;
import net.vhsworld.rec.item.StructureLocatorItem;
import net.vhsworld.rec.item.VideocassetteItem;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, RECMod.MOD_ID);

    // A filmadora foi removida: a camera nao e um item que se segura, e o estado
    // do mundo. O jogador ja esta filmando desde que acordou.

    // --- Aluminio: o metal da pilha ---
    public static final RegistryObject<Item> RAW_ALUMINUM = ITEMS.register("raw_aluminum",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ALUMINUM_INGOT = ITEMS.register("aluminum_ingot",
            () -> new Item(new Item.Properties()));

    // Itens dos blocos, para o minerio poder ser carregado e colocado de volta
    public static final RegistryObject<Item> ALUMINUM_ORE_ITEM = ITEMS.register("aluminum_ore",
            () -> new BlockItem(ModBlocks.ALUMINUM_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> DEEPSLATE_ALUMINUM_ORE_ITEM = ITEMS.register("deepslate_aluminum_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_ALUMINUM_ORE.get(), new Item.Properties()));

    // Pilha: recarrega a bateria da camera ao usar (botão direito)
    public static final RegistryObject<Item> BATTERY = ITEMS.register("battery",
            () -> new BatteryItem(new Item.Properties().stacksTo(16)));

    // --- Engenhocas de camera ---

    /** Lente infravermelha: na mao, revela o invisivel e come o dobro de bateria. */
    public static final RegistryObject<Item> INFRARED_LENS = ITEMS.register("infrared_lens",
            () -> new Item(new Item.Properties().stacksTo(1)));

    /** Pilha corrompida: enche a bateria de vez, mas cobra sanidade. */
    public static final RegistryObject<Item> CORRUPTED_BATTERY = ITEMS.register("corrupted_battery",
            () -> new CorruptedBatteryItem(new Item.Properties().stacksTo(16)));

    /** Fita virgem: usar comeca a gravar alguns segundos de imagem. */
    public static final RegistryObject<Item> BLANK_TAPE = ITEMS.register("blank_tape",
            () -> new BlankTapeItem(new Item.Properties().stacksTo(16)));

    /** Videocassete: usar abre o cofre de fitas para rever as gravacoes. */
    public static final RegistryObject<Item> VIDEOCASSETTE = ITEMS.register("videocassette",
            () -> new VideocassetteItem(new Item.Properties().stacksTo(1)));

    /** Tripe: bloco que se planta e filma por voce. */
    public static final RegistryObject<Item> TRIPOD_ITEM = ITEMS.register("tripod",
            () -> new BlockItem(ModBlocks.TRIPOD.get(), new Item.Properties()));

    // --- Pedra corrompida: a raiz da cadeia de itens ---
    public static final RegistryObject<Item> CORRUPTED_STONE_ITEM = ITEMS.register("corrupted_stone",
            () -> new BlockItem(ModBlocks.CORRUPTED_STONE.get(), new Item.Properties()));

    // --- A cadeia do ferro batido ---
    // Nada aqui e magico: e ferramenta feita na marra, com bigorna e pancada. O
    // contraste com o que ela abre depois (o rasgo, a anomalia) e o ponto.

    /** Duas barras batidas ate virar haste. Base de tudo que precisa de cabo. */
    public static final RegistryObject<Item> IRON_STICK = ITEMS.register("iron_stick",
            () -> new Item(new Item.Properties()));

    /** Prensa o ferro na bigorna. Gasta a cada pancada. */
    public static final RegistryObject<Item> HAMMER = ITEMS.register("hammer",
            () -> new Item(new Item.Properties().durability(128).setNoRepair()));

    /** Chapa de ferro batida. So sai da bigorna, nunca da bancada. */
    public static final RegistryObject<Item> PRESSED_IRON = ITEMS.register("pressed_iron",
            () -> new Item(new Item.Properties()));

    /**
     * Tesoura afiada.
     *
     * Feita para separar uma coisa do corpo dela — hoje nao tem alvo, porque as
     * anomalias ainda nao existem. Fica pronta esperando.
     */
    public static final RegistryObject<Item> SHARP_SCISSORS = ITEMS.register("sharp_scissors",
            () -> new Item(new Item.Properties().durability(64)));

    /**
     * Gosma preta.
     *
     * Hoje sai de fundir pedra corrompida — a corrupcao "cozinha para fora" e o que
     * pinga e isto. Quando o Extrator existir, ele passa a ser a fonte principal
     * (tirar a corrupcao de um item deixa a gosma para tras) e a fornalha vira o
     * caminho lento.
     */
    public static final RegistryObject<Item> BLACK_GOO = ITEMS.register("black_goo",
            () -> new Item(new Item.Properties()));

    // --- O kit de pedra corrompida ---
    // Craft de todos: a ferramenta vanilla no meio, 4 pedras corrompidas em volta.

    public static final RegistryObject<Item> CORRUPTED_SWORD = ITEMS.register("corrupted_sword",
            () -> new SwordItem(ModTiers.CORRUPTED, 3, -2.4F, new Item.Properties()));

    public static final RegistryObject<Item> CORRUPTED_PICKAXE = ITEMS.register("corrupted_pickaxe",
            () -> new PickaxeItem(ModTiers.CORRUPTED, 1, -2.8F, new Item.Properties()));

    public static final RegistryObject<Item> CORRUPTED_AXE = ITEMS.register("corrupted_axe",
            () -> new AxeItem(ModTiers.CORRUPTED, 5.0F, -3.0F, new Item.Properties()));

    public static final RegistryObject<Item> CORRUPTED_SHOVEL = ITEMS.register("corrupted_shovel",
            () -> new ShovelItem(ModTiers.CORRUPTED, 1.5F, -3.0F, new Item.Properties()));

    public static final RegistryObject<Item> CORRUPTED_HOE = ITEMS.register("corrupted_hoe",
            () -> new HoeItem(ModTiers.CORRUPTED, -2, -1.0F, new Item.Properties()));

    /** A chave do mod: e a unica coisa que tira o Rasgo da Realidade da parede. */
    public static final RegistryObject<Item> CORRUPTED_DIAMOND_PICKAXE =
            ITEMS.register("corrupted_diamond_pickaxe",
                    () -> new PickaxeItem(ModTiers.CORRUPTED_DIAMOND, 1, -2.8F, new Item.Properties()));

    // --- Dispositivos ---

    /** Peca base: dela nascem a Ancora e, depois, o Localizador de Estruturas. */
    public static final RegistryObject<Item> CORRUPTED_COMPASS = ITEMS.register("corrupted_compass",
            () -> new CorruptedCompassItem(new Item.Properties().stacksTo(1)));

    /** Volta para o spawn. Custa quatro segundos parado e cinco minutos de espera. */
    public static final RegistryObject<Item> ANCHOR = ITEMS.register("anchor",
            () -> new AnchorItem(new Item.Properties().stacksTo(1)));

    /** Faz barulho onde o jogador aponta, e leva o que estiver por perto para la. */
    public static final RegistryObject<Item> LURE_CLOCK = ITEMS.register("lure_clock",
            () -> new LureClockItem(new Item.Properties().stacksTo(1)));

    // --- Localizadores: rito de acesso, poder temporario, expiracao ---

    /** Rastreador de Minerios: 6 min apontando o minerio valioso mais perto, atraves da parede. */
    public static final RegistryObject<Item> ORE_TRACKER = ITEMS.register("ore_tracker",
            () -> new OreTrackerItem(new Item.Properties().stacksTo(1)));

    /** Localizador de Estruturas: 6 min apontando a estrutura mais proxima no horizonte. */
    public static final RegistryObject<Item> STRUCTURE_LOCATOR = ITEMS.register("structure_locator",
            () -> new StructureLocatorItem(new Item.Properties().stacksTo(1)));

    // === REBOOT: cadeia de sucata eletronica / VHS (ver vault "VHSWORLD - REBOOT") ===

    /** A bancada do mod: item do bloco Receptor de Frequencia. */
    public static final RegistryObject<Item> RF_RECEIVER_ITEM = ITEMS.register("rf_receiver",
            () -> new BlockItem(ModBlocks.RF_RECEIVER.get(), new Item.Properties()));

    // --- Tier 1: sucata e eletronicos basicos ---

    /** Sucata de Placa de Circuito. No futuro dropa de monitores/TVs quebrados. */
    public static final RegistryObject<Item> CIRCUIT_SCRAP = ITEMS.register("circuit_scrap",
            () -> new Item(new Item.Properties()));

    /** Cobre Magnetizado: fita de cobre imantada, base dos aparelhos. */
    public static final RegistryObject<Item> MAGNETIZED_COPPER_TAPE = ITEMS.register("magnetized_copper_tape",
            () -> new Item(new Item.Properties()));

    /** Fita Magnetica Virgem: o rolo em branco onde o sinal e gravado. */
    public static final RegistryObject<Item> BLANK_MAGNETIC_TAPE = ITEMS.register("blank_magnetic_tape",
            () -> new Item(new Item.Properties()));

    // --- Tier 2/3: captura de sinal e fusao anomala ---

    /** Residuo de Estatica. No futuro largado pela manifestacao de anomalias. */
    public static final RegistryObject<Item> STATIC_RESIDUE = ITEMS.register("static_residue",
            () -> new Item(new Item.Properties()));

    /** Condensador de Estatica: primeiro fruto do Receptor de Frequencia. */
    public static final RegistryObject<Item> STATIC_CONDENSER = ITEMS.register("static_condenser",
            () -> new Item(new Item.Properties()));

    // --- Tier 2: captura de sinal ---

    /** Sintonizador de Radio Danificado: o radio quebrado que capta o sinal anomalo. */
    public static final RegistryObject<Item> BUSTED_TUNER = ITEMS.register("busted_tuner",
            () -> new Item(new Item.Properties()));

    // --- Tier 3: fusao anomala e biomecanica ---

    /** Tubo CRT Normal: o cinescopio limpo, componente das telas. No futuro dropa de TVs. */
    public static final RegistryObject<Item> CRT_TUBE = ITEMS.register("crt_tube",
            () -> new Item(new Item.Properties()));

    /**
     * Seringa de Vidro Reforcada.
     *
     * Craftavel ja; a mecanica de "usar em si mesmo para tirar Sangue Contaminado" espera
     * o medidor de Infeccao existir (o preco e so pagavel acima de 50%). Fica pronta
     * esperando, como a tesoura afiada.
     */
    public static final RegistryObject<Item> REINFORCED_SYRINGE = ITEMS.register("reinforced_syringe",
            () -> new Item(new Item.Properties().stacksTo(16)));

    /** Sangue Contaminado: a seringa cheia. No futuro tirada de si mesmo com Infeccao alta. */
    public static final RegistryObject<Item> CONTAMINATED_BLOOD = ITEMS.register("contaminated_blood",
            () -> new Item(new Item.Properties().stacksTo(16)));

    /** Tubo CRT Corrompido: o cinescopio infectado, base dos aparelhos de topo. */
    public static final RegistryObject<Item> CORRUPTED_CRT_TUBE = ITEMS.register("corrupted_crt_tube",
            () -> new Item(new Item.Properties()));

    // --- O rasgo e a arma que sai dele ---

    /** O caco arrancado da parede. So a Corrupted Diamond Pickaxe consegue tirar. */
    public static final RegistryObject<Item> REALITY_TEAR = ITEMS.register("reality_tear",
            () -> new Item(new Item.Properties()));

    /**
     * FRACTURE. Dano 8 = 1 do jogador + 3 da lamina + 4 do material.
     * Textura animada (16x128 + .mcmeta), a unica do mod.
     */
    public static final RegistryObject<Item> FRACTURE = ITEMS.register("fracture",
            () -> new FractureItem(ModTiers.CORRUPTED_DIAMOND, 3, -2.4F,
                    new Item.Properties().stacksTo(1)));

    // ===== A FAMILIA DA FRATURA =====
    // Mesmo elemento da espada: haste de ferro batido, cabeca de energia que pulsa
    // em 8 quadros, e o brilho de encantamento por cima (isFoil).

    public static final RegistryObject<Item> FRACTURE_PICKAXE = ITEMS.register("fracture_pickaxe",
            () -> new FracturePickaxeItem(ModTiers.FRACTURE, 1, -2.8F, new Item.Properties()));

    public static final RegistryObject<Item> FRACTURE_AXE = ITEMS.register("fracture_axe",
            () -> new FractureAxeItem(ModTiers.FRACTURE, 5.0F, -3.0F, new Item.Properties()));

    public static final RegistryObject<Item> FRACTURE_SHOVEL = ITEMS.register("fracture_shovel",
            () -> new FractureShovelItem(ModTiers.FRACTURE, 1.5F, -3.0F, new Item.Properties()));

    // O traje. Sem .stacksTo(): peca de armadura ja nasce com durabilidade, e no
    // 1.20.1 durabilidade + stacksTo derruba o jogo na inicializacao.

    public static final RegistryObject<Item> FRACTURE_HELMET = ITEMS.register("fracture_helmet",
            () -> new FractureArmorItem(ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> FRACTURE_CHESTPLATE = ITEMS.register("fracture_chestplate",
            () -> new FractureArmorItem(ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> FRACTURE_LEGGINGS = ITEMS.register("fracture_leggings",
            () -> new FractureArmorItem(ArmorItem.Type.LEGGINGS, new Item.Properties()));

    public static final RegistryObject<Item> FRACTURE_BOOTS = ITEMS.register("fracture_boots",
            () -> new FractureArmorItem(ArmorItem.Type.BOOTS, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}