# -*- coding: utf-8 -*-
"""Gera as construcoes de vila no estilo antigo (1.12 e anteriores).

Tudo aqui e AUTORADO: nada foi extraido de versao antiga do jogo. A gramatica
velha e simples e cabe em codigo — base de pedregulho, paredes de tabua, cantos
de tronco, vidraca simples e telhado raso de laje com beiral.

Um gerador so atende todos os biomas. O que muda por bioma:
  - a paleta (o deserto e de arenito; o resto usa a madeira de sempre, porque
    antes de 1.14 savana, taiga e neve NAO tinham estilo proprio: nasciam a
    vila comum de carvalho)
  - os pools que os blocos de encaixe apontam, inclusive a variante zumbi
"""
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from nbtwrite import Structure, _c_str

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    'src', 'main', 'resources', 'data', 'recmod', 'structures', 'village', 'old')

WOOD = {
    "floor":  ("minecraft:cobblestone", None),
    "wall":   ("minecraft:oak_planks", None),
    "post":   ("minecraft:oak_log", {"axis": "y"}),
    "roof":   ("minecraft:oak_slab", {"type": "top", "waterlogged": "false"}),
    "heavy":  ("minecraft:cobblestone", None),
    "stairs": "minecraft:oak_stairs",
    "beam":   ("minecraft:oak_log", {"axis": "x"}),
    "pitched": True,
}
SAND = {
    "floor":  ("minecraft:sandstone", None),
    "wall":   ("minecraft:sandstone", None),
    "post":   ("minecraft:chiseled_sandstone", None),
    "roof":   ("minecraft:sandstone_slab", {"type": "top", "waterlogged": "false"}),
    "heavy":  ("minecraft:smooth_sandstone", None),
    "stairs": "minecraft:sandstone_stairs",
    "beam":   ("minecraft:cut_sandstone", None),
    # O deserto NAO ganha telhado de duas aguas: a casa de arenito sempre foi de
    # teto plano, e e isso que a diferencia da casa de madeira a distancia.
    "pitched": False,
}

BIOMES = {
    "plains":  WOOD,
    "savanna": WOOD,
    "taiga":   WOOD,
    "snowy":   WOOD,
    "desert":  SAND,
}


class Builder:
    def __init__(self, biome, zombie):
        self.p = BIOMES[biome]
        self.streets   = "minecraft:village/%s/%sstreets"   % (biome, "zombie/" if zombie else "")
        self.villagers = "minecraft:village/%s/%svillagers" % (biome, "zombie/" if zombie else "")

    def put(self, s, x, y, z, key):
        name, props = self.p[key]
        s.set(x, y, z, name, props)

    def shell(self, s, x1, z1, x2, z2, height):
        name, props = self.p["floor"]
        s.fill(x1, 0, z1, x2, 0, z2, name, props)
        for y in range(1, height + 1):
            for x in range(x1, x2 + 1):
                self.put(s, x, y, z1, "wall"); self.put(s, x, y, z2, "wall")
            for z in range(z1, z2 + 1):
                self.put(s, x1, y, z, "wall"); self.put(s, x2, y, z, "wall")
        for (cx, cz) in [(x1, z1), (x1, z2), (x2, z1), (x2, z2)]:
            for y in range(1, height + 1):
                self.put(s, cx, y, cz, "post")

    def roof(self, s, x1, z1, x2, z2, y):
        """O telhado.

        Duas aguas em ESCADA, com beiral de um bloco para fora — e a silhueta que
        diz "vila antiga" de longe, mais do que qualquer parede. A versao anterior
        era uma laje plana e por isso as casas pareciam caixas.

        A cumeeira corre no eixo X. A cada nivel as duas aguas avancam uma fileira
        uma na direcao da outra ate se encontrarem; o triangulo que sobra nas duas
        pontas e fechado com a parede (a empena). O deserto pula tudo isso: la o
        telhado plano e que e o certo.
        """
        if not self.p["pitched"]:
            name, props = self.p["roof"]
            s.fill(x1 - 1, y, z1 - 1, x2 + 1, y, z2 + 1, name, props)
            return

        stairs = self.p["stairs"]
        wall_name, wall_props = self.p["wall"]
        zn, zs, yy = z1 - 1, z2 + 1, y

        while zn < zs:
            # A escada aponta para FORA da cumeeira (conferido no telhado do
            # taiga_medium_house_1 do vanilla: agua norte com facing=north). Ao
            # contrario, o telhado nasce invertido, com o degrau virado para
            # dentro — e so se percebe olhando a casa pronta.
            for x in range(x1 - 1, x2 + 2):
                s.set(x, yy, zn, stairs, {"facing": "north", "half": "bottom",
                                          "shape": "straight", "waterlogged": "false"})
                s.set(x, yy, zs, stairs, {"facing": "south", "half": "bottom",
                                          "shape": "straight", "waterlogged": "false"})
            # empena: o triangulo de parede entre as duas aguas, nas pontas
            for z in range(zn + 1, zs):
                s.set(x1 - 1, yy, z, wall_name, wall_props)
                s.set(x2 + 1, yy, z, wall_name, wall_props)
            zn += 1
            zs -= 1
            yy += 1

        # A cumeeira. Se sobrou uma fileira, ela vira o espinhaco; se as aguas se
        # cruzaram, o nivel de baixo ja fechou e nao ha o que tampar.
        if zn == zs:
            name, props = self.p["roof"]
            for x in range(x1 - 1, x2 + 2):
                s.set(x, yy, zn, name, props)

    def rafters(self, s, x1, z1, x2, z2, y):
        """As vigas: troncos deitados atravessando o vao, logo abaixo do telhado.

        Sao o que se ve ao entrar numa casa da vila antiga — o teto nao e liso, e
        um madeiramento. Custam tres blocos e mudam o interior inteiro.
        """
        name, props = self.p["beam"]
        for x in range(x1 + 1, x2, 2):
            for z in range(z1, z2 + 1):
                s.set(x, y, z, name, props)

    def door(self, s, x, y, z, facing):
        common = {"facing": facing, "hinge": "left", "open": "false", "powered": "false"}
        s.set(x, y, z, "minecraft:oak_door", dict(common, half="lower"))
        s.set(x, y + 1, z, "minecraft:oak_door", dict(common, half="upper"))

    def pane(self, s, x, y, z, along_z):
        s.set(x, y, z, "minecraft:glass_pane", {
            "north": "true" if along_z else "false", "south": "true" if along_z else "false",
            "east": "false" if along_z else "true", "west": "false" if along_z else "true",
            "waterlogged": "false"})

    def bed(self, s, x, y, z, facing, dx, dz):
        for part, (ox, oz) in (("foot", (0, 0)), ("head", (dx, dz))):
            s.set(x + ox, y, z + oz, "minecraft:red_bed",
                  {"part": part, "facing": facing, "occupied": "false"},
                  [_c_str("id", "minecraft:bed")])

    def connectors(self, s, ez, cx, cz):
        s.jigsaw(0, 0, ez, "west_up", "minecraft:building_entrance", "minecraft:building_entrance",
                 self.streets,
                 "minecraft:oak_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]",
                 joint="aligned")
        floor_name = self.p["floor"][0]
        s.jigsaw(cx, 0, cz, "up_north", "minecraft:bottom", "minecraft:bottom",
                 self.villagers, floor_name)

    # ------------------------------------------------------------ construcoes

    def house_small(self):
        s = Structure(7, 8, 7)
        self.shell(s, 1, 1, 5, 5, 2)
        self.door(s, 1, 1, 3, "east")
        self.pane(s, 5, 2, 3, True)
        self.pane(s, 3, 2, 1, False)
        self.pane(s, 3, 2, 5, False)
        self.roof(s, 1, 1, 5, 5, 3)
        self.rafters(s, 1, 1, 5, 5, 3)
        self.bed(s, 2, 1, 2, "east", 1, 0)
        s.set(4, 2, 4, "minecraft:wall_torch", {"facing": "west"})
        s.set(4, 1, 4, "minecraft:crafting_table")
        self.connectors(s, 3, 3, 3)
        return s

    def house_large(self):
        s = Structure(9, 10, 9)
        self.shell(s, 1, 1, 7, 7, 3)
        self.door(s, 1, 1, 4, "east")
        for z in (3, 5):
            self.pane(s, 7, 2, z, True)
        for x in (3, 5):
            self.pane(s, x, 2, 1, False)
            self.pane(s, x, 2, 7, False)
        self.roof(s, 1, 1, 7, 7, 4)
        self.rafters(s, 1, 1, 7, 7, 4)
        self.bed(s, 2, 1, 2, "south", 0, 1)
        self.bed(s, 5, 1, 2, "south", 0, 1)
        s.set(6, 2, 6, "minecraft:wall_torch", {"facing": "west"})
        s.set(6, 1, 6, "minecraft:barrel", {"facing": "up", "open": "false"})
        self.connectors(s, 4, 4, 4)
        return s

    def smithy(self):
        """A ferraria: parede pesada, bau duplo e o poco de lava sem grade."""
        s = Structure(9, 10, 9)
        name, props = self.p["heavy"]
        s.fill(1, 0, 1, 7, 0, 7, name, props)
        for y in range(1, 4):
            for x in range(1, 8):
                s.set(x, y, 1, name, props); s.set(x, y, 7, name, props)
            for z in range(1, 8):
                s.set(1, y, z, name, props); s.set(7, y, z, name, props)
        for (cx, cz) in [(1, 1), (1, 7), (7, 1), (7, 7)]:
            for y in range(1, 4):
                self.put(s, cx, y, cz, "post")

        self.door(s, 1, 1, 4, "east")
        self.pane(s, 7, 2, 3, True)
        self.pane(s, 7, 2, 5, True)
        self.roof(s, 1, 1, 7, 7, 4)
        self.rafters(s, 1, 1, 7, 7, 4)

        s.set(5, 0, 5, "minecraft:lava", {"level": "0"})
        s.set(5, 1, 3, "minecraft:anvil", {"facing": "north"})
        s.set(3, 1, 5, "minecraft:furnace", {"facing": "south", "lit": "false"})
        # O bau duplo do ferreiro com o loot do jogo. Antes ele nascia VAZIO, e o
        # bau do ferreiro sempre foi o motivo de procurar uma vila.
        for x, half in ((2, "left"), (3, "right")):
            s.set(x, 1, 2, "minecraft:chest",
                  {"facing": "south", "type": half, "waterlogged": "false"},
                  [_c_str("id", "minecraft:chest"),
                   _c_str("LootTable", "minecraft:chests/village/village_weaponsmith")])
        s.set(6, 2, 6, "minecraft:wall_torch", {"facing": "west"})
        self.connectors(s, 4, 4, 4)
        return s

    def farm(self):
        """O rocado cercado: terra arada, o rego d'agua no meio e a plantacao.

        Na vila antiga a lavoura era plana e cercada, colada nas casas — nada de
        canteiro suspenso. Ela e o que faz a vila parecer habitada de longe.
        """
        s = Structure(9, 4, 9)
        floor_name, floor_props = self.p["floor"]

        for x in range(1, 8):
            for z in range(1, 8):
                border = x in (1, 7) or z in (1, 7)
                if border:
                    s.set(x, 0, z, "minecraft:dirt")
                    s.set(x, 1, z, "minecraft:oak_fence",
                          {"north": "false", "south": "false", "east": "false",
                           "west": "false", "waterlogged": "false"})
                elif z == 4:
                    s.set(x, 0, z, "minecraft:water", {"level": "0"})   # o rego
                else:
                    s.set(x, 0, z, "minecraft:farmland", {"moisture": "7"})
                    crop = "minecraft:wheat" if (x + z) % 3 else "minecraft:carrots"
                    age = "7" if (x * z) % 2 else "3"
                    s.set(x, 1, z, crop, {"age": age})

        # portao no lugar de uma estaca da cerca, senao ninguem entra
        s.set(4, 1, 1, "minecraft:oak_fence_gate",
              {"facing": "north", "in_wall": "false", "open": "false", "powered": "false"})
        s.set(4, 0, 1, floor_name, floor_props)
        self.connectors(s, 4, 4, 2)
        return s

    def church(self):
        """A torre da vila: pedra, escada de mao e uma janela em cada lado.

        E o unico predio alto da vila antiga — serve de ponto de referencia para
        quem volta de longe, e por isso vale mais que outra casa igual.
        """
        s = Structure(7, 14, 7)
        name, props = self.p["heavy"]
        s.fill(1, 0, 1, 5, 0, 5, name, props)
        for y in range(1, 8):
            for x in range(1, 6):
                s.set(x, y, 1, name, props); s.set(x, y, 5, name, props)
            for z in range(1, 6):
                s.set(1, y, z, name, props); s.set(5, y, z, name, props)
        for (cx, cz) in [(1, 1), (1, 5), (5, 1), (5, 5)]:
            for y in range(1, 8):
                self.put(s, cx, y, cz, "post")

        s.fill(2, 1, 2, 4, 7, 4, "minecraft:air")
        self.door(s, 1, 1, 3, "east")
        for y in (3, 6):
            self.pane(s, 5, y, 3, True)
            self.pane(s, 3, y, 1, False)
            self.pane(s, 3, y, 5, False)
        for y in range(1, 7):
            s.set(4, y, 4, "minecraft:ladder", {"facing": "west", "waterlogged": "false"})
        s.set(2, 1, 2, "minecraft:wall_torch", {"facing": "east"})
        self.roof(s, 1, 1, 5, 5, 8)
        self.connectors(s, 3, 3, 3)
        return s


def main():
    os.makedirs(ROOT, exist_ok=True)
    total = 0
    for biome in BIOMES:
        for zombie in (False, True):
            b = Builder(biome, zombie)
            suffix = "_zombie" if zombie else ""
            for kind in ("house_small", "house_large", "smithy", "farm", "church"):
                s = getattr(b, kind)()
                path = os.path.join(ROOT, "%s_%s%s.nbt" % (biome, kind, suffix))
                s.save(path)
                total += 1
    print("geradas", total, "construcoes em", ROOT)


if __name__ == "__main__":
    main()
