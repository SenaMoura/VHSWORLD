package net.vhsworld.rec.client.entity.geom;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * ARQUIVO GERADO — nao editar a mao.
 * Fonte: Shade_Segment.bbmodel
 * Gerador: tools/bbmodel_to_geometry.py
 *
 * A geometria vem do .bbmodel, e nao do .java que o Blockbench exporta: o export
 * espalhou os membros do Crawler_Void a nove blocos do corpo, e ja tinha historico
 * de nome de parte invalido e de partes que se sobrescreviam. A animacao fica na
 * classe de modelo, nao aqui.
 *
 * Partes na raiz: "shade_segment"
 */
public final class ShadeSegmentGeometry {

    private ShadeSegmentGeometry() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition shadeSegment = root.addOrReplaceChild("shade_segment", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition head = shadeSegment.addOrReplaceChild("head", CubeListBuilder.create()
        .texOffs(0, 22).addBox(-4.0F, -46.0F, -1.0F, 8.0F, 9.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition mouth = head.addOrReplaceChild("mouth", CubeListBuilder.create(), PartPose.offset(4.0F, -41.5F, 6.5F));
        PartDefinition cubeR = mouth.addOrReplaceChild("cube_r", CubeListBuilder.create().texOffs(0, 65).addBox(-3.9074F, -4.5F, -1.1014F, 4.0F, 9.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 1.9635F, 0.0F));
        PartDefinition cubeR2 = mouth.addOrReplaceChild("cube_r_2", CubeListBuilder.create().texOffs(10, 65).addBox(-0.0926F, -4.5F, -1.1014F, 4.0F, 9.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.0F, 0.0F, 0.0F, 0.0F, -2.0508F, 0.0F));
        PartDefinition mainBody = head.addOrReplaceChild("main_body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition cubeR3 = mainBody.addOrReplaceChild("cube_r_3", CubeListBuilder.create().texOffs(0, 39).addBox(-3.0F, 2.0F, 0.0F, 6.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -39.0F, 0.0F, 0.1309F, 0.0F, 0.0F));
        PartDefinition cubeR4 = mainBody.addOrReplaceChild("cube_r_4", CubeListBuilder.create().texOffs(32, 22).addBox(-4.0F, 6.0F, -1.0F, 8.0F, 7.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -40.3346F, 1.2776F, -0.0873F, 0.0F, 0.0F));
        PartDefinition cubeR5 = mainBody.addOrReplaceChild("cube_r_5", CubeListBuilder.create().texOffs(34, 0).addBox(-4.0F, 6.0F, -1.0F, 8.0F, 7.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -30.3346F, 0.2776F, -0.0873F, 0.0F, 0.0F));
        PartDefinition cubeR6 = mainBody.addOrReplaceChild("cube_r_6", CubeListBuilder.create().texOffs(32, 37).addBox(-3.0F, 2.0F, 0.0F, 6.0F, 8.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -20.0F, -1.0F, 0.0873F, 0.0F, 0.0F));
        PartDefinition cubeR7 = mainBody.addOrReplaceChild("cube_r_7", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 10.0F, -1.0F, 8.0F, 13.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -20.0F, -1.0F, 0.0873F, 0.0F, 0.0F));
        PartDefinition cubeR8 = mainBody.addOrReplaceChild("cube_r_8", CubeListBuilder.create().texOffs(0, 49).addBox(-3.0F, 2.0F, 0.0F, 6.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -30.0F, 1.0F, -0.1745F, 0.0F, 0.0F));
        PartDefinition ribs = mainBody.addOrReplaceChild("ribs", CubeListBuilder.create(), PartPose.offset(14.9973F, -32.0072F, 15.7819F));
        PartDefinition cubeR9 = ribs.addOrReplaceChild("cube_r_9", CubeListBuilder.create().texOffs(34, 15).addBox(-4.5F, -1.5F, -1.5F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.4973F, 1.5287F, -9.8495F, -0.0197F, -0.5729F, -0.0996F));
        PartDefinition cubeR10 = ribs.addOrReplaceChild("cube_r_10", CubeListBuilder.create().texOffs(24, 51).addBox(-4.5F, -1.5F, -1.5F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.5089F, 1.0485F, -5.5004F, -0.0235F, -0.791F, -0.0936F));
        PartDefinition cubeR11 = ribs.addOrReplaceChild("cube_r_11", CubeListBuilder.create().texOffs(64, 21).addBox(-4.5F, -0.5F, -1.5F, 7.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0334F, -1.0527F, -0.0813F));
        PartDefinition ribs4 = mainBody.addOrReplaceChild("ribs4", CubeListBuilder.create(), PartPose.offset(-14.9973F, -32.0072F, 15.7819F));
        PartDefinition cubeR12 = ribs4.addOrReplaceChild("cube_r_12", CubeListBuilder.create().texOffs(34, 15).mirror().addBox(-2.5F, -1.5F, -1.5F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(8.4973F, 1.5287F, -9.8495F, -0.0197F, 0.5729F, 0.0996F));
        PartDefinition cubeR13 = ribs4.addOrReplaceChild("cube_r_13", CubeListBuilder.create().texOffs(24, 51).mirror().addBox(-2.5F, -1.5F, -1.5F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(3.5089F, 1.0485F, -5.5004F, -0.0235F, 0.791F, 0.0936F));
        PartDefinition cubeR14 = ribs4.addOrReplaceChild("cube_r_14", CubeListBuilder.create().texOffs(64, 21).mirror().addBox(-2.5F, -0.5F, -1.5F, 7.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0334F, 1.0527F, 0.0813F));
        PartDefinition ribs2 = mainBody.addOrReplaceChild("ribs2", CubeListBuilder.create(), PartPose.offset(14.9973F, -23.0072F, 15.7819F));
        PartDefinition cubeR15 = ribs2.addOrReplaceChild("cube_r_15", CubeListBuilder.create().texOffs(34, 15).addBox(-4.5F, -1.5F, -1.5F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.4973F, 1.5287F, -9.8495F, -0.0197F, -0.5729F, -0.0996F));
        PartDefinition cubeR16 = ribs2.addOrReplaceChild("cube_r_16", CubeListBuilder.create().texOffs(24, 51).addBox(-4.5F, -1.5F, -1.5F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.5089F, 1.0485F, -5.5004F, -0.0235F, -0.791F, -0.0936F));
        PartDefinition cubeR17 = ribs2.addOrReplaceChild("cube_r_17", CubeListBuilder.create().texOffs(64, 21).addBox(-4.5F, -0.5F, -1.5F, 7.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0334F, -1.0527F, -0.0813F));
        PartDefinition ribs5 = mainBody.addOrReplaceChild("ribs5", CubeListBuilder.create(), PartPose.offset(-14.9973F, -23.0072F, 15.7819F));
        PartDefinition cubeR18 = ribs5.addOrReplaceChild("cube_r_18", CubeListBuilder.create().texOffs(34, 15).mirror().addBox(-2.5F, -1.5F, -1.5F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(8.4973F, 1.5287F, -9.8495F, -0.0197F, 0.5729F, 0.0996F));
        PartDefinition cubeR19 = ribs5.addOrReplaceChild("cube_r_19", CubeListBuilder.create().texOffs(24, 51).mirror().addBox(-2.5F, -1.5F, -1.5F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(3.5089F, 1.0485F, -5.5004F, -0.0235F, 0.791F, 0.0936F));
        PartDefinition cubeR20 = ribs5.addOrReplaceChild("cube_r_20", CubeListBuilder.create().texOffs(64, 21).mirror().addBox(-2.5F, -0.5F, -1.5F, 7.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0334F, 1.0527F, 0.0813F));
        PartDefinition ribs3 = mainBody.addOrReplaceChild("ribs3", CubeListBuilder.create(), PartPose.offset(14.9973F, -10.0072F, 15.7819F));
        PartDefinition cubeR21 = ribs3.addOrReplaceChild("cube_r_21", CubeListBuilder.create().texOffs(34, 15).addBox(-4.5F, -1.5F, -1.5F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.4973F, 1.5287F, -9.8495F, -0.0197F, -0.5729F, -0.0996F));
        PartDefinition cubeR22 = ribs3.addOrReplaceChild("cube_r_22", CubeListBuilder.create().texOffs(24, 51).addBox(-4.5F, -1.5F, -1.5F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.5089F, 1.0485F, -5.5004F, -0.0235F, -0.791F, -0.0936F));
        PartDefinition cubeR23 = ribs3.addOrReplaceChild("cube_r_23", CubeListBuilder.create().texOffs(64, 21).addBox(-4.5F, -0.5F, -1.5F, 7.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0334F, -1.0527F, -0.0813F));
        PartDefinition ribs6 = mainBody.addOrReplaceChild("ribs6", CubeListBuilder.create(), PartPose.offset(-14.9973F, -10.0072F, 15.7819F));
        PartDefinition cubeR24 = ribs6.addOrReplaceChild("cube_r_24", CubeListBuilder.create().texOffs(34, 15).mirror().addBox(-2.5F, -1.5F, -1.5F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(8.4973F, 1.5287F, -9.8495F, -0.0197F, 0.5729F, 0.0996F));
        PartDefinition cubeR25 = ribs6.addOrReplaceChild("cube_r_25", CubeListBuilder.create().texOffs(24, 51).mirror().addBox(-2.5F, -1.5F, -1.5F, 7.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(3.5089F, 1.0485F, -5.5004F, -0.0235F, 0.791F, 0.0936F));
        PartDefinition cubeR26 = ribs6.addOrReplaceChild("cube_r_26", CubeListBuilder.create().texOffs(64, 21).mirror().addBox(-2.5F, -0.5F, -1.5F, 7.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0334F, 1.0527F, 0.0813F));

        return LayerDefinition.create(mesh, 128, 128);
    }
}
