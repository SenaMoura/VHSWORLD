package net.vhsworld.rec.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * A ancora: volta para o ponto de spawn definido.
 *
 * O preco nao e material, e TEMPO. Segurar o botao por quatro segundos parado
 * significa que ela nao serve para escapar de uma perseguicao — qualquer coisa
 * que te alcance interrompe o uso. Ela e para quando voce se perdeu fundo demais
 * e ainda esta sozinho, que e exatamente o momento em que o mod quer que voce
 * pense se vale a pena continuar descendo.
 *
 * Depois de usar, ela dorme por cinco minutos.
 */
public class AnchorItem extends Item {

    /** Quanto tempo o jogador precisa ficar parado segurando. */
    private static final int CHANNEL_TICKS = 80;

    /** Descanso depois do uso, em ticks (5 minutos). */
    private static final int COOLDOWN_TICKS = 6000;

    public AnchorItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return CHANNEL_TICKS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return stack;

        ServerLevel destination = resolveLevel(player);
        BlockPos target = resolveSpawn(player, destination);

        level.playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRAVEL,
                SoundSource.PLAYERS, 0.4F, 1.4F);

        player.teleportTo(destination,
                target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D,
                player.getYRot(), player.getXRot());

        destination.playSound(null, target, SoundEvents.RESPAWN_ANCHOR_DEPLETE.get(),
                SoundSource.PLAYERS, 0.6F, 1.0F);

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return stack;
    }

    /** A dimensao do spawn do jogador; se ela nao existir mais, o overworld. */
    private static ServerLevel resolveLevel(ServerPlayer player) {
        ServerLevel spawn = player.server.getLevel(player.getRespawnDimension());
        return spawn != null ? spawn : player.server.overworld();
    }

    /**
     * Onde a ancora larga o jogador.
     *
     * Sem cama definida, vale o spawn do mundo — e ali a altura vem do heightmap,
     * nao do BlockPos cru: o spawn do mundo guarda so X e Z confiaveis, e usar o Y
     * dele enterraria o jogador na pedra em terreno acidentado.
     */
    private static BlockPos resolveSpawn(ServerPlayer player, ServerLevel level) {
        BlockPos bed = player.getRespawnPosition();
        if (bed != null) return bed;

        BlockPos shared = level.getSharedSpawnPos();
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, shared);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        // Soltou antes da hora: nada acontece, e nada e cobrado.
        if (!level.isClientSide && entity instanceof Player player && remaining > 0) {
            player.displayClientMessage(
                    Component.translatable("recmod.anchor.interrupted"), true);
        }
    }
}
