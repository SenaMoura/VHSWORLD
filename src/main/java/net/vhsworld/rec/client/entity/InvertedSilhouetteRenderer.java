package net.vhsworld.rec.client.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.InvertedSilhouetteEntity;

/**
 * O desenhista da Silhueta Invertida.
 *
 * O pedido do desenho e "preto solido com efeito unlit: ignora a iluminacao do jogo e
 * nao recebe sombra". Aqui isso e feito fixando a luz de bloco no maximo.
 *
 * Parece contraintuitivo iluminar ao maximo uma coisa que tem que ficar PRETA, mas e
 * justamente por isso: sem luz cheia, a textura preta ficaria mais escura ainda na
 * sombra e mais clara ao sol, e a silhueta mudaria de tom conforme o lugar. Com a luz
 * travada, o preto e sempre o MESMO preto — um buraco de forma humana recortado na
 * paisagem, que e o que faz o olho nao conseguir dizer a que distancia ela esta.
 *
 * (E feito assim, e nao com um RenderType proprio sem iluminacao, porque shader
 * proprio quebra com Oculus/Embeddium — a mesma pedra em que a ideia do core shader
 * das criaturas 3D ja tinha batido.)
 */
public class InvertedSilhouetteRenderer
        extends MobRenderer<InvertedSilhouetteEntity, InvertedSilhouetteModel> {

    private static final ResourceLocation SKIN =
            new ResourceLocation(RECMod.MOD_ID, "textures/entity/inverted_silhouette.png");

    public InvertedSilhouetteRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new InvertedSilhouetteModel(ctx.bakeLayer(InvertedSilhouetteModel.LAYER)), 0.5F);
    }

    @Override
    protected int getBlockLightLevel(InvertedSilhouetteEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(InvertedSilhouetteEntity entity) {
        return SKIN;
    }
}
