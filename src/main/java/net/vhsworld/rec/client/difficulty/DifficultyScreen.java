package net.vhsworld.rec.client.difficulty;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.vhsworld.rec.client.VHSScreenHelper;
import net.vhsworld.rec.client.fx.TentacleFx;
import net.vhsworld.rec.item.ModSounds;

import java.util.Random;

/**
 * A tela que abre quando o jogador entra no mundo pela primeira vez.
 *
 * Preta, com chiado forte, e dois cards. Nao da para sair sem escolher (sem ESC, sem
 * clique fora): a fita ja comecou a rodar e a escolha e parte de ligar o aparelho.
 *
 * O chiado e um som em loop que so vive enquanto esta tela existe — ele para no
 * removed(), inclusive se o jogo fechar a tela por fora.
 */
public class DifficultyScreen extends Screen {

    private static final int CARD_W = 150;
    private static final int CARD_H = 118;
    private static final int CARD_GAP = 26;

    private static final Random RANDOM = new Random();

    private final float[] hover = new float[GameDifficulty.values().length];
    private final long openedAt = System.currentTimeMillis();

    private SimpleSoundInstance staticSound;

    /**
     * O deslocamento do glitch usado no ULTIMO desenho.
     *
     * O clique tem que usar o mesmo numero que o desenho usou; recalcular na hora do
     * clique pode cair fora da janela do glitch e a caixa do card ficaria alguns
     * pixels longe de onde o jogador enxergou.
     */
    private int drawnShift;

    public DifficultyScreen() {
        super(Component.translatable("recmod.difficulty.title"));
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        if (staticSound == null) {
            staticSound = new SimpleSoundInstance(
                    ModSounds.TAPE_STATIC.get().getLocation(), SoundSource.MASTER,
                    1.0f, 1.0f, RandomSource.create(), true, 0,
                    SimpleSoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true);
            mc.getSoundManager().play(staticSound);
        }
    }

    @Override
    public void removed() {
        Minecraft mc = Minecraft.getInstance();
        if (staticSound != null) {
            mc.getSoundManager().stop(staticSound);
            staticSound = null;
        }
        super.removed();
    }

    /** Escolher nao e algo que se adie: sem ESC e sem clicar fora. */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    /** Pausa o jogo de um jogador so: ninguem apanha enquanto le os cards. */
    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int shift = VHSScreenHelper.glitchShift();
        drawnShift = shift;

        renderStatic(g);

        g.pose().pushPose();
        g.pose().translate(shift, 0, 0);

        String title = Component.translatable("recmod.difficulty.title").getString();
        int tw = font.width(title);
        g.pose().pushPose();
        g.pose().translate(width / 2.0, height * 0.16, 0);
        g.pose().scale(2.0f, 2.0f, 1.0f);
        g.drawString(font, title, -tw / 2, 0, 0xFFDDDDDD, false);
        g.pose().popPose();

        String hint = Component.translatable("recmod.difficulty.hint").getString();
        g.drawString(font, hint, (width - font.width(hint)) / 2, (int) (height * 0.16) + 26,
                0xFF777777, false);

        GameDifficulty[] all = GameDifficulty.values();
        int totalW = all.length * CARD_W + (all.length - 1) * CARD_GAP;
        int startX = (width - totalW) / 2;
        int baseY = (height - CARD_H) / 2 + 14;

        for (int i = 0; i < all.length; i++) {
            int x = startX + i * (CARD_W + CARD_GAP);
            boolean over = mouseX >= x + shift && mouseX < x + shift + CARD_W
                    && mouseY >= baseY && mouseY < baseY + CARD_H;
            hover[i] += ((over ? 1f : 0f) - hover[i]) * 0.2f;
            renderCard(g, all[i], x, baseY, hover[i]);
        }

        g.pose().popPose();
    }

    /** Fundo: preto sujo, scanlines e MUITO chiado — mais denso que o do menu. */
    private void renderStatic(GuiGraphics g) {
        int tone = 4 + RANDOM.nextInt(5);
        g.fill(0, 0, width, height, (0xFF << 24) | (tone << 16) | (tone << 8) | (tone + 6));

        for (int y = 0; y < height; y += 2) {
            g.fill(0, y, width, y + 1, 0x77000000);
        }

        int grains = Math.max(700, (width * height) / 260);
        for (int i = 0; i < grains; i++) {
            int rx = RANDOM.nextInt(width);
            int ry = RANDOM.nextInt(height);
            int v = 90 + RANDOM.nextInt(166);
            int a = 60 + RANDOM.nextInt(120);
            int w = RANDOM.nextInt(10) == 0 ? 2 + RANDOM.nextInt(3) : 1;
            g.fill(rx, ry, rx + w, ry + 1, (a << 24) | (v << 16) | (v << 8) | v);
        }

        // Barras de tracking: faixas claras que descem, o defeito classico da fita.
        float t = (System.currentTimeMillis() - openedAt) / 1000.0f;
        for (int b = 0; b < 2; b++) {
            int by = (int) (((t * (38 + b * 27)) + b * height / 2f) % (height + 40)) - 20;
            int bh = 8 + b * 5;
            g.fill(0, by, width, by + bh, 0x14FFFFFF);
            g.fill(0, by + bh, width, by + bh + 1, 0x33FFFFFF);
        }
    }

    private void renderCard(GuiGraphics g, GameDifficulty d, int x, int baseY, float lift) {
        int y = baseY - Math.round(lift * 7);
        float t = (System.currentTimeMillis() - openedAt) / 50.0f;

        double wig = 0.6 + lift * 0.9;
        TentacleFx.cluster(g, x + CARD_W / 2.0, y - 2, Math.atan2(-1, 0), 2, 1.4, 24 + lift * 10, 6, t, wig);
        TentacleFx.cluster(g, x - 2, y + CARD_H * 0.5, Math.atan2(0, -1), 2, 1.2, 22 + lift * 10, 6, t + 15, wig);
        TentacleFx.cluster(g, x + CARD_W + 2, y + CARD_H * 0.5, Math.atan2(0, 1), 2, 1.2, 22 + lift * 10, 6, t + 30, wig);

        if (lift > 0.02f) {
            int a = (int) (lift * 90) << 24;
            g.fill(x - 3, y - 3, x + CARD_W + 3, y + CARD_H + 3, a | (d.accent & 0x00FFFFFF));
        }
        g.fill(x - 1, y - 1, x + CARD_W + 1, y + CARD_H + 1, d.accent);
        g.fill(x, y, x + CARD_W, y + CARD_H, 0xFF0B0B10);

        String name = Component.translatable(d.key).getString();
        g.pose().pushPose();
        g.pose().translate(x + CARD_W / 2.0, y + 14, 0);
        g.pose().scale(1.5f, 1.5f, 1f);
        g.drawString(font, name, -font.width(name) / 2, 0, d.accent, false);
        g.pose().popPose();

        // Descricao, quebrada na largura do card.
        int ty = y + 40;
        for (FormattedCharSequence line : font.split(Component.translatable(d.key + ".desc"), CARD_W - 20)) {
            g.drawString(font, line, x + 10, ty, 0xFF9A9AA2, false);
            ty += 10;
        }

        // A etiqueta do dificil: e o recado que o Pedro pediu, nao um enfeite.
        String tag = Component.translatable(d.key + ".tag").getString();
        if (!tag.isEmpty()) {
            int tagW = font.width(tag) + 8;
            int tagX = x + (CARD_W - tagW) / 2;
            int tagY = y + CARD_H - 20;
            g.fill(tagX, tagY, tagX + tagW, tagY + 12, 0x66000000 | (d.accent & 0x00FFFFFF));
            g.drawString(font, tag, tagX + 4, tagY + 2, 0xFF101010, false);
        }

        if (lift > 0.02f) {
            int scanY = y + (int) ((t * 2) % CARD_H);
            g.fill(x, scanY, x + CARD_W, scanY + 1, 0x55FFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int shift = drawnShift;
        GameDifficulty[] all = GameDifficulty.values();
        int totalW = all.length * CARD_W + (all.length - 1) * CARD_GAP;
        int startX = (width - totalW) / 2;
        int baseY = (height - CARD_H) / 2 + 14;

        for (int i = 0; i < all.length; i++) {
            int x = startX + i * (CARD_W + CARD_GAP) + shift;
            if (mouseX >= x && mouseX < x + CARD_W && mouseY >= baseY && mouseY < baseY + CARD_H) {
                pick(all[i]);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void pick(GameDifficulty difficulty) {
        DifficultyState.choose(difficulty);
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.MENU_BUTTON.get(), 1.0f));
        mc.setScreen(null);
    }
}
