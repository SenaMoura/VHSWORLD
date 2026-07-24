package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.init.ModBlocks;
import net.vhsworld.rec.item.ModSounds;

import java.util.Random;

/**
 * O tripe, do lado de quem esta atras dele.
 *
 * O tripe e um bloco plantado. Quando o jogador esta no raio de um, o HUD normal da
 * camera some (voce nao esta mais na frente da lente, esta atras dela) e um pequeno
 * monitor VHS acende no canto. O monitor NAO e um segundo render do mundo — e um feed
 * estilizado: chiado, "CAM 01", timecode. O que ele faz de util e AVISAR: quando um
 * Rasgo ou um mob hostil entra no raio do tripe, ele pisca MOTION e apita.
 *
 * Feito assim de proposito. Um segundo render de verdade e o tipo de coisa que ja
 * brigou com o shaderpack neste mod (a lente da v1.0.0). O feed estilizado entrega o
 * papel de camera de seguranca sem tocar no framebuffer.
 */
public final class TripodMonitor {

    private static final Random RANDOM = new Random();

    /** Cap no raio da varredura de blocos, para nao custar caro em raios grandes. */
    private static final int SCAN_CAP = 20;

    private static BlockPos tripodPos;
    private static int scanTimer;
    private static int motionTimer;
    private static int beepCooldown;

    public static void tick(Minecraft mc) {
        GadgetState.tripodActive = false;
        GadgetState.tripodMotion = false;

        if (mc.player == null || mc.level == null || !RECConfig.CLIENT.tripod.get()) {
            tripodPos = null;
            return;
        }

        int range = RECConfig.CLIENT.tripodRange.get();

        // O tripe cacheado ainda vale? (barato, todo tick)
        if (tripodPos != null) {
            boolean stillThere = mc.level.getBlockState(tripodPos).is(ModBlocks.TRIPOD.get());
            boolean inRange = mc.player.blockPosition().closerThan(tripodPos, range);
            if (!stillThere || !inRange) tripodPos = null;
        }

        // De vez em quando, procura o tripe mais proximo (caro, entao nao todo tick).
        if (tripodPos == null && ++scanTimer >= 20) {
            scanTimer = 0;
            tripodPos = findNearestTripod(mc, Math.min(range, SCAN_CAP));
        }

        if (tripodPos == null) return;

        GadgetState.tripodActive = true;

        // Movimento no raio do tripe: hostis (query barata) e Rasgos (varredura curta).
        if (++motionTimer >= 8) {
            motionTimer = 0;
            GadgetState.tripodMotion = motionNear(mc, tripodPos, range);
        } else {
            // Mantem o estado entre as varreduras para o monitor nao piscar.
            GadgetState.tripodMotion = lastMotion;
        }
        lastMotion = GadgetState.tripodMotion;

        // Bipe do monitor quando ha movimento.
        if (GadgetState.tripodMotion && RECConfig.CLIENT.tripodBeep.get() && CameraState.audible()) {
            if (--beepCooldown <= 0) {
                beepCooldown = 20;
                mc.player.playSound(ModSounds.MENU_BUTTON.get(), CameraState.volume(0.5f), 1.6f);
            }
        } else {
            beepCooldown = 0;
        }
    }

    private static boolean lastMotion;

    private static BlockPos findNearestTripod(Minecraft mc, int range) {
        BlockPos center = mc.player.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-range, -range, -range),
                center.offset(range, range, range))) {
            if (!mc.level.getBlockState(pos).is(ModBlocks.TRIPOD.get())) continue;
            double d = center.distSqr(pos);
            if (d < bestDist) {
                bestDist = d;
                best = pos.immutable();
            }
        }
        return best;
    }

    private static boolean motionNear(Minecraft mc, BlockPos tripod, int range) {
        // Hostis: query de entidade, barata.
        if (RECConfig.CLIENT.tripodSeesHostiles.get()) {
            AABB box = new AABB(tripod).inflate(range);
            for (Entity e : mc.level.getEntities((Entity) null, box, en -> en instanceof Enemy)) {
                if (e.isAlive()) return true;
            }
        }

        // Rasgos: varredura curta em volta do tripe.
        int r = Math.min(range, 8);
        for (BlockPos pos : BlockPos.betweenClosed(
                tripod.offset(-r, -r, -r), tripod.offset(r, r, r))) {
            if (mc.level.getBlockState(pos).is(ModBlocks.REALITY_TEAR.get())) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ monitor

    public static final IGuiOverlay MONITOR = (gui, g, partialTick, width, height) -> {
        if (!GadgetState.tripodActive) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Monitor no canto inferior direito.
        int w = Math.max(120, width / 5);
        int h = w * 9 / 16;
        int x = width - w - 8;
        int y = height - h - 40;

        boolean motion = GadgetState.tripodMotion;

        // Moldura (vermelha quando ha movimento).
        int frame = motion ? 0xFFAA2020 : 0xFF303038;
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, frame);
        g.fill(x, y, x + w, y + h, 0xFF05050A);

        // Chiado do feed.
        int flakes = motion ? 90 : 50;
        for (int i = 0; i < flakes; i++) {
            int px = x + RANDOM.nextInt(w);
            int py = y + RANDOM.nextInt(h);
            g.fill(px, py, px + 1, py + 1, 0x66FFFFFF);
        }
        // Scanlines.
        for (int sy = y; sy < y + h; sy += 3) {
            g.fill(x, sy, x + w, sy + 1, 0x33000000);
        }

        long t = mc.level.getGameTime();

        // CAM 01 + REC piscando.
        boolean blink = (t / 10) % 2 == 0;
        g.drawString(mc.font, "CAM 01", x + 4, y + 4, 0xFFCCCCCC, true);
        if (blink) g.fill(x + w - 12, y + 5, x + w - 6, y + 11, 0xFFFF2020);

        // Timecode.
        long s = (t / 20) % 60, m = (t / 1200) % 60;
        String tc = String.format("%02d:%02d", m, s);
        g.drawString(mc.font, tc, x + 4, y + h - 12, 0xFF88FF88, true);

        // Aviso de movimento.
        if (motion && blink) {
            String warn = "▲ MOTION";
            g.drawString(mc.font, warn, x + w - mc.font.width(warn) - 4, y + h - 12, 0xFFFF4040, true);
        }
    };

    private TripodMonitor() {}
}
