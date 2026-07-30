package net.vhsworld.rec.worldgen.dim;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A planta da dimensao CHUNKS: colunas de mundo boiando em alturas diferentes.
 *
 * A ideia vem da propria construcao do Pedro. As duas ilhas nao sao "ilhas" — sao um
 * CHUNK inteiro, 16x16 macico do fundo ao topo, com grama em cima, terra, pedra e
 * minerio dentro, exatamente como o jogo o teria gerado. Uma delas tem mato e arvores
 * por cima, e a copa transborda a borda e fica pendurada sobre o vazio.
 *
 * Colunas ALTAS em alturas MUITO diferentes, o vazio entre elas, e o olho sem horizonte
 * para se apoiar. A ponte e plana, entao so liga colunas que calharam no mesmo patamar:
 * boa parte do mapa fica visivel e inalcancavel a pe. E o ponto — voce ve para onde nao
 * da para ir.
 *
 * ============================ ELA E INFINITA ============================
 *
 * Nao ha planta guardada. Antes havia: 60 colunas crescidas do centro para fora, uma
 * lista na memoria, e o mundo acabava na ultima. Agora TUDO aqui e funcao pura de
 * (casa, semente) — pergunta-se a qualquer ponto do infinito e a resposta sai na hora,
 * igual em qualquer ordem, em qualquer thread, sem nada ter sido gerado antes.
 *
 * O que essa troca custou: a regra dos patamares era um PROCESSO DE CRESCIMENTO ("a
 * coluna nova fica na mesma altura da que a gerou, com 40% de chance"), e crescimento
 * precisa de um comeco. Sem centro, a regra vira esta:
 *
 *   1. cada casa sorteia um POSTO (`rank`), um numero seu entre 0 e 1;
 *   2. a MAE de uma casa e a vizinha de posto MENOR, entre as arestas sorteadas como
 *      aceitas — e so entre elas;
 *   3. sobe-se a cadeia de maes ate a raiz (quem nao tem mae), e o patamar da casa e
 *      o patamar da RAIZ.
 *
 * Como o posto sempre diminui subindo, a cadeia nunca da volta e sempre acaba. Grupos
 * de mesma altura continuam existindo (sao as arvores dessa floresta), mas agora cada
 * um se descobre olhando so a vizinhanca, e nao a historia do mapa.
 *
 * MEDIDO em 24 sementes numa janela de 23x23 casas, antes de abrir o jogo, porque a
 * troca de regra podia mudar o feitio do mapa sem que os numeros mudassem: ficaram 31%
 * de colunas totalmente sozinhas (o mapa finito dava 32%) e 0,45 ponte por coluna (dava
 * 0,48). A cadeia de maes nunca passou de 4 passos — e por isso que perguntar a altura
 * de uma casa e barato o bastante para acontecer por chunk.
 */
public final class ChunksLayout {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Onde a fita larga o jogador. A coluna do meio nasce sempre nesta altura. */
    public static final int SPAWN_Y = 168;

    /**
     * O tabuleiro de alturas.
     *
     * Os topos nao sao continuos: sao PATAMARES, multiplos de STEP. Duas colunas ou
     * estao exatamente na mesma altura (e podem receber ponte) ou estao longe uma da
     * outra. Altura continua daria degraus de 1 ou 2 blocos, que a vista le como
     * erro de encaixe em vez de leitura de projeto.
     */
    private static final int STEP = 14;
    private static final int LEVELS_UP = 5;      // patamares acima do spawn
    private static final int LEVELS_DOWN = 4;    // e abaixo

    /**
     * Quanto a coluna desce alem da base da peca do Pedro (33 blocos de pedra).
     *
     * O sorteio e por coluna: e daqui que sai o fundo desigual da referencia. O piso
     * do mundo esta em y=0, e a coluna mais baixa possivel comeca em
     * SPAWN_Y - LEVELS_DOWN*STEP - 33 - DEEP_MAX = 168 - 56 - 33 - 70 = 9. Sobra folga.
     */
    private static final int DEEP_MIN = 22;
    private static final int DEEP_MAX = 70;

    /** O lado de um chunk. As pecas do Pedro sao chunks de verdade. */
    private static final int ISLAND = 16;

    /**
     * De centro a centro de uma coluna para a seguinte.
     *
     * 32 = ilha de 16 + vao de 16: tanto chao quanto vazio. Vao menor e o mundo
     * volta a parecer contínuo e a queda deixa de assustar; vao maior e a ponte vira
     * um corredor comprido de madeira e a coluna do outro lado some na distancia.
     */
    private static final int CELL = 32;

    /**
     * A chance de uma casa do tabuleiro ficar VAZIA.
     *
     * Nao existia no mapa finito: la o crescimento a partir do centro ja deixava a
     * borda irregular de graca. Num tabuleiro infinito, sem isto, toda casa teria a
     * sua coluna e o mapa viraria uma grade perfeita de 32 em 32 — de dentro, o olho
     * pega a cadencia em segundos e o vazio deixa de ser vazio, vira padrao. Com 28%
     * de buracos aparecem rasgos largos, que e o que da a queda de volta.
     */
    private static final float VOID_CHANCE = 0.28F;

    /**
     * A chance de uma aresta entre duas vizinhas ser aceita como ligacao de mae.
     *
     * E este numero, e nao o das pontes, que decide quantas pontes existem: a ponte so
     * cabe entre colunas de mesmo patamar, e sao as arestas aceitas que fazem duas
     * casas dividirem patamar. Em 0.0 nao haveria ponte nenhuma e o mapa seria so
     * paisagem; perto de 1.0 quase tudo cairia num plano so, que e o tabuleiro plano
     * que estamos desfazendo.
     */
    private static final float SAME_LEVEL = 0.40F;

    /** Quantas ligacoes a mais, entre vizinhas de mesma altura que sobraram sem ponte. */
    private static final float EXTRA_BRIDGES = 0.35F;

    /**
     * Teto de passos ao subir a cadeia de maes.
     *
     * A cadeia nao pode dar volta (o posto sempre diminui), entao isto nao e uma trava
     * contra laco infinito e sim contra CUSTO: uma cadeia comprida faria uma consulta
     * de altura varrer meio mapa. Medido, o pior caso foi 4; 24 e folga pura.
     */
    private static final int MAX_CHAIN = 24;

    /**
     * Quanto uma peca pode transbordar a casa dela.
     *
     * A coluna com arvore tem 21x23 porque a copa passa da borda do chao. Sem esta
     * folga na varredura, o chunk vizinho nao saberia que uma copa entra nele e a
     * arvore sairia cortada no ar.
     */
    private static final int OVERREACH = 16;

    private final long seed;

    private final DimPiece island;
    private final DimPiece grove;
    private final DimPiece[] bridge;

    /**
     * O patamar ja descoberto de cada casa.
     *
     * So para nao repetir a subida da cadeia — o resultado e o mesmo com ou sem ele.
     * Esvazia inteiro quando cresce demais: num mundo infinito, um mapa que so cresce
     * e um vazamento de memoria com outro nome, e recalcular custa 4 passos.
     */
    private final Map<Long, Integer> levels = new ConcurrentHashMap<>();

    private static final int LEVEL_CACHE_MAX = 20000;

    private ChunksLayout(long seed, DimPiece island, DimPiece grove, DimPiece[] bridge) {
        this.seed = seed;
        this.island = island;
        this.grove = grove;
        this.bridge = bridge;
    }

    // ------------------------------------------------------------------ construcao
    public static ChunksLayout build(PieceSet set, long seed) {
        DimPiece island = set.byName("island");
        DimPiece grove = set.byName("grove");
        DimPiece[] bridge = {set.byName("bridge_a"), set.byName("bridge_b"), set.byName("bridge_c")};
        if (island == null || grove == null || bridge[0] == null) {
            LOGGER.warn("[dimensao] chunks sem pecas: a dimensao vai nascer vazia");
            return new ChunksLayout(seed, null, null, bridge);
        }
        LOGGER.info("[dimensao] chunks: campo infinito pronto (semente {})", seed);
        return new ChunksLayout(seed, island, grove, bridge);
    }

    public BlockPos spawnPos() {
        return new BlockPos(ISLAND / 2, SPAWN_Y + 1, ISLAND / 2);
    }

    /**
     * O mesmo, mas numa coluna SORTEADA — e nunca a mesma duas vezes.
     *
     * Regra do Pedro para as 21: "spawn deve ser em diferentes locais das dimensoes e
     * nunca no mesmo spawn". Aqui ela muda o que a dimensao E: nascer sempre na coluna do
     * centro dava sempre o mesmo patamar (o centro tem posto negativo justamente para
     * isso) e sempre a mesma vista. Sorteando, o jogador as vezes cai num grupo grande com
     * pontes para todo lado e as vezes numa coluna sozinha de onde nao se sai a pe — e as
     * duas coisas ja existiam no mapa, ele so nunca tinha caido nelas.
     *
     * ⚠️ A altura vem do `levelOf` daquela casa, e nao de SPAWN_Y. O patamar do centro e
     * SPAWN_Y por construcao, mas de qualquer outra casa nao: usar SPAWN_Y direto largaria
     * o jogador 56 blocos dentro da pedra ou 56 acima do chao.
     */
    public BlockPos randomSpawn() {
        if (island == null) return spawnPos();
        java.util.Random dice = new java.util.Random();
        for (int tries = 0; tries < SPAWN_TRIES; tries++) {
            int cx = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
            int cz = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
            if (!occupied(cx, cz)) continue;
            BlockPos found = standingSpot(column(cx, cz));
            if (found != null) return found;
        }
        return spawnPos();
    }

    /**
     * Um ponto da ilha em que da para ficar de pe: grama embaixo e dois ares em cima.
     *
     * ⚠️ Nao da para chutar "o meio do quadrado". Metade das casas recebe o BOSQUE, e no
     * bosque o meio pode ser tronco de carvalho — nascer dentro de um tronco seria a
     * primeira coisa que o jogador veria da dimensao. Perguntar a peca custa um punhado de
     * leituras de vetor e responde certo nas duas.
     *
     * A varredura comeca no meio e vai para fora para o spawn nao acabar sempre na quina:
     * a quina da ilha e onde se olha o vazio de dois lados de uma vez, o que e a segunda
     * coisa a se ver, nao a primeira.
     */
    private BlockPos standingSpot(Placement column) {
        DimPiece piece = column.piece;
        int grass = piece.anchorY;
        if (grass + 2 >= piece.height) return null;
        int mid = ISLAND / 2;
        for (int ring = 0; ring < mid; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int lx = piece.anchorX + mid + dx;
                    int lz = piece.anchorZ + mid + dz;
                    if (lx < 0 || lz < 0 || lx >= piece.width || lz >= piece.length) continue;
                    if (piece.at(lx, grass, lz, 0).isAir()) continue;
                    if (!piece.at(lx, grass + 1, lz, 0).isAir()) continue;
                    if (!piece.at(lx, grass + 2, lz, 0).isAir()) continue;
                    return new BlockPos(column.worldX(lx, lz), column.oy + grass + 1,
                            column.worldZ(lx, lz));
                }
            }
        }
        return null;
    }

    /** O quadrado de casas em que a fita pode largar o jogador. */
    private static final int SPAWN_SPREAD = 64;
    private static final int SPAWN_TRIES = 64;

    // ------------------------------------------------------------------ o tabuleiro
    private static long cellKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    /** Ha coluna nesta casa? O centro tem sempre — e onde a fita larga o jogador. */
    private boolean occupied(int cx, int cz) {
        if (cx == 0 && cz == 0) return true;
        return frac(hash(seed, cx, cz, 1L)) >= VOID_CHANCE;
    }

    /**
     * O posto da casa: quem manda em quem na hora de escolher a mae.
     *
     * O centro tem posto negativo de proposito. Assim ele nunca e filho de ninguem,
     * e o patamar do spawn e sempre o dele — o jogador nao chega numa dimensao que
     * decidiu, no sorteio, comecar 56 blocos acima do que a fita prometeu.
     */
    private double rank(int cx, int cz) {
        if (cx == 0 && cz == 0) return -1.0D;
        return frac(hash(seed, cx, cz, 2L));
    }

    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** A aresta entre duas vizinhas foi sorteada como ligacao? A ordem nao importa. */
    private boolean edgeAccepted(int ax, int az, int bx, int bz, long salt, float chance) {
        boolean first = ax < bx || (ax == bx && az < bz);
        int lx = first ? ax : bx, lz = first ? az : bz;
        int hx = first ? bx : ax, hz = first ? bz : az;
        return frac(hash(seed, lx, lz, hx * 31L + hz * 7919L + salt)) < chance;
    }

    /**
     * A vizinha para a qual esta casa aponta, ou null se ela e raiz.
     *
     * Entre as vizinhas de aresta aceita, vale a de MENOR posto — e so se o posto dela
     * for menor que o meu. E o que garante que subir a cadeia sempre chega a algum
     * lugar em vez de dar voltas entre duas casas apontando uma para a outra.
     */
    private long parent(int cx, int cz) {
        if (!occupied(cx, cz)) return NO_PARENT;

        // A afilhada do centro entra na marra. Sem ela, a casa do spawn pode calhar
        // sozinha no seu patamar, e o jogador chega numa coluna sem ponte nenhuma,
        // olhando o mapa inteiro de longe sem ter para onde ir a pe.
        if (isFirstChild(cx, cz)) return cellKey(0, 0);

        double mine = rank(cx, cz);
        long best = NO_PARENT;
        double bestRank = mine;
        for (int[] side : SIDES) {
            int nx = cx + side[0], nz = cz + side[1];
            if (!occupied(nx, nz)) continue;
            double other = rank(nx, nz);
            if (other >= bestRank) continue;
            if (!edgeAccepted(cx, cz, nx, nz, 3L, SAME_LEVEL)) continue;
            best = cellKey(nx, nz);
            bestRank = other;
        }
        return best;
    }

    private static final long NO_PARENT = Long.MIN_VALUE;

    /** Esta casa e a vizinha do centro escolhida para herdar o patamar dele? */
    private boolean isFirstChild(int cx, int cz) {
        if (Math.abs(cx) + Math.abs(cz) != 1) return false;
        int start = (int) Math.floorMod(hash(seed, 0, 0, 9L), 4L);
        for (int i = 0; i < SIDES.length; i++) {
            int[] side = SIDES[(start + i) & 3];
            if (occupied(side[0], side[1])) return cx == side[0] && cz == side[1];
        }
        return false;
    }

    /**
     * A altura da grama desta casa, ou -1 se ali so ha vazio.
     *
     * Sobe a cadeia de maes ate a raiz e devolve o patamar DELA: e assim que um grupo
     * inteiro sai no mesmo nivel sem ninguem ter crescido nada.
     */
    private int levelOf(int cx, int cz) {
        if (!occupied(cx, cz)) return -1;
        long key = cellKey(cx, cz);
        Integer known = levels.get(key);
        if (known != null) return known;

        int x = cx, z = cz;
        for (int step = 0; step < MAX_CHAIN; step++) {
            long up = parent(x, z);
            if (up == NO_PARENT) break;
            x = (int) (up >> 32);
            z = (int) up;
        }

        int value = x == 0 && z == 0
                ? SPAWN_Y
                : SPAWN_Y + ((int) Math.floorMod(hash(seed, x, z, 4L), (long) (LEVELS_UP + LEVELS_DOWN + 1))
                        - LEVELS_DOWN) * STEP;

        if (levels.size() > LEVEL_CACHE_MAX) levels.clear();
        levels.put(key, value);
        return value;
    }

    /** Cabe ponte entre estas duas vizinhas? */
    private boolean bridged(int ax, int az, int bx, int bz) {
        if (!occupied(ax, az) || !occupied(bx, bz)) return false;
        if (levelOf(ax, az) != levelOf(bx, bz)) return false;
        if (parent(ax, az) == cellKey(bx, bz) || parent(bx, bz) == cellKey(ax, az)) return true;
        return edgeAccepted(ax, az, bx, bz, 5L, EXTRA_BRIDGES);
    }

    /** A altura da grama num ponto do mundo, ou -1 se ali so ha vazio. */
    public int topAt(int worldX, int worldZ) {
        if (island == null) return -1;
        if (Math.floorMod(worldX, CELL) >= ISLAND || Math.floorMod(worldZ, CELL) >= ISLAND) return -1;
        return levelOf(Math.floorDiv(worldX, CELL), Math.floorDiv(worldZ, CELL));
    }

    // ------------------------------------------------------------------ o carimbo
    /**
     * As pecas que encostam neste chunk, montadas na hora.
     *
     * Varre so as casas que podem alcancar o chunk e joga fora o que nao o toca de
     * fato. Nada disso fica guardado: sao alguns objetos por chunk, e guardar seria
     * segurar o mapa inteiro na memoria de novo — que e justamente o que esta
     * dimensao deixou de fazer.
     */
    public List<Placement> piecesIn(ChunkPos pos) {
        if (island == null) return List.of();

        int x0 = pos.getMinBlockX(), x1 = pos.getMaxBlockX();
        int z0 = pos.getMinBlockZ(), z1 = pos.getMaxBlockZ();

        List<Placement> found = new ArrayList<>(4);
        int cx0 = Math.floorDiv(x0 - OVERREACH, CELL), cx1 = Math.floorDiv(x1 + OVERREACH, CELL);
        int cz0 = Math.floorDiv(z0 - OVERREACH, CELL), cz1 = Math.floorDiv(z1 + OVERREACH, CELL);

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (!occupied(cx, cz)) continue;
                add(found, column(cx, cz), x0, z0, x1, z1);
                // A ponte pertence a casa de tras: assim cada vao e desenhado uma vez
                // so, por quem esta a oeste ou ao norte dele.
                if (bridged(cx, cz, cx + 1, cz)) span(found, cx, cz, true, x0, z0, x1, z1);
                if (bridged(cx, cz, cx, cz + 1)) span(found, cx, cz, false, x0, z0, x1, z1);
            }
        }
        return found;
    }

    private static void add(List<Placement> into, Placement placement,
                            int x0, int z0, int x1, int z1) {
        if (placement.maxX() < x0 || placement.minX() > x1) return;
        if (placement.maxZ() < z0 || placement.minZ() > z1) return;
        into.add(placement);
    }

    /**
     * A coluna de uma casa, com a ANCORA dela no canto do chao daquela casa.
     *
     * Sem isto a coluna com arvore ficaria fora de esquadro com a pelada: a selecao
     * dela e 21x23 porque a copa transborda, e o quadrado de chao comeca no meio da
     * selecao. Alinhar pela caixa alinharia a copa; alinhar pela ancora alinha o chao,
     * que e por onde se anda.
     *
     * A do meio e sempre a PELADA: e onde a fita larga o jogador, e nascer dentro de
     * um tronco de carvalho seria a primeira coisa que ele veria da dimensao.
     */
    private Placement column(int cx, int cz) {
        boolean center = cx == 0 && cz == 0;
        DimPiece piece = center || Math.floorMod(hash(seed, cx, cz, 6L), 10L) >= 4L ? island : grove;
        int deep = DEEP_MIN + (int) Math.floorMod(hash(seed, cx, cz, 7L), (long) (DEEP_MAX - DEEP_MIN + 1));
        return new Placement(piece, 0,
                cx * CELL - piece.anchorX,
                levelOf(cx, cz) - piece.anchorY,
                cz * CELL - piece.anchorZ,
                deep, hash(seed, cx, cz, 8L));
    }

    /**
     * A ponte de uma coluna ate a vizinha, fatia por fatia.
     *
     * A ponte do Pedro nao e uma peca inteira, e um modulo de tres blocos que se
     * repete (poste, vao, vao). Ladrilhando `i % 3` a ponte cobre qualquer vao — e e
     * por isso que ela e guardada como tres fatias de um bloco em vez de inteira:
     * guardada inteira, todo vao teria que ter exatamente os 10 blocos do original.
     *
     * So e chamada entre casas de mesmo patamar: a ponte e plana e nao sabe descer.
     */
    private void span(List<Placement> into, int cx, int cz, boolean alongX,
                      int x0, int z0, int x1, int z1) {
        int gapStart = (alongX ? cx : cz) * CELL + ISLAND;
        int gap = CELL - ISLAND;

        // Encostada no meio da borda: a ponte tem 8 de largura e a ilha 16.
        int side = (alongX ? cz : cx) * CELL + (ISLAND - 8) / 2;
        int y = levelOf(cx, cz) - bridge[0].anchorY;

        for (int i = 0; i < gap; i++) {
            DimPiece slice = bridge[i % bridge.length];
            // Girada, a fatia deixa de correr em X e passa a correr em Z; os
            // corrimaos acompanham porque quem gira e a paleta inteira.
            add(into, alongX
                    ? new Placement(slice, 0, gapStart + i, y, side)
                    : new Placement(slice, 1, side, y, gapStart + i), x0, z0, x1, z1);
        }
    }

    // ------------------------------------------------------------------ as colunas
    /**
     * O MEIO DA GRAMA das colunas em volta de um ponto.
     *
     * Existe para quem precisa por alguma coisa (ou alguem) em cima de chao de verdade
     * sem sair procurando: o Ofanim nasce a partir daqui, e e aqui que o julgamento
     * larga quem ele levou. Procurar por mapa de alturas nao serviria — entre as
     * colunas o mapa devolve o fundo do mundo, e a busca cairia no vazio.
     *
     * EM VOLTA DE UM PONTO, e nao "todas": num mapa infinito nao existe a lista de
     * todas. Nao se perdeu nada com isso — os tres que perguntam (o Ofanim solitario, o
     * cerco e o julgamento) ja jogavam fora tudo que estivesse fora de um alcance.
     */
    public List<BlockPos> columnsAround(BlockPos center, int radius) {
        if (island == null) return List.of();

        int cx0 = Math.floorDiv(center.getX() - radius, CELL);
        int cx1 = Math.floorDiv(center.getX() + radius, CELL);
        int cz0 = Math.floorDiv(center.getZ() - radius, CELL);
        int cz1 = Math.floorDiv(center.getZ() + radius, CELL);

        List<BlockPos> found = new ArrayList<>();
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (!occupied(cx, cz)) continue;
                found.add(new BlockPos(cx * CELL + ISLAND / 2, levelOf(cx, cz), cz * CELL + ISLAND / 2));
            }
        }
        return found;
    }

    // ------------------------------------------------------------------ o sorteio
    /**
     * O sorteio de tudo aqui: mistura a semente com a casa e um sal.
     *
     * Tem que ser hash, e nao um RandomSource andando: um gerador so responde na ordem
     * em que foi chamado, e num mundo infinito as perguntas chegam na ordem em que o
     * jogador anda. Duas partidas veriam mapas diferentes com a mesma semente.
     */
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

    /** O hash como numero entre 0 e 1, para comparar com as chances. */
    private static double frac(long hash) {
        return (hash >>> 11) * 0x1.0p-53D;
    }
}
