package net.vhsworld.rec.client.entity.mesh;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Uma escultura de verdade, com a animacao ja resolvida, pronta para ser desenhada.
 *
 * O PROBLEMA QUE ISTO RESOLVE. O Minecraft nao sabe deformar malha com esqueleto: os
 * modelos dele sao hierarquias de CAIXAS, e nao ha como entregar ao jogo uma malha com
 * ossos e pedir que ele a dobre. Foi por isso que estas criaturas viraram sprite antes.
 * A saida nao e ensinar skinning ao jogo — e nao precisar dele: o Blender ja dobrou a
 * malha, quadro a quadro, e o que esta guardado aqui sao poses inteiras. Desenhar e
 * pegar duas poses e misturar.
 *
 * E a ideia do objmc (assar o vertice em vez de pedir skinning) sem a parte dele que nao
 * serve aqui. O objmc esconde os vertices dentro de um PNG e os remonta num CORE SHADER,
 * porque um resource pack nao roda codigo — e core shader morre com Embeddium e Oculus
 * ligados, que e o que o pack tem. Nos somos um mod: lemos o binario e desenhamos pelo
 * caminho de entidade comum, o mesmo de qualquer mob. Nada para quebrar.
 *
 * O formato sai de tools/pack_mesh.py; o contrato esta escrito la e e conferido por
 * tools/preview_mesh.py, que desenha o mesmo arquivo fora do jogo.
 */
public final class BakedMesh {

    private static final byte[] MAGIC = {'R', 'E', 'C', 'M'};

    private final int frames;
    private final int vertCount;

    /** A caixa da quantizacao: posicao = min + (u16 / 65535) * span. */
    private final float[] qmin;
    private final float[] qspan;

    /** Para cada canto de triangulo: de qual vertice ele puxa a posicao, e o UV dele. */
    private final int[] cornerVert;
    private final float[] cornerU;
    private final float[] cornerV;

    /** As poses. Indice = (quadro * vertices + vertice) * 3. */
    private final short[] positions;
    private final byte[] normals;

    /** Rascunho da pose misturada, reaproveitado — desenhar nao deveria criar lixo. */
    private final float[] blendPos;
    private final float[] blendNrm;

    private BakedMesh(int frames, int vertCount, float[] qmin, float[] qspan,
                      int[] cornerVert, float[] cornerU, float[] cornerV,
                      short[] positions, byte[] normals) {
        this.frames = frames;
        this.vertCount = vertCount;
        this.qmin = qmin;
        this.qspan = qspan;
        this.cornerVert = cornerVert;
        this.cornerU = cornerU;
        this.cornerV = cornerV;
        this.positions = positions;
        this.normals = normals;
        this.blendPos = new float[vertCount * 3];
        this.blendNrm = new float[vertCount * 3];
    }

    public int frames() {
        return this.frames;
    }

    public int triangles() {
        return this.cornerVert.length / 3;
    }

    public static BakedMesh read(InputStream stream) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(stream))) {
            byte[] magic = new byte[4];
            in.readFully(magic);
            for (int i = 0; i < 4; i++) {
                if (magic[i] != MAGIC[i]) throw new IOException("nao e um .mesh do recmod");
            }

            int version = in.readUnsignedByte();
            if (version != 1) throw new IOException("versao de .mesh desconhecida: " + version);

            int frames = in.readUnsignedByte();
            int vertCount = in.readUnsignedShort();
            int cornerCount = in.readInt();

            float[] qmin = new float[3];
            float[] qmax = new float[3];
            for (int i = 0; i < 3; i++) qmin[i] = in.readFloat();
            for (int i = 0; i < 3; i++) qmax[i] = in.readFloat();
            float[] qspan = new float[3];
            for (int i = 0; i < 3; i++) qspan[i] = qmax[i] - qmin[i];

            int[] cornerVert = new int[cornerCount];
            float[] cornerU = new float[cornerCount];
            float[] cornerV = new float[cornerCount];
            for (int i = 0; i < cornerCount; i++) {
                cornerVert[i] = in.readUnsignedShort();
                cornerU[i] = in.readFloat();
                cornerV[i] = in.readFloat();
            }

            short[] positions = new short[frames * vertCount * 3];
            byte[] normals = new byte[frames * vertCount * 3];
            for (int f = 0; f < frames; f++) {
                int base = f * vertCount * 3;
                for (int v = 0; v < vertCount; v++) {
                    int p = base + v * 3;
                    positions[p] = in.readShort();
                    positions[p + 1] = in.readShort();
                    positions[p + 2] = in.readShort();
                    normals[p] = in.readByte();
                    normals[p + 1] = in.readByte();
                    normals[p + 2] = in.readByte();
                }
            }

            return new BakedMesh(frames, vertCount, qmin, qspan,
                    cornerVert, cornerU, cornerV, positions, normals);
        }
    }

    /**
     * Mistura duas poses e cospe os triangulos.
     *
     * @param time     onde estamos na animacao, em quadros (com casa decimal — a parte
     *                 fracionaria e a mistura entre a pose atual e a proxima)
     * @param alpha    0-255; e por aqui que a criatura se desfaz em vez de sumir de um golpe
     */
    public void draw(PoseStack.Pose pose, VertexConsumer vc, int light, float time,
                     int red, int green, int blue, int alpha) {
        blend(time);

        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        for (int t = 0; t < this.cornerVert.length; t += 3) {
            // O RenderType de entidade desenha QUADRILATEROS, e malha e feita de
            // triangulos. O terceiro canto vai duas vezes: o quarto vertice cai em cima
            // do terceiro e o quad nasce com area zero de um lado — desenha o triangulo
            // exato, sem precisar de um RenderType proprio so para mudar o modo.
            corner(vc, matrix, normal, light, t, red, green, blue, alpha);
            corner(vc, matrix, normal, light, t + 1, red, green, blue, alpha);
            corner(vc, matrix, normal, light, t + 2, red, green, blue, alpha);
            corner(vc, matrix, normal, light, t + 2, red, green, blue, alpha);
        }
    }

    /**
     * Monta a pose do instante, uma vez por desenho.
     *
     * Feito aqui e nao dentro do laco dos cantos de proposito: cada vertice e usado por
     * varios triangulos, entao misturar no canto refaria a mesma conta seis ou sete vezes
     * por ponto.
     */
    private void blend(float time) {
        int a = Math.floorMod((int) Math.floor(time), this.frames);
        int b = (a + 1) % this.frames;
        float t = time - (float) Math.floor(time);

        int baseA = a * this.vertCount * 3;
        int baseB = b * this.vertCount * 3;

        for (int i = 0; i < this.vertCount * 3; i++) {
            int axis = i % 3;
            float pa = this.qmin[axis] + (this.positions[baseA + i] & 0xFFFF) / 65535.0F * this.qspan[axis];
            float pb = this.qmin[axis] + (this.positions[baseB + i] & 0xFFFF) / 65535.0F * this.qspan[axis];
            this.blendPos[i] = pa + (pb - pa) * t;

            float na = this.normals[baseA + i] / 127.0F;
            float nb = this.normals[baseB + i] / 127.0F;
            this.blendNrm[i] = na + (nb - na) * t;
        }
    }

    private void corner(VertexConsumer vc, Matrix4f matrix, Matrix3f normal, int light,
                        int c, int red, int green, int blue, int alpha) {
        int v = this.cornerVert[c] * 3;
        vc.vertex(matrix, this.blendPos[v], this.blendPos[v + 1], this.blendPos[v + 2])
                .color(red, green, blue, alpha)
                .uv(this.cornerU[c], this.cornerV[c])
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, this.blendNrm[v], this.blendNrm[v + 1], this.blendNrm[v + 2])
                .endVertex();
        }
}
