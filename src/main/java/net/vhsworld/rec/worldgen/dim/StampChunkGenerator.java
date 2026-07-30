package net.vhsworld.rec.worldgen.dim;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * O tronco comum das dimensoes do mod: as que nao calculam terreno, so CARIMBAM.
 *
 * ⚠️ POR QUE ISTO EXISTE. As tres primeiras (DATA, CHUNKS, INSIDIOUS) sao tres
 * arquivos de ~230 linhas em que umas 150 sao identicas palavra por palavra: pegar a
 * semente no `createState`, embrulhar o carimbo num `CompletableFuture`, dizer "nao ha
 * superficie" em `buildSurface`, "nao ha o que cavar" em `applyCarvers`, devolver
 * `NoiseColumn` vazio, e o triplo laco que le a peca pulando o ar e atualiza os dois
 * mapas de altura. Ao escrever a QUARTA, essas 150 linhas iam virar 600.
 *
 * Nao e arrumacao por gosto: cada copia daquele triplo laco e um lugar onde esquecer
 * `ocean.update` ou trocar um `x & 15` por `x` da um defeito que so aparece dentro do
 * jogo, num chunk especifico. Com um lugar so, a correcao vale para as 21.
 *
 * As tres primeiras NAO foram migradas de proposito: elas estao em pe e testadas, e
 * mexer nelas para ficar bonito e apostar o que funciona contra o que e simetrico.
 *
 * O que a filha precisa dizer: onde comeca e onde acaba a altura, e o que escrever no
 * chunk (`carve`). Todo o resto ja esta respondido aqui.
 */
public abstract class StampChunkGenerator extends ChunkGenerator implements DimSpawn {

    private long seed;

    protected StampChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    /**
     * A semente do mundo.
     *
     * ⚠️ Ela SO passa pela nossa mao aqui. `ChunkGenerator` nao recebe a semente no
     * construtor (ele nasce do codec, antes de o mundo existir), e `createState` e a
     * unica janela em que o jogo a entrega. Ler `level.getSeed()` nao serve: o carimbo
     * roda sem `Level` na mao.
     */
    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSets,
                                                    RandomState randomState, long seed) {
        this.seed = seed;
        return super.createState(structureSets, randomState, seed);
    }

    protected final long seed() {
        return seed;
    }

    /** O primeiro Y do mundo desta dimensao. */
    protected abstract int minY();

    /** Quantos blocos de altura ela tem, a partir de `minY`. */
    protected abstract int genHeight();

    /** O que esta dimensao escreve neste chunk. */
    protected abstract void carve(Brush brush);

    // ------------------------------------------------------------------ o pincel
    /**
     * Escrever num chunk, sem ter que lembrar das tres pegadinhas toda vez.
     *
     * 1. O `setBlockState` de um chunk quer coordenada LOCAL (0..15), mas tudo que a
     *    planta diz e em coordenada de mundo. Misturar as duas nao da erro: da peca
     *    carimbada no lugar errado, o que so se ve entrando no jogo.
     * 2. Bloco fora do chunk da vez tem que ser DESCARTADO, nao escrito no vizinho.
     *    O vizinho vai pedir o dele e desenhar a sua metade sozinho.
     * 3. Os dois mapas de altura (OCEAN_FLOOR_WG e WORLD_SURFACE_WG) tem que ser
     *    alimentados junto. Quem esquece descobre quando alguem pergunta "onde e o
     *    chao" — spawn, estrutura, mob — e recebe o fundo do mundo.
     */
    protected final class Brush {

        public final ChunkAccess chunk;
        public final ChunkPos pos;
        /** A caixa do chunk em coordenada de MUNDO: o que sair daqui e descartado. */
        public final int x0, z0, x1, z1;

        private final Heightmap ocean;
        private final Heightmap surface;
        private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        private Brush(ChunkAccess chunk) {
            this.chunk = chunk;
            this.pos = chunk.getPos();
            this.x0 = pos.getMinBlockX();
            this.z0 = pos.getMinBlockZ();
            this.x1 = pos.getMaxBlockX();
            this.z1 = pos.getMaxBlockZ();
            this.ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
            this.surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        }

        /** Um bloco, em coordenada de mundo. Fora do chunk ou da altura, nao faz nada. */
        public void set(int worldX, int y, int worldZ, BlockState state) {
            if (worldX < x0 || worldX > x1 || worldZ < z0 || worldZ > z1) return;
            if (y < minY() || y >= minY() + genHeight()) return;
            int lx = worldX & 15, lz = worldZ & 15;
            cursor.set(lx, y, lz);
            chunk.setBlockState(cursor, state, false);
            ocean.update(lx, y, lz, state);
            surface.update(lx, y, lz, state);
        }

        /** O que ja esta escrito ali. Fora do chunk, devolve ar. */
        public BlockState get(int worldX, int y, int worldZ) {
            if (worldX < x0 || worldX > x1 || worldZ < z0 || worldZ > z1) {
                return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            }
            if (y < minY() || y >= minY() + genHeight()) {
                return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            }
            cursor.set(worldX & 15, y, worldZ & 15);
            return chunk.getBlockState(cursor);
        }

        /** Uma coluna inteira do mesmo bloco, de `yFrom` a `yTo` (inclusive). */
        public void column(int worldX, int yFrom, int yTo, int worldZ, BlockState state) {
            for (int y = yFrom; y <= yTo; y++) set(worldX, y, worldZ, state);
        }

        /**
         * Uma peca do Pedro, posta no mundo.
         *
         * O AR E PULADO, e nao carimbado — e isso que faz duas pecas se sobreporem sem
         * uma apagar a outra, e e o que deixa a casca construida pelo Java (piso, rua,
         * agua) continuar de pe por baixo do que a peca nao ocupa.
         */
        public void stamp(Placement placement) {
            int ax0 = Math.max(placement.minX(), x0), ax1 = Math.min(placement.maxX(), x1);
            int az0 = Math.max(placement.minZ(), z0), az1 = Math.min(placement.maxZ(), z1);
            for (int x = ax0; x <= ax1; x++) {
                for (int z = az0; z <= az1; z++) {
                    int localX = placement.localX(x, z);
                    int localZ = placement.localZ(x, z);
                    for (int localY = 0; localY < placement.piece.height; localY++) {
                        BlockState state = placement.piece.at(localX, localY, localZ, placement.rotation);
                        if (state.isAir()) continue;
                        set(x, placement.oy + localY, z, state);
                    }
                }
            }
        }

        /** A peca encosta neste chunk? */
        public boolean touches(Placement placement) {
            return placement.maxX() >= x0 && placement.minX() <= x1
                    && placement.maxZ() >= z0 && placement.minZ() <= z1;
        }
    }

    // ------------------------------------------------------------------ o ciclo
    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
                                                        RandomState randomState,
                                                        StructureManager structures, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            carve(new Brush(chunk));
            return chunk;
        }, executor);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures,
                             RandomState randomState, ChunkAccess chunk) {
        // Nao ha terreno de ruido para receber superficie: o chao ja veio carimbado.
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState,
                             BiomeManager biomes, StructureManager structures,
                             ChunkAccess chunk, GenerationStep.Carving step) {
        // Caverna aqui abriria o piso por baixo — e em varias delas o piso tem 1 bloco.
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
        // Por padrao nada: o bioma destas dimensoes nao tem feature nenhuma. Quem
        // precisa de bloco-entidade (bau) sobrescreve — la ja existe `WorldGenLevel`.
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return genHeight();
    }

    @Override
    public int getMinY() {
        return minY();
    }

    @Override
    public int getSeaLevel() {
        return minY();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        return new NoiseColumn(minY(), new BlockState[0]);
    }

    @Override
    public void addDebugScreenInfo(List<String> lines, RandomState randomState, BlockPos pos) {
        lines.add("VHSWORLD dimension " + name());
    }

    /** O nome que aparece no F3. */
    protected abstract String name();
}
