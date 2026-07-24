package net.vhsworld.rec.client;

/**
 * O estado, todo tick, das engenhocas de camera que ligam por PRESENCA e nao por clique:
 * a lente na mao, o tripe plantado por perto. Um lugar so para o resto do client
 * perguntar "isto esta ativo agora?", em vez de cada sistema refazer a deteccao.
 *
 * Reescrito a cada tick pelo ClientTickHandler; lido pela descarga da bateria, pelas
 * overlays (tinta infravermelha, monitor do tripe) e pelo HUD (que se esconde com o
 * tripe ligado).
 */
public final class GadgetState {

    /** A lente infravermelha esta na mao. */
    public static boolean infraredActive = false;

    /** Ha um tripe plantado no raio de vigilancia. */
    public static boolean tripodActive = false;

    /** O tripe viu alguma coisa se mexer (rasgo ou hostil) neste instante. */
    public static boolean tripodMotion = false;

    private GadgetState() {}
}
