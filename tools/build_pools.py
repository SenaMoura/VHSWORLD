# -*- coding: utf-8 -*-
"""Escreve os template_pool que trocam as casas do vanilla pelas antigas.

So o pool de CASAS e sobrescrito. Ruas, pracas, decoracao e espacamento
continuam sendo do jogo — a vila muda de roupa, nao de comportamento.
"""
import io, json, os

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    'src', 'main', 'resources', 'data', 'minecraft', 'worldgen', 'template_pool', 'village')

# fallback lido de cada arquivo do vanilla; nao adianta adivinhar
FALLBACKS = {
    ("plains",  False): "minecraft:village/plains/terminators",
    ("plains",  True):  "minecraft:village/plains/terminators",
    ("desert",  False): "minecraft:village/desert/terminators",
    ("desert",  True):  "minecraft:village/desert/zombie/terminators",
    ("savanna", False): "minecraft:village/savanna/terminators",
    ("savanna", True):  "minecraft:village/savanna/zombie/terminators",
    ("taiga",   False): "minecraft:village/taiga/terminators",
    ("taiga",   True):  "minecraft:village/taiga/terminators",
    ("snowy",   False): "minecraft:village/snowy/terminators",
    ("snowy",   True):  "minecraft:village/snowy/terminators",
}

WEIGHTS = [("house_small", 4), ("house_large", 3), ("smithy", 1)]


def pool(biome, zombie):
    suffix = "_zombie" if zombie else ""
    elements = []
    for kind, weight in WEIGHTS:
        elements.append({
            "element": {
                "element_type": "minecraft:legacy_single_pool_element",
                "location": "recmod:village/old/%s_%s%s" % (biome, kind, suffix),
                "processors": "minecraft:mossify_10_percent",
                "projection": "rigid",
            },
            "weight": weight,
        })
    # Lote vazio, como o vanilla: sem ele a vila nasce entupida, sem respiro.
    elements.append({"element": {"element_type": "minecraft:empty_pool_element"}, "weight": 4})
    return {"elements": elements, "fallback": FALLBACKS[(biome, zombie)]}


def main():
    for (biome, zombie), _ in FALLBACKS.items():
        parts = [ROOT, biome] + (["zombie"] if zombie else []) + ["houses.json"]
        path = os.path.join(*parts)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        io.open(path, 'w', encoding='utf-8', newline='').write(
            json.dumps(pool(biome, zombie), indent=2) + "\n")
        print("escrito", os.path.relpath(path, ROOT))


if __name__ == "__main__":
    main()
