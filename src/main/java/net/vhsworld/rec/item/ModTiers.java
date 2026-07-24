package net.vhsworld.rec.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.vhsworld.rec.init.ModBlocks;
import net.vhsworld.rec.init.ModItems;

import java.util.function.Supplier;

/**
 * Os materiais de ferramenta do mod.
 *
 * PEDRA CORROMPIDA nao e um diamante mais barato: ela e RAPIDA e bate FORTE, mas
 * so alcanca o mesmo que o ferro (nao quebra obsidiana) e gasta rapido. Assim ela
 * nao aposenta o diamante — ocupa um lugar proprio, para quem prefere velocidade
 * e dano a durabilidade. Se ela minerasse tudo, o diamante viraria decoracao.
 *
 * DIAMANTE CORROMPIDO e o topo, e existe por um motivo so: e a unica coisa que
 * arranca o Rasgo da Realidade da parede. Ela quebra tudo que o jogo tem.
 */
public enum ModTiers implements Tier {

    /** dano de espada = 7, igual ao diamante; alcance de ferro; vida curta. */
    CORRUPTED(2, 900, 7.0F, 3.0F, 18,
            () -> Ingredient.of(ModBlocks.CORRUPTED_STONE.get())),

    /** Acima do netherite em alcance. Conserta com gosma preta, nao com pedra. */
    CORRUPTED_DIAMOND(4, 2200, 9.0F, 4.0F, 15,
            () -> Ingredient.of(ModItems.BLACK_GOO.get()));

    private final int level;
    private final int uses;
    private final float speed;
    private final float damage;
    private final int enchantmentValue;
    private final Supplier<Ingredient> repair;

    ModTiers(int level, int uses, float speed, float damage, int enchantmentValue,
             Supplier<Ingredient> repair) {
        this.level = level;
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.enchantmentValue = enchantmentValue;
        this.repair = repair;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return damage;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repair.get();
    }
}
