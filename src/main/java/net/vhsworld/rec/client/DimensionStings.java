package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.item.ModSounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * O susto que pertence a ESTA dimensao.
 *
 * ============================ POR QUE NAO E MUSICA ============================
 *
 * A trilha que o Pedro montou vem em pastas por dimensao, e dentro de varias delas
 * ha faixas de 2 a 30 segundos misturadas com as de cinco minutos: um grito, um
 * rangido de metal, um chute de bumbo distorcido de 1,75s na parkourland.
 *
 * Elas nao podem entrar no pool de musica do bioma. O jogo sorteia UMA entrada do
 * evento cada vez que a trilha vai tocar; com delay 0, um chute de 1,75s tocaria e
 * o jogo sortearia de novo no mesmo segundo. Uma em cada tres "musicas" da
 * parkourland seria um bumbo — o que se ouviria e uma bateria travada, nao um
 * susto. Entao tudo abaixo de 40 segundos foi separado no build_sounds.py e vem
 * parar aqui.
 *
 * ============================ DIFERENCA PARA O AmbientDread ============================
 *
 * O AmbientDread e o clima do MOD: os seis DREAD_*, os mesmos em todo lugar, desde
 * o primeiro minuto. Este e o clima do LUGAR — o que toca na biblioteca nao toca no
 * shopping. Os dois somam de proposito, e e justamente por somarem que o intervalo
 * padrao daqui e mais longo (120–420s contra 90–300s): dois sorteios enchendo o
 * mesmo silencio viram barulho de fundo constante, e barulho constante e a unica
 * coisa que o ouvido aprende a ignorar.
 *
 * ⚠️ O POOL GERAL SEMPRE ENTRA. Alem da lista da dimensao, os cinco arquivos de
 * "Sons Gerais/random sounds effect" (passos distantes, sussurro, dois gritos, uma
 * transicao) tocam em toda parte, inclusive no overworld. Sao eles que garantem que
 * dimensao sem lista propria — a maioria — nao fique muda.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DimensionStings {

    private DimensionStings() {}

    private static final Random RANDOM = new Random();

    /**
     * Dimensao -> os sustos so dela.
     *
     * So aparecem aqui as dimensoes que TEM faixa curta; as outras caem no pool
     * geral e nao precisam de linha. A chave e o id da dimensao, e nao o do bioma,
     * porque no cliente a dimensao esta sempre a mao (`level.dimension()`) e o bioma
     * exigiria consultar o registro a cada tique.
     */
    private static final Map<ResourceLocation, RegistryObject<SoundEvent>> BY_DIMENSION = Map.of(
            dim("biblioteca"), ModSounds.STING_BIBLIOTECA,
            dim("chunks"), ModSounds.STING_CHUNKS,
            dim("mall"), ModSounds.STING_MALL,
            dim("parkourland"), ModSounds.STING_PARKOURLAND,
            dim("pipe_tunels"), ModSounds.STING_PIPE_TUNELS,
            dim("under_pressure"), ModSounds.STING_UNDER_PRESSURE);

    private static ResourceLocation dim(String path) {
        return ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, path);
    }

    /** Ticks ate o proximo. -1 = ainda nem sorteado (entrou no mundo agora). */
    private static int countdown = -1;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            countdown = -1;
            return;
        }
        if (mc.isPaused()) return;

        if (!RECConfig.CLIENT.dimensionStings.get() || !CameraState.audible()) {
            countdown = -1;
            return;
        }

        if (countdown < 0) {
            rearm();
            return;
        }
        if (--countdown > 0) return;

        play(mc);
        rearm();
    }

    private static void rearm() {
        int min = RECConfig.CLIENT.dimensionStingMinSeconds.get();
        int max = Math.max(min, RECConfig.CLIENT.dimensionStingMaxSeconds.get());
        countdown = (min + RANDOM.nextInt(max - min + 1)) * 20;
    }

    private static void play(Minecraft mc) {
        List<SoundEvent> pool = new ArrayList<>(2);
        pool.add(ModSounds.STING_GERAL.get());

        RegistryObject<SoundEvent> mine = BY_DIMENSION.get(mc.level.dimension().location());
        if (mine != null) {
            // Duas vezes: numa dimensao que tem som proprio, o som proprio deve ser
            // o que se ouve na maior parte das vezes. O pool geral esta ali para
            // nao deixar o lugar previsivel, nao para dominar.
            pool.add(mine.get());
            pool.add(mine.get());
        }

        SoundEvent sound = pool.get(RANDOM.nextInt(pool.size()));

        // Em volta do jogador, nunca em cima dele: som colado no ouvido soa como
        // efeito de menu, e o que assusta e vir de um canto do mundo.
        double angle = RANDOM.nextDouble() * Math.PI * 2.0;
        double distance = 8.0 + RANDOM.nextDouble() * 12.0;

        Vec3 at = mc.player.position().add(
                Math.cos(angle) * distance,
                RANDOM.nextDouble() * 4.0 - 1.5,
                Math.sin(angle) * distance);

        float volume = CameraState.volume(
                RECConfig.CLIENT.dimensionStingVolume.get().floatValue());

        mc.level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.AMBIENT,
                volume, 0.9f + RANDOM.nextFloat() * 0.2f, false);
    }
}
