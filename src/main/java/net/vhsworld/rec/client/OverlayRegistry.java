package net.vhsworld.rec.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.sanity.SanityOverlay;

// Registra o HUD da filmadora (REC + BATERIA + apagão + flash) por cima de tudo.
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class OverlayRegistry {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("sanity_bar", SanityOverlay.SANITY_BAR);
        event.registerAboveAll("camcorder_hud", CamcorderOverlay.HUD_CAMCORDER);
        // Depois do HUD: numa fita de verdade, o chiado cai em cima do texto também.
        event.registerAboveAll("vhs_tape", VHSEffectOverlay.VHS_TAPE);
    }
}
