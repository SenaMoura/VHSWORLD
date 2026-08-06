package net.vhsworld.rec.client.entity.mesh;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * UM CORPO SO, DOBRADO POR OSSOS — o skinning que o Minecraft nao tem.
 *
 * <h3>Por que isto existe, tendo PartedMesh</h3>
 * O {@link PartedMesh} desenha uma peca rigida por osso. Foi o primeiro passo e morreu no
 * teste do Pedro: no jogo <b>da para contar os tubos</b> — o pescoco nao encosta no peito, o
 * ombro abre uma fenda ao girar. Junta rigida so se disfarca com sobreposicao, e sobreposicao
 * nao produz um corpo: produz um monte de pedacos que se atravessam.
 *
 * Corpo continuo exige DEFORMACAO, e deformacao exige peso por vertice. E o que este arquivo
 * faz, e e o mesmo salto que toda criatura organica deste mod vai precisar dar — por isso o
 * formato nasce generico, e nao especifico do Escutador.
 *
 * <h3>A conta</h3>
 * Para cada osso: <b>matriz de agora × inversa da matriz de repouso</b>. O vertice esta
 * guardado em espaco de MODELO na pose de repouso, entao a inversa o leva ao espaco do osso e
 * a matriz atual o traz de volta ja dobrado. O vertice final e a soma ponderada de ate quatro
 * ossos.
 *
 * ⚠️ ESQUECER A INVERSA E O ERRO CLASSICO — a criatura explode para longe da origem, e a
 * tentacao e mexer no rig, que estava certo. A mesma conta roda em
 * tools/preview_listener_smesh.py, fora do jogo: se sair torto la, sai torto aqui, e
 * descobrir custa dez segundos em vez de um ciclo de build.
 *
 * ⚠️ AS MATRIZES DE BIND SAO CALCULADAS DO PROPRIO ModelPart (pelo `getInitialPose`), e nao
 * lidas do arquivo. O Blender e o Java teriam que concordar sobre a pose de repouso, e duas
 * fontes para a mesma verdade e como se produz criatura torta que ninguem explica. Aqui so
 * existe uma: o rig.
 *
 * <h3>O nulo e resposta valida</h3>
 * Sem o arquivo, quem chama volta a desenhar as caixas. Mesma doutrina do MeshLibrary: melhor
 * uma criatura de aparencia velha do que jogo quebrado.
 */
public final class SkinnedMesh {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final byte[] MAGIC = {'R', 'E', 'C', 'S'};

    /** O ModelPart trabalha em pixels; o mundo, em blocos. */
    private static final float PIXEL = 1.0f / 16.0f;

    private final String[] bones;

    private final float[] pos;      // x,y,z por vertice (pose de repouso)
    private final float[] normal;
    private final float[] uv;

    /**
     * ⚠️ A COR MORA NO VERTICE, e nao numa textura — decisao do pipeline, ver
     * tools/sculpt_listener.py. Com low poly facetado, um atlas so acrescentaria borrao e
     * uma segunda coisa para desalinhar (o UV e gerado a cada rebuild). A textura do
     * renderer vira um branco chapado que serve so de multiplicador.
     */
    private final byte[] color;     // r,g,b por vertice

    private final int[] boneIndex;  // 4 por vertice
    private final float[] weight;   // 4 por vertice
    private final int[] tris;

    /** Rascunho da pose montada, reaproveitado — desenhar nao deveria criar lixo. */
    private final float[] skinPos;
    private final float[] skinNormal;

    private SkinnedMesh(String[] bones, float[] pos, float[] normal, float[] uv, byte[] color,
                        int[] boneIndex, float[] weight, int[] tris) {
        this.bones = bones;
        this.pos = pos;
        this.normal = normal;
        this.uv = uv;
        this.color = color;
        this.boneIndex = boneIndex;
        this.weight = weight;
        this.tris = tris;
        this.skinPos = new float[pos.length];
        this.skinNormal = new float[normal.length];
    }

    public String[] bones() {
        return this.bones;
    }

    public int triangles() {
        return this.tris.length / 3;
    }

    /**
     * Dobra a malha e desenha.
     *
     * @param skin uma matriz por osso, na ordem de {@link #bones()}, ja no formato
     *             "agora × repouso⁻¹". Quem monta isso e o modelo, porque so ele conhece o rig.
     */
    public void draw(PoseStack.Pose pose, VertexConsumer vc, Matrix4f[] skin, int light,
                     int overlay, float red, float green, float blue, float alpha) {

        deform(skin);

        Matrix4f matrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        int r = (int) (red * 255.0f);
        int g = (int) (green * 255.0f);
        int b = (int) (blue * 255.0f);
        int a = (int) (alpha * 255.0f);

        // ⚠️ O RenderType de entidade desenha QUADRILATEROS e malha e feita de triangulos,
        // entao o terceiro canto vai duas vezes: o quarto vertice cai em cima do terceiro e o
        // quad nasce com area zero de um lado. Mesmo truque do BakedMesh.
        for (int t = 0; t < this.tris.length; t += 3) {
            vertex(vc, matrix, normalMatrix, this.tris[t], light, overlay, r, g, b, a);
            vertex(vc, matrix, normalMatrix, this.tris[t + 1], light, overlay, r, g, b, a);
            vertex(vc, matrix, normalMatrix, this.tris[t + 2], light, overlay, r, g, b, a);
            vertex(vc, matrix, normalMatrix, this.tris[t + 2], light, overlay, r, g, b, a);
        }
    }

    /**
     * Monta a pose do instante, uma vez por desenho.
     *
     * Feito aqui e nao dentro do laco dos triangulos de proposito: cada vertice e usado por
     * varios triangulos, entao deformar no canto refaria a mesma conta seis ou sete vezes por
     * ponto — e esta conta e quatro multiplicacoes de matriz.
     */
    private void deform(Matrix4f[] skin) {
        int count = this.pos.length / 3;

        for (int v = 0; v < count; v++) {
            int p = v * 3;
            int w = v * 4;

            float px = 0.0f, py = 0.0f, pz = 0.0f;
            float nx = 0.0f, ny = 0.0f, nz = 0.0f;
            float total = 0.0f;

            for (int k = 0; k < 4; k++) {
                float influence = this.weight[w + k];
                if (influence <= 0.0f) continue;

                int bone = this.boneIndex[w + k];
                if (bone < 0 || bone >= skin.length) continue;
                Matrix4f m = skin[bone];
                if (m == null) continue;

                float x = this.pos[p], y = this.pos[p + 1], z = this.pos[p + 2];
                px += (m.m00() * x + m.m10() * y + m.m20() * z + m.m30()) * influence;
                py += (m.m01() * x + m.m11() * y + m.m21() * z + m.m31()) * influence;
                pz += (m.m02() * x + m.m12() * y + m.m22() * z + m.m32()) * influence;

                float ax = this.normal[p], ay = this.normal[p + 1], az = this.normal[p + 2];
                nx += (m.m00() * ax + m.m10() * ay + m.m20() * az) * influence;
                ny += (m.m01() * ax + m.m11() * ay + m.m21() * az) * influence;
                nz += (m.m02() * ax + m.m12() * ay + m.m22() * az) * influence;

                total += influence;
            }

            // Vertice sem peso nenhum fica onde estava. Acontece quando um detalhe entra na
            // malha depois do bind e alguem esquece de amarra-lo — e melhor ele ficar parado
            // no lugar certo do que ir para a origem do mundo.
            if (total <= 0.0f) {
                this.skinPos[p] = this.pos[p];
                this.skinPos[p + 1] = this.pos[p + 1];
                this.skinPos[p + 2] = this.pos[p + 2];
                this.skinNormal[p] = this.normal[p];
                this.skinNormal[p + 1] = this.normal[p + 1];
                this.skinNormal[p + 2] = this.normal[p + 2];
                continue;
            }

            this.skinPos[p] = px;
            this.skinPos[p + 1] = py;
            this.skinPos[p + 2] = pz;

            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length > 1.0e-5f) {
                this.skinNormal[p] = nx / length;
                this.skinNormal[p + 1] = ny / length;
                this.skinNormal[p + 2] = nz / length;
            }
        }
    }

    private void vertex(VertexConsumer vc, Matrix4f matrix, Matrix3f normalMatrix, int index,
                        int light, int overlay, int r, int g, int b, int a) {
        int p = index * 3;
        int t = index * 2;

        // A cor do vertice MULTIPLICA a cor pedida por quem desenha — assim o tinting do
        // renderer (dano piscando, fade) continua funcionando por cima da pintura.
        int cr = (this.color[p] & 0xFF) * r / 255;
        int cg = (this.color[p + 1] & 0xFF) * g / 255;
        int cb = (this.color[p + 2] & 0xFF) * b / 255;

        vc.vertex(matrix,
                        this.skinPos[p] * PIXEL,
                        this.skinPos[p + 1] * PIXEL,
                        this.skinPos[p + 2] * PIXEL)
                .color(cr, cg, cb, a)
                .uv(this.uv[t], this.uv[t + 1])
                .overlayCoords(overlay)
                .uv2(light)
                .normal(normalMatrix, this.skinNormal[p], this.skinNormal[p + 1], this.skinNormal[p + 2])
                .endVertex();
    }

    // ------------------------------------------------------------------ leitura

    public static SkinnedMesh read(InputStream stream) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(stream))) {
            byte[] magic = new byte[4];
            in.readFully(magic);
            for (int i = 0; i < 4; i++) {
                if (magic[i] != MAGIC[i]) throw new IOException("nao e um .smesh do recmod");
            }

            int version = in.readUnsignedByte();
            if (version != 1) throw new IOException("versao de .smesh desconhecida: " + version);

            int boneCount = in.readUnsignedShort();
            String[] bones = new String[boneCount];
            for (int i = 0; i < boneCount; i++) {
                int length = in.readUnsignedByte();
                byte[] raw = new byte[length];
                in.readFully(raw);
                bones[i] = new String(raw, StandardCharsets.UTF_8);
            }

            int vertCount = in.readInt();
            float[] pos = new float[vertCount * 3];
            float[] normal = new float[vertCount * 3];
            float[] uv = new float[vertCount * 2];
            byte[] color = new byte[vertCount * 3];
            int[] boneIndex = new int[vertCount * 4];
            float[] weight = new float[vertCount * 4];

            for (int v = 0; v < vertCount; v++) {
                pos[v * 3] = in.readFloat();
                pos[v * 3 + 1] = in.readFloat();
                pos[v * 3 + 2] = in.readFloat();
                normal[v * 3] = in.readByte() / 127.0f;
                normal[v * 3 + 1] = in.readByte() / 127.0f;
                normal[v * 3 + 2] = in.readByte() / 127.0f;
                uv[v * 2] = in.readFloat();
                uv[v * 2 + 1] = in.readFloat();
                color[v * 3] = in.readByte();
                color[v * 3 + 1] = in.readByte();
                color[v * 3 + 2] = in.readByte();
                for (int k = 0; k < 4; k++) {
                    boneIndex[v * 4 + k] = in.readUnsignedShort();
                    weight[v * 4 + k] = in.readFloat();
                }
            }

            int triCount = in.readInt();
            int[] tris = new int[triCount * 3];
            for (int i = 0; i < triCount * 3; i++) {
                tris[i] = in.readInt();
            }

            return new SkinnedMesh(bones, pos, normal, uv, color, boneIndex, weight, tris);
        }
    }

    // ------------------------------------------------------------------ cache

    private static final Map<ResourceLocation, SkinnedMesh> CACHE = new HashMap<>();
    private static final Map<ResourceLocation, Boolean> FAILED = new HashMap<>();

    public static SkinnedMesh get(ResourceLocation path) {
        SkinnedMesh cached = CACHE.get(path);
        if (cached != null) return cached;
        if (FAILED.containsKey(path)) return null;

        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(path);
            if (res.isEmpty()) {
                LOGGER.warn("[malha] nao achei {} — a criatura volta para as caixas", path);
                FAILED.put(path, true);
                return null;
            }
            try (InputStream in = res.get().open()) {
                SkinnedMesh mesh = SkinnedMesh.read(in);
                CACHE.put(path, mesh);
                LOGGER.info("[malha] {} carregada: {} triangulos, {} ossos",
                        path, mesh.triangles(), mesh.bones.length);
                return mesh;
            }
        } catch (Exception e) {
            LOGGER.error("[malha] falhei ao ler {}: {}", path, e.toString());
            FAILED.put(path, true);
            return null;
        }
    }

    public static void reset() {
        CACHE.clear();
        FAILED.clear();
    }
}
