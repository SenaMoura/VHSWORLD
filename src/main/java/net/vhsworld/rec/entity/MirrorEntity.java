package net.vhsworld.rec.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.vhsworld.rec.escape.Escape;
import net.vhsworld.rec.escape.ExitMethod;

/**
 * O ESPELHO: a saida de treze das quinze dimensoes.
 *
 * Um painel preto de dois metros e meio por oito, com tres olhos desenhados. Ele nao anda,
 * nao ataca e nao faz barulho. Fica de pe no ponto dele, virado para quem chegar.
 *
 * ============================ A REGRA ============================
 *
 * Encostar nele sem ter olhado = sair da dimensao.
 * Olhar para ele = perder sanidade depressa, levar o susto, e ser avisado.
 * Continuar olhando = voltar para o inicio da dimensao.
 *
 * ⚠️ E A UNICA COISA DO MOD QUE PUNE OLHAR, e e por isso que ela funciona como fim de
 * jornada. Todo o resto do VHSWORLD ensina o jogador a olhar: a camera e o verbo do mod,
 * o flash revela, a foto cataloga, o Ofanim so anda quando NAO se olha para ele. Depois de
 * quinze dimensoes treinando o olho, a saida e a coisa que nao se pode ver.
 *
 * ============================ POR QUE E `Mob` E NAO `Entity` ============================
 *
 * ⚠️ Ela nao tem IA nenhuma e mesmo assim herda de `Mob`, o que parece desperdicio. A
 * razao e o TARGETING: `Player.pick`, o raio que decide o que o jogador esta mirando, e a
 * conta de linha de visada do servidor so consideram entidades com caixa de colisao
 * "atacavel". Um `Entity` cru seria atravessado pelo olhar do jogador — e a regra inteira
 * desta criatura e sobre para onde ele esta olhando.
 *
 * O que ela desliga, ela desliga na mao: nao cai, nao empurra, nao apanha, nao morre, nao
 * some com o tempo e nao pode ser levada pelo despawn do jogo. Ver `MirrorDirector`, que
 * e quem a repoe.
 */
public class MirrorEntity extends Mob {

    /**
     * Ela ja levou alguem embora?
     *
     * Sincronizado para o cliente porque o renderizador o le: espelho gasto nao desenha
     * os olhos. E o unico feedback visual de que aquele ali ja serviu.
     */
    private static final EntityDataAccessor<Boolean> SPENT =
            SynchedEntityData.defineId(MirrorEntity.class, EntityDataSerializers.BOOLEAN);

    public MirrorEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SPENT, Boolean.FALSE);
    }

    public boolean isSpent() {
        return this.entityData.get(SPENT);
    }

    public void setSpent(boolean spent) {
        this.entityData.set(SPENT, spent);
    }

    /**
     * Encostar nele e sair.
     *
     * ⚠️ NAO CONFERE SE O JOGADOR ESTAVA OLHANDO. Parece uma brecha — bastaria encarar e
     * andar de frente ate ele — e nao e: quem encara nao CHEGA. A sanidade cai, o susto
     * entra e, antes de vencer a distancia, o `MirrorDirector` ja o devolveu ao inicio da
     * dimensao. A regra e cobrada durante o caminho, que e onde ela pode ser cobrada; aqui
     * no fim so resta abrir a porta para quem pagou.
     */
    @Override
    public InteractionResult interactAt(Player player, net.minecraft.world.phys.Vec3 hit, InteractionHand hand) {
        if (this.level().isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) {
            this.setSpent(true);
            Escape.leave(serverPlayer, ExitMethod.MIRROR);
        }
        return InteractionResult.CONSUME;
    }

    /** Encostar de verdade tambem vale: quem chegou ate aqui nao precisa procurar botao. */
    @Override
    public void playerTouch(Player player) {
        if (this.level().isClientSide) return;
        if (player instanceof ServerPlayer serverPlayer) {
            this.setSpent(true);
            Escape.leave(serverPlayer, ExitMethod.MIRROR);
        }
    }

    // ------------------------------------------------------------------ o que ela nao faz
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    /**
     * ⚠️ NUNCA DESPAWNA POR DISTANCIA. Sem isto, o jogo remove a criatura assim que o
     * jogador se afasta o bastante — e o que ele removeria e a saida da dimensao. O
     * `MirrorDirector` repoe, mas repor uma coisa que nao deveria ter sumido e desperdicio
     * e uma janela em que ela nao esta la.
     */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Spent", isSpent());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSpent(tag.getBoolean("Spent"));
    }
}
