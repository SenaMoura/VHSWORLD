package net.vhsworld.rec.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.vhsworld.rec.RECMod;

/** Registro do nosso gerador de terreno. */
public class ModChunkGenerators {

    public static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, RECMod.MOD_ID);

    public static final RegistryObject<Codec<? extends ChunkGenerator>> ALPHA =
            GENERATORS.register("alpha", () -> AlphaChunkGenerator.CODEC);

    public static void register(IEventBus eventBus) {
        GENERATORS.register(eventBus);
    }
}
