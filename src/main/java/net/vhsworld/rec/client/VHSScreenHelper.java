package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.item.ModSounds;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * O fundo das telas do mod: a mesma fita que o jogador ve dentro do jogo.
 *
 * A ideia e que o menu nao pareca a antessala do jogo, e sim o comeco dele. Quem
 * abre o VHSWORLD ja esta olhando por uma camera: mesmo letterbox, mesmas scanlines,
 * mesmo REC piscando, mesma fonte. Quando o mundo carrega, nada muda de linguagem —
 * so aparece chao embaixo.
 */
public class VHSScreenHelper {

    private static final Random random = new Random();
    private static final SimpleDateFormat STAMP = new SimpleDateFormat("dd MMM yyyy  HH:mm");

    private static SimpleSoundInstance ambienceSound;

    /** Instante em que o menu abriu, para o contador de fita correr de verdade. */
    private static final long OPENED_AT = System.currentTimeMillis();

    /** Janela do glitch forte: quando comecou e quanto dura. */
    private static long glitchUntil = 0L;
    private static long nextGlitch = System.currentTimeMillis() + 4000L;
    private static int glitchShift = 0;

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

    /**
     * Deslocamento horizontal do glitch neste frame.
     *
     * As telas usam isto para empurrar o proprio conteudo, para o rasgo pegar tambem
     * o texto e os botoes. Fita ruim nao estraga so o fundo.
     */
    public static int glitchShift() {
        long now = System.currentTimeMillis();

        if (now > nextGlitch) {
            glitchUntil = now + 90 + random.nextInt(120);
            nextGlitch = now + 3000 + random.nextInt(6000);
            glitchShift = (random.nextBoolean() ? 1 : -1) * (3 + random.nextInt(7));
        }

        return now < glitchUntil ? glitchShift : 0;
    }

    // Renderiza o fundo preto estático com ruído e scanlines
    public static void renderVHSBackground(GuiGraphics guiGraphics, int width, int height, String subTitleText) {
        ensureAmbienceSound();

        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();

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

        // 4. Faixa de tracking subindo, igual a do jogo
        int bandH = 22;
        int travel = height + bandH;
        int bandTop = (int) (height - ((now / 24) % travel));
        guiGraphics.fill(0, bandTop, width, bandTop + bandH, 0x18FFFFFF);
        for (int i = 0; i < 3; i++) {
            int ly = bandTop + random.nextInt(bandH);
            int lx = random.nextInt(Math.max(1, width / 2));
            guiGraphics.fill(lx, ly, Math.min(width, lx + width / 2), ly + 1, 0x55FFFFFF);
        }

        // 5. Glitches horizontais
        if (random.nextFloat() < 0.35f) {
            int glitchY = random.nextInt(height);
            int glitchHeight = random.nextInt(15) + 2;
            int glitchAlpha = random.nextInt(100) + 40;
            int glitchColor = (glitchAlpha << 24) | 0x00FFFFFF;

            guiGraphics.fill(0, glitchY, width, glitchY + glitchHeight, glitchColor);
        }

        // 6. Letterbox — o mesmo recorte do jogo, para o menu ja estar "dentro da fita"
        int barX = CamcorderOverlay.letterboxBarWidth(width);
        if (barX > 0) {
            guiGraphics.fill(0, 0, barX, height, 0xFF000000);
            guiGraphics.fill(width - barX, 0, width, height, 0xFF000000);
        }
        int barY = CamcorderOverlay.letterboxBarHeight(height);
        if (barY > 0) {
            guiGraphics.fill(0, 0, width, barY, 0xFF000000);
            guiGraphics.fill(0, height - barY, width, height, 0xFF000000);
        }

        // 7. HUD do camcorder, dentro do recorte
        int left = barX + 15;
        int top = barY + 12;
        int textGlitchX = (random.nextFloat() < 0.10f) ? (random.nextInt(6) - 3) : 0;

        boolean recOn = !RECConfig.CLIENT.showRecBlink.get() || (now / 400) % 2 == 0;
        if (recOn) {
            guiGraphics.drawString(mc.font, "● REC", left + textGlitchX, top, 0xFFFF0000, false);
        }

        guiGraphics.drawString(mc.font, "PLAY SP", left + textGlitchX, top + 12, 0xFFFFFFFF, false);
        guiGraphics.drawString(mc.font, tapeTime(now), left + textGlitchX, top + 24, 0xFFCCCCCC, false);

        if (subTitleText != null && !subTitleText.isEmpty()) {
            guiGraphics.drawString(mc.font, subTitleText, left + textGlitchX, top + 40, 0xAAAAAAAA, false);
        }

        // 8. Carimbo de data no canto, como as camcorders faziam
        String stamp = STAMP.format(new Date()).toUpperCase();
        guiGraphics.drawString(mc.font, stamp,
                width - barX - 15 - mc.font.width(stamp), height - barY - 20, 0xFFBBBBBB, false);
    }

    /** Contador de fita correndo desde que o jogo abriu. */
    private static String tapeTime(long now) {
        long seconds = (now - OPENED_AT) / 1000L;
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
    }
}
