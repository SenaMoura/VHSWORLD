package net.vhsworld.rec.director;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.net.DreadPacket;
import net.vhsworld.rec.net.MusicPacket;
import net.vhsworld.rec.net.RECNetwork;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * O DIRETOR. Quem conta o compasso.
 *
 * ⚠️ LEIA ISTO ANTES DE ACRESCENTAR QUALQUER COISA QUE ACONTECA COM O JOGADOR.
 *
 * O PROBLEMA QUE ELE RESOLVE. O mod tinha muita coisa e nenhum ritmo. Cada mecanica
 * tinha o proprio relogio: o som ambiente sorteava um intervalo e tocava, o spawn era
 * vanilla puro, cada criatura decidia sozinha. Nenhuma sabia da outra. Isso produz
 * exatamente o que se estava sentindo no jogo — sustos empilhados numa hora, nada em
 * outra, e um som de fundo que o jogador para de escutar em quinze minutos porque ele
 * nao SIGNIFICA nada. Aleatorio nao e imprevisivel: aleatorio e ruido, e o cerebro
 * aprende a ignorar ruido mais rapido do que aprende um padrao.
 *
 * AS DUAS COISAS QUE ELE FAZ, e que nenhum sistema do mod fazia:
 *
 *   NEGAR. Quando o jogador ja esta tenso, o Diretor recusa tudo. A coisa mais
 *   assustadora que se pode fazer com quem esta com medo e nada — e o vazio depois do
 *   susto que transforma um susto em medo. Ver a queda lenta da pressao em Tension.
 *
 *   CORRELACIONAR. O som do nada agora toca por um MOTIVO: ou tem uma criatura nossa por
 *   perto (e ele vem da direcao dela), ou o silencio ja passou do limite (e ai ele e
 *   mentira, em direcao qualquer). O jogador nao tem como distinguir os dois casos — e
 *   e exatamente por isso que ele volta a escutar. Um som que as vezes e verdade e
 *   informacao; um som que nunca e verdade e papel de parede.
 *
 * A REGRA DA CASA: nada no mod acontece sozinho com o jogador. Pede-se `allow`, e quando
 * acontece, avisa-se com `report`. Mecanica que nao pedir volta a ser um relogio solto e
 * desfaz este arquivo inteiro.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID)
public final class Director {

    private Director() {}

    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private static final Random RANDOM = new Random();

    private static final Map<UUID, Tension> STATE = new HashMap<>();

    /** Para quem o Diretor mandou tocar e ainda nao mandou calar. Ver maybeMusic. */
    private static final java.util.Set<UUID> MUSIC_ON = new java.util.HashSet<>();

    private static int tick;

    /**
     * ⚠️ INSTRUMENTACAO DE DIAGNOSTICO — o PULSO.
     *
     * Existe porque a primeira sessao de teste (1.76.1, ~9 minutos) nao produziu UMA
     * linha do Diretor, e o registro que havia nao era capaz de dizer por que. Nao dava
     * para distinguir "o tique nao roda", "o piso nao venceu", "a pressao esta no teto" e
     * "o silencio e zerado por outra batida antes de vencer o piso" — quatro causas, o
     * mesmo sintoma (nada acontece), consertos diferentes.
     *
     * Uma linha a cada trinta segundos com os tres relogios e a pressao responde as
     * quatro de uma vez. Pode sair quando o ritmo estiver ajustado.
     */
    private static final int PULSE_TICKS = 600;

    // ------------------------------------------------------------------ a porta

    /**
     * "POSSO?" — a unica pergunta que o resto do mod faz a este arquivo.
     *
     * Duas portas em serie, e cada uma existe por um defeito observado no jogo:
     *   1. o piso de silencio, contra coisas grudadas uma na outra;
     *   2. o teto de pressao, contra empilhar em cima de quem ja esta com medo.
     *
     * ⚠️ AQUI NAO HA DADO, E ISSO E PROPOSITAL. Esta e a pergunta de PERMISSAO, e a
     * resposta tem que ser a mesma para as mesmas condicoes. Se houvesse sorteio aqui,
     * quem pergunta viraria o dono da frequencia — e quem pergunta pelo SPAWN e o motor
     * do proprio jogo, que tenta em ritmo proprio, incontrolavel e diferente em cada
     * bioma. O dado, quando ha, mora em `wants`, que so vale para batida que o Diretor
     * inicia sozinho.
     */
    public static boolean allow(ServerPlayer player, Beat beat) {
        // Desligado, o mod volta a ser o que era: cada mecanica no proprio relogio.
        if (!RECConfig.COMMON.director.get()) return true;

        Tension t = of(player);

        // ⚠️ O silencio que vale e o DESTA batida, nao o geral. Ver Beat.heavy().
        if (t.quiet(beat) < beat.floorTicks()) return false;
        if (t.pressure() > beat.ceiling()) return false;

        return true;
    }

    /**
     * "E EU QUERO, AGORA?" — so para batida que parte do Diretor (ver urgePerSecond).
     *
     * A chance sobe conforme o silencio passa do piso: e a curva que garante que a
     * calmaria SEMPRE quebra, sem que a hora seja previsivel. O teto no `over` existe
     * para o fim da espera nao virar certeza — silencio ja muito longo tem que continuar
     * podendo esticar mais um pouco, senao ele vira hora marcada de novo.
     */
    private static boolean wants(Tension t, Beat beat) {
        if (beat.urgePerSecond() <= 0.0f) return false;

        float over = Math.min(4.0f,
                (t.quiet(beat) - beat.floorTicks()) / (float) beat.floorTicks());

        // ⚠️ A taxa e POR SEGUNDO, e este metodo roda a cada SAMPLE_TICKS. Sem esta
        // conversao, a mesma constante daria um ruido a cada vinte e seis segundos,
        // cravados — o metronomo que o Diretor existe para nao ser.
        float perSample = beat.urgePerSecond() * (1.0f + over) * (Tension.SAMPLE_TICKS / 20.0f);

        return RANDOM.nextFloat() < perSample;
    }

    /**
     * "ACONTECEU." Zera o silencio e cobra a pressao.
     *
     * ⚠️ Quem chama `allow` e faz a coisa TEM que chamar isto. Sem o report, a batida
     * nao existe para o Diretor e ele libera a proxima em seguida — que e o defeito
     * original, so que agora com mais codigo.
     */
    public static void report(ServerPlayer player, Beat beat) {
        report(player, beat, 1.0f);
    }

    /**
     * A mesma coisa, mas cobrando so uma FRACAO da pressao.
     *
     * ⚠️ ISTO EXISTE POR UM DEFEITO MEDIDO NO JOGO (2026-08-02): um shade_segment nasceu
     * a 114 blocos do jogador e cobrou os 0.55 de pressao inteiros. A 114 blocos nao ha o
     * que ver, ouvir ou suspeitar — e mesmo assim aquele bicho invisivel bloqueou a
     * ausencia (teto 0.50), cortou a trilha (corte em 0.45) e travou o proximo spawn.
     *
     * O PRINCIPIO QUE FALTAVA, e que vale para toda batida futura: <b>o Diretor modela a
     * EXPERIENCIA do jogador, nao os eventos do mundo.</b> Acontecimento que o jogador nao
     * tem como perceber nao pode mexer no estado emocional dele. Ja tinha aparecido uma
     * vez, no relogio do ruido, e eu consertei o relogio e esqueci a pressao.
     *
     * O RELOGIO CONTINUA ZERANDO INTEIRO, de proposito: o compasso de "uma criatura de
     * cada vez" vale a mesma coisa perto ou longe, porque ele existe para nao empilhar
     * bicho no mundo. Quem tem que respeitar a distancia e a pressao, nao a cadencia.
     */
    public static void report(ServerPlayer player, Beat beat, float costScale) {
        of(player).onBeat(beat, costScale);
    }

    /**
     * O quanto uma coisa a esta distancia pesa no jogador: 1 colado, 0 a partir do raio
     * em que Tension ja para de enxergar. E a mesma fronteira dos dois lados — se elas
     * discordassem, um bicho poderia cobrar pressao ao nascer e depois sumir da conta.
     */
    public static float perceived(double distance) {
        float weight = (float) (1.0D - distance / Tension.NEAR);
        return Math.max(0.0f, Math.min(1.0f, weight));
    }

    /** Para quem precisa ler a tensao sem pedir nada (efeito de tela, musica, HUD). */
    public static float pressure(ServerPlayer player) {
        return of(player).pressure();
    }

    /** Uma criatura DESTE mod? E como o Diretor sabe do que ele e dono. */
    public static boolean isOurs(Entity entity) {
        var key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && RECMod.MOD_ID.equals(key.getNamespace());
    }

    // ------------------------------------------------------------------ o compasso

    /** Prova de que a classe foi registrada. Se esta linha nao sair, o problema e o bus. */
    @SubscribeEvent
    public static void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        LOG.info("[DIRETOR] armado (director={}, absence={})",
                RECConfig.COMMON.director.get(), RECConfig.COMMON.absence.get());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!RECConfig.COMMON.director.get()) return;

        if (++tick % Tension.SAMPLE_TICKS != 0) return;

        MinecraftServer server = event.getServer();
        for (ServerLevel level : server.getAllLevels()) {
            // ⚠️ Copia da lista: as batidas podem mexer em entidade, e mexer no mundo
            // enquanto se itera a lista dele foi exatamente o crash do MirrorWalk.
            for (ServerPlayer player : List.copyOf(level.players())) {
                if (player.isSpectator() || !player.isAlive()) continue;

                Tension t = of(player);
                t.sample(player);

                if (tick % PULSE_TICKS == 0) {
                    LOG.info("[DIRETOR] pulso {} | pressao {} | ruido {}s/{}s | ausencia {}s/{}s | spawn {}s/{}s | rastro {}",
                            player.getGameProfile().getName(),
                            String.format("%.2f", t.pressure()),
                            t.quiet(Beat.NOISE) / 20, Beat.NOISE.floorTicks() / 20,
                            t.quiet(Beat.ABSENCE) / 20, Beat.ABSENCE.floorTicks() / 20,
                            t.quiet(Beat.SPAWN) / 20, Beat.SPAWN.floorTicks() / 20,
                            PlacementTrace.of(player).size());
                }

                maybeMusic(player, t);
                maybeStaging(player, t);
                maybeAbsence(player, t);
                maybeNoise(player, t);
            }
        }
    }

    /**
     * O SOM DO NADA — agora com motivo.
     *
     * Este metodo e o coracao do arquivo. Antes ele morava no cliente (AmbientDread) com
     * um `countdown` sorteado, independente de tudo: tocava igual se voce tinha acabado
     * de escapar de alguma coisa ou se estava minerando em paz ha uma hora. Aqui ele so
     * toca em dois casos, e o jogador nao consegue saber em qual dos dois esta:
     *
     *   A DENUNCIA — existe criatura nossa no raio. O som vem da DIRECAO dela, mas nao da
     *   distancia dela: diz mais ou menos ONDE, nunca O QUE nem QUAO PERTO. E verdade.
     *
     *   A MENTIRA — nao ha nada, mas o silencio ja passou do limite longo. Direcao
     *   qualquer. E o Diretor quebrando a calmaria de proposito.
     *
     * Duas verdades e uma mentira depois, o jogador para de conseguir ignorar som. Esse e
     * o objetivo inteiro: devolver ao audio o peso de ser informacao.
     */
    /**
     * A TRILHA — e, mais importante, o CORTE dela.
     *
     * ⚠️ A ORDEM DAS DUAS METADES IMPORTA. O corte vem primeiro e sem pedir licenca a
     * nada: se a pressao subiu, a musica morre AGORA, mesmo que o piso dela nao tenha
     * vencido, mesmo que ela tenha comecado ha tres segundos. Faixa que insiste enquanto
     * a coisa se aproxima e trilha de filme; faixa que some sem explicacao e a melhor
     * ferramenta de terror que este mod tem, e ela sai de graca da pressao que ja
     * calculavamos.
     *
     * O servidor nao sabe se a faixa acabou sozinha, e nao precisa: PLAY em cima de
     * musica tocando o cliente ignora, e STOP sem musica nao faz nada. O estado aqui e
     * so para nao mandar um STOP por tique enquanto a tensao dura.
     */
    private static void maybeMusic(ServerPlayer player, Tension t) {
        if (!RECConfig.COMMON.directorMusic.get()) return;

        float cut = RECConfig.COMMON.directorMusicCutPressure.get().floatValue();

        if (t.pressure() >= cut) {
            if (MUSIC_ON.remove(player.getUUID())) {
                RECNetwork.toPlayer(player, new MusicPacket(MusicPacket.Action.STOP));
                LOG.info("[DIRETOR] trilha CORTADA | pressao {}", String.format("%.2f", t.pressure()));
            }
            return;
        }

        if (!allow(player, Beat.MUSIC)) return;
        if (!wants(t, Beat.MUSIC)) return;

        int silence = t.quiet(Beat.MUSIC) / 20;

        MUSIC_ON.add(player.getUUID());
        RECNetwork.toPlayer(player, new MusicPacket(MusicPacket.Action.PLAY));
        report(player, Beat.MUSIC);

        LOG.info("[DIRETOR] trilha | pressao {} | silencio {}s",
                String.format("%.2f", t.pressure()), silence);
    }

    /**
     * A COLOCACAO — o Diretor finalmente PEDINDO um encontro, em vez de so poder negar.
     *
     * ⚠️ VEM PRIMEIRO ENTRE AS TRES QUE MEXEM COM O JOGADOR, e a ordem e a regra. Ela e a
     * batida mais cara (0.55) e a de teto mais baixo (0.35): so cabe em calmaria de
     * verdade, e e a mais rara das tres. Se corresse por ultimo, ausencia e ruido — que
     * sao baratos e esperam menos — gastariam a calmaria antes, e o encontro, que e a
     * unica batida que o mod tem com uma criatura dentro, quase nunca aconteceria.
     *
     * E o `report` mora no Staging, nao aqui, porque so ele sabe se achou lugar. Cobrar a
     * pressao por uma colocacao que nao encontrou chao valido atras do jogador seria o
     * Diretor calar o mundo por um encontro que nao existe.
     */
    private static void maybeStaging(ServerPlayer player, Tension t) {
        if (!allow(player, Beat.SPAWN)) return;
        if (!wants(t, Beat.SPAWN)) return;

        Staging.tryPlace(player);
    }

    /**
     * A AUSENCIA — a batida que nao acontece na hora em que acontece.
     *
     * ⚠️ Ela vem ANTES do ruido no tique de proposito. As duas competem pelo mesmo
     * silencio, e se o ruido corresse primeiro ele levaria quase sempre: e mais barato e
     * espera menos. A ausencia e a batida melhor das duas — vale mais dar a vez a ela nas
     * raras vezes em que ela esta pronta.
     *
     * E o `report` so acontece se a ausencia REALMENTE mexeu em alguma coisa. Cobrar o
     * silencio por uma tentativa que nao achou tocha nenhuma seria o Diretor calando o
     * mundo por um evento que o jogador nao tem como perceber.
     */
    private static void maybeAbsence(ServerPlayer player, Tension t) {
        if (!allow(player, Beat.ABSENCE)) return;
        if (!wants(t, Beat.ABSENCE)) return;

        if (Absence.tryApply(player)) {
            report(player, Beat.ABSENCE);
        }
    }

    private static void maybeNoise(ServerPlayer player, Tension t) {
        if (!allow(player, Beat.NOISE)) return;
        if (!wants(t, Beat.NOISE)) return;

        double range = RECConfig.COMMON.directorNoiseTellRange.get();
        Mob tell = nearestOurs(player, range);

        double angle;
        if (tell != null) {
            // A direcao certa, torta de proposito: som nao e bussola.
            double real = Math.atan2(tell.getZ() - player.getZ(), tell.getX() - player.getX());
            angle = real + (RANDOM.nextDouble() - 0.5D) * (Math.PI / 2.6D);
        } else {
            // ⚠️ O relogio DO RUIDO, nao o silencio geral. A mentira existe para quebrar
            // um silencio AUDITIVO longo; uma criatura que nasceu a noventa blocos, que o
            // jogador nem soube que existe, nao quebrou silencio nenhum. Ler o relogio
            // geral aqui foi o que tornou a mentira quase inalcancavel.
            int longSilence = RECConfig.COMMON.directorLongSilenceSeconds.get() * 20;
            if (t.quiet(Beat.NOISE) < longSilence) return;
            angle = RANDOM.nextDouble() * Math.PI * 2.0D;
        }

        // Distancia sempre inventada, com ou sem denuncia: se ela fosse a real, o jogador
        // usaria o som como sonar e a duvida — que e a mecanica — acabaria.
        double distance = 6.0D + RANDOM.nextDouble() * 10.0D;
        Vec3 offset = new Vec3(
                Math.cos(angle) * distance,
                RANDOM.nextDouble() * 3.0D - 1.0D,
                Math.sin(angle) * distance);

        // ⚠️ Lido ANTES do report: report zera o relogio, e a primeira versao registrava
        // sempre "silencio 0s" — o numero mais inutil possivel num log de ritmo.
        int silence = t.quiet(Beat.NOISE) / 20;

        RECNetwork.toPlayer(player, new DreadPacket(RANDOM.nextInt(DreadPacket.POOL_SIZE), offset));
        report(player, Beat.NOISE);

        // Uma linha por ruido (~1 por minuto). E a prova de vida do Diretor: sem ela,
        // "nao aconteceu nada" e indistinguivel de "o tique nao esta rodando".
        LOG.info("[DIRETOR] ruido {} | pressao {} | silencio {}s",
                tell != null ? "DENUNCIA (" + tell.getType().getDescriptionId() + ")" : "mentira",
                String.format("%.2f", t.pressure()),
                t.quiet(Beat.NOISE) / 20);
    }

    /** A criatura nossa mais perto, dentro do raio. Null se nao ha nenhuma. */
    private static Mob nearestOurs(ServerPlayer player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        Mob best = null;
        double bestSqr = range * range;

        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, box, Director::isOurs)) {
            double sqr = mob.distanceToSqr(player);
            if (sqr < bestSqr) {
                bestSqr = sqr;
                best = mob;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ estado

    private static Tension of(ServerPlayer player) {
        return STATE.computeIfAbsent(player.getUUID(), id -> new Tension());
    }

    /**
     * ⚠️ Sem isto o mapa cresce para sempre num servidor com gente entrando e saindo.
     * A tensao morre com a sessao de proposito: voltar ao mundo depois de sair e comecar
     * de novo, e comecar de novo tem que ser calmo.
     */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATE.remove(event.getEntity().getUUID());
        MUSIC_ON.remove(event.getEntity().getUUID());
        PlacementTrace.clear(event.getEntity().getUUID());
        Staging.clear(event.getEntity().getUUID());
    }
}
