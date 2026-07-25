package net.vhsworld.rec.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;

/**
 * A mancha comendo a TELA DE CARREGAMENTO, ainda no fundo de terra.
 *
 * ⚠️ Por que desenhar por cima e nao trocar a tela: a {@link ReceivingLevelScreen} se
 * FECHA SOZINHA no proprio tick() — ela vigia se o chunk onde o jogador esta ja foi
 * compilado e so entao chama onClose(). Quem troca essa tela por outra assume essa
 * vigilancia, e errar ali significa cair num mundo que ainda nao terminou de montar,
 * ou nunca sair da tela. Entao nao se troca nada: a mancha entra como uma camada por
 * cima, no ScreenEvent.Render.Post, e o carregamento do jogo segue intocado.
 *
 * Enquanto esta tela estiver de pe, este handler tambem SEGURA o preto (hold), senao
 * a mancha se recolheria depois de meio segundo e o fundo de terra reapareceria.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LoadingScreenInk {

    private LoadingScreenInk() {}

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof ReceivingLevelScreen)) return;

        GuiGraphics g = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        int w = event.getScreen().width;
        int h = event.getScreen().height;

        InkTransition.renderOver(g, w, h, event.getPartialTick());

        // Coberto e ainda carregando: a tela nao pode parecer travada. Um chiado leve
        // e o carretel piscando dizem que a fita continua correndo.
        if (InkTransition.covered()) {
            VHSScreenStatic.draw(g, w, h, 0.35f);

            String text = Component.translatable("recmod.loading.tape").getString();
            int tw = mc.font.width(text);
            boolean on = (System.currentTimeMillis() / 420L) % 2L == 0L;
            g.drawString(mc.font, text, (w - tw) / 2, h / 2 - 4, on ? 0xFF8A8A8A : 0xFF3A3A3A, false);
        }
    }
}
