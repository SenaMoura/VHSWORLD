package net.vhsworld.rec.item;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.vhsworld.rec.client.ClientBatteryHandler;

// Pilha: ao usar (botão direito) recarrega a bateria da filmadora e é consumida.
public class BatteryItem extends Item {

    public BatteryItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            // Recarrega o HUD da câmera (valor client-side)
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientBatteryHandler::recharge);
        } else {
            // Lado servidor: som, consumo da pilha e um pequeno cooldown
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.FLASHLIGHT_CLICK.get(), SoundSource.PLAYERS, 0.9f, 1.0f);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.getCooldowns().addCooldown(this, 8);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
