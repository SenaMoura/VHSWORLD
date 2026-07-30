package net.vhsworld.rec.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.vhsworld.rec.RECMod;

/** Registro do nosso gerador de terreno e da fonte de biomas que anda com ele. */
public class ModChunkGenerators {

    public static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, RECMod.MOD_ID);

    public static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, RECMod.MOD_ID);

    public static final RegistryObject<Codec<? extends ChunkGenerator>> ALPHA =
            GENERATORS.register("alpha", () -> AlphaChunkGenerator.CODEC);

    public static final RegistryObject<Codec<? extends BiomeSource>> ALPHA_BIOMES =
            BIOME_SOURCES.register("alpha", () -> AlphaBiomeSource.CODEC);

    /**
     * A dimensao DATA. Nao usa fonte de biomas propria: e um bioma so no mapa
     * inteiro (`minecraft:fixed`), porque a variacao dela vem das construcoes, nao
     * do terreno — nao ha terreno.
     */
    public static final RegistryObject<Codec<? extends ChunkGenerator>> DATA =
            GENERATORS.register("data", () -> net.vhsworld.rec.worldgen.dim.DataChunkGenerator.CODEC);

    /** CHUNKS: pedacos de mundo boiando, ligados por pontes. Mesmo esquema da DATA. */
    public static final RegistryObject<Codec<? extends ChunkGenerator>> CHUNKS =
            GENERATORS.register("chunks", () -> net.vhsworld.rec.worldgen.dim.ChunksChunkGenerator.CODEC);

    /** INSIDIOUS: saloes de pedra sem teto sobre o vazio, em labirinto com becos. */
    public static final RegistryObject<Codec<? extends ChunkGenerator>> INSIDIOUS =
            GENERATORS.register("insidious", () -> net.vhsworld.rec.worldgen.dim.InsidiousChunkGenerator.CODEC);

    // ------------------------------------------------------------------ o lote de 6
    //
    // Nenhuma delas usa fonte de biomas propria, pelo mesmo motivo das tres primeiras:
    // e um bioma so no mapa inteiro (`minecraft:fixed`). A variacao de uma dimensao do
    // mod vem da construcao, e nao do terreno — em varias delas nao ha terreno.

    /** VILLAGE: a mesma casa, para sempre, dos dois lados da mesma rua. */
    public static final RegistryObject<Codec<? extends ChunkGenerator>> VILLAGE =
            GENERATORS.register("village", () -> net.vhsworld.rec.worldgen.dim.VillageChunkGenerator.CODEC);

    /** GRASSROOMS: liminal space branco, iluminado, com grama crescendo dentro. */
    public static final RegistryObject<Codec<? extends ChunkGenerator>> GRASSROOMS =
            GENERATORS.register("grassrooms", () -> net.vhsworld.rec.worldgen.dim.GrassroomsChunkGenerator.CODEC);

    /** TRAIN: uma linha de trem reta sobre o vazio, e safe spots de vez em quando. */
    public static final RegistryObject<Codec<? extends ChunkGenerator>> TRAIN =
            GENERATORS.register("train", () -> net.vhsworld.rec.worldgen.dim.TrainChunkGenerator.CODEC);

    /** UNDER PRESSURE: 92 blocos de agua, e submarinos dentro. */
    public static final RegistryObject<Codec<? extends ChunkGenerator>> UNDER_PRESSURE =
            GENERATORS.register("under_pressure",
                    () -> net.vhsworld.rec.worldgen.dim.UnderPressureChunkGenerator.CODEC);

    /** BIBLIOTECA: o salao de estantes ladrilhado, no breu. */
    public static final RegistryObject<Codec<? extends ChunkGenerator>> BIBLIOTECA =
            GENERATORS.register("biblioteca", () -> net.vhsworld.rec.worldgen.dim.BibliotecaChunkGenerator.CODEC);

    /** PARKOURLAND: a unica finita, e a unica de que se cai para fora. */
    public static final RegistryObject<Codec<? extends ChunkGenerator>> PARKOURLAND =
            GENERATORS.register("parkourland",
                    () -> net.vhsworld.rec.worldgen.dim.ParkourlandChunkGenerator.CODEC);

    public static void register(IEventBus eventBus) {
        GENERATORS.register(eventBus);
        BIOME_SOURCES.register(eventBus);
    }
}
