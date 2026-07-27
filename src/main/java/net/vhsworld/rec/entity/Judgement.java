package net.vhsworld.rec.entity;

/**
 * O que o Ofanim faz com quem olhou tempo demais — e a ordem em que ele escala.
 *
 * NENHUM DELES MATA, e isso nao e delicadeza: e a regra do mod. O que os tres fazem e
 * tirar do jogador, por alguns segundos, a coisa de que ele mais depende na CHUNKS —
 * enxergar onde termina o chao. Numa ponte de oito blocos sobre o vazio, isso e mais
 * ameaca do que qualquer dano teria sido, e quem cai cai por conta propria.
 *
 * A ESCALA existe porque o segundo encontro nao pode ser o primeiro de novo. O castigo
 * sobe a cada julgamento e volta ao comeco depois do terceiro: sempre no maximo, o
 * jogador aprenderia a evitar o Ofanim de uma vez e nunca mais o veria de perto.
 */
public enum Judgement {

    /**
     * VERTIGEM. O horizonte deixa de ser confiavel: a tela rola e balanca, e o vazio
     * continua a vista o tempo todo. E o mais brando porque voce ainda enxerga — so
     * nao confia mais no proprio pe.
     */
    VERTIGO,

    /**
     * CEGUEIRA. Cinco segundos sem nada. Na CHUNKS isto e o castigo exato: voce sabe
     * onde estava a ponte, e vai ter que decidir se acredita na propria memoria ou se
     * fica parado esperando enxergar de novo.
     */
    BLINDNESS,

    /**
     * ELE TE LEVA. Voce apaga e acorda noutra coluna — talvez numa sem ponte nenhuma.
     * "Voce ve para onde nao da para ir" deixa de ser paisagem e vira o seu problema.
     *
     * Nao e beco sem saida: a fita sempre devolve o jogador para onde ele entrou. Sem
     * essa garantia este castigo nao poderia existir.
     */
    TAKEN;

    public static Judgement byIndex(int index) {
        Judgement[] all = values();
        return all[Math.floorMod(index, all.length)];
    }
}
