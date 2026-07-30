package net.vhsworld.rec.worldgen.dim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.vhsworld.rec.RECMod;

/**
 * A dimensao PARKOURLAND: a unica que NAO e infinita, e a unica de que se cai para fora.
 *
 * "diferente de todos ele nn e infinito e sim contem apenas uma plataforma dentro uma
 * grade de madeira gigantesca, o jogador spawn dentro dele e havera um parkour que ele
 * tera que fazer ate chegar no topo, se ele errar ele cai no void e e levado pra alguma
 * dimensao aleatoria menos a DATA."
 *
 * ============================ O QUE O PEDRO CONSTRUIU ============================
 *
 * ⚠️ SO A GAIOLA. Conferi camada por camada antes de escrever uma linha, porque o resto
 * do desenho depende disso: dos 22960 blocos do `parkour_land.schem`, 4092 sao tabua
 * (piso em y=0..1 e um tampo macico de 5 camadas em y=180..184) e os outros 18868 sao
 * cerca — 106 por camada, de y=2 a y=179, que e exatamente o perimetro de uma caixa
 * 30x25. Nao ha UM bloco entre y=2 e y=179 que nao seja o muro.
 *
 * Ou seja: o recipiente e dele, o PARKOUR DE DENTRO NAO EXISTE NO ARQUIVO. Ele e
 * desenhado aqui, e por isso e desenhado como uma regra e nao como uma lista — assim o
 * Pedro pode trocar a dificuldade em uma constante, ou substituir tudo por um schematic
 * proprio depois, sem que nada mais mude.
 *
 * E "apenas uma plataforma" tambem esta no arquivo, e literal: em y=0 a tabua nao cobre
 * o piso todo, cobre o perimetro MAIS um retangulo de 11x13 no canto noroeste. O resto
 * de y=0 e aberto. Aquele retangulo e onde o jogador nasce, e o buraco em volta e por
 * onde ele sai da dimensao quando erra — nao precisei abrir nada.
 *
 * ============================ O PARKOUR ============================
 *
 * Uma espiral: as plataformas andam em volta da parede interna e sobem UM bloco cada.
 * Subir um por pulo e a unica altura que se vence sem bloco de apoio, e andar 3 no plano
 * e o pulo de corrida padrao — os dois numeros juntos dao a escada mais longa que ainda e
 * so pulo. Da gaiola inteira saem 177 saltos.
 *
 * O passo de 3 contra um perimetro de 82 nao fecha em volta inteira (82 nao e multiplo de
 * 3), e isso e o ponto: a espiral escorrega um pouco a cada volta, entao as plataformas
 * nunca empilham em coluna e nunca da para subir duas de uma vez pelo mesmo lado.
 *
 * De 20 em 20 vem uma plataforma de descanso, mais larga e de outra madeira. Nao e
 * bondade: sem um lugar em que da para soltar o teclado, 177 saltos seguidos nao sao
 * dificeis, sao so compridos.
 */
public class ParkourlandChunkGenerator extends StampChunkGenerator {

    public static final Codec<ParkourlandChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, ParkourlandChunkGenerator::new));

    private static final int MIN_Y = 0;
    private static final int GEN_HEIGHT = 256;

    /** Onde a base da gaiola encosta. */
    public static final int CAGE_Y = 1;

    /** A pegada da gaiola, do schematic. */
    private static final int CAGE_W = 30;
    private static final int CAGE_L = 25;

    /** A plataforma de nascimento, em coordenada da gaiola: o retangulo de tabua de y=0. */
    private static final int START_X0 = 1, START_X1 = 9;
    private static final int START_Z0 = 2, START_Z1 = 12;

    // O caminho da espiral: um retangulo 3 blocos dentro da cerca.
    private static final int PATH_X0 = 3, PATH_X1 = 26;
    private static final int PATH_Z0 = 3, PATH_Z1 = 21;
    private static final int RUN_X = PATH_X1 - PATH_X0;   // 23
    private static final int RUN_Z = PATH_Z1 - PATH_Z0;   // 18
    private static final int PERIMETER = 2 * RUN_X + 2 * RUN_Z;   // 82

    /**
     * Onde a espiral comeca a contar.
     *
     * 9 e a primeira posicao do caminho que cai FORA da plataforma de nascimento (x=12,
     * e a plataforma acaba em x=10). Comecando em 0, o primeiro degrau nasceria em cima
     * da propria plataforma, um bloco no meio do pe de quem acabou de chegar — e o
     * primeiro salto de verdade seria o segundo.
     */
    private static final int ARC_START = 9;

    /** Quanto se anda no plano de um degrau ao seguinte. 3 = pulo de corrida. */
    private static final int STRIDE = 3;

    /** De quantos em quantos degraus vem um descanso. */
    private static final int REST_EVERY = 20;

    /**
     * O lado de cada tipo de plataforma.
     *
     * 2 e o degrau: um bloco so seria pulo de precisao de pixel, e 3 ja e largo o bastante
     * para se andar em vez de pular. O descanso de 4 e onde da para soltar o teclado, e o
     * tampo de 5 e o topo — chegar la e ter espaco de sobra e parte de ter chegado.
     */
    private static final int STEP_SIDE = 2;
    private static final int REST_SIDE = 4;
    private static final int TOP_SIDE = 5;

    /** O primeiro degrau, e quantos ha. O ultimo tem que sobrar 2 para o tampo. */
    private static final int FIRST_Y = CAGE_Y + 1;
    private static final int STEPS = 177;

    private static final BlockState STEP = Blocks.OAK_PLANKS.defaultBlockState();
    private static final BlockState REST = Blocks.SPRUCE_PLANKS.defaultBlockState();

    /** O premio do topo usa o mesmo bau da DATA: e o mesmo mundo, e o mesmo achado. */
    private static final ResourceLocation LOOT =
            ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "chests/data");

    private volatile DimPiece cage;
    private volatile boolean looked;

    public ParkourlandChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    private DimPiece cage() {
        if (looked) return cage;
        synchronized (this) {
            if (!looked) {
                cage = PieceSet.get("parkourland").hub();
                looked = true;
            }
            return cage;
        }
    }

    // ------------------------------------------------------------------ a espiral
    /**
     * Onde cai o degrau numero `k`: {x, z, y}.
     *
     * O caminho e o perimetro do retangulo interno, percorrido por comprimento de arco.
     * Os quatro trechos sao escritos na mao de proposito: uma formula "esperta" com
     * modulo e sinal aqui e o tipo de coisa que sai certa em tres cantos e errada no
     * quarto, e o quarto e o que ninguem testa.
     */
    private static int[] step(int k) {
        int t = Math.floorMod(ARC_START + k * STRIDE, PERIMETER);
        int x, z;
        if (t < RUN_X) {                                  // borda norte, para leste
            x = PATH_X0 + t;
            z = PATH_Z0;
        } else if (t < RUN_X + RUN_Z) {                   // borda leste, para o sul
            x = PATH_X1;
            z = PATH_Z0 + (t - RUN_X);
        } else if (t < 2 * RUN_X + RUN_Z) {               // borda sul, para oeste
            x = PATH_X1 - (t - RUN_X - RUN_Z);
            z = PATH_Z1;
        } else {                                          // borda oeste, para o norte
            x = PATH_X0;
            z = PATH_Z1 - (t - 2 * RUN_X - RUN_Z);
        }
        return new int[]{x, z, FIRST_Y + k};
    }

    /**
     * O bau do topo, no MEIO do tampo.
     *
     * A conta do canto e a mesma do `pad`, e nao "o ultimo degrau + 1": o tampo pode ter
     * sido empurrado para dentro pela trava da cerca, e ai o bau nasceria ao lado dele —
     * caindo. Duas contas do mesmo lugar tem que sair do mesmo lugar.
     */
    private static BlockPos prize() {
        int[] last = step(STEPS - 1);
        return new BlockPos(corner(last[0], CAGE_W, TOP_SIDE) + TOP_SIDE / 2,
                last[2] + 1,
                corner(last[1], CAGE_L, TOP_SIDE) + TOP_SIDE / 2);
    }

    /** O canto de uma plataforma, ja trancado dentro da cerca. */
    private static int corner(int wanted, int cageSide, int side) {
        return Math.min(Math.max(wanted, 1), cageSide - 1 - side);
    }

    // ------------------------------------------------------------------ o carimbo
    @Override
    protected void carve(Brush brush) {
        // Fora da gaiola nao ha nada — e isso e a dimensao, nao economia.
        if (brush.x1 < 0 || brush.x0 >= CAGE_W || brush.z1 < 0 || brush.z0 >= CAGE_L) return;

        DimPiece piece = cage();
        if (piece != null) brush.stamp(new Placement(piece, 0, 0, CAGE_Y, 0));

        for (int k = 0; k < STEPS; k++) {
            int[] at = step(k);
            boolean rest = k > 0 && k % REST_EVERY == 0;
            pad(brush, at[0], at[2], at[1], rest ? REST_SIDE : STEP_SIDE, rest ? REST : STEP);
        }
        // O tampo do topo: largo o bastante para se parar em cima dele sem cair de volta.
        int[] last = step(STEPS - 1);
        pad(brush, last[0], last[2], last[1], TOP_SIDE, REST);
    }

    /**
     * Uma plataforma de `side` blocos de lado, com o canto em (x,z).
     *
     * O canto e trancado DENTRO da cerca antes de desenhar. Sem isso, um descanso de 4 de
     * lado na borda leste passaria por cima do muro (a cerca esta em x=29) e o jogador
     * sairia da gaiola andando pelo topo dela, em vez de pelo parkour — que e a unica
     * saida que a dimensao devia ter.
     */
    private void pad(Brush brush, int x, int y, int z, int side, BlockState state) {
        int cx = corner(x, CAGE_W, side);
        int cz = corner(z, CAGE_L, side);
        for (int dx = 0; dx < side; dx++) {
            for (int dz = 0; dz < side; dz++) {
                brush.set(cx + dx, y, cz + dz, state);
            }
        }
    }

    /** O bau do topo. Vai aqui porque bloco-entidade precisa de `WorldGenLevel`. */
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
        BlockPos where = prize();
        ChunkPos pos = chunk.getPos();
        if (SectionPos.blockToSectionCoord(where.getX()) != pos.x
                || SectionPos.blockToSectionCoord(where.getZ()) != pos.z) return;
        if (!level.getBlockState(where).isAir()) return;

        level.setBlock(where, Blocks.CHEST.defaultBlockState(), 2);
        if (level.getBlockEntity(where) instanceof RandomizableContainerBlockEntity container) {
            container.setLootTable(LOOT, where.asLong() ^ seed());
        }
    }

    // ------------------------------------------------------------------ o spawn
    /**
     * Na plataforma de nascimento, num ponto sorteado dela.
     *
     * A regra do Pedro ("nunca no mesmo spawn") vale aqui tambem, mas aqui ela tem teto:
     * a dimensao TEM um lugar so, entao o que varia e onde na plataforma. E o bastante
     * para o primeiro salto nunca ser o mesmo — a plataforma mede 9x11 e o parkour comeca
     * na quina leste dela.
     */
    @Override
    public BlockPos dimensionSpawn() {
        java.util.Random dice = new java.util.Random();
        int x = START_X0 + dice.nextInt(START_X1 - START_X0 + 1);
        int z = START_Z0 + dice.nextInt(START_Z1 - START_Z0 + 1);
        return new BlockPos(x, CAGE_Y + 1, z);
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
        return "PARKOURLAND";
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return CAGE_Y + 1;
    }
}
