package net.vhsworld.rec.worldgen.dim;

/**
 * Uma peca posta no mundo: qual construcao, quanto girada, e onde fica o canto dela.
 *
 * O giro e 0..3 em passos de 90 graus no sentido horario. As formulas de ida e volta
 * estao aqui, e nao espalhadas: errar um sinal nelas nao da erro de compilacao, da
 * um corredor desencontrado que so aparece depois de entrar no mundo.
 */
public final class Placement {

    public final DimPiece piece;
    public final int rotation;
    public final int ox, oy, oz;

    /**
     * Quantos blocos a peca desce ALEM da propria base.
     *
     * So a CHUNKS usa. A ilha que o Pedro recortou tem 33 blocos de pedra sob a
     * grama; sozinha ela e um ladrilho, nao uma coluna. O gerador repete a faixa de
     * pedra dela para baixo por esta quantidade, e cada ilha sorteia a sua — e o que
     * faz o fundo ficar desigual em vez de um tabuleiro suspenso. Na DATA e sempre 0.
     */
    public final int depth;

    /** Semente de qual camada de pedra reaparece em cada altura da extensao. */
    public final long depthSeed;

    /**
     * Quais portas desta peca deram em outra peca, uma por bit.
     *
     * Tudo que NAO esta aqui e parede: as portas que nao levaram a lugar nenhum e,
     * principalmente, os buracos que nao sao porta nenhuma. As construcoes do Pedro
     * tem varios — a parede oeste da hub, por exemplo, tem um vao de 2x2 do lado da
     * porta de verdade. Sao enfeite dentro do predio dele; num predio cercado de
     * vazio, cada um seria uma queda sem fundo que o jogador so descobriria pisando.
     */
    private int open;

    public Placement(DimPiece piece, int rotation, int ox, int oy, int oz) {
        this(piece, rotation, ox, oy, oz, 0, 0L);
    }

    public Placement(DimPiece piece, int rotation, int ox, int oy, int oz, int depth, long depthSeed) {
        this.piece = piece;
        this.rotation = rotation;
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.depth = depth;
        this.depthSeed = depthSeed;
    }

    public int minX() { return ox; }
    public int minY() { return oy - depth; }
    public int minZ() { return oz; }
    public int maxX() { return ox + piece.rotatedWidth(rotation) - 1; }
    public int maxY() { return oy + piece.height - 1; }
    public int maxZ() { return oz + piece.rotatedLength(rotation) - 1; }

    public boolean overlaps(Placement other) {
        return minX() <= other.maxX() && other.minX() <= maxX()
                && minZ() <= other.maxZ() && other.minZ() <= maxZ()
                && minY() <= other.maxY() && other.minY() <= maxY();
    }

    // ------------------------------------------------------------------ ida
    public int worldX(int x, int z) {
        return ox + switch (rotation) {
            case 1 -> piece.length - 1 - z;
            case 2 -> piece.width - 1 - x;
            case 3 -> z;
            default -> x;
        };
    }

    public int worldZ(int x, int z) {
        return oz + switch (rotation) {
            case 1 -> x;
            case 2 -> piece.length - 1 - z;
            case 3 -> piece.width - 1 - x;
            default -> z;
        };
    }

    // ------------------------------------------------------------------ volta
    /** Inversa da de cima: dado um ponto do mundo, qual bloco da peca cai ali. */
    public int localX(int worldX, int worldZ) {
        return switch (rotation) {
            case 1 -> worldZ - oz;
            case 2 -> piece.width - 1 - (worldX - ox);
            case 3 -> piece.width - 1 - (worldZ - oz);
            default -> worldX - ox;
        };
    }

    public int localZ(int worldX, int worldZ) {
        return switch (rotation) {
            case 1 -> piece.length - 1 - (worldX - ox);
            case 2 -> piece.length - 1 - (worldZ - oz);
            case 3 -> worldX - ox;
            default -> worldZ - oz;
        };
    }

    // ------------------------------------------------------------------ portas
    public int worldFace(Connector connector) {
        return (connector.face() + rotation) & 3;
    }

    /**
     * Onde a porta cai no mundo: {inicio, fim, plano}.
     *
     * `inicio..fim` corre no eixo da parede ja girada (X nas paredes norte/sul, Z
     * nas leste/oeste) e `plano` e a coordenada do proprio muro — o X de uma parede
     * leste, o Z de uma parede norte. E dai que sai o encaixe: a peca nova entra
     * colada em `plano + 1` ou `plano - 1`.
     */
    public int[] doorSpan(Connector connector) {
        int[] first = doorCell(connector, connector.a0());
        int[] last = doorCell(connector, connector.a1());
        int face = worldFace(connector);
        if (Connector.alongX(face)) {
            return new int[]{Math.min(first[0], last[0]), Math.max(first[0], last[0]), first[1]};
        }
        return new int[]{Math.min(first[1], last[1]), Math.max(first[1], last[1]), first[0]};
    }

    /** A celula do mundo em que a porta esta, para a posicao `a` ao longo da parede. */
    private int[] doorCell(Connector connector, int a) {
        int x, z;
        switch (connector.face()) {
            case Connector.NORTH -> { x = a; z = 0; }
            case Connector.SOUTH -> { x = a; z = piece.length - 1; }
            case Connector.WEST  -> { x = 0; z = a; }
            default              -> { x = piece.width - 1; z = a; }
        }
        return new int[]{worldX(x, z), worldZ(x, z)};
    }

    void markOpen(int connectorIndex) {
        open |= 1 << connectorIndex;
    }

    /** A celula esta num vao que leva a outra peca? Se nao, ela vira parede. */
    public boolean isDoorway(int worldX, int worldY, int worldZ) {
        for (int i = 0; i < piece.connectors.length; i++) {
            if ((open & (1 << i)) == 0) continue;
            int[] box = doorBox(piece.connectors[i]);
            if (worldX >= box[0] && worldX <= box[3]
                    && worldY >= box[1] && worldY <= box[4]
                    && worldZ >= box[2] && worldZ <= box[5]) return true;
        }
        return false;
    }

    /** A celula esta numa das quatro paredes externas da peca? */
    public boolean onOuterWall(int localX, int localZ) {
        return localX == 0 || localX == piece.width - 1
                || localZ == 0 || localZ == piece.length - 1;
    }

    /** A caixa de blocos do vao, em coordenadas do mundo. */
    public int[] doorBox(Connector connector) {
        int[] first = doorCell(connector, connector.a0());
        int[] last = doorCell(connector, connector.a1());
        return new int[]{
                Math.min(first[0], last[0]), oy + connector.y0(), Math.min(first[1], last[1]),
                Math.max(first[0], last[0]), oy + connector.y1(), Math.max(first[1], last[1])
        };
    }
}
