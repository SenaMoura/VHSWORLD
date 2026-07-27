"""
Inventario dos modelos 3D que o Pedro baixou. RODA DENTRO DO BLENDER:

    blender -b -P tools/inspect_models.py

O que a gente precisa saber de cada um, e que nenhuma imagem responde:
  1. quantos TRIANGULOS  -> decide se da para renderizar vivo ou so pre-renderizado
  2. tem ESQUELETO?      -> sem armature nao ha como animar, so poses estaticas
  3. tem ANIMACAO?       -> se ja vem com ciclo de andar, o pre-render sai de graca
  4. tamanho real        -> Sketchfab exporta em escalas malucas (metros vs cm)

Escreve o resultado em tools/models_report.txt, porque a saida do Blender no
terminal vem afogada em log de importacao.
"""

import os
import sys

import bpy

ROOT = r"C:\Users\Hamilton\Downloads\vhsworldentities\3d"
REPORT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models_report.txt")


def find_fbx(folder):
    hits = []
    for base, _, files in os.walk(folder):
        for f in files:
            if f.lower().endswith((".fbx", ".gltf", ".glb", ".obj")):
                hits.append(os.path.join(base, f))
    # o maior costuma ser a malha de verdade; os pequenos sao props soltos
    hits.sort(key=os.path.getsize, reverse=True)
    return hits


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


def describe(name, path, out):
    wipe()
    try:
        load(path)
    except Exception as e:
        out.append(f"{name}: FALHOU ao importar ({e})")
        return

    tris = 0
    meshes = 0
    for obj in bpy.data.objects:
        if obj.type == "MESH":
            meshes += 1
            mesh = obj.data
            mesh.calc_loop_triangles()
            tris += len(mesh.loop_triangles)

    armatures = [o for o in bpy.data.objects if o.type == "ARMATURE"]
    bones = sum(len(a.data.bones) for a in armatures)
    actions = [a.name for a in bpy.data.actions]

    # caixa do mundo inteiro, para saber a escala em que veio
    lo = [1e9, 1e9, 1e9]
    hi = [-1e9, -1e9, -1e9]
    for obj in bpy.data.objects:
        if obj.type != "MESH":
            continue
        for corner in obj.bound_box:
            world = obj.matrix_world @ type(obj.location)(corner)
            for i in range(3):
                lo[i] = min(lo[i], world[i])
                hi[i] = max(hi[i], world[i])
    size = [round(hi[i] - lo[i], 2) for i in range(3)] if meshes else [0, 0, 0]

    frames = ""
    if actions:
        spans = []
        for a in bpy.data.actions:
            start, end = a.frame_range
            spans.append(f"{a.name}({int(start)}-{int(end)})")
        frames = " | acoes: " + ", ".join(spans[:6])

    out.append(
        f"{name}\n"
        f"    arquivo   : {os.path.basename(path)}\n"
        f"    malhas    : {meshes}   triangulos: {tris:,}\n"
        f"    esqueleto : {'SIM (' + str(bones) + ' ossos)' if armatures else 'NAO'}\n"
        f"    animacao  : {'SIM' if actions else 'NAO'}{frames}\n"
        f"    tamanho   : {size} (unidades do arquivo)\n"
    )


def main():
    out = []
    for entry in sorted(os.listdir(ROOT)):
        folder = os.path.join(ROOT, entry)
        if not os.path.isdir(folder):
            continue
        found = find_fbx(folder)
        if not found:
            out.append(f"{entry}: nenhum modelo encontrado (zip aninhado?)\n")
            continue
        describe(entry, found[0], out)

    text = "\n".join(out)
    with open(REPORT, "w", encoding="utf-8") as f:
        f.write(text)
    print("\n\n===== RELATORIO =====\n" + text)


main()
