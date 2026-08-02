package net.vhsworld.rec.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.CrawlerVoidEntity;

/**
 * O modelo do Rastreador do Rastejo.
 *
 * Os membros dele estao em grupos "Arms_N" pendurados no corpo (o modelo nao separa
 * frente e tras nem esquerda e direita por nome). Aqui eles sao lidos pelo nome e
 * postos em DUAS FASES alternadas: pares e impares em contratempo. E o suficiente
 * para ler como caminhada de quadrupede, e nao depende de adivinhar qual grupo e
 * qual perna — se o Pedro renomear no Blockbench, isto continua funcionando.
 *
 * Parado, ele para de vez. Ele nao e estatua (isso e o Homem de Pedra), mas o
 * congelamento tem que ser NITIDO: o jogador precisa poder testar a regra — olho para
 * ele, ele para; olho para a parede, ele anda. Uma animacao de "esperando" tornaria o
 * teste ambiguo, e regra que nao da para testar nao vira estrategia, vira frustracao.
 */
public class CrawlerVoidModel extends EntityModel<CrawlerVoidEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(RECMod.MOD_ID, "crawler_void"), "main");

    /** Os grupos de membro, na ordem em que o modelo os declara. */
    private static final String[] LIMBS = {
            "arms_3", "arms_8", "arms_2", "arms_10", "arms", "arms3",
            "arms_4", "arms_12", "arms_5"};

    private final ModelPart crawler;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart[] limbs;

    public CrawlerVoidModel(ModelPart root) {
        this.crawler = root.getChild("crawler_void");
        this.head = this.crawler.getChild("head");
        this.body = this.head.getChild("main_body");

        this.limbs = new ModelPart[LIMBS.length];
        for (int i = 0; i < LIMBS.length; i++) {
            this.limbs[i] = this.body.getChild(LIMBS[i]);
        }
    }

    @Override
    public void setupAnim(CrawlerVoidEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        if (!entity.isMoving()) {
            for (ModelPart limb : this.limbs) limb.xRot = 0.0F;
            this.crawler.y = 0.0F;
            return;
        }

        float swing = limbSwing * 0.7F;
        float amount = Math.min(limbSwingAmount, 1.0F);

        for (int i = 0; i < this.limbs.length; i++) {
            float phase = (i % 2 == 0) ? swing : swing + (float) Math.PI;
            this.limbs[i].xRot = Mth.cos(phase) * 0.55F * amount;
        }

        // O corpo sobe e desce no DOBRO da frequencia das pernas: e o balanco que
        // qualquer quadrupede faz, e sem ele a criatura desliza como se estivesse
        // sobre rodas — o que estraga justamente o som de passo que ela produz.
        this.crawler.y = Mth.cos(swing * 2.0F) * 0.9F * amount;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               float r, float g, float b, float a) {
        this.crawler.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
