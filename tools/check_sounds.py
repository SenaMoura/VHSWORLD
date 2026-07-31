# -*- coding: utf-8 -*-
"""Confere se cada som esta LIGADO nas tres pontas, sem abrir o jogo.

⚠️ POR QUE ISTO EXISTE. Um som do mod so toca se TRES arquivos concordarem sobre o nome
dele: o `sounds.json` (que diz quais .ogg formam o evento), o `ModSounds.java` (que
registra o evento) e quem PEDE o evento — o campo `effects.music` de um bioma, o
`biome_modifier` do overworld, ou uma chamada em Java.

Nenhuma das quatro falhas possiveis da erro de compilacao, e nenhuma para o jogo:

  - .ogg citado no sounds.json que nao existe no disco -> o evento simplesmente nao toca,
    e o log solta uma linha que ninguem le.
  - evento no sounds.json sem registro no ModSounds -> idem.
  - evento registrado no Java sem entrada no sounds.json -> idem.
  - bioma pedindo um evento que nao existe -> a dimensao fica MUDA, e "dimensao sem
    musica" e indistinguivel de "ainda nao botei musica nessa".

Foi por isso que a MALL quase entrou muda: a pasta dela so tinha um stinger de 23s, e o
gerador de trilha, ao nao achar musica, teria escrito um evento vazio sem reclamar.

Rodar:
    python tools/check_sounds.py
"""
import json
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "recmod")
DATA = os.path.join(RES, "data", "recmod")
JAVA = os.path.join(REPO, "src", "main", "java", "net", "vhsworld", "rec")

problems = []


def bad(what):
    problems.append(what)
    print("  XX " + what)


def read(path):
    with open(path, encoding="utf-8") as fh:
        return fh.read()


def main():
    sounds = json.loads(read(os.path.join(ASSETS, "sounds.json")))
    java = read(os.path.join(JAVA, "item", "ModSounds.java"))
    registered = set(re.findall(r'SOUND_EVENTS\.register\("([a-z0-9_]+)"', java))

    print("=== 1. os .ogg que o sounds.json cita existem?")
    missing = 0
    for event, body in sorted(sounds.items()):
        for entry in body.get("sounds", []):
            name = entry["name"] if isinstance(entry, dict) else entry
            path = os.path.join(ASSETS, "sounds", name.split(":", 1)[-1] + ".ogg")
            if not os.path.exists(path) or os.path.getsize(path) == 0:
                bad("o evento '%s' cita '%s', e nao ha %s"
                    % (event, name, os.path.relpath(path, REPO)))
                missing += 1
        if not body.get("sounds"):
            bad("o evento '%s' nao tem som nenhum dentro" % event)
    if not missing:
        print("  ok todos os %d eventos apontam para arquivo que existe" % len(sounds))

    print("\n=== 2. sounds.json e ModSounds falam dos mesmos eventos?")
    for event in sorted(sounds):
        if event not in registered:
            bad("'%s' esta no sounds.json e nao esta registrado no ModSounds" % event)
    for event in sorted(registered):
        if event not in sounds:
            bad("'%s' esta registrado no ModSounds e nao esta no sounds.json" % event)
    print("  ok %d eventos dos dois lados" % len(registered & set(sounds)))

    print("\n=== 3. toda dimensao tem trilha, e a trilha existe?")
    stems = sorted(f[:-5] for f in os.listdir(os.path.join(DATA, "dimension"))
                   if f.endswith(".json"))
    for name in stems:
        biome_path = os.path.join(DATA, "worldgen", "biome", name + ".json")
        if not os.path.exists(biome_path):
            continue
        effects = json.loads(read(biome_path)).get("effects", {})
        music = effects.get("music")
        if music is None:
            bad("a dimensao '%s' nao tem `effects.music` no bioma — ela nasce MUDA" % name)
            continue
        event = music["sound"].split(":", 1)[-1]
        if event not in sounds:
            bad("o bioma '%s' pede '%s', que nao existe no sounds.json" % (name, music["sound"]))
        else:
            print("  ok %-16s %s (%d faixa(s))"
                  % (name, event, len(sounds[event]["sounds"])))

    print("\n=== 4. o overworld, que nao tem bioma nosso")
    mod_path = os.path.join(DATA, "forge", "biome_modifier", "overworld_music.json")
    if not os.path.exists(mod_path):
        bad("nao ha o biome_modifier `overworld_music` — o overworld fica mudo")
    else:
        modifier = json.loads(read(mod_path))
        event = modifier["music"].split(":", 1)[-1]
        serializers = read(os.path.join(JAVA, "worldgen", "ModBiomeModifiers.java"))
        short = modifier["type"].split(":", 1)[-1]
        if 'SERIALIZERS.register("%s"' % short not in serializers:
            bad("o tipo '%s' nao esta no ModBiomeModifiers — o Forge descarta o json"
                % modifier["type"])
        elif event not in sounds:
            bad("o overworld pede '%s', que nao existe no sounds.json" % modifier["music"])
        else:
            print("  ok overworld         %s (%d faixa(s))" % (event, len(sounds[event]["sounds"])))

    total = sum(os.path.getsize(os.path.join(root, f))
                for root, _, files in os.walk(os.path.join(ASSETS, "sounds"))
                for f in files)
    print("\n%.1f MB de audio no jar" % (total / 1e6))

    if problems:
        print("\n=== %d PROBLEMA(S) ===" % len(problems))
        for p in problems:
            print("  - " + p)
        sys.exit(1)
    print("=== todo som esta ligado nas tres pontas ===")


if __name__ == "__main__":
    main()
