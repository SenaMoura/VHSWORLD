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
 * A dimensao ESCRITORIO: um andar de baias, e do lado de fora so predio e neblina.
 *
 * "Um local com diversos escritorios e quando vc observar a vista havera uma neblina
 * densa e diversos predios altos de pedra ao fundo, deve haver corredores com visao ampla
 * pros predios e variacoes de salas."
 *
 * ============================ A DECISAO DE FORMA ============================
 *
 * ⚠️ O PEDIDO EXIGE UM "FORA", e e isso que decide toda a geometria. "Observar a vista",
 * "predios ao fundo", "corredor com visao ampla" — nada disso existe se o escritorio for
 * um chao infinito, que e como as outras dimensoes de interior do mod sao feitas. Um plano
 * sem fim nao tem janela: nao ha do lado de la.
 *
 * Entao aqui o infinito e feito de TORRES SOLTAS. Cada casa da grade e uma torre de 32x32
 * com 16 blocos de vazio em volta, e o jogador esta num andar la dentro. A vista pela
 * janela e a torre vizinha, e a de tras dela, ate a neblina comer. As torres nao sao
 * cenario pintado no fundo: sao as casas vizinhas da mesma grade, com o mesmo escritorio
 * dentro. Andando, chega-se nelas.
 *
 * ⚠️ E POR ISSO QUE EXISTE A PASSARELA. Torre solta sobre o vazio e uma dimensao de uma
 * sala so — sem ligacao, o jogador nasce num andar e nao ha para onde ir. A passarela e
 * um tubo fechado de 4 de largura atravessando os 16 do vao, e ela e VIDRO dos dois lados
 * de proposito: e o unico lugar da dimensao em que se esta suspenso sobre a queda com o
 * vazio visivel embaixo dos pes.
 *
 * ============================ AS "VARIACOES DE SALAS" ============================
 *
 * O andar e um corredor em cruz (que e o que tem a "visao ampla": ele acaba nas quatro
 * janelas) e quatro quadrantes. Cada quadrante sorteia o que e:
 *
 *   BAIAS   — o cubiculo da foto: divisorias baixas de 2 blocos numa malha, sem porta.
 *   SALA    — um gabinete fechado, com parede ate o forro e uma porta so.
 *   VAZIO   — o andar que ninguem ocupou: so o carpete.
 *
 * Sao tres por quadrante e quatro quadrantes por torre, o que da 81 plantas de andar por
 * torre e nenhuma repetida na vizinha. Nao e variedade por variedade: num predio em que
 * todo andar e igual, o jogador para de olhar depois do terceiro.
 *
 * ⚠️ O FORRO E DE `recmod:white_light`, o mesmo bloco que a GRASSROOMS usa. A foto do
 * Pedro e um escritorio de forro modular com luminaria embutida, e e ele que da a
 * iluminacao chapada e sem sombra dessas fotos — nao ha ceu nesta dimensao e nao ha uma
 * unica tocha. Ele entra num xadrez, e nao no forro inteiro: forro todo aceso apaga a
 * propria malha do forro e o teto vira uma chapa branca.
 */
public class EscritorioChunkGenerator extends StampChunkGenerator {

    public static final Codec<EscritorioChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, EscritorioChunkGenerator::new));

    private static final int MIN_Y = 0;
    private static final int GEN_HEIGHT = 192;

    /**
     * O andar em que se anda.
     *
     * ⚠️ ALTO DE PROPOSITO, e nao no meio por gosto: com 120 abaixo e 68 acima, a torre
     * vizinha sobe alem do topo da janela E desce alem da soleira dela. Se o andar
     * estivesse perto do fim da torre, dava para ver o fim do predio de fora — e um
     * predio com fim visivel deixa de ser "predio alto ao fundo" e vira uma caixa.
     */
    public static final int FLOOR_Y = 120;

    /** Pe-direito de escritorio: 4 blocos livres e o forro no quinto. */
    private static final int CEIL_H = 5;

    /** A grade. A torre e 32x32 e sobra o vao entre uma e outra. */
    private static final int CELL = 48;
    private static final int TOWER = 32;

    /** A largura do corredor em cruz, e tambem a da passarela e a da janela. */
    private static final int HALL_W = 4;

    /** A chance de haver passarela entre duas torres vizinhas. */
    private static final double BRIDGE_CHANCE = 0.60D;

    /** As tres plantas de quadrante. Ver o comentario da classe. */
    private static final int QUAD_CUBICLES = 0, QUAD_ROOM = 1, QUAD_EMPTY = 2;

    /** O passo da malha de baias: 6 de baia e 1 de divisoria. */
    private static final int DESK_STEP = 7;

    private static final int SPAWN_SPREAD = 256;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    /** O carpete cinza do andar, e a casca da torre. */
    private static final BlockState SLAB = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
    private static final BlockState SHELL = Blocks.STONE_BRICKS.defaultBlockState();
    /** A divisoria da baia e a parede do gabinete. */
    private static final BlockState PARTITION = Blocks.GRAY_CONCRETE.defaultBlockState();
    private static final BlockState DESK = Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
    private static final BlockState WINDOW = Blocks.GLASS.defaultBlockState();
    private static final BlockState CEIL = Blocks.SMOOTH_STONE.defaultBlockState();

    private static volatile BlockState panel;

    /**
     * A luminaria do forro.
     *
     * Buscada tarde pelo mesmo motivo da GRASSROOMS: e um bloco NOSSO, e o gerador nasce
     * do codec durante os eventos de registro — um `static final` aqui estoura com
     * "Registry Object not present" no carregamento do jogo.
     */
    private static BlockState panel() {
        BlockState ready = panel;
        if (ready != null) return ready;
        ready = net.vhsworld.rec.init.ModBlocks.WHITE_LIGHT.get().defaultBlockState();
        panel = ready;
        return ready;
    }

    public EscritorioChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    // ------------------------------------------------------------------ a grade
    /** O canto de menor coordenada da torre desta casa: ela e centrada na casa. */
    private static int towerX(int cx) {
        return cx * CELL + (CELL - TOWER) / 2;
    }

    private static int towerZ(int cz) {
        return cz * CELL + (CELL - TOWER) / 2;
    }

    /** O primeiro bloco do corredor, no eixo em que ele corre. */
    private static int hallX(int cx) {
        return towerX(cx) + (TOWER - HALL_W) / 2;
    }

    private static int hallZ(int cz) {
        return towerZ(cz) + (TOWER - HALL_W) / 2;
    }

    private boolean bridge(int cx, int cz, boolean westward) {
        if (bridgeRaw(cx, cz, westward)) return true;
        // A trava contra a torre isolada. Aqui ela nao e conforto, e a diferenca entre
        // uma dimensao e uma sala: sem passarela nenhuma, so se sai de uma torre caindo.
        if (!westward) return false;
        return !bridgeRaw(cx, cz, false)
                && !bridgeRaw(cx + 1, cz, true)
                && !bridgeRaw(cx, cz + 1, false);
    }

    private boolean bridgeRaw(int cx, int cz, boolean westward) {
        int nx = westward ? cx - 1 : cx;
        int nz = westward ? cz : cz - 1;
        return DimHash.edge(seed(), nx, nz, cx, cz, westward ? 71L : 72L, BRIDGE_CHANCE);
    }

    // ------------------------------------------------------------------ o carimbo
    @Override
    protected void carve(Brush brush) {
        int cx0 = Math.floorDiv(brush.x0, CELL), cx1 = Math.floorDiv(brush.x1, CELL);
        int cz0 = Math.floorDiv(brush.z0, CELL), cz1 = Math.floorDiv(brush.z1, CELL);

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                tower(brush, cx, cz);
                floor(brush, cx, cz);
            }
        }
        // As passarelas depois de TODAS as torres: ela fura a casca das duas pontas, e
        // uma torre carimbada depois a fecharia de novo.
        for (int cx = cx0; cx <= cx1 + 1; cx++) {
            for (int cz = cz0; cz <= cz1 + 1; cz++) {
                bridges(brush, cx, cz);
            }
        }
    }

    /**
     * A casca da torre: as quatro paredes, do fundo do mundo ao teto.
     *
     * OCA POR DENTRO, e nao macica. Um predio de 32x32x192 cheio seriam 196 mil blocos
     * por torre para tapar um miolo que ninguem alcança — o unico andar que existe e o
     * do jogador. O que a casca precisa entregar e a SILHUETA vista de fora, e para isso
     * a parede de um bloco basta.
     *
     * A janela e uma faixa de vidro na altura dos olhos, e SO nas quatro pontas do
     * corredor. Envidracar a torre inteira daria um aquario e mataria a frase do pedido
     * — "corredores com visao ampla pros predios" quer dizer que a vista e o premio de
     * chegar ao fim do corredor, e nao o papel de parede do andar todo.
     */
    private void tower(Brush brush, int cx, int cz) {
        int bx = towerX(cx), bz = towerZ(cz);
        int x0 = Math.max(bx, brush.x0), x1 = Math.min(bx + TOWER - 1, brush.x1);
        int z0 = Math.max(bz, brush.z0), z1 = Math.min(bz + TOWER - 1, brush.z1);

        int glass0 = FLOOR_Y + 1, glass1 = FLOOR_Y + CEIL_H - 2;
        int ax = hallX(cx), az = hallZ(cz);

        for (int x = x0; x <= x1; x++) {
            boolean edgeX = x == bx || x == bx + TOWER - 1;
            for (int z = z0; z <= z1; z++) {
                boolean edgeZ = z == bz || z == bz + TOWER - 1;
                if (!edgeX && !edgeZ) continue;
                // A ponta do corredor: vidro na altura dos olhos, pedra no resto.
                boolean onHall = (edgeX && z >= az && z < az + HALL_W)
                        || (edgeZ && x >= ax && x < ax + HALL_W);
                for (int y = minY(); y < minY() + genHeight(); y++) {
                    boolean pane = onHall && y >= glass0 && y <= glass1;
                    brush.set(x, y, z, pane ? WINDOW : SHELL);
                }
            }
        }
    }

    /** O andar: laje, forro, corredor em cruz e os quatro quadrantes. */
    private void floor(Brush brush, int cx, int cz) {
        int bx = towerX(cx), bz = towerZ(cz);
        int roof = FLOOR_Y + CEIL_H;
        int x0 = Math.max(bx + 1, brush.x0), x1 = Math.min(bx + TOWER - 2, brush.x1);
        int z0 = Math.max(bz + 1, brush.z0), z1 = Math.min(bz + TOWER - 2, brush.z1);

        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                brush.set(x, FLOOR_Y, z, SLAB);
                brush.column(x, FLOOR_Y + 1, roof - 1, z, AIR);
                // O xadrez do forro: uma luminaria a cada duas placas nos dois eixos.
                boolean lit = (Math.floorDiv(x - bx, 2) + Math.floorDiv(z - bz, 2)) % 2 == 0;
                brush.set(x, roof, z, lit ? panel() : CEIL);
            }
        }
        quadrants(brush, cx, cz);
    }

    /**
     * Os quatro quadrantes, cada um com a sua planta sorteada.
     *
     * O quadrante e o retangulo entre a parede da torre e o corredor. Ele e passado ja
     * recortado (`qx0..qx1`, `qz0..qz1`) porque as quatro chamadas so diferem nisso — e
     * porque as contas de canto sao o lugar em que um erro de um bloco vira uma baia
     * atravessada no corredor.
     */
    private void quadrants(Brush brush, int cx, int cz) {
        int bx = towerX(cx), bz = towerZ(cz);
        int ax = hallX(cx), az = hallZ(cz);
        int inner0X = bx + 1, inner1X = bx + TOWER - 2;
        int inner0Z = bz + 1, inner1Z = bz + TOWER - 2;

        // O quinto e o sexto numero sao as duas bordas que dao PARA O CORREDOR, e mudam
        // de quadrante para quadrante: o do noroeste encosta no corredor pelo lado de
        // maior X e maior Z, o do sudeste pelo de menor. Passar isso pronto e o que
        // impede o erro classico daqui — a parede do gabinete construida na parede da
        // torre, que fecha o quadrante inteiro e nao deixa entrada nenhuma.
        quadrant(brush, cx, cz, 0, inner0X, ax - 1, inner0Z, az - 1, ax - 1, az - 1);
        quadrant(brush, cx, cz, 1, ax + HALL_W, inner1X, inner0Z, az - 1, ax + HALL_W, az - 1);
        quadrant(brush, cx, cz, 2, inner0X, ax - 1, az + HALL_W, inner1Z, ax - 1, az + HALL_W);
        quadrant(brush, cx, cz, 3, ax + HALL_W, inner1X, az + HALL_W, inner1Z, ax + HALL_W, az + HALL_W);
    }

    private void quadrant(Brush brush, int cx, int cz, int index,
                          int qx0, int qx1, int qz0, int qz1, int wallX, int wallZ) {
        if (qx1 < qx0 || qz1 < qz0) return;
        int kind = DimHash.pick(seed(), cx * 4 + index, cz, 73L, 3);
        if (kind == QUAD_EMPTY) return;

        int x0 = Math.max(qx0, brush.x0), x1 = Math.min(qx1, brush.x1);
        int z0 = Math.max(qz0, brush.z0), z1 = Math.min(qz1, brush.z1);
        int roof = FLOOR_Y + CEIL_H;

        if (kind == QUAD_ROOM) {
            // O gabinete: parede ate o forro nas duas bordas que dao para o corredor, com
            // uma porta de 2x2 no meio de cada uma. As outras duas bordas ja sao a casca
            // da torre — nao ha o que fechar ali, e fechar de novo engrossaria a parede.
            int doorZ = (qz0 + qz1) / 2;
            int doorX = (qx0 + qx1) / 2;
            if (wallX >= brush.x0 && wallX <= brush.x1) {
                for (int z = z0; z <= z1; z++) {
                    boolean gap = z >= doorZ && z <= doorZ + 1;
                    for (int y = FLOOR_Y + 1; y < roof; y++) {
                        if (gap && y <= FLOOR_Y + 2) continue;
                        brush.set(wallX, y, z, PARTITION);
                    }
                }
            }
            if (wallZ >= brush.z0 && wallZ <= brush.z1) {
                for (int x = x0; x <= x1; x++) {
                    boolean gap = x >= doorX && x <= doorX + 1;
                    for (int y = FLOOR_Y + 1; y < roof; y++) {
                        if (gap && y <= FLOOR_Y + 2) continue;
                        brush.set(x, y, wallZ, PARTITION);
                    }
                }
            }
            return;
        }

        // As baias: divisorias de 2 blocos numa malha de 7, e uma bancada encostada nelas.
        // Sem porta e sem teto — o cubiculo da foto do Pedro nao fecha, ele so impede que
        // voce veja o vizinho sentado. E o que faz o andar parecer ocupado e vazio ao
        // mesmo tempo, que e o assunto da dimensao.
        for (int x = x0; x <= x1; x++) {
            boolean spineX = Math.floorMod(x - qx0, DESK_STEP) == 0;
            for (int z = z0; z <= z1; z++) {
                boolean spineZ = Math.floorMod(z - qz0, DESK_STEP) == 0;
                if (!spineX && !spineZ) continue;
                // O cruzamento das duas divisorias fica ABERTO: senao cada baia vira uma
                // caixa fechada de 6x6 e nao ha como entrar nela.
                if (spineX && spineZ) continue;
                brush.column(x, FLOOR_Y + 1, FLOOR_Y + 2, z, PARTITION);
                brush.set(x, FLOOR_Y + 3, z, DESK);
            }
        }
    }

    /**
     * As passarelas: um tubo de 4 de largo atravessando o vao entre duas torres.
     *
     * O piso e a casca; as duas laterais e o teto sao VIDRO. Nao e enfeite — e o unico
     * ponto da dimensao em que se ve o vazio embaixo dos pes e as torres de perfil, e e
     * ele que transforma "andar entre salas" em "atravessar".
     *
     * O tunel entra 1 bloco em cada torre para furar a casca (que tem 1 de espessura) e
     * emendar no corredor de dentro.
     */
    private void bridges(Brush brush, int cx, int cz) {
        if (bridge(cx, cz, true)) {
            int from = towerX(cx - 1) + TOWER - 1;
            int to = towerX(cx);
            span(brush, from, to, hallZ(cz), hallZ(cz) + HALL_W - 1, true);
        }
        if (bridge(cx, cz, false)) {
            int from = towerZ(cz - 1) + TOWER - 1;
            int to = towerZ(cz);
            span(brush, from, to, hallX(cx), hallX(cx) + HALL_W - 1, false);
        }
    }

    /** Um trecho de passarela, de `from` a `to` no eixo dela e de `a0` a `a1` na largura. */
    private void span(Brush brush, int from, int to, int a0, int a1, boolean alongX) {
        int roof = FLOOR_Y + CEIL_H - 1;
        for (int t = from; t <= to; t++) {
            for (int a = a0; a <= a1; a++) {
                int x = alongX ? t : a;
                int z = alongX ? a : t;
                brush.set(x, FLOOR_Y, z, SLAB);
                brush.column(x, FLOOR_Y + 1, roof - 1, z, AIR);
                brush.set(x, roof, z, WINDOW);
                // As laterais: so nas duas bordas da largura.
                if (a != a0 && a != a1) continue;
                brush.column(x, FLOOR_Y + 1, roof - 1, z, WINDOW);
            }
        }
    }

    // ------------------------------------------------------------------ o spawn
    /**
     * No cruzamento do corredor de uma torre sorteada.
     *
     * O cruzamento e o unico ponto do andar que NENHUMA das plantas de quadrante pode
     * ocupar — os quatro quadrantes acabam antes dele por construcao. Todo o resto do
     * andar depende do sorteio, e nascer dentro de uma divisoria de baia seria nascer
     * dentro de um bloco.
     */
    @Override
    public BlockPos dimensionSpawn() {
        java.util.Random dice = new java.util.Random();
        int cx = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
        int cz = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
        return new BlockPos(hallX(cx) + HALL_W / 2, FLOOR_Y + 1, hallZ(cz) + HALL_W / 2);
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
        return "ESCRITORIO";
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return FLOOR_Y + 1;
    }

    // ------------------------------------------------------------------ a saida
    @Override
    public String dimensionId() {
        return "escritorio";
    }

    /** No meio da passarela, como o spawn. */
    @Override
    public BlockPos exitAnchor(int rx, int rz) {
        int cx = ExitSite.cellInRegion(rx, CELL);
        int cz = ExitSite.cellInRegion(rz, CELL);
        return new BlockPos(hallX(cx) + HALL_W / 2, FLOOR_Y + 1, hallZ(cz) + HALL_W / 2);
    }
}
