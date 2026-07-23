package net.vhsworld.rec.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CustomMainMenuScreen extends Screen {

    public CustomMainMenuScreen() {
        super(Component.literal("REC Mod Menu"));
    }

    @Override
    protected void init() {
        int buttonWidth = 140;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int startY = this.height / 2 - 10;

        VHSScreenHelper.ensureAmbienceSound();

        // Botão TAPES (Singleplayer)
        this.addRenderableWidget(new VHSButton(centerX, startY, buttonWidth, buttonHeight,
                Component.literal("TAPES"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new CustomSelectWorldScreen(this));
            }
        }));

        // Botão MULTIPLAYER
        this.addRenderableWidget(new VHSButton(centerX, startY + 26, buttonWidth, buttonHeight,
                Component.literal("MULTIPLAYER"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new CustomJoinMultiplayerScreen(this));
            }
        }));

        // Botão OPTIONS
        this.addRenderableWidget(new VHSButton(centerX, startY + 52, buttonWidth, buttonHeight,
                Component.literal("OPTIONS"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options));
            }
        }));

        // Botão QUIT GAME
        this.addRenderableWidget(new VHSButton(centerX, startY + 78, buttonWidth, buttonHeight,
                Component.literal("QUIT GAME"), button -> {
            if (this.minecraft != null) {
                VHSScreenHelper.stopAmbienceSound();
                this.minecraft.stop();
            }
        }));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        VHSScreenHelper.renderVHSBackground(guiGraphics, this.width, this.height, "TAPE #001 - UNKNOWN");

        String title = "VHSWORLD - FOOTAGE";
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, (this.width - titleWidth) / 2, this.height / 2 - 50, 0xFFFF0000, false);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}