package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.vhsworld.rec.config.RECConfig;

/**
 * A tinta de visao noturna enquanto a lente infravermelha esta na mao.
 *
 * So uma camada verde translucida com um clareado no centro e um vinheteado nas bordas,
 * desenhada por cima de tudo — o mesmo principio do resto do mod: efeito de camera vive na
 * camada de GUI, nunca no framebuffer do mundo, entao nao briga com shaderpack.
 */
public final class InfraredOverlay {

    public static final IGuiOverlay INFRARED = (gui, g, partialTick, width, height) -> {
        if (!GadgetState.infraredActive) return;
        if (!RECConfig.CLIENT.infraredTint.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Verde por cima da imagem toda.
        g.fill(0, 0, width, height, 0x3312FF4C);

        // Escurece as bordas (vinheta) para o olho ir para o centro.
        int band = Math.max(8, width / 12);
        g.fill(0, 0, band, height, 0x33000000);
        g.fill(width - band, 0, width, height, 0x33000000);
        g.fill(0, 0, width, band, 0x33000000);
        g.fill(0, height - band, width, height, 0x33000000);

        // Linhas de varredura verdes, marca de camera termica barata.
        for (int y = 0; y < height; y += 4) {
            g.fill(0, y, width, y + 1, 0x2200FF44);
        }
    };

    private InfraredOverlay() {}
}
