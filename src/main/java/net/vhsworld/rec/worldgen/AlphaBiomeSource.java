package net.vhsworld.rec.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.stream.Stream;

/**
 * De onde vem o bioma no mundo alpha.
 *
 * POR QUE ISTO EXISTE: o jogo so calcula clima de verdade para gerador de ruido.
 * Para qualquer outro — o nosso — o ChunkMap monta o RandomState com
 * `NoiseGeneratorSettings.dummy()`, cujo roteador e uma pilha de zeros. O
 * amostrador de clima entao devolve sempre o mesmo ponto, e o multi_noise responde
 * sempre o MESMO bioma: o mundo inteiro sairia de um sabor so, com uma arvore so.
 * Nao da para consertar isso por dado; ou se escreve a fonte de biomas, ou o mundo
 * fica monotono.
 *
 * COMO FUNCIONA: do jeito antigo, e o antigo era simples — dois mapas de ruido
 * planos, um de temperatura e um de umidade, e uma tabela que cruza os dois. Nao ha
 * nada tridimensional, nada de "raridade", nada de ilhas de bioma desenhadas: o
 * deserto encosta na floresta porque ali a chuva acabou, e nao porque alguem
 * decidiu. Um terceiro ruido bem miudo suja as duas medidas, para a divisa nao sair
 * lisa como curva de nivel de mapa.
 *
 * A tabela le a temperatura PRIMEIRO e a chuva depois. E o que impede o absurdo de
 * uma selva encostar na tundra: no frio, chuva nenhuma leva a selva — leva a taiga.
 */
public class AlphaBiomeSource extends BiomeSource {

    public static final Codec<AlphaBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    RegistryOps.retrieveGetter(Registries.BIOME)
            ).apply(instance, AlphaBiomeSource::new));

    /** Tudo que este mundo pode devolver; a ordem nao importa, a lista sim. */
    private static final List<ResourceKey<Biome>> PALETTE = List.of(
            Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA,
            Biomes.PLAINS, Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.SWAMP,
            Biomes.SAVANNA, Biomes.DESERT, Biomes.SPARSE_JUNGLE, Biomes.JUNGLE);

    private final HolderGetter<Biome> registry;

    private long seed;
    private AlphaNoise temperatureNoise;
    private AlphaNoise humidityNoise;
    private AlphaNoise detailNoise;
    private volatile boolean ready = false;

    public AlphaBiomeSource(HolderGetter<Biome> registry) {
        this.registry = registry;
    }

    /**
     * A semente chega de fora.
     *
     * A fonte de biomas moderna nunca ve a semente do mundo — ela recebe o
     * amostrador de clima ja pronto, e o nosso vem zerado. Entao quem passa a
     * semente e o gerador, no createState, que acontece uma vez so e antes de
     * qualquer chunk. Se por algum caminho ninguem passar, o mundo ainda nasce
     * (semente 0) em vez de quebrar.
     */
    public void setSeed(long seed) {
        if (ready && this.seed == seed) return;
        synchronized (this) {
            this.seed = seed;
            RandomSource random = RandomSource.create(seed ^ 0x5643_4C49_4D41L); // "VCLIMA"
            temperatureNoise = new AlphaNoise(random, 4);
            humidityNoise = new AlphaNoise(random, 4);
            detailNoise = new AlphaNoise(random, 2);
            ready = true;
        }
    }

    private void ensureNoise() {
        if (!ready) setSeed(0L);
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return PALETTE.stream().map(registry::getOrThrow);
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        ensureNoise();
        // As coordenadas chegam em quartos de bloco (o mundo guarda bioma a cada 4).
        int x = quartX << 2;
        int z = quartZ << 2;
        return registry.getOrThrow(pick(temperature(x, z), humidity(x, z)));
    }

    // ------------------------------------------------------------------ clima

    /**
     * O ruido miudo que suja as duas medidas.
     *
     * Sem ele a divisa entre dois biomas sai lisa como curva de nivel; com ele a
     * borda fica rendilhada, com dedos de floresta entrando na planicie.
     */
    private double detail(int x, int z) {
        return detailNoise.sample2d(x, z, 0.25) * 0.06;
    }

    /**
     * AMPLITUDE: aprendida na marra. A primeira versao usava os coeficientes da
     * epoca (ruido * 0.15 + 0.7) e o servidor de teste devolveu DOIS biomas em 144
     * chunks — todo o mapa caiu na mesma faixa temperada. O motivo e que a soma de
     * oitavas daqui nao tem a mesma escala da do jogo antigo; copiar o numero sem
     * medir a saida nao vale de nada. Com media 0.5 e um espalhamento largo, cada
     * faixa da tabela recebe a sua fatia.
     */
    private double temperature(int x, int z) {
        return Mth.clamp(temperatureNoise.sample2d(x, z, 0.010) * 0.70 + 0.5 + detail(x, z), 0.0, 1.0);
    }

    private double humidity(int x, int z) {
        return Mth.clamp(humidityNoise.sample2d(x, z, 0.014) * 0.70 + 0.5 + detail(x, z), 0.0, 1.0);
    }

    /**
     * A tabela.
     *
     * Le-se de cima para baixo como uma regra de clima real: primeiro o frio (onde
     * a chuva quase nao conta), depois o temperado, por fim o quente, onde a chuva
     * decide tudo entre deserto e selva.
     */
    private ResourceKey<Biome> pick(double temperature, double humidity) {
        if (temperature < 0.25) {
            return humidity > 0.45 ? Biomes.SNOWY_TAIGA : Biomes.SNOWY_PLAINS;
        }
        if (temperature < 0.42) {
            return humidity > 0.60 ? Biomes.OLD_GROWTH_PINE_TAIGA : Biomes.TAIGA;
        }
        if (temperature < 0.62) {
            if (humidity < 0.30) return Biomes.PLAINS;
            if (humidity < 0.60) return Biomes.FOREST;
            return humidity < 0.80 ? Biomes.BIRCH_FOREST : Biomes.SWAMP;
        }
        if (temperature < 0.78) {
            if (humidity < 0.30) return Biomes.SAVANNA;
            if (humidity < 0.55) return Biomes.PLAINS;
            return humidity < 0.80 ? Biomes.FOREST : Biomes.DARK_FOREST;
        }
        if (humidity < 0.40) return Biomes.DESERT;
        if (humidity < 0.60) return Biomes.SAVANNA;
        return humidity < 0.80 ? Biomes.SPARSE_JUNGLE : Biomes.JUNGLE;
    }
}
