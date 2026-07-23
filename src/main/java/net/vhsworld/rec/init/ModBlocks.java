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

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
