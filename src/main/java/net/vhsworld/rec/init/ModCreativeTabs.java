package net.vhsworld.rec.init;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;

// Coloca os itens do mod na aba "Ferramentas e Utilitários" do modo criativo.
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCreativeTabs {

    @SubscribeEvent
    public static void addToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.CAMCORDER);
            event.accept(ModItems.BATTERY);
        }
    }
}
