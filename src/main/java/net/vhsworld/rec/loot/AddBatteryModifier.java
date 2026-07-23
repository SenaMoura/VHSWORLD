package net.vhsworld.rec.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.vhsworld.rec.init.ModItems;

import java.util.function.Supplier;

// Adiciona pilhas aos baús (loot tables) com uma chance e quantidade configuráveis no JSON.
public class AddBatteryModifier extends LootModifier {

    public static final Supplier<Codec<AddBatteryModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(inst -> codecStart(inst).and(inst.group(
                    Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance),
                    Codec.INT.optionalFieldOf("min", 1).forGetter(m -> m.min),
                    Codec.INT.optionalFieldOf("max", 1).forGetter(m -> m.max)
            )).apply(inst, AddBatteryModifier::new)));

    private final float chance;
    private final int min;
    private final int max;

    protected AddBatteryModifier(LootItemCondition[] conditions, float chance, int min, int max) {
        super(conditions);
        this.chance = chance;
        this.min = min;
        this.max = max;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() <= chance) {
            int count = min >= max ? min : min + context.getRandom().nextInt(max - min + 1);
            if (count > 0) {
                generatedLoot.add(new ItemStack(ModItems.BATTERY.get(), count));
            }
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
