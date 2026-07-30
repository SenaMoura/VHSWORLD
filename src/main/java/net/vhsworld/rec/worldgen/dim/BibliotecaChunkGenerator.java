package net.vhsworld.rec.worldgen.dim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * A dimensao BIBLIOTECA: um salao de estantes que nao tem porta nem fim, no breu.
 *
 * "uma biblioteca fechada com diversas estantes de livros, o local deve ser escuro tbm".
 * FECHADA e a palavra que manda: nao ha janela, nao ha ceu, nao ha saida. Ela e a unica
 * das seis em que o jogador precisa carregar a propria luz — e como a luz que ele carrega
 * alcanca menos que o salao, ele nunca ve a estante do fim.
 *
 * ============================ ELA LADRILHA SOZINHA ============================
 *
 * Isto e sorte da construcao do Pedro, e eu conferi antes de contar com ela. O
 * `biblioteca_hub.schem` mede 23x18x35 e tem 805 blocos solidos em y=0 — que e
 * exatamente 23x35 — e outros 805 em y=17. Ou seja: piso e teto sao planos CHEIOS da
 * pegada, e as quatro paredes da borda sao ar de ponta a ponta.
 *
 * Uma peca assim encostada nas copias dela nao tem emenda: o piso vira um chao continuo,
 * o teto vira um forro continuo, e o que sobra por dentro (as estantes, os corredores, os
 * tapetes) vira o labirinto. Nao ha casca nenhuma a construir e nao ha parede a fechar —
 * o unico trabalho do Java e repetir e nao deixar a repeticao aparecer.
 *
 * ⚠️ NAO GIRA 90 GRAUS. 23x35 girado viraria 35x23 e sairia da grade: apareceria degrau
 * de piso e fresta de teto em toda emenda. O giro de 180 mantem a pegada, e e o unico
 * usado — ele espelha o salao nos dois eixos, que ja e o bastante para o corredor nao
 * sair sempre no mesmo lugar.
 *
 * As estantes extras entram em DOIS pontos, e os dois foram MEDIDOS no plano do salao
 * (varri, coluna por coluna, onde y=1..8 e ar e y=0 e solido): a estante alta cabe em
 * x=0..6 / z=8..13, e a comprida em x=18..22 / z=2..10. Nao sao pontos sorteados — num
 * ponto sorteado a estante nasceria atravessada num corredor ou dentro de outra estante,
 * e "sorteado" seria a palavra bonita para "as vezes tapa a passagem".
 */
public class BibliotecaChunkGenerator extends StampChunkGenerator {

    public static final Codec<BibliotecaChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, BibliotecaChunkGenerator::new));

    private static final int MIN_Y = 0;
    private static final int GEN_HEIGHT = 128;

    /** O piso do salao. */
    public static final int FLOOR_Y = 64;

    /** A pegada do salao, do schematic. E ela que e a grade. */
    private static final int HALL_W = 23;
    private static final int HALL_L = 35;

    /** A chance de cada uma das duas estantes extras entrar num salao. */
    private static final double SHELF_CHANCE = 0.45D;

    // Os dois cantos livres, em coordenada do salao. Ver o comentario da classe.
    private static final int SHELF_A_X = 18, SHELF_A_Z = 2;   // estante_1: 5x10x9
    private static final int SHELF_B_X = 0, SHELF_B_Z = 8;    // estante_2: 7x16x6

    private static final int SPAWN_SPREAD = 64;

    private volatile DimPiece hall;
    private volatile DimPiece shelfA;
    private volatile DimPiece shelfB;
    private volatile boolean looked;

    public BibliotecaChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    private void look() {
        if (looked) return;
        synchronized (this) {
            if (looked) return;
            PieceSet set = PieceSet.get("biblioteca");
            hall = set.byName("hall");
            shelfA = set.byName("shelf_a");
            shelfB = set.byName("shelf_b");
            looked = true;
        }
    }

    private int rotationAt(int cx, int cz) {
        return DimHash.pick(seed(), cx, cz, 41L, 2) * 2;
    }

    private Placement hallAt(int cx, int cz) {
        return new Placement(hall, rotationAt(cx, cz), cx * HALL_W, FLOOR_Y, cz * HALL_L);
    }

    // ------------------------------------------------------------------ o carimbo
    @Override
    protected void carve(Brush brush) {
        look();
        if (hall == null) return;

        int cx0 = Math.floorDiv(brush.x0, HALL_W), cx1 = Math.floorDiv(brush.x1, HALL_W);
        int cz0 = Math.floorDiv(brush.z0, HALL_L), cz1 = Math.floorDiv(brush.z1, HALL_L);

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                Placement placement = hallAt(cx, cz);
                brush.stamp(placement);
                shelf(brush, placement, shelfA, SHELF_A_X, SHELF_A_Z, cx, cz, 42L);
                shelf(brush, placement, shelfB, SHELF_B_X, SHELF_B_Z, cx, cz, 43L);
            }
        }
    }

    /**
     * Uma estante extra, num ponto do salao dado em coordenada do SALAO.
     *
     * ⚠️ O giro e a pegadinha. A estante tem que entrar girada junto com o salao, senao
     * um salao espelhado ganharia uma estante virada ao contrario — e as estantes tem
     * frente (o `spruce_log` da lateral). E o CANTO dela muda: girada 180, o canto de
     * menor X passa a ser o que era o de maior X, entao o deslocamento dentro do salao
     * conta a partir do fim da peca, e nao do comeco.
     */
    private void shelf(Brush brush, Placement hallPlacement, DimPiece shelf,
                       int localX, int localZ, int cx, int cz, long salt) {
        if (shelf == null) return;
        if (DimHash.frac(seed(), cx, cz, salt) >= SHELF_CHANCE) return;

        int rotation = hallPlacement.rotation;
        int ox, oz;
        if (rotation == 0) {
            ox = hallPlacement.ox + localX;
            oz = hallPlacement.oz + localZ;
        } else {
            ox = hallPlacement.ox + (HALL_W - localX - shelf.width);
            oz = hallPlacement.oz + (HALL_L - localZ - shelf.length);
        }
        Placement placement = new Placement(shelf, rotation, ox, FLOOR_Y, oz);
        if (brush.touches(placement)) brush.stamp(placement);
    }

    // ------------------------------------------------------------------ o spawn
    /**
     * Num salao sorteado, no canto que o plano diz estar livre.
     *
     * O ponto e (1,1) em coordenada do salao: a faixa z=0..2 dele nao tem um bloco entre
     * y=1 e y=8 em nenhuma coluna, e o piso de y=0 e cheio. Passando pela `Placement`, o
     * giro de 180 e resolvido de graca — o canto oposto tambem esta livre, pelo mesmo
     * plano.
     */
    @Override
    public BlockPos dimensionSpawn() {
        look();
        java.util.Random dice = new java.util.Random();
        int cx = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
        int cz = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
        if (hall == null) return new BlockPos(cx * HALL_W + 1, FLOOR_Y + 1, cz * HALL_L + 1);
        Placement placement = hallAt(cx, cz);
        return new BlockPos(placement.worldX(1, 1), FLOOR_Y + 1, placement.worldZ(1, 1));
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
        return "BIBLIOTECA";
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return FLOOR_Y + 1;
    }
}
