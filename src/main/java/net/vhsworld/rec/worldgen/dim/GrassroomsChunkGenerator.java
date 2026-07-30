package net.vhsworld.rec.worldgen.dim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.List;

/**
 * A dimensao GRASSROOMS: um liminal space branco em que cresce grama.
 *
 * O pedido do Pedro e "um liminal space inspirado no level das backrooms", e a foto que
 * ele deu e um salao branco alto com canteiros de grama em plataformas de alturas
 * diferentes. E a unica dimensao ILUMINADA do mod, e e de proposito: o que assusta ali
 * nao e nao ver, e ver tudo e nao haver nada.
 *
 * ============================ POR QUE A CASCA E DO JAVA ============================
 *
 * As outras dimensoes sao a peca do Pedro carimbada e ponto. Esta nao PODE ser, e eu
 * conferi antes de escolher: medi as quatro paredes das cinco salas e elas nao sao
 * modulos. A parede leste da `room_b` e ar de ponta a ponta; a `room_c` tem TRES paredes
 * inteiras abertas; a `room_e`, duas. Sao recortes de uma obra continua, nao pecas
 * seladas com porta como as da DATA — a deteccao automatica de porta (que exige vao
 * comecando em y=1) nao acha uma unica porta em nenhuma delas, e encaixa-las pela borda
 * encostaria parede aberta em parede aberta: a sala vizinha apareceria por dentro.
 *
 * Entao a regra aqui se inverte. Cada casa da grade e uma SALA CONSTRUIDA pelo Java —
 * piso, teto e paredes de quartzo — e a peca do Pedro fica DENTRO dela, sem tocar as
 * paredes. O que estava aberto nela deixa de ser vazamento e passa a ser a entrada; e o
 * corredor de 4 blocos que sobra em volta e o que faz a sala ser sempre atravessavel,
 * seja qual for a peca que caiu ali.
 *
 * ⚠️ A ALTURA DO TETO E A DA PECA, e nao um numero fixo. As cinco tem 8, 14, 9, 9 e 10
 * de altura. Teto fixo no maior deixaria a sala pequena com o proprio teto dela
 * flutuando no meio do ar; teto fixo no menor cortaria a `room_b` pela metade. Como cada
 * sala tem a sua altura, a parede entre duas vizinhas sobe ate o teto da MAIS ALTA das
 * duas — senao a mais alta amanhece com uma fresta para a vizinha ao longo de todo o
 * limite, que e o tipo de buraco que se descobre caindo nele.
 *
 * ⚠️ A LUZ NAO E ENFEITE, E O QUE MANTEM A GRAMA VIVA. Sem ceu e sem lampada, o jogo
 * transforma `grass_block` em terra no primeiro tick aleatorio (luz < 4) e derruba o mato
 * de cima. `ambient_light` do dimension_type NAO resolve: ele muda o quanto a tela
 * clareia, nao o nivel de luz que o bloco consulta. Entao a casca planta blocos
 * `minecraft:light` — invisiveis — logo abaixo do teto, de 8 em 8. Eles entram DEPOIS da
 * peca e olhando o que ja esta escrito, para caírem tambem dentro dos comodos fechados
 * que a propria peca tem: e la que a grama morreria primeiro.
 */
public class GrassroomsChunkGenerator extends StampChunkGenerator {

    public static final Codec<GrassroomsChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, GrassroomsChunkGenerator::new));

    private static final int MIN_Y = 0;
    private static final int GEN_HEIGHT = 128;

    /** O piso. O mesmo em toda a dimensao: liminal space nao tem topografia. */
    public static final int FLOOR_Y = 64;

    /**
     * O lado de uma sala.
     *
     * 56 nao e gosto: a maior peca mede 43 no eixo Z, e a sala tem que caber ela MAIS o
     * corredor de 4 dos dois lados mais as duas paredes. 43+8+2 = 53, e 56 deixa folga.
     * Menor que isso e a `room_b` encosta na parede e a porta daquele lado abre contra
     * ela — um beco que o jogador so descobre andando ate o fim.
     */
    private static final int CELL = 56;

    /** O corredor branco que sobra em volta da peca. E ele que garante a passagem. */
    private static final int MARGIN = 4;

    /** A porta entre duas salas: 4 de largo, 5 de alto, no meio da parede. */
    private static final int DOOR_W = 4;
    private static final int DOOR_H = 5;

    /**
     * A chance de duas salas vizinhas terem porta entre elas.
     *
     * Com 0.62 nos dois eixos, a chance de uma sala ficar com as quatro paredes fechadas
     * e (0.38)^4, uns 2% — e num liminal space uma sala inalcancavel nem e defeito, e
     * paisagem que voce nunca ve. Mas ela seria defeito se a fita largasse o jogador
     * DENTRO dela, entao `door` tem uma trava: sala sem nenhuma porta ganha a do oeste.
     */
    private static final double DOOR_CHANCE = 0.62D;

    /** De quantos em quantos blocos entra uma lampada invisivel no teto. */
    private static final int LIGHT_EVERY = 8;

    private static final int SPAWN_SPREAD = 64;

    private static final BlockState FLOOR = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState CEILING = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState WALL = Blocks.QUARTZ_BLOCK.defaultBlockState();
    private static final BlockState LIGHT = Blocks.LIGHT.defaultBlockState();

    private volatile List<DimPiece> rooms;

    public GrassroomsChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    private List<DimPiece> rooms() {
        List<DimPiece> ready = rooms;
        if (ready != null) return ready;
        synchronized (this) {
            if (rooms == null) rooms = PieceSet.get("grassrooms").pieces();
            return rooms;
        }
    }

    // ------------------------------------------------------------------ a grade
    /** Qual das cinco salas caiu nesta casa. */
    private DimPiece roomAt(int cx, int cz) {
        List<DimPiece> all = rooms();
        return all.isEmpty() ? null : DimHash.weighted(all, seed(), cx, cz, 11L);
    }

    /** A altura do teto desta casa, contada do piso. */
    private int heightAt(int cx, int cz) {
        DimPiece room = roomAt(cx, cz);
        return room == null ? 9 : room.height;
    }

    /**
     * Ha porta entre esta casa e a vizinha? `westward` diz em qual das duas paredes.
     *
     * A pergunta e feita pela casa da DIREITA (ou de baixo): assim cada parede e
     * desenhada por um lado so, e a resposta e a mesma vindo de qualquer um deles.
     */
    private boolean door(int cx, int cz, boolean westward) {
        if (doorRaw(cx, cz, westward)) return true;
        // A trava: se nenhuma das quatro deu porta, a do oeste entra na marra.
        if (!westward) return false;
        return !doorRaw(cx, cz, false)
                && !doorRaw(cx + 1, cz, true)
                && !doorRaw(cx, cz + 1, false);
    }

    private boolean doorRaw(int cx, int cz, boolean westward) {
        int nx = westward ? cx - 1 : cx;
        int nz = westward ? cz : cz - 1;
        return DimHash.edge(seed(), nx, nz, cx, cz, westward ? 12L : 13L, DOOR_CHANCE);
    }

    // ------------------------------------------------------------------ o carimbo
    @Override
    protected void carve(Brush brush) {
        int cx0 = Math.floorDiv(brush.x0, CELL), cx1 = Math.floorDiv(brush.x1, CELL);
        int cz0 = Math.floorDiv(brush.z0, CELL), cz1 = Math.floorDiv(brush.z1, CELL);

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                shell(brush, cx, cz);
            }
        }
        // A peca depois da casca: onde ela tem bloco, ela manda; onde tem ar, o branco
        // do Java continua de pe. A varredura passa uma casa para tras porque a peca
        // fica dentro da casa dela e nunca a transborda — mas a casa a oeste ainda pode
        // ter a peca dela encostando neste chunk.
        for (int cx = cx0 - 1; cx <= cx1; cx++) {
            for (int cz = cz0 - 1; cz <= cz1; cz++) {
                DimPiece room = roomAt(cx, cz);
                if (room == null) continue;
                Placement placement = place(room, cx, cz);
                if (brush.touches(placement)) brush.stamp(placement);
            }
        }
        lights(brush, cx0, cz0, cx1, cz1);
    }

    /** A peca centralizada no miolo da sala, dentro do corredor. */
    private Placement place(DimPiece room, int cx, int cz) {
        int inner = CELL - 1 - 2 * MARGIN;
        int ox = cx * CELL + 1 + MARGIN + Math.max(0, (inner - room.width) / 2);
        int oz = cz * CELL + 1 + MARGIN + Math.max(0, (inner - room.length) / 2);
        return new Placement(room, 0, ox, FLOOR_Y, oz);
    }

    /**
     * O piso, o teto e as duas paredes desta sala.
     *
     * SO DUAS PAREDES: a do oeste e a do norte. A do leste e a parede oeste da vizinha
     * de la, e a do sul e a norte da de baixo. Desenhar as quatro faria toda parede
     * interna ter 2 blocos de espessura e, pior, obrigaria as duas vizinhas a concordar
     * sobre onde fica a porta — que e o tipo de acordo que quebra quando as duas sao
     * carimbadas por threads diferentes.
     */
    private void shell(Brush brush, int cx, int cz) {
        int bx = cx * CELL, bz = cz * CELL;
        int height = heightAt(cx, cz);
        int ceiling = FLOOR_Y + height - 1;

        int x0 = Math.max(bx, brush.x0), x1 = Math.min(bx + CELL - 1, brush.x1);
        int z0 = Math.max(bz, brush.z0), z1 = Math.min(bz + CELL - 1, brush.z1);

        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                brush.set(x, FLOOR_Y, z, FLOOR);
                brush.set(x, ceiling, z, CEILING);
            }
        }

        // Parede oeste: corre em Z, no plano x = bx.
        if (bx >= brush.x0 && bx <= brush.x1) {
            int top = FLOOR_Y + Math.max(height, heightAt(cx - 1, cz)) - 1;
            int gap = bz + (CELL - DOOR_W) / 2;
            boolean open = door(cx, cz, true);
            for (int z = z0; z <= z1; z++) {
                for (int y = FLOOR_Y + 1; y <= top; y++) {
                    if (open && z >= gap && z < gap + DOOR_W && y < FLOOR_Y + 1 + DOOR_H) continue;
                    brush.set(bx, y, z, WALL);
                }
            }
        }

        // Parede norte: corre em X, no plano z = bz.
        if (bz >= brush.z0 && bz <= brush.z1) {
            int top = FLOOR_Y + Math.max(height, heightAt(cx, cz - 1)) - 1;
            int gap = bx + (CELL - DOOR_W) / 2;
            boolean open = door(cx, cz, false);
            for (int x = x0; x <= x1; x++) {
                for (int y = FLOOR_Y + 1; y <= top; y++) {
                    if (open && x >= gap && x < gap + DOOR_W && y < FLOOR_Y + 1 + DOOR_H) continue;
                    brush.set(x, y, bz, WALL);
                }
            }
        }
    }

    /**
     * As lampadas invisiveis, de 8 em 8, procurando de cima para baixo.
     *
     * DE CIMA PARA BAIXO, e nao de baixo para cima: descendo, a lampada para no primeiro
     * ar abaixo do teto — que dentro de um comodo fechado da peca e o teto DELE, e la
     * dentro. Subindo, ela pararia no primeiro ar acima do piso, que num canteiro de
     * grama e a altura do tornozelo: ficaria uma lampada no chao iluminando so o proprio
     * canteiro e o resto da sala escureceria.
     */
    private void lights(Brush brush, int cx0, int cz0, int cx1, int cz1) {
        int highest = FLOOR_Y;
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                highest = Math.max(highest, FLOOR_Y + heightAt(cx, cz) - 1);
            }
        }
        for (int x = brush.x0; x <= brush.x1; x++) {
            if (Math.floorMod(x, LIGHT_EVERY) != 0) continue;
            for (int z = brush.z0; z <= brush.z1; z++) {
                if (Math.floorMod(z, LIGHT_EVERY) != 0) continue;
                for (int y = highest - 1; y > FLOOR_Y + 1; y--) {
                    if (!brush.get(x, y, z).isAir()) continue;
                    brush.set(x, y, z, LIGHT);
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------ o spawn
    /**
     * No corredor branco de uma sala sorteada — nunca dentro da peca.
     *
     * O canto do corredor e o unico lugar de que se sabe, sem olhar nada, que tem piso
     * embaixo e ar em cima: a peca comeca 4 blocos depois dele e a parede acaba 2 antes.
     */
    @Override
    public BlockPos dimensionSpawn() {
        java.util.Random dice = new java.util.Random();
        int cx = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
        int cz = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
        return new BlockPos(cx * CELL + 2, FLOOR_Y + 1, cz * CELL + 2);
    }

    // ------------------------------------------------------------------ o resto
    @Override
    protected Codec<? extends net.minecraft.world.level.chunk.ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    protected int minY() {
        return MIN_Y;
    }

    @Override
    protected int genHeight() {
        return GEN_HEIGHT;
    }

    @Override
    protected String name() {
        return "GRASSROOMS";
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return FLOOR_Y + 1;
    }
}
