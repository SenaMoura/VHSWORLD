package net.vhsworld.rec.client.difficulty;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.fx.InkTransition;
import net.vhsworld.rec.config.RECConfig;

/**
 * Quem abre a tela de dificuldade ao entrar no mundo.
 *
 * A tela sobe DEBAIXO da mancha, no instante em que ela termina de comer a imagem:
 * o jogador nunca ve a troca acontecer, so a tela preta virando os dois cards. Por
 * isso o gatilho e o {@link InkTransition#covered()}, e nao um temporizador.
 *
 * Enquanto a tela estiver aberta, este gate segura o preto (hold) — a mancha so vai
 * se recolher quando o jogador escolher, e ai ela abre revelando o mundo.
 *
 * Se a mancha estiver desligada no config, cai no plano B: uma carencia curta depois
 * de entrar. A escolha nunca depende do efeito visual estar ligado.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DifficultyGate {

    /** Plano B, quando nao ha mancha: um segundo de mundo rodando antes de perguntar. */
    private static final int GRACE_TICKS = 20;

    private static int ticksInWorld;

    private DifficultyGate() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            ticksInWorld = 0;
            return;
        }
        ticksInWorld++;

        // Com a tela ja aberta: segura o preto e nao deixa a mancha abrir sozinha.
        if (mc.screen instanceof DifficultyScreen) {
            InkTransition.hold();
            return;
        }

        if (!RECConfig.CLIENT.difficultyPrompt.get()) return;
        if (DifficultyState.chosen()) return;
        if (mc.screen != null) return;

        boolean underInk = InkTransition.covered();
        boolean noInk = !InkTransition.running() && ticksInWorld > GRACE_TICKS;

        if (underInk || noInk) {
            InkTransition.hold();
            mc.setScreen(new DifficultyScreen());
        }
    }
}
