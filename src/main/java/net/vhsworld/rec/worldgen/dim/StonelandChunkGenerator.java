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
 * A dimensao STONELAND: o overworld, e so pedregulho.
 *
 * "um overworld mas feito totalmente de pedregulho a neblina deve ser cinza tbm".
 *
 * ⚠️ A INSTRUCAO E CURTA E TEM UMA ARMADILHA. "Totalmente de pedregulho" e facil; o que
 * carrega a dimensao e o "mas overworld". Se eu enchesse o mundo de pedregulho plano,
 * daria um deserto cinza — e um lugar chato nao assusta, so cansa. O que da medo aqui e o
 * RECONHECIMENTO: a silhueta e a de um mundo em que se sabe andar, com morro, encosta e
 * vale, e ela esta toda feita do material errado. Nao ha grama, nao ha arvore, nao ha
 * agua, nao ha minerio — e nao falta nada disso por economia, falta porque a falta e o
 * assunto. E o overworld depois que tiraram tudo dele menos a forma.
 *
 * Por isso o trabalho todo deste arquivo esta no RELEVO, e nao no bloco.
 *
 * ============================ O RELEVO ============================
 *
 * Ruido de valor em tres oitavas, interpolado com `smoothstep`. Nao e o ruido do jogo, e
 * nao e preguica: o `NoiseChunkGenerator` do vanilla so responde dentro do maquinario de
 * `NoiseGeneratorSettings` (densidade, ruido de aquifero, regras de superficie), que e um
 * datapack inteiro para produzir um morro de pedregulho. Tres oitavas de hash dao a mesma
 * silhueta em trinta linhas, sao deterministicas por construcao (ver `DimHash`) e nao
 * dependem de nenhum arquivo.
 *
 * As tres oitavas fazem coisas diferentes e vale saber qual e qual antes de mexer:
 *   96 blocos = a REGIAO. E ela que decide serra ou baixada, e e a unica que se percebe
 *               andando: o periodo e maior que a distancia de render com neblina.
 *   32 blocos = o MORRO. A forma que se ve inteira de uma vez.
 *   11 blocos = a QUEBRA da encosta, para o barranco nao ser uma rampa lisa.
 *
 * A soma e normalizada antes de virar altura, senao as amplitudes se somariam e os picos
 * estourariam o teto do mundo justamente nos lugares em que as tres concordam.
 */
public class StonelandChunkGenerator extends StampChunkGenerator {

    public static final Codec<StonelandChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, StonelandChunkGenerator::new));

    private static final int MIN_Y = 0;
    private static final int GEN_HEIGHT = 128;

    /** A altura media do chao. Os morros sobem daqui e os vales descem. */
    private static final int BASE_Y = 62;

    /** Quanto o relevo sobe ou desce alem da media, no maximo. */
    private static final int RELIEF = 34;

    // As tres oitavas: {periodo, peso}. Ver o comentario da classe.
    private static final int[] PERIODS = {96, 32, 11};
    private static final double[] WEIGHTS = {1.00D, 0.42D, 0.17D};

    /** Onde a fita pode largar o jogador. */
    private static final int SPAWN_SPREAD = 2048;

    /**
     * Pedregulho, e so ele.
     *
     * Nem um veio de pedra lisa, nem musgo, nem minerio. Foi tentador salpicar alguma
     * coisa para "quebrar a repeticao" — e seria trair o pedido: a repeticao E o efeito.
     * O que quebra a monotonia aqui e a sombra do relevo, que a luz ambiente baixa e a
     * neblina cinza ja dao de graca.
     */
    private static final BlockState GROUND = Blocks.COBBLESTONE.defaultBlockState();

    public StonelandChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    // ------------------------------------------------------------------ o ruido
    /**
     * Ruido de valor num periodo: sorteia um numero por canto da grade e interpola.
     *
     * ⚠️ O `smoothstep` (t*t*(3-2t)) nao e enfeite. Com interpolacao linear pura, a
     * derivada quebra em cada linha da grade e o terreno sai com vincos retos de 96 em 96
     * blocos — um quadriculado que o olho acha na hora e que denuncia a grade inteira.
     */
    private double noise(int x, int z, int period, long salt) {
        int gx = Math.floorDiv(x, period), gz = Math.floorDiv(z, period);
        double fx = (x - gx * (double) period) / period;
        double fz = (z - gz * (double) period) / period;
        fx = fx * fx * (3.0D - 2.0D * fx);
        fz = fz * fz * (3.0D - 2.0D * fz);

        double n00 = DimHash.frac(seed(), gx, gz, salt);
        double n10 = DimHash.frac(seed(), gx + 1, gz, salt);
        double n01 = DimHash.frac(seed(), gx, gz + 1, salt);
        double n11 = DimHash.frac(seed(), gx + 1, gz + 1, salt);

        double top = n00 + (n10 - n00) * fx;
        double bottom = n01 + (n11 - n01) * fx;
        return top + (bottom - top) * fz;
    }

    /** A altura do chao nesta coluna. */
    private int surface(int x, int z) {
        double sum = 0.0D, total = 0.0D;
        for (int i = 0; i < PERIODS.length; i++) {
            sum += WEIGHTS[i] * noise(x, z, PERIODS[i], 61L + i);
            total += WEIGHTS[i];
        }
        // De 0..1 para -1..1: sem isto o mundo inteiro ficaria ACIMA da media.
        double signed = (sum / total) * 2.0D - 1.0D;
        return BASE_Y + (int) Math.round(signed * RELIEF);
    }

    // ------------------------------------------------------------------ o carimbo
    @Override
    protected void carve(Brush brush) {
        for (int x = brush.x0; x <= brush.x1; x++) {
            for (int z = brush.z0; z <= brush.z1; z++) {
                brush.column(x, minY(), surface(x, z), z, GROUND);
            }
        }
    }

    // ------------------------------------------------------------------ o spawn
    /**
     * Em cima do chao, num ponto sorteado — e o chao aqui NAO tem altura fixa.
     *
     * ⚠️ E a primeira das nossas dimensoes em que isso acontece, e por isso a conta tem
     * que perguntar a altura em vez de saber. Todas as outras sao plataforma plana ou
     * peca carimbada num Y conhecido; aqui o spawn cai em cima de um morro ou dentro de
     * um vale, e devolver um Y fixo emparedaria o jogador na encosta metade das vezes.
     */
    @Override
    public BlockPos dimensionSpawn() {
        java.util.Random dice = new java.util.Random();
        int x = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
        int z = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
        return new BlockPos(x, surface(x, z) + 1, z);
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
        return "STONELAND";
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return surface(x, z) + 1;
    }

    // ------------------------------------------------------------------ a saida
    @Override
    public String dimensionId() {
        return "stoneland";
    }

    /** No meio da regiao, em cima do relevo — aqui todo ponto e chao. */
    @Override
    public BlockPos exitAnchor(int rx, int rz) {
        int x = rx * ExitSite.REGION + ExitSite.REGION / 2;
        int z = rz * ExitSite.REGION + ExitSite.REGION / 2;
        return new BlockPos(x, surface(x, z) + 1, z);
    }
}
