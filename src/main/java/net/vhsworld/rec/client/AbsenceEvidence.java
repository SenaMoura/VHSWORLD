package net.vhsworld.rec.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.director.Absence;
import net.vhsworld.rec.net.AbsencePacket;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * O VISOR: a lente ve o que o olho nao ve.
 *
 * Esta e a outra metade da ausencia, e a razao de ela ser terror em vez de sacanagem.
 * O mundo tirou a sua tocha; o olho nao tem como provar que ela estava ali. A lente tem.
 * Levante ela, aponte para o lugar, e o quadrado esta la — no ponto exato, parado, sem
 * explicacao.
 *
 * ⚠️ POR QUE ISTO NAO E "UM DETECTOR DE FANTASMA". A diferenca esta em quem comeca. Um
 * detector avisa: apita, acende, manda voce olhar, e ai o jogo esta te dando um objetivo.
 * Aqui NADA avisa. A marca fica muda para sempre se o jogador nao desconfiar sozinho. O
 * gesto de levantar a lente nasce da duvida dele — "peraí, essa tocha nao estava ali?" —
 * e por isso ele nao entedia: nao e tarefa, e conferir.
 *
 * ⚠️ E A CONFIRMACAO PIORA. Este arquivo devolve certeza, que e a coisa que o jogador
 * quer e a pior que ele pode receber: enquanto era memoria falha, dava para tocar a vida.
 * Confirmado, aquilo passa a existir. E o dilema em que o mod inteiro deveria morar — a
 * duvida e insuportavel, a certeza e perigosa.
 *
 * O gesto, por ora, e a lente infravermelha na mao (GadgetState.infraredActive). Nao e
 * um visor dedicado; e o gesto que ja existia no mod e serve exatamente para isto — e um
 * teste nao deve inventar item para provar uma ideia.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AbsenceEvidence {

    private AbsenceEvidence() {}

    /** Quantas marcas o mundo guarda ao mesmo tempo. */
    private static final int MEMORY = 16;

    /**
     * Quanto tempo uma marca dura, em ticks (cinco minutos).
     *
     * ⚠️ ELA TEM QUE APAGAR. Marca eterna transforma a base num museu de quadradinhos, e
     * a partir do terceiro o jogador para de sentir qualquer coisa — vira decoracao. O
     * esquecimento tambem e o que devolve a duvida: quem demorou demais para desconfiar
     * perde a chance de saber, e fica so com a suspeita. Que e o estado certo.
     */
    private static final int LIFETIME = 20 * 60 * 5;

    private static final class Evidence {
        final BlockPos pos;
        final Absence.Kind kind;
        int age;

        Evidence(BlockPos pos, Absence.Kind kind) {
            this.pos = pos;
            this.kind = kind;
        }
    }

    private static final List<Evidence> MARKS = new ArrayList<>();

    /** Chegou um aviso do servidor. Guarda calado — nada aparece na tela agora. */
    public static void remember(AbsencePacket packet) {
        MARKS.add(new Evidence(packet.pos(), packet.kind()));
        while (MARKS.size() > MEMORY) MARKS.remove(0);
    }

    /** Mundo novo, marcas velhas nao valem. */
    public static void clear() {
        MARKS.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            MARKS.clear();
            return;
        }
        if (mc.isPaused()) return;

        Iterator<Evidence> it = MARKS.iterator();
        while (it.hasNext()) {
            if (++it.next().age > LIFETIME) it.remove();
        }
    }

    /**
     * Desenha as marcas — so com a lente levantada.
     *
     * ⚠️ COM TESTE DE PROFUNDIDADE, ao contrario do ponto do rastreador. La o ponto tem
     * que atravessar parede porque e um rastreador e a graca e saber a direcao. Aqui e o
     * oposto: a prova ocupa um LUGAR do mundo, e ter que voltar ate onde voce jura ter
     * fincado a tocha e metade do gesto. Marca que aparece atraves da parede viraria
     * mapa, e mapa nao da medo.
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        if (MARKS.isEmpty()) return;
        if (!GadgetState.infraredActive) return;
        if (!RECConfig.CLIENT.absenceEvidence.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 camPos = cam.getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        // Explicito: a marca TEM que ser escondida por parede. Ver o comentario acima.
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);

        PoseStack pose = event.getPoseStack();

        for (Evidence mark : MARKS) {
            Vec3 at = Vec3.atCenterOf(mark.pos);
            double dx = at.x - camPos.x;
            double dy = at.y - camPos.y;
            double dz = at.z - camPos.z;

            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > 48.0D) continue;

            // Some no fim da vida: a prova envelhece na fita, nao apaga de uma vez.
            float life = 1.0f - (mark.age / (float) LIFETIME);
            float fade = Mth.clamp(life * 2.0f, 0.0f, 1.0f);

            // Chiado: a marca nao e estavel, ela treme como imagem de fita gasta.
            float flicker = 0.6f + 0.4f * Mth.sin(
                    (mc.level.getGameTime() + event.getPartialTick()) * 0.9f + mark.pos.hashCode());

            int alpha = (int) (fade * flicker * 200);
            if (alpha <= 4) continue;

            pose.pushPose();
            pose.translate(dx, dy, dz);
            pose.mulPose(cam.rotation());
            Matrix4f mat = pose.last().pose();

            float half = (float) Math.max(0.10D, dist * 0.012D);

            if (mark.kind == Absence.Kind.TORCH_TAKEN) {
                // O calor que ficou: o formato de uma chama que nao esta mais acesa.
                quad(mat, half * 2.2f, 120, 70, 20, alpha / 4);
                quad(mat, half, 235, 170, 60, alpha);
                quad(mat, half * 0.45f, 255, 240, 210, alpha);
            } else {
                // A porta: mais fria, e mais larga que alta, como uma fresta.
                quad(mat, half * 2.0f, 40, 80, 110, alpha / 4);
                quad(mat, half, 150, 205, 235, alpha);
            }

            pose.popPose();
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
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
