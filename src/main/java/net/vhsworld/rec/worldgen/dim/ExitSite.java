package net.vhsworld.rec.worldgen.dim;

import net.minecraft.core.BlockPos;

/**
 * ONDE FICA A SAIDA de cada dimensao — e so isso.
 *
 * ============================ O QUE ESTA EM JOGO AQUI ============================
 *
 * Desde a v1.70.0 a fita e so ida. Isto quer dizer que a resposta deste arquivo e o unico
 * caminho de volta das quinze dimensoes, e que um defeito nele nao e um enfeite torto: e
 * um jogador preso para sempre.
 *
 * ============================ ELE JA FOI MUITO MAIOR ============================
 *
 * ⚠️ ATE A v1.71.0 ESTE ARQUIVO CONSTRUIA SALAS. Eram quatrocentas linhas: uma torre de
 * transmissao, uma sala de projecao com poltronas, um corredor de vidro preto, uma camara
 * escura — cada uma carimbada chunk a chunk, com escritor proprio, paleta por feitio de
 * dimensao e porta nos quatro lados para nao virar parede no meio do corredor.
 *
 * Tudo isso saiu na v1.72.0, quando o Pedro modelou a entidade do Espelho e ela virou a
 * saida de treze das quinze. A entidade nao precisa de sala: ela FICA DE PE no ponto, no
 * meio da dimensao, sem construcao nenhuma em volta — foi a escolha dele, e ela e melhor.
 * Um painel preto de dois metros e meio por oito, com tres olhos, sozinho no meio de uma
 * floresta de neblina, e mais estranho do que o mesmo painel dentro de um quarto. A sala
 * explicava a coisa; sem ela, a coisa nao tem explicacao.
 *
 * Sobrou a unica pergunta que sempre foi o assunto real: EM QUE PONTO. Quem responde
 * continua sendo cada gerador, pelo `DimSpawn.exitAnchor` — porque so a planta de cada
 * dimensao sabe onde ha chao.
 */
public final class ExitSite {

    private ExitSite() {}

    /**
     * Uma saida a cada tanto, nos dois eixos.
     *
     * ⚠️ 256 E PEQUENO DE PROPOSITO, e foi discutido contra 512. Saida rara e mais
     * assustadora — mas desde que ela e a unica volta, "raro" deixou de ser tempero e
     * virou risco: o jogador que nao achar nenhuma nao perde a partida, fica preso nela.
     * Achavel ganha de escasso toda vez que o custo do erro e esse.
     */
    public static final int REGION = 256;

    /** A regiao a que este bloco pertence. */
    public static int regionOf(int v) {
        return Math.floorDiv(v, REGION);
    }

    /**
     * A casa da grade `period` que fica no MEIO da regiao `r`.
     *
     * Serve para traduzir "regiao 3 do ExitSite" para "casa 16 da grade da MAZE" sem que
     * cada gerador tenha que saber o tamanho da regiao. E o meio e nao a borda de
     * proposito: uma saida ancorada na divisa ficaria a poucos passos da saida da regiao
     * vizinha, e duas ao alcance de vista uma da outra deixam de ser raras.
     */
    public static int cellInRegion(int r, int period) {
        return Math.floorDiv(r * REGION + REGION / 2, period);
    }

    /** O ponto da saida da regiao (rx, rz), ou null se o gerador nao souber. */
    public static BlockPos anchor(DimSpawn generator, int rx, int rz) {
        return generator.exitAnchor(rx, rz);
    }

    /**
     * A saida mais proxima deste ponto.
     *
     * Olha as nove regioes em volta e nao so a de baixo: quem esta perto da divisa tem a
     * saida do vizinho mais perto do que a propria, e mandar o jogador para a saida "dele"
     * seria mandar ele andar para longe da que ele quase alcancava.
     */
    public static BlockPos nearest(DimSpawn generator, BlockPos from) {
        int rx = regionOf(from.getX()), rz = regionOf(from.getZ());
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos at = anchor(generator, rx + dx, rz + dz);
                if (at == null) continue;
                double distance = at.distSqr(from);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = at;
                }
            }
        }
        return best;
    }
}
