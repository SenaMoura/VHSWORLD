package net.vhsworld.rec.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * "TOCA A TRILHA" / "CORTA A TRILHA." Do servidor para UM jogador.
 *
 * ⚠️ O PACOTE NAO DIZ QUAL FAIXA, e isso e decisao e nao economia. Que musica toca em
 * cada lugar ja esta configurado nos biomas (`music` em cada dimensao e no overworld), e
 * essa configuracao e boa — o que estava errado era o QUANDO, nao o QUAL. Entao o
 * servidor manda so o verbo e o cliente pergunta ao proprio jogo qual e a musica da
 * situacao (Minecraft.getSituationalMusic). Assim as quinze trilhas por dimensao
 * continuam valendo sem precisar reescrever um JSON sequer, e trilha nova entra pelo
 * mesmo caminho de sempre.
 */
public class MusicPacket {

    public enum Action {
        /** Comeca a musica do lugar onde o jogador esta. */
        PLAY,
        /**
         * Corta o que estiver tocando.
         *
         * ⚠️ E o pedaco mais assustador de tudo isto, e ele e de graca: a faixa sumir
         * porque alguma coisa se aproximou. O jogador nao sabe por que a musica parou —
         * so sabe que parou.
         */
        STOP;

        private static final Action[] VALUES = values();

        static Action of(int index) {
            return index >= 0 && index < VALUES.length ? VALUES[index] : STOP;
        }
    }

    private final Action action;

    public MusicPacket(Action action) {
        this.action = action;
    }

    public Action action() {
        return action;
    }

    public static void encode(MusicPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.action.ordinal());
    }

    public static MusicPacket decode(FriendlyByteBuf buffer) {
        return new MusicPacket(Action.of(buffer.readByte()));
    }

    public static void handle(MusicPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> net.vhsworld.rec.client.MusicDirector.handle(packet.action())));
        ctx.setPacketHandled(true);
    }
}
