"""
Desenha o .smesh do Escutador COM SKINNING, fora do jogo.

    python tools/preview_listener_smesh.py [repouso|ouvindo|andando]

⚠️ ESTE ARQUIVO NAO E SO UM VISUALIZADOR — ele e a conferencia da CONTA do skinning. A
formula (matriz do osso agora × inversa da matriz de repouso, ponderada pelos pesos) e a
mesma que o Java vai fazer; se a criatura sair torcida aqui, sai torcida la, e descobrir isso
aqui custa dez segundos em vez de um ciclo inteiro de build, jogo, procurar o bicho no escuro.
"""

import math
import os
import struct
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import listener_rig as RIG  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MESH = os.path.join(ROOT, "src", "main", "resources", "assets", "recmod",
                    "meshes", "listener.smesh")
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_preview_listener.png")


def read(path):
    with open(path, "rb") as f:
        assert f.read(4) == b"RECS"
        struct.unpack("B", f.read(1))

        bone_count = struct.unpack(">H", f.read(2))[0]
        bones = []
        for _ in range(bone_count):
            n = struct.unpack("B", f.read(1))[0]
            bones.append(f.read(n).decode("utf-8"))

        vcount = struct.unpack(">I", f.read(4))[0]
        verts = []
        for _ in range(vcount):
            x, y, z = struct.unpack(">fff", f.read(12))
            nx, ny, nz = struct.unpack("bbb", f.read(3))
            u, v = struct.unpack(">ff", f.read(8))
            cr, cg, cb = struct.unpack("BBB", f.read(3))
            w = [struct.unpack(">Hf", f.read(6)) for _ in range(4)]
            verts.append(((x, y, z), (nx / 127.0, ny / 127.0, nz / 127.0), (u, v),
                          (cr, cg, cb), w))

        tcount = struct.unpack(">I", f.read(4))[0]
        tris = [struct.unpack(">III", f.read(12)) for _ in range(tcount)]

    return bones, verts, tris


def pose_for(name):
    """As poses de teste. Somadas ao repouso, como o Java faz."""
    if name == "ouvindo":
        return {"jaw": (0.85, 0.0, 0.0), "head": (0.10, 0.35, 0.0), "spine": (0.06, 0.0, 0.0)}
    if name == "andando":
        return {
            "front_upper_right": (0.35, 0.0, 0.0), "front_upper_left": (-0.35, 0.0, 0.0),
            "front_fore_right": (0.30, 0.0, 0.0),
            "back_thigh_right": (-0.30, 0.0, 0.0), "back_thigh_left": (0.30, 0.0, 0.0),
            "jaw": (0.25, 0.0, 0.0),
        }
    return {}


def skin_matrices(pose):
    """
    ⚠️ A CONTA DO SKINNING: para cada osso, `agora × repouso⁻¹`.

    O vertice esta guardado em espaco de MODELO na pose de repouso, entao a inversa o leva
    para o espaco do osso e a matriz atual o traz de volta ja deformado. Esquecer a inversa e
    o erro classico — a criatura explode para longe da origem, e a tentacao e "consertar" o
    rig, que estava certo.
    """
    bind = RIG.frames()
    now = RIG.frames(pose)
    return {name: RIG.multiply(now[name], RIG.invert_rigid(bind[name])) for name in bind}


def deform(bones, verts, mats):
    out = []
    for (co, normal, uv, color, weights) in verts:
        px = py = pz = 0.0
        nx = ny = nz = 0.0
        total = 0.0
        for (index, w) in weights:
            if w <= 0.0 or index >= len(bones):
                continue
            m = mats.get(bones[index])
            if m is None:
                continue
            p = RIG.apply(m, co)
            n = RIG.apply_dir(m, normal)
            px += p[0] * w
            py += p[1] * w
            pz += p[2] * w
            nx += n[0] * w
            ny += n[1] * w
            nz += n[2] * w
            total += w
        if total <= 0.0:
            px, py, pz = co
            nx, ny, nz = normal
        out.append(((px, py, pz), (nx, ny, nz), color))
    return out


def project(p, yaw, pitch, scale, cx, cy):
    x, y, z = p
    y = 24.0 - y
    c, s = math.cos(yaw), math.sin(yaw)
    x, z = x * c + z * s, -x * s + z * c
    c, s = math.cos(pitch), math.sin(pitch)
    y, z = y * c - z * s, y * s + z * c
    return (cx + x * scale, cy - y * scale, z)


def draw(verts, tris, path, label, size=(760, 500)):
    img = Image.new("RGB", size, (14, 14, 16))
    d = ImageDraw.Draw(img)
    cx, cy, scale = size[0] / 2, size[1] * 0.84, 11.5
    d.line([(0, cy), (size[0], cy)], fill=(52, 52, 58))

    yaw, pitch = math.radians(38), math.radians(14)
    polys = []
    for (a, b, c) in tris:
        pa = project(verts[a][0], yaw, pitch, scale, cx, cy)
        pb = project(verts[b][0], yaw, pitch, scale, cx, cy)
        pc = project(verts[c][0], yaw, pitch, scale, cx, cy)
        depth = (pa[2] + pb[2] + pc[2]) / 3.0

        n = verts[a][1]
        lit = 0.35 + 0.65 * max(0.0, -n[1] * 0.75 + n[2] * 0.25 + 0.3)
        base = verts[a][2]
        color = tuple(int(min(255, ch * lit)) for ch in base)
        polys.append((depth, [(pa[0], pa[1]), (pb[0], pb[1]), (pc[0], pc[1])], color))

    polys.sort(key=lambda p: -p[0])
    for _, poly, color in polys:
        d.polygon(poly, fill=color)

    d.text((10, 10), "ESCUTADOR - malha unica esculpida - " + label, fill=(180, 180, 180))
    img.save(path)
    print("escrito:", path)


def main():
    which = sys.argv[1] if len(sys.argv) > 1 else "repouso"
    bones, verts, tris = read(MESH)
    mats = skin_matrices(pose_for(which))
    posed = deform(bones, verts, mats)
    draw(posed, tris, OUT, "%s (%d tris, %d ossos)" % (which, len(tris), len(bones)))


if __name__ == "__main__":
    main()
