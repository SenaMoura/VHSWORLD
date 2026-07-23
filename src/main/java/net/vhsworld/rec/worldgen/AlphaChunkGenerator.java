package net.vhsworld.rec.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.vhsworld.rec.RECMod;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * O terreno alpha, escrito aqui.
 *
 * COMO FUNCIONA, em uma frase: o mundo nao e um mapa de altura, e um campo de
 * densidade em tres dimensoes. Para cada ponto do espaco existe um numero; onde ele
 * passa de zero vira pedra, onde nao passa vira ar. E por isso que o terreno antigo
 * tem paredoes, arcos e ilhas flutuantes — coisas que um mapa de altura nao consegue
 * descrever, porque nele cada coluna tem um topo e mais nada.
 *
 * O campo nao e calculado bloco a bloco: seria caro e, pior, ficaria ruidoso demais.
 * Ele e calculado numa grade grossa (a cada 4 blocos na horizontal e 8 na vertical) e
 * interpolado no meio. Essa grade grossa E o visual antigo: os degraus largos e as
 * encostas retas da sua screenshot saem da interpolacao, nao do ruido.
 */
public class AlphaChunkGenerator extends ChunkGenerator {

    public static final Codec<AlphaChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, AlphaChunkGenerator::new));

    // --- forma do mundo ---
    private static final int MIN_Y = 0;
    private static final int GEN_HEIGHT = 128;      // o teto de sempre
    private static final int SEA_LEVEL = 64;

    // --- grade grossa ---
    private static final int CELL_XZ = 4;
    private static final int CELL_Y = 8;
    private static final int GRID_XZ = 16 / CELL_XZ + 1;         // 5
    private static final int GRID_Y = GEN_HEIGHT / CELL_Y + 1;   // 17

    // --- escalas do ruido, os numeros que dao o "sabor" do terreno ---
    private static final double XZ_SCALE = 684.412 / 4096.0;
    private static final double Y_SCALE = 684.412 / 8192.0;
    private static final double MAIN_XZ = XZ_SCALE / 80.0;
    private static final double MAIN_Y = Y_SCALE / 160.0;
    private static final double DEPTH_SCALE = 0.0625;

    private final BlockState stone = Blocks.STONE.defaultBlockState();
    private final BlockState water = Blocks.WATER.defaultBlockState();
    private final BlockState air = Blocks.AIR.defaultBlockState();

    private AlphaNoise minLimit;
    private AlphaNoise maxLimit;
    private AlphaNoise main;
    private AlphaNoise depth;
    private AlphaNoise surface;
    private boolean noiseReady = false;

    private final AlphaCaves caves = new AlphaCaves(SEA_LEVEL, GEN_HEIGHT);

    public AlphaChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    /**
     * O gerador nao recebe a semente no construtor no Minecraft moderno; ela chega
     * junto com o RandomState, ja no meio da geracao. Entao o ruido nasce na
     * primeira vez que alguem pede um chunk.
     */
    private void ensureNoise(RandomState randomState) {
        if (noiseReady) return;
        synchronized (this) {
            if (noiseReady) return;
            // O gerador nao ve a semente do mundo; o que ele tem e a fabrica de
            // aleatorios do RandomState, que ja nasce derivada dela. Pedir um ramo
            // com nome nosso da uma sequencia estavel e so nossa.
            RandomSource random = randomState
                    .getOrCreateRandomFactory(new ResourceLocation(RECMod.MOD_ID, "alpha_terrain"))
                    .fromHashOf("terrain");
            minLimit = new AlphaNoise(random, 16);
            maxLimit = new AlphaNoise(random, 16);
            main = new AlphaNoise(random, 8);
            depth = new AlphaNoise(random, 16);
            surface = new AlphaNoise(random, 4);
            noiseReady = true;
        }
    }

    // ------------------------------------------------------------------ densidade

    /**
     * O numero que decide pedra ou ar.
     *
     * Sao tres ruidos: dois "limites" que descrevem terrenos diferentes e um terceiro
     * que escolhe entre eles. Misturar dois mundos com um seletor e o que produz as
     * transicoes bruscas do terreno antigo — penhasco de um lado, planicie do outro,
     * sem nada suavizando no meio.
     */
    private double density(int x, int y, int z) {
        double d = depth.sample2d(x, z, DEPTH_SCALE) * 0.5;
        double base = (SEA_LEVEL + 6.0) + d * 26.0;

        // O gradiente e o que decide se ha ilha flutuante ou nao.
        //
        // Ele puxa a densidade para baixo conforme se sobe. Se puxar forte, o
        // terreno vira mapa de altura: nada sobrevive destacado no ar. Se puxar de
        // menos, o ceu enche de pedra e o mundo fica ilegivel. 0.085 e o ponto em
        // que o ruido ainda vence o gradiente uns 15 blocos acima do chao — o
        // suficiente para arrancar um pedaco de terreno e deixa-lo boiando.
        double gradient = (base - y) * 0.085;

        double lo = minLimit.sample(x, y, z, XZ_SCALE, Y_SCALE) * 0.75;
        double hi = maxLimit.sample(x, y, z, XZ_SCALE, Y_SCALE) * 0.75;
        double selector = (main.sample(x, y, z, MAIN_XZ, MAIN_Y) * 8.0 + 1.0) * 0.5;

        double value;
        if (selector <= 0.0) {
            value = lo;
        } else if (selector >= 1.0) {
            value = hi;
        } else {
            value = Mth.lerp(selector, lo, hi);
        }

        value += gradient;

        // Perto do teto o terreno afina, senao o mundo fecha numa tampa de pedra.
        // A tampa do mundo. Comeca alto e sobe devagar, para cortar a pedra colada
        // no teto sem apagar as ilhas que nascem no meio do caminho.
        int fadeStart = GEN_HEIGHT - 16;
        if (y > fadeStart) {
            double t = (y - fadeStart) / 16.0;
            value -= t * t * 8.0;
        }
        return value;
    }

    // ------------------------------------------------------------------ terreno

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
                                                        RandomState randomState,
                                                        StructureManager structures, ChunkAccess chunk) {
        ensureNoise(randomState);
        return CompletableFuture.supplyAsync(() -> {
            fillChunk(chunk);
            return chunk;
        }, executor);
    }

    private void fillChunk(ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        int originX = pos.getMinBlockX();
        int originZ = pos.getMinBlockZ();

        // Grade grossa: 5 x 17 x 5 amostras por chunk, e nao 16 x 128 x 16.
        double[][][] grid = new double[GRID_XZ][GRID_Y][GRID_XZ];
        for (int gx = 0; gx < GRID_XZ; gx++) {
            for (int gz = 0; gz < GRID_XZ; gz++) {
                for (int gy = 0; gy < GRID_Y; gy++) {
                    grid[gx][gy][gz] = density(originX + gx * CELL_XZ,
                                               MIN_Y + gy * CELL_Y,
                                               originZ + gz * CELL_XZ);
                }
            }
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surfaceMap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < GEN_HEIGHT; y++) {
                    double value = interpolate(grid, x, y, z);

                    BlockState state;
                    if (value > 0.0) {
                        state = stone;
                    } else if (y < SEA_LEVEL) {
                        state = water;
                    } else {
                        state = air;
                    }

                    if (state != air) {
                        cursor.set(x, MIN_Y + y, z);
                        chunk.setBlockState(cursor, state, false);
                        ocean.update(x, MIN_Y + y, z, state);
                        surfaceMap.update(x, MIN_Y + y, z, state);
                    }
                }
            }
        }
    }

    /** Interpolacao trilinear dentro de uma celula da grade. */
    private double interpolate(double[][][] grid, int x, int y, int z) {
        int gx = x / CELL_XZ, gz = z / CELL_XZ, gy = y / CELL_Y;
        double fx = (x % CELL_XZ) / (double) CELL_XZ;
        double fz = (z % CELL_XZ) / (double) CELL_XZ;
        double fy = (y % CELL_Y) / (double) CELL_Y;

        double c000 = grid[gx][gy][gz],         c100 = grid[gx + 1][gy][gz];
        double c001 = grid[gx][gy][gz + 1],     c101 = grid[gx + 1][gy][gz + 1];
        double c010 = grid[gx][gy + 1][gz],     c110 = grid[gx + 1][gy + 1][gz];
        double c011 = grid[gx][gy + 1][gz + 1], c111 = grid[gx + 1][gy + 1][gz + 1];

        double x00 = Mth.lerp(fx, c000, c100), x01 = Mth.lerp(fx, c001, c101);
        double x10 = Mth.lerp(fx, c010, c110), x11 = Mth.lerp(fx, c011, c111);

        return Mth.lerp(fy, Mth.lerp(fz, x00, x01), Mth.lerp(fz, x10, x11));
    }

    // ------------------------------------------------------------------ superficie

    /**
     * Grama por cima, terra embaixo, areia na beira d'agua.
     *
     * O alpha nao tinha regra de superficie por bioma: era esta, para o mundo
     * inteiro. A praia sai de um ruido proprio, e nao de "esta perto do mar" —
     * por isso as praias antigas apareciam tambem em lagos no meio do nada.
     */
    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures,
                             RandomState randomState, ChunkAccess chunk) {
        ensureNoise(randomState);

        ChunkPos pos = chunk.getPos();
        int originX = pos.getMinBlockX();
        int originZ = pos.getMinBlockZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState sand = Blocks.SAND.defaultBlockState();
        BlockState gravel = Blocks.GRAVEL.defaultBlockState();
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                double beach = surface.sample2d(originX + x, originZ + z, 0.03125);
                boolean sandy = beach + 0.15 > 0.0;

                int depthLeft = -1;
                for (int y = GEN_HEIGHT - 1; y >= MIN_Y; y--) {
                    cursor.set(x, y, z);
                    BlockState here = chunk.getBlockState(cursor);

                    if (here.isAir()) {
                        depthLeft = -1;
                        continue;
                    }
                    if (!here.is(Blocks.STONE)) continue;

                    if (depthLeft == -1) {
                        // Primeiro solido de cima para baixo: e a superficie.
                        depthLeft = 4;
                        if (y >= SEA_LEVEL - 1) {
                            chunk.setBlockState(cursor, sandy && y <= SEA_LEVEL + 1 ? sand : grass, false);
                        } else {
                            chunk.setBlockState(cursor, sandy ? sand : gravel, false);
                        }
                    } else if (depthLeft > 0) {
                        depthLeft--;
                        chunk.setBlockState(cursor, sandy && y >= SEA_LEVEL - 2 ? sand : dirt, false);
                    }
                }

                // Fundo do mundo irregular, como sempre foi.
                chunk.setBlockState(cursor.set(x, MIN_Y, z), bedrock, false);
            }
        }
    }

    // ------------------------------------------------------------------ o resto

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState,
                             BiomeManager biomes, StructureManager structures,
                             ChunkAccess chunk, GenerationStep.Carving step) {
        if (step != GenerationStep.Carving.AIR) return;
        ensureNoise(randomState);

        // Cada chunk sorteia os proprios tuneis a partir de uma semente estavel de
        // posicao: o mesmo mundo da sempre a mesma caverna, e um tunel que nasce
        // longe chega inteiro aqui.
        var factory = randomState.getOrCreateRandomFactory(
                new ResourceLocation(RECMod.MOD_ID, "alpha_caves"));

        caves.carve(chunk, (cx, cz) -> factory.at(cx, 0, cz));
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // Sem a leva inicial de mobs; o spawn normal continua funcionando.
    }

    @Override
    public int getGenDepth() {
        return GEN_HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return SEA_LEVEL;
    }

    @Override
    public int getMinY() {
        return MIN_Y;
    }

    /**
     * Altura do terreno numa coluna. Nao e enfeite: e o que as estruturas usam para
     * decidir onde pousar. Sem isto, vila nasce dentro da pedra ou flutuando.
     */
    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        ensureNoise(randomState);
        for (int y = GEN_HEIGHT - 1; y >= MIN_Y; y--) {
            if (density(x, y, z) > 0.0) {
                return y + 1;
            }
        }
        return type == Heightmap.Types.WORLD_SURFACE_WG ? SEA_LEVEL : MIN_Y;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        ensureNoise(randomState);
        BlockState[] column = new BlockState[GEN_HEIGHT];
        for (int y = 0; y < GEN_HEIGHT; y++) {
            double value = density(x, MIN_Y + y, z);
            column[y] = value > 0.0 ? stone : (MIN_Y + y < SEA_LEVEL ? water : air);
        }
        return new NoiseColumn(MIN_Y, column);
    }

    @Override
    public void addDebugScreenInfo(List<String> lines, RandomState randomState, BlockPos pos) {
        lines.add("VHSWORLD alpha terrain");
    }
}
