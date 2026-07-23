package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.photo.PhotoCapture;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.item.ModSounds;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class InputHandler {

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        // Ignora enquanto uma tela (inventário, pause, chat...) estiver aberta
        if (mc.screen != null) return;

        int key = event.getKey();
        int action = event.getAction();

        // --- APAGÃO: apertar ESPAÇO 25 vezes para religar a câmera ---
        if (CamcorderOverlay.isBatteryDead) {
            if (key == GLFW.GLFW_KEY_SPACE && action == GLFW.GLFW_PRESS) {
                CamcorderOverlay.miniGameProgress += CamcorderOverlay.pressStep();

                if (CamcorderOverlay.miniGameProgress >= CamcorderOverlay.MAX_PROGRESS) {
                    // Câmera religada: bateria cheia e HUD normal de volta
                    CamcorderOverlay.isBatteryDead = false;
                    CamcorderOverlay.batteryLevel = 100.0f;
                    CamcorderOverlay.miniGameProgress = 0.0f;
                }
            }
            // Durante o apagão a câmera está desligada: R não dispara flash
            return;
        }

        // --- FLASH: segurar R para carregar e soltar para disparar ---
        // Sem a câmera ativa não há flash (ver effectsOnlyWhenHolding no config).
        if (key == GLFW.GLFW_KEY_R && CameraState.isActive()) {
            if (action == GLFW.GLFW_PRESS) {
                CamcorderOverlay.isChargingFlash = true;
                CamcorderOverlay.flashChargeTime = 0.0f;
            } else if (action == GLFW.GLFW_RELEASE && CamcorderOverlay.isChargingFlash) {
                CamcorderOverlay.isChargingFlash = false;
                CamcorderOverlay.flashChargeTime = 0.0f;
                // Clarão branco na tela + som do flash da filmadora
                CamcorderOverlay.activeFlashAlpha = 1.0f;
                if (CameraState.audible()) {
                    mc.player.playSound(ModSounds.FLASH.get(), CameraState.volume(1.0f), 1.0f);
                }
                // O clarão não é só efeito: é o obturador. A foto sai daqui.
                if (RECConfig.CLIENT.photos.get()) {
                    PhotoCapture.request();
                }
            }
        }
    }
}
