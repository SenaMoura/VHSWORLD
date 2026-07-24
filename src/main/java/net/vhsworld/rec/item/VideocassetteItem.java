package net.vhsworld.rec.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Videocassete: usar abre o cofre de fitas para rever as gravacoes.
 *
 * E onde o atraso da fita virgem se paga. O aparelho nao grava nada — so reproduz o que
 * ja foi gravado, no ritmo em que foi. O jogador senta em seguranca e finalmente ve o
 * corredor que filmou vazio, quadro a quadro.
 */
public class VideocassetteItem extends Item {

    public VideocassetteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> VideocassetteItem::openVault);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** So carregada no cliente; respeita o config CLIENT aqui, com seguranca. */
    private static void openVault() {
        if (!net.vhsworld.rec.config.RECConfig.CLIENT.tapes.get()) return;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        mc.setScreen(new net.vhsworld.rec.client.tape.TapeVaultScreen());
    }
}
