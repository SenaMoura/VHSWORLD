package net.vhsworld.rec.client.codex;

/**
 * As tres divisoes do registro. Cada ficha pertence a uma; a tela abre com um card por
 * categoria e so entra na lista de itens depois de escolher — assim nenhuma lista fica
 * comprida o bastante para vazar da tela, que era o problema do registro antigo.
 *
 * A divisao segue o que o jogador FAZ com a coisa, nao do que ela e feita:
 *   CAMERA    — a camera e o que a alimenta (a pilha e, no futuro, as lentes/fitas);
 *   KILL      — o que se empunha para ferir (o kit corrompido, a FRACTURE, a tesoura);
 *   SURVIVE   — o que mantem vivo sem bater: fuga, orientacao e os materiais da cadeia.
 */
public enum CodexCategory {
    CAMERA("recmod.codex.category.camera", 0xFF6FB0C9),
    KILL("recmod.codex.category.kill", 0xFFC96F6F),
    SURVIVE("recmod.codex.category.survive", 0xFF7FC98F);

    /** Chave no lang para o titulo do card. */
    public final String titleKey;
    /** Cor de destaque do card e do contorno. */
    public final int accent;

    CodexCategory(String titleKey, int accent) {
        this.titleKey = titleKey;
        this.accent = accent;
    }
}
