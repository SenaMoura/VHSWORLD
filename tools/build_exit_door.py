# -*- coding: utf-8 -*-
"""A textura da Porta de Saida: moldura fria e um vazio que se mexe dentro dela.

⚠️ O QUE FOI COPIADO DO DIMENSIONAL DOORS E O QUE NAO FOI. O Pedro pediu "igual a do mod
dimensional doors", e o que faz aquela porta ser reconhecivel sao duas coisas de FORMA, e
nenhuma delas e arte:

  1. E uma PORTA DE VERDADE — dois blocos de altura, fina, com dobradica, que abre. Nao um
     cubo com desenho de porta. Isso e forma do proprio Minecraft (o modelo `block/door_*`
     e da Mojang), e a nossa herda dele.
  2. Tem um VAZIO ANIMADO no lugar do painel, em vez de madeira. A ideia e deles; o
     desenho e nosso.

O que NAO veio: nenhum pixel. As texturas do dimdoors sao arte autoral do mod deles, e
copia-las seria plagio — a mesma decisao que fez a plataforma de petroleo do worldgen ser
refeita do zero em vez de sair do Jeff's. O vazio deles e azul-petroleo; o nosso e chumbo
e branco de chiado, porque a paleta do VHSWORLD nao tem cor e o assunto do mod e fita, nao
magia.

A animacao sai daqui como uma tira vertical de 8 quadros + .mcmeta, que e o formato de
textura animada do proprio jogo — sem codigo nenhum do nosso lado.

Rodar:
    python tools/build_exit_door.py
"""
import io
import json
import os
import random

from PIL import Image

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(REPO, 'src', 'main', 'resources', 'assets', 'recmod', 'textures', 'block')

FRAMES = 8

# A moldura: chumbo CLARO. Ela precisa ganhar do vazio por valor e nao por cor — a
# primeira versao tinha moldura escura e vazio ruidoso, e os dois batiam no mesmo tom
# medio: de longe a porta virava um retangulo de chuvisco, sem forma de porta nenhuma. O
# que faz o olho ler "porta" e a MOLDURA CLARA em volta de um buraco preto.
EDGE = (18, 18, 22, 255)
FRAME = (104, 110, 122, 255)
FRAME_LIT = (146, 152, 166, 255)
RIVET = (182, 188, 198, 255)


def frame_mask():
    """1 = moldura, 0 = vazio. A porta e vista de frente, 16x16 por metade."""
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            border = x <= 1 or x >= 14 or y <= 0 or y >= 15
            # a travessa do meio, que e o que divide o vao em dois e faz o olho ler porta
            cross = 7 <= y <= 8
            row.append(1 if border or cross else 0)
        rows.append(row)
    return rows


def build(name, seed, top):
    mask = frame_mask()
    sheet = Image.new('RGBA', (16, 16 * FRAMES), (0, 0, 0, 0))
    rng = random.Random(seed)

    # ⚠️ O RUIDO E SORTEADO UMA VEZ POR QUADRO E NAO POR PIXEL-QUADRO INDEPENDENTE: os
    # quadros sao gerados a partir de um campo que ANDA para cima, e nao redesenhados do
    # zero. Ruido novo a cada quadro pisca como chuvisco de TV morta; ruido que se desloca
    # le como uma coisa se movendo la dentro, que e o que se quer.
    field = [[rng.random() for _ in range(16)] for _ in range(16 * 2)]

    for f in range(FRAMES):
        for y in range(16):
            for x in range(16):
                if mask[y][x]:
                    if (x + y) % 7 == 0:
                        px = FRAME_LIT
                    elif (x in (2, 13) and y in (3, 12)) or (top and y == 1 and x in (4, 11)):
                        px = RIVET
                    elif x <= 1 or x >= 14 or y <= 0 or y >= 15:
                        px = EDGE if (x == 0 or x == 15 or y == 0 or y == 15) else FRAME
                    else:
                        px = FRAME
                else:
                    # O campo desliza para cima ao longo dos quadros.
                    v = field[(y + f * 2) % (16 * 2)][x]
                    # ⚠️ ELEVADO A QUARTA, e nao ao quadrado. Ao quadrado, metade do vao
                    # ficava cinza-medio e o vazio parecia so uma textura suja; a quarta
                    # potencia joga quase tudo para o preto e deixa poucos pontos claros
                    # sobrando. E o que muda a leitura de "painel granulado" para "buraco
                    # com alguma coisa se mexendo dentro".
                    v = v * v * v * v
                    g = int(4 + v * 210)
                    px = (g, g, min(255, int(g * 1.08)), 255)
                sheet.putpixel((x, f * 16 + y), px)

    path = os.path.join(OUT, name + '.png')
    sheet.save(path)
    with io.open(path + '.mcmeta', 'w', encoding='utf-8') as fh:
        json.dump({'animation': {'frametime': 3}}, fh, indent=2)
        fh.write('\n')
    print('%-22s %s  %.1f KB' % (name, sheet.size, os.path.getsize(path) / 1024))


def main():
    os.makedirs(OUT, exist_ok=True)
    # Sementes diferentes para as duas metades: o vazio da parte de cima nao pode ser a
    # continuacao exata do de baixo, senao a emenda vira uma listra visivel no meio.
    build('exit_door_top', 71, top=True)
    build('exit_door_bottom', 137, top=False)

    old = os.path.join(OUT, 'exit_door.png')
    if os.path.exists(old):
        os.remove(old)
        print('textura de cubo antiga removida')


if __name__ == '__main__':
    main()
