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
 * A dimensao CHUNKS: pedacos de mundo boiando, ligados por pontes de madeira.
 *
 * Como o da DATA, este gerador nao calcula terreno nenhum — ele carimba as pecas que
 * a planta mandou e deixa o resto vazio. A diferenca esta no que ele NAO faz: aqui
 * nao ha vedacao de parede. Na DATA, ar na borda de uma peca era um buraco para o
 * nada e tinha que virar parede; aqui a borda da ilha e o ponto — o mundo acaba ali,
 * a olhos vistos, e a queda e o assunto.
 */
public class ChunksChunkGenerator extends ChunkGenerator implements DimSpawn {

    public static final Codec<ChunksChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, ChunksChunkGenerator::new));

    private static final int MIN_Y = 0;
    private static final int GEN_HEIGHT = 256;

    /**
     * A faixa da peca que se repete para baixo, para a coluna ficar alta.
     *
     * Medido na peca do Pedro: de y=1 a y=18 e pedra com veios de carvao, ferro,
     * diamante e cascalho; de y=19 para cima ja vira terra. Repetir alem disso poria
     * uma camada de terra no meio da rocha, que o olho le como emenda. y=0 fica de
     * fora porque e a base que ele mesmo fechou.
     */
    private static final int STONE_LO = 1;
    private static final int STONE_SPAN = 18;

    private long seed;
    private volatile ChunksLayout layout;

    public ChunksChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    public BlockPos dimensionSpawn() {
        // SORTEADO, e nao o ponto fixo: regra do Pedro para as 21 — "spawn deve ser em
        // diferentes locais das dimensoes e nunca no mesmo spawn". Quem sabe onde da para
        // ficar de pe e a planta; o motivo de cada escolha esta no `randomSpawn` dela.
        return layout().randomSpawn();
    }

    public ChunksLayout layout() {
        ChunksLayout ready = layout;
        if (ready != null) return ready;
        synchronized (this) {
            if (layout == null) {
                layout = ChunksLayout.build(PieceSet.get("chunks"), seed);
            }
            return layout;
        }
    }

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
        ChunksLayout plan = layout();
        return CompletableFuture.supplyAsync(() -> {
            stamp(plan, chunk);
            return chunk;
        }, executor);
    }

    private void stamp(ChunksLayout plan, ChunkAccess chunk) {
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
            deepen(placement, chunk, cursor, x0, x1, z0, z1);
        }
    }

    /**
     * Estica a coluna para baixo da base da peca.
     *
     * A peca do Pedro e um chunk de 34 blocos: grama, terra e uns 18 de pedra. Isso
     * e um ladrilho, nao uma coluna — de dentro da dimensao se veria o fundo de
     * todas elas de uma vez. Aqui a faixa de pedra reaparece para baixo, com a
     * profundidade que a planta sorteou para esta coluna.
     *
     * A camada de origem de cada altura e sorteada por um hash da posicao, e nao
     * ciclada em ordem: as 18 camadas sao intercambiaveis (todas pedra com veio), e
     * repeti-las na ordem faria o mesmo desenho de minerio reaparecer a cada 18
     * blocos — de fora, uma coluna listrada.
     */
    private void deepen(Placement placement, ChunkAccess chunk, BlockPos.MutableBlockPos cursor,
                        int x0, int x1, int z0, int z1) {
        if (placement.depth <= 0) return;

        for (int d = 1; d <= placement.depth; d++) {
            int y = placement.oy - d;
            if (y < MIN_Y || y >= GEN_HEIGHT) continue;
            int localY = STONE_LO + Math.floorMod(mix(placement.depthSeed, d), STONE_SPAN);

            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    BlockState state = placement.piece.at(
                            placement.localX(x, z), localY, placement.localZ(x, z), placement.rotation);
                    if (state.isAir()) continue;
                    cursor.set(x & 15, y, z & 15);
                    chunk.setBlockState(cursor, state, false);
                }
            }
        }
    }

    private static int mix(long seed, int depth) {
        long h = seed * 6364136223846793005L + depth * 1442695040888963407L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return (int) h;
    }

    // ------------------------------------------------------------------ o resto
    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures,
                             RandomState randomState, ChunkAccess chunk) {
        // A grama, a terra e o minerio ja vieram dentro do chunk que o Pedro recortou:
        // ele nao construiu uma ilha, ele copiou um pedaco de mundo pronto.
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState,
                             BiomeManager biomes, StructureManager structures,
                             ChunkAccess chunk, GenerationStep.Carving step) {
        // Caverna aqui abriria a ilha por baixo e ela se esvaziaria no vazio.
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
        // As arvores ja vem na peca. Deixar o bioma decorar plantaria arvore nova em
        // cima da que o Pedro recortou.
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
        // Agora que cada coluna tem a sua altura, uma constante aqui mandaria o jogo
        // procurar chao no lugar errado — inclusive no meio do vazio, onde nao ha
        // chao nenhum. Sobre o vazio devolve o nivel do spawn: e a resposta menos
        // pior para quem pergunta "onde e o chao" num lugar que nao tem.
        int top = layout().topAt(x, z);
        return (top < 0 ? ChunksLayout.SPAWN_Y : top) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        return new NoiseColumn(MIN_Y, new BlockState[0]);
    }

    @Override
    public void addDebugScreenInfo(List<String> lines, RandomState randomState, BlockPos pos) {
        lines.add("VHSWORLD dimension CHUNKS");
    }

    // ------------------------------------------------------------------ a saida
    @Override
    public String dimensionId() {
        return "chunks";
    }

    /**
     * O hub da planta, e a MESMA sala para toda regiao.
     *
     * ⚠️ IGNORA rx E rz, e nao e preguica. A planta desta dimensao nao e uma grade: ela
     * CRESCE a partir de um hub, peca por peca, e so o hub e alcancavel com certeza — o
     * resto depende de que pecas nasceram por perto, o que a grade do ExitSite nao tem
     * como saber. Devolver um ponto por regiao aqui poria a sala de saida boiando no
     * vazio entre duas pecas, e desde que a fita virou so ida isso e um jogador preso.
     *
     * O preco e conhecido e foi aceito: ha UMA saida nesta dimensao, e o jogador tem que
     * andar ate ela. Foi por isso que ela nao ficou com o metodo EJECT, que precisa de
     * tres salas — ver a nota do reparto no DimensionProfile.
     */
    @Override
    public BlockPos exitAnchor(int rx, int rz) {
        return layout().spawnPos();
    }
}
