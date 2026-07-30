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

    /** A largura do estrado, do schematic: z=0..11. */
    private static final int DECK_WIDE = 12;

    private static final int SPAWN_SPREAD = 512;

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
}
