package net.vhsworld.rec.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * "TOCA ESTE SOM ALI, AGORA." Do servidor para UM jogador.
 *
 * ⚠️ POR QUE O SOM MUDOU DE LADO. Ele sempre foi 100% cliente (AmbientDread), com um
 * relogio proprio sorteando o intervalo. Funcionava, e era justamente o problema: o
 * cliente nao tem como saber se ha uma criatura nossa a quarenta blocos, entao o som nao
 * podia significar nada. Som que nunca significa nada o jogador desliga na cabeca em
 * quinze minutos — e foi o que aconteceu. Quem sabe do mundo e o servidor, entao a
 * DECISAO subiu para la (ver Director.maybeNoise) e o cliente ficou so com a execucao.
 *
 * O pacote carrega o minimo: qual som e o deslocamento em relacao ao jogador. Volume,
 * afinacao e o estado da camera continuam sendo do cliente, porque sao aparencia — e
 * porque o servidor nao tem nada que saber se a fita esta rodando.
 *
 * ⚠️ O DESLOCAMENTO E RELATIVO DE PROPOSITO. Com posicao absoluta, o som ficaria parado
 * no mundo entre o pacote sair e o cliente tocar; e ele tem que sair de perto do OUVIDO
 * de quem recebe, nao de um ponto do mapa.
 */
public class DreadPacket {

    /**
     * Quantos sons ha na piscina do cliente.
     *
     * ⚠️ ESTE NUMERO TEM QUE BATER COM O TAMANHO DA LISTA EM AmbientDread. O servidor
     * sorteia o indice sem poder ver a lista (ela e client-only), entao um numero maior
     * aqui faz o cliente receber um indice que nao existe. Ver o clamp no decode: ele
     * transforma esse defeito em "tocou o som errado" em vez de derrubar o jogo.
     */
    public static final int POOL_SIZE = 6;

    private final int sound;
    private final float dx;
    private final float dy;
    private final float dz;

    public DreadPacket(int sound, Vec3 offset) {
        this.sound = sound;
        this.dx = (float) offset.x;
        this.dy = (float) offset.y;
        this.dz = (float) offset.z;
    }

    private DreadPacket(int sound, float dx, float dy, float dz) {
        this.sound = sound;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    public int sound() {
        return sound;
    }

    public Vec3 offset() {
        return new Vec3(dx, dy, dz);
    }

    public static void encode(DreadPacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.sound);
        buffer.writeFloat(packet.dx);
        buffer.writeFloat(packet.dy);
        buffer.writeFloat(packet.dz);
    }

    public static DreadPacket decode(FriendlyByteBuf buffer) {
        int sound = Math.floorMod(buffer.readByte(), POOL_SIZE);
        return new DreadPacket(sound, buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(DreadPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() ->
                // A classe do som e client-only: chamada direta faria o servidor dedicado
                // procurar uma classe que ele nao tem.
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> net.vhsworld.rec.client.AmbientDread.fromDirector(packet)));
        ctx.setPacketHandled(true);
    }
}
