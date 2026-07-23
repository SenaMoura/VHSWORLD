package net.vhsworld.rec.init;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.item.BatteryItem;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, RECMod.MOD_ID);

    // Registo do item Camcorder (empilhável até 1 unidade)
    public static final RegistryObject<Item> CAMCORDER = ITEMS.register("camcorder",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // Pilha: recarrega a bateria da filmadora ao usar (botão direito)
    public static final RegistryObject<Item> BATTERY = ITEMS.register("battery",
            () -> new BatteryItem(new Item.Properties().stacksTo(16)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}