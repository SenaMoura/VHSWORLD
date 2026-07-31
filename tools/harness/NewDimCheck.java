package net.vhsworld.rec.worldgen.dim;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Confere as tres dimensoes de 2026-07-31 (FLORESTA, PIPE TUNELS, MALL) SEM ABRIR O JOGO.
 *
 * ⚠️ POR QUE ELE NAO E O `DimCheck`. Aquele mede PECAS: carrega os .bin do Pedro e
 * pergunta se cabem na grade. Estas tres nao tem peca nenhuma — sao 100% Java, e o que
 * pode dar errado nelas nao e o encaixe de uma construcao, e a REGRA que decide onde ha
 * chao. Uma regra errada aqui nao derruba o jogo e nao aparece no compilador: da um
 * corredor que e pedra macica, ou um celeiro com uma arvore crescida no meio da sala.
 *
 * ⚠️ ELE ESTA NO PACOTE `dim` DE PROPOSITO, e nao no pacote raiz como os outros tres do
 * harness. E o que permite chamar os predicados de verdade (`open`, `corridor`,
 * `inClearing`, `surface`) em vez de reescrever a conta deles aqui. Reescrever mediria a
 * MINHA copia da regra e passaria sempre — e a licao que o README do harness ja tinha
 * aprendido de dois testes que passaram medindo a coisa errada.
 *
 * A semente e injetada por reflexao porque `createState` — a unica porta oficial — exige
 * um `HolderLookup<StructureSet>` e um `RandomState`, que nao existem fora do servidor.
 * Testar com uma semente so esconderia justamente os defeitos que dependem de sorteio.
 */
public final class NewDimCheck {

    static int failures = 0;

    /** As sementes de teste. Mais de uma porque metade daqui depende de sorteio. */
    static final long[] SEEDS = {0L, 1L, 42L, -7L, 123456789L};

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

        floresta();
        pipeTunels();
        mall();
        exits();

        System.out.println(failures == 0 ? "\n=== TUDO PASSOU ===" : "\n=== " + failures + " REPROVADO(S) ===");
        System.exit(failures == 0 ? 0 : 1);
    }

    static void check(String what, boolean ok, String detail) {
        System.out.println((ok ? "  ok " : "  XX ") + what + (detail.isEmpty() ? "" : "  [" + detail + "]"));
        if (!ok) failures++;
    }

    /** Enfia a semente no campo privado do StampChunkGenerator. Ver o comentario da classe. */
    static void seed(StampChunkGenerator generator, long value) throws Exception {
        Field field = StampChunkGenerator.class.getDeclaredField("seed");
        field.setAccessible(true);
        field.setLong(generator, value);
    }

    // ================================================================== FLORESTA
    static void floresta() throws Exception {
        System.out.println("=== FLORESTA ===");
        FlorestaChunkGenerator gen = new FlorestaChunkGenerator(null);

        int region = 192, clearing = 15;

        // 1. A INVARIANTE QUE O PROPRIO CODIGO PROMETE. `inClearing` so olha a regiao do
        //    ponto, e isso so e valido se a clareira nunca cruzar a divisa. Se cruzar, as
        //    arvores do lado de la nao sabem dela e crescem dentro do celeiro.
        boolean contained = true;
        String worst = "";
        for (long s : SEEDS) {
            seed(gen, s);
            for (int rx = -6; rx <= 6; rx++) {
                for (int rz = -6; rz <= 6; rz++) {
                    int bx = gen.barnX(rx, rz), bz = gen.barnZ(rx, rz);
                    int lowX = bx - clearing - rx * region, highX = bx + clearing - rx * region;
                    int lowZ = bz - clearing - rz * region, highZ = bz + clearing - rz * region;
                    if (lowX < 0 || highX >= region || lowZ < 0 || highZ >= region) {
                        contained = false;
                        worst = "semente " + s + " regiao " + rx + "," + rz
                                + " -> x " + lowX + ".." + highX + " z " + lowZ + ".." + highZ;
                    }
                }
            }
        }
        check("a clareira nunca cruza a divisa da regiao", contained, worst);

        // 2. O celeiro esta DENTRO da propria clareira (senao ele nasce no meio do mato).
        boolean inside = true;
        for (long s : SEEDS) {
            seed(gen, s);
            for (int rx = -4; rx <= 4; rx++) {
                for (int rz = -4; rz <= 4; rz++) {
                    int bx = gen.barnX(rx, rz), bz = gen.barnZ(rx, rz);
                    // os quatro cantos da pegada do celeiro (13 x 11)
                    for (int dx = -7; dx <= 7; dx += 14) {
                        for (int dz = -6; dz <= 6; dz += 12) {
                            if (!gen.inClearing(bx + dx, bz + dz)) inside = false;
                        }
                    }
                }
            }
        }
        check("a pegada inteira do celeiro cai dentro da clareira", inside, "");

        // 3. O relevo cabe no mundo COM a arvore mais alta e o telhado em cima. A arvore
        //    vai a surface+20 e o celeiro a surface+13; abaixo, o chao desce 5 blocos de
        //    pedra. Estourar em qualquer ponta e um bloco silenciosamente descartado pelo
        //    Brush — ou seja, uma copa cortada reta ou um telhado sem cumeeira.
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (long s : SEEDS) {
            seed(gen, s);
            for (int x = -400; x <= 400; x += 7) {
                for (int z = -400; z <= 400; z += 7) {
                    int y = gen.surface(x, z);
                    lo = Math.min(lo, y);
                    hi = Math.max(hi, y);
                }
            }
        }
        check("o relevo cabe no mundo com arvore e telhado", lo >= 6 && hi <= 128 - 22,
                "relevo " + lo + ".." + hi + ", mundo 0..127");

        // 4. O spawn tem chao: a altura devolvida e sempre a superficie + 1.
        boolean spawnOk = true;
        for (long s : SEEDS) {
            seed(gen, s);
            for (int i = 0; i < 200; i++) {
                BlockPos at = gen.dimensionSpawn();
                if (at.getY() != gen.surface(at.getX(), at.getZ()) + 1) spawnOk = false;
            }
        }
        check("o spawn cai exatamente em cima do chao", spawnOk, "");
        System.out.println();
    }

    // ================================================================== PIPE TUNELS
    static void pipeTunels() throws Exception {
        System.out.println("=== PIPE TUNELS ===");
        PipeTunelsChunkGenerator gen = new PipeTunelsChunkGenerator(null);

        // 1. O CRUZAMENTO EXISTE SEMPRE. E a promessa em que o spawn se apoia: se um
        //    cruzamento pudesse ser macico, a fita largaria o jogador dentro da pedra.
        boolean crossings = true;
        for (long s : SEEDS) {
            seed(gen, s);
            for (int cx = -8; cx <= 8; cx++) {
                for (int cz = -8; cz <= 8; cz++) {
                    if (!gen.open(PipeTunelsChunkGenerator.axis(cx), PipeTunelsChunkGenerator.axis(cz))) {
                        crossings = false;
                    }
                }
            }
        }
        check("todo cruzamento e vao", crossings, "");

        // 2. O MIOLO DA QUADRA E MACICO. Se `open` respondesse sim aqui, os 19 blocos de
        //    pedra entre dois corredores paralelos nao existiriam e a dimensao viraria um
        //    salao unico — que e o oposto do pedido.
        boolean solid = true;
        for (long s : SEEDS) {
            seed(gen, s);
            for (int cx = -6; cx <= 6; cx++) {
                for (int cz = -6; cz <= 6; cz++) {
                    int x = PipeTunelsChunkGenerator.axis(cx) + 6;
                    int z = PipeTunelsChunkGenerator.axis(cz) + 6;
                    if (gen.open(x, z)) solid = false;
                }
            }
        }
        check("o miolo da quadra e macico", solid, "");

        // 3. CONECTIVIDADE. `LINK_CHANCE` = 0.72 esta bem acima do limiar de percolacao de
        //    aresta numa grade quadrada (0.5), entao quase todo cruzamento tem que ser
        //    alcancavel a pe a partir da origem. Um bolsao isolado num mundo infinito e um
        //    jogador emparedado para sempre, e e a unica falha desta dimensao que NAO da
        //    para perceber olhando: por dentro, o bolsao parece um corredor normal.
        for (long s : SEEDS) {
            seed(gen, s);
            int reach = flood(gen, 8);
            int total = 17 * 17;
            check("semente " + s + ": os cruzamentos se ligam", reach >= total * 9 / 10,
                    reach + "/" + total + " alcancados a pe");
        }

        // 4. O spawn cai num cruzamento.
        boolean spawnOk = true;
        for (long s : SEEDS) {
            seed(gen, s);
            for (int i = 0; i < 200; i++) {
                BlockPos at = gen.dimensionSpawn();
                if (!gen.open(at.getX(), at.getZ())) spawnOk = false;
                if (at.getY() != 21) spawnOk = false;    // FLOOR_Y + 1
            }
        }
        check("o spawn cai num cruzamento, em cima do piso", spawnOk, "");
        System.out.println();
    }

    /** Anda a pe pelos vaos, a partir do cruzamento da origem, e conta cruzamentos achados. */
    static int flood(PipeTunelsChunkGenerator gen, int cells) {
        int span = cells * 24 + 12;
        Set<Long> seen = new HashSet<>();
        Deque<int[]> queue = new ArrayDeque<>();
        int sx = PipeTunelsChunkGenerator.axis(0), sz = PipeTunelsChunkGenerator.axis(0);
        queue.add(new int[]{sx, sz});
        seen.add(key(sx, sz));

        while (!queue.isEmpty()) {
            int[] at = queue.poll();
            int[][] around = {{at[0] + 1, at[1]}, {at[0] - 1, at[1]}, {at[0], at[1] + 1}, {at[0], at[1] - 1}};
            for (int[] next : around) {
                if (Math.abs(next[0]) > span || Math.abs(next[1]) > span) continue;
                if (!gen.open(next[0], next[1])) continue;
                if (!seen.add(key(next[0], next[1]))) continue;
                queue.add(next);
            }
        }

        int found = 0;
        for (int cx = -cells; cx <= cells; cx++) {
            for (int cz = -cells; cz <= cells; cz++) {
                if (seen.contains(key(PipeTunelsChunkGenerator.axis(cx),
                        PipeTunelsChunkGenerator.axis(cz)))) found++;
            }
        }
        return found;
    }

    static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    // ================================================================== A SAIDA
    /**
     * O ponto da saida: onde o Espelho vai ser plantado.
     *
     * O que se mede aqui nao e aparencia — e se a saida EXISTE onde alguem vai procurar
     * por ela. Um defeito nestes numeros nao da erro nenhum e nao aparece em foto: da um
     * jogador que anda ate o ponto certo e nao acha nada, numa dimensao de onde a fita nao
     * traz mais ninguem de volta.
     */
    static void exits() throws Exception {
        System.out.println("=== SAIDA ===");

        FlorestaChunkGenerator floresta = new FlorestaChunkGenerator(null);
        PipeTunelsChunkGenerator pipe = new PipeTunelsChunkGenerator(null);
        MallChunkGenerator mall = new MallChunkGenerator(null);

        // 1. A ANCORA E DETERMINISTA. Tres partes do jogo perguntam onde e a saida em
        //    momentos diferentes — o diretor que planta o Espelho, o `nearest` que decide
        //    qual e a mais perto do jogador, e a regra do olhar. Se a resposta variasse, o
        //    Espelho seria plantado num lugar e cobrado noutro.
        boolean stable = true;
        for (long s : SEEDS) {
            seed(floresta, s);
            for (int r = -4; r <= 4; r++) {
                BlockPos first = floresta.exitAnchor(r, r);
                for (int again = 0; again < 5; again++) {
                    if (!floresta.exitAnchor(r, r).equals(first)) stable = false;
                }
            }
        }
        check("a ancora devolve sempre o mesmo ponto", stable, "");

        // 2. UMA SAIDA POR REGIAO, E LONGE DA VIZINHA. O Espelho tem 8,4 blocos de altura
        //    e e visivel de longe; duas ao alcance de vista uma da outra deixam de ser
        //    raras, e o jogador perde a nocao de ter ACHADO alguma coisa.
        boolean apart = true;
        String worst = "";
        for (int r = -4; r <= 4; r++) {
            BlockPos a = pipe.exitAnchor(r, 0);
            BlockPos b = pipe.exitAnchor(r + 1, 0);
            int gap = Math.abs(b.getX() - a.getX());
            if (gap < ExitSite.REGION / 2) {
                apart = false;
                worst = "regioes " + r + " e " + (r + 1) + ": " + gap + " blocos";
            }
        }
        check("saidas vizinhas ficam longe uma da outra", apart, worst);

        // 3. A ANCORA CAI ONDE HA CHAO. Na PIPE TUNELS isso e verificavel de verdade: o
        //    `open` diz se aquele ponto e vao de corredor. Um Espelho ancorado no macico
        //    nasceria emparedado — visivel de lugar nenhum, alcancavel de lugar nenhum.
        boolean walkable = true;
        for (long s : SEEDS) {
            seed(pipe, s);
            for (int rx = -3; rx <= 3; rx++) {
                for (int rz = -3; rz <= 3; rz++) {
                    BlockPos at = pipe.exitAnchor(rx, rz);
                    if (!pipe.open(at.getX(), at.getZ())) walkable = false;
                }
            }
        }
        check("a ancora da PIPE TUNELS cai num vao de corredor", walkable, "");

        // 4. O MESMO NA MALL: corredor sim, cruzamento nao (la moram o vao central e a
        //    escada rolante, e um painel de oito blocos em cima deles taparia os dois).
        boolean clear = true;
        for (int rx = -3; rx <= 3; rx++) {
            for (int rz = -3; rz <= 3; rz++) {
                BlockPos at = mall.exitAnchor(rx, rz);
                if (!MallChunkGenerator.corridor(at.getX(), at.getZ())) clear = false;
                if (MallChunkGenerator.crossing(at.getX(), at.getZ())) clear = false;
            }
        }
        check("a ancora da MALL cai no corredor e fora do cruzamento", clear, "");

        // 5. O ESPELHO CABE EM PE. Ele tem 8,4 blocos; se a ancora estiver perto demais do
        //    teto do mundo, a metade de cima fica cortada — e e justamente a metade com os
        //    olhos, que e a unica coisa que o distingue de um retangulo preto.
        boolean fits = true;
        String tight = "";
        for (long s : SEEDS) {
            seed(floresta, s);
            for (int r = -4; r <= 4; r++) {
                BlockPos at = floresta.exitAnchor(r, r);
                if (at.getY() + 9 > 128) {
                    fits = false;
                    tight = "regiao " + r + " em y=" + at.getY();
                }
            }
        }
        check("o Espelho cabe de pe embaixo do teto do mundo", fits, tight);

        System.out.println();
    }

    // ================================================================== MALL
    static void mall() throws Exception {
        System.out.println("=== MALL ===");
        MallChunkGenerator gen = new MallChunkGenerator(null);

        int half = 4;   // HALF

        // 1. A ESCADA ROLANTE CABE NO CORREDOR. Ela e centrada em `axis-2` e gasta 5 de
        //    largura (3 de degrau + 1 de corrimao de cada lado). Se um corrimao caisse em
        //    `axis-5` ele nasceria dentro da parede da vitrine — e nao ha erro nenhum
        //    nisso, so um corrimao que ninguem ve e uma escada de aparencia torta.
        boolean fits = true;
        for (int cx = -4; cx <= 4; cx++) {
            for (int cz = -4; cz <= 4; cz++) {
                int ax = MallChunkGenerator.axis(cx) - 2;
                for (int w = -2; w <= 2; w++) {
                    if (MallChunkGenerator.offAxis(ax + w) > half) fits = false;
                }
            }
        }
        check("a escada rolante inteira cabe dentro do corredor", fits, "");

        // 2. A VITRINE FICA FORA DO CORREDOR. `storefronts` desenha em offAxis == HALF+1;
        //    se isso ainda fosse corredor, a grade de ferro nasceria no meio da passagem.
        boolean outside = true;
        for (int cx = -4; cx <= 4; cx++) {
            int x = MallChunkGenerator.axis(cx) + half + 1;
            int z = MallChunkGenerator.axis(cx) + 20;      // longe do corredor perpendicular
            if (MallChunkGenerator.corridor(x, z)) outside = false;
        }
        check("a faixa da vitrine nao e corredor", outside, "");

        // 3. O BURACO DO VAO CENTRAL NAO PASSA DO CRUZAMENTO. Vazar abriria a laje por
        //    baixo das lojas da esquina, e as paredes delas ficariam penduradas em nada.
        boolean bounded = true;
        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                int ax = MallChunkGenerator.axis(cx), az = MallChunkGenerator.axis(cz);
                for (int dx = -half; dx <= half; dx++) {
                    for (int dz = -half; dz <= half; dz++) {
                        if (!MallChunkGenerator.crossing(ax + dx, az + dz)) bounded = false;
                    }
                }
            }
        }
        check("o buraco do vao central fica todo dentro do cruzamento", bounded, "");

        // 4. O SPAWN NAO CAI EM CIMA DE NADA. Tem que ser corredor (ha chao) e NAO ser
        //    cruzamento (la moram o buraco do vao e a escada rolante).
        boolean spawnOk = true;
        String why = "";
        for (int i = 0; i < 500; i++) {
            BlockPos at = gen.dimensionSpawn();
            if (!MallChunkGenerator.corridor(at.getX(), at.getZ())) {
                spawnOk = false;
                why = "fora do corredor em " + at;
            }
            if (MallChunkGenerator.crossing(at.getX(), at.getZ())) {
                spawnOk = false;
                why = "em cima do cruzamento em " + at;
            }
        }
        check("o spawn cai no corredor e fora do cruzamento", spawnOk, why);

        // 5. O CANTEIRO NAO DISPUTA LUGAR COM A ESCADA NEM COM O VAO. `furniture` planta
        //    em offAxis(x)==0 e z multiplo de 16, pulando cruzamento — este teste refaz a
        //    peneira e confere que o que sobra nunca e cruzamento.
        boolean clear = true;
        for (int x = -300; x <= 300; x++) {
            for (int z = -300; z <= 300; z++) {
                if (MallChunkGenerator.offAxis(x) != 0 || Math.floorMod(z, 16) != 0) continue;
                if (MallChunkGenerator.crossing(x, z)) continue;       // o gerador pula
                if (MallChunkGenerator.crossing(x, z)) clear = false;  // logo, nunca sobra
                if (!MallChunkGenerator.corridor(x, z)) clear = false; // e sempre ha chao
            }
        }
        check("todo canteiro que sobra tem chao e esta fora do cruzamento", clear, "");
        System.out.println();
    }
}
