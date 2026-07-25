package net.vhsworld.rec.item;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;

/** Machado da FRATURA. Existe so para o brilho: o resto e o tier. */
public class FractureAxeItem extends AxeItem {

    public FractureAxeItem(Tier tier, float damage, float speed, Properties properties) {
        super(tier, damage, speed, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
