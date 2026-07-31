package net.vhsworld.rec.worldgen.dim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * A dimensao MALL: o shopping que nao acaba.
 *
 * "Mall - Um shopping gigante e infinito ao pode acessar as lojas, escada rolantes e
 * ares mais abertas e espacosas".
 *
 * ⚠️ O PEDIDO TEM QUATRO EXIGENCIAS E TRES DELAS SAO SOBRE ESPACO, nao sobre decoracao.
 * "Gigante", "areas mais abertas e espacosas" e "escadas rolantes" dizem todas a mesma
 * coisa por caminhos diferentes: este lugar nao pode ser um labirinto. E a primeira
 * dimensao do mod cujo medo NAO vem do aperto.
 *
 * E uma escolha de projeto, e ela custa. O reflexo — o mesmo que fez a MAZE e a DATA
 * darem certo — seria estreitar o corredor e fechar a bruma, e aqui isso mataria o
 * pedido. As tres fotos do Pedro sao todas de shopping ABANDONADO, e o que assusta nelas
 * e o contrario do aperto: um corredor de nove blocos de largura, teto alto, piso
 * polido refletindo, e nenhuma pessoa. O vazio de um lugar que foi feito para caber
 * multidao e pior do que um corredor apertado, porque o corredor apertado nunca prometeu
 * companhia.
 *
 * Por isso a bruma desta dimensao e a MAIS ABERTA das quinze (ver DimensionProfile). Se
 * o jogador nao puder ver o corredor sumindo ao longe, o pedido inteiro se perde.
 *
 * ============================ OS DOIS ANDARES ============================
 *
 * A segunda foto e a terceira sao do andar de baixo, olhando para uma enfiada de lojas
 * fechadas; a primeira e do vao central, de cima, com as escadas rolantes cruzando e a
 * clarabóia. Duas leituras do mesmo predio, e as duas precisam existir para nenhuma
 * funcionar sozinha: um shopping de um andar so e um corredor comercial, e o que faz o
 * lugar ser "gigante" e poder olhar para BAIXO.
 *
 * ============================ AS DUAS GRADES ============================
 *
 * Ha duas grades independentes aqui, e a independencia e o truque.
 *
 * A grade dos CORREDORES tem 48 blocos e e regular: e a planta do predio, e predio tem
 * planta. A das LOJAS tem 13 e nao esta alinhada com ela — as paredes de loja caem onde
 * caem. O resultado e que as lojas saem de tamanhos e formatos diferentes conforme onde
 * a parede delas encontra o corredor, que e exatamente o que acontece num shopping de
 * verdade e o que NAO acontece se as duas grades forem a mesma. Com uma grade so, toda
 * loja teria a mesma largura e o corredor viraria um acordeao — e um acordeao se decora
 * em dois minutos, e um lugar decorado deixa de ser grande.
 */
public class MallChunkGenerator extends StampChunkGenerator {

    public static final Codec<MallChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, MallChunkGenerator::new));

    private static final int MIN_Y = 0;
    private static final int GEN_HEIGHT = 32;

    /** O piso do andar de baixo. */
    private static final int GROUND_Y = 4;

    /** De um piso ao piso de cima. Sete blocos de pe-direito e a laje. */
    private static final int LEVEL_H = 8;

    private static final int UPPER_Y = GROUND_Y + LEVEL_H;   // 12
    private static final int ROOF_Y = UPPER_Y + LEVEL_H;     // 20

    // ------------------------------------------------------------------ as grades
    /** Lado da grade dos corredores. */
    private static final int CELL = 48;

    /** Meia largura do corredor: 4 para cada lado do eixo da 9 de vao. */
    private static final int HALF = 4;

    /**
     * Lado da grade das lojas.
     *
     * 13 e primo com o 48 do corredor de proposito — ver o comentario da classe. Se
     * fosse divisor, as paredes de loja cairiam sempre no mesmo ponto de cada quadra.
     */
    private static final int SHOP = 13;

    private static final long ATRIUM_SALT = 6100L;
    private static final long SHOP_SALT = 6200L;

    /** Chance de um cruzamento ser vao central em vez de piso fechado. */
    private static final double ATRIUM_CHANCE = 0.42D;

    private static final int SPAWN_SPREAD = 480;

    // ------------------------------------------------------------------ os blocos
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /** A estrutura macica: e dela que tudo e cavado. */
    private static final BlockState STRUCTURE = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
    /** A parede acabada do corredor. */
    private static final BlockState WALL = Blocks.WHITE_CONCRETE.defaultBlockState();
    /** O rodape escuro e as testeiras das lojas: e ele que da o desenho da terceira foto. */
    private static final BlockState TRIM = Blocks.BLACK_CONCRETE.defaultBlockState();

    /** O piso polido do corredor. */
    private static final BlockState TILE = Blocks.SMOOTH_QUARTZ.defaultBlockState();
    /** O losango do piso, aquele ladrilho dourado espacado da segunda foto. */
    private static final BlockState TILE_MARK = Blocks.WAXED_CUT_COPPER.defaultBlockState();
    /** O chao das lojas: cimento cru, porque loja fechada nao tem piso acabado. */
    private static final BlockState SHOP_FLOOR = Blocks.GRAY_CONCRETE.defaultBlockState();

    /** A grade de fechar loja. */
    private static final BlockState GRATE = Blocks.IRON_BARS.defaultBlockState();
    /** A clarabóia. */
    private static final BlockState SKYLIGHT = Blocks.GLASS.defaultBlockState();

    private static final BlockState RAIL = Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState();
    private static final BlockState PLANTER = Blocks.STRIPPED_OAK_WOOD.defaultBlockState();
    private static final BlockState SOIL = Blocks.COARSE_DIRT.defaultBlockState();
    private static final BlockState BUSH = Blocks.DEAD_BUSH.defaultBlockState();

    private static final BlockState STEP = Blocks.QUARTZ_STAIRS.defaultBlockState();

    /** A luz embutida do teto e a que vem de cima da clarabóia. Ver `lamp()`. */
    private static volatile BlockState lamp;

    /**
     * ⚠️ Buscada TARDE. `WHITE_LIGHT` e bloco NOSSO, e quando esta classe carrega o
     * registro do mod ainda nao rodou — um `static final` aqui estoura com "Registry
     * Object not present" no carregamento. Ver o `ceiling()` da GRASSROOMS.
     */
    private static BlockState lamp() {
        BlockState ready = lamp;
        if (ready != null) return ready;
        ready = net.vhsworld.rec.init.ModBlocks.WHITE_LIGHT.get().defaultBlockState();
        lamp = ready;
        return ready;
    }

    public MallChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    // ------------------------------------------------------------------ a planta
    static int cellOf(int v) {
        return Math.floorDiv(v, CELL);
    }

    /** O eixo do corredor desta casa. */
    static int axis(int cell) {
        return cell * CELL + CELL / 2;
    }

    /** Distancia ao eixo do corredor mais proximo neste eixo de coordenada. */
    static int offAxis(int v) {
        return Math.abs(v - axis(cellOf(v)));
    }

    /** Vao de corredor (os dois eixos valem, e o cruzamento e a soma dos dois). */
    static boolean corridor(int x, int z) {
        return offAxis(x) <= HALF || offAxis(z) <= HALF;
    }

    /** O cruzamento desta casa e vao central de dois pes-direitos? */
    boolean atrium(int cx, int cz) {
        return DimHash.frac(seed(), cx, cz, ATRIUM_SALT) < ATRIUM_CHANCE;
    }

    /** Este ponto esta sobre o cruzamento (a area quadrada onde os dois corredores se cruzam)? */
    static boolean crossing(int x, int z) {
        return offAxis(x) <= HALF && offAxis(z) <= HALF;
    }

    /** Parede divisoria de loja. */
    static boolean shopWall(int x, int z) {
        return Math.floorMod(x, SHOP) == 0 || Math.floorMod(z, SHOP) == 0;
    }

    // ------------------------------------------------------------------ o carimbo
    @Override
    protected void carve(Brush brush) {
        shell(brush);
        levels(brush);
        storefronts(brush);
        atriums(brush);
        escalators(brush);
        furniture(brush);
    }

    /** Primeiro tudo macico: o predio e o que sobra depois de cavar. */
    private void shell(Brush brush) {
        for (int x = brush.x0; x <= brush.x1; x++) {
            for (int z = brush.z0; z <= brush.z1; z++) {
                brush.column(x, minY(), minY() + genHeight() - 1, z, STRUCTURE);
            }
        }
    }

    /** Os dois andares: piso, vao e forro, no corredor e dentro das lojas. */
    private void levels(Brush brush) {
        for (int x = brush.x0; x <= brush.x1; x++) {
            for (int z = brush.z0; z <= brush.z1; z++) {
                boolean hall = corridor(x, z);

                if (hall) {
                    floor(brush, x, GROUND_Y, z, true);
                    brush.column(x, GROUND_Y + 1, UPPER_Y - 1, z, AIR);
                    floor(brush, x, UPPER_Y, z, true);
                    brush.column(x, UPPER_Y + 1, ROOF_Y - 1, z, AIR);
                    ceilingLight(brush, x, z, UPPER_Y - 1);
                    ceilingLight(brush, x, z, ROOF_Y - 1);
                    continue;
                }

                // Loja. A parede divisoria fica de pe do piso ao forro dos dois andares;
                // o miolo e vazio e escuro — loja fechada nao tem luz acesa.
                if (shopWall(x, z)) {
                    // O rodape preto ate a altura da testeira, e branco acima: e o
                    // desenho que a terceira foto tem em toda parede de loja.
                    brush.column(x, GROUND_Y + 1, GROUND_Y + 4, z, TRIM);
                    brush.column(x, GROUND_Y + 5, UPPER_Y - 1, z, WALL);
                    brush.column(x, UPPER_Y + 1, UPPER_Y + 4, z, TRIM);
                    brush.column(x, UPPER_Y + 5, ROOF_Y - 1, z, WALL);
                }
                floor(brush, x, GROUND_Y, z, false);
                floor(brush, x, UPPER_Y, z, false);
                if (!shopWall(x, z)) {
                    brush.column(x, GROUND_Y + 1, UPPER_Y - 1, z, AIR);
                    brush.column(x, UPPER_Y + 1, ROOF_Y - 1, z, AIR);
                }
            }
        }
    }

    /**
     * Uma laje de piso.
     *
     * O losango do corredor sai de uma conta e nao de um sorteio: `(x+z) % 6 == 0` da uma
     * diagonal regular, que e como ladrilho de shopping e assentado de verdade. Sorteado,
     * viraria sujeira no chao — e sujeira nao e o assunto, o piso ali e POLIDO e e o
     * brilho dele que faz o corredor vazio doer.
     */
    private void floor(Brush brush, int x, int y, int z, boolean hall) {
        if (!hall) {
            brush.set(x, y, z, SHOP_FLOOR);
            return;
        }
        brush.set(x, y, z, Math.floorMod(x + z, 6) == 0 && Math.floorMod(x - z, 6) == 0
                ? TILE_MARK : TILE);
    }

    /** A luminaria embutida do forro, alinhada com o eixo do corredor. */
    private void ceilingLight(Brush brush, int x, int z, int y) {
        boolean onX = offAxis(x) == 0 && Math.floorMod(z, 7) == 0;
        boolean onZ = offAxis(z) == 0 && Math.floorMod(x, 7) == 0;
        if (onX || onZ) brush.set(x, y, z, lamp());
    }

    // ------------------------------------------------------------------ as vitrines
    /**
     * A testeira da loja: o vao aberto, e a grade que fecha metade delas.
     *
     * "ao pode acessar as lojas" e uma exigencia e nao um detalhe: se toda loja estivesse
     * fechada, o shopping seria um corredor com paredes pintadas. Mas se TODA estivesse
     * aberta, entrar em uma deixaria de ser uma decisao. A metade e o numero: o jogador
     * passa por quatro grades fechadas e no quinto vao ha um buraco preto que da para
     * entrar, e ai entrar vira escolha dele.
     *
     * ⚠️ A grade vai do piso ao alto do vao, e nao ate o forro. As lojas da terceira foto
     * tem a grade descendo de uma testeira, e e essa faixa escura por cima que faz a
     * fileira de lojas se ler como fileira; grade ate o teto daria uma jaula.
     */
    private void storefronts(Brush brush) {
        for (int x = brush.x0; x <= brush.x1; x++) {
            for (int z = brush.z0; z <= brush.z1; z++) {
                if (corridor(x, z)) continue;

                // A vitrine e a primeira faixa de loja encostada no corredor.
                boolean facingX = offAxis(x) == HALF + 1;
                boolean facingZ = offAxis(z) == HALF + 1;
                if (!facingX && !facingZ) continue;
                if (shopWall(x, z)) continue;   // aqui e divisoria entre duas lojas

                // Uma decisao por LOJA e nao por bloco: senao a mesma vitrine sairia
                // metade aberta e metade grade.
                int unitX = Math.floorDiv(x, SHOP), unitZ = Math.floorDiv(z, SHOP);
                boolean shut = DimHash.frac(seed(), unitX, unitZ, SHOP_SALT) < 0.5D;

                for (int level : new int[]{GROUND_Y, UPPER_Y}) {
                    brush.column(x, level + 1, level + 4, z, shut ? GRATE : AIR);
                    brush.column(x, level + 5, level + LEVEL_H - 1, z, TRIM);
                }
            }
        }
    }

    // ------------------------------------------------------------------ o vao central
    /**
     * O vao de dois pes-direitos, com clarabóia.
     *
     * Tira a laje do andar de cima em volta do cruzamento e abre o teto. E o que a
     * primeira foto e inteira: o andar de baixo visto de cima, com a luz caindo de uma
     * clarabóia quadrada.
     *
     * A guarda de meio bloco na beirada nao e seguranca, e leitura: sem ela a laje
     * cortada fica com um gume de um bloco e o jogador nao ve onde o chao acaba —
     * principalmente com o piso claro e a luz da clarabóia batendo nele.
     */
    private void atriums(Brush brush) {
        int cx0 = cellOf(brush.x0), cx1 = cellOf(brush.x1);
        int cz0 = cellOf(brush.z0), cz1 = cellOf(brush.z1);

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (!atrium(cx, cz)) continue;

                int ax = axis(cx), az = axis(cz);

                // ⚠️ O BURACO PARA NO CRUZAMENTO, e nao um bloco alem. Vazar para
                // `HALF + n` abriria a laje por baixo das lojas da esquina: as paredes
                // divisorias delas continuariam de pe, penduradas em nada, e o piso do
                // andar de cima teria um rombo que nao pertence a planta nenhuma.
                for (int x = ax - HALF - 1; x <= ax + HALF + 1; x++) {
                    for (int z = az - HALF - 1; z <= az + HALF + 1; z++) {
                        if (x < brush.x0 || x > brush.x1 || z < brush.z0 || z > brush.z1) continue;

                        if (Math.abs(x - ax) > HALF || Math.abs(z - az) > HALF) {
                            // o anel da beirada: guarda de meio bloco, so onde ha corredor
                            if (corridor(x, z)) brush.set(x, UPPER_Y + 1, z, RAIL);
                            continue;
                        }
                        brush.column(x, UPPER_Y, UPPER_Y + 1, z, AIR);
                        brush.set(x, ROOF_Y - 1, z, SKYLIGHT);
                        brush.set(x, ROOF_Y, z, lamp());
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ as escadas
    /**
     * A escada rolante.
     *
     * Uma por cruzamento, encostada no lado -X do eixo, subindo no sentido +Z. Sao oito
     * blocos de altura em oito de corrida, ou seja 45 graus — mais ingreme que uma escada
     * rolante de verdade, e nao ha o que fazer: em Minecraft um degrau custa um bloco de
     * altura, e alongar a corrida para suavizar exigiria um corredor mais largo que os
     * nove que o vao central precisa.
     *
     * ⚠️ O FURO NA LAJE VEM ANTES DO DEGRAU, sempre. Se a laje do andar de cima fosse
     * escrita depois, ela taparia a boca da escada — e o defeito seria uma escada que sobe
     * e bate num teto, o que so se descobre subindo. Aqui a ordem esta garantida porque
     * `escalators` roda depois de `levels`, e nao por acaso.
     */
    private void escalators(Brush brush) {
        int cx0 = cellOf(brush.x0) - 1, cx1 = cellOf(brush.x1) + 1;
        int cz0 = cellOf(brush.z0) - 1, cz1 = cellOf(brush.z1) + 1;

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                // ⚠️ ENCOSTADA NO LADO -X DO CORREDOR, E DENTRO DELE. O corredor vai de
                // `axis-4` a `axis+4`; a escada gasta 3 de degrau mais 1 de corrimao de
                // cada lado, ou seja 5. Centrando em `axis-2` ela ocupa `axis-4..axis` e
                // sobram quatro blocos de passagem do outro lado. Centrar em `axis-3`
                // (o obvio, para "encostar na parede") poria o corrimao em `axis-5`, que
                // ja e loja — e o corrimao nasceria dentro da parede da vitrine.
                int ax = axis(cx) - 2;
                int az = axis(cz) - LEVEL_H / 2;

                for (int i = 0; i < LEVEL_H; i++) {
                    int z = az + i;
                    int y = GROUND_Y + 1 + i;

                    // abre a laje de cima na boca da escada
                    for (int w = -1; w <= 1; w++) {
                        brush.column(ax + w, y, UPPER_Y + 1, z, AIR);
                    }
                    for (int w = -1; w <= 1; w++) {
                        brush.set(ax + w, y - 1, z, STEP.setValue(
                                BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
                    }
                    // o corrimao dos dois lados
                    brush.set(ax - 2, y, z, RAIL);
                    brush.set(ax + 2, y, z, RAIL);
                }
                // o patamar de cima, para nao desembocar no ar
                for (int w = -1; w <= 1; w++) {
                    brush.set(ax + w, UPPER_Y, az + LEVEL_H, TILE);
                }
            }
        }
    }

    // ------------------------------------------------------------------ o mobiliario
    /**
     * Os canteiros e os bancos do meio do corredor.
     *
     * Ralos de proposito: um a cada 24 blocos, e so no eixo. As tres fotos tem quase
     * nada no chao — um shopping abandonado ja teve o mobiliario retirado, e e a
     * AUSENCIA dele que datou o lugar. Encher o corredor de banco daria um shopping
     * fechado ontem, e o que o Pedro mandou e um fechado ha vinte anos.
     */
    private void furniture(Brush brush) {
        for (int x = brush.x0; x <= brush.x1; x++) {
            for (int z = brush.z0; z <= brush.z1; z++) {
                // So no eixo do corredor que corre em Z, e a cada 16 blocos.
                if (offAxis(x) != 0 || Math.floorMod(z, 16) != 0) continue;

                // ⚠️ FORA DO CRUZAMENTO. O cruzamento e onde moram o vao central e a
                // escada rolante — um canteiro ali nasceria dentro do corrimao, ou
                // boiando sobre o buraco do vao. Este `continue` e a unica coisa que
                // mantem os tres sistemas fora do caminho um do outro.
                if (crossing(x, z)) continue;

                brush.set(x, GROUND_Y + 1, z, PLANTER);
                brush.set(x, GROUND_Y + 2, z, SOIL);
                brush.set(x, GROUND_Y + 3, z, BUSH);
            }
        }
    }

    // ------------------------------------------------------------------ o spawn
    /** No corredor, sobre o eixo — o unico ponto que existe em toda casa da grade. */
    @Override
    public BlockPos dimensionSpawn() {
        java.util.Random dice = new java.util.Random();
        int cx = dice.nextInt(-SPAWN_SPREAD / CELL, SPAWN_SPREAD / CELL + 1);
        int cz = dice.nextInt(-SPAWN_SPREAD / CELL, SPAWN_SPREAD / CELL + 1);
        // Deslocado do cruzamento: o cruzamento pode ser vao central, e cair de costas
        // para um buraco de dois andares e comecar a dimensao perdendo vida.
        return new BlockPos(axis(cx), GROUND_Y + 1, axis(cz) + HALF + 6);
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
        return "MALL";
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return GROUND_Y + 1;
    }

    // ------------------------------------------------------------------ a saida
    @Override
    public String dimensionId() {
        return "mall";
    }

    /** No corredor e fora do cruzamento, pelo mesmo motivo do spawn. */
    @Override
    public BlockPos exitAnchor(int rx, int rz) {
        int cx = ExitSite.cellInRegion(rx, CELL);
        int cz = ExitSite.cellInRegion(rz, CELL);
        return new BlockPos(axis(cx), GROUND_Y + 1, axis(cz) + HALF + 6);
    }
}
