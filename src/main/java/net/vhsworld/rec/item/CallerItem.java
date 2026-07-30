package net.vhsworld.rec.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.vhsworld.rec.apocalypse.ApocalypseState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * O CALLER. Chama, e o que atende nao vai embora.
 *
 * O QUE ELE FAZ: liga o apocalipse (ver ApocalypseState / ApocalypseHandler). O dia
 * para de vir, a tempestade nao passa, e o Cara Cinza deixa de ser um encontro para
 * virar rotina. Nao ha como desligar.
 *
 * POR QUE ELE PERGUNTA DUAS VEZES: e a lição nº2 das autopsias — nada que nao se
 * desfaz acontece por clique. Agachado e usar ARMA (e diz, em letra vermelha, o que
 * vai acontecer); usar de novo em dez segundos DISPARA. Quem apertou sem querer tem
 * dez segundos para nao apertar de novo, e quem apertou de proposito nao tem do que
 * reclamar depois. A janela de dez segundos existe para o "sim" ser do mesmo minuto
 * que o "confirma" — armado para sempre viraria uma armadilha guardada no inventario.
 */
public class CallerItem extends Item {

    /** Quanto tempo o "sim" vale. Passou disso, tem que armar de novo. */
    private static final int WINDOW = 200;   // 10 segundos

    /** Quando cada jogador armou. Nao vai para o NBT de proposito: ver acima. */
    private static final java.util.Map<java.util.UUID, Long> ARMED = new java.util.HashMap<>();

    public CallerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)
                || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        ApocalypseState state = ApocalypseState.get(serverLevel);
        if (state.isActive()) {
            // Ja chamaram. O item vira o que ele sempre foi: um pedaco de metal.
            serverPlayer.displayClientMessage(
                    Component.translatable("message.recmod.caller_already").withStyle(ChatFormatting.DARK_GRAY), true);
            return InteractionResultHolder.fail(stack);
        }

        long now = level.getGameTime();
        Long armedAt = ARMED.get(serverPlayer.getUUID());

        if (player.isShiftKeyDown()) {
            ARMED.put(serverPlayer.getUUID(), now);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.recmod.caller_armed").withStyle(ChatFormatting.DARK_RED), false);
            level.playSound(null, player.blockPosition(), ModSounds.TAPE_PLAYER.get(),
                    SoundSource.PLAYERS, 1.0F, 0.6F);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        if (armedAt == null || now - armedAt > WINDOW) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.recmod.caller_arm_first").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }

        ARMED.remove(serverPlayer.getUUID());
        fire(serverLevel, serverPlayer, state);
        stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /** O momento. Uma vez por mundo. */
    private static void fire(ServerLevel level, ServerPlayer player, ApocalypseState state) {
        state.begin(level.getGameTime());

        BlockPos where = player.blockPosition();
        level.playSound(null, where, ModSounds.ENTITY_SCREAM.get(), SoundSource.MASTER, 1.0F, 0.7F);
        level.playSound(null, where, ModSounds.HORROR_SANITY.get(), SoundSource.MASTER, 1.0F, 1.0F);

        // O ceu responde na hora, em vez de esperar o proximo passe do handler: o
        // jogador tem que ligar o trovao ao que ele acabou de fazer.
        ServerLevel overworld = level.getServer().overworld();
        overworld.setWeatherParameters(0, 24000, true, true);

        for (ServerPlayer everyone : level.getServer().getPlayerList().getPlayers()) {
            everyone.displayClientMessage(
                    Component.translatable("message.recmod.caller_fired").withStyle(ChatFormatting.DARK_RED), false);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.recmod.caller").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("tooltip.recmod.caller_warning").withStyle(ChatFormatting.DARK_RED));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}