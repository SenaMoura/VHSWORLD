package net.vhsworld.rec.item;

import net.minecraft.sounds.SoundEvent;
import net.vhsworld.rec.item.ModSounds;

/**
 * O Rastreador de Minerios.
 *
 * Depois do rito, o cliente varre a rocha em volta e crava um ponto vermelho no
 * minerio mais proximo da lista de valiosos — atravessando parede. Como a varredura
 * roda no cliente todo tanto de tick, ele reaponta sozinho: cavou o veio, ele pula
 * para o proximo. Nao grava nada no NBT alem da janela de tempo; o alvo e sempre o
 * "mais perto agora".
 */
public class OreTrackerItem extends TrackerItem {

    public OreTrackerItem(Properties properties) {
        super(properties);
    }

    @Override
    protected SoundEvent activationSound() {
        return ModSounds.HEARTBEAT.get();
    }

    @Override
    protected String activationKey() {
        return "recmod.ore_tracker.activated";
    }

    @Override
    protected String hintKey() {
        return "recmod.ore_tracker.hint";
    }
}
