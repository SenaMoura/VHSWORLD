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

    /**
     * O Programmer Art e a PROPRIA Mojang: sao as texturas de antes da reforma de
     * 1.14, que o jogo ainda distribui como pack embutido. Ele entra por baixo do
     * Golden Days porque tapa justamente o buraco que o outro deixa — o Golden Days
     * refaz blocos e interface, mas nao mexe nas ferramentas: espada, picareta, pa e
     * enxada continuavam com a arte nova. Nao ha nada a redistribuir aqui; o arquivo
     * ja veio com o jogo.
     */
    private static final String PROGRAMMER_ART = "programmer_art";

    /**
     * A marca em disco guarda uma VERSAO, nao um "ja fiz".
     *
     * Quando esta lista muda (foi o caso ao entrar o Programmer Art), quem ja tinha a
     * marca antiga nunca mais receberia o pack novo — o mod olharia o arquivo, veria
     * que existe, e iria embora. Guardar a versao deixa passar exatamente uma vez a
     * cada mudanca nossa, sem voltar a insistir depois.
     */
    private static final String MARKER_VERSION = "2";

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
        try {
            if (Files.exists(marker) && Files.readString(marker).trim().endsWith(MARKER_VERSION)) return;
        } catch (Throwable ignored) {
            return;
        }

        try {
            PackRepository repo = mc.getResourcePackRepository();
            repo.reload();

            // A selecao que vale e a do REPOSITORIO, nao a lista das opcoes.
            // Mexer so em options.resourcePacks nao liga nada: reloadResourcePacks()
            // chama repo.reload(), que reaplica a selecao do proprio repositorio e
            // joga fora o que escrevemos. Foi exatamente o que aconteceu na 1.11.1 —
            // o log dizia "ligadas" e nada mudava na tela.
            List<String> selected = new ArrayList<>(repo.getSelectedIds());
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
            Files.writeString(marker, "beta textures checked v" + MARKER_VERSION);

            // "base" antes de "compat": o de compatibilidade tem que vencer o de base.
            found.sort((a, b) -> Boolean.compare(
                    a.toLowerCase(Locale.ROOT).contains("compat"),
                    b.toLowerCase(Locale.ROOT).contains("compat")));

            // O Programmer Art vai na FRENTE de todos: na selecao do jogo, quem vem
            // depois manda. Ele e o piso — o Golden Days pinta por cima do que sabe
            // pintar, e o que sobra (as ferramentas) fica com a arte antiga da Mojang.
            if (repo.getAvailableIds().contains(PROGRAMMER_ART) && !selected.contains(PROGRAMMER_ART)) {
                found.add(0, PROGRAMMER_ART);
            }

            if (found.isEmpty()) return;

            selected.addAll(found);

            repo.setSelected(selected);                              // o que de fato liga
            mc.options.resourcePacks = new ArrayList<>(selected);    // para sobreviver ao restart
            mc.options.save();

            LOG.info("[REC] texturas beta ligadas: {}", found);
            mc.reloadResourcePacks();
        } catch (Throwable t) {
            LOG.warn("[REC] nao consegui ligar os packs de textura beta", t);
        }
    }

    private BetaTexturePacks() {}
}
