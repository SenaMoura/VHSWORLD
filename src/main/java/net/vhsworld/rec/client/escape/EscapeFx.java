package net.vhsworld.rec.client.escape;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.net.EscapeFxPacket;

import java.util.Random;

/**
 * O QUE A TELA FAZ quando a dimensao solta o jogador — ou quando ela o pega olhando.
 *
 * ⚠️ O CORTE EXISTE PARA ESCONDER UMA COSTURA, e nao para enfeitar. O `changeDimension` do
 * jogo troca o mundo num quadro so: o jogador ve o corredor da MAZE e, no quadro seguinte,
 * o quintal de casa. Sem nada por cima, uma fuga que custou atravessar a dimensao inteira
 * termina num pisca. O efeito e o que transforma esse pisca num FIM.
 *
 * Por isso ele comeca ANTES do teleporte (ver Escape.leave) e atravessa a troca de mundo:
 * o estado vive aqui no cliente, e nao no nivel, entao ele sobrevive ao mundo ser
 * substituido embaixo dele. Um efeito guardado no mundo antigo morreria junto com ele,
 * que e exatamente no meio do corte.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EscapeFx {

    private EscapeFx() {}

    private static final Random RANDOM = new Random();

    /**
     * Os rostos do susto.
     *
     * Sao os mesmos recortes que as anomalias 2D usam — ja matizados (fundo removido,
     * rosto claro sobre transparente), que e o trabalho mais chato do pipeline e ja
     * estava feito. Desenhados sobre preto, eles reproduzem exatamente as fotos que o
     * Pedro mandou.
     */
    private static final ResourceLocation[] FACES = {
            face("face_skull"), face("face_smile"), face("face_rings"), face("face_scream"),
    };

    private static ResourceLocation face(String name) {
        return ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "textures/gui/scare/" + name + ".png");
    }

    /** Quanto dura cada final, em tiques. */
    private static final int MIRROR_TICKS = 30;
    private static final int BROKEN_TICKS = 34;
    private static final int DOOR_TICKS = 36;

    /**
     * O susto e CURTO. Meio segundo.
     *
     * ⚠️ E o numero mais importante deste arquivo. Um rosto na tela por tres segundos deixa
     * de ser susto e vira uma imagem que o jogador ESTUDA — e uma imagem estudada nunca
     * mais assusta. Dez tiques e o tempo de reconhecer que havia um rosto sem ter tempo de
     * olhar para ele; e a diferenca entre lembrar do susto e lembrar da textura.
     */
    private static final int SCARE_TICKS = 10;

    private static EscapeFxPacket.Kind current;
    private static ResourceLocation currentFace;
    private static int left;
    private static int total = 1;

    /** Chamado pelo EscapeFxPacket. */
    public static void play(EscapeFxPacket.Kind kind) {
        current = kind;
        total = switch (kind) {
            case MIRROR_THROUGH -> MIRROR_TICKS;
            case MIRROR_BROKEN -> BROKEN_TICKS;
            case MIRROR_SCARE -> SCARE_TICKS;
            case DOOR_THROUGH -> DOOR_TICKS;
        };
        left = total;
        if (kind == EscapeFxPacket.Kind.MIRROR_SCARE) {
            // O rosto e sorteado A CADA susto, e nao fixado por espelho: o jogador que
            // insiste em olhar tem que levar uma cara diferente, senao o segundo susto ja
            // e previsivel.
            currentFace = FACES[RANDOM.nextInt(FACES.length)];

            // ⚠️ A SANIDADE CAI AQUI, no cliente, e nao no servidor — e isso e a
            // arquitetura do mod e nao um atalho. O medidor de sanidade do VHSWORLD e
            // client-side por desenho (ver SanityState: ele salva no proprio disco do
            // jogador). O servidor nao tem o numero para descontar dele. O que o servidor
            // decide e o que ele sabe: que este jogador esta encarando. O quanto isso
            // custa e conta de quem guarda o medidor.
            net.vhsworld.rec.client.sanity.SanityState.get().drain(SCARE_SANITY);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.playSound(net.vhsworld.rec.item.ModSounds.ENTITY_SCREAM.get(), 1.0F, 0.8F);
            }
        }
    }

    /**
     * Quanto de sanidade cada susto cobra.
     *
     * O medidor tem 100. Com 14 por susto e um susto a cada dois segundos de encarada, um
     * jogador cheio aguenta uns quinze segundos olhando antes de chegar ao fundo — e o
     * `MirrorDirector` o manda de volta ao inicio antes disso. Os dois relogios foram
     * calibrados juntos de proposito: o castigo tem que chegar ANTES de a sanidade zerar,
     * senao o jogador aprende que dava para encarar ate o fim e sobreviver.
     */
    private static final float SCARE_SANITY = 14.0f;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (left > 0) left--;
    }

    /**
     * A camada de cima de tudo.
     *
     * ⚠️ Desenha por cima do HUD inteiro, e nao por baixo. Durante o corte a barra de vida,
     * a mochila e o REC nao podem aparecer: a fita nao esta mais gravando, e o HUD e a
     * ultima coisa que denuncia que ainda ha um jogo rodando ali embaixo.
     */
    public static final IGuiOverlay OVERLAY = (gui, graphics, partialTick, width, height) -> {
        if (left <= 0 || current == null) return;
        float progress = 1.0f - (left / (float) total);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        switch (current) {
            case MIRROR_THROUGH -> mirrorThrough(graphics, width, height, progress);
            case MIRROR_BROKEN -> mirrorBroken(graphics, width, height, progress);
            case MIRROR_SCARE -> scare(graphics, width, height, progress);
            case DOOR_THROUGH -> doorThrough(graphics, width, height, progress);
        }

        RenderSystem.disableBlend();
    };

    /**
     * O SUSTO: preto na tela toda, e um rosto em cima.
     *
     * O preto e obrigatorio e nao e fundo — e o que APAGA o mundo. Um rosto desenhado por
     * cima do jogo continua sendo um adesivo na tela; com o mundo apagado atras, ele
     * ocupa o lugar do mundo, e por meio segundo nao existe mais nada.
     */
    private static void scare(GuiGraphics g, int w, int h, float p) {
        if (currentFace == null) return;
        g.fill(0, 0, w, h, argb(255, 0, 0, 0));

        // O rosto cresce um pouco durante o susto — nao o bastante para se ver crescendo,
        // o bastante para a imagem nunca ficar parada.
        float zoom = 0.92f + p * 0.16f;
        int tall = (int) (h * zoom);
        int wide = (int) (tall * 0.72f);
        int x = (w - wide) / 2;
        int y = (h - tall) / 2;
        g.blit(currentFace, x, y, 0, 0.0F, 0.0F, wide, tall, wide, tall);
    }

    /** MIRROR: o reflexo engole — fecha das bordas para o meio. */
    private static void mirrorThrough(GuiGraphics g, int w, int h, float p) {
        int inset = (int) (w * 0.5f * p);
        g.fill(0, 0, inset, h, argb(255, 0x05, 0x05, 0x08));
        g.fill(w - inset, 0, w, h, argb(255, 0x05, 0x05, 0x08));
        g.fill(0, 0, w, h, argb((int) (90 * p), 0x20, 0x28, 0x30));
    }

    /** Olhou demais: o vidro estala, e o mundo pula. Nao e saida — e o caminho de volta. */
    private static void mirrorBroken(GuiGraphics g, int w, int h, float p) {
        float fade = 1.0f - p;
        g.fill(0, 0, w, h, argb((int) (200 * fade), 0x00, 0x00, 0x00));
        for (int i = 0; i < 40; i++) {
            int x = RANDOM.nextInt(w);
            int y = RANDOM.nextInt(h);
            int len = 20 + RANDOM.nextInt(120);
            g.fill(x, y, Math.min(w, x + len), y + 1, argb((int) (220 * fade), 0xFF, 0xFF, 0xFF));
        }
    }

    /**
     * DOOR: a fita chega no fim do rolo.
     *
     * Fecha de CIMA E DE BAIXO ao mesmo tempo, e nao das bordas como o espelho. E a unica
     * forma de fechamento que cita um projetor em vez de um reflexo — e o que a TRAIN e a
     * PARKOURLAND pedem, porque o que acaba ali nao e o lugar, e o TRECHO.
     */
    private static void doorThrough(GuiGraphics g, int w, int h, float p) {
        int band = (int) (h * 0.5f * p);
        g.fill(0, 0, w, band, argb(255, 0x04, 0x04, 0x06));
        g.fill(0, h - band, w, h, argb(255, 0x04, 0x04, 0x06));
        if (p > 0.75f) {
            g.fill(0, h / 2 - 1, w, h / 2 + 1, argb((int) (255 * (p - 0.75f) * 4), 0xE8, 0xE4, 0xD8));
        }
    }

    private static int argb(int a, int r, int g, int b) {
        return (Math.min(255, Math.max(0, a)) << 24) | (r << 16) | (g << 8) | b;
    }
}
