package net.vhsworld.rec.client.photo;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.codex.Codex;
import net.vhsworld.rec.config.RECConfig;
import org.slf4j.Logger;

/**
 * Tira a foto: pega o frame que acabou de ser desenhado e guarda no album.
 *
 * O momento importa. A captura acontece DEPOIS do mundo e ANTES do HUD, entao a
 * foto sai limpa: sem REC, sem bateria, sem clarao branco por cima. E o que a
 * camera viu, nao o que a tela mostrou.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PhotoCapture {

    private static final Logger LOG = LogUtils.getLogger();

    /** Tamanho do arquivo guardado. Frame cheio em 4K por foto seria absurdo. */
    private static final int PHOTO_WIDTH = 512;
    private static final int PHOTO_HEIGHT = 288;

    /** Alcance da revelacao: o que estiver mais longe que isto nao sai no filme. */
    private static final double SUBJECT_RANGE = 48.0;

    /** Quao centralizado o alvo precisa estar (cosseno do angulo). ~60 graus de cone. */
    private static final double SUBJECT_CONE = 0.5;

    private static boolean pending = false;

    /** Chamado quando o flash dispara. A captura em si acontece no proximo frame. */
    public static void request() {
        pending = true;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!pending) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        pending = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        NativeImage frame = null;
        NativeImage photo = null;
        try {
            frame = Screenshot.takeScreenshot(mc.getMainRenderTarget());

            photo = new NativeImage(PHOTO_WIDTH, PHOTO_HEIGHT, false);
            frame.resizeSubRectTo(0, 0, frame.getWidth(), frame.getHeight(), photo);

            PhotoAlbum.get().add(photo, findSubject(mc));
            photo = null; // o album fecha a imagem

            // O mesmo disparo que guarda a foto tambem destranca o registro do que
            // estava enquadrado. Fotografar é o verbo de descoberta deste jogo.
            if (RECConfig.CLIENT.codex.get()) {
                Codex.unlockFromFlash(mc);
            }
        } catch (Throwable t) {
            if (photo != null) photo.close();
        } finally {
            if (frame != null) frame.close();
        }
    }

    /**
     * Quem estava na frente da lente na hora do clique.
     *
     * Hoje isto quase sempre devolve null, porque o mod ainda nao tem entidades
     * proprias e mobs comuns nao interessam. Quando as entidades existirem, elas
     * ja caem aqui sozinhas — nao ha nada a mudar neste metodo.
     */
    private static String findSubject(Minecraft mc) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();

        Entity best = null;
        double bestDot = SUBJECT_CONE;

        // Diagnostico: quando a foto sai vazia, e sempre por um destes tres motivos.
        int living = 0, tooFar = 0, offCamera = 0, filtered = 0;
        double closestDot = -1.0;

        boolean anyMob = RECConfig.CLIENT.photoCatchesAnyMob.get();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) continue;
            // Bicho, nunca item no chao: "CAPTURADO: TOCHA" nao assusta ninguem.
            if (!(entity instanceof LivingEntity)) continue;
            living++;

            if (!anyMob && !isSubjectWorthy(entity)) {
                filtered++;
                continue;
            }

            Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
            Vec3 delta = center.subtract(eye);

            double distance = delta.length();
            if (distance < 0.01 || distance > SUBJECT_RANGE) {
                tooFar++;
                continue;
            }

            double dot = delta.normalize().dot(look);
            closestDot = Math.max(closestDot, dot);

            if (dot > bestDot) {
                bestDot = dot;
                best = entity;
            } else {
                offCamera++;
            }
        }

        if (best == null) {
            LOG.info("[REC] foto vazia — bichos vistos: {}, fora do filtro: {}, longe demais: {}, "
                            + "fora do cone: {}, melhor centralizacao: {} (precisa > {}), photoCatchesAnyMob={}",
                    living, filtered, tooFar, offCamera, String.format("%.2f", closestDot),
                    SUBJECT_CONE, anyMob);
            return null;
        }

        LOG.info("[REC] foto com assunto: {}", best.getDisplayName().getString());
        return best.getDisplayName().getString();
    }

    /**
     * Filtro do que "vale" como revelacao.
     *
     * Enquanto nao houver entidade nossa, so o que nao e um bicho comum do jogo
     * conta — assim uma vaca no pasto nao vira o susto da foto. Quando o mod
     * tiver entidades proprias, e aqui que elas entram na lista.
     */
    private static boolean isSubjectWorthy(Entity entity) {
        return entity.getType().getDescriptionId().contains(RECMod.MOD_ID);
    }

    private PhotoCapture() {}
}
