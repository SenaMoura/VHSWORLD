package net.vhsworld.rec.director;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;

/**
 * A PORTA DO SPAWN: nenhuma criatura nossa nasce sem o Diretor deixar.
 *
 * ⚠️ POR QUE ISTO E UM EVENTO E NAO UMA MUDANCA NOS biome_modifier. O spawn das nossas
 * criaturas e vanilla puro — `add_spawns` num biome_modifier, o jogo sorteia um ponto
 * valido e poe o bicho la. Peso e raridade dao para ajustar no JSON, mas o JSON nao tem
 * como saber a UNICA coisa que importa aqui: se ja tem alguma coisa nossa em cima do
 * jogador neste minuto. Essa pergunta so existe em tempo de jogo, entao a trava tem que
 * ser aqui.
 *
 * E o que corrige o empilhamento. Antes, tres criaturas podiam nascer em volta do mesmo
 * jogador em vinte segundos porque nenhuma das tres sabia das outras — e tres monstros
 * ao mesmo tempo nao e o triplo do medo, e o fim dele: vira briga, e briga o jogador
 * sabe resolver.
 *
 * ⚠️ O QUE ESTA PORTA NAO TOCA: ovo, comando, spawner e qualquer invocacao proposital.
 * Se ela travasse esses, testar o mod viraria adivinhacao — o Pedro poe o ovo no chao,
 * nao nasce nada, e nao ha como saber se o bug e da criatura ou do Diretor. Invocacao
 * pedida sempre acontece.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID)
public final class SpawnGate {

    private SpawnGate() {}

    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    /** Ninguem por perto neste raio = ninguem para assustar. */
    private static final double AUDIENCE = 128.0D;

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!RECConfig.COMMON.director.get()) return;

        // So o que o mundo faz por conta propria.
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION) return;

        Mob mob = event.getEntity();
        if (!Director.isOurs(mob)) return;

        // ⚠️ `true` no ultimo argumento inclui quem esta em criativo. Com `false`, testar
        // em criativo faria o Diretor achar que o mundo esta vazio e negar tudo — o mod
        // pareceria quebrado exatamente para quem esta olhando.
        Player nearest = event.getLevel().getNearestPlayer(
                event.getX(), event.getY(), event.getZ(), AUDIENCE, true);

        if (!(nearest instanceof ServerPlayer player)) {
            event.setSpawnCancelled(true);
            return;
        }

        if (!Director.allow(player, Beat.SPAWN)) {
            event.setSpawnCancelled(true);
            return;
        }

        // ⚠️ A PRESSAO E COBRADA POR DISTANCIA. Um bicho que nasce a 114 blocos nao pode
        // deixar ninguem tenso — ver Director.report(…, costScale) para a historia inteira.
        double distance = Math.sqrt(mob.distanceToSqr(player));
        float perceived = Director.perceived(distance);

        Director.report(player, Beat.SPAWN, perceived);

        // ⚠️ Sem esta linha, a batida mais pesada do mod era a unica invisivel no log — e
        // foi justamente ela que estava sufocando as outras duas. O defeito so apareceu
        // porque os tres relogios zeravam juntos no pulso; se o spawn tivesse voz propria,
        // a leitura teria sido imediata.
        LOG.info("[DIRETOR] SPAWN {} a {} blocos de {} (pressao x{})",
                net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()),
                String.format("%.0f", distance),
                player.getGameProfile().getName(),
                String.format("%.2f", perceived));
    }
}
