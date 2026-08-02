package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.net.MusicPacket;

/**
 * QUEM TOCA A TRILHA — e, principalmente, quem NAO deixa mais ninguem tocar.
 *
 * ⚠️ AS DUAS METADES, e a segunda e a que faz a coisa funcionar.
 *
 * A METADE FACIL e tocar quando o Diretor mandar. A METADE QUE IMPORTA e CALAR o
 * MusicManager do jogo, que sem isso continua emendando faixa em faixa por conta propria.
 * Nao adianta so abrir os intervalos nos JSON: o `nextSongDelay` do MusicManager nasce em
 * 100 ticks, entao a primeira faixa entra cinco segundos depois de o mundo carregar por
 * mais alto que seja o `min_delay`. Enquanto existir uma segunda pessoa decidindo quando
 * ha musica, o silencio do Diretor nao e silencio — e um intervalo entre faixas.
 *
 * Por isso o corte e na fonte: toda musica que nao tenha sido comecada por este arquivo e
 * cancelada no PlaySoundEvent. Vale para a trilha do mod, para a do vanilla e para a de
 * quem mais tentar — no VHSWORLD, ou o Diretor mandou tocar, ou nao ha musica.
 *
 * ⚠️ ISTO E DELIBERADAMENTE AGRESSIVO com outros mods de som. Se um dia isso incomodar, o
 * conserto NAO e afrouxar o cancelamento (volta o tapete), e sim ensinar o Diretor a
 * mandar tocar a faixa daquele outro mod.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MusicDirector {

    private MusicDirector() {}

    /** A instancia que NOS comecamos. Tudo que nao for ela morre no PlaySoundEvent. */
    private static SoundInstance ours;

    /** Liberado por um instante enquanto a nossa propria musica entra no motor. */
    private static boolean opening;

    public static void handle(MusicPacket.Action action) {
        switch (action) {
            case PLAY -> play();
            case STOP -> stop();
        }
    }

    private static void play() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!RECConfig.CLIENT.directorMusic.get()) return;
        if (ours != null && mc.getSoundManager().isActive(ours)) return;

        // ⚠️ QUAL faixa continua sendo pergunta do jogo, nao nossa: isto devolve a musica
        // configurada no bioma/dimensao em que o jogador esta. E o que preserva as quinze
        // trilhas por dimensao sem este arquivo saber que elas existem.
        Music situational = mc.getSituationalMusic();
        if (situational == null) return;

        ours = SimpleSoundInstance.forMusic(situational.getEvent().value());

        opening = true;
        try {
            mc.getSoundManager().play(ours);
        } finally {
            opening = false;
        }
    }

    private static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (ours == null) return;
        mc.getSoundManager().stop(ours);
        ours = null;
    }

    /** Esta tocando alguma coisa nossa agora? O servidor nao sabe; quem sabe e a caixa. */
    public static boolean playing() {
        Minecraft mc = Minecraft.getInstance();
        return ours != null && mc.getSoundManager().isActive(ours);
    }

    /**
     * O CORTE. Toda musica que nao seja a nossa nao acontece.
     *
     * ⚠️ Só `SoundSource.MUSIC`. Disco de jukebox e `RECORDS` e continua tocando — o
     * jogador pode fazer barulho de proposito, essa e uma decisao dele.
     */
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (!RECConfig.CLIENT.directorMusic.get()) return;
        if (opening) return;

        SoundInstance sound = event.getSound();
        if (sound == null || sound.getSource() != SoundSource.MUSIC) return;
        if (sound == ours) return;

        event.setSound(null);
    }

    /** Sair do mundo tem que apagar a lembranca, senao a proxima sessao nasce achando que toca algo. */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().level == null) ours = null;
    }
}
