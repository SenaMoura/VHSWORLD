"""
A TABELA DO RIG DO ESCUTADOR — uma so, em Python puro (nao importa bpy).

⚠️ ESTE ARQUIVO EXISTE PARA NAO HAVER DUAS COPIAS. O escultor (sculpt_listener.py, que roda
dentro do Blender) e o conferidor (preview_listener_smesh.py, que roda fora) precisam dos
mesmos pivos e das mesmas rotacoes de repouso. Duas listas iguais e o jeito conhecido de
produzir a criatura "quase certa" — o membro nascendo dois pixels fora da junta, sem ninguem
saber de onde veio.

A terceira copia, essa inevitavel, e o ListenerModel.java: la a hierarquia e um ModelPart, e
e ele quem manda em tempo de jogo. Mexeu la, mexe aqui.

Espaco: pixels do Minecraft (1/16 de bloco), Y crescendo para BAIXO, chao em y=24.
"""

import math

FRONT_UPPER_Z = 2.18
FRONT_FORE_Z = 2.08
FRONT_HAND_Z = 0.10
BACK_THIGH_X = 2.00
BACK_SHIN_X = -2.10
BACK_FOOT_X = 0.10
# ⚠️ A CABECA SUBIU depois do teste de frente. Com pescoco em 0.30 ela caia por baixo do
# peito: de frente o bicho quase nao tinha rosto — so o dorso e as pernas, com a boca
# escondida embaixo. A cabeca dele e a unica coisa que o identifica, entao ela precisa
# cortar a silhueta do corpo em vez de se esconder nela. Ainda fica ABAIXO da linha dos
# ombros (e um quadrupede que chega rente ao chao), so que agora a frente.
NECK_X = 0.12
HEAD_X = -0.06

# ⚠️ A MANDIBULA NASCE FECHADA (zero), e nao entreaberta. O ListenerModel monta o osso com
# `PartPose.offset` puro — sem rotacao — e a malha tinha sido esculpida supondo 0.22. Uma
# malha assada numa pose de repouso que o rig nao tem produz uma boca que nunca fecha
# direito, e o erro so aparece no jogo.
JAW_X = 0.0

# (nome, pai, pivo, rotacao de repouso, raio da carne na base, raio na ponta)
# ⚠️ AS PROPORCOES FORAM REFEITAS depois do teste do Pedro: a boca ocupava quase metade da
# criatura e o corpo era magro demais para sustenta-la. Na referencia que ele mandou o peso
# esta no CORPO — pescoco grosso, nuca musculosa, cabeca menor que o peito — e a boca
# assusta por ser cheia de dente, nao por ser grande. Aqui: o torso engordou, o pescoco
# virou um volume de verdade (ele funde a cabeca no corpo, que era onde aparecia a fenda) e
# a cabeca encolheu de 13 para 10 de comprimento.
BONES = [
    ("spine", None, (0.0, 13.0, 0.0), (0.0, 0.0, 0.0), 4.6, 4.4),
    ("chest", "spine", (0.0, 0.0, -6.0), (0.0, 0.0, 0.0), 5.2, 4.0),
    ("neck", "chest", (0.0, 1.5, -6.5), (NECK_X, 0.0, 0.0), 3.6, 3.0),
    ("head", "neck", (0.0, 0.0, -4.0), (HEAD_X, 0.0, 0.0), 3.0, 1.0),
    ("jaw", "head", (0.0, 2.0, 0.0), (JAW_X, 0.0, 0.0), 2.2, 0.8),
    ("hip", "spine", (0.0, 0.0, 6.0), (0.0, 0.0, 0.0), 4.4, 2.2),
]

for _side, _s in (("right", -1.0), ("left", 1.0)):
    BONES += [
        ("front_upper_" + _side, "chest", (_s * 4.0, -2.0, -4.0),
         (0.0, 0.0, -_s * FRONT_UPPER_Z), 2.4, 1.4),
        ("front_fore_" + _side, "front_upper_" + _side, (0.0, 11.0, 0.0),
         (0.0, 0.0, _s * FRONT_FORE_Z), 1.5, 1.1),
        ("front_hand_" + _side, "front_fore_" + _side, (0.0, 17.5, 0.0),
         (0.0, 0.0, _s * FRONT_HAND_Z), 1.8, 1.0),
        ("back_thigh_" + _side, "hip", (_s * 3.5, -1.0, 3.0),
         (BACK_THIGH_X, 0.0, 0.0), 3.0, 1.8),
        ("back_shin_" + _side, "back_thigh_" + _side, (0.0, 9.0, 0.0),
         (BACK_SHIN_X, 0.0, 0.0), 1.9, 1.2),
        ("back_foot_" + _side, "back_shin_" + _side, (0.0, 14.5, 0.0),
         (BACK_FOOT_X, 0.0, 0.0), 1.7, 1.0),
    ]

# --------------------------------------------------------------------- os segmentos
#
# ⚠️ CADA OSSO TEM O SEU EIXO, E ISSO JA CUSTOU UMA RODADA. A primeira versao supos que todo
# osso cresce no proprio +Y (como os membros) e a CABECA SUMIU: pescoco, cabeca, mandibula,
# mao e pe crescem para a FRENTE (-Z) nas caixas originais, entao a cabeca virou uma coluna
# descendo do pescoco e o remesh a fundiu dentro do peito. O sintoma — "o bicho ficou sem
# cabeca" — nao apontava para "eixo trocado" de jeito nenhum.
#
# Agora o segmento e explicito: de onde ate onde, em coordenadas locais do osso. Sem regra
# implicita, sem excecao para lembrar.
#
#   nome: (ponto inicial, ponto final)
SEGMENT = {
    # o tronco, comprido no Z: quadrupede baixo
    "spine": ((0.0, -0.5, -6.0), (0.0, -0.5, 6.0)),
    "chest": ((0.0, -0.8, 0.0), (0.0, -1.0, -7.0)),
    "hip": ((0.0, -0.2, 0.0), (0.0, 0.2, 6.0)),

    # a frente: tudo aponta para -Z, e a cabeca fica MAIS BAIXA que os ombros
    "neck": ((0.0, 0.5, 0.0), (0.0, 0.5, -4.0)),
    "head": ((0.0, -0.8, 0.0), (0.0, -1.2, -10.0)),
    "jaw": ((0.0, 1.4, 0.0), (0.0, 1.0, -9.0)),
}

for _side in ("right", "left"):
    SEGMENT.update({
        # os membros, esses sim, descem no +Y
        "front_upper_" + _side: ((0.0, -1.0, 0.0), (0.0, 12.0, 0.0)),
        "front_fore_" + _side: ((0.0, 0.0, 0.0), (0.0, 18.0, 0.0)),
        "back_thigh_" + _side: ((0.0, -1.0, 0.0), (0.0, 10.0, 0.0)),
        "back_shin_" + _side: ((0.0, 0.0, 0.0), (0.0, 15.0, 0.0)),

        # a mao e o pe sao chatos e apontam para a frente
        "front_hand_" + _side: ((0.0, 0.5, 0.0), (0.0, 1.5, -4.0)),
        "back_foot_" + _side: ((0.0, 0.5, 0.0), (0.0, 1.5, -4.5)),
    })


# --------------------------------------------------------------------- matriz 4x4

def identity():
    return [[1.0 if i == j else 0.0 for j in range(4)] for i in range(4)]


def multiply(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def translation(v):
    m = identity()
    m[0][3], m[1][3], m[2][3] = v
    return m


def rotation(rot):
    """
    A ordem do ModelPart: Z, depois Y, depois X — como MULTIPLICACAO A DIREITA.

    ⚠️ ISTO ESTAVA INVERTIDO E ERA UM BUG A ESPERA DE ACONTECER. `translateAndRotate` faz
    `M · Rz · Ry · Rx`, e aqui estava `Rx · Ry · Rz`. Com um eixo so por osso — que e o caso
    de quase todos — as duas dao no mesmo, e por isso o preview batia com o jogo. Na primeira
    animacao que gira dois eixos juntos (a cabeca varrendo, que tem yaw E pitch) as duas
    divergem, e o sintoma seria a cabeca deformando de um jeito que ninguem liga a "ordem de
    multiplicacao no conferidor".
    """
    rx, ry, rz = rot
    cz, sz = math.cos(rz), math.sin(rz)
    cy, sy = math.cos(ry), math.sin(ry)
    cx, sx = math.cos(rx), math.sin(rx)

    mz = identity()
    mz[0][0], mz[0][1] = cz, -sz
    mz[1][0], mz[1][1] = sz, cz

    my = identity()
    my[0][0], my[0][2] = cy, sy
    my[2][0], my[2][2] = -sy, cy

    mx = identity()
    mx[1][1], mx[1][2] = cx, -sx
    mx[2][1], mx[2][2] = sx, cx

    return multiply(mz, multiply(my, mx))


def apply(m, p):
    return (m[0][0] * p[0] + m[0][1] * p[1] + m[0][2] * p[2] + m[0][3],
            m[1][0] * p[0] + m[1][1] * p[1] + m[1][2] * p[2] + m[1][3],
            m[2][0] * p[0] + m[2][1] * p[1] + m[2][2] * p[2] + m[2][3])


def apply_dir(m, p):
    return (m[0][0] * p[0] + m[0][1] * p[1] + m[0][2] * p[2],
            m[1][0] * p[0] + m[1][1] * p[1] + m[1][2] * p[2],
            m[2][0] * p[0] + m[2][1] * p[1] + m[2][2] * p[2])


def invert_rigid(m):
    """Inversa de rotacao+translacao (sem escala) — a transposta da rotacao resolve."""
    r = [[m[j][i] for j in range(3)] for i in range(3)]
    t = [m[0][3], m[1][3], m[2][3]]
    inv = identity()
    for i in range(3):
        for j in range(3):
            inv[i][j] = r[i][j]
        inv[i][3] = -(r[i][0] * t[0] + r[i][1] * t[1] + r[i][2] * t[2])
    return inv


def frames(pose=None):
    """
    A matriz de cada osso. Sem `pose`, e a POSE DE REPOUSO (a de bind).

    `pose` e um dicionario nome -> (rx, ry, rz) SOMADO a rotacao de repouso — que e
    exatamente como o ListenerModel anima: soma, nunca escreve por cima, senao o braco
    perderia a abertura no primeiro tick.
    """
    out = {}
    for (name, parent, pivot, rot, r0, r1) in BONES:
        base = out[parent] if parent else identity()
        extra = pose.get(name, (0.0, 0.0, 0.0)) if pose else (0.0, 0.0, 0.0)
        total = (rot[0] + extra[0], rot[1] + extra[1], rot[2] + extra[2])
        out[name] = multiply(base, multiply(translation(pivot), rotation(total)))
    return out


def segment(name, frame):
    """Onde o osso comeca e termina, em espaco de modelo."""
    a, b = SEGMENT.get(name, ((0.0, 0.0, 0.0), (0.0, 4.0, 0.0)))
    return apply(frame, a), apply(frame, b)
