package net.vhsworld.rec.client.difficulty;

import net.vhsworld.rec.config.RECConfig;

/**
 * As duas fitas que o jogador pode escolher ao entrar no mundo.
 *
 * O DIFICIL nao adiciona castigo novo: ele aperta o que ja existe. Sanidade cai
 * mais fundo por revelacao, a bateria dura menos e a fita comeca a se voltar contra
 * voce mais cedo. **Nao existe morte por sanidade em nenhuma das duas** — essa foi
 * uma decisao de design tomada la atras e o dificil nao a reabre: a sanidade so cai
 * por ESCOLHA do jogador (revelar a foto), entao matar por isso seria castigar quem
 * olhou. O preco continua sendo a camera deixar de ser lugar seguro.
 *
 * Os numeros do dificil moram no config (secao [dificuldade]), nao aqui: da para
 * afinar sem recompilar.
 */
public enum GameDifficulty {

    NORMAL("recmod.difficulty.normal", 0xFF6FB0C9),
    HARD("recmod.difficulty.hard", 0xFFC96F6F);

    /** Chave do lang do nome; a descricao e ".desc" e a etiqueta ".tag". */
    public final String key;

    /** Cor do card e do contorno, no mesmo idioma visual dos cards do registro. */
    public final int accent;

    GameDifficulty(String key, int accent) {
        this.key = key;
        this.accent = accent;
    }

    public boolean hard() {
        return this == HARD;
    }

    /** Quanto a revelacao de uma foto custa de sanidade, vezes isto. */
    public float sanityLossMultiplier() {
        return hard() ? RECConfig.CLIENT.hardSanityMultiplier.get().floatValue() : 1.0f;
    }

    /** Quanto a bateria gasta por tick, vezes isto (maior = dura menos). */
    public float batteryDrainMultiplier() {
        return hard() ? RECConfig.CLIENT.hardBatteryMultiplier.get().floatValue() : 1.0f;
    }

    /** Frequencia dos apagoes espontaneos e dos sons fantasma, vezes isto. */
    public float hauntingMultiplier() {
        return hard() ? RECConfig.CLIENT.hardHauntingMultiplier.get().floatValue() : 1.0f;
    }

    /**
     * Quantos pontos percentuais o limiar da sanidade sobe.
     *
     * O limiar e onde a fita comeca a apodrecer. Subir ele NAO tira sanidade — faz a
     * degradacao comecar antes, com o medidor mais cheio.
     */
    public int thresholdBonus() {
        return hard() ? RECConfig.CLIENT.hardThresholdBonus.get() : 0;
    }

    public static GameDifficulty byName(String name) {
        for (GameDifficulty d : values()) {
            if (d.name().equalsIgnoreCase(name)) return d;
        }
        return NORMAL;
    }
}
