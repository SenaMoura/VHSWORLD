package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.codex.Codex;
import net.vhsworld.rec.client.difficulty.DifficultyState;
import net.vhsworld.rec.client.photo.PhotoAlbum;

import java.nio.file.Path;

/**
 * Onde os dados de client (codex, album) do jogador ficam guardados.
 *
 * Antes tudo caia num arquivo unico em .minecraft/ — entao um mundo NOVO nascia com
 * as fotos e as fichas de um mundo VELHO. O verbo do mod e descobrir do zero; herdar
 * o registro estraga isso. Agora cada mundo tem a sua pasta e, ao trocar de mundo, os
 * caches em memoria sao zerados para reabrirem apontando para a pasta certa.
 *
 * Chave da pasta:
 *   - um jogo (singleplayer): o nome da pasta do save, que o Minecraft ja mantem unico
 *     ("Novo mundo", "Novo mundo (1)"...), entao cada save fica isolado;
 *   - servidor (multiplayer): o endereco do servidor;
 *   - fora de mundo (menu): "menu", so como ultimo recurso — os dados so sao lidos com
 *     o jogador ja dentro do mundo.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientWorldData {

    private ClientWorldData() {}

    /** A pasta dos dados do mundo atual: .minecraft/vhsworld/worlds/&lt;chave&gt;/ */
    public static Path worldDir() {
        Minecraft mc = Minecraft.getInstance();
        Path base = mc.gameDirectory.toPath().resolve("vhsworld").resolve("worlds");
        return base.resolve(worldKey(mc));
    }

    private static String worldKey(Minecraft mc) {
        IntegratedServer single = mc.getSingleplayerServer();
        if (single != null) {
            Path save = single.getWorldPath(LevelResource.ROOT);
            Path name = save.getFileName();
            return "sp-" + sanitize(name != null ? name.toString() : "world");
        }

        ServerData server = mc.getCurrentServer();
        if (server != null && server.ip != null && !server.ip.isEmpty()) {
            return "mp-" + sanitize(server.ip);
        }

        return "menu";
    }

    /** So deixa passar o que e seguro num nome de pasta; o resto vira "_". */
    private static String sanitize(String raw) {
        String cleaned = raw.replaceAll("[^a-zA-Z0-9-_]", "_");
        if (cleaned.length() > 64) cleaned = cleaned.substring(0, 64);
        return cleaned.isEmpty() ? "world" : cleaned;
    }

    // ---------------------------------------------------------------- reset

    // Entrar num mundo zera os caches: o proximo acesso reabre lendo a pasta do mundo
    // novo. Sair faz o mesmo para nao vazar o registro de um mundo no menu / no proximo.

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        clearCaches();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clearCaches();
    }

    private static void clearCaches() {
        Codex.reset();
        PhotoAlbum.reset();
        DifficultyState.reset();
    }
}
