# -*- coding: utf-8 -*-
"""Seis infectados novos, a partir das 7 fotos de referencia do Pedro.

O que as referencias tem em comum, e que virou a regra de anatomia deste lote:
**poucos volumes grandes e MUITOS membros longos e finos**. O corpo e pequeno,
quem faz a silhueta e a perna comprida, o braco que varre o ar e o cacho de
tentaculos pendurado. Boca e a unica coisa larga.

A paleta e a do VHSWORLD - a corrupcao TIRA a cor: corpo em char/soot/ash e
carne so como acento (dente, gengiva, viscera). O `maw_hound` e o `gilded_maw`
puxam um pouco mais para a carne porque no ref a boca e o assunto.

Reaproveita o Rig de build_parasite_models (esqueleto esticado + rotacao no
osso, empacotamento de UV e pintura por face). Ver as 3 armadilhas de eixo la.

    python tools/build_infected_models.py --preview
    python tools/build_infected_models.py
"""

import argparse
import json
import os

from build_parasite_models import Rig, ROOT

OUT_DIR = os.path.join(ROOT, "Infection entities")


# ------------------------------------------------------- 1. STILT WALKER
def stilt_walker():
    """Ref do End: cabeca de lata, cacho de bracos pendurados, pernas de pau."""
    r = Rig("Stilt_Walker", "stilt_walker.png", res=128)
    r.bone("root", None, [0, 0, 0])
    r.bone("torso", "root", [0, 44, 0], [-8, 0, 0])
    r.cube("torso", ([-5, 42, -4], [5, 54, 4]), "char", "cube_torso_base")
    r.cube("torso", ([-4, 36, -3], [4, 42, 3]), "soot", "cube_torso_pelvis")

    # cabeca-lata inclinada, com a boca no fundo dela
    r.bone("head", "torso", [0, 53, -1], [12, 0, 0])
    r.cube("head", ([-4, 53, -6], [4, 63, 3]), "ash", "cube_head_base",
           detail={"teeth": True, "eye": True, "teeth_side": True})

    # cacho de bracos: 6 de cada lado, finos, pendurados e abrindo
    for side, sx in (("right", 1), ("left", -1)):
        for i, (yy, out, fwd) in enumerate(((52, 62, -18), (49, 78, 4),
                                            (46, 54, 20))):
            sh = [3.5 * sx, float(yy), 0.0]
            r.bone("shoulder_%s_%s" % (side, "abc"[i]), "torso", sh)
            r.limb("arm_%s_%s" % (side, "abc"[i]), "shoulder_%s_%s" % (side, "abc"[i]), sh,
                   [(13, 2.0, [fwd * 0.5, 0, out * 0.45 * sx]),
                    (13, 1.6, [fwd * 0.4, 0, out * 0.30 * sx]),
                    (10, 1.2, [fwd * 0.3, 0, out * 0.20 * sx])],
                   ["soot", "char", "char"])

    # pernas de pau: 2 elos longuissimos + pe curto
    for side, sx in (("right", 1), ("left", -1)):
        hip = [3.0 * sx, 38.0, 0.0]
        r.bone("hip_%s" % side, "torso", hip)
        r.limb("leg_%s" % side, "hip_%s" % side, hip,
               [(16, 2.6, [12, 0, 4 * sx]), (16, 2.2, [-14, 0, 2 * sx]),
                (5, 3.0, [4, 0, 0])], ["soot", "char", "char"])
    return r


# ---------------------------------------------------------- 2. MAW HOUND
def maw_hound():
    """Quadrupede de mandibula comprida, com o cacho de dedos sob o peito."""
    r = Rig("Maw_Hound", "maw_hound.png", res=128)
    r.bone("root", None, [0, 0, 0])
    r.bone("torso", "root", [0, 30, 0], [6, 0, 0])
    r.cube("torso", ([-6, 28, -6], [6, 38, 12]), "soot", "cube_torso_base")
    r.cube("torso", ([-5, 30, 10], [5, 37, 18]), "ash", "cube_torso_rear")

    # cabeca deitada pra frente + mandibula com os dentes palidos
    r.bone("neck", "torso", [0, 36, -5], [-10, 0, 0])
    r.cube("neck", ([-4, 32, -12], [4, 39, -4]), "soot", "cube_neck_1")
    r.bone("head", "neck", [0, 36, -11], [4, 0, 0])
    r.cube("head", ([-5, 31, -26], [5, 40, -11]), "grime", "cube_head_base",
           detail={"eye": True, "teeth": True, "teeth_side": True})
    r.bone("jaw", "head", [0, 32, -12], [10, 0, 0])
    r.cube("jaw", ([-4.5, 27, -27], [4.5, 32, -12]), "grime", "cube_jaw_1",
           detail={"teeth": True, "gums": True, "teeth_side": True})

    # cacho de dedos pendurado do peito (o detalhe que faz o ref)
    for side, sx in (("right", 1), ("left", -1)):
        for i, dz in enumerate((-2.0, 2.0, 6.0)):
            j = [2.5 * sx, 29.0, dz]
            r.bone("gut_%s_%s" % (side, "abc"[i]), "torso", j)
            r.limb("tentacle_%s_%s" % (side, "abc"[i]), "gut_%s_%s" % (side, "abc"[i]), j,
                   [(9, 1.8, [20 - 12 * i, 0, 16 * sx]),
                    (9, 1.4, [26, 0, 10 * sx])], ["ash", "soot"])

    # traseiras altas e dobradas, dianteiras retas (proporcao do ref)
    for side, sx in (("right", 1), ("left", -1)):
        hip = [5.5 * sx, 36.0, 14.0]
        r.bone("hip_%s_b" % side, "torso", hip)
        r.limb("leg_%s_b" % side, "hip_%s_b" % side, hip,
               [(16, 3.0, [-34, 0, 5 * sx]), (20, 2.4, [40, 0, -3 * sx]),
                (5, 3.2, [-8, 0, 0])], ["soot", "char", "char"])
        sh = [5.5 * sx, 30.0, -4.0]
        r.bone("hip_%s_a" % side, "torso", sh)
        r.limb("leg_%s_a" % side, "hip_%s_a" % side, sh,
               [(15, 2.8, [10, 0, 6 * sx]), (13, 2.2, [-14, 0, -2 * sx]),
                (4, 3.0, [4, 0, 0])], ["soot", "char", "char"])
    return r


# --------------------------------------------------------- 3. GILDED MAW
def gilded_maw():
    """Bipede curvado com a boca amarela tomando a cara e um braco varrendo."""
    r = Rig("Gilded_Maw", "gilded_maw.png", res=128)
    r.bone("root", None, [0, 0, 0])
    r.bone("torso", "root", [0, 24, 0], [-18, 0, 0])
    r.cube("torso", ([-6, 24, -5], [6, 40, 5]), "soot", "cube_torso_base",
           detail={"bands": True})
    r.cube("torso", ([-5, 18, -4], [5, 24, 4]), "ash", "cube_torso_pelvis")

    r.bone("head", "torso", [0, 39, -2], [14, 0, 0])
    r.cube("head", ([-5.5, 34, -13], [5.5, 46, -1]), "ash", "cube_head_base",
           detail={"teeth": True, "teeth_mat": "teeth_y", "gums": True, "eye": True,
                   "teeth_side": True})
    r.bone("jaw", "head", [0, 35, -3], [16, 0, 0])
    r.cube("jaw", ([-5, 30, -13], [5, 35, -2]), "ash", "cube_jaw_1",
           detail={"teeth": True, "teeth_mat": "teeth_y", "gums": True,
                   "teeth_side": True})

    # braco DIREITO gigante varrendo pra cima e pra tras; esquerdo curto na frente
    sh = [6.0, 38.0, 0.0]
    r.bone("shoulder_right", "torso", sh)
    r.limb("arm_right", "shoulder_right", sh,
           [(16, 3.4, [-124, 0, 16]), (17, 2.6, [46, 0, 8]),
            (13, 2.0, [42, 0, 4])], ["soot", "ash", "char"])
    sh = [-6.0, 36.0, 0.0]
    r.bone("shoulder_left", "torso", sh)
    r.limb("arm_left", "shoulder_left", sh,
           [(15, 3.0, [64, 0, -14]), (16, 2.3, [40, 0, -6])], ["soot", "ash"])

    # pernas curtas e grossas, com casco
    for side, sx in (("right", 1), ("left", -1)):
        hip = [4.0 * sx, 20.0, 0.0]
        r.bone("hip_%s" % side, "torso", hip)
        tip, pos = r.limb("leg_%s" % side, "hip_%s" % side, hip,
                          [(11, 4.0, [20, 0, 4 * sx]), (9, 3.4, [-22, 0, -2 * sx])],
                          ["soot", "ash"])
        r.bone("hoof_%s" % side, tip, pos, [4, 0, 0])
        r.cube("hoof_%s" % side, ([pos[0] - 3, pos[1] - 4, pos[2] - 3.5],
                                  [pos[0] + 3, pos[1], pos[2] + 3.5]), "char",
               "cube_hoof_%s_1" % side)
    return r


# -------------------------------------------------------- 4. RIB DRIFTER
def rib_drifter():
    """A coisa pequena que paira: nucleo de costelas com um ponto aceso e dois
    membros arqueados por cima, como asas que nao voam."""
    r = Rig("Rib_Drifter", "rib_drifter.png", res=64)
    r.bone("root", None, [0, 0, 0])
    r.bone("torso", "root", [0, 22, 0])
    r.cube("torso", ([-4, 20, -3], [4, 30, 3]), "soot", "cube_torso_base",
           detail={"glow": True})
    for i in range(4):                      # costelas claras dos dois lados
        y = 20 + i * 2.5
        r.cube("torso", ([-6.5, y, -2.5], [-3.5, y + 1.5, 2.5]), "bone",
               "cube_torso_rib_l%d" % (i + 1), detail={"blood": True})
        r.cube("torso", ([3.5, y, -2.5], [6.5, y + 1.5, 2.5]), "bone",
               "cube_torso_rib_r%d" % (i + 1), detail={"blood": True})
    r.cube("torso", ([-3, 29, -2], [3, 33, 2]), "viscera", "cube_torso_crown",
           detail={"meat": True})

    # os dois arcos: sobem, abrem e descem (curva feita com 3 elos)
    for side, sx in (("right", 1), ("left", -1)):
        sh = [3.0 * sx, 30.0, 0.0]
        r.bone("shoulder_%s" % side, "torso", sh)
        r.limb("arm_%s" % side, "shoulder_%s" % side, sh,
               [(9, 3.2, [0, 0, 132 * sx]), (11, 2.8, [0, 0, -52 * sx]),
                (11, 2.4, [0, 0, -46 * sx])], ["char", "soot", "char"])

    # pezinho unico, quase escondido
    r.bone("hip_c", "torso", [0, 20, 0])
    r.limb("leg_c", "hip_c", [0, 20, 0], [(9, 2.6, [0, 0, 0]), (5, 2.2, [0, 0, 0])],
           ["soot", "char"])
    return r


# ----------------------------------------------------- 5. LATTICE PUPPET
def lattice_puppet():
    """Torso empilhado em segmentos e quatro bracos compridos abrindo em roda."""
    r = Rig("Lattice_Puppet", "lattice_puppet.png", res=128)
    r.bone("root", None, [0, 0, 0])
    r.bone("torso", "root", [0, 30, 0], [-6, 0, 0])
    for i in range(5):                      # a pilha de segmentos do ref
        y = 30 + i * 5
        w = 5.5 - 0.5 * i
        r.cube("torso", ([-w, y, -w * 0.7], [w, y + 4.5, w * 0.7]), "char",
               "cube_torso_seg%d" % (i + 1), detail={"bands": True} if i % 2 else None)
    r.bone("head", "torso", [0, 55, 0], [8, 0, 0])
    r.cube("head", ([-3.5, 55, -5], [3.5, 62, 2]), "soot", "cube_head_base",
           detail={"eye": True})

    # 4 bracos por lado, radiais: 2 pra cima/fora, 2 pra baixo/frente
    for side, sx in (("right", 1), ("left", -1)):
        for i, (yy, zrot, xrot) in enumerate(((50, 150, -20), (44, 118, 26))):
            sh = [4.0 * sx, float(yy), 0.0]
            r.bone("shoulder_%s_%s" % (side, "ab"[i]), "torso", sh)
            r.limb("arm_%s_%s" % (side, "ab"[i]), "shoulder_%s_%s" % (side, "ab"[i]), sh,
                   [(13, 2.4, [xrot, 0, zrot * sx]), (13, 2.0, [-xrot * 0.6, 0, -28 * sx]),
                    (7, 1.6, [xrot * 0.4, 0, -20 * sx])], ["soot", "char", "char"])

    for side, sx in (("right", 1), ("left", -1)):
        hip = [2.6 * sx, 30.0, 0.0]
        r.bone("hip_%s" % side, "torso", hip)
        r.limb("leg_%s" % side, "hip_%s" % side, hip,
               [(13, 2.8, [10, 0, 5 * sx]), (11, 2.2, [-12, 0, -3 * sx]),
                (4, 3.0, [2, 0, 0])], ["soot", "char", "char"])
    return r


# --------------------------------------------------------- 6. SICKLE ARM
def sickle_arm():
    """Bipede alto com UM braco-foice enorme varrendo o ar e boca de presas."""
    r = Rig("Sickle_Arm", "sickle_arm.png", res=128)
    r.bone("root", None, [0, 0, 0])
    r.bone("torso", "root", [0, 26, 0], [-14, 0, 0])
    r.cube("torso", ([-5.5, 26, -4], [5.5, 42, 4]), "char", "cube_torso_base")
    r.cube("torso", ([-5, 20, -3.5], [5, 26, 3.5]), "soot", "cube_torso_pelvis")

    r.bone("head", "torso", [0, 41, -2], [10, 0, 0])
    r.cube("head", ([-4.5, 38, -14], [4.5, 48, -2]), "ash", "cube_head_base",
           detail={"teeth": True, "gums": True, "eye": True, "teeth_side": True})
    r.bone("jaw", "head", [0, 39, -3], [18, 0, 0])
    r.cube("jaw", ([-4, 34, -14], [4, 39, -3]), "soot", "cube_jaw_1",
           detail={"teeth": True, "gums": True, "teeth_side": True})
    # antena/chifre curvo saindo da nuca, como no ref
    r.bone("horn", "head", [0, 47, -3], [-40, 0, 0])
    r.cube("horn", ([-1.2, 47, -4.2], [1.2, 63, -1.8]), "char", "cube_horn_1")

    # a foice: 4 elos com a mesma curvatura, varrendo pra cima e pra tras
    sh = [5.5, 40.0, 0.0]
    r.bone("shoulder_right", "torso", sh)
    r.limb("arm_right", "shoulder_right", sh,
           [(15, 3.2, [-118, 0, 12]), (16, 2.6, [40, 0, 10]),
            (15, 2.0, [38, 0, 8]), (9, 1.6, [30, 0, 6])],
           ["soot", "char", "char", "char"])
    sh = [-5.5, 38.0, 0.0]
    r.bone("shoulder_left", "torso", sh)
    r.limb("arm_left", "shoulder_left", sh,
           [(17, 3.0, [74, 0, -14]), (18, 2.4, [22, 0, -8]),
            (10, 1.8, [16, 0, -4])], ["soot", "char", "char"])

    for side, sx in (("right", 1), ("left", -1)):
        hip = [3.6 * sx, 22.0, 0.0]
        r.bone("hip_%s" % side, "torso", hip)
        tip, pos = r.limb("leg_%s" % side, "hip_%s" % side, hip,
                          [(12, 3.6, [18, 0, 4 * sx]), (10, 3.0, [-20, 0, -2 * sx])],
                          ["soot", "char"])
        r.bone("hoof_%s" % side, tip, pos, [2, 0, 0])
        r.cube("hoof_%s" % side, ([pos[0] - 3, pos[1] - 4, pos[2] - 4],
                                  [pos[0] + 3, pos[1], pos[2] + 4]), "char",
               "cube_hoof_%s_1" % side)
    return r


BUILDERS = [("stilt_walker", stilt_walker), ("maw_hound", maw_hound),
            ("gilded_maw", gilded_maw), ("rib_drifter", rib_drifter),
            ("lattice_puppet", lattice_puppet), ("sickle_arm", sickle_arm)]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", action="store_true")
    args = ap.parse_args()
    for folder, fn in BUILDERS:
        rig = fn()
        doc, im, used = rig.doc()
        dims, _mn, _mx = rig.span()
        print("%-16s cubos=%-3d ossos=%-3d  %.2f x %.2f x %.2f bl  atlas ate y=%d/%d"
              % (rig.name, len(rig.parts), len(rig.order), dims[0], dims[1], dims[2],
                 used, rig.res))
        if args.preview:
            continue
        d = os.path.join(OUT_DIR, folder)
        os.makedirs(d, exist_ok=True)
        with open(os.path.join(d, rig.name + ".bbmodel"), "w", encoding="utf-8") as f:
            json.dump(doc, f, separators=(",", ":"), ensure_ascii=False)
        im.save(os.path.join(d, rig.tex_name))
        print("   gravado em Infection entities/%s/" % folder)


if __name__ == "__main__":
    raise SystemExit(main())
