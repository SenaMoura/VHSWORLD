"""
Confere os pesos do .smesh: quem manda em cada vertice, e onde ele esta.

    python tools/check_listener_weights.py

⚠️ Escrito para um defeito concreto: na pose de caminhada, dente e garra ESTICAM e explodem.
Peca com peso 1 num osso so nao deforma — entao, se ela deforma, o peso nao esta onde eu
acho que esta. Isto responde essa pergunta olhando o arquivo, em vez de deduzir.
"""

import os
import struct
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import listener_rig as RIG  # noqa: E402

MESH = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "recmod",
                    "meshes", "listener.smesh")


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
            f.read(3)
            f.read(8)
            f.read(3)
            w = [struct.unpack(">Hf", f.read(6)) for _ in range(4)]
            verts.append(((x, y, z), w))
    return bones, verts


def main():
    bones, verts = read(MESH)
    print("ossos no arquivo (%d):" % len(bones))
    for i, b in enumerate(bones):
        print("  %2d %s" % (i, b))

    frames = RIG.frames()

    # Para cada vertice: qual osso manda, e a que distancia ele esta do OSSO que o comanda.
    # Vertice longe do proprio osso e o que estica quando o osso gira.
    worst = []
    counts = {}
    unweighted = 0

    for (co, weights) in verts:
        total = sum(w for _, w in weights)
        if total <= 0.0001:
            unweighted += 1
            continue

        index, weight = max(weights, key=lambda w: w[1])
        name = bones[index] if index < len(bones) else "???"
        counts[name] = counts.get(name, 0) + 1

        origin = RIG.apply(frames.get(name, RIG.identity()), (0.0, 0.0, 0.0))
        d = sum((co[k] - origin[k]) ** 2 for k in range(3)) ** 0.5
        worst.append((d, name, co, weight))

    print("\nvertices por osso dominante:")
    for name in sorted(counts, key=lambda n: -counts[n]):
        print("  %-22s %4d" % (name, counts[name]))

    print("\nsem peso nenhum: %d" % unweighted)

    worst.sort(key=lambda x: -x[0])
    print("\nos 12 vertices MAIS LONGE do proprio osso (candidatos a esticar):")
    for (d, name, co, weight) in worst[:12]:
        print("  %6.1f px  %-22s peso %.2f  em (%.1f, %.1f, %.1f)"
              % (d, name, weight, co[0], co[1], co[2]))


if __name__ == "__main__":
    main()
