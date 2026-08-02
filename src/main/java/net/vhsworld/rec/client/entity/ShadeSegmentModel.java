package net.vhsworld.rec.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.ShadeSegmentEntity;

/**
 * O modelo do Anomalo da Sombra.
 *
 * Ele nao tem pernas — logo, nao existe passada para animar. O deslocamento e um
 * ARRASTO, e a animacao tem que vender isso: o corpo inclina para a frente na
 * direcao da marcha e a coluna ondula, como fita magnetica sendo puxada.
 *
 * Preso (luz ou olhar), o movimento inteiro morre — inclusive a ondulacao. E a mesma
 * decisao do Homem de Pedra e pelo mesmo motivo: qualquer tremor de "respiracao"
 * enquanto ele deveria estar parado avisa que a trava e temporaria, e o jogador para
 * de confiar na tocha. A trava tem que parecer definitiva para valer alguma coisa.
 */
public class ShadeSegmentModel extends EntityModel<ShadeSegmentEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(RECMod.MOD_ID, "shade_segment"), "main");

    private final ModelPart segment;
    private final ModelPart head;
    private final ModelPart mouth;
    private final ModelPart body;
    private final ModelPart[] ribs;

    public ShadeSegmentModel(ModelPart root) {
        this.segment = root.getChild("shade_segment");
        this.head = this.segment.getChild("head");
        this.mouth = this.head.getChild("mouth");
        this.body = this.head.getChild("main_body");
        this.ribs = new ModelPart[]{
                this.body.getChild("ribs"), this.body.getChild("ribs2"), this.body.getChild("ribs3"),
                this.body.getChild("ribs4"), this.body.getChild("ribs5"), this.body.getChild("ribs6")};
    }

    @Override
    public void setupAnim(ShadeSegmentEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isHeld()) {
            this.segment.xRot = 0.0F;
            this.segment.zRot = 0.0F;
            this.body.zRot = 0.0F;
            this.mouth.xRot = 0.0F;
            for (ModelPart rib : this.ribs) rib.zRot = 0.0F;
            return;
        }

        // A ondulacao ganha corpo nos primeiros segundos soltos, em vez de comecar
        // no maximo. O arranque e a metade do susto: ele estava morto na parede e
        // agora esta vindo, e o olho tem que conseguir ver essa passagem.
        float wake = Math.min(1.0F, entity.getLooseTicks() / 20.0F);
        float wave = ageInTicks * 0.35F;

        this.segment.xRot = -0.22F * wake;                       // joga para a frente
        this.segment.zRot = Mth.cos(wave) * 0.10F * wake;
        this.body.zRot = Mth.cos(wave + (float) Math.PI) * 0.07F * wake;

        // As costelas abrem e fecham fora de fase entre os lados: da a ela o volume
        // de bicho respirando, e nao de tira de pano balancando.
        for (int i = 0; i < this.ribs.length; i++) {
            float phase = wave + i * 0.7F;
            float side = (i % 2 == 0) ? 1.0F : -1.0F;
            this.ribs[i].zRot = Mth.cos(phase) * 0.16F * wake * side;
        }

        this.mouth.xRot = (0.20F + Mth.cos(wave * 0.5F) * 0.12F) * wake;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               float r, float g, float b, float a) {
        this.segment.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
