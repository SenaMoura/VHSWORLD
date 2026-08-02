package net.vhsworld.rec.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.StaticWatcherEntity;

/**
 * O desenhista do Observador Estatico.
 *
 * Duas coisas fora do comum:
 *
 * 1. APAGADO ELE NAO E DESENHADO. O `render` sai antes de tudo. Sumir tem que ser
 *    instantaneo e total — um fade, uma sombra ou meio corpo restante contariam a
 *    verdade (que ele continua ali) e a criatura inteira depende da mentira.
 *
 * 2. A cabeca dele e uma televisao fora do ar, e televisao nao escurece quando entra
 *    na sombra. O modelo e desenhado com luz cheia; o preto do corpo continua preto
 *    de dia ou de noite, e o chuvisco da cabeca brilha igual no breu — que e o que
 *    faz o bicho ser visivel a oitenta blocos no meio da noite.
 */
public class StaticWatcherRenderer extends MobRenderer<StaticWatcherEntity, StaticWatcherModel> {

    private static final ResourceLocation SKIN =
            new ResourceLocation(RECMod.MOD_ID, "textures/entity/static_watcher.png");

    public StaticWatcherRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new StaticWatcherModel(ctx.bakeLayer(StaticWatcherModel.LAYER)), 0.6F);
    }

    @Override
    public void render(StaticWatcherEntity entity, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int light) {
        if (entity.isOff()) return;
        super.render(entity, yaw, partialTick, pose, buffer, light);
    }

    /**
     * Luz de bloco no maximo: e o "unlit" do pedido, feito pelo caminho barato.
     *
     * Trocar o RenderType por um sem iluminacao daria o mesmo resultado e custaria um
     * shader proprio, que quebra com Oculus/Embeddium — o mesmo motivo que ja tinha
     * derrubado a ideia do core shader nas criaturas 3D.
     */
    @Override
    protected int getBlockLightLevel(StaticWatcherEntity entity, net.minecraft.core.BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(StaticWatcherEntity entity) {
        return SKIN;
    }
}
