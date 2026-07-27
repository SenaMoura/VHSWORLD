"""Converte os 3 exports do Blockbench numa classe de modelo valida.

O export vem com nomes de parte COM ESPACO ("Right Arm"), o que nao compila em
Java, e com o namespace placeholder "modid". Aqui os nomes viram snake_case e a
geometria e copiada literalmente — nada de redigitar numero a mao.
"""
import re, os

SRC = r"C:\Users\Hamilton\Downloads\vhsworldentities\Stoneman"
OUT = r"C:\Users\Hamilton\Downloads\GitHub\VHSWORLD\src\main\java\net\vhsworld\rec\client\entity\StonemanModel.java"

def snake(name):
    return name.strip().replace(" ", "_").lower()


def camel(name):
    bits = snake(name).split("_")
    return "".join(w.capitalize() if i else w for i, w in enumerate(bits))


def body_of(path):
    s = open(path, encoding="utf-8").read()
    start = s.index("MeshDefinition meshdefinition")
    end = s.index("return LayerDefinition.create")
    block = s[start:end]

    # Descobre os nomes NO ARQUIVO em vez de manter uma lista a mao — foi assim que
    # "Right Arm_r1" (so existe na variante 2) escapou na primeira tentativa.
    names = sorted(set(re.findall(r'addOrReplaceChild\("([^"]+)"', block)),
                    key=len, reverse=True)
    for old in names:
        block = block.replace(f'"{old}"', f'"{snake(old)}"')
        block = re.sub(rf'\bPartDefinition {re.escape(old)}\s*=', f'PartDefinition {camel(old)} =', block)
        block = re.sub(rf'(?<![\w."]){re.escape(old)}\.addOrReplaceChild', f'{camel(old)}.addOrReplaceChild', block)
    block = block.replace("partdefinition.addOrReplaceChild", "root.addOrReplaceChild")
    block = block.replace("MeshDefinition meshdefinition = new MeshDefinition();",
                          "MeshDefinition mesh = new MeshDefinition();")
    block = block.replace("PartDefinition partdefinition = meshdefinition.getRoot();",
                          "PartDefinition root = mesh.getRoot();")
    lines = [("        " + l.strip()) if l.strip() else "" for l in block.strip().split("\n")]
    return "\n".join(lines)


HEAD = '''package net.vhsworld.rec.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.entity.StonemanEntity;

/**
 * O modelo do Homem de Pedra.
 *
 * A geometria e a que o Pedro modelou no Blockbench, transcrita por script a partir
 * dos tres exports (tools/… no scratchpad) — nao foi redigitada. Duas coisas o
 * export nao entrega prontas: os nomes das partes vinham COM ESPACO ("Right Arm"),
 * que nao compila em Java, e o namespace era o placeholder "modid".
 *
 * Ele teve tres variantes de geometria ate a v1.38.0. Foram removidas: um monstro
 * com tres corpos diferentes vira "qual delas e essa?" na cabeca do jogador, e a
 * duvida que interessa aqui e outra — se ele se mexeu ou nao.
 *
 * A animacao e escrita aqui porque o formato modded_entity nao guarda animacao —
 * o setupAnim do export vem vazio.
 */
public class StonemanModel extends EntityModel<StonemanEntity> {

    public static final ModelLayerLocation BASE =
            new ModelLayerLocation(new ResourceLocation(RECMod.MOD_ID, "stoneman"), "main");

    private final ModelPart waist;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public StonemanModel(ModelPart root) {
        this.waist = root.getChild("waist");
        this.head = this.waist.getChild("head");
        this.body = this.waist.getChild("body");
        this.rightArm = this.waist.getChild("right_arm");
        this.leftArm = this.waist.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }
'''

TAIL = '''
    /**
     * Ele so se mexe quando ninguem esta olhando.
     *
     * Parado, e pedra: membro nenhum se move, nem um tremor. A imobilidade TOTAL e o
     * efeito — qualquer balancinho de respiracao entregaria que e um bicho vivo e
     * mataria o susto de ver que ele mudou de lugar.
     *
     * A cabeca continua apontada para onde estava: o servidor congela a rotacao, entao
     * os angulos que chegam aqui simplesmente param de mudar. Ele fica encarando.
     */
    @Override
    public void setupAnim(StonemanEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        if (entity.isWatched()) {
            this.rightArm.xRot = 0.0F;
            this.leftArm.xRot = 0.0F;
            this.rightLeg.xRot = 0.0F;
            this.leftLeg.xRot = 0.0F;
            this.waist.zRot = 0.0F;
            return;
        }

        // Passada pesada: pernas e bracos em contrafase, e a cintura jogando de um
        // lado para o outro no dobro da frequencia — peso de pedra, nao de gente.
        float swing = limbSwing * 0.6662F;
        float amount = Math.min(limbSwingAmount, 1.0F);

        this.rightLeg.xRot = Mth.cos(swing) * 1.15F * amount;
        this.leftLeg.xRot = Mth.cos(swing + (float) Math.PI) * 1.15F * amount;
        this.rightArm.xRot = Mth.cos(swing + (float) Math.PI) * 0.9F * amount;
        this.leftArm.xRot = Mth.cos(swing) * 0.9F * amount;
        this.waist.zRot = Mth.cos(swing * 2.0F) * 0.06F * amount;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               float r, float g, float b, float a) {
        this.waist.render(pose, buffer, light, overlay, r, g, b, a);
        this.rightLeg.render(pose, buffer, light, overlay, r, g, b, a);
        this.leftLeg.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
'''

parts = [HEAD]
# So a geometria base. As variantes sairam na v1.38.0; para trazer de volta, basta
# reacrescentar ("Stoneman_variant1", "createVariant1") aqui e a ModelLayerLocation
# no HEAD — os .java exportados continuam em Downloads/vhsworldentities.
for name, method in (("Stoneman", "createBase"),):
    geom = body_of(os.path.join(SRC, name + ".java"))
    parts.append(f"\n    /** Geometria de {name}.bbmodel. */\n"
                 f"    public static LayerDefinition {method}() {{\n{geom}\n"
                 f"        return LayerDefinition.create(mesh, 64, 64);\n    }}\n")
parts.append(TAIL)

os.makedirs(os.path.dirname(OUT), exist_ok=True)
open(OUT, "w", encoding="utf-8").write("".join(parts))
print("escrito:", OUT)
for l in open(OUT, encoding="utf-8").read().split("\n"):
    if " " in l and re.search(r'PartDefinition [A-Za-z]+ [A-Za-z]', l):
        print("  !! nome com espaco sobrou:", l.strip())
