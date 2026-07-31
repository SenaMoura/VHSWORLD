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
        biome=biome(temperature=0.7, effects={"fog_color": 15922165, "sky_color": 15922165,
                                              "grass_color": 8437607, "foliage_color": 7716687}),
    ),
    # TRAIN: neblina CINZA, e nao mais o preto com que ela nasceu. O pedido de
    # 2026-07-30 e "neblina cinza claro pra escuro como na foto", e o preto puro nao era
    # neblina nenhuma — era ausencia. Com 0x3C3C40 o paredao de pedra dos dois lados da
    # linha (ver TrainChunkGenerator.cliffs) aparece como massa escura e some na bruma.
    "train": dict(
        type=dim_type(ambient_light=0.08),
        biome=biome(effects={"fog_color": 3947584, "sky_color": 3947584}),
    ),
    # UNDER PRESSURE: tem ceu para a luz morrer com a profundidade — 92 blocos de coluna
    # so assustam se der para ver a superficie ficando longe. `height` 128 cobre o leito
    # em y=4 e a linha d'agua em y=96.
    "under_pressure": dict(
        # `fixed_time` 18000 (meia-noite) desde 2026-07-30: "under pressure deixar de
        # noite". Com ceu ligado e `ambient_light` 0, a superficie fica na luz 4 da lua —
        # da para ver que ha agua e nao da para ver o que ha nela.
        type=dim_type(has_skylight=True, effects="minecraft:overworld", fixed_time=18000),
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
        # 2026-07-30: "deixar de noite com uma neblina muito densa e escura pra dificultar
        # o parkour". A luz ambiente caiu de 0.35 para 0.15 e o cinza claro do fundo virou
        # quase preto. NAO caiu para zero de proposito: a bruma e para tirar o PLANEJAMENTO
        # de tres pulos a frente, nao para tirar o pulo seguinte — sem nenhuma luz o
        # parkour deixaria de ser dificil e passaria a ser impossivel, que e outra coisa.
        type=dim_type(height=256, logical_height=256, ambient_light=0.15,
                      effects="minecraft:overworld"),
        biome=biome(temperature=0.7, effects={"fog_color": 723726, "sky_color": 723726}),
    ),

    # ------------------------------------------------------------------ o lote de 3
    #
    # As tres do bloco "NOVAS DIMENSOES" das notas de 2026-07-30.

    # STONELAND: a silhueta do overworld em pedregulho. Sem ceu desenhado (efeito do
    # nether) para o cinza da neblina ocupar tambem o alto — com ceu, o azul entregaria
    # que aquilo e um overworld pintado, e o assunto e justamente que NAO e.
    # `ambient_light` 0.25 e o minimo em que a sombra do relevo ainda desenha o morro.
    "stoneland": dict(
        type=dim_type(ambient_light=0.25),
        biome=biome(effects={"fog_color": 5789792, "sky_color": 5789792}),
    ),

    # ESCRITORIO: quase sem luz ambiente de proposito — quem ilumina o andar sao as
    # luminarias do forro (`recmod:white_light`), e e por isso que o corredor e claro e
    # o vao entre as torres e um buraco preto. `height` 192 porque a torre vai do fundo
    # do mundo ao teto e o andar do jogador esta em y=120.
    "escritorio": dict(
        type=dim_type(height=192, logical_height=192, ambient_light=0.02),
        biome=biome(effects={"fog_color": 6974054, "sky_color": 6974054}),
    ),

    # MAZE: `has_skylight` LIGADO, e nao e distracao.
    #
    # ⚠️ O CHAO DO LABIRINTO E GRAMA — e do Pedro, esta na peca dele. Bloco de grama com
    # luz abaixo de 4 vira terra no primeiro tick aleatorio, e o labirinto amanheceria de
    # terra batida. Como os corredores sao abertos em cima (a parede tem 163 e nao ha
    # teto), a luz do ceu desce a coluna inteira e chega no chao — e por isso o ceu
    # resolve aqui o que na GRASSROOMS precisou de bloco de luz plantado.
    #
    # E ao mesmo tempo o efeito e o do NETHER, que NAO desenha ceu: olhando para cima
    # entre duas paredes de 163 nao se ve azul, se ve a mesma bruma cinza da foto. Ou
    # seja, a luz do ceu existe e o ceu nao aparece — que e exatamente o que a dimensao
    # precisa. `height` 176 cobre os 164 da peca.
    #
    # ⚠️ `fixed_time` 18000 — MEIA-NOITE, e nao mais meio-dia. A primeira versao nasceu
    # ao meio-dia e a foto que o Pedro mandou de dentro do jogo mostrou o problema: um
    # labirinto de dia e um labirinto, com sombra dura e grama viva; a foto de referencia
    # dele e quase preta, com uma mancha cinza de parede aparecendo na bruma. A meia-noite
    # a luz do ceu continua CHEGANDO no chao (o que importa para a grama) mas e desenhada
    # a ~24% — o `skyDarken` do jogo mexe no quanto a tela clareia, nao no nivel guardado.
    #
    # ⚠️ E POR ISSO QUE A GRAMA SOBREVIVE, e por pouco: `getRawBrightness` de noite e
    # 15-11 = 4, e a grama so vira terra ABAIXO de 4. Coluna tapada cai para 3 e morre —
    # medi as pecas, 22,6% das colunas de grama tem bloco por cima, e sao as que estao
    # DEBAIXO da parede de 163. Essas ninguem ve. As do corredor veem o ceu inteiro.
    #
    # A troca de horario e o UNICO botao mexido aqui: `ambient_light` ficou em 0.10 de
    # proposito. Escurecer os dois de uma vez daria o breu certo por acaso e sem saber
    # qual dos dois ajustar depois.
    "maze": dict(
        type=dim_type(has_skylight=True, height=176, logical_height=176,
                      ambient_light=0.10, fixed_time=18000),
        biome=biome(temperature=0.7,
                    effects={"fog_color": 1184276, "sky_color": 1184276,
                             "grass_color": 4872760, "foliage_color": 4872760}),
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
