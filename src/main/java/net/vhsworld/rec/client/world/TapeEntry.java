package net.vhsworld.rec.client.world;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelSummary;
import net.vhsworld.rec.RECMod;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Um mundo na estante, tratado como uma fita.
 *
 * Guarda o resumo do mundo, a textura do icone (carregada so quando o mouse chega
 * perto, para nao abrir dezenas de PNG de uma vez) e o quanto o fade de destaque
 * ja avancou.
 */
public class TapeEntry {

    public final LevelSummary summary;

    /** 0.0 = em repouso, 1.0 = destacado. Anda sozinho conforme o mouse entra e sai. */
    public float highlight = 0.0f;

    private ResourceLocation icon;
    private boolean iconTried = false;

    public TapeEntry(LevelSummary summary) {
        this.summary = summary;
    }

    public String name() {
        return summary.getLevelName();
    }

    public String id() {
        return summary.getLevelId();
    }

    /**
     * O icone do mundo, carregado na primeira vez que alguem olha para ele.
     * null quando o mundo nao tem icone — a tela desenha uma fita sem rotulo.
     */
    public ResourceLocation icon() {
        if (iconTried) return icon;
        iconTried = true;

        Path file = summary.getIcon();
        if (file == null || !Files.isRegularFile(file)) return null;

        try (InputStream in = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(in);
            DynamicTexture texture = new DynamicTexture(image);

            ResourceLocation id = new ResourceLocation(RECMod.MOD_ID,
                    "tape/" + summary.getLevelId().toLowerCase().replaceAll("[^a-z0-9_./-]", "_"));
            Minecraft.getInstance().getTextureManager().register(id, texture);
            icon = id;
        } catch (Exception ignored) {
            icon = null;
        }
        return icon;
    }

    /** Aproxima o destaque do alvo, para o fade nao depender do FPS. */
    public void advance(boolean hovered, float delta) {
        float target = hovered ? 1.0f : 0.0f;
        float speed = hovered ? 6.0f : 4.0f;

        highlight += (target - highlight) * Math.min(1.0f, speed * delta);
        if (Math.abs(highlight - target) < 0.005f) highlight = target;
    }
}
