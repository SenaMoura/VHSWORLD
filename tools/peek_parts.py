"""Lista as pecas de um modelo antes de juntar. blender -b -P tools/peek_parts.py -- greyface"""
import sys, os, bpy

ROOT = r"C:\Users\Hamilton\Downloads\vhsworldentities\3d"
MODELS = {
    "ophanim":  "ophanim-angel/source/opaawmea.fbx",
    "greyface": "greyface/source/inner/greyface.gltf",
    "void": "void-creature/source/KelFinal.fbx",
}

argv = sys.argv[sys.argv.index("--") + 1:]
name = argv[0]
path = os.path.join(ROOT, MODELS[name])

bpy.ops.wm.read_factory_settings(use_empty=True)
if path.lower().endswith(".fbx"):
    bpy.ops.import_scene.fbx(filepath=path)
else:
    bpy.ops.import_scene.gltf(filepath=path)

for o in bpy.data.objects:
    if o.type != "MESH":
        continue
    me = o.data
    tris = sum(len(p.vertices) - 2 for p in me.polygons)
    bb = [o.matrix_world @ __import__("mathutils").Vector(c) for c in o.bound_box]
    xs = [v.x for v in bb]; ys = [v.y for v in bb]; zs = [v.z for v in bb]
    mats = ",".join(m.name for m in me.materials if m)
    print("[peek] %-28s verts=%-7d tris=%-7d tam=[%.2f %.2f %.2f] mats=%s"
          % (o.name, len(me.vertices), tris,
             max(xs)-min(xs), max(ys)-min(ys), max(zs)-min(zs), mats), flush=True)
