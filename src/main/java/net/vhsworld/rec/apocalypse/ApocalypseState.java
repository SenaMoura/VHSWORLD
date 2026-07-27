package net.vhsworld.rec.apocalypse;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * A unica coisa que o CALLER deixa para tras: um sim, gravado no mundo.
 *
 * Mora no OVERWORLD e vale para o mundo inteiro, inclusive para quem entrar depois.
 * O apocalipse nao e um efeito com duracao — e um estado. Guardar isso num campo
 * estatico faria ele evaporar no primeiro reinicio do servidor, e o jogador voltaria
 * a um mundo curado sem ter feito nada por isso; e o oposto do que o item significa.
 *
 * Nao ha desfazer. Nao por castigo: se desse para desligar, apertar o botao viraria
 * uma experiencia, e a unica coisa que o CALLER tem para oferecer e o peso de ser
 * irreversivel.
 */
public class ApocalypseState extends SavedData {

    private static final String NAME = "recmod_apocalypse";

    private boolean active;
    private long startedAt;

    public static ApocalypseState get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                ApocalypseState::load, ApocalypseState::new, NAME);
    }

    public boolean isActive() {
        return active;
    }

    public long startedAt() {
        return startedAt;
    }

    public void begin(long gameTime) {
        if (active) return;
        this.active = true;
        this.startedAt = gameTime;
        setDirty();
    }

    private static ApocalypseState load(CompoundTag tag) {
        ApocalypseState state = new ApocalypseState();
        state.active = tag.getBoolean("Active");
        state.startedAt = tag.getLong("StartedAt");
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("Active", active);
        tag.putLong("StartedAt", startedAt);
        return tag;
    }
}
