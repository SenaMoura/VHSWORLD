# -*- coding: utf-8 -*-
"""Reestrutura o rig dos .bbmodel de Downloads\\vhsworldentities.

O que ele faz:
  1. HIERARQUIA  - desfaz o aninhamento em que a cabeca e o pai do corpo, cria um
                   osso raiz `root` e pendura os membros em CADEIA articulada
                   (arm_left_1 -> arm_left_2 -> arm_left_3 ...).
  2. NOMES       - todo osso e todo cubo ganham nome descritivo em ingles;
                   cubo generico ("cube") vira cube_<osso>_<n>.
  3. DADOS       - tamanho, UV, inflate e PIVO de cada peca ficam intactos.

A trava: `verify()` recalcula os 8 cantos de cada cubo no espaco do mundo,
aplicando a cadeia inteira de rotacoes dos pais, ANTES e DEPOIS. Se um so canto
andar mais de 1e-6, o arquivo nao e gravado. Osso novo nasce sempre com
rotacao [0,0,0], que e o unico jeito de reparentar sem mexer na pose.

Uso:
    python tools/restructure_bbmodels.py --dry-run   # so mostra a arvore nova
    python tools/restructure_bbmodels.py             # grava (faz backup antes)
"""

import argparse
import json
import math
import os
import re
import shutil
import unicodedata
import uuid as uuidlib

ROOT = r"C:\Users\Hamilton\Downloads\vhsworldentities"
BACKUP = ROOT + "_BACKUP_rig"
EPS = 1e-6

# ---------------------------------------------------------------- utilidades


def slug(s):
    s = unicodedata.normalize("NFKD", s or "").encode("ascii", "ignore").decode()
    s = re.sub(r"[^A-Za-z0-9]+", "_", s).strip("_").lower()
    return re.sub(r"_+", "_", s)


GENERIC_CUBE = {"", "cube", "mirrored", "bone", "group"}

# nome original do cubo -> sufixo curto (quando o nome so repete o papel do osso)
CUBE_SUFFIX = {
    "head": "base", "body": "base", "torso": "base", "cube": None, "mirrored": None,
    "hat_layer": "hat", "body_layer": "layer",
    "right_arm": "base", "left_arm": "base", "right_leg": "base", "left_leg": "base",
    "right_arm_layer": "layer", "left_arm_layer": "layer",
    "right_leg_layer": "layer", "left_leg_layer": "layer",
    "leg1": "base", "leg2": "base", "leg3": "base", "leg4": "base",
    "nose": "nose", "bodywear": "wear", "headwear": "wear",
}


class Cube(object):
    def __init__(self, el):
        self.el = el

    @property
    def uuid(self):
        return self.el["uuid"]

    @property
    def origin(self):
        return self.el.get("origin") or [0, 0, 0]

    @property
    def rotation(self):
        return self.el.get("rotation") or [0, 0, 0]

    def center(self):
        f, t = self.el["from"], self.el["to"]
        return [(f[i] + t[i]) / 2.0 for i in range(3)]


class Bone(object):
    def __init__(self, name, origin, rotation=None, src=None):
        self.name = name
        self.origin = list(origin)
        self.rotation = list(rotation or [0, 0, 0])
        self.src = src          # dict original em doc['groups'] (None = osso novo)
        self.children = []

    def cubes(self):
        out = []
        for c in self.children:
            out.extend([c] if isinstance(c, Cube) else c.cubes())
        return out

    def own_cubes(self):
        return [c for c in self.children if isinstance(c, Cube)]

    def bones(self):
        return [c for c in self.children if isinstance(c, Bone)]

    def empty(self):
        return not self.cubes()

    def __repr__(self):
        return "<Bone %s>" % self.name


# ---------------------------------------------------------------- ler/gravar


def parse(doc):
    groups = {g["uuid"]: g for g in doc.get("groups", [])}
    elems = {e["uuid"]: e for e in doc.get("elements", [])}

    def build(nodes):
        out = []
        for n in nodes:
            if isinstance(n, str):
                if n in elems:
                    out.append(Cube(elems[n]))
                continue
            g = groups.get(n["uuid"], {})
            b = Bone(g.get("name", "group"), g.get("origin") or [0, 0, 0],
                     g.get("rotation") or [0, 0, 0], src=g)
            b.children = build(n.get("children", []))
            out.append(b)
        return out

    return build(doc.get("outliner", []))


GROUP_TEMPLATE = {
    "export": True, "locked": False, "scope": 0, "selected": False,
    "color": 0, "children": [], "reset": False, "shade": True,
    "mirror_uv": False, "visibility": True, "autouv": 0, "isOpen": True,
    "primary_selected": False,
}


def serialize(doc, roots):
    groups = []

    def emit(node):
        if isinstance(node, Cube):
            return node.uuid
        g = node.src
        if g is None:
            g = dict(GROUP_TEMPLATE)
            g["uuid"] = str(uuidlib.uuid4())
            g["_static"] = {"properties": {}, "temp_data": {}}
        g["name"] = node.name
        g["origin"] = node.origin
        g["rotation"] = node.rotation
        g["children"] = []
        groups.append(g)
        return {"uuid": g["uuid"], "isOpen": True,
                "children": [emit(c) for c in node.children]}

    outliner = [emit(r) for r in roots]
    doc["groups"] = groups
    doc["outliner"] = outliner


# ---------------------------------------------------------------- verificacao


def rot_matrix(deg):
    rx, ry, rz = [math.radians(a) for a in deg]
    cx, sx, cy, sy, cz, sz = (math.cos(rx), math.sin(rx), math.cos(ry),
                              math.sin(ry), math.cos(rz), math.sin(rz))
    mx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    my = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    mz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return mul(mz, mul(my, mx))


def mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def apply(m, p):
    return [sum(m[i][k] * p[k] for k in range(3)) for i in range(3)]


def corners(cube, chain):
    """8 cantos do cubo no mundo. chain = [(origin, rotation)] da folha p/ a raiz."""
    f, t = cube.el["from"], cube.el["to"]
    pts = [[x, y, z] for x in (f[0], t[0]) for y in (f[1], t[1]) for z in (f[2], t[2])]
    steps = [(cube.origin, cube.rotation)] + list(chain)
    for origin, rot in steps:
        if rot == [0, 0, 0]:
            continue
        m = rot_matrix(rot)
        pts = [[apply(m, [p[i] - origin[i] for i in range(3)])[k] + origin[k]
                for k in range(3)] for p in pts]
    return pts


def snapshot(roots):
    """uuid do cubo -> (cantos, from, to, uv_offset, faces, inflate)."""
    out = {}

    def walk(nodes, chain):
        for n in nodes:
            if isinstance(n, Cube):
                out[n.uuid] = (
                    corners(n, chain),
                    n.el.get("from"), n.el.get("to"), n.el.get("uv_offset"),
                    json.dumps(n.el.get("faces"), sort_keys=True),
                    n.el.get("inflate", 0), n.el.get("mirror_uv", False),
                )
            else:
                walk(n.children, [(n.origin, n.rotation)] + chain)

    walk(roots, [])
    return out


def verify(before, after):
    problems = []
    if set(before) != set(after):
        problems.append("conjunto de cubos mudou (%d -> %d)" % (len(before), len(after)))
        return problems
    for k in before:
        b, a = before[k], after[k]
        for i in range(1, 7):
            if b[i] != a[i]:
                problems.append("cubo %s: campo %d alterado" % (k[:8], i))
        worst = max(abs(b[0][p][c] - a[0][p][c]) for p in range(8) for c in range(3))
        if worst > EPS:
            problems.append("cubo %s andou %.6g" % (k[:8], worst))
    return problems


# ------------------------------------------------------------ transformacoes


def side_of(node):
    """+X = direita nestes modelos (Right Arm tem pivo x=+5)."""
    cubes = node.cubes() if isinstance(node, Bone) else [node]
    if cubes:
        x = sum(c.center()[0] for c in cubes) / float(len(cubes))
    else:
        x = node.origin[0]
    if x > 0.01:
        return "right"
    if x < -0.01:
        return "left"
    return None


TOUCH = 1.0   # folga (em pixels do modelo) para dizer que dois cubos se encostam


def world_aabb(cube):
    pts = corners(cube, [])
    return [[min(p[i] for p in pts) for i in range(3)],
            [max(p[i] for p in pts) for i in range(3)]]


def gap(a, b):
    """Maior folga entre duas caixas (0 = encostam ou se cruzam)."""
    return max(max(a[0][i] - b[1][i], b[0][i] - a[1][i], 0.0) for i in range(3))


def touching(a, b, tol=TOUCH):
    return gap(a, b) <= tol


def union_box(boxes):
    return [[min(b[0][i] for b in boxes) for i in range(3)],
            [max(b[1][i] for b in boxes) for i in range(3)]]


def contact_point(a, b):
    """Centro da regiao onde as duas caixas se tocam = a junta."""
    out = []
    for i in range(3):
        lo, hi = max(a[0][i], b[0][i]), min(a[1][i], b[1][i])
        out.append(round((lo + hi) / 2.0 if lo <= hi else (a[1][i] + b[0][i]) / 2.0
                         if a[1][i] < b[0][i] else (b[1][i] + a[0][i]) / 2.0, 5))
    return out


def limb_pivot(cubes, fallback):
    """Pivo de um membro que nasceu de uma separacao.

    Sem isto, ao partir um osso 'Arms' em dois, um braco fica com o pivo do
    container (o centro do modelo) e o outro com um pivo bom - rig torto.
    Ordem: pivo do container se ele ja cai na peca; senao o pivo que o proprio
    cubo trazia; senao o ponto da peca mais perto de onde o container estava.
    """
    box = union_box([world_aabb(c) for c in cubes])

    def inside(p):
        return all(box[0][k] - 2 <= p[k] <= box[1][k] + 2 for k in range(3))

    if inside(fallback):
        return list(fallback)
    own = cubes[0].origin
    if inside(own):
        return list(own)
    return [round(min(max(fallback[k], box[0][k]), box[1][k]), 5) for k in range(3)]


def split_limbs(cubes):
    """Cubos que se ENCOSTAM sao segmentos do mesmo membro; o resto e outro membro.

    E o que separa as duas pernas do carneiro (mesmo osso, 11 blocos de distancia)
    dos segmentos de um tentaculo, que nascem colados um no outro.
    """
    boxes = [world_aabb(c) for c in cubes]
    parent = list(range(len(cubes)))

    def find(i):
        while parent[i] != i:
            parent[i] = parent[parent[i]]
            i = parent[i]
        return i

    for i in range(len(cubes)):
        for j in range(i + 1, len(cubes)):
            if touching(boxes[i], boxes[j]):
                parent[find(i)] = find(j)
    comps = {}
    for i, c in enumerate(cubes):
        comps.setdefault(find(i), []).append(c)
    return list(comps.values())


def segments_of(cubes):
    """Cubos com a MESMA origem sao um segmento so (camada dupla, detalhe)."""
    segs = []
    for c in cubes:
        key = tuple(round(v, 5) for v in c.origin)
        for s in segs:
            if s["key"] == key:
                s["cubes"].append(c)
                break
        else:
            segs.append({"key": key, "origin": list(c.origin), "cubes": [c]})
    return segs


def dist3(a, b):
    return sum((a[i] - b[i]) ** 2 for i in range(3)) ** 0.5


def chainify(bone, base, report):
    """Monta a cadeia: pivo de cada elo = a origem do proprio cubo (dado original).

    A ordem e por VIZINHO MAIS PROXIMO a partir da raiz do membro, nao por
    distancia ate a raiz: membro que se curva de volta (o tentaculo do porco)
    sai fora de ordem no criterio da distancia.
    """
    subs = bone.bones()
    segs = segments_of(bone.own_cubes())
    bone.name = base + "_1"
    if len(segs) <= 1:
        bone.children = [c for s in segs for c in s["cubes"]] + subs
        return bone

    for s in segs:
        s["boxes"] = [world_aabb(c) for c in s["cubes"]]
        s["center"] = [sum((b[0][k] + b[1][k]) / 2.0 for b in s["boxes"]) / len(s["boxes"])
                       for k in range(3)]

    # Liga cada segmento ao vizinho mais proximo (arvore geradora minima) e so
    # aceita a cadeia se a arvore for um CAMINHO. Se um segmento tem 3 vizinhos,
    # aquilo e um bolo de cubos (o braco do Nightmare) e encadear inventaria
    # juntas de 76 blocos. Nao da pra usar "as caixas se encostam": cubo girado
    # tem caixa alinhada gorda e todos encostam em todos.
    n = len(segs)
    edges, inside = [], {0}
    while len(inside) < n:
        best = min(((i, j) for i in inside for j in range(n) if j not in inside),
                   key=lambda e: dist3(segs[e[0]]["center"], segs[e[1]]["center"]))
        edges.append(best)
        inside.add(best[1])
    deg = {i: [] for i in range(n)}
    for i, j in edges:
        deg[i].append(j)
        deg[j].append(i)
    ends = [i for i in deg if len(deg[i]) == 1]
    if any(len(v) > 2 for v in deg.values()):
        report.append("      %s: %d cubos nao formam cadeia (ramificam) - deixados soltos"
                      % (base, n))
        bone.name = base
        bone.children = bone.own_cubes() + subs
        return bone

    start = min(ends, key=lambda i: dist3(segs[i]["center"], bone.origin))
    order, prev, cur = [start], None, start
    while len(order) < n:
        nxt = [j for j in deg[cur] if j != prev][0]
        prev, cur = cur, nxt
        order.append(cur)
    chain = [segs[i] for i in order]

    # ultima trava: junta de verdade tem os dois segmentos ENCOSTADOS. A MST liga
    # ate nuvens distantes numa linha; isso aqui e o que separa membro de bolo.
    for k in range(1, len(chain)):
        g = min(gap(x, y) for x in chain[k - 1]["boxes"] for y in chain[k]["boxes"])
        if g > 2.0:
            report.append("      %s: %d cubos nao formam cadeia (vao de %.3g entre elos)"
                          " - deixados soltos" % (base, n, g))
            bone.name = base
            bone.children = bone.own_cubes() + subs
            return bone

    bone.children = list(chain[0]["cubes"]) + subs
    tip, moved = bone, 0
    for i, s in enumerate(chain[1:], 2):
        box = union_box(s["boxes"])
        origin = s["origin"]
        if not all(box[0][k] - 2 <= origin[k] <= box[1][k] + 2 for k in range(3)):
            # o pivo que o cubo trazia esta longe da propria peca (o artista girou
            # em torno de um ponto distante). Pivo assim nao articula: a junta e o
            # CONTATO com o elo anterior. So o osso NOVO usa isso; o cubo nao muda.
            origin = contact_point(union_box(chain[i - 2]["boxes"]), box)
            moved += 1
        nb = Bone("%s_%d" % (base, i), origin)
        nb.children = list(s["cubes"])
        tip.children.append(nb)
        tip = nb
    if moved:
        report.append("      %s: %d elo(s) com pivo posto na junta (o do cubo caia fora da peca)"
                      % (base, moved))
    report.append("      cadeia %s: %d elos (passo %s)"
                  % (base, len(chain), " -> ".join(
                      "%.3g" % dist3(chain[k]["origin"], chain[k - 1]["origin"])
                      for k in range(1, len(chain)))))
    return bone


def limbify(bones, kind, parent_children, report):
    """Achata embrulhos, separa membro por membro, da lado/letra e encadeia."""
    # 1) embrulho = osso sem cubo proprio que so guarda outros ossos ("Legs" -> 4 pernas).
    #    ⚠️ embrulho COM rotacao nao pode ser achatado: a rotacao dele e parte da pose
    #    dos filhos (foi o que moveu o villager 17 blocos e a verificacao pegou).
    flat = []
    for b in bones:
        if not b.own_cubes() and b.bones() and b.rotation == [0, 0, 0]:
            report.append("      embrulho '%s' achatado (%d membros)" % (b.name, len(b.bones())))
            flat.extend(b.bones())
        else:
            flat.append(b)

    # 2) um osso pode carregar mais de um membro (dois cubos de perna soltos juntos)
    limbs, unused = [], []
    for b in flat:
        if b.empty():
            unused.append(b)
            continue
        parts = split_limbs(b.own_cubes()) if (b.rotation == [0, 0, 0] and b.own_cubes()) else []
        if len(parts) <= 1:
            limbs.append(b)
            continue
        parts.sort(key=lambda cs: dist3(cs[0].origin, b.origin))
        old = list(b.origin)
        b.children = parts[0] + b.bones()
        b.origin = limb_pivot(parts[0], old)
        limbs.append(b)
        for extra in parts[1:]:
            nb = Bone(b.name, limb_pivot(extra, old))
            nb.children = list(extra)
            limbs.append(nb)
        report.append("      osso '%s' tinha %d membros soltos juntos -> separados "
                      "(pivo de cada um posto na propria peca)" % (b.name, len(parts)))

    # 3) lado, letra (quando ha mais de um membro do mesmo tipo no lado) e cadeia
    by_side = {"right": [], "left": [], None: []}
    for b in limbs:
        by_side[side_of(b)].append(b)
    for side, group in by_side.items():
        group.sort(key=lambda b: (-(b.cubes()[0].center()[1]), b.cubes()[0].center()[2]))
        letters = "abcdefghijklmnop"
        for i, b in enumerate(group):
            tag = kind if side is None else "%s_%s" % (kind, side)
            if len(group) > 1:
                tag = "%s_%s" % (tag, letters[i])
            relimb(b, report)
            box = union_box([world_aabb(c) for c in b.own_cubes()]) if b.own_cubes() else None
            if box and not all(box[0][k] - 2 <= b.origin[k] <= box[1][k] + 2 for k in range(3)):
                report.append("      ~ %s: pivo original %s fica fora da peca (nao mexi; "
                              "vale arrastar p/ a junta no Blockbench)" % (tag, b.origin))
            chainify(b, tag, report)
            parent_children.append(b)
    for b in unused:
        b.name = "unused_" + slug(b.name)
        b.children = []
        parent_children.append(b)


def relimb(bone, report):
    """Aplica a mesma regra aos ossos filhos (tentaculo pendurado no braco)."""
    subs = bone.bones()
    if not subs:
        return
    keep = [c for c in bone.children if isinstance(c, Cube)]
    groups = {}
    for s in subs:
        k = kind_of(s.name)
        if k in LIMB_KINDS:
            groups.setdefault(k, []).append(s)
        else:                       # nao e membro: mantem, so normaliza o nome
            s.name = slug(s.name)
            relimb(s, report)
            keep.append(s)
    bone.children = keep
    for kind, g in groups.items():
        limbify(g, kind, bone.children, report)


KIND_BY_NAME = [
    (r"^(main[_ ]?body|mainbody)", "torso_main"),
    (r"^(torso|body)\d*$", "chest"),
    (r"^(waist)", "root"),
    (r"^(head)", "head"),
    (r"^(mouth|jaw)", "jaw"),
    (r"^(nose)", "nose"),
    (r"^(headwear)", "head_wear"),
    (r"^(bodywear)", "chest_wear"),
    (r"^(arms?)([_ ]?\d+)?$", "arm"),
    (r"^(legs?)([_ ]?\d+)?$", "leg"),
    (r"^(tentacles?)([_ ]?\d+)?$", "tentacle"),
    (r"^(ribs?)([_ ]?\d+)?$", "rib"),
    (r"^(tail)", "tail"),
    (r"^(wing)", "wing"),
]


def kind_of(name):
    s = slug(name)
    for pat, kind in KIND_BY_NAME:
        if re.match(pat, s):
            return kind
    return None


LIMB_KINDS = ("arm", "leg", "tentacle", "rib", "tail", "wing")

ORDER = {"head": 0, "head_wear": 1, "nose": 2, "jaw": 3, "chest": 4, "chest_wear": 5,
         "arm": 6, "rib": 7, "tentacle": 8, "leg": 9, "tail": 10, "unused": 99}


def sort_children(bone):
    def key(c):
        if isinstance(c, Cube):
            return (-1, "")
        base = c.name.split("_")[0]
        if c.name.startswith("unused"):
            return (99, c.name)
        return (ORDER.get(base, 50), c.name)
    bone.children.sort(key=key)
    for b in bone.bones():
        sort_children(b)


def rename_cubes(bone, used):
    n = 0
    for c in bone.own_cubes():
        s = slug(c.el.get("name"))
        suffix = CUBE_SUFFIX.get(s, s if s and s not in GENERIC_CUBE else None)
        if suffix is None or suffix == bone.name:
            n += 1
            suffix = str(n)
        name = "cube_%s_%s" % (bone.name, suffix)
        base, k = name, 2
        while name in used:
            name = "%s_%d" % (base, k)
            k += 1
        used.add(name)
        c.el["name"] = name
    for b in bone.bones():
        rename_cubes(b, used)


def unique_bone_names(bone, used):
    base, name, k = bone.name, bone.name, 2
    while name in used:
        name = "%s_%d" % (base, k)
        k += 1
    bone.name = name
    used.add(name)
    for b in bone.bones():
        unique_bone_names(b, used)


# ------------------------------------------------------------------- planos


def plan_steve(roots, report):
    """Waist -> [Head, Body, Right Arm, Left Arm] + pernas soltas na raiz."""
    waist = next(b for b in roots if isinstance(b, Bone) and slug(b.name) == "waist")
    legs = [b for b in roots if isinstance(b, Bone) and b is not waist]
    head = next(b for b in waist.bones() if slug(b.name) == "head")
    torso = next(b for b in waist.bones() if slug(b.name) == "body")
    arms = [b for b in waist.bones() if slug(b.name) in ("right_arm", "left_arm")]
    loose = [c for c in waist.children if isinstance(c, Cube)]

    waist.name = "root"
    head.name = "head"
    torso.name = "torso"
    for a in arms:
        a.name = "arm_%s_1" % ("right" if slug(a.name).startswith("right") else "left")
    for l in legs:
        l.name = "leg_%s_1" % ("right" if slug(l.name).startswith("right") else "left")

    torso.children = torso.own_cubes() + [head] + arms
    waist.children = loose + [torso] + legs
    return [waist]


def plan_head_parent(roots, report):
    """root -> Head -> [cubos, Main_body -> membros]  vira  root -> torso -> head."""
    outer = next(b for b in roots if isinstance(b, Bone))
    extra = [n for n in roots if n is not outer]
    head = next(b for b in outer.bones() if kind_of(b.name) == "head")
    main = next(b for b in head.bones() if kind_of(b.name) == "torso_main")

    head.children = [c for c in head.children if c is not main]
    head.name = "head"
    outer.name = "root"
    main.name = "torso"

    for hc in head.bones():                     # Mouth -> jaw, e afins
        k = kind_of(hc.name)
        hc.name = k if k in ("jaw", "nose", "head_wear") else "head_" + slug(hc.name)

    limbs, keep = {}, []
    for child in list(main.children):
        if isinstance(child, Cube):
            keep.append(child)
            continue
        k = kind_of(child.name)
        if not child.children:                  # osso vazio de verdade
            child.name = "unused_" + slug(child.name)
            keep.append(child)
        elif k in LIMB_KINDS:
            limbs.setdefault(k, []).append(child)
        elif not child.cubes():                 # so ossos vazios dentro
            child.name = "unused_" + slug(child.name)
            keep.append(child)
        elif k == "chest":
            child.name = "chest"
            relimb(child, report)
            keep.append(child)
        else:
            keep.append(child)

    main.children = keep + [head]
    for kind, group in limbs.items():
        limbify(group, kind, main.children, report)
    outer.children = [main] + [n for n in outer.children if isinstance(n, Cube)]
    return [outer] + extra


def _new_root(children, origin=(0, 0, 0)):
    r = Bone("root", list(origin))
    r.children = list(children)
    return r


def plan_pig(roots, report):
    """Tudo solto na raiz e o osso `body` gira -90: torso novo (rot 0) por cima."""
    by = {slug(b.name): b for b in roots if isinstance(b, Bone)}
    body, head = by["body"], by["head"]
    torso = Bone("torso", list(body.origin))          # novo, rotacao 0
    body.name = "torso_shell"                         # guarda o -90 original
    head.name = "head"
    torso.children = [body, head]
    for k in ("tentacle", "tentacle2"):
        limbify([by[k]], "tentacle", torso.children, report)
    legs = [by["leg3"], by["leg4"], by["leg1"], by["leg2"]]   # frente -> tras
    root = _new_root([torso])
    limbify(legs, "leg", root.children, report)
    return [root]


def plan_villager(roots, report):
    by = {slug(b.name): b for b in roots if isinstance(b, Bone)}
    chest, chest_wear = by["body"], by["bodywear"]
    torso = Bone("torso", list(chest.origin))
    chest.name = "chest"
    chest_wear.name = "chest_wear"
    head = by["head"]
    head.name = "head"
    nose, wear1, wear2 = by["nose"], by["headwear"], by["headwear2"]
    nose.name = "nose"
    wear1.name = "head_wear"
    wear2.name = "head_wear_2"
    head.children += [nose, wear1, wear2]
    torso.children = [chest, chest_wear, head]
    for arm in (by["arms"], by["arms2"]):          # 'mirrored' e a manga do braco
        for sub in arm.bones():
            if slug(sub.name).startswith("mirrored"):
                sub.name = "arm_%s_shell" % (side_of(sub) or "center")
    limbify([by["arms"], by["arms2"]], "arm", torso.children, report)
    root = _new_root([torso])
    for k, side in (("right_leg", "right"), ("left_leg", "left")):
        by[k].name = "leg_%s_1" % side
        root.children.append(by[k])
    return [root]


def plan_nightmare(roots, report):
    bones = [n for n in roots if isinstance(n, Bone)]
    loose = [n for n in roots if isinstance(n, Cube)]
    torso = Bone("torso", [0, 0, 0])
    torso.children = list(loose)
    arms = [b for b in bones if slug(b.name).startswith("arm")]
    legs = [b for b in bones if slug(b.name).startswith("leg")]
    limbify(arms, "arm", torso.children, report)
    root = _new_root([torso])
    limbify(legs, "leg", root.children, report)
    return [root]


def plan_serpent(roots, report):
    """head -> [body, body2..body6 irmaos] vira cadeia body_1 -> ... -> body_6."""
    outer = next(b for b in roots if isinstance(b, Bone))
    head = next(b for b in outer.bones() if slug(b.name) == "head")
    segs = sorted([b for b in head.bones() if slug(b.name).startswith("body")],
                  key=lambda b: -b.origin[2])
    head_cubes = [c for c in head.children if isinstance(c, Cube)]
    outer.name = "root"
    head.name = "head"
    head.children = head_cubes
    for i, s in enumerate(segs, 1):
        s.name = "body_%d" % i
        s.children = [c for c in s.children if isinstance(c, Cube)]
    for i in range(len(segs) - 1):
        segs[i].children.append(segs[i + 1])
    segs[0].children.insert(0, head)
    outer.children = [segs[0]]
    report.append("      cadeia body_1..body_%d (z %s)"
                  % (len(segs), ", ".join(str(s.origin[2]) for s in segs)))
    return [outer]


def plan_flat(roots, report):
    """Modelo de peca unica (mirror): so ganha a raiz."""
    return [_new_root(roots)]


PLANS = {
    "Corrupted\\corrupted_1.bbmodel": plan_steve,
    "Corrupted\\corrupted_2.bbmodel": plan_steve,
    "Corrupted\\corrupted_3.bbmodel": plan_steve,
    "Corrupted\\corrupted_4.bbmodel": plan_steve,
    "Stoneman\\corrupted_1.bbmodel": plan_steve,
    "Stoneman\\corrupted_2.bbmodel": plan_steve,
    "Stoneman\\corrupted_3.bbmodel": plan_steve,
    "Stoneman\\corrupted_4.bbmodel": plan_steve,
    "Stoneman\\Stoneman.bbmodel": plan_steve,
    "Stoneman\\Stoneman_variant1.bbmodel": plan_steve,
    "Stoneman\\Stoneman_variant2.bbmodel": plan_steve,
    "Frequency Mimic\\Frequency  Mimic.bbmodel": plan_steve,
    "Inverted_silhoutte\\Inverted_silhoutte.bbmodel": plan_steve,
    "lotofeyes\\LotEyesMan.bbmodel": plan_steve,

    "Static_Watcher\\Static_Watcher.bbmodel": plan_head_parent,
    "Static_Specter\\Static_Specter.bbmodel": plan_head_parent,
    "Stoneman\\Static_Specter.bbmodel": plan_head_parent,
    "Frame_Stalker\\Frame Stalker.bbmodel": plan_head_parent,
    "Shade_Segment\\Shade_Segment.bbmodel": plan_head_parent,
    "Crawler_void\\Crawler_Void.bbmodel": plan_head_parent,
    "Infection entities\\Infected_zombie\\Infected_Zombie.bbmodel": plan_head_parent,
    "Infection entities\\Infecter_Worm\\Infecter_Anomaly.bbmodel": plan_head_parent,
    "Infection entities\\infected cow\\infected  cow.bbmodel": plan_head_parent,
    "Infection entities\\infected_sheep\\infected_sheep.bbmodel": plan_head_parent,

    "Infection entities\\infected_pig\\infected_pig.bbmodel": plan_pig,
    "Infection entities\\infected_villager\\villager - Converted.bbmodel": plan_villager,
    "Stoneman\\Nightmare_1.bbmodel": plan_nightmare,
    "giant_serpent\\CustomModel.bbmodel": plan_serpent,
    "mirror\\mirror.bbmodel": plan_flat,

    "Stoneman\\model.bbmodel": None,   # formato `skin`, 0 cubos: nada a fazer
}


# --------------------------------------------------------------------- main


def draw(nodes, depth=0, out=None):
    out = [] if out is None else out
    for n in nodes:
        if isinstance(n, Cube):
            out.append("%s- %s" % ("  " * depth, n.el["name"]))
        else:
            r = "" if n.rotation == [0, 0, 0] else " rot=%s" % n.rotation
            new = "" if n.src is not None else "  (osso novo)"
            out.append("%s+ %s  pivot=%s%s%s" % ("  " * depth, n.name, n.origin, r, new))
            draw(n.children, depth + 1, out)
    return out


def process(path, rel, dry):
    with open(path, "r", encoding="utf-8") as f:
        doc = json.load(f)
    planner = PLANS.get(rel, "missing")
    if planner == "missing":
        return "SEM PLANO", []
    if planner is None:
        return "pulado (formato skin)", []

    roots = parse(doc)
    before = snapshot(roots)
    report = []
    roots = planner(roots, report)

    for r in roots:
        sort_children(r)
    used_b, used_c = set(), set()
    for r in roots:
        unique_bone_names(r, used_b)
    for r in roots:
        rename_cubes(r, used_c)

    after = snapshot(roots)
    problems = verify(before, after)
    tree = draw(roots)
    if problems:
        return "REPROVADO: " + "; ".join(problems[:4]), report + tree
    if not dry:
        serialize(doc, roots)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(doc, f, separators=(",", ":"), ensure_ascii=False)
    return "ok (%d cubos conferidos)" % len(after), report + tree


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--quiet", action="store_true")
    ap.add_argument("--only", default=None)
    args = ap.parse_args()

    if not args.dry_run and not os.path.isdir(BACKUP):
        shutil.copytree(ROOT, BACKUP)
        print("backup: %s" % BACKUP)

    files = []
    for dp, _d, fs in os.walk(ROOT):
        for f in fs:
            if f.lower().endswith(".bbmodel"):
                files.append(os.path.join(dp, f))
    files.sort()

    bad = 0
    for p in files:
        rel = os.path.relpath(p, ROOT)
        if args.only and args.only.lower() not in rel.lower():
            continue
        status, lines = process(p, rel, args.dry_run)
        if not status.startswith("ok") and not status.startswith("pulado"):
            bad += 1
        print("\n=== %s  ->  %s" % (rel, status))
        if not args.quiet:
            for l in lines:
                print("    " + l)
    print("\n%d arquivo(s) com problema." % bad)
    return 1 if bad else 0


if __name__ == "__main__":
    raise SystemExit(main())
