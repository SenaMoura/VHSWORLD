import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.biome.Biome;
import java.nio.file.*;

/** Passa cada bioma nosso pelo codec do proprio jogo: e o que o servidor vai fazer. */
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
        System.out.println(bad == 0 ? "\ntodos os biomas passaram pelo codec do jogo" : "\n" + bad + " REPROVADO(S)");
        System.exit(bad == 0 ? 0 : 1);
    }
}
