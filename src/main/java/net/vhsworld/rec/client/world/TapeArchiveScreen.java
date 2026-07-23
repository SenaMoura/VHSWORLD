package net.vhsworld.rec.client.world;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.vhsworld.rec.client.VHSButton;
import net.vhsworld.rec.client.VHSScreenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * O arquivo de fitas: a tela de escolher mundo, reescrita do zero.
 *
 * A anterior herdava de SelectWorldScreen e tentava esconder a lista do vanilla por
 * cima do nosso fundo — dai os textos sobrepostos. Aqui nada e herdado: a tela le os
 * mundos direto do disco e desenha as proprias fitas.
 *
 * O destaque no hover nao e so cosmetico. A miniatura do mundo NASCE do escuro
 * conforme o mouse fica em cima, como uma fita que precisa de um instante para dar
 * imagem. E o mesmo gesto da revelacao das fotos, aplicado ao menu.
 */
public class TapeArchiveScreen extends Screen {

    private static final Random RANDOM = new Random();
    private static final SimpleDateFormat STAMP = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private static final int ROW_H = 30;
    private static final int LIST_W = 250;
    private static final int PREVIEW_W = 176;
    private static final int PREVIEW_H = 99;

    private final Screen parent;
    private final List<TapeEntry> tapes = new ArrayList<>();

    private int selected = -1;
    private int hovered = -1;
    private int scroll = 0;
    private long lastFrame = System.currentTimeMillis();

    private VHSButton playButton;
    private VHSButton deleteButton;

    public TapeArchiveScreen(Screen parent) {
        super(Component.literal("TAPES ARCHIVE"));
        this.parent = parent;
    }

    // ------------------------------------------------------------------ dados

    private void loadTapes() {
        tapes.clear();
        if (minecraft == null) return;

        try {
            LevelStorageSource source = minecraft.getLevelSource();
            List<LevelSummary> summaries = source.loadLevelSummaries(source.findLevelCandidates()).join();
            for (LevelSummary summary : summaries) {
                tapes.add(new TapeEntry(summary));
            }
        } catch (Exception e) {
            // Sem mundos legiveis: a estante fica vazia, e a tela diz isso.
        }
    }

    private int visibleRows() {
        return Math.max(1, (listBottom() - listTop()) / ROW_H);
    }

    private int listTop() {
        return 74;
    }

    private int listBottom() {
        return height - 62;
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        loadTapes();

        int y = height - 50;
        int w = 128;
        int gap = 6;
        int totalW = w * 4 + gap * 3;
        int x = (width - totalW) / 2;

        playButton = new VHSButton(x, y, w, 22, Component.literal("REPRODUZIR"), b -> play());
        addRenderableWidget(playButton);

        addRenderableWidget(new VHSButton(x + (w + gap), y, w, 22,
                Component.literal("NOVA FITA"), b -> {
            if (minecraft != null) CreateWorldScreen.openFresh(minecraft, this);
        }));

        deleteButton = new VHSButton(x + (w + gap) * 2, y, w, 22,
                Component.literal("APAGAR"), b -> confirmDelete());
        addRenderableWidget(deleteButton);

        addRenderableWidget(new VHSButton(x + (w + gap) * 3, y, w, 22,
                Component.literal("VOLTAR"), b -> {
            if (minecraft != null) minecraft.setScreen(parent);
        }));

        updateButtons();
    }

    private void updateButtons() {
        boolean ok = selected >= 0 && selected < tapes.size();
        if (playButton != null) playButton.active = ok;
        if (deleteButton != null) deleteButton.active = ok;
    }

    // ------------------------------------------------------------------ acoes

    private void play() {
        if (minecraft == null || selected < 0 || selected >= tapes.size()) return;
        TapeEntry tape = tapes.get(selected);
        minecraft.createWorldOpenFlows().loadLevel(this, tape.id());
    }

    private void confirmDelete() {
        if (minecraft == null || selected < 0 || selected >= tapes.size()) return;
        TapeEntry tape = tapes.get(selected);

        minecraft.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) deleteTape(tape);
                    if (minecraft != null) minecraft.setScreen(this);
                },
                Component.literal("APAGAR A FITA?"),
                Component.literal("\"" + tape.name() + "\" nao volta. A gravacao se perde."),
                Component.literal("APAGAR"),
                Component.literal("CANCELAR")));
    }

    private void deleteTape(TapeEntry tape) {
        if (minecraft == null) return;
        try (LevelStorageSource.LevelStorageAccess access =
                     minecraft.getLevelSource().createAccess(tape.id())) {
            access.deleteLevel();
        } catch (Exception ignored) {
            // Mundo em uso ou sem permissao: nao apaga, e a lista se recarrega igual.
        }
        selected = -1;
        loadTapes();
        updateButtons();
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        VHSScreenHelper.renderVHSBackground(g, width, height, "TAPES ARCHIVE — " + tapes.size() + " GRAVACOES");

        float delta = (System.currentTimeMillis() - lastFrame) / 1000.0f;
        lastFrame = System.currentTimeMillis();
        delta = Mth.clamp(delta, 0.0f, 0.1f);

        int shift = VHSScreenHelper.glitchShift();
        g.pose().pushPose();
        g.pose().translate(shift, 0.0f, 0.0f);

        int listX = (width - LIST_W) / 2 - 90;
        hovered = -1;

        renderList(g, listX, mouseX, mouseY, delta);
        renderPreview(g, listX + LIST_W + 24);

        super.render(g, mouseX, mouseY, partialTick);
        g.pose().popPose();
    }

    private void renderList(GuiGraphics g, int x, int mouseX, int mouseY, float delta) {
        int top = listTop();
        int bottom = listBottom();

        if (tapes.isEmpty()) {
            String empty = "NENHUMA FITA GRAVADA";
            g.drawString(font, empty, x + (LIST_W - font.width(empty)) / 2, top + 20, 0xFF888888, false);
            return;
        }

        int rows = visibleRows();
        scroll = Mth.clamp(scroll, 0, Math.max(0, tapes.size() - rows));

        for (int i = 0; i < rows; i++) {
            int index = scroll + i;
            if (index >= tapes.size()) break;

            TapeEntry tape = tapes.get(index);
            int y = top + i * ROW_H;

            boolean isHover = mouseX >= x && mouseX < x + LIST_W && mouseY >= y && mouseY < y + ROW_H - 4;
            if (isHover) hovered = index;

            tape.advance(isHover || index == selected, delta);
            drawRow(g, tape, x, y, index == selected);
        }

        drawScrollbar(g, x + LIST_W + 4, top, bottom, rows);
    }

    /** Uma fita da estante: rotulo, nome e a data da ultima gravacao. */
    private void drawRow(GuiGraphics g, TapeEntry tape, int x, int y, boolean isSelected) {
        int h = ROW_H - 6;
        float lit = tape.highlight;

        int back = isSelected ? 0xCC1C1C22 : (int) (0x55 + lit * 0x55) << 24;
        g.fill(x, y, x + LIST_W, y + h, isSelected ? back : (back | 0x00121218));

        // Moldura: acende com o destaque
        int border = isSelected ? 0xFFFFFFFF
                : (0xFF000000 | (int) (0x44 + lit * 0x99) * 0x010101);
        g.fill(x, y, x + LIST_W, y + 1, border);
        g.fill(x, y + h - 1, x + LIST_W, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + LIST_W - 1, y, x + LIST_W, y + h, border);

        // Marca de seleção à esquerda, como o botão de play do aparelho
        if (isSelected || lit > 0.1f) {
            g.fill(x + 3, y + 6, x + 5, y + h - 6, 0xFFCC2222);
        }

        String name = tape.name();
        g.drawString(font, name, x + 12, y + 5, isSelected ? 0xFFFFFFFF : 0xFFCCCCCC, false);

        String info = STAMP.format(new Date(tape.summary.getLastPlayed()));
        g.drawString(font, info, x + 12, y + 15, 0xFF777777, false);
    }

    private void drawScrollbar(GuiGraphics g, int x, int top, int bottom, int rows) {
        if (tapes.size() <= rows) return;

        g.fill(x, top, x + 3, bottom, 0x55000000);

        float frac = rows / (float) tapes.size();
        int barH = Math.max(16, (int) ((bottom - top) * frac));
        int maxScroll = Math.max(1, tapes.size() - rows);
        int barY = top + (int) ((bottom - top - barH) * (scroll / (float) maxScroll));

        g.fill(x, barY, x + 3, barY + barH, 0xAAFFFFFF);
    }

    /**
     * A miniatura do mundo nascendo do escuro.
     *
     * Enquanto o destaque sobe, a imagem ganha alfa por cima de um retangulo de
     * chiado — a fita levando um instante para dar imagem. Sem mouse em cima, o
     * painel volta a ser um vazio com o texto de instrucao.
     */
    private void renderPreview(GuiGraphics g, int x) {
        int index = hovered >= 0 ? hovered : selected;

        int y = listTop();
        int w = PREVIEW_W;
        int h = PREVIEW_H;

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF333338);
        g.fill(x, y, x + w, y + h, 0xFF0A0A0E);

        if (index < 0 || index >= tapes.size()) {
            String hint = "PASSE O MOUSE";
            g.drawString(font, hint, x + (w - font.width(hint)) / 2, y + h / 2 - 4, 0xFF555555, false);
            return;
        }

        TapeEntry tape = tapes.get(index);
        float lit = tape.highlight;

        // Chiado por baixo, sempre — e o que a imagem "atravessa" ao aparecer
        int specks = (int) (140 * (1.0f - lit) + 20);
        for (int i = 0; i < specks; i++) {
            int px = x + RANDOM.nextInt(w);
            int py = y + RANDOM.nextInt(h);
            int a = 40 + RANDOM.nextInt(90);
            g.fill(px, py, px + 1 + RANDOM.nextInt(3), py + 1, (a << 24) | 0x00FFFFFF);
        }

        ResourceLocation icon = tape.icon();
        if (icon != null && lit > 0.01f) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, lit);
            g.blit(icon, x, y, 0.0f, 0.0f, w, h, w, h);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }

        // Ficha da fita embaixo da miniatura
        int textY = y + h + 8;
        g.drawString(font, tape.name(), x, textY, 0xFFDDDDDD, false);
        g.drawString(font, tape.summary.getInfo().getString(), x, textY + 12, 0xFF888888, false);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hovered >= 0) {
            boolean sameAgain = hovered == selected;
            selected = hovered;
            updateButtons();

            if (sameAgain) {
                play();       // segundo clique na mesma fita: toca
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scroll -= (int) Math.signum(amount);
        scroll = Mth.clamp(scroll, 0, Math.max(0, tapes.size() - visibleRows()));
        return true;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
