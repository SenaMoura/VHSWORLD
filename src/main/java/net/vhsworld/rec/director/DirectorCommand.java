package net.vhsworld.rec.director;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;

/**
 * A MESA DO DIRETOR: ver o compasso e forcar uma batida.
 *
 * ⚠️ POR QUE ISTO EXISTE, E POR QUE VEIO ANTES DE QUALQUER MECANICA NOVA. A colocacao
 * (v1.79.0) ficou escrita e nao testada por um motivo bobo e caro: para ver UMA colocacao
 * era preciso esperar o Diretor querer — piso de noventa segundos, teto de pressao 0.35 e
 * um dado de 0.005 por segundo. Na sessao de 2026-08-03 isso deu dez minutos de jogo sem
 * uma unica linha de colocacao no log: nem COLOCOU, nem sem lugar, nem sem elenco. Nao
 * havia como saber se a mecanica funciona, se o lugar nunca serve ou se o dado nunca caiu.
 *
 * E esse e, palavra por palavra, o modo de falha que este pacote inteiro ja pagou tres
 * vezes: <b>no jogo, "nada aconteceu" e indistinguivel de "esta quebrado"</b>. A diferenca
 * e que ali o sintoma escondia um bug e aqui ele esconde o teste — mesmo estrago.
 *
 * ⚠️ O QUE ELE PULA E SO O <i>QUANDO</i>. `colocar` nao pula o racionamento, nem a regra de
 * spawn, nem o arco de tras, nem o report da pressao. Um comando que fizesse a criatura
 * aparecer por um caminho proprio provaria que o comando funciona — que e a unica coisa que
 * nao interessa saber. O ritmo continua so podendo ser medido jogando.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID)
public final class DirectorCommand {

    private DirectorCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("diretor")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> state(ctx.getSource()))
                .then(Commands.literal("colocar").executes(ctx -> place(ctx.getSource())))
                .then(Commands.literal("ausencia").executes(ctx -> absence(ctx.getSource())))
                .then(Commands.literal("limpar").executes(ctx -> forget(ctx.getSource()))));
    }

    // ------------------------------------------------------------------ o estado

    /**
     * O PULSO, SO QUE NA HORA EM QUE SE PERGUNTA.
     *
     * A linha de log a cada trinta segundos responde bem depois do fato; esta responde
     * enquanto se esta olhando para o lugar onde nada aconteceu. E ela mostra a CHANCE,
     * que o log nao mostra — sem esse numero, "esta liberado ha dois minutos e nao veio
     * nada" parece defeito e quase sempre e o dado fazendo o trabalho dele.
     */
    private static int state(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Tension t = Director.tension(player);

        boolean on = RECConfig.COMMON.director.get();

        source.sendSuccess(() -> Component.literal("[DIRETOR] ")
                .withStyle(ChatFormatting.DARK_PURPLE)
                .append(Component.literal(on ? "no compasso" : "DESLIGADO (director=false)")
                        .withStyle(on ? ChatFormatting.GRAY : ChatFormatting.RED))
                .append(Component.literal("  pressao " + fixed(t.pressure()))
                        .withStyle(pressureColor(t.pressure()))), false);

        for (Beat beat : Beat.values()) {
            source.sendSuccess(() -> line(player, t, beat), false);
        }

        // O elenco, com quem esta em espera — a metade que o log nao conta.
        StringBuilder cast = new StringBuilder();
        for (Staging.CastMember member : Staging.cast(player)) {
            if (cast.length() > 0) cast.append("  ");
            cast.append(name(member.type()));
            cast.append(member.waitSeconds() == 0 ? " livre" : " espera " + clock(member.waitSeconds()));
        }
        String elenco = cast.length() == 0
                ? "elenco VAZIO — este bioma nao lista criatura nossa"
                : "elenco: " + cast;

        source.sendSuccess(() -> Component.literal(elenco)
                .withStyle(cast.length() == 0 ? ChatFormatting.RED : ChatFormatting.GRAY), false);

        source.sendSuccess(() -> Component.literal("rastro: "
                + PlacementTrace.of(player).size() + " blocos seus")
                .withStyle(ChatFormatting.DARK_GRAY), false);

        return 1;
    }

    /** Uma batida: relogio, piso, teto e a chance por segundo agora. */
    private static Component line(ServerPlayer player, Tension t, Beat beat) {
        int quiet = t.quiet(beat) / 20;
        int floor = beat.floorTicks() / 20;

        boolean waiting = quiet < floor;
        boolean capped = t.pressure() > beat.ceiling();

        String verdict;
        ChatFormatting color;
        if (waiting) {
            verdict = "espera o piso";
            color = ChatFormatting.DARK_GRAY;
        } else if (capped) {
            verdict = "NEGADA (pressao acima de " + fixed(beat.ceiling()) + ")";
            color = ChatFormatting.RED;
        } else if (beat.urgePerSecond() <= 0.0f) {
            verdict = "liberada (so o vanilla pede)";
            color = ChatFormatting.YELLOW;
        } else {
            verdict = "liberada, chance " + percent(Director.urgeNow(t, beat)) + "/s";
            color = ChatFormatting.GREEN;
        }

        return Component.literal(String.format("  %-9s %4ds/%3ds  ", label(beat), quiet, floor))
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(verdict).withStyle(color));
    }

    // ------------------------------------------------------------------ forcar

    /**
     * ⚠️ Nao confunde "nao colocou" com "quebrou": cada saida diz qual das tres causas foi.
     * Era exatamente isto que faltava para a v1.79.0 poder ser testada.
     */
    private static int place(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Staging.Attempt attempt = Staging.force(player);

        switch (attempt.outcome()) {
            case COLOCOU -> source.sendSuccess(() -> Component.literal(
                    "COLOCOU " + name(attempt.type()) + " a " + Math.round(attempt.distance())
                            + " blocos, atras de voce, em " + attempt.pos().toShortString())
                    .withStyle(ChatFormatting.GREEN), false);

            case SEM_LUGAR -> source.sendSuccess(() -> Component.literal(
                    "sem lugar — o elenco tinha " + attempt.roster() + " tipo(s) e nenhuma das 24"
                            + " tentativas achou chao valido no arco de tras (regra de spawn: ceu,"
                            + " luz, altura)")
                    .withStyle(ChatFormatting.YELLOW), false);

            case SEM_ELENCO -> source.sendSuccess(() -> Component.literal(
                    "sem elenco — ou todos em espera (veja /diretor), ou este bioma nao lista"
                            + " criatura nossa em MONSTER")
                    .withStyle(ChatFormatting.YELLOW), false);

            case DESLIGADO -> source.sendSuccess(() -> Component.literal(
                    "colocacao desligada no config (directorStaging=false)")
                    .withStyle(ChatFormatting.RED), false);
        }
        return 1;
    }

    /**
     * A ausencia forcada continua obedecendo as tres travas (e seu, e pelas costas, e
     * barato de desfazer) e o alvo continua sendo escolhido por solidao. So o relogio sai
     * do caminho.
     */
    private static int absence(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        if (Absence.tryApply(player)) {
            Director.report(player, Beat.ABSENCE);
            source.sendSuccess(() -> Component.literal("a ausencia aconteceu — escute")
                    .withStyle(ChatFormatting.GREEN), false);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "nao achou alvo — o log diz o motivo (perto/longe/visao/mudou)")
                    .withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private static int forget(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Staging.forget(player);
        source.sendSuccess(() -> Component.literal("racionamento zerado: o elenco inteiro pode aparecer de novo")
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    // ------------------------------------------------------------------ formato

    private static String label(Beat beat) {
        return switch (beat) {
            case NOISE -> "ruido";
            case ABSENCE -> "ausencia";
            case SPAWN -> "spawn";
            case MUSIC -> "trilha";
        };
    }

    private static String name(net.minecraft.world.entity.EntityType<?> type) {
        var key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return key == null ? "?" : key.getPath();
    }

    private static String clock(int seconds) {
        return seconds >= 60 ? (seconds / 60) + "m" + String.format("%02d", seconds % 60) + "s"
                : seconds + "s";
    }

    private static String fixed(float value) {
        return String.format("%.2f", value);
    }

    private static String percent(float value) {
        return String.format("%.1f%%", value * 100.0f);
    }

    private static ChatFormatting pressureColor(float pressure) {
        if (pressure > 0.50f) return ChatFormatting.RED;
        if (pressure > 0.35f) return ChatFormatting.YELLOW;
        return ChatFormatting.GREEN;
    }
}
