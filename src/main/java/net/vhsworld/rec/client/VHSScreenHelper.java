package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import net.vhsworld.rec.item.ModSounds;

import java.util.Random;

public class VHSScreenHelper {

    private static final Random random = new Random();
    private static SimpleSoundInstance ambienceSound;

    // Toca o som de terror em loop
    public static void ensureAmbienceSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && (ambienceSound == null || !mc.getSoundManager().isActive(ambienceSound))) {
            ambienceSound = new SimpleSoundInstance(
                    ModSounds.HORROR_AMBIENCE.get().getLocation(),
                    SoundSource.MASTER,
                    1.0f, 1.0f,
                    RandomSource.create(),
                    true, 0,
                    SimpleSoundInstance.Attenuation.NONE,
                    0.0D, 0.0D, 0.0D, true
            );
            mc.getSoundManager().play(ambienceSound);
        }
    }

    public static void stopAmbienceSound() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && ambienceSound != null) {
            mc.getSoundManager().stop(ambienceSound);
            ambienceSound = null;
        }
    }

    // Renderiza o fundo preto estático com ruído e scanlines
    public static void renderVHSBackground(GuiGraphics guiGraphics, int width, int height, String subTitleText) {
        ensureAmbienceSound();

        long time = System.currentTimeMillis();

        // 1. Fundo escuro com ruído
        int bgTone = 8 + random.nextInt(6);
        int bgColor = (0xFF << 24) | (bgTone << 16) | (bgTone << 8) | (bgTone + 10);
        guiGraphics.fill(0, 0, width, height, bgColor);

        // 2. Scanlines
        for (int y = 0; y < height; y += 2) {
            guiGraphics.fill(0, y, width, y + 1, 0x66000000);
        }

        // 3. Estática / Noise
        for (int i = 0; i < 300; i++) {
            int rx = random.nextInt(width);
            int ry = random.nextInt(height);
            int rw = random.nextInt(25) + 1;
            int gray = random.nextInt(180);
            int alpha = random.nextInt(90) + 30;
            int noiseColor = (alpha << 24) | (gray << 16) | (gray << 8) | gray;

            guiGraphics.fill(rx, ry, rx + rw, ry + 1, noiseColor);
        }

        // 4. Glitches horizontais
        if (random.nextFloat() < 0.35f) {
            int glitchY = random.nextInt(height);
            int glitchHeight = random.nextInt(15) + 2;
            int glitchAlpha = random.nextInt(100) + 40;
            int glitchColor = (glitchAlpha << 24) | 0x00FFFFFF;

            guiGraphics.fill(0, glitchY, width, glitchY + glitchHeight, glitchColor);
        }

        // 5. HUD estilo Camcorder no topo
        Minecraft mc = Minecraft.getInstance();
        int textGlitchX = (random.nextFloat() < 0.10f) ? (random.nextInt(6) - 3) : 0;

        if ((time / 400) % 2 == 0) {
            guiGraphics.drawString(mc.font, "● REC", 15 + textGlitchX, 12, 0xFFFF0000, false);
        }

        guiGraphics.drawString(mc.font, "PLAY", 15 + textGlitchX, 24, 0xFFFFFFFF, false);
        if (subTitleText != null && !subTitleText.isEmpty()) {
            guiGraphics.drawString(mc.font, subTitleText, 15 + textGlitchX, 36, 0xAAAAAAAA, false);
        }
    }
}