package net.vhsworld.rec.debug;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.vhsworld.rec.director.Director;
import net.vhsworld.rec.director.Staging;
import net.vhsworld.rec.entity.StonemanEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A BANCADA — todo evento do mod, disparavel na hora.
 *
 * <h3>A regra da casa (Pedro, 2026-08-03)</h3>
 * <b>Nenhuma entidade e nenhum evento entra no mod sem um jeito de ver a coisa acontecer
 * agora.</b> Nao e conforto de programador: e o unico jeito de o mod poder ser TESTADO. A
 * historia deste projeto ja provou tres vezes que, no jogo, "nao aconteceu nada" e
 * indistinguivel de "esta quebrado" — e o sintoma nunca aponta para a causa. A colocacao da
 * v1.79.0 ficou escrita e nao testada por dez dias porque ver UMA acontecer exigia esperar
 * o Diretor querer; a ausencia da v1.76 passou num teste que nao provava nada.
 *
 * <h3>Como acrescentar</h3>
 * Uma linha em {@link #register()}. O comando (/rec eventos, /rec evento) le esta lista
 * sozinho — nao ha uma segunda lista para lembrar de atualizar, e e por isso que a regra
 * tem chance de durar depois desta sessao.
 *
 * ⚠️ TODO GATILHO PULA SO O <i>QUANDO</i>. Ele chama o mesmo caminho que o relogio chamaria,
 * com o mesmo som, o mesmo pacote e o mesmo report. Um gatilho que produzisse o efeito por
 * fora provaria que o gatilho funciona — a unica coisa que nao interessa saber.
 */
public final class TestBench {

    private TestBench() {}

    /**
     * Um evento disparavel. A resposta e o texto que vai para o chat: quando NAO acontecer,
     * ela tem que dizer por que — "nao aconteceu" sozinho e o mesmo silencio que a bancada
     * existe para acabar.
     */
    public record Trigger(String id, String help, Function<ServerPlayer, String> fire) {}

    private static final Map<String, Trigger> TRIGGERS = new LinkedHashMap<>();

    static {
        register();
    }

    private static void add(String id, String help, Function<ServerPlayer, String> fire) {
        TRIGGERS.put(id, new Trigger(id, help, fire));
    }

    private static void register() {

        // ---------------- o Diretor ----------------

        add("ausencia", "o mundo tira uma coisa que voce colocou (e voce ouve)",
                player -> Director.testAbsence(player)
                        ? "a ausencia aconteceu — escute"
                        : "nao achou alvo; o log diz o motivo (perto/longe/visao/mudou)");

        add("colocacao", "poe uma criatura no arco de tras, a 18-40 blocos",
                player -> {
                    Staging.Attempt a = Director.testStaging(player);
                    return switch (a.outcome()) {
                        case COLOCOU -> "COLOCOU " + a.type().getDescriptionId()
                                + " a " + Math.round(a.distance()) + " blocos, atras de voce";
                        case SEM_LUGAR -> "sem lugar: elenco=" + a.roster()
                                + ", nenhuma das 24 tentativas achou chao valido atras de voce"
                                + " (regra de spawn: ceu, luz, altura)";
                        case SEM_ELENCO -> "sem elenco: todos em espera, ou este bioma nao"
                                + " lista criatura nossa em MONSTER";
                        case DESLIGADO -> "colocacao desligada no config";
                    };
                });

        add("ruido", "o som que vem do nada (denuncia se ha criatura perto, senao mentira)",
                player -> Director.testNoise(player)
                        ? "o ruido saiu — a direcao e torta de proposito"
                        : "nao saiu");

        add("trilha", "manda a trilha entrar agora",
                player -> {
                    Director.testMusic(player, true);
                    return "trilha PLAY (o Diretor corta sozinha se a pressao subir)";
                });

        add("silencio", "corta a trilha agora",
                player -> {
                    Director.testMusic(player, false);
                    return "trilha STOP";
                });

        add("tenso", "poe a pressao em 0.9 para ver o Diretor NEGAR tudo",
                player -> {
                    Director.testPressure(player, 0.9f);
                    return "pressao 0.90 — ela cai sozinha em ~80s; use /diretor para ver";
                });

        add("calmo", "zera a pressao para ver o Diretor voltar a liberar",
                player -> {
                    Director.testPressure(player, 0.0f);
                    return "pressao 0.00";
                });

        add("elenco", "zera o racionamento: o elenco inteiro pode aparecer de novo",
                player -> {
                    Director.testForgetCast(player);
                    return "racionamento zerado";
                });

        // ---------------- criaturas ----------------

        add("surto", "faz o Homem de Pedra mais proximo QUEBRAR a regra do olhar",
                player -> {
                    StonemanEntity stoneman = nearest(player, StonemanEntity.class, 48.0D);
                    if (stoneman == null) return "nenhum Homem de Pedra em 48 blocos";
                    stoneman.testSurge();
                    return "surto: ele anda mesmo sendo olhado, por 2 a 4 segundos";
                });
    }

    // ------------------------------------------------------------------ leitura

    public static List<Trigger> all() {
        return new ArrayList<>(TRIGGERS.values());
    }

    public static Trigger get(String id) {
        return TRIGGERS.get(id);
    }

    private static <T extends Mob> T nearest(ServerPlayer player, Class<T> type, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        T best = null;
        double bestSqr = range * range;

        for (T mob : player.level().getEntitiesOfClass(type, box)) {
            double sqr = mob.distanceToSqr(player);
            if (sqr < bestSqr) {
                bestSqr = sqr;
                best = mob;
            }
        }
        return best;
    }
}
