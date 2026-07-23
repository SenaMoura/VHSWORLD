package net.vhsworld.rec.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;

import java.util.ArrayList;
import java.util.List;

public class CustomJoinMultiplayerScreen extends JoinMultiplayerScreen {

    private final List<VHSButtonPair> customButtons = new ArrayList<>();

    public CustomJoinMultiplayerScreen(Screen lastScreen) {
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

        int buttonWidth = 120;
        int buttonHeight = 20;
        int spacing = 6;
        int startY = this.height - 52;

        int totalWidth = (buttonWidth * 4) + (spacing * 3);
        int startX = (this.width - totalWidth) / 2;

        for (int i = 0; i < vanillaButtons.size(); i++) {
            Button original = vanillaButtons.get(i);

            int row = i / 4;
            int col = i % 4;

            int x = startX + (col * (buttonWidth + spacing));
            int y = startY + (row * (buttonHeight + 4));

            VHSButton vhsBtn = new VHSButton(x, y, buttonWidth, buttonHeight, original.getMessage(), b -> original.onPress());
            this.addRenderableWidget(vhsBtn);

            customButtons.add(new VHSButtonPair(original, vhsBtn));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        VHSScreenHelper.renderVHSBackground(guiGraphics, this.width, this.height, "SIGNAL SEARCH - MULTIPLAYER");

        for (VHSButtonPair pair : customButtons) {
            pair.customButton().active = pair.vanillaButton().active;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        VHSScreenHelper.ensureAmbienceSound();
    }
}