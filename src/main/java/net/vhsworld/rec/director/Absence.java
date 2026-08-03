package net.vhsworld.rec.director;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.net.AbsencePacket;
import net.vhsworld.rec.net.RECNetwork;

import java.util.ArrayList;
import java.util.List;

/**
 * A AUSENCIA: o mundo mexe numa coisa que o jogador poe, pelas costas dele.
 *
 * A tocha que ele fincou nao esta mais la. A porta que ele fechou esta aberta. Nao ha
 * criatura, nao ha vulto, nao ha nada para apontar — e e exatamente por isso que ela e a
 * coisa mais assustadora que este mod pode fazer por duzentas linhas. Terror psicologico
 * nao e medo do monstro: e o jogador duvidando da propria memoria, sozinho, sem nada que
 * prove que ele esta certo.
 *
 * ⚠️ AS TRES TRAVAS QUE FAZEM ISTO SER MEDO E NAO SACANAGEM:
 *
 *   E SEU. So mexe no que ESTE jogador colocou (ver PlacementTrace). Tocha anonima do
 *   mapa nao produz nada, porque ninguem sabia que ela existia.
 *
 *   E PELAS COSTAS. Nunca no campo de visao e nunca colado. O que se ve acontecer e um
 *   bug; o que se descobre ter acontecido e uma assombracao.
 *
 *   E BARATO DE DESFAZER. Tocha e porta, mais nada. No dia em que isto mexer num bau, o
 *   jogador para de ter medo e passa a ter raiva — e raiva desinstala mod.
 *
 * ⚠️ A QUARTA TRAVA, PAGA COM UM TESTE PERDIDO (2026-08-02). A mecanica funcionou de ponta
 * a ponta — tocha removida a 22,5 blocos, marca no visor — e O JOGADOR NAO NOTOU NADA. O
 * defeito nao era tecnico: <b>a ausencia mudava o ESTADO do mundo sem produzir um
 * ACONTECIMENTO</b>. Ela dependia de o jogador lembrar sozinho de como o mundo estava, e
 * jogador de Minecraft nao inventaria bloco. O resultado padrao da mecanica era "nada
 * aconteceu" — um modo de falha silencioso, e o desfecho mais provavel.
 *
 * As duas correcoes, e elas andam juntas:
 *
 *   TEM QUE ACONTECER (ver {@link #hiss}). A tocha se apaga COM O SOM DE APAGAR, pelas
 *   costas. Voce ouve, vira, e nao ha nada — mas agora voce ESTA OLHANDO. E a diferenca
 *   entre um estado que mudou e uma coisa que aconteceu.
 *
 *   TEM QUE SER UMA COISA (ver {@link #pick}). Quarenta e duas tochas fincadas em sequencia
 *   nao sao objetos, sao confete: sem identidade nao ha o que sentir falta. Agora a
 *   ausencia escolhe a marca SOZINHA e VELHA, nunca uma do meio da fileira.
 */
public final class Absence {

    private Absence() {}

    /** O que aconteceu. Vai no fio para o visor saber o que desenhar. */
    public enum Kind {
        /** A tocha sumiu. */
        TORCH_TAKEN,
        /** A porta mudou de estado sozinha. */
        DOOR_MOVED
    }

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    /** Freio do aviso de "nao achou alvo". Ver o comentario no fim do tryApply. */
    private static long lastMissLog = 0L;

    private static final double MIN_DISTANCE = 8.0D;

    /** O alvo escolhido, ja com tudo que o resto precisa saber sobre ele. */
    private record Target(PlacementTrace.Mark mark, BlockState state, double distance,
                          double solitude, long age) {}

    /**
     * Tenta fazer uma ausencia acontecer com este jogador. Devolve true se aconteceu.
     *
     * ⚠️ Quem decide QUANDO nao e este arquivo — e o Diretor. Aqui so se responde "deu
     * para fazer alguma coisa agora?".
     */
    static boolean tryApply(ServerPlayer player) {
        if (!RECConfig.COMMON.absence.get()) return false;
        if (!(player.level() instanceof ServerLevel level)) return false;

        List<PlacementTrace.Mark> candidates = new ArrayList<>(PlacementTrace.of(player));
        Target target = pick(level, player, candidates);

        if (target != null) {
            BlockPos pos = target.mark().pos();
            if (apply(level, player, pos, target.state(), target.distance())) {
                PlacementTrace.forget(player, target.mark());
                LOG.info("[DIRETOR] AUSENCIA em {} ({}), a {} blocos de {} | sozinha ha {} blocos, fincada ha {}s",
                        pos, target.state().getBlock().getName().getString(),
                        String.format("%.1f", target.distance()), player.getGameProfile().getName(),
                        String.format("%.0f", target.solitude()), target.age() / 20);
                return true;
            }
        }

        // ⚠️ ESTE RAMO E O MAIS IMPORTANTE DO REGISTRO. Se a ausencia nunca acontecer no
        // jogo, e ele que separa as causas possiveis: o Diretor nem quis (nao sai linha
        // nenhuma), quis e o rastro estava vazio (rastro=0), ou quis e nada servia — longe
        // demais, perto demais, no campo de visao, ja quebrado, novo demais, no meio da
        // fileira (rastro>0). Sem isto, "nao aconteceu nada" seria indistinguivel de "esta
        // quebrado".
        //
        // ⚠️ COM FREIO. Quando nao ha alvo, a batida nunca e reportada, entao o relogio
        // dela nao zera e ela volta a querer a cada poucos segundos — sem o freio, este
        // aviso sozinho enche o log e esconde o que importa.
        long now = System.currentTimeMillis();
        if (now - lastMissLog > 60_000L) {
            lastMissLog = now;
            LOG.info("[DIRETOR] ausencia sem alvo | rastro={} perto(<{})={} longe(>{})={} visao={} mudou={} novo={} fileira={} outro={}",
                    candidates.size(), (int) MIN_DISTANCE, perto,
                    RECConfig.COMMON.absenceHearingRange.get().intValue(), longe,
                    visao, mudou, novo, fileira, outro);
        }
        return false;
    }

    /**
     * ⚠️ Contadores por MOTIVO. A primeira versao registrava so "nao achou alvo
     * (rastro=42)", e no primeiro teste em que o rastro nao estava vazio esse aviso foi
     * inutil exatamente como o silencio que ele substituiu: 42 marcas, nenhuma pista de
     * por que as 42 falharam. Aviso de diagnostico que nao separa causas nao e
     * diagnostico, e um "nao sei" mais comprido.
     */
    private static int outro, longe, perto, visao, mudou, novo, fileira;

    /**
     * QUAL COISA. A pergunta que o teste perdido de 2026-08-02 mostrou que ninguem estava
     * fazendo.
     *
     * Antes isto era um `Collections.shuffle` e o primeiro que servisse. Serve para achar
     * um bloco valido; nao serve para achar um bloco que o jogador VAI SENTIR FALTA — e o
     * segundo e a mecanica inteira. As duas coisas que dao identidade a uma tocha, e nesta
     * ordem de importancia:
     *
     *   SOLIDAO. A tocha do corredor e uma tocha; a decima quarta de uma fileira e textura
     *   de parede. E a solidao tambem e o unico jeito honesto de a falta CARREGAR PESO: se
     *   ela era a unica luz daquele pedaco, a ausencia dela muda o que voce consegue fazer
     *   ali, nao so o que voce ve. Tirar a decima quarta nao apaga nada.
     *
     *   IDADE. Marca velha ja foi passada, revisitada, incorporada ao mapa mental. Marca de
     *   trinta segundos atras ainda esta na mao do jogador, e sumir com ela nao produz
     *   duvida: produz a conclusao imediata e correta de que o mod fez.
     *
     * ⚠️ POR QUE NAO A CAMA (nem o bau, nem a fornalha), que "carregariam peso" de verdade:
     * a terceira trava veta. Perda de progresso vira raiva, e raiva nao e medo. O peso tem
     * que sair da ESCOLHA do alvo, nunca do valor dele — e por isso a lista de candidatos em
     * PlacementTrace.isCandidate continua curta e chata.
     */
    private static Target pick(ServerLevel level, ServerPlayer player, List<PlacementTrace.Mark> candidates) {
        double maxDistance = RECConfig.COMMON.absenceHearingRange.get();
        double loneRadius = RECConfig.COMMON.absenceLoneRadius.get();
        long minAge = RECConfig.COMMON.absenceMinAgeSeconds.get() * 20L;
        long gameTime = level.getGameTime();

        outro = longe = perto = visao = mudou = novo = fileira = 0;

        Target best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (PlacementTrace.Mark mark : candidates) {
            if (!mark.dimension().equals(level.dimension())) { outro++; continue; }

            BlockPos pos = mark.pos();
            if (!level.isLoaded(pos)) { outro++; continue; }

            double distance = Math.sqrt(pos.distToCenterSqr(player.position()));
            if (distance < MIN_DISTANCE) { perto++; continue; }

            // ⚠️ O TETO ENCOLHEU DE 64 PARA O ALCANCE DA ORELHA, e isso e consequencia
            // direta do gancho sensorial: uma ausencia que o jogador nao pode OUVIR
            // acontecer volta a ser a versao que falhou no teste. Melhor nao acontecer do
            // que acontecer no vazio — batida gasta que nao vira nada e pior que espera.
            if (distance > maxDistance) { longe++; continue; }

            if (inSight(player, pos)) { visao++; continue; }

            long age = gameTime - mark.placedAt();
            if (age < minAge) { novo++; continue; }

            BlockState now = level.getBlockState(pos);

            // O jogador pode ter quebrado, trocado ou movido a coisa. Se o mundo nao bate
            // mais com a anotacao, a anotacao e lixo — some com ela e tenta a proxima.
            if (!matches(mark.state(), now)) {
                PlacementTrace.forget(player, mark);
                mudou++;
                continue;
            }

            double solitude = solitude(candidates, mark);
            if (solitude < loneRadius) { fileira++; continue; }

            // A solidao manda; a idade so desempata. Os tetos existem para uma tocha
            // esquecida no fim do mundo ha uma hora nao ganhar de uma tocha sozinha aqui.
            double score = Math.min(solitude, 24.0D) + Math.min(age / 1200.0D, 6.0D);

            if (score > bestScore) {
                bestScore = score;
                best = new Target(mark, now, distance, solitude, age);
            }
        }

        return best;
    }

    /** A quantos blocos esta a coisa mais proxima que o MESMO jogador colocou. */
    private static double solitude(List<PlacementTrace.Mark> all, PlacementTrace.Mark mark) {
        double nearestSqr = Double.MAX_VALUE;
        for (PlacementTrace.Mark other : all) {
            if (other == mark) continue;
            if (!other.dimension().equals(mark.dimension())) continue;
            double sqr = other.pos().distSqr(mark.pos());
            if (sqr < nearestSqr) nearestSqr = sqr;
        }
        return nearestSqr == Double.MAX_VALUE ? Double.MAX_VALUE : Math.sqrt(nearestSqr);
    }

    /** Faz a coisa e avisa o cliente, para o visor poder mostrar o lugar depois. */
    private static boolean apply(ServerLevel level, ServerPlayer player, BlockPos pos,
                                 BlockState state, double distance) {
        if (state.getBlock() instanceof DoorBlock door) {
            boolean open = state.getValue(DoorBlock.OPEN);
            // O rangido toca no lugar da porta, e isso e proposital: o jogador ouve algo
            // acontecer em algum lugar da base e nao sabe onde. A porta sempre teve o
            // gancho sensorial que faltava a tocha — ela e a unica ausencia que se
            // anuncia, e ela se anuncia mal, que e o jeito certo.
            door.setOpen(null, level, state, pos, !open);
            RECNetwork.toPlayer(player, new AbsencePacket(pos, Kind.DOOR_MOVED));
            return true;
        }

        if (isTorch(state)) {
            // Sem drop e sem particula: ela simplesmente nao esta mais ali.
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            hiss(level, player, pos, distance);
            RECNetwork.toPlayer(player, new AbsencePacket(pos, Kind.TORCH_TAKEN));
            return true;
        }

        return false;
    }

    /**
     * O GANCHO SENSORIAL: a chama morrendo, pelas suas costas.
     *
     * ⚠️ ISTO CONTRADIZ O COMENTARIO QUE ESTAVA AQUI ("qualquer efeito seria uma prova, e a
     * mecanica inteira depende de nao haver prova"). A frase estava certa e a conclusao
     * estava errada, e o teste de 2026-08-02 cobrou a diferenca: <b>som nao e prova.</b>
     * Particula fica, item dropado fica, marca no chao fica — todos podem ser apontados
     * depois, inclusive para outra pessoa. O som ja acabou quando voce vira a cabeca. Ele
     * nao deixa nada alem da sua palavra, que e exatamente o material desta mecanica.
     *
     * E ele NAO E UM AVISO, pela mesma razao que o visor nao e um detector: aviso aponta
     * para o mod e entrega uma tarefa. Uma chama se apagando e o som que aquele objeto faz
     * ao morrer — pode ter sido goteira, pode ter sido nada, pode ter sido voce ouvindo
     * coisa. So que ele vem de um lugar onde agora nao ha mais nada, e voce ja esta olhando
     * para la. E o unico jeito de a ausencia deixar de depender de o jogador lembrar
     * espontaneamente de como o mundo estava.
     *
     * ⚠️ SO PARA ESTE JOGADOR (pacote direto, nao level.playSound). O Diretor modela a
     * EXPERIENCIA de um jogador, e a fita e dele: quem estiver junto nao ouve, e ter que
     * perguntar "voce ouviu isso?" e receber "nao" e melhor do que qualquer efeito que eu
     * saiba escrever.
     *
     * ⚠️ O VOLUME SOBE COM A DISTANCIA porque no Minecraft volume E alcance (o raio audivel
     * e ~16 x volume). Sem esta conta, tudo acima de 16 blocos sumiria em silencio e o
     * gancho existiria so no codigo — que foi, palavra por palavra, o defeito original.
     */
    private static void hiss(ServerLevel level, ServerPlayer player, BlockPos pos, double distance) {
        if (!RECConfig.COMMON.absenceSound.get()) return;

        float volume = (float) Mth.clamp(distance / 10.0D, 0.8D, 3.5D);
        float pitch = 0.9f + level.getRandom().nextFloat() * 0.2f;

        player.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.FIRE_EXTINGUISH),
                SoundSource.BLOCKS,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                volume, pitch, level.getRandom().nextLong()));
    }

    /**
     * O jogador esta olhando para esse lado?
     *
     * Grosseiro de proposito — cone largo, sem raycast. Errar para o lado do "ele PODE
     * estar vendo" e barato (a ausencia so espera a proxima chance); errar para o outro
     * lado significa sumir com uma tocha na cara do jogador, que estraga o efeito de vez.
     */
    private static boolean inSight(ServerPlayer player, BlockPos pos) {
        Vec3 toBlock = Vec3.atCenterOf(pos).subtract(player.getEyePosition()).normalize();
        return player.getLookAngle().normalize().dot(toBlock) > 0.2D;
    }

    private static boolean matches(BlockState placed, BlockState now) {
        if (isTorch(placed)) return isTorch(now);
        return placed.getBlock() == now.getBlock();
    }

    private static boolean isTorch(BlockState state) {
        return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH);
    }
}
