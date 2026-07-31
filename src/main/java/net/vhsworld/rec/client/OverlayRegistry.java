package net.vhsworld.rec.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.escape.EscapeFx;
import net.vhsworld.rec.client.fx.InkTransition;
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
        // E a mancha vem por cima ATE do chiado: ela nao e um efeito da fita, ela e
        // o que engole a fita inteira.
        event.registerAboveAll("ink", InkTransition.INK);
        // O corte da fuga vem por cima de TUDO, inclusive da mancha. Nao e mais um efeito
        // do mod: e o fim da gravacao, e nada pode aparecer depois dele — nem o chiado,
        // nem a barra de sanidade. Ver EscapeFx.
        event.registerAboveAll("escape_cut", EscapeFx.OVERLAY);
    }
}
