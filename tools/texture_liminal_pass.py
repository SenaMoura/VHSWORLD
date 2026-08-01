# -*- coding: utf-8 -*-
"""Paleta de horror liminar + dithering nas texturas de vhsworldentities.

Duas operacoes:

  GRADE     - mapeia a LUMINANCIA de cada pixel para uma rampa da paleta, mantendo
              o desenho e o alfa. Pixel quase preto NAO e tocado: sete texturas do
              lote sao pretas de proposito (a Inverted_Silhoutte tem uma cor so) e
              a lei do mod e "a corrupcao nao tem cor, ela TIRA a cor". A familia
              de cor de cada pixel decide a rampa: vermelho -> sangue coagulado,
              amarelo -> amarelo esteril, saturado -> carne morta, cinza -> concreto.

  BASECOAT  - para as duas texturas que sao PLACEHOLDER do Blockbench
              (Crawler_void, Shade_segment: marcadores verde-neon/oliva), pinta uma
              base lendo o UV de cada face do proprio modelo: cor por altura da
              peca, face de cima mais clara, de baixo mais escura, contorno cravado
              e corrosao pontilhada. Nao substitui pintura a mao - da um ponto de
              partida no lugar do marcador.

    python tools/texture_liminal_pass.py --dry-run
    python tools/texture_liminal_pass.py
"""

import argparse
import base64
import io
import json
import os
import shutil

from PIL import Image

ROOT = r"C:\Users\Hamilton\Downloads\vhsworldentities"
BACKUP = ROOT + "_BACKUP_tex"

# ------------------------------------------------------------------ paleta

BLACK_FLOOR = 10          # abaixo disto o pixel fica exatamente como esta

FLESH = [(26, 20, 20), (58, 44, 42), (96, 74, 68), (138, 106, 96), (176, 145, 132)]
CONCRETE = [(14, 14, 15), (38, 39, 41), (68, 70, 74), (104, 107, 112), (146, 149, 154)]
BLOOD = [(24, 6, 7), (58, 14, 15), (98, 24, 22), (140, 40, 32), (176, 68, 54)]
STERILE = [(26, 24, 12), (62, 58, 26), (104, 97, 44), (150, 140, 66), (196, 184, 96)]

BAYER = [[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]]


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def ramp_at(ramp, t):
    t = max(0.0, min(1.0, t)) * (len(ramp) - 1)
    i = int(t)
    if i >= len(ramp) - 1:
        return ramp[-1]
    f = t - i
    a, b = ramp[i], ramp[i + 1]
    return tuple(int(round(a[k] + (b[k] - a[k]) * f)) for k in range(3))


def pick_ramp(c):
    r, g, b = c[:3]
    mx, mn = max(r, g, b), min(r, g, b)
    sat = mx - mn
    if sat < 26:
        return CONCRETE
    if r >= g and r - b > 18 and r - g > 18:
        return BLOOD
    if r > b + 25 and g > b + 25:
        return STERILE
    return FLESH


def grade(im, strength=0.85, dither=7):
    im = im.convert("RGBA")
    w, h = im.size
    px = im.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            L = lum((r, g, b))
            if L < BLACK_FLOOR:
                continue                      # silhueta preta continua preta
            t = L / 255.0
            # dithering ordenado, so no meio-tom: nas pontas viraria sujeira
            edge = min(t, 1.0 - t) * 2.0
            t += ((BAYER[y % 4][x % 4] - 7.5) / 15.0) * (dither / 255.0) * edge
            tgt = ramp_at(pick_ramp((r, g, b)), t)
            px[x, y] = (int(round(r + (tgt[0] - r) * strength)),
                        int(round(g + (tgt[1] - g) * strength)),
                        int(round(b + (tgt[2] - b) * strength)), a)
    return im


# ---------------------------------------------------------------- basecoat

FACE_SHADE = {"up": 1.28, "down": 0.55, "north": 1.0, "south": 0.86,
              "east": 0.93, "west": 0.79}


def basecoat(doc, im):
    """Pinta a base lendo o UV das faces do modelo (so p/ box_uv)."""
    res = doc.get("resolution", {})
    sx = im.size[0] / float(res.get("width") or im.size[0])
    sy = im.size[1] / float(res.get("height") or im.size[1])
    els = [e for e in doc.get("elements", []) if e.get("type", "cube") == "cube"]
    if not els:
        return im, 0
    ys = [min(e["from"][1], e["to"][1]) for e in els] + [max(e["from"][1], e["to"][1]) for e in els]
    lo, hi = min(ys), max(ys)
    px = im.load()
    painted = 0
    for e in els:
        cy = (e["from"][1] + e["to"][1]) / 2.0
        t = 0.0 if hi == lo else (cy - lo) / float(hi - lo)
        # pe = concreto sujo, tronco/cabeca = carne morta
        ramp = CONCRETE if t < 0.38 else FLESH
        for name, face in (e.get("faces") or {}).items():
            uv = face.get("uv")
            if not uv or face.get("texture") is None:
                continue
            x1, x2 = sorted((uv[0] * sx, uv[2] * sx))
            y1, y2 = sorted((uv[1] * sy, uv[3] * sy))
            x1, y1, x2, y2 = int(round(x1)), int(round(y1)), int(round(x2)), int(round(y2))
            if x2 <= x1 or y2 <= y1:
                continue
            shade = FACE_SHADE.get(name, 1.0)
            for y in range(max(0, y1), min(im.size[1], y2)):
                for x in range(max(0, x1), min(im.size[0], x2)):
                    fy = (y - y1) / float(max(1, y2 - y1 - 1))
                    v = 0.34 + 0.42 * t + 0.30 * (1.0 - fy)      # topo da face mais claro
                    v *= shade
                    v += ((BAYER[y % 4][x % 4] - 7.5) / 15.0) * 0.10
                    n = ((x * 73856093) ^ (y * 19349663)) & 0xFF
                    if n < 14:
                        v -= 0.18                                 # corrosao
                    elif n > 246:
                        v += 0.10
                    if x in (x1, x2 - 1) or y in (y1, y2 - 1):
                        v -= 0.28                                 # contorno cravado
                    c = ramp_at(ramp, v)
                    px[x, y] = (c[0], c[1], c[2], 255)
            painted += 1
    return im, painted


# -------------------------------------------------------------------- main

PLACEHOLDERS = {"Crawler_void.png", "Shade_segment.png"}


def load_embedded(tex):
    src = tex.get("source", "")
    if not src.startswith("data:"):
        return None
    return Image.open(io.BytesIO(base64.b64decode(src.split(",", 1)[1]))).convert("RGBA")


def save_embedded(tex, im):
    buf = io.BytesIO()
    im.save(buf, format="PNG")
    tex["source"] = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--only", default=None)
    ap.add_argument("--strength", type=float, default=0.85)
    args = ap.parse_args()

    if not args.dry_run and not os.path.isdir(BACKUP):
        os.makedirs(BACKUP)
        for dp, _d, fs in os.walk(ROOT):
            for f in fs:
                if f.lower().endswith(".png"):
                    rel = os.path.relpath(os.path.join(dp, f), ROOT)
                    dst = os.path.join(BACKUP, rel)
                    os.makedirs(os.path.dirname(dst), exist_ok=True)
                    shutil.copy2(os.path.join(dp, f), dst)
        print("backup dos PNG: %s" % BACKUP)

    files = []
    for dp, _d, fs in os.walk(ROOT):
        for f in fs:
            if f.lower().endswith(".bbmodel"):
                files.append(os.path.join(dp, f))
    files.sort()

    for p in files:
        rel = os.path.relpath(p, ROOT)
        if args.only and args.only.lower() not in rel.lower():
            continue
        with open(p, "r", encoding="utf-8") as f:
            doc = json.load(f)
        texs = doc.get("textures", [])
        if not texs:
            print("%-58s sem textura" % rel)
            continue
        notes = []
        for tex in texs:
            im = load_embedded(tex)
            if im is None:
                notes.append("%s: textura nao embutida, pulada" % tex.get("name"))
                continue
            name = tex.get("name", "")
            base = 0
            if name in PLACEHOLDERS:
                im, base = basecoat(doc, im)
            im = grade(im, strength=args.strength)
            if not args.dry_run:
                save_embedded(tex, im)
                side = os.path.join(os.path.dirname(p), name)
                if os.path.isfile(side):
                    im.save(side)
            notes.append("%s %dx%d%s" % (name, im.size[0], im.size[1],
                                         " BASECOAT %d faces" % base if base else ""))
        if not args.dry_run:
            with open(p, "w", encoding="utf-8") as f:
                json.dump(doc, f, separators=(",", ":"), ensure_ascii=False)
        print("%-58s %s" % (rel, " | ".join(notes)))


if __name__ == "__main__":
    raise SystemExit(main())
