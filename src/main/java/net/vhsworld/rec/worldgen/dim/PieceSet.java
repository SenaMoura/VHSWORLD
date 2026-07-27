package net.vhsworld.rec.worldgen.dim;

import com.mojang.logging.LogUtils;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Le o arquivo que o `import_dimension.py` assou.
 *
 * LE DO JAR, e nao do gerenciador de recursos. O gerador de chunk roda fora da
 * thread do servidor, antes de o mundo existir e as vezes sem `Level` na mao —
 * pedir recurso ali e uma corrida esperando acontecer. O classpath esta sempre
 * pronto. O preco e que datapack nao consegue trocar as pecas, o que para o que
 * elas sao (a planta de uma dimensao do mod) nao faz falta.
 */
public final class PieceSet {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, PieceSet> CACHE = new HashMap<>();

    private static final byte[] MAGIC = {'V', 'H', 'S', 'D'};
    private static final int VERSION = 2;

    private final List<DimPiece> pieces;
    private final DimPiece hub;

    private PieceSet(List<DimPiece> pieces, DimPiece hub) {
        this.pieces = pieces;
        this.hub = hub;
    }

    public List<DimPiece> pieces() {
        return pieces;
    }

    public DimPiece hub() {
        return hub;
    }

    /** Uma peca pelo nome que ela tem no `import_dimension.py`. */
    public DimPiece byName(String name) {
        for (DimPiece piece : pieces) {
            if (piece.name.equals(name)) return piece;
        }
        return null;
    }

    /** `name` e o nome do arquivo sem extensao, em `data/recmod/dimension_pieces/`. */
    public static synchronized PieceSet get(String name) {
        PieceSet cached = CACHE.get(name);
        if (cached != null) return cached;

        PieceSet loaded;
        String path = "/data/recmod/dimension_pieces/" + name + ".bin";
        try (InputStream in = PieceSet.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("nao achei " + path);
            loaded = read(new DataInputStream(new BufferedInputStream(in)));
            LOGGER.info("[dimensao] {} carregada: {} pecas", name, loaded.pieces.size());
        } catch (Exception e) {
            // Devolver um conjunto vazio deixa a dimensao nascer VAZIA em vez de
            // derrubar o servidor. O mod ja tem essa licao das malhas: nunca
            // crashar de proposito quando da para degradar.
            LOGGER.error("[dimensao] falhei ao ler {}: {}", path, e.toString());
            loaded = new PieceSet(List.of(), null);
        }
        CACHE.put(name, loaded);
        return loaded;
    }

    private static PieceSet read(DataInputStream in) throws Exception {
        for (byte expected : MAGIC) {
            if (in.readByte() != expected) throw new IllegalStateException("assinatura errada");
        }
        int version = in.readInt();
        if (version != VERSION) throw new IllegalStateException("versao " + version + ", esperava " + VERSION);

        int count = in.readInt();
        List<DimPiece> pieces = new ArrayList<>(count);
        DimPiece hub = null;

        for (int p = 0; p < count; p++) {
            String name = in.readUTF();
            int width = in.readInt(), height = in.readInt(), length = in.readInt();
            int weight = in.readInt();
            boolean isHub = in.readByte() != 0;
            int anchorX = in.readInt(), anchorY = in.readInt(), anchorZ = in.readInt();

            int paletteSize = in.readInt();
            BlockState[] palette = new BlockState[paletteSize];
            for (int i = 0; i < paletteSize; i++) {
                palette[i] = parse(in.readUTF());
            }

            int byteLength = in.readInt();
            short[] cells = new short[byteLength / 2];
            for (int i = 0; i < cells.length; i++) {
                cells[i] = in.readShort();
            }

            int connectorCount = in.readInt();
            Connector[] connectors = new Connector[connectorCount];
            for (int i = 0; i < connectorCount; i++) {
                connectors[i] = new Connector(in.readByte(), in.readInt(), in.readInt(),
                        in.readInt(), in.readInt(), in.readByte());
            }

            DimPiece piece = new DimPiece(name, width, height, length, weight, isHub,
                    anchorX, anchorY, anchorZ, palette, cells, connectors);
            pieces.add(piece);
            if (isHub) hub = piece;
        }
        return new PieceSet(List.copyOf(pieces), hub);
    }

    /**
     * "minecraft:polished_andesite_stairs[facing=east,half=bottom]" -> BlockState.
     *
     * Um estado que nao existe mais (bloco renomeado entre versoes) vira pedra em
     * vez de excecao: perde-se um bloco, nao a dimensao inteira.
     */
    private static BlockState parse(String description) {
        try {
            return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), description, false)
                    .blockState();
        } catch (Exception e) {
            LOGGER.warn("[dimensao] estado desconhecido '{}', virou pedra", description);
            return Blocks.STONE.defaultBlockState();
        }
    }
}
