package net.vhsworld.rec.init;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.block.ExitDoorBlock;
import net.vhsworld.rec.block.RealityTearBlock;
import net.vhsworld.rec.block.RFReceiverBlock;

/**
 * Blocos do mod.
 *
 * Por enquanto so o aluminio: o metal da pilha. Ele existe para a bateria deixar de
 * ser um item achado e virar um item PRODUZIDO — quem depende de pilha para nao
 * enlouquecer precisa de um motivo para descer para a caverna.
 */
public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, RECMod.MOD_ID);

    public static final RegistryObject<Block> ALUMINUM_ORE = BLOCKS.register("aluminum_ore",
            () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .strength(3.0F, 3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE),
                    UniformInt.of(0, 2)));

    public static final RegistryObject<Block> DEEPSLATE_ALUMINUM_ORE = BLOCKS.register("deepslate_aluminum_ore",
            () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.DEEPSLATE)
                            .strength(4.5F, 3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.DEEPSLATE),
                    UniformInt.of(0, 2)));

    /**
     * Pedra corrompida.
     *
     * Nasce em veios raros no fundo das cavernas, e e a raiz de quase tudo que o mod
     * constroi depois. Ela existe no MUNDO, e nao so no corpo de um bicho, de proposito:
     * corrupcao que so sai de mob parece loot, enquanto pedra doente crescendo na parede
     * da caverna conta sozinha que alguma coisa esta errada aqui embaixo.
     *
     * Mais dura que pedra comum (o dobro), sem soltar experiencia: nao e minerio, e o
     * mundo apodrecido.
     */
    public static final RegistryObject<Block> CORRUPTED_STONE = BLOCKS.register("corrupted_stone",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(3.0F, 6.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.DEEPSLATE)));

    /**
     * O Rasgo da Realidade. Duro como obsidiana e so entrega o caco para a
     * picareta certa — quem bater com netherite quebra e nao leva nada.
     */
    public static final RegistryObject<Block> REALITY_TEAR = BLOCKS.register("reality_tear",
            () -> new RealityTearBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PINK)
                            .strength(35.0F, 1200.0F)
                            .requiresCorrectToolForDrops()
                            .lightLevel(state -> 4)
                            .noOcclusion()
                            .noCollission()
                            .sound(SoundType.AMETHYST)));

    /**
     * O tripe.
     *
     * Um bloco leve que se planta no chao e filma por voce: chegar perto esconde o teu HUD
     * e liga o monitor. Sem colisao (voce anda por cima dele sem tropecar) e sem ocultar a
     * luz, porque e um armario de pernas finas, nao uma parede. Quebra rapido e na mao.
     */
    public static final RegistryObject<Block> TRIPOD = BLOCKS.register("tripod",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(0.6F)
                            .noOcclusion()
                            .noCollission()
                            .sound(SoundType.METAL)));

    /**
     * O Receptor de Frequencia: a bancada do mod.
     *
     * Uma carcaca de aparelho velho (TV/receptor) que se abre no botao direito. Metal,
     * quebra na picareta, olha para a frente. Onde se processa sinal e se funde
     * eletronico anomalo.
     */
    public static final RegistryObject<Block> RF_RECEIVER = BLOCKS.register("rf_receiver",
            () -> new RFReceiverBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GRAY)
                            .strength(2.5F, 3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)));

    /**
     * Luz Branca: um bloco branco de ponta a ponta que acende sozinho.
     *
     * "como ha janelas que podem mostrar o void, cria um bloco totalmente branco e que
     * brilha e que emite efeito de luz".
     *
     * ⚠️ O PROBLEMA QUE ELE RESOLVE E DE OLHAR PARA CIMA, e vale registrar qual e. As
     * salas da GRASSROOMS tem abobada de vidro (esta na foto do Pedro, e esta na peca),
     * e a abobada e a camada mais alta da peca — a mesma altura em que o Java escrevia o
     * teto. Como a peca e carimbada DEPOIS da casca, o vidro dela apagava o teto e
     * sobrava vidro olhando para o nada. Dentro de um liminal space iluminado, o unico
     * lugar preto era justo o ceu.
     *
     * Nao e `minecraft:light` e nao e um vidro claro: aqueles nao aparecem. Este e uma
     * superficie branca opaca, e e ISSO que o vidro tem que mostrar — o teto de estudio
     * fotografico da foto, sem fonte de luz visivel e sem sombra.
     *
     * Luz 15 porque ele e o SOL da dimensao. E `mapColor` branco para o mapa nao pintar
     * o teto de cinza-pedra.
     */
    public static final RegistryObject<Block> WHITE_LIGHT = BLOCKS.register("white_light",
            () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.SNOW)
                            .strength(0.6F)
                            .lightLevel(state -> 15)
                            .sound(SoundType.GLASS)));

    /**
     * A porta da TRAIN e da PARKOURLAND, no molde da do Dimensional Doors.
     *
     * ⚠️ ELA TEM COLISAO, e a versao anterior nao tinha. Enquanto era um cubo que se
     * atravessava, "abrir" nao existia — encostar ja levava embora. Agora que e uma porta
     * de verdade, a colisao E a mecanica: fechada ela barra, e so depois de aberta o
     * jogador entra no espaco dela e o `entityInside` dispara. Tirar a colisao daqui
     * desmonta a porta inteira.
     *
     * ⚠️ E `noOcclusion`, senao o vazio animado do painel some. Um bloco que oclui faz o
     * jogo descartar as faces vizinhas E aplicar sombreamento de cubo cheio no proprio
     * modelo fino — o painel ficaria chapado e preto, sem o chiado se mexendo dentro, que
     * e a unica coisa que a distingue de uma porta de ferro comum.
     *
     * Luz 7 porque as duas dimensoes que a usam sao escuras e de bruma fechada: sem brilho
     * proprio, a porta apareceria como uma mancha e o jogador passaria direto pela unica
     * saida que ele tem.
     */
    public static final RegistryObject<Block> EXIT_DOOR = BLOCKS.register("exit_door",
            () -> new ExitDoorBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(-1.0F, 3600000.0F)
                            .noLootTable()
                            .noOcclusion()
                            .lightLevel(state -> 7)
                            .sound(SoundType.METAL)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
