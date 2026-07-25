package net.vhsworld.rec.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.vhsworld.rec.config.RECConfig;
import org.jetbrains.annotations.Nullable;

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
 * TUDO AQUI E SERVIDOR. O "estao me olhando?" e calculado no servidor e mandado ao
 * cliente por SynchedEntityData; o modelo so le a bandeira para decidir se anima. Um
 * calculo desses no cliente daria, em multiplayer, um bicho parado para um jogador e
 * andando para outro. E os numeros vem do config COMMON, nunca do CLIENT — item ou
 * entidade que le config de cliente no ramo do servidor derruba servidor dedicado.
 */
public class StonemanEntity extends Monster {

    private static final EntityDataAccessor<Boolean> DATA_WATCHED =
            SynchedEntityData.defineId(StonemanEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(StonemanEntity.class, EntityDataSerializers.INT);

    /** Quantas peles/modelos existem. */
    public static final int VARIANTS = 3;

    /**
     * Cosseno do meio-angulo do cone de visao que conta como "olhando".
     *
     * 0.55 da ~114 graus no total, um pouco mais estreito que a tela do jogador. Mais
     * largo e ele congelaria de canto de olho, sem o jogador saber por que; mais
     * estreito e o jogador olharia bem para ele e o bicho andaria assim mesmo.
     */
    private static final double LOOK_CONE = 0.55D;

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
                // Rapido de proposito: so se move sem plateia, entao a velocidade e o
                // susto. Parado ele nunca alcanca ninguem; solto, alcanca.
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_WATCHED, false);
        this.entityData.define(DATA_VARIANT, 0);
    }

    // ------------------------------------------------------------------ estado

    /** O cliente le isto para saber se desenha o bicho parado. */
    public boolean isWatched() {
        return this.entityData.get(DATA_WATCHED);
    }

    public int getVariant() {
        return Mth.clamp(this.entityData.get(DATA_VARIANT), 0, VARIANTS - 1);
    }

    public void setVariant(int variant) {
        this.entityData.set(DATA_VARIANT, Mth.clamp(variant, 0, VARIANTS - 1));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant")) setVariant(tag.getInt("Variant"));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        setVariant(this.random.nextInt(VARIANTS));
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
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
            if (watched && !this.entityData.get(DATA_WATCHED)) {
                // O estalo e o unico aviso de que ele parou por sua causa.
                this.level().playSound(null, this.blockPosition(), SoundEvents.STONE_PLACE,
                        SoundSource.HOSTILE, 0.7F, 0.5F);
            }
            this.entityData.set(DATA_WATCHED, watched);
        }
        super.tick();
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
