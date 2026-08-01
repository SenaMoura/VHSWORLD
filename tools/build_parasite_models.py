# -*- coding: utf-8 -*-
"""Refaz Crawler_Void e Shade_Segment no estilo dos refs do Pedro (SRParasites).

Malha E textura sao geradas aqui: como eu mesmo empacoto o UV, sei onde cai cada
face de cada cubo e consigo pintar por face (topo mais claro, baixo cravado,
sangue na juntura, dentes na cara) em vez de jogar ruido por cima.

Convencao do rig (a mesma que o resto do lote usa):
  - o esqueleto e authorado ESTICADO: cada segmento sai reto do proprio pivo e
    quem dobra e a rotacao do osso. A origem de um osso filho e a ponta do pai
    ANTES de girar - e assim que o Blockbench compoe pai->filho, e e o que faz a
    cadeia continuar certa quando a animacao mexe no pai.
  - nomes: root -> torso -> head / arm_<lado>_<n> / leg_<lado>_<letra>_<n>.

    python tools/build_parasite_models.py            # grava os 2 .bbmodel + .png
    python tools/build_parasite_models.py --preview  # so desenha, nao grava
"""

import argparse
import base64
import io
import json
import math
import os
import uuid as uuidlib

from PIL import Image

ROOT = r"C:\Users\Hamilton\Downloads\vhsworldentities"

# ------------------------------------------------------------------ paleta
# lida dos refs: carne palida no corpo, escurecendo ate quase preto na ponta
# dos membros, sangue coagulado nas junturas, dente/osso quase branco.
MATS = {
    "flesh":    (198, 164, 137),
    "flesh_lo": (156, 124, 101),
    "meat":     (142, 62, 52),
    "limb":     (120, 92, 74),
    "limb_dk":  (74, 55, 44),
    "limb_bk":  (34, 25, 21),
    "head_dk":  (54, 40, 34),
    "teeth":    (226, 219, 201),
    "bone":     (198, 190, 168),
    # paleta VHSWORLD: a corrupcao TIRA a cor - o corpo e quase preto e a carne
    # so aparece como acento (dente, gengiva, viscera)
    "char":     (62, 55, 51),
    "soot":     (94, 84, 77),
    "ash":      (130, 117, 106),
    "grime":    (166, 151, 137),
    "teeth_y":  (206, 186, 92),
    "gums":     (146, 40, 40),
    "glow":     (232, 206, 96),
    "viscera":  (118, 34, 32),
}
BLOOD = (128, 38, 30)
BLOOD_DK = (78, 22, 18)

FACE_SHADE = {"up": 1.18, "down": 0.58, "north": 1.02, "south": 0.86,
              "east": 0.95, "west": 0.80}
FACE_ORDER = ("north", "east", "south", "west", "up", "down")


def rot3(deg):
    rx, ry, rz = [math.radians(a) for a in deg]
    cx, sx, cy, sy, cz, sz = (math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry),
                              math.cos(rz), math.sin(rz))
    mx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    my = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    mz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]

    def mul(a, b):
        return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]

    return mul(mz, mul(my, mx))


def clamp8(v):
    return max(0, min(255, int(round(v))))


def shade(c, k):
    return tuple(clamp8(v * k) for v in c)


# ------------------------------------------------------------------ modelo


class Part(object):
    def __init__(self, name, bone, box, mat, rotation=None, origin=None, detail=None):
        self.name = name
        self.bone = bone
        self.f = [float(v) for v in box[0]]
        self.t = [float(v) for v in box[1]]
        self.mat = mat
        self.rotation = list(rotation or [0, 0, 0])
        self.origin = list(origin or [0, 0, 0])
        self.detail = detail or {}
        self.uv = [0, 0]

    def size(self):
        return [int(round(self.t[i] - self.f[i])) for i in range(3)]


class Rig(object):
    def __init__(self, name, tex_name, res=128):
        self.name = name
        self.tex_name = tex_name
        self.res = res
        self.bones = {}          # nome -> dict(origin, rotation, parent)
        self.order = []
        self.parts = []

    def bone(self, name, parent, origin, rotation=None):
        self.bones[name] = {"origin": [float(v) for v in origin],
                            "rotation": [float(v) for v in (rotation or [0, 0, 0])],
                            "parent": parent}
        self.order.append(name)
        return name

    def cube(self, bone, box, mat, name=None, detail=None):
        p = Part(name or "%s_%d" % (bone, sum(1 for q in self.parts if q.bone == bone) + 1),
                 bone, box, mat, detail=detail)
        self.parts.append(p)
        return p

    # ---- cadeia de membro: cada elo pende reto do pivo, a rotacao e que dobra
    def limb(self, base_name, parent, joint, segs, mats, detail_first=None):
        """segs = [(comprimento, espessura, (rx, ry, rz)), ...] de cima p/ baixo."""
        prev = parent
        pos = list(joint)
        for i, (ln, th, rot) in enumerate(segs, 1):
            nm = "%s_%d" % (base_name, i)
            self.bone(nm, prev, pos, rot)
            h = th / 2.0
            self.cube(nm, ([pos[0] - h, pos[1] - ln, pos[2] - h],
                           [pos[0] + h, pos[1], pos[2] + h]),
                      mats[min(i - 1, len(mats) - 1)],
                      detail=(detail_first if i == 1 else None))
            pos = [pos[0], pos[1] - ln, pos[2]]     # ponta do elo, ainda sem girar
            prev = nm
        return prev, pos

    # ------------------------------------------------ por o bicho NO CHAO
    def world_min_y(self, only=None):
        parents = {n: self.bones[n]["parent"] for n in self.order}
        lowest = 1e9
        for p in self.parts:
            if only and not p.bone.startswith(only):
                continue
            chain = []
            n = p.bone
            while n is not None:
                b = self.bones[n]
                chain.append((b["origin"], b["rotation"]))
                n = parents[n]
            pts = [[x, y, z] for x in (p.f[0], p.t[0]) for y in (p.f[1], p.t[1])
                   for z in (p.f[2], p.t[2])]
            for o, r in chain:
                if r == [0, 0, 0]:
                    continue
                m = rot3(r)
                pts = [[sum(m[i][k] * (q[k] - o[k]) for k in range(3)) + o[i]
                        for i in range(3)] for q in pts]
            lowest = min(lowest, min(q[1] for q in pts))
        return lowest

    def plant(self):
        """Desce/sobe tudo para o ponto mais baixo encostar em y=0.

        Com membro dobrado nao da para saber a altura do pe de cabeca: ela sai da
        cadeia de rotacoes. Entao eu construo solto e ajusto no fim - senao o
        bicho nasce flutuando (foi o que aconteceu na 1a versao do Crawler).

        ⚠️ A ancora e a PERNA, nao o ponto mais baixo: no Crawler a lamina da
        cabeca desce mais que o pe, e apoiar por ela pendurava o bicho pelo
        queixo com as patas no ar."""
        dy = self.world_min_y(only="leg_")
        if dy > 1e8:
            dy = self.world_min_y()
        for b in self.bones.values():
            b["origin"][1] -= dy
        for p in self.parts:
            p.f[1] -= dy
            p.t[1] -= dy
            p.origin[1] -= dy
        return dy

    def span(self):
        parents = {n: self.bones[n]["parent"] for n in self.order}
        pts_all = []
        for p in self.parts:
            chain, n = [], p.bone
            while n is not None:
                b = self.bones[n]
                chain.append((b["origin"], b["rotation"]))
                n = parents[n]
            pts = [[x, y, z] for x in (p.f[0], p.t[0]) for y in (p.f[1], p.t[1])
                   for z in (p.f[2], p.t[2])]
            for o, r in chain:
                if r == [0, 0, 0]:
                    continue
                m = rot3(r)
                pts = [[sum(m[i][k] * (q[k] - o[k]) for k in range(3)) + o[i]
                        for i in range(3)] for q in pts]
            pts_all += pts
        mn = [min(q[i] for q in pts_all) for i in range(3)]
        mx = [max(q[i] for q in pts_all) for i in range(3)]
        return [(mx[i] - mn[i]) / 16.0 for i in range(3)], mn, mx

    # ---------------------------------------------------------- empacotar UV
    def pack(self):
        """Prateleiras simples. Cada cubo ocupa 2*(d+w) x (d+h) no atlas."""
        parts = sorted(self.parts, key=lambda p: -(p.size()[2] + p.size()[1]))
        x = y = shelf = 0
        for p in parts:
            w, h, d = p.size()
            bw, bh = 2 * (d + w), d + h
            if x + bw > self.res:
                x, y = 0, y + shelf + 1
                shelf = 0
            if y + bh > self.res:
                raise SystemExit("UV nao cabe em %dx%d - aumentar a resolucao" % (self.res, self.res))
            p.uv = [x, y]
            x += bw + 1
            shelf = max(shelf, bh)
        return y + shelf

    def regions(self, p):
        u, v = p.uv
        w, h, d = p.size()
        return {
            "up":    (u + d, v, u + d + w, v + d),
            "down":  (u + d + w, v, u + d + 2 * w, v + d),
            "east":  (u, v + d, u + d, v + d + h),
            "north": (u + d, v + d, u + d + w, v + d + h),
            "west":  (u + d + w, v + d, u + d + w + d, v + d + h),
            "south": (u + d + w + d, v + d, u + 2 * d + 2 * w, v + d + h),
        }

    def faces_json(self, p):
        r = self.regions(p)
        out = {}
        for k in FACE_ORDER:
            x1, y1, x2, y2 = r[k]
            uv = [x2, y2, x1, y1] if k in ("up", "down") else [x1, y1, x2, y2]
            out[k] = {"uv": [float(v) for v in uv], "texture": 0}
        return out

    # ------------------------------------------------------------- pintura
    def paint(self):
        im = Image.new("RGBA", (self.res, self.res), (0, 0, 0, 0))
        px = im.load()
        for p in self.parts:
            base = MATS[p.mat]
            w, h, d = p.size()
            for fname, (x1, y1, x2, y2) in self.regions(p).items():
                k = FACE_SHADE[fname]
                fw, fh = max(1, x2 - x1), max(1, y2 - y1)
                for yy in range(y1, y2):
                    for xx in range(x1, x2):
                        if not (0 <= xx < self.res and 0 <= yy < self.res):
                            continue
                        u = (xx - x1) / float(fw)
                        v = (yy - y1) / float(fh)
                        c = shade(base, k)
                        # volume: mais claro em cima da face, cravado embaixo
                        c = shade(c, 1.10 - 0.26 * v)
                        # manchas duras (sem ruido fino: os refs sao de bloco chapado)
                        n = ((xx * 73856093) ^ (yy * 19349663) ^ (id(p) & 0xFFFF)) & 0xFF
                        if n < 26:
                            c = shade(c, 0.82)
                        elif n > 232:
                            c = shade(c, 1.10)
                        # contorno cravado
                        if xx in (x1, x2 - 1) or yy in (y1, y2 - 1):
                            c = shade(c, 0.72)
                        px[xx, yy] = (c[0], c[1], c[2], 255)
                self._detail(px, p, fname, (x1, y1, x2, y2))
        return im

    def _detail(self, px, p, fname, box):
        x1, y1, x2, y2 = box
        det = p.detail or {}
        if det.get("blood") and fname not in ("up", "down"):
            # sangue escorrendo da juntura: faixa no topo da face + pingos
            for xx in range(x1, x2):
                run = 1 + (((xx * 2654435761) >> 5) & 3)
                for yy in range(y1, min(y2, y1 + run)):
                    c = BLOOD if (xx + yy) % 5 else BLOOD_DK
                    px[xx, yy] = (c[0], c[1], c[2], 255)
        if det.get("meat") and fname == "up":
            for yy in range(y1, y2):
                for xx in range(x1, x2):
                    n = ((xx * 374761393) ^ (yy * 668265263)) & 0xFF
                    c = MATS["meat"] if n > 90 else BLOOD_DK
                    px[xx, yy] = (c[0], c[1], c[2], 255)
        sides = ("north", "east", "west") if det.get("teeth_side") else ("north",)
        if det.get("teeth") and fname in sides:
            # fileira de dentes na cara, listras verticais coladas na base
            tooth = MATS[det.get("teeth_mat", "teeth")]
            base_y = y2 - max(2, (y2 - y1) // 3)
            if det.get("gums"):
                for xx in range(x1, x2):
                    for yy in range(max(y1, base_y - 2), base_y):
                        c = MATS["gums"] if (xx + yy) % 4 else BLOOD_DK
                        px[xx, yy] = (c[0], c[1], c[2], 255)
            for xx in range(x1 + 1, x2 - 1):
                if (xx - x1) % 2:
                    continue
                for yy in range(base_y, y2 - 1):
                    c = tooth if (yy - base_y) < (y2 - base_y - 1) else MATS["bone"]
                    px[xx, yy] = (c[0], c[1], c[2], 255)
        if det.get("glow") and fname in ("north", "south"):
            cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
            for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                if x1 <= cx + dx < x2 and y1 <= cy + dy < y2:
                    c = MATS["glow"]
                    px[cx + dx, cy + dy] = (c[0], c[1], c[2], 255)
        if det.get("bands"):
            for yy in range(y1, y2):
                if ((yy - y1) // 2) % 3:
                    continue
                for xx in range(x1, x2):
                    c = shade(MATS[p.mat], 0.55)
                    px[xx, yy] = (c[0], c[1], c[2], 255)
        if det.get("eye") and fname == "north":
            cy = y1 + (y2 - y1) // 3
            for xx in (x1 + 1, x2 - 2):
                for yy in (cy, cy + 1):
                    if x1 <= xx < x2 and y1 <= yy < y2:
                        px[xx, yy] = (12, 10, 10, 255)

    # -------------------------------------------------------------- gravar
    def doc(self):
        self.plant()
        used = self.pack()
        im = self.paint()
        buf = io.BytesIO()
        im.save(buf, format="PNG")
        gid = {n: str(uuidlib.uuid4()) for n in self.order}
        elements, by_bone = [], {}
        for p in self.parts:
            u = str(uuidlib.uuid4())
            elements.append({
                "name": p.name, "box_uv": True, "rescale": False, "locked": False,
                "render_order": "default", "allow_mirror_modeling": True,
                "from": p.f, "to": p.t, "autouv": 0, "color": 0,
                "origin": p.origin or [0, 0, 0], "uv_offset": [float(v) for v in p.uv],
                "faces": self.faces_json(p), "type": "cube", "uuid": u,
            })
            by_bone.setdefault(p.bone, []).append(u)
        groups, children_of = [], {}
        for n in self.order:
            b = self.bones[n]
            groups.append({
                "name": n, "uuid": gid[n], "export": True, "locked": False, "scope": 0,
                "selected": False, "_static": {"properties": {}, "temp_data": {}},
                "origin": b["origin"], "rotation": b["rotation"], "color": 0,
                "children": [], "reset": False, "shade": True, "mirror_uv": False,
                "visibility": True, "autouv": 0, "isOpen": True, "primary_selected": False,
            })
            children_of.setdefault(b["parent"], []).append(n)

        def node(n):
            return {"uuid": gid[n], "isOpen": True,
                    "children": by_bone.get(n, []) + [node(c) for c in children_of.get(n, [])]}

        outliner = [node(n) for n in children_of.get(None, [])]
        return {
            "meta": {"format_version": "5.0", "model_format": "modded_entity", "box_uv": True},
            "name": self.name, "model_identifier": "", "modded_entity_entity_class": "",
            "modded_entity_version": "1.20", "modded_entity_flip_y": True,
            "visible_box": [3, 3, 0], "variable_placeholders": "",
            "variable_placeholder_buttons": [], "timeline_setups": [],
            "unhandled_root_fields": {}, "resolution": {"width": self.res, "height": self.res},
            "elements": elements, "groups": groups, "outliner": outliner,
            "textures": [{
                "path": "", "name": self.tex_name, "folder": "", "namespace": "",
                "id": "0", "group": "", "width": self.res, "height": self.res,
                "uv_width": self.res, "uv_height": self.res, "particle": False,
                "layers_enabled": False, "sync_to_project": "", "render_mode": "default",
                "render_sides": "auto", "frame_time": 1, "frame_order_type": "loop",
                "frame_order": "", "frame_interpolate": False, "visible": True,
                "internal": True, "saved": False, "uuid": str(uuidlib.uuid4()),
                "relative_path": self.tex_name,
                "source": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode(),
            }],
        }, im, used


# ---------------------------------------------------- CRAWLER VOID (ref 1)


def crawler():
    """Opiliao do ref: corpo baixo, 4 pernas com o JOELHO ACIMA do corpo e uma
    lamina de cabeca que desce pela frente ate quase o chao."""
    r = Rig("Crawler_Void", "Crawler_void.png", res=128)
    r.bone("root", None, [0, 0, 0])
    r.bone("torso", "root", [0, 21, 0], [-6, 0, 0])
    r.cube("torso", ([-6, 19, -8], [6, 25, 7]), "flesh", "cube_torso_base",
           detail={"meat": True})
    r.cube("torso", ([-4.5, 16, -6], [4.5, 19, 4]), "flesh_lo", "cube_torso_belly")
    # costelas claras penduradas na barriga (o detalhe listrado do ref)
    for i, x in enumerate((-3.5, 0, 3.5)):
        r.cube("torso", ([x - 1.5, 11, -3], [x + 1.5, 16, 0]), "bone",
               "cube_torso_rib_%d" % (i + 1), detail={"blood": True})

    # cabeca-lamina: 3 elos descendo pela frente, escurecendo ate quase preto
    r.bone("head", "torso", [0, 25, -5], [10, 0, 0])
    r.cube("head", ([-2.5, 15, -7], [2.5, 27, -3]), "head_dk", "cube_head_base",
           detail={"eye": True})
    r.bone("head_2", "head", [0, 15, -5], [-4, 0, 0])
    r.cube("head_2", ([-2, 5, -7], [2, 15, -3]), "limb_dk", "cube_head_2_1")
    r.bone("jaw", "head_2", [0, 8, -5], [-8, 0, 0])
    r.cube("jaw", ([-1.5, 3, -6.5], [1.5, 8, -3.5]), "limb_bk", "cube_jaw_1",
           detail={"teeth": True})

    # pernas: femur 13 sobe 30 graus pra fora, tibia 28 desce quase reta, pe 8.
    # Os numeros sairam da conta da cadeia (nao de chute): com femur a -46 e
    # tibia a +92 os pes paravam 0,7 bloco no ar e o bicho abria 4,2 blocos.
    for side, sx in (("right", 1), ("left", -1)):
        for letter, z, spread in (("a", -6, -14), ("b", 5, 14)):
            hip = [5.0 * sx, 24.0, float(z)]
            r.bone("hip_%s_%s" % (side, letter), "torso", hip)
            fem = "leg_%s_%s_1" % (side, letter)
            r.bone(fem, "hip_%s_%s" % (side, letter), hip, [spread, 0, -40 * sx])
            r.cube(fem, ([hip[0] - 1.75, hip[1], hip[2] - 1.75],
                         [hip[0] + 1.75, hip[1] + 16, hip[2] + 1.75]), "limb",
                   "cube_%s_1" % fem, detail={"blood": True})
            knee = [hip[0], hip[1] + 16, hip[2]]
            tib = "leg_%s_%s_2" % (side, letter)
            r.bone(tib, fem, knee, [-spread * 0.7, 0, 62 * sx])
            r.cube(tib, ([knee[0] - 1.25, knee[1] - 25, knee[2] - 1.25],
                         [knee[0] + 1.25, knee[1], knee[2] + 1.25]), "limb_dk",
                   "cube_%s_1" % tib)
            ank = [knee[0], knee[1] - 25, knee[2]]
            foot = "leg_%s_%s_3" % (side, letter)
            r.bone(foot, tib, ank, [-spread * 0.3, 0, -22 * sx])
            r.cube(foot, ([ank[0] - 1, ank[1] - 6, ank[2] - 1],
                          [ank[0] + 1, ank[1], ank[2] + 1]), "limb_bk", "cube_%s_1" % foot)
    return r


# --------------------------------------------------- SHADE SEGMENT (ref 2)


def shade_segment():
    """Parasita curvado do ref: tronco tombado pra frente, bracos compridos
    alcancando a frente com dedos finos, 4 pernas magras escuras.

    ⚠️ Sinal: rotacao +X joga o topo pra TRAS (+Z). Curvar pra frente e NEGATIVO -
    a 1a versao usou +24 e o bicho nasceu empinado."""
    r = Rig("Shade_Segment", "Shade_segment.png", res=128)
    r.bone("root", None, [0, 0, 0])
    r.bone("torso", "root", [0, 20, 0], [-26, 0, 0])
    r.cube("torso", ([-5, 20, -4], [5, 33, 4]), "flesh", "cube_torso_base",
           detail={"blood": True})
    r.cube("torso", ([-4, 14, -3.5], [4, 20, 3.5]), "flesh_lo", "cube_torso_pelvis")
    r.cube("torso", ([-5.5, 28, 2], [5.5, 33, 6]), "meat", "cube_torso_back",
           detail={"meat": True})

    # cabeca projetada pra FRENTE (-Z), com a fileira de dentes na cara
    r.bone("neck", "torso", [0, 31, -3], [10, 0, 0])
    r.cube("neck", ([-2.5, 28, -9], [2.5, 33, -2]), "flesh_lo", "cube_neck_1",
           detail={"blood": True})
    r.bone("head", "neck", [0, 31, -8], [8, 0, 0])
    r.cube("head", ([-4, 27, -19], [4, 35, -8]), "flesh", "cube_head_base",
           detail={"teeth": True, "eye": True})
    r.bone("jaw", "head", [0, 28, -9], [14, 0, 0])
    r.cube("jaw", ([-3.5, 24, -18], [3.5, 28, -9]), "flesh_lo", "cube_jaw_1",
           detail={"teeth": True, "blood": True})

    # bracos: cotovelo pra cima/tras, antebraco alcancando a frente e pra baixo.
    # tip de um vetor que pende (0,-L,0): Rx positivo leva a ponta pra FRENTE.
    for side, sx in (("right", 1), ("left", -1)):
        sh = [6.0 * sx, 30.0, 0.0]
        r.bone("shoulder_%s" % side, "torso", sh)
        r.cube("shoulder_%s" % side, ([min(sh[0], sh[0] + 3 * sx), 28, -2.5],
                                      [max(sh[0], sh[0] + 3 * sx), 34, 3.5]), "flesh",
               "cube_shoulder_%s_1" % side, detail={"blood": True})
        # angulo de MUNDO desejado: braco ~70 graus a frente da vertical, antebraco
        # ~85. Como o braco e filho do torso (-26), o local soma 26.
        tip, pos = r.limb("arm_%s" % side, "shoulder_%s" % side, sh,
                          [(15, 3.0, [96, -22 * sx, 6 * sx]), (16, 2.5, [15, -8 * sx, -4 * sx])],
                          ["flesh", "flesh_lo"], detail_first={"blood": True})
        r.bone("hand_%s" % side, tip, pos, [-72, 0, 0])
        r.cube("hand_%s" % side, ([pos[0] - 2, pos[1] - 3.5, pos[2] - 1.5],
                                  [pos[0] + 2, pos[1], pos[2] + 1.5]), "flesh_lo",
               "cube_hand_%s_1" % side, detail={"blood": True})
        for k, dx in enumerate((-1.4, 0.0, 1.4)):
            fb = "finger_%s_%d" % (side, k + 1)
            r.bone(fb, "hand_%s" % side, [pos[0] + dx, pos[1] - 3.5, pos[2]],
                   [10 - 10 * k, 0, 0])
            r.cube(fb, ([pos[0] + dx - 0.5, pos[1] - 9, pos[2] - 0.5],
                        [pos[0] + dx + 0.5, pos[1] - 3.5, pos[2] + 0.5]), "limb_dk",
                   "cube_%s_1" % fb)

    # 4 pernas magras: joelho pra tras, pe curto no chao
    for side, sx in (("right", 1), ("left", -1)):
        for letter, z, ang in (("a", -5, 10), ("b", 5, -10)):
            hip = [5.5 * sx, 18.0, float(z)]
            r.bone("hip_%s_%s" % (side, letter), "torso", hip)
            # quase na vertical no mundo: 26 cancela a inclinacao do torso
            r.limb("leg_%s_%s" % (side, letter), "hip_%s_%s" % (side, letter), hip,
                   [(13, 2.6, [26 + ang, 0, 15 * sx]), (13, 2.2, [-ang - 16, 0, 4 * sx]),
                    (4, 2.8, [6, 0, 0])],
                   ["limb", "limb_dk", "limb_bk"], detail_first={"blood": True})
    return r


# --------------------------------------------------------------------- main


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", action="store_true")
    args = ap.parse_args()
    for rig, rel in ((crawler(), r"Crawler_void\Crawler_Void.bbmodel"),
                     (shade_segment(), r"Shade_Segment\Shade_Segment.bbmodel")):
        doc, im, used = rig.doc()
        print("%-16s cubos=%-3d ossos=%-3d  atlas usado ate y=%d de %d"
              % (rig.name, len(rig.parts), len(rig.order), used, rig.res))
        if args.preview:
            continue
        path = os.path.join(ROOT, rel)
        if os.path.isfile(path) and not os.path.isfile(path + ".pre_parasite"):
            os.replace(path, path + ".pre_parasite")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(doc, f, separators=(",", ":"), ensure_ascii=False)
        png = os.path.join(os.path.dirname(path), rig.tex_name)
        im.save(png)
        print("   gravado: %s + %s" % (rel, rig.tex_name))


if __name__ == "__main__":
    raise SystemExit(main())
