package net.vhsworld.rec.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
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
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.vhsworld.rec.RECMod;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.vhsworld.rec.worldgen.dim.DimSpawn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * A fita de uma dimensao. Poe no aparelho e voce e puxado para dentro dela.
 *
 * POR QUE FITA E NAO PORTAL: sao 21 dimensoes planejadas. Vinte e um portais seriam
 * 21 blocos, 21 receitas e 21 molduras de pedra plantadas pelo mundo — e o mod
 * inteiro fala de VHS. A fita ja e o objeto do jogo; so faltava ela levar a algum
 * lugar.
 *
 * ⚠️ A FITA E SO IDA (v1.70.0). Ate a 1.69 ela era tambem a volta, e isso foi
 * removido de proposito: com ela no bolso, entrar numa dimensao nao custava nada e
 * o medo tinha botao de pausa. Agora quem tira o jogador de la e o aparelho de
 * saida daquela dimensao — ver o pacote `escape` e o `ExitMethod`.
 *
 * O que a fita ainda faz, e que passou a ser a coisa mais importante dela: GRAVAR A
 * MARCA de onde o jogador saiu, nos dados persistentes (que sobrevivem a morte e a
 * sair do mundo). E essa marca que a saida consome depois. A chave mora no
 * `Escape.RETURN_TAG` e nao aqui: quem escreve e quem le tem que ser o mesmo
 * arquivo, senao a ida grava numa chave e a saida procura noutra, e o sintoma —
 * todo mundo voltando para o spawn do mundo — parece decisao de design e nao
 * defeito.
 */
public class DimensionTapeItem extends Item {

    /** Onde o jogador estava antes de entrar. Mora no NBT persistente dele. */
    private static final String RETURN_TAG = net.vhsworld.rec.escape.Escape.RETURN_TAG;

    private final ResourceKey<Level> target;

    public DimensionTapeItem(Properties properties, String dimension) {
        super(properties);
        this.target = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, dimension));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        MinecraftServer server = serverPlayer.getServer();
        if (server == null) return InteractionResultHolder.pass(stack);

        // O chiado sai do lugar de ONDE se sai, e nao de onde se chega: e o aviso de
        // que a fita pegou, antes de a tela mudar.
        level.playSound(null, player.blockPosition(), ModSounds.TAPE_PLAYER.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);

        // ⚠️ DENTRO DA PROPRIA DIMENSAO A FITA NAO FAZ MAIS NADA (v1.70.0). Ela era a ida
        // E a volta; agora e so ida, e quem tira o jogador de la e o aparelho de saida
        // daquela dimensao — ver o pacote `escape`. O aviso na tela e obrigatorio e nao
        // enfeite: sem ele, o jogador aperta a fita, nao acontece nada, e a leitura mais
        // razoavel que lhe resta e "o mod bugou" — nao "existe outra saida".
        if (level.dimension().equals(target)) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.recmod.tape_one_way"), true);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        enter(serverPlayer, server);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    // ------------------------------------------------------------------ ida
    private void enter(ServerPlayer player, MinecraftServer server) {
        ServerLevel destination = server.getLevel(target);
        if (destination == null) {
            player.displayClientMessage(Component.translatable("message.recmod.tape_dead"), true);
            return;
        }

        CompoundTag data = player.getPersistentData();
        CompoundTag mark = new CompoundTag();
        mark.putString("dimension", player.level().dimension().location().toString());
        mark.putDouble("x", player.getX());
        mark.putDouble("y", player.getY());
        mark.putDouble("z", player.getZ());
        data.put(RETURN_TAG, mark);

        player.changeDimension(destination, new FixedPoint(spawnOf(destination)));
    }

    // A VOLTA SAIU DAQUI. Ela agora e `Escape.leave`, chamada pelo aparelho de saida da
    // dimensao. O codigo era quase identico ao que esta la — o que mudou nao foi a conta,
    // foi QUEM tem o direito de chama-la.

    /**
     * O hub da dimensao, perguntado ao proprio gerador dela.
     *
     * ⚠️ Aqui havia um `instanceof` por gerador e o comentario "dimensao nova, uma
     * linha nova" — e a CHUNKS foi justamente a que ficou de fora: ela caia no
     * (0,64,0) de reserva, que naquela dimensao era o miolo da pedra da coluna
     * central, e o jogador nascia dentro de um bloco. Agora quem responde e a
     * interface `DimSpawn`: gerador que nao a implementa nao serve como dimensao do
     * mod, e isso o compilador cobra — o arquivo distante deixou de existir.
     */
    public static Vec3 spawnOf(ServerLevel level) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        BlockPos pos = generator instanceof DimSpawn dimension ? dimension.dimensionSpawn() : null;
        return pos == null
                ? new Vec3(0.5, 64, 0.5)
                : new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    /**
     * Teleporte sem portal: a fita nao constroi nada, ela so troca voce de lugar.
     *
     * O `ITeleporter` do Forge existe justamente para isto — sem ele, o jogo procura
     * (e cava) um portal do Nether no destino.
     */
    public record FixedPoint(Vec3 where) implements ITeleporter {
        @Override
        public PortalInfo getPortalInfo(net.minecraft.world.entity.Entity entity, ServerLevel destination,
                                        Function<ServerLevel, PortalInfo> vanilla) {
            return new PortalInfo(where, Vec3.ZERO, entity.getYRot(), entity.getXRot());
        }

        @Override
        public boolean isVanilla() {
            return false;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.recmod.dimension_tape")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    public ResourceKey<Level> target() {
        return target;
    }
}
