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

    # As seis do lote de 2026-07-29. A regra da etiqueta e sempre a mesma: a cor CLARA
    # e o material de que a dimensao e feita, e o respingo ESCURO e o que ela faz com
    # quem entra. Nenhuma delas e saturada — a cor aqui e etiqueta de fita velha, nao
    # icone de menu; e o dedo do jogador na fileira do bau tem que dar pra distinguir
    # sete fitas de longe, o que se resolve pelo VALOR (claro/escuro) e nao pelo matiz.

    # VILLAGE: tijolo e cobblestone da casa. O respingo e ralo e regular — a rua e uma
    # cadencia, e e justamente a repeticao que assusta ali.
    build("tape_village",
          light=(196, 156, 130, 255),
          dark=(82, 60, 52, 255),
          speckle=["#...#...#.",
                   ".#...#...#"])

    # GRASSROOMS: quartzo quase branco com a grama entrando. A etiqueta mais CLARA das
    # sete de proposito: e a unica dimensao iluminada, e ela se le no bau pelo brilho.
    build("tape_grassrooms",
          light=(228, 230, 226, 255),
          dark=(104, 148, 82, 255),
          speckle=[".#....#...",
                   "....#...#."])

    # TRAIN: aco e fuligem. O respingo em PAR e o unico ritmo desenhado das sete — sao
    # os dormentes passando debaixo de quem anda na linha.
    build("tape_train",
          light=(126, 126, 132, 255),
          dark=(28, 26, 24, 255),
          speckle=["##..##..##",
                   "..##..##.."])

    # UNDER PRESSURE: a agua funda. A etiqueta mais ESCURA das sete, e o respingo e
    # quase tudo — na fita, a agua ja cobriu a etiqueta.
    build("tape_under_pressure",
          light=(58, 112, 138, 255),
          dark=(8, 24, 36, 255),
          speckle=["#.##..#.##",
                   "##..##.#.#"])

    # BIBLIOTECA: couro de lombada e o amarelo do tapete, no escuro.
    build("tape_biblioteca",
          light=(178, 148, 86, 255),
          dark=(48, 34, 22, 255),
          speckle=["#..#..#..#",
                   "#..#..#..#"])

    # PARKOURLAND: madeira da gaiola. O respingo em coluna e o unico VERTICAL das sete
    # — a unica dimensao em que o assunto e altura, e a unica em que errar tira voce
    # dela.
    build("tape_parkourland",
          light=(190, 158, 104, 255),
          dark=(40, 34, 28, 255),
          speckle=["..#....#..",
                   "..#....#.."])

    # ------------------------------------------------------------------ o lote de 3
    #
    # As tres de 2026-07-30. A regra continua a mesma (claro = do que a dimensao e feita,
    # escuro = o que ela faz com quem entra), e o cuidado NOVO e que agora sao doze fitas
    # na mesma aba: as tres precisam se separar das nove por VALOR, e nao so por matiz.
    # Por isso nenhuma delas repete a faixa media em que village/biblioteca/parkourland
    # ja moram.

    # STONELAND: pedregulho, e nada alem de pedregulho. A unica etiqueta SEM MATIZ das
    # doze — cinza puro nos dois tons. E a leitura literal da dimensao: e o overworld com
    # a cor arrancada. O respingo denso e a granulacao do proprio pedregulho.
    build("tape_stoneland",
          light=(146, 146, 146, 255),
          dark=(58, 58, 58, 255),
          speckle=["#.#..##.#.",
                   ".##.#..##."])

    # ESCRITORIO: o bege de forro de escritorio, com o preto do vao entre as torres. E o
    # unico respingo em BLOCO das doze (dois pares grudados), e e de proposito: sao as
    # baias vistas de cima, que e a planta da dimensao.
    build("tape_escritorio",
          light=(198, 192, 170, 255),
          dark=(22, 22, 26, 255),
          speckle=["##...##...",
                   "##...##..."])

    # MAZE: o verde acinzentado da grama do chao contra a pedra da parede. O respingo e
    # o mais IRREGULAR das doze, sem nenhum par alinhado entre as duas linhas — as outras
    # onze tem cadencia, e esta e a unica dimensao cujo assunto e nao haver cadencia
    # nenhuma para se agarrar.
    build("tape_maze",
          light=(118, 132, 96, 255),
          dark=(46, 48, 44, 255),
          speckle=["#...#..#..",
                   "..#...#..#"])

    # ------------------------------------------------------------------ o lote de 3
    #
    # As tres de 2026-07-31. Sao QUINZE fitas na mesma aba agora, e o problema que isso
    # cria e novo: com doze ja e dificil achar uma pelo matiz, porque a paleta do mod e
    # estreita de proposito. As tres entram ocupando faixas de VALOR que ainda estavam
    # livres — a floresta e a mais escura de todas, a mall a mais clara depois da
    # grassrooms, e a pipe tunels fica na faixa media mas e a unica QUENTE (ferrugem)
    # entre catorze frias.

    # FLORESTA: o verde-preto do abeto na bruma. A etiqueta MAIS ESCURA das quinze, e e
    # a leitura literal das fotos — nelas o mato e uma massa preta e o unico claro e o
    # ceu, que nao cabe numa etiqueta. O respingo cerrado e sem cadencia nenhuma e a
    # propria densidade que o Pedro pediu: nao ha caminho a enxergar ali dentro.
    build("tape_floresta",
          light=(62, 78, 58, 255),
          dark=(18, 22, 18, 255),
          speckle=["#.##.#.##.",
                   ".##.##.#.#"])

    # PIPE TUNELS: o concreto claro do tunel, e a ferrugem do cano por cima. A unica
    # etiqueta QUENTE das quinze, e o respingo corre em LINHA CONTINUA nas duas fileiras
    # — e o unico desenho horizontal ininterrupto do lote, e e o cano, que e a coisa que
    # define a dimensao. As outras catorze tem respingo picado; esta atravessa.
    build("tape_pipe_tunels",
          light=(150, 144, 134, 255),
          dark=(122, 74, 44, 255),
          speckle=["..######..",
                   "..######.."])

    # MALL: o bege polido do piso, com o preto das lojas fechadas. O respingo em PARES
    # ESPACADOS e regular e a fileira de vitrines vista de cima — a unica dimensao do mod
    # cuja planta e uma cadencia comercial, e a unica etiqueta que a desenha.
    build("tape_mall",
          light=(214, 204, 178, 255),
          dark=(24, 22, 20, 255),
          speckle=["##..##..##",
                   "##..##..##"])


if __name__ == "__main__":
    main()
