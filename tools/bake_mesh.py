"""
Assa uma escultura 3D em malha animada que o mod desenha de verdade. RODA NO BLENDER:

    blender -b -P tools/bake_mesh.py -- ophanim

POR QUE ESTE ARQUIVO EXISTE. O caminho antigo (tools/render_creatures.py) achatava a
escultura em sprite porque o Minecraft nao tem skinning de malha com ossos: nao existe
como entregar ao jogo uma malha e um esqueleto e pedir que ele deforme. A saida nao e
desistir da malha — e resolver o esqueleto AQUI. O Blender deforma, e o que sai sao as
posicoes de cada vertice ja prontas, quadro a quadro. O jogo so interpola e desenha.

E a mesma ideia do objmc (assar o vertice em vez de pedir skinning ao motor), mas sem o
core shader dele: o objmc esconde os vertices dentro de um PNG porque um resource pack
nao roda codigo. Nos rodamos. Entao o dado vem em binario limpo e quem desenha e o nosso
renderer, no caminho de entidade comum — o mesmo que todo mod usa. Isso importa muito
aqui: core shader morre com Embeddium/Oculus ligados, e os dois estao no pack.

O QUE SAI: `<id>_raw.npz` (geometria + quadros) e `<id>_mats.json` (quais imagens cada
material usa). O empacotador (tools/pack_mesh.py, fora do Blender) transforma os dois no
`.mesh` e no atlas `.png` que entram no jar.

A DIVISAO DE TRABALHO e de proposito: o Blender mexe em geometria, o PIL mexe em imagem.
Blender nao tem PIL, e escrever atlas de 4K em Python puro dentro dele e lento e fragil.
"""

import json
import os
import sys

import bpy
import numpy as np
from mathutils import Vector

ROOT = r"C:\Users\Hamilton\Downloads\vhsworldentities\3d"
OUT = r"C:\Users\Hamilton\Downloads\vhsworldentities\3d\_mesh"

# Orcamento de triangulos DEPOIS da decimacao.
#
# O caminho de render de entidade do Minecraft escreve vertice na CPU, um por um, todo
# quadro. As esculturas originais tem de 19 mil a 147 mil triangulos — nessa escala o
# jogo engasga com uma criatura na tela. 3500 e o numero que escolhi porque estas duas
# aparecem no escuro, atras do grao de VHS, e o Ofanim e uma torre que voce olha de
# longe: detalhe de malha que ninguem chega perto para ver e so custo.
TRI_BUDGET = int(os.environ.get("BAKE_TRIS", "3500"))

# Quantos quadros da animacao amostrar. O renderer interpola entre dois, entao 16 pousos
# viram movimento continuo — nao e a taxa de quadros da animacao, e o numero de POSES
# guardadas. Cada quadro custa 9 bytes por vertice.
FRAMES = 16

# caminho do modelo por apelido
MODELS = {
    "ophanim":  "ophanim-angel/source/opaawmea.fbx",
    "greyface": "greyface/source/inner/greyface.gltf",
    "void":     "void-creature/source/KelFinal.fbx",
    # As anomalias que nasceram em PNG, infladas por tools/inflate_png.py. A chave e o
    # id do AnomalyType (e nao o nome do recorte), porque e dela que sai o nome do .mesh
    # que o Java procura.
    "tall":         "_inflated/body_tall.obj",
    "claws_scream": "_inflated/body_claws.obj",
}

# Pecas para NAO importar, por apelido do modelo.
#
# Uma cena do Sketchfab nao vem so com a criatura: vem com o cenario da vitrine. O Cara
# Cinza trazia um "Floor" (o pedestal onde ele posa) e uma "Icosphere" de 2 metros (a
# esfera de iluminacao do estudio). A esfera era a pior: sendo mais alta que o proprio
# bicho, era ELA que definia a altura na hora de normalizar — a criatura sairia encolhida
# dentro de uma bolha invisivel. Peca que nao e corpo tem que cair antes do join.
DROP = {
    # Floor/Icosphere = o cenario da vitrine do Sketchfab.
    # GF_Bottles/GF_Items = a tralha que ele carrega (a caixa pendurada na mao, os frascos
    # na cintura). O Pedro pediu para tirar depois de ver no jogo, e ele tem razao: a
    # criatura e uma silhueta magra e alta, e bagagem pendurada a le como NPC carregando
    # loot em vez de coisa que veio atras de voce.
    "greyface": ("Floor", "Icosphere", "GF_Bottles", "GF_Items"),
}

# Faces para apagar por MATERIAL (nome exato do slot).
#
# O Ofanim vinha com dois materiais sem imagem nenhuma. Um deles, "Material.006", sao 520
# triangulos com a mesma extensao dos olhos: e a calota de VIDRO por cima de cada olho.
# No asset original ela e transparente; o FBX nao carrega esse tipo de shader, entao ela
# chegou como cor chapada e virou uma bola branca leitosa tapando cada olho — que e a
# unica coisa que o Ofanim tem para oferecer.
#
# Apagar e melhor que pintar de transparente: face invisivel ainda custa vertice, ainda
# entra nos 16 quadros e ainda gasta orcamento de decimacao que os olhos querem.
DROP_MATERIALS = {
    "ophanim": ("Material.006",),
}

# Modelos que chegam DEITADOS (FBX de Maya/C4D vem com Y para cima e o importador do
# Blender nao converte). A correcao nao mexe na malha: escolhe outro mapeamento de eixos
# na hora de converter para o espaco do Minecraft (ver `sample`). Os dois mapeamentos tem
# determinante +1, entao nenhum espelha a criatura — espelhar seria pior que deitar,
# porque nao se nota olhando e inverte a mao dela.
#
# ⚠️ LICAO CARA (void-creature): "o maior vao e a altura" NAO e regra. Eu olhei a caixa
# [0.40, 0.49, 0.26], vi o maior vao em Y, conclui que ela estava deitada e a levantei —
# e ela foi para o jogo em pe, empinada como gente. So que ela e uma ARANHA: o maior vao
# dela e o COMPRIMENTO do corpo, da cabeca a cauda, e nao a altura. Bicho de quatro (ou
# oito) patas e mais comprido que alto, e o modelo ja estava certo. A regra vale para
# humanoide e mais nada; para o resto, olhar o desenho antes de girar.
Y_UP_SOURCE = set()

# Criaturas cujo PONTO MAIS BAIXO nao e o pe.
#
# A void-creature tem uma cauda que desce mais que as pernas. Apoiando pelo ponto mais
# baixo, quem encostava no chao era a ponta da cauda e o corpo inteiro ficava no ar — foi
# o que o Pedro viu no jogo. O chao dela nao e onde a malha acaba, e onde a malha tem
# MASSA: a cauda e um fio de poucos vertices por altura, os pes sao varios de uma vez.
#
# ⚠️ A void-creature SAIU desta lista. Ela precisava disto enquanto estava em pe por
# engano: naquela pose a cauda pendia e era ela que encostava no chao. De quatro, como o
# modelo sempre foi, o ponto mais baixo ja sao os pes e a heuristica so afundava a
# criatura 8% no terreno. Remendo que existia por causa de outro erro.
GROUND_BY_MASS = set()


def log(msg):
    print("[bake] " + msg, flush=True)


def wipe():
    bpy.ops.wm.read_factory_settings(use_empty=True)


def load(path):
    ext = path.lower().rsplit(".", 1)[-1]
    if ext == "fbx":
        bpy.ops.import_scene.fbx(filepath=path)
    elif ext in ("gltf", "glb"):
        bpy.ops.import_scene.gltf(filepath=path)
    elif ext == "obj":
        bpy.ops.wm.obj_import(filepath=path)
    else:
        raise SystemExit("formato desconhecido: " + path)


def mesh_objects():
    return [o for o in bpy.data.objects if o.type == "MESH" and len(o.data.polygons) > 0]


def drop_scenery(name):
    """Tira da cena o que nao e a criatura (pedestal, esfera de luz do estudio)."""
    for junk in DROP.get(name, ()):
        for o in list(bpy.data.objects):
            if o.name == junk or o.name.startswith(junk + "."):
                log("descartada a peca de cenario: %s" % o.name)
                bpy.data.objects.remove(o, do_unlink=True)


def drop_materials(obj, name):
    """
    Apaga as faces de um material inteiro. Roda ANTES de decimar, de proposito: assim o
    orcamento de triangulos vai todo para geometria que alguem vai ver.
    """
    wanted = DROP_MATERIALS.get(name, ())
    if not wanted:
        return

    bpy.context.view_layer.objects.active = obj
    for i, slot in enumerate(obj.data.materials):
        if slot is None or slot.name not in wanted:
            continue
        before = len(obj.data.polygons)
        obj.active_material_index = i
        bpy.ops.object.mode_set(mode="EDIT")
        bpy.ops.mesh.select_mode(type="FACE")
        bpy.ops.mesh.select_all(action="DESELECT")
        bpy.ops.object.material_slot_select()
        bpy.ops.mesh.delete(type="FACE")
        bpy.ops.object.mode_set(mode="OBJECT")
        log("apagadas %d faces do material %s" % (before - len(obj.data.polygons), slot.name))


def clean_loose(obj):
    """
    Varre vertice e aresta que nao pertencem a face nenhuma.

    O Cara Cinza chegava com 13.434 vertices para 3.500 triangulos — o triplo do que a
    conta pede. A sobra vem do proprio asset (a malha das armas tem 16 mil vertices para
    8 mil triangulos) e o Decimate nao a leva embora: ele colapsa FACE, e vertice orfao
    nao esta em face nenhuma. Sem esta limpeza a gente pagaria, em cada um dos 16 quadros,
    o preco de guardar a posicao de milhares de pontos que ninguem desenha.
    """
    before = len(obj.data.vertices)
    bpy.context.view_layer.objects.active = obj
    bpy.ops.object.mode_set(mode="EDIT")
    bpy.ops.mesh.select_all(action="SELECT")
    bpy.ops.mesh.delete_loose(use_verts=True, use_edges=True, use_faces=False)
    bpy.ops.object.mode_set(mode="OBJECT")
    after = len(obj.data.vertices)
    if after != before:
        log("vertices soltos removidos: %d -> %d" % (before, after))


def join_all():
    """
    Junta as malhas numa so.

    O Ofanim vem em 6 pedacos; seis objetos seriam seis chamadas de desenho e seis
    texturas. Juntando, a criatura inteira e um lote so. Os modificadores que sobrevivem
    sao os do objeto ATIVO, entao o ativo tem que ser um que carregue o Armature.
    """
    objs = mesh_objects()
    if not objs:
        raise SystemExit("nenhuma malha no arquivo")
    if len(objs) == 1:
        return objs[0]

    # ativo = o que tem armature (se algum tiver), para o deform sobreviver ao join
    active = next((o for o in objs if any(m.type == "ARMATURE" for m in o.modifiers)), objs[0])

    bpy.ops.object.select_all(action="DESELECT")
    for o in objs:
        o.select_set(True)
    bpy.context.view_layer.objects.active = active
    bpy.ops.object.join()
    log("juntadas %d malhas em uma" % len(objs))
    return bpy.context.view_layer.objects.active


def decimate(obj, budget):
    """
    Decima ANTES de amostrar a animacao, e na malha BASE.

    Esta ordem nao e detalhe: se o Decimate rodasse depois do Armature, ele recalcularia
    o colapso sobre a malha ja deformada, e a topologia sairia DIFERENTE a cada quadro —
    o vertice 700 do quadro 3 nao seria o vertice 700 do quadro 4, e interpolar entre os
    dois viraria pure. Aplicado na base, com o Armature depois, a topologia fica fixa e
    os pesos dos ossos sao reinterpolados junto pelo proprio Blender.
    """
    tris = sum(len(p.vertices) - 2 for p in obj.data.polygons)
    log("triangulos originais: %d" % tris)
    if tris <= budget:
        log("ja cabe no orcamento, sem decimar")
        return

    bpy.context.view_layer.objects.active = obj
    mod = obj.modifiers.new("rec_decimate", "DECIMATE")
    mod.decimate_type = "COLLAPSE"
    mod.ratio = budget / float(tris)
    mod.use_collapse_triangulate = True
    # primeiro da pilha: tem que rodar antes do Armature
    bpy.ops.object.modifier_move_to_index(modifier=mod.name, index=0)
    bpy.ops.object.modifier_apply(modifier=mod.name)

    tris2 = sum(len(p.vertices) - 2 for p in obj.data.polygons)
    log("triangulos depois da decimacao: %d (alvo %d)" % (tris2, budget))


def frame_range():
    """Onde a animacao comeca e acaba — pelas acoes, nao pelo intervalo da cena."""
    lo, hi = None, None
    for act in bpy.data.actions:
        s, e = act.frame_range
        lo = s if lo is None else min(lo, s)
        hi = e if hi is None else max(hi, e)
    if lo is None:
        sc = bpy.context.scene
        return sc.frame_start, sc.frame_start
    return int(lo), int(hi)


def materials_manifest(obj):
    """
    Que imagem cada material usa. So a cor base — o resto do PBR (normal, roughness,
    metallic) o Minecraft nao consome no caminho de entidade, entao seria peso morto
    no jar.
    """
    mats = []
    for slot in obj.data.materials:
        img = None
        # Cor chapada de reserva: nem todo material tem textura (o Ofanim tem dois que
        # sao so uma cor). Sem isto eles sairiam como buraco branco no atlas.
        rgba = [0.8, 0.8, 0.8, 1.0]
        if slot and slot.use_nodes:
            for node in slot.node_tree.nodes:
                if node.type == "BSDF_PRINCIPLED":
                    inp = node.inputs.get("Base Color")
                    if inp is not None and not inp.is_linked:
                        rgba = list(inp.default_value)
                    break
        if slot and slot.use_nodes:
            # a imagem ligada ao Base Color do Principled, e nao a primeira que aparecer:
            # estes assets tem 4 ou 5 texturas por material e a primeira costuma ser o
            # normal map, que assado como cor deixa a criatura azul-lavanda.
            for node in slot.node_tree.nodes:
                if node.type != "BSDF_PRINCIPLED":
                    continue
                inp = node.inputs.get("Base Color")
                if inp and inp.is_linked:
                    src = inp.links[0].from_node
                    while src.type != "TEX_IMAGE":
                        nxt = None
                        for i in src.inputs:
                            if i.is_linked:
                                nxt = i.links[0].from_node
                                break
                        if nxt is None:
                            break
                        src = nxt
                    if src.type == "TEX_IMAGE" and src.image:
                        img = src.image
                break
            if img is None:  # sem Principled ligado: aceita qualquer imagem do material
                for node in slot.node_tree.nodes:
                    if node.type == "TEX_IMAGE" and node.image:
                        img = node.image
                        break

        path = ""
        if img:
            path = bpy.path.abspath(img.filepath_from_user()) if img.filepath_raw else ""
        mats.append({"name": slot.name if slot else "", "image": path, "color": rgba})
        log("  material %-24s -> %s" % (slot.name if slot else "?", os.path.basename(path) or "SEM IMAGEM"))
    return mats


def sample(obj, frames, y_up=False):
    """
    A parte que interessa: le a malha DEFORMADA em cada quadro.

    `evaluated_get(depsgraph)` devolve o objeto com todos os modificadores ja rodados —
    inclusive o Armature. Ou seja, o esqueleto e resolvido aqui e some do resultado: o
    que sai e so um monte de vertice em posicao. E exatamente o que falta para a malha
    caber no Minecraft.
    """
    dg = bpy.context.evaluated_depsgraph_get()
    positions, normals = [], []
    static = None

    for i, f in enumerate(frames):
        bpy.context.scene.frame_set(f)
        dg = bpy.context.evaluated_depsgraph_get()
        eo = obj.evaluated_get(dg)
        me = eo.to_mesh()
        mw = eo.matrix_world

        n = len(me.vertices)
        co = np.empty(n * 3, dtype=np.float64)
        me.vertices.foreach_get("co", co)
        co = co.reshape(n, 3)
        # para o espaco do mundo (o objeto pode ter escala/rotacao propria)
        m = np.array(mw.to_4x4())
        co = co @ m[:3, :3].T + m[:3, 3]

        no = np.empty(n * 3, dtype=np.float64)
        me.vertices.foreach_get("normal", no)
        no = no.reshape(n, 3) @ m[:3, :3].T

        # Blender e Z para cima; Minecraft e Y para cima. Quando o FBX ja veio com Y para
        # cima (modelo de Maya que o importador nao converteu), o mapeamento e a
        # identidade — o eixo comprido dele ja esta onde o Minecraft espera.
        if y_up:
            co = np.stack([co[:, 0], co[:, 1], co[:, 2]], axis=1)
            no = np.stack([no[:, 0], no[:, 1], no[:, 2]], axis=1)
        else:
            co = np.stack([co[:, 0], co[:, 2], -co[:, 1]], axis=1)
            no = np.stack([no[:, 0], no[:, 2], -no[:, 1]], axis=1)

        positions.append(co.astype(np.float32))
        normals.append(no.astype(np.float32))

        if static is None:
            static = topology(me)
            log("  vertices: %d   triangulos: %d" % (n, len(static["tri_loops"]) ))

        eo.to_mesh_clear()

    return np.array(positions), np.array(normals), static


def topology(me):
    """
    A topologia sai UMA vez, do primeiro quadro: quem liga em quem, e o UV de cada canto.

    Vem do canto (loop) e nao do vertice porque a costura de UV divide o vertice: o mesmo
    ponto do corpo tem dois UVs de lados diferentes da ilha. A posicao continua indexada
    pelo vertice de malha — que e o que se move — e o canto so aponta para ela.
    """
    me.calc_loop_triangles()

    uv_layer = me.uv_layers.active
    nloops = len(me.loops)
    uvs = np.zeros((nloops, 2), dtype=np.float32)
    if uv_layer:
        flat = np.empty(nloops * 2, dtype=np.float64)
        uv_layer.data.foreach_get("uv", flat)
        uvs = flat.reshape(nloops, 2).astype(np.float32)
    else:
        log("  AVISO: malha sem UV")

    tri_loops = np.array([lt.loops[:] for lt in me.loop_triangles], dtype=np.int32)
    tri_verts = np.array([lt.vertices[:] for lt in me.loop_triangles], dtype=np.int32)
    tri_mat = np.array([lt.material_index for lt in me.loop_triangles], dtype=np.int32)

    return {"uvs": uvs, "tri_loops": tri_loops, "tri_verts": tri_verts, "tri_mat": tri_mat}


def deroot(positions):
    """
    Tira o DESLOCAMENTO da animacao, deixando o passo no lugar.

    Uma caminhada de verdade anda: o corpo viaja para frente ao longo dos quadros. Isso
    presta no Blender e nao presta aqui, porque quem move a criatura no mundo e o jogo —
    a entidade anda pelo seu proprio caminho e a malha e desenhada em cima dela. Com o
    deslocamento embutido, a malha escorrega para fora da propria caixa de colisao e volta
    com um tranco no fim do laco. Na void-creature isso dava 83% da altura dela: quase
    cinco blocos de deriva e um solavanco por volta.

    Tiro a TENDENCIA (a reta ajustada ao centro de massa), e nao o centro de massa quadro
    a quadro. A diferenca importa: o centro oscila de verdade quando o peso passa de uma
    perna para a outra, e apagar isso deixaria a caminhada dura. A reta leva embora so a
    viagem. De brinde, o laco fecha melhor — o ultimo quadro passa a terminar onde o
    primeiro comeca.
    """
    n = positions.shape[0]
    if n < 3:
        return positions

    centre = positions.mean(axis=1)
    f = np.arange(n, dtype=np.float64)
    out = positions.copy()
    moved = 0.0

    for axis in range(3):
        slope, intercept = np.polyfit(f, centre[:, axis], 1)
        trend = slope * f            # ancorada no quadro 0: trend[0] = 0
        out[:, :, axis] -= trend[:, None]
        moved += (slope * (n - 1)) ** 2

    if moved > 0:
        log("deriva removida da animacao: %.4f (nas unidades do modelo)" % np.sqrt(moved))
    return out


def ground_of(rest, by_mass):
    """
    Onde ficam os PES, que nem sempre e onde a malha acaba.

    Varro a altura de baixo para cima contando quantos vertices ha em cada faixa e paro na
    primeira que tem massa de verdade. Um apendice pendurado (cauda, corrente, dedo) e um
    fio: contribui com dois ou tres vertices por faixa. Os pes chegam todos juntos, e a
    contagem salta. Essa e a linha do chao.
    """
    y = rest[:, 1]
    lo, hi = float(y.min()), float(y.max())
    if not by_mass or hi <= lo:
        return lo

    # O criterio e quantas DIRECOES o corpo ocupa naquela altura.
    #
    # Tentei antes por massa e por largura, e os dois erraram pelo mesmo motivo: a cauda
    # desta criatura varre para o lado enquanto desce, entao no ponto mais baixo ela tem
    # muitos vertices (as vertebras sao densas) E esta longe do centro (parece larga). O
    # que ela nao consegue e estar em varios lados ao mesmo tempo. As oito pernas
    # conseguem. Entao fatio a altura, olho de que angulos em volta do corpo ha malha, e
    # o chao e a primeira faixa em que a criatura se apoia em varias direcoes.
    bands, sectors = 60, 12
    centre = rest[:, [0, 2]].mean(axis=0)
    idx = np.clip(((y - lo) / (hi - lo) * bands).astype(int), 0, bands - 1)

    ang = np.arctan2(rest[:, 2] - centre[1], rest[:, 0] - centre[0])
    sec = ((ang + np.pi) / (2 * np.pi) * sectors).astype(int) % sectors

    ground = lo
    for b in range(bands):
        m = idx == b
        if m.sum() < 8:
            continue
        if len(np.unique(sec[m])) >= 4:
            ground = lo + (hi - lo) * b / bands
            break

    log("chao pelas direcoes de apoio: %.1f%% da altura acima do ponto mais baixo"
        % (100.0 * (ground - lo) / (hi - lo)))
    return ground


def normalize(positions, by_mass=False):
    """
    Poe a criatura em escala de bloco: altura 1.0, pes no zero, centrada no proprio eixo.

    Assim o tamanho de verdade continua sendo decidido no Java (AnomalyType.height()), que
    e onde o Pedro mexe. Reassar so para mudar altura seria absurdo.

    A caixa e a do PRIMEIRO quadro, nao a de todos: se a animacao levanta um braco, a caixa
    de todos os quadros cresceria e a criatura encolheria para caber — o passo da animacao
    mudaria a altura dela. Com a caixa do quadro 1, a pose de repouso e que manda.
    """
    rest = positions[0]
    lo, hi = rest.min(axis=0), rest.max(axis=0)
    ground = ground_of(rest, by_mass)
    height = float(hi[1] - ground)
    if height <= 0:
        raise SystemExit("altura zero — modelo degenerado")

    scale = 1.0 / height
    cx = (lo[0] + hi[0]) * 0.5
    cz = (lo[2] + hi[2]) * 0.5
    offset = np.array([cx, ground, cz], dtype=np.float32)

    out = (positions - offset) * scale
    log("altura original %.3f -> escala %.5f" % (height, scale))
    return out.astype(np.float32)


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    if not argv:
        raise SystemExit("uso: blender -b -P tools/bake_mesh.py -- <apelido>")
    name = argv[0]
    if name not in MODELS:
        raise SystemExit("apelido desconhecido: %s (tenho %s)" % (name, ", ".join(MODELS)))

    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(ROOT, MODELS[name])
    if not os.path.exists(path):
        raise SystemExit("nao achei o modelo: " + path)

    log("=== %s ===" % name)
    wipe()
    load(path)

    drop_scenery(name)
    obj = join_all()
    drop_materials(obj, name)
    decimate(obj, TRI_BUDGET)
    clean_loose(obj)

    lo, hi = frame_range()
    if hi > lo:
        # amostra ao longo da animacao inteira, sem repetir o primeiro no ultimo (o
        # renderer fecha o ciclo sozinho voltando do ultimo para o primeiro)
        frames = [int(round(lo + (hi - lo) * i / float(FRAMES))) for i in range(FRAMES)]
    else:
        frames = [lo]
    log("animacao %d..%d, amostrando %d quadros" % (lo, hi, len(frames)))

    mats = materials_manifest(obj)
    positions, normals, static = sample(obj, frames, y_up=name in Y_UP_SOURCE)
    positions = normalize(deroot(positions), by_mass=name in GROUND_BY_MASS)

    raw = os.path.join(OUT, name + "_raw.npz")
    np.savez_compressed(
        raw,
        positions=positions,
        normals=normals,
        uvs=static["uvs"],
        tri_loops=static["tri_loops"],
        tri_verts=static["tri_verts"],
        tri_mat=static["tri_mat"],
    )
    with open(os.path.join(OUT, name + "_mats.json"), "w", encoding="utf-8") as fh:
        json.dump(mats, fh, indent=2)

    log("escrito %s (%.1f MB)" % (raw, os.path.getsize(raw) / 1e6))


main()
