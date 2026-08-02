package net.vhsworld.rec.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * "Estao olhando para mim?" — a conta que quase toda criatura deste mod faz.
 *
 * O Homem de Pedra foi o primeiro a precisar dela e ficou com uma copia privada. Da
 * segunda leva em diante sao quatro bichos usando a MESMA pergunta com respostas
 * diferentes (um congela, um se apaga, um so anda no escuro, um so avanca pelas
 * costas), entao ela virou um lugar so.
 *
 * Tres cuidados que valem para todos:
 *
 * - Parede corta o olhar. Sem `hasLineOfSight`, olhar na direcao de um bicho atras de
 *   uma montanha valeria como encarar, e a regra viraria loteria.
 * - O alcance tem teto. Sem ele, um jogador do outro lado do mapa apontado por acaso
 *   na direcao certa prende a criatura para sempre.
 * - Isto e conta de SERVIDOR. Quem calcula e o servidor e manda por SynchedEntityData;
 *   calcular no cliente daria, em multiplayer, bicho parado para um jogador e andando
 *   para outro — e nenhum dos dois estaria vendo a verdade.
 */
public final class Gaze {

    /** ~114 graus: "esta no seu campo de visao". O cone do Homem de Pedra. */
    public static final double CONE_WIDE = 0.55D;

    /** ~46 graus: "esta olhando DIRETO para isto", nao so com o bicho na tela. */
    public static final double CONE_TIGHT = 0.92D;

    private Gaze() {}

    /** Este jogador esta com a entidade no cone e com linha livre? */
    public static boolean sees(Player player, Entity target, double range, double cone) {
        if (player.isSpectator() || !player.isAlive()) return false;
        if (player.distanceToSqr(target) > range * range) return false;

        Vec3 toTarget = new Vec3(target.getX() - player.getX(),
                target.getEyeY() - player.getEyeY(),
                target.getZ() - player.getZ());
        double length = toTarget.length();

        // Colado no olho: nao ha direcao definida, e "esta vendo" e a unica resposta
        // honesta — o bicho ocupa a tela inteira.
        if (length < 1.0E-4D) return true;

        return player.getViewVector(1.0F).normalize().dot(toTarget.scale(1.0D / length)) > cone
                && player.hasLineOfSight(target);
    }

    /** Algum jogador do mundo esta vendo? */
    public static boolean seenByAny(Entity target, double range, double cone) {
        return watcher(target, range, cone) != null;
    }

    /** O primeiro jogador que esta vendo, ou null. */
    public static Player watcher(Entity target, double range, double cone) {
        for (Player player : target.level().players()) {
            if (sees(player, target, range, cone)) return player;
        }
        return null;
    }

    /**
     * Nenhum jogador consegue ver ESTE PONTO agora?
     *
     * E a pergunta do teletransporte: para onde eu posso reaparecer sem que ninguem
     * me veja chegar. Aqui a linha de visao e testada contra o ponto, e nao contra
     * uma entidade, porque no momento da escolha a criatura ainda nao esta la.
     *
     * O `margin` sobe a mira meio corpo: mirar no pe de um bicho alto acha um ponto
     * "escondido" cuja cabeca aparece por cima do muro.
     */
    public static boolean hiddenFromAll(Level level, Vec3 spot, double range, double cone,
                                        double margin) {
        Vec3 aim = spot.add(0.0D, margin, 0.0D);

        for (Player player : level.players()) {
            if (player.isSpectator() || !player.isAlive()) continue;
            if (player.position().distanceToSqr(spot) > range * range) continue;

            Vec3 toSpot = aim.subtract(player.getEyePosition());
            double length = toSpot.length();
            if (length < 1.0E-4D) return false;

            boolean inCone = player.getViewVector(1.0F).normalize()
                    .dot(toSpot.scale(1.0D / length)) > cone;
            if (!inCone) continue;

            // Dentro do cone; so escapa se houver parede no caminho.
            if (level.clip(new net.minecraft.world.level.ClipContext(
                    player.getEyePosition(), aim,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    player)).getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                return false;
            }
        }
        return true;
    }
}
