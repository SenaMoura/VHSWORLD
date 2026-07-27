package net.vhsworld.rec.client.photo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.vhsworld.rec.client.ClientWorldData;
import net.vhsworld.rec.entity.AnomalyType;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Quantas vezes voce ja revelou a foto de cada anomalia.
 *
 * E a conta que traz a anomalia do TAPE_THEN_REAL para o mundo: passado o limite,
 * o cliente comeca a desenha-la a olho nu. Cresce so na REVELACAO, nunca no clique —
 * o preco e olhar, nao fotografar, e essa e a mesma regra que a sanidade ja segue.
 *
 * Fica na pasta do mundo (ClientWorldData), ao lado do album e do registro: mundo
 * novo comeca do zero, senao o jogador chegaria num save novo com criaturas ja
 * "conquistadas" de outro.
 */
public final class AnomalySightings {

    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Integer>>() {}.getType();

    private static AnomalySightings instance;

    private final Map<String, Integer> counts = new HashMap<>();

    private AnomalySightings() {
        load();
    }

    public static AnomalySightings get() {
        if (instance == null) instance = new AnomalySightings();
        return instance;
    }

    /** Troca de mundo: esquece tudo, para reabrir na pasta certa. */
    public static void reset() {
        instance = null;
    }

    // ------------------------------------------------------------------ contas

    public int count(AnomalyType type) {
        return this.counts.getOrDefault(type.id(), 0);
    }

    /** Mais uma revelacao. Chamado quando a foto termina de revelar. */
    public void note(AnomalyType type) {
        this.counts.merge(type.id(), 1, Integer::sum);
        save();
    }

    // ------------------------------------------------------------------ disco

    private static Path file() {
        return ClientWorldData.worldDir().resolve("anomaly_sightings.json");
    }

    private void load() {
        Path path = file();
        if (!Files.exists(path)) return;

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Map<String, Integer> saved = GSON.fromJson(reader, MAP_TYPE);
            if (saved != null) this.counts.putAll(saved);
        } catch (Exception e) {
            LOG.warn("[REC] nao consegui ler {}: {}", path, e.toString());
        }
    }

    private void save() {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(this.counts, writer);
            }
        } catch (Exception e) {
            LOG.warn("[REC] nao consegui gravar {}: {}", path, e.toString());
        }
    }
}
