package net.vhsworld.rec.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.vhsworld.rec.client.world.TapeArchiveScreen;

import java.util.Random;

/**
 * A tela inicial.
 *
 * Nao e um menu com tema de VHS: e uma fita ja rodando, e o menu esta gravado nela.
 * Por isso o titulo treme, a fita conta o tempo desde que o jogo abriu, e o glitch
 * pega o texto e os botoes junto — nao so o fundo.
 */
public class CustomMainMenuScreen extends Screen {

    private static final Random RANDOM = new Random();

    private static final String TITLE = "VHSWORLD";
    private static final String SUBTITLE = "FOUND FOOTAGE";

    /** Duracao da entrada: o titulo desce do tamanho do loading ate o do menu. */
    private static final long INTRO_MS = 900L;

    /** Uma vez por sessao. Reabrir o menu no meio do jogo nao repete a abertura. */
    private static boolean introPlayed = false;

    private long openedAt = 0L;

    public CustomMainMenuScreen() {
        super(Component.literal("REC Mod Menu"));
    }

    @Override
    protected void init() {
        if (openedAt == 0L) openedAt = System.currentTimeMillis();

        int buttonWidth = 160;
        int buttonHeight = 22;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = this.height / 2 + 6;

        VHSScreenHelper.ensureAmbienceSound();

        this.addRenderableWidget(new VHSButton(centerX, startY, buttonWidth, buttonHeight,
                Component.literal("TAPES"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new TapeArchiveScreen(this));
            }
        }));

        this.addRenderableWidget(new VHSButton(centerX, startY + 26, buttonWidth, buttonHeight,
                Component.literal("MULTIPLAYER"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new CustomJoinMultiplayerScreen(this));
            }
        }));

        this.addRenderableWidget(new VHSButton(centerX, startY + 52, buttonWidth, buttonHeight,
                Component.literal("OPTIONS"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options));
            }
        }));

        this.addRenderableWidget(new VHSButton(centerX, startY + 78, buttonWidth, buttonHeight,
                Component.literal("EJECT"), button -> {
            if (this.minecraft != null) {
                VHSScreenHelper.stopAmbienceSound();
                this.minecraft.stop();
            }
        }));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        VHSScreenHelper.renderVHSBackground(guiGraphics, this.width, this.height, "TAPE #001 — UNKNOWN");

        // O rasgo da fita pega a tela inteira: titulo, botoes e tudo.
        int shift = VHSScreenHelper.glitchShift();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(shift, 0.0f, 0.0f);

        float intro = introProgress();

        renderTitle(guiGraphics, intro);

        // Os botoes so entram quando o titulo chega no lugar: a fita "assenta"
        // antes de virar menu.
        if (intro >= 1.0f) {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        guiGraphics.pose().popPose();

        // Estatica cobrindo o corte, forte no inicio e sumindo
        if (intro < 1.0f) {
            int burst = (int) ((1.0f - intro) * 160);
            guiGraphics.fill(0, 0, this.width, this.height, (burst << 24) | 0x00101014);
            for (int i = 0; i < (int) ((1.0f - intro) * 220); i++) {
                int x = RANDOM.nextInt(this.width);
                int y = RANDOM.nextInt(this.height);
                int w = 1 + RANDOM.nextInt(20);
                guiGraphics.fill(x, y, x + w, y + 1, 0x99FFFFFF);
            }
        }
    }

    /** 0.0 no primeiro frame, 1.0 quando o titulo terminou de assentar. */
    private float introProgress() {
        if (introPlayed) return 1.0f;

        long elapsed = System.currentTimeMillis() - openedAt;
        if (elapsed >= INTRO_MS) {
            introPlayed = true;
            return 1.0f;
        }
        float t = elapsed / (float) INTRO_MS;
        return 1.0f - (1.0f - t) * (1.0f - t);   // desacelera no fim
    }

    /**
     * O titulo com uma sombra vermelha deslocada.
     *
     * E o defeito de sincronia de cor da fita velha: o canal vermelho chega um pixel
     * antes do resto. Custa duas chamadas de texto e faz mais pela ambientacao do que
     * qualquer imagem que eu pudesse desenhar aqui.
     */
    private void renderTitle(GuiGraphics g, float intro) {
        int centerX = this.width / 2;
        int titleY = this.height / 2 - 60;

        // Sai de onde o loading deixou (grande, no meio) e chega no lugar do menu.
        float scale = Mth.lerp(intro, 4.0f, 1.0f);
        float y = Mth.lerp(intro, this.height / 2.0f - 54.0f, (float) titleY);

        int jitter = RANDOM.nextFloat() < 0.12f ? RANDOM.nextInt(3) - 1 : 0;
        int w = this.font.width(TITLE);

        g.pose().pushPose();
        g.pose().translate(centerX + jitter, y, 0.0f);
        g.pose().scale(scale, scale, 1.0f);
        g.drawString(this.font, TITLE, -w / 2 - 1, 0, 0xFFAA1111, false);
        g.drawString(this.font, TITLE, -w / 2, 0, 0xFFEEEEEE, false);
        g.pose().popPose();

        if (intro < 1.0f) return;   // subtitulo e linha so depois de assentar

        int sw = this.font.width(SUBTITLE);
        g.drawString(this.font, SUBTITLE, centerX - sw / 2, titleY + 14, 0xFF888888, false);
        g.fill(centerX - 90, titleY + 30, centerX + 90, titleY + 31, 0x44FFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
