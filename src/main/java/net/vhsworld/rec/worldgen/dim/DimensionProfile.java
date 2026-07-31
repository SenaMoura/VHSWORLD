package net.vhsworld.rec.worldgen.dim;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.AnomalyType;
import net.vhsworld.rec.escape.ExitMethod;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A tabela das dimensoes do mod: uma linha por dimensao, e nada mais.
 *
 * ⚠️ POR QUE ISTO EXISTE. Ate a 2a dimensao, "em que dimensao estou" era uma
 * comparacao de string espalhada por cinco arquivos: `inMaze()` perguntava por
 * "data", `inChunks()` por "chunks", o Diretor tinha o seu `isData`, o Ofanim o seu
 * `isChunks`, e `forcedTypeFor` era um switch a parte. Cada uma dessas linhas e um
 * lugar em que a 3a dimensao pode ser ESQUECIDA — e esquecer nao da erro de
 * compilacao, da uma dimensao que se comporta como overworld sem ninguem notar. Ja
 * aconteceu uma vez: a fita da CHUNKS nunca perguntou o spawn ao gerador dela e
 * largava o jogador no meio da pedra.
 *
 * Sao 21 dimensoes planejadas. Com o molde antigo, a de numero 21 custaria 21 linhas
 * espalhadas; aqui ela custa UMA, e as perguntas todas passam a ser sobre o perfil.
 *
 * O que NAO esta aqui, de proposito: nada que o datapack ja saiba dizer. Ceu, neblina,
 * cor e trilha moram no `dimension_type` e no bioma, que sao os arquivos que o jogo
 * le sozinho. Isto aqui e so o que o JAVA precisa decidir.
 */
public record DimensionProfile(
        String id,
        Mold mold,
        @Nullable AnomalyType anomaly,
        Director director,
        float fog,
        ExitMethod exit) {

    /** Uma dimensao com a bruma na distancia normal. */
    public DimensionProfile(String id, Mold mold, @Nullable AnomalyType anomaly, Director director,
                            ExitMethod exit) {
        this(id, mold, anomaly, director, NORMAL_FOG, exit);
    }

    /**
     * A bruma na distancia da curva do alpha, sem aperto nem folga.
     *
     * ⚠️ ISTO NAO E COR. A cor da neblina mora no `effects.fog_color` do bioma, que e
     * arquivo de datapack; o que esta aqui e so QUAO LONGE ela deixa ver. Sao duas
     * perguntas independentes e vale nao confundi-las: a UNDER PRESSURE precisa da
     * bruma colada no rosto e a PARKOURLAND precisa dela colada E preta, mas o preto e
     * o bioma que diz.
     */
    public static final float NORMAL_FOG = 1.0f;

    /**
     * O feitio do espaco. Decide o comportamento que depende da FORMA do lugar, e
     * nao de qual dimensao e.
     */
    public enum Mold {
        /** Predio fechado de corredores: a caçadora precisa do faro de labirinto. */
        MAZE,
        /** Pedacos de chao boiando: quem manda e o vazio entre eles. */
        ISLANDS,
        /** Saloes abertos ligados por corredor, sem teto, sobre o vazio. */
        HALLS,
        /** Chao plano aberto sem fim, com construcao repetida em cima. */
        STREETS,
        /** Salas fechadas encostadas umas nas outras, com porta entre elas. */
        ROOMS,
        /** Um caminho de largura de pessoa sobre o vazio, e mais nada. */
        LINE,
        /** Debaixo da agua: quem entrar aqui gasta ar antes de gastar coragem. */
        FLOODED,
        /** Uma torre fechada e FINITA: sobe-se, ou cai-se para fora da dimensao. */
        TOWER,
        /**
         * Terreno aberto sem fim, com morro e vale — e mais nada construido.
         *
         * A primeira das 21 em que nao ha planta, so relevo. Quem perguntar "onde e o
         * chao" aqui tem que perguntar a coluna, e nao a uma constante: e o unico feitio
         * do mod em que dois pontos vizinhos estao em alturas diferentes.
         */
        TERRAIN
    }

    /**
     * Quem a dimensao mantem viva — e por qual regra.
     *
     * Dimensao de criatura unica NUNCA usa tabela de spawn: o teto de monstros do
     * jogo e dividido entre os monstros do bioma, e num bioma de uma criatura so ela
     * enche o teto sozinha (foi assim que a DATA nasceu com 100+ caçadoras e 1 fps).
     */
    public enum Director {
        /** Ninguem mora aqui ainda. */
        NONE,
        /** Exatamente uma caçadora, reposta quando se perde do jogador. */
        HUNTER,
        /** Exatamente um Ofanim, com o olhar reciproco. */
        OPHANIM
    }

    // ------------------------------------------------------------------ a tabela
    //
    // ⚠️ A COLUNA DA SAIDA. Duas linhas so, e ela ja teve cinco.
    //
    //   MIRROR (13) todas, menos as duas de baixo. A entidade do Espelho fica parada num
    //          ponto fixo por regiao, encarando; quem olha para ela perde sanidade, leva o
    //          susto e e avisado, e quem insiste volta para o inicio da dimensao. Sai-se
    //          chegando nela SEM olhar, e encostando.
    //   DOOR    (2) train e parkourland. As duas em que nao ha o que procurar — uma e uma
    //          reta entre paredoes intransponiveis, a outra e uma torre fechada e finita.
    //          Nas duas, avancar e a unica acao possivel, e a porta cobra exatamente ela.
    //
    // ⚠️ POR QUE AS OUTRAS TRES MORRERAM (2026-07-31, decisao do Pedro). RADIO, EJECT e
    // DARKROOM existiram, funcionaram e foram removidos no dia em que ele modelou a
    // entidade do Espelho. O que os quatro metodos do `fuga.rtf` tinham de bom era
    // VARIEDADE; o que se perdia era IDENTIDADE — a saida de uma dimensao de terror
    // acabava sendo um quebra-cabeca de radio ou uma caca a tres itens, e nenhum dos dois
    // da medo. O Espelho da. Trocar quatro mecanicas boas por uma que assusta foi o
    // negocio certo, e o codigo dos quatro saiu junto: bloco registrado que nada mais gera
    // vira enfeite no criativo prometendo mecanica que nao existe.
    //
    // Sobrou UMA regra de quem mexer nisto: as quinze precisam de saida, sempre. Ver
    // `DimSpawn.exitAnchor`, que nao tem implementacao padrao por causa disso.
    private static final Map<String, DimensionProfile> BY_ID = new LinkedHashMap<>();

    private static void put(DimensionProfile profile) {
        BY_ID.put(profile.id(), profile);
    }

    static {
        // DATA: labirinto fechado e escuro. O Greyface caça pelos corredores.
        put(new DimensionProfile("data", Mold.MAZE, AnomalyType.GREYFACE, Director.HUNTER, ExitMethod.MIRROR));

        // CHUNKS: o mundo picado. O Ofanim e a unica criatura que faz sentido ali —
        // ele voa, e num lugar onde o chao acaba a cada dezesseis blocos uma coisa
        // que atravessa o vazio em linha reta e a pior noticia possivel.
        put(new DimensionProfile("chunks", Mold.ISLANDS, AnomalyType.OPHANIM, Director.OPHANIM, ExitMethod.MIRROR));

        // INSIDIOUS: saloes de pedra sem teto sobre o vazio preto, em labirinto com
        // becos sem saida. Ainda NAO tem morador — o Pedro escolhe depois, junto com
        // o coracao que ele vai modelar. Ate la, `anomaly` nulo quer dizer "aqui vale
        // o sorteio normal", e `Director.NONE` quer dizer "ninguem repoe nada".
        put(new DimensionProfile("insidious", Mold.HALLS, null, Director.NONE, ExitMethod.MIRROR));

        // ------------------------------------------------------------------ o lote de 6
        //
        // As seis de 2026-07-29. Todas com `Director.NONE` e `anomaly` nulo, e isso e
        // decisao e nao pendencia: quem mora em cada uma e escolha do Pedro, e por a
        // caçadora nas nove so porque ela existe apagaria o que distingue uma da outra.
        // A LICAO Nº2 vale para as seis quando a hora chegar — dimensao de criatura unica
        // usa DIRETOR, nunca tabela de spawn. Os biomas delas nascem com `spawners` vazio
        // justamente para nao dar para esquecer disso.

        put(new DimensionProfile("village", Mold.STREETS, null, Director.NONE, ExitMethod.MIRROR));
        put(new DimensionProfile("grassrooms", Mold.ROOMS, null, Director.NONE, ExitMethod.MIRROR));

        // TRAIN: 0.70. A foto que o Pedro deu para esta e um breu com bruma cinza em
        // que se ve um morro e mais nada. Fechar a bruma aqui tem uma segunda funcao,
        // que e a razao de ela ser mais apertada que a media: o paredao de pedra dos
        // lados da linha precisa MORRER dentro da neblina. Se der para ver o topo dele,
        // deixa de ser "o mundo acaba ali" e vira uma parede com altura conhecida.
        // ⚠️ A UNICA COM `ExitMethod.DOOR`, e a unica sem sala de saida nenhuma. O primeiro
        // reparto deu RADIO a ela, por ter ceu — e estava errado pelo motivo mais basico
        // possivel: a TRAIN e uma reta entre dois paredoes que nao se pode alcancar, e a
        // unica coisa que existe para se fazer nela e andar para frente ou para tras. Uma
        // sala com um radio dentro seria um LUGAR AONDE IR numa dimensao cujo assunto e
        // nao haver aonde ir; ela apagaria justamente o que a distingue das outras catorze.
        //
        // A porta nao esta em lugar nenhum ate existir: ela nasce nos trilhos, a frente de
        // quem ja andou o bastante. O metodo cobra a unica coisa que a dimensao permite.
        put(new DimensionProfile("train", Mold.LINE, null, Director.NONE, 0.60f, ExitMethod.DOOR));

        // UNDER PRESSURE: 0.45, o pedido literal de "deixar o fog mais proximo do
        // jogador". Debaixo d'agua o jogo poe a neblina dele e a nossa nem entra (o
        // OldFog sai fora quando `FogType != NONE`); este numero e o que vale na
        // SUPERFICIE, que e onde o jogador spawna e o unico lugar de onde daria para
        // ver quantos submarinos existem.
        put(new DimensionProfile("under_pressure", Mold.FLOODED, null, Director.NONE, 0.35f, ExitMethod.MIRROR));

        put(new DimensionProfile("biblioteca", Mold.ROOMS, null, Director.NONE, ExitMethod.MIRROR));

        // PARKOURLAND: 0.30, a mais fechada de todas, e aqui a bruma e REGRA DE JOGO e
        // nao clima. "Neblina muito densa e escura pra dificultar o parkour" quer dizer
        // que o proximo bloco tem que aparecer perto o bastante para o pulo ainda ser
        // justo e longe o bastante para nao dar para planejar tres pulos a frente.
        //
        // A CONTA, para quem for mexer: o fim da bruma e `rd*16*2 * curva * densidade`.
        // Com 12 chunks de render a curva do chao vale 0.80, entao o normal termina em
        // 307 blocos e este 0.15 termina em 46 — a espiral inteira tem 24 de largura,
        // logo o jogador ve o lado em que esta e perde o outro. E o numero que eu mais
        // esperaria o Pedro querer mexer depois de jogar.
        put(new DimensionProfile("parkourland", Mold.TOWER, null, Director.NONE, 0.15f, ExitMethod.DOOR));

        // ------------------------------------------------------------------ o lote de 3
        //
        // As tres do bloco "NOVAS DIMENSOES" de 2026-07-30. Como as seis anteriores,
        // nascem com `anomaly` nulo e `Director.NONE`: quem mora em cada uma e escolha do
        // Pedro, e a LICAO Nº2 (dimensao de criatura unica usa Diretor, nunca tabela de
        // spawn) continua valendo para as tres quando a hora chegar.

        // STONELAND: 0.85. Levemente mais fechada que o normal e so isso — e o unico
        // lugar do mod com horizonte, e um horizonte precisa caber na vista para que o
        // relevo conte a piada dele (a forma do overworld no material errado). Fechar
        // mais esconderia o morro do fundo, que e a metade da imagem.
        put(new DimensionProfile("stoneland", Mold.TERRAIN, null, Director.NONE, 0.85f, ExitMethod.MIRROR));

        // ESCRITORIO: 0.55, e aqui a bruma tem TRABALHO e nao clima. "Havera uma neblina
        // densa e diversos predios altos de pedra ao fundo": e ela que faz a terceira
        // fileira de torres virar silhueta e a quarta sumir. Sem ela o jogador contaria
        // as torres, e uma coisa que se pode contar deixa de ser infinita.
        put(new DimensionProfile("escritorio", Mold.ROOMS, null, Director.NONE, 0.55f, ExitMethod.MIRROR));

        // MAZE: 0.14, e nao mais 0.50. A bruma e regra de jogo do mesmo jeito que na
        // PARKOURLAND — enxergar o corredor inteiro ate a parede do fim entregaria de
        // graca que aquele braco e um beco —, mas 0.50 dava 153 blocos e o salao tem 76:
        // a bruma terminava DEPOIS do fim do corredor e portanto nao existia. Foi o que a
        // foto de dentro do jogo mostrou. Com 0.14 ela termina em 43 e o fundo do salao
        // ja e mancha, que e o que a foto de referencia do Pedro tem.
        put(new DimensionProfile("maze", Mold.MAZE, null, Director.NONE, 0.14f, ExitMethod.MIRROR));

        // ------------------------------------------------------------------ o lote de 3
        //
        // As tres do `dimensions.rtf`, 2026-07-31. Como as nove anteriores, nascem com
        // `anomaly` nulo e `Director.NONE`: quem mora em cada uma e escolha do Pedro, e a
        // LICAO Nº2 (dimensao de criatura unica usa Diretor, nunca tabela de spawn)
        // continua valendo para as tres quando a hora chegar.

        // FLORESTA: 0.38. As duas fotos sao QUASE SO BRUMA — o celeiro da primeira ja
        // esta meio comido por ela a vinte metros, e as arvores do fundo da segunda sao
        // manchas. Fechar assim tem um segundo efeito, que e o que justifica o numero
        // baixo num lugar aberto: com a floresta densa e a bruma curta, o jogador perde a
        // referencia de direcao em uns poucos passos. Numa dimensao sem parede, e a
        // neblina que faz as vezes de labirinto.
        put(new DimensionProfile("floresta", Mold.TERRAIN, null, Director.NONE, 0.38f, ExitMethod.MIRROR));

        // PIPE TUNELS: 0.30. Corredor reto de concreto, e a bruma aqui trabalha igual a
        // da MAZE: o fim do tunel tem que morrer no escuro. A diferenca e a luminaria —
        // ela poe uma poça de luz a cada 11 blocos, e com a bruma neste ponto o jogador
        // ve a proxima luz mas nao o que ha entre ela e ele. Abrir isto entregaria o
        // cruzamento inteiro de longe e a dimensao viraria um mapa.
        put(new DimensionProfile("pipe_tunels", Mold.MAZE, null, Director.NONE, 0.30f, ExitMethod.MIRROR));

        // MALL: NORMAL_FOG, e e a UNICA das quinze que nao aperta a bruma nem um pouco.
        //
        // ⚠️ ISTO CONTRARIA O REFLEXO DAS OUTRAS CATORZE, de proposito. Em toda outra
        // dimensao a bruma esconde, e esconder assusta. Aqui o pedido do Pedro tem tres
        // palavras que dizem o contrario — "gigante", "abertas", "espacosas" — e as tres
        // fotos sao de corredor vazio SUMINDO AO LONGE. Apertar a bruma como nas outras
        // acabaria o corredor a quarenta blocos, e o shopping infinito viraria uma sala.
        // O medo aqui nao e de nao ver o fim: e de VER o fim, e ele estar tao longe
        // quanto o comeco, e nao haver ninguem em todo o caminho.
        //
        // ⚠️ E POR QUE NAO MAIS DE 1.0, ja que o pedido e "aberto"? Porque acima disso a
        // bruma passa a terminar DEPOIS da ultima chunk desenhada, e ai o corredor nao
        // sumiria na neblina — ele acabaria num corte seco, com o vazio do mundo nao
        // carregado atras. O `far` do OldFog ja e o dobro da distancia de render
        // justamente para essa margem existir; gastar a margem toda entrega a borda do
        // mundo, que e o oposto exato de "gigante". 1.0 e o teto util, e nao um meio
        // termo.
        put(new DimensionProfile("mall", Mold.STREETS, null, Director.NONE, ExitMethod.MIRROR));
    }

    /** Todas as dimensoes do mod, na ordem em que foram feitas. */
    public static java.util.Collection<DimensionProfile> all() {
        return BY_ID.values();
    }

    // ------------------------------------------------------------------ consulta
    /**
     * O perfil pelo id cru ("maze", "pipe_tunels"), sem passar por `Level`.
     *
     * Serve a quem esta DENTRO da geracao: o gerador de chunk conhece o proprio nome mas
     * nao tem `Level` na mao — ele roda antes de o mundo existir, e numa thread de
     * trabalho. Ver ExitSite.stamp.
     */
    @Nullable
    public static DimensionProfile byId(String id) {
        return BY_ID.get(id);
    }

    /** O perfil desta dimensao, ou null se ela nao e nossa. */
    @Nullable
    public static DimensionProfile of(@Nullable ResourceLocation dimension) {
        if (dimension == null || !dimension.getNamespace().equals(RECMod.MOD_ID)) return null;
        return BY_ID.get(dimension.getPath());
    }

    /**
     * Aceita `LevelAccessor` para atender aos dois chamadores sem sobrecarga.
     *
     * ⚠️ Sobrecarga aqui NAO compila: `ServerLevel` e ao mesmo tempo um `Level` e um
     * `ServerLevelAccessor`, entao `of(serverLevel)` ficaria ambiguo. O `instanceof`
     * resolve, e o de `Level` vem primeiro porque `ServerLevel` cai nele e ja sabe
     * responder `dimension()` por conta propria.
     */
    @Nullable
    public static DimensionProfile of(@Nullable LevelAccessor level) {
        if (level instanceof Level actual) return of(actual.dimension().location());
        if (level instanceof ServerLevelAccessor access) {
            return of(access.getLevel().dimension().location());
        }
        return null;
    }

    /** Estamos numa dimensao nossa com este feitio? */
    public static boolean isMold(@Nullable LevelAccessor level, Mold mold) {
        DimensionProfile profile = of(level);
        return profile != null && profile.mold() == mold;
    }

    /** Estamos numa dimensao nossa conduzida por este Diretor? */
    public static boolean isDirectedBy(@Nullable LevelAccessor level, Director director) {
        DimensionProfile profile = of(level);
        return profile != null && profile.director() == director;
    }
}
