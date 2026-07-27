package net.vhsworld.rec.worldgen.dim;

/**
 * Uma porta na parede de uma peca.
 *
 * O Pedro nao coloca bloco de jigsaw nas construcoes dele — ele so deixa o buraco.
 * Entao o conector E o buraco: um retangulo de ar na parede, achado pelo
 * `import_dimension.py` e assado no arquivo junto com os blocos.
 *
 * `a0..a1` corre AO LONGO da parede: e o X nas paredes norte/sul e o Z nas paredes
 * leste/oeste. `y0..y1` e a altura do vao, e sempre comeca em 1 (o piso e y=0).
 */
public record Connector(int face, int a0, int a1, int y0, int y1, int gauge) {

    public static final int NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3;

    /** Bitolas: um corredor baixo nunca desemboca na parede de um alto. */
    public static final int SMALL = 0, TALL = 1;

    public static int opposite(int face) {
        return (face + 2) & 3;
    }

    /** O eixo em que a porta se espalha, ja no mundo: X nas paredes N/S, Z nas L/O. */
    public static boolean alongX(int face) {
        return face == NORTH || face == SOUTH;
    }

    public int center() {
        return a0 + a1;   // dobro do centro, para casar sem perder o meio bloco
    }
}
