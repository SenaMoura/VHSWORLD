package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.client.photo.PhotoAlbumScreen;
import net.vhsworld.rec.client.sanity.SanityState;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.item.ModSounds;
import net.vhsworld.rec.RECMod;

@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientTickHandler {

    private static int blackoutTimer = 0;
    private static int stepTimer = 0;
    private static int stepInterval = 35;
    private static float soundVolume = 2.0f;
    private static boolean hasScreamed = false;
    private static boolean wasBatteryDead = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        SanityState.get().tick();

        // --- ÁLBUM DE FOTOS (tecla C) ---
        while (RECKeys.OPEN_ALBUM.consumeClick()) {
            if (mc.screen == null && RECConfig.CLIENT.photos.get()) {
                mc.setScreen(new PhotoAlbumScreen());
            }
        }

        // A lente da v1.0.0 voltou, mas atrás de uma trava: só carrega sem shaderpack.
        // Quem decide é o LegacyLensShader, todo tick — inclusive para descarregar
        // sozinho se o jogador ligar um shaderpack com a lente ativa.
        LegacyLensShader.tick(mc);

        // --- DESCARGA DA BATERIA (por tick, não por frame) ---
        // Antes isto vivia dentro do render do HUD, então a bateria durava mais em PC
        // fraco e menos em PC bom. Por tick o tempo é o mesmo para todo mundo.
        if (!CamcorderOverlay.isBatteryDead
                && RECConfig.CLIENT.batteryDrains.get()
                && CameraState.isActive()) {
            CamcorderOverlay.batteryLevel -= RECConfig.CLIENT.batteryDrainPerTick.get().floatValue();
            if (CamcorderOverlay.batteryLevel <= 0.0f) {
                CamcorderOverlay.batteryLevel = 0.0f;
                CamcorderOverlay.isBatteryDead = true;
                CamcorderOverlay.miniGameProgress = 0.0f;
            }
        }

        // --- LÓGICA DE CARGA E FADE DO FLASH ---
        if (CamcorderOverlay.isChargingFlash && !CamcorderOverlay.isBatteryDead) {
            if (CamcorderOverlay.flashChargeTime < CamcorderOverlay.maxFlashCharge()) {
                CamcorderOverlay.flashChargeTime += 1.0f;
            }
        }

        if (CamcorderOverlay.activeFlashAlpha > 0.0f) {
            CamcorderOverlay.activeFlashAlpha -= CamcorderOverlay.flashFadeSpeed();
            if (CamcorderOverlay.activeFlashAlpha < 0.0f) {
                CamcorderOverlay.activeFlashAlpha = 0.0f;
            }
        }

        // --- SONS DE TRANSIÇÃO (máquina desligando / religando) ---
        if (CameraState.audible()) {
            if (CamcorderOverlay.isBatteryDead && !wasBatteryDead) {
                // Bateria acabou -> câmera desligando
                mc.player.playSound(ModSounds.CAMERA_OFF.get(), CameraState.volume(1.0f), 1.0f);
            } else if (!CamcorderOverlay.isBatteryDead && wasBatteryDead) {
                // Câmera religada -> máquina voltando a funcionar
                mc.player.playSound(ModSounds.CAMERA_ON.get(), CameraState.volume(1.0f), 1.0f);
            }
        }
        wasBatteryDead = CamcorderOverlay.isBatteryDead;

        // --- LÓGICA DO APAGÃO / MINI-GAME ---
        if (CamcorderOverlay.isBatteryDead) {
            blackoutTimer++;

            // A barra vaza sozinha: o jogador precisa apertar ESPAÇO rápido p/ vencer o vazamento
            if (CamcorderOverlay.miniGameProgress > 0.0f) {
                CamcorderOverlay.miniGameProgress -= CamcorderOverlay.blackoutDrainPerTick();
                if (CamcorderOverlay.miniGameProgress < 0.0f) {
                    CamcorderOverlay.miniGameProgress = 0.0f;
                }
            }

            // Escuridão por N segundos -> grito
            int screamAt = RECConfig.CLIENT.secondsUntilScream.get() * 20;
            if (blackoutTimer >= screamAt && !hasScreamed) {
                hasScreamed = true;
                if (CameraState.audible()) {
                    mc.level.playLocalSound(
                            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            ModSounds.ENTITY_SCREAM.get(),
                            SoundSource.HOSTILE, CameraState.volume(3.5f), 1.0f, false
                    );
                }
            }

            if (hasScreamed && RECConfig.CLIENT.blackoutFootsteps.get() && CameraState.audible()) {
                stepTimer++;
                if (stepTimer >= stepInterval) {
                    stepTimer = 0;
                    mc.level.playLocalSound(
                            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            SoundEvents.STONE_STEP,
                            SoundSource.HOSTILE, CameraState.volume(soundVolume), 0.8f, false
                    );

                    if (stepInterval > 6) stepInterval -= 3;
                    if (soundVolume < 6.0f) soundVolume += 0.5f;
                }
            }

        } else {
            blackoutTimer = 0;
            hasScreamed = false;
            stepTimer = 0;
            stepInterval = 35;
            soundVolume = 2.0f;
        }
    }
}