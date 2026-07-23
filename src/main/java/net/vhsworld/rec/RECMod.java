package net.vhsworld.rec;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.vhsworld.rec.init.ModItems;
import net.vhsworld.rec.item.ModSounds;
import net.vhsworld.rec.loot.ModLootModifiers;
import org.slf4j.Logger;

@Mod(RECMod.MOD_ID)
public class RECMod {
    public static final String MOD_ID = "recmod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RECMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        //Registra os itens no clico de vida do mod
        ModItems.register(modEventBus);

        ModSounds.register(modEventBus);

        ModLootModifiers.register(modEventBus);

        // Aqui vamos registar os Itens, Blocos e Sons no futuro!
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("RECMod inicializado com sucesso!");
    }
}