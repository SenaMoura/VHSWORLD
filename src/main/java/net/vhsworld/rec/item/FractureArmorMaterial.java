package net.vhsworld.rec.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.vhsworld.rec.init.ModItems;

/**
 * O material do traje da FRATURA.
 *
 * Defesa igual a do netherite de proposito: o traje ja e o topo da arvore do mod e
 * nao passa pelo Nether, entao subir os numeros acima do netherite aposentaria o
 * netherite sem pedir nada em troca. O que ele tem de diferente e a durabilidade
 * alta e o conserto por caco de realidade — nao um numero maior.
 */
public enum FractureArmorMaterial implements ArmorMaterial {
    FRACTURE;

    /** Base de durabilidade do vanilla por peca, na ordem de EquipmentSlot. */
    private static final int[] HEALTH_PER_SLOT = {13, 15, 16, 11};

    /** Netherite usa 37. */
    private static final int DURABILITY_MULTIPLIER = 40;

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return HEALTH_PER_SLOT[type.getSlot().getIndex()] * DURABILITY_MULTIPLIER;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 3;
            case CHESTPLATE -> 8;
            case LEGGINGS -> 6;
            case BOOTS -> 3;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 20;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.REALITY_TEAR.get());
    }

    /**
     * Com dominio, para o caminho padrao cair em assets/recmod e nao em assets/minecraft.
     * A textura que aparece vestida vem do FractureArmorItem (um arquivo por quadro);
     * este nome so alimenta o caminho de reserva, que existe no jar por seguranca.
     */
    @Override
    public String getName() {
        return "recmod:fracture";
    }

    @Override
    public float getToughness() {
        return 3.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.1F;
    }
}
