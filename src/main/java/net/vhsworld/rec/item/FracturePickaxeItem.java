package net.vhsworld.rec.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;

/** Picareta da FRATURA. Existe so para o brilho: o resto e o tier. */
public class FracturePickaxeItem extends PickaxeItem {

    public FracturePickaxeItem(Tier tier, int damage, float speed, Properties properties) {
        super(tier, damage, speed, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
