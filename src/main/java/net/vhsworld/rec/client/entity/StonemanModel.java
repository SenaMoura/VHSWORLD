package net.vhsworld.rec.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.StonemanEntity;

/**
 * O modelo do Homem de Pedra.
 *
 * A geometria e a que o Pedro modelou no Blockbench, transcrita por script a partir
 * do export (tools/build_stoneman_model.py) — nao foi redigitada. Duas coisas o
 * export nao entrega prontas: os nomes das partes vinham COM ESPACO ("Right Arm"),
 * que nao compila em Java, e o namespace era o placeholder "modid".
 *
 * Ele teve tres variantes de geometria ate a v1.38.0. Foram removidas: um monstro
 * com tres corpos diferentes vira "qual delas e essa?" na cabeca do jogador, e a
 * duvida que interessa aqui e outra — se ele se mexeu ou nao.
 *
 * A animacao e escrita aqui porque o formato modded_entity nao guarda animacao —
 * o setupAnim do export vem vazio.
 */
public class StonemanModel extends EntityModel<StonemanEntity> {

    public static final ModelLayerLocation BASE =
            new ModelLayerLocation(new ResourceLocation(RECMod.MOD_ID, "stoneman"), "main");

    private final ModelPart waist;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public StonemanModel(ModelPart root) {
        this.waist = root.getChild("waist");
        this.head = this.waist.getChild("head");
        this.body = this.waist.getChild("body");
        this.rightArm = this.waist.getChild("right_arm");
        this.leftArm = this.waist.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    /** Geometria de Stoneman.bbmodel. */
    public static LayerDefinition createBase() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition waist = root.addOrReplaceChild("waist", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));

        PartDefinition head = waist.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
        .texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)), PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition body = waist.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(16, 32).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(0.0F, -12.0F, 0.0F));

        PartDefinition rightArm = waist.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(40, 32).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(-5.0F, -10.0F, 0.0F));

        PartDefinition leftArm = waist.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(48, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(5.0F, -10.0F, 0.0F));

        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(-1.9F, 12.0F, 0.0F));

        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(0, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offset(1.9F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * Ele so se mexe quando ninguem esta olhando.
     *
     * Parado, e pedra: membro nenhum se move, nem um tremor. A imobilidade TOTAL e o
     * efeito — qualquer balancinho de respiracao entregaria que e um bicho vivo e
     * mataria o susto de ver que ele mudou de lugar.
     *
     * A cabeca continua apontada para onde estava: o servidor congela a rotacao, entao
     * os angulos que chegam aqui simplesmente param de mudar. Ele fica encarando.
     */
    @Override
    public void setupAnim(StonemanEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        if (entity.isWatched()) {
            this.rightArm.xRot = 0.0F;
            this.leftArm.xRot = 0.0F;
            this.rightLeg.xRot = 0.0F;
            this.leftLeg.xRot = 0.0F;
            this.waist.zRot = 0.0F;
            return;
        }

        // Passada pesada: pernas e bracos em contrafase, e a cintura jogando de um
        // lado para o outro no dobro da frequencia — peso de pedra, nao de gente.
        float swing = limbSwing * 0.6662F;
        float amount = Math.min(limbSwingAmount, 1.0F);

        this.rightLeg.xRot = Mth.cos(swing) * 1.15F * amount;
        this.leftLeg.xRot = Mth.cos(swing + (float) Math.PI) * 1.15F * amount;
        this.rightArm.xRot = Mth.cos(swing + (float) Math.PI) * 0.9F * amount;
        this.leftArm.xRot = Mth.cos(swing) * 0.9F * amount;
        this.waist.zRot = Mth.cos(swing * 2.0F) * 0.06F * amount;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               float r, float g, float b, float a) {
        this.waist.render(pose, buffer, light, overlay, r, g, b, a);
        this.rightLeg.render(pose, buffer, light, overlay, r, g, b, a);
        this.leftLeg.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
