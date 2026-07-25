package net.vhsworld.rec.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.vhsworld.rec.config.RECConfig;

/**
 * A MANCHA. A transicao de entrada no mundo.
 *
 * Quando a tela de "loading terrain" do jogo termina, uma mancha preta nasce no meio
 * e come a tela depressa. E ela que entrega o jogo: sob o preto, ou a escolha de
 * dificuldade abre, ou a mancha se recolhe e o mundo ja esta ali.
 *
 * Nao e um circulo. E um borrao de tinta — um miolo grande e uma coroa de bolhas de
 * tamanhos diferentes em volta, com fios que disparam na frente, como tinta caindo
 * na agua. O contorno irregular e o que separa "mancha" de "fade preto".
 *
 * ⚠️ TRAVA DE SEGURANCA: existe um teto de tempo no estado COBERTO. Se por qualquer
 * motivo ninguem mandar abrir (a tela de dificuldade nao subiu, um mod de terceiro
 * atravessou), a mancha se recolhe sozinha. O jogador nunca fica preso no preto —
 * essa e a licao das versoes antigas que travavam a tela.
 */
public final class InkTransition {

    private InkTransition() {}

    private enum Phase { IDLE, GROWING, COVERED, OPENING }

    private static Phase phase = Phase.IDLE;

    /** Ticks dentro da fase atual (com fracao de frame somada na hora de desenhar). */
    private static int ticks;

    /**
     * Quanto tempo a tela fica preta antes de a mancha abrir sozinha, em ticks.
     *
     * E tambem a TRAVA: quem quiser segurar o preto (a tela de dificuldade) precisa
     * chamar hold() a cada tick. Parou de chamar, a mancha abre em menos de um
     * segundo. Assim nao existe caminho que deixe o jogador presto no preto.
     */
    private static final int COVERED_MAX = 14;

    /** Quantas bolhas formam a coroa do borrao. */
    private static final int LOBES = 22;

    /** Quantos fios disparam na frente da mancha. */
    private static final int THREADS = 10;

    // ------------------------------------------------------------------ controle

    /** Comeca a comer a tela. Chamado quando o "loading terrain" termina. */
    public static void consume() {
        if (!RECConfig.CLIENT.inkTransition.get()) return;
        phase = Phase.GROWING;
        ticks = 0;
    }

    /** Manda a mancha se recolher e devolver a imagem. */
    public static void open() {
        if (phase == Phase.GROWING || phase == Phase.COVERED) {
            phase = Phase.OPENING;
            ticks = 0;
        }
    }

    /** Segura o preto por mais um tick. Quem abre uma tela por baixo chama isto. */
    public static void hold() {
        if (phase == Phase.COVERED) ticks = 0;
    }

    /** A tela esta totalmente preta? E o momento de trocar o que esta por baixo. */
    public static boolean covered() {
        return phase == Phase.COVERED;
    }

    public static boolean running() {
        return phase != Phase.IDLE;
    }

    /** Zera tudo — ao sair do mundo, para o proximo mundo comecar limpo. */
    public static void reset() {
        phase = Phase.IDLE;
        ticks = 0;
    }

    public static void tick() {
        if (phase == Phase.IDLE) return;
        ticks++;

        int span = growTicks();
        switch (phase) {
            case GROWING -> {
                if (ticks >= span) {
                    phase = Phase.COVERED;
                    ticks = 0;
                }
            }
            case COVERED -> {
                if (ticks >= COVERED_MAX) open();
            }
            case OPENING -> {
                if (ticks >= span) reset();
            }
            default -> {
            }
        }
    }

    private static int growTicks() {
        return Math.max(1, (int) Math.round(RECConfig.CLIENT.inkSeconds.get() * 20.0D));
    }

    // ------------------------------------------------------------------ desenho

    public static final IGuiOverlay INK = (gui, g, partialTick, width, height) -> render(g, width, height, partialTick);

    private static void render(GuiGraphics g, int width, int height, float partialTick) {
        if (phase == Phase.IDLE) return;

        if (phase == Phase.COVERED) {
            g.fill(0, 0, width, height, 0xFF000000);
            return;
        }

        float t = Math.min(1.0f, (ticks + partialTick) / growTicks());

        // Crescer acelera no fim (come a tela de vez); abrir desacelera (a imagem
        // volta com calma). Sao curvas diferentes de proposito.
        float f = phase == Phase.GROWING ? t * t : 1.0f - (1.0f - (1.0f - t) * (1.0f - t));

        double cx = width / 2.0;
        double cy = height / 2.0;
        // 1.45x a meia-diagonal, e nao 1.12x: com a borda irregular, o RAIO nao e o
        // alcance. Nas direcoes em que as bolhas encolhem, o alcance real e ~0.75 do
        // raio — com 1.12 tres dos quatro cantos ainda estavam claros no instante em
        // que a fase virava preto total, e a troca aparecia como um pulo. (Medido
        // desenhando a formula fora do jogo, nao no olho.)
        double full = Math.sqrt(cx * cx + cy * cy) * 1.45;
        double radius = full * f;
        if (radius <= 0.5) return;

        int ink = 0xFF000000;

        // Miolo.
        TentacleFx.disc(g, cx, cy, radius * 0.74, ink);

        // Coroa de bolhas: raios e distancias variados dao a borda rasgada. Os
        // numeros vem de senos fixos, nao de random — a mancha tem que ser a MESMA
        // forma em todo frame, senao ela ferve em vez de crescer.
        for (int i = 0; i < LOBES; i++) {
            double a = (i / (double) LOBES) * Math.PI * 2.0;
            // O piso do jitter (0.78) e o que garante que nenhuma direcao fique para
            // tras; a variacao por cima e so o rasgado da borda.
            double jitter = 0.78 + 0.20 * Math.sin(a * 3.0 + 1.3) + 0.12 * Math.sin(a * 7.0 + 0.4);
            double dist = radius * 0.55;
            TentacleFx.disc(g, cx + Math.cos(a) * dist, cy + Math.sin(a) * dist,
                    radius * 0.44 * jitter, ink);
        }

        // Fios na frente: a tinta nao chega com a borda reta, chega com dedos.
        for (int i = 0; i < THREADS; i++) {
            double a = (i / (double) THREADS) * Math.PI * 2.0 + 0.31;
            double reach = radius * (0.92 + 0.34 * Math.sin(a * 5.0 + 2.1));
            double girth = Math.max(1.5, radius * 0.055);
            TentacleFx.draw(g, cx + Math.cos(a) * radius * 0.5, cy + Math.sin(a) * radius * 0.5,
                    a, reach * 0.55, girth, i * 1.7, ticks + partialTick, 0.5);
        }

        // Enquanto cresce, a beirada chia: a mancha e feita de fita estragada.
        if (phase == Phase.GROWING) {
            int grains = 90;
            for (int i = 0; i < grains; i++) {
                double a = (i / (double) grains) * Math.PI * 2.0;
                double d = radius * (1.0 + 0.06 * Math.sin(a * 11.0 + ticks * 0.7));
                int px = (int) (cx + Math.cos(a) * d);
                int py = (int) (cy + Math.sin(a) * d);
                g.fill(px, py, px + 2, py + 1, 0x66000000);
            }
        }
    }
}
