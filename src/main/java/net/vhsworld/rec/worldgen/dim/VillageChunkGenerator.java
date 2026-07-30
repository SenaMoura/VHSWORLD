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

/**
 * A dimensao VILLAGE: a mesma casa, para sempre, dos dois lados da mesma rua.
 *
 * A instrucao do Pedro e literal — "um lugar plano com diversas casas identicas, elas
 * devem estar alinhadas uma com as outras infinitamente" — e a foto que ele deu e uma
 * rua de casas iguais sumindo no horizonte, sol a pino, ceu azul chapado. Entao o medo
 * daqui NAO e escuridao: e reconhecer a casa. Voce anda dez minutos e chega na mesma
 * esquina, e nao ha como provar que e outra.
 *
 * Por isso a casa nao varia. Sortear cor de parede ou girar uma aqui e outra la deixaria
 * o lugar mais bonito e desmancharia exatamente o efeito — com variacao, andar passa a
 * levar a algum lugar.
 *
 * ============================ COMO ELA E DESENHADA ============================
 *
 * Nao ha planta guardada e nao ha nada crescido: cada bloco sai de uma conta sobre a
 * propria coordenada dele. Toda a dimensao e o resto de duas divisoes.
 *
 *   u = x mod 64   onde estou na FAIXA que atravessa a rua
 *   v = z mod 32   onde estou no LOTE ao longo dela
 *
 * A faixa de 64, de fora para dentro: quintal, casa, gramado, calcada, RUA, calcada,
 * gramado, casa, quintal. As duas fileiras encaram a rua porque a de baixo entra girada
 * 180 — e da para saber para que lado a casa olha porque a porta dela e o vao na parede
 * leste (medido no schematic: x=15, z=8..9), e nao um palpite.
 *
 * NAO HA RUA TRANSVERSAL, de proposito. Uma malha de quadras daria esquina, e esquina
 * da referencia: "virei a direita duas vezes, entao voltei". Ruas paralelas que nunca
 * se cruzam tiram esse apoio — o unico jeito de sair da rua e atravessar quintal.
 */
public class VillageChunkGenerator extends StampChunkGenerator {

    public static final Codec<VillageChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, VillageChunkGenerator::new));

    private static final int MIN_Y = 0;
    private static final int GEN_HEIGHT = 128;

    /** O topo da grama. Tudo aqui e plano: nao existe segunda altura nesta dimensao. */
    public static final int GROUND_Y = 64;

    private static final int DIRT_DEPTH = 3;    // GROUND_Y-1 .. GROUND_Y-3
    private static final int STONE_DEPTH = 5;   // e mais cinco de pedra embaixo

    /** De uma rua a proxima. A casa tem 18 de largura e cabem duas fileiras e as sobras. */
    private static final int PERIOD_X = 64;

    /** De uma casa a seguinte ao longo da rua: 17 de casa e 15 de gramado. */
    private static final int PERIOD_Z = 32;

    // A faixa de 64, em coordenada u. Os numeros nao sao gosto: a casa mede 18, a rua
    // de 8 e a largura em que dois carros passam e ainda se ve o outro lado, e a
    // calcada de 2 e o minimo em que um poste nao fica no meio do caminho.
    private static final int ROAD_MIN = 28, ROAD_MAX = 35;
    private static final int CURB_NEAR = 26, CURB_FAR = 37;   // 26,27 e 36,37
    private static final int HOUSE_A_X = 4;    // fileira norte-da-rua, encara +X
    private static final int HOUSE_B_X = 42;   // fileira sul-da-rua, encara -X
    private static final int HOUSE_Z = 7;      // o lote comeca 7 depois da emenda

    /**
     * A porta, em coordenada da peca: e daqui que sai o caminho de concreto.
     *
     * MEDIDO no schematic, nao chutado: a parede de tijolo da casa (x local 13) falta nos
     * z locais 4 e 5, e e ali a porta da frente.
     */
    private static final int DOOR_LOCAL_Z = 4;

    /**
     * Quantas fileiras de concreto o caminho tem.
     *
     * ⚠️ TRES, e nao duas, e o motivo e uma medicao: entre a porta e a rua ha o quintal
     * cercado, e a cerca (x local 17) so tem vao nos z locais 2, 6, 10 e 14. Um caminho de
     * duas fileiras saindo da porta (z 4 e 5) morreria contra o mourao da cerca — desenho
     * bonito levando a parede. Com tres, ele alcanca o vao de z=6, que e o portao.
     */
    private static final int WALK_ROWS = 3;

    /** De quanto em quanto poste. Um por lote seria fileira de poste, nao rua. */
    private static final int LAMP_EVERY = 2;

    /** Onde a fita pode largar o jogador: qualquer lugar num quadrado deste raio. */
    private static final int SPAWN_SPREAD = 64;

    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState ROAD = Blocks.GRAY_CONCRETE.defaultBlockState();
    private static final BlockState PAVE = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
    private static final BlockState POST = Blocks.IRON_BARS.defaultBlockState();
    private static final BlockState LAMP = Blocks.LANTERN.defaultBlockState();

    private volatile DimPiece house;
    private volatile boolean looked;

    public VillageChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    /**
     * A casa, lida do .bin na primeira vez que alguem pede um chunk.
     *
     * Ela nao pode ser lida no construtor: o gerador nasce do codec durante a leitura
     * do datapack, e ali os blocos ainda nao existem no registro — a paleta da peca
     * viraria pedra inteira.
     */
    private DimPiece house() {
        DimPiece ready = house;
        if (ready != null || looked) return ready;
        synchronized (this) {
            if (!looked) {
                house = PieceSet.get("village").hub();
                looked = true;
            }
            return house;
        }
    }

    // ------------------------------------------------------------------ o carimbo
    @Override
    protected void carve(Brush brush) {
        ground(brush);
        street(brush);
        DimPiece piece = house();
        if (piece == null) return;

        // As casas que podem alcancar este chunk. A varredura sai um lote para tras nos
        // dois eixos porque a casa comeca dentro do lote e acaba depois do meio dele:
        // uma casa cujo canto esta no lote anterior ainda entra aqui.
        int gx0 = Math.floorDiv(brush.x0 - PERIOD_X, PERIOD_X), gx1 = Math.floorDiv(brush.x1, PERIOD_X);
        int gz0 = Math.floorDiv(brush.z0 - PERIOD_Z, PERIOD_Z), gz1 = Math.floorDiv(brush.z1, PERIOD_Z);

        for (int gx = gx0; gx <= gx1; gx++) {
            for (int gz = gz0; gz <= gz1; gz++) {
                int zStart = gz * PERIOD_Z + HOUSE_Z;
                lot(brush, piece, gx * PERIOD_X + HOUSE_A_X, zStart, 0);
                lot(brush, piece, gx * PERIOD_X + HOUSE_B_X, zStart, 2);
            }
        }
    }

    /** A casa e o caminho de concreto que sai da porta dela ate a calcada. */
    private void lot(Brush brush, DimPiece piece, int ox, int oz, int rotation) {
        Placement placement = new Placement(piece, rotation, ox, GROUND_Y, oz);
        // O caminho e desenhado ANTES da casa: onde os dois se encontram, o degrau da
        // fundacao tem que ganhar do concreto, senao a soleira sai afundada.
        walkway(brush, placement, rotation);
        if (brush.touches(placement)) brush.stamp(placement);
    }

    /**
     * O caminho da porta ate a calcada.
     *
     * A porta e o vao na parede leste da peca, em `DOOR_LOCAL_Z..+1`. Perguntar a
     * `Placement` onde aquele bloco caiu no mundo e mais seguro que refazer a conta do
     * giro aqui: a formula de ida e volta mora num lugar so justamente porque errar um
     * sinal nela nao da erro de compilacao, da um caminho que sai pelo fundo da casa.
     */
    private void walkway(Brush brush, Placement placement, int rotation) {
        int doorX = placement.piece.width - 1;
        int lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE;
        for (int row = 0; row < WALK_ROWS; row++) {
            int z = placement.worldZ(doorX, DOOR_LOCAL_Z + row);
            lowest = Math.min(lowest, z);
            highest = Math.max(highest, z);
        }

        int from, to;
        if (rotation == 0) {
            from = placement.maxX() + 1;
            to = Math.floorDiv(placement.ox, PERIOD_X) * PERIOD_X + CURB_NEAR - 1;
        } else {
            from = Math.floorDiv(placement.ox, PERIOD_X) * PERIOD_X + CURB_FAR + 1;
            to = placement.minX() - 1;
        }
        for (int x = Math.min(from, to); x <= Math.max(from, to); x++) {
            for (int z = lowest; z <= highest; z++) {
                brush.set(x, GROUND_Y, z, PAVE);
            }
        }
    }

    /** O plano de grama, terra e pedra. Cinco de pedra e o bastante: ninguem cava aqui. */
    private void ground(Brush brush) {
        for (int x = brush.x0; x <= brush.x1; x++) {
            for (int z = brush.z0; z <= brush.z1; z++) {
                brush.set(x, GROUND_Y, z, GRASS);
                brush.column(x, GROUND_Y - DIRT_DEPTH, GROUND_Y - 1, z, DIRT);
                brush.column(x, GROUND_Y - DIRT_DEPTH - STONE_DEPTH, GROUND_Y - DIRT_DEPTH - 1, z, STONE);
            }
        }
    }

    /** Asfalto, calcada e poste. Escrito por cima da grama, que ja esta la. */
    private void street(Brush brush) {
        for (int x = brush.x0; x <= brush.x1; x++) {
            int u = Math.floorMod(x, PERIOD_X);
            boolean road = u >= ROAD_MIN && u <= ROAD_MAX;
            boolean curb = u == CURB_NEAR || u == CURB_NEAR + 1 || u == CURB_FAR - 1 || u == CURB_FAR;
            if (!road && !curb) continue;
            for (int z = brush.z0; z <= brush.z1; z++) {
                brush.set(x, GROUND_Y, z, road ? ROAD : PAVE);
            }
        }
        lamps(brush);
    }

    /**
     * Os postes, nas duas calcadas.
     *
     * Um em cada lado, mas em lotes ALTERNADOS (o do outro lado entra meio periodo
     * depois): frente a frente eles viram portico, e portico e arquitetura. Alternados
     * eles viram o que a foto tem, que e um poste solto de vez em quando.
     */
    private void lamps(Brush brush) {
        int gx0 = Math.floorDiv(brush.x0, PERIOD_X), gx1 = Math.floorDiv(brush.x1, PERIOD_X);
        int gz0 = Math.floorDiv(brush.z0, PERIOD_Z), gz1 = Math.floorDiv(brush.z1, PERIOD_Z);
        for (int gx = gx0; gx <= gx1; gx++) {
            for (int gz = gz0; gz <= gz1; gz++) {
                if (Math.floorMod(gz, LAMP_EVERY) == 0) {
                    post(brush, gx * PERIOD_X + CURB_NEAR + 1, gz * PERIOD_Z + PERIOD_Z / 2);
                } else {
                    post(brush, gx * PERIOD_X + CURB_FAR - 1, gz * PERIOD_Z + PERIOD_Z / 2);
                }
            }
        }
    }

    private void post(Brush brush, int x, int z) {
        brush.column(x, GROUND_Y + 1, GROUND_Y + 4, z, POST);
        brush.set(x, GROUND_Y + 5, z, LAMP);
    }

    // ------------------------------------------------------------------ o spawn
    /**
     * Na calcada, num lote sorteado — e num lote DIFERENTE a cada vez que a fita roda.
     *
     * A regra e do Pedro, e vale para as 21: "spawn deve ser em diferentes locais das
     * dimensoes e nunca no mesmo spawn". Aqui ela tem um efeito de graca: como todo
     * lote e igual, cair num lote diferente e indistinguivel de cair no mesmo — e essa
     * duvida e a propria dimensao.
     */
    @Override
    public BlockPos dimensionSpawn() {
        java.util.Random dice = new java.util.Random();
        int gx = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
        int gz = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
        // No MEIO da rua: e de onde a fileira se ve dos dois lados de uma vez.
        int x = gx * PERIOD_X + (ROAD_MIN + ROAD_MAX) / 2;
        int z = gz * PERIOD_Z + PERIOD_Z / 2;
        return new BlockPos(x, GROUND_Y + 1, z);
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
        return "VILLAGE";
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return GROUND_Y + 1;
    }
}
