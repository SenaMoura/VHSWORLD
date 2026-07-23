package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.vhsworld.rec.config.RECConfig;

import java.util.Random;

/**
 * A cara da fita VHS: scanlines, chiado e barra de tracking.
 *
 * POR QUE ASSIM: a v1.0.0 fazia isto com um post-shader (recmod:fisheye), que toma conta
 * do framebuffer principal. Com Oculus/Iris ligado, o shaderpack tambem quer esse
 * framebuffer — dois donos, e o resultado eram as chunks distantes corrompidas.
 *
 * Aqui nada disso acontece: e desenho de GUI, depois que o mundo ja foi renderizado.
 * Nao le, nao substitui e nao redimensiona nenhum render target. E compativel com
 * qualquer shaderpack por construcao, nao por sorte.
 *
 * O que se perde: a curvatura geometrica real da lente (isso so o post-shader faz).
 * Ela e compensada pelo aumento de FOV do CameraDistortionEvent e pela moldura do visor.
 */
public final class VHSEffectOverlay {

    private static final Random RANDOM = new Random();

    /** Altura da faixa de tracking, em pixels. */
    private static final int TRACKING_HEIGHT = 26;

    public static final IGuiOverlay VHS_TAPE = (gui, guiGraphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!RECConfig.CLIENT.vhsEffect.get()) return;

        // A fita continua rodando no escuro do apagao.
        if (!CameraState.isActive() && !CamcorderOverlay.isBatteryDead) return;

        float wear = wearFactor();

        if (RECConfig.CLIENT.scanlines.get()) {
            drawScanlines(guiGraphics, width, height);
        }
        if (RECConfig.CLIENT.staticNoise.get()) {
            drawStatic(guiGraphics, width, height, wear);
        }
        if (RECConfig.CLIENT.trackingBar.get()) {
            drawTracking(guiGraphics, width, height, mc, partialTick, wear);
        }
    };

    /**
     * 1.0 = fita nova. Cresce ate ~3.0 conforme a bateria morre, se
     * degradeWithBattery estiver ligado. E o que faz o apagao parecer chegando.
     */
    private static float wearFactor() {
        if (!RECConfig.CLIENT.degradeWithBattery.get()) return 1.0f;
        if (CamcorderOverlay.isBatteryDead) return 3.0f;

        float charge = Math.max(0.0f, Math.min(100.0f, CamcorderOverlay.batteryLevel)) / 100.0f;
        return 1.0f + (1.0f - charge) * 2.0f;
    }

    private static void drawScanlines(net.minecraft.client.gui.GuiGraphics g, int width, int height) {
        int alpha = (int) (RECConfig.CLIENT.scanlineOpacity.get() * 255.0D);
        if (alpha <= 0) return;

        int color = (alpha << 24);            // preto com alfa: escurece a linha
        int spacing = RECConfig.CLIENT.scanlineSpacing.get();

        for (int y = 0; y < height; y += spacing) {
            g.fill(0, y, width, y + 1, color);
        }
    }

    private static void drawStatic(net.minecraft.client.gui.GuiGraphics g, int width, int height, float wear) {
        double amount = RECConfig.CLIENT.staticAmount.get();
        if (amount <= 0.0D) return;

        // Teto proposital: mesmo com wear alto o chiado nao vira custo de render.
        int specks = (int) Math.min(900, amount * 350.0D * wear);

        for (int i = 0; i < specks; i++) {
            int x = RANDOM.nextInt(width);
            int y = RANDOM.nextInt(height);
            int w = 1 + RANDOM.nextInt(3);
            int a = 40 + RANDOM.nextInt(70);
            g.fill(x, y, x + w, y + 1, (a << 24) | 0x00FFFFFF);
        }
    }

    private static void drawTracking(net.minecraft.client.gui.GuiGraphics g, int width, int height,
                                     Minecraft mc, float partialTick, float wear) {
        int periodTicks = RECConfig.CLIENT.trackingPeriodSeconds.get() * 20;
        double time = (mc.level.getGameTime() + partialTick) % periodTicks;

        // Faixa sobe de baixo para cima, saindo e entrando fora da tela.
        int travel = height + TRACKING_HEIGHT;
        int top = (int) (height - (time / periodTicks) * travel);

        int bandAlpha = (int) Math.min(60, 14 * wear);
        g.fill(0, top, width, top + TRACKING_HEIGHT, (bandAlpha << 24) | 0x00FFFFFF);

        // Linhas rasgadas dentro da faixa: o "rolo" da fita.
        int tears = 2 + (int) wear;
        for (int i = 0; i < tears; i++) {
            int y = top + RANDOM.nextInt(TRACKING_HEIGHT);
            int x = RANDOM.nextInt(Math.max(1, width / 2));
            int w = width / 3 + RANDOM.nextInt(Math.max(1, width / 3));
            g.fill(x, y, Math.min(width, x + w), y + 1, 0x66FFFFFF);
        }
    }

    private VHSEffectOverlay() {}
}
