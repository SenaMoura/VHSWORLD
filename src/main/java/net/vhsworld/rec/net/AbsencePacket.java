package net.vhsworld.rec.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.vhsworld.rec.director.Absence;

import java.util.function.Supplier;

/**
 * "AQUI TINHA UMA COISA SUA." Do servidor para UM jogador.
 *
 * ⚠️ ESTE PACOTE NAO MUDA NADA NA TELA QUANDO CHEGA, e essa e a coisa mais importante
 * dele. Ele so deixa a marca guardada no cliente, calada. O jogador nao recebe aviso,
 * nao pisca nada, nao toca nada — se recebesse, a mecanica estaria contando o segredo
 * exatamente para quem ela deveria fazer duvidar.
 *
 * A marca so vira imagem quando o jogador LEVANTA a lente e aponta para o lugar certo
 * (ver AbsenceEvidence). Ate la ela e so uma coordenada esperando alguem desconfiar.
 *
 * ⚠️ POR QUE O CLIENTE PODE SABER ANTES DE MOSTRAR. Da para argumentar que isto entrega
 * a informacao cedo demais. Entrega mesmo — e a alternativa e pior: pedir a prova ao
 * servidor no instante em que a lente sobe custa uma ida-e-volta, e o vulto apareceria
 * um piscar depois de o jogador ja estar olhando. A confirmacao tem que ser instantanea,
 * senao ela parece efeito de jogo em vez de registro de camera.
 */
public class AbsencePacket {

    private final BlockPos pos;
    private final Absence.Kind kind;

    public AbsencePacket(BlockPos pos, Absence.Kind kind) {
        this.pos = pos;
        this.kind = kind;
    }

    public BlockPos pos() {
        return pos;
    }

    public Absence.Kind kind() {
        return kind;
    }

    public static void encode(AbsencePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeByte(packet.kind.ordinal());
    }

    public static AbsencePacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        Absence.Kind[] kinds = Absence.Kind.values();
        int index = buffer.readByte();
        return new AbsencePacket(pos, kinds[Math.floorMod(index, kinds.length)]);
    }

    public static void handle(AbsencePacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> net.vhsworld.rec.client.AbsenceEvidence.remember(packet)));
        ctx.setPacketHandled(true);
    }
}
