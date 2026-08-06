package net.vhsworld.rec.client.entity;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * O mesmo som, mais baixo.
 *
 * ⚠️ Um envelope em volta do som original, e nao um som novo: quem toca continua sendo
 * quem tocou, com a mesma posicao, a mesma atenuacao e o mesmo arquivo. Trocar por um
 * SimpleSoundInstance perderia tudo isso e o mundo abafado sairia com os sons no lugar
 * errado — que soa como bug, e nao como presenca.
 */
public record HushedSound(SoundInstance parent, float scale) implements SoundInstance {

    @Override
    public ResourceLocation getLocation() {
        return this.parent.getLocation();
    }

    @Nullable
    @Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        return this.parent.resolve(manager);
    }

    @Override
    public Sound getSound() {
        return this.parent.getSound();
    }

    @Override
    public SoundSource getSource() {
        return this.parent.getSource();
    }

    @Override
    public boolean isLooping() {
        return this.parent.isLooping();
    }

    @Override
    public boolean isRelative() {
        return this.parent.isRelative();
    }

    @Override
    public int getDelay() {
        return this.parent.getDelay();
    }

    @Override
    public float getVolume() {
        return this.parent.getVolume() * this.scale;
    }

    @Override
    public float getPitch() {
        return this.parent.getPitch();
    }

    @Override
    public double getX() {
        return this.parent.getX();
    }

    @Override
    public double getY() {
        return this.parent.getY();
    }

    @Override
    public double getZ() {
        return this.parent.getZ();
    }

    @Override
    public Attenuation getAttenuation() {
        return this.parent.getAttenuation();
    }

    @Override
    public boolean canStartSilent() {
        return this.parent.canStartSilent();
    }

    @Override
    public boolean canPlaySound() {
        return this.parent.canPlaySound();
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
        return this.parent.getStream(buffers, sound, looping);
    }
}
