"""Gerador da familia FRACTURE: 3 ferramentas + 4 icones de armadura + as camadas
vestidas (layer_1 / layer_2), todas animadas em 8 quadros na paleta do fracture.png."""
import math, os
from PIL import Image

RES = r"C:\Users\Hamilton\Downloads\GitHub\VHSWORLD\src\main\resources\assets\recmod"
TEX = os.path.join(RES, "textures", "item")
ARMDIR = os.path.join(RES, "textures", "models", "armor")
os.makedirs(ARMDIR, exist_ok=True)

FRAMES = 8

# Paleta lida do fracture.png (mesma rampa, mesma ordem)
RAMP = [
    (86, 14, 56, 150),     # 0 penumbra
    (156, 28, 96, 220),    # 1 borda
    (232, 72, 150, 255),   # 2 corpo
    (255, 176, 214, 255),  # 3 realce
    (255, 255, 255, 255),  # 4 nucleo
]
# Haste: mesma rampa de metal do iron_stick
IRON = [(24, 24, 28, 255), (78, 82, 90, 255), (128, 133, 143, 255),
        (176, 181, 190, 255), (214, 218, 224, 255)]


def wave(x, y, f, scale=9.0, amp=1.15):
    """Onda de brilho que atravessa a peca na diagonal, uma volta a cada ciclo."""
    return math.sin(((x + y) / scale - f / FRAMES) * 2 * math.pi) * amp


def energy(level, x, y, f, amp=1.15):
    i = int(round(max(0.0, min(4.0, level + wave(x, y, f, amp=amp)))))
    return RAMP[i]


# ---------------------------------------------------------------- ferramentas
def tool_from(src_name, out_name):
    """Repinta a ferramenta corrompida: haste vira ferro, cabeca vira energia."""
    src = Image.open(os.path.join(TEX, src_name)).convert("RGBA")
    sheet = Image.new("RGBA", (16, 16 * FRAMES), (0, 0, 0, 0))
    for f in range(FRAMES):
        for y in range(16):
            for x in range(16):
                r, g, b, a = src.getpixel((x, y))
                if a == 0:
                    continue
                lum = (r + g + b) / 3.0
                if r > b + 6:                        # madeira -> haste de ferro
                    lvl = max(0.0, min(4.0, (lum - 20) / 22.0))
                    px = IRON[int(round(lvl))]
                else:                                 # cabeca -> energia
                    px = energy((lum - 12) / 28.0, x, y, f)
                sheet.putpixel((x, y + f * 16), px)
    sheet.save(os.path.join(TEX, out_name + ".png"))


# --------------------------------------------------------- icones de armadura
# 'o' = contorno (escuro, quase parado)   2..4 = interior, onde a luz anda
HELMET = """
................
................
....oooooooo....
...o22222222o...
..o2233333322o..
..o23oooooo32o..
..o23o....o32o..
..o23o....o32o..
..o233o..o332o..
..o2233oo3322o..
..o22o....o22o..
..ooo......ooo..
................
................
................
................
"""

CHEST = """
................
................
..oooo....oooo..
.o2222oooo2222o.
.o222222222222o.
.o223333333322o.
.o233333333332o.
.o233333333332o.
.o223333333322o.
.o222222222222o.
..o2222222222o..
..o22o....o22o..
..o22o....o22o..
...oo......oo...
................
................
"""

LEGGINGS = """
................
................
................
..oooooooooo....
.o2222222222o...
.o2233333322o...
.o22o....o22o...
.o22o....o22o...
.o22o....o22o...
.o22o....o22o...
.o22o....o22o...
..oo......oo....
................
................
................
................
"""

BOOTS = """
................
................
................
...oooo...oooo..
...o33o...o33o..
...o33o...o33o..
..oo33oo.oo33oo.
..o3333o.o3333o.
..o3333o.o3333o.
..o2222o.o2222o.
..oooooo.oooooo.
................
................
................
................
................
"""


def icon(grid, name):
    """O contorno fica; so o interior pulsa. Sem isso a onda come a silhueta."""
    rows = grid.strip("\n").split("\n")
    sheet = Image.new("RGBA", (16, 16 * FRAMES), (0, 0, 0, 0))
    for f in range(FRAMES):
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch == ".":
                    continue
                if ch == "o":
                    px = energy(0.45, x, y, f, amp=0.35)
                else:
                    px = energy(float(ch) - 0.4, x, y, f, amp=0.9)
                sheet.putpixel((x, y + f * 16), px)
    sheet.save(os.path.join(TEX, name + ".png"))


# ------------------------------------------------------ camadas vestidas 64x32
# Retangulos de UV do HumanoidModel padrao (x0, y0, x1, y1)
HEAD = dict(top=(8, 0, 16, 8), bottom=(16, 0, 24, 8), right=(0, 8, 8, 16),
            front=(8, 8, 16, 16), left=(16, 8, 24, 16), back=(24, 8, 32, 16))
BODY = dict(top=(20, 16, 28, 20), bottom=(28, 16, 36, 20), right=(16, 20, 20, 32),
            front=(20, 20, 28, 32), left=(28, 20, 32, 32), back=(32, 20, 40, 32))
ARMUV = dict(top=(44, 16, 48, 20), bottom=(48, 16, 52, 20), right=(40, 20, 44, 32),
             front=(44, 20, 48, 32), left=(48, 20, 52, 32), back=(52, 20, 56, 32))
LEG = dict(top=(4, 16, 8, 20), bottom=(8, 16, 12, 20), right=(0, 20, 4, 32),
           front=(4, 20, 8, 32), left=(8, 20, 12, 32), back=(12, 20, 16, 32))


def paint(img, box, f, rows=None, cols=None, level=2.0, edge=True):
    """Pinta uma face inteira ou so as linhas/colunas pedidas (j=0 e o topo da face)."""
    x0, y0, x1, y1 = box
    w, h = x1 - x0, y1 - y0
    sel_rows = list(range(h)) if rows is None else list(rows)
    sel_cols = list(range(w)) if cols is None else list(cols)
    for j in sel_rows:
        for i in sel_cols:
            lvl = level
            if edge and (i == 0 or i == w - 1 or j == sel_rows[0] or j == sel_rows[-1]):
                lvl = level - 1.2
            img.putpixel((x0 + i, y0 + j), energy(lvl, x0 + i, y0 + j, f, amp=0.85))


def seam(img, box, f, rows=(), cols=()):
    """Vinco escuro: sem isto a placa vira um degrade liso, sem leitura de armadura."""
    x0, y0, x1, y1 = box
    for j in rows:
        for i in range(x1 - x0):
            img.putpixel((x0 + i, y0 + j), energy(0.8, x0 + i, y0 + j, f, amp=0.5))
    for i in cols:
        for j in range(y1 - y0):
            img.putpixel((x0 + i, y0 + j), energy(0.8, x0 + i, y0 + j, f, amp=0.5))


def clear(img, box, rows=None, cols=None):
    x0, y0, x1, y1 = box
    for j in (range(y1 - y0) if rows is None else rows):
        for i in (range(x1 - x0) if cols is None else cols):
            img.putpixel((x0 + i, y0 + j), (0, 0, 0, 0))


def layer1(f):
    """Elmo (cabeca) + peitoral (tronco e bracos) + botas (parte baixa das pernas)."""
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    # --- ELMO: coroa fechada, rosto aberto ---
    paint(im, HEAD["top"], f, level=2.6)                        # calota inteira
    paint(im, HEAD["back"], f, rows=range(0, 6), level=2.1)     # nuca
    paint(im, HEAD["right"], f, rows=range(0, 5), level=2.1)    # laterais ate a orelha
    paint(im, HEAD["left"], f, rows=range(0, 5), level=2.1)
    paint(im, HEAD["front"], f, rows=range(0, 2), level=2.6)    # testa
    # temporas: duas tiras finas descendo; o meio (olhos/boca) fica aberto
    paint(im, HEAD["front"], f, rows=range(2, 5), cols=(0, 7), level=2.0, edge=False)

    # --- PEITORAL: tronco inteiro + bracos ate o cotovelo ---
    paint(im, BODY["top"], f, level=2.3)
    clear(im, BODY["top"], rows=(1, 2), cols=(3, 4))            # buraco do pescoco
    paint(im, BODY["bottom"], f, level=1.6)
    paint(im, BODY["front"], f, level=2.5)
    paint(im, BODY["back"], f, level=2.1)
    paint(im, BODY["right"], f, level=2.1)
    paint(im, BODY["left"], f, level=2.1)
    for k in ("front", "back", "right", "left"):                # manga ate o cotovelo
        paint(im, ARMUV[k], f, rows=range(0, 7), level=2.3)
    paint(im, ARMUV["top"], f, level=2.3)                       # ombreira
    seam(im, BODY["front"], f, rows=(4, 9))                     # peitoral e cinturao
    seam(im, BODY["back"], f, rows=(4, 9))
    for k in ("front", "back", "right", "left"):
        seam(im, ARMUV[k], f, rows=(3,))                        # gomo do braco

    # --- BOTAS: so da canela para baixo + sola ---
    for k in ("front", "back", "right", "left"):
        paint(im, LEG[k], f, rows=range(8, 12), level=2.3)
        seam(im, LEG[k], f, rows=(10,))                         # cano do pe
    paint(im, LEG["bottom"], f, level=1.7)
    return im


def layer2(f):
    """Calcas: cinto na base do tronco + pernas ate o tornozelo."""
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for k in ("front", "back", "right", "left"):
        paint(im, BODY[k], f, rows=range(7, 12), level=2.1)
    paint(im, BODY["bottom"], f, level=1.9)
    for k in ("front", "back", "right", "left"):
        paint(im, LEG[k], f, rows=range(0, 9), level=2.3)
        seam(im, LEG[k], f, rows=(4,))                          # joelheira
    paint(im, LEG["top"], f, level=1.9)
    return im


MCMETA = '{\n  "animation": {\n    "frametime": 2,\n    "interpolate": true\n  }\n}\n'

ITEMS = ("fracture_pickaxe", "fracture_axe", "fracture_shovel", "fracture_helmet",
         "fracture_chestplate", "fracture_leggings", "fracture_boots")

if __name__ == "__main__":
    tool_from("corrupted_pickaxe.png", "fracture_pickaxe")
    tool_from("corrupted_axe.png", "fracture_axe")
    tool_from("corrupted_shovel.png", "fracture_shovel")
    icon(HELMET, "fracture_helmet")
    icon(CHEST, "fracture_chestplate")
    icon(LEGGINGS, "fracture_leggings")
    icon(BOOTS, "fracture_boots")
    for n in ITEMS:
        with open(os.path.join(TEX, n + ".png.mcmeta"), "w") as fh:
            fh.write(MCMETA)
    for f in range(FRAMES):
        layer1(f).save(os.path.join(ARMDIR, f"fracture_layer_1_{f}.png"))
        layer2(f).save(os.path.join(ARMDIR, f"fracture_layer_2_{f}.png"))
    print("ok: 7 sprites 16x128 + 16 camadas 64x32")
