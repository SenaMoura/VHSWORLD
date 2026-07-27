package net.vhsworld.rec.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.vhsworld.rec.config.RECConfig;

import java.util.EnumSet;

/**
 * O Homem de Pedra constroi para te alcancar.
 *
 * Subir numa torre de terra e a saida classica contra qualquer monstro do Minecraft —
 * e ela desmonta este bicho por completo, porque a regra dele (parar sob o olhar) fica
 * de graca para quem esta a salvo em cima. Entao ele empilha pedregulho e sobe atras,
 * e atravessa buraco pondo chao na frente dos proprios pes.
 *
 * O QUE ISSO FAZ COM O TERROR: ele so constroi quando ninguem esta olhando, porque
 * congelado nao roda IA nenhuma. Voce nunca ve a torre crescer. Voce olha, ele esta
 * parado dois blocos abaixo; voce pisca; ele esta parado na sua altura. E a pilha de
 * pedra fica la depois, na sua base, como prova de que nao foi impressao sua.
 *
 * PRIORIDADE 0 e de proposito: com a flag MOVE, o MeleeAttackGoal (prioridade 1) o
 * bloquearia sempre que houvesse alvo — e alvo sempre ha. Ele so ganha a vez quando
 * realmente empacou, entao a perseguicao normal continua sendo do goal de ataque.
 */
public class StonemanBuildGoal extends Goal {

    private final StonemanEntity mob;

    /** Longe demais nao vale a pena construir: ele desiste e volta a andar. */
    private static final double MAX_RANGE = 32.0D;

    /** Descanso depois de gastar a cota de blocos, para nao virar uma obra sem fim. */
    private static final int REST = 60;

    private int placed;    // blocos desta empreitada
    private int cooldown;  // entre um bloco e o proximo
    private int rest;      // entre uma empreitada e a proxima
    private double baseY;  // altura de onde ele pulou, para saber quando por a pedra
    private boolean climbing;

    public StonemanBuildGoal(StonemanEntity mob) {
        this.mob = mob;
        // JUMP junto com MOVE: a torre e feita pulando, e sem a flag o jumpControl
        // continuaria nas maos de outro goal no mesmo tick.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.rest > 0) {
            this.rest--;
            return false;
        }
        if (!allowed()) return false;

        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (this.mob.distanceToSqr(target) > MAX_RANGE * MAX_RANGE) return false;

        // Duas razoes para pegar na pedra: ele empacou, ou o alvo esta em cima de
        // alguma coisa. A segunda existe porque quem se refugia numa torre nao deixa
        // o bicho "preso" — ele fica andando em circulos embaixo, feliz da vida.
        return this.mob.isStuck() || targetAbove();
    }

    @Override
    public boolean canContinueToUse() {
        if (!allowed()) return false;
        if (this.placed >= RECConfig.COMMON.stonemanBuildMaxBlocks.get()) return false;

        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (this.mob.distanceToSqr(target) > MAX_RANGE * MAX_RANGE) return false;

        // Subindo, ele so para quando alcanca a altura do alvo. No chao, para assim
        // que destravar — dai o goal de ataque assume e ele volta a simplesmente vir.
        return this.climbing ? targetAbove() : this.mob.isStuck();
    }

    @Override
    public void start() {
        this.placed = 0;
        this.cooldown = 0;
        this.climbing = false;
        this.baseY = this.mob.getY();
    }

    @Override
    public void stop() {
        this.rest = REST;
        this.climbing = false;
        this.mob.clearStuck();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (this.cooldown > 0) this.cooldown--;

        if (targetAbove()) {
            pillar();
        } else {
            this.climbing = false;
            bridge(target);
        }
    }

    // ------------------------------------------------------------------ as duas obras

    /**
     * A torre: pula e poe a pedra no chao que acabou de deixar.
     *
     * A pedra so entra quando ele ja passou de um bloco de altura — colocar antes o
     * enfiaria dentro do proprio bloco. A navegacao e desligada no meio disso, senao
     * ela o puxa para o lado e ele cai da propria obra.
     */
    private void pillar() {
        Level level = this.mob.level();

        if (!this.climbing) {
            this.climbing = true;
            this.baseY = this.mob.getY();
        }

        this.mob.getNavigation().stop();

        if (this.mob.onGround()) {
            this.baseY = this.mob.getY();
            this.mob.getJumpControl().jump();
            return;
        }

        if (this.cooldown > 0) return;
        if (this.mob.getY() < this.baseY + 1.0D) return;   // ainda nao subiu o bastante

        BlockPos below = BlockPos.containing(this.mob.getX(), this.baseY, this.mob.getZ());
        place(level, below);
    }

    /**
     * A ponte: um bloco de chao na frente do proprio pe, buraco a buraco.
     *
     * So entra onde falta chao E falta o degrau — assim ele nao sai tapando o mundo
     * por onde passa, so o que o separa de voce.
     */
    private void bridge(LivingEntity target) {
        Level level = this.mob.level();
        if (this.cooldown > 0) return;

        Vec3 dir = target.position().subtract(this.mob.position());
        if (dir.horizontalDistanceSqr() < 1.0E-4D) return;
        dir = new Vec3(dir.x, 0.0D, dir.z).normalize();

        BlockPos ahead = BlockPos.containing(this.mob.getX() + dir.x,
                this.mob.getY(), this.mob.getZ() + dir.z);
        BlockPos floor = ahead.below();

        boolean gap = level.getBlockState(ahead).canBeReplaced()
                && level.getBlockState(floor).canBeReplaced();
        if (!gap) return;

        place(level, floor);
    }

    // ------------------------------------------------------------------ a pedra

    /** Poe um pedregulho, se couber e se nao houver ninguem no caminho. */
    private void place(Level level, BlockPos pos) {
        BlockState state = Blocks.COBBLESTONE.defaultBlockState();

        if (!level.getBlockState(pos).canBeReplaced()) return;
        // Ninguem e murado vivo: se tem gente ou bicho ocupando o espaco, a pedra
        // nao entra. Sufocar o jogador de dentro de um bloco nao e susto, e defeito.
        if (!level.isUnobstructed(state, pos, CollisionContext.empty())) return;

        level.setBlock(pos, state, 3);
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(this.mob, state));
        level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.HOSTILE, 1.0F, 0.6F);

        this.placed++;
        this.cooldown = RECConfig.COMMON.stonemanBuildCooldown.get();
    }

    /** O alvo esta acima e mais ou menos em cima dele? */
    private boolean targetAbove() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return false;

        double dy = target.getY() - this.mob.getY();
        if (dy < 1.6D) return false;

        double dx = target.getX() - this.mob.getX();
        double dz = target.getZ() - this.mob.getZ();
        return dx * dx + dz * dz <= 16.0D;   // 4 blocos de raio
    }

    /**
     * Construir e mexer no mundo alheio: fora do knob do mod, obedece tambem ao
     * mobGriefing, que e onde todo servidor ja diz se aceita mob mudando o cenario.
     */
    private boolean allowed() {
        return RECConfig.COMMON.stonemanBuilds.get()
                && this.mob.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }
}
