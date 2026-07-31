package net.vhsworld.rec.worldgen.dim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.List;

/**
 * A dimensao MAZE: um labirinto de paredes altas demais para se ver por cima.
 *
 * "Um labirinto gigantesco inspirado nesse video" — e o video e um daqueles em que a
 * camera anda por corredores de parede lisa que sobem alem do enquadramento. E dai que
 * sai a unica medida que importa aqui, e ela e do Pedro e nao minha: as paredes que ele
 * construiu tem 163 BLOCOS DE ALTURA sobre um chao de grama. Nao ha como pular, nao ha
 * como escalar, e nao ha como se orientar por nada que esteja acima da parede. Num
 * labirinto normal a saida e um problema de memoria; neste, e que voce nao tem mapa e
 * nunca vai ter.
 *
 * ============================ O QUE ELE CONSTRUIU ============================
 *
 * Oito arquivos `maze_*.schem`, que sao TRES construcoes — o md5 confere: maze_1=2=3,
 * maze_5=6, maze_7=8, e a maze_4 sozinha. Entram aqui as duas de 76x164x20, e a razao
 * e a unica que serve para uma grade: sao as duas com a MESMA PEGADA. A maze_4 (81x27)
 * e a maze_7 (142x103) abririam degrau de piso e fresta de parede em toda emenda.
 *
 * As duas nao sao intercambiaveis, e e por isso que as duas entram:
 *
 *   `hall`  (maze_1) e uma caixa FECHADA, com vao so na parede oeste.
 *   `cross` (maze_5) tem vao nas quatro.
 *
 * So `hall` daria um corredor unico sem bifurcacao; so `cross` daria um campo aberto com
 * pilares. Sorteadas meio a meio, o mapa tem beco sem saida E encruzilhada, que e o que
 * faz um labirinto ser um labirinto.
 *
 * ============================ QUEM ABRE AS PASSAGENS ============================
 *
 * ⚠️ O JAVA, E NAO A PECA — e esta e a decisao que sustenta a dimensao inteira. As duas
 * pecas trazem a parede pronta, e se o encaixe dependesse dos vaos DELAS o mapa teria
 * duas fraquezas fatais: uma casa de `hall` cercada de `hall` ficaria murada por todos os
 * lados (ela so abre a oeste), e o vao de 18 blocos de largura da parede oeste nao e uma
 * porta, e a parede faltando — encostar duas dessas daria um salao de 40 de largura, nao
 * um corredor.
 *
 * Entao a peca da a PAREDE e o Java da a PORTA: depois de carimbar, ele fura a divisa
 * entre duas casas vizinhas com um tunel de 4x5, e so quando o sorteio da aresta diz que
 * sim. Assim o desenho do labirinto e uma decisao de gerador (que se pode ajustar em uma
 * constante) e nao um efeito colateral de onde o Pedro parou de construir a parede.
 *
 * ⚠️ O TUNEL FURA 4 BLOCOS DE ESPESSURA, e o numero foi medido e nao arredondado: a
 * parede LESTE da peca tem DOIS blocos (x=74 e x=75, conferido no plano de y=5) enquanto
 * a norte tem um so. Furando so o plano da divisa, metade das portas daria em parede — e
 * uma porta que da em parede so se descobre depois de andar ate ela.
 */
public class MazeChunkGenerator extends StampChunkGenerator {

    public static final Codec<MazeChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, MazeChunkGenerator::new));

    private static final int MIN_Y = 0;
    private static final int GEN_HEIGHT = 176;

    /** O chao de grama e a camada y=0 da peca, entao a dimensao comeca no proprio zero. */
    public static final int FLOOR_Y = 0;

    /** A pegada das duas pecas, e portanto a grade. */
    public static final int CELL_X = 76;
    public static final int CELL_Z = 20;

    /**
     * A chance de haver passagem entre duas casas vizinhas.
     *
     * 0.55 e baixo de proposito, e e o que separa esta dimensao da BIBLIOTECA: la o
     * jogador precisa circular e a porta e cortesia; aqui o beco sem saida E o conteudo.
     * Abaixo de 0.5 o mapa comeca a se partir em bolsoes incomunicaveis (e o limiar de
     * percolacao da grade quadrada), e um bolsao fechado num labirinto de parede de 163
     * nao e um desafio, e uma prisao.
     */
    private static final double DOOR_CHANCE = 0.55D;

    /** O vao: 4 de largo, 5 de alto. Cabe uma pessoa e nao cabe uma vista. */
    public static final int DOOR_W = 4;
    private static final int DOOR_H = 5;

    /**
     * Quantos blocos de cada lado da divisa o tunel fura.
     *
     * 2, porque a parede mais grossa das pecas tem 2 (o muro leste, em x=74 e x=75). O
     * tunel vai de `divisa-2` a `divisa+1`, o que atravessa parede de 1 ou de 2 dos dois
     * lados. Onde a parede e fina ele sobra para dentro do corredor — e sobra nao faz mal
     * nenhum, enquanto falta faz uma porta cega.
     */
    public static final int PIERCE = 2;

    /** Onde a fita larga o jogador: uma casa qualquer neste raio de casas. */
    private static final int SPAWN_SPREAD = 256;

    /**
     * O ponto livre dentro da peca, em coordenada dela.
     *
     * MEDIDO nas DUAS: em (10,10) a coluna e grama em y=0 e ar de y=1 a y=5 em ambas.
     * Nao serve um ponto qualquer — a `cross` tem mato alto plantado em varias colunas, e
     * nascer dentro de um `tall_grass` nao machuca mas ja e nascer dentro de um bloco.
     */
    public static final int SPAWN_LOCAL_X = 10;
    public static final int SPAWN_LOCAL_Z = 10;

    // ------------------------------------------------------------------ o enfeite
    /**
     * ⚠️ POR QUE ESTA METADE DO ARQUIVO EXISTE. As duas pecas da grade diferem em 2318
     * blocos de 249280 — 0,9%, medido bloco a bloco. Ou seja o sorteio entre `hall` e
     * `cross` devolve praticamente a MESMA parede em toda casa, e a foto que o Pedro
     * mandou de dentro do jogo mostra o resultado: um corredor cinza que se repete igual
     * ate onde a vista alcanca. "Colocar variacoes nele de estruturas" nao da para
     * atender trocando o peso do sorteio, porque nao ha o que sortear.
     *
     * Entao a variacao entra DEPOIS do carimbo, e o vocabulario dela nao foi inventado:
     * e o da maze_7, a terceira construcao do Pedro, que eu li camada por camada. Ela
     * tem massa de pedra de alturas diferentes, pedregulho musgoso, trepadeira descendo
     * a parede e touceira de folha de carvalho boiando perto do chao. E isso que o Java
     * passa a espalhar — a peca dele nao entra inteira porque a pegada e 142x103 e a
     * grade e 76x20, mas o repertorio dela entra.
     */
    public static final int EDGE_X = 8;
    public static final int EDGE_Z = 4;

    /** Quantas massas de pedra podem nascer numa casa, e a chance de cada uma. */
    private static final int TOWERS = 3;
    private static final double TOWER_CHANCE = 0.55D;

    /** A pegada de uma massa: de MIN a MIN+RANGE-1 em cada eixo. */
    private static final int TOWER_W_MIN = 3, TOWER_W_RANGE = 6;
    private static final int TOWER_L_MIN = 3, TOWER_L_RANGE = 5;

    /** O maior enfeite possivel — o que o harness usa para medir a folga que sobra. */
    public static final int PROP_MAX_W = TOWER_W_MIN + TOWER_W_RANGE - 1;
    public static final int PROP_MAX_L = TOWER_L_MIN + TOWER_L_RANGE - 1;

    /**
     * A chance de uma massa ser TORRE em vez de toco.
     *
     * Sem isto todas teriam a mesma faixa de altura e o salao ficaria com tres tocos
     * parecidos — que e trocar uma repeticao por outra. Uma em cada cinco sobe quatro
     * vezes mais alto e vira referencia visual: e a unica coisa nesta dimensao que se
     * ve de longe e que nao e a parede.
     */
    private static final double TALL_CHANCE = 0.22D;

    /** Massa mais baixa que isto ganha copa de folha, como as da maze_7. */
    private static final int CANOPY_TOP = 12;

    /** Ate onde o desgaste da parede sobe. Acima disso ninguem olha, e custa. */
    private static final int WEATHER_TOP = 14;
    private static final double MOSS_CHANCE = 0.10D;
    private static final double VINE_CHANCE = 0.035D;
    private static final int VINE_MAX = 6;

    /** A cabana do Pedro (peso 0 no .bin: nunca sorteada como casa, so como enfeite). */
    private static final double CABIN_CHANCE = 0.14D;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState MOSS = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
    private static final BlockState LEAVES =
            Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

    private static final Direction[] SIDES =
            {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private volatile List<DimPiece> tiles;
    private volatile DimPiece cabinPiece;
    private volatile boolean cabinLooked;

    public MazeChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    private List<DimPiece> tiles() {
        List<DimPiece> ready = tiles;
        if (ready != null) return ready;
        synchronized (this) {
            if (tiles == null) tiles = PieceSet.get("maze").pieces();
            return tiles;
        }
    }

    /**
     * A cabana, procurada UMA vez.
     *
     * O sinalizador separado e porque a resposta legitima pode ser `null` (um .bin
     * antigo, sem a peca) — sem ele, cada casa sem cabana refaria a busca, e a busca
     * passa por um `synchronized` que as threads de geracao disputam.
     */
    private DimPiece cabinPiece() {
        if (cabinLooked) return cabinPiece;
        synchronized (this) {
            if (!cabinLooked) {
                cabinPiece = PieceSet.get("maze").byName("cabin");
                cabinLooked = true;
            }
            return cabinPiece;
        }
    }

    // ------------------------------------------------------------------ a grade
    private Placement cell(int cx, int cz) {
        List<DimPiece> all = tiles();
        if (all.isEmpty()) return null;
        DimPiece piece = DimHash.weighted(all, seed(), cx, cz, 51L);
        // So 0 e 180: a peca e 76x20, e girada 90 graus viraria 20x76 e sairia da grade.
        // O giro de 180 mantem a pegada e ja tira a repeticao de lugar — e o que faz a
        // saida unica da `hall` cair ora a oeste, ora a leste.
        int rotation = DimHash.pick(seed(), cx, cz, 52L, 2) * 2;
        return new Placement(piece, rotation, cx * CELL_X, FLOOR_Y, cz * CELL_Z);
    }

    private boolean door(int cx, int cz, boolean westward) {
        if (doorRaw(cx, cz, westward)) return true;
        // A trava contra a casa murada nos quatro lados. Ver a nota da GRASSROOMS: sem
        // ela, uma casa em cada 25 nasce sem saida — e aqui isso e pior, porque a fita
        // pode largar o jogador dentro dela e nao ha por onde subir.
        if (!westward) return false;
        return !doorRaw(cx, cz, false)
                && !doorRaw(cx + 1, cz, true)
                && !doorRaw(cx, cz + 1, false);
    }

    private boolean doorRaw(int cx, int cz, boolean westward) {
        int nx = westward ? cx - 1 : cx;
        int nz = westward ? cz : cz - 1;
        return DimHash.edge(seed(), nx, nz, cx, cz, westward ? 53L : 54L, DOOR_CHANCE);
    }

    // ------------------------------------------------------------------ o carimbo
    @Override
    protected void carve(Brush brush) {
        int cx0 = Math.floorDiv(brush.x0, CELL_X), cx1 = Math.floorDiv(brush.x1, CELL_X);
        int cz0 = Math.floorDiv(brush.z0, CELL_Z), cz1 = Math.floorDiv(brush.z1, CELL_Z);

        // ⚠️ DOIS PASSES, e nao um. O tunel de uma divisa apaga bloco das DUAS casas que
        // ela separa; furando dentro do mesmo laco que carimba, a casa carimbada depois
        // escreveria a parede dela por cima do tunel recem-aberto — e o defeito sairia so
        // em metade das portas, que e a metade que ninguem repara ate cair nela.
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                Placement placement = cell(cx, cz);
                if (placement != null && brush.touches(placement)) brush.stamp(placement);
            }
        }

        // O enfeite vem entre o carimbo e o tunel, e a ordem dos tres importa. Depois do
        // carimbo, senao a peca escreveria a parede dela por cima da massa; e ANTES do
        // tunel, para que o tunel apague o musgo e a trepadeira que cairem no vao — a
        // porta e a unica coisa desta dimensao que nao pode ter enfeite nenhum.
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                props(brush, cx, cz);
            }
        }
        weather(brush);

        for (int cx = cx0; cx <= cx1 + 1; cx++) {
            for (int cz = cz0; cz <= cz1 + 1; cz++) {
                pierce(brush, cx, cz);
            }
        }
    }

    // ------------------------------------------------------------------ as massas
    /**
     * O que esta casa tem de proprio: as massas de pedra e, de vez em quando, a cabana.
     *
     * Tudo aqui mora no MIOLO da casa, a `EDGE_X` das divisas leste/oeste e a `EDGE_Z`
     * das norte/sul, e a folga nao e estetica: e ela que garante que enfeite nenhum
     * possa tapar um tunel. O vao e furado no plano da divisa e sobra dois blocos para
     * dentro; com 8 e 4 de margem, o pior enfeite possivel ainda para longe dele.
     *
     * ⚠️ E ela tambem que garante que a casa nunca fecha. A massa mais funda tem 7 de
     * um salao de 20, e nao pode encostar em nenhuma das duas paredes longas — sobram
     * pelo menos 4 blocos livres dos dois lados, sempre. Um labirinto pode ter beco;
     * nao pode ter corredor entupido por sorteio.
     */
    private void props(Brush brush, int cx, int cz) {
        Placement cabin = cabin(cx, cz);
        for (int i = 0; i < TOWERS; i++) {
            long salt = 70L + i * 13L;
            if (DimHash.frac(seed(), cx, cz, salt) >= TOWER_CHANCE) continue;

            int w = TOWER_W_MIN + DimHash.pick(seed(), cx, cz, salt + 1L, TOWER_W_RANGE);
            int l = TOWER_L_MIN + DimHash.pick(seed(), cx, cz, salt + 2L, TOWER_L_RANGE);
            int x0 = cx * CELL_X + EDGE_X + DimHash.pick(seed(), cx, cz, salt + 3L,
                    CELL_X - 2 * EDGE_X - w + 1);
            int z0 = cz * CELL_Z + EDGE_Z + DimHash.pick(seed(), cx, cz, salt + 4L,
                    CELL_Z - 2 * EDGE_Z - l + 1);
            int x1 = x0 + w - 1, z1 = z0 + l - 1;

            if (x1 < brush.x0 || x0 > brush.x1 || z1 < brush.z0 || z0 > brush.z1) continue;
            if (onSpawn(cx, cz, x0, z0, x1, z1)) continue;
            if (cabin != null && x0 <= cabin.maxX() && cabin.minX() <= x1
                    && z0 <= cabin.maxZ() && cabin.minZ() <= z1) continue;

            int rise = 4 + DimHash.pick(seed(), cx, cz, salt + 5L, 24);
            if (DimHash.frac(seed(), cx, cz, salt + 6L) < TALL_CHANCE) rise *= 4;
            int top = Math.min(FLOOR_Y + rise, minY() + genHeight() - 4);

            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    for (int y = FLOOR_Y + 1; y <= top; y++) {
                        boolean mossy = DimHash.frac(seed(), x, z, y * 31L + 90L) < MOSS_CHANCE;
                        brush.set(x, y, z, mossy ? MOSS : STONE);
                    }
                }
            }
            if (top <= FLOOR_Y + CANOPY_TOP) canopy(brush, x0, z0, w, l, top);
        }
        if (cabin != null && brush.touches(cabin)) brush.stamp(cabin);
    }

    /** A touceira de folha em cima de um toco — a da maze_7, que boia perto do chao. */
    private void canopy(Brush brush, int x0, int z0, int w, int l, int top) {
        int mx = x0 + w / 2, mz = z0 + l / 2;
        int r = 2 + (w + l) / 8;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    // O `(dy-1)*2` achata a bola: copa e mais larga que alta.
                    if (dx * dx + dz * dz + (dy - 1) * (dy - 1) * 2 > r * r) continue;
                    brush.set(mx + dx, top + 1 + dy, mz + dz, LEAVES);
                }
            }
        }
    }

    /** Onde a cabana caiu nesta casa, ou null se nao caiu nenhuma. */
    private Placement cabin(int cx, int cz) {
        if (DimHash.frac(seed(), cx, cz, 61L) >= CABIN_CHANCE) return null;
        DimPiece piece = cabinPiece();
        if (piece == null) return null;

        int rotation = DimHash.pick(seed(), cx, cz, 62L, 4);
        int w = piece.rotatedWidth(rotation), l = piece.rotatedLength(rotation);
        int spanX = CELL_X - 2 * EDGE_X - w, spanZ = CELL_Z - 2 * EDGE_Z - l;
        if (spanX < 0 || spanZ < 0) return null;

        Placement placement = new Placement(piece, rotation,
                cx * CELL_X + EDGE_X + DimHash.pick(seed(), cx, cz, 63L, spanX + 1), FLOOR_Y,
                cz * CELL_Z + EDGE_Z + DimHash.pick(seed(), cx, cz, 64L, spanZ + 1));
        if (onSpawn(cx, cz, placement.minX(), placement.minZ(),
                placement.maxX(), placement.maxZ())) return null;
        return placement;
    }

    /**
     * O enfeite cairia em cima do ponto em que a fita larga o jogador?
     *
     * ⚠️ ISTO NAO E ZELO A MAIS, e o buraco que o enfeite abriu. `dimensionSpawn` larga
     * o jogador no ponto local (10,10), que foi medido como livre NA PECA — e o miolo
     * onde as massas nascem comeca no local (8,4), ou seja bem em cima dele. Sem esta
     * pergunta, uma casa em cada tantas passaria a receber o jogador dentro de uma
     * coluna de pedra de ate 112 blocos, e num labirinto sem mapa isso nao se percebe
     * como defeito: percebe-se como estar preso.
     *
     * A folga de 2 e o que cabe uma pessoa mais o passo para sair de dentro dela.
     */
    private boolean onSpawn(int cx, int cz, int x0, int z0, int x1, int z1) {
        BlockPos spawn = spawnIn(cx, cz);
        return x0 <= spawn.getX() + 2 && spawn.getX() - 2 <= x1
                && z0 <= spawn.getZ() + 2 && spawn.getZ() - 2 <= z1;
    }

    /** O ponto livre desta casa, ja convertido de coordenada da peca para o mundo. */
    private BlockPos spawnIn(int cx, int cz) {
        Placement placement = cell(cx, cz);
        if (placement == null) {
            return new BlockPos(cx * CELL_X + SPAWN_LOCAL_X, FLOOR_Y + 1,
                    cz * CELL_Z + SPAWN_LOCAL_Z);
        }
        return new BlockPos(placement.worldX(SPAWN_LOCAL_X, SPAWN_LOCAL_Z), FLOOR_Y + 1,
                placement.worldZ(SPAWN_LOCAL_X, SPAWN_LOCAL_Z));
    }

    // ------------------------------------------------------------------ o desgaste
    /**
     * Musgo e trepadeira nas faces de parede que dao para o corredor.
     *
     * ⚠️ A PERGUNTA "TEM AR DO LADO?" E FEITA A PECA, E NAO AO CHUNK. `brush.get`
     * devolve ar para tudo que esta fora do chunk da vez, entao no bloco da borda ele
     * responderia "exposto" para uma face que na verdade encosta na parede do chunk
     * vizinho — e sairia uma trepadeira presa dentro da pedra a cada 16 blocos, que e o
     * tipo de defeito que se ve de longe justamente por ser periodico. `pieceAt` resolve
     * a casa e a coordenada local do ponto pedido, entao ele atravessa borda de chunk e
     * ate divisa de casa sem mentir.
     */
    private void weather(Brush brush) {
        Cells cells = new Cells(brush);
        for (int x = brush.x0; x <= brush.x1; x++) {
            for (int z = brush.z0; z <= brush.z1; z++) {
                for (int y = FLOOR_Y + 1; y <= FLOOR_Y + WEATHER_TOP; y++) {
                    if (!pieceAt(cells, x, y, z).is(Blocks.STONE)) continue;
                    Direction open = exposed(cells, x, y, z);
                    if (open == null) continue;
                    if (DimHash.frac(seed(), x, z, y * 31L + 91L) < MOSS_CHANCE) {
                        brush.set(x, y, z, MOSS);
                    }
                    if (DimHash.frac(seed(), x, z, y * 31L + 92L) < VINE_CHANCE) {
                        vine(brush, cells, x, y, z, open);
                    }
                }
            }
        }
    }

    /**
     * As casas que este chunk alcanca, resolvidas UMA vez.
     *
     * ⚠️ ISTO E ALOCACAO, e nao arrumacao. `cell()` fabrica um `Placement` a cada
     * chamada, e o desgaste pergunta pela peca umas 18 mil vezes por chunk (16x16
     * colunas x 14 andares x ela mesma mais os quatro vizinhos). Sao uns 800 KB de lixo
     * por chunk numa dimensao infinita, so para reconstruir dezesseis objetos que nao
     * mudam. Um chunk toca no maximo tres casas por eixo (a casa tem 76x20 e o chunk 16),
     * entao a tabela e minuscula.
     */
    private final class Cells {
        private final int cx0, cz0, wide, deep;
        private final Placement[] slots;

        Cells(Brush brush) {
            cx0 = Math.floorDiv(brush.x0 - 1, CELL_X);
            cz0 = Math.floorDiv(brush.z0 - 1, CELL_Z);
            wide = Math.floorDiv(brush.x1 + 1, CELL_X) - cx0 + 1;
            deep = Math.floorDiv(brush.z1 + 1, CELL_Z) - cz0 + 1;
            slots = new Placement[wide * deep];
            for (int i = 0; i < wide; i++) {
                for (int j = 0; j < deep; j++) slots[i * deep + j] = cell(cx0 + i, cz0 + j);
            }
        }

        Placement at(int cx, int cz) {
            int i = cx - cx0, j = cz - cz0;
            // Fora da janela nao devia acontecer, mas cair de volta no calculo e mais
            // barato que descobrir o engano por um bloco no lugar errado.
            if (i < 0 || j < 0 || i >= wide || j >= deep) return cell(cx, cz);
            return slots[i * deep + j];
        }
    }

    /** Para que lado esta pedra da para o corredor, ou null se ela esta enterrada. */
    private Direction exposed(Cells cells, int x, int y, int z) {
        for (Direction side : SIDES) {
            if (pieceAt(cells, x + side.getStepX(), y, z + side.getStepZ()).isAir()) return side;
        }
        return null;
    }

    /**
     * Uma cortina de trepadeira descendo pela face exposta.
     *
     * Ela nasce no bloco de AR ao lado da pedra e aponta de volta para ela: uma
     * trepadeira so se sustenta se o lado marcado tiver face cheia, e a pedra da parede
     * tem. Descendo, para no primeiro andar em que a parede acabou — trepadeira sem
     * parede atras cai no primeiro tique.
     */
    private void vine(Brush brush, Cells cells, int x, int y, int z, Direction open) {
        int ax = x + open.getStepX(), az = z + open.getStepZ();
        BlockState state = Blocks.VINE.defaultBlockState()
                .setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(open.getOpposite()), true);
        int len = 1 + DimHash.pick(seed(), x, z, y * 31L + 93L, VINE_MAX);
        for (int i = 0; i < len; i++) {
            int vy = y - i;
            if (vy <= FLOOR_Y) return;
            if (!pieceAt(cells, ax, vy, az).isAir()) return;
            if (!pieceAt(cells, x, vy, z).is(Blocks.STONE)) return;
            brush.set(ax, vy, az, state);
        }
    }

    /**
     * O que a PECA tem naquele ponto do mundo — atravessando divisa de casa.
     *
     * Nao le o chunk de proposito (ver a nota de `weather`), e por isso responde sobre a
     * parede que ainda nem foi carimbada na casa vizinha.
     */
    private BlockState pieceAt(Cells cells, int x, int y, int z) {
        if (y < FLOOR_Y) return AIR;
        Placement placement = cells.at(Math.floorDiv(x, CELL_X), Math.floorDiv(z, CELL_Z));
        if (placement == null || y - FLOOR_Y >= placement.piece.height) return AIR;
        int lx = placement.localX(x, z), lz = placement.localZ(x, z);
        if (lx < 0 || lz < 0 || lx >= placement.piece.width || lz >= placement.piece.length) {
            return AIR;
        }
        return placement.piece.at(lx, y - FLOOR_Y, lz, placement.rotation);
    }

    /**
     * Os dois tuneis desta casa: o da divisa oeste e o da divisa norte.
     *
     * DOIS e nao quatro, pelo mesmo motivo das paredes da GRASSROOMS e da BIBLIOTECA — a
     * divisa leste desta casa e a divisa oeste da vizinha de la, e quem a fura e ela. O
     * laco de `carve` vai um a mais nos dois eixos justamente para alcancar a divisa da
     * casa seguinte quando ela cai dentro deste chunk.
     */
    private void pierce(Brush brush, int cx, int cz) {
        int wall = cx * CELL_X;
        if (door(cx, cz, true)) {
            int from = cz * CELL_Z + (CELL_Z - DOOR_W) / 2;
            for (int x = wall - PIERCE; x < wall + PIERCE; x++) {
                for (int z = from; z < from + DOOR_W; z++) {
                    brush.column(x, FLOOR_Y + 1, FLOOR_Y + DOOR_H, z, AIR);
                }
            }
        }

        wall = cz * CELL_Z;
        if (door(cx, cz, false)) {
            int from = cx * CELL_X + (CELL_X - DOOR_W) / 2;
            for (int z = wall - PIERCE; z < wall + PIERCE; z++) {
                for (int x = from; x < from + DOOR_W; x++) {
                    brush.column(x, FLOOR_Y + 1, FLOOR_Y + DOOR_H, z, AIR);
                }
            }
        }
    }

    // ------------------------------------------------------------------ o spawn
    /**
     * Numa casa sorteada, no ponto que o plano das duas pecas diz estar livre.
     *
     * Passando pela `Placement`, o giro de 180 sai de graca: o ponto espelhado tambem e
     * ar, porque a peca inteira e a mesma peca. E como toda casa e uma caixa de parede de
     * 163, cair numa casa diferente e indistinguivel de cair na mesma — a regra do Pedro
     * ("nunca no mesmo spawn") aqui rende de graca o desnorteamento que a dimensao quer.
     */
    @Override
    public BlockPos dimensionSpawn() {
        java.util.Random dice = new java.util.Random();
        return spawnIn(dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1),
                dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1));
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
        return "MAZE";
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return FLOOR_Y + 1;
    }

    // ------------------------------------------------------------------ a saida
    @Override
    public String dimensionId() {
        return "maze";
    }

    /** O mesmo `spawnIn` do spawn, mas na casa fixa da regiao em vez de uma sorteada. */
    @Override
    public BlockPos exitAnchor(int rx, int rz) {
        return spawnIn(ExitSite.cellInRegion(rx, CELL_X), ExitSite.cellInRegion(rz, CELL_Z));
    }
}
