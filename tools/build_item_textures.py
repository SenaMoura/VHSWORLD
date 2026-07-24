"""
Texturas 16x16 dos itens e blocos da cadeia da pedra corrupta.

Por que gerar por codigo em vez de desenhar: ajustar o tom de um metal ou a
espessura de uma lamina vira editar uma linha e rodar de novo, e o resultado
fica versionado no git junto com o resto. Mesmo criterio dos geradores de vila.

Uso:  python tools/build_item_textures.py
Saida: src/main/resources/assets/recmod/textures/{item,block}/*.png
"""

import os
import random

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEM_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "recmod", "textures", "item")
BLOCK_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "recmod", "textures", "block")

S = 16

# --- paleta ---------------------------------------------------------------
# O ferro do mod e mais frio e sujo que o do vanilla: estas coisas foram feitas
# na marra, com o que tinha, e nao sairam de uma forja limpa.
IRON_D = (78, 82, 90, 255)
IRON_M = (128, 133, 143, 255)
IRON_L = (176, 181, 190, 255)
IRON_H = (214, 218, 224, 255)

WOOD_D = (74, 52, 34, 255)
WOOD_M = (108, 78, 50, 255)
WOOD_L = (140, 104, 68, 255)

STONE_D = (72, 72, 76, 255)
STONE_M = (108, 108, 112, 255)
STONE_L = (140, 140, 145, 255)

# Cinzas da cabeca do martelo e da chapa. Quatro degraus bem separados: em 16x16
# gradiente suave vira sujeira, entao a luz tem que andar em degrau.
GREY = [
    (96, 96, 100, 255),
    (140, 140, 145, 255),
    (186, 186, 190, 255),
    (232, 232, 236, 255),
]

# A pedra comum: cinza medio com pontinho claro e escuro, como pedra de caverna.
ROCK_D = (104, 104, 108, 255)
ROCK_M = (126, 126, 130, 255)
ROCK_L = (146, 146, 150, 255)

# O tentaculo: preto de verdade, com um brilho frio so na borda de cima para ele
# nao virar um buraco chapado no meio do bloco.
TENT_K = (10, 10, 12, 255)
TENT_H = (44, 40, 52, 255)

# A corrupcao: pedra que perdeu a cor e ganhou uma luz que nao deveria estar ali.
CORR_BG_D = (38, 34, 44, 255)
CORR_BG_M = (54, 48, 62, 255)
CORR_BG_L = (70, 63, 80, 255)
CORR_VEIN_D = (58, 24, 74, 255)
CORR_VEIN_M = (104, 44, 130, 255)
CORR_VEIN_L = (156, 78, 188, 255)

# Pedra corrompida como MATERIAL de ferramenta: mais escura e puxada para o roxo
# que a pedra do bloco, senao a ferramenta lia como ferramenta de pedra comum.
CT_D = (44, 40, 52, 255)
CT_M = (72, 66, 84, 255)
CT_L = (104, 96, 120, 255)

# Gosma preta: preto molhado. O brilho e o unico jeito de um preto chapado
# parecer liquido em 16x16.
GOO_D = (12, 10, 16, 255)
GOO_M = (26, 22, 34, 255)
GOO_L = (58, 48, 72, 255)

# O diamante corrompido: o ciano ainda esta la, mas ja tomado.
DIA_D = (28, 108, 108, 255)
DIA_M = (58, 168, 166, 255)
DIA_L = (126, 224, 220, 255)

CLEAR = (0, 0, 0, 0)


def blank():
    return Image.new("RGBA", (S, S), CLEAR)


def put(img, x, y, color):
    if 0 <= x < S and 0 <= y < S:
        img.putpixel((x, y), color)


def line(img, x0, y0, x1, y1, color):
    """Bresenham simples. Serve para haste, lamina e cabo."""
    dx, dy = abs(x1 - x0), abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx - dy
    while True:
        put(img, x0, y0, color)
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 > -dy:
            err -= dy
            x0 += sx
        if e2 < dx:
            err += dx
            y0 += sy


def rect(img, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(img, x, y, color)


def outline(img, color=(24, 24, 28, 255)):
    """
    Contorno escuro em volta do que ja foi desenhado.

    Item de Minecraft sem contorno some no inventario, que tem fundo cinza —
    o contorno e o que faz a silhueta ler a 16 pixels.
    """
    src = img.copy()
    for y in range(S):
        for x in range(S):
            if src.getpixel((x, y))[3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < S and 0 <= ny < S and src.getpixel((nx, ny))[3] != 0:
                    put(img, x, y, color)
                    break


def save(img, folder, name):
    os.makedirs(folder, exist_ok=True)
    path = os.path.join(folder, name + ".png")
    img.save(path)
    print("  " + os.path.relpath(path, ROOT).replace("\\", "/"))


# --- pedra corrupta -------------------------------------------------------

def corrupted_stone():
    """
    Bloco: pedra COMUM, com tentaculos pretos saindo do centro.

    A base e pedra cinza desenhada aqui, e nao a `stone.png` do jogo: textura do
    vanilla e ativo da Mojang e nao entra no nosso jar (mesma regra dos packs beta).
    O ruido imita o granulado da pedra de caverna, que e o que o olho reconhece.

    A leitura que a gente quer e "isto era pedra ate agora": por isso o cinza
    ocupa a borda inteira e o preto so come o miolo. Se o tentaculo tocasse a
    borda, blocos vizinhos se ligariam e o efeito viraria uma mancha continua em
    vez de uma coisa saindo de dentro de cada pedra.

    Semente fixa: a textura tem que sair igual toda vez que o script rodar.
    """
    img = Image.new("RGBA", (S, S), ROCK_M)
    rnd = random.Random(1985)

    for y in range(S):
        for x in range(S):
            r = rnd.random()
            if r < 0.30:
                img.putpixel((x, y), ROCK_D)
            elif r > 0.78:
                img.putpixel((x, y), ROCK_L)

    cx, cy = 7.5, 7.5

    # Nucleo pequeno: e a boca de onde os bracos saem, nao uma mancha.
    for y in range(S):
        for x in range(S):
            if (x - cx) ** 2 + (y - cy) ** 2 <= 1.6 ** 2:
                img.putpixel((x, y), TENT_K)

    # Seis bracos. A curva e uma senoide ao longo do raio, e NAO um passeio
    # aleatorio: com passeio, a direcao acumulava desvio, os bracos se encontravam
    # e o miolo virava um borrao preto so. Com a curva presa ao angulo de origem,
    # cada braco sai para o seu lado e a pedra respira entre eles.
    import math
    for i in range(6):
        base = (2 * math.pi / 6) * i + 0.4
        bend = (0.9 if i % 2 else -0.9)        # alterna o lado da curva
        reach = 6.2 if i % 2 else 5.4          # comprimentos diferentes

        r = 0.0
        while r < reach:
            r += 0.25
            t = r / reach                       # 0 na raiz, 1 na ponta
            ang = base + bend * math.sin(t * math.pi) * 0.5
            x = cx + math.cos(ang) * r
            y = cy + math.sin(ang) * r

            thick = 1.05 * (1.0 - t) + 0.30     # grosso na raiz, fio na ponta
            ix, iy = int(round(x)), int(round(y))
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    if dx * dx + dy * dy <= thick * thick:
                        px, py = ix + dx, iy + dy
                        # A borda fica de pedra: o tentaculo nao vaza para o vizinho,
                        # senao blocos lado a lado viram uma mancha continua.
                        if 1 <= px <= S - 2 and 1 <= py <= S - 2:
                            img.putpixel((px, py), TENT_K)

    # Brilho frio na aresta de cima do preto, so para o miolo ter volume.
    base = img.copy()
    for y in range(1, S):
        for x in range(S):
            if base.getpixel((x, y)) == TENT_K and base.getpixel((x, y - 1)) != TENT_K:
                img.putpixel((x, y), TENT_H)

    return img


# --- itens ----------------------------------------------------------------

def iron_stick():
    """Duas barras de ferro batidas ate virar uma haste. Diagonal, como o graveto."""
    img = blank()
    for i in range(12):
        x, y = 11 - i, 3 + i
        put(img, x, y, IRON_M)
        put(img, x + 1, y, IRON_L)
        put(img, x, y + 1, IRON_D)
    # Brilho curto no meio: metal, nao madeira.
    for i in range(4, 8):
        put(img, 12 - i, 3 + i, IRON_H)
    outline(img)
    return img


# Tons das duas pecas de metal batido. Mais degraus que a paleta GREY porque a
# forma delas depende de sombreado fino: com quatro tons a chapa perde a
# perspectiva e volta a parecer um bloco chapado.
PLATE = {
    "H": (244, 244, 248, 255),
    "E": (244, 244, 248, 255),
    "A": (214, 214, 220, 255),
    "C": (170, 170, 176, 255),
    "F": (118, 118, 124, 255),
    "G": (98, 98, 104, 255),
    "B": (92, 92, 98, 255),
    "D": (56, 56, 62, 255),
    # madeira do cabo
    "c": (44, 32, 16, 255),
    "d": (76, 56, 26, 255),
    "g": (108, 80, 34, 255),
    "i": (140, 106, 44, 255),
}


def grid(rows, palette=None):
    """
    Desenha a partir de um mapa de caracteres. Para silhueta com forma decidida,
    mapa e mais confiavel que primitiva: da para conferir o desenho lendo o
    proprio codigo.
    """
    if palette is None:
        palette = {
            "a": GREY[0], "b": GREY[1], "c": GREY[2], "d": GREY[3],
            "v": WOOD_D, "w": WOOD_M, "W": WOOD_L,
        }
    assert len(rows) == S, f"mapa com {len(rows)} linhas, esperado {S}"
    img = blank()
    for y, row in enumerate(rows):
        assert len(row) == S, f"linha {y} com {len(row)} colunas, esperado {S}"
        for x, ch in enumerate(row):
            if ch != " ":
                img.putpixel((x, y), palette[ch])
    return img


def hammer():
    """
    Martelo: cabeca em losango no alto, cabo de madeira descendo para a esquerda.

    A SILHUETA e a mesma do martelo do Create Diesel Generators (MIT), que foi a
    referencia que o Pedro mandou — os tons sao os nossos. Credito em CREDITS.txt.

    O losango e o ponto: a cabeca esta em PERSPECTIVA, nao de frente. Foi por isso
    que as duas tentativas anteriores (cabeca retangular reta) nunca bateram — elas
    desenhavam o objeto de frente, e a referencia mostra ele de canto.
    """
    img = grid([
        "                ",
        "        A       ",
        "       AHA      ",
        "      AHEHA     ",
        "     AAFFEdg    ",
        "      BAFAEcB   ",
        "       BdFAEFB  ",
        "       dgcFEEAB ",
        "      dicBAAAB  ",
        "     dgc  BAB   ",
        "    dgc    B    ",
        "   dic          ",
        "  dgc           ",
        " dic            ",
        " cc             ",
        "                ",
    ], PLATE)
    # Sem outline(): a grade transcrita ja tem as bordas escuras dela. Somar a
    # nossa por cima engorda a silhueta e a peca perde a leveza da referencia.
    return img


def pressed_iron():
    """
    Chapa de ferro batida.

    A SILHUETA e a mesma do iron sheet do Create (MIT), que foi a referencia que o
    Pedro mandou — os tons sao os nossos. Credito em CREDITS.txt.

    A forma nao e retangulo nem oval: e um LOSANGO, ou seja um retangulo visto de
    canto. E por isso que as duas tentativas anteriores erraram — uma desenhou a
    chapa de frente e a outra arredondou os cantos; a referencia mostra uma peca
    chata deitada em perspectiva, e a espessura aparece so na aresta de baixo.
    """
    img = grid([
        "                ",
        "                ",
        "                ",
        "                ",
        "        BBB     ",
        "     BBBCACB    ",
        "  BBBCAAAAACB   ",
        " BEAAAAAAAAACB  ",
        " BCEAAAAAAAAACB ",
        "  BCEAAAAAACFGD ",
        "   BCEAACFGDDD  ",
        "    BAFFDDD     ",
        "     DDD        ",
        "                ",
        "                ",
        "                ",
    ], PLATE)
    # Sem outline(): a grade transcrita ja tem as bordas escuras dela. Somar a
    # nossa por cima engorda a silhueta e a peca perde a leveza da referencia.
    return img


def sharp_scissors():
    """
    Nao e a tesoura do jogo: e uma ferramenta de corte feita para separar uma
    coisa do corpo dela. Laminas longas e cruzadas, cabo curto.
    """
    img = blank()

    # Duas laminas cruzando logo acima do rebite.
    line(img, 3, 1, 7, 7, IRON_M)
    line(img, 4, 1, 8, 7, IRON_L)
    line(img, 12, 1, 8, 7, IRON_M)
    line(img, 11, 1, 7, 7, IRON_L)

    # Fio, o pixel mais claro do item: e o que faz a tesoura parecer AFIADA.
    for x, y in ((3, 1), (4, 2), (12, 1), (11, 2)):
        put(img, x, y, IRON_H)

    # Rebite.
    put(img, 7, 8, IRON_H)
    put(img, 8, 8, IRON_D)

    # Cabos: aneis VAZADOS. Cheios viravam um borrao escuro no fundo do item.
    for cx in (4, 11):
        side = -1 if cx < 8 else 1
        for x, y in ((cx, 11), (cx + side, 11),
                     (cx - side, 12), (cx + side * 2, 12),
                     (cx - side, 13), (cx + side * 2, 13),
                     (cx, 14), (cx + side, 14)):
            put(img, x, y, IRON_M)
        # Haste ligando o rebite ao anel.
        line(img, 8 if side > 0 else 7, 9, cx + side, 11, IRON_D)

    outline(img)
    return img


# --- o tier da pedra corrompida -------------------------------------------
#
# As cinco ferramentas seguem a FORMA das ferramentas de diamante do jogo, que e
# o desenho que o olho de qualquer jogador ja conhece. Nao ha textura da Mojang
# dentro do jar: o que esta aqui e a silhueta transcrita em letras, e cada letra
# vira uma cor NOSSA na hora de gerar. O resultado e um recolorido derivado, nao
# uma copia de arquivo.
#
# Depois do recolorido entra a assinatura do tier: manchas pretas comendo a
# cabeca da ferramenta, sempre nos mesmos lugares (semente fixa por ferramenta).

TOOL_SHAPES = {
    "sword": [
        "             222", "            2881", "           28681", "          28681 ",
        "         28671  ", "        28671   ", "  22   27671    ", "  232 27671     ",
        "   2417471      ", "   244371       ", "    2321        ", "   bc1221       ",
        "  bda 1121      ", "22ca    11      ", "231             ", "111             ",
    ],
    "pickaxe": [
        "                ", "                ", "      22222     ", "     2765562bc  ",
        "      211155da  ", "          b651  ", "         bca561 ", "        bda 151 ",
        "       bca  151 ", "      bda   161 ", "     bca    171 ", "    bda      1  ",
        "   bca          ", "  bda           ", "  aa            ", "                ",
    ],
    "axe": [
        "                ", "         22     ", "        2772    ", "       27562    ",
        "      27555bc   ", "      176545a   ", "       11b5451  ", "        bca551  ",
        "       bda 11   ", "      bca       ", "     bca        ", "    bda         ",
        "   bca          ", "  bda           ", "  aa            ", "                ",
    ],
    "shovel": [
        "                ", "                ", "           221  ", "          27751 ",
        "         276571 ", "        2765671 ", "         b5671  ", "        bda71   ",
        "       bda 1    ", "      bca       ", "     bda        ", "    bca         ",
        "  bbda          ", "  bda           ", "   aa           ", "                ",
    ],
    "hoe": [
        "                ", "       222      ", "      27662     ", "       11562bc  ",
        "         155da  ", "          b651  ", "         bca1   ", "        bda     ",
        "       bca      ", "      bda       ", "     bca        ", "    bda         ",
        "   bca          ", "  bda           ", "  aa            ", "                ",
    ],
}

# Rampa da pedra corrompida: mesma escada de luz do diamante, virada para o roxo
# sujo. O cabo continua de madeira, so que envelhecida.
CORRUPT_RAMP = {
    "1": (16, 13, 22, 255),  "2": (32, 26, 42, 255),  "3": (48, 40, 62, 255),
    "4": (66, 56, 84, 255),  "5": (86, 74, 108, 255), "6": (100, 86, 124, 255),
    "7": (122, 106, 150, 255), "8": (186, 172, 212, 255),
    "a": (28, 22, 16, 255),  "b": (52, 40, 28, 255),
    "c": (74, 58, 40, 255),  "d": (98, 78, 54, 255),
}

# O diamante de verdade, para a picareta corrompida: nela o ciano FICA, porque a
# graca e ver o material bom sendo tomado.
DIAMOND_RAMP = {
    "1": (8, 37, 32, 255),   "2": (14, 63, 54, 255),  "3": (21, 99, 85, 255),
    "4": (30, 138, 119, 255),"5": (39, 178, 154, 255),"6": (43, 199, 172, 255),
    "7": (51, 235, 203, 255),"8": (164, 253, 240, 255),
    "a": (20, 16, 18, 255),  "b": (34, 28, 34, 255),
    "c": (48, 40, 48, 255),  "d": (66, 56, 66, 255),
}


def _tool(shape, ramp, seed, rate):
    """
    Monta a ferramenta: pinta a forma com a rampa escolhida e depois deixa o preto
    comer parte da cabeca.

    So os tons ALTOS (5 a 8) podem ser comidos. Se o preto pudesse cair nos tons
    baixos, ele se misturaria com o contorno e a ferramenta perderia a silhueta —
    a corrupcao tem que aparecer na luz, nao na sombra.
    """
    img = blank()
    rnd = random.Random(seed)

    for y, row in enumerate(shape):
        for x, ch in enumerate(row):
            if ch == " ":
                continue
            if ch in "5678" and rnd.random() < rate:
                img.putpixel((x, y), GOO_D)
                # Um vizinho de baixo mais claro: a mancha ganha volume em vez de
                # virar um furo de um pixel.
                if y + 1 < S and row[x] != " ":
                    below = shape[y + 1][x] if y + 1 < len(shape) else " "
                    if below in "45678":
                        img.putpixel((x, y + 1), GOO_M)
                continue
            img.putpixel((x, y), ramp[ch])

    return img


def corrupted_sword():
    return _tool(TOOL_SHAPES["sword"], CORRUPT_RAMP, 101, 0.30)


def corrupted_pickaxe():
    return _tool(TOOL_SHAPES["pickaxe"], CORRUPT_RAMP, 202, 0.30)


def corrupted_axe():
    return _tool(TOOL_SHAPES["axe"], CORRUPT_RAMP, 303, 0.30)


def corrupted_shovel():
    return _tool(TOOL_SHAPES["shovel"], CORRUPT_RAMP, 404, 0.30)


def corrupted_hoe():
    return _tool(TOOL_SHAPES["hoe"], CORRUPT_RAMP, 505, 0.30)


def black_goo():
    """
    Gota de gosma preta. Larga embaixo e afinando em cima, como coisa pesada
    que ainda esta escorrendo.
    """
    img = blank()
    for y in range(4, 13):
        half = 1 + (y - 4) // 2
        if y >= 11:
            half -= 1
        for x in range(8 - half, 9 + half):
            put(img, x, y, GOO_M)
    for y in range(6, 12):
        put(img, 8, y, GOO_D)
        put(img, 9, y, GOO_D)
    # Brilho: a luz batendo na superficie molhada.
    for x, y in ((6, 8), (7, 7), (10, 9), (6, 9)):
        put(img, x, y, GOO_L)
    # Pingo destacando embaixo.
    put(img, 8, 13, GOO_M)
    outline(img)
    return img


def corrupted_diamond_pickaxe():
    """
    A picareta que abre o que o mod tranca.

    Mesma forma da picareta de diamante, o ciano intacto, e a corrupcao MUITO mais
    agressiva que no kit de pedra — nesta o preto e o assunto.
    """
    return _tool(TOOL_SHAPES["pickaxe"], DIAMOND_RAMP, 909, 0.55)


# --- dispositivos: bussola, ancora e relogio ------------------------------
#
# Bussola e relogio seguem a forma dos itens do jogo (mesma tecnica das
# ferramentas: silhueta em letras, cores nossas). A ancora e desenho original —
# nao existe nada parecido no vanilla para servir de base.

COMPASS_SHAPE = [
    "                ", "                ", "     CCCCCC     ", "   CCFFDEDECC   ",
    "  BEDCAAAACEEB  ", " BECAAAAJAAACEB ", " BCAAAAAJAAAACB ", " BCAAAAHJKAAACB ",
    " BFCAAAAHAAACDB ", " BDIIAAAAAADDHB ", " BDGGIIFGGDEHHB ", "  BFGGGDFFEEHB  ",
    "   BBFGDFFEBB   ", "     BBBBBB     ", "                ", "                ",
]

# O corpo vira metal frio e escuro; a agulha continua VERMELHA, e o unico ponto
# de cor do item — e o mesmo vermelho da regua do tooltip, para o mod falar uma
# lingua so.
COMPASS_RAMP = {
    "A": (34, 30, 42, 255),   "B": (16, 14, 22, 255),   "C": (46, 40, 56, 255),
    "D": (110, 100, 128, 255),"E": (78, 70, 94, 255),   "F": (146, 134, 168, 255),
    "G": (190, 178, 212, 255),"H": (64, 58, 76, 255),   "I": (224, 214, 240, 255),
    "J": (176, 24, 24, 255),  "K": (84, 76, 100, 255),
}

CLOCK_SHAPE = [
    "      AAAA      ", "    AADDBBAA    ", "   AFDFEEFBFA   ", "  AFDEELLEEBFA  ",
    "  ADEEIIIIGEBC  ", " ADFEGIIIIGGFBC ", " ADEEGGIIGGGEBC ", " ABEEGGKDEGGEBC ",
    " ABAJJJDBJJJABC ", " CDBKKDBFDDBFBC ", " CHDBBBBFFFFBHC ", "  CHDDBBFFBBHC  ",
    "  CAHHDDDBHAAC  ", "   CAHHHAAAAC   ", "    CCAHAACC    ", "      CCCC      ",
]

# O ouro vira aluminio (e o metal que o jogador ja fabrica) e o mostrador vira
# gosma: o relogio nao marca hora nenhuma, ele so faz barulho.
CLOCK_RAMP = {
    "A": (130, 132, 140, 255), "B": (196, 199, 208, 255), "C": (54, 54, 60, 255),
    "D": (222, 225, 232, 255), "E": (52, 44, 68, 255),    "F": (170, 173, 182, 255),
    "G": (70, 60, 90, 255),    "H": (150, 152, 162, 255), "I": (98, 86, 122, 255),
    "J": (12, 10, 16, 255),    "K": (232, 234, 240, 255), "L": (48, 40, 62, 255),
}


def corrupted_compass():
    return grid(COMPASS_SHAPE, COMPASS_RAMP)


def lure_clock():
    return grid(CLOCK_SHAPE, CLOCK_RAMP)


def anchor():
    """
    Ancora. Desenho nosso: argola em cima, travessa, haste e as duas unhas
    curvando para dentro embaixo.

    As unhas sobem nas pontas de proposito. Ancora com as pontas retas lia como
    um tridente de cabeca para baixo.
    """
    img = grid([
        "                ",
        "      aca       ",
        "     ac ca      ",
        "     ab ba      ",
        "      aca       ",
        "   abcccccba    ",
        "      bc        ",
        "      bc        ",
        "      bk        ",
        "      bc        ",
        "  a   bc   a    ",
        "  b   bc   b    ",
        "  bc  bc  cb    ",
        "  ab  bc  ba    ",
        "   abcbcbca     ",
        "     abba       ",
    ], {"a": GREY[0], "b": GREY[1], "c": GREY[2], "k": GOO_D})
    return img


# --- o rasgo da realidade e a FRACTURE -------------------------------------

# A cor da fenda: miolo branco de brasa, halo rosa e magenta escuro morrendo nas
# beiradas. E a unica coisa saturada do mod inteiro, de proposito — tudo aqui e
# cinza, fita e caverna, entao quando a realidade racha a cor grita.
FRAC_CORE = (255, 255, 255, 255)
FRAC_HOT = (255, 176, 214, 255)
FRAC_MID = (232, 72, 150, 255)
FRAC_LOW = (156, 28, 96, 220)
FRAC_FAR = (86, 14, 56, 150)

# O caminho da fenda: ziguezague subindo da esquerda para a direita. Fixo, para a
# animacao mexer so na LUZ — se o rasgo mudasse de forma a cada quadro, viraria
# ruido em vez de uma coisa pulsando.
FRAC_PATH = [
    (3, 13), (5, 11), (4, 10), (6, 8), (8, 7), (7, 5), (9, 4), (11, 3), (12, 2),
]


def _spine():
    """Todos os pixels do miolo da fenda, ligando ponto a ponto."""
    pts = []
    for i in range(len(FRAC_PATH) - 1):
        x0, y0 = FRAC_PATH[i]
        x1, y1 = FRAC_PATH[i + 1]
        steps = max(abs(x1 - x0), abs(y1 - y0)) * 4
        for k in range(steps + 1):
            t = k / steps
            pts.append((int(round(x0 + (x1 - x0) * t)),
                        int(round(y0 + (y1 - y0) * t))))
    return sorted(set(pts))


def fracture_frame(spine, glow, jitter_seed):
    """
    Um quadro da fenda.

    Desenha de fora para dentro — halo, meio, quente, miolo — porque assim cada
    camada cobre a anterior e o miolo branco nunca fica comido pelo brilho.
    """
    img = blank()
    rnd = random.Random(jitter_seed)

    layers = [(glow + 1.9, FRAC_FAR), (glow + 1.15, FRAC_LOW),
              (glow + 0.55, FRAC_MID), (glow, FRAC_HOT)]

    for radius, color in layers:
        r = int(radius) + 1
        for (sx, sy) in spine:
            for dy in range(-r, r + 1):
                for dx in range(-r, r + 1):
                    if dx * dx + dy * dy <= radius * radius:
                        put(img, sx + dx, sy + dy, color)

    for (sx, sy) in spine:
        put(img, sx, sy, FRAC_CORE)

    # Fagulhas soltas: sao elas que dao vida ao loop, porque mudam de lugar a
    # cada quadro enquanto a fenda fica parada.
    for _ in range(5):
        (sx, sy) = spine[rnd.randrange(len(spine))]
        put(img, sx + rnd.randint(-4, 4), sy + rnd.randint(-4, 4), FRAC_MID)

    return img


def fracture_animated(frames=8):
    """
    Folha de animacao: os quadros empilhados em uma tira de 16 x (16*n).

    O brilho vai e volta com uma senoide, entao o ultimo quadro emenda no
    primeiro sem solavanco quando o jogo repete o loop.
    """
    import math
    spine = _spine()
    sheet = Image.new("RGBA", (S, S * frames), CLEAR)

    for i in range(frames):
        pulse = 0.5 - 0.5 * math.cos(2 * math.pi * i / frames)   # 0 -> 1 -> 0
        glow = 0.9 + pulse * 1.0
        sheet.alpha_composite(fracture_frame(spine, glow, 700 + i), (0, i * S))

    return sheet


def reality_tear():
    """
    O item: um caco da fenda, arrancado da parede.

    Menor e mais fechado que a espada — ele e o material, nao a arma, entao nao
    ganha o halo grande nem as fagulhas.
    """
    img = blank()
    core = [(6, 11), (7, 10), (7, 9), (8, 8), (8, 7), (9, 6), (9, 5), (10, 4)]

    for (sx, sy) in core:
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                put(img, sx + dx, sy + dy, FRAC_LOW)
    for (sx, sy) in core:
        put(img, sx, sy, FRAC_MID)
        put(img, sx, sy - 1, FRAC_HOT)
    for (sx, sy) in core[1:-1]:
        put(img, sx, sy, FRAC_CORE)

    return img


def main():
    print("blocos:")
    save(corrupted_stone(), BLOCK_DIR, "corrupted_stone")

    print("itens:")
    save(iron_stick(), ITEM_DIR, "iron_stick")
    save(hammer(), ITEM_DIR, "hammer")
    save(pressed_iron(), ITEM_DIR, "pressed_iron")
    save(sharp_scissors(), ITEM_DIR, "sharp_scissors")

    print("tier da pedra corrompida:")
    save(corrupted_sword(), ITEM_DIR, "corrupted_sword")
    save(corrupted_pickaxe(), ITEM_DIR, "corrupted_pickaxe")
    save(corrupted_axe(), ITEM_DIR, "corrupted_axe")
    save(corrupted_shovel(), ITEM_DIR, "corrupted_shovel")
    save(corrupted_hoe(), ITEM_DIR, "corrupted_hoe")
    save(black_goo(), ITEM_DIR, "black_goo")
    save(corrupted_compass(), ITEM_DIR, "corrupted_compass")
    save(anchor(), ITEM_DIR, "anchor")
    save(lure_clock(), ITEM_DIR, "lure_clock")
    save(reality_tear(), ITEM_DIR, "reality_tear")

    print("fenda animada:")
    save(fracture_animated(), ITEM_DIR, "fracture")
    save(corrupted_diamond_pickaxe(), ITEM_DIR, "corrupted_diamond_pickaxe")


if __name__ == "__main__":
    main()
