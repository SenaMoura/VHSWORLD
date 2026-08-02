package net.vhsworld.rec.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.CrawlerVoidEntity;

/**
 * O desenhista do Rastreador do Rastejo.
 *
 * Sem truque de iluminacao aqui, ao contrario dos outros tres desta leva. Ele PRECISA
 * escurecer com o ambiente: o bicho e um vulto de sombra que se aproxima por tras, e
 * um vulto que brilha no escuro se entrega de longe. Ele tem que ser a coisa que voce
 * ouviu antes de ver.
 */
public class CrawlerVoidRenderer extends MobRenderer<CrawlerVoidEntity, CrawlerVoidModel> {

    private static final ResourceLocation SKIN =
            new ResourceLocation(RECMod.MOD_ID, "textures/entity/crawler_void.png");

    public CrawlerVoidRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CrawlerVoidModel(ctx.bakeLayer(CrawlerVoidModel.LAYER)), 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(CrawlerVoidEntity entity) {
        return SKIN;
    }
}
