package net.vhsworld.rec.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.vhsworld.rec.entity.OphanimGaze;

import java.util.function.Supplier;

/**
 * "EU DISPAREI O FLASH AGORA." Do cliente para o servidor, e nao carrega mais nada.
 *
 * Nao vai posicao nem direcao no pacote de proposito: o servidor JA sabe onde o jogador
 * esta e para onde ele olha (a rotacao dele chega todo tick pelo movimento vanilla).
 * Mandar de novo daria ao cliente a chance de mentir sobre isso, e um pacote vazio nao
 * tem como ser adulterado — o unico poder que ele da e "houve um clarao", que e
 * exatamente o que se quer contar.
 *
 * ⚠️ O servidor nao confia no ritmo do cliente: quem limita e o COOLDOWN do lado de la
 * (ver OphanimGaze.flash). Sem isso, um cliente modificado mandaria mil por segundo e
 * empurraria o Ofanim para fora do mapa.
 */
public class FlashPacket {

    public FlashPacket() {}

    public static void encode(FlashPacket packet, FriendlyByteBuf buffer) {
        // vazio de proposito
    }

    public static FlashPacket decode(FriendlyByteBuf buffer) {
        return new FlashPacket();
    }

    public static void handle(FlashPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        // enqueueWork: o pacote chega na thread de rede, e mexer em entidade fora da
        // thread do servidor e como se pede um ConcurrentModificationException.
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            OphanimGaze.flash(player);
        });
        ctx.setPacketHandled(true);
    }
}
