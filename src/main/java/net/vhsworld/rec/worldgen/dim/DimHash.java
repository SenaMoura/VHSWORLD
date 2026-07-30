package net.vhsworld.rec.worldgen.dim;

/**
 * O sorteio das dimensoes infinitas: mistura a semente do mundo com uma casa da grade
 * e um sal, e devolve sempre o mesmo numero para a mesma pergunta.
 *
 * ⚠️ POR QUE NAO E UM `RandomSource`. Um gerador de numeros responde na ORDEM em que
 * foi chamado, e num mundo infinito as perguntas chegam na ordem em que o jogador
 * andou — e em varias threads ao mesmo tempo. Duas partidas com a mesma semente
 * veriam mapas diferentes, e a mesma partida veria a casa mudar ao voltar nela. Hash
 * de (casa, sal) nao tem ordem: e uma funcao, nao um fluxo.
 *
 * Isto e a mesma mistura que a `ChunksLayout` ja usava por dentro. Ela nao foi mexida
 * — mexer no que esta em pe e de graca so na conta de quem nao paga o teste — mas as
 * seis dimensoes de 2026-07-29 leem daqui, para nao virarem seis copias do mesmo
 * punhado de constantes magicas.
 */
public final class DimHash {

    private DimHash() {
    }

    /** O numero cru. Use `frac` ou `pick` para transformar em decisao. */
    public static long of(long seed, int cx, int cz, long salt) {
        long h = seed;
        h = (h ^ (cx * 0x9E3779B97F4A7C15L)) * 0xFF51AFD7ED558CCDL;
        h ^= h >>> 29;
        h = (h ^ (cz * 0xC2B2AE3D27D4EB4FL)) * 0x94D049BB133111EBL;
        h ^= h >>> 32;
        h = (h ^ salt) * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 31;
        return h;
    }

    /** Entre 0 e 1, para comparar com chance. */
    public static double frac(long seed, int cx, int cz, long salt) {
        return (of(seed, cx, cz, salt) >>> 11) * 0x1.0p-53D;
    }

    /** Um inteiro em 0..bound-1. */
    public static int pick(long seed, int cx, int cz, long salt, int bound) {
        return (int) Math.floorMod(of(seed, cx, cz, salt), (long) bound);
    }

    /**
     * A aresta entre duas casas vizinhas, com a MESMA resposta pelos dois lados.
     *
     * Sem isto, a casa da esquerda decidiria "tem porta" e a da direita "nao tem", e
     * quem carimbasse por ultimo ganharia — o que muda com a ordem em que o jogador
     * carrega os chunks. Ordenar o par antes de misturar tira a ordem da conta.
     */
    public static boolean edge(long seed, int ax, int az, int bx, int bz, long salt, double chance) {
        boolean first = ax < bx || (ax == bx && az < bz);
        int lx = first ? ax : bx, lz = first ? az : bz;
        int hx = first ? bx : ax, hz = first ? bz : az;
        return frac(seed, lx, lz, hx * 31L + hz * 7919L + salt) < chance;
    }

    /** Escolhe uma peca respeitando o peso de cada uma. */
    public static DimPiece weighted(java.util.List<DimPiece> pieces, long seed, int cx, int cz, long salt) {
        int total = 0;
        for (DimPiece piece : pieces) total += Math.max(piece.weight, 0);
        if (total <= 0) return pieces.isEmpty() ? null : pieces.get(0);
        int roll = pick(seed, cx, cz, salt, total);
        for (DimPiece piece : pieces) {
            roll -= Math.max(piece.weight, 0);
            if (roll < 0) return piece;
        }
        return pieces.get(pieces.size() - 1);
    }
}
