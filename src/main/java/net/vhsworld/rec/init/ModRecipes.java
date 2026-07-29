package net.vhsworld.rec.init;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.recipe.RFReceiverRecipe;

/**
 * O tipo de receita da bancada do mod. Ter um RecipeType proprio e o que separa as
 * receitas do Receptor de Frequencia das da mesa de trabalho: as nossas nao aparecem
 * no craft comum, e o craft comum nao aparece no Receptor.
 */
public class ModRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, RECMod.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, RECMod.MOD_ID);

    public static final RegistryObject<RecipeType<RFReceiverRecipe>> RF_RECEIVER_TYPE =
            RECIPE_TYPES.register("rf_receiver", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return RECMod.MOD_ID + ":rf_receiver";
                }
            });
    public static final RegistryObject<RecipeSerializer<RFReceiverRecipe>> RF_RECEIVER_SERIALIZER =
            RECIPE_SERIALIZERS.register("rf_receiver", RFReceiverRecipe.Serializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
