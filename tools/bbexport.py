"""Leitura dos exports "modded_entity" do Blockbench.

Um lugar so para os defeitos conhecidos do export, porque eles se repetem em TODA
leva nova de modelo (a lista saiu da primeira leva, a do Homem de Pedra):

1. nome de parte COM ESPACO ("Right Arm") -> o .java exportado nao compila;
2. namespace placeholder "modid:" -> tem que virar "recmod:";
3. classe/layer com nome de outra entidade (o corrupted_4 saiu como corrupted_3);
4. nome de layer com espaco e traco ("model_- converted") -> ResourceLocation invalido.

Aqui a gente le a GEOMETRIA e nada mais: os nomes viram snake_case e o texto do
bloco e copiado literalmente, sem redigitar numero a mao.
"""
import math
import re

NUM = r"(-?[\d.]+)F"


def snake(name):
    return name.strip().replace(" ", "_").lower()


def camel(name):
    bits = snake(name).split("_")
    return "".join(w.capitalize() if i else w for i, w in enumerate(bits))


def _unique(taken, want):
    """Primeiro que sobrar de want, want_v2, want_v3..."""
    if want not in taken:
        taken.add(want)
        return want
    n = 2
    while "%s_v%d" % (want, n) in taken:
        n += 1
    out = "%s_v%d" % (want, n)
    taken.add(out)
    return out


def name_map(block, report=None):
    """Nome do Blockbench -> (nome da parte, nome da variavel), sem colisao.

    ⚠️ Defeito 5, achado no Crawler_Void: o export trouxe "Arms_3" E "Arms3". Os dois
    viram a MESMA variavel Java (arms3), e o `addOrReplaceChild` com o mesmo nome
    SUBSTITUI em silencio — o bicho perde membros e ninguem ve erro nenhum. Foi o que
    fez a medicao acusar 9 blocos de largura num bicho de 3,7: metade das pernas
    aterrissou no grupo errado. Por isso a desambiguacao e obrigatoria aqui.
    """
    names = re.findall(r'addOrReplaceChild\("([^"]+)"', block)
    seen = []
    for n in names:                       # ordem do arquivo, sem repetir
        if n not in seen:
            seen.append(n)

    parts_taken, vars_taken = set(), set()
    mapping = {}
    for n in seen:
        p = _unique(parts_taken, snake(n))
        v = _unique(vars_taken, camel(n))
        mapping[n] = (p, v)
        if report is not None and (p != snake(n) or v != camel(n)):
            report.append("%s -> parte %s / var %s (colisao desfeita)" % (n, p, v))
    return mapping


def geometry_block(path, report=None):
    """O trecho entre a MeshDefinition e o return, ja com os nomes normalizados."""
    src = open(path, encoding="utf-8").read()
    block = src[src.index("MeshDefinition"):src.index("return LayerDefinition")]

    mapping = name_map(block, report)

    # Do mais longo para o mais curto, senao "Arm" comeria "Right Arm". E o alvo e
    # sempre o nome ORIGINAL (com maiuscula), que nunca colide com o ja substituido
    # (minusculo) — e o que impede uma troca de desfazer a anterior.
    for old in sorted(mapping, key=len, reverse=True):
        part, var = mapping[old]
        block = block.replace('"%s"' % old, '"%s"' % part)
        block = re.sub(r"\bPartDefinition %s\s*=" % re.escape(old),
                       "PartDefinition %s =" % var, block)
        block = re.sub(r'(?<![\w."])%s\.addOrReplaceChild' % re.escape(old),
                       "%s.addOrReplaceChild" % var, block)

    block = block.replace("partdefinition.addOrReplaceChild", "root.addOrReplaceChild")
    block = block.replace("MeshDefinition meshdefinition = new MeshDefinition();",
                          "MeshDefinition mesh = new MeshDefinition();")
    block = block.replace("PartDefinition partdefinition = meshdefinition.getRoot();",
                          "PartDefinition root = mesh.getRoot();")
    return block


def texture_size(path):
    src = open(path, encoding="utf-8").read()
    m = re.search(r"LayerDefinition\.create\(meshdefinition,\s*(\d+),\s*(\d+)\)", src)
    return (int(m.group(1)), int(m.group(2))) if m else (64, 64)


def parts(path, report=None):
    """Arvore de partes: {var: {parent, name, off, rot, boxes, children}}."""
    block = geometry_block(path, report)
    decls = list(re.finditer(
        r"PartDefinition (\w+) = (\w+)\.addOrReplaceChild\(\"([^\"]+)\",", block))

    out = {}
    order = []
    for i, m in enumerate(decls):
        var, parent, name = m.group(1), m.group(2), m.group(3)
        end = decls[i + 1].start() if i + 1 < len(decls) else len(block)
        body = block[m.start():end]

        pose = re.search(r"PartPose\.offsetAndRotation\(" + r",\s*".join([NUM] * 6), body)
        if pose:
            off = [float(pose.group(k)) for k in (1, 2, 3)]
            rot = [float(pose.group(k)) for k in (4, 5, 6)]
        else:
            pose = re.search(r"PartPose\.offset\(" + r",\s*".join([NUM] * 3), body)
            off = [float(pose.group(k)) for k in (1, 2, 3)] if pose else [0.0, 0.0, 0.0]
            rot = [0.0, 0.0, 0.0]

        boxes = []
        # sem exigir ")" no fim: depois do sexto numero vem ", new CubeDeformation(...)"
        for b in re.finditer(r"addBox\(" + r",\s*".join([NUM] * 6), body):
            v = [float(b.group(k)) for k in range(1, 7)]
            boxes.append((v[:3], [v[0] + v[3], v[1] + v[4], v[2] + v[5]]))

        out[var] = dict(parent=parent, name=name, off=off, rot=rot,
                        boxes=boxes, children=[])
        order.append(var)

    for var in order:
        p = out[var]["parent"]
        if p in out:
            out[p]["children"].append(var)
    out["__roots__"] = [v for v in order if out[v]["parent"] == "root"]
    out["__order__"] = order
    return out


# ---------------------------------------------------------------- geometria


def _mat_rot(rx, ry, rz):
    """A rotacao do ModelPart: Z, depois Y, depois X na pilha -> X, Y, Z no ponto."""
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    mx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    my = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    mz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return _mul(mz, _mul(my, mx))


def _mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def _apply(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


def corners(path, report=None):
    """Todos os cantos de todos os cubos, no espaco da entidade (Y cresce para BAIXO).

    Dois cuidados, os dois ja tendo custado uma medicao errada:

    ⚠️ A transformada e encadeada por MATRIZ, nao somando os angulos de Euler dos
    ancestrais: o offset de um filho vale no referencial JA GIRADO do pai.

    ⚠️ O angulo do .java JA ESTA EM RADIANOS. Passar por math.radians de novo nao
    da erro nenhum — so encolhe todo giro por 57x, e o modelo aparece com os membros
    esticados para longe, como se o export estivesse quebrado. Foi o que fez o
    Crawler_Void "medir" 11,5 blocos de largura quando ele tem 3,7.
    """
    tree = parts(path, report)
    pts = []

    def walk(var, mat, org):
        p = tree[var]
        org = [org[i] + _apply(mat, p["off"])[i] for i in range(3)]
        mat = _mul(mat, _mat_rot(*p["rot"]))

        for f, t in p["boxes"]:
            for i in range(8):
                c = [f[0] if i & 1 else t[0],
                     f[1] if i & 2 else t[1],
                     f[2] if i & 4 else t[2]]
                c = _apply(mat, c)
                pts.append([c[k] + org[k] for k in range(3)])

        for child in p["children"]:
            walk(child, mat, org)

    ident = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
    for var in tree["__roots__"]:
        walk(var, ident, [0.0, 0.0, 0.0])
    return pts


def bounds(path, report=None):
    pts = corners(path, report)
    mn = [min(p[i] for p in pts) for i in range(3)]
    mx = [max(p[i] for p in pts) for i in range(3)]
    return mn, mx
