package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.vhsworld.rec.config.RECConfig;

/**
 * O flash iluminando o mundo de verdade, e nao so pintando a tela de branco.
 *
 * COMO: sobe o gamma do jogo por alguns instantes e devolve depois. E um truque
 * client-side de proposito — o mod nao tem rede, e por um sexto de segundo de luz
 * nao vale abrir um canal com o servidor nem enfiar blocos de luz no mundo.
 *
 * O efeito colateral e bem-vindo: a foto e capturada no frame seguinte ao disparo,
 * entao ela sai iluminada pelo proprio flash. A caverna escura vira uma foto que
 * mostra o que havia ali.
 *
 * A restauracao acontece todo tick, incondicionalmente. Se o jogo fechar no meio de
 * um flash, o gamma volta na proxima entrada — ele nunca fica presente no alto.
 */
public final class FlashLight {

    private static int ticks;
    private static int total;
    private static double originalGamma = -1.0;

    /** Dispara a luz. Chamado no mesmo instante do clarao. */
    public static void trigger() {
        if (!RECConfig.CLIENT.flashLights.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        if (originalGamma < 0.0) {
            originalGamma = mc.options.gamma().get();
        }

        total = Math.max(1, (int) Math.round(RECConfig.CLIENT.flashLightSeconds.get() * 20.0D));
        ticks = total;
    }

    public static void tick(Minecraft mc) {
        if (originalGamma < 0.0 || mc.options == null) return;

        if (ticks <= 0) {
            mc.options.gamma().set(originalGamma);
            originalGamma = -1.0;
            return;
        }

        ticks--;

        // Pico no disparo e queda rapida: um flash e um estouro, nao um holofote.
        double envelope = (double) ticks / total;
        double boost = RECConfig.CLIENT.flashLightBoost.get() * envelope * envelope;

        mc.options.gamma().set(originalGamma + boost);
    }

    /** Devolve o gamma na marra — usado ao sair do mundo. */
    public static void restore(Minecraft mc) {
        if (originalGamma >= 0.0 && mc.options != null) {
            mc.options.gamma().set(originalGamma);
        }
        originalGamma = -1.0;
        ticks = 0;
    }

    private FlashLight() {}
}
