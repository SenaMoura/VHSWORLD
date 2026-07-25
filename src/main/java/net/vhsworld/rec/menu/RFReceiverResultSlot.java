package net.vhsworld.rec.menu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.vhsworld.rec.recipe.RFReceiverRecipe;

/**
 * O slot de resultado do Receptor: nao aceita nada solto e, ao ser retirado, cobra da
 * grade a contagem EXATA da receita casada (o consumption() do RFReceiverRecipe). Como
 * a grade e um TransientCraftingContainer, o removeItem dispara o slotsChanged do menu,
 * que recalcula o resultado — entao, se ainda sobrar material, ja aparece o proximo.
 */
public class RFReceiverResultSlot extends Slot {

    private final RFReceiverMenu menu;
    private final Player player;
    private final CraftingContainer craftSlots;

    public RFReceiverResultSlot(RFReceiverMenu menu, Player player, CraftingContainer craftSlots,
                                ResultContainer result, int slot, int x, int y) {
        super(result, slot, x, y);
        this.menu = menu;
        this.player = player;
        this.craftSlots = craftSlots;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public void onTake(Player taker, ItemStack taken) {
        RFReceiverRecipe recipe = menu.currentRecipe;
        if (recipe != null) {
            int[] take = recipe.consumption(craftSlots);
            for (int i = 0; i < take.length; i++) {
                if (take[i] > 0) {
                    craftSlots.removeItem(i, take[i]);
                }
            }
        }
        super.onTake(taker, taken);
    }
}
