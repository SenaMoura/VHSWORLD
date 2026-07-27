package net.vhsworld.rec.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.StonemanEntity;

/**
 * O desenhista do Homem de Pedra.
 *
 * Ate a v1.38.0 ele assava tres modelos e trocava o `this.model` no render(),
 * conforme a variante sorteada no spawn. Agora e um bicho so: um modelo, uma pele.
 * Se as variantes voltarem, o padrao de troca esta no historico deste arquivo.
 */
public class StonemanRenderer extends MobRenderer<StonemanEntity, StonemanModel> {

    private static final ResourceLocation SKIN =
            new ResourceLocation(RECMod.MOD_ID, "textures/entity/stoneman.png");

    public StonemanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new StonemanModel(ctx.bakeLayer(StonemanModel.BASE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(StonemanEntity entity) {
        return SKIN;
    }
}
