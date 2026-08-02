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
 * O RASTREADOR DO RASTEJO — ele avanca exatamente enquanto voce esta ocupado.
 *
 * Quadrupede baixo e comprido, sem rosto, so com dentes retos e brancos na frente da
 * cabeca. Ele nao te caca de peito aberto: ele espera voce virar as costas para
 * minerar ou para mexer num bau, e cobre o terreno nesses segundos. Quando voce olha,
 * ele para. Nao congela como estatua — ele PARA, como bicho que sabe que foi visto.
 *
 * A REGRA E UM IMPOSTO SOBRE MINERAR. Cavar de costas e o gesto mais automatico do
 * Minecraft, e ele e o unico bicho do jogo que cobra por isso. O resultado nao e
 * medo de morrer: e voce virando a camera de tres em tres blocos sem conseguir
 * explicar por que — que e exatamente o que o VHSWORLD quer produzir.
 *
 * ⚠️ O passo dele e ALTO de proposito. Ver o bicho e o fim do susto; ouvir e o
 * comeco. Sem o som ele seria so um mob que aparece atras de voce.
 */
public class CrawlerVoidEntity extends Monster {

    /** Ele esta avancando agora? O cliente anima a passada com isto. */
    private static final EntityDataAccessor<Boolean> DATA_MOVING =
            SynchedEntityData.defineId(CrawlerVoidEntity.class, EntityDataSerializers.BOOLEAN);

    private int stepClock;

    public CrawlerVoidEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                // Rapido porque so anda em janelas curtas. A conta que importa nao e
                // "quanto ele corre", e "quanto terreno cabe numa distracao sua".
                .add(Attributes.MOVEMENT_SPEED, 0.48D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MOVING, false);
    }

    public boolean isMoving() {
        return this.entityData.get(DATA_MOVING);
    }

    /**
     * A conta vem ANTES do super pelo motivo de sempre: parado, o ciclo de IA nao
     * roda, e a conta que o soltaria mora nele. Ele nunca mais sairia do lugar.
     */
    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            boolean go = mayAdvance();
            this.entityData.set(DATA_MOVING, go);
            if (go) footsteps();
        }
        super.tick();
    }

    /**
     * Ele avanca se o alvo estiver DISTRAIDO — ou se ainda nao houver alvo nenhum.
     *
     * "Distraido" tem tres portas, e as tres sao coisas que o jogador faz sem pensar:
     *
     *  1. nao estar olhando para ele (o caso normal: voce esta de frente para a
     *     parede que esta cavando);
     *  2. estar com um bau, forno ou bancada aberto — a tela tapa o mundo inteiro,
     *     e nesses segundos voce esta mais cego do que de costas;
     *  3. estar balancando o braco (minerando ou batendo), mesmo de frente. Esta e a
     *     mais cruel e a mais justa: cavar de frente para ele tambem custa.
     *
     * De perto ele nao consulta mais nada (`pounceRange`): a essa altura ja e bote, e
     * um bicho que congelasse a um passo de voce seria um brinquedo, nao uma ameaca.
     */
    private boolean mayAdvance() {
        if (!(this.getTarget() instanceof Player player)) return true;

        double reach = RECConfig.COMMON.crawlerPounceRange.get();
        if (this.distanceToSqr(player) <= reach * reach) return true;

        if (player.containerMenu != player.inventoryMenu) return true;
        if (player.swinging) return true;

        return !Gaze.sees(player, this, RECConfig.COMMON.crawlerWatchRange.get(), Gaze.CONE_WIDE);
    }

    /**
     * A passada de madeira e pedra, rapida, no ritmo do avanco.
     *
     * Isto nao passa por `playStepSound`: aquele so toca quando o jogo acha que houve
     * passo, e um bicho que desliza rente ao chao quase nunca dispara o gatilho. O
     * som e a metade do bicho — ele nao pode depender de sorte.
     */
    private void footsteps() {
        if (this.getDeltaMovement().horizontalDistanceSqr() < 0.001D) return;
        if (--this.stepClock > 0) return;

        this.stepClock = 4;
        BlockState under = this.level().getBlockState(this.blockPosition().below());
        SoundEvent step = under.getSoundType(this.level(), this.blockPosition().below(), this)
                .getStepSound();

        this.level().playSound(null, this.blockPosition(), step, SoundSource.HOSTILE,
                1.1F, 0.55F + this.random.nextFloat() * 0.15F);
    }

    /**
     * Visto e longe, ele para. Nao congela: PARA.
     *
     * A diferenca com o Homem de Pedra e proposital. La a imobilidade e total, de
     * estatua, e o susto e voce nao ter visto o bicho se mexer. Aqui ele continua
     * respirando na sua frente, e o susto e outro: e voce saber que, no segundo em
     * que abrir o bau, ele volta a andar.
     */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || !isMoving();
    }

    // ------------------------------------------------------------------ som

    @Override
    protected SoundEvent getAmbientSound() {
        return isMoving() ? SoundEvents.SPIDER_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SPIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }

    /** O passo dele e tocado no tick, com ritmo proprio. Aqui fica mudo. */
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {}

    @Override
    public boolean fireImmune() {
        return true;
    }
}
