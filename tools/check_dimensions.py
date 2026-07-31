# -*- coding: utf-8 -*-
"""Confere se cada dimensao esta LIGADA em todas as pontas, sem abrir o jogo.

⚠️ POR QUE ISTO EXISTE. Uma dimensao do mod so funciona se SEIS coisas concordarem sobre
o nome dela: o level stem, o dimension_type, o bioma, o codec do gerador no Java, o item
da fita e a aba do criativo. Errar qualquer uma NAO da erro de compilacao e nao para o
jogo — da uma fita que gira em falso, ou uma dimensao que nao carrega e leva as outras
com ela.

E nao e hipotese. Quando peguei este trabalho, o `dimension/grassrooms.json` apontava para
um `recmod:grassrooms_type` que nao existia e o bioma dele tinha ZERO BYTES; e a INSIDIOUS
estava pronta no Java, com gerador, planta e .bin, mas sem textura de fita, sem modelo, sem
lang e fora da aba do criativo — ou seja, existia e era inalcancavel. Nenhuma das duas
falhas aparece em nada que o compilador leia.

Rodar:
    python tools/check_dimensions.py
"""
import json
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")
DATA = os.path.join(RES, "data", "recmod")
ASSETS = os.path.join(RES, "assets", "recmod")
JAVA = os.path.join(REPO, "src", "main", "java", "net", "vhsworld", "rec")

problems = []


def bad(what):
    problems.append(what)
    print("  XX " + what)


def ok(what):
    print("  ok " + what)


def read(path):
    with open(path, encoding="utf-8") as fh:
        return fh.read()


def main():
    stems = sorted(f[:-5] for f in os.listdir(os.path.join(DATA, "dimension"))
                   if f.endswith(".json"))
    print("dimensoes encontradas: %s\n" % ", ".join(stems))

    generators = read(os.path.join(JAVA, "worldgen", "ModChunkGenerators.java"))
    items = read(os.path.join(JAVA, "init", "ModItems.java"))
    tabs = read(os.path.join(JAVA, "init", "ModCreativeTabs.java"))
    profiles = read(os.path.join(JAVA, "worldgen", "dim", "DimensionProfile.java"))
    en = json.loads(read(os.path.join(ASSETS, "lang", "en_us.json")))
    pt = json.loads(read(os.path.join(ASSETS, "lang", "pt_br.json")))

    # Todo o Java de uma vez, para poder perguntar "alguem le a peca desta dimensao?".
    all_java = []
    for root, _, files in os.walk(JAVA):
        for f in files:
            if f.endswith(".java"):
                all_java.append(read(os.path.join(root, f)))
    all_java = "\n".join(all_java)

    for name in stems:
        print("=== %s" % name)
        stem_path = os.path.join(DATA, "dimension", name + ".json")

        if os.path.getsize(stem_path) == 0:
            bad("%s: o level stem esta VAZIO" % name)
            continue
        try:
            stem = json.loads(read(stem_path))
        except Exception as e:
            bad("%s: level stem nao e JSON valido (%s)" % (name, e))
            continue

        # 1. o dimension_type existe?
        dim_type = stem.get("type", "")
        type_file = os.path.join(DATA, "dimension_type", dim_type.split(":")[-1] + ".json")
        if not dim_type.startswith("recmod:"):
            ok("%s: usa dimension_type de fora (%s)" % (name, dim_type))
        elif not os.path.exists(type_file):
            bad("%s: aponta para o dimension_type '%s', que NAO EXISTE" % (name, dim_type))
        elif os.path.getsize(type_file) == 0:
            bad("%s: o dimension_type '%s' tem zero bytes" % (name, dim_type))
        else:
            json.loads(read(type_file))
            ok("%s: dimension_type ok" % name)

        # 2. o gerador esta registrado no Java?
        gen = stem.get("generator", {}).get("type", "")
        if gen.startswith("recmod:"):
            short = gen.split(":")[-1]
            if 'GENERATORS.register("%s"' % short not in generators:
                bad("%s: o gerador '%s' nao esta no ModChunkGenerators" % (name, gen))
            else:
                ok("%s: gerador registrado" % name)
        else:
            ok("%s: usa gerador de fora (%s)" % (name, gen))

        # 3. o bioma existe e tem conteudo?
        biome = stem.get("generator", {}).get("biome_source", {}).get("biome")
        if biome is None:
            settings = stem.get("generator", {}).get("settings", {})
            biome = settings.get("biome") if isinstance(settings, dict) else None
        if biome is None:
            bad("%s: nao da para dizer qual bioma o gerador usa" % name)
        elif biome.startswith("recmod:"):
            biome_file = os.path.join(DATA, "worldgen", "biome", biome.split(":")[-1] + ".json")
            if not os.path.exists(biome_file):
                bad("%s: o bioma '%s' NAO EXISTE" % (name, biome))
            elif os.path.getsize(biome_file) == 0:
                bad("%s: o bioma '%s' tem ZERO BYTES" % (name, biome))
            else:
                json.loads(read(biome_file))
                ok("%s: bioma ok" % name)

        # 4. a peca assada existe — SE o gerador pedir uma.
        #
        # ⚠️ ANTES ISTO EXIGIA O .bin DE TODA DIMENSAO, e a exigencia era errada: ela
        # media a regra de ontem em vez da pergunta. Ate 2026-07-30 toda dimensao do mod
        # carimbava alguma coisa que o Pedro construiu, entao "tem .bin?" e "esta ligada?"
        # davam a mesma resposta por acidente. A STONELAND e a ESCRITORIO sao 100% Java e
        # nao carimbam peca nenhuma — reprovar as duas por falta de um arquivo que elas
        # nao leem seria o teste virar burocracia.
        #
        # A pergunta certa e a de sempre: as pontas concordam? Ou seja — quem CHAMA
        # `PieceSet.get("x")` precisa do `x.bin`, e quem nao chama nao precisa. E o
        # contrario tambem e defeito e continua reprovando: um .bin que ninguem le e
        # trabalho do Pedro que nao entrou no jogo, e isso e pior do que um arquivo a
        # mais, porque ninguem sente falta.
        bin_path = os.path.join(DATA, "dimension_pieces", name + ".bin")
        has_bin = os.path.exists(bin_path) and os.path.getsize(bin_path) > 0
        wants = 'PieceSet.get("%s")' % name in all_java
        if wants and has_bin:
            ok("%s: pecas assadas (%.1f KB)" % (name, os.path.getsize(bin_path) / 1024.0))
        elif wants:
            bad("%s: o gerador le PieceSet.get(\"%s\") e nao ha dimension_pieces/%s.bin"
                % (name, name, name))
        elif has_bin:
            bad("%s: ha um %s.bin de %.1f KB que gerador nenhum le — peca construida "
                "fora do jogo" % (name, name, os.path.getsize(bin_path) / 1024.0))
        else:
            ok("%s: sem peca, e nao pede nenhuma (dimensao 100%% Java)" % name)

        # 5. a fita: item, modelo, textura, aba do criativo e as duas linguas
        tape = "tape_" + name
        if 'ITEMS.register("%s"' % tape not in items:
            bad("%s: nao ha item %s no ModItems" % (name, tape))
        elif '"%s")' % name not in items:
            bad("%s: o item %s existe mas nao aponta para a dimensao '%s'" % (name, tape, name))
        else:
            ok("%s: item da fita ok" % name)

        for kind, folder, ext in (("modelo", ("models", "item"), ".json"),
                                  ("textura", ("textures", "item"), ".png")):
            path = os.path.join(ASSETS, *folder, tape + ext)
            if not os.path.exists(path) or os.path.getsize(path) == 0:
                bad("%s: falta a %s da fita (%s)" % (name, kind, os.path.relpath(path, REPO)))
            else:
                ok("%s: %s da fita ok" % (name, kind))

        upper = re.sub(r"[^A-Z_]", "", tape.upper())
        if "ModItems.TAPE_%s" % name.upper() not in tabs:
            bad("%s: a fita nao esta na aba do criativo (e nao tem receita: e o unico "
                "lugar de onde ela sai)" % name)
        else:
            ok("%s: na aba do criativo" % name)

        for lang, table in (("en_us", en), ("pt_br", pt)):
            if "item.recmod." + tape not in table:
                bad("%s: falta a linha de %s no lang %s" % (name, tape, lang))
        if "item.recmod." + tape in en and "item.recmod." + tape in pt:
            ok("%s: lang nas duas linguas" % name)

        # 6. o perfil no DimensionProfile
        if 'DimensionProfile("%s"' % name not in profiles:
            bad("%s: nao ha linha no DimensionProfile" % name)
        else:
            ok("%s: perfil ok" % name)
        print()

    if problems:
        print("=== %d PROBLEMA(S) ===" % len(problems))
        for p in problems:
            print("  - " + p)
        sys.exit(1)
    print("=== todas as %d dimensoes estao ligadas em todas as pontas ===" % len(stems))


if __name__ == "__main__":
    main()
