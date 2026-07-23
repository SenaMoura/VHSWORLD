package net.vhsworld.rec.init;

import net.minecraft.world.item.BlockItem;
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

    // A filmadora foi removida: a camera nao e um item que se segura, e o estado
    // do mundo. O jogador ja esta filmando desde que acordou.

    // --- Aluminio: o metal da pilha ---
    public static final RegistryObject<Item> RAW_ALUMINUM = ITEMS.register("raw_aluminum",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ALUMINUM_INGOT = ITEMS.register("aluminum_ingot",
            () -> new Item(new Item.Properties()));

    // Itens dos blocos, para o minerio poder ser carregado e colocado de volta
    public static final RegistryObject<Item> ALUMINUM_ORE_ITEM = ITEMS.register("aluminum_ore",
            () -> new BlockItem(ModBlocks.ALUMINUM_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> DEEPSLATE_ALUMINUM_ORE_ITEM = ITEMS.register("deepslate_aluminum_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_ALUMINUM_ORE.get(), new Item.Properties()));

    // Pilha: recarrega a bateria da camera ao usar (botão direito)
    public static final RegistryObject<Item> BATTERY = ITEMS.register("battery",
            () -> new BatteryItem(new Item.Properties().stacksTo(16)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}