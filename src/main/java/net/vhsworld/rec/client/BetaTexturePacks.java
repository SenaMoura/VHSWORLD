package net.vhsworld.rec.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Liga sozinho os packs de textura beta que o jogador ja tenha instalado.
 *
 * POR QUE ASSIM, e nao com as texturas dentro do mod: as texturas beta sao ativos
 * da Mojang, e os packs que as recriam (Golden Days e afins) sao trabalho de outra
 * pessoa. Colocar esses arquivos dentro do nosso jar seria redistribuir o trabalho
 * alheio num pack publico. Aqui nao redistribuimos nada — so ativamos o que ja esta
 * na maquina do jogador.
 *
 * E acontece UMA VEZ. Depois de ligar, fica registrado em disco e nunca mais insiste.
 * Se o jogador desligar o pack porque nao gostou, ele fica desligado — um mod que
 * reimpoe a propria escolha a cada abertura e um mod chato.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BetaTexturePacks {

    private static final Logger LOG = LogUtils.getLogger();

    /** Pedacos de nome que identificam um pack de textura beta. */
    private static final String[] WANTED = {"golden-days", "golden_days", "golden days"};

    private static boolean handled = false;

    @SubscribeEvent
    public static void onScreen(ScreenEvent.Opening event) {
        if (handled) return;
        if (!(event.getScreen() instanceof TitleScreen) && !(event.getScreen() instanceof CustomMainMenuScreen)) return;

        handled = true;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return;
        if (!RECConfig.CLIENT.autoEnableBetaTextures.get()) return;

        Path marker = mc.gameDirectory.toPath().resolve("vhsworld_packs_applied");
        if (Files.exists(marker)) return;

        try {
            PackRepository repo = mc.getResourcePackRepository();
            repo.reload();

            List<String> selected = new ArrayList<>(mc.options.resourcePacks);
            List<String> found = new ArrayList<>();

            for (String id : repo.getAvailableIds()) {
                String lower = id.toLowerCase(Locale.ROOT);
                for (String wanted : WANTED) {
                    if (lower.contains(wanted) && !selected.contains(id)) {
                        found.add(id);
                        break;
                    }
                }
            }

            // Marca mesmo sem achar nada: quem nao tem o pack nao precisa desta
            // varredura toda vez que abre o jogo.
            Files.writeString(marker, "beta textures checked");

            if (found.isEmpty()) return;

            // "base" antes de "compat": o de compatibilidade tem que vencer o de base.
            found.sort((a, b) -> Boolean.compare(
                    a.toLowerCase(Locale.ROOT).contains("compat"),
                    b.toLowerCase(Locale.ROOT).contains("compat")));

            selected.addAll(found);
            mc.options.resourcePacks = selected;
            mc.options.save();

            LOG.info("[REC] texturas beta ligadas: {}", found);
            mc.reloadResourcePacks();
        } catch (Throwable t) {
            LOG.warn("[REC] nao consegui ligar os packs de textura beta", t);
        }
    }

    private BetaTexturePacks() {}
}
