package net.vhsworld.rec.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.ShadeSegmentEntity;

/**
 * O desenhista do Anomalo da Sombra.
 *
 * O corpo e desenhado normalmente — ele PRECISA escurecer com o ambiente, porque a
 * regra dele e a luz e o jogador tem que ver a luz agindo sobre ele. Os olhos e que
 * sao emissivos: dois pontos brancos que continuam acesos no breu.
 *
 * Isso monta a leitura que o desenho pede: no escuro voce so ve os dois pontos vindo,
 * sem corpo em volta; com a tocha acesa aparece a coluna rasgada inteira, parada.
 */
public class ShadeSegmentRenderer extends MobRenderer<ShadeSegmentEntity, ShadeSegmentModel> {

    private static final ResourceLocation SKIN =
            new ResourceLocation(RECMod.MOD_ID, "textures/entity/shade_segment.png");

    private static final ResourceLocation EYES =
            new ResourceLocation(RECMod.MOD_ID, "textures/entity/shade_segment_eyes.png");

    public ShadeSegmentRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ShadeSegmentModel(ctx.bakeLayer(ShadeSegmentModel.LAYER)), 0.7F);
        this.addLayer(new Eyes(this));
    }

    @Override
    public ResourceLocation getTextureLocation(ShadeSegmentEntity entity) {
        return SKIN;
    }

    /**
     * A camada dos olhos.
     *
     * `RenderType.eyes` e a mesma coisa que faz os olhos do Enderman e do Spider
     * brilharem: ela ignora a luz do mundo e some no fundo por soma de cor. Nao e
     * `entityTranslucent` — aquele respeitaria a iluminacao e os pontos apagariam
     * junto com o resto do bicho, que e exatamente o que nao pode acontecer.
     */
    private static final class Eyes
            extends RenderLayer<ShadeSegmentEntity, ShadeSegmentModel> {

        private Eyes(RenderLayerParent<ShadeSegmentEntity, ShadeSegmentModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light,
                           ShadeSegmentEntity entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            VertexConsumer eyes = buffer.getBuffer(RenderType.eyes(EYES));
            this.getParentModel().renderToBuffer(pose, eyes, 15728640,
                    OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
