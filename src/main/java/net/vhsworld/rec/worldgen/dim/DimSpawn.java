package net.vhsworld.rec.worldgen.dim;

import net.minecraft.core.BlockPos;

/**
 * "Onde a fita larga o jogador" — perguntado ao proprio gerador da dimensao.
 *
 * ⚠️ ISTO E UMA CORRECAO DE CLASSE DE BUG, nao arrumacao. Antes, a fita descobria o
 * ponto de nascimento com um `instanceof` em cadeia, um ramo por gerador, e o
 * comentario dizia "dimensao nova, uma linha nova". A CHUNKS foi esquecida nessa
 * cadeia e caiu no (0,64,0) de reserva, que naquela dimensao era o miolo da pedra da
 * coluna central — o jogador entrava dentro de um bloco. O defeito nao foi distracao:
 * foi um lugar onde acrescentar dimensao exige lembrar de um arquivo distante.
 *
 * Com a interface, o gerador que nao responder isto nao COMPILA como dimensao do mod,
 * e a cadeia de `instanceof` vira uma linha so que vale para as 21.
 */
public interface DimSpawn {

    /** O bloco em que o jogador nasce ao entrar pela fita. */
    BlockPos dimensionSpawn();

    /**
     * O id desta dimensao — "maze", "pipe_tunels" —, o mesmo do arquivo em
     * `data/recmod/dimension/`.
     *
     * ⚠️ NAO E O `name()` DO F3, e a tentacao de reaproveitar aquele foi real: ele ja
     * existe e da "PIPE TUNELS", que vira "pipe_tunels" com um lowercase e um replace.
     * Funcionaria hoje para as quinze e e um defeito esperando: `name()` e um rotulo de
     * DEPURACAO, ninguem tem obrigacao de mante-lo parecido com o id, e no dia em que
     * alguem escrever "MAZE (v2)" ali a dimensao perde a sala de saida — sem erro de
     * compilacao, sem log, sem nada. So um jogador preso.
     */
    String dimensionId();

    /**
     * Um ponto ANDAVEL desta dimensao, na regiao (rx, rz) da grade do ExitSite.
     *
     * ⚠️ ISTO NAO TEM IMPLEMENTACAO PADRAO DE PROPOSITO, e e a trava mais importante que
     * o mod ganhou na v1.70.0. Desde que a fita virou so ida, a sala de saida e o UNICO
     * caminho de volta: uma dimensao que nao souber dizer onde ela cabe e um jogador
     * preso para sempre. Sem padrao, gerador novo que esqueca disto nao compila — e o
     * compilador e a unica coisa que nao esquece.
     *
     * "Andavel" quer dizer o mesmo que no `dimensionSpawn`: chao solido embaixo e ar para
     * o corpo. A diferenca e que aqui a resposta tem que ser DETERMINISTA — a mesma
     * regiao devolve sempre o mesmo ponto, porque o bilhete, os fragmentos e o proprio
     * chunk que desenha a sala perguntam isto separadamente e precisam concordar.
     * Devolver `dimensionSpawn()` aqui e um defeito, ainda que compile: ele sorteia.
     *
     * O Y devolvido e o do PISO em que se anda (o bloco de ar em cima do chao), que e a
     * mesma convencao do `dimensionSpawn`.
     */
    BlockPos exitAnchor(int rx, int rz);
}
