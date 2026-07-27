package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.item.ModSounds;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Random;

/**
 * O barulho que vem do nada.
 *
 * De tempos em tempos, sem criatura nenhuma por perto e sem aviso, o mundo faz um
 * ruido: fala embolada, fita rasgando, passos pesados. Nao ha nada ali. E esse e o
 * ponto — a maior parte do medo de um jogo de terror acontece quando NAO esta
 * acontecendo nada, e o jogador para de andar sozinho, so por causa de um som.
 *
 * DIFERENCA PARA O SanityHaunting: aquele so acorda com a sanidade no chao e e
 * castigo por ter olhado demais. Este toca desde o primeiro minuto, com o medidor
 * cheio, e nao e castigo de nada — e o clima do mundo. Os dois somam de proposito:
 * quem esta bem ouve pouco, quem esta acabado ouve os dois.
 *
 * Nunca na frente do jogador: o que se ve nao assusta.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AmbientDread {

    private AmbientDread() {}

    private static final Random RANDOM = new Random();

    private static final List<RegistryObject<SoundEvent>> POOL = List.of(
            ModSounds.DREAD_SPEECH,
            ModSounds.DREAD_BROKEN,
            ModSounds.DREAD_GLITCH,
            ModSounds.DREAD_STEPS,
            ModSounds.DREAD_BURNT,
            ModSounds.DREAD_FLESH);

    /** Ticks ate o proximo. -1 = ainda nem sorteado (entrou no mundo agora). */
    private static int countdown = -1;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            countdown = -1;
            return;
        }
        if (mc.isPaused()) return;

        if (!RECConfig.CLIENT.ambientDread.get() || !CameraState.audible()) {
            countdown = -1;
            return;
        }

        if (countdown < 0) {
            rearm();
            return;
        }
        if (--countdown > 0) return;

        play(mc);
        rearm();
    }

    /**
     * Sorteia a espera ate o proximo.
     *
     * A distancia entre o minimo e o maximo e a mecanica: se fosse intervalo fixo, o
     * jogador aprenderia o compasso em dez minutos e o som viraria metronomo. O que
     * assusta e nao dar para prever.
     */
    private static void rearm() {
        int min = RECConfig.CLIENT.ambientDreadMinSeconds.get();
        int max = Math.max(min, RECConfig.CLIENT.ambientDreadMaxSeconds.get());
        countdown = (min + RANDOM.nextInt(max - min + 1)) * 20;
    }

    private static void play(Minecraft mc) {
        SoundEvent sound = POOL.get(RANDOM.nextInt(POOL.size())).get();

        // Atras ou ao lado, a uma distancia que da para acreditar.
        double angle = RANDOM.nextDouble() * Math.PI * 2.0;
        double distance = 6.0 + RANDOM.nextDouble() * 10.0;

        Vec3 at = mc.player.position().add(
                Math.cos(angle) * distance,
                RANDOM.nextDouble() * 3.0 - 1.0,
                Math.sin(angle) * distance);

        // Baixo de proposito: tem que caber a duvida de ter ouvido mesmo.
        float volume = CameraState.volume(
                RECConfig.CLIENT.ambientDreadVolume.get().floatValue());

        mc.level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.AMBIENT,
                volume, 0.85f + RANDOM.nextFloat() * 0.3f, false);
    }
}
