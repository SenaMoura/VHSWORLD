package net.vhsworld.rec.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import java.util.Random;

/**
 * O que o jogador ve enquanto o jogo carrega: a fita entrando no aparelho.
 *
 * Desenhado por cima da tela da Mojang (ver LoadingOverlayMixin), com o progresso
 * REAL do carregamento — o circulo nao gira por enfeite, ele mede.
 */
public final class VHSLoadingRenderer {

    private static final Random RANDOM = new Random();

    /** Quantos pontos formam o circulo. */
    private static final int DOTS = 12;
    private static final int RADIUS = 26;

    private static boolean soundPlayed = false;

    public static void render(GuiGraphics g, float progress, long fadeOutStart) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;

        int width = g.guiWidth();
        int height = g.guiHeight();

        // Acompanha o fade do vanilla: se sumissemos de uma vez, o corte para o menu
        // apareceria como um salto.
        float alpha = 1.0f;
        if (fadeOutStart > -1L) {
            long elapsed = Util_millis() - fadeOutStart;
            alpha = 1.0f - Mth.clamp(elapsed / 1000.0f, 0.0f, 1.0f);
        }
        if (alpha <= 0.01f) return;

        if (!soundPlayed) {
            soundPlayed = true;
            playTapeSound(mc);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int a = (int) (alpha * 255.0f) << 24;

        // 1. Fundo: preto quase total, cobrindo a tela da Mojang
        g.fill(0, 0, width, height, a | 0x00050508);

        // 2. Scanlines e chiado
        for (int y = 0; y < height; y += 3) {
            g.fill(0, y, width, y + 1, (int) (alpha * 0x44) << 24);
        }
        for (int i = 0; i < 140; i++) {
            int x = RANDOM.nextInt(width);
            int y = RANDOM.nextInt(height);
            int w = 1 + RANDOM.nextInt(18);
            int shade = 60 + RANDOM.nextInt(120);
            g.fill(x, y, x + w, y + 1, ((int) (alpha * (30 + RANDOM.nextInt(60))) << 24)
                    | (shade << 16) | (shade << 8) | shade);
        }

        // 3. Letterbox, o mesmo do jogo
        int barX = CamcorderOverlay.letterboxBarWidth(width);
        if (barX > 0) {
            g.fill(0, 0, barX, height, a | 0x00000000);
            g.fill(width - barX, 0, width, height, a | 0x00000000);
        }

        // 4. VHSWORLD grande, com o fantasma vermelho da fita
        drawBigTitle(g, mc, width, height, alpha);

        // 5. Circulo girando, movido pelo progresso de verdade
        drawSpinner(g, width / 2, height / 2 + 26, progress, alpha);

        // 6. Barra e porcentagem
        int pct = (int) (Mth.clamp(progress, 0.0f, 1.0f) * 100.0f);
        String label = "CARREGANDO FITA  " + pct + "%";
        int lw = mc.font.width(label);
        g.drawString(mc.font, label, (width - lw) / 2, height / 2 + 66,
                ((int) (alpha * 0xCC) << 24) | 0x00CCCCCC, false);

        RenderSystem.disableBlend();
    }

    private static void drawBigTitle(GuiGraphics g, Minecraft mc, int width, int height, float alpha) {
        String title = "VHSWORLD";
        float scale = 4.0f;

        int jitter = RANDOM.nextFloat() < 0.10f ? RANDOM.nextInt(3) - 1 : 0;

        g.pose().pushPose();
        g.pose().translate(width / 2.0f + jitter, height / 2.0f - 46.0f, 0.0f);
        g.pose().scale(scale, scale, 1.0f);

        int w = mc.font.width(title);
        int red   = ((int) (alpha * 0xFF) << 24) | 0x00AA1111;
        int white = ((int) (alpha * 0xFF) << 24) | 0x00EEEEEE;

        g.drawString(mc.font, title, -w / 2 - 1, -8, red, false);
        g.drawString(mc.font, title, -w / 2, -8, white, false);
        g.pose().popPose();
    }

    /**
     * O circulo: os pontos acendem conforme o carregamento anda, e um brilho corre
     * por cima o tempo todo. Assim ele responde ao progresso sem parecer travado
     * quando o carregamento demora num passo so.
     */
    private static void drawSpinner(GuiGraphics g, int cx, int cy, float progress, float alpha) {
        long now = Util_millis();
        int head = (int) ((now / 90) % DOTS);
        int filled = Math.round(Mth.clamp(progress, 0.0f, 1.0f) * DOTS);

        for (int i = 0; i < DOTS; i++) {
            double angle = (Math.PI * 2.0 * i / DOTS) - Math.PI / 2.0;
            int x = cx + (int) Math.round(Math.cos(angle) * RADIUS);
            int y = cy + (int) Math.round(Math.sin(angle) * RADIUS);

            boolean done = i < filled;
            boolean lit = i == head || i == (head + 1) % DOTS;

            int color;
            if (lit) {
                color = 0x00FFFFFF;
            } else if (done) {
                color = 0x00BB2222;
            } else {
                color = 0x00404048;
            }

            int size = lit ? 3 : 2;
            g.fill(x - size, y - size, x + size, y + size,
                    ((int) (alpha * 0xFF) << 24) | color);
        }
    }

    private static void playTapeSound(Minecraft mc) {
        try {
            mc.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.vhsworld.rec.item.ModSounds.TAPE_PLAYER.get(), 1.0f));
        } catch (Throwable ignored) {
            // Som e enfeite: se o registro ainda nao estiver pronto, o loading segue.
        }
    }

    private static long Util_millis() {
        return System.currentTimeMillis();
    }

    private VHSLoadingRenderer() {}
}
