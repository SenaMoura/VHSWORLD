package net.vhsworld.rec.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.InvertedSilhouetteEntity;

/**
 * O modelo da Silhueta Invertida.
 *
 * E o esqueleto do jogador, e a animacao tambem e a do jogador — de proposito. Ela se
 * passa por gente a distancia, e qualquer trejeito de monstro (membro torto, passada
 * pesada, cabeca de lado) entregaria a mentira antes da hora. O que entrega, quando
 * entrega, e o comportamento: ela anda para tras.
 *
 * ⚠️ A passada e INVERTIDA quando ela recua. O jogo manda um `limbSwing` que so sabe
 * quanto o bicho andou, nao para onde; sem inverter, ela recuaria com a perna indo
 * para a frente, e o olho pega isso na hora, mesmo sem saber nomear.
 */
public class InvertedSilhouetteModel extends EntityModel<InvertedSilhouetteEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(RECMod.MOD_ID, "inverted_silhouette"), "main");

    private final ModelPart waist;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public InvertedSilhouetteModel(ModelPart root) {
        this.waist = root.getChild("waist");
        this.head = this.waist.getChild("head");
        this.rightArm = this.waist.getChild("right_arm");
        this.leftArm = this.waist.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    @Override
    public void setupAnim(InvertedSilhouetteEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // A cabeca acompanha, mas so ate certo ponto: o corpo dela ja esta virado
        // para voce (o servidor gira o yBodyRot), entao o resto e um ajuste fino.
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        float swing = limbSwing * 0.6662F;
        float amount = Math.min(limbSwingAmount, 1.0F);
        float dir = entity.isBacking() ? -1.0F : 1.0F;

        this.rightLeg.xRot = Mth.cos(swing) * 1.4F * amount * dir;
        this.leftLeg.xRot = Mth.cos(swing + (float) Math.PI) * 1.4F * amount * dir;
        this.rightArm.xRot = Mth.cos(swing + (float) Math.PI) * 1.0F * amount * dir;
        this.leftArm.xRot = Mth.cos(swing) * 1.0F * amount * dir;

        // Os bracos ficam colados no corpo, sem o afastamento do jogador vanilla. E o
        // que da a ela a silhueta LIMPA que o desenho pede: sem vao entre braco e
        // tronco, de longe ela vira uma unica mancha preta com forma de gente.
        this.rightArm.zRot = 0.0F;
        this.leftArm.zRot = 0.0F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               float r, float g, float b, float a) {
        this.waist.render(pose, buffer, light, overlay, r, g, b, a);
        this.rightLeg.render(pose, buffer, light, overlay, r, g, b, a);
        this.leftLeg.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
