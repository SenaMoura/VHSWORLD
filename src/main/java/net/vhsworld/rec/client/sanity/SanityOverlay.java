package net.vhsworld.rec.client.sanity;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.CamcorderOverlay;
import net.vhsworld.rec.config.RECConfig;

/**
 * A barra de sanidade: cerebro e coluna vertical, na lateral esquerda.
 *
 * Fica colada na borda esquerda em vez de perto da hotbar de proposito — o HUD
 * da filmadora ja ocupa o alto da tela, e a sanidade nao e informacao de combate,
 * e um sinal vital. Ela mora sozinha.
 */
public final class SanityOverlay {

    private static final ResourceLocation BRAIN =
            new ResourceLocation(RECMod.MOD_ID, "textures/gui/sanity_brain.png");

    private static final int MARGIN_X = 12;
    private static final int ICON = 16;
    private static final int BAR_W = 6;
    private static final int BAR_H = 84;

    public static final IGuiOverlay SANITY_BAR = (gui, g, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!RECConfig.CLIENT.sanity.get() || !RECConfig.CLIENT.sanityBar.get()) return;

        // Durante o apagao a tela e preta e o mini-game manda; a barra sai da frente.
        if (CamcorderOverlay.isBatteryDead) return;

        SanityState state = SanityState.get();
        float fraction = state.fraction();

        int totalH = ICON + 4 + BAR_H;
        int top = (height - totalH) / 2;

        int iconX = MARGIN_X + (BAR_W - ICON) / 2;
        g.blit(BRAIN, iconX, top, 0.0f, 0.0f, ICON, ICON, ICON, ICON);

        int barX = MARGIN_X;
        int barY = top + ICON + 4;

        // Moldura + fundo
        g.fill(barX - 1, barY - 1, barX + BAR_W + 1, barY + BAR_H + 1, 0xFF000000);
        g.fill(barX, barY, barX + BAR_W, barY + BAR_H, 0xCC141418);

        // Preenchimento: cresce de baixo para cima, como um termometro
        int filled = Math.round(BAR_H * fraction);
        if (filled > 0) {
            g.fill(barX, barY + (BAR_H - filled), barX + BAR_W, barY + BAR_H, color(fraction, mc));
        }

        // Marcas de escala, para a barra nao virar um retangulo liso
        for (int i = 1; i < 4; i++) {
            int y = barY + (BAR_H * i) / 4;
            g.fill(barX, y, barX + BAR_W, y + 1, 0x55000000);
        }
    };

    /**
     * Verde palido enquanto ha juizo, amarelo no meio, vermelho no fim.
     * Abaixo de 25% a cor pulsa: e a barra pedindo socorro.
     */
    private static int color(float fraction, Minecraft mc) {
        if (fraction > 0.6f) return 0xFF6FBF7A;
        if (fraction > 0.25f) return 0xFFD2C05A;

        long time = mc.level.getGameTime();
        boolean bright = (time / 4) % 2 == 0;
        return bright ? 0xFFE05050 : 0xFF9A2A2A;
    }

    private SanityOverlay() {}
}
