package net.vhsworld.rec.worldgen.dim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * A dimensao FLORESTA: taiga densa sem fim, bruma branca, e um celeiro.
 *
 * "floresta - dimensao aonde ha uma floresta de taiga densa e havera uma casa".
 *
 * ⚠️ O PEDIDO TEM DUAS PARTES E ELAS BRIGAM. "Taiga densa" e "havera uma casa" nao sao
 * dois enfeites somados: numa floresta que nao acaba, uma casa que aparece de vez em
 * quando e a UNICA coisa que da direcao. Se o celeiro fosse comum, a dimensao viraria
 * um subúrbio arborizado e perderia o assunto; se fosse unico num mundo infinito, o
 * jogador nunca acharia nenhum e a dimensao seria so mato. Por isso ele nasce um por
 * regiao de 192 blocos: anda-se por uns minutos sem ver nada, e ai ele esta ali.
 *
 * As duas fotos que o Pedro deu dizem o resto, e as duas dizem a MESMA coisa: o ceu e
 * branco estourado, sem sol, e as arvores morrem dentro da bruma a meia distancia.
 * Nao ha escuridao nenhuma nas referencias — o medo ali e de dia, e e de nao ver o
 * fundo. E por isso que o `dimension_type` fixa o horario e usa os efeitos do Nether:
 * o ceu vira a propria cor da neblina, o que da o branco lavado das fotos, e nao ha
 * noite para o jogador esperar passar.
 *
 * ============================ POR QUE AS ARVORES SAO JAVA ============================
 *
 * O caminho obvio seria pendurar `minecraft:trees_taiga` no campo `features` do bioma e
 * deixar o jogo plantar. Nao vai por ai, por duas razoes:
 *
 * 1. O `StampChunkGenerator` desliga `applyBiomeDecoration` de proposito — as outras
 *    dimensoes tem piso de UM bloco, e feature vanilla cava e planta por conta propria.
 *    Religar aqui seria abrir esse caminho para as doze.
 * 2. Densidade. `trees_taiga` foi calibrada para o overworld, onde a taiga divide
 *    espaco com clareira e lago. "Densa" e o pedido, e mexer nisso por datapack e
 *    reescrever a placed feature inteira dentro do nosso jar.
 *
 * ⚠️ A PEGADINHA DA ARVORE NA BORDA DO CHUNK. Uma arvore nao cabe num chunk: o tronco
 * pode estar em um e metade da copa no vizinho. Por isso o laco NAO percorre as casas
 * de arvore dentro do chunk — ele percorre as casas dentro do chunk MAIS uma margem, e
 * deixa o `Brush` descartar o que sai. Cada chunk desenha a sua fatia da mesma arvore,
 * e como a posicao dela e uma funcao de (casa, semente) e nao um sorteio em fluxo, os
 * dois lados calculam exatamente a mesma arvore sem se falar. Sem a margem, toda copa
 * na borda sairia cortada em linha reta — e o corte seria a grade dos chunks, visivel a
 * quilometros.
 */
public class FlorestaChunkGenerator extends StampChunkGenerator {

    public static final Codec<FlorestaChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, FlorestaChunkGenerator::new));

    private static final int MIN_Y = 0;
    private static final int GEN_HEIGHT = 128;

    /** A altura media do chao. */
    private static final int BASE_Y = 64;

    /**
     * Quanto o relevo sobe ou desce alem da media.
     *
     * Bem menor que o da STONELAND (34), e nao por preguica: nas fotos o chao e quase
     * plano e quem faz a parede vertical da imagem sao os TRONCOS. Morro alto aqui
     * abriria vista por cima das copas, e ver por cima da floresta e ver que ela acaba.
     */
    private static final int RELIEF = 11;

    private static final int[] PERIODS = {112, 37, 13};
    private static final double[] WEIGHTS = {1.00D, 0.40D, 0.15D};

    // ------------------------------------------------------------------ as arvores
    /** Lado da casa em que cabe no maximo uma arvore. */
    private static final int TREE_CELL = 5;

    /**
     * Chance de a casa ter arvore.
     *
     * 0.78 com casa de 5 blocos da uma arvore a cada ~32 blocos quadrados. E denso: da
     * para andar, mas nunca da para ver longe em linha reta, que e o ponto.
     */
    private static final double TREE_DENSITY = 0.78D;

    /** Margem em que a copa de uma arvore de fora ainda alcanca este chunk. */
    private static final int TREE_REACH = 5;

    private static final long TREE_SALT = 4100L;

    // ------------------------------------------------------------------ o celeiro
    /** Um celeiro por quadrado deste lado. */
    private static final int BARN_REGION = 192;

    /** Metade da largura da clareira em volta do celeiro. */
    private static final int CLEARING = 15;

    private static final long BARN_SALT = 4200L;

    private static final int BARN_W = 13;   // no eixo X
    private static final int BARN_D = 11;   // no eixo Z
    private static final int BARN_WALL = 6; // altura da parede ate o beiral

    /** Onde a fita pode largar o jogador. */
    private static final int SPAWN_SPREAD = 1536;

    // ------------------------------------------------------------------ os blocos
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState PODZOL = Blocks.PODZOL.defaultBlockState();
    private static final BlockState COARSE = Blocks.COARSE_DIRT.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState LOG = Blocks.SPRUCE_LOG.defaultBlockState();
    private static final BlockState LEAVES = Blocks.SPRUCE_LEAVES.defaultBlockState()
            .setValue(BlockStateProperties.PERSISTENT, Boolean.TRUE);
    private static final BlockState FERN = Blocks.FERN.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /**
     * A madeira do celeiro.
     *
     * Carvalho escuro e nao abeto, mesmo a floresta sendo de abeto — e de proposito. Nas
     * fotos o celeiro e visivelmente MAIS ESCURO que os troncos em volta; ele nao se
     * camufla no mato, ele e um buraco preto no meio dele. Usar a mesma madeira das
     * arvores daria uma casa que se le como parte da floresta, e a construcao que a gente
     * quer e a que nao pertence ao lugar.
     */
    private static final BlockState BARN_WOOD = Blocks.DARK_OAK_PLANKS.defaultBlockState();
    private static final BlockState BARN_POST = Blocks.DARK_OAK_LOG.defaultBlockState();
    /** As cruzes de Santo Andre da fachada: pálidas, o unico contraste da imagem. */
    private static final BlockState BARN_BRACE = Blocks.BIRCH_PLANKS.defaultBlockState();
    private static final BlockState BARN_FLOOR = Blocks.SPRUCE_PLANKS.defaultBlockState();
    private static final BlockState BARN_ROOF = Blocks.DARK_OAK_SLAB.defaultBlockState()
            .setValue(SlabBlock.TYPE, SlabType.DOUBLE);
    private static final BlockState HAY = Blocks.HAY_BLOCK.defaultBlockState();

    public FlorestaChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    // ------------------------------------------------------------------ o ruido
    /** Igual ao da STONELAND: ver o comentario de la sobre o `smoothstep`. */
    private double noise(int x, int z, int period, long salt) {
        int gx = Math.floorDiv(x, period), gz = Math.floorDiv(z, period);
        double fx = (x - gx * (double) period) / period;
        double fz = (z - gz * (double) period) / period;
        fx = fx * fx * (3.0D - 2.0D * fx);
        fz = fz * fz * (3.0D - 2.0D * fz);

        double n00 = DimHash.frac(seed(), gx, gz, salt);
        double n10 = DimHash.frac(seed(), gx + 1, gz, salt);
        double n01 = DimHash.frac(seed(), gx, gz + 1, salt);
        double n11 = DimHash.frac(seed(), gx + 1, gz + 1, salt);

        double top = n00 + (n10 - n00) * fx;
        double bottom = n01 + (n11 - n01) * fx;
        return top + (bottom - top) * fz;
    }

    /** A altura do chao nesta coluna. */
    int surface(int x, int z) {
        double sum = 0.0D, total = 0.0D;
        for (int i = 0; i < PERIODS.length; i++) {
            sum += WEIGHTS[i] * noise(x, z, PERIODS[i], 71L + i);
            total += WEIGHTS[i];
        }
        double signed = (sum / total) * 2.0D - 1.0D;
        return BASE_Y + (int) Math.round(signed * RELIEF);
    }

    // ------------------------------------------------------------------ o carimbo
    @Override
    protected void carve(Brush brush) {
        ground(brush);
        barn(brush);
        trees(brush);
    }

    /** Terra, e a camada de cima escolhida por ruido curto. */
    private void ground(Brush brush) {
        for (int x = brush.x0; x <= brush.x1; x++) {
            for (int z = brush.z0; z <= brush.z1; z++) {
                int top = surface(x, z);
                brush.column(x, minY(), top - 5, z, STONE);
                brush.column(x, Math.max(minY(), top - 4), top - 1, z, DIRT);

                // A manta do chao de taiga: grama por padrao, podzol em manchas grandes
                // e terra batida so nas bordas da mancha. Duas alturas de ruido em vez
                // de sorteio por bloco — sorteio por bloco daria chuvisco, e chao de
                // floresta e feito de MANCHAS.
                double patch = noise(x, z, 19, 88L);
                brush.set(x, top, z, patch > 0.66D ? PODZOL : (patch > 0.62D ? COARSE : GRASS));

                if (DimHash.frac(seed(), x, z, 91L) < 0.16D) {
                    brush.set(x, top + 1, z, FERN);
                }
            }
        }
    }

    // ------------------------------------------------------------------ as arvores
    private void trees(Brush brush) {
        int cx0 = Math.floorDiv(brush.x0 - TREE_REACH, TREE_CELL);
        int cx1 = Math.floorDiv(brush.x1 + TREE_REACH, TREE_CELL);
        int cz0 = Math.floorDiv(brush.z0 - TREE_REACH, TREE_CELL);
        int cz1 = Math.floorDiv(brush.z1 + TREE_REACH, TREE_CELL);

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (DimHash.frac(seed(), cx, cz, TREE_SALT) > TREE_DENSITY) continue;

                int x = cx * TREE_CELL + DimHash.pick(seed(), cx, cz, TREE_SALT + 1, TREE_CELL);
                int z = cz * TREE_CELL + DimHash.pick(seed(), cx, cz, TREE_SALT + 2, TREE_CELL);
                if (inClearing(x, z)) continue;

                tree(brush, x, z, DimHash.frac(seed(), cx, cz, TREE_SALT + 3));
            }
        }
    }

    /**
     * Um abeto.
     *
     * Duas formas, e a proporcao entre elas e o que da a silhueta das fotos: 65% de PINHO
     * ALTO (tronco longo e nu, copa so no topo) contra 35% de abeto conico normal. E o
     * inverso da taiga do vanilla, e de proposito — o que as duas referencias tem em
     * comum e uma parede de troncos retos com a copa fora do enquadramento. Copa comecando
     * na altura do olho daria um matagal fechado, que esconde o fundo mas tambem esconde a
     * PROFUNDIDADE, e e a profundidade que assusta ali.
     */
    private void tree(Brush brush, int x, int z, double roll) {
        int base = surface(x, z) + 1;
        boolean pine = roll < 0.65D;

        int trunk = pine
                ? 13 + (int) (roll * 14.0D) % 7    // 13..19
                : 7 + (int) (roll * 23.0D) % 4;    // 7..10
        brush.column(x, base, base + trunk, z, LOG);

        // Onde a copa comeca. No pinho ela e um tufo curto colado no topo — e por isso
        // que o tronco dele se le como coluna nua; no abeto ela desce quase ate o chao.
        int start = pine ? base + trunk - 4 : base + 2;
        int span = base + trunk - start;

        for (int y = start; y <= base + trunk; y++) {
            int up = y - start;
            // De raio 2 na base a raio 0 na ponta, proporcional a altura da copa. A conta
            // e inteira de proposito: o arredondamento e que faz a copa ficar ESCALONADA
            // em degraus, e nao um cone liso — cone liso nao parece arvore, parece antena.
            int radius = 2 - (up * 3) / Math.max(1, span + 1);
            disc(brush, x, y, z, Math.max(0, radius));
        }
        brush.set(x, base + trunk + 1, z, LEAVES);
    }

    /** Um disco de folha, sem tocar no bloco central (que e tronco). */
    private void disc(Brush brush, int x, int y, int z, int radius) {
        if (radius <= 0) return;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) continue;
                // Corta o canto do quadrado: sem isto a copa vista de cima e um losango
                // perfeito, e losango perfeito nao existe em floresta nenhuma.
                if (Math.abs(dx) == radius && Math.abs(dz) == radius && radius > 1) continue;
                if (brush.get(x + dx, y, z + dz).isAir()) {
                    brush.set(x + dx, y, z + dz, LEAVES);
                }
            }
        }
    }

    // ------------------------------------------------------------------ o celeiro
    int barnX(int rx, int rz) {
        return rx * BARN_REGION + 48 + DimHash.pick(seed(), rx, rz, BARN_SALT, BARN_REGION - 96);
    }

    int barnZ(int rx, int rz) {
        return rz * BARN_REGION + 48 + DimHash.pick(seed(), rx, rz, BARN_SALT + 1, BARN_REGION - 96);
    }

    /**
     * Este ponto esta na clareira do celeiro da regiao dele?
     *
     * ⚠️ SO OLHA A PROPRIA REGIAO, e isso e uma garantia e nao um atalho. O celeiro nasce
     * entre 48 e 144 dentro dos 192 da regiao, e a clareira tem 15 de raio: 48-15 = 33 > 0
     * e 144+15 = 159 < 192. A clareira NUNCA cruza a divisa. Quem for mexer no BARN_REGION,
     * no jitter ou no CLEARING tem que refazer essa conta — se a clareira passar a cruzar,
     * as arvores do lado de la nao vao saber dela e vao crescer dentro do celeiro.
     */
    boolean inClearing(int x, int z) {
        int rx = Math.floorDiv(x, BARN_REGION), rz = Math.floorDiv(z, BARN_REGION);
        return Math.abs(x - barnX(rx, rz)) <= CLEARING && Math.abs(z - barnZ(rx, rz)) <= CLEARING;
    }

    private void barn(Brush brush) {
        // As regioes que alcancam este chunk. O celeiro tem 13 de largura e a clareira 15,
        // entao um chunk na divisa pode ver o celeiro da regiao vizinha.
        int rx0 = Math.floorDiv(brush.x0 - CLEARING, BARN_REGION);
        int rx1 = Math.floorDiv(brush.x1 + CLEARING, BARN_REGION);
        int rz0 = Math.floorDiv(brush.z0 - CLEARING, BARN_REGION);
        int rz1 = Math.floorDiv(brush.z1 + CLEARING, BARN_REGION);

        for (int rx = rx0; rx <= rx1; rx++) {
            for (int rz = rz0; rz <= rz1; rz++) {
                int bx = barnX(rx, rz), bz = barnZ(rx, rz);
                int floorY = surface(bx, bz);
                clearing(brush, bx, bz, floorY);
                building(brush, bx - BARN_W / 2, floorY, bz - BARN_D / 2);
            }
        }
    }

    /** Aplaina a clareira e tira o que estiver por cima dela. */
    private void clearing(Brush brush, int bx, int bz, int floorY) {
        for (int x = bx - CLEARING; x <= bx + CLEARING; x++) {
            for (int z = bz - CLEARING; z <= bz + CLEARING; z++) {
                if (x < brush.x0 || x > brush.x1 || z < brush.z0 || z > brush.z1) continue;

                // Longe do centro a clareira volta ao relevo aos poucos, senao o celeiro
                // fica num tabuleiro quadrado e a mao que o pos ali fica visivel.
                double away = Math.max(Math.abs(x - bx), Math.abs(z - bz)) / (double) CLEARING;
                double blend = away * away;
                int target = (int) Math.round(floorY * (1.0D - blend) + surface(x, z) * blend);

                brush.column(x, minY(), target - 5, z, STONE);
                brush.column(x, Math.max(minY(), target - 4), target - 1, z, DIRT);
                brush.set(x, target, z, away < 0.55D ? COARSE : GRASS);
                brush.column(x, target + 1, target + 26, z, AIR);
            }
        }
    }

    /**
     * O celeiro em si, com o canto minimo em (ox, oy, oz).
     *
     * O TELHADO E GAMBREL (duas aguas por lado, a de baixo ingreme e a de cima mansa) e
     * nao um telhado comum de duas aguas. E a unica coisa da foto que nao da para trocar:
     * essa quebra no meio da encosta e o que faz um galpao ser lido como CELEIRO por
     * qualquer pessoa, antes de qualquer outro detalhe. Um telhado reto no mesmo corpo
     * daria um barracao.
     */
    private void building(Brush brush, int ox, int oy, int oz) {
        int x1 = ox + BARN_W - 1, z1 = oz + BARN_D - 1;
        int top = oy + BARN_WALL;

        // piso
        for (int x = ox; x <= x1; x++) {
            for (int z = oz; z <= z1; z++) {
                brush.set(x, oy, z, BARN_FLOOR);
            }
        }

        // paredes e postes de canto
        for (int y = oy + 1; y <= top; y++) {
            for (int x = ox; x <= x1; x++) {
                brush.set(x, y, oz, BARN_WOOD);
                brush.set(x, y, z1, BARN_WOOD);
            }
            for (int z = oz; z <= z1; z++) {
                brush.set(ox, y, z, BARN_WOOD);
                brush.set(x1, y, z, BARN_WOOD);
            }
            brush.set(ox, y, oz, BARN_POST);
            brush.set(x1, y, oz, BARN_POST);
            brush.set(ox, y, z1, BARN_POST);
            brush.set(x1, y, z1, BARN_POST);
        }

        // As cruzes claras da fachada, nos dois lados da porta.
        for (int i = 0; i < 4; i++) {
            brush.set(ox + 1 + i, oy + 1 + i, oz, BARN_BRACE);
            brush.set(ox + 4 - i, oy + 1 + i, oz, BARN_BRACE);
            brush.set(x1 - 1 - i, oy + 1 + i, oz, BARN_BRACE);
            brush.set(x1 - 4 + i, oy + 1 + i, oz, BARN_BRACE);
        }

        // O portao duplo: um rasgo de 4x4 no meio da fachada. Sem porta de verdade —
        // porta que abre e fecha faz barulho de porta, e o susto aqui e o vao preto.
        for (int x = ox + 5; x <= ox + 7; x++) {
            brush.column(x, oy + 1, oy + 4, oz, AIR);
        }
        // A janela alta do sotao, que nas duas fotos e o unico buraco na madeira.
        brush.column(ox + 6, top - 1, top, oz, AIR);

        roof(brush, ox, oy, oz, x1, z1, top);
        loft(brush, ox, oz, x1, z1, top);

        // Feno solto no chao. Duas pilhas, sempre nos mesmos cantos: o interior tem que
        // ser reconhecivel de um celeiro para o proximo, porque e a repeticao que conta
        // ao jogador que ele nao esta achando LUGARES, esta achando o MESMO lugar.
        brush.set(ox + 2, oy + 1, z1 - 2, HAY);
        brush.set(ox + 2, oy + 2, z1 - 2, HAY);
        brush.set(x1 - 2, oy + 1, z1 - 3, HAY);
    }

    /**
     * O gambrel: sobe ingreme ate a quebra, e ai deita ate a cumeeira.
     *
     * A altura sobe no maximo UM bloco por passo (top+1..top+4 na parte ingreme, depois
     * top+5 e top+6), e e por isso que nao ha nenhum preenchimento de fresta aqui: cada
     * degrau encosta no seguinte. Quem for deixar o telhado mais ingreme que 1:1 tem que
     * fechar o vao, senao o telhado passa a vazar luz pelas escadas.
     */
    private void roof(Brush brush, int ox, int oy, int oz, int x1, int z1, int top) {
        int half = BARN_W / 2;                 // 6
        for (int i = 0; i <= half; i++) {
            // i = distancia da beirada. A quebra da agua fica em i = 3.
            int y = roofY(top, i);
            // O beiral avanca um bloco alem da parede nas duas pontas: e a sombra dele
            // que da o volume do telhado visto de fora.
            for (int z = oz - 1; z <= z1 + 1; z++) {
                brush.set(ox + i, y, z, BARN_ROOF);
                brush.set(x1 - i, y, z, BARN_ROOF);
            }
        }
        // O oitao: a parede triangular das duas pontas, por baixo do telhado.
        for (int i = 0; i <= half; i++) {
            int y = roofY(top, i);
            for (int fill = top + 1; fill < y; fill++) {
                brush.set(ox + i, fill, oz, BARN_WOOD);
                brush.set(x1 - i, fill, oz, BARN_WOOD);
                brush.set(ox + i, fill, z1, BARN_WOOD);
                brush.set(x1 - i, fill, z1, BARN_WOOD);
            }
        }
        // A janela alta do sotao de novo, agora furando o oitao.
        brush.column(ox + half, top + 2, top + 3, oz, AIR);
    }

    /**
     * A altura do telhado a `i` blocos da beirada.
     *
     * i=0..3 sobe um por um (a agua ingreme), i=4..6 sobe de dois em dois (a agua mansa).
     * A cumeeira sai plana com tres blocos de largura, que e o que a foto mostra.
     */
    private static int roofY(int top, int i) {
        return i <= 3 ? top + i + 1 : top + 4 + (i - 3) / 2 + 1;
    }

    /** O meio-piso do sotao, encostado nas paredes e aberto no meio. */
    private void loft(Brush brush, int ox, int oz, int x1, int z1, int top) {
        int y = top - 2;
        for (int x = ox + 1; x <= x1 - 1; x++) {
            for (int z = oz + 1; z <= z1 - 1; z++) {
                boolean edge = z <= oz + 2 || z >= z1 - 2;
                if (edge) brush.set(x, y, z, BARN_FLOOR);
            }
        }
    }

    // ------------------------------------------------------------------ o spawn
    @Override
    public BlockPos dimensionSpawn() {
        java.util.Random dice = new java.util.Random();
        int x = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
        int z = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
        return new BlockPos(x, surface(x, z) + 1, z);
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
        return "FLORESTA";
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return surface(x, z) + 1;
    }

    // ------------------------------------------------------------------ a saida
    @Override
    public String dimensionId() {
        return "floresta";
    }

    /** No meio da regiao, em cima do relevo. A sala e uma caixa: as arvores cedem a ela. */
    @Override
    public BlockPos exitAnchor(int rx, int rz) {
        int x = rx * ExitSite.REGION + ExitSite.REGION / 2;
        int z = rz * ExitSite.REGION + ExitSite.REGION / 2;
        return new BlockPos(x, surface(x, z) + 1, z);
    }
}
