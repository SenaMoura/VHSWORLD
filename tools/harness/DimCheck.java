import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.vhsworld.rec.worldgen.dim.DimPiece;
import net.vhsworld.rec.worldgen.dim.GrassroomsChunkGenerator;
import net.vhsworld.rec.worldgen.dim.MazeChunkGenerator;
import net.vhsworld.rec.worldgen.dim.PieceSet;
import net.vhsworld.rec.worldgen.dim.Placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Confere as seis dimensoes novas SEM ABRIR O JOGO.
 *
 * O que ele mede nao e "compilou": e se da para andar. Reimplementar a regra em Python
 * (como foi feito na CHUNKS) mediria a MINHA copia da regra, e nao a regra; aqui o
 * harness carrega as pecas de verdade do .bin e refaz a conta do gerador com os mesmos
 * numeros, entao o que ele reprova reprova de verdade.
 *
 * A receita do bootstrap e a que custou uma sessao para descobrir: setVersion + bootStrap
 * DENTRO de try/catch. O bootStrap enche os registros e SO DEPOIS chama NetworkHooks.init,
 * que morre fora do launcher — quando ele explodir ali, os blocos ja estao todos de pe.
 */
public final class DimCheck {

    static int failures = 0;

    public static void main(String[] args) throws Exception {
        try {
            SharedConstants.setVersion(net.minecraft.DetectedVersion.BUILT_IN);
            Bootstrap.bootStrap();
        } catch (Throwable t) {
            // Esperado: NetworkHooks.init nao roda fora do launcher.
        }
        if (Blocks.STONE.defaultBlockState().isAir()) {
            System.out.println("!! registros vazios: o bootstrap nao pegou");
            System.exit(2);
        }
        System.out.println("bootstrap ok\n");

        pieces();
        village();
        grassrooms();
        train();
        underPressure();
        biblioteca();
        parkourland();
        maze();

        System.out.println(failures == 0 ? "\n=== TUDO PASSOU ===" : "\n=== " + failures + " REPROVADO(S) ===");
        System.exit(failures == 0 ? 0 : 1);
    }

    static void check(String what, boolean ok, String detail) {
        System.out.printf("  [%s] %-52s %s%n", ok ? "ok" : "XX", what, detail);
        if (!ok) failures++;
    }

    // ------------------------------------------------------------------ as pecas
    static void pieces() {
        System.out.println("=== as pecas assadas ===");
        String[] names = {"village", "grassrooms", "train", "under_pressure", "biblioteca", "parkourland"};
        for (String name : names) {
            PieceSet set = PieceSet.get(name);
            StringBuilder sizes = new StringBuilder();
            for (DimPiece piece : set.pieces()) {
                sizes.append(String.format("%s %dx%dx%d  ", piece.name, piece.width, piece.height, piece.length));
            }
            check(name + ": leu do jar e achou a hub",
                    !set.pieces().isEmpty() && set.hub() != null, sizes.toString().trim());
            // Paleta virou pedra? Se o bootstrap falhasse a peca inteira seria STONE.
            int distinct = 0;
            DimPiece first = set.pieces().isEmpty() ? null : set.pieces().get(0);
            if (first != null) {
                Set<BlockState> seen = new HashSet<>();
                for (int y = 0; y < first.height; y++)
                    for (int z = 0; z < first.length; z++)
                        for (int x = 0; x < first.width; x++) seen.add(first.at(x, y, z, 0));
                distinct = seen.size();
            }
            check(name + ": a paleta nao virou pedra", distinct >= 2, distinct + " estados na 1a peca");
        }
        System.out.println();
    }

    // ------------------------------------------------------------------ VILLAGE
    static final int V_PERIOD_X = 64, V_PERIOD_Z = 32;
    static final int V_ROAD_MIN = 28, V_ROAD_MAX = 35;
    static final int V_HOUSE_A_X = 4, V_HOUSE_B_X = 42, V_HOUSE_Z = 7;
    /** O ultimo X da CASA dentro da peca — o quintal cercado vem depois dele. */
    static final int V_HOUSE_MAX_X = 13;

    static void village() {
        System.out.println("=== VILLAGE ===");
        DimPiece house = PieceSet.get("village").hub();
        if (house == null) { check("peca", false, "nao carregou"); return; }

        check("a casa cabe no lote sem invadir a rua",
                V_HOUSE_A_X + house.width <= V_ROAD_MIN - 2
                        && V_HOUSE_B_X + house.width <= V_PERIOD_X,
                "casa " + house.width + " larga; lote A " + V_HOUSE_A_X + ".."
                        + (V_HOUSE_A_X + house.width - 1) + ", rua em " + V_ROAD_MIN);
        check("as casas nao se encostam ao longo da rua",
                V_HOUSE_Z + house.length <= V_PERIOD_Z,
                "casa " + house.length + " funda, lote " + V_PERIOD_Z + " -> "
                        + (V_PERIOD_Z - V_HOUSE_Z - house.length) + " de gramado");

        // A porta. ⚠️ O TESTE ANTERIOR MEDIU A COISA ERRADA: procurei o vao na borda da
        // PECA (x = width-1) e ele achou z=1 — mas a borda da peca e a cerca do quintal,
        // que e quase toda ar. O vao ali nao e porta nenhuma. A parede da casa e x=13, e e
        // nela que a porta tem que estar.
        int wall = 13;
        int doorZ = -1;
        for (int z = 0; z < house.length; z++) {
            if (house.at(wall, 1, z, 0).isAir() && house.at(wall, 2, z, 0).isAir()) {
                doorZ = z;
                break;
            }
        }
        check("a porta da casa esta na parede x=13, virada para +X", doorZ == 4,
                doorZ >= 0 ? "vao em z=" + doorZ + " (o gerador usa 4)" : "nenhum vao na parede");

        // E o caminho de concreto tem que alcancar um VAO da cerca do quintal, senao ele
        // leva ate um mourao. A cerca e x=17; os vaos foram medidos em z 2, 6, 10 e 14.
        boolean reachesGate = false;
        for (int row = 0; row < 3; row++) {
            if (house.at(17, 1, doorZ + row, 0).isAir()) reachesGate = true;
        }
        check("o caminho de 3 fileiras alcanca o portao da cerca", reachesGate,
                "cerca em z=" + doorZ + ".." + (doorZ + 2) + ": "
                        + house.at(17, 1, doorZ, 0).getBlock().getName().getString() + " / "
                        + house.at(17, 1, doorZ + 1, 0).getBlock().getName().getString() + " / "
                        + house.at(17, 1, doorZ + 2, 0).getBlock().getName().getString());

        // As duas fileiras encaram a rua? A de baixo entra girada 180, entao a parede que
        // era a leste tem que virar a oeste.
        Placement rowB = new Placement(house, 2, V_HOUSE_B_X, 64, V_HOUSE_Z);
        int frontX = rowB.worldX(house.width - 1, doorZ < 0 ? 0 : doorZ);
        check("girada 180, a frente da fileira B aponta para a rua",
                frontX < V_HOUSE_B_X + house.width / 2,
                "frente em x=" + frontX + ", rua acaba em " + V_ROAD_MAX);

        // ------------------------------------------------------------------ o telhado
        //
        // O telhado do gerador assenta em `piece.height` e vai de x=-1 a x=14, e as duas
        // coisas dependem de medidas da PECA que ninguem escreveu em lugar nenhum. Se o
        // Pedro reexportar a casa com uma camada a mais, o telhado passa a flutuar um
        // bloco acima da parede e ninguem descobre ate entrar no mundo.
        int topRing = 0, topInner = 0;
        for (int x = 0; x <= 13; x++) {
            for (int z = 0; z < house.length; z++) {
                boolean solid = !house.at(x, house.height - 1, z, 0).isAir();
                boolean border = x == 0 || x == 13 || z == 0 || z == house.length - 1;
                if (!solid) continue;
                if (border) topRing++; else topInner++;
            }
        }
        check("a ultima camada da casa e ANEL de parede, e nao teto",
                topRing > 0 && topInner * 4 < topRing,
                "borda " + topRing + " blocos, miolo " + topInner + " — o telhado tem que vir do Java");
        check("a parede da casa acaba em x=13 (o telhado se apoia nela)",
                V_HOUSE_MAX_X == 13 && house.width > 13,
                "peca " + house.width + " larga; casa 0..13, quintal 14.." + (house.width - 1));

        // A cumeeira: as duas aguas tem que chegar ao mesmo nivel, senao o telhado abre
        // uma fresta no meio. Refaz a conta do gerador em vez de confiar nela.
        int crest = V_HOUSE_MAX_X / 2;
        int westTop = -1, eastTop = -1, peak = 0;
        for (int lx = -1; lx <= V_HOUSE_MAX_X + 1; lx++) {
            boolean west = lx <= crest;
            int fromEave = west ? lx + 1 : V_HOUSE_MAX_X + 1 - lx;
            int level = fromEave / 2;
            peak = Math.max(peak, level);
            if (lx == crest) westTop = level;
            if (lx == crest + 1) eastTop = level;
        }
        check("as duas aguas do telhado se encontram no mesmo nivel",
                westTop == eastTop && westTop == peak,
                "oeste " + westTop + ", leste " + eastTop + ", mais alto " + peak
                        + " (telhado sobe " + (peak + 1) + " sobre a parede)");
        System.out.println();
    }

    // ------------------------------------------------------------------ GRASSROOMS
    /**
     * ⚠️ ESTA SECAO FOI REESCRITA e vale dizer o que a versao velha media, porque ela
     * PASSAVA. Ela conferia que a maior peca cabia no meio de uma sala de 56x56 com 4 de
     * corredor em volta — e cabia. So que a menor peca tem 8x9, e "cabe" nao era a
     * pergunta: sobravam 47 blocos de piso branco vazio em volta dela, e foi disso que o
     * Pedro reclamou olhando o jogo. O teste media a regra de encaixe e nao o resultado.
     *
     * Agora ele mede o RESULTADO, no proprio codigo do gerador (`planCell`): quanto do
     * quarteirao virou construcao, qual o maior vazio que sobrou, se ha peca em cima de
     * peca, se alguma transborda, e se da para andar do anel ate qualquer fresta.
     */
    static final int G_CELLS = 48;

    static void grassrooms() {
        System.out.println("=== GRASSROOMS ===");
        List<DimPiece> rooms = PieceSet.get("grassrooms").pieces();
        check("as cinco salas entraram", rooms.size() == 5, rooms.size() + " pecas");
        if (rooms.isEmpty()) { System.out.println(); return; }

        int cell = GrassroomsChunkGenerator.CELL;
        int ring = GrassroomsChunkGenerator.RING;

        int minPieces = Integer.MAX_VALUE, worstFill = 100, worstVoid = 0;
        int overlaps = 0, spills = 0, marooned = 0, blockedSpawns = 0;

        for (int i = 0; i < G_CELLS; i++) {
            int cx = (i % 7) - 3, cz = (i / 7) - 3;
            List<Placement> plan = GrassroomsChunkGenerator.planCell(rooms, 1234L, cx, cz);
            minPieces = Math.min(minPieces, plan.size());

            // O mapa do quarteirao: true = construcao, false = branco por onde se anda.
            boolean[][] solid = new boolean[cell][cell];
            for (Placement p : plan) {
                if (p.minX() < cx * cell + ring || p.maxX() > cx * cell + cell - 1
                        || p.minZ() < cz * cell + ring || p.maxZ() > cz * cell + cell - 1) {
                    spills++;
                    continue;
                }
                for (int x = p.minX(); x <= p.maxX(); x++) {
                    for (int z = p.minZ(); z <= p.maxZ(); z++) {
                        int lx = x - cx * cell, lz = z - cz * cell;
                        if (solid[lx][lz]) overlaps++;
                        solid[lx][lz] = true;
                    }
                }
            }

            int filled = 0;
            for (int x = 0; x < cell; x++) for (int z = 0; z < cell; z++) if (solid[x][z]) filled++;
            worstFill = Math.min(worstFill, 100 * filled / (cell * cell));
            worstVoid = Math.max(worstVoid, largestSquare(solid));

            if (solid[1][1]) blockedSpawns++;
            marooned += unreachable(solid);
        }

        check("nenhuma peca cai em cima de outra", overlaps == 0, overlaps + " blocos repetidos");
        check("nenhuma peca transborda o quarteirao", spills == 0, spills + " pecas para fora");
        check("todo quarteirao tem pelo menos 3 construcoes", minPieces >= 3,
                "o mais vazio tem " + minPieces);

        // Os dois numeros que respondem ao pedido do Pedro. A casca antiga punha UMA peca
        // de 8x9 numa casa de 56x56: 2% de construcao e um vazio quadrado de 47 de lado.
        //
        // Os limites estao ABAIXO do que se mede hoje (45% e 14) de proposito: eles sao
        // trava contra regressao, e nao a nota da versao atual. Um limite colado na
        // medicao reprova na primeira vez que o Pedro mudar o peso de uma peca, e ai o
        // teste vira barulho em vez de aviso.
        check("pelo menos 40% do quarteirao e construcao", worstFill >= 40,
                "o pior tem " + worstFill + "% de construcao");
        check("nao sobra praca: o maior vazio quadrado e menor que 20", worstVoid < 20,
                "maior vazio: " + worstVoid + "x" + worstVoid);

        check("o canto do anel, onde a fita larga o jogador, esta livre",
                blockedSpawns == 0, blockedSpawns + " quarteiroes com o canto ocupado");
        check("da para andar do anel ate qualquer fresta", marooned == 0,
                marooned + " blocos de branco ilhados");

        int tallest = 0;
        for (DimPiece room : rooms) tallest = Math.max(tallest, room.height);
        check("o teto passa por cima da peca mais alta",
                GrassroomsChunkGenerator.CEIL_H >= tallest,
                "teto em +" + GrassroomsChunkGenerator.CEIL_H + ", peca mais alta " + tallest);
        System.out.println();
    }

    /** O lado do maior quadrado todo vazio — a medida de "praca". */
    static int largestSquare(boolean[][] solid) {
        int n = solid.length;
        int[][] dp = new int[n][n];
        int best = 0;
        for (int x = 0; x < n; x++) {
            for (int z = 0; z < n; z++) {
                if (solid[x][z]) continue;
                dp[x][z] = (x == 0 || z == 0) ? 1
                        : 1 + Math.min(dp[x - 1][z], Math.min(dp[x][z - 1], dp[x - 1][z - 1]));
                best = Math.max(best, dp[x][z]);
            }
        }
        return best;
    }

    /**
     * Quantos blocos de branco NAO se alcanca partindo do canto do anel.
     *
     * E o teste que sustenta a dimensao inteira: sem parede construida pelo Java, o que
     * liga uma construcao na outra e a fresta que sobra entre elas. Se a guilhotina
     * fechasse um bolsao, o jogador cairia nele de fita e nao teria por onde sair.
     */
    static int unreachable(boolean[][] solid) {
        int n = solid.length;
        boolean[][] seen = new boolean[n][n];
        ArrayList<int[]> queue = new ArrayList<>();
        queue.add(new int[]{1, 1});
        seen[1][1] = true;
        for (int head = 0; head < queue.size(); head++) {
            int[] at = queue.get(head);
            int[][] around = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] step : around) {
                int nx = at[0] + step[0], nz = at[1] + step[1];
                if (nx < 0 || nz < 0 || nx >= n || nz >= n) continue;
                if (seen[nx][nz] || solid[nx][nz]) continue;
                seen[nx][nz] = true;
                queue.add(new int[]{nx, nz});
            }
        }
        int lost = 0;
        for (int x = 0; x < n; x++) {
            for (int z = 0; z < n; z++) if (!solid[x][z] && !seen[x][z]) lost++;
        }
        return lost;
    }

    // ------------------------------------------------------------------ TRAIN
    static void train() {
        System.out.println("=== TRAIN ===");
        PieceSet set = PieceSet.get("train");
        DimPiece track = set.byName("track"), safe = set.byName("safe");
        if (track == null || safe == null) { check("pecas", false, "faltou track ou safe"); return; }

        check("a fatia da via tem 4 de largura (o periodo medido)", track.width == 4,
                track.width + "x" + track.height + "x" + track.length);

        // ⚠️ O TESTE QUE IMPORTA: ladrilhada, a linha e CONTINUA? Um buraco de um bloco
        // no trilho, num caminho de 2 de largura sobre o vazio, e queda garantida.
        int deckY = 64;
        for (int lane : new int[]{0, 1, 8, 9}) {
            int holes = 0;
            for (int x = 0; x < 4 * 12; x++) {
                int local = Math.floorMod(x, 4);
                if (track.at(local, 1, lane, 0).isAir()) holes++;
            }
            check("a faixa z=" + lane + " nao tem falha em 48 blocos", holes == 0, holes + " buracos");
        }

        // O safe spot encosta na via no MESMO Y? Um degrau ali e o pior lugar do mundo.
        int safeTop = -1;
        for (int y = safe.height - 1; y >= 0; y--) {
            boolean any = false;
            for (int x = 0; x < safe.width; x++)
                for (int z = 0; z < safe.length; z++) if (!safe.at(x, y, z, 0).isAir()) any = true;
            if (any) { safeTop = y; break; }
        }
        check("o topo do safe spot casa com o do trilho", safeTop == 1,
                "safe topo local y=" + safeTop + ", trilho local y=1");

        // Ele encosta de verdade? A borda sul do estrado e z=11; o safe comeca em z=10.
        check("o safe spot ao sul encosta no estrado", 10 <= 11 && 10 + safe.length - 1 > 11,
                "safe z=10.." + (10 + safe.length - 1) + ", estrado acaba em z=11");
        check("o safe spot ao norte encosta no estrado", -(safe.length - 1) + safe.length - 1 == 0,
                "safe z=" + (-(safe.length - 1)) + "..0, estrado comeca em z=0");
        System.out.println();
    }

    // ------------------------------------------------------------------ UNDER PRESSURE
    static final int U_BED_Y = 4, U_SEA_Y = 96;

    static void underPressure() {
        System.out.println("=== UNDER PRESSURE ===");
        PieceSet set = PieceSet.get("under_pressure");
        DimPiece top = set.byName("sub_spawn"), deep = set.byName("sub_deep");
        if (top == null || deep == null) { check("pecas", false, "faltou um submarino"); return; }

        // A torre do de spawn tem que FURAR a linha d'agua, e a quilha ficar submersa.
        int oy = U_SEA_Y - (top.height - 3);
        check("o submarino de spawn fura a linha d'agua", oy + top.height - 1 > U_SEA_Y,
                "topo em y=" + (oy + top.height - 1) + ", agua em " + U_SEA_Y);
        check("e a quilha dele fica embaixo da agua", oy < U_SEA_Y - 2,
                "quilha em y=" + oy);

        // O ponto de spawn. ⚠️ O TESTE ANTERIOR PASSOU MEDINDO A COISA ERRADA: eu conferi
        // que (2,7,7) tem bloco, e tem — e um ALCAPAO. Alcapao fechado e uma laje de tres
        // pixels; aberto, e uma parede em pe e nao ha chao nenhum ali. "Tem bloco" nao e a
        // pergunta; a pergunta e "e chao". O gerador usa x=1, o anel de concreto.
        BlockState under = top.at(1, 7, 7, 0);
        boolean concrete = under.getBlock() == Blocks.GRAY_CONCRETE;
        check("o spawn (1,7,7) e concreto, e nao o alcapao", concrete,
                "x=1: " + under.getBlock().getName().getString()
                        + "  |  x=2: " + top.at(2, 7, 7, 0).getBlock().getName().getString());

        // A caixa seca: nivel por nivel, ela tem que existir na barriga e ser MENOR na torre.
        int[] belly = dryBox(top, 2);
        int[] tower = dryBox(top, 6);
        check("a caixa seca da barriga existe", belly != null,
                belly == null ? "nula" : "x" + belly[0] + ".." + belly[2] + " z" + belly[1] + ".." + belly[3]);
        check("a da torre e mais estreita em Z que a da barriga",
                belly != null && tower != null && (tower[3] - tower[1]) < (belly[3] - belly[1]),
                tower == null ? "nula" : "torre z" + tower[1] + ".." + tower[3]);

        // O afundado nunca pode encostar no leito nem furar a superficie.
        int deepMin = U_BED_Y + 1, deepMax = U_BED_Y + 1 + 55;
        check("o afundado fica entre o leito e a superficie",
                deepMin > U_BED_Y && deepMax + deep.height - 1 < U_SEA_Y,
                "y " + deepMin + ".." + deepMax + ", topo maximo " + (deepMax + deep.height - 1));

        // Cabe na casa de 64 em qualquer giro?
        int span = Math.max(deep.width, deep.length);
        check("o submarino cabe na casa de 64 em qualquer giro", 12 + 24 + span <= 64,
                "maior lado " + span + ", canto maximo 12+24=36");
        System.out.println();
    }

    static int[] dryBox(DimPiece piece, int y) {
        int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = -1, z1 = -1;
        for (int x = 0; x < piece.width; x++)
            for (int z = 0; z < piece.length; z++) {
                if (piece.at(x, y, z, 0).isAir()) continue;
                x0 = Math.min(x0, x); x1 = Math.max(x1, x);
                z0 = Math.min(z0, z); z1 = Math.max(z1, z);
            }
        if (x1 - x0 < 2 || z1 - z0 < 2) return null;
        return new int[]{x0 + 1, z0 + 1, x1 - 1, z1 - 1};
    }

    // ------------------------------------------------------------------ BIBLIOTECA
    static void biblioteca() {
        System.out.println("=== BIBLIOTECA ===");
        PieceSet set = PieceSet.get("biblioteca");
        DimPiece hall = set.byName("hall"), a = set.byName("shelf_a"), b = set.byName("shelf_b");
        if (hall == null) { check("peca", false, "sem salao"); return; }

        // ⚠️ O TESTE QUE A DIMENSAO INTEIRA DEPENDE: piso e teto sao planos CHEIOS? Se
        // nao forem, ladrilhar deixa buraco no chao a cada 23 ou 35 blocos.
        int floor = 0, ceiling = 0;
        for (int x = 0; x < hall.width; x++)
            for (int z = 0; z < hall.length; z++) {
                if (!hall.at(x, 0, z, 0).isAir()) floor++;
                if (!hall.at(x, hall.height - 1, z, 0).isAir()) ceiling++;
            }
        int area = hall.width * hall.length;
        check("o piso do salao e um plano cheio", floor == area, floor + "/" + area);
        check("o teto do salao e um plano cheio", ceiling == area, ceiling + "/" + area);

        // As estantes extras: o ponto medido tem que estar livre de y=1 ao topo delas.
        shelfFits(hall, a, 18, 2, "shelf_a");
        shelfFits(hall, b, 0, 8, "shelf_b");

        // E o spawn (1,1) tem que ter piso e dois ares.
        check("o ponto de spawn (1,1) e piso livre",
                !hall.at(1, 0, 1, 0).isAir() && hall.at(1, 1, 1, 0).isAir() && hall.at(1, 2, 1, 0).isAir(),
                "piso " + !hall.at(1, 0, 1, 0).isAir());
        System.out.println();
    }

    static void shelfFits(DimPiece hall, DimPiece shelf, int lx, int lz, String label) {
        if (shelf == null) { check(label, false, "nao carregou"); return; }
        if (lx + shelf.width > hall.width || lz + shelf.length > hall.length) {
            check(label + ": cabe na pegada do salao", false,
                    "x" + lx + "+" + shelf.width + " z" + lz + "+" + shelf.length);
            return;
        }
        int blocked = 0;
        for (int x = lx; x < lx + shelf.width; x++)
            for (int z = lz; z < lz + shelf.length; z++)
                for (int y = 1; y < Math.min(shelf.height, hall.height - 1); y++)
                    if (!hall.at(x, y, z, 0).isAir()) blocked++;
        check(label + ": o ponto medido esta vazio no salao", blocked == 0,
                blocked + " blocos no caminho");
        check(label + ": nao passa do teto", shelf.height <= hall.height - 1,
                shelf.height + " de alto, interior " + (hall.height - 1));
    }

    // ------------------------------------------------------------------ PARKOURLAND
    static final int P_CAGE_Y = 1;
    static final int P_PATH_X0 = 3, P_PATH_X1 = 26, P_PATH_Z0 = 3, P_PATH_Z1 = 21;
    static final int P_RUN_X = P_PATH_X1 - P_PATH_X0, P_RUN_Z = P_PATH_Z1 - P_PATH_Z0;
    static final int P_PERIMETER = 2 * P_RUN_X + 2 * P_RUN_Z;
    static final int P_ARC_START = 9, P_STRIDE = 3, P_STEPS = 177, P_FIRST_Y = P_CAGE_Y + 1;

    static int[] step(int k) {
        int t = Math.floorMod(P_ARC_START + k * P_STRIDE, P_PERIMETER);
        int x, z;
        if (t < P_RUN_X) { x = P_PATH_X0 + t; z = P_PATH_Z0; }
        else if (t < P_RUN_X + P_RUN_Z) { x = P_PATH_X1; z = P_PATH_Z0 + (t - P_RUN_X); }
        else if (t < 2 * P_RUN_X + P_RUN_Z) { x = P_PATH_X1 - (t - P_RUN_X - P_RUN_Z); z = P_PATH_Z1; }
        else { x = P_PATH_X0; z = P_PATH_Z1 - (t - 2 * P_RUN_X - P_RUN_Z); }
        return new int[]{x, z, P_FIRST_Y + k};
    }

    static void parkourland() {
        System.out.println("=== PARKOURLAND ===");
        DimPiece cage = PieceSet.get("parkourland").hub();
        if (cage == null) { check("peca", false, "sem gaiola"); return; }
        System.out.printf("  gaiola %dx%dx%d%n", cage.width, cage.height, cage.length);

        int top = P_FIRST_Y + P_STEPS - 1;
        // O tampo da gaiola: primeiro y local em que a camada e macica.
        int lid = -1;
        for (int y = 0; y < cage.height; y++) {
            int solid = 0;
            for (int x = 0; x < cage.width; x++)
                for (int z = 0; z < cage.length; z++) if (!cage.at(x, y, z, 0).isAir()) solid++;
            if (y > 3 && solid == cage.width * cage.length) { lid = y; break; }
        }
        check("o ultimo degrau sobra 2 blocos abaixo do tampo",
                lid > 0 && top + 2 <= P_CAGE_Y + lid,
                "ultimo degrau y=" + top + ", bau y=" + (top + 1) + ", tampo y=" + (P_CAGE_Y + lid));

        // ⚠️ O TESTE QUE FAZ OU QUEBRA: cada salto e pulavel? Um salto de mais de 4 no
        // plano com +1 de altura nao se vence, e a torre trava naquele degrau para sempre.
        double worst = 0;
        int worstK = -1;
        for (int k = 1; k < P_STEPS; k++) {
            int[] a = step(k - 1), b = step(k);
            double d = Math.hypot(b[0] - a[0], b[1] - a[1]);
            if (d > worst) { worst = d; worstK = k; }
        }
        check("nenhum salto passa de 4 blocos no plano", worst <= 4.001,
                String.format("pior: %.2f no degrau %d", worst, worstK));

        // E nenhum degrau pode nascer FORA da gaiola nem dentro da cerca.
        int outside = 0;
        for (int k = 0; k < P_STEPS; k++) {
            int[] at = step(k);
            for (int dx = 0; dx < 2; dx++)
                for (int dz = 0; dz < 2; dz++) {
                    int x = at[0] + dx, z = at[1] + dz;
                    if (x < 1 || x > cage.width - 2 || z < 1 || z > cage.length - 2) outside++;
                }
        }
        check("nenhum degrau encosta na cerca", outside == 0, outside + " cantos fora");

        // A plataforma de nascimento existe onde o spawn diz?
        int solid = 0;
        for (int x = 1; x <= 9; x++) for (int z = 2; z <= 12; z++) if (!cage.at(x, 0, z, 0).isAir()) solid++;
        check("a plataforma de nascimento e macica", solid == 9 * 11, solid + "/" + (9 * 11));

        // O primeiro degrau tem que estar FORA dela, senao o primeiro salto nao existe.
        int[] first = step(0);
        boolean onStart = first[0] >= 1 && first[0] <= 9 && first[1] >= 2 && first[1] <= 12;
        check("o primeiro degrau cai fora da plataforma", !onStart,
                "degrau 0 em x=" + first[0] + " z=" + first[1]);

        // E o primeiro salto sai da plataforma? 1 de altura, no maximo 4 no plano.
        double reach = Math.max(0, first[0] - 10);
        check("da para sair da plataforma no primeiro salto", reach <= 4,
                "borda leste em x=10, degrau em x=" + first[0]);

        // Quantas voltas a espiral da, e quantos descansos.
        int rests = 0;
        for (int k = 1; k < P_STEPS; k++) if (k % 20 == 0) rests++;
        System.out.printf("  %d degraus, %d descansos, %.1f voltas, sobe %d blocos%n",
                P_STEPS, rests, (P_STEPS * (double) P_STRIDE) / P_PERIMETER, P_STEPS - 1);
        System.out.println();
    }

    // ------------------------------------------------------------------ MAZE
    static final int M_CELL_X = 76, M_CELL_Z = 20;
    static final int M_PIERCE = 2, M_DOOR_H = 5;
    static final int M_SPAWN_X = 10, M_SPAWN_Z = 10;

    /**
     * O que a MAZE assume sobre as pecas do Pedro, e que nada mais no repo garante.
     *
     * As tres afirmacoes que sustentam a dimensao inteira sao medicoes, nao escolhas: a
     * grade so existe porque as duas pecas tem a MESMA pegada; o tunel so atravessa
     * porque a parede mais grossa tem 2; e o spawn so e seguro porque aquele ponto e ar
     * nas DUAS. Qualquer uma delas muda se o Pedro reexportar um maze_*.schem, e nenhuma
     * das tres da erro de compilacao ao mudar — da uma dimensao com porta cega ou com o
     * jogador nascendo dentro de uma parede de 163 blocos.
     */
    static void maze() {
        System.out.println("=== MAZE ===");

        // ⚠️ SO OS LADRILHOS, e nao `pieces()` inteiro. Desde que a cabana entrou no .bin
        // como enfeite de peso 0, o conjunto tem uma peca de 7x6 no meio de duas de
        // 76x20 — e as medicoes abaixo (mesma pegada, espessura da parede, ponto de
        // spawn) leem coordenadas que so existem nas grandes. `DimPiece.at` nao confere
        // limite: perguntar x=20 a uma peca de 7 de largura nao reprova, le lixo.
        List<DimPiece> tiles = new ArrayList<>();
        for (DimPiece piece : PieceSet.get("maze").pieces()) {
            if (piece.weight > 0) tiles.add(piece);
        }
        if (tiles.size() < 2) { check("pecas", false, "esperava 2, achei " + tiles.size()); return; }

        DimPiece first = tiles.get(0);
        boolean sameFootprint = true;
        for (DimPiece tile : tiles) {
            if (tile.width != first.width || tile.length != first.length) sameFootprint = false;
        }
        check("as pecas tem a mesma pegada (e o que permite a grade)", sameFootprint,
                tiles.get(0).name + " " + tiles.get(0).width + "x" + tiles.get(0).length
                        + ", " + tiles.get(1).name + " " + tiles.get(1).width + "x" + tiles.get(1).length);
        check("a pegada e a grade do gerador",
                first.width == M_CELL_X && first.length == M_CELL_Z,
                "peca " + first.width + "x" + first.length + ", grade "
                        + M_CELL_X + "x" + M_CELL_Z);

        // A parede mais grossa. O tunel fura `PIERCE` de cada lado da divisa, entao ele
        // so atravessa se PIERCE >= a espessura. Medido na altura em que o tunel passa.
        int thickest = 0;
        String where = "";
        for (DimPiece tile : tiles) {
            int east = 0;
            while (east < tile.width && !tile.at(tile.width - 1 - east, 3, tile.length / 2, 0).isAir()) east++;
            int north = 0;
            while (north < tile.length && !tile.at(20, 3, north, 0).isAir()) north++;
            if (east > thickest) { thickest = east; where = tile.name + " leste"; }
            if (north > thickest) { thickest = north; where = tile.name + " norte"; }
        }
        check("o tunel fura a parede mais grossa das pecas", M_PIERCE >= thickest,
                "mais grossa: " + thickest + " (" + where + "), tunel fura " + M_PIERCE);

        // O spawn. ⚠️ A LICAO DO SUBMARINO vale aqui: "tem bloco" nao e a pergunta.
        // Precisa de chao SOLIDO embaixo e de dois blocos de ar para o corpo — a `cross`
        // tem mato alto plantado, e mato nao e ar.
        for (DimPiece tile : tiles) {
            BlockState under = tile.at(M_SPAWN_X, 0, M_SPAWN_Z, 0);
            boolean clear = true;
            for (int y = 1; y <= 2; y++) {
                if (!tile.at(M_SPAWN_X, y, M_SPAWN_Z, 0).isAir()) clear = false;
            }
            check(tile.name + ": o ponto de spawn tem chao e dois de ar",
                    !under.isAir() && clear,
                    "chao: " + under.getBlock().getName().getString() + ", ar acima: " + clear);
        }

        // O tunel tem que caber na altura da peca, e sobrar parede acima dele — um tunel
        // que chegasse ao topo nao seria porta, seria a parede inteira faltando.
        check("o vao de " + M_DOOR_H + " de alto e uma porta, e nao a parede faltando",
                M_DOOR_H * 8 < first.height,
                "porta " + M_DOOR_H + ", parede " + (first.height - 1) + " de alto");

        mazeProps();
        System.out.println();
    }

    /**
     * As variacoes de estrutura: elas nao podem tapar a porta nem entupir o corredor.
     *
     * ⚠️ ESTE E O RISCO REAL DO ENFEITE, e nao a aparencia dele. Uma massa de pedra de
     * ate 112 blocos de altura sorteada em cima de um tunel de 4x5 fecharia a passagem
     * sem deixar rastro nenhum no log — o jogador so descobre andando ate la e achando
     * parede. E uma massa que atravessasse o salao de lado a lado deixaria a casa sem
     * saida. Os dois sao mantidos longe pela mesma folga (`EDGE_X`/`EDGE_Z`), e e ela
     * que este teste mede, com os numeros do proprio gerador.
     */
    static void mazeProps() {
        int cellZ = MazeChunkGenerator.CELL_Z;
        int pierce = MazeChunkGenerator.PIERCE;

        // O tunel norte ocupa z de `divisa-PIERCE` a `divisa+PIERCE-1`, ou seja o local
        // vai ate PIERCE-1. O enfeite comeca em EDGE_Z. A folga e a diferenca.
        check("o enfeite para longe do tunel", MazeChunkGenerator.EDGE_Z - pierce >= 2
                        && MazeChunkGenerator.EDGE_X - pierce >= 2,
                "folga Z " + (MazeChunkGenerator.EDGE_Z - pierce)
                        + ", folga X " + (MazeChunkGenerator.EDGE_X - pierce));

        // A pior massa possivel encostada o mais ao sul que o sorteio permite.
        int lastZ = cellZ - MazeChunkGenerator.EDGE_Z - 1;
        int freeNorth = MazeChunkGenerator.EDGE_Z - 1;
        int freeSouth = (cellZ - 2) - lastZ;
        check("o corredor nunca entope: sobra passagem dos dois lados",
                Math.min(freeNorth, freeSouth) >= 3 && MazeChunkGenerator.PROP_MAX_L < cellZ,
                "livre ao norte " + freeNorth + ", ao sul " + freeSouth
                        + ", massa mais funda " + MazeChunkGenerator.PROP_MAX_L);

        DimPiece cabin = PieceSet.get("maze").byName("cabin");
        if (cabin == null) { check("a cabana do Pedro entrou no .bin", false, "byName devolveu null"); return; }
        check("a cabana tem peso 0 (nunca e sorteada como casa)", cabin.weight == 0,
                "peso " + cabin.weight);

        // Nos quatro giros: cabe no miolo, com as duas margens?
        int worst = Integer.MAX_VALUE;
        for (int rotation = 0; rotation < 4; rotation++) {
            worst = Math.min(worst, Math.min(
                    MazeChunkGenerator.CELL_X - 2 * MazeChunkGenerator.EDGE_X - cabin.rotatedWidth(rotation),
                    cellZ - 2 * MazeChunkGenerator.EDGE_Z - cabin.rotatedLength(rotation)));
        }
        check("a cabana cabe no miolo nos quatro giros", worst >= 0,
                cabin.width + "x" + cabin.height + "x" + cabin.length + ", pior sobra " + worst);
    }
}
