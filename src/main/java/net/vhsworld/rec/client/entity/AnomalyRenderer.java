package net.vhsworld.rec.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.vhsworld.rec.client.entity.mesh.BakedMesh;
import net.vhsworld.rec.client.entity.mesh.MeshLibrary;
import net.vhsworld.rec.entity.AnomalyEntity;
import net.vhsworld.rec.entity.AnomalyType;
import org.joml.Matrix4f;

/**
 * O desenhista das anomalias: um cartaz plano que sempre encara a camera.
 *
 * POR QUE 2D: estas criaturas nao sao feitas de cubos, e nao deveriam ser. Um
 * modelo 3D convida o jogador a andar em volta, achar a costura e concluir que e um
 * boneco. O cartaz nao tem costas — voce nunca vai ver o outro lado dela, porque
 * ela nao tem outro lado. E a mesma economia dos monstros de sprite dos jogos
 * antigos, e ela funciona pelo mesmo motivo: o que falta, a cabeca preenche.
 *
 * RenderType.entityTranslucent, e nao entityCutoutNoCull: o rosto destas texturas
 * tem meio-tom (ele nasce do escuro em vez de terminar numa borda). Cutout faz teste
 * de alfa — corta no liga/desliga e devolve exatamente a borda dura que o recorte
 * inteiro foi feito para evitar.
 */
public class AnomalyRenderer extends EntityRenderer<AnomalyEntity> {

    public AnomalyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;   // presenca nao faz sombra
    }

    @Override
    public void render(AnomalyEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light) {
        AnomalyType type = entity.type();

        // AQUI mora o sistema: o corpo dela existe sempre, mas so chega a tela se os
        // seus olhos (ou a fita) alcancarem. Sair antes do desenho e o suficiente —
        // e como a foto e o proprio framebuffer, ela some da foto junto.
        if (!AnomalyVision.canSee(type)) return;

        // Escultura de verdade, quando existe. O null nao e falha: e a criatura sem
        // malha (as tres 2D) ou o arquivo faltando — e nos dois casos o cartaz atende.
        if (type.hasMesh()) {
            BakedMesh mesh = MeshLibrary.get(type.meshPath());
            if (mesh != null) {
                renderMesh(entity, type, mesh, yaw, partialTick, pose, buffer, light);
                return;
            }
        }

        pose.pushPose();

        // Encara a camera de pe: gira so no eixo Y, seguindo o observador. Billboard
        // completo (com a inclinacao da camera) faria o bicho DEITAR quando o jogador
        // olhasse para o chao — e cartaz deitado entrega o truque na hora.
        Vec3 camera = this.entityRenderDispatcher.camera.getPosition();
        double dx = camera.x - entity.getX();
        double dz = camera.z - entity.getZ();
        // Angulo do OBSERVADOR em volta da criatura. Serve para duas coisas: virar o
        // cartaz para a camera, e decidir qual das vistas pre-renderizadas desenhar.
        float viewAngle = (float) (Math.atan2(dz, dx) * (180.0D / Math.PI));
        pose.mulPose(Axis.YP.rotationDegrees(-(viewAngle - 90.0F)));

        float w = type.width() * 0.5F;
        float h = type.height();

        // A CELULA da folha: coluna = de que lado voce esta, linha = quadro do tempo.
        int col = column(entity, type, viewAngle);
        int row = type.frames() > 1
                ? (entity.tickCount / type.ticksPerFrame()) % type.frames()
                : 0;

        float uw = 1.0F / type.angles();
        float vh = 1.0F / type.frames();
        float u0 = col * uw, u1 = u0 + uw;
        float v0 = row * vh, v1 = v0 + vh;

        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(type.texture()));
        Matrix4f matrix = pose.last().pose();

        quad(vc, matrix, pose, light, -w, 0.0F, u0, v1);
        quad(vc, matrix, pose, light, w, 0.0F, u1, v1);
        quad(vc, matrix, pose, light, w, h, u1, v0);
        quad(vc, matrix, pose, light, -w, h, u0, v0);

        pose.popPose();
    }

    /**
     * Desenha a escultura: a malha de verdade, com a animacao rodando.
     *
     * A malha chega normalizada — 1 bloco de altura, pes no zero, centrada no proprio
     * eixo. A altura de verdade continua sendo `AnomalyType.height()`, entao mudar o
     * tamanho do Ofanim e trocar um numero no enum, sem reassar nada no Blender.
     */
    private void renderMesh(AnomalyEntity entity, AnomalyType type, BakedMesh mesh, float yaw,
                            float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        pose.pushPose();

        // A convencao do jogo: modelo esculpido encarando -Z, virado por 180 - yaw. O
        // meshYaw em cima disso corrige o lado para o qual ESTA escultura foi modelada
        // (medido fora do jogo, nao chutado — ver AnomalyType.meshYaw).
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        pose.mulPose(Axis.YP.rotationDegrees(type.meshYaw()));

        float scale = type.height();
        pose.scale(scale, scale, scale);

        // Quanto da animacao passa por tick — decidido por criatura (ver animSpeed).
        float time = (entity.tickCount + partialTick) * type.animSpeed();

        // entityCutoutNoCull e nao entityTranslucent: a escultura e solida, e translucido
        // pagaria ordenacao por 3.500 triangulos a toa. O "noCull" e o que importa —
        // malha decimada sai com faces de mao trocada aqui e ali, e com corte de face de
        // tras elas viram BURACO no corpo da criatura.
        // A cor do vertice MULTIPLICA a textura, entao 0x000000 apaga a pele inteira e
        // sobra a silhueta — que e o que o Pedro pediu para o Cara Cinza. A textura
        // continua no jar: voltar atras e trocar o numero no AnomalyType.
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(type.meshTexture()));
        mesh.draw(pose.last(), vc, light, time,
                type.meshTintRed(), type.meshTintGreen(), type.meshTintBlue(), 255);

        pose.popPose();
    }

    /**
     * De que lado da criatura o jogador esta — e, portanto, qual das 8 vistas desenhar.
     *
     * Este e o truque inteiro do Doom: o cartaz continua sempre virado para a camera
     * (por isso ele nunca aparece de canto e nunca some), mas a IMAGEM nele muda
     * conforme voce anda em volta. Quem da a volta ve as costas da criatura sem que
     * exista geometria nenhuma girando.
     *
     * `angleOffset` existe porque cada escultura foi modelada encarando um lado
     * diferente; e um numero para acertar vendo no jogo, sem re-renderizar nada.
     */
    private static int column(AnomalyEntity entity, AnomalyType type, float viewAngle) {
        if (!type.directional()) return 0;

        // A direcao para onde a criatura olha, no mesmo sistema do viewAngle.
        float facing = 90.0F + entity.getYRot();

        float step = 360.0F / type.angles();
        float relative = viewAngle - facing + type.angleOffset();

        return Math.floorMod(Math.round(relative / step), type.angles());
    }

    private static void quad(VertexConsumer vc, Matrix4f matrix, PoseStack pose,
                             int light, float x, float y, float u, float v) {
        vc.vertex(matrix, x, y, 0.0F)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.last().normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(AnomalyEntity entity) {
        return entity.type().texture();
    }
}
