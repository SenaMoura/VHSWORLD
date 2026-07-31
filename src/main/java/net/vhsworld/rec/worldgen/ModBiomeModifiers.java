package net.vhsworld.rec.worldgen;

import com.mojang.serialization.Codec;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.vhsworld.rec.RECMod;

/**
 * Os serializadores de BiomeModifier do mod.
 *
 * Os cinco modifiers que ja existiam (`add_anomaly`, `add_stoneman`, os minerios)
 * usam tipos que o proprio Forge registra — `forge:add_spawns`, `forge:add_features`
 * — e por isso nunca precisaram de uma classe destas. `recmod:overworld_music` e o
 * primeiro tipo NOSSO, e sem este registro o Forge nao sabe ler o `"type"` do json
 * e o arquivo e descartado ao carregar o datapack.
 */
public class ModBiomeModifiers {

    public static final DeferredRegister<Codec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, RECMod.MOD_ID);

    /** A trilha do overworld, que nao cabe num bioma nosso. Ver OverworldMusicModifier. */
    public static final RegistryObject<Codec<? extends BiomeModifier>> OVERWORLD_MUSIC =
            SERIALIZERS.register("overworld_music", () -> OverworldMusicModifier.CODEC);

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
