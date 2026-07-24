package net.vhsworld.rec.client.tape;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.vhsworld.rec.client.VHSButton;

import java.util.List;
import java.util.Random;

/**
 * O videocassete: rever as fitas gravadas.
 *
 * A esquerda, a pilha de fitas do mundo. A direita, a selecionada rodando em loop no
 * ritmo em que foi gravada — e aqui, em seguranca, que o jogador finalmente ve o que a
 * fita pegou. A tela veste a fita de VHS (tarjas, scanlines, PLAY e timecode) para o
 * momento parecer um aparelho velho lendo a cassete, nao um visualizador de imagens.
 */
public class TapeVaultScreen extends Screen {

    private static final Random RANDOM = new Random();

    private List<TapeLibrary.Reel> reels;
    private int selected = -1;
    private int playTicks = 0;

    public TapeVaultScreen() {
        super(Component.literal("VIDEOCASSETE"));
    }

    @Override
    protected void init() {
        reels = TapeLibrary.list();
        if (!reels.isEmpty() && selected < 0) selected = 0;

        addRenderableWidget(new VHSButton(width / 2 - 40, height - 30, 80, 20,
                Component.literal("EJECT"), b -> onClose()));

        if (selected >= 0) {
            addRenderableWidget(new VHSButton(width - 100, height - 30, 80, 20,
                    Component.literal("APAGAR"), b -> deleteSelected()));
        }
    }

    private void deleteSelected() {
        if (reels == null || selected < 0 || selected >= reels.size()) return;
        reels.get(selected).deleteFromDisk();
        reels = TapeLibrary.list();
        selected = reels.isEmpty() ? -1 : Math.min(selected, reels.size() - 1);
        playTicks = 0;
        rebuildWidgets();
    }

    @Override
    public void tick() {
        playTicks++;
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF07070A);
        for (int y = 0; y < height; y += 3) g.fill(0, y, width, y + 1, 0x44000000);

        g.drawString(font, "VIDEOCASSETE", 16, 14, 0xFFCCCCCC, false);

        if (reels == null || reels.isEmpty()) {
            String empty = "NENHUMA FITA GRAVADA";
            g.drawString(font, empty, (width - font.width(empty)) / 2, height / 2, 0xFF777777, false);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        renderList(g, mouseX, mouseY);
        renderPlayer(g);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        int x = 16;
        int top = 40;
        int rowH = 22;

        for (int i = 0; i < reels.size(); i++) {
            TapeLibrary.Reel reel = reels.get(i);
            int y = top + i * rowH;
            if (y > height - 44) break;

            boolean hover = mouseX >= x && mouseX < x + 130 && mouseY >= y && mouseY < y + rowH - 2;
            int border = (i == selected) ? 0xFFFFFFFF : (hover ? 0xFFAAAAAA : 0xFF444444);
            g.fill(x - 1, y - 1, x + 130 + 1, y + rowH - 2 + 1, border);
            g.fill(x, y, x + 130, y + rowH - 2, 0xFF121216);

            g.drawString(font, "FITA " + (reels.size() - i), x + 5, y + 3, 0xFFCCCCCC, false);
            g.drawString(font, "0:" + String.format("%02d", reel.seconds()), x + 5, y + 12, 0xFF777777, false);
        }
    }

    private void renderPlayer(GuiGraphics g) {
        if (selected < 0 || selected >= reels.size()) return;
        TapeLibrary.Reel reel = reels.get(selected);

        int panelX = 160;
        int panelW = width - panelX - 24;
        int panelH = panelW * 9 / 16;
        int panelY = 48;

        // Moldura + tarjas de cinema.
        g.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + panelH + 2, 0xFF2A2A32);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF000000);

        int frameIndex = (playTicks / Math.max(1, reel.frameEvery)) % Math.max(1, reel.frames);
        ResourceLocation tex = reel.frame(frameIndex);
        if (tex != null) {
            g.blit(tex, panelX, panelY, 0.0f, 0.0f, panelW, panelH, panelW, panelH);
        } else {
            // Sem quadro: chiado.
            for (int i = 0; i < 200; i++) {
                int px = panelX + RANDOM.nextInt(panelW);
                int py = panelY + RANDOM.nextInt(panelH);
                g.fill(px, py, px + 1, py + 1, 0x55FFFFFF);
            }
        }

        // Scanlines por cima do quadro.
        for (int y = panelY; y < panelY + panelH; y += 3) {
            g.fill(panelX, y, panelX + panelW, y + 1, 0x33000000);
        }

        // PLAY e timecode.
        boolean blink = (playTicks / 10) % 2 == 0;
        if (blink) g.drawString(font, "▶ PLAY", panelX + 4, panelY + 4, 0xFFFF3030, true);

        int curSec = frameIndex * reel.frameEvery / 20;
        String tc = String.format("0:%02d / 0:%02d", curSec, reel.seconds());
        g.drawString(font, tc, panelX + panelW - font.width(tc) - 4, panelY + panelH - 12, 0xFFFFFFFF, true);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (reels != null) {
            int x = 16, top = 40, rowH = 22;
            for (int i = 0; i < reels.size(); i++) {
                int y = top + i * rowH;
                if (mouseX >= x && mouseX < x + 130 && mouseY >= y && mouseY < y + rowH - 2) {
                    if (selected != i) {
                        selected = i;
                        playTicks = 0;
                        rebuildWidgets();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (reels != null) {
            for (TapeLibrary.Reel reel : reels) reel.releaseTextures();
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
