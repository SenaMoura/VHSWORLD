package net.vhsworld.rec.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.ListenerEntity;

/**
 * O desenhista do Escutador.
 *
 * ⚠️ NENHUM TRUQUE DE ILUMINACAO — ele escurece com o ambiente, como o Rastejo. Bicho que
 * brilha no escuro se entrega de longe, e este e o unico do elenco que o jogador tem que
 * OUVIR antes de ver. Se a silhueta aparecesse na caverna escura antes do estalo, a ordem
 * da descoberta se inverteria, e a ordem e a criatura.
 */
public class ListenerRenderer extends MobRenderer<ListenerEntity, ListenerModel> {

    private static final ResourceLocation SKIN =
            new ResourceLocation(RECMod.MOD_ID, "textures/entity/listener.png");

    public ListenerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ListenerModel(ctx.bakeLayer(ListenerModel.LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(ListenerEntity entity) {
        return SKIN;
    }
}
