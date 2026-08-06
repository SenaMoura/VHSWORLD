package net.vhsworld.rec.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * O OUVIDO DO ESCUTADOR — o unico sentido que ele tem.
 *
 * ⚠️ POR QUE ELE ESCUTA GameEvent DO VANILLA EM VEZ DE UM SISTEMA PROPRIO. A tentacao era
 * inventar a nossa lista de "coisas que fazem barulho", e ela estaria errada por um motivo
 * que nao tem conserto depois: <b>o jogador ja sabe esta regra</b>. O sensor de sculk e o
 * Warden ensinaram Minecraft inteiro que quebrar bloco faz barulho, que correr faz barulho
 * e que <i>agachar nao faz</i>. Uma regra propria seria uma segunda regra parecida com a
 * primeira mas diferente nos detalhes — que e a pior coisa que se pode fazer com uma
 * mecanica cujo valor inteiro esta em ser APRENDIVEL sem tutorial.
 *
 * E a peca que faz o contra-jogo existir de graca: `Player.getMovementEmission()` devolve
 * NONE quando o jogador esta agachado no chao. Nao ha uma linha nossa fazendo o silencio
 * funcionar — ele ja funciona, e o jogador ja desconfia que funciona.
 *
 * ⚠️ O QUE E NOSSO E O VOLUME. O vanilla so diz que o evento aconteceu; quanto ele PESA e
 * decisao de design, e e por isso que existe a tabela aqui embaixo: quebrar um bloco tem
 * que valer muito mais que dar um passo, senao o jogador nunca sente que escolheu nada.
 */
public final class Ears implements GameEventListener {

    /**
     * O que cada coisa vale, de 0 a 1.
     *
     * ⚠️ ISTO E A INTERFACE DA CRIATURA, e nao uma tabela de ajuste. O jogador vai
     * construir o modelo mental dele a partir destes numeros, entao eles tem que bater com
     * a intuicao dele sobre o que e "alto": minerar e a coisa mais barulhenta que uma
     * pessoa faz em Minecraft, e passo e o piso do que da para evitar sem parar de jogar.
     * Numero que contraria a intuicao vira "o bicho e aleatorio".
     */
    private static final Map<GameEvent, Float> LOUDNESS = new IdentityHashMap<>();

    static {
        // O estrago: mineracao, explosao, tiro. O que o jogador FAZ com o mundo.
        put(GameEvent.BLOCK_DESTROY, 1.00f);
        put(GameEvent.EXPLODE, 1.00f);
        put(GameEvent.LIGHTNING_STRIKE, 1.00f);
        put(GameEvent.PROJECTILE_LAND, 0.85f);
        put(GameEvent.PROJECTILE_SHOOT, 0.70f);
        put(GameEvent.BLOCK_PLACE, 0.70f);
        put(GameEvent.PRIME_FUSE, 0.80f);
        put(GameEvent.ENTITY_DAMAGE, 0.80f);
        put(GameEvent.ENTITY_DIE, 0.80f);

        // Barulho de instrumento: alto de proposito. Tocar jukebox perto dele e um erro
        // que o jogador comete UMA vez, e essa e a melhor aula que a criatura pode dar.
        put(GameEvent.INSTRUMENT_PLAY, 1.00f);
        put(GameEvent.JUKEBOX_PLAY, 1.00f);
        put(GameEvent.NOTE_BLOCK_PLAY, 0.90f);

        // Mexer em coisa: porta, bau, alavanca. O barulho da BASE — e por isso a base
        // deixa de ser lugar seguro sem que ninguem tenha dito isso.
        put(GameEvent.BLOCK_OPEN, 0.60f);
        put(GameEvent.BLOCK_CLOSE, 0.55f);
        put(GameEvent.CONTAINER_OPEN, 0.60f);
        put(GameEvent.CONTAINER_CLOSE, 0.55f);
        put(GameEvent.BLOCK_ACTIVATE, 0.55f);
        put(GameEvent.BLOCK_CHANGE, 0.45f);

        // Agua. Cair na agua e escandaloso; nadar, so meio.
        put(GameEvent.SPLASH, 0.75f);
        put(GameEvent.SWIM, 0.40f);

        // O corpo: passo, queda, comer, vestir. O piso do que se pode evitar.
        put(GameEvent.STEP, 0.35f);
        put(GameEvent.HIT_GROUND, 0.55f);
        put(GameEvent.EAT, 0.30f);
        put(GameEvent.DRINK, 0.30f);
        put(GameEvent.EQUIP, 0.25f);
        put(GameEvent.SHEAR, 0.50f);
        put(GameEvent.ITEM_INTERACT_FINISH, 0.35f);

        // ⚠️ FLAP fica de fora de proposito, junto com TELEPORT e ENTITY_PLACE: sao
        // eventos que o MUNDO produz o tempo todo (morcego, galinha, mob nascendo) e nao
        // dizem nada sobre onde o jogador esta. Criatura que corre atras de galinha nao
        // assusta ninguem — ela vira fauna, que e o defeito que este elenco inteiro tenta
        // evitar.
    }

    private static void put(GameEvent event, float loudness) {
        LOUDNESS.put(event, loudness);
    }

    /** Quanto isto vale para ele, ou 0 se nao e coisa que ele escute. */
    public static float loudnessOf(GameEvent event) {
        return LOUDNESS.getOrDefault(event, 0.0f);
    }

    // ------------------------------------------------------------------ o listener

    private final ListenerEntity owner;
    private final PositionSource source;
    private final int radius;

    public Ears(ListenerEntity owner, int radius) {
        this.owner = owner;
        // A meia altura dele: o ouvido nao fica no pe nem no teto. Muda pouco no alcance
        // e muda a leitura de quem sobe ou desce em relacao a ele.
        this.source = new EntityPositionSource(owner, owner.getEyeHeight());
        this.radius = radius;
    }

    @Override
    public PositionSource getListenerSource() {
        return this.source;
    }

    @Override
    public int getListenerRadius() {
        return this.radius;
    }

    /**
     * Chegou um som.
     *
     * ⚠️ ELE NAO OUVE A SI MESMO, e isto nao e detalhe: ele anda, e andar emite STEP. Sem
     * este filtro a criatura persegue o proprio passo — e o sintoma no jogo seria um bicho
     * andando em circulos, que ninguem leria como "bug de audicao".
     *
     * ⚠️ E ELE NAO OUVE OS OUTROS DA ESPECIE pelo mesmo motivo, elevado: dois Escutadores
     * perto viram um la-o-la eterno, cada um correndo atras do passo do outro, e o jogador
     * assiste a dois bichos se caçando. A criatura tem que ser sobre VOCE.
     */
    @Override
    public boolean handleGameEvent(ServerLevel level, GameEvent event, GameEvent.Context context, Vec3 pos) {
        if (!this.owner.isAlive()) return false;

        Entity source = context.sourceEntity();
        if (source == this.owner) return false;
        if (source instanceof ListenerEntity) return false;

        float loudness = loudnessOf(event);
        if (loudness <= 0.0f) return false;

        // ⚠️ O SOM PERDE FORCA COM A DISTANCIA, e e essa conta que faz "longe" existir.
        // Sem ela, um passo a quarenta blocos valeria o mesmo que um martelo do lado, e a
        // criatura sempre saberia tudo — ou seja, o jogador nunca teria escolha nenhuma
        // alem de nao se mexer.
        double distance = Math.sqrt(pos.distanceToSqr(this.owner.position()));
        float reach = 1.0f - (float) (distance / this.radius);
        if (reach <= 0.0f) return false;

        float strength = loudness * reach;
        if (strength < 0.08f) return false;

        this.owner.hear(pos, strength, BlockPos.containing(pos));
        return true;
    }
}
