package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.sanity.SanityState;
import net.vhsworld.rec.init.ModBlocks;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Como o jogador acha uma coisa que nao da para ver.
 *
 * Tres camadas, nesta ordem:
 *   1. PERTO — uma frase na tela avisa que ha algo ali, sem dizer onde.
 *   2. FLASH — o disparo revela os rasgos no alcance por alguns segundos, e a
 *      tela treme na hora em que aparece.
 *   3. MIRA — o contorno branco de bloco so acende num rasgo ja revelado.
 *
 * A terceira camada e o que impede o truque de "varrer a parede com o cursor ate
 * o contorno piscar". Sem ela nada disso seria necessario: bastava passar o mouse.
 *
 * Tudo do lado do cliente, como o resto da camera.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RealityTearSense {

    /** Ate onde o jogador SENTE que ha alguma coisa. */
    private static final int HINT_RANGE = 10;

    /** Ate onde o flash consegue revelar. */
    private static final int FLASH_RANGE = 14;

    /** Quanto tempo o rasgo fica visivel depois do disparo, em ticks. */
    private static final int REVEAL_TICKS = 120;

    /** Espera entre duas frases, para o aviso nao virar spam. */
    private static final long HINT_COOLDOWN = 400L;

    private static final Map<BlockPos, Integer> revealed = new HashMap<>();
    private static long nextHint;
    private static int scanTimer;

    public static boolean isRevealed(BlockPos pos) {
        return revealed.containsKey(pos);
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            revealed.clear();
            return;
        }

        revealed.entrySet().removeIf(e -> {
            int left = e.getValue() - 1;
            e.setValue(left);
            return left <= 0;
        });

        for (Map.Entry<BlockPos, Integer> e : revealed.entrySet()) {
            BlockPos pos = e.getKey();
            for (int i = 0; i < 2; i++) {
                mc.level.addParticle(ParticleTypes.END_ROD,
                        pos.getX() + mc.level.random.nextDouble(),
                        pos.getY() + mc.level.random.nextDouble(),
                        pos.getZ() + mc.level.random.nextDouble(),
                        0.0D, 0.005D, 0.0D);
            }
            mc.level.addParticle(ParticleTypes.PORTAL,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    (mc.level.random.nextDouble() - 0.5D) * 0.6D, 0.0D,
                    (mc.level.random.nextDouble() - 0.5D) * 0.6D);
        }

        // A varredura e cara para rodar todo tick: 21x21x21 blocos. De dois em
        // dois segundos e o bastante — o jogador nao anda 10 blocos nesse tempo.
        if (++scanTimer < 40) return;
        scanTimer = 0;

        if (mc.level.getGameTime() < nextHint) return;
        if (!anyTearNear(mc, HINT_RANGE)) return;

        nextHint = mc.level.getGameTime() + HINT_COOLDOWN;
        mc.player.displayClientMessage(Component.translatable("recmod.tear.sense"), true);
    }

    /**
     * Chamado quando o flash dispara.
     *
     * Revela tudo que estiver no alcance, sem cone: o clarao enche a caverna,
     * nao aponta. E o tremor entra so quando aparece um rasgo NOVO — senao ficar
     * fotografando o mesmo lugar sacudiria a tela sem parar.
     */
    public static void onFlash(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        BlockPos center = mc.player.blockPosition();
        boolean found = false;

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-FLASH_RANGE, -FLASH_RANGE, -FLASH_RANGE),
                center.offset(FLASH_RANGE, FLASH_RANGE, FLASH_RANGE))) {

            if (!mc.level.getBlockState(pos).is(ModBlocks.REALITY_TEAR.get())) continue;

            if (revealed.put(pos.immutable(), REVEAL_TICKS) == null) found = true;
        }

        if (found) {
            SanityState.get().sighting(0.0F);
        }
    }

    private static boolean anyTearNear(Minecraft mc, int range) {
        BlockPos center = mc.player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-range, -range, -range),
                center.offset(range, range, range))) {
            if (mc.level.getBlockState(pos).is(ModBlocks.REALITY_TEAR.get())) return true;
        }
        return false;
    }

    /** Some com o contorno de mira enquanto o rasgo nao foi revelado. */
    @SubscribeEvent
    public static void onHighlight(RenderHighlightEvent.Block event) {
        BlockHitResult hit = event.getTarget();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        if (!mc.level.getBlockState(hit.getBlockPos()).is(ModBlocks.REALITY_TEAR.get())) return;
        if (isRevealed(hit.getBlockPos())) return;

        event.setCanceled(true);
    }

    private RealityTearSense() {}
}
