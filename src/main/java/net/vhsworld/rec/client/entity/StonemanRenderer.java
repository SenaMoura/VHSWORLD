package net.vhsworld.rec.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.StonemanEntity;

/**
 * O desenhista do Homem de Pedra.
 *
 * As tres variantes nao sao so texturas diferentes: cada uma tem a sua geometria (a
 * 1 e a 2 tem pedras a mais, encaixadas em angulo). Entao os tres modelos sao
 * assados no construtor e o renderer TROCA o modelo antes de desenhar, conforme a
 * variante que veio do servidor.
 */
public class StonemanRenderer extends MobRenderer<StonemanEntity, StonemanModel> {

    private static final ResourceLocation[] SKINS = {
            new ResourceLocation(RECMod.MOD_ID, "textures/entity/stoneman.png"),
            new ResourceLocation(RECMod.MOD_ID, "textures/entity/stoneman_variant1.png"),
            new ResourceLocation(RECMod.MOD_ID, "textures/entity/stoneman_variant2.png"),
    };

    private final StonemanModel[] models;

    public StonemanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new StonemanModel(ctx.bakeLayer(StonemanModel.BASE)), 0.5F);
        this.models = new StonemanModel[]{
                this.model,
                new StonemanModel(ctx.bakeLayer(StonemanModel.VARIANT_1)),
                new StonemanModel(ctx.bakeLayer(StonemanModel.VARIANT_2)),
        };
    }

    @Override
    public void render(StonemanEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light) {
        this.model = this.models[entity.getVariant() % this.models.length];
        super.render(entity, yaw, partialTick, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(StonemanEntity entity) {
        return SKINS[entity.getVariant() % SKINS.length];
    }
}
