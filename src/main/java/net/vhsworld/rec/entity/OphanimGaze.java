package net.vhsworld.rec.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.init.ModEntities;
import net.vhsworld.rec.item.ModSounds;
import net.vhsworld.rec.net.JudgementPacket;
import net.vhsworld.rec.net.RECNetwork;
import net.vhsworld.rec.worldgen.dim.ChunksChunkGenerator;
import net.vhsworld.rec.worldgen.dim.OphanimDirector;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

/**
 * O OLHAR RECIPROCO — a regra inteira do Ofanim, e ela e uma so:
 *
 *                    ELE SO SE MEXE ENQUANTO VOCE ESTA OLHANDO.
 *
 * E o Homem de Pedra ao contrario, e de proposito. O de pedra congela sob o olhar: a
 * jogada dele e olhar. Este anda sob o olhar: a jogada dele e BAIXAR OS OLHOS. Duas
 * criaturas com a mesma gramatica (o jogo le para onde voce aponta a cara) e solucoes
 * opostas — e por isso a segunda nao parece a primeira repintada.
 *
 * POR QUE ISSO E O VERBO DA FUGA CERTO PARA A CHUNKS: naquela dimensao o chao acaba a
 * cada dezesseis blocos e a ponte tem oito de largura. Atravessar de cabeca baixa,
 * sabendo que a coisa esta se aproximando e nao podendo conferir, custa o que o pilar
 * do mod pede que custe — TEMPO — sem tirar o controle do jogador em momento nenhum.
 * Ele pode olhar quando quiser. So vai pagar.
 *
 * O MEDIDOR SOBE RAPIDO E DESCE DEVAGAR. Desviar os olhos por um instante nao desfaz o
 * que voce ja deu a ele; e um alivio parcial, e o jogador sente que esta devendo. Se
 * subisse e descesse na mesma velocidade, a jogada otima seria piscar em ritmo e a
 * criatura viraria um jogo de compasso.
 *
 * ⚠️ TUDO AQUI E SERVIDOR. O servidor sabe para onde cada jogador olha (a rotacao
 * chega todo tick pelo movimento vanilla), entao o laco fecha sem pacote nenhum — o
 * canal so entra no fim, para a TELA de quem foi julgado, e para o flash poder falar.
 */
public final class OphanimGaze {

    private OphanimGaze() {}

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Quao no centro da tela ele precisa estar para contar como "olhado" (cosseno).
     *
     * 0.88 sao ~28 graus para cada lado. Mais fechado que isso e o jogador poderia ter
     * a coisa cheia na tela sem que o jogo considerasse que ele a esta vendo, o que le
     * como injustica; mais aberto e andar de lado ja seria olhar, e nao haveria fuga.
     */
    private static final double LOOK_CONE = 0.88D;

    /** Nome do estagio do julgamento no NBT persistente do jogador. */
    private static final String STAGE_TAG = "recmod:ophanim_stage";

    /** Trava do flash no lado do servidor, em ticks. O cliente nao manda no ritmo. */
    private static final int FLASH_REST = 20;

    /**
     * Ate onde o julgamento procura a coluna em que vai largar o jogador.
     *
     * 192 blocos: longe o bastante para ele nao reconhecer onde caiu (o castigo e
     * perder o rumo, nao andar um pouco), e perto o bastante para a coluna ja estar
     * carregada — largar alguem sobre chunk nao gerado seria uma queda no vazio
     * enquanto o mundo tenta alcanca-lo.
     */
    private static final int CARRY_RANGE = 192;

    // ------------------------------------------------------------------ o laco

    /** Chamado todo tick pela anomalia, so no servidor e so quando ela e o Ofanim. */
    public static void tick(AnomalyEntity self) {
        if (!RECConfig.COMMON.ophanimGaze.get()) return;
        if (!(self.level() instanceof ServerLevel level)) return;

        float rise = 1.0F / Math.max(1, (int) Math.round(
                RECConfig.COMMON.ophanimGazeSeconds.get() * 20.0D));

        // QUEIMADO PELO FLASH: ele nao ganha medidor e nao anda. E a unica janela em
        // que o jogador pode olhar de graca — e ela e curta.
        if (self.burnTicks() > 0) {
            self.setBurnTicks(self.burnTicks() - 1);
            self.setGaze(Math.max(0.0F, self.gaze() - rise));
            return;
        }

        ServerPlayer looker = looker(self);
        self.setWatched(looker != null);

        if (looker == null) {
            float fall = rise * RECConfig.COMMON.ophanimReleaseFactor.get().floatValue();
            self.setGaze(Math.max(0.0F, self.gaze() - fall));

            // O BANDO ANDA JUNTO. Olhar para UM faz os tres virem — este aqui nao
            // esta sendo olhado, mas um irmao esta, e por isso ele avanca do mesmo
            // jeito. Sem esta regra o trio seria so tres criaturas independentes no
            // mesmo ceu, e a saida seria escolher uma para encarar enquanto se anda
            // de lado: baratissimo. Com ela, a unica saida continua sendo a mesma de
            // sempre — nao olhar para NENHUM — e agora ela custa de verdade, porque
            // eles estao em tres direcoes e voce precisa enxergar para andar.
            //
            // Ele avanca mas NAO enche o medidor: o preco continua sendo cobrado por
            // quem voce olhou, e nao por quem voce evitou.
            if (self.isSwarm()) {
                ServerPlayer prey = swarmPrey(self);
                if (prey != null) {
                    approach(self, prey);
                    if (horizontalDistance(self, prey)
                            <= RECConfig.COMMON.ophanimContactRange.get()) {
                        judge(level, self, prey);
                    }
                }
            }
            return;
        }

        self.setGaze(Math.min(1.0F, self.gaze() + rise));
        approach(self, looker);

        // DEIXOU CHEGAR. Sem isto, quem corre para debaixo dele escaparia do
        // julgamento por causa do proprio avanco — e "deixar a coisa encostar" e
        // justamente o pior jeito de lidar com ela.
        double contact = RECConfig.COMMON.ophanimContactRange.get();
        boolean touched = horizontalDistance(self, looker) <= contact;

        if (touched || self.gaze() >= 1.0F) {
            judge(level, self, looker);
        }
    }

    /**
     * O jogador mais perto que esta com ele no campo de visao, com linha limpa.
     *
     * Um so, e nao "todos que olham": o medidor e da criatura, nao do jogador. Em
     * multiplayer isso significa que dois olhando enchem o mesmo medidor, e o
     * julgamento cai em quem estava mais perto. E o comportamento certo — a coisa e
     * uma so e vai atras de uma pessoa por vez.
     */
    @Nullable
    private static ServerPlayer looker(AnomalyEntity self) {
        double range = RECConfig.COMMON.ophanimGazeRange.get();
        double rangeSq = range * range;

        ServerPlayer best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Player player : self.level().players()) {
            if (!(player instanceof ServerPlayer server)) continue;
            // Espectador conta como presenca em outros lugares do mod, mas nao aqui:
            // ele nao pode ser cegado nem levado, entao encher o medidor com o olhar
            // dele so serviria para gastar o julgamento em quem nao pode sofre-lo.
            if (server.isSpectator() || !server.isAlive()) continue;

            double distanceSq = self.distanceToSqr(server);
            if (distanceSq > rangeSq || distanceSq >= bestDistance) continue;
            if (!looksAt(server, self)) continue;

            best = server;
            bestDistance = distanceSq;
        }
        return best;
    }

    /**
     * A presa do BANDO: quem esta sendo olhado por um IRMAO deste aqui.
     *
     * ⚠️ Le a bandeira do tick ANTERIOR (o `watched` que cada um grava em si mesmo no
     * proprio tick). Ha um tick de atraso entre um irmao ser olhado e os outros
     * reagirem, e ele e proposital — sincronizar isso de verdade exigiria uma passada
     * a mais por todo o bando antes de qualquer um andar, para ganhar um vigesimo de
     * segundo que ninguem enxerga.
     *
     * Devolve o jogador mais perto DELE, e nao o que o irmao estava olhando: o que o
     * bando faz e fechar o cerco, e cada um fecha pelo lado em que esta.
     */
    @Nullable
    private static ServerPlayer swarmPrey(AnomalyEntity self) {
        boolean anyWatched = false;
        for (AnomalyEntity other : self.level().getEntitiesOfClass(AnomalyEntity.class,
                self.getBoundingBox().inflate(RECConfig.COMMON.ophanimGazeRange.get()))) {
            if (other != self && other.isSwarm() && other.isAlive() && other.wasWatched()) {
                anyWatched = true;
                break;
            }
        }
        if (!anyWatched) return null;

        ServerPlayer best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player player : self.level().players()) {
            if (!(player instanceof ServerPlayer server)) continue;
            if (server.isSpectator() || !server.isAlive()) continue;

            double distanceSq = self.distanceToSqr(server);
            if (distanceSq < bestDistance) {
                best = server;
                bestDistance = distanceSq;
            }
        }
        return best;
    }

    /**
     * Este jogador esta olhando para ele AGORA?
     *
     * ⚠️ A mira e o MEIO DO DESENHO, e nao a caixa de colisao. A caixa do Ofanim tem
     * 2.6 blocos e o cartaz tem treze: mirando na caixa, olhar para o corpo dele —
     * que e o que qualquer pessoa faz — nao contaria como olhar, e a criatura ficaria
     * parada enquanto o jogador a encara.
     */
    private static boolean looksAt(ServerPlayer player, AnomalyEntity self) {
        Vec3 eye = player.getEyePosition();
        Vec3 middle = self.position().add(0.0D, self.type().height() * 0.5D, 0.0D);

        Vec3 toIt = middle.subtract(eye);
        double length = toIt.length();
        if (length < 1.0E-4D) return true;

        if (player.getViewVector(1.0F).normalize().dot(toIt.scale(1.0D / length)) < LOOK_CONE) {
            return false;
        }
        // Parede corta o olhar. Na CHUNKS quase nunca ha parede — mas ha COLUNA, e
        // uma coluna de dezesseis blocos entre voce e ele e exatamente o esconderijo
        // que esta dimensao oferece.
        return player.level().clip(new ClipContext(eye, middle, ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }

    /**
     * Ele desliza na sua direcao — so enquanto esta sendo olhado.
     *
     * Anda no ar e nao no chao: nao ha caminho para calcular, e nao deveria haver. Ele
     * atravessa o vazio em linha reta porque o vazio nao e obstaculo para ele, e e
     * justamente isso que a CHUNKS precisa que o jogador entenda — a coluna do outro
     * lado nao e um lugar seguro, e so um lugar longe.
     *
     * A altura mira ACIMA dos olhos do jogador. Chegando por cima, quanto mais perto,
     * mais ele obriga a levantar a cabeca; e levantar a cabeca, aqui, e continuar
     * olhando. A propria aproximacao cobra mais medidor.
     */
    private static void approach(AnomalyEntity self, ServerPlayer prey) {
        double speed = RECConfig.COMMON.ophanimApproach.get();
        if (speed <= 0.0D) return;

        double above = RECConfig.COMMON.ophanimHoverAbove.get();
        Vec3 target = new Vec3(prey.getX(), prey.getEyeY() + above, prey.getZ());
        Vec3 step = target.subtract(self.position());

        double length = step.length();
        if (length < 1.0E-4D) return;
        if (length > speed) step = step.scale(speed / length);

        Vec3 next = self.position().add(step);
        self.setPos(next.x, next.y, next.z);
    }

    private static double horizontalDistance(AnomalyEntity self, Player player) {
        double dx = self.getX() - player.getX();
        double dz = self.getZ() - player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    // ------------------------------------------------------------------ o julgamento

    /**
     * O preco. Escala a cada vez, e some junto com ele.
     *
     * Ele SE DESFAZ depois de julgar, e o Diretor o recoloca longe uns segundos
     * depois. Isso e o piso de silencio: sem ele, a criatura ficaria em cima do
     * jogador e o segundo julgamento viria antes de o primeiro ter sido entendido.
     */
    private static void judge(ServerLevel level, AnomalyEntity self, ServerPlayer prey) {
        Judgement stage = nextStage(prey);

        // ⚠️ "Ele te leva" SO na CHUNKS. Fora dela nao ha planta de colunas para onde
        // levar ninguem, e a alternativa (largar o jogador num ponto qualquer do mapa
        // de alturas) tem dois defeitos serios: pode deixa-lo dentro de lava — e o mod
        // nao mata por atmosfera — e obriga o servidor a gerar o chunk de destino na
        // hora, travando o tique. Sem coluna, o castigo para na cegueira.
        if (stage == Judgement.TAKEN && !isChunks(level)) stage = Judgement.BLINDNESS;

        float sanity = RECConfig.COMMON.ophanimSanityCost.get().floatValue()
                * (1.0F + stage.ordinal() * 0.5F);

        int ticks;
        switch (stage) {
            case VERTIGO -> {
                ticks = seconds(RECConfig.COMMON.ophanimVertigoSeconds.get());
            }
            case BLINDNESS -> {
                ticks = seconds(RECConfig.COMMON.ophanimBlindSeconds.get());
                // Cegueira e efeito vanilla: chega no cliente sozinha, sem pacote.
                prey.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0, false, false));
            }
            default -> {
                // Ele te leva: um piscar de cegueira cobre o teleporte, senao o mundo
                // troca num corte seco e le como bug em vez de como sequestro.
                ticks = 40;
                prey.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0, false, false));
                carryAway(level, prey);
            }
        }

        // O som sai do MUNDO e nao do ouvido: quem estiver junto ouve o que aconteceu
        // com o outro, e em multiplayer isso e metade da graca.
        level.playSound(null, prey.getX(), prey.getY(), prey.getZ(),
                ModSounds.SIGHT_OPHANIM.get(), SoundSource.HOSTILE, 1.4F, 0.85F);

        RECNetwork.toPlayer(prey, new JudgementPacket(stage, ticks, sanity));

        LOGGER.info("[ofanim] julgamento {} em {} ({} ticks)",
                stage, prey.getGameProfile().getName(), ticks);

        self.setGaze(0.0F);
        OphanimDirector.judged(level);
        self.discard();
    }

    /** O estagio desta vez, e ja deixa o proximo marcado no jogador. */
    private static Judgement nextStage(ServerPlayer prey) {
        if (!RECConfig.COMMON.ophanimEscalates.get()) return Judgement.VERTIGO;

        var data = prey.getPersistentData();
        int index = data.getInt(STAGE_TAG);
        data.putInt(STAGE_TAG, index + 1);
        return Judgement.byIndex(index);
    }

    /**
     * Larga o jogador em cima de outra coluna.
     *
     * A lista vem da propria planta da dimensao, e nao de uma busca no mundo: entre as
     * colunas o mapa de alturas devolve o fundo do vazio, e procurar chao por ali
     * largaria o jogador caindo. Perguntando a planta, ele cai sempre sobre grama de
     * verdade — inclusive numa coluna sem ponte nenhuma, que e o resultado que da medo.
     *
     * Se por qualquer motivo nao houver planta, ele nao e levado a lugar nenhum. Ficar
     * onde estava e um castigo fraco; ser jogado num lugar calculado errado, nesta
     * dimensao, seria uma queda de duzentos blocos.
     */
    private static void carryAway(ServerLevel level, ServerPlayer prey) {
        if (!(level.getChunkSource().getGenerator() instanceof ChunksChunkGenerator chunks)) return;

        // Um alcance, e nao o mapa inteiro: a dimensao e infinita e a lista de "todas
        // as colunas" nao existe. CARRY_RANGE e generoso de proposito — perto demais e
        // o castigo vira um empurrao, e longe demais e uma tela de carregamento.
        List<BlockPos> columns = chunks.layout().columnsAround(prey.blockPosition(), CARRY_RANGE);
        if (columns.isEmpty()) return;

        for (int attempt = 0; attempt < 24; attempt++) {
            BlockPos column = columns.get(level.getRandom().nextInt(columns.size()));
            // Nao vale largar o jogador onde ele ja estava: acordar no mesmo lugar
            // faria o castigo parecer que falhou.
            if (column.distSqr(prey.blockPosition()) < 48.0D * 48.0D) continue;
            prey.teleportTo(column.getX() + 0.5D, column.getY() + 1.0D, column.getZ() + 0.5D);
            return;
        }
    }

    private static int seconds(double value) {
        return Math.max(1, (int) Math.round(value * 20.0D));
    }

    // ------------------------------------------------------------------ o flash

    /**
     * O CLARAO QUEIMA O OLHO DELE. Vindo do cliente pelo FlashPacket.
     *
     * E a unica coisa no mod que EMPURRA uma anomalia, e ela existe porque a camera
     * precisava poder agir, e nao so olhar. O preco ja esta pago em outro lugar: o
     * flash custa a carga do botao e come bateria, e bateria e recurso finito que se
     * troca por pilha. Nao ha ganho de graca aqui.
     *
     * Ele nao morre nem foge para sempre — recua, fica cego uns segundos e volta. A
     * fronteira continua de pe: nao se mata, se ganha tempo.
     */
    public static void flash(ServerPlayer player) {
        if (!RECConfig.COMMON.ophanimFlashBurn.get()) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        // Trava do servidor: o cliente pode mandar o quanto quiser, so um por segundo
        // e ouvido.
        long now = level.getGameTime();
        var data = player.getPersistentData();
        if (now - data.getLong("recmod:flash_at") < FLASH_REST) return;
        data.putLong("recmod:flash_at", now);

        double range = RECConfig.COMMON.ophanimFlashRange.get();
        AnomalyEntity target = null;
        double bestDistance = range * range;

        for (AnomalyEntity anomaly : level.getEntities(ModEntities.ANOMALY.get(),
                a -> a.type() == AnomalyType.OPHANIM && a.isAlive())) {
            double distanceSq = anomaly.distanceToSqr(player);
            if (distanceSq > bestDistance) continue;
            // Tem que estar ENQUADRADO. O flash aponta para onde a camera aponta; um
            // clarao de costas para ele nao poderia queima-lo, e permitir isso faria
            // do flash um botao de espantar em vez de uma mira.
            if (!looksAt(player, anomaly)) continue;
            target = anomaly;
            bestDistance = distanceSq;
        }
        if (target == null) return;

        target.setGaze(0.0F);
        target.setBurnTicks(seconds(RECConfig.COMMON.ophanimFlashBlindSeconds.get()));

        // Recua na linha do olhar, mantendo a altura: empurrado para os lados ele
        // apareceria de esguelha, e o que se quer e ve-lo ficar PEQUENO.
        double push = RECConfig.COMMON.ophanimFlashPushback.get();
        Vec3 away = target.position().subtract(player.position());
        double length = Math.sqrt(away.x * away.x + away.z * away.z);
        if (length > 1.0E-4D) {
            target.setPos(target.getX() + away.x / length * push,
                    target.getY(),
                    target.getZ() + away.z / length * push);
        }

        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                ModSounds.SIGHT_OPHANIM.get(), SoundSource.HOSTILE, 1.2F, 1.35F);
    }

    /** So para o Diretor conferir de que dimensao se trata. */
    public static boolean isChunks(Level level) {
        return net.vhsworld.rec.worldgen.dim.DimensionProfile.isDirectedBy(
                level, net.vhsworld.rec.worldgen.dim.DimensionProfile.Director.OPHANIM);
    }
}
