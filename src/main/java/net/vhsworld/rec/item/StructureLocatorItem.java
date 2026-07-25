package net.vhsworld.rec.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.item.ModSounds;

/**
 * O Localizador de Estruturas.
 *
 * A estrutura mais proxima so o SERVIDOR sabe achar (findNearestMapStructure), entao,
 * ao contrario do Rastreador, o alvo e fixado UMA vez, no rito, e gravado no NBT — que
 * sincroniza sozinho para o cliente, sem pacote proprio (o mod nao tem rede). Dai o
 * cliente desenha o ponto no horizonte e toca o coracao pela distancia horizontal.
 *
 * A lista de "o que conta como estrutura" e uma tag propria do datapack
 * (recmod:locatable), entao da para mexer sem tocar em codigo.
 */
public class StructureLocatorItem extends TrackerItem {

    /** O que o localizador enxerga. Editavel em data/recmod/tags/worldgen/structure/locatable.json. */
    private static final TagKey<Structure> LOCATABLE = TagKey.create(
            Registries.STRUCTURE, new ResourceLocation(RECMod.MOD_ID, "locatable"));

    /** Ate quantos chunks procurar. O mesmo teto que o /locate do vanilla usa. */
    private static final int SEARCH_RADIUS = 100;

    private static final String KEY_HAS_TARGET = "HasTarget";
    private static final String KEY_TARGET = "Target";

    public StructureLocatorItem(Properties properties) {
        super(properties);
    }

    @Override
    protected Component onActivate(ServerLevel level, ServerPlayer player, ItemStack stack) {
        // false = nao pula estruturas ja geradas; queremos a mais proxima que exista.
        BlockPos found = level.findNearestMapStructure(LOCATABLE, player.blockPosition(),
                SEARCH_RADIUS, false);

        CompoundTag tag = stack.getOrCreateTag();
        if (found == null) {
            tag.putBoolean(KEY_HAS_TARGET, false);
            tag.remove(KEY_TARGET);
            return Component.translatable("recmod.structure_locator.empty");
        }

        tag.putBoolean(KEY_HAS_TARGET, true);
        tag.putLong(KEY_TARGET, found.asLong());

        double dx = found.getX() - player.getX();
        double dz = found.getZ() - player.getZ();
        int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        return Component.translatable("recmod.structure_locator.locked", distance);
    }

    /** O alvo fixado no rito, ou null se o rito nao achou nada. Lido pelo cliente. */
    public static BlockPos target(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean(KEY_HAS_TARGET)) return null;
        return BlockPos.of(tag.getLong(KEY_TARGET));
    }

    @Override
    protected SoundEvent activationSound() {
        return ModSounds.HEARTBEAT.get();
    }

    @Override
    protected String activationKey() {
        return "recmod.structure_locator.activated";
    }

    @Override
    protected String hintKey() {
        return "recmod.structure_locator.hint";
    }
}
