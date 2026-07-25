package net.vhsworld.rec.client.fx;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A aparencia unica dos cards do mod: PRETO com moldura BRANCA.
 *
 * Todo card do jogo (registro, dificuldade) passa por aqui, entao mudar a cara de
 * todos e mudar este arquivo. Antes cada tela pintava a propria borda com a cor da
 * categoria, e o resultado era um menu colorido dentro de um mod que e preto e
 * branco — a cor tirava a tela do mundo dele.
 *
 * A moldura acende no hover: apagada quando o card esta parado, branca viva quando
 * o mouse esta em cima. E o unico "brilho" que sobrou.
 */
public final class CardStyle {

    private CardStyle() {}

    /** Miolo do card: preto, um tom acima do fundo para o card nao sumir nele. */
    public static final int BODY = 0xFF060607;

    /** Moldura com o mouse em cima. */
    public static final int BORDER = 0xFFEDEDED;

    /** Moldura parada — branco sujo, para o card so acender quando olhado. */
    public static final int BORDER_IDLE = 0xFF6E6E72;

    /** Titulo dentro do card. */
    public static final int TITLE = 0xFFF2F2F2;

    /** Texto corrido dentro do card. */
    public static final int TEXT = 0xFF9A9AA2;

    /** Texto secundario (contadores). */
    public static final int TEXT_DIM = 0xFF6A6A70;

    /**
     * Desenha o halo, a moldura e o miolo. O conteudo vem por cima, na tela que chamou.
     *
     * @param hover 0.0 parado, 1.0 com o mouse em cima
     */
    public static void frame(GuiGraphics g, int x, int y, int w, int h, float hover) {
        if (hover > 0.02f) {
            int a = (int) (hover * 70) << 24;
            g.fill(x - 3, y - 3, x + w + 3, y + h + 3, a | 0x00FFFFFF);
        }
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, border(hover));
        g.fill(x, y, x + w, y + h, BODY);
    }

    /** A cor da moldura agora, entre a apagada e a viva. */
    public static int border(float hover) {
        return lerp(BORDER_IDLE, BORDER, Math.max(0.0f, Math.min(1.0f, hover)));
    }

    private static int lerp(int from, int to, float t) {
        int a = mix((from >> 24) & 0xFF, (to >> 24) & 0xFF, t);
        int r = mix((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int gg = mix((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = mix(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }

    private static int mix(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }
}
