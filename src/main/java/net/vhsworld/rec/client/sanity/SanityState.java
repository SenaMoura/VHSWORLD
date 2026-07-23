package net.vhsworld.rec.client.sanity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.vhsworld.rec.config.RECConfig;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A sanidade do jogador.
 *
 * Uma regra so, por enquanto: ela cai quando o jogador REVELA uma foto e descobre
 * que havia alguma coisa ali com ele. Nao cai no escuro, nao cai perto de monstro,
 * nao cai com fome. E o preco de olhar.
 *
 * Client-side, como todo o resto da camera. Guardada em .minecraft/vhsworld_sanity.json,
 * entao sobrevive a fechar o jogo.
 */
public final class SanityState {

    private static final Logger LOG = LogUtils.getLogger();
    private static SanityState instance;

    public static final float MAX = 100.0f;

    private final Path file;

    private float sanity = MAX;

    /** Quanto falta do tremor, em ticks, e de quanto ele partiu. */
    private int shakeTicks;
    private int shakeTotal;

    private SanityState() {
        this.file = Minecraft.getInstance().gameDirectory.toPath().resolve("vhsworld_sanity.json");
        load();
    }

    public static SanityState get() {
        if (instance == null) instance = new SanityState();
        return instance;
    }

    public float value() {
        return sanity;
    }

    /** 0.0 a 1.0, para desenhar a barra. */
    public float fraction() {
        return Math.max(0.0f, Math.min(1.0f, sanity / MAX));
    }

    public boolean shaking() {
        return shakeTicks > 0;
    }

    /**
     * Amplitude do tremor agora, ja com a queda ao longo do tempo.
     *
     * Comeca forte e vai morrendo: um susto tem pico e rescaldo, nao um platô.
     * Quanto menor a sanidade, mais a mao treme — o mesmo susto machuca mais
     * alguem que ja viu coisa demais.
     */
    public float shakeAmount() {
        if (shakeTicks <= 0 || shakeTotal <= 0) return 0.0f;

        float envelope = (float) shakeTicks / shakeTotal;
        float nerves = 1.0f + (1.0f - fraction());          // 1.0 sao -> 2.0 acabado
        return envelope * envelope * nerves
                * RECConfig.CLIENT.sanityShakeStrength.get().floatValue();
    }

    /**
     * O golpe: viu a criatura na foto.
     *
     * @param loss quanto de sanidade some; se <= 0, so treme
     */
    public void sighting(float loss) {
        if (!RECConfig.CLIENT.sanity.get()) return;

        sanity = Math.max(0.0f, sanity - loss);

        shakeTotal = (int) Math.round(RECConfig.CLIENT.sanityShakeSeconds.get() * 20.0D);
        shakeTicks = shakeTotal;

        save();
    }

    public void tick() {
        if (shakeTicks > 0) shakeTicks--;

        double perMinute = RECConfig.CLIENT.sanityRegenPerMinute.get();
        if (perMinute > 0.0D && sanity < MAX) {
            sanity = Math.min(MAX, sanity + (float) (perMinute / 1200.0D));   // 1200 ticks = 1 min
        }
    }

    /** Painel de controle para testes e para quando houver um item que cure. */
    public void set(float value) {
        sanity = Math.max(0.0f, Math.min(MAX, value));
        save();
    }

    // ------------------------------------------------------------------ disco

    private void load() {
        if (!Files.isRegularFile(file)) return;
        try {
            JsonObject json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (json.has("sanity")) {
                sanity = Math.max(0.0f, Math.min(MAX, json.get("sanity").getAsFloat()));
            }
        } catch (Exception e) {
            LOG.error("vhsworld_sanity.json ilegivel; comecando sao", e);
        }
    }

    private void save() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("sanity", sanity);
            Files.writeString(file, json.toString());
        } catch (Exception e) {
            LOG.error("Nao consegui salvar a sanidade", e);
        }
    }
}
