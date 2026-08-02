package net.vhsworld.rec.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.vhsworld.rec.config.RECConfig;

/**
 * A SILHUETA INVERTIDA — tem a sua forma, e nunca deixa voce chegar.
 *
 * De longe e um jogador parado no campo. Voce anda para la, e ela anda para tras na
 * mesma velocidade, de frente para voce, sem nunca virar as costas. A distancia entre
 * voces dois nao muda um metro. Se voce a encurralar num canto, ela nao briga nem
 * foge para o lado: ela simplesmente deixa de estar la.
 *
 * POR QUE NAO PODE PEGAR: o desconforto inteiro mora em nao haver desfecho. Uma
 * silhueta que se deixasse alcancar viraria, em um segundo, ou um mob comum (se
 * atacasse) ou um enfeite (se nao atacasse) — e nos dois casos o jogador para de
 * pensar nela. Enquanto ela recua, ela continua sendo uma pergunta. Por isso o
 * `desaparecer no encurralamento` nao e desistencia do design: e o que protege a
 * pergunta de ser respondida.
 *
 * ⚠️ Ela nao ataca, nunca. Nao tem ATTACK_DAMAGE e nao tem goal de alvo.
 */
public class InvertedSilhouetteEntity extends Monster {

    /** Ela esta recuando agora? O cliente inverte a passada com isto. */
    private static final EntityDataAccessor<Boolean> DATA_BACKING =
            SynchedEntityData.defineId(InvertedSilhouetteEntity.class, EntityDataSerializers.BOOLEAN);

    /** Ha quantos ticks ela quer recuar e nao esta conseguindo (parede atras). */
    private int cornered;

    private Vec3 lastSpot = Vec3.ZERO;

    public InvertedSilhouetteEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                // A velocidade tem que ser MAIOR que a do jogador correndo (~0.28 com
                // sprint). Igual nao basta: qualquer tropeco dela viraria um metro
                // ganho, e em vinte segundos de perseguicao o jogador encostava. A
                // regra e "a distancia nao muda", e regra so vale se nao vaza.
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    /** Sem goals: o recuo e escrito no tick, porque depende de olhar e andar juntos. */
    @Override
    protected void registerGoals() {}

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_BACKING, false);
    }

    public boolean isBacking() {
        return this.entityData.get(DATA_BACKING);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        Player player = this.level().getNearestPlayer(this, RECConfig.COMMON.silhouetteRange.get());
        if (player == null) {
            this.entityData.set(DATA_BACKING, false);
            this.cornered = 0;
            this.navigation.stop();
            return;
        }

        face(player);

        double keep = RECConfig.COMMON.silhouetteKeepDistance.get();
        double gap = Math.sqrt(this.distanceToSqr(player));

        if (gap >= keep) {
            // Na distancia certa: ela para e encara. Parada e como ela se parece
            // com um jogador — e a isca inteira.
            this.entityData.set(DATA_BACKING, false);
            this.cornered = 0;
            this.navigation.stop();
            this.lastSpot = this.position();
            return;
        }

        this.entityData.set(DATA_BACKING, true);
        retreat(player, keep);
        trackCornered();
    }

    /**
     * Ela olha para voce o tempo todo — inclusive andando para tras.
     *
     * O corpo gira junto com a cabeca (`yBodyRot`) porque o efeito depende de ela
     * ficar de FRENTE. Um recuo com o corpo virado para o caminho seria uma fuga
     * comum, e fuga comum nao incomoda ninguem.
     */
    private void face(Player player) {
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;

        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.getLookControl().setLookAt(player, 30.0F, 30.0F);
    }

    /**
     * Recua na linha reta que sai do jogador, com um pouco de desvio.
     *
     * O desvio existe para ela nao entrar em toda parede que houver atras dela. Sem
     * ele, o "encurralamento" aconteceria o tempo todo, em terreno normal, e o
     * sumico — que devia ser o final raro de uma perseguicao teimosa — viraria a
     * coisa mais comum do bicho.
     */
    private void retreat(Player player, double keep) {
        Vec3 away = this.position().subtract(player.position());
        if (away.horizontalDistanceSqr() < 1.0E-4D) {
            away = new Vec3(this.random.nextDouble() - 0.5D, 0.0D, this.random.nextDouble() - 0.5D);
        }
        away = new Vec3(away.x, 0.0D, away.z).normalize();

        double swerve = (this.random.nextDouble() - 0.5D) * 0.6D;
        Vec3 side = new Vec3(-away.z, 0.0D, away.x).scale(swerve);
        Vec3 goal = this.position().add(away.scale(keep * 0.6D)).add(side);

        this.navigation.moveTo(goal.x, goal.y, goal.z, 1.0D);
    }

    /**
     * "Encurralada" se mede por POSICAO, nao perguntando a navegacao se ha caminho.
     *
     * Caminho "existe" em um monte de situacao em que ela nao anda (dando volta num
     * muro, subindo e caindo do mesmo bloco). O chao nao mente: se ela quis recuar e
     * nao saiu do lugar, ela esta presa. E a mesma licao do goal de construcao do
     * Homem de Pedra.
     */
    private void trackCornered() {
        Vec3 here = this.position();
        double moved = here.subtract(this.lastSpot).horizontalDistanceSqr();
        this.lastSpot = here;

        if (moved > 0.0016D) {            // andou mais de 4cm no tick
            this.cornered = 0;
            return;
        }

        if (++this.cornered >= RECConfig.COMMON.silhouetteCorneredTicks.get()) {
            vanish();
        }
    }

    /**
     * Ela deixa de estar la. Sem briga, sem grito, sem drop.
     *
     * A fumaca e discreta de proposito: um efeito grande viraria "eu derrotei alguma
     * coisa", e a sensacao certa e a oposta — a de ter perdido tempo com uma coisa
     * que nunca esteve ali para ser pega.
     */
    private void vanish() {
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(),
                    24, 0.25D, 0.5D, 0.25D, 0.01D);
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.WOOL_BREAK,
                SoundSource.HOSTILE, 0.5F, 0.5F);
        this.discard();
    }

    // ------------------------------------------------------------------ ela nao briga

    /**
     * Bater nela e como bater na neblina: ela some, e so.
     *
     * Deixar matar daria ao jogador a unica coisa que ela nao pode oferecer — um
     * desfecho. E deixar dropar alguma coisa transformaria o encontro em farm.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) return false;
        if (source.isCreativePlayer()) return super.hurt(source, amount);
        vanish();
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {}

    @Override
    public void setTarget(LivingEntity target) {}

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    /** Silenciosa ate no passo: o que assusta e ela nao fazer barulho nenhum. */
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {}

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
