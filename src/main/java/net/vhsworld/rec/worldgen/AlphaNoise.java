package net.vhsworld.rec.worldgen;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

/**
 * Ruido em oitavas no formato antigo.
 *
 * O Minecraft moderno tem PerlinNoise proprio, mas ele normaliza e amostra de um
 * jeito diferente — usa-lo daria um terreno moderno com outra cara. O jeito alpha e
 * este: N camadas de ruido, cada uma com o dobro da frequencia e METADE do peso,
 * somadas cruas. E dessa soma que saem os paredoes e as ilhas flutuantes; o terreno
 * moderno nao tem overhang porque foi justamente isso que a Mojang domou.
 *
 * O ImprovedNoise usado por camada e o do proprio jogo: nao ha motivo para
 * reescrever Perlin, so para amostra-lo do jeito antigo.
 */
public final class AlphaNoise {

    private final ImprovedNoise[] octaves;

    public AlphaNoise(RandomSource random, int count) {
        this.octaves = new ImprovedNoise[count];
        for (int i = 0; i < count; i++) {
            this.octaves[i] = new ImprovedNoise(random);
        }
    }

    /**
     * Amostra tridimensional. Usada para o campo de densidade que decide onde ha
     * pedra e onde ha ar.
     */
    public double sample(double x, double y, double z, double xzScale, double yScale) {
        double total = 0.0;
        double frequency = 1.0;
        double amplitude = 1.0;

        for (ImprovedNoise octave : octaves) {
            total += octave.noise(
                    wrap(x * xzScale * frequency),
                    wrap(y * yScale * frequency),
                    wrap(z * xzScale * frequency)) * amplitude;

            frequency *= 2.0;
            amplitude *= 0.5;
        }
        return total;
    }

    /** Amostra plana, para mapas de altura e de praia. */
    public double sample2d(double x, double z, double scale) {
        double total = 0.0;
        double frequency = 1.0;
        double amplitude = 1.0;

        for (ImprovedNoise octave : octaves) {
            total += octave.noise(wrap(x * scale * frequency), 0.0, wrap(z * scale * frequency)) * amplitude;
            frequency *= 2.0;
            amplitude *= 0.5;
        }
        return total;
    }

    /**
     * Perlin perde precisao longe da origem e o terreno vira papel amassado.
     * Dobrar a coordenada de volta para perto do zero e o truque de sempre.
     */
    private static double wrap(double value) {
        double folded = value % 16777216.0;
        return folded;
    }
}
