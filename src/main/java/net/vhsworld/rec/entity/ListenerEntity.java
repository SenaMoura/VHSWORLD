package net.vhsworld.rec.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.vhsworld.rec.config.RECConfig;

import java.util.function.BiConsumer;

/**
 * O ESCUTADOR — o cego que caça pelo som que VOCE faz.
 *
 * ⚠️ POR QUE ELE EXISTE: PARA QUEBRAR A MONOCULTURA DO OLHAR. Ate aqui o mod tinha sete
 * criaturas e um verbo so. Homem de Pedra, Ofanim, Observador, Silhueta, Rastejo, Sombra e
 * anomalia — todas penduradas em `Gaze`, todas resolvidas com o mesmo gesto. Regra que se
 * aprende inteira para de assustar, e o jogador aprendia a regra inteira em dez minutos.
 * Criatura nova no MESMO eixo nao teria consertado nada: seria a oitava variacao de um
 * puzzle ja resolvido, que e exatamente o que se chamou de slop.
 *
 * Nele, OLHAR NAO FAZ NADA. Nem prende, nem provoca, nem adianta — ele nao tem olho e o
 * codigo nao consulta `Gaze` uma unica vez. O verbo dele e outro: <b>ficar quieto</b>. E o
 * primeiro contra-jogo do mod que cobra alguma coisa de verdade, porque parar de fazer
 * barulho em Minecraft quer dizer parar de MINERAR — ou seja, parar de progredir. O olhar
 * era gratis; o silencio custa tempo.
 *
 * ⚠️ O VOCABULARIO E EMPRESTADO DO VANILLA DE PROPOSITO (ver Ears): quem ja fugiu de um
 * Warden ja sabe que agachar nao faz barulho. Nenhuma linha nossa faz o silencio funcionar —
 * `Player.getMovementEmission()` devolve NONE quando o jogador esta agachado no chao. A
 * criatura chega ao jogo com um contra-jogo que ele ja desconfia que existe.
 *
 * <h3>O segundo em que o jogador percebe que ela agiu</h3>
 * A pergunta que toda mecanica deste mod tem que responder antes de ser escrita, e a que
 * matou a primeira versao da AUSENCIA. Aqui a resposta e o ESTALO: quando ele ouve alguma
 * coisa, ele trava no lugar e estala, e o estalo sai do lugar onde ele esta. Voce quebrou
 * uma pedra e alguma coisa, a trinta blocos, no escuro, reagiu. Nao ha aviso na tela, nao
 * ha marca, nao ha nada para apontar depois — mas ha um instante exato em que o jogador
 * sabe que fez barulho demais, e ele acontece por causa DELE.
 *
 * <h3>E ele nao anda a toa</h3>
 * Sem som, ele nao se move. Nada de perambular. E o que torna a regra testavel: o jogador
 * para, ele para; o jogador bate uma pedra, ele vem. Regra que nao da para testar nao vira
 * estrategia, vira frustracao — a mesma licao do Rastejo.
 */
public class ListenerEntity extends Monster {

    // ------------------------------------------------------------------ o que o cliente ve

    /** 0 tateando, 1 travado escutando, 2 indo atras. O desenho inteiro sai daqui. */
    private static final EntityDataAccessor<Byte> DATA_POSTURE =
            SynchedEntityData.defineId(ListenerEntity.class, EntityDataSerializers.BYTE);

    /** Quao fresco e forte e o som que ele esta perseguindo, de 0 a 1. */
    private static final EntityDataAccessor<Float> DATA_ALERT =
            SynchedEntityData.defineId(ListenerEntity.class, EntityDataSerializers.FLOAT);

    /**
     * Quao BARULHENTO e o chao debaixo dele, de 0 a 1.
     *
     * ⚠️ UMA VERDADE SO PARA OS DOIS LADOS. O servidor le isto para andar mais devagar em
     * folha e em cascalho, e o cliente le o MESMO numero para levantar mais o pe e demorar
     * mais no passo. Se cada lado calculasse o seu, a animacao mentiria sobre a velocidade
     * — e a mentira apareceria como pe patinando, o defeito que denuncia bicho de mod.
     */
    private static final EntityDataAccessor<Float> DATA_GROUND =
            SynchedEntityData.defineId(ListenerEntity.class, EntityDataSerializers.FLOAT);

    public static final byte POSTURE_GROPING = 0;
    public static final byte POSTURE_FROZEN = 1;
    public static final byte POSTURE_SEEKING = 2;

    // ------------------------------------------------------------------ o estado dele

    /** O ouvido, pendurado na secao de chunk em que ele esta. */
    private final DynamicGameEventListener<Ears> ears;

    /** Onde ele acha que o barulho foi. Nao onde voce esta — onde voce FEZ BARULHO. */
    private Vec3 heard;

    /** A forca daquele som, que envelhece. */
    private float alert;

    /** Ha quantos ticks ele ouviu. */
    private int since = Integer.MAX_VALUE / 2;

    /** Ticks travado no lugar depois de ouvir. */
    private int freeze;

    /** Para nao bater duas vezes no mesmo instante. */
    private int hitCooldown;

    /**
     * ⚠️ Quanto tempo ele guarda um som antes de desistir. Longo o bastante para atravessar
     * a distancia, curto o bastante para o jogador poder ESPERAR ele desistir — se nunca
     * desistisse, ficar quieto nao seria contra-jogo, seria adiamento.
     */
    private static final int FORGET = 400;

    public ListenerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.ears = new DynamicGameEventListener<>(
                new Ears(this, RECConfig.COMMON.listenerHearingRange.get()));
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                // ⚠️ Rapido quando tem alvo, e isso e seguro: ele so anda quando VOCE fez
                // barulho. Velocidade nao vira dificuldade constante, vira o tamanho do
                // erro que voce acabou de cometer.
                .add(Attributes.MOVEMENT_SPEED, 0.36D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    /**
     * ⚠️ NENHUM `NearestAttackableTargetGoal`, e esta ausencia e a criatura inteira.
     *
     * Alvo por proximidade seria visao com outro nome: o bicho saberia onde voce esta sem
     * voce ter feito nada, e todo o resto viraria enfeite em cima de um zumbi. Ele nao tem
     * alvo — ele tem um LUGAR onde ouviu um barulho, e vai ate la. Se voce ainda estiver
     * la, o problema e seu.
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new ListenerSeekGoal(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_POSTURE, POSTURE_GROPING);
        this.entityData.define(DATA_ALERT, 0.0F);
        this.entityData.define(DATA_GROUND, 0.0F);
    }

    // ------------------------------------------------------------------ o ouvido

    /**
     * O ouvido acompanha o corpo pela secao de chunk. Sem isto ele so escutaria o pedaco
     * de mundo onde nasceu.
     */
    @Override
    public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> consumer) {
        if (this.level() instanceof ServerLevel server) {
            consumer.accept(this.ears, server);
        }
    }

    /**
     * OUVIU. Chamado pelo Ears.
     *
     * ⚠️ SOM NOVO SO GANHA DE SOM VELHO SE FOR MAIS FORTE — ou se o velho ja esfriou. Sem
     * essa comparacao, o passo dele mesmo... nao: o passo de qualquer bicho, uma gota, uma
     * porta longe, qualquer coisa mais recente roubaria a atencao, e a criatura viraria uma
     * agulha de bussola girando. O que ele persegue tem que ser o BARULHO QUE IMPORTOU.
     */
    void hear(Vec3 pos, float strength, BlockPos block) {
        if (this.level().isClientSide) return;

        float current = this.alert * fade();
        if (strength <= current && this.since < FORGET / 2) return;

        boolean fresh = this.heard == null || this.heard.distanceToSqr(pos) > 4.0D;

        this.heard = pos;
        this.alert = strength;
        this.since = 0;

        // ⚠️ O ESTALO — o acontecimento perceptivel, e a razao de esta criatura funcionar
        // ou nao. Ele nao avisa "ha um monstro aqui": ele e o som que o corpo dela faz ao
        // travar. Sai do lugar DELA, nao do jogador, e por isso responde uma pergunta que
        // o jogador nao tinha feito — de onde veio isso? O gancho e o mesmo da AUSENCIA:
        // som ja acabou quando voce vira a cabeca, entao ele nao e prova de nada. E o que
        // faz voce virar.
        if (fresh) {
            this.freeze = 14;
            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.SCULK_CLICKING, SoundSource.HOSTILE,
                    0.6F + strength * 0.5F, 0.7F + this.random.nextFloat() * 0.2F);
        }
    }

    /** Ele esta perseguindo alguma coisa agora? */
    public boolean hasSound() {
        return this.heard != null && this.since < FORGET;
    }

    public Vec3 soundPos() {
        return this.heard;
    }

    /**
     * CHEGOU AO LUGAR DO BARULHO E NAO HAVIA NADA.
     *
     * ⚠️ Ele nao "desiste": ele TATEIA um pouco antes de largar. Sao dois segundos parado
     * em cima da coordenada, e eles sao a melhor parte da criatura para quem esta escondido
     * a tres blocos prendendo a respiracao. Largar na hora seria mais justo e muito pior.
     */
    void arrive() {
        // ⚠️ Ja tateando: nao renova o relogio. Sem esta guarda o goal chamaria isto a cada
        // tick em cima da coordenada e o tateio nunca terminaria — ele ficaria plantado ali
        // para sempre, que e o defeito que o proprio metodo veio consertar.
        if (this.freeze > 0) return;

        this.freeze = 40;
        this.since = FORGET - 40;
        this.getNavigation().stop();
    }

    /** A forca do som ja descontado o envelhecimento. */
    public float alert() {
        return this.alert * fade();
    }

    private float fade() {
        if (this.since >= FORGET) return 0.0f;
        return 1.0f - (float) this.since / FORGET;
    }

    // ------------------------------------------------------------------ o corpo

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            serverStep();
        }
        super.tick();
    }

    private void serverStep() {
        this.since++;
        if (this.hitCooldown > 0) this.hitCooldown--;

        if (this.since >= FORGET) {
            this.heard = null;
            this.alert = 0.0f;
        }

        if (this.freeze > 0) {
            this.freeze--;
            faceSound();
        }

        // O chao a cada meio segundo: e informacao lenta e custa uma leitura de bloco.
        if (this.tickCount % 10 == 0) {
            this.entityData.set(DATA_GROUND, groundNoise());
        }

        this.entityData.set(DATA_ALERT, alert());
        this.entityData.set(DATA_POSTURE,
                this.freeze > 0 ? POSTURE_FROZEN : (hasSound() ? POSTURE_SEEKING : POSTURE_GROPING));

        touch();
    }

    /**
     * ⚠️ ELE MATA POR CONTATO, e nao por perseguicao com alvo. A diferenca importa: um mob
     * com alvo continua sabendo onde voce esta depois que voce para de fazer barulho, e ai
     * o silencio deixaria de ser saida. Aqui ele chega ao LUGAR do barulho e machuca quem
     * estiver encostado nele — quem ficou quieto e se afastou nao esta.
     */
    private void touch() {
        if (this.hitCooldown > 0) return;

        AABB box = this.getBoundingBox().inflate(0.4D);
        for (Player player : this.level().getEntitiesOfClass(Player.class, box)) {
            if (player.isCreative() || player.isSpectator()) continue;
            this.doHurtTarget(player);
            this.hitCooldown = 20;
            return;
        }
    }

    /** Vira o corpo inteiro para o som. Travado, e a unica coisa que ele faz. */
    private void faceSound() {
        if (this.heard == null) return;

        double dx = this.heard.x - this.getX();
        double dz = this.heard.z - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0F / Math.PI)) - 90.0F;

        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    /**
     * QUAO BARULHENTO E O CHAO DEBAIXO DELE.
     *
     * ⚠️ ISTO NAO E ENFEITE DE ANIMACAO — e a regra dele aplicada a ele mesmo. Ele tambem
     * escuta, entao pisa devagar onde o chao denuncia: folha, neve, cascalho, madeira. O
     * jogador le isso sem tutorial nenhum e tira a conclusao certa sozinho: <i>a coisa tem
     * medo do proprio passo</i>. E, do lado pratico, e o unico terreno em que da para
     * ganhar distancia dele — o que transforma o mapa em informacao.
     */
    private float groundNoise() {
        BlockState below = this.level().getBlockState(this.blockPosition().below());
        if (below.isAir()) return 0.0f;

        SoundType sound = below.getSoundType(this.level(), this.blockPosition().below(), this);
        float volume = sound.getVolume();

        // O vanilla vai de ~0.5 (la, neve) a 1.0 (pedra, madeira). Aqui interessa o
        // contrario do que o numero diz: o que importa nao e o volume que o bloco toca, e
        // o quanto ele DENUNCIA quem pisa. Folha e cascalho sao os piores, e os dois tem
        // volume alto no vanilla, entao o mapeamento e direto.
        return Mth.clamp((volume - 0.4f) / 0.6f, 0.0f, 1.0f);
    }

    /** O quanto ele desacelera por causa do chao. Lido pelo goal, mandado pelo DATA. */
    public float paceFactor() {
        return 1.0f - 0.45f * this.entityData.get(DATA_GROUND);
    }

    // ------------------------------------------------------------------ leitura do cliente

    public byte posture() {
        return this.entityData.get(DATA_POSTURE);
    }

    public float alertClient() {
        return this.entityData.get(DATA_ALERT);
    }

    public float groundNoiseClient() {
        return this.entityData.get(DATA_GROUND);
    }

    /** Travado, ele e uma estatua — nem o passo escorrega. */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.freeze > 0;
    }

    // ------------------------------------------------------------------ som

    /**
     * ⚠️ ELE NAO TEM SOM AMBIENTE, e isso e uma decisao, nao um esquecimento. Toda criatura
     * do mod tem um rangido que denuncia onde ela esta; esta nao pode ter, porque o produto
     * dela e o SILENCIO — e um bicho que se anuncia sozinho devolveria ao jogador
     * exatamente a informacao que a criatura existe para tirar dele. O unico som que ele
     * faz e o estalo de quando OUVE, e esse fala do jogador, nao dele.
     */
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SCULK_BLOCK_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SCULK_SHRIEKER_BREAK;
    }

    /** Passo abafado: ele pisa como quem nao quer ser ouvido. */
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.BONE_BLOCK_STEP, 0.10F, 0.6F);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }
}
