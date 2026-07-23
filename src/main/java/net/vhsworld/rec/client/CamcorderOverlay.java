package net.vhsworld.rec.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.vhsworld.rec.RECMod;

public class CamcorderOverlay {

    // Moldura preta (viewfinder do camcorder) desenhada como overlay 2D — roda DEPOIS
    // do mundo, então não mexe no framebuffer e não conflita com shaderpacks (Oculus).
    // Substitui a antiga borda que vinha do pós-shader fisheye (que quebrava o render).
    private static final ResourceLocation VIGNETTE =
            new ResourceLocation(RECMod.MOD_ID, "textures/gui/vignette.png");

    public static float batteryLevel = 100.0f;
    public static boolean isBatteryDead = false;

    // Mini-Game Variables
    public static float miniGameProgress = 0.0f;
    public static final float MAX_PROGRESS = 100.0f;
    // Cada aperto de ESPAÇO enche a barra; ela vaza sozinha (ver DRAIN_PER_TICK)
    public static final int PRESSES_TO_RECHARGE = 25;
    public static final float PRESS_STEP = MAX_PROGRESS / PRESSES_TO_RECHARGE; // 4.0 por aperto
    // Quanto a barra desce a cada tick enquanto o jogador tenta enchê-la (dificuldade)
    public static final float DRAIN_PER_TICK = 0.8f;

    // --- VARIÁVEIS DO FLASH (TECLA R) ---
    public static boolean isChargingFlash = false;
    public static float flashChargeTime = 0.0f;
    public static final float MAX_FLASH_CHARGE = 60.0f;

    public static float activeFlashAlpha = 0.0f;
    public static float flashFadeSpeed = 0.05f;

    public static final IGuiOverlay HUD_CAMCORDER = (gui, guiGraphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        long gameTime = mc.level.getGameTime();

        // 0. MOLDURA PRETA (viewfinder) — sempre por cima do mundo, atrás do HUD.
        //    Textura esticada para a resolução atual; alfa cuida da transparência.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(VIGNETTE, 0, 0, width, height, 0.0f, 0.0f, 1024, 1024, 1024, 1024);
        RenderSystem.disableBlend();

        // 1. DESCARGA DA BATERIA
        if (!isBatteryDead) {
            batteryLevel -= 0.004f;
            if (batteryLevel <= 0.0f) {
                batteryLevel = 0.0f;
                isBatteryDead = true;
                miniGameProgress = 0.0f;
            }
        }

        // 2. HUD NORMAL (Bateria Ligada)
        if (!isBatteryDead) {
            long seconds = (gameTime / 20) % 60;
            long minutes = (gameTime / 1200) % 60;
            long hours = (gameTime / 7200);
            String timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds);

            if ((gameTime / 10) % 2 == 0) {
                guiGraphics.drawString(mc.font, "REC", (int)(width * 0.05), 20, 0xFFFF0000, true);
            }

            guiGraphics.drawString(mc.font, "PLAY SP", (int)(width * 0.05), 32, 0xFFFFFFFF, true);
            guiGraphics.drawString(mc.font, timeStr, (int)(width * 0.05), 44, 0xFFFFFFFF, true);

            int batColor = batteryLevel > 50 ? 0xFF00FF00 : (batteryLevel > 20 ? 0xFFFFFF00 : 0xFFFF0000);
            guiGraphics.drawString(mc.font, "BATTERY " + (int)batteryLevel + "%", (int)(width * 0.80), 20, batColor, true);

            // Carga do Flash
            if (isChargingFlash) {
                String chargeText = "FLASH: " + (int)((flashChargeTime / MAX_FLASH_CHARGE) * 100) + "%";
                guiGraphics.drawString(mc.font, chargeText, (width - mc.font.width(chargeText)) / 2, height - 50, 0xFFFFFF00, true);
            }
        }

        // 3. MINI-GAME DE EMERGÊNCIA (Bateria Zerada)
        else {
            guiGraphics.fill(0, 0, width, height, 0xFF000000);

            String warningText = "It's coming";
            int shakeX = (int) ((Math.random() - 0.5) * 6);
            int shakeY = (int) ((Math.random() - 0.5) * 6);
            int centerX = (width - mc.font.width(warningText)) / 2 + shakeX;
            int centerY = (height / 2) - 35 + shakeY;
            guiGraphics.drawString(mc.font, warningText, centerX, centerY, 0xFFFF0000, true);

            int pressesLeft = Math.max(0, PRESSES_TO_RECHARGE - (int) (miniGameProgress / PRESS_STEP));
            String actionText = "[ APERTE ESPACO: " + pressesLeft + " ]";
            int actionX = (width - mc.font.width(actionText)) / 2;
            guiGraphics.drawString(mc.font, actionText, actionX, (height / 2) + 5, 0xFFFFFF00, true);

            int barWidth = 160;
            int barHeight = 4;
            int barX = (width - barWidth) / 2;
            int barY = (height / 2) + 25;

            guiGraphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, 0xFF555555);
            guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF111111);

            int fillWidth = (int) ((miniGameProgress / MAX_PROGRESS) * barWidth);
            guiGraphics.fill(barX, barY, barX + fillWidth, barY + barHeight, 0xFF00FF00);
        }

        // 4. FLASH BRANCO
        if (activeFlashAlpha > 0.0f) {
            int alphaInt = (int)(activeFlashAlpha * 255);
            int flashColor = (alphaInt << 24) | 0x00FFFFFF;
            guiGraphics.fill(0, 0, width, height, flashColor);
        }
    };
}