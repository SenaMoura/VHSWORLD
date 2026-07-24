package net.vhsworld.rec.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;
import org.joml.Vector2ic;

import java.util.List;
import java.util.Locale;

/**
 * A caixinha que aparece quando o mouse para em cima de um item.
 *
 * O jogo desenha uma caixa arredondada de borda roxa — a unica coisa na tela
 * ainda com cara de Minecraft depois que o mod refez menu, loading e selecao de
 * mundo. Aqui ela vira um quadro de camera, com uma FICHA no topo:
 *
 *     +--------------------------------+
 *     | [icone]  Nome do item          |
 *     |          RARIDADE              |
 *     |--------------------------------|   <- regua vermelha
 *     | descricao...                   |
 *     |                                |
 *     |                       VHSWORLD |   <- so nos itens do mod
 *     +--------------------------------+
 *
 * O CABECALHO E MONTADO A MAO, e nao herdado da lista de linhas do jogo: para o
 * icone caber ao lado do nome, o nome precisa sair do fluxo normal e ser
 * desenhado numa coluna propria. As linhas restantes (a descricao, o que outros
 * mods acrescentaram) continuam intactas embaixo — a gente troca a moldura e a
 * primeira linha, nunca o conteudo.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VHSTooltip {

    private static final int BG       = 0xF0080809;
    private static final int BORDER_L = 0xFFBFBFBF;
    private static final int BORDER_D = 0xFF4A4A4A;
    private static final int SCANLINE = 0x20000000;
    private static final int BRACKET  = 0xFFFFFFFF;
    private static final int RULE     = 0xFFB01818;
    private static final int TRACKING = 0x26FFFFFF;
    private static final int SLOT_BG  = 0xFF16161A;
    private static final int SLOT_EDGE= 0xFF5A5A62;
    private static final int FOOTER   = 0xFF7A6A6A;

    private static final int PAD = 4;
    private static final int BRACKET_LEN = 4;

    /** Lado do quadradinho do icone. */
    private static final int ICON = 16;
    private static final int ICON_GAP = 5;

    /**
     * Altura da faixa do cabecalho.
     *
     * A conta: o nome ocupa y+1..y+10 e a raridade y+11..y+20, entao o conteudo
     * termina em +20 (mais fundo que o icone, que acaba em +16). Sobram 3 para a
     * regua vermelha respirar. Com o valor curto de antes, a regua era desenhada
     * EM CIMA da palavra da raridade.
     */
    private static final int HEADER_H = 23;

    private static final int LINE = 10;

    @SubscribeEvent
    public static void onRenderTooltip(RenderTooltipEvent.Pre event) {
        if (!RECConfig.CLIENT.vhsTooltip.get()) return;

        List<ClientTooltipComponent> components = event.getComponents();
        if (components.isEmpty()) return;

        GuiGraphics graphics = event.getGraphics();
        Font font = event.getFont();
        ItemStack stack = event.getItemStack();

        // Tooltip sem item (texto solto de alguma tela) nao ganha ficha: nao ha
        // icone para mostrar nem raridade para ler.
        boolean header = !stack.isEmpty() && RECConfig.CLIENT.tooltipHeader.get();

        Component name = stack.isEmpty() ? Component.empty() : stack.getHoverName();
        String rarity = stack.isEmpty() ? "" : rarityLabel(stack);
        String source = ours(stack) ? "VHSWORLD" : null;

        // --- medidas -------------------------------------------------------
        int headerH = header ? HEADER_H : 0;
        int headerW = header
                ? ICON + ICON_GAP + Math.max(font.width(name), font.width(rarity))
                : 0;

        // Com ficha, a primeira linha (o nome) sai do fluxo e vira cabecalho.
        int firstBody = header ? 1 : 0;

        int bodyW = 0;
        int bodyH = 0;
        for (int i = firstBody; i < components.size(); i++) {
            bodyW = Math.max(bodyW, components.get(i).getWidth(font));
            bodyH += components.get(i).getHeight();
        }
        if (!header && components.size() == 1) bodyH -= 2;

        int footerH = source != null ? LINE : 0;
        int footerW = source != null ? font.width(source) : 0;

        int w = Math.max(Math.max(headerW, bodyW), footerW);
        int h = headerH + bodyH + footerH;

        ClientTooltipPositioner positioner = event.getTooltipPositioner();
        Vector2ic anchor = positioner.positionTooltip(
                event.getScreenWidth(), event.getScreenHeight(),
                event.getX(), event.getY(), w, h);

        final int x = anchor.x();
        final int y = anchor.y();
        final int boxW = w;
        final int boxH = h;
        final int ruleY = header ? y + headerH - 2 : -1;

        // --- desenho -------------------------------------------------------
        PoseStack pose = graphics.pose();
        pose.pushPose();

        // Z=400 e a altura em que o jogo desenha tooltip. O fundo vai dentro de
        // drawManaged porque isso o despeja na tela ANTES do texto — sem essa
        // ordem, fundo e texto ficam na mesma profundidade e o texto some.
        pose.translate(0.0F, 0.0F, 400.0F);
        graphics.drawManaged(() -> drawFrame(graphics, x, y, boxW, boxH, ruleY));

        int line = y;

        if (header) {
            drawIconSlot(graphics, x - 1, y - 1);
            graphics.renderItem(stack, x, y);

            int textX = x + ICON + ICON_GAP;
            graphics.drawString(font, name, textX, y + 1, rarityColor(stack), true);
            if (!rarity.isEmpty()) {
                graphics.drawString(font, rarity, textX, y + 1 + LINE, FOOTER, false);
            }
            line = y + headerH;
        }

        for (int i = firstBody; i < components.size(); i++) {
            ClientTooltipComponent c = components.get(i);
            c.renderText(font, x, line, pose.last().pose(), graphics.bufferSource());
            line += c.getHeight();
        }

        line = header ? y + headerH : y;
        for (int i = firstBody; i < components.size(); i++) {
            ClientTooltipComponent c = components.get(i);
            c.renderImage(font, x, line, graphics);
            line += c.getHeight();
        }

        if (source != null) {
            graphics.drawString(font, source, x + boxW - footerW, y + boxH - LINE + 1, FOOTER, false);
        }

        pose.popPose();
        event.setCanceled(true);
    }

    // ------------------------------------------------------------------ moldura

    private static void drawFrame(GuiGraphics g, int x, int y, int w, int h, int ruleY) {
        int x0 = x - PAD;
        int y0 = y - PAD;
        int x1 = x + w + PAD;
        int y1 = y + h + PAD;

        g.fill(x0, y0, x1, y1, BG);

        for (int sy = y0 + 1; sy < y1 - 1; sy += 2) {
            g.fill(x0 + 1, sy, x1 - 1, sy + 1, SCANLINE);
        }

        if (RECConfig.CLIENT.tooltipTracking.get()) {
            int span = (y1 - y0) + 24;
            int t = (int) ((System.currentTimeMillis() / 26L) % span) - 12;
            int ly = y0 + t;
            if (ly > y0 && ly < y1 - 1) {
                g.fill(x0 + 1, ly, x1 - 1, ly + 1, TRACKING);
            }
        }

        g.fill(x0, y0, x1, y0 + 1, BORDER_L);
        g.fill(x0, y0, x0 + 1, y1, BORDER_L);
        g.fill(x0, y1 - 1, x1, y1, BORDER_D);
        g.fill(x1 - 1, y0, x1, y1, BORDER_D);

        if (ruleY > 0) {
            g.fill(x0 + 2, ruleY, x1 - 2, ruleY + 1, RULE);
        }

        drawBrackets(g, x0, y0, x1, y1);
    }

    /** O encaixe do icone: fundo mais fundo que a caixa, com aresta clara. */
    private static void drawIconSlot(GuiGraphics g, int x, int y) {
        int s = ICON + 2;
        g.fill(x, y, x + s, y + s, SLOT_BG);
        g.fill(x, y, x + s, y + 1, SLOT_EDGE);
        g.fill(x, y, x + 1, y + s, SLOT_EDGE);
    }

    private static void drawBrackets(GuiGraphics g, int x0, int y0, int x1, int y1) {
        int n = BRACKET_LEN;

        g.fill(x0 - 1, y0 - 1, x0 + n, y0, BRACKET);
        g.fill(x0 - 1, y0 - 1, x0, y0 + n, BRACKET);

        g.fill(x1 - n, y0 - 1, x1 + 1, y0, BRACKET);
        g.fill(x1, y0 - 1, x1 + 1, y0 + n, BRACKET);

        g.fill(x0 - 1, y1, x0 + n, y1 + 1, BRACKET);
        g.fill(x0 - 1, y1 - n, x0, y1 + 1, BRACKET);

        g.fill(x1 - n, y1, x1 + 1, y1 + 1, BRACKET);
        g.fill(x1, y1 - n, x1 + 1, y1 + 1, BRACKET);
    }

    // ------------------------------------------------------------------ ficha

    private static boolean ours(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && RECMod.MOD_ID.equals(id.getNamespace());
    }

    /** COMMON vira "Common". Fica no lugar onde a referencia poe a raridade. */
    private static String rarityLabel(ItemStack stack) {
        String raw = stack.getRarity().name();
        return raw.charAt(0) + raw.substring(1).toLowerCase(Locale.ROOT);
    }

    private static int rarityColor(ItemStack stack) {
        ChatFormatting color = stack.getRarity().color;
        Integer value = color.getColor();
        return value != null ? value : 0xFFFFFF;
    }

    private VHSTooltip() {}
}
