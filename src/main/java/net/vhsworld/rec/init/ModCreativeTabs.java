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
            event.accept(ModItems.BATTERY);
            event.accept(ModItems.HAMMER);
            event.accept(ModItems.SHARP_SCISSORS);
            event.accept(ModItems.CORRUPTED_SWORD);
            event.accept(ModItems.CORRUPTED_PICKAXE);
            event.accept(ModItems.CORRUPTED_AXE);
            event.accept(ModItems.CORRUPTED_SHOVEL);
            event.accept(ModItems.CORRUPTED_HOE);
            event.accept(ModItems.CORRUPTED_DIAMOND_PICKAXE);
            event.accept(ModItems.CORRUPTED_COMPASS);
            event.accept(ModItems.ANCHOR);
            event.accept(ModItems.LURE_CLOCK);
            event.accept(ModItems.FRACTURE);
        }
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModItems.ALUMINUM_ORE_ITEM);
            event.accept(ModItems.DEEPSLATE_ALUMINUM_ORE_ITEM);
            event.accept(ModItems.CORRUPTED_STONE_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.RAW_ALUMINUM);
            event.accept(ModItems.ALUMINUM_INGOT);
            event.accept(ModItems.IRON_STICK);
            event.accept(ModItems.PRESSED_IRON);
            event.accept(ModItems.BLACK_GOO);
            event.accept(ModItems.REALITY_TEAR);
        }
    }
}
