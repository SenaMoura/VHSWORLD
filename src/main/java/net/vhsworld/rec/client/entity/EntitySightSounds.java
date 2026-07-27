package net.vhsworld.rec.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.CameraState;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.entity.AnomalyEntity;
import net.vhsworld.rec.entity.StonemanEntity;
import net.vhsworld.rec.item.ModSounds;

import java.util.HashMap;
import java.util.Map;

/**
 * O som de VER uma coisa.
 *
 * Cada criatura tem o seu, e isso e a metade que importa: o jogador aprende a
 * associar aquele ruido AQUELA coisa, e a partir de certo ponto o som sozinho ja
 * carrega o medo — ele nao precisa nem ver de novo. Um susto generico para todas
 * seria so barulho.
 *
 * Toca UMA VEZ por encontro. Enquanto a criatura continua na tela, silencio; ela
 * so volta a soar se sair de vista e voltar (ou depois do descanso do config).
 * Sem isso, olhar fixo viraria uma metralhadora de sustos e o efeito morreria em
 * dez segundos.
 *
 * Client-side puro, e proposital: quem viu foi ESTE jogador, com estes olhos. Em
 * multiplayer o susto de um nao e o susto do outro.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntitySightSounds {

    private EntitySightSounds() {}

    /** Alem disto nao conta como "ver" — e um vulto, nao um encontro. */
    private static final double RANGE = 32.0D;

    /** Cosseno do meio-angulo do cone. 0.5 = ~120 graus, a largura confortavel da tela. */
    private static final double CONE = 0.5D;

    /** id da entidade -> tick em que ela pode assustar de novo. */
    private static final Map<Integer, Long> rest = new HashMap<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            rest.clear();
            return;
        }
        if (mc.isPaused()) return;
        if (!RECConfig.CLIENT.sightSounds.get() || !CameraState.audible()) return;

        long now = mc.level.getGameTime();
        rest.entrySet().removeIf(e -> e.getValue() < now);

        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();

        for (Entity entity : mc.level.entitiesForRendering()) {
            SoundEvent sound = soundFor(entity);
            if (sound == null) continue;
            if (rest.containsKey(entity.getId())) continue;

            Vec3 center = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            Vec3 delta = center.subtract(eye);

            double distance = delta.length();
            if (distance < 0.5D || distance > RANGE) continue;
            if (delta.normalize().dot(look) < CONE) continue;
            if (!mc.player.hasLineOfSight(entity)) continue;

            int seconds = RECConfig.CLIENT.sightSoundRest.get();
            rest.put(entity.getId(), now + seconds * 20L);

            // No proprio bicho, e nao na cabeca do jogador: da para virar o rosto
            // atras do som, que e o movimento que a gente quer provocar.
            mc.level.playLocalSound(center.x, center.y, center.z, sound,
                    SoundSource.HOSTILE, CameraState.volume(1.0f), 1.0f, false);
        }
    }

    /**
     * O som daquela criatura — ou null se ela nao tem um.
     *
     * ⚠️ A anomalia so soa se voce puder VE-LA. As de fita sao invisiveis a olho nu,
     * e um grito saindo do nada entregaria a posicao de uma coisa que o jogo esta
     * jurando que nao esta ali. O susto delas e na foto, nao no ouvido.
     */
    private static SoundEvent soundFor(Entity entity) {
        if (entity instanceof AnomalyEntity anomaly) {
            if (!AnomalyVision.canSee(anomaly.type())) return null;
            switch (anomaly.type()) {
                case TALL:     return ModSounds.SIGHT_TALL.get();
                case SPIDER:   return ModSounds.SIGHT_SPIDER.get();
                case CLAWS:    return ModSounds.SIGHT_CLAWS.get();
                // As duas esculturas caiam neste `default` e passavam MUDAS pela
                // frente do jogador — o pior defeito possivel numa criatura que a
                // gente renderizou de oito angulos.
                case GREYFACE: return ModSounds.SIGHT_GREYFACE.get();
                case OPHANIM:  return ModSounds.SIGHT_OPHANIM.get();
                default:       return null;
            }
        }

        // O Homem de Pedra soa quando voce o pega no olhar. O estalo de pedra que ele
        // ja dava e outra coisa: aquele e o som de ELE parar, e vem do servidor.
        if (entity instanceof StonemanEntity) {
            return ModSounds.SIGHT_STONEMAN.get();
        }
        return null;
    }
}
