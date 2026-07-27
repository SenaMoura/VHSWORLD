package net.vhsworld.rec.net;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.vhsworld.rec.RECMod;

/**
 * O CANAL DO MOD. Um so, e o primeiro que este mod tem.
 *
 * POR QUE ELE NAO EXISTIA ATE AGORA: tudo no REC era ou 100% servidor (o apocalipse,
 * que so mexe em hora, clima e entidade — coisas que o jogo ja sincroniza sozinho) ou
 * 100% cliente (sanidade, fotos, codex, assombracao). Enquanto deu, foi melhor assim:
 * canal e superficie de bug, e cada pacote e um jeito novo de cliente e servidor
 * discordarem do jogo.
 *
 * O QUE OBRIGOU A ABRIR: o flash. Ele e o verbo do mod inteiro e mora no cliente (sobe
 * o gamma por um sexto de segundo, ver FlashLight) — o servidor nunca soube que ele
 * disparou. Sem isso a camera nunca poderia AGIR sobre nada: ela so podia olhar. Para
 * o Ofanim recuar do clarao, o servidor precisa saber que houve clarao.
 *
 * E a mesma porta que os EVENTOS vao precisar depois (corromper o HUD de um jogador so,
 * mandar um som para quem esta na sala, escurecer a tela por causa de servidor). Por
 * isso o canal nasce generico e nao "o canal do Ofanim".
 *
 * ⚠️ Os dois lados PRECISAM do mod — o que ja era verdade, o recmod nunca foi
 * client-side. As duas versoes tem que bater exatamente; mudar o formato de um pacote
 * sem subir a VERSAO faz o cliente antigo ler lixo em vez de recusar a conexao.
 */
public final class RECNetwork {

    private RECNetwork() {}

    /** Suba isto ao mudar QUALQUER formato de pacote. */
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RECMod.MOD_ID, "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    /**
     * O indice de cada pacote.
     *
     * ⚠️ E POSICIONAL: os dois lados casam pacote por NUMERO, nao por nome. Acrescente
     * sempre no FIM desta lista. Trocar a ordem faz um cliente antigo decodificar um
     * pacote com o codec de outro, e o erro sai como campo com valor absurdo, nao como
     * excecao.
     */
    public static void register() {
        int id = 0;

        CHANNEL.registerMessage(id++, FlashPacket.class,
                FlashPacket::encode, FlashPacket::decode, FlashPacket::handle);

        CHANNEL.registerMessage(id++, JudgementPacket.class,
                JudgementPacket::encode, JudgementPacket::decode, JudgementPacket::handle);
    }

    /** Do servidor para UM jogador. */
    public static void toPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    /** Do cliente para o servidor. */
    public static void toServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
