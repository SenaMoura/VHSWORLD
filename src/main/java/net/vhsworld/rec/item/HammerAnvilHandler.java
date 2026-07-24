package net.vhsworld.rec.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.init.ModItems;

/**
 * O ferro prensado nao sai da bancada: sai da bigorna, na pancada.
 *
 * O martelo vai no slot da ESQUERDA e o ferro no da DIREITA. A ordem nao e gosto —
 * a bigorna do jogo esvazia o slot da esquerda inteiro ao entregar o resultado e so
 * o da direita respeita a quantidade consumida. Com o martelo a esquerda, o ferro
 * some na conta certa; e o martelo, que e ferramenta e nao ingrediente, a gente
 * devolve gasto em vez de deixar sumir.
 *
 * Cada chapa custa um ponto de martelo, entao um martelo rende 128 chapas e depois
 * acaba. Ele nao aceita conserto (`setNoRepair`): martelo gasto se refaz, nao se
 * remenda.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HammerAnvilHandler {

    /** Experiencia cobrada pela bigorna. Barata de proposito: isto e trabalho braçal. */
    private static final int XP_COST = 1;

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack hammer = event.getLeft();
        ItemStack iron = event.getRight();

        if (!hammer.is(ModItems.HAMMER.get()) || !iron.is(Items.IRON_INGOT)) return;

        // Quantas chapas cabem nesta batida: limitado pelo ferro na bigorna E pelo
        // que ainda sobra de martelo. Sem o segundo limite, uma pilha de 64 ferros
        // gastaria mais martelo do que existe.
        int remaining = hammer.getMaxDamage() - hammer.getDamageValue();
        int amount = Math.min(iron.getCount(), remaining);
        if (amount <= 0) return;

        event.setOutput(new ItemStack(ModItems.PRESSED_IRON.get(), amount));
        event.setMaterialCost(amount);
        event.setCost(XP_COST);
    }

    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        ItemStack hammer = event.getLeft();

        if (!hammer.is(ModItems.HAMMER.get())) return;
        if (!event.getOutput().is(ModItems.PRESSED_IRON.get())) return;

        ItemStack returned = hammer.copy();
        returned.setCount(1);
        returned.setDamageValue(returned.getDamageValue() + event.getOutput().getCount());

        // Gastou tudo: o martelo morre na ultima chapa, e essa chapa o jogador leva.
        if (returned.getDamageValue() >= returned.getMaxDamage()) return;

        Player player = event.getEntity();
        if (!player.getInventory().add(returned)) {
            player.drop(returned, false);
        }
    }

    private HammerAnvilHandler() {}
}
