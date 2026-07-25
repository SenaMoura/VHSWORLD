package net.vhsworld.rec.client.difficulty;

import com.mojang.logging.LogUtils;
import net.vhsworld.rec.client.ClientWorldData;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A dificuldade escolhida, guardada POR MUNDO.
 *
 * Fica junto do codex e das fotos, em .minecraft/vhsworld/worlds/&lt;chave&gt;/, pelo
 * mesmo motivo: mundo novo e comeco novo, e a escolha de um save nao pode vazar para
 * o outro. Escolhe-se UMA vez por mundo — a fita ja esta rodando, nao da para voltar
 * e regravar o comeco.
 *
 * Tudo aqui e client-side, como a sanidade: o mod nao tem rede.
 */
public final class DifficultyState {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static GameDifficulty current;
    private static boolean loaded;

    private DifficultyState() {}

    private static Path file() {
        return ClientWorldData.worldDir().resolve("difficulty.txt");
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        current = null;
        try {
            Path p = file();
            if (Files.exists(p)) {
                String raw = Files.readString(p).trim();
                if (!raw.isEmpty()) current = GameDifficulty.byName(raw);
            }
        } catch (IOException e) {
            LOGGER.warn("[recmod] nao consegui ler a dificuldade do mundo", e);
        }
    }

    /** Ja escolheu neste mundo? Enquanto for falso, a tela abre. */
    public static boolean chosen() {
        load();
        return current != null;
    }

    /** A dificuldade em vigor. Antes da escolha vale o NORMAL, para nada ler nulo. */
    public static GameDifficulty current() {
        load();
        return current == null ? GameDifficulty.NORMAL : current;
    }

    public static void choose(GameDifficulty difficulty) {
        load();
        current = difficulty;
        try {
            Path p = file();
            Files.createDirectories(p.getParent());
            Files.writeString(p, difficulty.name());
        } catch (IOException e) {
            LOGGER.warn("[recmod] nao consegui gravar a dificuldade do mundo", e);
        }
    }

    /** Trocar de mundo zera o cache: o proximo acesso le a pasta do mundo novo. */
    public static void reset() {
        loaded = false;
        current = null;
    }
}
