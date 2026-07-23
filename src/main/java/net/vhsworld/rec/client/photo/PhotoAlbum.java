package net.vhsworld.rec.client.photo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * O album do jogador: a lista de fotos e os arquivos no disco.
 *
 * Tudo client-side. As fotos ficam em .minecraft/vhsworld_photos/, com um index.json
 * ao lado guardando o que ja foi revelado. Sobrevive a fechar o jogo.
 */
public final class PhotoAlbum {

    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<ArrayList<Photo>>() {}.getType();

    private static PhotoAlbum instance;

    private final Path folder;
    private final Path index;
    private final List<Photo> photos = new ArrayList<>();

    private PhotoAlbum() {
        this.folder = Minecraft.getInstance().gameDirectory.toPath().resolve("vhsworld_photos");
        this.index = folder.resolve("index.json");
        load();
    }

    public static PhotoAlbum get() {
        if (instance == null) instance = new PhotoAlbum();
        return instance;
    }

    /** Da mais nova para a mais velha — o carretel mostra a ultima primeiro. */
    public List<Photo> photos() {
        return photos;
    }

    // ------------------------------------------------------------------ escrita

    /**
     * Guarda uma imagem recem-capturada. A NativeImage e consumida aqui (fechada no fim).
     *
     * @param subject o que o filme pegou, ou null se nao pegou nada
     */
    public void add(NativeImage image, String subject) {
        try {
            Files.createDirectories(folder);

            String name = "photo_" + System.currentTimeMillis() + ".png";
            image.writeToFile(folder.resolve(name));

            Photo photo = new Photo(name, System.currentTimeMillis());
            photo.subject = subject;
            photos.add(0, photo);

            trim();
            save();
        } catch (IOException e) {
            LOG.error("Nao consegui salvar a foto", e);
        } finally {
            image.close();
        }
    }

    /** Joga fora as fotos mais antigas quando passa do limite do config. */
    private void trim() {
        int max = RECConfig.CLIENT.maxPhotos.get();
        while (photos.size() > max) {
            Photo old = photos.remove(photos.size() - 1);
            releaseTexture(old);
            try {
                Files.deleteIfExists(folder.resolve(old.file));
            } catch (IOException e) {
                LOG.warn("Nao consegui apagar a foto antiga {}", old.file);
            }
        }
    }

    public void delete(Photo photo) {
        photos.remove(photo);
        releaseTexture(photo);
        try {
            Files.deleteIfExists(folder.resolve(photo.file));
        } catch (IOException e) {
            LOG.warn("Nao consegui apagar {}", photo.file);
        }
        save();
    }

    public void save() {
        try {
            Files.createDirectories(folder);
            Files.writeString(index, GSON.toJson(photos));
        } catch (IOException e) {
            LOG.error("Nao consegui salvar o index do album", e);
        }
    }

    private void load() {
        if (!Files.isRegularFile(index)) return;
        try {
            List<Photo> read = GSON.fromJson(Files.readString(index), LIST_TYPE);
            if (read != null) {
                photos.addAll(read);
                photos.sort(Comparator.comparingLong((Photo p) -> p.takenAt).reversed());
            }
        } catch (Exception e) {
            LOG.error("index.json do album esta corrompido; comecando vazio", e);
        }
    }

    // ------------------------------------------------------------------ texturas

    /**
     * Devolve a textura da foto, carregando do disco na primeira vez.
     * null se o arquivo sumiu (a ficha fica marcada como quebrada).
     */
    public ResourceLocation texture(Photo photo) {
        if (photo.texture != null || photo.broken) return photo.texture;

        Path file = folder.resolve(photo.file);
        if (!Files.isRegularFile(file)) {
            photo.broken = true;
            return null;
        }

        try (InputStream in = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(in);
            DynamicTexture tex = new DynamicTexture(image);
            ResourceLocation id = new ResourceLocation(RECMod.MOD_ID,
                    "photo/" + photo.file.toLowerCase().replace(".png", ""));
            Minecraft.getInstance().getTextureManager().register(id, tex);
            photo.texture = id;
            return id;
        } catch (Exception e) {
            LOG.error("Nao consegui carregar a foto {}", photo.file, e);
            photo.broken = true;
            return null;
        }
    }

    private void releaseTexture(Photo photo) {
        if (photo.texture != null) {
            Minecraft.getInstance().getTextureManager().release(photo.texture);
            photo.texture = null;
        }
    }
}
