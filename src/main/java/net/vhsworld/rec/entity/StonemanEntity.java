package net.vhsworld.rec.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.vhsworld.rec.config.RECConfig;

/**
 * O HOMEM DE PEDRA.
 *
 * Ele so anda quando ninguem esta olhando. Sob o olhar de um jogador vira pedra —
 * nao devagar, nao "quase": para por completo, no mesmo lugar, encarando. Desviar
 * os olhos por um segundo e o bastante para ele estar mais perto quando voltarem.
 *
 * DECISAO DE DESENHO: quem prende e o OLHAR, nao a distancia nem a luz. E por isso
 * que ele conversa com o resto do mod — no VHSWORLD a camera e o estado do mundo, e
 * toda vez que o jogador abre o album ou o registro para se orientar, ele deixou de
 * olhar para o mundo. A ferramenta que te salva e a mesma que te entrega.
 *
 * A REGRA NAO E CONFIAVEL (instabilidade de visao). De tempos em tempos — e sempre
 * que alguem o encara sem piscar por tempo demais — ele tem um SURTO de poucos
 * segundos em que anda com voce olhando bem para ele. E o que separa este bicho de um
 * quebra-cabeca: uma regra que sempre vale vira tecnica, o jogador aprende a andar de
 * costas e nunca mais sente nada. Uma regra que quase sempre vale deixa o jogador
 * inseguro com a propria estrategia, que e onde o medo mora. O surto e curto e
 * anunciado (pedra rachando + tela) de proposito: susto, nao roubo.
 *
 * TUDO AQUI E SERVIDOR. O "estao me olhando?" e calculado no servidor e mandado ao
 * cliente por SynchedEntityData; o modelo so le a bandeira para decidir se anima. Um
 * calculo desses no cliente daria, em multiplayer, um bicho parado para um jogador e
 * andando para outro. E os numeros vem do config COMMON, nunca do CLIENT — item ou
 * entidade que le config de cliente no ramo do servidor derruba servidor dedicado.
 */
public class StonemanEntity extends Monster {

    /**
     * ATENCAO: esta bandeira diz "esta CONGELADO agora", nao "tem alguem olhando".
     * Durante o surto ele esta sendo olhado e mesmo assim anda — e o modelo precisa
     * animar. O olhar cru nao sai daqui: vive no computeWatched(), no servidor.
     */
    private static final EntityDataAccessor<Boolean> DATA_WATCHED =
            SynchedEntityData.defineId(StonemanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SURGING =
            SynchedEntityData.defineId(StonemanEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * Cosseno do meio-angulo do cone de visao que conta como "olhando".
     *
     * 0.55 da ~114 graus no total, um pouco mais estreito que a tela do jogador. Mais
     * largo e ele congelaria de canto de olho, sem o jogador saber por que; mais
     * estreito e o jogador olharia bem para ele e o bicho andaria assim mesmo.
     */
    private static final double LOOK_CONE = 0.55D;

    /** Quantos ticks ele fica sem poder emboscar de novo (1s). Senao vira metralhadora. */
    private static final int AMBUSH_REST = 20;

    // Estado do surto. Tudo SERVIDOR e transiente: ao carregar o mundo o relogio
    // rearma sozinho, e ninguem volta ao jogo com um surto pendente da sessao passada.
    private int surgeTicks;         // quanto falta do surto atual (0 = sem surto)
    private int surgeCooldown = -1; // -1 = relogio ainda nem armado (nunca teve plateia)
    private int stareTicks;         // ha quantos ticks seguidos alguem o encara
    private int ambushRest;         // trava da emboscada
    private int stuckTicks;         // ha quantos ticks ele quer chegar em voce e nao sai do lugar
    private Vec3 lastSpot = Vec3.ZERO;

    public StonemanEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0D)
                // MUITO rapido de proposito (v1.38.0: 0.32 -> 0.62, quase o dobro).
                // Ele so anda sem plateia, entao a velocidade nao e dificuldade
                // constante — e o tamanho do buraco que o teu piscar abre. Um bicho
                // lento que congela e um obstaculo; um bicho assim atravessa a
                // clareira no tempo em que voce olhou o inventario.
                // Acima de ~0.8 a navegacao comeca a passar do ponto nas quinas e ele
                // fica dando voltas em volta do jogador — se for subir, subir olhando.
                .add(Attributes.MOVEMENT_SPEED, 0.62D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        // Prioridade 0: ver StonemanBuildGoal. Ele so toma a vez quando ele empaca ou
        // quando voce esta em cima de alguma coisa; no resto do tempo quem manda e o
        // goal de ataque logo abaixo.
        this.goalSelector.addGoal(0, new StonemanBuildGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
        // A perambulacao ficou LENTA de proposito (0.7 -> 0.4). Com a velocidade nova,
        // andar a toa no mesmo ritmo da cacada entregaria o bicho de longe; assim ele
        // so mostra o que sabe correr quando ja e tarde.
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.4D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_WATCHED, false);
        this.entityData.define(DATA_SURGING, false);
    }

    // ------------------------------------------------------------------ estado

    /** O cliente le isto para saber se desenha o bicho parado. */
    public boolean isWatched() {
        return this.entityData.get(DATA_WATCHED);
    }

    /**
     * Ele esta com a regra quebrada agora?
     *
     * O cliente le isto para tremer a tela e sujar a imagem. Chega de graca junto com
     * a entidade — SynchedEntityData nao precisa do pacote proprio que o mod nao tem.
     */
    public boolean isSurging() {
        return this.entityData.get(DATA_SURGING);
    }

    /**
     * Ele quer chegar em voce ha um tempo e nao esta saindo do lugar?
     *
     * E o sinal que manda ele pegar na pedra. Medir por posicao, e nao perguntando a
     * navegacao se o caminho existe, e de proposito: caminho "existe" em um monte de
     * situacao em que ele nao chega (dando volta eterna num muro, subindo e caindo do
     * mesmo bloco). O chao nao mente — se ele nao andou, ele nao andou.
     */
    public boolean isStuck() {
        return this.stuckTicks >= 30;
    }

    /** Zera a paciencia (o goal de construcao chama ao terminar a obra). */
    public void clearStuck() {
        this.stuckTicks = 0;
    }

    private void trackStuck(boolean frozen) {
        Vec3 here = this.position();

        // Congelado ele nao esta empacado, esta obedecendo a regra. E de perto nao ha
        // o que construir: e hora de bater, nao de fazer obra.
        LivingEntity target = getTarget();
        if (frozen || target == null || this.distanceToSqr(target) < 4.0D) {
            this.stuckTicks = 0;
            this.lastSpot = here;
            return;
        }

        double moved = here.subtract(this.lastSpot).horizontalDistanceSqr();
        if (moved < 0.0016D) this.stuckTicks++;      // menos de 4cm no tick
        else this.stuckTicks = 0;

        this.lastSpot = here;
    }

    // ------------------------------------------------------------------ o congelamento

    /**
     * O "estao me olhando?" e resolvido ANTES do super, para valer ja neste tick.
     *
     * Nao da para calcular dentro do ciclo de IA: quando ele congela, o ciclo de IA
     * inteiro deixa de rodar — inclusive o que faria a conta de descongelar. Ele
     * ficaria de pedra para sempre.
     */
    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            boolean watched = computeWatched();
            boolean wasFrozen = this.entityData.get(DATA_WATCHED);

            tickSurge(watched);

            // Sendo olhado ele congela — a nao ser que esteja em surto, e ai o olhar
            // simplesmente nao vale nada.
            boolean frozen = watched && this.surgeTicks <= 0;

            if (frozen && !wasFrozen) {
                // O estalo e o unico aviso de que ele parou por sua causa.
                this.level().playSound(null, this.blockPosition(), SoundEvents.STONE_PLACE,
                        SoundSource.HOSTILE, 0.7F, 0.5F);
            } else if (wasFrozen && !frozen) {
                // Soltou. Se ja estava em cima de voce quando voce desviou os olhos,
                // nao ha perseguicao nenhuma: ha uma paulada.
                tryAmbush();
            }

            this.entityData.set(DATA_WATCHED, frozen);
            trackStuck(frozen);
        }
        super.tick();
    }

    // ------------------------------------------------------------------ a instabilidade

    /**
     * O relogio do surto: conta, dispara e encerra.
     *
     * Duas portas levam ao surto — o relogio aleatorio e o olhar fixo demais. A
     * segunda existe para fechar a saida barata: encarar sem piscar seria uma jogada
     * dominante (custa nada, prende para sempre), e jogo de terror onde existe uma
     * jogada dominante deixa de ser jogo de terror no dia em que o jogador a descobre.
     */
    private void tickSurge(boolean watched) {
        if (this.ambushRest > 0) this.ambushRest--;

        // Surto em andamento: so contar ate acabar.
        if (this.surgeTicks > 0) {
            if (--this.surgeTicks <= 0) {
                this.surgeTicks = 0;
                this.entityData.set(DATA_SURGING, false);
                this.stareTicks = 0;
                rearmSurge();
            }
            return;
        }

        if (!RECConfig.COMMON.stonemanSurge.get()) {
            this.stareTicks = 0;
            return;
        }

        // O relogio so corre com plateia. Sem ninguem por perto ele nao "gasta" o
        // surto no vazio — e nem chega perto de voce com o cronometro ja vencido.
        double range = RECConfig.COMMON.stonemanWatchRange.get();
        boolean audience = this.getTarget() != null
                || this.level().getNearestPlayer(this, range) != null;
        if (!audience) {
            this.stareTicks = 0;
            return;
        }

        // Primeiro tick com alguem por perto: o relogio nasce agora. Armar no
        // construtor faria o bicho aparecer com o cronometro ja vencido, e o primeiro
        // encontro comecaria com um surto — o pior tick possivel para gastar o susto.
        if (this.surgeCooldown < 0) rearmSurge();

        this.stareTicks = watched ? this.stareTicks + 1 : 0;

        int stareLimit = ticks(RECConfig.COMMON.stonemanStareBreakSeconds.get());
        boolean stareBroke = this.stareTicks >= stareLimit;

        if (this.surgeCooldown > 0) this.surgeCooldown--;

        if (stareBroke || this.surgeCooldown <= 0) {
            startSurge();
        }
    }

    /** Quebra a regra por 2 a 4 segundos, com aviso. */
    private void startSurge() {
        int min = ticks(RECConfig.COMMON.stonemanSurgeMinSeconds.get());
        int max = Math.max(min, ticks(RECConfig.COMMON.stonemanSurgeMaxSeconds.get()));

        this.surgeTicks = min + this.random.nextInt(max - min + 1);
        this.stareTicks = 0;
        this.entityData.set(DATA_SURGING, true);

        // Pedra rachando: o jogador PRECISA saber que a regra caiu, senao a morte
        // parece bug do mod e nao escolha do jogo. (Licao 2 das autopsias: nunca
        // tirar o chao do jogador sem avisar.)
        this.level().playSound(null, this.blockPosition(), SoundEvents.STONE_BREAK,
                SoundSource.HOSTILE, 1.4F, 0.45F);

        // Surtou colado em voce? Nao da tempo de correr.
        tryAmbush();
    }

    /** Sorteia a espera ate o proximo surto espontaneo. */
    private void rearmSurge() {
        int min = RECConfig.COMMON.stonemanSurgeIntervalMin.get();
        int max = Math.max(min, RECConfig.COMMON.stonemanSurgeIntervalMax.get());
        this.surgeCooldown = (min + this.random.nextInt(max - min + 1)) * 20;
    }

    /**
     * A emboscada: solto a dois passos de voce, ele nao caminha — ele acerta.
     *
     * Sem isto o surto perto seria inofensivo (o MeleeAttackGoal ainda ia se
     * aproximar, mirar e so entao bater, e 2 segundos acabam antes disso).
     */
    private void tryAmbush() {
        if (this.ambushRest > 0) return;

        double reach = RECConfig.COMMON.stonemanAmbushRange.get();
        if (reach <= 0.0D) return;

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;
        if (target instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        if (this.distanceToSqr(target) > reach * reach) return;
        if (!this.hasLineOfSight(target)) return;

        this.ambushRest = AMBUSH_REST;
        this.swing(InteractionHand.MAIN_HAND);
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);

        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * RECConfig.COMMON.stonemanAmbushDamage.get().floatValue();
        target.hurt(this.damageSources().mobAttack(this), damage);

        // O empurrao e o que vende o peso: nao foi um tapa, foi uma estatua chegando.
        Vec3 push = target.position().subtract(this.position());
        if (push.horizontalDistanceSqr() > 1.0E-4D) {
            push = push.normalize().scale(0.85D);
            target.push(push.x, 0.35D, push.z);
            target.hurtMarked = true;
        }

        this.level().playSound(null, this.blockPosition(), SoundEvents.STONE_BREAK,
                SoundSource.HOSTILE, 1.2F, 0.4F);
    }

    private static int ticks(double seconds) {
        return Math.max(1, (int) Math.round(seconds * 20.0D));
    }

    /**
     * O congelamento inteiro sai daqui.
     *
     * `isImmobile` e o gancho que o proprio jogo usa para "esta criatura nao age": o
     * LivingEntity zera os comandos de andar e pular e PULA o ciclo de IA — sentidos,
     * goals, navegacao, controle de movimento e controle de olhar, tudo de uma vez.
     * Nada gira, nada escorrega, e a ilusao de estatua fica perfeita.
     *
     * ⚠️ Nao adianta tentar isso por `serverAiStep`: no Mob ele e FINAL. E fazer pelo
     * `customServerAiStep` nao resolveria, porque ele roda ANTES dos controles — o
     * moveControl e o lookControl desfariam o congelamento no mesmo tick.
     *
     * A gravidade continua valendo: quem congela no ar, cai.
     */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isWatched();
    }

    /**
     * Algum jogador esta com ele dentro do campo de visao E com linha de tiro livre?
     *
     * Parede corta o olhar: ficar "olhando" atraves de pedra nao prende. O alcance
     * tem teto porque, sem ele, um jogador do outro lado do mapa apontado por acaso
     * na direcao certa congelaria o bicho para sempre.
     */
    private boolean computeWatched() {
        double range = RECConfig.COMMON.stonemanWatchRange.get();
        double rangeSq = range * range;

        for (Player player : this.level().players()) {
            if (player.isSpectator() || !player.isAlive()) continue;
            if (this.distanceToSqr(player) > rangeSq) continue;

            Vec3 toMe = new Vec3(this.getX() - player.getX(),
                    this.getEyeY() - player.getEyeY(),
                    this.getZ() - player.getZ());
            double length = toMe.length();
            if (length < 1.0E-4D) return true;

            if (player.getViewVector(1.0F).normalize().dot(toMe.scale(1.0D / length)) > LOOK_CONE
                    && player.hasLineOfSight(this)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ som e sensacao

    @Override
    protected SoundEvent getAmbientSound() {
        return isWatched() ? null : SoundEvents.STONE_HIT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STONE_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STONE_BREAK;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.STONE_STEP, 0.6F, 0.7F);
    }

    /** Estatua nao arde nem se afoga com pressa; e pedra. */
    @Override
    public boolean fireImmune() {
        return true;
    }
}
