package net.vhsworld.rec.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import org.lwjgl.glfw.GLFW;

/**
 * Teclas do mod, registradas de verdade — assim aparecem em Opcoes > Controles
 * e o jogador pode trocar.
 *
 * O flash (R) ainda e lido cru no InputHandler; so o album passou por aqui.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RECKeys {

    public static final String CATEGORY = "key.categories.recmod";

    // C: F e a troca de mao do vanilla e as duas acoes disparavam no mesmo aperto.
    public static final KeyMapping OPEN_ALBUM = new KeyMapping(
            "key.recmod.album",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY
    );

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_ALBUM);
    }

    private RECKeys() {}
}
