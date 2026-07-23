package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.init.ModItems;

/**
 * Um lugar so para responder "a camera esta ligada agora?".
 *
 * Antes o fisheye e a tremida rodavam sempre, sem excecao e sem desligar. Agora passam
 * por aqui: quem manda e o config, e opcionalmente a filmadora estar na mao.
 */
public final class CameraState {

    public static boolean isActive() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return false;

        if (!RECConfig.CLIENT.effectsOnlyWhenHolding.get()) return true;

        return player.getMainHandItem().is(ModItems.CAMCORDER.get())
                || player.getOffhandItem().is(ModItems.CAMCORDER.get());
    }

    /** Volume final de um som do mod, ja com o multiplicador do config. */
    public static float volume(float base) {
        return base * RECConfig.CLIENT.horrorVolume.get().floatValue();
    }

    /** true se o som do mod deve tocar (volume zerado no config = silencio). */
    public static boolean audible() {
        return RECConfig.CLIENT.horrorVolume.get() > 0.0D;
    }

    private CameraState() {}
}
