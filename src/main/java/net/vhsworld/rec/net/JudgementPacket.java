package net.vhsworld.rec.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.vhsworld.rec.entity.Judgement;

import java.util.function.Supplier;

/**
 * O JULGAMENTO, do servidor para a tela de UM jogador.
 *
 * O que e efeito de mundo (cegueira, teleporte, som) o servidor resolve sozinho — sao
 * coisas que o jogo ja sincroniza. O que sobra para este pacote e o que so existe na
 * tela de quem sofreu: o rolar da camera e o preco em sanidade, que e um numero que
 * mora no cliente desde sempre.
 *
 * A duracao vem DENTRO do pacote em vez de ser constante do lado de la porque quem
 * manda no relogio e o config do servidor. Cliente e servidor com numeros diferentes
 * dariam uma tela que volta ao normal antes (ou depois) de o castigo acabar.
 */
public class JudgementPacket {

    private final Judgement stage;
    private final int ticks;
    private final float sanity;

    public JudgementPacket(Judgement stage, int ticks, float sanity) {
        this.stage = stage;
        this.ticks = ticks;
        this.sanity = sanity;
    }

    public Judgement stage() {
        return this.stage;
    }

    public int ticks() {
        return this.ticks;
    }

    public float sanity() {
        return this.sanity;
    }

    public static void encode(JudgementPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.stage.ordinal());
        buffer.writeVarInt(packet.ticks);
        buffer.writeFloat(packet.sanity);
    }

    public static JudgementPacket decode(FriendlyByteBuf buffer) {
        // byIndex e nao values()[i]: um pacote de outra versao com um estagio que este
        // jogo nao conhece daria ArrayIndexOutOfBounds no meio da thread de rede.
        return new JudgementPacket(Judgement.byIndex(buffer.readByte()),
                buffer.readVarInt(), buffer.readFloat());
    }

    public static void handle(JudgementPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() ->
                // ⚠️ DistExecutor: sem ele o servidor dedicado carregaria a classe de
                // cliente so por ela aparecer aqui, e cai no carregamento por
                // NoClassDefFoundError.
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> net.vhsworld.rec.client.entity.OphanimGazeFx.judged(packet)));
        ctx.setPacketHandled(true);
    }
}
