# -*- coding: utf-8 -*-
"""Recorta as construcoes que o Pedro fez a mao e as transforma em pecas de vila.

O Pedro montou um set inteiro de vila num canteiro subterraneo de um mundo de
teste (as construcoes em fileira, todas com a frente virada para o oeste). Em vez
de ele salvar cada uma com bloco de estrutura, esta ferramenta le a regiao do
save direto, recorta cada predio pela caixa delimitadora, poe os conectores de
jigsaw que a vila precisa (entrada + ponto do aldeao) e escreve os `.nbt`.

Decisoes (do Pedro, 2026-07-23):
  - As pecas dele SUBSTITUEM as minhas em todos os biomas MENOS o deserto.
  - Planicie: o carvalho original dele. Taiga e neve: troca para spruce. Savana:
    troca para acacia. (Cada bioma com a sua madeira, como o vanilla faz.)
  - O deserto continua com as minhas pecas de arenito (build_village.py).
  - Ele nao fez fazenda; mantenho a minha (fica claro no pool).

Fonte: saves/Novo mundo (158). Se o save mudar, ajustar WORLD e as caixas.

Rodar depois de build_village/well/pools/streets:
    python tools/import_builds.py
"""
import os, sys, collections

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from nbtwrite import Structure, _c_str
import nbtlib  # noqa: usado indiretamente pelo leitor abaixo
import zlib, struct, glob
from nbtlib import Reader

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORLD = os.path.expandvars(r'%APPDATA%\.minecraft\saves\Novo mundo (158)\region')
OUT = os.path.join(REPO, 'src', 'main', 'resources', 'data', 'recmod',
                   'structures', 'village', 'built')
POOLS = os.path.join(REPO, 'src', 'main', 'resources', 'data', 'minecraft',
                     'worldgen', 'template_pool', 'village')

# --- as construcoes, pelas caixas que a varredura achou -------------------
# (x0,x1, y0,y1, z0,z1)  nome_do_tipo
BUILDS = [
    ("house_small",  22, 27, -39, -35,   9,  13),
    ("house_tower",  22, 27, -39, -34,  22,  26),
    # o topo da mini ia ate -34: a cumeeira era de TRONCO, e a 1a varredura
    # tratava tronco como arvore e cortava fora (a "casa sem teto" do Pedro).
    ("house_mini",   22, 27, -39, -34,  34,  37),
    ("well",         23, 26, -39, -35,  44,  47),
    ("house_large",  22, 33, -38, -32,  58,  67),
    ("smithy",       22, 29, -38, -33,  81,  90),
    ("library",      23, 30, -38, -30, 100, 108),
    ("house_large2", 23, 34, -38, -32, 121, 129),
    ("church",       23, 32, -38, -27, 145, 149),
]

# Biomas que recebem as pecas dele, e a madeira de cada um. O deserto nao entra.
BIOME_WOOD = {"plains": "oak", "taiga": "spruce", "snowy": "spruce", "savanna": "acacia"}

# Pecas GARANTIDAS em toda vila (decisao do Pedro). Cada uma pendura direto no
# centro (o poco), que e a peca-raiz sempre gerada — por isso aparecem sempre,
# em vez de depender do sorteio do pool de casas. O valor e a parede pela qual a
# peca encosta na praca (a que aponta de volta para o poco).
FORCED = {"smithy": "west", "church": "south", "house_large": "north"}
# de que lado do poco cada uma sai, e para onde o conector aponta
FORCED_SIDE = {"smithy": ("east", 8, 4), "church": ("north", 4, 0), "house_large": ("south", 4, 8)}

# Os blocos de madeira que trocam de especie. Cobblestone, vidro, cama, bau, etc.
# ficam como estao.
WOOD_SUFFIXES = ("planks", "stairs", "slab", "log", "wood", "door", "fence",
                 "fence_gate", "trapdoor", "pressure_plate", "button", "sign",
                 "hanging_sign", "wall_sign")
AIR = "minecraft:air"


# ---------------------------------------------------------------- leitura do save
def decode(sec):
    bs = sec.get('block_states', {})
    pal = bs.get('palette', [])
    if len(pal) <= 1:
        return pal, None
    data = bs.get('data')
    if not data:
        return pal, None
    bits = max(4, (len(pal) - 1).bit_length())
    per = 64 // bits
    mask = (1 << bits) - 1
    out = []
    for v in data:
        v &= 0xFFFFFFFFFFFFFFFF
        for k in range(per):
            if len(out) >= 4096:
                break
            out.append((v >> (k * bits)) & mask)
    return pal, out


def chunks(path):
    d = open(path, 'rb').read()
    for i in range(1024):
        off = struct.unpack_from('>I', d, i * 4)[0] >> 8
        if off == 0:
            continue
        s = off * 4096
        size = struct.unpack_from('>I', d, s)[0]
        if size == 0:
            continue
        try:
            raw = zlib.decompress(d[s + 5:s + 4 + size])
        except Exception:
            continue
        r = Reader(raw)
        t = r.u(">b", 1)
        r.string()
        yield r.payload(t)


def load_volume():
    """Le todo o canteiro para um dicionario {(x,y,z): (Name, Properties)}."""
    vol = {}
    for f in glob.glob(os.path.join(WORLD, '*.mca')):
        if os.path.getsize(f) == 0:
            continue
        for c in chunks(f):
            cx, cz = c.get('xPos'), c.get('zPos')
            if not (0 <= cx <= 3 and -1 <= cz <= 11):
                continue
            bx, bz = cx * 16, cz * 16
            for sec in c.get('sections', []):
                pal, idx = decode(sec)
                if not pal:
                    continue
                sy = sec['Y'] * 16
                for i in range(4096):
                    p = pal[idx[i]] if idx else pal[0]
                    nm = p['Name']
                    if nm.endswith('air'):
                        continue
                    y = sy + (i >> 8)
                    lz = (i >> 4) & 15
                    lx = i & 15
                    vol[(bx + lx, y, bz + lz)] = (nm, p.get('Properties'))
    return vol


# ---------------------------------------------------------------- troca de madeira
def swap_wood(name, wood):
    if wood == "oak" or not name.startswith("minecraft:oak_"):
        return name
    suffix = name[len("minecraft:oak_"):]
    if suffix in WOOD_SUFFIXES:
        return "minecraft:%s_%s" % (wood, suffix)
    return name


def block_nbt(name):
    """O `.nbt` de estrutura guarda o bloco-entidade junto; so o id ja basta para
    bau, fornalha, cama e sino nascerem certos."""
    base = name.split(":")[1]
    if base in ("chest", "furnace", "bed") or base.endswith("_bed"):
        bid = "minecraft:bed" if base.endswith("_bed") or base == "bed" else name
        return [_c_str("id", bid)]
    return None


# ---------------------------------------------------------------- recorte
def carve(vol, box, wood):
    """Recorta a caixa para uma Structure, ja com a madeira do bioma."""
    _, x0, x1, y0, y1, z0, z1 = box
    sx, sy, sz = x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1
    s = Structure(sx, sy, sz)
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                lx, ly, lz = x - x0, y - y0, z - z0
                cell = vol.get((x, y, z))
                if cell is None:
                    s.set(lx, ly, lz, AIR)               # carrega o vazio: carve
                    continue
                name, props = cell
                name = swap_wood(name, wood)
                s.set(lx, ly, lz, name, props, block_nbt(name))
    return s, (sx, sy, sz)


def local_blocks(vol, box):
    """Blocos da caixa em coordenada local, para achar porta/degrau/chao."""
    _, x0, x1, y0, y1, z0, z1 = box
    out = {}
    for (x, y, z), (name, props) in vol.items():
        if x0 <= x <= x1 and y0 <= y <= y1 and z0 <= z <= z1:
            out[(x - x0, y - y0, z - z0)] = (name.split(":")[1], props)
    return out


def entrance_z(local):
    """Z da entrada: onde estao os degraus de pedregulho na parede oeste (x=0);
    se nao houver, onde esta a porta mais a oeste."""
    stair_z = [z for (x, y, z), (n, _) in local.items()
               if x == 0 and n == "cobblestone_stairs"]
    if stair_z:
        stair_z.sort()
        return stair_z[len(stair_z) // 2]
    door_z = [z for (x, y, z), (n, p) in local.items()
              if x <= 1 and n.endswith("door") and (p or {}).get("half") == "lower"]
    if door_z:
        door_z.sort()
        return door_z[len(door_z) // 2]
    return None


def villager_spot(local, size):
    """Um piso interno (chao com ar por cima), o mais perto do centro possivel."""
    sx, sy, sz = size
    floor = {(x, z): n for (x, y, z), (n, _) in local.items()
             if y == 0 and n in ("cobblestone", "oak_planks", "spruce_planks",
                                  "acacia_planks", "birch_planks")}
    cx, cz = sx // 2, sz // 2
    best = None
    for (x, z), n in floor.items():
        if (x, 1, z) in local:            # tem bloco em cima: nao e chao livre
            continue
        d = abs(x - cx) + abs(z - cz)
        if best is None or d < best[0]:
            best = (d, x, z)
    if best:
        return best[1], best[2]
    return cx, cz


# ---------------------------------------------------------------- conectores
def wall_entrance(s, wall, size, along, pool, target="minecraft:building_entrance",
                  final="minecraft:cobblestone"):
    """Poe o conector de entrada na parede escolhida, apontando para fora.

    `along` e a coordenada ao longo da parede (o z para as paredes leste/oeste, o
    x para norte/sul). `pool` e para onde o conector puxa a proxima peca (a rua,
    no caso da casa normal; vazio, no caso da peca forcada, que e folha)."""
    sx, sy, sz = size
    place = {
        "west":  (0,      along,  "west_up"),
        "east":  (sx - 1, along,  "east_up"),
        "north": (along,  0,      "north_up"),
        "south": (along,  sz - 1, "south_up"),
    }
    if wall in ("west", "east"):
        x, z, orient = place[wall][0], place[wall][1], place[wall][2]
    else:
        x, z, orient = place[wall][0], place[wall][1], place[wall][2]
    s.jigsaw(x, 0, z, orient, "minecraft:building_entrance", target, pool, final,
             joint="aligned")


def add_house_connectors(s, local, size, biome, zombie, forced_wall=None):
    streets = "minecraft:village/%s/%sstreets" % (biome, "zombie/" if zombie else "")
    villagers = "minecraft:village/%s/villagers" % biome
    if forced_wall:
        # Peca forcada: encosta no poco por uma parede so, e nao expande mais nada
        # (pool vazio = folha). O centro dela vem do conector do poco.
        along = size[2] // 2 if forced_wall in ("west", "east") else size[0] // 2
        wall_entrance(s, forced_wall, size, along, "minecraft:empty", target="minecraft:empty")
    else:
        ez = entrance_z(local)
        if ez is None:
            ez = size[2] // 2
        # Entrada na parede oeste, apontando para a rua; o degrau reaparece.
        s.jigsaw(0, 0, ez, "west_up", "minecraft:building_entrance",
                 "minecraft:building_entrance", streets,
                 "minecraft:cobblestone_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]",
                 joint="aligned")
    vx, vz = villager_spot(local, size)
    s.jigsaw(vx, 0, vz, "up_north", "minecraft:bottom", "minecraft:bottom",
             villagers, "minecraft:cobblestone")


def build_town_center(vol, box, biome, zombie):
    """O poco dele no centro de uma praca de cascalho com tres BRACOS.

    Por que os bracos: as pecas garantidas (ferreiro/igreja/casa grande) penduram
    nas pontas dos bracos, e nao coladas na praca. Coladas, elas brigavam por
    espaco com a peca de rua vizinha (em terreno dificil a rua avancava e as
    derrubava — foi o que sumiu com a igreja na taiga). Afastadas ~4 blocos, a
    rua nao alcanca e elas aparecem sempre.

    Planta 17x17: praca 9x9 no meio (x,z 4..12), bracos de cascalho de 3 de
    largura saindo para norte/leste/sul, e a rua saindo a oeste da propria praca.
    """
    C = 17
    MID = 8               # centro
    P0, P1 = 4, 12        # praca 9x9
    s = Structure(C, 6, C)
    streets = "minecraft:village/%s/%sstreets" % (biome, "zombie/" if zombie else "")
    villagers = "minecraft:village/%s/villagers" % biome
    path = "minecraft:gravel"

    # praca (cantos abertos) + os tres bracos de cascalho ate a borda
    for x in range(P0, P1 + 1):
        for z in range(P0, P1 + 1):
            if not (x in (P0, P1) and z in (P0, P1)):
                s.set(x, 0, z, path)
    for a in range(MID - 1, MID + 2):
        for t in range(0, P0):          # bracos norte (z) e...
            s.set(a, 0, t, path)         # norte
            s.set(a, 0, C - 1 - t, path)  # sul
            s.set(t, 0, a, path)         # (oeste, so o piso; a rua sai daqui)
            s.set(C - 1 - t, 0, a, path)  # leste

    # carimba o poco dele no centro da praca
    _, x0, x1, y0, y1, z0, z1 = box
    wx, wz = x1 - x0 + 1, z1 - z0 + 1
    ox, oz = MID - wx // 2, MID - wz // 2
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            for z in range(z0, z1 + 1):
                cell = vol.get((x, y, z))
                if cell is None:
                    continue
                name, props = cell
                s.set(ox + x - x0, y - y0, oz + z - z0, name, props, block_nbt(name))

    # Uma rua so, saindo da borda OESTE da praca. A vila cresce dela (as ruas tem
    # cruzamentos proprios; o teto e a profundidade do jigsaw, nao o nº de ruas).
    s.jigsaw(0, 1, MID, "west_up", "minecraft:street", "minecraft:street",
             streets, "minecraft:structure_void", joint="aligned")

    # As pecas GARANTIDAS penduram nas PONTAS dos bracos (norte/leste/sul), longe
    # da rua. Como o poco e sempre gerado, elas aparecem sempre.
    zsuf = "_zombie" if zombie else ""
    tips = {"smithy": ("east_up", C - 1, MID), "church": ("north_up", MID, 0),
            "house_large": ("south_up", MID, C - 1)}
    for kind, (orient, x, z) in tips.items():
        pool = "recmod:village/%s/force_%s%s" % (biome, kind, zsuf)
        s.jigsaw(x, 1, z, orient, "minecraft:street", "minecraft:building_entrance",
                 pool, "minecraft:structure_void", joint="aligned")

    # moradores da praca, nas quinas do poco
    if not zombie:
        for (x, z) in [(6, 6), (10, 6), (6, 10)]:
            s.jigsaw(x, 0, z, "up_north", "minecraft:bottom", "minecraft:bottom",
                     villagers, path)
        s.jigsaw(10, 0, 10, "up_north", "minecraft:bottom", "minecraft:bottom",
                 "minecraft:village/common/iron_golem", path)
    for (x, z) in [(6, 8), (10, 8)]:
        s.jigsaw(x, 0, z, "up_north", "minecraft:bottom", "minecraft:bottom",
                 "minecraft:village/common/cats", path)
    return s


# ---------------------------------------------------------------- escrita
def write_pools():
    import io, json

    # Pecas de casa do sorteio. Ferreiro, igreja e casa grande SAIRAM daqui: agora
    # sao garantidas pelo poco (FORCED). A fazenda e MINHA (o Pedro nao fez uma);
    # fica anotada para ser facil de tirar. A house_large2 segue no sorteio como
    # variacao de casa grande extra.
    house_pieces = [
        ("house_small", 4), ("house_tower", 3), ("house_mini", 4),
        ("house_large2", 2), ("library", 1),
    ]
    ZOMBIE_PROC = {"plains": "minecraft:zombie_plains", "taiga": "minecraft:zombie_taiga",
                   "savanna": "minecraft:zombie_savanna", "snowy": {"processors": []}}
    FALLBACK = {"plains": "minecraft:village/plains/terminators",
                "taiga": "minecraft:village/taiga/terminators",
                "savanna": "minecraft:village/savanna/terminators",
                "snowy": "minecraft:village/snowy/terminators"}

    def elem(loc, weight, proc="minecraft:empty"):
        return {"element": {"element_type": "minecraft:legacy_single_pool_element",
                            "location": loc, "processors": proc, "projection": "rigid"},
                "weight": weight}

    def write(path, data):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        io.open(path, 'w', encoding='utf-8', newline='').write(json.dumps(data, indent=2) + "\n")
        print("pool", os.path.relpath(path, REPO))

    for biome in BIOME_WOOD:
        for zombie in (False, True):
            suffix = "_zombie" if zombie else ""
            els = [elem("recmod:village/built/%s_%s%s" % (biome, kind, suffix), w)
                   for kind, w in house_pieces]
            # a minha fazenda, madeira ja certa por bioma (build_village)
            els.append(elem("recmod:village/old/%s_farm%s" % (biome, suffix), 3))
            els.append({"element": {"element_type": "minecraft:empty_pool_element"}, "weight": 4})
            parts = [POOLS, biome] + (["zombie"] if zombie else []) + ["houses.json"]
            write(os.path.join(*parts),
                  {"elements": els, "fallback": FALLBACK[biome]})

        proc = ZOMBIE_PROC[biome]
        tc = {"elements": [
            elem("recmod:village/built/%s_well" % biome, 98, "minecraft:mossify_20_percent"),
            elem("recmod:village/built/%s_well_zombie" % biome, 2, proc),
        ], "fallback": "minecraft:empty"}
        write(os.path.join(POOLS, biome, "town_centers.json"), tc)

        # Um pool por peca garantida (um elemento so, sem lote vazio: tem que sair).
        # Ficam sob recmod: para o poco os referenciar direto.
        for kind in FORCED:
            for zombie in (False, True):
                suffix = "_zombie" if zombie else ""
                pool = {"elements": [
                    elem("recmod:village/built/%s_force_%s%s" % (biome, kind, suffix), 1)
                ], "fallback": "minecraft:empty"}
                path = os.path.join(REPO, 'src', 'main', 'resources', 'data', 'recmod',
                                    'worldgen', 'template_pool', 'village', biome,
                                    "force_%s%s.json" % (kind, suffix))
                write(path, pool)


def main():
    os.makedirs(OUT, exist_ok=True)
    vol = load_volume()
    total = 0
    box_by_kind = {b[0]: b for b in BUILDS}
    for biome, wood in BIOME_WOOD.items():
        for box in BUILDS:
            kind = box[0]
            for zombie in (False, True):
                suffix = "_zombie" if zombie else ""
                name = "%s_%s%s.nbt" % (biome, kind, suffix)
                if kind == "well":
                    s = build_town_center(vol, box, biome, zombie)
                else:
                    s, size = carve(vol, box, wood)
                    add_house_connectors(s, local_blocks(vol, box), size, biome, zombie)
                s.save(os.path.join(OUT, name))
                total += 1
        # as copias FORCADAS: mesma geometria, mas a entrada na parede que encosta
        # no poco e sem expandir mais nada (folha).
        for kind, wall in FORCED.items():
            box = box_by_kind[kind]
            for zombie in (False, True):
                suffix = "_zombie" if zombie else ""
                s, size = carve(vol, box, wood)
                add_house_connectors(s, local_blocks(vol, box), size, biome, zombie,
                                     forced_wall=wall)
                s.save(os.path.join(OUT, "%s_force_%s%s.nbt" % (biome, kind, suffix)))
                total += 1
    print("gerados", total, "nbt em", os.path.relpath(OUT, REPO))
    write_pools()


if __name__ == "__main__":
    main()
