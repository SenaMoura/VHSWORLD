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
 * A VOLTA E PELA MESMA FITA. O lugar de onde o jogador saiu fica gravado nos dados
 * persistentes dele (sobrevive a morte e a saida do mundo), entao usar a fita
 * dentro da dimensao devolve exatamente onde ele estava. Se o registro se perder,
 * a volta cai no ponto de nascimento do overworld em vez de deixar o jogador preso.
 */
public class DimensionTapeItem extends Item {

    /** Onde o jogador estava antes de entrar. Mora no NBT persistente dele. */
    private static final String RETURN_TAG = "recmod:tape_return";

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

        if (level.dimension().equals(target)) {
            leave(serverPlayer, server);
        } else {
            enter(serverPlayer, server);
        }
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

    // ------------------------------------------------------------------ volta
    private void leave(ServerPlayer player, MinecraftServer server) {
        CompoundTag data = player.getPersistentData();
        ServerLevel destination = null;
        Vec3 where = null;

        if (data.contains(RETURN_TAG)) {
            CompoundTag mark = data.getCompound(RETURN_TAG);
            ResourceLocation id = ResourceLocation.tryParse(mark.getString("dimension"));
            if (id != null) {
                destination = server.getLevel(
                        ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id));
            }
            where = new Vec3(mark.getDouble("x"), mark.getDouble("y"), mark.getDouble("z"));
        }

        // Sem marca (mundo antigo, ou o jogador entrou por comando): o overworld
        // resolve. Ficar preso na dimensao seria pior do que voltar no lugar errado.
        if (destination == null) {
            destination = server.overworld();
            BlockPos spawn = destination.getSharedSpawnPos();
            where = new Vec3(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        }

        data.remove(RETURN_TAG);
        player.changeDimension(destination, new FixedPoint(where));
    }

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
