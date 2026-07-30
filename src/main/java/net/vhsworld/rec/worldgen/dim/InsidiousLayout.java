package net.vhsworld.rec.worldgen.dim;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * A planta da dimensao INSIDIOUS: saloes de pedra SEM TETO sobre o vazio preto.
 *
 * O que o Pedro construiu foram quatro pecas que dividem a mesma bitola — piso de 7 de
 * largura com muro nas duas bordas, 5 de passagem no meio. Um corredor reto, dois
 * cruzamentos de quatro vias (um pequeno, um grande com um buraco de 3x3 no meio do
 * chao) e uma sala redonda com um sigilo de carvao no piso.
 *
 * A referencia que ele deu e um salao de pedra escuro com tocha rara. A diferenca que
 * importa e que aqui NAO HA TETO: o corredor e uma fita de pedra boiando no preto. Como
 * a dimensao nao tem sol nem lua, olhar para cima e o mesmo breu — mas a queda esta
 * sempre a um passo, e e ela que faz o lugar.
 *
 * ============================ ELA E INFINITA ============================
 *
 * Mesma tecnica da CHUNKS: nada e guardado. Tudo aqui e funcao pura de (casa, semente),
 * entao pergunta-se a qualquer ponto do infinito e a resposta sai igual em qualquer
 * ordem e em qualquer thread. Um gerador de numeros que ANDA nao serviria — ele so
 * responde na ordem em que foi chamado, e num mundo infinito as perguntas chegam na
 * ordem em que o jogador caminha.
 *
 * ============================ O LABIRINTO ============================
 *
 * Uma grade de CELL em CELL. Cada casa ou tem um cruzamento ou e vazio; entre duas casas
 * ocupadas pode haver corredor. Tres coisas fazem dela um labirinto e nao uma malha:
 *
 *   1. CASAS VAZIAS (`VOID_CHANCE`) — rasgos largos no meio do desenho.
 *   2. ARESTAS SORTEADAS (`EDGE_CHANCE`) — vizinhas que existem e nao se ligam.
 *   3. BECOS (`STUB_CHANCE`) — o cruzamento tem quatro bracos SEMPRE, porque foi assim
 *      que ele foi construido. Braco sem corredor ja e um beco: anda-se ate a ponta e
 *      la esta o vazio. O beco sorteado estica esse braco mais alguns blocos de
 *      corredor antes de acabar em nada, para que nem todo fim de linha fique a mesma
 *      distancia do cruzamento.
 *
 * ============================ O CORACAO ============================
 *
 * A sala redonda e UNICA no mundo inteiro — decisao do Pedro: e nela que vai ficar o
 * coracao da INSIDIOUS. Achar a sala e o objetivo da dimensao, entao a fita larga o
 * jogador longe dela.
 *
 * ⚠️ E por isso existe a ESPINHA: um caminho em L de arestas FORCADAS ligando o
 * nascimento ate o coracao. Sem ela, "existe um caminho a pe" seria torcer pelo sorteio
 * — e um labirinto de arestas independentes se parte em ilhas com facilidade. Com ela, a
 * garantia e por construcao e nao por estatistica. O L vai primeiro em X e so depois em
 * Z, de proposito: assim a chegada no coracao e sempre pelo eixo Z.
 *
 * ⚠️ E TEM QUE SER PELO Z. As bocas norte e sul da sala tem 5 de largura e casam com o
 * corredor; as bocas leste e oeste tem 3 e nao casam. Ligar o coracao pelo lado seria
 * encostar um corredor de 5 numa porta de 3 e deixar dois buracos para o vazio.
 */
public final class InsidiousLayout {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * O Y do PISO de tudo. A dimensao inteira e um plano so.
     *
     * A peca do cruzamento grande pendura uma laje 11 blocos abaixo do piso e a sala
     * sobe 10 acima; com o piso em 64 e o mundo de 128, sobra folga dos dois lados.
     */
    public static final int FLOOR_Y = 64;

    /**
     * De centro a centro de um cruzamento para o seguinte.
     *
     * 48 e o menor numero que acomoda os dois cruzamentos sem encostar um no outro: o
     * grande estica 18 blocos para um lado do cruzamento, e dois deles vizinhos ja
     * somam 36. O que sobra vira corredor — de 12 a 30 blocos, que e uma caminhada com
     * fim visivel mas nao imediato.
     */
    private static final int CELL = 48;

    /** A chance de uma casa ficar sem cruzamento nenhum. */
    private static final float VOID_CHANCE = 0.22F;

    /** A chance de duas vizinhas ocupadas terem corredor entre elas. */
    private static final float EDGE_CHANCE = 0.62F;

    /** A chance de um braco solto ganhar um pedaco de corredor antes de acabar no vazio. */
    private static final float STUB_CHANCE = 0.55F;

    /** O tamanho desse pedaco. */
    private static final int STUB_MIN = 4, STUB_MAX = 20;

    /** Quantas casas de distancia o coracao fica do nascimento, no eixo Z. */
    private static final int HEART_Z_MIN = 6, HEART_Z_SPAN = 4;

    /** E o quanto ele pode andar de lado. */
    private static final int HEART_X_SPAN = 7;

    /**
     * Quanto uma peca (ou o corredor que sai dela) pode passar da casa dela.
     *
     * O corredor de uma casa vai ate a vizinha, entao alcanca CELL inteiro. Sem esta
     * folga na varredura, o chunk do meio de um corredor nao saberia que ha corredor
     * nele e o vao sairia aberto.
     */
    private static final int OVERREACH = CELL;

    /** O modulo do corredor: 5 fatias de 1 bloco que se repetem. */
    private static final int HALL_PERIOD = 5;

    private final long seed;
    private final DimPiece heart;
    private final DimPiece cross;
    private final DimPiece crossBig;
    private final DimPiece[] hall;

    /** A casa em que mora a sala unica. */
    private final int heartX, heartZ;

    private InsidiousLayout(long seed, DimPiece heart, DimPiece cross, DimPiece crossBig,
                            DimPiece[] hall, int heartX, int heartZ) {
        this.seed = seed;
        this.heart = heart;
        this.cross = cross;
        this.crossBig = crossBig;
        this.hall = hall;
        this.heartX = heartX;
        this.heartZ = heartZ;
    }

    // ------------------------------------------------------------------ construcao
    public static InsidiousLayout build(PieceSet set, long seed) {
        DimPiece heart = set.byName("heart");
        DimPiece cross = set.byName("cross");
        DimPiece crossBig = set.byName("cross_big");
        DimPiece[] hall = {
                set.byName("hall_a"), set.byName("hall_b"), set.byName("hall_c"),
                set.byName("hall_d"), set.byName("hall_e")
        };

        if (heart == null || cross == null || crossBig == null || hall[0] == null) {
            LOGGER.warn("[dimensao] insidious sem pecas: a dimensao vai nascer vazia");
            return new InsidiousLayout(seed, null, null, null, hall, 0, 0);
        }

        // O coracao: uma casa so, sorteada pela semente. |z| >= 6 garante que ele nunca
        // cai na perna de X da espinha nem na casa do nascimento, e que a ultima etapa
        // do caminho e sempre longa.
        int hx = (int) Math.floorMod(hash(seed, 0, 0, 11L), (long) (HEART_X_SPAN * 2 + 1)) - HEART_X_SPAN;
        int hz = HEART_Z_MIN + (int) Math.floorMod(hash(seed, 0, 0, 12L), (long) HEART_Z_SPAN);
        if ((hash(seed, 0, 0, 13L) & 1L) == 0L) hz = -hz;

        LOGGER.info("[dimensao] insidious: labirinto infinito pronto (semente {}), coracao na casa {},{}",
                seed, hx, hz);
        return new InsidiousLayout(seed, heart, cross, crossBig, hall, hx, hz);
    }

    /** Onde a fita larga o jogador: o meio do cruzamento do centro. */
    public BlockPos spawnPos() {
        return new BlockPos(0, FLOOR_Y + 1, 0);
    }

    /**
     * O mesmo, mas num cruzamento SORTEADO — e nunca o mesmo duas vezes.
     *
     * Regra do Pedro para as 21: "spawn deve ser em diferentes locais das dimensoes e
     * nunca no mesmo spawn". Aqui ela vale duplo, porque a INSIDIOUS tem UMA sala escondida
     * no meio dela: nascendo sempre no centro, a distancia ate o coracao seria sempre a
     * mesma e o caminho seria decorado na segunda ida. Sorteando, achar a sala volta a ser
     * procurar.
     *
     * ⚠️ Nao sorteia a casa do CORACAO. Ele e o achado da dimensao; nascer dentro dele e
     * receber de graca a unica coisa que ela tem para dar.
     *
     * O ponto e o centro da casa porque a ancora de todas as pecas da INSIDIOUS e o bloco
     * do piso no meio da passagem — se a casa esta ocupada, ali ha piso, sem ter que olhar.
     */
    public BlockPos randomSpawn() {
        if (cross == null) return spawnPos();
        java.util.Random dice = new java.util.Random();
        for (int tries = 0; tries < SPAWN_TRIES; tries++) {
            int cx = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
            int cz = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
            if (!occupied(cx, cz) || isHeart(cx, cz)) continue;
            return new BlockPos(cx * CELL, FLOOR_Y + 1, cz * CELL);
        }
        return spawnPos();
    }

    /** O quadrado de casas em que a fita pode largar o jogador. */
    private static final int SPAWN_SPREAD = 48;
    private static final int SPAWN_TRIES = 64;

    /** O meio da sala unica. Serve para o que for morar nela e para quem for procura-la. */
    public BlockPos heartPos() {
        return new BlockPos(heartX * CELL, FLOOR_Y + 1, heartZ * CELL);
    }

    // ------------------------------------------------------------------ a grade
    private boolean isHeart(int cx, int cz) {
        return cx == heartX && cz == heartZ;
    }

    /**
     * Ha cruzamento nesta casa?
     *
     * O centro, o coracao e toda a espinha entram na marra: sao eles que sustentam a
     * garantia de que da para ir de um ao outro a pe.
     */
    private boolean occupied(int cx, int cz) {
        if (cx == 0 && cz == 0) return true;
        if (isHeart(cx, cz)) return true;
        if (onSpine(cx, cz)) return true;
        return frac(hash(seed, cx, cz, 1L)) >= VOID_CHANCE;
    }

    /** Esta casa esta no caminho em L que liga o nascimento ao coracao? */
    private boolean onSpine(int cx, int cz) {
        if (cz == 0 && between(cx, 0, heartX)) return true;          // a perna em X
        return cx == heartX && between(cz, 0, heartZ);               // a perna em Z
    }

    private static boolean between(int value, int a, int b) {
        return value >= Math.min(a, b) && value <= Math.max(a, b);
    }

    /** A aresta entre duas vizinhas esta na espinha? Nesse caso ela existe sempre. */
    private boolean spineEdge(int ax, int az, int bx, int bz) {
        if (az == 0 && bz == 0 && between(ax, 0, heartX) && between(bx, 0, heartX)) return true;
        return ax == heartX && bx == heartX && between(az, 0, heartZ) && between(bz, 0, heartZ);
    }

    private static final int[][] SIDES = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};   // N, L, S, O

    /**
     * Ha corredor entre estas duas vizinhas?
     *
     * ⚠️ O coracao so liga no eixo Z. As bocas leste e oeste da sala tem 3 de largura
     * contra os 5 do corredor — encostar um no outro deixaria dois buracos abertos
     * para o vazio de cada lado da porta.
     */
    private boolean linked(int ax, int az, int bx, int bz) {
        if (!occupied(ax, az) || !occupied(bx, bz)) return false;
        boolean alongX = az == bz;
        if ((isHeart(ax, az) || isHeart(bx, bz)) && alongX) return false;
        if (spineEdge(ax, az, bx, bz)) return true;

        // A ordem das duas casas nao pode mudar a resposta, senao uma metade do
        // corredor existiria e a outra nao.
        boolean first = ax < bx || (ax == bx && az < bz);
        int lx = first ? ax : bx, lz = first ? az : bz;
        int hx = first ? bx : ax, hz = first ? bz : az;
        return frac(hash(seed, lx, lz, hx * 31L + hz * 7919L + 3L)) < EDGE_CHANCE;
    }

    /**
     * Esta casa tem boca virada para este lado?
     *
     * Os cruzamentos tem as quatro. A sala tem so norte e sul — as outras duas paredes
     * dela sao arco de 3, que nao e porta de corredor. Sem esta pergunta, um beco
     * sorteado sairia colado numa parede fechada da sala.
     */
    private boolean hasMouth(int cx, int cz, int side) {
        if (!isHeart(cx, cz)) return true;
        return side == 0 || side == 2;
    }

    // ------------------------------------------------------------------ as pecas
    /**
     * O cruzamento de uma casa, ja girado.
     *
     * A sala nunca gira: girada de lado, as bocas de 5 iriam para leste/oeste e as de 3
     * para norte/sul, e o corredor deixaria de casar com ela.
     */
    private Placement junction(int cx, int cz) {
        if (isHeart(cx, cz)) {
            return anchored(heart, 0, cx * CELL, cz * CELL);
        }
        boolean big = Math.floorMod(hash(seed, cx, cz, 6L), 10L) < 4L;
        int rotation = (int) Math.floorMod(hash(seed, cx, cz, 7L), 4L);
        return anchored(big ? crossBig : cross, rotation, cx * CELL, cz * CELL);
    }

    /**
     * Poe a peca com a ANCORA dela num ponto do mundo, qualquer que seja o giro.
     *
     * A ancora de todas as pecas da INSIDIOUS e o mesmo ponto conceitual: o bloco do
     * piso no meio da passagem. Alinhar pela caixa nao serviria — o cruzamento grande
     * tem o piso em y=11 da selecao e os outros em y=1, e os bracos dele nao sao
     * simetricos (estica 18 para um lado e 9 para o outro). Alinhando pela ancora, todo
     * piso cai no mesmo Y e toda passagem cai na mesma linha de centro.
     *
     * O giro entra sozinho: pergunta-se a propria `Placement` para onde a ancora foi
     * parar com aquele giro, e desconta. Fazer a conta na mao por giro e onde nasce o
     * corredor desencontrado que so aparece depois de entrar no mundo.
     */
    private static Placement anchored(DimPiece piece, int rotation, int worldX, int worldZ) {
        Placement probe = new Placement(piece, rotation, 0, 0, 0);
        int dx = probe.worldX(piece.anchorX, piece.anchorZ);
        int dz = probe.worldZ(piece.anchorX, piece.anchorZ);
        return new Placement(piece, rotation, worldX - dx, FLOOR_Y - piece.anchorY, worldZ - dz);
    }

    /**
     * Uma fatia de corredor.
     *
     * A fase sai da COORDENADA DO MUNDO, e nao de um contador por corredor: assim o
     * desenho dos pilares e continuo mesmo quando dois corredores se encontram, e
     * continua igual seja qual for a ordem em que os chunks foram pedidos.
     *
     * Correndo em Z a fatia entra girada — ela nasce com 1 bloco de largura em X e 7 de
     * comprimento em Z, e girada troca os dois. Os muros acompanham porque quem gira e
     * a paleta inteira.
     */
    private Placement hallSlice(int worldX, int worldZ, boolean alongX) {
        int along = alongX ? worldX : worldZ;
        DimPiece slice = hall[Math.floorMod(along, HALL_PERIOD)];
        return anchored(slice, alongX ? 0 : 1, worldX, worldZ);
    }

    // ------------------------------------------------------------------ o carimbo
    /**
     * As pecas que encostam neste chunk, montadas na hora.
     *
     * ⚠️ A ORDEM IMPORTA e e por isso que os corredores entram antes dos cruzamentos.
     * O corredor sobrepoe de proposito a fileira da borda do cruzamento: e assim que o
     * piso fica selado na emenda. Na sala redonda essa fileira tem so 5 blocos de piso
     * (a boca) contra os 7 do corredor, entao sem a sobreposicao sobrariam dois buracos
     * de 1x1 caindo para o vazio, um de cada lado da porta. Com ela, o corredor poe os
     * 7 e o cruzamento carimba os dele por cima; onde o cruzamento tem ar (as duas
     * quinas da porta da sala) o muro do corredor fica de pe e vira batente.
     */
    public List<Placement> piecesIn(ChunkPos pos) {
        if (heart == null) return List.of();

        int x0 = pos.getMinBlockX(), x1 = pos.getMaxBlockX();
        int z0 = pos.getMinBlockZ(), z1 = pos.getMaxBlockZ();

        int cx0 = Math.floorDiv(x0 - OVERREACH, CELL), cx1 = Math.floorDiv(x1 + OVERREACH, CELL);
        int cz0 = Math.floorDiv(z0 - OVERREACH, CELL), cz1 = Math.floorDiv(z1 + OVERREACH, CELL);

        List<Placement> halls = new ArrayList<>();
        List<Placement> junctions = new ArrayList<>();

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (!occupied(cx, cz)) continue;
                Placement here = junction(cx, cz);
                add(junctions, here, x0, z0, x1, z1);

                // Cada corredor pertence a casa a oeste ou ao norte dele, para que o
                // vao seja desenhado uma vez so.
                corridor(halls, cx, cz, here, 1, x0, z0, x1, z1);   // para o leste
                corridor(halls, cx, cz, here, 2, x0, z0, x1, z1);   // para o sul
                // O beco, ao contrario, pertence a casa de onde ele sai, nos 4 lados.
                for (int side = 0; side < 4; side++) {
                    stub(halls, cx, cz, here, side, x0, z0, x1, z1);
                }
            }
        }

        halls.addAll(junctions);
        return halls;
    }

    /**
     * O corredor de uma casa ate a vizinha, fatia por fatia.
     *
     * Vai da fileira da borda de um cruzamento ate a fileira da borda do outro,
     * INCLUSIVE as duas — e a sobreposicao que sela a emenda.
     */
    private void corridor(List<Placement> into, int cx, int cz, Placement here, int side,
                          int x0, int z0, int x1, int z1) {
        int nx = cx + SIDES[side][0], nz = cz + SIDES[side][1];
        if (!linked(cx, cz, nx, nz)) return;

        Placement there = junction(nx, nz);
        boolean alongX = SIDES[side][0] != 0;
        int from = alongX ? here.maxX() : here.maxZ();
        int to = alongX ? there.minX() : there.minZ();
        if (to < from) return;      // dois cruzamentos encostados: nao cabe corredor

        int lane = alongX ? cz * CELL : cx * CELL;
        for (int at = from; at <= to; at++) {
            add(into, alongX ? hallSlice(at, lane, true) : hallSlice(lane, at, false),
                    x0, z0, x1, z1);
        }
    }

    /**
     * O BECO: um pedaco de corredor que sai do cruzamento e acaba no vazio.
     *
     * So nasce onde nao houve corredor. O braco solto do cruzamento ja e um beco por si
     * — anda-se ate a ponta dele e la esta o nada; isto aqui so faz com que nem todos
     * acabem a mesma distancia do centro, que e o que o olho pegaria depressa.
     */
    private void stub(List<Placement> into, int cx, int cz, Placement here, int side,
                      int x0, int z0, int x1, int z1) {
        int nx = cx + SIDES[side][0], nz = cz + SIDES[side][1];
        if (linked(cx, cz, nx, nz)) return;
        if (!hasMouth(cx, cz, side)) return;
        if (frac(hash(seed, cx, cz, 20L + side)) >= STUB_CHANCE) return;

        int length = STUB_MIN + (int) Math.floorMod(hash(seed, cx, cz, 30L + side),
                (long) (STUB_MAX - STUB_MIN + 1));
        boolean alongX = SIDES[side][0] != 0;
        boolean forward = SIDES[side][0] > 0 || SIDES[side][1] > 0;

        int edge = alongX ? (forward ? here.maxX() : here.minX())
                          : (forward ? here.maxZ() : here.minZ());
        int lane = alongX ? cz * CELL : cx * CELL;

        for (int i = 0; i < length; i++) {
            int at = forward ? edge + i : edge - i;
            add(into, alongX ? hallSlice(at, lane, true) : hallSlice(lane, at, false),
                    x0, z0, x1, z1);
        }
    }

    private static void add(List<Placement> into, Placement placement,
                            int x0, int z0, int x1, int z1) {
        if (placement.maxX() < x0 || placement.minX() > x1) return;
        if (placement.maxZ() < z0 || placement.minZ() > z1) return;
        into.add(placement);
    }

    // ------------------------------------------------------------------ o sorteio
    /** O mesmo hash da CHUNKS: mistura a semente com a casa e um sal. */
    private static long hash(long seed, int cx, int cz, long salt) {
        long h = seed;
        h = (h ^ (cx * 0x9E3779B97F4A7C15L)) * 0xFF51AFD7ED558CCDL;
        h ^= h >>> 29;
        h = (h ^ (cz * 0xC2B2AE3D27D4EB4FL)) * 0x94D049BB133111EBL;
        h ^= h >>> 32;
        h = (h ^ salt) * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 31;
        return h;
    }

    private static double frac(long hash) {
        return (hash >>> 11) * 0x1.0p-53D;
    }
}
