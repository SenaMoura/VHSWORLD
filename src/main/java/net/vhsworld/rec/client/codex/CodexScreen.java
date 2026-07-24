package net.vhsworld.rec.client.codex;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.vhsworld.rec.client.VHSButton;
import net.vhsworld.rec.client.fx.TentacleFx;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * O registro (tecla G): o que cada item faz, como se consegue e a receita animada.
 *
 * Duas telas. A de fora sao TRES cards — camera, para matar, para sobreviver — com
 * tentaculos nos cantos e em volta de cada um; passar o mouse levanta o card e acelera
 * os tentaculos. Escolher um card entra na lista daquela categoria. A divisao existe por
 * um motivo pratico: a lista unica de todos os itens vazava pela borda de baixo e escondia
 * as receitas. Cada categoria e curta, e a lista ainda rola com a roda por garantia.
 *
 * A ficha tem duas camadas: nome, receita e "como conseguir" ficam abertos desde o comeco,
 * porque sao informacao mecanica e servem ANTES de o jogador ter o item — e porque um item
 * so-craftavel, trancado, nunca poderia ser descoberto. O flash abre a outra camada: o que
 * a coisa e de verdade.
 */
public class CodexScreen extends Screen {

    private static final Random RANDOM = new Random();

    private static final int SLOT = 18;
    private static final int GRID = SLOT * 3;

    private static final int CARD_W = 118;
    private static final int CARD_H = 152;
    private static final int CARD_GAP = 22;

    private static final int ROW_H = SLOT + 4;

    /** null = na tela de cards; senao, na lista da categoria escolhida. */
    private CodexCategory category = null;

    private int selected = 0;
    private int scroll = 0;
    private int ticks = 0;
    private float partial = 0f;

    /** Quanto cada card esta "levantado" pelo hover (0..1), suavizado por frame. */
    private final float[] cardHover = new float[CodexCategory.values().length];

    public CodexScreen() {
        super(Component.translatable("recmod.codex.title"));
    }

    @Override
    protected void init() {
        addRenderableWidget(new VHSButton(width / 2 - 40, height - 30, 80, 20,
                Component.literal("FECHAR"), b -> onClose()));

        // So dentro de uma categoria: um VOLTAR para os cards.
        if (category != null) {
            addRenderableWidget(new VHSButton(20, height - 30, 80, 20,
                    Component.translatable("recmod.codex.back"), b -> openCards()));
        }
    }

    private void openCards() {
        category = null;
        selected = 0;
        scroll = 0;
        rebuildWidgets();
    }

    private void openCategory(CodexCategory cat) {
        category = cat;
        selected = 0;
        scroll = 0;
        rebuildWidgets();
    }

    @Override
    public void tick() {
        ticks++;
    }

    private float time() {
        return ticks + partial;
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.partial = partialTick;

        // Fundo VHS: preto esverdeado, scanlines e uma poeira de estatica.
        g.fill(0, 0, width, height, 0xFF07070A);
        for (int y = 0; y < height; y += 3) {
            g.fill(0, y, width, y + 1, 0x44000000);
        }
        for (int i = 0; i < 30; i++) {
            int x = RANDOM.nextInt(width);
            int y = RANDOM.nextInt(height);
            g.fill(x, y, x + 1, y + 1, 0x22FFFFFF);
        }

        cornerTentacles(g);

        g.drawString(font, Component.translatable("recmod.codex.title").getString(),
                16, 14, 0xFFCCCCCC, false);

        if (category == null) {
            renderCards(g, mouseX, mouseY);
        } else {
            renderCategory(g, mouseX, mouseY);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    /** Tentaculos rastejando para dentro a partir dos quatro cantos da tela. */
    private void cornerTentacles(GuiGraphics g) {
        float t = time();
        double len = Math.min(width, height) * 0.42;
        TentacleFx.cluster(g, -6, -6, Math.atan2(1, 1), 3, 1.1, len, 7, t, 1.0);
        TentacleFx.cluster(g, width + 6, -6, Math.atan2(1, -1), 3, 1.1, len, 7, t + 40, 1.0);
        TentacleFx.cluster(g, -6, height + 6, Math.atan2(-1, 1), 3, 1.1, len, 7, t + 80, 1.0);
        TentacleFx.cluster(g, width + 6, height + 6, Math.atan2(-1, -1), 3, 1.1, len, 7, t + 120, 1.0);
    }

    // ------------------------------------------------------------------ cards

    private void renderCards(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, Component.translatable("recmod.codex.cards.hint").getString(),
                16, 26, 0xFF777777, false);

        CodexCategory[] cats = CodexCategory.values();
        int totalW = cats.length * CARD_W + (cats.length - 1) * CARD_GAP;
        int startX = (width - totalW) / 2;
        int baseY = (height - CARD_H) / 2 + 6;

        for (int i = 0; i < cats.length; i++) {
            int x = startX + i * (CARD_W + CARD_GAP);
            boolean hover = mouseX >= x && mouseX < x + CARD_W && mouseY >= baseY && mouseY < baseY + CARD_H;

            // Suaviza o hover: alvo 1 quando em cima, 0 fora; ~15%/frame.
            cardHover[i] += ((hover ? 1f : 0f) - cardHover[i]) * 0.2f;
            renderCard(g, cats[i], x, baseY, cardHover[i]);
        }
    }

    private void renderCard(GuiGraphics g, CodexCategory cat, int x, int baseY, float hover) {
        int lift = Math.round(hover * 7);
        int y = baseY - lift;
        float t = time();

        // Tentaculos em volta do card (mais soltos com o hover).
        double wig = 0.6 + hover * 0.9;
        TentacleFx.cluster(g, x + CARD_W / 2.0, y - 2, Math.atan2(-1, 0), 2, 1.4, 26 + hover * 10, 6, t, wig);
        TentacleFx.cluster(g, x - 2, y + CARD_H * 0.35, Math.atan2(0, -1), 2, 1.2, 24 + hover * 10, 6, t + 15, wig);
        TentacleFx.cluster(g, x + CARD_W + 2, y + CARD_H * 0.65, Math.atan2(0, 1), 2, 1.2, 24 + hover * 10, 6, t + 30, wig);
        TentacleFx.cluster(g, x + CARD_W / 2.0, y + CARD_H + 2, Math.atan2(1, 0), 2, 1.4, 26 + hover * 10, 6, t + 45, wig);

        // Corpo do card + brilho de contorno no hover.
        int glow = 0x22000000 | (cat.accent & 0x00FFFFFF);
        if (hover > 0.02f) {
            int a = (int) (hover * 90) << 24;
            g.fill(x - 3, y - 3, x + CARD_W + 3, y + CARD_H + 3, a | (cat.accent & 0x00FFFFFF));
        }
        g.fill(x - 1, y - 1, x + CARD_W + 1, y + CARD_H + 1, cat.accent);
        g.fill(x, y, x + CARD_W, y + CARD_H, 0xFF101016);

        // Titulo da categoria.
        String title = Component.translatable(cat.titleKey).getString();
        int tw = font.width(title);
        g.drawString(font, title, x + (CARD_W - tw) / 2, y + 12, cat.accent, false);

        // Capa: o primeiro item da categoria, ampliado ao centro.
        List<CodexEntry> entries = Codex.entries(cat);
        if (!entries.isEmpty()) {
            ItemStack cover = new ItemStack(entries.get(0).item);
            float s = 3.0f + hover * 0.5f;
            g.pose().pushPose();
            g.pose().translate(x + CARD_W / 2.0, y + CARD_H / 2.0 - 6, 0);
            g.pose().scale(s, s, 1f);
            g.renderItem(cover, -8, -8);
            g.pose().popPose();

            // Uma fileira dos proximos icones, como um carretel.
            int previewN = Math.min(4, entries.size());
            int pw = previewN * 12;
            int px = x + (CARD_W - pw) / 2;
            int py = y + CARD_H - 40;
            for (int i = 0; i < previewN; i++) {
                g.pose().pushPose();
                g.pose().translate(px + i * 12, py, 0);
                g.pose().scale(0.66f, 0.66f, 1f);
                g.renderItem(new ItemStack(entries.get(i).item), 0, 0);
                g.pose().popPose();
            }
        }

        // Progresso da categoria.
        int known = known(entries);
        String count = Component.translatable("recmod.codex.count", known, entries.size()).getString();
        int cw = font.width(count);
        g.drawString(font, count, x + (CARD_W - cw) / 2, y + CARD_H - 16, 0xFF888888, false);

        // Varredura descendo, so no card sob o mouse.
        if (hover > 0.02f) {
            int scanY = y + (int) ((t * 2) % CARD_H);
            g.fill(x, scanY, x + CARD_W, scanY + 1, 0x55FFFFFF);
        }
    }

    // ------------------------------------------------------------------ categoria

    private void renderCategory(GuiGraphics g, int mouseX, int mouseY) {
        List<CodexEntry> entries = Codex.entries(category);

        String title = Component.translatable(category.titleKey).getString();
        g.drawString(font, title, 16, 26, category.accent, false);
        g.drawString(font, Component.translatable("recmod.codex.count", known(entries), entries.size()).getString(),
                16 + font.width(title) + 10, 26, 0xFF777777, false);

        if (entries.isEmpty()) return;
        if (selected >= entries.size()) selected = 0;

        renderList(g, entries, mouseX, mouseY);
        renderEntry(g, entries.get(selected));
    }

    private int known(List<CodexEntry> entries) {
        int n = 0;
        for (CodexEntry e : entries) if (Codex.get().isUnlocked(e.item)) n++;
        return n;
    }

    private int listTop() { return 46; }
    private int listBottom() { return height - 38; }

    private int maxScroll(int count) {
        return Math.max(0, count * ROW_H - (listBottom() - listTop()));
    }

    /** Coluna da esquerda: um icone por item, apagado enquanto a ficha esta trancada. */
    private void renderList(GuiGraphics g, List<CodexEntry> entries, int mouseX, int mouseY) {
        int x = 20;
        int top = listTop();
        int bottom = listBottom();

        scroll = Mth.clamp(scroll, 0, maxScroll(entries.size()));

        g.enableScissor(0, top, x + SLOT + 6, bottom);
        for (int i = 0; i < entries.size(); i++) {
            CodexEntry entry = entries.get(i);
            int slotY = top + i * ROW_H - scroll;
            if (slotY + SLOT < top || slotY > bottom) continue;

            boolean hover = mouseX >= x && mouseX < x + SLOT && mouseY >= slotY && mouseY < slotY + SLOT;
            int border = (i == selected) ? 0xFFFFFFFF : (hover ? 0xFFAAAAAA : 0xFF444444);

            g.fill(x - 1, slotY - 1, x + SLOT + 1, slotY + SLOT + 1, border);
            g.fill(x, slotY, x + SLOT, slotY + SLOT, 0xFF121216);

            // O item aparece sempre: o que se esconde e o que ele E, nao que ele existe.
            g.renderItem(new ItemStack(entry.item), x + 1, slotY + 1);
            if (!Codex.get().isUnlocked(entry.item)) {
                g.fill(x, slotY, x + SLOT, slotY + SLOT, 0x99000000);
            }
        }
        g.disableScissor();

        // Barra de rolagem, so quando ha o que rolar.
        int max = maxScroll(entries.size());
        if (max > 0) {
            int trackH = bottom - top;
            int barH = Math.max(12, trackH * (bottom - top) / (entries.size() * ROW_H));
            int barY = top + (trackH - barH) * scroll / max;
            g.fill(x + SLOT + 3, top, x + SLOT + 5, bottom, 0xFF1A1A20);
            g.fill(x + SLOT + 3, barY, x + SLOT + 5, barY + barH, 0xFF555560);
        }
    }

    private void renderEntry(GuiGraphics g, CodexEntry entry) {
        int x = 64;
        int y = 48;
        int wrap = width - x - 30;

        boolean known = Codex.get().isUnlocked(entry.item);

        g.drawString(font, new ItemStack(entry.item).getHoverName().getString().toUpperCase(),
                x, y, 0xFFCCCCCC, false);

        int cursor = y + 16;

        if (known) {
            cursor = drawWrapped(g, Component.translatable(entry.descKey()).getString(),
                    x, cursor, wrap, 0xFFAAAAAA) + 10;
        } else {
            g.drawString(font, "ANALISE PENDENTE", x, cursor, 0xFF886644, false);
            cursor = drawWrapped(g, Component.translatable("recmod.tooltip.locked").getString(),
                    x, cursor + 12, wrap, 0xFF666666) + 10;
        }

        g.drawString(font, "COMO CONSEGUIR", x, cursor, 0xFF777777, false);
        cursor = drawWrapped(g, Component.translatable(entry.obtainKey()).getString(),
                x, cursor + 12, wrap, 0xFFAAAAAA) + 14;

        renderRecipe(g, entry, x, cursor);
    }

    /**
     * A receita, animada. Cada casa mostra um dos itens que servem ali, trocando com o
     * tempo — e assim que o jogador ve que "qualquer tabua serve" sem uma lista. Uma linha
     * de varredura passa por cima para parecer um monitor lendo a fita, nao uma tabela parada.
     */
    private void renderRecipe(GuiGraphics g, CodexEntry entry, int x, int y) {
        if (entry.recipe == null || minecraft == null || minecraft.level == null) return;

        Optional<? extends Recipe<?>> found = minecraft.level.getRecipeManager().byKey(entry.recipe);
        if (found.isEmpty()) {
            g.drawString(font, "SEM RECEITA CONHECIDA", x, y, 0xFF777777, false);
            return;
        }

        Recipe<?> recipe = found.get();
        NonNullList<Ingredient> ingredients = recipe.getIngredients();

        int gridW = 3, gridH = 3;
        if (recipe instanceof ShapedRecipe shaped) {
            gridW = shaped.getWidth();
            gridH = shaped.getHeight();
        }

        g.drawString(font, "RECEITA", x, y, 0xFF777777, false);
        int top = y + 12;

        g.fill(x - 2, top - 2, x + GRID + 2, top + GRID + 2, 0xFF1A1A20);

        int cycle = ticks / 20;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = x + col * SLOT;
                int sy = top + row * SLOT;

                g.fill(sx, sy, sx + SLOT - 2, sy + SLOT - 2, 0xFF0E0E12);

                if (col >= gridW || row >= gridH) continue;

                int index = row * gridW + col;
                if (index >= ingredients.size()) continue;

                ItemStack[] options = ingredients.get(index).getItems();
                if (options.length == 0) continue;

                g.renderItem(options[cycle % options.length], sx + 1, sy + 1);
            }
        }

        int arrowX = x + GRID + 8;
        int midY = top + GRID / 2 - 4;
        g.drawString(font, ">", arrowX, midY, 0xFFCCCCCC, false);

        int resultX = arrowX + 16;
        g.fill(resultX - 1, midY - 6, resultX + SLOT - 1, midY + SLOT - 8, 0xFF1A1A20);
        g.renderItem(recipe.getResultItem(minecraft.level.registryAccess()), resultX, midY - 5);

        int scanY = top + (ticks * 2) % GRID;
        g.fill(x - 2, scanY, x + GRID + 2, scanY + 1, 0x66FFFFFF);
    }

    private int drawWrapped(GuiGraphics g, String text, int x, int y, int maxWidth, int color) {
        for (var line : font.split(Component.literal(text), maxWidth)) {
            g.drawString(font, line, x, y, color, false);
            y += 11;
        }
        return y;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (category == null) {
            CodexCategory[] cats = CodexCategory.values();
            int totalW = cats.length * CARD_W + (cats.length - 1) * CARD_GAP;
            int startX = (width - totalW) / 2;
            int baseY = (height - CARD_H) / 2 + 6;
            for (int i = 0; i < cats.length; i++) {
                int x = startX + i * (CARD_W + CARD_GAP);
                if (mouseX >= x && mouseX < x + CARD_W && mouseY >= baseY && mouseY < baseY + CARD_H) {
                    openCategory(cats[i]);
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        List<CodexEntry> entries = Codex.entries(category);
        int x = 20;
        int top = listTop();
        int bottom = listBottom();
        for (int i = 0; i < entries.size(); i++) {
            int slotY = top + i * ROW_H - scroll;
            if (slotY + SLOT < top || slotY > bottom) continue;
            if (mouseX >= x && mouseX < x + SLOT && mouseY >= slotY && mouseY < slotY + SLOT) {
                selected = i;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (category != null) {
            int count = Codex.entries(category).size();
            scroll = Mth.clamp(scroll - (int) (delta * ROW_H), 0, maxScroll(count));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
