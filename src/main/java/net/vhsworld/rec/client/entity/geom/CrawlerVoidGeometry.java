package net.vhsworld.rec.client.entity.geom;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * ARQUIVO GERADO — nao editar a mao.
 * Fonte: Crawler_Void.bbmodel
 * Gerador: tools/bbmodel_to_geometry.py
 *
 * A geometria vem do .bbmodel, e nao do .java que o Blockbench exporta: o export
 * espalhou os membros do Crawler_Void a nove blocos do corpo, e ja tinha historico
 * de nome de parte invalido e de partes que se sobrescreviam. A animacao fica na
 * classe de modelo, nao aqui.
 *
 * Partes na raiz: "crawler_void"
 */
public final class CrawlerVoidGeometry {

    private CrawlerVoidGeometry() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition crawlerVoid = root.addOrReplaceChild("crawler_void", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition head = crawlerVoid.addOrReplaceChild("head", CubeListBuilder.create()
        .texOffs(192, 212).addBox(-4.0F, -14.0F, -4.0F, 8.0F, 13.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition mainBody = head.addOrReplaceChild("main_body", CubeListBuilder.create()
        .texOffs(134, 18).addBox(-6.0F, -11.0F, 4.0F, 12.0F, 9.0F, 8.0F, new CubeDeformation(0.0F))
        .texOffs(114, 166).addBox(-1.0F, -12.0F, 4.0F, 2.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition cubeR = mainBody.addOrReplaceChild("cube_r", CubeListBuilder.create().texOffs(134, 0).addBox(-6.0F, -10.0F, 4.0F, 12.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.9647F, 8.1363F, 0.2618F, 0.0F, 0.0F));
        PartDefinition cubeR2 = mainBody.addOrReplaceChild("cube_r_2", CubeListBuilder.create().texOffs(164, 68).addBox(-1.0F, -11.0F, 4.0F, 2.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.9647F, 8.1363F, 0.2618F, 0.0F, 0.0F));
        PartDefinition cubeR3 = mainBody.addOrReplaceChild("cube_r_3", CubeListBuilder.create().texOffs(70, 77).addBox(-8.0F, -13.0F, 4.0F, 16.0F, 13.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.4444F, 28.7616F, 1.1345F, 0.0F, 0.0F));
        PartDefinition cubeR4 = mainBody.addOrReplaceChild("cube_r_4", CubeListBuilder.create().texOffs(116, 151).addBox(-1.0F, -14.0F, 4.0F, 2.0F, 1.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -9.4444F, 28.7616F, 1.1345F, 0.0F, 0.0F));
        PartDefinition cubeR5 = mainBody.addOrReplaceChild("cube_r_5", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -13.0F, 4.0F, 16.0F, 13.0F, 18.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -18.1386F, 28.198F, 0.5672F, 0.0F, 0.0F));
        PartDefinition cubeR6 = mainBody.addOrReplaceChild("cube_r_6", CubeListBuilder.create().texOffs(130, 77).addBox(-1.0F, -14.0F, 4.0F, 2.0F, 2.0F, 18.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -18.1386F, 28.198F, 0.5672F, 0.0F, 0.0F));
        PartDefinition cubeR7 = mainBody.addOrReplaceChild("cube_r_7", CubeListBuilder.create().texOffs(0, 31).addBox(-7.0F, -13.0F, 4.0F, 14.0F, 13.0F, 18.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -27.5124F, 37.4988F, 0.1309F, 0.0F, 0.0F));
        PartDefinition cubeR8 = mainBody.addOrReplaceChild("cube_r_8", CubeListBuilder.create().texOffs(0, 132).addBox(-1.0F, -14.0F, 4.0F, 2.0F, 1.0F, 18.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -27.5124F, 37.4988F, 0.1309F, 0.0F, 0.0F));
        PartDefinition cubeR9 = mainBody.addOrReplaceChild("cube_r_9", CubeListBuilder.create().texOffs(62, 85).addBox(-1.0F, -3.5F, -1.5F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-4.0F, -42.659F, 49.1212F, -0.6109F, 0.0F, 0.0F));
        PartDefinition cubeR10 = mainBody.addOrReplaceChild("cube_r_10", CubeListBuilder.create().texOffs(150, 166).addBox(-1.0F, -3.5F, -1.5F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -42.659F, 45.1212F, -0.6109F, 0.0F, 0.0F));
        PartDefinition cubeR11 = mainBody.addOrReplaceChild("cube_r_11", CubeListBuilder.create().texOffs(158, 166).addBox(-1.0F, -3.5F, -1.5F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -42.659F, 53.1212F, -0.6109F, 0.0F, 0.0F));
        PartDefinition cubeR12 = mainBody.addOrReplaceChild("cube_r_12", CubeListBuilder.create().texOffs(166, 166).addBox(-1.0F, -3.5F, -1.5F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, -42.659F, 53.1212F, -0.6109F, 0.0F, 0.0F));
        PartDefinition cubeR13 = mainBody.addOrReplaceChild("cube_r_13", CubeListBuilder.create().texOffs(0, 170).addBox(-1.0F, -3.5F, -1.5F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, -42.659F, 44.1212F, -0.6109F, 0.0F, 0.0F));
        PartDefinition cubeR14 = mainBody.addOrReplaceChild("cube_r_14", CubeListBuilder.create().texOffs(8, 170).addBox(-1.0F, -3.5F, -1.5F, 2.0F, 9.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, -42.659F, 50.1212F, -0.6109F, 0.0F, 0.0F));
        PartDefinition arms3 = mainBody.addOrReplaceChild("arms_3", CubeListBuilder.create(), PartPose.offset(-33.5197F, -56.6326F, 54.5215F));
        PartDefinition cubeR15 = arms3.addOrReplaceChild("cube_r_15", CubeListBuilder.create().texOffs(68, 0).addBox(-58.5F, -3.5F, -6.0F, 21.0F, 11.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.9463F, 0.2261F, -1.8681F));
        PartDefinition cubeR16 = arms3.addOrReplaceChild("cube_r_16", CubeListBuilder.create().texOffs(0, 62).addBox(-6.5F, -3.5F, -6.0F, 23.0F, 11.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(19.0197F, 20.0271F, -11.8519F, 0.7561F, 0.6711F, 0.5303F));
        PartDefinition cubeR17 = arms3.addOrReplaceChild("cube_r_17", CubeListBuilder.create().texOffs(0, 85).addBox(-25.5F, -3.5F, -6.0F, 19.0F, 11.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(18.8718F, 13.7168F, -15.6806F, -0.4423F, 0.8887F, -1.0054F));
        PartDefinition cubeR18 = arms3.addOrReplaceChild("cube_r_18", CubeListBuilder.create().texOffs(56, 127).addBox(-37.5F, -3.5F, -6.0F, 12.0F, 11.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(9.8211F, 3.9443F, -10.1952F, -0.847F, 0.5348F, -1.6051F));
        PartDefinition arms8 = mainBody.addOrReplaceChild("arms_8", CubeListBuilder.create(), PartPose.offset(33.5197F, -56.6326F, 54.5215F));
        PartDefinition cubeR19 = arms8.addOrReplaceChild("cube_r_19", CubeListBuilder.create().texOffs(68, 0).mirror().addBox(37.5F, -3.5F, -6.0F, 21.0F, 11.0F, 12.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.9463F, -0.2261F, 1.8681F));
        PartDefinition cubeR20 = arms8.addOrReplaceChild("cube_r_20", CubeListBuilder.create().texOffs(0, 62).mirror().addBox(-16.5F, -3.5F, -6.0F, 23.0F, 11.0F, 12.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-19.0197F, 20.0271F, -11.8519F, 0.7561F, -0.6711F, -0.5303F));
        PartDefinition cubeR21 = arms8.addOrReplaceChild("cube_r_21", CubeListBuilder.create().texOffs(0, 85).mirror().addBox(6.5F, -3.5F, -6.0F, 19.0F, 11.0F, 12.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-18.8718F, 13.7168F, -15.6806F, -0.4423F, -0.8887F, 1.0054F));
        PartDefinition cubeR22 = arms8.addOrReplaceChild("cube_r_22", CubeListBuilder.create().texOffs(56, 127).mirror().addBox(25.5F, -3.5F, -6.0F, 12.0F, 11.0F, 12.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-9.8211F, 3.9443F, -10.1952F, -0.847F, -0.5348F, 1.6051F));
        PartDefinition arms6 = mainBody.addOrReplaceChild("arms_6", CubeListBuilder.create(), PartPose.offset(33.5197F, -56.6326F, 54.5215F));
        PartDefinition arms9 = mainBody.addOrReplaceChild("arms_9", CubeListBuilder.create(), PartPose.offset(-33.5197F, -56.6326F, 54.5215F));
        PartDefinition arms2 = mainBody.addOrReplaceChild("arms_2", CubeListBuilder.create(), PartPose.offsetAndRotation(-15.2955F, -8.7097F, 6.6069F, 0.0F, -0.8727F, 0.0F));
        PartDefinition cubeR23 = arms2.addOrReplaceChild("cube_r_23", CubeListBuilder.create().texOffs(40, 141).addBox(-8.5F, -3.0F, -3.0F, 3.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, 0.2618F, -1.5708F));
        PartDefinition cubeR24 = arms2.addOrReplaceChild("cube_r_24", CubeListBuilder.create().texOffs(96, 23).addBox(-5.5F, -2.0F, -2.0F, 10.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 3.0F, -1.0F, -0.5125F, 0.6702F, -0.9893F));
        PartDefinition cubeR25 = arms2.addOrReplaceChild("cube_r_25", CubeListBuilder.create().texOffs(70, 162).addBox(-3.5F, -2.0F, -2.0F, 7.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.4205F, 1.5811F, -4.9214F, 0.3622F, 0.7519F, 0.2533F));
        PartDefinition arms10 = mainBody.addOrReplaceChild("arms_10", CubeListBuilder.create(), PartPose.offsetAndRotation(15.2955F, -8.7097F, 6.6069F, 0.0F, 0.8727F, 0.0F));
        PartDefinition cubeR26 = arms10.addOrReplaceChild("cube_r_26", CubeListBuilder.create().texOffs(40, 141).mirror().addBox(5.5F, -3.0F, -3.0F, 3.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.7854F, -0.2618F, 1.5708F));
        PartDefinition cubeR27 = arms10.addOrReplaceChild("cube_r_27", CubeListBuilder.create().texOffs(96, 23).mirror().addBox(-4.5F, -2.0F, -2.0F, 10.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-1.0F, 3.0F, -1.0F, -0.5125F, -0.6702F, 0.9893F));
        PartDefinition cubeR28 = arms10.addOrReplaceChild("cube_r_28", CubeListBuilder.create().texOffs(70, 162).mirror().addBox(-3.5F, -2.0F, -2.0F, 7.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-4.4205F, 1.5811F, -4.9214F, 0.3622F, -0.7519F, -0.2533F));
        PartDefinition arms7 = mainBody.addOrReplaceChild("arms_7", CubeListBuilder.create(), PartPose.offsetAndRotation(15.2955F, -8.7097F, 6.6069F, 0.0F, 0.8727F, 0.0F));
        PartDefinition arms11 = mainBody.addOrReplaceChild("arms_11", CubeListBuilder.create(), PartPose.offsetAndRotation(-15.2955F, -8.7097F, 6.6069F, 0.0F, -0.8727F, 0.0F));
        PartDefinition arms = mainBody.addOrReplaceChild("arms", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition cubeR29 = arms.addOrReplaceChild("cube_r_29", CubeListBuilder.create().texOffs(152, 142).addBox(-3.5F, -2.0F, -2.0F, 7.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-7.875F, -7.1286F, 12.6855F, 0.3622F, 0.7519F, 0.2533F));
        PartDefinition cubeR30 = arms.addOrReplaceChild("cube_r_30", CubeListBuilder.create().texOffs(68, 23).addBox(-5.5F, -2.0F, -2.0F, 10.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-11.2955F, -5.7097F, 16.6069F, -0.5125F, 0.6702F, -0.9893F));
        PartDefinition cubeR31 = arms.addOrReplaceChild("cube_r_31", CubeListBuilder.create().texOffs(40, 132).addBox(-8.5F, -3.0F, -3.0F, 3.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.2955F, -8.7097F, 17.6069F, -0.7854F, 0.2618F, -1.5708F));
        PartDefinition arms3_2 = mainBody.addOrReplaceChild("arms3", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition cubeR32 = arms3_2.addOrReplaceChild("cube_r_32", CubeListBuilder.create().texOffs(152, 142).mirror().addBox(-3.5F, -2.0F, -2.0F, 7.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(7.875F, -7.1286F, 12.6855F, 0.3622F, -0.7519F, -0.2533F));
        PartDefinition cubeR33 = arms3_2.addOrReplaceChild("cube_r_33", CubeListBuilder.create().texOffs(68, 23).mirror().addBox(-4.5F, -2.0F, -2.0F, 10.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(11.2955F, -5.7097F, 16.6069F, -0.5125F, -0.6702F, 0.9893F));
        PartDefinition cubeR34 = arms3_2.addOrReplaceChild("cube_r_34", CubeListBuilder.create().texOffs(40, 132).mirror().addBox(5.5F, -3.0F, -3.0F, 3.0F, 4.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(12.2955F, -8.7097F, 17.6069F, -0.7854F, -0.2618F, 1.5708F));
        PartDefinition arms2_2 = mainBody.addOrReplaceChild("arms2", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition arms4 = mainBody.addOrReplaceChild("arms4", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition arms4_2 = mainBody.addOrReplaceChild("arms_4", CubeListBuilder.create(), PartPose.offset(-16.4513F, -12.7297F, 27.4907F));
        PartDefinition cubeR35 = arms4_2.addOrReplaceChild("cube_r_35", CubeListBuilder.create().texOffs(136, 46).addBox(-9.0F, -2.5F, -3.0F, 13.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0542F, 0.8671F, -0.8609F));
        PartDefinition cubeR36 = arms4_2.addOrReplaceChild("cube_r_36", CubeListBuilder.create().texOffs(148, 151).addBox(-1.0F, -11.0F, 4.0F, 2.0F, 1.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(16.4513F, 11.2303F, -10.8275F, 0.6981F, 0.0F, 0.0F));
        PartDefinition cubeR37 = arms4_2.addOrReplaceChild("cube_r_37", CubeListBuilder.create().texOffs(134, 35).addBox(-9.0F, -2.5F, -3.0F, 13.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(7.4513F, 1.3552F, -5.0596F, 0.7828F, 0.4242F, 0.3887F));
        PartDefinition cubeR38 = arms4_2.addOrReplaceChild("cube_r_38", CubeListBuilder.create().texOffs(136, 57).addBox(-9.0F, -2.5F, -3.0F, 13.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.2268F, 6.5774F, 8.2026F, -0.6249F, 0.6491F, -1.6632F));
        PartDefinition arms12 = mainBody.addOrReplaceChild("arms_12", CubeListBuilder.create(), PartPose.offset(16.4513F, -12.7297F, 27.4907F));
        PartDefinition cubeR39 = arms12.addOrReplaceChild("cube_r_39", CubeListBuilder.create().texOffs(136, 46).mirror().addBox(-4.0F, -2.5F, -3.0F, 13.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0542F, -0.8671F, 0.8609F));
        PartDefinition cubeR40 = arms12.addOrReplaceChild("cube_r_40", CubeListBuilder.create().texOffs(148, 151).mirror().addBox(-1.0F, -11.0F, 4.0F, 2.0F, 1.0F, 14.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-16.4513F, 11.2303F, -10.8275F, 0.6981F, 0.0F, 0.0F));
        PartDefinition cubeR41 = arms12.addOrReplaceChild("cube_r_41", CubeListBuilder.create().texOffs(134, 35).mirror().addBox(-4.0F, -2.5F, -3.0F, 13.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-7.4513F, 1.3552F, -5.0596F, 0.7828F, -0.4242F, -0.3887F));
        PartDefinition cubeR42 = arms12.addOrReplaceChild("cube_r_42", CubeListBuilder.create().texOffs(136, 57).mirror().addBox(-4.0F, -2.5F, -3.0F, 13.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(3.2268F, 6.5774F, 8.2026F, -0.6249F, -0.6491F, 1.6632F));
        PartDefinition arms5 = mainBody.addOrReplaceChild("arms_5", CubeListBuilder.create(), PartPose.offset(16.4513F, -12.7297F, 27.4907F));
        PartDefinition cubeR43 = arms5.addOrReplaceChild("cube_r_43", CubeListBuilder.create().texOffs(124, 104).addBox(-7.0F, -10.0F, 4.0F, 14.0F, 10.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-16.4513F, 11.2303F, -10.8275F, 0.6981F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 256, 256);
    }
}
