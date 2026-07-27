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
}
