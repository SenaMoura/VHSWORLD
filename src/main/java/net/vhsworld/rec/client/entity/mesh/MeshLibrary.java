package net.vhsworld.rec.client.entity.mesh;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Guarda as esculturas ja lidas, e nunca deixa uma falha de leitura virar tela preta.
 *
 * Carregar e caro (meio mega de vertice por criatura) e acontece uma vez so, na primeira
 * vez que a criatura aparece — nao no arranque do jogo. Quem nunca encontrar o Ofanim
 * nunca paga por ele.
 *
 * O NULO E RESPOSTA VALIDA. Se o arquivo faltar ou vier corrompido, isto devolve null e o
 * desenhista volta para o cartaz 2D. E deliberado: a alternativa seria a criatura sumir do
 * mundo ou o render explodir a cada quadro, e o mod ja tem uma licao antiga sobre isso —
 * nunca crashar de proposito. Melhor uma criatura de aparencia velha do que jogo quebrado.
 */
public final class MeshLibrary {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<ResourceLocation, BakedMesh> CACHE = new HashMap<>();

    /** Quem ja falhou nao e tentado de novo: senao seriam 60 leituras por segundo. */
    private static final Map<ResourceLocation, Boolean> FAILED = new HashMap<>();

    private MeshLibrary() {
    }

    public static BakedMesh get(ResourceLocation path) {
        BakedMesh cached = CACHE.get(path);
        if (cached != null) return cached;
        if (FAILED.containsKey(path)) return null;

        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(path);
            if (res.isEmpty()) {
                LOGGER.warn("[malha] nao achei {}", path);
                FAILED.put(path, true);
                return null;
            }
            try (InputStream in = res.get().open()) {
                BakedMesh mesh = BakedMesh.read(in);
                CACHE.put(path, mesh);
                LOGGER.info("[malha] {} carregada: {} triangulos, {} poses",
                        path, mesh.triangles(), mesh.frames());
                return mesh;
            }
        } catch (Exception e) {
            LOGGER.error("[malha] falhei ao ler {}: {}", path, e.toString());
            FAILED.put(path, true);
            return null;
        }
    }

    /** Chamado quando os recursos recarregam (F3+T): um resource pack pode trocar a malha. */
    public static void reset() {
        CACHE.clear();
        FAILED.clear();
    }
}
