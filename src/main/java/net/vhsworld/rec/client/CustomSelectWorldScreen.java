package net.vhsworld.rec.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

import java.util.ArrayList;
import java.util.List;

public class CustomSelectWorldScreen extends SelectWorldScreen {

    private final List<VHSButtonPair> customButtons = new ArrayList<>();

    public CustomSelectWorldScreen(Screen lastScreen) {
        super(lastScreen);
    }

    private record VHSButtonPair(Button vanillaButton, VHSButton customButton) {}

    @Override
    protected void init() {
        super.init();
        customButtons.clear();

        for (var child : this.children()) {
            if (child instanceof AbstractSelectionList<?> list) {
                list.setRenderBackground(false);
                list.setRenderTopAndBottom(false);
                list.updateSize(this.width, this.height, 48, this.height - 62);
            }
        }

        List<Button> vanillaButtons = new ArrayList<>();
        for (var child : this.children()) {
            if (child instanceof Button button) {
                vanillaButtons.add(button);
            }
        }

        for (Button btn : vanillaButtons) {
            this.removeWidget(btn);
        }

        int buttonWidth = 130;
        int buttonHeight = 20;
        int spacing = 8;
        int startY = this.height - 52;

        int totalWidth = (buttonWidth * 3) + (spacing * 2);
        int startX = (this.width - totalWidth) / 2;

        for (int i = 0; i < vanillaButtons.size(); i++) {
            Button original = vanillaButtons.get(i);

            int row = i / 3;
            int col = i % 3;

            int x = startX + (col * (buttonWidth + spacing));
            int y = startY + (row * (buttonHeight + 4));

            VHSButton vhsBtn = new VHSButton(x, y, buttonWidth, buttonHeight, original.getMessage(), b -> original.onPress());
            this.addRenderableWidget(vhsBtn);

            customButtons.add(new VHSButtonPair(original, vhsBtn));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        VHSScreenHelper.renderVHSBackground(guiGraphics, this.width, this.height, "TAPES ARCHIVE - SELECT RECORDING");

        for (VHSButtonPair pair : customButtons) {
            pair.customButton().active = pair.vanillaButton().active;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        VHSScreenHelper.ensureAmbienceSound();
    }
}