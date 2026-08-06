"""
O ESCUTADOR esculpido: UM corpo so, com esqueleto e pesos. RODA NO BLENDER.

    "C:\\Program Files\\Blender Foundation\\Blender 4.2\\blender.exe" -b -P tools/sculpt_listener.py

Escreve:
  • assets/recmod/meshes/listener.smesh   — malha unica + pesos por vertice (o que o jogo le)
  • tools/_blend/listener.blend           — o arquivo para o Pedro abrir e ESCULPIR a mao

⚠️ POR QUE ISTO SUBSTITUI AS PECAS RIGIDAS. A versao anterior desenhava um tubo por osso, e
no jogo da para contar os tubos: o pescoco nao encosta no peito, o ombro abre uma fenda ao
girar. Junta rigida so se esconde com sobreposicao, e sobreposicao nao produz um corpo — ela
produz um monte de pecas que se atravessam. Corpo continuo exige DEFORMACAO, e deformacao
exige peso por vertice. E o mesmo salto que qualquer criatura organica deste mod vai precisar
dar (referencia do Pedro: The Mimic), entao o formato nasce generico.

<h3>A escultura e PROCEDURAL, e ainda assim escultura</h3>
Ninguem passa pincel aqui: o corpo sai de um esqueleto de arestas + `Skin` (que da volume por
vertice) + `Remesh` em voxel — e o remesh e o passo que interessa, porque e ele que FUNDE
tudo numa superficie unica e fechada. Depois `Decimate` traz de volta para o orcamento low
poly. O resultado e um bicho de uma peca so, organico, facetado.

O .blend fica salvo com o corpo ja pronto e o armature ja pesado: abrir, entrar em Sculpt
Mode e puxar a massa continua funcionando — e depois e so rodar o export de novo.

⚠️ AS MEDIDAS SAO AS DO RIG (ListenerModel). Espaco do Minecraft: pixels, Y para BAIXO, e a
mesma pose de repouso. E o que faz a malha nova cair exatamente onde a animacao ja conferida
espera encontra-la.
"""

import math
import os
import struct
import sys

import bpy
import bmesh
from mathutils import Matrix, Vector

HERE = os.path.dirname(os.path.abspath(bpy.data.filepath or __file__))
ROOT = os.path.dirname(HERE) if os.path.basename(HERE) == "tools" else os.getcwd()

OUT_MESH = os.path.join(ROOT, "src", "main", "resources", "assets", "recmod",
                        "meshes", "listener.smesh")
OUT_BLEND = os.path.join(ROOT, "tools", "_blend", "listener.blend")

MAGIC = b"RECS"
VERSION = 1

# Orcamento low poly. Alto o bastante para a curva ler, baixo o bastante para o VHS.
TRI_BUDGET = 2600

# Tamanho do voxel do remesh, em pixels de Minecraft. Menor = mais detalhe e mais custo;
# ⚠️ maior que ~1.4 come as garras e os dentes, que sao a leitura da criatura de perto.
VOXEL = 0.9


# --------------------------------------------------------------------- o esqueleto
#
# ⚠️ A TABELA MORA EM listener_rig.py, EM UM LUGAR SO — este script e o conferidor de fora do
# jogo leem a mesma. Duas listas iguais e o jeito conhecido de produzir a criatura "quase
# certa", com o membro nascendo fora da junta e ninguem sabendo de onde veio.

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import listener_rig as RIG  # noqa: E402

BONES = RIG.BONES


def world_frames():
    """Pivo e rotacao acumulados de cada osso, na pose de repouso."""
    return {name: Matrix(m) for name, m in RIG.frames().items()}


def bone_segment(name, frames):
    """Onde o osso comeca e onde termina, em espaco de modelo."""
    a, b = RIG.segment(name, [list(row) for row in frames[name]])
    return Vector(a), Vector(b)


# --------------------------------------------------------------------- construcao

def clear_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)


def build_skin_body(frames):
    """
    O CORPO: esqueleto de arestas -> Skin -> Remesh.

    ⚠️ O `Skin` da volume a cada VERTICE, entao a espessura da criatura e controlada aqui,
    osso a osso — grosso no torso, quase osso no punho. E o `Remesh` em voxel e o passo que
    junta tudo: e ele que transforma vinte tubos que se atravessam numa superficie unica.
    Sem ele o problema da screenshot continuaria existindo, so que dentro do Blender.
    """
    mesh = bpy.data.meshes.new("listener")
    obj = bpy.data.objects.new("listener", mesh)
    bpy.context.collection.objects.link(obj)

    bm = bmesh.new()
    made = {}

    for (name, parent, pivot, rot, r0, r1) in BONES:
        a, b = bone_segment(name, frames)
        va = bm.verts.new(a)
        vb = bm.verts.new(b)
        bm.edges.new((va, vb))
        made[name] = (va, vb, r0, r1)

    # Costura os ossos ao pai para o esqueleto ser UM grafo conexo — Skin em ilhas soltas
    # devolveria o mesmo problema das pecas.
    bm.verts.ensure_lookup_table()
    for (name, parent, pivot, rot, r0, r1) in BONES:
        if parent is None:
            continue
        # ⚠️ Costura no vertice do pai que estiver MAIS PERTO: pendurar sempre na ponta
        # faria o braco nascer no fim do peito em vez do ombro, e o Skin esticaria carne
        # entre dois pontos que nao se tocam.
        va = made[name][0]
        pa, pb_end = made[parent][0], made[parent][1]
        d0 = (pa.co - va.co).length
        d1 = (pb_end.co - va.co).length
        pb = pa if d0 <= d1 else pb_end
        try:
            bm.edges.new((pb, va))
        except ValueError:
            pass  # ja existe

    bm.to_mesh(mesh)
    bm.free()

    skin = obj.modifiers.new("Skin", 'SKIN')
    skin.use_smooth_shade = False

    # Os raios por vertice: e o que da a silhueta esfomeada — poucos volumes grandes e
    # membros muito finos, a regra de anatomia que ja saiu do lote dos infectados.
    mesh = obj.data
    layer = mesh.skin_vertices[0].data
    index = 0
    for (name, parent, pivot, rot, r0, r1) in BONES:
        layer[index].radius = (r0, r0)
        layer[index + 1].radius = (r1, r1)
        index += 2

    remesh = obj.modifiers.new("Remesh", 'REMESH')
    remesh.mode = 'VOXEL'
    remesh.voxel_size = VOXEL
    remesh.adaptivity = 0.0

    return obj


def sculpt_pass(obj):
    """
    O passe de escultura: o que o pincel faria, feito por operador.

    ⚠️ Sculpt Mode de verdade e interativo e nao roda sem tela. O que da para fazer sem tela
    e o que importa aqui — inflar o peito, afinar a cintura, arrastar a massa da mandibula —
    e isso e deslocamento por proximidade, exatamente o que um brush faz. O .blend sai salvo
    com o corpo ja pronto para o Pedro terminar a mao.
    """
    mesh = obj.data
    matrix = obj.matrix_world

    # (centro, raio, forca) — forca positiva infla, negativa afunda.
    #
    # ⚠️ Refeito depois do teste: o peso da criatura tem que estar no CORPO. Na referencia
    # que o Pedro mandou a nuca e um volume musculoso que empurra a cabeca para a frente, e e
    # isso que faz a boca parecer presa a um bicho em vez de flutuando. As pancadas fortes
    # agora sao no tronco; a cabeca so recebe massa na base.
    strokes = [
        (Vector((0.0, 8.0, -9.0)), 8.0, 1.5),     # peito, cheio
        (Vector((0.0, 7.0, -13.0)), 6.0, 1.3),    # a NUCA: o volume que empurra a cabeca
        (Vector((0.0, 11.0, -6.0)), 6.5, 0.7),    # a caixa toracica desce
        (Vector((0.0, 13.5, 1.0)), 6.0, -0.9),    # cintura afunda: o bicho e esfomeado
        (Vector((0.0, 10.5, 2.0)), 5.0, -0.5),
        (Vector((0.0, 9.0, 7.0)), 7.0, 1.1),      # ancas, para a perna traseira ter de onde sair
        (Vector((0.0, 15.5, -17.0)), 3.5, 0.35),  # so a base da mandibula
    ]

    for v in mesh.vertices:
        p = v.co
        delta = Vector((0.0, 0.0, 0.0))
        for (center, radius, force) in strokes:
            d = (p - center).length
            if d > radius:
                continue
            falloff = (1.0 - d / radius) ** 2
            direction = (p - center).normalized() if d > 1e-4 else Vector((0.0, -1.0, 0.0))
            delta += direction * force * falloff
        v.co = p + delta


def decimate(obj):
    """Volta para o orcamento low poly — e a faceta e a estetica, nao um defeito."""
    bpy.context.view_layer.objects.active = obj
    bpy.ops.object.modifier_apply(modifier="Skin")
    bpy.ops.object.modifier_apply(modifier="Remesh")

    sculpt_pass(obj)

    tris = len(obj.data.polygons) * 2
    if tris > TRI_BUDGET:
        dec = obj.modifiers.new("Decimate", 'DECIMATE')
        dec.ratio = min(1.0, TRI_BUDGET / float(tris))
        bpy.ops.object.modifier_apply(modifier="Decimate")


def build_armature(frames):
    """
    O ESQUELETO DE VERDADE, com um osso por parte do rig.

    ⚠️ Os nomes tem que ser os mesmos do ModelPart no Java: e por eles que o jogo casa a
    matriz de cada osso com o peso de cada vertice. Nome trocado = membro que nao se mexe, e
    o sintoma no jogo (um braco parado) nao aponta para "erro de nome no exportador".
    """
    arm_data = bpy.data.armatures.new("rig")
    arm = bpy.data.objects.new("rig", arm_data)
    bpy.context.collection.objects.link(arm)

    bpy.context.view_layer.objects.active = arm
    bpy.ops.object.mode_set(mode='EDIT')

    edit = {}
    for (name, parent, pivot, rot, r0, r1) in BONES:
        a, b = bone_segment(name, frames)
        bone = arm_data.edit_bones.new(name)
        bone.head = a
        bone.tail = b if (b - a).length > 0.05 else a + Vector((0.0, 1.0, 0.0))
        edit[name] = bone

    for (name, parent, pivot, rot, r0, r1) in BONES:
        if parent:
            edit[name].parent = edit[parent]

    bpy.ops.object.mode_set(mode='OBJECT')
    return arm


def bind(obj, arm):
    """Pesos automaticos por envelope de osso — e o que faz a junta DOBRAR em vez de abrir."""
    bpy.ops.object.select_all(action='DESELECT')
    obj.select_set(True)
    arm.select_set(True)
    bpy.context.view_layer.objects.active = arm
    bpy.ops.object.parent_set(type='ARMATURE_AUTO')


def add_details(obj, frames):
    """
    DENTE, GARRA E CRISTA — depois do remesh, e presos direto ao osso.

    ⚠️ ELES NAO PODEM PASSAR PELO REMESH. Um voxel de 0.9 come qualquer ponta mais fina que
    isso, e dente e garra sao justamente a leitura da criatura de perto — o remesh devolveria
    um bicho liso e sem mordida. Entao entram depois, ja na malha final.
    E, por entrarem depois do `parent_set`, os pesos automaticos nao os alcancam: cada
    vertice novo e amarrado a MAO no osso certo, com peso 1. Sem isso o dente ficaria parado
    no ar enquanto a cabeca vira — e o sintoma ("dentes flutuando") nao aponta para "os pesos
    nao foram atribuidos".
    """
    mesh = obj.data
    bm = bmesh.new()
    bm.from_mesh(mesh)

    # ⚠️ O DONO E MARCADO NO PROPRIO VERTICE, e nao por faixa de indice.
    #
    # A primeira versao anotava `range(antes, depois)` em volta de cada `create_cone` — e
    # ESTAVA ERRADA: `len(bm.verts)` le uma tabela que so e reconstruida no
    # `ensure_lookup_table`, entao as faixas saiam defasadas e cada detalhe ficava amarrado
    # ao osso do detalhe SEGUINTE. A garra da mao esquerda ganhou peso 1.0 em
    # `back_foot_left`, a 36 pixels do osso que a comandava.
    #
    # ⚠️ E o defeito era INVISIVEL PARADO: com todos os ossos em repouso, as matrizes de
    # skinning sao identidade e o vertice fica no lugar certo mesmo amarrado no osso errado.
    # So andando ele parte atras do osso errado — que foi exatamente o sintoma no jogo
    # ("os dentes e as unhas desmancham quando ele anda"). Marcar no vertice tira a
    # contabilidade de indices do caminho de vez.
    owner = bm.verts.layers.int.new("owner")
    kind_layer = bm.verts.layers.int.new("kind")

    bone_ids = {}

    def mark(verts, bone, kind):
        bone_id = bone_ids.setdefault(bone, len(bone_ids) + 1)
        code = 1 if kind == "bone" else 2
        for v in verts:
            v[owner] = bone_id
            v[kind_layer] = code

    def cone(bone, base, tip, radius, segments=4):
        d = tip - base
        length = d.length
        if length < 0.01:
            return
        rot = Vector((0.0, 0.0, 1.0)).rotation_difference(d.normalized()).to_matrix().to_4x4()
        matrix = Matrix.Translation((base + tip) / 2.0) @ rot

        result = bmesh.ops.create_cone(bm, cap_ends=True, cap_tris=True, segments=segments,
                                       radius1=radius, radius2=0.0, depth=length, matrix=matrix)
        mark(result["verts"], bone, "bone")

    def blade(bone, a, b, c):
        # ⚠️ O verso precisa dos PROPRIOS vertices: `faces.new` com os mesmos tres devolve
        # "face already exists". E sem verso a lamina some quando vista do outro lado, que e
        # metade das vezes.
        front = [bm.verts.new(a), bm.verts.new(b), bm.verts.new(c)]
        back = [bm.verts.new(c), bm.verts.new(b), bm.verts.new(a)]
        bm.faces.new(front)
        bm.faces.new(back)
        mark(front + back, bone, "crest")

    def at(bone, p):
        """Um ponto local do osso, levado para o espaco do modelo."""
        return Vector(RIG.apply([list(r) for r in frames[bone]], p))

    # ---- OS DENTES.
    #
    # ⚠️ A LICAO DA REFERENCIA QUE O PEDRO MANDOU: o que assusta e a QUANTIDADE, nao o
    # tamanho. Eram seis pares de cones gordos por arcada, e o resultado no jogo foi uma boca
    # de brinquedo — dente grande le como plastico. Agora sao dez por arcada, finos, de
    # alturas alternadas e levemente tortos, apinhados na borda. Um dentao no meio de dentes
    # pequenos e o que da a impressao de dentadura que cresceu errado, e essa impressao vale
    # mais que qualquer detalhe de malha.
    TEETH = 10
    for i in range(TEETH):
        t = i / float(TEETH - 1)
        # o comprimento alterna: dente grande, dois pequenos, dente grande...
        big = 1.0 if i % 3 == 0 else 0.62
        lean = (0.30 if i % 2 == 0 else -0.22)

        for s in (-1.0, 1.0):
            zt = -1.2 - t * 7.4
            xt = 2.3 - t * 1.7
            cone("head",
                 at("head", (s * xt, 0.5, zt)),
                 at("head", (s * (xt - 0.35), 2.0 + big * 1.9, zt + lean)),
                 (0.30 + (1.0 - t) * 0.12) * (1.0 if big > 0.8 else 0.78),
                 segments=3)

            zb = -1.2 - t * 6.8
            xb = 1.8 - t * 1.2
            cone("jaw",
                 at("jaw", (s * xb, 1.2, zb)),
                 at("jaw", (s * (xb - 0.25), -1.2 - big * 1.7, zb + lean)),
                 (0.27 + (1.0 - t) * 0.10) * (1.0 if big > 0.8 else 0.78),
                 segments=3)

    # ---- as garras
    for side in ("right", "left"):
        hand = "front_hand_" + side
        for c in range(3):
            off = (c - 1) * 1.5
            cone(hand, at(hand, (off, 1.6, -2.5)), at(hand, (off * 1.5, 3.2, -8.0)), 0.6)

        foot = "back_foot_" + side
        for c in range(2):
            off = (c - 0.5) * 1.8
            cone(foot, at(foot, (off, 1.6, -3.0)), at(foot, (off * 1.4, 2.8, -7.0)), 0.5)

    # ---- a crista: laminas irregulares no dorso, nao um pente
    for i in range(7):
        z = 6.0 - i * 2.0
        h = 3.2 + (2.4 if i in (2, 3) else 0.0) - abs(i - 3) * 0.3
        lean = -0.6 - i * 0.12
        blade("spine",
              at("spine", (0.0, -3.4, z + 1.1)),
              at("spine", (0.0, -3.4 - h, z + lean)),
              at("spine", (0.0, -3.4, z - 1.1)))

    # Agora sim: os indices finais, lidos DEPOIS de o bmesh se acertar.
    bm.verts.index_update()
    id_to_bone = {v: k for k, v in bone_ids.items()}

    owned = {}
    kinds = {}
    for v in bm.verts:
        bone_id = v[owner]
        if bone_id == 0:
            continue
        owned.setdefault(id_to_bone[bone_id], []).append(v.index)
        kinds[v.index] = "bone" if v[kind_layer] == 1 else "crest"

    bm.to_mesh(mesh)
    bm.free()

    for bone, indices in owned.items():
        group = obj.vertex_groups.get(bone) or obj.vertex_groups.new(name=bone)
        group.add(indices, 1.0, 'REPLACE')

        # E tira esses vertices de qualquer outro grupo: peso dividido num dente produz
        # dente derretendo em vez de dente virando com a mandibula.
        for other in obj.vertex_groups:
            if other.name != bone:
                other.remove(indices)

    return kinds


# --------------------------------------------------------------------- cor

CHAR = (0.13, 0.10, 0.11)
SOOT = (0.21, 0.18, 0.19)
ASH = (0.34, 0.29, 0.29)
PALE = (0.48, 0.41, 0.40)
BLOOD = (0.28, 0.07, 0.08)
RAW = (0.59, 0.16, 0.15)
BONE_C = (0.78, 0.74, 0.65)
CREST_C = (0.07, 0.06, 0.07)


def paint(obj, kinds, frames):
    """
    A COR VAI NO VERTICE, e nao numa textura.

    ⚠️ E a escolha certa para low poly facetado, e nao uma economia: com faces chapadas, um
    atlas de 128px so acrescenta borrao e uma segunda coisa para desalinhar (o UV do
    `smart_project` muda a cada rebuild — pintar ilha a ilha seria refazer a arte toda vez).
    Cor por vertice nasce colada na geometria e some da lista de coisas que podem
    dessincronizar. O grao de VHS do jogo por cima ja da a sujeira que a textura daria.

    A regra: o corpo escurece de cima para baixo (dorso claro, barriga preta) e da raiz para a
    ponta dos membros; carne viva SO na boca; e dente, garra e crista sao as unicas coisas que
    fogem da paleta — porque sao as unicas que o jogador precisa ler no escuro.
    """
    mesh = obj.data
    colors = mesh.color_attributes.get("col") or mesh.color_attributes.new(
        name="col", type='FLOAT_COLOR', domain='POINT')

    # ⚠️ A COR E DECIDIDA NO ESPACO DO OSSO DOMINANTE, e nao pela posicao no mundo. A versao
    # anterior pintava "tudo que estiver perto da cabeca e embaixo" e o resultado foi a
    # MANDIBULA INTEIRA vermelha por fora — a criatura parecia ter levado um banho de sangue
    # em vez de ter uma boca. Gengiva e uma FAIXA na borda das arcadas, e so se sabe onde e
    # essa borda perguntando ao osso.
    bone_of = {}
    for v in mesh.vertices:
        best, best_w = None, 0.0
        for g in v.groups:
            if g.weight > best_w:
                best_w = g.weight
                best = obj.vertex_groups[g.group].name
        bone_of[v.index] = best

    inverse = {name: RIG.invert_rigid([list(r) for r in m]) for name, m in frames.items()}

    for i, v in enumerate(mesh.vertices):
        kind = kinds.get(i)

        if kind == "bone":
            c = BONE_C
        elif kind == "crest":
            c = CREST_C
        else:
            p = v.co
            bone = bone_of.get(i)

            # ⚠️ y CRESCE PARA BAIXO: 4 e o alto do dorso, 24 e o chao. Inverter isto pinta
            # o bicho de cabeca para baixo e a "correcao" seria mexer no rig, que esta certo.
            t = max(0.0, min(1.0, (p.y - 4.0) / 18.0))
            c = tuple(PALE[k] + (CHAR[k] - PALE[k]) * (t ** 1.3) for k in range(3))

            if bone in ("head", "jaw"):
                local = RIG.apply(inverse[bone], (p.x, p.y, p.z))

                # A gengiva: a faixa colada na linha da boca. Na cabeca e a borda de BAIXO
                # (local y perto de +1); na mandibula, a de CIMA (local y perto de 0).
                edge = abs(local[1] - (1.0 if bone == "head" else 0.2))
                inside = max(0.0, 1.0 - edge / 1.8)

                # e so na parte da frente: a nuca nao tem gengiva.
                if local[2] > -1.0:
                    inside = 0.0

                c = tuple(c[k] + (RAW[k] - c[k]) * inside for k in range(3))

                # o resto da cabeca e mais escuro que o corpo — ela e a parte que chega
                # primeiro no escuro, e tem que ler como vulto ate a boca abrir.
                c = tuple(c[k] * (0.85 + 0.15 * inside) for k in range(3))

            # sangue seco nas dobras fundas do corpo
            elif 0.35 < t < 0.55:
                c = tuple(c[k] * 0.75 + BLOOD[k] * 0.25 for k in range(3))

        colors.data[i].color = (c[0], c[1], c[2], 1.0)


def unwrap(obj):
    """UV automatico. A textura e pintada por cima disto, entao o corte pode ser feio."""
    bpy.ops.object.select_all(action='DESELECT')
    obj.select_set(True)
    bpy.context.view_layer.objects.active = obj
    bpy.ops.object.mode_set(mode='EDIT')
    bpy.ops.mesh.select_all(action='SELECT')
    bpy.ops.uv.smart_project(angle_limit=math.radians(66.0), island_margin=0.02)
    bpy.ops.object.mode_set(mode='OBJECT')


# --------------------------------------------------------------------- export

def export(obj, arm, path):
    """
    O formato .smesh: uma malha so + ate quatro ossos por vertice.

    ⚠️ NAO GRAVA A MATRIZ DE BIND. Quem sabe a pose de repouso e o Java, pelos `initialPose`
    dos ModelPart — e ter duas fontes para a mesma verdade e como se produz criatura torta
    que ninguem explica. Aqui so vai o vertice em espaco de MODELO na pose de repouso, que e
    a mesma coisa que o ModelPart entende.
    """
    mesh = obj.data
    mesh.calc_loop_triangles()

    # ⚠️ O Blender 4.1 tirou `calc_normals_split` — as normais de canto passaram a ser
    # calculadas sozinhas e a morar em `mesh.corner_normals`. O fallback existe para o
    # script nao virar refem da versao do Blender que a maquina tiver.
    corner_normals = None
    if hasattr(mesh, "corner_normals"):
        corner_normals = [n.vector.copy() for n in mesh.corner_normals]
    elif hasattr(mesh, "calc_normals_split"):
        mesh.calc_normals_split()

    bone_index = {b.name: i for i, b in enumerate(arm.data.bones)}
    group_bone = {g.index: bone_index.get(g.name, -1) for g in obj.vertex_groups}

    uv_layer = mesh.uv_layers.active.data
    color_layer = mesh.color_attributes.get("col")

    # Um vertice por CANTO (loop): o UV e por canto, e juntar por vertice colaria as ilhas.
    verts = []
    index_of = {}
    tris = []

    for tri in mesh.loop_triangles:
        out = []
        for loop_index in tri.loops:
            loop = mesh.loops[loop_index]
            vid = loop.vertex_index
            uv = uv_layer[loop_index].uv
            normal = (corner_normals[loop_index] if corner_normals is not None
                      else mesh.vertices[vid].normal)
            key = (vid, round(uv[0], 5), round(uv[1], 5))

            if key not in index_of:
                v = mesh.vertices[vid]

                weights = []
                for g in v.groups:
                    b = group_bone.get(g.group, -1)
                    if b >= 0 and g.weight > 0.0001:
                        weights.append((b, g.weight))
                weights.sort(key=lambda w: -w[1])
                weights = weights[:4]

                total = sum(w for _, w in weights) or 1.0
                weights = [(b, w / total) for b, w in weights]
                while len(weights) < 4:
                    weights.append((0, 0.0))

                col = color_layer.data[vid].color if color_layer else (1.0, 1.0, 1.0, 1.0)

                index_of[key] = len(verts)
                verts.append((v.co, normal, uv, weights, col))
            out.append(index_of[key])
        tris.append(tuple(out))

    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(MAGIC)
        f.write(struct.pack("B", VERSION))

        names = [b.name for b in arm.data.bones]
        f.write(struct.pack(">H", len(names)))
        for n in names:
            raw = n.encode("utf-8")
            f.write(struct.pack("B", len(raw)))
            f.write(raw)

        f.write(struct.pack(">I", len(verts)))
        for (co, normal, uv, weights, col) in verts:
            f.write(struct.pack(">fff", co.x, co.y, co.z))
            f.write(struct.pack("bbb",
                                max(-127, min(127, int(normal.x * 127))),
                                max(-127, min(127, int(normal.y * 127))),
                                max(-127, min(127, int(normal.z * 127)))))
            f.write(struct.pack(">ff", uv[0], 1.0 - uv[1]))
            # A cor do vertice: tres bytes, sem alfa (a transparencia da criatura, quando
            # houver, e do renderer inteiro e nao de um pedaco dela).
            f.write(struct.pack("BBB",
                                max(0, min(255, int(col[0] * 255))),
                                max(0, min(255, int(col[1] * 255))),
                                max(0, min(255, int(col[2] * 255)))))
            for (b, w) in weights:
                f.write(struct.pack(">Hf", b, w))

        f.write(struct.pack(">I", len(tris)))
        for (a, b, c) in tris:
            f.write(struct.pack(">III", a, b, c))

    return len(verts), len(tris), names


def main():
    clear_scene()
    frames = world_frames()

    body = build_skin_body(frames)
    decimate(body)

    arm = build_armature(frames)
    bind(body, arm)

    # ⚠️ Os detalhes entram DEPOIS do bind (para escapar dos pesos automaticos, que
    # derreteriam os dentes) e ANTES do unwrap (para terem UV como todo o resto).
    kinds = add_details(body, frames)
    unwrap(body)
    paint(body, kinds, frames)

    verts, tris, names = export(body, arm, OUT_MESH)

    os.makedirs(os.path.dirname(OUT_BLEND), exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=OUT_BLEND)

    print("=" * 60)
    print("escrito:", OUT_MESH)
    print("  %d vertices, %d triangulos, %d ossos" % (verts, tris, len(names)))
    print("blend para esculpir a mao:", OUT_BLEND)
    print("=" * 60)


if __name__ == "__main__":
    main()
