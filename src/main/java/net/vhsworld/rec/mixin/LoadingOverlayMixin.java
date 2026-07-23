package net.vhsworld.rec.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.vhsworld.rec.client.VHSLoadingRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A tela de carregamento do VHSWORLD, por cima da tela da Mojang.
 *
 * POR QUE MIXIN: a LoadingOverlay nao tem evento nem API — e classe do proprio
 * Minecraft. E o mesmo caminho que FancyMenu e Drip Loading Screen usam.
 *
 * POR QUE NO FIM E NAO NO LUGAR: injetar no TAIL e desenhar por cima deixa toda a
 * logica original intacta — o controle do reload, o fade e a chamada que devolve o
 * jogo ao menu. Cancelar o render original obrigaria a reescrever essa maquinaria
 * aqui, e um erro nela trava o jogo na tela de loading para sempre. O jogador nao
 * ve a tela da Mojang porque a nossa e opaca; o jogo continua acreditando que ela
 * esta ali.
 */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {

    /** Progresso do reload, 0.0 a 1.0 — o mesmo numero que move a barra vanilla. */
    @Shadow
    private float currentProgress;

    /** Instante em que o fade de saida comecou, ou -1 enquanto ainda esta carregando. */
    @Shadow
    private long fadeOutStart;

    @Inject(method = "render", at = @At("TAIL"))
    private void recmod$drawTapeLoading(GuiGraphics graphics, int mouseX, int mouseY,
                                        float partialTick, CallbackInfo ci) {
        VHSLoadingRenderer.render(graphics, currentProgress, fadeOutStart);
    }
}
