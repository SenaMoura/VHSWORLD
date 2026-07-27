"""
Empacota os quadros renderizados numa FOLHA por criatura, para o mod.

    python tools/pack_sprites.py

Layout: COLUNA = angulo (o jogador andando em volta), LINHA = quadro da animacao.
O renderer no jogo escolhe a celula por dois numeros — o angulo entre a camera e a
criatura, e o tempo — e desenha so aquele pedaco da textura.

⚠️ O RECORTE E COMUM A TODOS OS QUADROS, nunca por quadro. Se cada quadro fosse
recortado no seu proprio conteudo, a criatura "pularia" de tamanho e de posicao a cada
troca de quadro e a cada passo que o jogador desse em volta dela. Uma unica caixa,
calculada sobre TODOS os quadros, mantem ela plantada no chao.
"""

import os

from PIL import Image

SRC = r"C:\Users\Hamilton\Downloads\vhsworldentities\3d\_sprites"
DEST = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "recmod",
                    "textures", "entity", "anomaly")

# lado da celula na folha final, em pixels
CELL = 128

# Quem esta NO JOGO. As outras esculturas continuam renderizadas em _sprites (custou
# caro para chegar la), mas nao viram textura do mod — sem esta lista, rodar o script
# de novo ressuscitaria as quatro que o Pedro mandou tirar.
KEEP = {"greyface", "ophanim"}


def frames_of(folder):
    """{(quadro, angulo): caminho} a partir dos nomes fXX_aYY.png."""
    out = {}
    for name in sorted(os.listdir(folder)):
        if not name.startswith("f") or not name.endswith(".png"):
            continue
        try:
            f = int(name[1:3])
            a = int(name[5:7])
        except ValueError:
            continue
        out[(f, a)] = os.path.join(folder, name)
    return out


def common_box(paths):
    """A caixa que cabe a criatura em TODOS os quadros."""
    box = None
    for p in paths:
        bb = Image.open(p).convert("RGBA").getbbox()
        if bb is None:
            continue
        box = bb if box is None else (min(box[0], bb[0]), min(box[1], bb[1]),
                                      max(box[2], bb[2]), max(box[3], bb[3]))
    return box


def pack(nick, folder):
    cells = frames_of(folder)
    if not cells:
        print(f"  !! {nick}: sem quadros")
        return None

    frames = max(f for f, _ in cells) + 1
    angles = max(a for _, a in cells) + 1

    box = common_box(cells.values())
    if box is None:
        print(f"  !! {nick}: quadros vazios")
        return None

    # Quadrado, para a criatura nao esticar quando o jogo desenhar um quad quadrado.
    w, h = box[2] - box[0], box[3] - box[1]
    side = max(w, h)
    cx, cy = (box[0] + box[2]) // 2, (box[1] + box[3]) // 2
    box = (cx - side // 2, cy - side // 2, cx + side // 2, cy + side // 2)

    sheet = Image.new("RGBA", (CELL * angles, CELL * frames), (0, 0, 0, 0))
    for (f, a), path in cells.items():
        im = Image.open(path).convert("RGBA").crop(box).resize((CELL, CELL), Image.LANCZOS)
        sheet.paste(im, (a * CELL, f * CELL))

    os.makedirs(DEST, exist_ok=True)
    sheet.save(os.path.join(DEST, nick + ".png"))
    print(f"  {nick:16s} {angles} angulos x {frames} quadros -> {sheet.width}x{sheet.height}")
    return angles, frames


def main():
    print("folhas:")
    for entry in sorted(os.listdir(SRC)):
        folder = os.path.join(SRC, entry)
        # so as pastas de quadros (fXX_aYY), nao as de teste de voxel
        if not os.path.isdir(folder) or entry not in KEEP:
            continue
        pack(entry, folder)


if __name__ == "__main__":
    main()
