package net.vhsworld.rec.item;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.model.FractureArmorModel;

import java.util.function.Consumer;

/**
 * As quatro pecas do traje da FRATURA.
 *
 * ⚠️ A CAMADA VESTIDA NAO ANIMA POR .mcmeta. O .mcmeta so vale para textura que
 * passa pelo atlas (icone de item, bloco); a armadura e carregada como textura
 * solta, entao o arquivo animado seria mostrado esticado, com os 8 quadros
 * empilhados de uma vez. A animacao aqui e feita TROCANDO O ARQUIVO a cada 2
 * ticks — o mesmo frametime do icone, para a peca na mao e a peca no corpo
 * pulsarem juntas.
 */
public class FractureArmorItem extends ArmorItem {

    /** Quantos arquivos existem por camada (fracture_layer_N_0 .. _7). */
    private static final int FRAMES = 8;

    /** Igual ao "frametime": 2 ticks por quadro. */
    private static final int TICKS_PER_FRAME = 2;

    public FractureArmorItem(Type type, Properties properties) {
        super(FractureArmorMaterial.FRACTURE, type, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        long time = entity == null ? 0L : entity.level().getGameTime();
        int frame = (int) Math.floorMod(time / TICKS_PER_FRAME, FRAMES);
        int layer = slot == EquipmentSlot.LEGS ? 2 : 1;
        return RECMod.MOD_ID + ":textures/models/armor/fracture_layer_" + layer + "_" + frame + ".png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                          EquipmentSlot slot, HumanoidModel<?> original) {
                return FractureArmorModel.forSlot(slot, original);
            }
        });
    }
}
