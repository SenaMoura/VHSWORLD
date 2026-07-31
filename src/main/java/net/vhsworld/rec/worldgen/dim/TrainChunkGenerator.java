package net.vhsworld.rec.worldgen.dim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * A dimensao TRAIN: uma linha reta, e nada dos dois lados dela.
 *
 * "spawn em cima de um trilho de trem que segue infinitamente, enquanto o jogador anda
 * sobre essa linha reta tem a chance dele achar safe spots". E a dimensao mais simples
 * de desenhar das nove e a mais dificil de estar dentro: nao ha escolha nenhuma a fazer.
 * Voce anda para frente ou para tras, e as duas direcoes sao iguais.
 *
 * ⚠️ A LINHA E UMA FATIA DE 4 BLOCOS, e nao uma peca comprida. Medi o `rails.schem`: o
 * dormente esta em x=1,2 e volta em x=5,6, x=9,10, x=13,14 — periodo 4 — e o trilho de
 * bigorna em y=1 e `laje bigorna bigorna laje`, o mesmo periodo. Guardar os 16 blocos
 * inteiros nao daria nada de novo e obrigaria todo pedaco de linha a ter multiplo de 16.
 *
 * O PISO TEM BURACO, e isso e da construcao do Pedro, nao defeito meu: os dormentes
 * deixam vao de 2 em 2, e o unico caminho continuo em cima do vazio sao os dois trilhos
 * — 2 blocos de largura, laje e bigorna alternando. Andar na linha e literalmente andar
 * na LINHA. E por isso que os safe spots importam: eles sao o unico lugar em que da para
 * parar de prestar atencao.
 */
public class TrainChunkGenerator extends StampChunkGenerator {

    public static final Codec<TrainChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, TrainChunkGenerator::new));

    private static final int MIN_Y = 0;
    private static final int GEN_HEIGHT = 128;

    /** A base do estrado. O trilho fica em DECK_Y+1 e se anda em DECH_Y+2. */
    public static final int DECK_Y = 64;

    /** O periodo do modulo da via, medido no schematic. */
    private static final int MODULE = 4;

    /**
     * De quanto em quanto se SORTEIA um safe spot — nao de quanto em quanto ele aparece.
     *
     * 96 com 45% de chance da um a cada 210 blocos em media, e o intervalo e desigual:
     * as vezes vem dois quase juntos e as vezes se anda 500 blocos sem nenhum. Periodo
     * fixo viraria estacao de metro, e ai a linha teria ritmo — e ritmo e informacao.
     */
    private static final int SAFE_PERIOD = 96;
    private static final double SAFE_CHANCE = 0.45D;

    /**
     * A largura do estrado, do schematic: z=0..11.
     *
     * Publica porque o TrackDoor varre esta faixa atras da fileira de trilho em que a
     * porta de saida vai nascer.
     */
    public static final int DECK_WIDE = 12;

    private static final int SPAWN_SPREAD = 512;

    // ------------------------------------------------------------------ o paredao
    //
    // "colocar ao redor dos trilhos a uma distancia que o player nn consiga alcancar
    // chunks altas de pedra e pedregulho que vao ser geradas infinitamentes tbm".
    //
    // A parte que decide o desenho e "que o player nn consiga alcancar": o paredao nao
    // e cenario de fundo, e o LIMITE. Enquanto houver vazio intransponivel entre ele e
    // a linha, a unica coisa que se pode fazer nesta dimensao continua sendo andar para
    // frente ou para tras — e e disso que ela vive. Um paredao alcancavel viraria uma
    // segunda opcao, e a TRAIN deixaria de ser a dimensao sem escolha.

    /**
     * Quantos blocos de vazio entre a borda do estrado e a primeira pedra possivel.
     *
     * ⚠️ 24, e o numero tem que ser conferido contra os SAFE SPOTS e nao contra o
     * estrado. Eles avancam 4 para fora dos dois lados (peca de 5x5, encostada em z=10
     * ao sul e em z=-4 ao norte), entao o vao real de quem estiver na ponta de um safe
     * spot e 20 — ainda mais que o dobro dos ~4,5 blocos de um pulo de corrida. Medir
     * pelo estrado daria 24 aqui e 20 la, e o pior lugar para descobrir isso e no ar.
     */
    private static final int CLIFF_GAP = 24;

    /** O quanto uma casa pode recuar alem do vao minimo, para a borda ficar irregular. */
    private static final int CLIFF_JITTER = 12;

    /**
     * O lado da casa do paredao.
     *
     * ⚠️ E ele que faz a palavra "CHUNKS" do pedido virar desenho. Sorteando altura por
     * BLOCO sairia terreno de ruido, que e morro; sorteando por casa de 12 e mantendo o
     * topo chapado dentro dela, sai o que o Pedro escreveu — blocos gigantes de pedra
     * empilhados, com quina viva e sombra dura. Alem disso e o que deixa a borda de
     * dentro em degraus retos em vez de serrilhada.
     */
    private static final int CLIFF_CELL = 12;

    /** O teto do paredao: da altura do estrado (64) ate quase o teto do mundo. */
    private static final int CLIFF_TOP_MIN = 70;
    private static final int CLIFF_TOP_RANGE = 56;

    /** O grao do salpico de pedregulho: cubinhos de 2, nao bloco a bloco. */
    private static final int GRAIN = 1;   // deslocamento: 1 = casas de 2x2x2

    private static final net.minecraft.world.level.block.state.BlockState STONE =
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
    private static final net.minecraft.world.level.block.state.BlockState COBBLE =
            net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState();

    private volatile DimPiece track;
    private volatile DimPiece safe;
    private volatile boolean looked;

    public TrainChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    private void look() {
        if (looked) return;
        synchronized (this) {
            if (looked) return;
            PieceSet set = PieceSet.get("train");
            track = set.byName("track");
            safe = set.byName("safe");
            looked = true;
        }
    }

    // ------------------------------------------------------------------ o carimbo
    @Override
    protected void carve(Brush brush) {
        look();
        if (track == null) return;

        // Um modulo por 4 blocos de X. Comeca um antes do chunk porque o modulo alinhado
        // em x=-4 ainda cobre x=-1..-4 e pode encostar aqui pela borda.
        int first = Math.floorDiv(brush.x0, MODULE) * MODULE;
        for (int x = first; x <= brush.x1; x += MODULE) {
            brush.stamp(new Placement(track, 0, x, DECK_Y, 0));
        }
        safeSpots(brush);
        cliffs(brush);
    }

    /**
     * Os paredoes dos dois lados, do fundo do mundo ate a altura sorteada da casa.
     *
     * O afastamento e medido em `away`, que e a distancia ate a borda do estrado do lado
     * em que se esta — e negativa ou zero em cima do proprio estrado, o que ja descarta
     * a faixa da linha sem precisar de um teste separado para ela.
     *
     * MACICO DESDE y=0, e nao uma placa flutuante. O jogador esta a 24 blocos e a 6 de
     * altura do pe do paredao: dali se ve para baixo, e um paredao que acabasse no ar
     * mostraria a propria espessura e viraria cenario. Custa o mesmo que terreno normal
     * custa — a secao macica de pedra e um indice so na paleta do chunk.
     */
    private void cliffs(Brush brush) {
        for (int z = brush.z0; z <= brush.z1; z++) {
            int away = z < 0 ? -z : z - (DECK_WIDE - 1);
            if (away < CLIFF_GAP) continue;
            int cellZ = Math.floorDiv(z, CLIFF_CELL);

            for (int x = brush.x0; x <= brush.x1; x++) {
                int cellX = Math.floorDiv(x, CLIFF_CELL);
                if (away < CLIFF_GAP + DimHash.pick(seed(), cellX, cellZ, 31L, CLIFF_JITTER + 1)) {
                    continue;
                }
                int top = CLIFF_TOP_MIN + DimHash.pick(seed(), cellX, cellZ, 32L, CLIFF_TOP_RANGE);
                // A casa tem uma pedra dominante e a outra entra salpicada: pedra pura de
                // ponta a ponta vira uma superficie chapada de tao repetida, e o pedregulho
                // sozinho vira caverna. Um em cada cinco cubinhos troca.
                boolean cobbleCell = DimHash.pick(seed(), cellX, cellZ, 33L, 2) == 0;
                for (int y = minY(); y <= top; y++) {
                    boolean flip = DimHash.pick(seed(), x >> GRAIN, z >> GRAIN,
                            (y >> GRAIN) * 31L + 34L, 5) == 0;
                    brush.set(x, y, z, cobbleCell != flip ? COBBLE : STONE);
                }
            }
        }
    }

    /**
     * Os safe spots, encostados na linha.
     *
     * O topo deles cai em DECK_Y+1, o mesmo Y do trilho — medi no schematic: a peca tem
     * pe em y=0 e a plataforma inteira em y=1. Entao se sai do trilho para o safe spot
     * andando, sem pulo. Se ele nascesse um bloco acima, cada safe spot seria um degrau
     * a subir com o vazio embaixo, que e o contrario do que ele e para ser.
     *
     * Ele alterna de lado (norte da via / sul da via) pelo sorteio: sempre do mesmo lado,
     * o jogador aprenderia a andar olhando so para um lado.
     */
    private void safeSpots(Brush brush) {
        if (safe == null) return;
        int g0 = Math.floorDiv(brush.x0 - SAFE_PERIOD, SAFE_PERIOD);
        int g1 = Math.floorDiv(brush.x1, SAFE_PERIOD);
        for (int g = g0; g <= g1; g++) {
            if (DimHash.frac(seed(), g, 0, 21L) >= SAFE_CHANCE) continue;
            int x = g * SAFE_PERIOD + DimHash.pick(seed(), g, 0, 22L, SAFE_PERIOD - safe.width);
            boolean south = DimHash.pick(seed(), g, 0, 23L, 2) == 0;
            // Ao sul encosta na borda do estrado (z=10); ao norte, no z=0 dele.
            int z = south ? DECK_WIDE - 2 : -(safe.length - 1);
            Placement placement = new Placement(safe, 0, x, DECK_Y, z);
            if (brush.touches(placement)) brush.stamp(placement);
        }
    }

    // ------------------------------------------------------------------ o spawn
    /**
     * Em cima do trilho, num ponto sorteado da linha — e alinhado ao modulo.
     *
     * Alinhado porque o trilho e continuo mas o ESTRADO nao e: caindo num x em que o
     * dormente falta, o jogador nasceria dentro de um vao com o vazio logo abaixo.
     */
    @Override
    public BlockPos dimensionSpawn() {
        java.util.Random dice = new java.util.Random();
        int x = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1) * MODULE;
        // z=0 e a fileira do trilho do norte. O do sul esta em z=8..9.
        return new BlockPos(x + 1, DECK_Y + 2, 0);
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
        return "TRAIN";
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return DECK_Y + 2;
    }

    // ------------------------------------------------------------------ a saida
    @Override
    public String dimensionId() {
        return "train";
    }

    /**
     * Na propria linha.
     *
     * ⚠️ O `rz` E IGNORADO DE PROPOSITO. A TRAIN e uma reta que so existe em z proximo de
     * zero; devolver um ponto por regiao no eixo Z poria a sala a duzentos blocos da
     * linha, boiando no vazio, e ninguem a alcancaria nunca. Aqui "regiao" so faz sentido
     * ao longo do trilho.
     */
    @Override
    public BlockPos exitAnchor(int rx, int rz) {
        return new BlockPos(ExitSite.cellInRegion(rx, MODULE) * MODULE + 1, DECK_Y + 2, 0);
    }
}
