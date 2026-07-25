package net.vhsworld.rec.menu;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.vhsworld.rec.init.ModBlocks;
import net.vhsworld.rec.init.ModMenus;
import net.vhsworld.rec.init.ModRecipes;
import net.vhsworld.rec.recipe.RFReceiverRecipe;

import java.util.Optional;

/**
 * O menu do Receptor de Frequencia.
 *
 * Mesma engenharia da mesa de trabalho do vanilla: grade 3x3 TRANSIENTE + um slot
 * de resultado. A diferenca e o RecipeType — so as receitas recmod:rf_receiver
 * casam aqui. A grade avisa o menu quando muda (o TransientCraftingContainer chama
 * slotsChanged), e ai o resultado e recalculado no servidor e mandado para o cliente.
 *
 * Sem RecipeBookMenu de proposito: a bancada do mod nao usa o livro de receitas, o
 * que evita toda a papelada de recipe book em troca de nao ter o botao do livrinho.
 */
public class RFReceiverMenu extends AbstractContainerMenu {

    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;
    private final Level level;

    /** A receita casada agora — o slot de resultado le dela para saber quanto consumir. */
    RFReceiverRecipe currentRecipe;

    public RFReceiverMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public RFReceiverMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModMenus.RF_RECEIVER.get(), id);
        this.access = access;
        this.player = inv.player;
        this.level = inv.player.level();

        this.addSlot(new RFReceiverResultSlot(this, this.player, craftSlots, resultSlots, 0, 124, 35));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(craftSlots, col + row * 3, 30 + col * 18, 17 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    CraftingContainer craftSlots() {
        return craftSlots;
    }

    @Override
    public void slotsChanged(Container container) {
        this.access.execute((lvl, pos) -> updateResult(lvl));
    }

    private void updateResult(Level lvl) {
        if (lvl.isClientSide || !(player instanceof ServerPlayer server)) return;

        ItemStack result = ItemStack.EMPTY;
        Optional<RFReceiverRecipe> match = lvl.getRecipeManager()
                .getRecipeFor(ModRecipes.RF_RECEIVER_TYPE.get(), craftSlots, lvl);
        if (match.isPresent()) {
            currentRecipe = match.get();
            result = currentRecipe.assemble(craftSlots, lvl.registryAccess());
        } else {
            currentRecipe = null;
        }

        resultSlots.setItem(0, result);
        server.connection.send(new ClientboundContainerSetSlotPacket(
                containerId, incrementStateId(), 0, result));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((lvl, pos) -> clearContainer(player, craftSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.RF_RECEIVER.get());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index == 0) {
                // Resultado: joga para o inventario. Consumo dos ingredientes vem no onTake.
                if (!this.moveItemStackTo(stack, 10, 46, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(stack, result);
            } else if (index >= 10) {
                // Do inventario para a grade.
                if (!this.moveItemStackTo(stack, 1, 10, false)) return ItemStack.EMPTY;
            } else {
                // Da grade para o inventario.
                if (!this.moveItemStackTo(stack, 10, 46, false)) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
            if (index == 0) player.drop(stack, false);
        }
        return result;
    }
}
