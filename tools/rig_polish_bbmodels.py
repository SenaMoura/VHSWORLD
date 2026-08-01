# -*- coding: utf-8 -*-
"""Passe 2 do rig: poe os pivos NAS ARTICULACOES e escreve idle/walk/attack.

Roda depois do `restructure_bbmodels.py` (que ja deixou a hierarquia e os nomes
padronizados). Duas coisas:

  PIVOS  - pescoco no fundo da cabeca, ombro/quadril no topo do membro, joelho e
           cotovelo no CONTATO entre dois elos da cadeia. So mexe em osso com
           rotacao [0,0,0]: em osso girado, mudar a origem MOVE a peca (nao ha
           campo de translacao em grupo de .bbmodel para compensar).

  ANIMS  - idle / walk / attack, catmullrom nos membros moles, `step` no impacto,
           e MoLang nos eixos secundarios (tremor parasitario + atraso de fase de
           0.10 a 0.18s ao longo da cadeia). Sai nos dois lugares: dentro do
           .bbmodel e num `<nome>.animation.json` (Bedrock 1.8.0) ao lado.

A mesma trava de sempre: os 8 cantos de cada cubo sao recalculados antes e
depois; se um canto anda, o arquivo nao e gravado.

    python tools/rig_polish_bbmodels.py --dry-run
    python tools/rig_polish_bbmodels.py
"""

import argparse
import json
import os
import re
import uuid as uuidlib

from restructure_bbmodels import (ROOT, Bone, Cube, parse, serialize, snapshot,
                                  verify, union_box, world_aabb, dist3)

FPS = 24

# ------------------------------------------------------------------- papeis

NAME_RE = re.compile(r"^(arm|leg|tentacle|rib|tail|wing|body)"
                     r"(?:_(left|right))?(?:_([a-p]))?(?:_(\d+))?$")


def parse_name(name):
    m = NAME_RE.match(name)
    if not m:
        return None
    kind, side, letter, seg = m.groups()
    return kind, side, letter or "", int(seg or 1)


def collect(bone, out=None, parent=None):
    out = {} if out is None else out
    out[bone.name] = (bone, parent)
    for b in bone.bones():
        collect(b, out, bone)
    return out


def limb_chains(index):
    """{(kind, side, letter): [osso_1, osso_2, ...]} em ordem de segmento."""
    chains = {}
    for name, (bone, _p) in index.items():
        info = parse_name(name)
        if not info:
            continue
        kind, side, letter, seg = info
        chains.setdefault((kind, side or "", letter), []).append((seg, bone))
    return {k: [b for _s, b in sorted(v)] for k, v in chains.items()}


# -------------------------------------------------------------------- pivos


def box_of(bone, own_only=True):
    cubes = bone.own_cubes() if own_only else bone.cubes()
    if not cubes:
        return None
    return union_box([world_aabb(c) for c in cubes])


def face_center(box, toward, axis=None):
    """Centro da face da caixa virada para `toward` (o pai)."""
    c = [(box[0][i] + box[1][i]) / 2.0 for i in range(3)]
    if axis is None:
        axis = max(range(3), key=lambda i: abs(toward[i] - c[i]))
    c[axis] = box[1][axis] if toward[axis] > c[axis] else box[0][axis]
    return [round(v, 5) for v in c]


def contact(box_a, box_b):
    out = []
    for i in range(3):
        lo, hi = max(box_a[0][i], box_b[0][i]), min(box_a[1][i], box_b[1][i])
        out.append(round((lo + hi) / 2.0 if lo <= hi
                         else (box_a[1][i] + box_b[0][i]) / 2.0 if box_a[1][i] < box_b[0][i]
                         else (box_b[1][i] + box_a[0][i]) / 2.0, 5))
    return out


def fix_pivots(roots, report):
    index = {}
    for r in roots:
        collect(r, index)
    moved, blocked = 0, 0

    def setp(bone, target, why, box=None, low_is_wrong=False):
        """So mexe em pivo ERRADO.

        Dois testes, e os dois vieram de erro meu: (1) se o pivo ja cai DENTRO da
        propria peca, ele foi posto ali de proposito - o ombro vanilla [5,22,0] e
        melhor que meu calculo, e o do Crawler em x=-33 e a base do membro girado;
        (2) a excecao e ombro/quadril na metade de baixo de um membro que pende,
        que esta objetivamente errado."""
        nonlocal moved, blocked
        if target is None:
            return
        target = [round(v, 5) for v in target]
        if dist3(bone.origin, target) <= 3.0:
            return
        if box is not None:
            inside = all(box[0][i] - 2 <= bone.origin[i] <= box[1][i] + 2 for i in range(3))
            high = bone.origin[1] >= (box[0][1] + box[1][1]) / 2.0
            if inside and (high or not low_is_wrong):
                return
        if bone.rotation != [0, 0, 0]:
            blocked += 1
            report.append("      ~ %s: pivo nao ajustado (osso tem rotacao %s; mexer moveria a peca)"
                          % (bone.name, bone.rotation))
            return
        report.append("      %s: pivo %s -> %s (%s)" % (bone.name, bone.origin, target, why))
        bone.origin = target
        moved += 1

    # pescoco: base da cabeca
    for name in ("head",):
        if name in index:
            bone, parent = index[name]
            box = box_of(bone) or box_of(bone, own_only=False)
            if box:
                cx = round((box[0][0] + box[1][0]) / 2.0, 5)
                cz = round((box[0][2] + box[1][2]) / 2.0, 5)
                setp(bone, [cx, round(box[0][1], 5), cz], "pescoco = base da cabeca", box)

    # mandibula: dobradica no fundo, atras
    if "jaw" in index:
        bone, _p = index["jaw"]
        box = box_of(bone, own_only=False)
        if box:
            setp(bone, [round((box[0][0] + box[1][0]) / 2.0, 5), round(box[0][1], 5),
                        round(box[0][2], 5)], "dobradica da mandibula", box)

    # membros: raiz no ombro/quadril, elos seguintes no contato (joelho/cotovelo)
    for key, chain in limb_chains(index).items():
        kind = key[0]
        first = chain[0]
        parent = index[first.name][1]
        pbox = box_of(parent) if parent else None
        fbox = box_of(first, own_only=False)
        if fbox:
            if kind in ("arm", "leg"):
                # ombro e quadril ficam no TOPO do membro (o membro pende dali),
                # nunca no meio da faixa de contato com o tronco
                xz = contact(pbox, fbox) if pbox else [(fbox[0][i] + fbox[1][i]) / 2.0
                                                       for i in range(3)]
                target = [min(max(xz[0], fbox[0][0]), fbox[1][0]), fbox[1][1],
                          min(max(xz[2], fbox[0][2]), fbox[1][2])]
                why = "ombro/quadril no topo do membro"
            elif pbox:
                target = contact(pbox, fbox)
                target = [min(max(target[i], fbox[0][i]), fbox[1][i]) for i in range(3)]
                why = "encaixe no %s" % (parent.name if parent else "tronco")
            else:
                anchor = parent.origin if parent else [0, 1e4, 0]
                target = face_center(fbox, anchor)
                why = "ponta do membro virada para o tronco"
            setp(first, target, why, fbox, low_is_wrong=kind in ("arm", "leg"))
        for a, b in zip(chain, chain[1:]):
            ba, bb = box_of(a, own_only=False), box_of(b, own_only=False)
            if ba and bb:
                j = contact(ba, bb)
                j = [min(max(j[i], bb[0][i]), bb[1][i]) for i in range(3)]
                setp(b, [round(v, 5) for v in j], "junta com %s" % a.name, bb)
    return moved, blocked


# ---------------------------------------------------------------- animacao

TREMOR = "math.sin(query.anim_time * 1200) * %s"
SWAY = "math.sin(query.anim_time * %d + %s) * %s"


def kf(time, values, interp="catmullrom", channel="rotation"):
    x, y, z = [str(v) for v in values]
    return {
        "channel": channel,
        "data_points": [{"x": x, "y": y, "z": z}],
        "uuid": str(uuidlib.uuid4()),
        "time": round(time, 4),
        "color": -1,
        "interpolation": interp,
        "bezier_linked": True,
        "bezier_left_time": [-0.1, -0.1, -0.1],
        "bezier_left_value": [0, 0, 0],
        "bezier_right_time": [0.1, 0.1, 0.1],
        "bezier_right_value": [0, 0, 0],
    }


def phase(i):
    """Atraso de fase da cadeia: 0.10s no 1o elo ate 0.18s no 5o."""
    return round(min(0.10 + 0.02 * i, 0.18), 3)


def build_anims(roots, entity):
    index = {}
    for r in roots:
        collect(r, index)
    chains = limb_chains(index)
    arms = {k: v for k, v in chains.items() if k[0] == "arm"}
    legs = {k: v for k, v in chains.items() if k[0] == "leg"}
    soft = {k: v for k, v in chains.items() if k[0] in ("tentacle", "rib", "tail", "body")}
    head = index.get("head", (None, None))[0]
    torso = index.get("torso", (None, None))[0]
    jaw = index.get("jaw", (None, None))[0]

    out = {}

    # ---------------- idle: respiracao lenta + tremor parasitario
    tracks = {}
    if torso:
        tracks[torso] = [kf(0, [0, 0, 0]), kf(1.5, [1.5, TREMOR % "0.4", 0]), kf(3.0, [0, 0, 0])]
    if head:
        tracks[head] = [kf(0, [0, 0, 0]),
                        kf(0.9, [-2, -9, TREMOR % "1.5"]),
                        kf(1.8, [1, 7, TREMOR % "1.5"]),
                        kf(3.0, [0, 0, 0])]
    for i, (_k, chain) in enumerate(sorted(soft.items())):
        for j, b in enumerate(chain):
            d = phase(j)
            amp = 5 + 2 * j
            tracks[b] = [kf(0, [SWAY % (140, d + 0.3 * i, amp), SWAY % (95, d, amp * 0.6),
                                TREMOR % "0.8"])]
    for _k, chain in sorted(arms.items()):
        for j, b in enumerate(chain):
            tracks.setdefault(b, [kf(0, [0, 0, 0]),
                                  kf(1.5, [3 + j, 0, TREMOR % "0.6"]),
                                  kf(3.0, [0, 0, 0])])
    out["idle"] = ("loop", 3.0, tracks)

    # ---------------- walk: pernas alternadas, bracos contra, moles arrastando
    tracks = {}
    import math
    SAMPLES = [0.0, 0.25, 0.5, 0.75, 1.0]

    def cyc(t, ph):
        return math.cos(2 * math.pi * (t + ph))

    for key, chain in sorted(legs.items()):
        # a defasagem entra no VALOR, nao no tempo do keyframe: mexer no tempo
        # desalinha o comeco com o fim e o ciclo deixa de fechar
        ph = 0.5 if key[1] == "right" else 0.0
        ph = (ph + 0.25 * max(0, "abcdefgh".find(key[2] or "a"))) % 1.0
        for j, b in enumerate(chain):
            if j == 0:
                tracks[b] = [kf(t, [round(26 * cyc(t, ph), 2), 0, 0]) for t in SAMPLES]
            else:
                lag = phase(j) * 0.5          # joelho/tornozelo chegam atrasados
                amp = max(4, 16 - 4 * j)
                tracks[b] = [kf(t, [round(-amp * min(0, cyc(t, ph + lag)), 2), 0, 0])
                             for t in SAMPLES]
    for key, chain in sorted(arms.items()):
        ph = 0.0 if key[1] == "right" else 0.5      # braco contra a perna do mesmo lado
        for j, b in enumerate(chain[:2]):
            amp = 18 if j == 0 else 8
            tracks[b] = [kf(t, [round(amp * cyc(t, ph + phase(j) * 0.3), 2), 0,
                                TREMOR % "0.8"]) for t in SAMPLES]
    if torso:
        tracks[torso] = [kf(0, [0, 0, 0], channel="position"),
                         kf(0.25, [0, 0.6, 0], channel="position"),
                         kf(0.5, [0, 0, 0], channel="position"),
                         kf(0.75, [0, 0.6, 0], channel="position"),
                         kf(1.0, [0, 0, 0], channel="position")]
    if head:
        tracks[head] = [kf(0, [0, 0, 0]), kf(0.5, [2, 0, TREMOR % "2"]), kf(1.0, [0, 0, 0])]
    for _k, chain in sorted(soft.items()):
        for j, b in enumerate(chain):
            d = phase(j)
            tracks[b] = [kf(0, [SWAY % (360, d, 9 + 2 * j), SWAY % (360, d + 0.05, 6),
                                TREMOR % "1.2"])]
    out["walk"] = ("loop", 1.0, tracks)

    # ---------------- attack: recuo lento, impacto seco (step), recuperacao
    tracks = {}
    if torso:
        tracks[torso] = [kf(0, [0, 0, 0]), kf(0.30, [-9, 0, 0]),
                         kf(0.38, [14, 0, 0], "step"), kf(0.80, [0, 0, 0])]
    if head:
        tracks[head] = [kf(0, [0, 0, 0]), kf(0.30, [-14, 0, TREMOR % "3"]),
                        kf(0.38, [18, 0, 0], "step"), kf(0.80, [0, 0, 0])]
    if jaw:
        tracks[jaw] = [kf(0, [0, 0, 0]), kf(0.28, [-32, 0, 0]),
                       kf(0.38, [-4, 0, 0], "step"), kf(0.80, [0, 0, 0])]
    for key, chain in sorted(arms.items()):
        s = 1 if key[1] == "right" else -1
        for j, b in enumerate(chain):
            d = phase(j) * 0.4
            tracks[b] = [kf(0, [0, 0, 0]),
                         kf(0.30 + d, [-58 + 8 * j, 12 * s, 10 * s]),
                         kf(0.38 + d, [42 - 6 * j, -6 * s, -4 * s], "step"),
                         kf(0.80, [0, 0, 0])]
    for key, chain in sorted(legs.items()):
        tracks[chain[0]] = [kf(0, [0, 0, 0]), kf(0.30, [10, 0, 0]),
                            kf(0.38, [-8, 0, 0], "step"), kf(0.80, [0, 0, 0])]
    for _k, chain in sorted(soft.items()):
        for j, b in enumerate(chain):
            d = phase(j)
            tracks[b] = [kf(0, [0, 0, 0]),
                         kf(0.30 + d, [-24 - 3 * j, SWAY % (420, d, 14), TREMOR % "2.5"]),
                         kf(0.38 + d, [30 + 4 * j, 0, 0], "step"),
                         kf(0.80, [0, 0, 0])]
    out["attack"] = ("once", 0.8, tracks)
    return out


def write_anims(doc, roots, anims, entity):
    uuid_of = {}

    def walk(nodes):
        for n in nodes:
            if isinstance(n, Bone):
                uuid_of[n.name] = (n.src or {}).get("uuid")
                walk(n.children)

    walk(roots)

    bb, bedrock = [], {}
    for name, (loop, length, tracks) in anims.items():
        animators = {}
        bones = {}
        for bone, keys in tracks.items():
            u = uuid_of.get(bone.name)
            if not u:
                continue
            animators[u] = {"name": bone.name, "type": "bone", "rotation_global": False,
                            "quaternion_interpolation": False, "keyframes": keys}
            chan = {}
            for k in keys:
                d = k["data_points"][0]
                entry = [d["x"], d["y"], d["z"]]
                slot = chan.setdefault(k["channel"], {})
                slot["%.4g" % k["time"]] = ({"post": entry, "lerp_mode": "catmullrom"}
                                            if k["interpolation"] == "catmullrom" else entry)
            bones[bone.name] = chan
        bb.append({
            "uuid": str(uuidlib.uuid4()), "name": name,
            "loop": loop if loop != "once" else "once",
            "override": False, "length": length, "snapping": FPS, "selected": False,
            "saved": True, "path": "", "anim_time_update": "", "blend_weight": "",
            "start_delay": "", "loop_delay": "", "animators": animators,
        })
        bedrock["animation.%s.%s" % (entity, name)] = {
            "loop": loop == "loop", "animation_length": length, "bones": bones,
        }
    doc["animations"] = bb
    return {"format_version": "1.8.0", "animations": bedrock}


# ------------------------------------------------------------------- main


def entity_id(rel):
    return re.sub(r"[^a-z0-9]+", "_", os.path.splitext(os.path.basename(rel))[0].lower()).strip("_")


def process(path, rel, dry, do_pivots, do_anims):
    with open(path, "r", encoding="utf-8") as f:
        doc = json.load(f)
    if not doc.get("elements"):
        return "pulado (sem cubos)", []
    roots = parse(doc)
    before = snapshot(roots)
    report, moved, blocked = [], 0, 0
    if do_pivots:
        moved, blocked = fix_pivots(roots, report)
    side = None
    if do_anims:
        anims = build_anims(roots, entity_id(rel))
        side = write_anims(doc, roots, anims, entity_id(rel))
    after = snapshot(roots)
    problems = verify(before, after)
    if problems:
        return "REPROVADO: " + "; ".join(problems[:3]), report
    if not dry:
        serialize(doc, roots)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(doc, f, separators=(",", ":"), ensure_ascii=False)
        if side:
            with open(os.path.splitext(path)[0] + ".animation.json", "w", encoding="utf-8") as f:
                json.dump(side, f, indent=2, ensure_ascii=False)
    n = sum(len(a[2]) for a in (anims.values() if do_anims else []))
    return ("ok - %d pivo(s) na junta, %d travado(s), %d faixa(s) de animacao"
            % (moved, blocked, n)), report


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--only", default=None)
    ap.add_argument("--quiet", action="store_true")
    ap.add_argument("--no-pivots", action="store_true")
    ap.add_argument("--no-anims", action="store_true")
    args = ap.parse_args()

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
        status, lines = process(p, rel, args.dry_run, not args.no_pivots, not args.no_anims)
        if status.startswith("REPROVADO"):
            bad += 1
        print("\n=== %s  ->  %s" % (rel, status))
        if not args.quiet:
            for l in lines:
                print("    " + l)
    print("\n%d arquivo(s) com problema." % bad)
    return 1 if bad else 0


if __name__ == "__main__":
    raise SystemExit(main())
