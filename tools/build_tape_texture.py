# -*- coding: utf-8 -*-
"""A textura 16x16 da fita de uma dimensao.

As fitas sao todas o MESMO cassete: mesma moldura preta, mesmos dois carreteis. O que
muda de uma para outra e a faixa da ETIQUETA, em cima — duas cores e um respingo. E o
unico lugar em que o mod deixa entrar cor, e ele e pequeno de proposito: a paleta do
VHSWORLD e preto, chumbo, cinza e moldura branca, e a corrupcao nao tem cor propria,
ela TIRA a cor.

Ja existem:
    DATA   verde de terminal  (122,196,132)
    CHUNKS madeira das pontes (176,134,88)

Rodar:
    python tools/build_tape_texture.py
"""
import os

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(REPO, "src", "main", "resources", "assets", "recmod", "textures", "item")

# O corpo do cassete, igual em todas. '.' e transparente.
BODY = [
    "................",
    "................",
    "................",
    "................",
    "...OOOOOOOOOO...",
    "..OLLLLLLLLLLO..",
    "..OLLLLLLLLLLO..",
    "..OSSSSSSSSSSO..",
    "..OSSRRRSRRRSO..",
    "..OSSRHRSRHRSO..",
    "..OSSRRRSRRRSO..",
    "..OSSSSSSSSSSO..",
    "...OOOOOOOOOO...",
    "................",
    "................",
    "................",
]

OUTLINE = (10, 10, 12, 255)      # O — a moldura preta
SHELL = (16, 16, 20, 255)        # S — o corpo
REEL = (70, 70, 78, 255)         # R — o carretel
HUB = (10, 10, 12, 255)          # H — o furo do carretel


def build(name, light, dark, speckle):
    """`speckle` marca, nas duas linhas da etiqueta, onde entra a cor ESCURA."""
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = image.load()

    label_rows = [5, 6]
    for y, row in enumerate(BODY):
        for x, code in enumerate(row):
            if code == ".":
                continue
            if code == "O":
                px[x, y] = OUTLINE
            elif code == "S":
                px[x, y] = SHELL
            elif code == "R":
                px[x, y] = REEL
            elif code == "H":
                px[x, y] = HUB
            elif code == "L":
                mark = speckle[label_rows.index(y)][x - 3]
                px[x, y] = dark if mark == "#" else light

    path = os.path.join(OUT, name + ".png")
    image.save(path)
    print("%-20s -> %s" % (name, os.path.relpath(path, REPO)))


def main():
    # INSIDIOUS: pedra fria. A referencia que o Pedro deu e um salao de pedra no breu
    # com tocha rara, entao a etiqueta e um cinza azulado sem calor nenhum — longe do
    # verde da DATA e da madeira da CHUNKS, e dentro da paleta do mod.
    #
    # O respingo escuro e ESPARSO e desalinhado entre as duas linhas: e a leitura de
    # "luz rara no escuro", nao de textura de pedra. Alinhado, viraria listra.
    build("tape_insidious",
          light=(148, 156, 172, 255),
          dark=(26, 30, 40, 255),
          speckle=["#..#....#.",
                   "...#..#..."])


if __name__ == "__main__":
    main()
