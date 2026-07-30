import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.dimension.DimensionType;
import java.nio.file.*;

public class FieldCheck {
    public static void main(String[] a) throws Exception {
        try { SharedConstants.setVersion(net.minecraft.DetectedVersion.BUILT_IN); Bootstrap.bootStrap(); }
        catch (Throwable t) { }
        int bad = 0;
        Path dir = Paths.get("src/main/resources/data/recmod/dimension_type");
        for (Path p : Files.newDirectoryStream(dir, "*.json")) {
            var json = JsonParser.parseString(Files.readString(p));
            var result = DimensionType.DIRECT_CODEC.parse(JsonOps.INSTANCE, json);
            if (result.error().isPresent()) {
                System.out.println("  XX " + p.getFileName() + ": " + result.error().get().message());
                bad++;
            } else {
                DimensionType dt = result.result().get();
                System.out.printf("  ok %-22s minY=%d height=%d ceu=%s hora=%s luz=%.2f%n",
                        p.getFileName(), dt.minY(), dt.height(), dt.hasSkyLight(),
                        dt.fixedTime().isPresent() ? String.valueOf(dt.fixedTime().getAsLong()) : "livre",
                        dt.ambientLight());
            }
        }
        System.out.println(bad == 0 ? "\ntodos os dimension_type passaram pelo codec do jogo"
                                    : "\n" + bad + " REPROVADO(S)");
        System.exit(bad == 0 ? 0 : 1);
    }
}
