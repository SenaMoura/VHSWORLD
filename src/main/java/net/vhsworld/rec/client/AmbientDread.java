package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.item.ModSounds;
import net.vhsworld.rec.net.DreadPacket;

import java.util.List;
import java.util.Random;

/**
 * O barulho que vem do nada — a MAO, nao mais a cabeca.
 *
 * De tempos em tempos o mundo faz um ruido: fala embolada, fita rasgando, passos
 * pesados. Nao ha nada ali. E esse e o ponto — a maior parte do medo de um jogo de
 * terror acontece quando NAO esta acontecendo nada, e o jogador para de andar sozinho,
 * so por causa de um som.
 *
 * ⚠️ O QUE MUDOU, E POR QUE. Este arquivo tinha um `countdown` proprio: sorteava um
 * intervalo entre dois numeros do config e tocava, para sempre, sem olhar para nada. O
 * comentario antigo defendia o sorteio dizendo que intervalo fixo viraria metronomo — o
 * que e verdade, e nao era o problema. O problema e que sorteio puro tambem e aprendido,
 * e mais rapido: como o som nunca se correlacionava com coisa nenhuma, o jogador concluia
 * (corretamente) que ele nao significava nada, e parava de escutar. Papel de parede.
 *
 * A DECISAO SUBIU PARA O SERVIDOR (ver Director.maybeNoise), porque so la da para saber
 * a unica coisa que faz o som valer: se ha uma criatura nossa por perto. Agora o ruido ou
 * vem da direcao de algo que existe de verdade, ou e mentira deliberada para quebrar um
 * silencio longo demais — e o jogador nao consegue distinguir os dois. Foi assim que o
 * som virou informacao em vez de enfeite.
 *
 * DIFERENCA PARA O SanityHaunting: aquele so acorda com a sanidade no chao e e castigo
 * por ter olhado demais. Este toca desde o primeiro minuto, com o medidor cheio, e nao e
 * castigo de nada — e o clima do mundo.
 *
 * O que sobrou aqui e o que sempre foi do cliente: qual som sai da caixa, com que volume,
 * e se a fita esta rodando.
 */
public final class AmbientDread {

    private AmbientDread() {}

    private static final Random RANDOM = new Random();

    /**
     * ⚠️ O TAMANHO DESTA LISTA E O DreadPacket.POOL_SIZE TEM QUE BATER. O servidor
     * sorteia o indice sem enxergar esta lista — ela e client-only.
     */
    private static final List<RegistryObject<SoundEvent>> POOL = List.of(
            ModSounds.DREAD_SPEECH,
            ModSounds.DREAD_BROKEN,
            ModSounds.DREAD_GLITCH,
            ModSounds.DREAD_STEPS,
            ModSounds.DREAD_BURNT,
            ModSounds.DREAD_FLESH);

    /**
     * O Diretor mandou tocar. Chamado pelo DreadPacket, ja na thread do cliente.
     *
     * ⚠️ As recusas daqui sao todas de CONFORTO, nunca de ritmo: o jogador desligou o som
     * no config, ou a fita nao esta rodando. Ritmo e assunto do servidor — se este metodo
     * comecar a decidir QUANDO tocar, o Diretor ganha um concorrente e o mod volta a ter
     * dois relogios, que e o defeito que ele existe para acabar.
     */
    public static void fromDirector(DreadPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!RECConfig.CLIENT.ambientDread.get()) return;
        if (!CameraState.audible()) return;

        SoundEvent sound = POOL.get(Math.floorMod(packet.sound(), POOL.size())).get();
        Vec3 at = mc.player.position().add(packet.offset());

        // Baixo de proposito: tem que caber a duvida de ter ouvido mesmo.
        float volume = CameraState.volume(
                RECConfig.CLIENT.ambientDreadVolume.get().floatValue());

        mc.level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.AMBIENT,
                volume, 0.85f + RANDOM.nextFloat() * 0.3f, false);
    }
}
