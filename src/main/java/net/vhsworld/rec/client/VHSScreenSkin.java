package net.vhsworld.rec.client;

import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Veste telas do vanilla com a cara do mod, sem herdar delas.
 *
 * POR QUE ASSIM: as versoes anteriores de "criar mundo" e "multiplayer" estendiam a
 * tela do vanilla e tentavam esconder o que ela desenhava. Nao da: a tela original
 * continua desenhando e o resultado sao textos empilhados uns sobre os outros.
 *
 * Aqui a gente nao briga com a tela — a gente entra nos ganchos dela:
 *   Init.Post          -> tira a lista do fundo do vanilla e remonta os botoes
 *   BackgroundRendered -> pinta a nossa fita DEPOIS do fundo e ANTES dos widgets
 *
 * A logica da tela continua sendo a do jogo. Criar mundo continua criando mundo,
 * com todas as opcoes; a gente so troca a roupa. E isso vale para qualquer tela
 * nova que a gente queira vestir: basta acrescentar na lista de alvos.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VHSScreenSkin {

    /** Onde a lista de conteudo pode comecar, para nao passar por baixo do nosso HUD. */
    private static final int CONTENT_TOP = 78;

    /** Altura reservada embaixo para a fileira de botoes. */
    private static final int FOOTER = 62;

    /** Botoes trocados por tela, para manter o "ativo" em dia com o original. */
    private static final WeakHashMap<Screen, List<Swap>> SWAPS = new WeakHashMap<>();

    private record Swap(Button original, VHSButton proxy) {}

    private static String labelFor(Screen screen) {
        if (screen instanceof CreateWorldScreen) return "NEW TAPE — RECORDING SETUP";
        if (screen instanceof JoinMultiplayerScreen) return "SIGNAL SEARCH — MULTIPLAYER";
        return null;
    }

    // ------------------------------------------------------------------ layout

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (labelFor(screen) == null) return;

        try {
            List<Swap> swaps = new ArrayList<>();
            List<Button> footerButtons = new ArrayList<>();

            for (var child : screen.children()) {
                // A lista do vanilla desenha faixas de terra em cima e embaixo; some com elas
                if (child instanceof AbstractSelectionList<?> list) {
                    list.setRenderBackground(false);
                    list.setRenderTopAndBottom(false);
                    list.updateSize(screen.width, screen.height, CONTENT_TOP, screen.height - FOOTER);
                }

                // So a fileira de baixo e remontada; abas e campos ficam onde estao
                if (child instanceof Button button && button.getY() >= screen.height - FOOTER) {
                    footerButtons.add(button);
                }
            }

            if (footerButtons.isEmpty()) return;

            int n = footerButtons.size();
            int gap = 6;
            int available = screen.width - 40 - gap * (n - 1);
            int w = Math.max(70, Math.min(140, available / n));
            int totalW = w * n + gap * (n - 1);
            int x = (screen.width - totalW) / 2;
            int y = screen.height - 46;

            for (int i = 0; i < n; i++) {
                Button original = footerButtons.get(i);
                event.removeListener(original);

                VHSButton proxy = new VHSButton(x + i * (w + gap), y, w, 22,
                        original.getMessage(), b -> original.onPress());
                proxy.active = original.active;

                event.addListener(proxy);
                swaps.add(new Swap(original, proxy));
            }

            SWAPS.put(screen, swaps);
        } catch (Throwable ignored) {
            // Se a tela mudar de forma numa versao futura, ela volta a ser a do vanilla
            // em vez de quebrar. Feia, mas funcionando.
        }
    }

    // ------------------------------------------------------------------ pintura

    @SubscribeEvent
    public static void onBackground(ScreenEvent.BackgroundRendered event) {
        Screen screen = event.getScreen();
        String label = labelFor(screen);
        if (label == null) return;

        VHSScreenHelper.renderVHSBackground(event.getGuiGraphics(), screen.width, screen.height, label);
    }

    /**
     * Mantem o botao trocado com o mesmo estado do original.
     *
     * O vanilla liga e desliga os botoes dele conforme a selecao muda — se a gente
     * nao acompanhar, o jogador clica em REPRODUZIR sem nada selecionado.
     */
    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Pre event) {
        List<Swap> swaps = SWAPS.get(event.getScreen());
        if (swaps == null) return;

        for (Swap swap : swaps) {
            swap.proxy().active = swap.original().active;
            swap.proxy().visible = swap.original().visible;
        }
    }

    private VHSScreenSkin() {}
}
