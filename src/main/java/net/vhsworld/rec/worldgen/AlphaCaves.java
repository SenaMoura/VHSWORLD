package net.vhsworld.rec.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * As cavernas antigas: tuneis longos, sinuosos e que se cruzam.
 *
 * A diferenca para a caverna moderna nao e o tamanho, e a INTENCAO. A moderna e
 * desenhada para ser navegavel, com cavernas-catedral e ligacoes claras. Esta aqui e
 * uma minhoca cega: parte de um ponto aleatorio, escolhe uma direcao e cava,
 * virando devagar, as vezes se partindo em duas. Ninguem projetou o resultado — e
 * por isso que ele surpreende.
 *
 * Cada chunk decide os seus proprios tuneis a partir de uma semente estavel, e o
 * chunk sendo gerado consulta os vizinhos num raio de 8. Assim um tunel que nasce
 * longe ainda chega inteiro aqui, e o mesmo mundo sempre da a mesma caverna.
 */
public final class AlphaCaves {

    /** De quantos chunks de distancia um tunel ainda pode alcancar este aqui. */
    private static final int RANGE = 8;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState LAVA = Blocks.LAVA.defaultBlockState();

    private final int seaLevel;
    private final int genHeight;

    public AlphaCaves(int seaLevel, int genHeight) {
        this.seaLevel = seaLevel;
        this.genHeight = genHeight;
    }

    public void carve(ChunkAccess chunk, PositionalSeed seeds) {
        ChunkPos pos = chunk.getPos();

        for (int cx = pos.x - RANGE; cx <= pos.x + RANGE; cx++) {
            for (int cz = pos.z - RANGE; cz <= pos.z + RANGE; cz++) {
                RandomSource random = seeds.at(cx, cz);
                carveFromChunk(chunk, random, cx, cz);
            }
        }
    }

    /**
     * Quantos sistemas de caverna nascem neste chunk.
     *
     * O sorteio aninhado (aleatorio de aleatorio de aleatorio) e o coracao da coisa:
     * ele faz o zero ser comum e o numero alto ser raro. A maior parte do mundo fica
     * macica, e de vez em quando aparece um emaranhado enorme. Um sorteio plano
     * daria cavernas de densidade uniforme, e o mundo perderia o susto.
     */
    private void carveFromChunk(ChunkAccess chunk, RandomSource random, int cx, int cz) {
        int systems = random.nextInt(random.nextInt(random.nextInt(15) + 1) + 1);
        if (random.nextInt(7) != 0) {
            systems = 0;
        }

        for (int i = 0; i < systems; i++) {
            double x = cx * 16 + random.nextInt(16);
            double y = random.nextInt(random.nextInt(genHeight - 8) + 8);
            double z = cz * 16 + random.nextInt(16);

            int branches = 1;
            if (random.nextInt(4) == 0) {
                // Uma sala redonda no ponto de partida, e nao so um tunel
                room(chunk, random.nextLong(), x, y, z, 1.0f + random.nextFloat() * 6.0f);
                branches += random.nextInt(4);
            }

            for (int b = 0; b < branches; b++) {
                float yaw = random.nextFloat() * (float) Math.PI * 2.0f;
                float pitch = (random.nextFloat() - 0.5f) * 2.0f / 8.0f;
                float radius = random.nextFloat() * 2.0f + random.nextFloat();

                tunnel(chunk, random.nextLong(), x, y, z, radius, yaw, pitch, 0, 0, 1.0);
            }
        }
    }

    private void room(ChunkAccess chunk, long seed, double x, double y, double z, float radius) {
        tunnel(chunk, seed, x, y, z, radius, 0.0f, 0.0f, -1, -1, 0.5);
    }

    /**
     * Cava um tunel andando passo a passo.
     *
     * A direcao muda pouco a cada passo e as mudancas sao amortecidas — e isso que
     * produz curva longa em vez de zigue-zague. O tunel tambem se parte em dois de
     * vez em quando, que e como surgem os cruzamentos.
     */
    private void tunnel(ChunkAccess chunk, long seed, double x, double y, double z,
                        float radius, float yaw, float pitch,
                        int step, int steps, double heightRatio) {
        RandomSource random = RandomSource.create(seed);

        ChunkPos pos = chunk.getPos();
        double centerX = pos.getMinBlockX() + 8;
        double centerZ = pos.getMinBlockZ() + 8;

        float yawChange = 0.0f;
        float pitchChange = 0.0f;

        if (steps <= 0) {
            int limit = RANGE * 16 - 16;
            steps = limit - random.nextInt(limit / 4);
        }

        boolean single = false;
        if (step == -1) {
            step = steps / 2;
            single = true;
        }

        int split = random.nextInt(steps / 2) + steps / 4;

        for (; step < steps; step++) {
            double rx = 1.5 + Mth.sin(step * (float) Math.PI / steps) * radius;
            double ry = rx * heightRatio;

            x += Mth.cos(yaw) * Mth.cos(pitch);
            z += Mth.sin(yaw) * Mth.cos(pitch);
            y += Mth.sin(pitch);

            pitch *= single ? 0.92f : 0.7f;
            pitch += pitchChange * 0.1f;
            yaw += yawChange * 0.1f;

            pitchChange *= 0.9f;
            yawChange *= 0.75f;
            pitchChange += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0f;
            yawChange += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0f;

            // O tunel se parte em dois: e assim que nascem os cruzamentos
            if (!single && step == split && radius > 1.0f) {
                tunnel(chunk, random.nextLong(), x, y, z,
                        random.nextFloat() * 0.5f + 0.5f,
                        yaw - (float) Math.PI / 2.0f, pitch / 3.0f, step, steps, 1.0);
                tunnel(chunk, random.nextLong(), x, y, z,
                        random.nextFloat() * 0.5f + 0.5f,
                        yaw + (float) Math.PI / 2.0f, pitch / 3.0f, step, steps, 1.0);
                return;
            }

            if (!single && random.nextInt(4) == 0) continue;

            // Longe demais deste chunk para valer a pena continuar calculando
            double dx = x - centerX;
            double dz = z - centerZ;
            double stepsLeft = steps - step;
            double reach = radius + 18.0f;
            if (dx * dx + dz * dz - stepsLeft * stepsLeft > reach * reach) return;

            if (x < centerX - 16 - rx * 2 || z < centerZ - 16 - rx * 2
                    || x > centerX + 16 + rx * 2 || z > centerZ + 16 + rx * 2) continue;

            hollow(chunk, x, y, z, rx, ry);
        }
    }

    /** Abre o elipsoide de ar em volta de um ponto. */
    private void hollow(ChunkAccess chunk, double x, double y, double z, double rx, double ry) {
        ChunkPos pos = chunk.getPos();

        int x0 = Mth.clamp(Mth.floor(x - rx) - pos.getMinBlockX() - 1, 0, 16);
        int x1 = Mth.clamp(Mth.floor(x + rx) - pos.getMinBlockX() + 1, 0, 16);
        int y0 = Mth.clamp(Mth.floor(y - ry) - 1, 1, genHeight - 1);
        int y1 = Mth.clamp(Mth.floor(y + ry) + 1, 1, genHeight - 1);
        int z0 = Mth.clamp(Mth.floor(z - rx) - pos.getMinBlockZ() - 1, 0, 16);
        int z1 = Mth.clamp(Mth.floor(z + rx) - pos.getMinBlockZ() + 1, 0, 16);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // Nao cava por baixo de agua: uma caverna que abre no fundo do mar dreno
        // o oceano inteiro para dentro dela.
        for (int bx = x0; bx < x1; bx++) {
            for (int bz = z0; bz < z1; bz++) {
                for (int by = y1 + 1; by >= y0 - 1; by--) {
                    if (by < 0 || by >= genHeight) continue;
                    if (chunk.getBlockState(cursor.set(bx, by, bz)).is(Blocks.WATER)) return;
                }
            }
        }

        for (int bx = x0; bx < x1; bx++) {
            double fx = (bx + pos.getMinBlockX() + 0.5 - x) / rx;
            for (int bz = z0; bz < z1; bz++) {
                double fz = (bz + pos.getMinBlockZ() + 0.5 - z) / rx;
                if (fx * fx + fz * fz >= 1.0) continue;

                for (int by = y1; by > y0; by--) {
                    double fy = (by - 0.5 - y) / ry;
                    if (fy * fy + (fx * fx + fz * fz) >= 1.0) continue;

                    cursor.set(bx, by, bz);
                    BlockState here = chunk.getBlockState(cursor);
                    if (!here.is(Blocks.STONE) && !here.is(Blocks.DIRT) && !here.is(Blocks.GRASS_BLOCK)) continue;

                    chunk.setBlockState(cursor, by < 10 ? LAVA : AIR, false);
                }
            }
        }
    }

    /** Semente estavel por chunk, para o mesmo mundo dar sempre a mesma caverna. */
    public interface PositionalSeed {
        RandomSource at(int chunkX, int chunkZ);
    }
}
