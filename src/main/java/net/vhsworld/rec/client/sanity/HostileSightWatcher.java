package net.vhsworld.rec.client.sanity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.vhsworld.rec.config.RECConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Faz a tela tremer quando um mob hostil entra no campo de visao.
 *
 * ISTO E ANDAIME. Serve para dar o que testar enquanto o mod nao tem criatura
 * propria: o susto de verdade vai vir da revelacao da foto, nao de olhar para um
 * slime. Por isso nao tira sanidade — so treme — e mora atras do knob
 * sanityShakeOnHostile, que sai de cena quando as entidades chegarem.
 */
public final class HostileSightWatcher {

    /** Alcance do olhar. Mais que isto e um ponto no horizonte, nao um encontro. */
    private static final double RANGE = 28.0;

    /** Cone de visao: ~53 graus para cada lado do centro da tela. */
    private static final double CONE = 0.6;

    /** Ticks ate o mesmo bicho poder assustar de novo. */
    private static final long COOLDOWN = 200;

    private static final Map<Integer, Long> lastScare = new HashMap<>();

    public static void tick(Minecraft mc) {
        if (!RECConfig.CLIENT.sanity.get()) return;
        if (!RECConfig.CLIENT.sanityShakeOnHostile.get()) return;
        if (mc.player == null || mc.level == null) return;

        long now = mc.level.getGameTime();

        // Uma varredura a cada meio segundo basta e nao pesa.
        if (now % 10 != 0) return;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Enemy)) continue;
            if (!entity.isAlive()) continue;

            Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
            Vec3 delta = center.subtract(eye);

            double distance = delta.length();
            if (distance < 0.5 || distance > RANGE) continue;
            if (delta.normalize().dot(look) < CONE) continue;

            Long last = lastScare.get(entity.getId());
            if (last != null && now - last < COOLDOWN) continue;

            if (!hasLineOfSight(mc, eye, center)) continue;

            lastScare.put(entity.getId(), now);
            SanityState.get().sighting(0.0f);   // treme, mas nao cobra: o preco e da foto
            return;                             // um susto por vez
        }

        // Limpeza: bicho que morreu ou saiu do mundo nao precisa de registro.
        if (now % 600 == 0) {
            lastScare.entrySet().removeIf(e -> now - e.getValue() > COOLDOWN * 4);
        }
    }

    private static boolean hasLineOfSight(Minecraft mc, Vec3 from, Vec3 to) {
        ClipContext context = new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player);
        return mc.level.clip(context).getType() == HitResult.Type.MISS;
    }

    private HostileSightWatcher() {}
}
