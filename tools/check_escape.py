# -*- coding: utf-8 -*-
"""Confere se TODA dimensao tem saida, e se a saida esta inteira.

⚠️ POR QUE ISTO EXISTE, E POR QUE ELE E O MAIS SERIO DOS TRES. Desde a v1.70.0 a fita e
so ida: a saida e o unico caminho de volta das quinze dimensoes. Isso mudou a gravidade de
toda falha desta parte do mod — antes, um aparelho torto era um enfeite torto; agora e um
jogador preso para sempre num mundo de onde ele nao pode sair nem morrendo (a marca de
volta sobrevive a morte, e o respawn e na propria dimensao).

Nenhuma das falhas abaixo da erro de compilacao:

  - `dimensionId()` devolvendo um nome que nao existe -> a dimensao nao acha o proprio
    perfil, e o diretor do Espelho nunca planta nada nela.
  - `exitAnchor` faltando -> nao ha ponto onde plantar o Espelho.
  - o renderizador ou a textura do Espelho faltando -> ele nasce INVISIVEL, e uma saida
    invisivel e o mesmo que nao ter saida.
  - falta de linha de lang -> o aviso do Espelho sai como "message.recmod.mirror_turn_back"
    cru, e a unica instrucao que o jogador recebe vira lixo na tela.

Rodar:
    python tools/check_escape.py
"""
import io
import json
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "recmod")
DATA = os.path.join(RES, "data", "recmod")
JAVA = os.path.join(REPO, "src", "main", "java", "net", "vhsworld", "rec")
DIM = os.path.join(JAVA, "worldgen", "dim")

problems = []


def bad(what):
    problems.append(what)
    print("  XX " + what)


def read(path):
    with io.open(path, encoding="utf-8") as fh:
        return fh.read()


def main():
    stems = sorted(f[:-5] for f in os.listdir(os.path.join(DATA, "dimension"))
                   if f.endswith(".json"))
    profiles = read(os.path.join(DIM, "DimensionProfile.java"))

    # ------------------------------------------------------------ 1. o reparto
    print("=== 1. toda dimensao tem metodo de saida?")
    assigned = dict(re.findall(r'DimensionProfile\("([a-z_]+)".*?ExitMethod\.([A-Z]+)\)',
                               profiles, re.S))
    for name in stems:
        if name not in assigned:
            bad("a dimensao '%s' nao tem ExitMethod no DimensionProfile — ela nao tem "
                "volta" % name)
        else:
            print("  ok %-16s %s" % (name, assigned[name]))

    # ------------------------------------------------------------ 2. o id de cada gerador
    #
    # A pergunta e sempre a mesma do check_dimensions: as pontas concordam? Aqui as pontas
    # sao o `dimensionId()` do Java e o nome do arquivo em data/recmod/dimension/.
    print("\n=== 2. o dimensionId() de cada gerador bate com o arquivo da dimensao?")
    claimed = {}
    for f in sorted(os.listdir(DIM)):
        if not f.endswith("ChunkGenerator.java"):
            continue
        src = read(os.path.join(DIM, f))
        # ⚠️ O TRONCO NAO E UMA DIMENSAO. `StampChunkGenerator` termina em
        # "ChunkGenerator.java" como as quinze, mas e a classe ABSTRATA de que doze delas
        # herdam — cobrar um `dimensionId()` dele seria pedir que a forma tivesse nome.
        # Quem responde e quem e concreto.
        if re.search(r'public abstract class ', src):
            continue
        found = re.search(r'public String dimensionId\(\)\s*\{\s*return "([a-z_]+)";', src)
        if not found:
            bad("%s nao implementa dimensionId()" % f)
            continue
        if not re.search(r'public BlockPos exitAnchor\(int rx, int rz\)', src):
            bad("%s nao implementa exitAnchor()" % f)
        dim_id = found.group(1)
        if dim_id in claimed:
            bad("'%s' e reivindicado por %s E por %s" % (dim_id, claimed[dim_id], f))
        claimed[dim_id] = f
        if dim_id not in stems:
            bad("%s diz ser a dimensao '%s', que NAO EXISTE em data/recmod/dimension/"
                % (f, dim_id))
        else:
            print("  ok %-34s -> %s" % (f, dim_id))

    for name in stems:
        if name not in claimed:
            bad("nenhum gerador diz ser a dimensao '%s' — ela nasce sem sala de saida"
                % name)

    # ------------------------------------------------------------ 3. lang
    print("\n=== 3. todo texto que o codigo pede existe nas duas linguas?")
    keys = set()
    for root, _, files in os.walk(JAVA):
        for f in files:
            if f.endswith(".java"):
                src = read(os.path.join(root, f))
                keys |= set(re.findall(r'"((?:message|note)\.recmod\.[a-z_]+)"', src))
    for lang in ("en_us", "pt_br"):
        table = json.loads(read(os.path.join(ASSETS, "lang", lang + ".json")))
        missing = sorted(k for k in keys if k not in table)
        if missing:
            for k in missing:
                bad("falta '%s' no lang %s" % (k, lang))
        else:
            print("  ok %s: as %d chaves do codigo estao la" % (lang, len(keys)))

    # ------------------------------------------------------------ 4. a porta
    #
    # ⚠️ A PORTA E UMA PORTA DE VERDADE (v1.73.0) e nao mais um cubo: sao 32 variantes de
    # blockstate (facing x half x hinge x open), oito modelos e DUAS texturas — a metade de
    # cima e a de baixo. Faltar uma variante nao da erro nenhum: da uma porta invisivel num
    # dos quatro lados, ou que abre para o lado errado. E as texturas precisam do .mcmeta,
    # senao o vazio do painel fica parado e ela vira uma porta de ferro comum.
    print("\n=== 4. a porta de saida")
    registry = read(os.path.join(JAVA, "init", "ModBlocks.java"))
    if 'BLOCKS.register("exit_door"' not in registry:
        bad("o bloco 'exit_door' nao esta no ModBlocks")

    state_path = os.path.join(ASSETS, "blockstates", "exit_door.json")
    if not os.path.exists(state_path):
        bad("falta o blockstate da porta")
    else:
        variants = json.loads(read(state_path)).get("variants", {})
        if len(variants) != 32:
            bad("o blockstate da porta tem %d variantes, e uma porta tem 32" % len(variants))
        else:
            print("  ok blockstate com as 32 variantes")
        wanted = {v["model"].split("/")[-1] for v in variants.values()}
        missing = sorted(m for m in wanted
                         if not os.path.exists(
                             os.path.join(ASSETS, "models", "block", m + ".json")))
        for m in missing:
            bad("o blockstate aponta para o modelo '%s', que nao existe" % m)
        if not missing:
            print("  ok os %d modelos que o blockstate pede existem" % len(wanted))

    for half in ("top", "bottom"):
        png = os.path.join(ASSETS, "textures", "block", "exit_door_%s.png" % half)
        if not os.path.exists(png) or os.path.getsize(png) == 0:
            bad("falta a textura da metade '%s' da porta" % half)
        elif not os.path.exists(png + ".mcmeta"):
            bad("a textura '%s' nao tem .mcmeta — o vazio da porta fica parado" % half)
        else:
            print("  ok textura %s (animada)" % half)

    # ------------------------------------------------------------ 5. a entidade
    #
    # ⚠️ O ESPELHO E A SAIDA DE TREZE DAS QUINZE, e ele nao e um bloco: e uma entidade
    # posta no mundo em tempo de jogo pelo MirrorDirector. Se o registro, o renderizador ou
    # a textura faltarem, ele nasce invisivel — e uma saida invisivel e a mesma coisa que
    # nao ter saida, sem nenhum erro no log para dizer isso.
    print("\n=== 5. a entidade do Espelho")
    entities = read(os.path.join(JAVA, "init", "ModEntities.java"))
    if 'ENTITIES.register("mirror"' not in entities:
        bad("a entidade 'mirror' nao esta no ModEntities")
    else:
        print("  ok registrada no ModEntities")

    client = read(os.path.join(JAVA, "client", "ClientSetup.java"))
    if "MirrorRenderer" not in client:
        bad("o MirrorRenderer nao esta registrado no ClientSetup — o Espelho fica invisivel")
    elif "MirrorModel.LAYER" not in client:
        bad("a camada do MirrorModel nao esta registrada — o modelo nao assa")
    else:
        print("  ok renderizador e camada ligados")

    for label, parts in (("textura", ("textures", "entity", "mirror.png")),
                         ("rosto do susto", ("textures", "gui", "scare", "face_skull.png"))):
        path = os.path.join(ASSETS, *parts)
        if not os.path.exists(path) or os.path.getsize(path) == 0:
            bad("falta a %s (%s)" % (label, os.path.relpath(path, REPO)))
        else:
            print("  ok " + label)

    scares = os.path.join(ASSETS, "textures", "gui", "scare")
    count = len([f for f in os.listdir(scares) if f.endswith(".png")]) if os.path.isdir(scares) else 0
    if count < 2:
        bad("so ha %d rosto(s) de susto — com um so, o segundo susto ja e previsivel" % count)
    else:
        print("  ok %d rostos no sorteio do susto" % count)

    if problems:
        print("\n=== %d PROBLEMA(S) ===" % len(problems))
        for p in problems:
            print("  - " + p)
        sys.exit(1)
    print("\n=== toda dimensao tem saida, e a saida esta inteira ===")


if __name__ == "__main__":
    main()
