package net.vhsworld.rec.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * IR ATE O BARULHO — o unico movimento que o Escutador tem.
 *
 * ⚠️ ELE PERSEGUE UM LUGAR, NAO UMA CRIATURA, e a diferenca e a mecanica. Um goal de alvo
 * corrige a rota a cada tick porque sabe onde o alvo esta; este aqui vai para uma coordenada
 * velha e chega la sozinho, mesmo que voce ja tenha saido de perto. E o que faz o silencio
 * ser saida: quem para de fazer barulho vira um lugar no passado.
 */
public class ListenerSeekGoal extends Goal {

    private final ListenerEntity owner;

    /** De quanto em quanto tempo se recalcula o caminho. */
    private static final int REPATH = 10;

    private int repath;

    public ListenerSeekGoal(ListenerEntity owner) {
        this.owner = owner;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.owner.hasSound();
    }

    @Override
    public boolean canContinueToUse() {
        return this.owner.hasSound();
    }

    @Override
    public void start() {
        this.repath = 0;
    }

    @Override
    public void stop() {
        this.owner.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Vec3 target = this.owner.soundPos();
        if (target == null) return;

        this.owner.getLookControl().setLookAt(target.x, target.y, target.z);

        // ⚠️ CHEGOU E NAO ACHOU NADA: esquece e volta a tatear. Sem isto ele ficaria
        // pisoteando a coordenada para sempre, e o jogador que ficou quieto veria o bicho
        // travado num ponto — que le como defeito, e nao como "ele te perdeu".
        if (this.owner.distanceToSqr(target) < 2.25D) {
            this.owner.arrive();
            return;
        }

        if (--this.repath > 0) return;
        this.repath = REPATH;

        // A velocidade sai de duas coisas, e as duas sao legiveis de fora: o chao que
        // denuncia o passo dele (paceFactor) e o quanto o barulho que ele ouviu foi
        // grande. Barulho pequeno vira uma aproximacao lenta, quase curiosa; quebrar
        // pedra a vinte blocos traz a coisa em passada inteira.
        double speed = this.owner.paceFactor() * (0.65D + 0.55D * this.owner.alert());

        this.owner.getNavigation().moveTo(target.x, target.y, target.z, speed);
    }
}
