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

import java.util.ArrayList;
import java.util.List;

/**
 * A dimensao GRASSROOMS: um liminal space branco em que cresce grama.
 *
 * O pedido do Pedro e "um liminal space inspirado no level das backrooms", e a foto que
 * ele deu e um salao branco alto, com abobada de vidro e canteiros de grama em
 * plataformas de alturas diferentes. E a unica dimensao ILUMINADA do mod, e e de
 * proposito: o que assusta ali nao e nao ver, e ver tudo e nao haver nada.
 *
 * ============================ O QUE MUDOU, E POR QUE ============================
 *
 * ⚠️ A PRIMEIRA VERSAO PUNHA CADA PECA NO MEIO DE UMA SALA DE 56x56 CONSTRUIDA PELO
 * JAVA, e estava errada. A ideia era razoavel no papel — as pecas nao sao modulos
 * (medi: a parede leste da `room_b` e ar de ponta a ponta, a `room_c` tem tres paredes
 * inteiras abertas), entao uma casca branca selava o que ficou aberto. So que a menor
 * peca tem 8x9 e a casa tinha 56x56: sobravam 47 blocos de piso branco vazio em volta
 * dela. A foto que o Pedro mandou de dentro do jogo e exatamente isso — uma praca
 * quadrada gigantesca com um pedaco de construcao encostado na beirada.
 *
 * O pedido novo e curto e resolve o desenho inteiro: "remover essas areas quadradas
 * gigantescas e usar os schematics do grassrooms apenas, juntando eles". Ou seja: a
 * dimensao E as construcoes dele. O Java para de construir sala e passa a ser so o
 * CHAO, o TETO e o espaco entre uma obra e outra.
 *
 * ============================ COMO AS PECAS SE JUNTAM ============================
 *
 * Elas tem tamanhos diferentes (14x8, 36x43, 35x17, 8x9 e 23x17), entao nao ha grade
 * em que todas caibam. O empacotamento e uma GUILHOTINA: pega-se o retangulo da casa,
 * poe-se uma peca que caiba no canto noroeste, e o que sobra vira dois retangulos
 * menores — a faixa a leste dela e a faixa ao sul — que recebem o mesmo tratamento.
 * Para quando nao cabe mais nada. O resultado e um quarteirao de construcoes coladas,
 * e o vazio que sobra e uma FRESTA de 3 blocos, nao uma praca.
 *
 * E as frestas sao o que liga tudo: sendo recortes de uma obra continua, as pecas tem
 * parede aberta em varios lados, entao sair de uma e entrar na outra e atravessar o
 * vao de uma construcao para a outra, com o branco de 3 blocos fazendo de soleira.
 *
 * ⚠️ POR QUE A FRESTA VAI ATE A BORDA DO RETANGULO, e nao so ate a esquina da peca.
 * E o que garante que o mapa inteiro seja atravessavel, e da para provar: a fresta
 * leste corre toda a altura do retangulo (logo encosta na borda NORTE dele) e a fresta
 * sul corre toda a largura da metade esquerda (logo encosta na borda OESTE), e as duas
 * se cruzam na quina. Como cada sub-retangulo por sua vez encosta nas SUAS bordas norte
 * e oeste, e essas bordas sao justamente as frestas de quem o gerou, o corredor de uma
 * casa e uma peca so. E a casa toda encosta no anel de 3 blocos que corre pela borda
 * oeste e norte dela, que e o que costura uma casa na vizinha. Sem esse anel a
 * dimensao poderia nascer com bolsoes fechados, e o jogador cai num deles de fita.
 *
 * ⚠️ A LUZ NAO E ENFEITE, E O QUE MANTEM A GRAMA VIVA. Sem ceu e sem lampada, o jogo
 * transforma `grass_block` em terra no primeiro tick aleatorio (luz < 4) e derruba o
 * mato de cima. `ambient_light` do dimension_type NAO resolve: ele muda o quanto a tela
 * clareia, nao o nivel de luz que o bloco consulta. O teto ja e a Luz Branca e resolve
 * o salao; quem resolve o resto sao os blocos `minecraft:light` — invisiveis — que
 * entram DEPOIS das pecas e olhando o que ja esta escrito, para cairem tambem dentro
 * dos comodos fechados que as proprias pecas tem. E la que a grama morreria primeiro.
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
     * O lado do quarteirao.
     *
     * 96 nao e o tamanho de nenhuma sala — nao ha mais sala. E so o tamanho do
     * retangulo que a guilhotina divide, e ele quer ser bem maior que a maior peca
     * (36x43) para caberem varias e o desenho de dentro ser diferente a cada casa. Com
     * 96 cabem tipicamente de quatro a oito construcoes por quarteirao.
     */
    public static final int CELL = 96;

    /**
     * O anel branco na borda oeste e norte de cada quarteirao, e a fresta entre pecas.
     *
     * Os dois valem 3, e nao por simetria: 3 e o menor corredor em que duas pessoas
     * passam de frente e em que a parede dos dois lados ainda enquadra a vista, que e o
     * assunto de um liminal space. Com 2 vira fresta de servico; com 5 ja comeca a virar
     * a praca de que a gente esta fugindo.
     */
    public static final int RING = 3;
    public static final int GAP = 3;

    /**
     * A altura do salao, contada do piso.
     *
     * 14 e a altura da MAIOR peca (a `room_b`), e o numero e dela e nao meu: com um teto
     * mais baixo ela seria decapitada, e com um mais alto ela ficaria com a laje dela
     * boiando abaixo do teto. Como todas as outras tem 8, 9, 9 e 10, elas ficam sendo
     * caixas mais baixas DENTRO de um salao alto — que e exatamente o que a foto de
     * referencia tem: uma nave alta com plataformas baixas espalhadas.
     */
    public static final int CEIL_H = 14;

    /** De quantos em quantos blocos entra uma lampada invisivel no teto. */
    private static final int LIGHT_EVERY = 8;

    /** Trava de seguranca da recursao: a guilhotina para muito antes disto. */
    private static final int MAX_DEPTH = 16;

    private static final int SPAWN_SPREAD = 64;

    private static final BlockState FLOOR = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState LIGHT = Blocks.LIGHT.defaultBlockState();

    private static volatile BlockState ceiling;

    /**
     * O teto: a Luz Branca.
     *
     * ⚠️ E ELE A RESPOSTA A "ha janelas que podem mostrar o void". A abobada de vidro das
     * salas do Pedro e a camada MAIS ALTA da peca, e batia na altura em que o teto de
     * pedra era escrito — como a peca e carimbada depois, o vidro dela apagava o teto e
     * ficava uma clarabroia dando no nada preto. Com o teto num plano so, acima de todas
     * as pecas, e branco luminoso, quem olha pela abobada ve um ceu branco de estudio.
     *
     * ⚠️ E POR ISSO QUE ELE E BUSCADO TARDE, e nao num `static final` ao lado dos outros.
     * Os blocos do vanilla ja existem quando esta classe carrega; um bloco NOSSO nao —
     * o gerador nasce do codec, e o codec e tocado durante os eventos de registro. Um
     * `ModBlocks.WHITE_LIGHT.get()` ali em cima estoura com "Registry Object not present"
     * no carregamento do jogo, que e a pior hora possivel para descobrir isso. E a mesma
     * razao de as pecas do Pedro tambem so serem lidas no primeiro chunk.
     */
    private static BlockState ceiling() {
        BlockState ready = ceiling;
        if (ready != null) return ready;
        ready = net.vhsworld.rec.init.ModBlocks.WHITE_LIGHT.get().defaultBlockState();
        ceiling = ready;
        return ready;
    }

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

    // ------------------------------------------------------------------ o carimbo
    @Override
    protected void carve(Brush brush) {
        int cx0 = Math.floorDiv(brush.x0, CELL), cx1 = Math.floorDiv(brush.x1, CELL);
        int cz0 = Math.floorDiv(brush.z0, CELL), cz1 = Math.floorDiv(brush.z1, CELL);

        slab(brush);
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                for (Placement placement : planCell(rooms(), seed(), cx, cz)) {
                    if (brush.touches(placement)) brush.stamp(placement);
                }
            }
        }
        lights(brush);
    }

    /** O piso e o teto, chapados: e tudo que o Java constroi nesta dimensao. */
    private void slab(Brush brush) {
        for (int x = brush.x0; x <= brush.x1; x++) {
            for (int z = brush.z0; z <= brush.z1; z++) {
                brush.set(x, FLOOR_Y, z, FLOOR);
                brush.set(x, FLOOR_Y + CEIL_H, z, ceiling());
            }
        }
    }

    /**
     * A planta de um quarteirao: onde cada construcao dele fica.
     *
     * ⚠️ E PUBLICA E NAO DEPENDE DE CHUNK NENHUM de proposito. O harness (DimCheck)
     * precisa medir o empacotamento — se ele se sobrepoe, se transborda o quarteirao,
     * se sobra praca, se da para atravessar —, e a licao ja paga do projeto e que um
     * teste que reimplementa a regra mede a copia e nao a regra. Assim ha um
     * empacotamento so, e e este; `carve` apenas carimba o que ele devolve.
     *
     * O anel fica de fora do retangulo empacotado: e ele o corredor que costura um
     * quarteirao no vizinho.
     */
    public static List<Placement> planCell(List<DimPiece> rooms, long seed, int cx, int cz) {
        List<Placement> out = new ArrayList<>();
        if (!rooms.isEmpty()) {
            pack(rooms, seed, out, cx * CELL + RING, cz * CELL + RING,
                    cx * CELL + CELL - 1, cz * CELL + CELL - 1, 0);
        }
        return out;
    }

    /**
     * A guilhotina: uma peca no canto noroeste, e o que sobra vira dois retangulos.
     *
     * As duas chamadas de baixo nao sao simetricas de proposito. A faixa LESTE leva a
     * altura inteira do retangulo; a faixa SUL leva so a largura da peca mais a fresta.
     * Cortar os dois pela metade deixaria a quina sudeste sem dono e ela apareceria como
     * um buraco quadrado no meio do quarteirao — que e o defeito que esta versao existe
     * para tirar.
     */
    private static void pack(List<DimPiece> rooms, long seed, List<Placement> out,
                             int x0, int z0, int x1, int z1, int depth) {
        if (depth >= MAX_DEPTH || x1 < x0 || z1 < z0) return;

        int fit = choose(rooms, seed, x1 - x0 + 1 - GAP, z1 - z0 + 1 - GAP, x0, z0);
        if (fit < 0) return;

        DimPiece piece = rooms.get(fit >> 2);
        int rotation = fit & 3;
        out.add(new Placement(piece, rotation, x0, FLOOR_Y, z0));

        int usedX = piece.rotatedWidth(rotation) + GAP;
        int usedZ = piece.rotatedLength(rotation) + GAP;

        // ⚠️ A FAIXA MORTA E ABSORVIDA, e sem isto o desenho tinha uma praca de 26x50.
        //
        // Foi o harness que mostrou: a faixa a leste que sobra menor que a menor peca
        // nao recebe nada, e ate ai tudo bem — o problema e que a faixa SUL herdava so a
        // largura da peca, entao a proxima rodada largava a SUA faixa morta encostada na
        // anterior. Quatro niveis de recursao empilhavam 8+6+9+3 blocos de vazio lado a
        // lado e o resultado era exatamente a praca que esta versao existe para tirar.
        //
        // Absorvendo, a faixa morta deixa de ser fronteira e vira parte do retangulo de
        // baixo, que e largo e recebe construcao. So absorve quando PROVADAMENTE nada
        // cabe ali (menor lado de peca + fresta), entao nunca se perde um encaixe bom.
        if (x1 - (x0 + usedX) + 1 < minSide(rooms) + GAP) usedX = x1 - x0 + 1;

        pack(rooms, seed, out, x0 + usedX, z0, x1, z1, depth + 1);
        pack(rooms, seed, out, x0, z0 + usedZ, x0 + usedX - 1, z1, depth + 1);
    }

    /** O menor lado que qualquer peca pode apresentar, em qualquer giro. */
    private static int minSide(List<DimPiece> rooms) {
        int least = Integer.MAX_VALUE;
        for (DimPiece piece : rooms) least = Math.min(least, Math.min(piece.width, piece.length));
        return least;
    }

    /**
     * Qual peca, e em que giro, entra neste retangulo. Devolve `peca*4 + giro`, ou -1.
     *
     * ⚠️ O PESO E `peso da peca x (largura + comprimento)`, e nao o peso puro nem a
     * area. Puro, a `room_d` (8x9) cairia tanto num retangulo de 90 quanto num de 12 e o
     * quarteirao viraria um campo de casinhas com fresta em volta. Pela area, a `room_b`
     * (1548) engoliria a `room_d` (72) em 20 para 1 e as pequenas so apareceriam nas
     * sobras. Pela soma dos lados a conta fica entre 102 e 160 para as cinco: a peca
     * grande e a preferida onde ha espaco, e as pequenas continuam existindo.
     *
     * Os quatro giros entram porque estas pecas nao tem frente: sao recortes de uma obra
     * continua, e girar um recorte so muda para que lado a parede aberta olha.
     */
    private static int choose(List<DimPiece> rooms, long seed, int maxW, int maxL, int x0, int z0) {
        int total = 0;
        for (DimPiece piece : rooms) {
            for (int rotation = 0; rotation < 4; rotation++) {
                total += score(piece, rotation, maxW, maxL);
            }
        }
        if (total <= 0) return -1;

        int roll = DimHash.pick(seed, x0, z0, 14L, total);
        for (int i = 0; i < rooms.size(); i++) {
            for (int rotation = 0; rotation < 4; rotation++) {
                roll -= score(rooms.get(i), rotation, maxW, maxL);
                if (roll < 0) return (i << 2) | rotation;
            }
        }
        return -1;
    }

    /** Quanto esta peca neste giro pesa no sorteio; 0 quer dizer "nao cabe". */
    private static int score(DimPiece piece, int rotation, int maxW, int maxL) {
        int w = piece.rotatedWidth(rotation), l = piece.rotatedLength(rotation);
        if (w > maxW || l > maxL) return 0;
        return Math.max(piece.weight, 1) * (w + l);
    }

    /**
     * As lampadas invisiveis, de 8 em 8, procurando de cima para baixo.
     *
     * DE CIMA PARA BAIXO, e nao de baixo para cima: descendo, a lampada para no primeiro
     * ar abaixo do teto — que dentro de um comodo fechado da peca e o teto DELE, e la
     * dentro. Subindo, ela pararia no primeiro ar acima do piso, que num canteiro de
     * grama e a altura do tornozelo: ficaria uma lampada no chao iluminando so o proprio
     * canteiro e o resto do salao escureceria.
     */
    private void lights(Brush brush) {
        for (int x = brush.x0; x <= brush.x1; x++) {
            if (Math.floorMod(x, LIGHT_EVERY) != 0) continue;
            for (int z = brush.z0; z <= brush.z1; z++) {
                if (Math.floorMod(z, LIGHT_EVERY) != 0) continue;
                for (int y = FLOOR_Y + CEIL_H - 1; y > FLOOR_Y + 1; y--) {
                    if (!brush.get(x, y, z).isAir()) continue;
                    brush.set(x, y, z, LIGHT);
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------ o spawn
    /**
     * Na quina do anel de um quarteirao sorteado — nunca dentro de uma construcao.
     *
     * A quina do anel e o unico ponto de que se sabe, sem olhar nada, que tem piso
     * embaixo e ar em cima: o empacotamento comeca `RING` blocos depois dela, e o Java
     * nao levanta mais parede nenhuma. E como o anel e corredor, quem nasce ali ja esta
     * na malha que atravessa a dimensao inteira.
     */
    @Override
    public BlockPos dimensionSpawn() {
        java.util.Random dice = new java.util.Random();
        int cx = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
        int cz = dice.nextInt(-SPAWN_SPREAD, SPAWN_SPREAD + 1);
        return new BlockPos(cx * CELL + 1, FLOOR_Y + 1, cz * CELL + 1);
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

    // ------------------------------------------------------------------ a saida
    @Override
    public String dimensionId() {
        return "grassrooms";
    }

    /** O mesmo canto de sala que o spawn usa: e o unico ponto ja provado como piso. */
    @Override
    public BlockPos exitAnchor(int rx, int rz) {
        int cx = ExitSite.cellInRegion(rx, CELL);
        int cz = ExitSite.cellInRegion(rz, CELL);
        return new BlockPos(cx * CELL + 1, FLOOR_Y + 1, cz * CELL + 1);
    }
}
