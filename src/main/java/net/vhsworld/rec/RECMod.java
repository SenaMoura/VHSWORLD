package net.vhsworld.rec;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.init.ModBlocks;
import net.vhsworld.rec.init.ModEntities;
import net.vhsworld.rec.init.ModItems;
import net.vhsworld.rec.init.ModMenus;
import net.vhsworld.rec.init.ModRecipes;
import net.vhsworld.rec.item.ModSounds;
import net.vhsworld.rec.loot.ModLootModifiers;
import net.vhsworld.rec.net.RECNetwork;
import net.vhsworld.rec.worldgen.ModBiomeModifiers;
import net.vhsworld.rec.worldgen.ModChunkGenerators;
import org.slf4j.Logger;

@Mod(RECMod.MOD_ID)
public class RECMod {
    public static final String MOD_ID = "recmod";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RECMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Config primeiro: as mecanicas leem os valores dele desde o primeiro tick.
        ModLoadingContext ctx = ModLoadingContext.get();
        ctx.registerConfig(ModConfig.Type.CLIENT, RECConfig.CLIENT_SPEC, "recmod-client.toml");
        ctx.registerConfig(ModConfig.Type.COMMON, RECConfig.COMMON_SPEC, "recmod-common.toml");

        //Registra os itens no clico de vida do mod
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);

        ModSounds.register(modEventBus);

        // Bancada do mod: menu (tela com slots) + tipo de receita proprio.
        ModMenus.register(modEventBus);
        ModRecipes.register(modEventBus);

        ModLootModifiers.register(modEventBus);
        ModChunkGenerators.register(modEventBus);
        ModBiomeModifiers.register(modEventBus);

        // O canal do mod. Registrar os pacotes na CONSTRUCAO e nao no setup: o
        // handshake com o cliente pode acontecer antes do commonSetup terminar, e um
        // canal sem codec no meio do handshake derruba a conexao.
        RECNetwork.register();

        // Aqui vamos registar os Itens, Blocos e Sons no futuro!
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("RECMod inicializado com sucesso!");
    }
}