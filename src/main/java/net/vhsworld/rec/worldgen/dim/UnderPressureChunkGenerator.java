package net.vhsworld.rec.worldgen.dim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A dimensao UNDER PRESSURE: 90 blocos de agua, e submarinos dentro dela.
 *
 * "um lugar submerso com diversos submarinos, o jogador spawna na superficie em um
 * submarino". Duas cascas de concreto: a de SPAWN, que tem escada e alcapao e fura a
 * linha d'agua, e a AFUNDADA, que tem mastro e nenhuma porta — a que se acha.
 *
 * ============================ A AGUA E O PROBLEMA INTEIRO ============================
 *
 * Encher de agua e facil. O dificil e que o submarino tem que ficar SECO por dentro, e
 * "por dentro" nao e a caixa da peca: a caixa e um paralelepipedo e o submarino e
 * arredondado, com uma torre estreita em cima. Enchendo tudo menos a caixa, sobrariam
 * bolsoes de ar colados no casco por fora — bolha de ar quadrada no meio do mar, que e
 * a coisa mais facil de notar que existe.
 *
 * ⚠️ Ent a caixa seca e calculada POR ALTURA, e nao uma so para a peca toda: para cada
 * y da peca eu meco a caixa dos blocos SOLIDOS naquele nivel e encolho um bloco para
 * dentro. No nivel da barriga isso da a cabine inteira; no nivel da torre, so o furo da
 * torre — e e por isso que o teto do casco nao amanhece com uma laje de ar em cima dele.
 * A conta e feita uma vez por peca, na primeira leitura, e nao por chunk.
 *
 * A ordem tambem importa e nao e simetrica: primeiro o leito e o casco, DEPOIS a agua,
 * e a agua so entra onde ficou ar. Enchendo antes, a cabine nasceria cheia e nao haveria
 * como saber depois o que era cabine e o que era mar.
 */
public class UnderPressureChunkGenerator extends StampChunkGenerator {

    public static final Codec<UnderPressureChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, UnderPressureChunkGenerator::new));

    private static final int MIN_Y = 0;
    private static final int GEN_HEIGHT = 128;

    /** O leito. Existe para a agua ter em que se apoiar — sem ele ela vaza no vazio. */
    private static final int BED_Y = 4;

    /** A linha d'agua. 92 blocos de coluna: o fundo nao se ve de cima. */
    public static final int SEA_Y = 96;

    /** De um submarino ao proximo. Em 64, ve-se um por vez e nunca dois. */
    private static final int CELL = 64;

    /** A chance de uma casa ter submarino. O resto e agua e mais nada. */
    private static final double SUB_CHANCE = 0.75D;

    /** Um em cada quatro e o de superficie — e sao esses que a fita pode usar. */
    private static final int SURFACE_ONE_IN = 4;

    /** Quanto o afundado pode estar acima do leito. */
    private static final int DEEP_SPREAD = 56;

    private static final int SPAWN_SPREAD = 64;
    private static final int SPAWN_TRIES = 256;

    private static final BlockState ROCK = Blocks.STONE.defaultBlockState();
    private static final BlockState BED = Blocks.GRAVEL.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();

    private volatile DimPiece surface;
    private volatile DimPiece deep;
    private volatile boolean looked;

    /** A caixa seca de cada peca, por altura local: {x0,z0,x1,z1} ou null. */
    private final Map<String, int[][]> dryBoxes = new HashMap<>();

    public UnderPressureChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    private void look() {
        if (looked) return;
        synchronized (this) {
            if (looked) return;
            PieceSet set = PieceSet.get("under_pressure");
            surface = set.byName("sub_spawn");
            deep = set.byName("sub_deep");
            if (surface != null) dryBoxes.put(surface.name, measure(surface));
            if (deep != null) dryBoxes.put(deep.name, measure(deep));
            looked = true;
        }
    }

    /**
     * A caixa seca da peca, nivel por nivel.
     *
     * Mede a caixa dos blocos solidos de cada altura e encolhe 1 para dentro. Onde nao
     * sobrar nada (a quilha, que tem 1 bloco de largura), a resposta e null e aquele
     * nivel inteiro recebe agua — o que esta certo: nao ha cabine na quilha.
     */
    private static int[][] measure(DimPiece piece) {
        int[][] boxes = new int[piece.height][];
        for (int y = 0; y < piece.height; y++) {
            int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = -1, z1 = -1;
            for (int x = 0; x < piece.width; x++) {
                for (int z = 0; z < piece.length; z++) {
                    if (piece.at(x, y, z, 0).isAir()) continue;
                    x0 = Math.min(x0, x);
                    x1 = Math.max(x1, x);
                    z0 = Math.min(z0, z);
                    z1 = Math.max(z1, z);
                }
            }
            if (x1 - x0 < 2 || z1 - z0 < 2) continue;
            boxes[y] = new int[]{x0 + 1, z0 + 1, x1 - 1, z1 - 1};
        }
        return boxes;
    }

    // ------------------------------------------------------------------ a grade
    private boolean occupied(int cx, int cz) {
        return DimHash.frac(seed(), cx, cz, 31L) < SUB_CHANCE;
    }

    private boolean atSurface(int cx, int cz) {
        return DimHash.pick(seed(), cx, cz, 32L, SURFACE_ONE_IN) == 0;
    }

    /** O submarino desta casa, ou null se ali so ha agua. */
    private Placement subAt(int cx, int cz) {
        if (!occupied(cx, cz)) return null;
        boolean top = atSurface(cx, cz);
        DimPiece piece = top ? surface : deep;
        if (piece == null) return null;

        int rotation = top ? DimHash.pick(seed(), cx, cz, 33L, 2) * 2 : DimHash.pick(seed(), cx, cz, 33L, 4);
        int ox = cx * CELL + 12 + DimHash.pick(seed(), cx, cz, 34L, CELL - 40);
        int oz = cz * CELL + 12 + DimHash.pick(seed(), cx, cz, 35L, CELL - 40);
        // O de superficie tem altura fixa: a torre tem que furar a linha d'agua, e um
        // bloco a mais ou a menos e a diferenca entre "submarino emergido" e "submarino
        // que afundou com a torre de fora".
        int oy = top ? SEA_Y - (piece.height - 3)
                : BED_Y + 1 + DimHash.pick(seed(), cx, cz, 36L, DEEP_SPREAD);
        return new Placement(piece, rotation, ox, oy, oz);
    }

    // ------------------------------------------------------------------ o carimbo
    @Override
    protected void carve(Brush brush) {
        look();
        floor(brush);

        List<Placement> subs = new ArrayList<>(4);
        int cx0 = Math.floorDiv(brush.x0 - CELL, CELL), cx1 = Math.floorDiv(brush.x1, CELL);
        int cz0 = Math.floorDiv(brush.z0 - CELL, CELL), cz1 = Math.floorDiv(brush.z1, CELL);
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                Placement sub = subAt(cx, cz);
                if (sub == null || !brush.touches(sub)) continue;
                brush.stamp(sub);
                subs.add(sub);
            }
        }
        flood(brush, subs);
    }

    private void floor(Brush brush) {
        for (int x = brush.x0; x <= brush.x1; x++) {
            for (int z = brush.z0; z <= brush.z1; z++) {
                brush.column(x, MIN_Y, BED_Y - 1, z, ROCK);
                brush.set(x, BED_Y, z, BED);
            }
        }
    }

    /** A agua, em tudo que ficou ar e nao e cabine. */
    private void flood(Brush brush, List<Placement> subs) {
        for (int x = brush.x0; x <= brush.x1; x++) {
            for (int z = brush.z0; z <= brush.z1; z++) {
                for (int y = BED_Y + 1; y <= SEA_Y; y++) {
                    if (!brush.get(x, y, z).isAir()) continue;
                    if (dry(subs, x, y, z)) continue;
                    brush.set(x, y, z, WATER);
                }
            }
        }
    }

    /** Este ponto esta dentro da cabine de algum submarino? */
    private boolean dry(List<Placement> subs, int worldX, int y, int worldZ) {
        for (Placement sub : subs) {
            int localY = y - sub.oy;
            if (localY < 0 || localY >= sub.piece.height) continue;
            int[][] boxes = dryBoxes.get(sub.piece.name);
            if (boxes == null) continue;
            int[] box = boxes[localY];
            if (box == null) continue;
            // A caixa esta em coordenada da PECA; converte-la e mais seguro que tentar
            // girar a caixa, porque a formula do giro ja mora na Placement.
            int lx = sub.localX(worldX, worldZ);
            int lz = sub.localZ(worldX, worldZ);
            if (lx >= box[0] && lx <= box[2] && lz >= box[1] && lz <= box[3]) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ o spawn
    /**
     * Em cima da torre de um submarino de superficie sorteado.
     *
     * A busca e por sorteio repetido e nao por espiral: uns 19% das casas tem submarino
     * de superficie, entao a chance de 256 tentativas falharem todas e menor que uma em
     * 10^24. E se falhar mesmo assim, a reserva e a propria linha d'agua — molhado, mas
     * nunca dentro de um bloco.
     */
    @Override
    public BlockPos dimensionSpawn() {
        look();
        java.util.Random dice = new java.util.Random();
        for (int tries = 0; tries < SPAWN_TRIES; tries++) {
            int cx = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
            int cz = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
            if (!occupied(cx, cz) || !atSurface(cx, cz)) continue;
            Placement sub = subAt(cx, cz);
            if (sub == null) continue;
            // O concreto do topo da torre, em coordenada da peca.
            //
            // ⚠️ x=1 e NAO x=2, e isto foi medido: o meio do topo da torre e o ALCAPAO
            // (dois `birch_trapdoor` em z=7 e z=8), que e por onde se entra. Nascer em
            // cima de um alcapao e ficar de pe numa laje de tres pixels que abre — e se
            // ele estiver aberto, nao ha nada ali. O anel de concreto em volta e chao.
            int x = sub.worldX(1, 7);
            int z = sub.worldZ(1, 7);
            return new BlockPos(x, sub.oy + sub.piece.height, z);
        }
        return new BlockPos(0, SEA_Y + 1, 0);
    }

    // ------------------------------------------------------------------ o resto
    @Override
    protected Codec<? extends net.minecraft.world.level.chunk.ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    protected int minY() {
        return MIN_Y;
    }

    @Override
    protected int genHeight() {
        return GEN_HEIGHT;
    }

    @Override
    protected String name() {
        return "UNDER PRESSURE";
    }

    @Override
    public int getSeaLevel() {
        return SEA_Y;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return BED_Y + 1;
    }

    // ------------------------------------------------------------------ a saida
    @Override
    public String dimensionId() {
        return "under_pressure";
    }

    /**
     * No leito, no meio da casa.
     *
     * ⚠️ A SALA FICA DEBAIXO D'AGUA E ALAGA pelos quatro vaos, e isso e aceito e nao
     * esquecido. Fechar os vaos daria uma caixa estanque que ninguem abre sem picareta —
     * pior. E a camara escura alagada continua funcionando: a lampada vermelha acende na
     * agua e o tanque tambem. O jogador chega nadando, que e como se chega a tudo aqui.
     */
    @Override
    public BlockPos exitAnchor(int rx, int rz) {
        int cx = ExitSite.cellInRegion(rx, CELL);
        int cz = ExitSite.cellInRegion(rz, CELL);
        return new BlockPos(cx * CELL + CELL / 2, BED_Y + 1, cz * CELL + CELL / 2);
    }
}
