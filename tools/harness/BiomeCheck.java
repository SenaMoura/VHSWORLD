import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.biome.Biome;
import java.nio.file.*;

/**
 * Passa cada bioma nosso pelo codec do proprio jogo: e o que o servidor vai fazer.
 *
 * ⚠️ A COLUNA `trilha` SEMPRE DIZ "nenhuma", E ISSO NAO E DEFEITO. O lookup aqui e o
 * `VanillaRegistries`, que so tem o que e da Mojang; os nossos `recmod:music_*` nao estao
 * nele porque o registro do mod nao roda fora do launcher. E o `music` do bioma e um
 * `optionalFieldOf`, que ENGOLE o erro quando o som nao resolve e devolve vazio.
 *
 * Foi conferido trocando o som de um bioma por um vanilla: o vanilla aparece, o nosso nao.
 * Ou seja, o caminho do codec esta certo e o que falta e o registro.
 *
 * A consequencia que importa e outra, e vale ler duas vezes: como o campo e opcional e
 * engole erro, um nome de som ERRADO num bioma nao da erro em lugar nenhum — a dimensao
 * so nasce muda. Quem confere isso e `tools/check_sounds.py`, que compara o nome pedido
 * pelo bioma com o sounds.json e com o ModSounds. Nao adianta procurar aqui.
 */
public class BiomeCheck {
    public static void main(String[] a) throws Exception {
        try { SharedConstants.setVersion(net.minecraft.DetectedVersion.BUILT_IN); Bootstrap.bootStrap(); }
        catch (Throwable t) { }
        var ops = RegistryOps.create(JsonOps.INSTANCE, VanillaRegistries.createLookup());
        int bad = 0;
        Path dir = Paths.get("src/main/resources/data/recmod/worldgen/biome");
        for (Path p : Files.newDirectoryStream(dir, "*.json")) {
            if (Files.size(p) == 0) { System.out.println("  XX " + p.getFileName() + ": ZERO BYTES"); bad++; continue; }
            var result = Biome.DIRECT_CODEC.parse(ops, JsonParser.parseString(Files.readString(p)));
            if (result.error().isPresent()) {
                System.out.println("  XX " + p.getFileName() + ": " + result.error().get().message());
                bad++;
            } else {
                Biome b = result.result().get();
                System.out.printf("  ok %-22s neblina=%06X ceu=%06X trilha=%s%n", p.getFileName(),
                        b.getFogColor(), b.getSkyColor(),
                        b.getBackgroundMusic().isPresent()
                                ? b.getBackgroundMusic().get().getEvent().value().getLocation() : "nenhuma");
            }
        }
        System.out.println("\n(trilha=nenhuma e esperado: os sons do mod nao existem fora do"
                + " launcher. Quem confere trilha e tools/check_sounds.py)");
        System.out.println(bad == 0 ? "todos os biomas passaram pelo codec do jogo" : bad + " REPROVADO(S)");
        System.exit(bad == 0 ? 0 : 1);
    }
}
