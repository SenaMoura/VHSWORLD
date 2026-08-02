package net.vhsworld.rec.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.vhsworld.rec.config.RECConfig;

/**
 * O OBSERVADOR ESTATICO — ele nao corre atras de voce. Ele so nao esta mais tao longe.
 *
 * A regra e o avesso da do Homem de Pedra, e de proposito. La, olhar PRENDE: o bicho
 * vira estatua e voce ganha tempo enquanto encara. Aqui, olhar EXPULSA: ele se apaga
 * como televisao desligada no instante em que voce mira nele — e volta mais perto
 * quando voce vira a cabeca. Olhar deixa de ser defesa e vira a coisa que o alimenta.
 *
 * O que isso faz com o jogador: contra o Homem de Pedra existe uma jogada (encarar).
 * Contra este nao existe nenhuma. Ele nao pode ser preso pelo olhar, nao pode ser
 * morto (o dano so o apaga) e nao pode ser deixado para tras, porque cada piscada
 * fecha um quarto da distancia. A unica saida e sair de perto — e e essa a doutrina
 * do VHSWORLD: nao se mata, se foge.
 *
 * ⚠️ Ele NAO tem goal de movimento nenhum. Toda a "locomocao" e teletransporte, e
 * so acontece no escuro do seu piscar. Ver o bicho andar arruinaria a ilusao: a
 * graca e nunca haver o momento em que ele se mexeu.
 */
public class StaticWatcherEntity extends Monster {

    /**
     * "Estou apagado agora?" O cliente le isto para nao desenhar nada.
     *
     * Vai por SynchedEntityData, e nao por pacote proprio — e o padrao do mod, e
     * aqui ainda cai bem: apagar e um ESTADO, nao um evento. Quem chega perto depois
     * que ele se apagou tem que ver o vazio, e um pacote perdido no meio do caminho
     * deixaria um poste preto de seis blocos plantado na paisagem.
     */
    private static final EntityDataAccessor<Boolean> DATA_OFF =
            SynchedEntityData.defineId(StaticWatcherEntity.class, EntityDataSerializers.BOOLEAN);

    /** Quantos ticks ainda faltam para ele poder reacender. */
    private int offTicks;

    /** Quantas aproximacoes ele ja fez nesta rodada (zera quando ele bate). */
    private int steps;

    public StaticWatcherEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                // Velocidade zero nao e enfeite: e a garantia de que nenhum goal
                // herdado, nenhum empurrao de agua e nenhum knockback o facam
                // deslizar. Ele so muda de lugar por teletransporte.
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    /** Sem goals. Ele nao caminha, nao persegue e nao vagueia. */
    @Override
    protected void registerGoals() {}

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OFF, false);
    }

    public boolean isOff() {
        return this.entityData.get(DATA_OFF);
    }

    // ------------------------------------------------------------------ o ciclo

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        if (isOff()) {
            if (--this.offTicks <= 0) tryReturn();
            return;
        }

        Player looker = Gaze.watcher(this, range(), Gaze.CONE_TIGHT);
        if (looker == null) {
            stare();
            return;
        }

        // Olhou para ele estando perto demais: nao da tempo de ele se apagar
        // educadamente. Sem isto, a aproximacao nao teria consequencia nenhuma e o
        // bicho seria um susto de jump scare infinito, sem preco.
        if (this.distanceToSqr(looker) <= strikeRange() * strikeRange()) {
            strike(looker);
        }
        shutOff(RECConfig.COMMON.staticWatcherOffSeconds.get());
    }

    /**
     * Parado, ele encara. E so o que ele faz enquanto esta aceso.
     *
     * A cabeca e o corpo giram juntos (`yBodyRot`): este modelo tem o tronco pendurado
     * NA cabeca, entao girar so a cabeca torceria o bicho inteiro pelo pescoco.
     */
    private void stare() {
        Player near = this.level().getNearestPlayer(this, range());
        if (near == null) return;

        double dx = near.getX() - this.getX();
        double dz = near.getZ() - this.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;

        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    /** Desliga: chiado curto, some da tela, e o relogio de volta comeca a correr. */
    private void shutOff(double seconds) {
        this.entityData.set(DATA_OFF, true);
        this.offTicks = Math.max(1, (int) Math.round(seconds * 20.0D));

        // O estalo de tubo apagando e o unico aviso de que a piscada foi contada.
        // Sem som, o jogador nao liga o sumico ao proprio olhar e a regra fica
        // invisivel — e regra que o jogador nao percebe nao e mecanica, e bug.
        this.level().playSound(null, this.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.HOSTILE, 0.6F, 2.0F);
        this.level().playSound(null, this.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                SoundSource.HOSTILE, 0.35F, 1.8F);
    }

    /**
     * Reacender: escolhe um lugar mais perto que ninguem esteja vendo.
     *
     * Se nao houver lugar assim, ele continua apagado. E o certo: um bicho cuja regra
     * e "voce nunca me ve chegar" nao pode abrir excecao porque o cenario esta aberto
     * demais — a excecao seria justamente o unico momento em que a mentira aparece.
     */
    private void tryReturn() {
        Player target = this.level().getNearestPlayer(this, range());
        if (target == null) {
            // Sem plateia nao ha por que reaparecer. Ele espera de graca.
            this.offTicks = 20;
            return;
        }

        double now = Math.sqrt(this.distanceToSqr(target));
        double closer = Math.max(minDistance(), now * (1.0D - RECConfig.COMMON.staticWatcherStep.get()));

        Vec3 spot = findSpot(target, closer);
        if (spot == null) {
            // Nada escondido a essa distancia; tenta de novo daqui a pouco.
            this.offTicks = 15;
            return;
        }

        this.teleportTo(spot.x, spot.y, spot.z);
        this.entityData.set(DATA_OFF, false);
        this.steps++;
        stare();
    }

    /**
     * Um ponto no anel de raio `dist` em volta do jogador: chao firme, espaco livre e
     * fora do campo de visao de todo mundo.
     *
     * Sorteia direcoes em vez de varrer em ordem porque varrer daria sempre a mesma
     * face do anel — ele apareceria sempre do mesmo lado, e o jogador aprenderia a
     * olhar para la. O sorteio e o que mantem a duvida.
     */
    private Vec3 findSpot(Player target, double dist) {
        double range = range();

        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double x = target.getX() + Math.cos(angle) * dist;
            double z = target.getZ() + Math.sin(angle) * dist;

            // Procura chao a partir da altura do jogador, para nao aterrissar no
            // fundo de uma caverna quando ele esta na superficie.
            net.minecraft.core.BlockPos top = this.level().getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    net.minecraft.core.BlockPos.containing(x, target.getY(), z));

            Vec3 spot = new Vec3(top.getX() + 0.5D, top.getY(), top.getZ() + 0.5D);
            if (Math.abs(spot.y - target.getY()) > 12.0D) continue;

            if (!this.level().noCollision(this, this.getType().getAABB(spot.x, spot.y, spot.z))) {
                continue;
            }
            if (!Gaze.hiddenFromAll(this.level(), spot, range, Gaze.CONE_WIDE,
                    this.getBbHeight() * 0.5D)) {
                continue;
            }
            return spot;
        }
        return null;
    }

    /**
     * Chegou em voce. Uma paulada, cegueira, e ele recomeca de longe.
     *
     * A cegueira e o golpe de verdade: tira exatamente o sentido com que voce estava
     * se defendendo. E o recuo depois de bater existe para o encontro TERMINAR —
     * sem ele, ele reapareceria colado, bateria de novo e viraria moedor de carne.
     */
    private void strike(Player victim) {
        if (victim.isCreative() || victim.isSpectator()) return;

        victim.hurt(this.damageSources().mobAttack(this),
                (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
        victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));

        Vec3 push = victim.position().subtract(this.position());
        if (push.horizontalDistanceSqr() > 1.0E-4D) {
            push = push.normalize().scale(0.6D);
            victim.push(push.x, 0.25D, push.z);
            victim.hurtMarked = true;
        }

        this.level().playSound(null, this.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE,
                SoundSource.HOSTILE, 1.0F, 1.6F);

        this.steps = 0;
        // Volta para o comeco: apagado por muito mais tempo e reaparecendo longe.
        shutOff(RECConfig.COMMON.staticWatcherOffSeconds.get() * 4.0D);
    }

    // ------------------------------------------------------------------ ele nao morre

    /**
     * Bater nele nao machuca: apaga.
     *
     * Deixar matar seria transformar a criatura no seu oposto. O medo dele e nao ter
     * jogada; uma espada devolve a jogada e resolve o problema para sempre. Assim o
     * golpe ate serve para alguma coisa (compra segundos), mas nunca encerra a
     * historia — e o segundo golpe compra menos, porque ele volta mais perto.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) return false;
        if (source.isCreativePlayer()) return super.hurt(source, amount);
        if (isOff()) return false;

        shutOff(RECConfig.COMMON.staticWatcherOffSeconds.get());
        return false;
    }

    /** Apagado ele nao existe: nao empurra, nao e mirado, nao apanha. */
    @Override
    public boolean isPickable() {
        return !isOff();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {}

    @Override
    public boolean fireImmune() {
        return true;
    }

    /** Apagado, ele nao pode ser alvo de nada — nem de mira automatica de outro mod. */
    @Override
    public boolean canBeSeenAsEnemy() {
        return !isOff() && super.canBeSeenAsEnemy();
    }

    /** Ele nunca respira fundo nem grunhe: o silencio faz parte. */
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

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos,
                                 net.minecraft.world.level.block.state.BlockState state) {}

    // ------------------------------------------------------------------ estado salvo

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Off", isOff());
        tag.putInt("OffTicks", this.offTicks);
        tag.putInt("Steps", this.steps);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_OFF, tag.getBoolean("Off"));
        this.offTicks = tag.getInt("OffTicks");
        this.steps = tag.getInt("Steps");
    }

    // ------------------------------------------------------------------ knobs

    private static double range() {
        return RECConfig.COMMON.staticWatcherRange.get();
    }

    private static double minDistance() {
        return RECConfig.COMMON.staticWatcherMinDistance.get();
    }

    private static double strikeRange() {
        return RECConfig.COMMON.staticWatcherStrikeRange.get();
    }

    /** Quantas vezes ele ja se aproximou sem ser interrompido. Usado pelo som. */
    public int getSteps() {
        return this.steps;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** Ele nunca deve alvejar como um mob normal: nao tem goal de alvo. */
    @Override
    public void setTarget(LivingEntity target) {}
}
