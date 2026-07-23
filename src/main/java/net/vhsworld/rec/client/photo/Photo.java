package net.vhsworld.rec.client.photo;

import net.minecraft.resources.ResourceLocation;

/**
 * Uma foto tirada com o flash.
 *
 * O arquivo PNG fica em .minecraft/vhsworld_photos/. Este objeto e so a ficha dela:
 * quando foi tirada, se ja foi revelada e o que o filme pegou.
 */
public class Photo {

    /** Nome do arquivo, sem pasta. Serve tambem de identidade da foto. */
    public final String file;

    /** Momento em que foi tirada (millis), so para ordenar e mostrar. */
    public final long takenAt;

    /**
     * O que o filme registrou, se registrou algo. null = nada apareceu.
     *
     * Hoje isto quase sempre vem null, porque o mod ainda nao tem entidades.
     * A deteccao ja funciona: quando as entidades existirem, elas caem aqui sozinhas.
     */
    public String subject;

    /** Revelada = da para ver a imagem. */
    public boolean developed;

    /** Progresso da revelacao, em ticks. */
    public int developTicks;

    /** Textura carregada sob demanda; null enquanto a foto nao foi aberta. */
    public transient ResourceLocation texture;

    /**
     * Ticks desde que a revelacao chegou a 100%, para a imagem nascer no lugar do
     * filme velado em vez de aparecer de estalo.
     *
     * -1 = sem animacao. E o valor de quem ja estava revelada quando o jogo abriu:
     * foto velha nao se revela de novo toda vez que voce olha para ela.
     */
    public transient int revealFade = -1;

    /** true se o PNG sumiu do disco — a foto vira uma ficha orfa. */
    public transient boolean broken;

    public Photo(String file, long takenAt) {
        this.file = file;
        this.takenAt = takenAt;
    }
}
