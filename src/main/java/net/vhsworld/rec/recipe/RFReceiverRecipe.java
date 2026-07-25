package net.vhsworld.rec.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.vhsworld.rec.init.ModRecipes;

import java.util.ArrayList;
import java.util.List;

/**
 * A receita do Receptor de Frequencia: informe (sem forma) uma lista de
 * ingredientes COM quantidade — algo que a receita shapeless do vanilla nao faz
 * (la todo ingrediente vale 1). Aqui "4x Residuo de Estatica" e um numero, nao
 * quatro slots.
 *
 * Casa por alocacao gulosa: para cada ingrediente exigido, tira a contagem dele
 * dos stacks da grade; no fim, a grade tem que ficar VAZIA — assim nao da para
 * fundir com lixo sobrando junto. Os itens do mod tem tipos distintos, entao a
 * ganancia nunca aloca errado.
 */
public class RFReceiverRecipe implements Recipe<CraftingContainer> {

    public record Entry(Ingredient ingredient, int count) {}

    private final ResourceLocation id;
    private final List<Entry> entries;
    private final ItemStack result;

    public RFReceiverRecipe(ResourceLocation id, List<Entry> entries, ItemStack result) {
        this.id = id;
        this.entries = entries;
        this.result = result;
    }

    public List<Entry> entries() {
        return entries;
    }

    /** Quanto de cada slot da grade a receita consome, na ordem dos slots. Usado ao retirar o resultado. */
    public int[] consumption(CraftingContainer grid) {
        int[] take = new int[grid.getContainerSize()];
        for (Entry e : entries) {
            int need = e.count();
            for (int i = 0; i < grid.getContainerSize() && need > 0; i++) {
                ItemStack in = grid.getItem(i);
                if (in.isEmpty()) continue;
                int free = in.getCount() - take[i];
                if (free <= 0 || !e.ingredient().test(in)) continue;
                int used = Math.min(free, need);
                take[i] += used;
                need -= used;
            }
        }
        return take;
    }

    @Override
    public boolean matches(CraftingContainer grid, Level level) {
        int[] take = consumption(grid);

        // 1) todos os ingredientes foram satisfeitos na contagem pedida.
        int required = 0;
        for (Entry e : entries) required += e.count();
        int allocated = 0;
        for (int v : take) allocated += v;
        if (allocated != required) return false;

        // 2) nada sobrou na grade: tudo que esta la foi alocado (sem lixo junto).
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack in = grid.getItem(i);
            if (!in.isEmpty() && take[i] != in.getCount()) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingContainer grid, RegistryAccess access) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= entries.size();
    }

    /**
     * Um Ingredient por entrada, na ordem. A contagem nao cabe aqui (o formato do jogo
     * so pede o ingrediente), mas e o que a tela do registro le para desenhar a receita
     * animada — sem isto, a ficha do Condensador sairia vazia.
     */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (Entry e : entries) list.add(e.ingredient());
        return list;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return result;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.RF_RECEIVER_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.RF_RECEIVER_TYPE.get();
    }

    // -------------------------------------------------------------- Serializer

    public static class Serializer implements RecipeSerializer<RFReceiverRecipe> {

        @Override
        public RFReceiverRecipe fromJson(ResourceLocation id, JsonObject json) {
            JsonArray arr = GsonHelper.getAsJsonArray(json, "ingredients");
            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JsonObject o = arr.get(i).getAsJsonObject();
                Ingredient ing = Ingredient.fromJson(o, false);
                int count = GsonHelper.getAsInt(o, "count", 1);
                entries.add(new Entry(ing, count));
            }
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            return new RFReceiverRecipe(id, entries, result);
        }

        @Override
        public RFReceiverRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            List<Entry> entries = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                Ingredient ing = Ingredient.fromNetwork(buf);
                int count = buf.readVarInt();
                entries.add(new Entry(ing, count));
            }
            ItemStack result = buf.readItem();
            return new RFReceiverRecipe(id, entries, result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, RFReceiverRecipe recipe) {
            buf.writeVarInt(recipe.entries.size());
            for (Entry e : recipe.entries) {
                e.ingredient().toNetwork(buf);
                buf.writeVarInt(e.count());
            }
            buf.writeItem(recipe.result);
        }
    }
}
