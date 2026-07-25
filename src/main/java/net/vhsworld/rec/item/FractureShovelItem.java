package net.vhsworld.rec.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;

/** Pa da FRATURA. Existe so para o brilho: o resto e o tier. */
public class FractureShovelItem extends ShovelItem {

    public FractureShovelItem(Tier tier, float damage, float speed, Properties properties) {
        super(tier, damage, speed, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
