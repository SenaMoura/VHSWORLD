package net.vhsworld.rec.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.sanity.SanityState;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.entity.AnomalyEntity;
import net.vhsworld.rec.entity.AnomalyType;
import net.vhsworld.rec.entity.Judgement;
import net.vhsworld.rec.net.JudgementPacket;

/**
 * O QUE O OLHAR DO OFANIM FAZ COM A TELA — o aviso antes, e a conta depois.
 *
 * Sao duas coisas com origens diferentes, e por isso as duas moram aqui:
 *
 * 1. A PRESSAO, que vem sozinha pelo medidor sincronizado da propria criatura (nao
 *    precisa de pacote nenhum). Enquanto ele enche, o horizonte vai entortando de
 *    leve. E o unico aviso que o jogador tem, e ele e HONESTO: se a tela nao dissesse
 *    nada, o julgamento chegaria do nada e leria como castigo aleatorio. Assim, quem
 *    foi julgado tinha como saber — e escolheu continuar olhando.
 *
 * 2. A VERTIGEM do julgamento, que vem do servidor pelo JudgementPacket, porque quem
 *    decide quando e por quanto tempo e o config do mundo, e nao esta maquina.
 *
 * ⚠️ TUDO AQUI TEM TETO E DECAI SOZINHO. Nada fica ligado esperando um "desliga" que
 * pode nunca chegar: se o jogador sair do mundo no meio da vertigem, o contador morre
 * com o nivel (o tick zera tudo sem `mc.level`). Efeito de tela que depende de receber
 * o fim e o jeito classico de deixar a tela torta para sempre.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OphanimGazeFx {

    private OphanimGazeFx() {}

    /** Alem disto o medidor dele nao mexe mais na sua tela. */
    private static final double EARSHOT = 128.0D;

    /** O medidor do Ofanim mais perto, suavizado — 0 a 1. */
    private static float pressure;

    /** Ticks que ainda faltam da vertigem, e o total dela (para o envelope). */
    private static int vertigoTicks;
    private static int vertigoTotal;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            reset();
            return;
        }
        if (mc.isPaused()) return;

        if (vertigoTicks > 0) vertigoTicks--;

        float target = nearestGaze(mc);
        // Sobe e desce suavizado: o medidor do servidor chega em saltos (a entidade so
        // e sincronizada de tres em tres ticks), e a camera acompanhando salto a salto
        // trepidaria em vez de entortar.
        pressure += (target - pressure) * 0.12F;
        if (pressure < 0.001F) pressure = 0.0F;
    }

    /** O medidor do Ofanim mais perto que os olhos alcancam, ou 0. */
    private static float nearestGaze(Minecraft mc) {
        if (!RECConfig.CLIENT.ophanimVertigo.get()) return 0.0F;

        float best = 0.0F;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof AnomalyEntity anomaly)) continue;
            if (anomaly.type() != AnomalyType.OPHANIM || !anomaly.isAlive()) continue;
            // Se os olhos nao a alcancam, ela nao pode estar torcendo a sua tela: a
            // tela e a evidencia de que voce a esta vendo.
            if (!AnomalyVision.canSee(anomaly.type())) continue;
            if (anomaly.distanceTo(mc.player) > EARSHOT) continue;

            best = Math.max(best, anomaly.gaze());
        }
        return best;
    }

    /** Chamado pelo JudgementPacket, no cliente de quem foi julgado. */
    public static void judged(JudgementPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // A sanidade cobra pelo caminho do avistamento (treme e estala), porque e
        // exatamente disso que se trata: voce olhou, e o olhar cobrou.
        if (packet.sanity() > 0.0F) {
            SanityState.get().sighting(packet.sanity());
        }

        // A cegueira e o teleporte ja chegaram pelo mundo (efeito vanilla e mudanca de
        // posicao). O que so a tela pode fazer e perder o prumo — e nos tres estagios
        // ela perde, mudando so o quanto e por quanto tempo.
        if (RECConfig.CLIENT.ophanimVertigo.get()) {
            vertigoTotal = Math.max(1, packet.stage() == Judgement.VERTIGO
                    ? packet.ticks()
                    : Math.min(packet.ticks(), 60));
            vertigoTicks = vertigoTotal;
        }
    }

    /**
     * Quanto a camera esta torta agora.
     *
     * A conta soma duas fontes com sentidos opostos: a PRESSAO e um peso que cresce
     * devagar enquanto ele te olha (quadratica — quase nada na primeira metade do
     * medidor, e feia no fim), e a VERTIGEM e um estouro que decai. Uma avisa, a outra
     * cobra.
     */
    public static float tiltAmount() {
        float building = pressure * pressure * 0.45F;

        float hit = 0.0F;
        if (vertigoTicks > 0 && vertigoTotal > 0) {
            float envelope = (float) vertigoTicks / vertigoTotal;
            hit = envelope * envelope;
        }
        return (building + hit) * RECConfig.CLIENT.ophanimVertigoStrength.get().floatValue();
    }

    /**
     * Chiado por cima da imagem enquanto ele te encara.
     *
     * So no fim do medidor (acima de 60%): antes disso ele seria constante, e chiado
     * constante e enfeite. Aqui ele e o aviso de que o tempo esta acabando.
     */
    public static double staticAmount() {
        if (!RECConfig.CLIENT.ophanimGazeStatic.get()) return 0.0D;
        if (pressure < 0.6F) return 0.0D;
        return (pressure - 0.6F) / 0.4F * 0.5D;
    }

    public static void reset() {
        pressure = 0.0F;
        vertigoTicks = 0;
        vertigoTotal = 0;
    }
}
