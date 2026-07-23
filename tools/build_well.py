# -*- coding: utf-8 -*-
"""Gera o POCO central da vila, no lugar da praca do sino.

Antes da 1.14 a vila nao tinha praca nem sino: tinha um poco de pedregulho no
meio, com quatro cercas e um telhadinho de laje. E a silhueta que diz "vila
antiga" antes de qualquer casa aparecer, porque e o que se ve de longe.

O poco e a peca RAIZ do jigsaw — e dele que as ruas saem. Por isso ele copia o
contrato da praca do vanilla, lido de plains_fountain_01.nbt:

  - 9x9 de largura, chao no y=0 e a altura util comecando no y=1
  - quatro conectores de RUA nas bordas, em [0,1,4] [4,1,0] [4,1,8] [8,1,4],
    orientation apontando para fora, name/target `minecraft:street`, joint
    `aligned` e final_state `structure_void`
  - conectores de ALDEAO no y=0, orientation `up_north`, name/target
    `minecraft:bottom`, joint `rollable`, com o caminho de terra como
    final_state (senao fica um buraco no chao)
  - os pools `village/common/cats` e `village/common/iron_golem`, que sao os
    que povoam a praca

O SINO FICA. Ele nao e enfeite: e o ponto de encontro que a IA dos aldeoes usa
para dormir, trabalhar e chamar o golem. Tirar o sino seria trocar aparencia
antiga por vila quebrada — a licao numero 1 do projeto. A solucao foi pendura-lo
sob o telhado do poco, onde ele parece que sempre esteve.

Rodar: python tools/build_well.py
"""
import os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from nbtwrite import Structure, _c_str

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    'src', 'main', 'resources', 'data', 'recmod', 'structures', 'village', 'old')

# A pedra do poco. So o deserto muda: antes da 1.14 a vila do deserto era a
# mesma vila, com o pedregulho trocado por arenito na hora de construir.
STONE = {
    "plains":  ("minecraft:cobblestone", "minecraft:cobblestone_slab"),
    "savanna": ("minecraft:cobblestone", "minecraft:cobblestone_slab"),
    "taiga":   ("minecraft:cobblestone", "minecraft:cobblestone_slab"),
    "snowy":   ("minecraft:cobblestone", "minecraft:cobblestone_slab"),
    "desert":  ("minecraft:sandstone",   "minecraft:sandstone_slab"),
}

# O chao da praca tem que ser o mesmo bloco da rua que encosta nela, senao a
# emenda aparece. E a rua agora e de CASCALHO em todo bioma: o caminho de terra
# so existe desde a 1.9, e antes disso a estrada da vila era de cascalho — e o
# que aparece na referencia que o Pedro mandou. Ver tools/build_streets.py.
PATH_BY_BIOME = {b: "minecraft:gravel" for b in
                 ("plains", "savanna", "taiga", "snowy", "desert")}

SIZE = 9        # largura da peca, igual a da praca do vanilla
RIM0, RIM1 = 2, 6   # a borda do poco (5x5)


def build(biome, zombie):
    stone, slab = STONE[biome]
    path = PATH_BY_BIOME[biome]
    s = Structure(SIZE, 5, SIZE)

    streets = "minecraft:village/%s/%sstreets" % (biome, "zombie/" if zombie else "")
    villagers = "minecraft:village/%s/villagers" % biome

    # --- y=0: o chao da praca, com os cantos abertos (o vanilla tambem os deixa
    # de fora, para a esquina nao virar um quadrado duro no meio do mato)
    for x in range(SIZE):
        for z in range(SIZE):
            corner = x in (0, SIZE - 1) and z in (0, SIZE - 1)
            if not corner:
                s.set(x, 0, z, path)
    # base do poco, para ele nao boiar quando o terreno cede
    s.fill(RIM0, 0, RIM0, RIM1, 0, RIM1, stone)

    # --- y=1: a boca do poco e a agua
    for x in range(RIM0, RIM1 + 1):
        for z in range(RIM0, RIM1 + 1):
            edge = x in (RIM0, RIM1) or z in (RIM0, RIM1)
            if edge:
                s.set(x, 1, z, stone)
            else:
                s.set(x, 1, z, "minecraft:water", {"level": "0"})

    # --- y=2 e y=3: as quatro cercas nos cantos da boca
    for (cx, cz) in [(RIM0, RIM0), (RIM0, RIM1), (RIM1, RIM0), (RIM1, RIM1)]:
        for y in (2, 3):
            s.set(cx, y, cz, "minecraft:oak_fence", {
                "north": "false", "south": "false", "east": "false", "west": "false",
                "waterlogged": "false"})

    # --- y=4: o telhado de laje, apoiado nas quatro cercas
    s.fill(RIM0, 4, RIM0, RIM1, 4, RIM1, slab,
           {"type": "bottom", "waterlogged": "false"})

    # --- o sino, pendurado sob o telhado, no eixo do poco
    s.set(4, 3, 4, "minecraft:bell",
          {"attachment": "ceiling", "facing": "north", "powered": "false"},
          [_c_str("id", "minecraft:bell")])

    # --- conectores de rua, um por lado
    for (x, z, orientation) in [(0, 4, "west_up"), (4, 0, "north_up"),
                                (4, 8, "south_up"), (8, 4, "east_up")]:
        s.jigsaw(x, 1, z, orientation, "minecraft:street", "minecraft:street",
                 streets, "minecraft:structure_void", joint="aligned")

    # --- quem mora na praca. A vila zumbi nao ganha aldeao (e o que o vanilla
    # faz na versao zumbi da praca), mas os gatos ficam nas duas.
    if not zombie:
        for (x, z) in [(1, 1), (7, 1), (1, 7)]:
            s.jigsaw(x, 0, z, "up_north", "minecraft:bottom", "minecraft:bottom",
                     villagers, path)
        s.jigsaw(7, 0, 7, "up_north", "minecraft:bottom", "minecraft:bottom",
                 "minecraft:village/common/iron_golem", path)
    for (x, z) in [(1, 4), (7, 4)]:
        s.jigsaw(x, 0, z, "up_north", "minecraft:bottom", "minecraft:bottom",
                 "minecraft:village/common/cats", path)

    return s


def main():
    os.makedirs(ROOT, exist_ok=True)
    for biome in STONE:
        for zombie in (False, True):
            name = "%s_well%s.nbt" % (biome, "_zombie" if zombie else "")
            build(biome, zombie).save(os.path.join(ROOT, name))
            print("escrito", name)


if __name__ == "__main__":
    main()
