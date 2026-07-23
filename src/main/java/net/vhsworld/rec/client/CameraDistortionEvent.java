package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;

@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CameraDistortionEvent {

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!RECConfig.CLIENT.fisheye.get() || !CameraState.isActive()) return;

        // Efeito Fisheye no FOV
        event.setNewFovModifier(event.getFovModifier() * RECConfig.CLIENT.fisheyeStrength.get().floatValue());
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!RECConfig.CLIENT.handShake.get() || !CameraState.isActive()) return;

        float amplitude = RECConfig.CLIENT.handShakeStrength.get().floatValue();
        if (amplitude <= 0.0f) return;

        // Balanço suave de mão
        double time = mc.player.level().getGameTime() + event.getPartialTick();
        float shakeX = (float) Math.sin(time * 0.15) * amplitude;
        float shakeY = (float) Math.cos(time * 0.2) * amplitude;

        event.setPitch(event.getPitch() + shakeX);
        event.setYaw(event.getYaw() + shakeY);
    }
}