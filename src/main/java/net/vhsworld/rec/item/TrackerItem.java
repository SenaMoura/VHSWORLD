package net.vhsworld.rec.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A base dos localizadores: o Rastreador de Minerios e o Localizador de Estruturas.
 *
 * O loop e o mesmo dos itens do From Below Land que inspiraram o Pedro: um RITO de
 * acesso, um poder TEMPORARIO, e a EXPIRACAO. Aqui o rito e segurar o botao por tres
 * segundos (interrompe se algo te encostar, como a Ancora); o poder e seis minutos de
 * ponto vermelho atravessando parede + o coracao que acelera perto do alvo; e a
 * expiracao vem do relogio do mundo gravado no proprio item.
 *
 * A janela inteira vira COOLDOWN do item (duracao + descanso), entao o sweep do icone
 * ja conta para o jogador quanto ainda dura, e nao da para reacender no meio.
 *
 * Tudo aqui e server-safe: a duracao vive em constante (nao em config CLIENT, que
 * crasharia num servidor dedicado — a licao das engenhocas). A parte que ve o alvo,
 * desenha o ponto e toca o coracao e 100% cliente, e mora no client/TrackerSense.
 */
public abstract class TrackerItem extends Item {

    /** O rito: tres segundos segurando. */
    protected static final int CHANNEL_TICKS = 60;

    /** Quanto dura o poder depois do rito: seis minutos. */
    public static final int DURATION_TICKS = 7200;

    /** Descanso depois que a janela fecha, antes de poder refazer o rito. */
    protected static final int REST_TICKS = 1200;

    private static final String KEY_ACTIVE_UNTIL = "ActiveUntil";

    public TrackerItem(Properties properties) {
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
        ItemStack stack = player.getItemInHand(hand);

        // A janela ativa E o cooldown: enquanto conta, o rito nao refaz.
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) return stack;

        long until = level.getGameTime() + DURATION_TICKS;
        stack.getOrCreateTag().putLong(KEY_ACTIVE_UNTIL, until);

        Component message = onActivate(player.serverLevel(), player, stack);

        level.playSound(null, player.blockPosition(), activationSound(),
                SoundSource.PLAYERS, 0.7F, 0.5F);
        player.getCooldowns().addCooldown(this, DURATION_TICKS + REST_TICKS);
        player.displayClientMessage(message, true);
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        // Soltou antes da hora: nada e gravado e nada e cobrado. Igual a Ancora.
        if (!level.isClientSide && entity instanceof Player player && remaining > 0) {
            player.displayClientMessage(
                    Component.translatable("recmod.tracker.interrupted"), true);
        }
    }

    /**
     * Fixa o alvo no momento do rito e devolve a frase que aparece na tela.
     *
     * O Rastreador varre no cliente todo tick (o minerio pode ser cavado, entao ele
     * reaponta sozinho), entao aqui nao faz nada alem da frase. O Localizador precisa do
     * servidor para achar a estrutura, entao grava a posicao no NBT — que sincroniza
     * sozinho para o cliente, sem pacote proprio.
     */
    protected Component onActivate(ServerLevel level, ServerPlayer player, ItemStack stack) {
        return Component.translatable(activationKey());
    }

    protected abstract SoundEvent activationSound();

    protected abstract String activationKey();

    /** Chave de lang da linha de tooltip fixa (o "como funciona"). */
    protected abstract String hintKey();

    public static boolean isActive(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getLong(KEY_ACTIVE_UNTIL) > level.getGameTime();
    }

    public static long activeUntil(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0L : tag.getLong(KEY_ACTIVE_UNTIL);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(hintKey())
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));

        if (level != null && isActive(stack, level)) {
            int left = (int) ((activeUntil(stack) - level.getGameTime()) / 20L);
            tooltip.add(Component.translatable("recmod.tracker.active", left / 60, left % 60)
                    .withStyle(net.minecraft.ChatFormatting.RED));
        }
    }
}
