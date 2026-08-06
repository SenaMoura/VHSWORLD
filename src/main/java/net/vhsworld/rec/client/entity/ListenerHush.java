package net.vhsworld.rec.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.ListenerEntity;

import java.util.List;

/**
 * O SILENCIO E O CORPO DELE.
 *
 * ⚠️ ESTE ARQUIVO E A RESPOSTA A PERGUNTA QUE MATA MECANICA NESTE MOD: <i>qual e o segundo
 * em que o jogador percebe que isto aconteceu?</i> Uma criatura cega que anda atras de som
 * e, do ponto de vista de quem joga, indistinguivel de um mob comum que ainda nao te viu —
 * a regra dela e invisivel, e regra invisivel nao vira estrategia. O corpo dela nao pode
 * ser a unica coisa que a anuncia, porque ver o corpo ja e tarde.
 *
 * Entao ela se anuncia pelo AVESSO: quando o Escutador se aproxima, o mundo abaixa. O
 * ambiente some, a chuva afina, a caverna emudece. O jogador nao ouve uma coisa nova
 * chegando — ele ouve as coisas velhas indo embora, que e o unico jeito de o silencio ser
 * percebido como acontecimento em vez de como ausencia de conteudo.
 *
 * ⚠️ E O QUE ELE <b>NAO</b> ABAFA E A MECANICA. Os sons que o proprio jogador produz —
 * picareta, passo, porta — passam inteiros, e ficam SOZINHOS no vazio. E a criatura
 * ensinando a propria regra sem uma linha de texto: no silencio dela, a unica coisa que se
 * escuta e voce fazendo barulho.
 *
 * O Diretor ja corta a trilha sozinho quando a pressao sobe (criatura perto = pressao), e
 * isso continua valendo. Aqui e a camada de baixo: o mundo, nao a musica.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ListenerHush {

    private ListenerHush() {}

    /** A partir daqui o mundo comeca a abaixar. */
    private static final double RANGE = 26.0D;

    /**
     * ⚠️ Recalculado uma vez por segundo e guardado, e nao a cada som: o PlaySoundEvent
     * dispara dezenas de vezes por segundo, e varrer a lista de entidades em cada um seria
     * pagar uma busca por passo de mob a cinquenta blocos.
     */
    private static float hush;
    private static long lastCheck;

    /** 0 = mundo normal, 1 = mudo. Para quem mais quiser reagir a presenca dele. */
    public static float hush() {
        return hush;
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || event.getSound() == null) return;

        long now = mc.level.getGameTime();
        if (now != lastCheck) {
            lastCheck = now;
            if (now % 20L == 0L) recompute();
        }

        if (hush <= 0.01f) return;

        SoundSource source = event.getSound().getSource();

        // ⚠️ O QUE PASSA INTEIRO. PLAYERS e o proprio jogador; BLOCKS e a picareta, a porta,
        // o bau — ou seja, o barulho que o Escutador esta ouvindo. Abafar isso seria tirar
        // do jogador justamente a informacao com a qual ele deveria se assustar. HOSTILE
        // passa porque o estalo dele e a unica voz que a criatura tem.
        if (source == SoundSource.PLAYERS || source == SoundSource.BLOCKS
                || source == SoundSource.HOSTILE || source == SoundSource.MASTER
                || source == SoundSource.VOICE) {
            return;
        }

        // O resto do mundo (ambiente, bichos, tempo, agua) afunda junto com a aproximacao.
        // Perto o bastante, some de vez: sumir e diferente de ficar baixinho, e a diferenca
        // e exatamente a sensacao de que alguma coisa esta ERRADA com o lugar.
        if (hush > 0.85f) {
            event.setSound(null);
            return;
        }

        event.setSound(new HushedSound(event.getSound(), 1.0f - hush));
    }

    private static void recompute() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            hush = 0.0f;
            return;
        }

        List<ListenerEntity> near = mc.level.getEntitiesOfClass(ListenerEntity.class,
                player.getBoundingBox().inflate(RANGE));

        float worst = 0.0f;
        for (ListenerEntity listener : near) {
            double distance = Math.sqrt(listener.distanceToSqr(player));
            if (distance > RANGE) continue;
            worst = Math.max(worst, (float) (1.0D - distance / RANGE));
        }

        // Curva ao quadrado: quase nada ate ele estar perto de verdade, e ai cai rapido.
        // Linear faria o mundo abaixar a vinte e cinco blocos, o que o jogador leria como
        // "o som do jogo esta com problema", e nao como uma presenca.
        hush = worst * worst;
    }
}
