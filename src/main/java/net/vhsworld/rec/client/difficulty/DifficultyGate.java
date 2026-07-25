package net.vhsworld.rec.client.difficulty;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;

/**
 * Quem abre a tela de dificuldade ao entrar no mundo.
 *
 * Nao da para abrir no evento de LoggingIn: naquele instante o mundo ainda esta
 * carregando e a pasta do save (que e a chave de onde a escolha fica guardada) pode
 * nem existir. Entao a checagem mora no tick, com uma carencia curta para o mundo
 * assentar, e so abre quando nao ha outra tela na frente.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DifficultyGate {

    /** Um segundo de mundo rodando antes de perguntar. */
    private static final int GRACE_TICKS = 20;

    private static int ticksInWorld;

    private DifficultyGate() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            ticksInWorld = 0;
            return;
        }

        if (ticksInWorld < GRACE_TICKS) {
            ticksInWorld++;
            return;
        }

        if (!RECConfig.CLIENT.difficultyPrompt.get()) return;
        if (DifficultyState.chosen()) return;
        if (mc.screen != null) return;

        mc.setScreen(new DifficultyScreen());
    }
}
