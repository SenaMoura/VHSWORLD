package net.vhsworld.rec.director;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * O ESTADO DE UM JOGADOR, do ponto de vista do medo.
 *
 * Duas contas, e o mod inteiro sai delas:
 *
 *   pressao — o quanto ele esta tenso AGORA
 *   silencio — ha quanto tempo nada acontece com ele
 *
 * ⚠️ A ASSIMETRIA DA PRESSAO E A MECANICA, NAO UM DETALHE DE AJUSTE. Ela sobe rapido e
 * cai devagar, de proposito: quando a criatura vai embora, o jogador NAO volta na hora
 * a ser alguem com quem se pode fazer coisas. Ele continua tenso por mais de um minuto,
 * e nesse minuto o Diretor nega tudo. E assim que se compra o vazio depois do susto —
 * que e onde o medo mora de verdade, e a coisa que faltava no mod. Antes, o silencio
 * depois de um encontro era acidente: dependia de o proximo sorteio demorar.
 */
public final class Tension {

    /** Quantos ticks entre duas leituras. Meia dezena de vezes por segundo e caro atoa. */
    static final int SAMPLE_TICKS = 10;

    /**
     * Raio em que uma criatura nossa ainda pesa na pressao.
     *
     * ⚠️ E tambem a fronteira que o Director.perceived usa para cobrar o spawn. Os dois
     * TEM que ler o mesmo numero: se discordassem, um bicho poderia cobrar pressao ao
     * nascer e no tique seguinte sumir da conta, e o jogador levaria um teto de pressao
     * vindo do nada.
     */
    static final double NEAR = 48.0D;

    /** Sobe assim de rapido em direcao ao que o mundo esta pedindo. */
    private static final float RISE = 0.25f;

    /**
     * E cai assim de devagar. Com 0.004 por leitura (duas por segundo), sair do panico
     * (1.0) e voltar a poder receber um spawn (0.35) leva perto de oitenta segundos.
     * Esse numero E o vazio: encurtar isso e desfazer o Diretor.
     */
    private static final float FALL = 0.004f;

    private float pressure;
    private Beat lastBeat;

    /**
     * Ticks desde a ultima vez que CADA tipo de batida aconteceu.
     *
     * ⚠️ Comeca tudo em zero de proposito: quem acabou de entrar no mundo tem que cumprir
     * o piso de todas antes de qualquer coisa acontecer. Chegar e calmo.
     */
    private final int[] since = new int[Beat.values().length];

    /** Le o mundo em volta do jogador e move a pressao em direcao ao que achou. */
    void sample(ServerPlayer player) {
        for (int i = 0; i < since.length; i++) since[i] += SAMPLE_TICKS;

        float target = read(player);

        if (target > pressure) {
            pressure += (target - pressure) * RISE;
        } else {
            pressure = Math.max(target, pressure - FALL);
        }
        pressure = Math.min(1.0f, Math.max(0.0f, pressure));
    }

    /**
     * Quanta tensao o mundo esta pedindo neste instante.
     *
     * A criatura perto domina a conta, e e certo que domine: o resto (escuro, apanhar)
     * e tempero. O que assusta e o que esta ali.
     */
    private float read(ServerPlayer player) {
        Level level = player.level();
        float target = 0.0f;

        AABB box = player.getBoundingBox().inflate(NEAR);
        List<Mob> near = level.getEntitiesOfClass(Mob.class, box, Director::isOurs);
        for (Mob mob : near) {
            double distance = Math.sqrt(mob.distanceToSqr(player));
            if (distance > NEAR) continue;

            float weight = (float) (1.0D - distance / NEAR);

            // Ser VISTO por ela pesa mais que ela existir. Uma criatura do outro lado da
            // parede e um problema; uma criatura com linha de visao e um encontro.
            if (mob.hasLineOfSight(player)) weight = Math.min(1.0f, weight * 1.6f);

            target = Math.max(target, weight);
        }

        // Ter apanhado ha pouco. Nao e medo de monstro, e o corpo ainda acelerado.
        if (player.tickCount - player.getLastHurtByMobTimestamp() < 200) {
            target = Math.min(1.0f, target + 0.20f);
        }

        // O escuro sozinho nao e medo — mas e o lugar onde o medo cabe.
        if (level.getMaxLocalRawBrightness(player.blockPosition()) <= 4) {
            target = Math.min(1.0f, target + 0.12f);
        }

        return target;
    }

    /** Registra que uma batida aconteceu: zera o relogio DELA e cobra a pressao. */
    void onBeat(Beat beat) {
        onBeat(beat, 1.0f);
    }

    /**
     * ⚠️ O relogio zera INTEIRO mesmo com escala zero. Ver Director.report(…, costScale):
     * cadencia e pressao respondem a perguntas diferentes, e so a segunda depende de o
     * jogador ter percebido.
     */
    void onBeat(Beat beat, float costScale) {
        since[beat.ordinal()] = 0;
        lastBeat = beat;
        pressure = Math.min(1.0f, pressure + beat.cost() * costScale);
    }

    public float pressure() {
        return pressure;
    }

    /**
     * O SILENCIO DESTA BATIDA: tempo desde que ELA aconteceu. So ela.
     *
     * ⚠️ 3ª E ULTIMA VERSAO DESTA REGRA, e as duas anteriores morreram medidas no jogo.
     * Vale ler, porque o erro se repetiu de tres jeitos diferentes.
     *
     * v1: um relogio so, zerado por qualquer batida. O ruido (25s) matava o spawn (90s).
     * v2: dois relogios, leve e pesado. A ausencia (barata, mas espera 120s) caiu no lado
     *     errado e continuou morrendo.
     * v3: o relogio da batida era o MINIMO entre todas as batidas de peso maior ou igual.
     *     Parecia a generalizacao certa e era pior: o SPAWN, que e o mais pesado e pode
     *     acontecer a cada 90s, zerava o relogio de TODO MUNDO. Como a ausencia precisa
     *     de 120s e o ruido mentiroso de 150s, e 90 < 120 < 150, as duas viraram
     *     IMPOSSIVEIS — nao raras, impossiveis. Medido no jogo em 2026-08-02: quatro
     *     pulsos seguidos com os tres relogios zerando juntos a cada spawn.
     *
     * O erro de fundo, nas tres: eu usei o RELOGIO para as batidas conversarem entre si.
     * Quem existe para isso e a PRESSAO. Relogio responde "ha quanto tempo isto nao
     * acontece"; pressao responde "o jogador aguenta mais alguma coisa agora?". Com cada
     * batida lendo so o proprio relogio e a conversa toda na pressao, some a classe de
     * bug inteira — e ela e traicoeira porque o sintoma no jogo ("nao acontece nada")
     * nunca aponta para ca.
     */
    public int quiet(Beat beat) {
        return since[beat.ordinal()];
    }

    /** Ticks desde a ultima batida QUALQUER — so para leitura de fora (HUD, depuracao). */
    public int quiet() {
        int min = Integer.MAX_VALUE;
        for (int s : since) min = Math.min(min, s);
        return min;
    }

    public Beat lastBeat() {
        return lastBeat;
    }
}
