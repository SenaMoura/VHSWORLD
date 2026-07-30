package net.vhsworld.rec.worldgen.dim;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.AnomalyType;

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
        Director director) {

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
        TOWER
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
    private static final Map<String, DimensionProfile> BY_ID = new LinkedHashMap<>();

    private static void put(DimensionProfile profile) {
        BY_ID.put(profile.id(), profile);
    }

    static {
        // DATA: labirinto fechado e escuro. O Greyface caça pelos corredores.
        put(new DimensionProfile("data", Mold.MAZE, AnomalyType.GREYFACE, Director.HUNTER));

        // CHUNKS: o mundo picado. O Ofanim e a unica criatura que faz sentido ali —
        // ele voa, e num lugar onde o chao acaba a cada dezesseis blocos uma coisa
        // que atravessa o vazio em linha reta e a pior noticia possivel.
        put(new DimensionProfile("chunks", Mold.ISLANDS, AnomalyType.OPHANIM, Director.OPHANIM));

        // INSIDIOUS: saloes de pedra sem teto sobre o vazio preto, em labirinto com
        // becos sem saida. Ainda NAO tem morador — o Pedro escolhe depois, junto com
        // o coracao que ele vai modelar. Ate la, `anomaly` nulo quer dizer "aqui vale
        // o sorteio normal", e `Director.NONE` quer dizer "ninguem repoe nada".
        put(new DimensionProfile("insidious", Mold.HALLS, null, Director.NONE));

        // ------------------------------------------------------------------ o lote de 6
        //
        // As seis de 2026-07-29. Todas com `Director.NONE` e `anomaly` nulo, e isso e
        // decisao e nao pendencia: quem mora em cada uma e escolha do Pedro, e por a
        // caçadora nas nove so porque ela existe apagaria o que distingue uma da outra.
        // A LICAO Nº2 vale para as seis quando a hora chegar — dimensao de criatura unica
        // usa DIRETOR, nunca tabela de spawn. Os biomas delas nascem com `spawners` vazio
        // justamente para nao dar para esquecer disso.

        put(new DimensionProfile("village", Mold.STREETS, null, Director.NONE));
        put(new DimensionProfile("grassrooms", Mold.ROOMS, null, Director.NONE));
        put(new DimensionProfile("train", Mold.LINE, null, Director.NONE));
        put(new DimensionProfile("under_pressure", Mold.FLOODED, null, Director.NONE));
        put(new DimensionProfile("biblioteca", Mold.ROOMS, null, Director.NONE));
        put(new DimensionProfile("parkourland", Mold.TOWER, null, Director.NONE));
    }

    /** Todas as dimensoes do mod, na ordem em que foram feitas. */
    public static java.util.Collection<DimensionProfile> all() {
        return BY_ID.values();
    }

    // ------------------------------------------------------------------ consulta
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
