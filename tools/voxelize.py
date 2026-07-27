"""
Voxeliza uma escultura 3D e mede se ela caberia no Minecraft. RODA NO BLENDER:

    blender -b -P tools/voxelize.py -- corpser 32

A pergunta que este script responde nao e "fica bonito?" — e "CABE?". Um modelo de
entidade do Minecraft e uma lista de CAIXAS, e cada caixa custa 6 faces. Modelo de mob
do vanilla tem dezenas de caixas; um modelo de mod caprichado tem algumas centenas. Se
a voxelizacao de uma escultura der milhares, ela nao entra por mais bonita que seja.

Por isso aqui tem duas contas: quantos voxels a casca tem, e quantas caixas sobram
depois de FUNDIR voxels vizinhos em blocos maiores (greedy meshing). A segunda e a que
vale, e costuma ser varias vezes menor.

Escreve tambem o resultado como malha no Blender, para renderizar e comparar com o
pre-render.
"""

import os
import sys
from collections import defaultdict

import bpy
import bmesh
from mathutils import Vector

sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from render_creatures import (MODELS, ROOT, load, rebuild_materials, stand_up,
                              world_bounds, setup_world, place_camera, aim)

OUT = r"C:\Users\Hamilton\Downloads\vhsworldentities\3d\_sprites"


def sample_texture(mat):
    """A imagem de cor base do material, ja carregada, ou None."""
    if not mat or not mat.use_nodes:
        return None
    for node in mat.node_tree.nodes:
        if node.type == "BSDF_PRINCIPLED":
            link = node.inputs["Base Color"].links
            if link and link[0].from_node.type == "TEX_IMAGE":
                img = link[0].from_node.image
                if img and img.has_data:
                    return img
    return None


def pixel(img, u, v):
    if img is None:
        return (0.5, 0.5, 0.5)
    w, h = img.size
    x = int((u % 1.0) * (w - 1))
    y = int((v % 1.0) * (h - 1))
    i = (y * w + x) * img.channels
    px = img.pixels[i:i + 3]
    return (px[0], px[1], px[2]) if len(px) == 3 else (0.5, 0.5, 0.5)


def voxelize(resolution):
    """
    Casca de voxels a partir dos VERTICES da malha.

    Jogar os vertices numa grade e o caminho mais direto: estas esculturas tem dezenas
    de milhares de vertices bem distribuidos, entao a casca sai continua sem eu precisar
    de teste de "dentro/fora" por raio — que e caro e falha justamente nas partes finas
    (garra, dedo, antena), que sao as que dao a silhueta destas criaturas.
    """
    lo, hi = world_bounds()
    span = max(hi.x - lo.x, hi.y - lo.y, hi.z - lo.z)
    step = span / resolution

    cells = {}
    for obj in bpy.data.objects:
        if obj.type != "MESH":
            continue

        mesh = obj.data
        img = sample_texture(obj.active_material)
        uv_layer = mesh.uv_layers.active

        # UV por vertice (a media das faces que o tocam ja basta para pegar a cor)
        uv_of = {}
        if uv_layer:
            for loop in mesh.loops:
                uv_of.setdefault(loop.vertex_index, uv_layer.data[loop.index].uv)

        for vert in mesh.vertices:
            p = obj.matrix_world @ vert.co
            key = (int((p.x - lo.x) / step),
                   int((p.y - lo.y) / step),
                   int((p.z - lo.z) / step))

            if key in cells:
                continue
            uv = uv_of.get(vert.index)
            cells[key] = pixel(img, uv[0], uv[1]) if uv else (0.5, 0.5, 0.5)

    return cells, lo, step


def greedy_boxes(cells):
    """
    Funde voxels vizinhos de cor parecida em caixas maiores.

    E o que decide a viabilidade: 3000 voxels soltos seriam 3000 caixas e nenhum modelo
    de entidade aguenta isso. Fundindo em X, depois em Y, depois em Z, um torso liso
    vira UMA caixa. Guloso e nao otimo — otimo e NP-dificil e nao vale o esforco aqui.
    """
    def bucket(c):
        return (round(c[0] * 6), round(c[1] * 6), round(c[2] * 6))

    remaining = dict(cells)
    boxes = []

    for key in sorted(remaining):
        if key not in remaining:
            continue
        color = remaining.pop(key)
        tag = bucket(color)
        x, y, z = key

        # cresce em X
        w = 1
        while (x + w, y, z) in remaining and bucket(remaining[(x + w, y, z)]) == tag:
            remaining.pop((x + w, y, z))
            w += 1

        # cresce em Y, so se a fileira inteira acompanhar
        d = 1
        while True:
            row = [(x + i, y + d, z) for i in range(w)]
            if all(c in remaining and bucket(remaining[c]) == tag for c in row):
                for c in row:
                    remaining.pop(c)
                d += 1
            else:
                break

        # cresce em Z, so se a laje inteira acompanhar
        t = 1
        while True:
            slab = [(x + i, y + j, z + t) for i in range(w) for j in range(d)]
            if all(c in remaining and bucket(remaining[c]) == tag for c in slab):
                for c in slab:
                    remaining.pop(c)
                t += 1
            else:
                break

        boxes.append((x, y, z, w, d, t, color))

    return boxes


def build_mesh(boxes, lo, step, name):
    mesh = bpy.data.meshes.new(name)
    bm = bmesh.new()
    layer = bm.loops.layers.color.new("Col")

    for (x, y, z, w, d, t, color) in boxes:
        origin = Vector((lo.x + x * step, lo.y + y * step, lo.z + z * step))
        size = Vector((w * step, d * step, t * step))

        start = len(bm.verts)
        bmesh.ops.create_cube(bm, size=1.0)
        bm.verts.ensure_lookup_table()
        for v in bm.verts[start:]:
            v.co.x = origin.x + (v.co.x + 0.5) * size.x
            v.co.y = origin.y + (v.co.y + 0.5) * size.y
            v.co.z = origin.z + (v.co.z + 0.5) * size.z

        # A cor vai nas 6 faces recem-criadas. Pintar agora, e nao no fim, evita ter
        # de descobrir depois qual face pertence a qual caixa.
        bm.faces.ensure_lookup_table()
        for face in bm.faces[-6:]:
            for loop in face.loops:
                loop[layer] = (color[0], color[1], color[2], 1.0)

    bm.to_mesh(mesh)
    bm.free()

    # material que le a cor por vertice, senao tudo sai cinza padrao
    mat = bpy.data.materials.new(name + "_mat")
    mat.use_nodes = True
    nodes, links = mat.node_tree.nodes, mat.node_tree.links
    bsdf = nodes["Principled BSDF"]
    attr = nodes.new("ShaderNodeVertexColor")
    attr.layer_name = "Col"
    links.new(attr.outputs["Color"], bsdf.inputs["Base Color"])
    bsdf.inputs["Roughness"].default_value = 0.85
    mesh.materials.append(mat)

    obj = bpy.data.objects.new(name, mesh)
    bpy.context.scene.collection.objects.link(obj)
    return obj


def render_compare(nick, resolution):
    """Renderiza a versao voxel nos mesmos angulos do pre-render, para comparar."""
    key, fill = setup_world()

    lo, hi = world_bounds()
    center = (lo + hi) * 0.5
    span = max(hi.x - lo.x, hi.y - lo.y, hi.z - lo.z)
    cam = place_camera(center, span)

    radius = span * 2.2
    key.data.energy = 55.0 * radius * radius
    fill.data.energy = 14.0 * radius * radius
    key.data.size = span * 0.8
    fill.data.size = span * 1.2

    scene = bpy.context.scene
    scene.render.resolution_x = 192
    scene.render.resolution_y = 192

    import math
    folder = os.path.join(OUT, f"{nick}_voxel{resolution}")
    os.makedirs(folder, exist_ok=True)
    for a in range(4):
        theta = math.radians(90.0 * a)
        cam.location = center + Vector((math.cos(theta) * radius, math.sin(theta) * radius, 0.0))
        aim(cam, center)
        key.location = center + Vector((math.cos(theta + 0.6) * radius,
                                        math.sin(theta + 0.6) * radius, span * 1.6))
        aim(key, center)
        fill.location = center + Vector((math.cos(theta - 1.2) * radius,
                                         math.sin(theta - 1.2) * radius, span * 0.2))
        aim(fill, center)
        scene.render.filepath = os.path.join(folder, f"a{a:02d}.png")
        bpy.ops.render.render(write_still=True)
    print(f"@@ render voxel -> {folder}")


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    nick = argv[0] if argv else "corpser"
    resolution = int(argv[1]) if len(argv) > 1 else 32

    rel, spin = MODELS[nick]
    path = os.path.join(ROOT, rel.replace("/", os.sep))

    bpy.ops.wm.read_factory_settings(use_empty=True)
    load(path)
    rebuild_materials(os.path.dirname(os.path.dirname(path)))
    stand_up()

    cells, lo, step = voxelize(resolution)
    boxes = greedy_boxes(cells)

    print(f"@@ {nick} @ {resolution}: {len(cells)} voxels -> {len(boxes)} caixas "
          f"(reducao {len(cells)/max(1,len(boxes)):.1f}x)")

    # tira a escultura de cena e deixa so os voxels, para renderizar a comparacao
    for obj in list(bpy.data.objects):
        if obj.type == "MESH":
            bpy.data.objects.remove(obj, do_unlink=True)

    build_mesh(boxes, lo, step, f"{nick}_voxel")
    render_compare(nick, resolution)


main()
