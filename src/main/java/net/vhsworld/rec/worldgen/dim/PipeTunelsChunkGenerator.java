package net.vhsworld.rec.worldgen.dim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * A dimensao PIPE TUNELS: tuneis com canos nas paredes.
 *
 * "Pipe tunels - tuneis com canos nas paredes".
 *
 * ⚠️ A INSTRUCAO E DE UMA LINHA E O ASSUNTO DELA ESTA NA SEGUNDA METADE. "Tunel" o mod
 * ja sabe fazer — a MAZE e a DATA sao corredor. O que esta dimensao acrescenta e o
 * CANO, e cano nao e enfeite de parede: e a prova de que o lugar foi construido por
 * alguem, para levar alguma coisa, de algum lugar para outro. Um corredor vazio e um
 * labirinto; um corredor com um cano que vem de tras de voce e vai para onde voce ainda
 * nao foi e uma INSTALACAO, e instalacao tem dono.
 *
 * Por isso o cano aqui e CONTINUO. Ele nao aparece em pedacos decorativos: ele corre o
 * comprimento inteiro do corredor, atravessa o cruzamento e segue. Foi a decisao mais
 * cara deste arquivo e a unica que nao da para trocar sem perder o pedido.
 *
 * As duas fotos do Pedro dividem o trabalho. A primeira e um tunel largo de concreto com
 * um feixe de canos GROSSOS e claros empilhados numa parede so, teto liso e luminaria
 * rasgando a distancia; a segunda e um corredor apertado com canos FINOS e enferrujados
 * dos dois lados, no meio da bagunca. As duas viram a mesma dimensao: o feixe grosso
 * numa parede, os finos na outra, e o corredor apertado.
 *
 * ============================ A GRADE ============================
 *
 * Corredor de 5 de largura por 5 de altura numa grade de 24. A conta que importa: 24 =
 * 5 de corredor + 19 de macico. O macico e GROSSO de proposito — num labirinto de
 * paredes finas o jogador ouve a si mesmo do outro lado e o lugar vira uma casa de
 * papelao. Aqui, entre dois corredores paralelos ha 19 blocos de pedra, e o silencio
 * entre eles e real.
 *
 * Nao ha teto do mundo nem chao do mundo: o macico e SOLIDO de cima a baixo e o corredor
 * e um vazio cavado nele. E o inverso da INSIDIOUS (que constroi salas sobre o vazio) e
 * e o que faz este lugar pesar — nao ha para onde cair, e nao ha por cima.
 */
public class PipeTunelsChunkGenerator extends StampChunkGenerator {

    public static final Codec<PipeTunelsChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, PipeTunelsChunkGenerator::new));

    private static final int MIN_Y = 0;
    private static final int GEN_HEIGHT = 48;

    /**
     * O piso do corredor.
     *
     * Ha 20 blocos de macico por baixo e 22 por cima. Nao e simetria por gosto: quem
     * cavar para qualquer lado tem que gastar um tempo desconfortavel antes de chegar ao
     * fim do mundo, e o numero foi escolhido para que o fim do mundo nao seja alcancavel
     * por acidente com uma picareta na mao.
     */
    private static final int FLOOR_Y = 20;

    /** Altura util do corredor, do piso ao teto (exclusivo). */
    private static final int HEIGHT = 5;

    /** Largura util do corredor. */
    private static final int WIDTH = 5;

    /** Lado da grade: um corredor a cada tanto, nos dois eixos. */
    private static final int CELL = 24;

    /**
     * Chance de um trecho de corredor entre dois cruzamentos existir.
     *
     * Nao e 1.0, e isso e o que impede a dimensao de ser um tabuleiro perfeito em que
     * toda direcao serve. Com 0.72 sobram becos e desvios sem que o grafo se parta —
     * abaixo de ~0.6 comecam a nascer bolsoes isolados, e um bolsao isolado num mundo
     * infinito e um jogador emparedado para sempre.
     */
    private static final double LINK_CHANCE = 0.72D;

    private static final long LINK_SALT = 5100L;

    /** Uma luminaria a cada tantos blocos ao longo do corredor. */
    private static final int LAMP_STEP = 11;

    private static final int SPAWN_SPREAD = 512;

    // ------------------------------------------------------------------ os blocos
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /** O concreto do macico. */
    private static final BlockState CONCRETE = Blocks.GRAY_CONCRETE.defaultBlockState();
    /** A mancha clara: reboco descascado. */
    private static final BlockState PALE = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
    /** A mancha escura: umidade. */
    private static final BlockState DAMP = Blocks.ANDESITE.defaultBlockState();
    private static final BlockState FLOOR = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState PUDDLE = Blocks.WATER.defaultBlockState();

    /**
     * O feixe grosso da primeira foto.
     *
     * Tronco despido de bétula: e a unica coisa do vanilla que da um cilindro claro,
     * liso e com ANEL nas pontas — e o anel e o que faz o olho ler "cano" e nao "viga".
     * O eixo dele e girado para o comprimento do corredor, o que so importa por causa
     * desse anel: deitado errado, o cano vira um poste emendado a cada bloco.
     */
    private static final BlockState BIG_PIPE = Blocks.STRIPPED_BIRCH_LOG.defaultBlockState();

    /** O cano fino e enferrujado da segunda foto. */
    private static final BlockState RUST_PIPE = Blocks.COPPER_BLOCK.defaultBlockState();
    /** A braçadeira que prende o cano fino na parede, de tantos em tantos blocos. */
    private static final BlockState CLAMP = Blocks.DEEPSLATE_TILES.defaultBlockState();

    /**
     * A luminaria.
     *
     * ⚠️ Buscada TARDE, e nao num `static final` como os blocos do vanilla: este bloco e
     * NOSSO, e quando esta classe carrega o registro do mod ainda nao rodou. Ver o
     * comentario do `ceiling()` na GRASSROOMS, que e o mesmo caso e explica por inteiro.
     */
    private static volatile BlockState lamp;

    private static BlockState lamp() {
        BlockState ready = lamp;
        if (ready != null) return ready;
        ready = net.vhsworld.rec.init.ModBlocks.WHITE_LIGHT.get().defaultBlockState();
        lamp = ready;
        return ready;
    }

    public PipeTunelsChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    // ------------------------------------------------------------------ a grade
    /**
     * O eixo de um corredor: o meio dele fica sempre no mesmo lugar de cada casa.
     *
     * Corredor de largura 5 => o eixo tem 2 de folga para cada lado.
     */
    static int axis(int cell) {
        return cell * CELL + CELL / 2;
    }

    /**
     * A casa da grade a que esta coordenada pertence.
     *
     * ⚠️ E `floorDiv(v, CELL)` e NAO `floorDiv(v + CELL/2, CELL)`. A segunda forma parece
     * certa — "arredonda para a casa mais proxima" — e esta errada exatamente onde mais
     * importa: em cima do eixo. Com axis(c) = 24c+12, ela devolve c+1 para o proprio
     * eixo da casa c, e ai o corredor procura o cruzamento da casa vizinha. O sintoma
     * seria a faixa central de todo corredor virar pedra macica, ou seja: a dimensao
     * inteira sem corredor nenhum.
     */
    static int cellOf(int v) {
        return Math.floorDiv(v, CELL);
    }

    /**
     * O trecho que sai do cruzamento (cx,cz) na direcao +X existe?
     *
     * Usa `DimHash.edge`, que devolve a MESMA resposta perguntada dos dois lados. Com um
     * hash comum, o cruzamento da esquerda poderia dizer "ha corredor" e o da direita
     * "nao ha", e quem carimbasse por ultimo ganharia — o que depende da ordem em que o
     * jogador carregou os chunks, ou seja: a parede apareceria e sumiria conforme o
     * caminho que ele fez para chegar ali.
     */
    private boolean linkX(int cx, int cz) {
        return DimHash.edge(seed(), cx, cz, cx + 1, cz, LINK_SALT, LINK_CHANCE);
    }

    private boolean linkZ(int cx, int cz) {
        return DimHash.edge(seed(), cx, cz, cx, cz + 1, LINK_SALT + 1, LINK_CHANCE);
    }

    // ------------------------------------------------------------------ o carimbo
    @Override
    protected void carve(Brush brush) {
        // 1. o macico. Tudo solido, e depois se cava.
        for (int x = brush.x0; x <= brush.x1; x++) {
            for (int z = brush.z0; z <= brush.z1; z++) {
                for (int y = minY(); y < minY() + genHeight(); y++) {
                    brush.set(x, y, z, wall(x, y, z));
                }
            }
        }

        // 2. os corredores.
        for (int x = brush.x0; x <= brush.x1; x++) {
            for (int z = brush.z0; z <= brush.z1; z++) {
                if (!open(x, z)) continue;
                brush.set(x, FLOOR_Y, z, floorAt(x, z));
                brush.column(x, FLOOR_Y + 1, FLOOR_Y + HEIGHT, z, AIR);
            }
        }

        // 3. o que so faz sentido depois de o vao existir.
        for (int x = brush.x0; x <= brush.x1; x++) {
            for (int z = brush.z0; z <= brush.z1; z++) {
                if (!open(x, z)) continue;
                pipes(brush, x, z);
                lamps(brush, x, z);
            }
        }
    }

    /**
     * Este ponto e vao de corredor?
     *
     * Um ponto e vao se estiver na faixa de um corredor que EXISTE. O cruzamento sempre
     * existe (e a casa em si); os bracos dependem do sorteio da aresta.
     */
    boolean open(int x, int z) {
        int cx = cellOf(x), cz = cellOf(z);
        boolean onAxisX = Math.abs(x - axis(cx)) <= WIDTH / 2;
        boolean onAxisZ = Math.abs(z - axis(cz)) <= WIDTH / 2;

        if (onAxisX && onAxisZ) return true;   // o cruzamento

        if (onAxisX) {
            // corre no eixo Z: depende do braco para o lado em que este z esta
            return z > axis(cz) ? linkZ(cx, cz) : linkZ(cx, cz - 1);
        }
        if (onAxisZ) {
            return x > axis(cx) ? linkX(cx, cz) : linkX(cx - 1, cz);
        }
        return false;
    }

    /**
     * De que e feito o macico neste ponto.
     *
     * Tres blocos misturados por ruido de hash, e a mistura e por BLOCO e nao por
     * mancha — ao contrario do chao da FLORESTA. E de proposito: parede de concreto
     * velho tem grao fino e sujeira pontual, e mancha grande aqui leria como camuflagem
     * de tanque. As proporcoes (78/14/8) foram escolhidas para o cinza dominar; assim
     * que o claro passa de ~20% a parede comeca a parecer xadrez.
     */
    private BlockState wall(int x, int y, int z) {
        double roll = DimHash.frac(seed(), x * 31 + y, z * 17 + y, 5200L);
        if (roll > 0.92D) return DAMP;
        if (roll > 0.78D) return PALE;
        return CONCRETE;
    }

    /** O piso, com as pocas rasas das duas fotos. */
    private BlockState floorAt(int x, int z) {
        return DimHash.frac(seed(), x, z, 5300L) < 0.07D ? PUDDLE : FLOOR;
    }

    // ------------------------------------------------------------------ os canos
    /**
     * Os canos, presos nas duas paredes do corredor.
     *
     * A parede da esquerda (o lado de menor coordenada) leva o FEIXE GROSSO da primeira
     * foto: tres canos empilhados, encostados na parede, na altura do peito. A da direita
     * leva os dois canos FINOS enferrujados da segunda, com braçadeira de tres em tres.
     *
     * ⚠️ O EIXO DO CILINDRO SEGUE O CORREDOR. Um tronco tem tres orientacoes, e a errada
     * poe o anel da tampa virado para quem anda — o cano vira uma pilha de latas. E por
     * isso que aqui se pergunta em que direcao este trecho corre antes de escolher o
     * estado do bloco, em vez de usar o `defaultBlockState()` e seguir a vida.
     */
    private void pipes(Brush brush, int x, int z) {
        int cx = cellOf(x), cz = cellOf(z);
        boolean runsZ = Math.abs(x - axis(cx)) <= WIDTH / 2;
        boolean runsX = Math.abs(z - axis(cz)) <= WIDTH / 2;
        if (!runsZ && !runsX) return;

        // No cruzamento os dois correm, e um so pode passar — senao os dois feixes se
        // atravessam e viram um no de madeira no meio do ar. Quem passa e o eixo Z,
        // sempre. A regra so precisa ser CONSTANTE: o que nao pode e o cano brotar e
        // sumir conforme o lado de que se chega ao cruzamento.
        Direction.Axis pipeAxis = runsZ ? Direction.Axis.Z : Direction.Axis.X;

        boolean alongZ = pipeAxis == Direction.Axis.Z;
        int near = alongZ ? axis(cx) - WIDTH / 2 : axis(cz) - WIDTH / 2;
        int far = alongZ ? axis(cx) + WIDTH / 2 : axis(cz) + WIDTH / 2;
        int across = alongZ ? x : z;
        int along = alongZ ? z : x;

        BlockState big = BIG_PIPE.setValue(RotatedPillarBlock.AXIS, pipeAxis);
        BlockState thin = RUST_PIPE;

        if (across == near) {
            // o feixe grosso: tres empilhados, do meio da parede para cima
            brush.set(x, FLOOR_Y + 2, z, big);
            brush.set(x, FLOOR_Y + 3, z, big);
            brush.set(x, FLOOR_Y + 4, z, big);
        } else if (across == far) {
            // os finos: um na altura do joelho, outro colado no teto
            brush.set(x, FLOOR_Y + 1, z, thin);
            brush.set(x, FLOOR_Y + HEIGHT - 1, z, thin);
            if (Math.floorMod(along, 3) == 0) {
                brush.set(x, FLOOR_Y + 2, z, CLAMP);
            }
        }
    }

    /**
     * A luminaria do teto.
     *
     * Uma a cada 11 blocos, no eixo do corredor. O intervalo e o assunto: com luz a cada
     * 5 o tunel fica uniformemente iluminado e deixa de ter fundo; com 11 a luz do
     * proximo poste morre antes de a do anterior alcancar, e o que se anda e de uma
     * poça de luz para a seguinte, atravessando escuro no meio. E o que as duas fotos
     * mostram, e e o unico jeito de uma dimensao ILUMINADA ainda assustar.
     *
     * O 11 tambem e primo com o 24 da grade de proposito: se fosse divisor, toda
     * luminaria cairia na mesma posicao relativa da casa e o corredor viraria um padrao
     * que o jogador decora.
     */
    private void lamps(Brush brush, int x, int z) {
        int cx = cellOf(x), cz = cellOf(z);
        boolean centerX = x == axis(cx);
        boolean centerZ = z == axis(cz);

        if (centerX && Math.floorMod(z, LAMP_STEP) == 0) {
            brush.set(x, FLOOR_Y + HEIGHT, z, lamp());
        } else if (centerZ && Math.floorMod(x, LAMP_STEP) == 0) {
            brush.set(x, FLOOR_Y + HEIGHT, z, lamp());
        }
    }

    // ------------------------------------------------------------------ o spawn
    /**
     * Num cruzamento, que e o unico ponto da grade que existe SEMPRE.
     *
     * Largar o jogador num ponto qualquer o poria dentro do macico metade das vezes, e
     * largar no meio de um braco o poria dentro da pedra sempre que aquele braco tivesse
     * sido sorteado como inexistente. O cruzamento e a unica garantia.
     */
    @Override
    public BlockPos dimensionSpawn() {
        java.util.Random dice = new java.util.Random();
        int cx = dice.nextInt(-SPAWN_SPREAD / CELL, SPAWN_SPREAD / CELL + 1);
        int cz = dice.nextInt(-SPAWN_SPREAD / CELL, SPAWN_SPREAD / CELL + 1);
        return new BlockPos(axis(cx), FLOOR_Y + 1, axis(cz));
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
        return "PIPE TUNELS";
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return FLOOR_Y + 1;
    }

    // ------------------------------------------------------------------ a saida
    @Override
    public String dimensionId() {
        return "pipe_tunels";
    }

    /** No cruzamento da casa fixa da regiao: o unico ponto que existe SEMPRE. */
    @Override
    public BlockPos exitAnchor(int rx, int rz) {
        return new BlockPos(axis(ExitSite.cellInRegion(rx, CELL)), FLOOR_Y + 1,
                axis(ExitSite.cellInRegion(rz, CELL)));
    }
}
