package net.vhsworld.rec.worldgen.dim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
 * A dimensao INSIDIOUS: saloes de pedra sem teto sobre o vazio preto.
 *
 * Como os da DATA e da CHUNKS, este gerador nao calcula terreno nenhum — ele carimba as
 * pecas que a planta mandou e deixa o resto vazio.
 *
 * O que ele NAO faz, e de proposito:
 *
 * - NAO VEDA PAREDE. Na DATA, ar na borda de uma peca era buraco para o nada e virava
 *   parede. Aqui a peca ja nasce com os dois muros do corredor, e o que esta aberto —
 *   o alto e as duas pontas de um beco — esta aberto porque e o assunto. O Pedro
 *   escolheu "sem teto, vazio preto" olhando as duas opcoes lado a lado.
 * - NAO ESTICA COLUNA. A CHUNKS repete a faixa de pedra da ilha para baixo porque uma
 *   ilha de 33 blocos boiando e um ladrilho, nao uma coluna. Aqui nao ha coluna: o
 *   corredor E uma fita fina de pedra, e ver que ela e fina e parte do medo.
 * - NAO DECORA pelo bioma. Nao ha chao de bioma nenhum onde plantar coisa.
 */
public class InsidiousChunkGenerator extends ChunkGenerator implements DimSpawn {

    public static final Codec<InsidiousChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, InsidiousChunkGenerator::new));

    private static final int MIN_Y = 0;
    private static final int GEN_HEIGHT = 128;

    private long seed;
    private volatile InsidiousLayout layout;

    public InsidiousChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    /**
     * A planta nasce na primeira vez que alguem pede um chunk, e vale para sempre.
     *
     * Ela nao guarda mapa nenhum — e funcao pura de (casa, semente) — entao as varias
     * threads de geracao podem ler a vontade; o cadeado aqui e so para nao montar duas.
     */
    public InsidiousLayout layout() {
        InsidiousLayout ready = layout;
        if (ready != null) return ready;
        synchronized (this) {
            if (layout == null) {
                layout = InsidiousLayout.build(PieceSet.get("insidious"), seed);
            }
            return layout;
        }
    }

    @Override
    public BlockPos dimensionSpawn() {
        // SORTEADO, e nao o ponto fixo: regra do Pedro para as 21 — "spawn deve ser em
        // diferentes locais das dimensoes e nunca no mesmo spawn". Quem sabe onde da para
        // ficar de pe e a planta; o motivo de cada escolha esta no `randomSpawn` dela.
        return layout().randomSpawn();
    }

    /** Unica janela em que a semente do mundo passa pela nossa mao. */
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
        InsidiousLayout plan = layout();
        return CompletableFuture.supplyAsync(() -> {
            stamp(plan, chunk);
            return chunk;
        }, executor);
    }

    /**
     * Carimba as pecas deste chunk, na ordem em que a planta as entregou.
     *
     * ⚠️ A ORDEM E SIGNIFICATIVA: a planta poe os corredores antes dos cruzamentos, para
     * que a fileira da emenda fique selada (ver `InsidiousLayout.piecesIn`). Reordenar
     * esta lista reabriria os dois buracos ao lado da porta da sala.
     *
     * O ar e PULADO, e nao carimbado. E isso que faz a sobreposicao funcionar: onde o
     * cruzamento tem ar, o que o corredor ja pos continua de pe.
     */
    private void stamp(InsidiousLayout plan, ChunkAccess chunk) {
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
                    for (int localY = 0; localY < placement.piece.height; localY++) {
                        BlockState state = placement.piece.at(localX, localY, localZ, placement.rotation);
                        if (state.isAir()) continue;
                        int y = placement.oy + localY;
                        if (y < MIN_Y || y >= GEN_HEIGHT) continue;
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
        // Nao ha terreno para receber superficie: a pedra ja veio com a peca.
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState,
                             BiomeManager biomes, StructureManager structures,
                             ChunkAccess chunk, GenerationStep.Carving step) {
        // Caverna aqui abriria o piso do corredor por baixo, e ele tem 1 bloco.
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
        // Sem loot ainda: quem mora na INSIDIOUS e o que ha nela e decisao do Pedro,
        // junto com o coracao que ele vai modelar.
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

    /**
     * A dimensao inteira e um plano so, entao a resposta e sempre a mesma.
     *
     * Ao contrario da CHUNKS, aqui nao ha altura por coluna a consultar: o piso esta em
     * FLOOR_Y em todo lugar que tem piso, e onde nao tem, esta e a resposta menos pior
     * para quem pergunta "onde e o chao" num lugar que nao tem chao.
     */
    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return InsidiousLayout.FLOOR_Y + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        return new NoiseColumn(MIN_Y, new BlockState[0]);
    }

    @Override
    public void addDebugScreenInfo(List<String> lines, RandomState randomState, BlockPos pos) {
        lines.add("VHSWORLD dimension INSIDIOUS");
    }
}
