package net.vhsworld.rec.worldgen.dim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.vhsworld.rec.RECMod;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A dimensao DATA: as construcoes do Pedro num vazio preto, e mais nada.
 *
 * NAO HA TERRENO. Este gerador nao calcula ruido nem altura: ele so carimba as
 * pecas que a planta (`DimLayout`) mandou. Um chunk que a planta nao toca sai
 * vazio, e o vazio e o assunto — o corredor existe porque alguem o construiu ali,
 * nao porque o mundo cresceu embaixo dele.
 */
public class DataChunkGenerator extends ChunkGenerator implements DimSpawn {

    public static final Codec<DataChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, DataChunkGenerator::new));

    private static final int MIN_Y = 0;
    private static final int GEN_HEIGHT = 128;

    /** Com que se tapa um vao que nao levou a lugar nenhum: o mesmo bloco da parede. */
    private static final BlockState CAP = Blocks.POLISHED_ANDESITE.defaultBlockState();

    /** O que ha nos baus da DATA. */
    private static final ResourceLocation LOOT =
            ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "chests/data");

    private long seed;
    private volatile DimLayout layout;

    public DataChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    /**
     * A planta nasce na primeira vez que alguem pede um chunk, e vale para sempre.
     *
     * Ela e imutavel depois de pronta, entao as varias threads de geracao podem ler
     * a vontade; o cadeado aqui e so para nao montar duas.
     */
    @Override
    public BlockPos dimensionSpawn() {
        return layout().spawnPos();
    }

    public DimLayout layout() {
        DimLayout ready = layout;
        if (ready != null) return ready;
        synchronized (this) {
            if (layout == null) {
                layout = DimLayout.build(PieceSet.get("data"), seed);
            }
            return layout;
        }
    }

    /** Unica janela em que a semente do mundo passa pela nossa mao (ver AlphaChunkGenerator). */
    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSets,
                                                    RandomState randomState, long seed) {
        this.seed = seed;
        return super.createState(structureSets, randomState, seed);
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
                                                        RandomState randomState,
                                                        StructureManager structures, ChunkAccess chunk) {
        DimLayout plan = layout();
        return CompletableFuture.supplyAsync(() -> {
            stamp(plan, chunk);
            return chunk;
        }, executor);
    }

    private void stamp(DimLayout plan, ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        List<Placement> pieces = plan.piecesIn(pos);
        if (pieces.isEmpty()) return;

        int chunkX0 = pos.getMinBlockX(), chunkX1 = pos.getMaxBlockX();
        int chunkZ0 = pos.getMinBlockZ(), chunkZ1 = pos.getMaxBlockZ();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

        for (Placement placement : pieces) {
            int x0 = Math.max(placement.minX(), chunkX0), x1 = Math.min(placement.maxX(), chunkX1);
            int z0 = Math.max(placement.minZ(), chunkZ0), z1 = Math.min(placement.maxZ(), chunkZ1);
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    int localX = placement.localX(x, z);
                    int localZ = placement.localZ(x, z);
                    boolean wall = placement.onOuterWall(localX, localZ);
                    for (int localY = 0; localY < placement.piece.height; localY++) {
                        int y = placement.oy + localY;
                        BlockState state = placement.piece.at(localX, localY, localZ, placement.rotation);
                        if (state.isAir()) {
                            // A VEDACAO. Buraco na parede de fora que nao leva a outra
                            // peca vira parede — inclusive os vaos de enfeite que o
                            // Pedro deixou nas construcoes, que aqui dariam no vazio.
                            if (!wall || placement.isDoorway(x, y, z)) continue;
                            state = CAP;
                        }
                        cursor.set(x & 15, y, z & 15);
                        chunk.setBlockState(cursor, state, false);
                        ocean.update(x & 15, y, z & 15, state);
                        surface.update(x & 15, y, z & 15, state);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ o resto
    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures,
                             RandomState randomState, ChunkAccess chunk) {
        // Nao ha superficie: o piso ja veio pronto dentro da peca.
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState,
                             BiomeManager biomes, StructureManager structures,
                             ChunkAccess chunk, GenerationStep.Carving step) {
        // Nada a cavar. Uma caverna aqui abriria o corredor para o vazio.
    }

    /**
     * Os baus entram AQUI, e nao no carimbo dos blocos.
     *
     * Um bau precisa de bloco-entidade com a tabela de loot dentro, e o carimbo roda
     * fora da thread do servidor, sobre um chunk que ainda nem virou mundo. Aqui ja
     * ha `WorldGenLevel`: da para pousar o bloco e pedir o bloco-entidade dele.
     *
     * O bau nasce VAZIO com a tabela anotada — quem sorteia e o jogo, na primeira vez
     * que alguem abre. E o que faz duas pessoas no mesmo servidor verem o mesmo bau, e
     * o que impede o conteudo de existir antes de alguem ir la.
     */
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
        // Sem arvore, sem minerio, sem lago: o bioma da DATA nao tem feature nenhuma.
        ChunkPos pos = chunk.getPos();
        List<DimLayout.Chest> chests = layout().chestsIn(pos);
        if (chests.isEmpty()) return;

        for (DimLayout.Chest chest : chests) {
            BlockPos where = new BlockPos(chest.x(), chest.y(), chest.z());
            // So dentro do chunk da vez: escrever no vizinho durante a decoracao e
            // como se pega travamento de geracao.
            if (SectionPos.blockToSectionCoord(where.getX()) != pos.x
                    || SectionPos.blockToSectionCoord(where.getZ()) != pos.z) continue;
            if (!level.getBlockState(where).isAir()) continue;

            level.setBlock(where, Blocks.CHEST.defaultBlockState()
                    .setValue(ChestBlock.FACING, chest.facing()), 2);
            if (level.getBlockEntity(where) instanceof RandomizableContainerBlockEntity container) {
                container.setLootTable(LOOT, where.asLong() ^ seed);
            }
        }
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return GEN_HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return MIN_Y;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return DimLayout.FLOOR_Y + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        return new NoiseColumn(MIN_Y, new BlockState[0]);
    }

    @Override
    public void addDebugScreenInfo(List<String> lines, RandomState randomState, BlockPos pos) {
        lines.add("VHSWORLD dimension DATA");
    }
}
