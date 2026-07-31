package net.vhsworld.rec.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.MirrorEntity;

/**
 * Desenha o Espelho, e o faz ENCARAR.
 *
 * ⚠️ ELE GIRA PARA O JOGADOR TODO QUADRO, e isso e a criatura e nao um atalho de
 * renderizacao. O painel tem espessura zero: visto de lado, ele sumiria — uma linha de um
 * pixel. Fixar o yaw na camera resolve isso e, de quebra, entrega o comportamento que o
 * Pedro pediu: "ela vai ficar encarando o jogador". Nao importa por onde se contorne, os
 * tres olhos continuam de frente.
 *
 * ⚠️ E O GIRO E SO NO EIXO Y. Deixar o painel inclinar junto com a altura da camera faria
 * ele deitar quando o jogador olha do alto, e um retangulo de oito blocos deitado no chao
 * vira um tapete. O Espelho fica sempre em pe, ancorado no chao — o que muda e para onde
 * a face aponta.
 */
public class MirrorRenderer extends MobRenderer<MirrorEntity, MirrorModel> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "textures/entity/mirror.png");

    public MirrorRenderer(EntityRendererProvider.Context context) {
        super(context, new MirrorModel(context.bakeLayer(MirrorModel.LAYER)), 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(MirrorEntity entity) {
        return TEXTURE;
    }

    /**
     * A face acompanha a camera.
     *
     * O `MobRenderer` do vanilla usa o `yBodyRot` da entidade; aqui ele e substituido pelo
     * angulo ate a camera, calculado no proprio quadro. Girar a ENTIDADE no servidor daria
     * o mesmo efeito visual e custaria um pacote de rotacao por tique por jogador — e num
     * mundo com um espelho por regiao, para uma criatura que nao faz mais nada, isso e
     * trafego para desenhar uma coisa que o cliente ja sabe desenhar sozinho.
     */
    @Override
    protected void setupRotations(MirrorEntity entity, PoseStack pose, float ageInTicks,
                                  float bodyYaw, float partialTick) {
        Entity camera = net.minecraft.client.Minecraft.getInstance().getCameraEntity();
        float facing = bodyYaw;
        if (camera != null) {
            double dx = camera.getX() - entity.getX();
            double dz = camera.getZ() - entity.getZ();
            facing = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        }
        super.setupRotations(entity, pose, ageInTicks, facing, partialTick);
    }

    /**
     * ⚠️ SEM SOMBRA E SEM ESCURECER COM A LUZ DO LUGAR. O painel e preto puro com os olhos
     * riscados em cinza; se ele recebesse a iluminacao do bloco, numa dimensao escura os
     * olhos sumiriam e o Espelho viraria um retangulo invisivel — e o jogador nao teria
     * como saber que passou ao lado da unica saida da dimensao.
     */
    @Override
    protected int getBlockLightLevel(MirrorEntity entity, net.minecraft.core.BlockPos pos) {
        return 12;
    }

    @Override
    public void render(MirrorEntity entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        super.render(entity, entityYaw, partialTick, pose, buffers, packedLight);
    }
}
