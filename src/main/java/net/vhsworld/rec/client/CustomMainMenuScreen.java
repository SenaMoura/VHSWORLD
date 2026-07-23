package net.vhsworld.rec.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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

    public CustomMainMenuScreen() {
        super(Component.literal("REC Mod Menu"));
    }

    @Override
    protected void init() {
        int buttonWidth = 160;
        int buttonHeight = 22;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = this.height / 2 + 6;

        VHSScreenHelper.ensureAmbienceSound();

        this.addRenderableWidget(new VHSButton(centerX, startY, buttonWidth, buttonHeight,
                Component.literal("TAPES"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new CustomSelectWorldScreen(this));
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

        renderTitle(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.pose().popPose();
    }

    /**
     * O titulo com uma sombra vermelha deslocada.
     *
     * E o defeito de sincronia de cor da fita velha: o canal vermelho chega um pixel
     * antes do resto. Custa duas chamadas de texto e faz mais pela ambientacao do que
     * qualquer imagem que eu pudesse desenhar aqui.
     */
    private void renderTitle(GuiGraphics g) {
        int centerX = this.width / 2;
        int titleY = this.height / 2 - 60;

        int jitter = RANDOM.nextFloat() < 0.12f ? RANDOM.nextInt(3) - 1 : 0;

        String title = TITLE;
        int w = this.font.width(title);

        // fantasma vermelho, depois o branco por cima
        g.drawString(this.font, title, centerX - w / 2 - 2 + jitter, titleY, 0xFFAA1111, false);
        g.drawString(this.font, title, centerX - w / 2 + jitter, titleY, 0xFFEEEEEE, false);

        int sw = this.font.width(SUBTITLE);
        g.drawString(this.font, SUBTITLE, centerX - sw / 2, titleY + 14, 0xFF888888, false);

        // Linha fina separando o cabecalho dos botoes
        g.fill(centerX - 90, titleY + 30, centerX + 90, titleY + 31, 0x44FFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
