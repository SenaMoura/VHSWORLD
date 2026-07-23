package net.vhsworld.rec.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.vhsworld.rec.item.ModSounds;

public class VHSButton extends Button {

    public VHSButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    /**
     * Troca o "plim" do vanilla pelo clique seco do mod.
     *
     * O mesmo som que abre o album e o registro dentro do jogo. Botao de menu e
     * botao de camera passam a soar igual — e a mesma maquina.
     */
    @Override
    public void playDownSound(SoundManager sounds) {
        sounds.play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                ModSounds.MENU_BUTTON.get(), 1.0f));
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean isHovered = this.isHoveredOrFocused() && this.active;

        // Fundo e bordas baseados na seleção e status ativo
        int backgroundColor;
        int borderColor;
        int textColor;

        if (!this.active) {
            backgroundColor = 0x88050505;
            borderColor = 0xFF444444;
            textColor = 0xFF666666;
        } else if (isHovered) {
            backgroundColor = 0xDD222222;
            borderColor = 0xFFFFFFFF;
            textColor = 0xFFFFFFFF;
        } else {
            backgroundColor = 0xAA0A0A0C;
            borderColor = 0xFF777777;
            textColor = 0xDDCCCCCC;
        }

        // Fundo retangular
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, backgroundColor);

        // Moldura fina (1px)
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, borderColor);
        guiGraphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, borderColor);
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, borderColor);
        guiGraphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, borderColor);

        // Indicador de Seleção ">"
        String buttonText = isHovered ? "> " + this.getMessage().getString() : this.getMessage().getString();

        var font = net.minecraft.client.Minecraft.getInstance().font;
        int textWidth = font.width(buttonText);
        int textX = this.getX() + (this.width - textWidth) / 2;
        int textY = this.getY() + (this.height - 8) / 2;

        guiGraphics.drawString(font, buttonText, textX, textY, textColor, false);
    }
}