"""Desenha a geometria numa PNG, para VER o bicho antes de programar nele.

Existe por causa do Crawler_void: a medicao acusou 11 blocos de largura num bicho de
3,7, e nao dava para decidir se o culpado era o export, a conta ou o modelo sem
olhar. (Era a conta — um math.radians a mais.) Numero nao resolve discussao de forma;
imagem resolve. Serve tanto para o .java exportado quanto para o gerado.

Nao e um render bonito — e projecao ortografica com o algoritmo do pintor, cara por
cara, tom pela normal. Serve para ler silhueta, proporcao e membro fora do lugar.

Uso: python tools/preview_entity_model.py [arquivo.java ...]
Saida: <scratchpad>/<nome>.png  (frente, lado e cima lado a lado)
"""
import math
import os
import sys

from PIL import Image, ImageDraw

import bbexport

SRC = r"C:\Users\Hamilton\Downloads\vhsworldentities"
OUT = (r"C:\Users\Hamilton\AppData\Local\Temp\claude\C--Users-Hamilton"
       r"\abe3639e-2138-4574-81be-607b25dc03d1\scratchpad")

DEFAULT = [
    r"Static_Watcher\Static_Watcher.java",
    r"Shade_Segment\Shade_Segment.java",
    r"Inverted_silhoutte\Inverted_silhoutte.java",
    r"Crawler_void\Crawler_Void.java",
]

# as 6 faces de um cubo, em indices dos 8 cantos
FACES = [(0, 1, 3, 2), (4, 6, 7, 5), (0, 4, 5, 1),
         (2, 3, 7, 6), (0, 2, 6, 4), (1, 5, 7, 3)]


def quads(path):
    """Todos os quadrilateros do modelo, ja transformados, em espaco de entidade."""
    tree = bbexport.parts(path)
    out = []

    def walk(var, mat, org):
        p = tree[var]
        org = [org[i] + bbexport._apply(mat, p["off"])[i] for i in range(3)]
        # ⚠️ o angulo do .java ja esta em radianos — converter de novo encolhe o giro
        mat = bbexport._mul(mat, bbexport._mat_rot(*p["rot"]))

        for f, t in p["boxes"]:
            pts = []
            for i in range(8):
                c = [t[0] if i & 1 else f[0],
                     t[1] if i & 2 else f[1],
                     t[2] if i & 4 else f[2]]
                c = bbexport._apply(mat, c)
                pts.append([c[k] + org[k] for k in range(3)])
            for face in FACES:
                out.append([pts[i] for i in face])

        for child in p["children"]:
            walk(child, mat, org)

    ident = [[1, 0, 0], [0, 1, 0], [0, 0, 1]]
    for var in tree["__roots__"]:
        walk(var, ident, [0.0, 0.0, 0.0])
    return out


def normal(q):
    a = [q[1][i] - q[0][i] for i in range(3)]
    b = [q[2][i] - q[0][i] for i in range(3)]
    n = [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]
    ln = math.sqrt(sum(v * v for v in n)) or 1.0
    return [v / ln for v in n]


def view(qs, axes, depth, flip, size, mn, mx, title):
    """Uma projecao ortografica. axes = (i, j) do plano; depth = eixo da ordenacao."""
    img = Image.new("RGB", (size, size), (18, 18, 22))
    d = ImageDraw.Draw(img)

    span = max(mx[k] - mn[k] for k in range(3)) or 1.0
    scale = (size - 40) / span
    cx = (mn[axes[0]] + mx[axes[0]]) / 2.0
    cy = (mn[axes[1]] + mx[axes[1]]) / 2.0

    def proj(p):
        x = (p[axes[0]] - cx) * scale * (flip[0]) + size / 2
        y = (p[axes[1]] - cy) * scale * (flip[1]) + size / 2
        return (x, y)

    # pintor: o mais longe primeiro
    order = sorted(qs, key=lambda q: -sum(p[depth] for p in q) / 4.0 * flip[2])
    light = [0.4, -0.7, -0.55]
    for q in order:
        n = normal(q)
        lam = max(0.15, min(1.0, sum(n[i] * light[i] for i in range(3)) * 0.6 + 0.5))
        tone = int(40 + 165 * lam)
        d.polygon([proj(p) for p in q], fill=(tone, tone, int(tone * 1.05)),
                  outline=(12, 12, 14))

    # o chao fica em y=24, nao em y=0: o renderer de entidade translada 1,5 bloco
    # antes de desenhar, entao o pe do modelo cai no 24. A regua marca 1 bloco.
    y0 = proj([0, 24, 0])[1] if axes[1] == 1 else None
    if y0 is not None:
        d.line([(0, y0), (size, y0)], fill=(200, 60, 60), width=1)
        step = 16 * scale
        for k in range(1, 12):
            yy = y0 - k * step
            if yy < 0:
                break
            d.line([(0, yy), (14, yy)], fill=(90, 90, 110), width=1)
    d.text((6, 6), title, fill=(230, 230, 240))
    return img


def main(files):
    os.makedirs(OUT, exist_ok=True)
    for f in files:
        path = f if os.path.isabs(f) else os.path.join(SRC, f)
        qs = quads(path)
        pts = [p for q in qs for p in q]
        mn = [min(p[i] for p in pts) for i in range(3)]
        mx = [max(p[i] for p in pts) for i in range(3)]

        size = 420
        views = [
            view(qs, (0, 1), 2, (1, 1, -1), size, mn, mx, "frente (-Z)"),
            view(qs, (2, 1), 0, (1, 1, 1), size, mn, mx, "lado (+X)"),
            view(qs, (0, 2), 1, (1, 1, 1), size, mn, mx, "cima"),
        ]
        sheet = Image.new("RGB", (size * 3, size), (10, 10, 12))
        for i, v in enumerate(views):
            sheet.paste(v, (i * size, 0))

        name = os.path.splitext(os.path.basename(path))[0]
        dest = os.path.join(OUT, name + "_preview.png")
        sheet.save(dest)
        print("%s  ->  %s" % (name, dest))
        print("   %.2f x %.2f x %.2f blocos"
              % ((mx[0] - mn[0]) / 16, (mx[1] - mn[1]) / 16, (mx[2] - mn[2]) / 16))


if __name__ == "__main__":
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    main(sys.argv[1:] or DEFAULT)
