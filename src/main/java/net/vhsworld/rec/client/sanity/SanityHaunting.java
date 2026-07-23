package net.vhsworld.rec.client.sanity;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.vhsworld.rec.client.CameraState;
import net.vhsworld.rec.client.CamcorderOverlay;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.item.ModSounds;

import java.util.Random;

/**
 * O que a fita faz com o jogador que olhou demais.
 *
 * Nada aqui mata, e nada aqui e uma criatura. Sao dois eventos que usam pecas que
 * o jogo ja tinha — o apagao e os sons — e que agora acontecem sozinhos, sem causa
 * visivel. A duvida e o ponto: nao ha nada la fora, e o jogador nao tem como saber.
 */
public final class SanityHaunting {

    private static final Random RANDOM = new Random();

    /** Chance por tick, em sanidade zero, de a camera apagar do nada. ~1 a cada 2 min. */
    private static final double BLACKOUT_CHANCE = 1.0 / 2400.0;

    /** Chance por tick, em sanidade zero, de um som sem dono. ~1 a cada 25 s. */
    private static final double SOUND_CHANCE = 1.0 / 500.0;

    public static void tick(Minecraft mc) {
        if (!RECConfig.CLIENT.sanity.get()) return;
        if (mc.player == null || mc.level == null) return;

        float dread = SanityState.get().dread();
        if (dread <= 0.0f) return;

        maybeBlackout(mc, dread);
        maybePhantomSound(mc, dread);
    }

    /**
     * A camera apaga sem a bateria ter acabado.
     *
     * O jogador vai olhar a porcentagem, ver 60%, e nao ter explicacao. E o momento
     * em que a ferramenta dele deixa de ser confiavel.
     */
    private static void maybeBlackout(Minecraft mc, float dread) {
        if (!RECConfig.CLIENT.sanityBlackouts.get()) return;
        if (CamcorderOverlay.isBatteryDead) return;
        if (RANDOM.nextDouble() > BLACKOUT_CHANCE * dread) return;

        CamcorderOverlay.isBatteryDead = true;
        CamcorderOverlay.miniGameProgress = 0.0f;
    }

    /** Passos ou um grito ao redor, sem nada por perto. */
    private static void maybePhantomSound(Minecraft mc, float dread) {
        if (!RECConfig.CLIENT.sanityPhantomSounds.get()) return;
        if (!CameraState.audible()) return;
        if (RANDOM.nextDouble() > SOUND_CHANCE * dread) return;

        // Atras ou ao lado, nunca na frente: o que se ve nao assusta.
        double angle = RANDOM.nextDouble() * Math.PI * 2.0;
        double distance = 4.0 + RANDOM.nextDouble() * 8.0;

        Vec3 at = mc.player.position().add(
                Math.cos(angle) * distance,
                RANDOM.nextDouble() * 2.0 - 0.5,
                Math.sin(angle) * distance);

        boolean scream = RANDOM.nextFloat() < 0.25f * dread;
        SoundEvent sound = scream
                ? ModSounds.ENTITY_SCREAM.get()
                : ModSounds.ENTITY_APPROACHING.get();

        // Baixo de proposito: tem que caber a duvida de ter ouvido mesmo.
        float volume = CameraState.volume(scream ? 0.7f : 0.9f);

        mc.level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.HOSTILE,
                volume, 0.9f + RANDOM.nextFloat() * 0.2f, false);
    }

    private SanityHaunting() {}
}
