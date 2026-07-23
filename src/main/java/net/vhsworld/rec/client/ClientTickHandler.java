package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
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

        // FIX v1.0.3: o pós-shader vanilla (fisheye.json) distorcia/granulava o frame
        // inteiro e conflitava com shaderpacks do Oculus/Iris, corrompendo o render
        // das chunks distantes. Removido. Se algum efeito nosso ainda estiver ativo
        // (de versões antigas), garante que ele seja desligado.
        if (mc.gameRenderer.currentEffect() != null
                && mc.gameRenderer.currentEffect().getName().contains(RECMod.MOD_ID)) {
            mc.gameRenderer.shutdownEffect();
        }

        // --- LÓGICA DE CARGA E FADE DO FLASH ---
        if (CamcorderOverlay.isChargingFlash && !CamcorderOverlay.isBatteryDead) {
            if (CamcorderOverlay.flashChargeTime < CamcorderOverlay.MAX_FLASH_CHARGE) {
                CamcorderOverlay.flashChargeTime += 1.0f;
            }
        }

        if (CamcorderOverlay.activeFlashAlpha > 0.0f) {
            CamcorderOverlay.activeFlashAlpha -= CamcorderOverlay.flashFadeSpeed;
            if (CamcorderOverlay.activeFlashAlpha < 0.0f) {
                CamcorderOverlay.activeFlashAlpha = 0.0f;
            }
        }

        // --- SONS DE TRANSIÇÃO (máquina desligando / religando) ---
        if (CamcorderOverlay.isBatteryDead && !wasBatteryDead) {
            // Bateria acabou -> câmera desligando
            mc.player.playSound(ModSounds.CAMERA_OFF.get(), 1.0f, 1.0f);
        } else if (!CamcorderOverlay.isBatteryDead && wasBatteryDead) {
            // Câmera religada -> máquina voltando a funcionar
            mc.player.playSound(ModSounds.CAMERA_ON.get(), 1.0f, 1.0f);
        }
        wasBatteryDead = CamcorderOverlay.isBatteryDead;

        // --- LÓGICA DO APAGÃO / MINI-GAME ---
        if (CamcorderOverlay.isBatteryDead) {
            blackoutTimer++;

            // A barra vaza sozinha: o jogador precisa apertar ESPAÇO rápido p/ vencer o vazamento
            if (CamcorderOverlay.miniGameProgress > 0.0f) {
                CamcorderOverlay.miniGameProgress -= CamcorderOverlay.DRAIN_PER_TICK;
                if (CamcorderOverlay.miniGameProgress < 0.0f) {
                    CamcorderOverlay.miniGameProgress = 0.0f;
                }
            }

            // 5 segundos (100 ticks) de escuridão -> grito
            if (blackoutTimer >= 100 && !hasScreamed) {
                hasScreamed = true;
                mc.level.playLocalSound(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        ModSounds.ENTITY_SCREAM.get(),
                        SoundSource.HOSTILE, 3.5f, 1.0f, false
                );
            }

            if (hasScreamed) {
                stepTimer++;
                if (stepTimer >= stepInterval) {
                    stepTimer = 0;
                    mc.level.playLocalSound(
                            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            SoundEvents.STONE_STEP,
                            SoundSource.HOSTILE, soundVolume, 0.8f, false
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