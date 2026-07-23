package net.vhsworld.rec.client.photo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.vhsworld.rec.client.VHSButton;
import net.vhsworld.rec.config.RECConfig;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * O album de fotografias (tecla G).
 *
 * Duas telas em uma: a grade de miniaturas e, ao clicar numa foto, ela grande.
 * Foto nao revelada aparece como filme velado — chiado e nada mais. Revelar
 * leva alguns segundos de proposito: a espera e onde mora o medo.
 */
public class PhotoAlbumScreen extends Screen {

    private static final SimpleDateFormat STAMP = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private static final Random RANDOM = new Random();

    private static final int COLUMNS = 4;
    private static final int ROWS = 3;
    private static final int THUMB_W = 112;
    private static final int THUMB_H = 63;
    private static final int GAP = 10;

    private int page = 0;
    private Photo open = null;

    private VHSButton developButton;
    private VHSButton deleteButton;

    public PhotoAlbumScreen() {
        super(Component.literal("FOTOGRAFIAS"));
    }

    @Override
    protected void init() {
        clearWidgets();

        if (open == null) {
            int y = height - 34;

            addRenderableWidget(new VHSButton(width / 2 - 130, y, 80, 20,
                    Component.literal("<< ANTES"), b -> {
                if (page > 0) { page--; rebuild(); }
            }));

            addRenderableWidget(new VHSButton(width / 2 - 40, y, 80, 20,
                    Component.literal("FECHAR"), b -> onClose()));

            addRenderableWidget(new VHSButton(width / 2 + 50, y, 80, 20,
                    Component.literal("DEPOIS >>"), b -> {
                if ((page + 1) * perPage() < album().size()) { page++; rebuild(); }
            }));
        } else {
            int y = height - 34;

            developButton = new VHSButton(width / 2 - 130, y, 100, 20,
                    Component.literal("REVELAR"), b -> startDeveloping());
            developButton.active = !open.developed && open.developTicks == 0;
            addRenderableWidget(developButton);

            addRenderableWidget(new VHSButton(width / 2 - 20, y, 60, 20,
                    Component.literal("VOLTAR"), b -> { open = null; rebuild(); }));

            deleteButton = new VHSButton(width / 2 + 50, y, 80, 20,
                    Component.literal("QUEIMAR"), b -> {
                PhotoAlbum.get().delete(open);
                open = null;
                rebuild();
            });
            addRenderableWidget(deleteButton);
        }
    }

    private void rebuild() {
        init();
    }

    private List<Photo> album() {
        return PhotoAlbum.get().photos();
    }

    private int perPage() {
        return COLUMNS * ROWS;
    }

    private void startDeveloping() {
        if (open == null || open.developed) return;
        open.developTicks = 1;
        if (developButton != null) developButton.active = false;
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        if (open == null || open.developed || open.developTicks <= 0) return;

        int needed = Math.max(1, RECConfig.CLIENT.photoDevelopSeconds.get() * 20);
        open.developTicks++;

        if (open.developTicks >= needed) {
            open.developed = true;
            PhotoAlbum.get().save();
        }
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        drawBackdrop(g);
        grain(g);

        if (open == null) {
            renderGrid(g, mouseX, mouseY);
        } else {
            renderOpen(g);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawBackdrop(GuiGraphics g) {
        g.fill(0, 0, width, height, 0xFF07070A);
        for (int y = 0; y < height; y += 3) {
            g.fill(0, y, width, y + 1, 0x44000000);
        }
    }

    /** Um pouco de sujeira, para a tela nao parecer um menu limpo de jogo. */
    private void grain(GuiGraphics g) {
        for (int i = 0; i < 40; i++) {
            int x = RANDOM.nextInt(width);
            int y = RANDOM.nextInt(height);
            g.fill(x, y, x + 1 + RANDOM.nextInt(2), y + 1, 0x22FFFFFF);
        }
    }

    private void renderGrid(GuiGraphics g, int mouseX, int mouseY) {
        List<Photo> photos = album();

        g.drawString(font, "FOTOGRAFIAS", 16, 14, 0xFFCCCCCC, false);
        g.drawString(font, photos.size() + " no carretel", 16, 26, 0xFF777777, false);

        if (photos.isEmpty()) {
            String empty = "NENHUMA FOTO. SEGURE R PARA DISPARAR O FLASH.";
            g.drawString(font, empty, (width - font.width(empty)) / 2, height / 2 - 4, 0xFF888888, false);
            return;
        }

        int gridW = COLUMNS * THUMB_W + (COLUMNS - 1) * GAP;
        int gridH = ROWS * THUMB_H + (ROWS - 1) * GAP;
        int startX = (width - gridW) / 2;
        int startY = (height - gridH) / 2 - 6;

        int first = page * perPage();

        for (int i = 0; i < perPage(); i++) {
            int index = first + i;
            if (index >= photos.size()) break;

            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int x = startX + col * (THUMB_W + GAP);
            int y = startY + row * (THUMB_H + GAP);

            boolean hover = mouseX >= x && mouseX < x + THUMB_W && mouseY >= y && mouseY < y + THUMB_H;
            drawPhoto(g, photos.get(index), x, y, THUMB_W, THUMB_H, hover);
        }

        int pages = (photos.size() + perPage() - 1) / perPage();
        String label = "CARRETEL " + (page + 1) + "/" + pages;
        g.drawString(font, label, (width - font.width(label)) / 2, height - 48, 0xFF777777, false);
    }

    private void renderOpen(GuiGraphics g) {
        int w = 384;
        int h = 216;
        int x = (width - w) / 2;
        int y = (height - h) / 2 - 14;

        drawPhoto(g, open, x, y, w, h, false);

        String stamp = STAMP.format(new Date(open.takenAt));
        g.drawString(font, stamp, x, y - 12, 0xFF777777, false);

        String status;
        int color;
        if (open.broken) {
            status = "FILME PERDIDO";
            color = 0xFFAA3333;
        } else if (open.developed) {
            status = open.subject == null
                    ? "NADA FOI CAPTURADO"
                    : "CAPTURADO: " + open.subject.toUpperCase();
            color = open.subject == null ? 0xFF777777 : 0xFFCC3333;
        } else if (open.developTicks > 0) {
            int needed = Math.max(1, RECConfig.CLIENT.photoDevelopSeconds.get() * 20);
            int pct = Math.min(100, open.developTicks * 100 / needed);
            status = "REVELANDO... " + pct + "%";
            color = 0xFFCCCC55;
        } else {
            status = "NAO REVELADA";
            color = 0xFF888888;
        }
        g.drawString(font, status, x, y + h + 6, color, false);
    }

    /**
     * Desenha uma foto. Se ainda nao foi revelada, desenha o filme velado no lugar
     * da imagem — o arquivo existe, mas o jogador ainda nao tem direito de ver.
     */
    private void drawPhoto(GuiGraphics g, Photo photo, int x, int y, int w, int h, boolean hover) {
        int border = hover ? 0xFFFFFFFF : 0xFF555555;
        g.fill(x - 1, y - 1, x + w + 1, y, border);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, border);
        g.fill(x - 1, y, x, y + h, border);
        g.fill(x + w, y, x + w + 1, y + h, border);

        if (photo.developed && !photo.broken) {
            ResourceLocation tex = PhotoAlbum.get().texture(photo);
            if (tex != null) {
                g.blit(tex, x, y, 0.0f, 0.0f, w, h, w, h);
                return;
            }
        }

        // Filme velado: escuro com chiado. Nao e a foto — e a ausencia dela.
        g.fill(x, y, x + w, y + h, 0xFF101014);
        int specks = w * h / 260;
        for (int i = 0; i < specks; i++) {
            int px = x + RANDOM.nextInt(w);
            int py = y + RANDOM.nextInt(h);
            int a = 30 + RANDOM.nextInt(90);
            g.fill(px, py, px + 1, py + 1, (a << 24) | 0x00FFFFFF);
        }

        if (photo.broken) {
            String lost = "X";
            g.drawString(font, lost, x + (w - font.width(lost)) / 2, y + h / 2 - 4, 0xFFAA3333, false);
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (open == null && button == 0) {
            List<Photo> photos = album();

            int gridW = COLUMNS * THUMB_W + (COLUMNS - 1) * GAP;
            int gridH = ROWS * THUMB_H + (ROWS - 1) * GAP;
            int startX = (width - gridW) / 2;
            int startY = (height - gridH) / 2 - 6;
            int first = page * perPage();

            for (int i = 0; i < perPage(); i++) {
                int index = first + i;
                if (index >= photos.size()) break;

                int x = startX + (i % COLUMNS) * (THUMB_W + GAP);
                int y = startY + (i / COLUMNS) * (THUMB_H + GAP);

                if (mouseX >= x && mouseX < x + THUMB_W && mouseY >= y && mouseY < y + THUMB_H) {
                    open = photos.get(index);
                    rebuild();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;   // o mundo continua correndo enquanto voce olha as fotos
    }
}
