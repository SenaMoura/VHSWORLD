package net.vhsworld.rec.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.vhsworld.rec.escape.ExitMethod;

import java.util.function.Supplier;

/**
 * "A TELA VAI FAZER ISTO AGORA." Do servidor para UM jogador.
 *
 * ⚠️ POR QUE PRECISA DE PACOTE. Os quatro finais sao efeitos de TELA — estatica branca,
 * as linhas de STOP/EJECT, o branco da revelacao — e tela e cliente. Mas quem decide que
 * a fuga deu certo e o SERVIDOR: e ele que sabe se a sequencia do radio bateu, se a fita
 * estava completa, se o jogador andou o corredor sem virar a camera. Sem este pacote o
 * cliente teria que adivinhar o desfecho pelo teleporte, e adivinhar significa desenhar o
 * efeito DEPOIS de o mundo ja ter mudado — o corte apareceria em cima do overworld, que
 * e exatamente o que ele deveria estar escondendo.
 *
 * O pacote carrega um byte: qual efeito. Nada mais — duracao, cor e curva sao do cliente,
 * porque sao decisao de aparencia e nao de jogo.
 */
public class EscapeFxPacket {

    /** O que a tela faz. A ordem importa: e ela que vai no fio. */
    public enum Kind {
        /** MIRROR: encostou nele sem olhar — o reflexo engole. */
        MIRROR_THROUGH,
        /** MIRROR: olhou demais. Nao e saida, e o caminho todo de volta. */
        MIRROR_BROKEN,
        /** MIRROR: o susto de olhar. Um dos rostos, na tela inteira, por um instante. */
        MIRROR_SCARE,
        /** DOOR: o caminho acaba. A imagem cai como uma fita chegando no fim do rolo. */
        DOOR_THROUGH;

        private static final Kind[] VALUES = values();

        static Kind of(int index) {
            return index >= 0 && index < VALUES.length ? VALUES[index] : MIRROR_THROUGH;
        }
    }

    private final Kind kind;

    public EscapeFxPacket(Kind kind) {
        this.kind = kind;
    }

    /** O final que combina com o metodo da dimensao. */
    public EscapeFxPacket(ExitMethod method) {
        this.kind = switch (method) {
            case MIRROR -> Kind.MIRROR_THROUGH;
            case DOOR -> Kind.DOOR_THROUGH;
        };
    }

    public Kind kind() {
        return kind;
    }

    public static void encode(EscapeFxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.kind.ordinal());
    }

    public static EscapeFxPacket decode(FriendlyByteBuf buffer) {
        return new EscapeFxPacket(Kind.of(buffer.readByte()));
    }

    public static void handle(EscapeFxPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() ->
                // ⚠️ DistExecutor: a classe do efeito e client-only. Chamar direto faria o
                // servidor dedicado tentar carregar uma classe que nao existe nele, e o
                // erro sai como NoClassDefFoundError no meio do jogo, nao na compilacao.
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> net.vhsworld.rec.client.escape.EscapeFx.play(packet.kind())));
        ctx.setPacketHandled(true);
    }
}
