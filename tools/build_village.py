# -*- coding: utf-8 -*-
"""Gera as casas de vila no estilo antigo (1.12 e anteriores).

As construcoes sao AUTORADAS aqui, nao extraidas de lugar nenhum: paredes de
tabua com cantos de tronco, base de pedregulho, janelas de vidro e telhado
raso de laje — a gramatica das vilas velhas, escrita em codigo.
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from nbtwrite import Structure

OUT = os.path.join(os.environ['USERPROFILE'], 'Downloads', 'GitHub', 'VHSWORLD',
                   'src', 'main', 'resources', 'data', 'recmod', 'structures', 'village', 'old')
os.makedirs(OUT, exist_ok=True)

PLANKS = "minecraft:oak_planks"
LOG    = "minecraft:oak_log"
COBBLE = "minecraft:cobblestone"
SLAB   = "minecraft:oak_slab"
PANE   = "minecraft:glass_pane"

def pane(ns):
    """Vidraca ja conectada: sem isso ela nasce como um poste solto."""
    return {"north": "true" if ns else "false", "south": "true" if ns else "false",
            "east": "false" if ns else "true", "west": "false" if ns else "true",
            "waterlogged": "false"}

def door(s, x, y, z, facing, hinge="left"):
    common = {"facing": facing, "hinge": hinge, "open": "false", "powered": "false"}
    s.set(x, y, z, "minecraft:oak_door", dict(common, half="lower"))
    s.set(x, y + 1, z, "minecraft:oak_door", dict(common, half="upper"))

def entrance(s, x, z):
    s.jigsaw(x, 0, z, "west_up", "minecraft:building_entrance", "minecraft:building_entrance",
             "minecraft:village/plains/streets",
             "minecraft:oak_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]",
             joint="aligned")

def villager(s, x, z, floor=COBBLE):
    s.jigsaw(x, 0, z, "up_north", "minecraft:bottom", "minecraft:bottom",
             "minecraft:village/plains/villagers", floor)

def shell(s, x1, z1, x2, z2, height):
    """Base de pedregulho, paredes de tabua e os quatro postes de tronco."""
    s.fill(x1, 0, z1, x2, 0, z2, COBBLE)
    for y in range(1, height + 1):
        for x in range(x1, x2 + 1):
            s.set(x, y, z1, PLANKS); s.set(x, y, z2, PLANKS)
        for z in range(z1, z2 + 1):
            s.set(x1, y, z, PLANKS); s.set(x2, y, z, PLANKS)
    for (cx, cz) in [(x1, z1), (x1, z2), (x2, z1), (x2, z2)]:
        for y in range(1, height + 1):
            s.set(cx, y, cz, LOG, {"axis": "y"})

def roof(s, x1, z1, x2, z2, y):
    """Telhado raso com beiral de uma casa — o jeito antigo, sem inclinacao."""
    s.fill(x1 - 1, y, z1 - 1, x2 + 1, y, z2 + 1, SLAB, {"type": "top", "waterlogged": "false"})


# ------------------------------------------------------------------ casa pequena
def house_small():
    s = Structure(7, 6, 7)
    shell(s, 1, 1, 5, 5, 2)
    door(s, 1, 1, 3, "east")
    s.set(5, 2, 3, PANE, pane(True))
    s.set(3, 2, 1, PANE, pane(False))
    s.set(3, 2, 5, PANE, pane(False))
    roof(s, 1, 1, 5, 5, 3)

    s.set(2, 1, 2, "minecraft:red_bed", {"part": "foot", "facing": "east", "occupied": "false"},
          [__import__("nbtwrite")._c_str("id", "minecraft:bed")])
    s.set(3, 1, 2, "minecraft:red_bed", {"part": "head", "facing": "east", "occupied": "false"},
          [__import__("nbtwrite")._c_str("id", "minecraft:bed")])
    s.set(4, 2, 4, "minecraft:wall_torch", {"facing": "west"})
    s.set(4, 1, 4, "minecraft:crafting_table")

    entrance(s, 0, 3)
    villager(s, 3, 3)
    s.save(os.path.join(OUT, "house_small.nbt"))
    return s

# ------------------------------------------------------------------ casa grande
def house_large():
    s = Structure(9, 8, 9)
    shell(s, 1, 1, 7, 7, 3)
    door(s, 1, 1, 4, "east")
    for z in (3, 5):
        s.set(7, 2, z, PANE, pane(True))
    for x in (3, 5):
        s.set(x, 2, 1, PANE, pane(False))
        s.set(x, 2, 7, PANE, pane(False))
    roof(s, 1, 1, 7, 7, 4)

    for (bx, bz) in [(2, 2), (5, 2)]:
        s.set(bx, 1, bz, "minecraft:red_bed", {"part": "foot", "facing": "south", "occupied": "false"},
              [__import__("nbtwrite")._c_str("id", "minecraft:bed")])
        s.set(bx, 1, bz + 1, "minecraft:red_bed", {"part": "head", "facing": "south", "occupied": "false"},
              [__import__("nbtwrite")._c_str("id", "minecraft:bed")])
    s.set(6, 2, 6, "minecraft:wall_torch", {"facing": "west"})
    s.set(6, 1, 6, "minecraft:barrel", {"facing": "up", "open": "false"})

    entrance(s, 0, 4)
    villager(s, 4, 4)
    s.save(os.path.join(OUT, "house_large.nbt"))
    return s

# ------------------------------------------------------------------ ferraria
def smithy():
    """A ferraria velha: pedregulho, lava aberta no chao e o bau duplo."""
    s = Structure(9, 8, 9)
    s.fill(1, 0, 1, 7, 0, 7, COBBLE)
    for y in range(1, 4):
        for x in range(1, 8):
            s.set(x, y, 1, COBBLE); s.set(x, y, 7, COBBLE)
        for z in range(1, 8):
            s.set(1, y, z, COBBLE); s.set(7, y, z, COBBLE)
    for (cx, cz) in [(1, 1), (1, 7), (7, 1), (7, 7)]:
        for y in range(1, 4):
            s.set(cx, y, cz, LOG, {"axis": "y"})

    door(s, 1, 1, 4, "east")
    s.set(7, 2, 3, PANE, pane(True))
    s.set(7, 2, 5, PANE, pane(True))
    roof(s, 1, 1, 7, 7, 4)

    # O poco de lava sem grade, do jeito que matava aldeao
    s.set(5, 0, 5, "minecraft:lava", {"level": "0"})
    s.set(5, 1, 3, "minecraft:anvil", {"facing": "north"})
    s.set(3, 1, 5, "minecraft:furnace", {"facing": "south", "lit": "false"})
    s.set(2, 1, 2, "minecraft:chest", {"facing": "south", "type": "left", "waterlogged": "false"})
    s.set(3, 1, 2, "minecraft:chest", {"facing": "south", "type": "right", "waterlogged": "false"})
    s.set(6, 2, 6, "minecraft:wall_torch", {"facing": "west"})

    entrance(s, 0, 4)
    villager(s, 4, 4, floor=COBBLE)
    s.save(os.path.join(OUT, "smithy.nbt"))
    return s


for fn in (house_small, house_large, smithy):
    st = fn()
    print(fn.__name__, "->", st.size, len(st.blocks), "blocos,", len(st.palette), "estados")
