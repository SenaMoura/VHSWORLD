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

/**
 * Pilha corrompida.
 *
 * Faz tudo que a pilha limpa faz — e mais: enche a bateria de vez. Mas cobra do juizo
 * em vez de devolver. E a saida para a emergencia de quem gastou tudo revelando fotos e
 * nao tem pilha comum a mao; usada de habito, afunda a sanidade mais rapido que qualquer
 * susto. Mesmo caminho da pilha comum: o efeito no HUD e na sanidade roda no cliente (que
 * respeita o config), o consumo e o som no servidor.
 */
public class CorruptedBatteryItem extends Item {

    public CorruptedBatteryItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientBatteryHandler::rechargeCorrupted);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.FLASHLIGHT_CLICK.get(), SoundSource.PLAYERS, 0.9f, 0.6f);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.getCooldowns().addCooldown(this, 8);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
