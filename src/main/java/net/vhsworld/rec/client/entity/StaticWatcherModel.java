package net.vhsworld.rec.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.StaticWatcherEntity;

/**
 * O modelo do Observador Estatico.
 *
 * A geometria vem de client/entity/geom/StaticWatcherGeometry (gerada do .bbmodel);
 * aqui so fica o movimento — o formato modded_entity nao guarda animacao nenhuma.
 *
 * ⚠️ NAO existe ciclo de caminhada, e isso e a criatura, nao uma pendencia. Ele nunca
 * anda: muda de lugar por teletransporte, no escuro do seu piscar. Uma perna balancando
 * contaria a mentira de que ele percorreu o caminho.
 *
 * ⚠️ A cabeca tambem nao gira sozinha. Neste modelo o tronco inteiro e FILHO da cabeca
 * (Head -> Main_body -> Torso/Legs/Arms), entao girar a cabeca torceria o bicho todo
 * pelo pescoco. Quem aponta para o jogador e a entidade, no servidor (`yBodyRot`).
 */
public class StaticWatcherModel extends EntityModel<StaticWatcherEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(RECMod.MOD_ID, "static_watcher"), "main");

    private final ModelPart watcher;

    public StaticWatcherModel(ModelPart root) {
        this.watcher = root.getChild("static_watcher");
    }

    /**
     * A unica coisa viva nele: um bamboleio lentissimo, quase imperceptivel.
     *
     * Imobilidade TOTAL e do Homem de Pedra, onde ela quer dizer "isto e pedra". Aqui
     * seria a leitura errada: o Observador nao e estatua, e uma coisa alta e fina que
     * fica de pe no vento a oitenta blocos de distancia. O bamboleio de meio grau e o
     * que faz o jogador nao ter certeza se aquilo la longe e um tronco seco ou nao —
     * e essa duvida e o bicho inteiro.
     */
    @Override
    public void setupAnim(StaticWatcherEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.watcher.zRot = Mth.cos(ageInTicks * 0.03F) * 0.012F;
        this.watcher.xRot = Mth.sin(ageInTicks * 0.021F) * 0.008F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               float r, float g, float b, float a) {
        this.watcher.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
