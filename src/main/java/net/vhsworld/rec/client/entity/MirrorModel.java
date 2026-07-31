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
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.MirrorEntity;

/**
 * O modelo do Espelho, saido do `mirror.bbmodel` do Pedro.
 *
 * Uma peca so: um plano de 45 x 134 x ZERO de espessura, ou seja 2,8 blocos de largura por
 * 8,4 de altura. Nao ha volume nenhum — e uma folha.
 *
 * ⚠️ O EXPORT DO BLOCKBENCH NAO ENTRA COMO VEIO, e vale saber o que mudou:
 *
 * 1. O `ResourceLocation("modid", ...)` do export vira `recmod`. Esquecer isso da uma
 *    camada de modelo que o jogo nunca acha, e o sintoma e a criatura invisivel — sem
 *    erro nenhum no log.
 * 2. `EntityModel<T extends Entity>` cru nao serve ao renderizador de `Mob`; o parametro
 *    foi amarrado em `MirrorEntity`.
 * 3. O nome da classe veio minusculo (`mirror`), o que compila e briga com a convencao de
 *    todo o resto do pacote.
 *
 * ⚠️ E A ESPESSURA ZERO E DE PROPOSITO, nao um descuido do Pedro. Uma caixa de profundidade
 * 0 no Minecraft desenha as duas faces no mesmo plano — o painel e visivel dos dois lados
 * e nao tem canto. E o que faz o Espelho parecer um RECORTE no mundo em vez de um objeto
 * dentro dele: virando em volta, ele nunca engrossa.
 */
public class MirrorModel extends EntityModel<MirrorEntity> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "mirror"), "main");

    private final ModelPart root;

    public MirrorModel(ModelPart root) {
        this.root = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        parts.addOrReplaceChild("bb_main", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-23.0F, -134.0F, 1.0F, 45.0F, 134.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(MirrorEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Nao ha animacao, e nao vai haver. O Espelho e a unica criatura do mod que nao se
        // mexe nem um pouco — nem respira, nem oscila. Todo movimento que ele tivesse seria
        // uma coisa a que o olho se acostuma, e o susto dele depende de ele estar
        // absolutamente parado ate o momento em que o jogador percebe que ele encara.
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
