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
import net.vhsworld.rec.client.tape.TapeRecorder;

/**
 * Fita virgem.
 *
 * Usar (botao direito) enfia a fita na camera e comeca a gravar alguns segundos de
 * imagem. O jogador nao ve nada de especial na hora — a graca esta em nao poder rever
 * agora. So depois, no videocassete e em seguranca, e que a fita conta o que passou.
 * A gravacao roda no cliente (le o framebuffer, e respeita o config la); o cooldown do
 * servidor cobre a duracao para o jogador nao empilhar fitas.
 */
public class BlankTapeItem extends Item {

    /** Cooldown fixo entre gravacoes. Nao le config CLIENT no servidor de proposito. */
    private static final int COOLDOWN_TICKS = 220;

    public BlankTapeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> TapeRecorder::start);
            return InteractionResultHolder.success(stack);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.FLASHLIGHT_CLICK.get(), SoundSource.PLAYERS, 0.7f, 1.3f);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        return InteractionResultHolder.success(stack);
    }
}
