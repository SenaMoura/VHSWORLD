package net.vhsworld.rec.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.menu.RFReceiverMenu;

/** A tela do Receptor de Frequencia: painel de aparelho velho, grade 3x3 -> resultado. */
public class RFReceiverScreen extends AbstractContainerScreen<RFReceiverMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(RECMod.MOD_ID, "textures/gui/rf_receiver.png");

    public RFReceiverScreen(RFReceiverMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }
}
