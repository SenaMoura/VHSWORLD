package net.vhsworld.rec.client.tape;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.ClientWorldData;
import net.vhsworld.rec.config.RECConfig;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * As fitas gravadas do mundo atual, no disco: cada uma e uma pasta de quadros com um
 * index.json ao lado. Tudo por-mundo (a mesma pasta das fotos), entao mundo novo nasce
 * sem fita nenhuma, como as fotos e o registro.
 */
public final class TapeLibrary {

    private static final Logger LOG = LogUtils.getLogger();

    private TapeLibrary() {}

    private static Path tapesDir() {
        return ClientWorldData.worldDir().resolve("tapes");
    }

    /** Escreve o index de uma fita recem-fechada. */
    public static void writeIndex(Path reelDir, int frames, int frameEvery) throws Exception {
        JsonObject json = new JsonObject();
        json.addProperty("frames", frames);
        json.addProperty("frameEvery", frameEvery);
        json.addProperty("createdAt", System.currentTimeMillis());
        Files.writeString(reelDir.resolve("index.json"), json.toString());
    }

    /** Todas as fitas, da mais nova para a mais velha. */
    public static List<Reel> list() {
        List<Reel> reels = new ArrayList<>();
        Path dir = tapesDir();
        if (!Files.isDirectory(dir)) return reels;

        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isDirectory).forEach(reelDir -> {
                Reel reel = read(reelDir);
                if (reel != null) reels.add(reel);
            });
        } catch (Exception e) {
            LOG.error("Nao consegui listar as fitas", e);
        }

        reels.sort(Comparator.comparingLong((Reel r) -> r.createdAt).reversed());
        return reels;
    }

    private static Reel read(Path reelDir) {
        Path index = reelDir.resolve("index.json");
        if (!Files.isRegularFile(index)) return null;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(index)).getAsJsonObject();
            int frames = json.has("frames") ? json.get("frames").getAsInt() : 0;
            int frameEvery = json.has("frameEvery") ? json.get("frameEvery").getAsInt() : 10;
            long createdAt = json.has("createdAt") ? json.get("createdAt").getAsLong() : 0L;
            if (frames <= 0) return null;
            return new Reel(reelDir, frames, frameEvery, createdAt);
        } catch (Exception e) {
            LOG.error("Fita ilegivel em {}", reelDir, e);
            return null;
        }
    }

    /** Apaga as fitas mais velhas quando passam do limite do config. */
    public static void trim() {
        List<Reel> reels = list();
        int max = RECConfig.CLIENT.tapeMaxReels.get();
        for (int i = max; i < reels.size(); i++) {
            reels.get(i).deleteFromDisk();
        }
    }

    // ------------------------------------------------------------------ uma fita

    public static final class Reel {
        public final Path dir;
        public final int frames;
        public final int frameEvery;
        public final long createdAt;

        private final Map<Integer, ResourceLocation> textures = new HashMap<>();

        Reel(Path dir, int frames, int frameEvery, long createdAt) {
            this.dir = dir;
            this.frames = frames;
            this.frameEvery = frameEvery;
            this.createdAt = createdAt;
        }

        public String label() {
            return dir.getFileName().toString();
        }

        /** Duracao total da fita em segundos (quadros x intervalo). */
        public int seconds() {
            return Math.max(1, Math.round(frames * frameEvery / 20.0f));
        }

        /** A textura de um quadro, carregada do disco sob demanda; null se sumiu. */
        public ResourceLocation frame(int i) {
            if (i < 0 || i >= frames) return null;
            ResourceLocation cached = textures.get(i);
            if (cached != null) return cached;

            Path file = dir.resolve(String.format("frame_%03d.png", i));
            if (!Files.isRegularFile(file)) return null;

            try (InputStream in = Files.newInputStream(file)) {
                NativeImage image = NativeImage.read(in);
                DynamicTexture tex = new DynamicTexture(image);
                ResourceLocation id = new ResourceLocation(RECMod.MOD_ID,
                        "tape/" + label().toLowerCase() + "_" + i);
                Minecraft.getInstance().getTextureManager().register(id, tex);
                textures.put(i, id);
                return id;
            } catch (Exception e) {
                LOG.error("Nao consegui carregar o quadro {} da fita {}", i, label(), e);
                return null;
            }
        }

        /** Solta da GPU os quadros ja carregados (ao fechar a tela). */
        public void releaseTextures() {
            for (ResourceLocation id : textures.values()) {
                Minecraft.getInstance().getTextureManager().release(id);
            }
            textures.clear();
        }

        public void deleteFromDisk() {
            releaseTextures();
            try (Stream<Path> stream = Files.walk(dir)) {
                stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
            } catch (Exception e) {
                LOG.warn("Nao consegui apagar a fita {}", label());
            }
        }
    }
}
