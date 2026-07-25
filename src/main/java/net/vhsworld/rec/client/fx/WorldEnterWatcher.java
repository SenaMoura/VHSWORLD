package net.vhsworld.rec.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;

/**
 * Quem percebe que a tela de "loading terrain" acabou, e solta a mancha.
 *
 * Nao existe evento para "o mundo terminou de carregar". O que existe e a
 * {@link ReceivingLevelScreen} — a tela de terreno do proprio jogo. Entao a regra e
 * de borda: no tick em que ela ESTAVA aberta e deixou de estar, a mancha comeca.
 *
 * Assim a transicao nao depende de temporizador nenhum: ela nasce exatamente quando
 * o jogo diz que acabou de carregar, seja em 1 segundo ou em 40.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class WorldEnterWatcher {

    private static boolean wasLoading;

    private WorldEnterWatcher() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        boolean loading = mc.screen instanceof ReceivingLevelScreen;

        if (wasLoading && !loading && mc.level != null) {
            InkTransition.consume();
        }
        wasLoading = loading;

        InkTransition.tick();
    }

    /** Sair do mundo limpa tudo, senao o proximo mundo herdaria a mancha pela metade. */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        wasLoading = false;
        InkTransition.reset();
    }
}
