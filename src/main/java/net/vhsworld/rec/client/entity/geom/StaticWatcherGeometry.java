package net.vhsworld.rec.client.entity.geom;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * ARQUIVO GERADO — nao editar a mao.
 * Fonte: Static_Watcher.bbmodel
 * Gerador: tools/bbmodel_to_geometry.py
 *
 * A geometria vem do .bbmodel, e nao do .java que o Blockbench exporta: o export
 * espalhou os membros do Crawler_Void a nove blocos do corpo, e ja tinha historico
 * de nome de parte invalido e de partes que se sobrescreviam. A animacao fica na
 * classe de modelo, nao aqui.
 *
 * Partes na raiz: "static_watcher"
 */
public final class StaticWatcherGeometry {

    private StaticWatcherGeometry() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition staticWatcher = root.addOrReplaceChild("static_watcher", CubeListBuilder.create(), PartPose.offset(0.0F, -15.0F, 0.0F));
        PartDefinition head = staticWatcher.addOrReplaceChild("head", CubeListBuilder.create()
        .texOffs(0, 18).addBox(-6.0F, -51.0F, -2.0F, 13.0F, 12.0F, 9.0F, new CubeDeformation(0.0F))
        .texOffs(80, 77).addBox(7.0F, -55.0F, -6.0F, 1.0F, 17.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(82, 33).addBox(-7.0F, -55.0F, -6.0F, 1.0F, 17.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(48, 11).addBox(-6.0F, -40.0F, -6.0F, 13.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(78, 11).addBox(-6.0F, -55.0F, -6.0F, 13.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(48, 58).addBox(-7.0F, -55.0F, -4.0F, 15.0F, 17.0F, 2.0F, new CubeDeformation(0.0F))
        .texOffs(48, 77).addBox(-6.0F, -53.0F, -5.0F, 13.0F, 13.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition cubeR = head.addOrReplaceChild("cube_r", CubeListBuilder.create().texOffs(44, 18).addBox(-6.0F, -13.0F, -6.0F, 13.0F, 5.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -40.0F, 0.0F, -0.2618F, 0.0F, 0.0F));
        PartDefinition mainBody = head.addOrReplaceChild("main_body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition torso = mainBody.addOrReplaceChild("torso", CubeListBuilder.create()
        .texOffs(82, 52).addBox(-1.0F, -39.0F, 1.0F, 3.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(0, 0).addBox(-7.0F, -35.0F, -2.0F, 15.0F, 9.0F, 9.0F, new CubeDeformation(0.0F))
        .texOffs(48, 33).addBox(-4.0F, -25.1553F, -1.4434F, 9.0F, 17.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition cubeR2 = torso.addOrReplaceChild("cube_r_2", CubeListBuilder.create().texOffs(48, 0).addBox(-7.0F, 5.0F, -2.0F, 15.0F, 2.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -31.1553F, -0.4434F, 0.0873F, 0.0F, 0.0F));
        PartDefinition legs = mainBody.addOrReplaceChild("legs", CubeListBuilder.create()
        .texOffs(36, 39).addBox(2.0F, -10.0F, 2.0F, 3.0F, 49.0F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(24, 39).addBox(-4.0F, -10.0F, 2.0F, 3.0F, 49.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition arms = mainBody.addOrReplaceChild("arms", CubeListBuilder.create()
        .texOffs(12, 39).addBox(7.0F, -34.0F, 2.0F, 3.0F, 49.0F, 3.0F, new CubeDeformation(0.0F))
        .texOffs(0, 39).addBox(-10.0F, -34.0F, 2.0F, 3.0F, 49.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }
}
