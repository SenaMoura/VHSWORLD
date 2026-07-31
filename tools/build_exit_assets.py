# -*- coding: utf-8 -*-
"""Blockstates, modelos, texturas e receita dos seis blocos da saida.

As texturas sao 16x16 pintadas a mao aqui, na paleta do mod (preto, chumbo, cinza,
moldura branca). O unico lugar em que entra cor e o mesmo dos outros: um detalhe pequeno
que diz para que serve o aparelho — o mostrador ambar do radio, o azul do OSD do
videocassete, o vermelho da camara escura.
"""
import io
import json
import os

from PIL import Image

REPO = r'C:/Users/Hamilton/Downloads/GitHub/VHSWORLD/src/main/resources'
ASSETS = os.path.join(REPO, 'assets', 'recmod')
DATA = os.path.join(REPO, 'data', 'recmod')

for sub in ('blockstates', 'models/block', 'models/item', 'textures/block', 'textures/item'):
    os.makedirs(os.path.join(ASSETS, *sub.split('/')), exist_ok=True)
os.makedirs(os.path.join(DATA, 'recipes'), exist_ok=True)


def write(path, obj):
    with io.open(path, 'w', encoding='utf-8') as fh:
        json.dump(obj, fh, indent=2)
        fh.write('\n')


def paint(name, rows, palette, where=ASSETS, folder='block'):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        for x, code in enumerate(row):
            if code != '.':
                px[x, y] = palette[code]
    path = os.path.join(where, 'textures', folder, name + '.png')
    img.save(path)
    print('  textura', os.path.relpath(path, REPO))


# ---------------------------------------------------------------- as texturas
SHELL = (34, 34, 38, 255)       # K  carcaca
EDGE = (14, 14, 16, 255)        # O  contorno
FACE = (58, 58, 64, 255)        # F  frente
AMBER = (206, 148, 46, 255)     # A  mostrador do radio
DIALK = (120, 120, 128, 255)    # D  botao
BLUE = (74, 136, 214, 255)      # B  OSD do videocassete
GLASS = (108, 122, 130, 255)    # G  vidro do espelho
CRACK = (222, 228, 232, 255)    # C  a rachadura
RED = (168, 44, 40, 255)        # R  luz da camara escura
FLUID = (52, 74, 66, 255)       # L  o liquido do tanque
PAPER = (214, 208, 188, 255)    # P  o bilhete
INK = (72, 66, 58, 255)         # I  a letra
TAPE = (24, 24, 28, 255)        # T  a fita
REEL = (96, 96, 104, 255)       # E  o carretel

P = {'K': SHELL, 'O': EDGE, 'F': FACE, 'A': AMBER, 'D': DIALK, 'B': BLUE, 'G': GLASS,
     'C': CRACK, 'R': RED, 'L': FLUID, 'P': PAPER, 'I': INK, 'T': TAPE, 'E': REEL}

RADIO = [
    "................",
    "................",
    "..OOOOOOOOOOOO..",
    "..OKKKKKKKKKKO..",
    "..OKAAAAAAAAKO..",
    "..OKAOOOOOOAKO..",
    "..OKAOAAAAOAKO..",
    "..OKAOAAAAOAKO..",
    "..OKAOOOOOOAKO..",
    "..OKAAAAAAAAKO..",
    "..OKKKKKKKKKKO..",
    "..OKDDKKKKDDKO..",
    "..OKDDKKKKDDKO..",
    "..OOOOOOOOOOOO..",
    "................",
    "................",
]

VCR = [
    "OOOOOOOOOOOOOOOO",
    "OKKKKKKKKKKKKKKO",
    "OKFFFFFFFFFFFFKO",
    "OKFOOOOOOOOOOFKO",
    "OKFOBBBBBBBBOFKO",
    "OKFOBOOOOOOBOFKO",
    "OKFOBOKKKKOBOFKO",
    "OKFOBOKKKKOBOFKO",
    "OKFOBOOOOOOBOFKO",
    "OKFOBBBBBBBBOFKO",
    "OKFOOOOOOOOOOFKO",
    "OKFFFFFFFFFFFFKO",
    "OKDDKKKKKKKKDDKO",
    "OKKKKKKKKKKKKKKO",
    "OKBBBBKKKKBBBBKO",
    "OOOOOOOOOOOOOOOO",
]

MIRROR = [
    "OOOOOOOOOOOOOOOO",
    "OKKKKKKKKKKKKKKO",
    "OKOOOOOOOOOOOOKO",
    "OKOGGGGGCGGGGOKO",
    "OKOGGGGCGGGGGOKO",
    "OKOGGGCGGGGGGOKO",
    "OKOGGCGGGGCGGOKO",
    "OKOGGGCGGCGGGOKO",
    "OKOGGGGCCGGGGOKO",
    "OKOGGGGGCGGGGOKO",
    "OKOGGGGGCGGGGOKO",
    "OKOGGGGCGGGGGOKO",
    "OKOOOOOOOOOOOOKO",
    "OKKKKKKKKKKKKKKO",
    "OKKKKKKKKKKKKKKO",
    "OOOOOOOOOOOOOOOO",
]

DARKROOM = [
    "................",
    "..RR........RR..",
    "..OOOOOOOOOOOO..",
    "..OKKKKKKKKKKO..",
    "..OKOOOOOOOOKO..",
    "..OKOLLLLLLOKO..",
    "..OKOLLLLLLOKO..",
    "..OKOLLLLLLOKO..",
    "..OKOLLLLLLOKO..",
    "..OKOLLLLLLOKO..",
    "..OKOOOOOOOOKO..",
    "..OKKKKKKKKKKO..",
    "..OKDDKKKKDDKO..",
    "..OOOOOOOOOOOO..",
    "................",
    "................",
]

NOTE = [
    "................",
    "...PPPPPPPPPP...",
    "...PPPPPPPPPP...",
    "...PIIIIIIPPP...",
    "...PPPPPPPPPP...",
    "...PIIIIIIIIP...",
    "...PPPPPPPPPP...",
    "...PIIIIIPPPP...",
    "...PPPPPPPPPP...",
    "...PIIIIIIIPP...",
    "...PPPPPPPPPP...",
    "...PIIIPPPPPP...",
    "...PPPPPPPPPP...",
    "...PPPPPPPPPP...",
    "................",
    "................",
]

FRAGMENT = [
    "................",
    "................",
    "................",
    "....OOOOOOOO....",
    "....OTTTTTTO....",
    "....OTEEEETO....",
    "....OTEOOETO....",
    "....OTEOOETO....",
    "....OTEEEETO....",
    "....OTTTTTTO....",
    "....OOOOO.......",
    ".......O........",
    "........O.......",
    "................",
    "................",
    "................",
]

REAL_TAPE = [
    "................",
    "................",
    "...OOOOOOOOOO...",
    "..OTTTTTTTTTTO..",
    "..OTEEEETEEETO..",
    "..OTEOOETEOETO..",
    "..OTEOOETEOETO..",
    "..OTEEEETEEETO..",
    "..OTTTTTTTTTTO..",
    "..OTCCTTTTCCTO..",
    "..OTTTTTTTTTTO..",
    "...OOOOOOOOOO...",
    "................",
    "................",
    "................",
    "................",
]

print('texturas de bloco:')
paint('exit_radio', RADIO, P)
paint('exit_vcr', VCR, P)
paint('exit_mirror', MIRROR, P)
paint('exit_darkroom', DARKROOM, P)
paint('exit_note', NOTE, P)
paint('tape_fragment', FRAGMENT, P)

print('texturas de item:')
paint('tape_fragment', FRAGMENT, P, folder='item')
paint('real_world_tape', REAL_TAPE, P, folder='item')

# ---------------------------------------------------------------- blockstates + modelos
BLOCKS = ['exit_radio', 'exit_vcr', 'exit_mirror', 'exit_darkroom', 'exit_note', 'tape_fragment']

print('blockstates e modelos:')
for name in BLOCKS:
    write(os.path.join(ASSETS, 'blockstates', name + '.json'),
          {"variants": {"": {"model": "recmod:block/" + name}}})
    write(os.path.join(ASSETS, 'models', 'block', name + '.json'),
          {"parent": "block/cube_all", "textures": {"all": "recmod:block/" + name}})
    print('  ', name)

# O item do fragmento usa a textura de ITEM (a fita de lado), e nao o cubo: na mao, um
# cubo com a fita estampada nas seis faces nao se le como pedaco de fita.
write(os.path.join(ASSETS, 'models', 'item', 'tape_fragment.json'),
      {"parent": "item/generated", "textures": {"layer0": "recmod:item/tape_fragment"}})
write(os.path.join(ASSETS, 'models', 'item', 'real_world_tape.json'),
      {"parent": "item/generated", "textures": {"layer0": "recmod:item/real_world_tape"}})

# ---------------------------------------------------------------- a receita
# SEM FORMA de proposito: cabe na grade 2x2 do inventario, e nao ha mesa de trabalho em
# dimensao nenhuma do mod.
write(os.path.join(DATA, 'recipes', 'real_world_tape.json'), {
    "type": "minecraft:crafting_shapeless",
    "ingredients": [
        {"item": "recmod:tape_fragment"},
        {"item": "recmod:tape_fragment"},
        {"item": "recmod:tape_fragment"}
    ],
    "result": {"item": "recmod:real_world_tape", "count": 1}
})
print('receita: real_world_tape (3 fragmentos, sem forma)')
