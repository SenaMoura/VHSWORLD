package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;

@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CameraDistortionEvent {

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Efeito Fisheye no FOV
        event.setNewFovModifier(event.getFovModifier() * 1.35f);
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Balanço suave de mão
        double time = mc.player.level().getGameTime() + event.getPartialTick();
        float shakeX = (float) Math.sin(time * 0.15) * 0.18f;
        float shakeY = (float) Math.cos(time * 0.2) * 0.18f;

        event.setPitch(event.getPitch() + shakeX);
        event.setYaw(event.getYaw() + shakeY);
    }
}