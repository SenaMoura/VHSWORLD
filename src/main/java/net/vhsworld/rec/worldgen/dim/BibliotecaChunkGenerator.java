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

    // ------------------------------------------------------------------ salas e corredores
    //
    // "biblioteca, criar variacoes de salas com paredes, corredores menores".
    //
    // ⚠️ O QUE ESTAVA ERRADO ANTES. O salao do Pedro ladrilha sem emenda, e por isso a
    // dimensao inteira era UM comodo de tamanho infinito: piso continuo, forro continuo,
    // 16 blocos de pe-direito em toda parte. Repetir sem emenda resolveu o problema de
    // construcao e criou um de jogo — num lugar sem parede nao ha "outra sala", so mais
    // do mesmo lugar, e as estantes viram enfeite em vez de labirinto.
    //
    // Agora cada salao e um COMODO FECHADO com porta, e uma parte deles nao e comodo:
    // e um corredor de 3 de largo e 4 de alto escavado num bloco macico de estantes. O
    // contraste e o ponto — depois de 16 blocos de pe-direito, um teto a 5 blocos da
    // cabeça e uma passagem em que nao da para se virar de lado sem raspar na estante.

    /** O pe-direito do salao: o piso e y=0 da peca e o forro e y=17. */
    private static final int HALL_TOP = 16;

    /** A chance de um salao virar corredor em vez de comodo. */
    private static final double CORRIDOR_CHANCE = 0.30D;

    /** A chance de haver porta entre dois salaos vizinhos. */
    private static final double DOOR_CHANCE = 0.70D;

    /** A bitola da passagem: 3 de largo, 4 de alto. E a mesma da porta, e nao por acaso. */
    private static final int PASS_W = 3;
    private static final int PASS_H = 4;

    private static final net.minecraft.world.level.block.state.BlockState AIR =
            net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    /** Andesito polido: e do que sao os pilares e as paredes do proprio salao do Pedro. */
    private static final net.minecraft.world.level.block.state.BlockState WALL =
            net.minecraft.world.level.block.Blocks.POLISHED_ANDESITE.defaultBlockState();
    /** O macico de onde o corredor e escavado. Estante, para a parede dele ser livro. */
    private static final net.minecraft.world.level.block.state.BlockState MASS =
            net.minecraft.world.level.block.Blocks.BOOKSHELF.defaultBlockState();

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
                // O corredor ANTES das paredes: ele escava e enche o miolo, e a parede
                // que vem depois abre a porta dela no macico ja posto. Na ordem trocada,
                // o enchimento taparia a porta recem-aberta.
                if (isCorridor(cx, cz)) corridor(brush, cx, cz);
                walls(brush, cx, cz);
            }
        }
    }

    // ------------------------------------------------------------------ o feitio da casa
    private boolean isCorridor(int cx, int cz) {
        return DimHash.frac(seed(), cx, cz, 44L) < CORRIDOR_CHANCE;
    }

    /** O canto do corredor que corre em Z, e o do que corre em X. Tambem sao as portas. */
    private static int legX(int cx) {
        return cx * HALL_W + (HALL_W - PASS_W) / 2;
    }

    private static int legZ(int cz) {
        return cz * HALL_L + (HALL_L - PASS_W) / 2;
    }

    /**
     * Ha porta entre este salao e o vizinho? A pergunta e sempre feita pelo lado de
     * MAIOR coordenada, para cada parede ser decidida uma vez so.
     *
     * A trava do fim e a mesma da GRASSROOMS e existe pelo mesmo motivo: com 30% de
     * parede fechada por lado, um salao em cada 120 nasce com as quatro trancadas. Num
     * lugar sem janela isso nao e uma sala escondida, e uma cela — e a fita pode largar
     * o jogador dentro dela.
     */
    private boolean door(int cx, int cz, boolean westward) {
        if (doorRaw(cx, cz, westward)) return true;
        if (!westward) return false;
        return !doorRaw(cx, cz, false)
                && !doorRaw(cx + 1, cz, true)
                && !doorRaw(cx, cz + 1, false);
    }

    private boolean doorRaw(int cx, int cz, boolean westward) {
        int nx = westward ? cx - 1 : cx;
        int nz = westward ? cz : cz - 1;
        return DimHash.edge(seed(), nx, nz, cx, cz, westward ? 45L : 46L, DOOR_CHANCE);
    }

    /**
     * As duas paredes deste salao: a do oeste e a do norte.
     *
     * SO DUAS, pelo mesmo motivo da GRASSROOMS — a parede leste deste e a oeste do
     * vizinho de la. Desenhar as quatro dobraria a espessura de toda parede interna e,
     * pior, exigiria que os dois vizinhos concordassem sobre onde fica a porta, o que
     * nao se pode garantir quando os dois sao carimbados por threads diferentes.
     *
     * ⚠️ Elas caem nas colunas de BORDA da peca (x=0 e z=0 do salao), e ali nao se apaga
     * nada: medido, as quatro bordas do `biblioteca_hub` sao ar de ponta a ponta em toda
     * a altura. E o que deixa o salao ladrilhar sem emenda, e e o que sobra de vago para
     * a parede ocupar.
     */
    private void walls(Brush brush, int cx, int cz) {
        int bx = cx * HALL_W, bz = cz * HALL_L;
        int top = FLOOR_Y + HALL_TOP;

        if (bx >= brush.x0 && bx <= brush.x1) {
            int gap = legZ(cz);
            boolean open = door(cx, cz, true);
            for (int z = Math.max(bz, brush.z0); z <= Math.min(bz + HALL_L - 1, brush.z1); z++) {
                boolean inDoor = open && z >= gap && z < gap + PASS_W;
                for (int y = FLOOR_Y + 1; y <= top; y++) {
                    if (inDoor && y <= FLOOR_Y + PASS_H) continue;
                    brush.set(bx, y, z, WALL);
                }
            }
        }

        if (bz >= brush.z0 && bz <= brush.z1) {
            int gap = legX(cx);
            boolean open = door(cx, cz, false);
            for (int x = Math.max(bx, brush.x0); x <= Math.min(bx + HALL_W - 1, brush.x1); x++) {
                boolean inDoor = open && x >= gap && x < gap + PASS_W;
                for (int y = FLOOR_Y + 1; y <= top; y++) {
                    if (inDoor && y <= FLOOR_Y + PASS_H) continue;
                    brush.set(x, y, bz, WALL);
                }
            }
        }
    }

    /**
     * O salao virado corredor: uma cruz de 3 de largo escavada num macico de estantes.
     *
     * A CRUZ, e nao uma passagem reta, porque as quatro portas ficam no MEIO de cada
     * parede — o mesmo `legX`/`legZ` decide as duas coisas. Uma passagem reta ligaria
     * duas portas e deixaria as outras duas dando na estante, e uma porta que da em
     * parede so se descobre depois de andar ate ela no escuro.
     *
     * SO ATE O FORRO DO CORREDOR, e nao ate o teto do salao. Encher os 16 blocos custaria
     * o triplo de escrita para tapar um vazio que ninguem alcança: acima do forro nao ha
     * porta em parede nenhuma, e o teto do proprio salao (y=17 da peca) ja fecha por cima.
     */
    private void corridor(Brush brush, int cx, int cz) {
        int bx = cx * HALL_W, bz = cz * HALL_L;
        int ax = legX(cx), az = legZ(cz);
        int roof = FLOOR_Y + PASS_H + 1;

        for (int x = Math.max(bx, brush.x0); x <= Math.min(bx + HALL_W - 1, brush.x1); x++) {
            boolean alongZ = x >= ax && x < ax + PASS_W;
            for (int z = Math.max(bz, brush.z0); z <= Math.min(bz + HALL_L - 1, brush.z1); z++) {
                boolean inCross = alongZ || (z >= az && z < az + PASS_W);
                for (int y = FLOOR_Y + 1; y < roof; y++) {
                    brush.set(x, y, z, inCross ? AIR : MASS);
                }
                brush.set(x, roof, z, MASS);
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
     *
     * ⚠️ MENOS SE A CASA VIROU CORREDOR. Ali aquele canto deixou de ser ar e virou o
     * macico de estantes de onde a cruz e escavada — nascer nele e nascer DENTRO de um
     * bloco. Numa casa dessas o unico ar que existe e a propria cruz, entao o spawn vai
     * para o cruzamento dela, que e o centro da casa.
     */
    @Override
    public BlockPos dimensionSpawn() {
        look();
        java.util.Random dice = new java.util.Random();
        int cx = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
        int cz = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
        if (isCorridor(cx, cz)) {
            return new BlockPos(legX(cx) + PASS_W / 2, FLOOR_Y + 1, legZ(cz) + PASS_W / 2);
        }
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

    // ------------------------------------------------------------------ a saida
    @Override
    public String dimensionId() {
        return "biblioteca";
    }

    /** No meio do salao da casa fixa da regiao. */
    @Override
    public BlockPos exitAnchor(int rx, int rz) {
        look();
        int cx = ExitSite.cellInRegion(rx, HALL_W);
        int cz = ExitSite.cellInRegion(rz, HALL_L);
        if (hall == null) {
            return new BlockPos(cx * HALL_W + HALL_W / 2, FLOOR_Y + 1, cz * HALL_L + HALL_L / 2);
        }
        Placement placement = hallAt(cx, cz);
        return new BlockPos(placement.worldX(HALL_W / 2, HALL_L / 2), FLOOR_Y + 1,
                placement.worldZ(HALL_W / 2, HALL_L / 2));
    }
}
