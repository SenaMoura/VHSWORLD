package net.vhsworld.rec.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * O relogio: faz barulho ONDE VOCE APONTA, e nao onde voce esta.
 *
 * Essa e a coisa toda. Um chamariz que toca na mao do jogador seria suicidio —
 * ele traria tudo para cima de quem usou. Apontando, o relogio vira o que ele
 * deveria ser: um jeito de tirar as coisas do seu caminho.
 *
 * Os bichos perdem o alvo atual antes de irem: sem isso, um zumbi que ja estava
 * te caçando ignoraria o som e continuaria vindo, e o item nao serviria para nada
 * justamente na hora em que ele mais importa.
 */
public class LureClockItem extends Item {

    /** Ate onde o jogador pode escolher o ponto do barulho. */
    private static final double AIM_RANGE = 32.0D;

    /** Raio, em volta do ponto, de quem escuta. */
    private static final double LURE_RADIUS = 24.0D;

    private static final int COOLDOWN_TICKS = 200;

    public LureClockItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            Vec3 spot = aimPoint(level, player);
            BlockPos pos = BlockPos.containing(spot);

            level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 3.0F, 0.8F);

            AABB heard = new AABB(spot, spot).inflate(LURE_RADIUS);
            for (Mob mob : level.getEntitiesOfClass(Mob.class, heard)) {
                // Quando existirem as criaturas cegas do mod, e aqui que elas
                // ganham raio maior: elas caçam por som, entao o relogio deveria
                // mandar nelas mais do que manda num zumbi.
                if (mob instanceof Enemy) {
                    mob.setTarget(null);
                }
                mob.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.0D);
            }

            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * O ponto para onde o jogador esta olhando.
     *
     * Se a mira nao encontra bloco nenhum (ceu aberto, mar), o som cai no fim do
     * alcance mesmo assim — o relogio nao pode simplesmente falhar por causa de
     * onde o jogador estava olhando.
     */
    private static Vec3 aimPoint(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 reach = eye.add(player.getLookAngle().scale(AIM_RANGE));

        BlockHitResult hit = level.clip(new ClipContext(
                eye, reach, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        return hit.getType() == BlockHitResult.Type.MISS ? reach : hit.getLocation();
    }
}
