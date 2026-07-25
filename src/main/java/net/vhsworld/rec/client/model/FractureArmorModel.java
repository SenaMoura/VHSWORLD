package net.vhsworld.rec.client.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;

/**
 * O modelo vestido do traje da FRATURA.
 *
 * Existe por um motivo so: a margem sobre a pele. O vanilla infla a camada de fora
 * em 1.0 e as calcas em 0.5, o que da aquele volume de "casca". Aqui a peca fica
 * justa ao corpo, como pedido.
 *
 * ⚠️ A MARGEM DAS CALCAS NAO PODE SER 0.25. A segunda camada da SKIN do jogador
 * (jaqueta/calca) e desenhada exatamente em 0.25 — igualar as duas poe dois planos
 * na mesma profundidade e o resultado e o z-fighting piscando, justamente o que a
 * margem existe para evitar. 0.35 e o menor valor que ainda passa por cima da skin
 * e por baixo da camada de fora (0.5). Se quiser testar outro numero, e so aqui.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FractureArmorModel extends HumanoidModel<LivingEntity> {

    /** Elmo, peitoral e botas. Vanilla usa 1.0. */
    public static final float OUTER_INFLATE = 0.5F;

    /** Calcas (layer_2). Vanilla usa 0.5; a skin do jogador ocupa 0.25. */
    public static final float INNER_INFLATE = 0.35F;

    public static final ModelLayerLocation OUTER =
            new ModelLayerLocation(new ResourceLocation(RECMod.MOD_ID, "fracture_armor"), "outer");
    public static final ModelLayerLocation INNER =
            new ModelLayerLocation(new ResourceLocation(RECMod.MOD_ID, "fracture_armor"), "inner");

    private static FractureArmorModel outer;
    private static FractureArmorModel inner;

    public FractureArmorModel(ModelPart root) {
        super(root, RenderType::armorCutoutNoCull);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(OUTER, () -> LayerDefinition.create(
                HumanoidModel.createMesh(new CubeDeformation(OUTER_INFLATE), 0.0F), 64, 32));
        event.registerLayerDefinition(INNER, () -> LayerDefinition.create(
                HumanoidModel.createMesh(new CubeDeformation(INNER_INFLATE), 0.0F), 64, 32));
    }

    /**
     * O modelo que o Forge vai desenhar no lugar do padrao.
     *
     * As duas pecas sao assadas UMA vez e reaproveitadas: assar de novo a cada quadro
     * criaria uma arvore de ModelPart por peca, por entidade, por frame.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static HumanoidModel<?> forSlot(EquipmentSlot slot, HumanoidModel<?> original) {
        FractureArmorModel model;
        if (slot == EquipmentSlot.LEGS) {
            if (inner == null) inner = bake(INNER);
            model = inner;
        } else {
            if (outer == null) outer = bake(OUTER);
            model = outer;
        }

        // A pose vem do modelo padrao (que ja foi posicionado pelo layer do vanilla).
        // copyPropertiesTo NAO copia visibilidade — por isso ela e refeita abaixo.
        ((HumanoidModel) original).copyPropertiesTo(model);

        model.setAllVisible(false);
        switch (slot) {
            case HEAD -> {
                model.head.visible = true;
                model.hat.visible = true;
            }
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGS -> {
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            case FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            default -> {
            }
        }
        return model;
    }

    private static FractureArmorModel bake(ModelLayerLocation layer) {
        return new FractureArmorModel(Minecraft.getInstance().getEntityModels().bakeLayer(layer));
    }
}
