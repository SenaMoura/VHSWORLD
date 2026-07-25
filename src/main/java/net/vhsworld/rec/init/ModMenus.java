package net.vhsworld.rec.init;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.menu.RFReceiverMenu;

/** Os menus (telas com slots) do mod. Hoje so o do Receptor de Frequencia. */
public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, RECMod.MOD_ID);

    public static final RegistryObject<MenuType<RFReceiverMenu>> RF_RECEIVER =
            MENUS.register("rf_receiver",
                    () -> new MenuType<>(RFReceiverMenu::new, FeatureFlags.VANILLA_SET));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
