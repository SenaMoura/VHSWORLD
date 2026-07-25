package net.vhsworld.rec.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.screen.RFReceiverScreen;
import net.vhsworld.rec.init.ModMenus;

/** Amarracoes de cliente que rodam no mod bus: hoje so ligar a tela ao menu do Receptor. */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MenuScreens.register(ModMenus.RF_RECEIVER.get(), RFReceiverScreen::new));
    }

    private ClientSetup() {}
}
