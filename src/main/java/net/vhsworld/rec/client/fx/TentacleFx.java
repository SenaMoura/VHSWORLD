package net.vhsworld.rec.client.fx;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Tentaculos desenhados a mao, so com retangulos (GuiGraphics.fill) — o mod inteiro
 * pinta a interface assim, sem textura. Servem para os cantos da tela do registro e
 * para contornar os cards: a corrupcao esta sempre no limite da imagem, tateando para
 * dentro, nunca no centro.
 *
 * A curva e uma reta na direcao pedida somada a uma onda senoidal que cresce em direcao
 * a ponta (base rigida, ponta solta) e balanca devagar com o tempo. A espessura afina do
 * pe ate a ponta. Cada segmento e um disco cheio; a ponta ganha um brilho fraco.
 */
public final class TentacleFx {

    private TentacleFx() {}

    // A corrupcao nao tem cor propria: ela TIRA a cor. Por isso o tentaculo sai do
    // preto na base e vai clareando para um cinza de cinza na ponta — o mesmo
    // caminho de uma fita que perdeu o sinal. (Antes era roxo com brasa verde, o
    // que dava um ar de magia; aqui nao ha magia, ha imagem estragada.)

    /** Base: preto sujo, quase o fundo. */
    public static final int BODY = 0xE6070708;

    /** Meio do caminho: cinza-chumbo. */
    public static final int MID = 0xD22A2A2E;

    /** Ponta: cinza de cinza, o unico ponto que ainda reflete luz. */
    public static final int TIP = 0xB2757580;

    /**
     * Desenha um tentaculo.
     *
     * @param baseX,baseY  onde ele nasce (de preferencia fora da area visivel)
     * @param angle        direcao geral, em radianos
     * @param length       comprimento em pixels
     * @param girth        espessura na base, em pixels
     * @param phase        defasagem da onda (tentaculos vizinhos usam valores diferentes)
     * @param time         tempo animado (ex.: ticks + partialTick)
     * @param wiggle       amplitude do balanco (0 = reto; ~1 = bem solto)
     */
    public static void draw(GuiGraphics g, double baseX, double baseY, double angle,
                            double length, double girth, double phase, float time, double wiggle) {
        int steps = 16;
        double perpX = Math.cos(angle + Math.PI / 2.0);
        double perpY = Math.sin(angle + Math.PI / 2.0);
        double dirX = Math.cos(angle);
        double dirY = Math.sin(angle);

        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;

            // A onda cresce com t (ponta solta) e balanca no tempo.
            double wave = Math.sin(t * 3.2 + phase + time * 0.12) * (0.15 + t) * girth * 1.9 * wiggle;
            double along = length * t;

            double x = baseX + dirX * along + perpX * wave;
            double y = baseY + dirY * along + perpY * wave;

            double radius = girth * (1.0 - t) * 0.5 + 0.7;   // afina ate a ponta

            // Preto na base -> chumbo -> cinza na ponta, sem degrau visivel.
            disc(g, x, y, radius, t < 0.5 ? blend(BODY, MID, (float) (t * 2.0)) : MID);

            // O ultimo terco recebe o cinza claro por cima, mais fino: e o brilho
            // seco da ponta, nao uma brasa.
            if (t > 0.66) disc(g, x, y, radius * 0.55, TIP);
        }
    }

    /** Mistura duas cores ARGB. */
    public static int blend(int from, int to, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int a = ch(from, 24, to, t), r = ch(from, 16, to, t);
        int g = ch(from, 8, to, t), b = ch(from, 0, to, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int ch(int from, int shift, int to, float t) {
        int a = (from >> shift) & 0xFF;
        int b = (to >> shift) & 0xFF;
        return Math.round(a + (b - a) * t);
    }

    /** Disco cheio de raio r centrado em (cx,cy), montado por linhas de fill. */
    public static void disc(GuiGraphics g, double cx, double cy, double r, int color) {
        if (r < 0.6) {
            int px = (int) Math.round(cx);
            int py = (int) Math.round(cy);
            g.fill(px, py, px + 1, py + 1, color);
            return;
        }
        int ri = (int) Math.ceil(r);
        double r2 = r * r;
        for (int dy = -ri; dy <= ri; dy++) {
            double inside = r2 - dy * dy;
            if (inside < 0) continue;
            int span = (int) Math.floor(Math.sqrt(inside));
            int x0 = (int) Math.round(cx) - span;
            int x1 = (int) Math.round(cx) + span + 1;
            int y = (int) Math.round(cy) + dy;
            g.fill(x0, y, x1, y + 1, color);
        }
    }

    /**
     * Um tufo de tentaculos saindo de um mesmo ponto, abrindo em leque em torno de
     * uma direcao. Usado nos quatro cantos da tela e nas bordas dos cards.
     *
     * @param count   quantos tentaculos
     * @param spread  abertura do leque em radianos
     */
    public static void cluster(GuiGraphics g, double baseX, double baseY, double centerAngle,
                               int count, double spread, double length, double girth,
                               float time, double wiggle) {
        for (int i = 0; i < count; i++) {
            double f = count == 1 ? 0.5 : i / (double) (count - 1);
            double angle = centerAngle + (f - 0.5) * spread;
            double len = length * (0.7 + 0.3 * ((i * 7 % 5) / 4.0));
            draw(g, baseX, baseY, angle, len, girth, i * 1.7, time, wiggle);
        }
    }
}
