# -*- coding: utf-8 -*-
"""Assa as pecas que o Pedro construiu a mao num arquivo que o gerador de dimensao le.

POR QUE NAO USAR `.nbt` DE ESTRUTURA: o gerador de chunk roda fora da thread do
servidor e sem `Level` na mao — pedir template ao `StructureTemplateManager` la
dentro e uma corrida esperando acontecer. Entao seguimos o mesmo caminho que o mod
ja usa para as malhas das criaturas (`pack_mesh.py` -> `MeshLibrary`): o python assa
um binario compacto, o Java le uma vez e guarda. O formato e nosso, e determinista.

AS PECAS SAO SELADAS. Toda peca do Pedro tem piso solido em y=0 e teto solido em
y=H-1, e as portas sao retangulos de ar nas quatro paredes comecando em y=1. E isso
que deixa o encaixe automatico funcionar sem ele ter que colocar bloco de jigsaw:
o conector E o buraco na parede.

Rodar (depois de salvar schematics novos pelo WorldEdit):
    python tools/import_dimension.py
"""
import io
import os
import struct
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from schem import Schem

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.expandvars(r"%APPDATA%\.minecraft\config\worldedit\schematics")
# NAO em `data/recmod/dimension/`: essa pasta e a do jogo, onde mora o level stem
# (`data.json`). Misturar o nosso binario com os JSON que o datapack carrega e pedir
# confusao na primeira vez que alguem for procurar um dos dois.
OUT = os.path.join(REPO, "src", "main", "resources", "data", "recmod", "dimension_pieces")

MAGIC = b"VHSD"
VERSION = 2

# Bitolas de corredor. O numero e a ALTURA do vao; so conectam portas da mesma
# bitola, senao um corredor baixo desemboca no meio da parede de um alto.
SMALL, TALL = 0, 1

# Uma porta de verdade tem no minimo isto. O que fica abaixo do corte sao as
# frestas/nichos que o Pedro deixou nas paredes das pecas altas (2 de largo por 3
# de alto): sao enfeite, e tratar como porta faria o gerador pendurar corredor no
# lugar errado.
MIN_DOOR_W = 3
MIN_DOOR_H = 4

# ---------------------------------------------------------------------- pecas
#
# Cada dimensao tem a sua lista. Os campos, em ordem:
#   nome, arquivo, peso no sorteio, e a hub?, recorte, detectar portas?, ancora
#
# RECORTE: o Pedro seleciona uma area maior que a obra pelo WorldEdit. Onde isso
# importa, o recorte vem explicito (x0,x1,y0,y1,z0,z1); None usa a selecao inteira.
#
# ANCORA (x, y, z): o ponto da peca que o gerador usa para encaixar. Nos corredores
# da DATA nao serve para nada (o encaixe e pelas portas). Nas ilhas da CHUNKS ela e
# essencial: (x,z) e o canto do quadrado de chao de 16x16 dentro da selecao e `y` e
# a altura da grama — e o que faz uma ilha com copa transbordando alinhar com uma
# ilha pelada, e o que faz o tabuleiro da ponte encostar no chao no mesmo nivel.
#
# data_5 NAO esta aqui de proposito: e byte a byte igual a data_4 (md5 confere).
# Deixar as duas so faria o corredor longo sair com o dobro da chance.
DIMENSIONS = {
    "data": [
        ("hub",       "data_hub.schem", 0, True,  None, True, (0, 0, 0)),
        ("small_t",   "data_1.schem",   3, False, None, True, (0, 0, 0)),
        ("small_run", "data_2.schem",   4, False, None, True, (0, 0, 0)),
        ("tall_junc", "data_3.schem",   3, False, None, True, (0, 0, 0)),
        ("tall_run",  "data_4.schem",   4, False, None, True, (0, 0, 0)),
    ],
    # CHUNKS: um mundo picado. As duas ilhas sao O MESMO chunk 16x16x34 macico
    # (so 9 blocos diferem no miolo) — a segunda tem mato e arvores por cima, e a
    # copa transborda a borda, o que e otimo: ela fica pendurada sobre o vazio.
    #
    # A ponte nao e uma peca inteira, sao TRES FATIAS de um bloco. Ela e um modulo
    # que se repete de 3 em 3 (poste, vao, vao — conferido nas 10 fatias do
    # original), entao o gerador ladrilha `i % 3` e faz ponte de qualquer tamanho.
    # Guardar a ponte inteira limitaria todo vao a 10 blocos.
    "chunks": [
        ("island",   "chunk_1.schem", 6, True,  None,                          False, (0, 33, 0)),
        ("grove",    "chunk_2.schem", 4, False, None,                          False, (0, 33, 7)),
        ("bridge_a", "chunks_wood_bridge.schem", 0, False, (5, 5, 1, 5, 4, 11), False, (0, 2, 0)),
        ("bridge_b", "chunks_wood_bridge.schem", 0, False, (6, 6, 1, 5, 4, 11), False, (0, 2, 0)),
        ("bridge_c", "chunks_wood_bridge.schem", 0, False, (7, 7, 1, 5, 4, 11), False, (0, 2, 0)),
    ],
    # INSIDIOUS: saloes de pedra SEM TETO sobre o vazio preto, em labirinto com becos.
    #
    # As quatro pecas do Pedro dividem a mesma BITOLA — piso de 7 de largura com muro
    # nas duas bordas, deixando 5 de passagem. E isso que permite grade: cruzamento e
    # corredor encaixam sem adaptador.
    #
    # A ANCORA de todas e o mesmo ponto conceitual: o bloco do PISO no meio da
    # passagem. Alinhar pela caixa nao serviria — as pecas tem sobra de selecao
    # diferente e alturas de piso diferentes (o cruzamento grande tem o piso em y=11,
    # os outros em y=1). Alinhando pela ancora, todo piso cai no mesmo Y e toda
    # passagem cai na mesma linha de centro.
    #
    # O CORREDOR NAO E UMA PECA, sao 5 FATIAS de 1 bloco. Medido: o padrao de pilar
    # tem periodo 5 (A B C C E) e o gerador ladrilha `i % 5`, entao o corredor cobre
    # qualquer vao. Guardado inteiro, todo vao teria que ter exatamente os 11 blocos
    # do original.
    #   ⚠️ A quinta fatia vem de x=13, e NAO de x=8. As duas sao a mesma posicao do
    #   modulo, mas a de x=8 esta com QUATRO BLOCOS FALTANDO no muro sul (y=3..6) —
    #   um buraco de 1x4 para o vazio na construcao do Pedro. Ladrilhada, ela abriria
    #   esse buraco a cada 5 blocos de corredor.
    #   ⚠️ E tambem NAO serve trocar por x=10: o muro guarda para que lado encosta
    #   (`west=tall` depois do pilar, `east=tall` antes dele). A fatia de x=10 e a de
    #   depois do pilar; posta antes, o muro apontaria para o lado errado.
    "insidious": [
        ("heart",     "insidious_middle_heart.schem",    0, True,  (3, 15, 1, 11, 3, 13),  False, (6, 0, 5)),
        ("cross",     "insidious_middle_bridge_1.schem", 6, False, (0, 20, 1, 7, 0, 19),   False, (10, 0, 9)),
        ("cross_big", "insidious_middle_bridge.schem",   4, False, (3, 30, 0, 17, 0, 27),  False, (14, 11, 18)),
        ("hall_a",    "insidious_bridge.schem",          0, False, (4, 4, 1, 8, 1, 7),     False, (0, 0, 3)),
        ("hall_b",    "insidious_bridge.schem",          0, False, (5, 5, 1, 8, 1, 7),     False, (0, 0, 3)),
        ("hall_c",    "insidious_bridge.schem",          0, False, (6, 6, 1, 8, 1, 7),     False, (0, 0, 3)),
        ("hall_d",    "insidious_bridge.schem",          0, False, (7, 7, 1, 8, 1, 7),     False, (0, 0, 3)),
        ("hall_e",    "insidious_bridge.schem",          0, False, (13, 13, 1, 8, 1, 7),   False, (0, 0, 3)),
    ],
    # ------------------------------------------------------------------ o lote de 6
    #
    # As seis de 2026-07-29. A diferenca delas em relacao as tres primeiras e que aqui
    # o gerador tambem CONSTROI: a DATA e a INSIDIOUS sao 100% peca do Pedro carimbada,
    # enquanto village, grassrooms, biblioteca e train precisam de casca em volta (chao,
    # rua, parede, teto, agua). O que ele construiu e o MOTIVO; o resto do quadro sai
    # do Java, senao cada uma pediria dezenas de schematics para cobrir o infinito.
    #
    # VILLAGE: uma casa so, repetida. "casas identicas" e a instrucao literal do Pedro,
    # e a foto de referencia dele e uma rua de casas iguais desaparecendo no horizonte —
    # variar a casa desmancharia justamente o efeito.
    #   Recorte x2..19 / z4..20: a selecao tem 2 blocos de folga em volta, e a cerca do
    #   quintal (x16..19) faz parte do lote, entao ela ENTRA. y0..9 pega da fundacao de
    #   cobblestone ao topo do telhado.
    #   A porta da frente e o vao na parede x=15, em z8..9 — ou seja a casa encara +X.
    #   E disso que o gerador precisa saber para poder virar as duas fileiras de frente
    #   para a rua.
    "village": [
        ("house", "village_house.schem", 0, True, (2, 19, 0, 9, 4, 20), False, (0, 0, 0)),
    ],
    # GRASSROOMS: cinco salas do mesmo liminal space branco.
    #
    # ⚠️ ELAS NAO SAO MODULOS. Medi as quatro paredes de cada uma: a leste da room_b e
    # ar de ponta a ponta, a room_c tem TRES paredes inteiras abertas, a room_e duas.
    # Sao pedacos recortados de uma obra continua, e nao pecas seladas com porta como as
    # da DATA — a deteccao automatica de porta (que exige vao comecando em y=1) nao acha
    # nada nelas, e encaixa-las pelas bordas encostaria parede aberta em parede aberta.
    # Por isso a GRASSROOMS e a unica em que a peca fica DENTRO de uma sala construida
    # pelo Java: a casca branca sela o que ficou aberto, e o que estava aberto virou a
    # porta para o espaco vazio da celula. Sem ancora: cada uma senta no piso da casca.
    "grassrooms": [
        ("room_a", "grass_room_1.schem", 5, True,  None, False, (0, 0, 0)),
        ("room_b", "grass_room_2.schem", 2, False, None, False, (0, 0, 0)),
        ("room_c", "grass_room_3.schem", 3, False, None, False, (0, 0, 0)),
        ("room_d", "grass_room_4.schem", 6, False, None, False, (0, 0, 0)),
        ("room_e", "grass_room_5.schem", 4, False, None, False, (0, 0, 0)),
    ],
    # TRAIN: a linha e UMA FATIA de 4 blocos, ladrilhada para sempre.
    #
    # Medido no rails.schem: o dormente esta em x1,x2 e reaparece em x5,x6, x9,x10,
    # x13,x14 — periodo 4. E o trilho de bigorna em y=1 e `laje bigorna bigorna laje`,
    # o mesmo periodo. Entao x0..3 e o modulo inteiro e o gerador repete `i % 4`... na
    # verdade nem precisa do resto: uma fatia de 4 e a linha toda.
    #   (rails_1.schem e byte a byte igual a rails.schem — md5 confere — entao nao esta
    #   aqui, pelo mesmo motivo que data_5 nao esta.)
    "train": [
        ("track", "rails.schem",           0, True,  (0, 3, 0, 1, 0, 11), False, (0, 0, 0)),
        ("safe",  "safe_spot_rails.schem", 0, False, None,                False, (0, 0, 0)),
    ],
    # UNDER PRESSURE: dois submarinos, cascas ocas de concreto.
    #
    # O recorte tira o vazio da selecao (y=0 e y=9/15 nao tem um bloco) e encosta a
    # quilha em y=0 da peca. O de spawn e o que tem ESCADA e alcapao no topo: e por ele
    # que se entra, e e ele que fura a linha d'agua. O outro tem mastro e nada por onde
    # entrar — e o que se acha afundado.
    "under_pressure": [
        ("sub_spawn", "submarine_spawn.schem", 0, True,  (2, 6, 1, 8, 4, 24),  False, (0, 0, 0)),
        ("sub_deep",  "submarine_1.schem",     0, False, (5, 9, 1, 14, 5, 25), False, (0, 0, 0)),
    ],
    # BIBLIOTECA: o salao do Pedro LADRILHA sozinho, e isso e sorte de construcao.
    #
    # Medido: y=0 tem 805 blocos solidos e 23x35 = 805; y=17 tambem. Ou seja piso e teto
    # sao planos CHEIOS da pegada, e as quatro paredes da borda sao ar. Encostando copias
    # lado a lado, o piso e o teto ficam continuos sem emenda visivel e o que sobra sao
    # as estantes internas formando o labirinto. Nao gira: 23x35 girado viraria 35x23 e
    # sairia da grade. A variacao vem do giro de 180 (que mantem a pegada) e das estantes
    # extras.
    #   estante_3a.schem e byte a byte igual a estante_2.schem, entao so uma entra.
    "biblioteca": [
        ("hall",    "biblioteca_hub.schem", 0, True,  None, False, (0, 0, 0)),
        ("shelf_a", "estante_1.schem",      5, False, None, False, (0, 0, 0)),
        ("shelf_b", "estante_2.schem",      5, False, None, False, (0, 0, 0)),
    ],
    # PARKOURLAND: a gaiola, e SO a gaiola.
    #
    # ⚠️ Conferi camada por camada antes de escrever o gerador: os 22960 blocos sao
    # tabua no piso (y0..1), 106 de cerca por camada de y=2 a y=179 — que e exatamente
    # o perimetro de uma caixa 30x25 — e tabua macica em y=180..184. Nao ha UM bloco
    # entre y=2 e y=179 que nao seja o muro. O Pedro construiu o recipiente; o parkour
    # de dentro nao existe no arquivo e por isso o Java o desenha.
    "parkourland": [
        ("cage", "parkour_land.schem", 0, True, None, False, (0, 0, 0)),
    ],
}

FACES = {"north": 0, "east": 1, "south": 2, "west": 3}


# ------------------------------------------------------------------ conectores
def _components(cells):
    """Agrupa celulas de ar vizinhas num mesmo buraco."""
    cells = set(cells)
    out = []
    while cells:
        comp = [cells.pop()]
        stack = list(comp)
        while stack:
            a, b = stack.pop()
            for nb in ((a + 1, b), (a - 1, b), (a, b + 1), (a, b - 1)):
                if nb in cells:
                    cells.discard(nb)
                    comp.append(nb)
                    stack.append(nb)
        out.append(comp)
    return out


def connectors(s):
    """Acha as portas nas quatro paredes.

    Devolve (face, a0, a1, y0, y1, gauge), onde `a` corre ao longo da parede: e o
    X nas paredes norte/sul e o Z nas paredes leste/oeste.
    """
    W, H, L = s.size
    planes = {
        "north": [(x, y) for x in range(W) for y in range(H) if s.is_air(x, y, 0)],
        "south": [(x, y) for x in range(W) for y in range(H) if s.is_air(x, y, L - 1)],
        "west":  [(z, y) for z in range(L) for y in range(H) if s.is_air(0, y, z)],
        "east":  [(z, y) for z in range(L) for y in range(H) if s.is_air(W - 1, y, z)],
    }
    out = []
    for face, cells in planes.items():
        for comp in _components(cells):
            a = [p[0] for p in comp]
            y = [p[1] for p in comp]
            a0, a1, y0, y1 = min(a), max(a), min(y), max(y)
            if y0 != 1:
                continue                      # porta nasce no piso, sempre
            if (a1 - a0 + 1) < MIN_DOOR_W or (y1 - y0 + 1) < MIN_DOOR_H:
                continue                      # fresta, nao porta
            gauge = TALL if (y1 - y0 + 1) > 6 else SMALL
            out.append((face, a0, a1, y0, y1, gauge))
    out.sort(key=lambda c: (FACES[c[0]], c[1]))
    return out


# ------------------------------------------------------------------ recorte
class Crop:
    """Uma janela sobre um .schem, para nao ter que copiar os blocos.

    O WorldEdit grava a selecao inteira, e o Pedro seleciona com folga. A ponte, por
    exemplo, vem numa caixa de 22x7x14 com a obra ocupando so 10x5x8 no meio dela.
    Sem recorte, o vazio da selecao viraria bloco de ar guardado no arquivo — e, pior,
    o gerador o trataria como parte da peca na hora de encostar uma coisa na outra.
    """

    def __init__(self, schem, box):
        self.s = schem
        self.x0, self.x1, self.y0, self.y1, self.z0, self.z1 = box
        self.width = self.x1 - self.x0 + 1
        self.height = self.y1 - self.y0 + 1
        self.length = self.z1 - self.z0 + 1
        self.palette = schem.palette

    @property
    def size(self):
        return (self.width, self.height, self.length)

    def get(self, x, y, z):
        return self.s.get(self.x0 + x, self.y0 + y, self.z0 + z)

    def is_air(self, x, y, z):
        return self.get(x, y, z).split("[")[0] == "minecraft:air"


# ------------------------------------------------------------------ escrita
def _utf(buf, text):
    raw = text.encode("utf-8")
    buf.write(struct.pack(">H", len(raw)))
    buf.write(raw)


def bake(pieces, path):
    buf = io.BytesIO()
    buf.write(MAGIC)
    buf.write(struct.pack(">ii", VERSION, len(pieces)))

    for name, s, weight, is_hub, conns, anchor in pieces:
        W, H, L = s.size
        _utf(buf, name)
        buf.write(struct.pack(">iiiiB", W, H, L, weight, 1 if is_hub else 0))
        buf.write(struct.pack(">iii", anchor[0], anchor[1], anchor[2]))

        # Paleta local: so os estados que a peca usa de verdade. As pecas de
        # andesito tem 3; a hub tem 24. Cabe folgado num short.
        order = []
        index = {}
        for state in s.palette:
            if state not in index:
                index[state] = len(order)
                order.append(state)
        if len(order) > 32767:
            raise SystemExit("paleta grande demais em %s" % name)

        buf.write(struct.pack(">i", len(order)))
        for state in order:
            _utf(buf, state)

        # Blocos, na ordem que o Java vai ler: y, depois z, depois x.
        cells = bytearray()
        for y in range(H):
            for z in range(L):
                for x in range(W):
                    cells += struct.pack(">h", index[s.get(x, y, z)])
        buf.write(struct.pack(">i", len(cells)))
        buf.write(cells)

        buf.write(struct.pack(">i", len(conns)))
        for face, a0, a1, y0, y1, gauge in conns:
            buf.write(struct.pack(">BiiiiB", FACES[face], a0, a1, y0, y1, gauge))

    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as fh:
        fh.write(buf.getvalue())
    return len(buf.getvalue())


def build(dimension, pieces):
    loaded = []
    cache = {}
    print("=== %s ===" % dimension.upper())
    for name, filename, weight, is_hub, crop, detect, anchor in pieces:
        src = os.path.join(SRC, filename)
        if not os.path.exists(src):
            print("  !! sumiu:", filename)
            continue
        if filename not in cache:
            cache[filename] = Schem.load(src)      # a ponte vira 3 fatias do mesmo arquivo
        piece = cache[filename]
        if crop is not None:
            piece = Crop(piece, crop)

        conns = connectors(piece) if detect else []
        loaded.append((name, piece, weight, is_hub, conns, anchor))

        W, H, L = piece.size
        print("%-10s %2dx%2dx%-3d  ancora %s  %d portas" % (name, W, H, L, anchor, len(conns)))
        for face, a0, a1, y0, y1, gauge in conns:
            print("     %-5s %2d..%-2d  y%d..%-2d  %s"
                  % (face, a0, a1, y0, y1, "alta" if gauge == TALL else "baixa"))

    if not loaded:
        raise SystemExit("%s: nenhuma peca carregada" % dimension)
    if not any(p[3] for p in loaded):
        raise SystemExit("%s: nenhuma peca marcada como hub" % dimension)

    out = os.path.join(OUT, dimension + ".bin")
    size = bake(loaded, out)
    print("-> %s  %.1f KB\n" % (os.path.relpath(out, REPO), size / 1024.0))


def main():
    for dimension, pieces in DIMENSIONS.items():
        build(dimension, pieces)


if __name__ == "__main__":
    main()
