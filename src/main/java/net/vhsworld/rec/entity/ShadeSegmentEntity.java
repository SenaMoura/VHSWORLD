package net.vhsworld.rec.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.vhsworld.rec.config.RECConfig;

/**
 * O ANOMALO DA SOMBRA — a luz e que segura ele, nao o seu olhar.
 *
 * Coluna preta rasgada, sem bracos e sem pernas, com dois pontos brancos no alto. Ele
 * fica encostado em parede de caverna e em canto escuro, e nao se move enquanto e
 * visto. Quando o breu fecha — a tocha acabou, a caverna virou a esquina — ele desliza
 * pelo chao na sua direcao. Rapido.
 *
 * O QUE ISSO MUDA NO JOGO: a tocha deixa de ser conveniencia e vira arma. O jogador
 * que sempre teve luz sobrando descobre que ela era a unica coisa que o mantinha
 * parado, e a partir dai cada carvao pesa. Nao ha nada para matar, so uma conta de
 * recurso que o proprio jogador ja estava fazendo distraidamente.
 *
 * ⚠️ A luz e medida no bloco DELE, nao no do jogador. Faz diferenca nas duas pontas:
 * dar tocha para si mesmo nao adianta se ele esta no escuro la atras, e jogar uma
 * tocha PARA CIMA dele o prende no lugar mesmo com voce no breu. A segunda e a jogada
 * boa, e ela so existe porque a medida e no bloco dele.
 */
public class ShadeSegmentEntity extends Monster {

    /** O cliente le isto para saber se desenha a ondulacao ou uma coluna dura. */
    private static final EntityDataAccessor<Boolean> DATA_HELD =
            SynchedEntityData.defineId(ShadeSegmentEntity.class, EntityDataSerializers.BOOLEAN);

    /** Ha quantos ticks ele esta solto. Alimenta o volume do arrasto. */
    private int loose;

    public ShadeSegmentEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                // Rapido, e isso e seguro aqui: ele so anda no escuro e sem plateia,
                // entao a velocidade nunca e dificuldade constante — e o tamanho do
                // buraco que a tocha apagada abre. Abaixo disso o breu nao assusta.
                .add(Attributes.MOVEMENT_SPEED, 0.52D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        // So caca. Nada de perambular: um bicho que anda a toa no escuro entrega,
        // pelo som, que a regra dele e o escuro — e a descoberta tem que ser sua.
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_HELD, true);
    }

    /** Ele esta preso agora (por luz ou por olhar)? */
    public boolean isHeld() {
        return this.entityData.get(DATA_HELD);
    }

    public int getLooseTicks() {
        return this.loose;
    }

    /**
     * A conta vai ANTES do super, pelo mesmo motivo do Homem de Pedra: preso, o ciclo
     * de IA inteiro deixa de rodar, e com ele a conta que o soltaria. Ele ficaria
     * parado para sempre.
     */
    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            boolean held = computeHeld();
            boolean wasHeld = this.entityData.get(DATA_HELD);

            if (held) {
                this.loose = 0;
            } else {
                this.loose++;
                // O arrasto e o unico aviso de que ele soltou. Sem ele, morrer no
                // breu nao ensina nada — o jogador nao chega a saber que existia
                // uma regra, e o escuro vira azar em vez de escolha.
                if (this.loose % 11 == 0) {
                    this.level().playSound(null, this.blockPosition(),
                            SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.HOSTILE, 0.7F, 0.6F);
                }
            }

            // Na borda preso->solto, um estalo seco: e o instante que interessa.
            if (wasHeld && !held) {
                this.level().playSound(null, this.blockPosition(),
                        SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.45F, 1.7F);
            }

            this.entityData.set(DATA_HELD, held);
        }
        super.tick();
    }

    /**
     * Preso por LUZ ou por OLHAR — as duas travas valem sozinhas.
     *
     * As duas existem porque resolvem medos diferentes. A luz e a regra que o jogador
     * pode planejar (carvao, tocha, fogueira). O olhar e a que salva quem foi pego
     * sem nada: virar a cabeca ainda compra um segundo. Sem a segunda, o breu sem
     * recurso seria morte certa e o jogo viraria inventario; sem a primeira, a tocha
     * nao teria valor nenhum e o breu nao seria assustador.
     */
    private boolean computeHeld() {
        int lit = this.level().getMaxLocalRawBrightness(this.blockPosition());
        if (lit > RECConfig.COMMON.shadeSegmentDarkLevel.get()) return true;

        return Gaze.seenByAny(this, RECConfig.COMMON.shadeSegmentWatchRange.get(), Gaze.CONE_WIDE);
    }

    /**
     * Preso e preso de verdade: `isImmobile` pula o ciclo de IA inteiro (sentidos,
     * goals, navegacao, moveControl, lookControl). Nada gira e nada escorrega.
     *
     * ⚠️ Nao da para fazer isso por `serverAiStep` (e final no Mob) nem por
     * `customServerAiStep` (roda antes dos controles, que desfariam no mesmo tick).
     */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isHeld();
    }

    // ------------------------------------------------------------------ som

    /**
     * Preso, ele e mudo. Nada de som ambiente denunciando onde ele esta enquanto voce
     * tem luz — de tocha na mao voce tem que passar por ele SEM SABER que passou.
     */
    @Override
    protected SoundEvent getAmbientSound() {
        return isHeld() ? null : SoundEvents.WARDEN_HEARTBEAT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SCULK_BLOCK_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SCULK_BLOCK_BREAK;
    }

    /** Ele desliza; nao tem pe para bater no chao. */
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {}

    @Override
    public boolean fireImmune() {
        return true;
    }
}
