package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.vhsworld.rec.config.RECConfig;

/**
 * Um lugar so para responder "a camera esta ligada agora?".
 *
 * A resposta hoje e: esta, sempre. A filmadora deixou de ser um item que se segura —
 * a camera e o estado do mundo, o jogador ja acordou filmando. Isto continua sendo
 * uma pergunta com metodo proprio porque um dia pode voltar a ter condicao
 * (bateria arrancada, camera quebrada, uma cena que desliga tudo).
 */
public final class CameraState {

    public static boolean isActive() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
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
