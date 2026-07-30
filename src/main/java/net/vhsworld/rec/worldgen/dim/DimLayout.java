package net.vhsworld.rec.worldgen.dim;

import com.mojang.logging.LogUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A planta da dimensao DATA: onde cada construcao do Pedro fica.
 *
 * COMO CRESCE: comeca pela hub e vai puxando peca por peca pelas portas. Cada porta
 * aberta pede uma peca com porta da MESMA BITOLA, gira a peca ate a porta dela ficar de
 * frente para a que chamou, encosta as duas paredes e centraliza os vaos. Se nao couber
 * (bateria em algo ja posto), tenta a proxima candidata; se nenhuma servir, o vao e
 * TAPADO.
 *
 * TAPAR E OBRIGATORIO, nao acabamento: a dimensao e vazio puro em volta dos
 * corredores. Um buraco na parede externa e uma queda sem fundo que o jogador so
 * descobre pisando. Por isso a regra aqui nao e "tapa a porta que sobrou" e sim a
 * inversa, que nao depende de eu ter adivinhado certo o que e porta: TODA parede
 * externa e solida, MENOS os vaos que deram em outra peca.
 *
 * ======================= ELA NAO ACABA MAIS =======================
 *
 * Antes o mapa era finito de proposito — 220 pecas dentro de um quadrado de 512, pelo
 * raciocinio de que um predio fechado assusta mais que um infinito, porque o jogador
 * decora e percebe que ja passou ali. O Pedro pediu infinito, entao o predio ficou
 * infinito; o que se guardou daquela decisao foi o essencial: ele continua sendo UM
 * PREDIO SO, ligado. Nao sao complexos soltos boiando no vazio, cada um com o seu
 * jogador preso dentro — todo corredor novo nasce de uma porta de um corredor velho,
 * entao daqui da para andar ate qualquer ponto do infinito a pe.
 *
 * O crescimento e SOB DEMANDA e por DISTANCIA: as portas pendentes esperam numa fila
 * de prioridade, a mais perta da hub na frente, e quem pede um chunk manda crescer ate
 * o raio dele. Puxar sempre a porta mais perta e o que faz a planta ser a mesma em
 * qualquer partida: crescer ate 400 e depois ate 900 da exatamente o mesmo resultado
 * que ir direto a 900, porque o segundo pedido so continua a lista de onde o primeiro
 * parou. Nao importa por onde o jogador andou, nem em que ordem as threads pediram os
 * chunks.
 *
 * ⚠️ O risco que sobra: uma peca posta agora em cima de um chunk que JA foi gerado e
 * salvo apareceria cortada, porque aquele chunk nao vai ser desenhado de novo. E por
 * isso que se cresce {@link #GROW_AHEAD} blocos ALEM do que pediram — a fronteira anda
 * sempre bem na frente do que o jogo esta desenhando.
 */
public final class DimLayout {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Chao de todas as pecas. Elas tem alturas diferentes, mas o piso e o mesmo. */
    public static final int FLOOR_Y = 32;

    /**
     * Quanto crescer alem do que o chunk pediu.
     *
     * Nao e folga de conforto, e a trava dos dois unicos jeitos de esta dimensao sair
     * errada. Um: peca posta depois que o chunk dela ja foi salvo, que apareceria
     * cortada. Dois, e pior: uma peca sendo carimbada por uma thread enquanto outra
     * abre uma porta dela (`markOpen`) — o carimbo TAPA o que ainda nao sabe que e
     * passagem, e o corredor amanhece com uma parede no meio.
     *
     * Os dois somem pela mesma conta. Quando alguem pede o chunk C, a planta ja cresceu
     * ate a distancia de C mais 160; as portas de qualquer peca dentro de C estao a bem
     * menos que isso da hub, entao TODAS ja foram resolvidas antes de o carimbo comecar.
     * 160 e varias vezes a maior peca do Pedro — e a margem que faz essa frase ser
     * verdade mesmo com o jogo desenhando varios chunks ao mesmo tempo.
     */
    private static final int GROW_AHEAD = 160;

    /** O lado da grade que acha vizinho na hora de testar batida entre pecas. */
    private static final int COLLISION_CELL = 32;

    /**
     * Quantas pecas ganham um bau. A hub ganha sempre — e a primeira sala que o
     * jogador ve, e uma dimensao que abre com uma sala vazia parece um erro de
     * geracao antes de parecer atmosfera.
     */
    private static final float CHEST_CHANCE = 0.35F;

    private final long seed;

    /** Toda porta que pode ser puxada, ja repetida pelo peso da peca. Nunca muda. */
    private final List<Candidate> candidates = new ArrayList<>();

    /** So o crescimento mexe: e ele que da o indice que as portas guardam. */
    private final List<Placement> placements = new ArrayList<>();

    private final PriorityQueue<Door> open = new PriorityQueue<>(
            Comparator.comparingDouble(Door::distance).thenComparingLong(Door::order));
    private long doorOrder;

    /**
     * UM indice so, numa grade de 32: onde esta cada peca e cada bau.
     *
     * Serve as duas perguntas — "bateu em alguem?" (crescimento) e "o que cai neste
     * chunk?" (geracao). Um chunk tem 16 e cai em no maximo 4 casas da grade, e toda
     * peca esta em todas as casas que ela toca, entao varrer essas 4 acha tudo que
     * encosta no chunk sem sobra nem margem de erro.
     *
     * As listas nunca sao alteradas depois de postas: crescer troca a lista inteira
     * por outra. Assim uma thread de geracao que esta lendo os pedacos de um chunk
     * nunca ve uma lista pela metade.
     */
    private final Map<Long, List<Placement>> piecesByCell = new ConcurrentHashMap<>();
    private final Map<Long, List<Chest>> chestsByCell = new ConcurrentHashMap<>();

    private Placement hub;

    /** Ate que raio a planta ja esta resolvida. So cresce. */
    private volatile double grown;

    /** Um bau encostado numa parede, ja em coordenada do mundo. */
    public record Chest(int x, int y, int z, net.minecraft.core.Direction facing) {}

    /** Uma porta ainda nao resolvida, e a que distancia da hub ela esta. */
    private record Door(int placement, int connector, double distance, long order) {}

    private record Candidate(DimPiece piece, int connector) {}

    private DimLayout(long seed) {
        this.seed = seed;
    }

    public Placement hub() {
        return hub;
    }

    // ------------------------------------------------------------------ construcao
    public static DimLayout build(PieceSet set, long seed) {
        DimLayout layout = new DimLayout(seed);
        if (set.hub() == null) {
            LOGGER.warn("[dimensao] sem peca de hub: a dimensao vai nascer vazia");
            return layout;
        }

        for (DimPiece piece : set.pieces()) {
            if (piece.weight <= 0) continue;          // a hub nao se repete
            for (int i = 0; i < piece.connectors.length; i++) {
                for (int w = 0; w < piece.weight; w++) {
                    layout.candidates.add(new Candidate(piece, i));
                }
            }
        }

        DimPiece hubPiece = set.hub();
        layout.hub = new Placement(hubPiece, 0, -hubPiece.width / 2, FLOOR_Y, -hubPiece.length / 2);
        layout.accept(layout.hub);
        for (int i = 0; i < hubPiece.connectors.length; i++) {
            layout.enqueue(0, i);
        }
        LOGGER.info("[dimensao] data: hub posta, o resto nasce conforme o jogador anda");
        return layout;
    }

    /**
     * Onde a fita larga o jogador.
     *
     * Nao e o centro da hub: o centro dela e a piscina. Procura o piso livre mais
     * perto do meio que tenha dois blocos de ar por cima — cair dentro d'agua ou
     * nascer entalado numa parede seria a primeira coisa que o jogador veria da
     * dimensao, e as duas contam a historia errada.
     */
    public net.minecraft.core.BlockPos spawnPos() {
        return spawnIn(hub);
    }

    /**
     * O mesmo, mas numa peca SORTEADA do labirinto — e nunca a mesma duas vezes.
     *
     * Regra do Pedro para as 21: "spawn deve ser em diferentes locais das dimensoes e
     * nunca no mesmo spawn". Aqui ela e mais que variedade: nascer sempre na hub fazia da
     * hub um ponto de referencia, e um labirinto com ponto de referencia deixa de ser
     * labirinto na segunda visita. Sem ela, entrar duas vezes e entrar em dois lugares.
     *
     * ⚠️ A planta E CRESCIDA ANTES de sortear. Ela nasce so com a hub e o resto aparece
     * conforme o jogador pede chunk; sorteando sem crescer, a lista de pecas teria uma so
     * e o sorteio devolveria a hub sempre — parecendo funcionar e nao funcionando.
     */
    public net.minecraft.core.BlockPos randomSpawn() {
        if (hub == null) return new net.minecraft.core.BlockPos(0, FLOOR_Y + 1, 0);
        growTo(SPAWN_RADIUS);
        List<Placement> options = List.copyOf(placements);
        if (options.isEmpty()) return spawnPos();
        return spawnIn(options.get(new java.util.Random().nextInt(options.size())));
    }

    /**
     * Ate onde a planta cresce para o sorteio do spawn.
     *
     * 600 blocos dao algumas centenas de pecas, o bastante para dois spawns seguidos
     * praticamente nunca calharem na mesma. Nao vale crescer mais: crescer e o que custa
     * nesta dimensao, e o jogador que quer ir longe leva a planta com ele andando.
     */
    private static final double SPAWN_RADIUS = 600.0D;

    /** O piso livre mais perto do meio desta peca. */
    private net.minecraft.core.BlockPos spawnIn(Placement where) {
        if (where == null) return new net.minecraft.core.BlockPos(0, FLOOR_Y + 1, 0);
        DimPiece piece = where.piece;
        int cx = piece.width / 2, cz = piece.length / 2;
        int bestX = cx, bestZ = cz, bestDistance = Integer.MAX_VALUE;
        for (int x = 0; x < piece.width; x++) {
            for (int z = 0; z < piece.length; z++) {
                if (!piece.at(x, 0, z, 0).isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,
                        net.minecraft.core.BlockPos.ZERO)) continue;
                if (!piece.at(x, 1, z, 0).isAir()) continue;
                if (piece.height > 2 && !piece.at(x, 2, z, 0).isAir()) continue;
                int distance = Math.abs(x - cx) + Math.abs(z - cz);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestX = x;
                    bestZ = z;
                }
            }
        }
        return new net.minecraft.core.BlockPos(where.worldX(bestX, bestZ), where.oy + 1,
                where.worldZ(bestX, bestZ));
    }

    // ------------------------------------------------------------------ consulta
    public List<Placement> piecesIn(ChunkPos pos) {
        growTo(reach(pos));
        List<Placement> found = new ArrayList<>(4);
        for (long cell : cellsOf(pos)) {
            for (Placement placement : piecesByCell.getOrDefault(cell, List.of())) {
                if (placement.maxX() < pos.getMinBlockX() || placement.minX() > pos.getMaxBlockX()) continue;
                if (placement.maxZ() < pos.getMinBlockZ() || placement.minZ() > pos.getMaxBlockZ()) continue;
                // Uma peca grande esta em varias casas da grade, e o chunk pode pegar
                // mais de uma delas: sem isto ela seria carimbada duas vezes.
                if (!found.contains(placement)) found.add(placement);
            }
        }
        return found;
    }

    public List<Chest> chestsIn(ChunkPos pos) {
        growTo(reach(pos));
        List<Chest> found = new ArrayList<>(2);
        for (long cell : cellsOf(pos)) {
            for (Chest chest : chestsByCell.getOrDefault(cell, List.of())) {
                if ((chest.x() >> 4) == pos.x && (chest.z() >> 4) == pos.z && !found.contains(chest)) {
                    found.add(chest);
                }
            }
        }
        return found;
    }

    /** As casas da grade que este chunk toca: no maximo quatro. */
    private static long[] cellsOf(ChunkPos pos) {
        int cx0 = Math.floorDiv(pos.getMinBlockX(), COLLISION_CELL);
        int cx1 = Math.floorDiv(pos.getMaxBlockX(), COLLISION_CELL);
        int cz0 = Math.floorDiv(pos.getMinBlockZ(), COLLISION_CELL);
        int cz1 = Math.floorDiv(pos.getMaxBlockZ(), COLLISION_CELL);
        long[] cells = new long[(cx1 - cx0 + 1) * (cz1 - cz0 + 1)];
        int i = 0;
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                cells[i++] = cellKey(cx, cz);
            }
        }
        return cells;
    }

    /** Ate onde a planta precisa estar resolvida para este chunk sair inteiro. */
    private static double reach(ChunkPos pos) {
        int x = Math.max(Math.abs(pos.getMinBlockX()), Math.abs(pos.getMaxBlockX()));
        int z = Math.max(Math.abs(pos.getMinBlockZ()), Math.abs(pos.getMaxBlockZ()));
        return Math.sqrt((double) x * x + (double) z * z) + GROW_AHEAD;
    }

    // ------------------------------------------------------------------ crescimento
    /**
     * Resolve toda porta pendente que esteja a menos de `radius` da hub.
     *
     * Sempre a mais perta primeiro. E dai que vem a garantia de que a planta e uma so:
     * o conjunto de portas com distancia menor que um raio nao depende de quando ou de
     * quantas vezes se pediu para crescer, so do raio.
     */
    private void growTo(double radius) {
        if (radius <= grown) return;
        synchronized (this) {
            if (radius <= grown) return;

            int before = placements.size();
            while (!open.isEmpty() && open.peek().distance() <= radius) {
                resolve(open.poll());
            }
            grown = radius;

            int made = placements.size() - before;
            if (made > 0) {
                LOGGER.debug("[dimensao] data: +{} pecas ate o raio {} ({} no total)",
                        made, (int) radius, placements.size());
            }
        }
    }

    /** Tenta encaixar uma peca nesta porta. Se nada couber, ela fica sendo parede. */
    private void resolve(Door door) {
        Placement from = placements.get(door.placement());
        Connector fromConnector = from.piece.connectors[door.connector()];

        // O sorteio sai da PORTA, e nao de um gerador que anda junto com o
        // crescimento: assim a peca escolhida aqui e a mesma tenha esta porta sido a
        // decima ou a decima-milesima a ser resolvida.
        List<Candidate> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled,
                new java.util.Random(seed ^ ((long) door.placement() << 8) ^ door.connector()));

        for (Candidate candidate : shuffled) {
            Connector target = candidate.piece.connectors[candidate.connector];
            if (target.gauge() != fromConnector.gauge()) continue;

            Placement placed = fit(from, fromConnector, candidate.piece, target);
            if (placed == null || collides(placed)) continue;

            int index = placements.size();
            accept(placed);
            from.markOpen(door.connector());
            placed.markOpen(candidate.connector);
            for (int i = 0; i < candidate.piece.connectors.length; i++) {
                if (i != candidate.connector) enqueue(index, i);
            }
            return;
        }
    }

    /** Poe a peca na planta: lista, indice da grade e o bau, se ela ganhar um. */
    private void accept(Placement placement) {
        placements.add(placement);
        forEachCell(placement, key -> append(piecesByCell, key, placement));
        placeChest(placement);
    }

    private void enqueue(int placement, int connector) {
        Placement from = placements.get(placement);
        int[] box = from.doorBox(from.piece.connectors[connector]);
        double x = (box[0] + box[3]) / 2.0D, z = (box[2] + box[5]) / 2.0D;
        open.add(new Door(placement, connector, Math.sqrt(x * x + z * z), doorOrder++));
    }

    // ------------------------------------------------------------------ baus
    /**
     * Um bau na peca sorteada, ENCOSTADO NUMA PAREDE.
     *
     * Encostado, e nao no meio do corredor, por duas razoes: no meio ele vira um
     * obstaculo numa fuga (e desta dimensao se foge), e um bau contra a parede le
     * como "alguem deixou isso aqui" — que e a historia que a DATA conta.
     *
     * O sorteio sai da SEMENTE e da posicao da peca, nunca de um contador: assim o
     * mesmo mundo poe os baus sempre nos mesmos lugares.
     */
    private void placeChest(Placement placement) {
        RandomSource random = RandomSource.create(
                seed ^ ((long) placement.ox * 341873128712L) ^ ((long) placement.oz * 132897987541L));
        boolean isHub = placement == hub;
        if (!isHub && random.nextFloat() > CHEST_CHANCE) return;

        List<int[]> spots = wallSpots(placement.piece);
        if (spots.isEmpty()) return;

        int[] spot = spots.get(random.nextInt(spots.size()));
        net.minecraft.core.Direction facing = net.minecraft.core.Direction.from3DDataValue(spot[2]);
        for (int i = 0; i < placement.rotation; i++) facing = facing.getClockWise();

        Chest chest = new Chest(placement.worldX(spot[0], spot[1]), placement.oy + 1,
                placement.worldZ(spot[0], spot[1]), facing);
        append(chestsByCell, cellKey(Math.floorDiv(chest.x(), COLLISION_CELL),
                Math.floorDiv(chest.z(), COLLISION_CELL)), chest);
    }

    /**
     * Onde cabe um bau: piso solido, dois blocos de ar em cima e uma parede do lado.
     *
     * Devolve {x, z, indice da direcao para onde o bau OLHA} em coordenada da peca.
     * Fica longe das paredes externas de proposito — um bau plantado dentro de uma
     * porta entalaria a passagem que aquela porta existe para abrir.
     */
    private static List<int[]> wallSpots(DimPiece piece) {
        List<int[]> spots = new ArrayList<>();
        if (piece.height < 3) return spots;
        for (int x = 1; x < piece.width - 1; x++) {
            for (int z = 1; z < piece.length - 1; z++) {
                if (piece.at(x, 0, z, 0).isAir()) continue;
                if (!piece.at(x, 1, z, 0).isAir() || !piece.at(x, 2, z, 0).isAir()) continue;
                for (net.minecraft.core.Direction side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    int nx = x + side.getStepX(), nz = z + side.getStepZ();
                    if (nx < 0 || nx >= piece.width || nz < 0 || nz >= piece.length) continue;
                    if (piece.at(nx, 1, nz, 0).isAir()) continue;
                    // Olha para o LADO OPOSTO ao da parede: um bau de costas para
                    // o corredor nao se abre.
                    spots.add(new int[]{x, z, side.getOpposite().get3DDataValue()});
                    break;
                }
            }
        }
        return spots;
    }

    // ------------------------------------------------------------------ encaixe
    /**
     * Encosta a peca nova na porta que chamou.
     *
     * Duas contas: o EIXO PERPENDICULAR, em que as duas paredes ficam uma colada na
     * outra (a de fora de uma no bloco seguinte a de fora da outra, e os dois vaos de
     * ar viram uma passagem so), e o EIXO DA PAREDE, em que os vaos sao
     * centralizados um no outro. As pecas do Pedro tem portas de larguras diferentes
     * (3, 4, 5 e 6); centralizar deixa a diferenca como um degrauzinho de canto, que
     * e o menos feio dos erros possiveis.
     */
    private static Placement fit(Placement from, Connector fromConnector,
                                 DimPiece piece, Connector target) {
        int fromFace = from.worldFace(fromConnector);
        int needFace = Connector.opposite(fromFace);
        int rotation = (needFace - target.face() + 4) & 3;

        int[] fromSpan = from.doorSpan(fromConnector);   // {inicio, fim, plano}
        int rotatedWidth = piece.rotatedWidth(rotation);
        int rotatedLength = piece.rotatedLength(rotation);

        // Mede a porta da peca nova com o canto na origem, para so depois deslocar.
        Placement probe = new Placement(piece, rotation, 0, from.oy, 0);
        int[] span = probe.doorSpan(target);

        int ox, oz;
        if (Connector.alongX(fromFace)) {
            // Parede norte/sul: encosta em Z, centraliza em X.
            oz = fromFace == Connector.SOUTH ? fromSpan[2] + 1 : fromSpan[2] - rotatedLength;
            ox = Math.floorDiv((fromSpan[0] + fromSpan[1]) - (span[0] + span[1]), 2);
        } else {
            // Parede leste/oeste: encosta em X, centraliza em Z.
            ox = fromFace == Connector.EAST ? fromSpan[2] + 1 : fromSpan[2] - rotatedWidth;
            oz = Math.floorDiv((fromSpan[0] + fromSpan[1]) - (span[0] + span[1]), 2);
        }
        return new Placement(piece, rotation, ox, from.oy, oz);
    }

    /**
     * Bateu em alguma peca ja posta?
     *
     * So contra as vizinhas de grade. Comparar com a lista inteira era barato quando o
     * mapa parava em 220 pecas; num predio que nao acaba, seria o custo subindo com o
     * quadrado do tamanho — a milesima peca comparada com 999, a decima-milesima com
     * 9999, e a dimensao travando quanto mais longe o jogador fosse.
     */
    private boolean collides(Placement candidate) {
        // Uma peca esta em todas as casas que toca, entao basta olhar as casas que a
        // candidata toca — quem bate nela esta em pelo menos uma delas. Testar a mesma
        // peca duas vezes (ela pode estar em duas casas) nao muda a resposta.
        for (int cx = Math.floorDiv(candidate.minX(), COLLISION_CELL);
             cx <= Math.floorDiv(candidate.maxX(), COLLISION_CELL); cx++) {
            for (int cz = Math.floorDiv(candidate.minZ(), COLLISION_CELL);
                 cz <= Math.floorDiv(candidate.maxZ(), COLLISION_CELL); cz++) {
                for (Placement other : piecesByCell.getOrDefault(cellKey(cx, cz), List.of())) {
                    if (candidate.overlaps(other)) return true;
                }
            }
        }
        return false;
    }

    private static long cellKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    private static void forEachCell(Placement placement, java.util.function.LongConsumer sink) {
        for (int cx = Math.floorDiv(placement.minX(), COLLISION_CELL);
             cx <= Math.floorDiv(placement.maxX(), COLLISION_CELL); cx++) {
            for (int cz = Math.floorDiv(placement.minZ(), COLLISION_CELL);
                 cz <= Math.floorDiv(placement.maxZ(), COLLISION_CELL); cz++) {
                sink.accept(cellKey(cx, cz));
            }
        }
    }

    private static <T> void append(Map<Long, List<T>> map, long key, T value) {
        // Troca a lista inteira em vez de mexer na que ja esta la: quem estiver lendo
        // continua com a antiga na mao, inteira, em vez de ver um item aparecendo no
        // meio da varredura dele.
        map.compute(key, (k, old) -> {
            List<T> next = old == null ? new ArrayList<>(2) : new ArrayList<>(old);
            next.add(value);
            return next;
        });
    }
}
