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
import net.vhsworld.rec.block.RealityTearBlock;

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

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
