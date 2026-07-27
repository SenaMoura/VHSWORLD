package net.vhsworld.rec.worldgen.dim;

import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Uma construcao do Pedro, ja em blocos, pronta para ser carimbada no mundo.
 *
 * Os blocos ficam num vetor de indices para a paleta local (a peca de andesito tem
 * 3 estados distintos; a hub tem 24). Uma peca inteira cabe em poucos kilobytes, e
 * o mapa inteiro da dimensao e a mesma peca repetida com giro e deslocamento
 * diferentes — nunca uma copia dos blocos.
 */
public final class DimPiece {

    public final String name;
    public final int width, height, length;
    public final int weight;
    public final boolean hub;

    /**
     * O ponto da peca pelo qual o gerador a encaixa.
     *
     * Nos corredores da DATA nao serve para nada — la o encaixe e pelas portas. Nas
     * ilhas da CHUNKS e o que sustenta tudo: (x,z) e o canto do quadrado de chao de
     * 16x16 dentro de uma selecao maior, e `y` e a altura da grama. E o que faz uma
     * ilha com copa transbordando alinhar com uma ilha pelada, e o que faz o tabuleiro
     * da ponte encostar no chao no mesmo nivel em vez de um degrau acima.
     */
    public final int anchorX, anchorY, anchorZ;

    /**
     * A paleta ja girada nos quatro sentidos.
     *
     * Girar a peca nao e so mover o bloco de lugar: uma escada virada para o leste
     * tem que passar a apontar para o sul. `BlockState.rotate` sabe fazer isso, mas
     * chamar por bloco seria centenas de milhares de vezes por mapa. Como a paleta
     * tem no maximo algumas dezenas de estados, gira-se a PALETA uma vez e o
     * carimbo vira uma leitura de vetor.
     */
    private final BlockState[][] palettes = new BlockState[4][];
    private final short[] cells;          // indice: y*length*width + z*width + x
    public final Connector[] connectors;

    private static final Rotation[] ROTATIONS = {
            Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90
    };

    DimPiece(String name, int width, int height, int length, int weight, boolean hub,
             int anchorX, int anchorY, int anchorZ,
             BlockState[] palette, short[] cells, Connector[] connectors) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.length = length;
        this.weight = weight;
        this.hub = hub;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.anchorZ = anchorZ;
        this.cells = cells;
        this.connectors = connectors;

        for (int rotation = 0; rotation < 4; rotation++) {
            BlockState[] turned = new BlockState[palette.length];
            for (int i = 0; i < palette.length; i++) {
                turned[i] = palette[i].rotate(ROTATIONS[rotation]);
            }
            palettes[rotation] = turned;
        }
    }

    public BlockState at(int x, int y, int z, int rotation) {
        int i = (y * length + z) * width + x;
        return palettes[rotation][cells[i]];
    }

    /** Tamanho depois do giro: nos giros impares a peca troca comprimento por largura. */
    public int rotatedWidth(int rotation) {
        return (rotation & 1) == 0 ? width : length;
    }

    public int rotatedLength(int rotation) {
        return (rotation & 1) == 0 ? length : width;
    }
}
