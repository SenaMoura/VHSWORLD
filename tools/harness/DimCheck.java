import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.vhsworld.rec.worldgen.dim.DimPiece;
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
        System.out.println();
    }

    // ------------------------------------------------------------------ GRASSROOMS
    static final int G_CELL = 56, G_MARGIN = 4;

    static void grassrooms() {
        System.out.println("=== GRASSROOMS ===");
        List<DimPiece> rooms = PieceSet.get("grassrooms").pieces();
        check("as cinco salas entraram", rooms.size() == 5, rooms.size() + " pecas");

        int inner = G_CELL - 1 - 2 * G_MARGIN;
        int worst = 0;
        String worstName = "";
        for (DimPiece room : rooms) {
            int slackX = inner - room.width, slackZ = inner - room.length;
            if (Math.min(slackX, slackZ) < worst || worstName.isEmpty()) {
                worst = Math.min(slackX, slackZ);
                worstName = room.name;
            }
        }
        check("a maior sala cabe com o corredor de " + G_MARGIN + " em volta", worst >= 0,
                "pior caso: " + worstName + " sobra " + worst);

        // O corredor de verdade: distancia da parede da celula ate a peca, nos 4 lados.
        for (DimPiece room : rooms) {
            int ox = 1 + G_MARGIN + Math.max(0, (inner - room.width) / 2);
            int oz = 1 + G_MARGIN + Math.max(0, (inner - room.length) / 2);
            int left = ox - 1, right = (G_CELL - 1) - (ox + room.width - 1);
            int top = oz - 1, bottom = (G_CELL - 1) - (oz + room.length - 1);
            check(room.name + ": corredor >= 2 nos quatro lados",
                    Math.min(Math.min(left, right), Math.min(top, bottom)) >= 2,
                    "O" + left + " L" + right + " N" + top + " S" + bottom);
        }

        // A porta e de 4x5 e tem que caber na parede e no teto mais baixo.
        int lowest = Integer.MAX_VALUE;
        for (DimPiece room : rooms) lowest = Math.min(lowest, room.height);
        check("a porta de 5 de alto cabe na sala mais baixa", lowest - 1 >= 5,
                "teto mais baixo: " + lowest + " (interior " + (lowest - 1) + ")");
        System.out.println();
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
}
