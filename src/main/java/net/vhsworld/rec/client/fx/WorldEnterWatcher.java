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
 * O relogio da mancha, e quem manda ela comecar.
 *
 * A mancha nasce assim que a tela de terreno do jogo APARECE — ela come o fundo de
 * terra na frente do jogador, e nao depois. Dai para a frente o preto se sustenta
 * ate o mundo estar pronto; quando a tela de terreno se fecha sozinha, a imagem por
 * baixo ja e outra e a troca nao tem costura.
 *
 * Enquanto a tela de terreno estiver de pe, este watcher SEGURA o preto. Sem isso a
 * mancha se recolheria em meio segundo e o fundo de terra voltaria a aparecer no meio
 * do carregamento.
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

        // Borda de subida: a tela de terreno acabou de abrir -> solta a mancha.
        if (loading && !wasLoading) {
            InkTransition.consume();
        }

        // Enquanto ela estiver de pe, o preto fica.
        if (loading) {
            InkTransition.hold();
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
