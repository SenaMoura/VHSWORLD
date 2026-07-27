package net.vhsworld.rec.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.entity.StonemanEntity;

/**
 * O aviso de que o Homem de Pedra quebrou a propria regra.
 *
 * A regra dele e a coisa mais importante que o jogador aprende sobre o mod: olhar
 * prende. Quando ela falha por dois segundos, o jogo TEM que dizer — senao a morte
 * parece defeito, e defeito nao assusta, irrita. Entao o surto chega por tres
 * caminhos ao mesmo tempo: a pedra rachando (som, do servidor, para todo mundo em
 * volta), a tela tremendo e o chiado subindo por cima da imagem.
 *
 * COMO O CLIENTE SABE: pela bandeira `isSurging()`, que a entidade sincroniza
 * sozinha. O mod nao tem canal de rede proprio, e esta e a razao de o estado do
 * surto morar em SynchedEntityData e nao numa variavel do servidor.
 *
 * A intensidade cai com a distancia. Quem esta colado leva o tranco inteiro; quem
 * ouviu de longe sente so a fita tremer — informacao suficiente para virar a cabeca,
 * que e exatamente o que a gente quer que ele faca.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StonemanSurgeFx {

    private StonemanSurgeFx() {}

    /** Alem disto o surto nao chega na tela. */
    private static final double RANGE = 24.0D;

    /** O tranco inicial, em ticks. Meio segundo: e um susto, nao um terremoto. */
    private static final int PUNCH_TICKS = 10;

    private static float intensity;   // 0..1, pela distancia do surto mais perto
    private static int punchTicks;    // o pico do comeco, morrendo
    private static boolean surging;   // ainda tem alguem em surto por perto?
    private static boolean wasSurging;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            reset();
            return;
        }
        if (mc.isPaused()) return;

        float found = scan(mc);
        boolean now = found > 0.0f;

        // O tranco e so na BORDA: entrou em surto agora. Enquanto dura fica o tremor
        // baixo, senao a tela sacode dois segundos inteiros e o jogador nao consegue
        // fazer a unica coisa que importa — achar o bicho com os olhos.
        if (now && !wasSurging) punchTicks = PUNCH_TICKS;

        wasSurging = now;
        surging = now;
        intensity = found;

        if (punchTicks > 0) punchTicks--;
    }

    /** O surto mais perto vira intensidade; sem surto por perto, zero. */
    private static float scan(Minecraft mc) {
        AABB box = mc.player.getBoundingBox().inflate(RANGE);
        float best = 0.0f;

        for (StonemanEntity stone : mc.level.getEntitiesOfClass(StonemanEntity.class, box,
                StonemanEntity::isSurging)) {
            double dist = Math.sqrt(stone.distanceToSqr(mc.player));
            if (dist > RANGE) continue;

            float near = (float) (1.0D - dist / RANGE);
            if (near > best) best = near;
        }
        return best;
    }

    /**
     * Amplitude do tremor agora.
     *
     * Soma o pico do comeco (quadratico, morre rapido) com um fundo constante que
     * dura o surto inteiro: a tela nunca fica quieta enquanto ele estiver solto.
     */
    public static float shakeAmount() {
        if (intensity <= 0.0f) return 0.0f;
        if (!RECConfig.CLIENT.stonemanSurgeShake.get()) return 0.0f;

        float punch = punchTicks > 0 ? (float) punchTicks / PUNCH_TICKS : 0.0f;
        float floor = surging ? 0.25f : 0.0f;

        return (punch * punch + floor) * intensity;
    }

    /** Quanto chiado o surto joga por cima da imagem (0 = nenhum). */
    public static double staticAmount() {
        if (!surging || intensity <= 0.0f) return 0.0D;
        if (!RECConfig.CLIENT.stonemanSurgeStatic.get()) return 0.0D;

        return 0.20D + intensity * 0.35D;
    }

    /** Sair do mundo apaga tudo: ninguem volta ao menu com a tela tremendo. */
    public static void reset() {
        intensity = 0.0f;
        punchTicks = 0;
        surging = false;
        wasSurging = false;
    }
}
