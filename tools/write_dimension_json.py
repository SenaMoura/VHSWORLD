# -*- coding: utf-8 -*-
"""Escreve dimension / dimension_type / biome das seis dimensoes novas.

Um script e nao 18 arquivos a mao: os tres JSON de uma dimensao repetem 90% do
conteudo, e e exatamente nos 10% que mora o defeito (o `grassrooms.json` que ja estava
no repo apontava para um `dimension_type` que nao existia, e o bioma dele tinha ZERO
bytes). Gerando, os campos que tem que ser iguais sao iguais por construcao.
"""
import json
import os

REPO = r"C:\Users\Hamilton\Downloads\GitHub\VHSWORLD"
DATA = os.path.join(REPO, "src", "main", "resources", "data", "recmod")


def dim_type(**over):
    base = {
        "ultrawarm": False,
        "natural": False,
        "piglin_safe": False,
        "respawn_anchor_works": False,
        "bed_works": False,
        "has_raids": False,
        "has_skylight": False,
        "has_ceiling": False,
        "coordinate_scale": 1.0,
        "ambient_light": 0.0,
        "logical_height": 128,
        "height": 128,
        "min_y": 0,
        "infiniburn": "#minecraft:infiniburn_overworld",
        "effects": "minecraft:the_nether",
        "monster_spawn_light_level": 0,
        "monster_spawn_block_light_limit": 0,
    }
    base.update(over)
    return base


def biome(**over):
    base = {
        "temperature": 0.5,
        "downfall": 0.0,
        "has_precipitation": False,
        "effects": {
            "fog_color": 0,
            "sky_color": 0,
            "water_color": 4159204,
            "water_fog_color": 329011,
        },
        # Sem trilha: as pastas de som destas seis estao vazias, e um bioma que aponta
        # para um evento de som que nao existe enche o log do cliente de aviso.
        "spawners": {
            "monster": [], "creature": [], "ambient": [], "axolotls": [],
            "underground_water_creature": [], "water_creature": [],
            "water_ambient": [], "misc": [],
        },
        "spawn_costs": {},
        "carvers": {},
        "features": [],
    }
    effects = over.pop("effects", None)
    if effects:
        base["effects"].update(effects)
    base.update(over)
    return base


DIMS = {
    # VILLAGE: a unica de dia. `fixed_time` 6000 = meio-dia travado; a referencia do
    # Pedro e sol a pino e ceu azul chapado, e uma noite ali daria o alivio de nao ver.
    "village": dict(
        type=dim_type(has_skylight=True, natural=True, bed_works=True,
                      effects="minecraft:overworld", fixed_time=6000),
        biome=biome(temperature=0.8, downfall=0.4, has_precipitation=False,
                    effects={"fog_color": 12638463, "sky_color": 2054112,
                             "grass_color": 9551193, "foliage_color": 7716687}),
    ),
    # GRASSROOMS: a unica ILUMINADA. Sem ceu (e uma sala fechada), mas com
    # `ambient_light` alto: a luz nao vem de lugar nenhum, o que e o assunto do liminal
    # space. Quem mantem a grama viva sao os blocos `light` que o gerador planta — ver o
    # comentario no GrassroomsChunkGenerator.
    "grassrooms": dict(
        type=dim_type(ambient_light=0.9, effects="minecraft:overworld"),
        biome=biome(temperature=0.7, effects={"fog_color": 15265267, "sky_color": 15265267,
                                              "grass_color": 8437607, "foliage_color": 7716687}),
    ),
    # TRAIN: vazio preto em volta da linha.
    "train": dict(
        type=dim_type(ambient_light=0.08),
        biome=biome(effects={"fog_color": 0, "sky_color": 0}),
    ),
    # UNDER PRESSURE: tem ceu para a luz morrer com a profundidade — 92 blocos de coluna
    # so assustam se der para ver a superficie ficando longe. `height` 128 cobre o leito
    # em y=4 e a linha d'agua em y=96.
    "under_pressure": dict(
        type=dim_type(has_skylight=True, effects="minecraft:overworld", fixed_time=6000),
        biome=biome(temperature=0.4,
                    effects={"fog_color": 1250067, "sky_color": 2437746,
                             "water_color": 1523067, "water_fog_color": 656387}),
    ),
    # BIBLIOTECA: o breu. `ambient_light` 0 e sem ceu: aqui o jogador carrega a luz.
    "biblioteca": dict(
        type=dim_type(ambient_light=0.0),
        biome=biome(effects={"fog_color": 1381653, "sky_color": 0}),
    ),
    # PARKOURLAND: a gaiola tem 185 de altura mais a base, entao `height` sobe para 256 —
    # com 128 o tampo de madeira seria cortado e o topo nao existiria.
    "parkourland": dict(
        type=dim_type(height=256, logical_height=256, ambient_light=0.35,
                      effects="minecraft:overworld"),
        biome=biome(temperature=0.7, effects={"fog_color": 8158332, "sky_color": 8158332}),
    ),
}


def dump(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")
    print("  ", os.path.relpath(path, REPO))


for name, spec in DIMS.items():
    print("===", name)
    dump(os.path.join(DATA, "dimension_type", name + ".json"), spec["type"])
    dump(os.path.join(DATA, "worldgen", "biome", name + ".json"), spec["biome"])
    dump(os.path.join(DATA, "dimension", name + ".json"), {
        "type": "recmod:" + name,
        "generator": {
            "type": "recmod:" + name,
            "biome_source": {"type": "minecraft:fixed", "biome": "recmod:" + name},
        },
    })
