package net.vhsworld.rec.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.init.ModBlocks;
import net.vhsworld.rec.item.ModSounds;
import net.vhsworld.rec.item.OreTrackerItem;
import net.vhsworld.rec.item.StructureLocatorItem;
import net.vhsworld.rec.item.TrackerItem;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * O lado cliente dos localizadores: acha o alvo, toca o coracao e desenha o ponto
 * vermelho que atravessa parede.
 *
 * Os itens (server-side) so gravam "estou ativo ate o tick X" e, no caso da estrutura,
 * a posicao achada. Aqui, todo tick, o mod le isso da mao do jogador e traduz em:
 *   - um ALVO no espaco (o minerio mais perto agora, ou a estrutura fixada);
 *   - o COMPASSO do coracao, que acelera conforme voce chega perto;
 *   - o PONTO desenhado no RenderLevelStageEvent, sem teste de profundidade, entao
 *     ele aparece mesmo atras da pedra — o "raio-x" temporario que o item promete.
 *
 * Tudo cliente, como o resto da camera; nao briga com shaderpack porque o ponto e
 * geometria simples desenhada depois das particulas, nao um post-shader.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TrackerSense {

    /** De quantos em quantos ticks o Rastreador revarre a rocha em volta. */
    private static final int SCAN_INTERVAL = 15;

    /** Alcance audivel do coracao para a estrutura (o ponto no horizonte vem antes disso). */
    private static final double STRUCTURE_HEARTBEAT_RANGE = 96.0;

    // --- estado lido pelo renderer ---
    private static Vec3 markerPos;
    private static boolean markerActive;

    // --- compasso do coracao ---
    private static int beatTimer;

    // --- cache da varredura de minerio ---
    private static int scanTimer;
    private static BlockPos oreTarget;

    private static Set<Block> valuableOres;

    private TrackerSense() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            clear();
            return;
        }

        ItemStack tracker = findActiveTracker(mc.player, mc.level);
        if (tracker.isEmpty()) {
            clear();
            return;
        }

        boolean structure = tracker.getItem() instanceof StructureLocatorItem;
        Vec3 target;
        double dist;

        if (structure) {
            BlockPos t = StructureLocatorItem.target(tracker);
            if (t == null) {          // rito nao achou nada: sem ponto, sem coracao.
                clear();
                return;
            }
            // No horizonte: mesma altura dos olhos, entao le como uma bussola 3D.
            target = new Vec3(t.getX() + 0.5, mc.player.getEyeY(), t.getZ() + 0.5);
            double dx = target.x - mc.player.getX();
            double dz = target.z - mc.player.getZ();
            dist = Math.sqrt(dx * dx + dz * dz);
        } else {
            if (++scanTimer >= SCAN_INTERVAL) {
                scanTimer = 0;
                rescanOre(mc);
            }
            if (oreTarget == null) {   // nada valioso no alcance agora.
                markerActive = false;
                markerPos = null;
                beatTimer = 0;
                return;
            }
            target = new Vec3(oreTarget.getX() + 0.5, oreTarget.getY() + 0.5, oreTarget.getZ() + 0.5);
            dist = mc.player.getEyePosition().distanceTo(target);
        }

        markerPos = target;
        markerActive = true;
        heartbeat(mc, dist, structure);
    }

    /** O minerio valioso mais proximo, por distancia ao quadrado. Roda no cliente. */
    private static void rescanOre(Minecraft mc) {
        Player p = mc.player;
        int r = RECConfig.CLIENT.trackerScanRadius.get();
        BlockPos center = p.blockPosition();
        Set<Block> targets = valuableOres();

        BlockPos best = null;
        double bestSq = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cur = new BlockPos.MutableBlockPos();

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    cur.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!targets.contains(mc.level.getBlockState(cur).getBlock())) continue;
                    double sq = dx * dx + dy * dy + dz * dz;
                    if (sq < bestSq) {
                        bestSq = sq;
                        best = cur.immutable();
                    }
                }
            }
        }
        oreTarget = best;
    }

    /**
     * O coracao: mais rapido, mais agudo e mais alto conforme o alvo chega perto.
     * Para a estrutura, longe demais fica em silencio — sobra so o ponto no horizonte.
     */
    private static void heartbeat(Minecraft mc, double dist, boolean structure) {
        if (!RECConfig.CLIENT.trackerHeartbeat.get()) return;
        if (!CameraState.audible()) return;

        double maxD = structure
                ? STRUCTURE_HEARTBEAT_RANGE
                : Math.max(8, RECConfig.CLIENT.trackerScanRadius.get());

        if (structure && dist > maxD) {
            beatTimer = 0;
            return;
        }

        double t = Mth.clamp(dist / maxD, 0.0, 1.0);
        int interval = (int) Mth.lerp(t, 7.0, 34.0);   // perto = rapido
        if (--beatTimer > 0) return;
        beatTimer = interval;

        float pitch = (float) Mth.lerp(t, 1.25, 0.85);
        float vol = (float) Mth.lerp(t, 1.0, 0.5);
        mc.player.playSound(ModSounds.HEARTBEAT.get(), CameraState.volume(vol), pitch);
    }

    private static ItemStack findActiveTracker(Player p, Level lvl) {
        ItemStack main = p.getMainHandItem();
        if (isEnabledActive(main, lvl)) return main;
        ItemStack off = p.getOffhandItem();
        if (isEnabledActive(off, lvl)) return off;
        return ItemStack.EMPTY;
    }

    private static boolean isEnabledActive(ItemStack s, Level lvl) {
        if (!(s.getItem() instanceof TrackerItem)) return false;
        if (!TrackerItem.isActive(s, lvl)) return false;
        if (s.getItem() instanceof OreTrackerItem) return RECConfig.CLIENT.oreTracker.get();
        if (s.getItem() instanceof StructureLocatorItem) return RECConfig.CLIENT.structureLocator.get();
        return true;
    }

    private static Set<Block> valuableOres() {
        if (valuableOres == null) {
            valuableOres = new HashSet<>(Arrays.asList(
                    Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
                    Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
                    Blocks.ANCIENT_DEBRIS,
                    Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE,
                    ModBlocks.CORRUPTED_STONE.get(),
                    ModBlocks.ALUMINUM_ORE.get(), ModBlocks.DEEPSLATE_ALUMINUM_ORE.get()));
        }
        return valuableOres;
    }

    private static void clear() {
        markerActive = false;
        markerPos = null;
        beatTimer = 0;
        scanTimer = 0;
        oreTarget = null;
    }

    // ------------------------------------------------------------------ RENDER

    /**
     * O ponto vermelho, desenhado depois das particulas, SEM teste de profundidade —
     * e por isso que ele aparece atraves da parede. Um quadrado que sempre encara a
     * camera (billboard) e cresce com a distancia para manter o tamanho aparente,
     * entao a estrutura a 500 blocos ainda e um ponto visivel, e o minerio a 3 blocos
     * nao vira uma mancha. Estetica de pixel VHS: e um quadrado, nao um circulo.
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (!markerActive || markerPos == null) return;
        if (!RECConfig.CLIENT.trackerMarker.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 camPos = cam.getPosition();
        double dx = markerPos.x - camPos.x;
        double dy = markerPos.y - camPos.y;
        double dz = markerPos.z - camPos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(dx, dy, dz);
        pose.mulPose(cam.rotation());   // encara a camera
        Matrix4f mat = pose.last().pose();

        float half = (float) Math.max(0.14, dist * 0.018);
        float pulse = 0.55f + 0.45f * Mth.sin((mc.level.getGameTime() + event.getPartialTick()) * 0.35f);
        int a = (int) (pulse * 255);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        quad(mat, half * 1.9f, 110, 0, 0, a / 3);      // brilho externo
        quad(mat, half, 225, 30, 30, a);               // nucleo vermelho
        quad(mat, half * 0.4f, 255, 160, 160, a);      // centro quente

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        pose.popPose();
    }

    private static void quad(Matrix4f mat, float h, int r, int g, int b, int a) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(mat, -h, -h, 0).color(r, g, b, a).endVertex();
        buf.vertex(mat, -h, h, 0).color(r, g, b, a).endVertex();
        buf.vertex(mat, h, h, 0).color(r, g, b, a).endVertex();
        buf.vertex(mat, h, -h, 0).color(r, g, b, a).endVertex();
        tess.end();
    }
}
