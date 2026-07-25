package net.vhsworld.rec.client.fx;

import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

/**
 * O chiado de fita das telas do mod, num lugar so.
 *
 * Mesma ideia do {@link CardStyle}: se a cara e comum a varias telas, ela mora em um
 * arquivo. Aqui ficam o grao, as scanlines e as barras de tracking.
 */
public final class VHSScreenStatic {

    private VHSScreenStatic() {}

    private static final Random RANDOM = new Random();

    /** So o grao, com a densidade pedida (1.0 = tela cheia de chiado). */
    public static void draw(GuiGraphics g, int width, int height, float intensity) {
        if (intensity <= 0.0f) return;
        int grains = (int) Math.max(60, (width * height) / 260.0f * intensity);
        for (int i = 0; i < grains; i++) {
            int rx = RANDOM.nextInt(width);
            int ry = RANDOM.nextInt(height);
            int v = 90 + RANDOM.nextInt(166);
            int a = (int) ((60 + RANDOM.nextInt(120)) * Math.min(1.0f, intensity));
            int w = RANDOM.nextInt(10) == 0 ? 2 + RANDOM.nextInt(3) : 1;
            g.fill(rx, ry, rx + w, ry + 1, (a << 24) | (v << 16) | (v << 8) | v);
        }
    }

    /** Scanlines por cima de tudo, a cada duas linhas. */
    public static void scanlines(GuiGraphics g, int width, int height, int alpha) {
        int color = (alpha << 24);
        for (int y = 0; y < height; y += 2) {
            g.fill(0, y, width, y + 1, color);
        }
    }

    /** As faixas claras que descem — o defeito classico da fita. */
    public static void trackingBars(GuiGraphics g, int width, int height, float seconds) {
        for (int b = 0; b < 2; b++) {
            int by = (int) (((seconds * (38 + b * 27)) + b * height / 2f) % (height + 40)) - 20;
            int bh = 8 + b * 5;
            g.fill(0, by, width, by + bh, 0x14FFFFFF);
            g.fill(0, by + bh, width, by + bh + 1, 0x33FFFFFF);
        }
    }

    /** O fundo completo: preto sujo, scanlines, grao pesado e tracking. */
    public static void full(GuiGraphics g, int width, int height, float seconds) {
        int tone = 4 + RANDOM.nextInt(5);
        g.fill(0, 0, width, height, (0xFF << 24) | (tone << 16) | (tone << 8) | (tone + 6));
        scanlines(g, width, height, 0x77);
        draw(g, width, height, 1.0f);
        trackingBars(g, width, height, seconds);
    }
}
