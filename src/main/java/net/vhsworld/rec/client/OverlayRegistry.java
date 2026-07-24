package net.vhsworld.rec.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.sanity.SanityOverlay;
import net.vhsworld.rec.client.tape.TapeRecorder;

// Registra o HUD da filmadora (REC + BATERIA + apagão + flash) por cima de tudo.
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class OverlayRegistry {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("sanity_bar", SanityOverlay.SANITY_BAR);
        event.registerAboveAll("camcorder_hud", CamcorderOverlay.HUD_CAMCORDER);
        // A tinta da lente e o monitor do tripe entram por cima do HUD, mas antes do
        // chiado da fita, que continua sendo a ultima camada.
        event.registerAboveAll("infrared", InfraredOverlay.INFRARED);
        event.registerAboveAll("tripod_monitor", TripodMonitor.MONITOR);
        event.registerAboveAll("tape_rec", TapeRecorder.REC_HUD);
        // Depois de tudo: numa fita de verdade, o chiado cai em cima do texto também.
        event.registerAboveAll("vhs_tape", VHSEffectOverlay.VHS_TAPE);
    }
}
