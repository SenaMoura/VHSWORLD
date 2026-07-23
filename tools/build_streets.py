# -*- coding: utf-8 -*-
"""Deixa as ruas da vila de CASCALHO, sem reescrever peca nenhuma.

O caminho de terra so existe desde a 1.9. Antes dele, a estrada da vila era de
cascalho — e e cascalho que aparece na vila da referencia. Mas reescrever as
pecas de rua do jogo (sao dezenas, cheias de conectores) seria trabalho enorme
para trocar UM bloco.

A saida: manter as pecas do vanilla e vesti-las com um PROCESSADOR nosso. O pool
continua apontando para `minecraft:village/<bioma>/streets/...`, so que com
`recmod:gravel_roads` no lugar do processador original — e ele troca caminho de
terra (e o arenito liso do deserto) por cascalho na hora de colocar.

Rodar: python tools/build_streets.py  [caminho do 1.20.1.jar]
"""
import io, json, os, sys, zipfile

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
POOLS = os.path.join(REPO, 'src', 'main', 'resources', 'data', 'minecraft',
                     'worldgen', 'template_pool', 'village')
PROCESSORS = os.path.join(REPO, 'src', 'main', 'resources', 'data', 'recmod',
                          'worldgen', 'processor_list')

VANILLA_JAR = os.path.expandvars(r'%APPDATA%\.minecraft\versions\1.20.1\1.20.1.jar')

BIOMES = ["plains", "desert", "savanna", "snowy", "taiga"]

# O que vira cascalho. O arenito liso entra porque a rua do deserto e feita dele
# — e so a RUA passa por aqui, entao as casas de arenito nao correm risco.
ROAD_BLOCKS = ["minecraft:dirt_path", "minecraft:smooth_sandstone"]


def processor_list():
    return {
        "processors": [
            {
                "processor_type": "minecraft:rule",
                "rules": [
                    {
                        "input_predicate": {
                            "predicate_type": "minecraft:block_match",
                            "block": block,
                        },
                        "location_predicate": {"predicate_type": "minecraft:always_true"},
                        "output_state": {"Name": "minecraft:gravel"},
                    }
                    for block in ROAD_BLOCKS
                ],
            }
        ]
    }


def main():
    jar = sys.argv[1] if len(sys.argv) > 1 else VANILLA_JAR
    if not os.path.exists(jar):
        sys.exit("nao achei o jar do jogo em %s (passe o caminho como argumento)" % jar)

    os.makedirs(PROCESSORS, exist_ok=True)
    write(os.path.join(PROCESSORS, 'gravel_roads.json'), processor_list())

    with zipfile.ZipFile(jar) as z:
        for biome in BIOMES:
            for zombie in (False, True):
                inner = 'data/minecraft/worldgen/template_pool/village/%s/%sstreets.json' % (
                    biome, 'zombie/' if zombie else '')
                if inner not in z.namelist():
                    print('  (sem %s no vanilla, pulando)' % inner)
                    continue
                pool = json.loads(z.read(inner))
                for element in pool["elements"]:
                    inner_element = element["element"]
                    if inner_element.get("element_type") == "minecraft:empty_pool_element":
                        continue
                    inner_element["processors"] = "recmod:gravel_roads"
                out = [POOLS, biome] + (['zombie'] if zombie else []) + ['streets.json']
                write(os.path.join(*out), pool)


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    io.open(path, 'w', encoding='utf-8', newline='').write(json.dumps(data, indent=2) + "\n")
    print("escrito", os.path.relpath(path, REPO))


if __name__ == "__main__":
    main()
