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

        // A fita só suja a IMAGEM. As barras pretas do letterbox ficam limpas — elas são
        // o recorte da câmera, não parte do vídeo.
        int barX = CamcorderOverlay.letterboxBarWidth(width);
        int barY = CamcorderOverlay.letterboxBarHeight(height);

        int left = barX;
        int right = width - barX;
        int top = barY;
        int bottom = height - barY;
        if (right <= left || bottom <= top) return;

        if (RECConfig.CLIENT.scanlines.get()) {
            drawScanlines(guiGraphics, left, right, top, bottom);
        }
        if (RECConfig.CLIENT.staticNoise.get()) {
            drawStatic(guiGraphics, left, right, top, bottom, wear);
        }
        if (RECConfig.CLIENT.trackingBar.get()) {
            drawTracking(guiGraphics, left, right, top, bottom, mc, partialTick, wear);
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

    private static void drawScanlines(net.minecraft.client.gui.GuiGraphics g,
                                      int left, int right, int top, int bottom) {
        int alpha = (int) (RECConfig.CLIENT.scanlineOpacity.get() * 255.0D);
        if (alpha <= 0) return;

        int color = (alpha << 24);            // preto com alfa: escurece a linha
        int spacing = RECConfig.CLIENT.scanlineSpacing.get();

        for (int y = top; y < bottom; y += spacing) {
            g.fill(left, y, right, y + 1, color);
        }
    }

    private static void drawStatic(net.minecraft.client.gui.GuiGraphics g,
                                   int left, int right, int top, int bottom, float wear) {
        double amount = RECConfig.CLIENT.staticAmount.get();
        if (amount <= 0.0D) return;

        int span = right - left;
        int band = bottom - top;

        // Teto proposital: mesmo com wear alto o chiado nao vira custo de render.
        int specks = (int) Math.min(1200, amount * 700.0D * wear);

        for (int i = 0; i < specks; i++) {
            int x = left + RANDOM.nextInt(span);
            int y = top + RANDOM.nextInt(band);
            int w = 1 + RANDOM.nextInt(4);
            int a = 60 + RANDOM.nextInt(120);
            g.fill(x, y, Math.min(right, x + w), y + 1, (a << 24) | 0x00FFFFFF);
        }
    }

    private static void drawTracking(net.minecraft.client.gui.GuiGraphics g,
                                     int left, int right, int top, int bottom,
                                     Minecraft mc, float partialTick, float wear) {
        int periodTicks = RECConfig.CLIENT.trackingPeriodSeconds.get() * 20;
        double time = (mc.level.getGameTime() + partialTick) % periodTicks;

        int span = right - left;
        int band = bottom - top;

        // Faixa sobe de baixo para cima, saindo e entrando fora da imagem.
        int travel = band + TRACKING_HEIGHT;
        int bandTop = (int) (bottom - (time / periodTicks) * travel);

        int y0 = Math.max(top, bandTop);
        int y1 = Math.min(bottom, bandTop + TRACKING_HEIGHT);
        if (y1 <= y0) return;

        int bandAlpha = (int) Math.min(120, 34 * wear);
        g.fill(left, y0, right, y1, (bandAlpha << 24) | 0x00FFFFFF);

        // Linhas rasgadas dentro da faixa: o "rolo" da fita.
        int tears = 3 + (int) (wear * 2);
        for (int i = 0; i < tears; i++) {
            int y = y0 + RANDOM.nextInt(Math.max(1, y1 - y0));
            int x = left + RANDOM.nextInt(Math.max(1, span / 2));
            int w = span / 3 + RANDOM.nextInt(Math.max(1, span / 3));
            g.fill(x, y, Math.min(right, x + w), y + 1, 0x99FFFFFF);
        }
    }

    private VHSEffectOverlay() {}
}
