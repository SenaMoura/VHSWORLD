package net.vhsworld.rec.client.tape;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.CameraState;
import net.vhsworld.rec.client.ClientWorldData;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.item.ModSounds;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A gravacao de uma fita.
 *
 * A fita virgem, ao ser usada, chama {@link #start} e o mundo passa a ser fotografado
 * de tantos em tantos ticks por alguns segundos. Os quadros vao direto para o disco,
 * na pasta do mundo, e so podem ser revistos DEPOIS, no videocassete. Esse atraso e a
 * mecanica: voce grava um corredor vazio e so mais tarde, em seguranca, descobre o que
 * passou por tras de voce enquanto a fita rodava.
 *
 * A captura em si acontece no mesmo estagio de render que a foto (AFTER_WEATHER, depois
 * do mundo e antes do HUD), entao o quadro sai limpo, sem REC nem bateria por cima.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TapeRecorder {

    private static final Logger LOG = LogUtils.getLogger();

    /** Tamanho de cada quadro no disco. Fita velha nao precisa de 4K. */
    private static final int FRAME_W = 256;
    private static final int FRAME_H = 144;

    private static boolean recording = false;
    private static int remainingTicks = 0;
    private static int sinceCapture = 0;
    private static int frameIndex = 0;
    private static int frameEvery = 10;
    private static Path reelDir;
    private static boolean captureRequested = false;

    public static boolean isRecording() {
        return recording;
    }

    /** Segundos que ainda faltam da gravacao, para o HUD. */
    public static int secondsLeft() {
        return (remainingTicks + 19) / 20;
    }

    /**
     * Comeca a gravar. Devolve false se ja havia uma fita rodando (nao empilha).
     * Sem argumento para ser chamada por metodo-referencia do item, do lado cliente.
     */
    public static boolean start() {
        Minecraft mc = Minecraft.getInstance();
        if (recording || mc.player == null) return false;
        if (!RECConfig.CLIENT.tapes.get()) return false;

        try {
            reelDir = ClientWorldData.worldDir().resolve("tapes")
                    .resolve("reel_" + System.currentTimeMillis());
            Files.createDirectories(reelDir);
        } catch (Exception e) {
            LOG.error("Nao consegui criar a pasta da fita", e);
            return false;
        }

        recording = true;
        frameEvery = RECConfig.CLIENT.tapeFrameEveryTicks.get();
        remainingTicks = RECConfig.CLIENT.tapeSeconds.get() * 20;
        sinceCapture = frameEvery;   // o primeiro quadro sai ja no proximo frame
        frameIndex = 0;

        if (CameraState.audible()) {
            mc.player.playSound(ModSounds.TAPE_PLAYER.get(), CameraState.volume(0.8f), 1.0f);
        }
        return true;
    }

    public static void tick(Minecraft mc) {
        if (!recording) return;

        if (mc.player == null || mc.level == null) {   // saiu do mundo no meio: fecha a fita
            finish(mc);
            return;
        }

        remainingTicks--;
        if (++sinceCapture >= frameEvery) {
            sinceCapture = 0;
            captureRequested = true;   // o pixel de verdade sai no estagio de render
        }

        if (remainingTicks <= 0) finish(mc);
    }

    private static void finish(Minecraft mc) {
        recording = false;
        captureRequested = false;

        try {
            TapeLibrary.writeIndex(reelDir, frameIndex, frameEvery);
            TapeLibrary.trim();
        } catch (Exception e) {
            LOG.error("Nao consegui fechar a fita", e);
        }

        if (mc.player != null && CameraState.audible()) {
            mc.player.playSound(ModSounds.CAMERA_OFF.get(), CameraState.volume(0.7f), 1.2f);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!captureRequested) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        captureRequested = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        NativeImage frame = null;
        NativeImage small = null;
        try {
            frame = Screenshot.takeScreenshot(mc.getMainRenderTarget());
            small = new NativeImage(FRAME_W, FRAME_H, false);
            frame.resizeSubRectTo(0, 0, frame.getWidth(), frame.getHeight(), small);
            small.writeToFile(reelDir.resolve(String.format("frame_%03d.png", frameIndex)));
            frameIndex++;
        } catch (Throwable t) {
            LOG.error("Falha ao gravar um quadro da fita", t);
        } finally {
            if (small != null) small.close();
            if (frame != null) frame.close();
        }
    }

    /** O ponto vermelho de REC e a contagem regressiva enquanto a fita roda. */
    public static final IGuiOverlay REC_HUD = (gui, g, partialTick, width, height) -> {
        if (!recording) return;
        Minecraft mc = Minecraft.getInstance();

        int cx = width / 2;
        int y = 24;

        // Ponto vermelho piscando.
        boolean on = (mc.level != null) && (mc.level.getGameTime() / 10) % 2 == 0;
        if (on) g.fill(cx - 34, y + 1, cx - 28, y + 7, 0xFFFF2020);

        int s = secondsLeft();
        String label = String.format("REC  0:%02d", s);
        g.drawString(mc.font, label, cx - 24, y, 0xFFFFFFFF, true);
    };

    private TapeRecorder() {}
}
