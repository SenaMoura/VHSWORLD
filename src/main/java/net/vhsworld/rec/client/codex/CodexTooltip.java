package net.vhsworld.rec.client.codex;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;

/**
 * A linha que aparece embaixo de todo item do mod.
 *
 * Trancado: "Use your flash to learn more about this item."
 * Destrancado: o atalho para abrir o registro.
 *
 * A frase entra em TODO item do namespace recmod, inclusive os que ainda nao tem
 * ficha escrita — assim nenhum item futuro nasce mudo por esquecimento.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CodexTooltip {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!RECConfig.CLIENT.codex.get()) return;

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem());
        if (id == null || !RECMod.MOD_ID.equals(id.getNamespace())) return;

        boolean known = Codex.get().isUnlocked(event.getItemStack().getItem());

        event.getToolTip().add(Component.translatable(
                        known ? "recmod.tooltip.known" : "recmod.tooltip.locked")
                .withStyle(known ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_GRAY)
                .withStyle(ChatFormatting.ITALIC));
    }

    private CodexTooltip() {}
}
