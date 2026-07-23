package net.vhsworld.rec.client.codex;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.vhsworld.rec.client.VHSButton;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * O registro (tecla G): o que cada item faz, como se consegue e a receita animada.
 *
 * A ficha tem duas camadas: nome, receita e "como conseguir" ficam abertos desde o
 * comeco, porque sao informacao mecanica e serve ANTES de o jogador ter o item — e
 * porque um item so-craftavel, trancado, nunca poderia ser descoberto. O flash abre
 * a outra camada: o que a coisa e de verdade.
 */
public class CodexScreen extends Screen {

    private static final Random RANDOM = new Random();

    private static final int SLOT = 18;
    private static final int GRID = SLOT * 3;

    private int selected = 0;
    private int ticks = 0;

    public CodexScreen() {
        super(Component.literal("REGISTRO"));
    }

    @Override
    protected void init() {
        addRenderableWidget(new VHSButton(width / 2 - 40, height - 34, 80, 20,
                Component.literal("FECHAR"), b -> onClose()));
    }

    @Override
    public void tick() {
        ticks++;
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xFF07070A);
        for (int y = 0; y < height; y += 3) {
            g.fill(0, y, width, y + 1, 0x44000000);
        }
        for (int i = 0; i < 30; i++) {
            int x = RANDOM.nextInt(width);
            int y = RANDOM.nextInt(height);
            g.fill(x, y, x + 1, y + 1, 0x22FFFFFF);
        }

        List<CodexEntry> entries = Codex.entries();

        g.drawString(font, "REGISTRO", 16, 14, 0xFFCCCCCC, false);
        g.drawString(font, known(entries) + "/" + entries.size() + " catalogados",
                16, 26, 0xFF777777, false);

        if (entries.isEmpty()) return;
        if (selected >= entries.size()) selected = 0;

        renderList(g, entries, mouseX, mouseY);
        renderEntry(g, entries.get(selected));

        super.render(g, mouseX, mouseY, partialTick);
    }

    private int known(List<CodexEntry> entries) {
        int n = 0;
        for (CodexEntry e : entries) if (Codex.get().isUnlocked(e.item)) n++;
        return n;
    }

    /** Coluna da esquerda: um icone por item, apagado enquanto a ficha esta trancada. */
    private void renderList(GuiGraphics g, List<CodexEntry> entries, int mouseX, int mouseY) {
        int x = 20;
        int y = 50;

        for (int i = 0; i < entries.size(); i++) {
            CodexEntry entry = entries.get(i);
            int slotY = y + i * (SLOT + 4);

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
    }

    /**
     * A ficha em duas camadas.
     *
     * ABERTO SEMPRE: nome, como conseguir e a receita. Sao informacao mecanica, e
     * informacao mecanica guardada atras do flash so chegaria depois de ja nao servir
     * — pior, um item que so existisse por craft nunca poderia ser descoberto.
     *
     * TRANCADO ATE O FLASH: o que a coisa é. A lore, o que ela faz com voce.
     * E o "learn MORE" da frase: o jogador nao fotografa para poder jogar, fotografa
     * porque quer saber.
     */
    private void renderEntry(GuiGraphics g, CodexEntry entry) {
        int x = 70;
        int y = 52;
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
     * A receita, animada.
     *
     * Cada casa mostra um dos itens que servem ali, trocando com o tempo — e assim que
     * o jogador ve que "qualquer tabua serve" sem precisar de uma lista. Uma linha de
     * varredura passa por cima para a coisa parecer um monitor lendo a fita, nao uma
     * tabela parada.
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

        // Fundo da grade
        g.fill(x - 2, top - 2, x + GRID + 2, top + GRID + 2, 0xFF1A1A20);

        int cycle = ticks / 20;   // troca de alternativa a cada segundo

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

        // Seta e resultado
        int arrowX = x + GRID + 8;
        int midY = top + GRID / 2 - 4;
        g.drawString(font, ">", arrowX, midY, 0xFFCCCCCC, false);

        int resultX = arrowX + 16;
        g.fill(resultX - 1, midY - 6, resultX + SLOT - 1, midY + SLOT - 8, 0xFF1A1A20);
        g.renderItem(recipe.getResultItem(minecraft.level.registryAccess()), resultX, midY - 5);

        // Linha de varredura descendo pela grade
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
        List<CodexEntry> entries = Codex.entries();
        int x = 20, y = 50;

        for (int i = 0; i < entries.size(); i++) {
            int slotY = y + i * (SLOT + 4);
            if (mouseX >= x && mouseX < x + SLOT && mouseY >= slotY && mouseY < slotY + SLOT) {
                selected = i;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
