package net.vhsworld.rec.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FRACTURE — a espada que abre a realidade.
 *
 * Botao direito abre o primeiro portal; de novo, o segundo. A partir dai, o que
 * encostar em um sai pelo outro — jogador, bicho, tanto faz. AGACHAR e apertar
 * fecha os dois.
 *
 * DECISAO IMPORTANTE: o que estava te caçando atravessa junto. O portal nao e uma
 * saida de emergencia, e um atalho de mao dupla — se fosse fuga garantida, ela
 * apagaria toda a tensao que o resto do mod constroi. Fugir por ele e uma aposta,
 * nao um botao de salvar.
 *
 * Os dois portais moram no NBT da PROPRIA espada, entao cada FRACTURE tem o seu
 * par e eles sobrevivem a fechar o jogo.
 */
public class FractureItem extends SwordItem {

    private static final String TAG_A = "FracturePortalA";
    private static final String TAG_B = "FracturePortalB";

    /** Ate onde o corte alcanca para escolher o lugar do portal. */
    private static final double REACH = 24.0D;

    /** Quao perto e preciso chegar para ser puxado. */
    private static final double MOUTH = 1.6D;

    /**
     * Carencia depois de atravessar, em ticks.
     *
     * Sem ela, quem chega no portal B esta imediatamente dentro do alcance do B e
     * volta para o A no tick seguinte — o par viraria uma maquina de ping-pong.
     * O relogio de portal do vanilla nao serve: ele e zero para quase todo bicho.
     */
    private static final int PASSAGE_COOLDOWN = 40;

    private static final Map<UUID, Long> lastPassage = new HashMap<>();

    public FractureItem(Tier tier, int damage, float speed, Properties properties) {
        super(tier, damage, speed, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        CompoundTag tag = stack.getOrCreateTag();

        if (player.isShiftKeyDown()) {
            tag.remove(TAG_A);
            tag.remove(TAG_B);
            player.displayClientMessage(Component.translatable("recmod.fracture.closed"), true);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 0.5F, 1.6F);
            return InteractionResultHolder.success(stack);
        }

        BlockPos spot = BlockPos.containing(aimPoint(level, player));

        // Com o par ja fechado, o proximo corte recomeça: vira o portal A novo e
        // apaga o antigo. Sem isso o jogador ficaria preso ao primeiro par ate
        // lembrar de agachar para limpar.
        if (tag.contains(TAG_A) && tag.contains(TAG_B)) {
            tag.remove(TAG_B);
            tag.putLong(TAG_A, spot.asLong());
            announce(level, player, "recmod.fracture.first");
        } else if (!tag.contains(TAG_A)) {
            tag.putLong(TAG_A, spot.asLong());
            announce(level, player, "recmod.fracture.first");
        } else {
            tag.putLong(TAG_B, spot.asLong());
            announce(level, player, "recmod.fracture.second");
        }

        return InteractionResultHolder.success(stack);
    }

    private static void announce(Level level, Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.8F, 0.6F);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_A) || !tag.contains(TAG_B)) return;

        BlockPos a = BlockPos.of(tag.getLong(TAG_A));
        BlockPos b = BlockPos.of(tag.getLong(TAG_B));

        if (level.isClientSide) {
            spin(level, a);
            spin(level, b);
            return;
        }

        // Os dois sentidos, com os mesmos numeros: um portal de mao unica seria
        // uma armadilha, nao uma passagem.
        drag(level, a, b);
        drag(level, b, a);
    }

    /** Redemoinho de particulas para o portal ser visivel sem precisar de entidade. */
    private static void spin(Level level, BlockPos pos) {
        double t = level.getGameTime() * 0.25D;
        for (int i = 0; i < 3; i++) {
            double angle = t + i * (Math.PI * 2.0D / 3.0D);
            double radius = 0.7D;
            level.addParticle(ParticleTypes.PORTAL,
                    pos.getX() + 0.5D + Math.cos(angle) * radius,
                    pos.getY() + 0.6D + Math.sin(t * 0.6D) * 0.3D,
                    pos.getZ() + 0.5D + Math.sin(angle) * radius,
                    0.0D, 0.02D, 0.0D);
        }
        if (level.random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D,
                    0.0D, 0.01D, 0.0D);
        }
    }

    private static void drag(Level level, BlockPos from, BlockPos to) {
        AABB mouth = new AABB(from).inflate(MOUTH);
        long now = level.getGameTime();

        for (Entity entity : level.getEntities((Entity) null, mouth, e -> e instanceof LivingEntity)) {
            Long last = lastPassage.get(entity.getUUID());
            if (last != null && now - last < PASSAGE_COOLDOWN) continue;

            lastPassage.put(entity.getUUID(), now);

            entity.teleportTo(to.getX() + 0.5D, to.getY(), to.getZ() + 0.5D);
            level.playSound(null, to, SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 0.6F, 1.2F);
        }

        // O mapa e estatico: sem essa limpeza ele guardaria todo bicho que ja
        // passou por qualquer FRACTURE ate o jogo fechar.
        if (now % 600L == 0L) {
            lastPassage.entrySet().removeIf(e -> now - e.getValue() > PASSAGE_COOLDOWN * 4L);
        }
    }

    private static Vec3 aimPoint(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 far = eye.add(player.getLookAngle().scale(REACH));

        BlockHitResult hit = level.clip(new ClipContext(
                eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        return hit.getType() == BlockHitResult.Type.MISS ? far : hit.getLocation();
    }
}
