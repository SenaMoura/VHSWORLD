package net.vhsworld.rec.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Os minerios que faltavam no fundo do mundo alpha.
 *
 * O PORQUE: o nosso terreno vai de y=0 a y=128, mas o tipo de dimensao continua sendo
 * o overworld normal, cujo fundo e -64. As faixas de altura do jogo moderno sao
 * escritas contra ESSE fundo — o diamante, por exemplo, e um trapezio de -144 a 16
 * com o pico em -64. Num mundo que comeca no zero, a distribuicao inteira cai fora:
 * o diamante praticamente nao existia, a redstone saia em um quinto do normal, o ouro
 * em um terco, o lapis pela metade. O fundo do mundo alpha estava esteril.
 *
 * A CURA e este passe, que roda so no nosso gerador — nao mexe em bioma nenhum, entao
 * o mundo padrao (Moderner Beta) continua exatamente como estava. Ele NAO repete o que
 * o jogo ja entrega bem: carvao, ferro, cobre e esmeralda tem faixas que cabem dentro
 * de 0..128 e continuam vindo da decoracao normal do bioma. Aqui so entra o que o
 * mundo de 128 blocos nao consegue servir, com a distribuicao da epoca: quanto mais
 * fundo, mais valioso, e o diamante colado na bedrock.
 *
 * Roda depois das cavernas de proposito. Assim nenhum veio e cavado fora depois de
 * colocado, e o minerio que aparece na parede do tunel e o mesmo que estava ali.
 */
public final class AlphaOres {

    /** Um tipo de veio: o bloco, quantos por chunk, o tamanho e a faixa de altura. */
    private record Vein(BlockState state, int count, int size, int minY, int maxY) {}

    private final Vein[] veins;

    public AlphaOres() {
        // Numeros da era alpha, ajustados para COMPLETAR o que o vanilla ainda entrega
        // nesta faixa, e nao para dobrar. O diamante e o unico que vem inteiro daqui.
        this.veins = new Vein[]{
                new Vein(Blocks.DIAMOND_ORE.defaultBlockState(),  2, 7,  2, 16),
                new Vein(Blocks.REDSTONE_ORE.defaultBlockState(), 5, 8,  2, 16),
                new Vein(Blocks.LAPIS_ORE.defaultBlockState(),    1, 6,  4, 30),
                new Vein(Blocks.GOLD_ORE.defaultBlockState(),     2, 8,  2, 32),
        };
    }

    public void place(ChunkAccess chunk, RandomSource random) {
        ChunkPos pos = chunk.getPos();
        int originX = pos.getMinBlockX();
        int originZ = pos.getMinBlockZ();

        for (Vein vein : veins) {
            for (int i = 0; i < vein.count(); i++) {
                int x = originX + random.nextInt(16);
                int y = vein.minY() + random.nextInt(vein.maxY() - vein.minY() + 1);
                int z = originZ + random.nextInt(16);
                placeVein(chunk, random, vein, x, y, z);
            }
        }
    }

    /**
     * Um veio.
     *
     * A forma vem do gerador antigo e vale a explicacao: o veio nao e uma bola, e um
     * SEGMENTO DE RETA com espessura variavel — sorteia-se um angulo no plano, marca-se
     * um comeco e um fim, e caminha-se de um ao outro engordando no meio (o seno) e
     * afinando nas pontas. E isso que da o veio comprido e torto de sempre, em vez do
     * amontoado redondo que um ruido simples produziria.
     */
    private void placeVein(ChunkAccess chunk, RandomSource random, Vein vein, int x, int y, int z) {
        int size = vein.size();
        float angle = random.nextFloat() * (float) Math.PI;
        double reach = size / 8.0;

        double x0 = x + Mth.sin(angle) * reach, x1 = x - Mth.sin(angle) * reach;
        double z0 = z + Mth.cos(angle) * reach, z1 = z - Mth.cos(angle) * reach;
        double y0 = y + random.nextInt(3) - 2, y1 = y + random.nextInt(3) - 2;

        ChunkPos pos = chunk.getPos();
        int chunkMinX = pos.getMinBlockX(), chunkMinZ = pos.getMinBlockZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int step = 0; step <= size; step++) {
            double t = step / (double) size;
            double cx = Mth.lerp(t, x0, x1);
            double cy = Mth.lerp(t, y0, y1);
            double cz = Mth.lerp(t, z0, z1);

            double fuzz = random.nextDouble() * size / 16.0;
            double radius = (Mth.sin((float) (Math.PI * t)) + 1.0) * fuzz + 1.0;
            double half = radius / 2.0;

            int bx0 = Mth.floor(cx - half), bx1 = Mth.floor(cx + half);
            int by0 = Mth.floor(cy - half), by1 = Mth.floor(cy + half);
            int bz0 = Mth.floor(cz - half), bz1 = Mth.floor(cz + half);

            for (int bx = bx0; bx <= bx1; bx++) {
                // O veio e cortado na borda do chunk: cada chunk so escreve em si
                // mesmo. Veio de 8 blocos raramente encosta na divisa, e escrever no
                // vizinho durante a geracao e o caminho curto para travar o jogo.
                if (bx < chunkMinX || bx > chunkMinX + 15) continue;
                double dx = (bx + 0.5 - cx) / half;

                for (int bz = bz0; bz <= bz1; bz++) {
                    if (bz < chunkMinZ || bz > chunkMinZ + 15) continue;
                    double dz = (bz + 0.5 - cz) / half;
                    if (dx * dx + dz * dz >= 1.0) continue;

                    for (int by = by0; by <= by1; by++) {
                        if (by < 1) continue;   // a bedrock do fundo fica de pe
                        double dy = (by + 0.5 - cy) / half;
                        if (dx * dx + dy * dy + dz * dz >= 1.0) continue;

                        cursor.set(bx, by, bz);
                        if (!chunk.getBlockState(cursor).is(Blocks.STONE)) continue;
                        chunk.setBlockState(cursor, vein.state(), false);
                    }
                }
            }
        }
    }
}
