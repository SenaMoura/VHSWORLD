"""
Pre-renderiza as criaturas 3D em sprites para o mod. RODA DENTRO DO BLENDER:

    blender -b -P tools/render_creatures.py -- corpser

POR QUE PRE-RENDERIZAR: estas malhas tem de 15 mil a 158 mil triangulos, e o caminho
de render de entidade do Minecraft aguanta alguns milhares. Alem disso o motor nao tem
skinning de malha com ossos — GeckoLib e companhia animam hierarquia de CAIXAS. Entao
a escultura nunca entraria viva. Renderizada aqui, ela entra com o detalhe todo (o
normal map, a luz, a anatomia) por um quadrilatero de custo.

E a tecnica do Doom, e ela cai bem demais neste mod: sprite pre-renderizado parece
filmagem que a camera nao conseguiu focar.

O que sai: uma FOLHA por criatura, colunas = angulos, linhas = quadros da animacao.
"""

import math
import os
import sys

import bpy
from mathutils import Vector

ROOT = r"C:\Users\Hamilton\Downloads\vhsworldentities\3d"
OUT = r"C:\Users\Hamilton\Downloads\vhsworldentities\3d\_sprites"

# quantos angulos em volta da criatura (8 = de 45 em 45 graus, o padrao do Doom)
ANGLES = 8

# quantos quadros da animacao amostrar
FRAMES = 8

# altura de cada quadro em pixels
CELL_H = 192

# caminho do modelo por apelido + de quantos graus girar para ele encarar a camera
MODELS = {
    "corpser":                   ("corpser/source/scream.fbx", 0.0),
    "moldman":                   ("moldman-in-shell/source/moldman.fbx", 0.0),
    "ophanim":                   ("ophanim-angel/source/opaawmea.fbx", 0.0),
    "horror_creature":           ("horror-creature-character/source/maya2sketchfab.fbx", 0.0),
    "greyface":                  ("greyface/source/inner/greyface.gltf", 0.0),
    "burnout":                   ("bulletstorm-burnouts/source/Bulletstorm burnouts.blend", 0.0),
}


def wipe():
    bpy.ops.wm.read_factory_settings(use_empty=True)


def load(path):
    ext = path.lower().rsplit(".", 1)[-1]
    if ext == "fbx":
        bpy.ops.import_scene.fbx(filepath=path)
    elif ext in ("gltf", "glb"):
        bpy.ops.import_scene.gltf(filepath=path)
    elif ext == "blend":
        bpy.ops.wm.open_mainfile(filepath=path)
    else:
        raise RuntimeError("formato desconhecido: " + path)


def find_on_disk(name, folder):
    """
    Acha a textura no disco ignorando pasta e extensao.

    ⚠️ Precisa existir porque os FBX do Sketchfab mentem o caminho: o corpser aponta
    para `source/tex/Geist_Corpser_CP_Tex.JPG` e o arquivo de verdade esta em
    `textures/Geist_Corpser_CP_Tex.jpeg` — pasta diferente E extensao diferente.
    """
    base = os.path.basename(name)
    # ⚠️ Blender numera nome repetido com ".001". Sem tirar isso, o nome-base de
    # "DefaultMaterial_BaseColor.png.001" vira "DefaultMaterial_BaseColor.png" e nunca
    # casa com o arquivo "DefaultMaterial_BaseColor.png" no disco. Foi o que deixou o
    # ophanim todo magenta (magenta = textura faltando).
    while True:
        stem, ext = os.path.splitext(base)
        if len(ext) == 4 and ext[1:].isdigit():
            base = stem
            continue
        base = stem
        break
    base = base.lower()
    for root, _, files in os.walk(folder):
        for f in files:
            if os.path.splitext(f)[0].lower() == base:
                return os.path.join(root, f)
    return None


def normalize(name):
    return "".join(c for c in name.lower() if c.isalnum())


def similarity(a, b):
    """
    Maior pedaco de nome que os dois tem em comum.

    Prefixo nao serve: o material do moldman se chama "MI_MoldmanBody" e a textura
    "T_Moldman_Body_D" — eles nao compartilham nem a primeira letra, mas compartilham
    "moldmanbody", que e o nome da coisa. Substring comum acha isso; prefixo nao.
    """
    a, b = normalize(a), normalize(b)
    best = 0
    for i in range(len(a)):
        for j in range(len(b)):
            n = 0
            while i + n < len(a) and j + n < len(b) and a[i + n] == b[j + n]:
                n += 1
            best = max(best, n)
    return best


def folder_images(folder):
    """Arquivos de imagem do pacote, para quando o modelo nao referencia nenhum."""
    out = []
    for root, _, files in os.walk(folder):
        for f in files:
            if f.lower().endswith((".png", ".jpg", ".jpeg", ".tga", ".bmp")):
                low = f.lower()
                # o mapa de metalico nao serve para nada aqui e alguns tem 30 MB
                if "_m." in low or "metallic" in low:
                    continue
                out.append(os.path.join(root, f))
    return out


def drop_backdrops():
    """
    Joga fora fundo e casca que vem junto no pacote.

    O moldman veio com um "Icosphere" de 42 vertices ENVOLVENDO a criatura — no render
    saiu uma bola branca e a criatura invisivel dentro. Regra: malha quase sem
    geometria mas do tamanho da cena inteira nao e criatura, e cenario.
    """
    lo, hi = world_bounds()
    scene_span = max(hi.x - lo.x, hi.y - lo.y, hi.z - lo.z, 1e-6)

    for obj in list(bpy.data.objects):
        if obj.type != "MESH" or len(obj.data.vertices) > 200:
            continue
        corners = [obj.matrix_world @ Vector(c) for c in obj.bound_box]
        span = max(max(c[i] for c in corners) - min(c[i] for c in corners) for i in range(3))
        if span >= scene_span * 0.7:
            print(f"@@ descartado cenario: '{obj.name}' ({len(obj.data.vertices)} vertices)")
            bpy.data.objects.remove(obj, do_unlink=True)


ROLES = {
    "base":   ("_tex", "basecolor", "diffuse", "albedo", "_c.", "_d."),
    "normal": ("_normal", "_nor", "_n."),
    "rough":  ("_specular", "roughness", "_rough", "_s.", "_r."),
    "emit":   ("_glow", "emissive", "emission"),
}


def rebuild_materials(folder):
    """
    Liga as texturas nos materiais na mao.

    ⚠️ TAMBEM nao e opcional. O importador de FBX traz as imagens como dados soltos e
    NAO monta os nos quando o material original era de um shader proprietario (Maya,
    C4D). O resultado e um Principled cinza 0.8 sem textura nenhuma — que, com pouca
    luz, renderiza preto e parece problema de iluminacao. Foi o que me custou uma
    rodada aqui.

    Casa imagem com material pelo maior prefixo comum do nome, que e a convencao que
    todos estes pacotes seguem (Geist_Corpser_CP_Mat <- Geist_Corpser_CP_Tex).
    """
    # conserta os caminhos primeiro
    for img in bpy.data.images:
        if img.has_data:
            continue
        found = find_on_disk(img.name, folder)
        if found:
            img.filepath = found
            try:
                img.reload()
            except Exception:
                pass

    on_disk = folder_images(folder)
    materials = [m for m in bpy.data.materials
                 if m.use_nodes and any(n.type == "BSDF_PRINCIPLED" for n in m.node_tree.nodes)]
    single = len(materials) <= 1

    for mat in bpy.data.materials:
        if not mat.use_nodes:
            mat.use_nodes = True
        nodes = mat.node_tree.nodes
        links = mat.node_tree.links

        bsdf = next((n for n in nodes if n.type == "BSDF_PRINCIPLED"), None)
        if bsdf is None:
            continue
        if any(n.type == "TEX_IMAGE" for n in nodes):
            continue      # ja veio montado (gltf costuma vir)

        picked = {}
        for role, marks in ROLES.items():
            best, best_score = None, -1

            # 1a opcao: imagem que o proprio arquivo trouxe
            for img in bpy.data.images:
                if not any(m in img.name.lower() for m in marks):
                    continue
                score = similarity(mat.name, img.name)
                if score > best_score:
                    best, best_score = img, score

            # 2a opcao: arquivo solto na pasta do pacote
            for path_img in on_disk:
                name = os.path.basename(path_img)
                if not any(m in name.lower() for m in marks):
                    continue
                score = similarity(mat.name, name)
                if score > best_score:
                    best, best_score = path_img, score

            # Com UM material so nao ha com o que confundir: o papel basta, e o nome
            # nao precisa casar. E o caso do horror_creature, cujas imagens o Maya
            # exportou como "file1".."file4" — nome nenhum casaria com nada.
            if best is not None and (best_score >= 4 or single):
                picked[role] = (bpy.data.images.load(best, check_existing=True)
                                if isinstance(best, str) else best)

        def tex(img, non_color=False):
            node = nodes.new("ShaderNodeTexImage")
            node.image = img
            if non_color:
                node.image.colorspace_settings.name = "Non-Color"
            return node

        if "base" in picked:
            links.new(tex(picked["base"]).outputs["Color"], bsdf.inputs["Base Color"])
        if "normal" in picked:
            nmap = nodes.new("ShaderNodeNormalMap")
            links.new(tex(picked["normal"], True).outputs["Color"], nmap.inputs["Color"])
            links.new(nmap.outputs["Normal"], bsdf.inputs["Normal"])
        if "rough" in picked:
            links.new(tex(picked["rough"], True).outputs["Color"], bsdf.inputs["Roughness"])
        if "emit" in picked and "Emission Color" in bsdf.inputs:
            links.new(tex(picked["emit"]).outputs["Color"], bsdf.inputs["Emission Color"])
            bsdf.inputs["Emission Strength"].default_value = 1.0

        print(f"@@ material {mat.name}: ligadas {sorted(picked)}")


def stand_up():
    """
    Poe a criatura de pe.

    ⚠️ ISTO NAO E OPCIONAL. Varios destes FBX vem de Maya/C4D em Y-up e o importador
    nao converte — o corpser chegou com a caixa [10.87, 20.64, 10.65], ou seja o maior
    eixo era o Y: ele estava DEITADO no mundo Z-up do Blender. O sintoma nao foi um
    erro, foi pior: a camera enquadrou pela largura, as luzes foram para o eixo errado
    e sairam oito silhuetas pretas que pareciam problema de material.

    Regra: se o vao em Y for claramente maior que o vao em Z, gira o conjunto -90 em X.
    """
    lo, hi = world_bounds()
    if (hi.y - lo.y) <= (hi.z - lo.z) * 1.3:
        return

    for obj in bpy.data.objects:
        if obj.parent is None:
            obj.rotation_euler.x += math.radians(-90.0)
    bpy.context.view_layer.update()
    print("@@ modelo estava deitado (Y-up): girado para ficar de pe")


def world_bounds():
    lo = Vector((1e9, 1e9, 1e9))
    hi = Vector((-1e9, -1e9, -1e9))
    for obj in bpy.data.objects:
        if obj.type != "MESH" or not obj.visible_get():
            continue
        for corner in obj.bound_box:
            p = obj.matrix_world @ Vector(corner)
            for i in range(3):
                lo[i] = min(lo[i], p[i])
                hi[i] = max(hi[i], p[i])
    return lo, hi


def setup_world():
    """
    Fundo transparente e luz pensada para o jogo, nao para portfolio.

    A luz e quase de cima e bem difusa DE PROPOSITO: o sprite nao responde a luz do
    mundo do Minecraft, entao qualquer luz lateral marcada brigaria com a iluminacao
    real da cena e denunciaria que aquilo e um cartaz. Luz de cima combina com
    qualquer lugar.
    """
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_EEVEE_NEXT"
    scene.render.film_transparent = True
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"

    # ⚠️ O padrao do Blender 4.2 e AgX, que lava e dessatura para parecer cinema.
    # Num sprite que vai ser visto a 100 pixels, isso vira uma mancha clara sem
    # contraste. "Standard" entrega a cor do material como ela e.
    scene.view_settings.view_transform = "Standard"
    scene.view_settings.look = "None"

    world = bpy.data.worlds.new("rec")
    scene.world = world
    world.use_nodes = True
    world.node_tree.nodes["Background"].inputs[1].default_value = 0.12   # ambiente: sem ele, o lado escuro some por completo

    key = bpy.data.lights.new("key", type="AREA")
    key.energy = 1000.0        # ajustado a escala em render_one
    key.size = 12.0
    key_obj = bpy.data.objects.new("key", key)
    scene.collection.objects.link(key_obj)

    fill = bpy.data.lights.new("fill", type="AREA")
    fill.energy = 220.0
    fill.size = 16.0
    fill_obj = bpy.data.objects.new("fill", fill)
    scene.collection.objects.link(fill_obj)

    return key_obj, fill_obj


def place_camera(center, height):
    """
    Camera ORTOGRAFICA, e nao em perspectiva.

    Num billboard, perspectiva e veneno: o quadro foi tirado a uma distancia so, e
    quando o jogo desenha a criatura perto ou longe a fuga das linhas nao acompanha.
    Ortografica nao tem ponto de fuga, entao a mesma imagem serve a qualquer distancia.
    """
    cam_data = bpy.data.cameras.new("cam")
    cam_data.type = "ORTHO"
    cam_data.ortho_scale = height * 1.15      # uma folga em volta
    cam = bpy.data.objects.new("cam", cam_data)
    bpy.context.scene.collection.objects.link(cam)
    bpy.context.scene.camera = cam
    return cam


def aim(obj, target):
    direction = (target - obj.location)
    obj.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()


def render_one(nick, rel_path, spin, angles=ANGLES, frames=FRAMES):
    path = os.path.join(ROOT, rel_path.replace("/", os.sep))
    wipe()
    load(path)

    key, fill = setup_world()
    rebuild_materials(os.path.dirname(os.path.dirname(path)))
    drop_backdrops()
    stand_up()

    lo, hi = world_bounds()
    center = (lo + hi) * 0.5
    height = max(hi.z - lo.z, 1e-3)
    span = max(hi.x - lo.x, hi.y - lo.y, height)
    print(f"@@ caixa: {[round(hi[i]-lo[i], 2) for i in range(3)]}")

    cam = place_camera(center, span)

    scene = bpy.context.scene
    # O intervalo vem da ACAO, e nao do frame_start/end da cena: o padrao do Blender
    # e 1-250 e a animacao do corpser vai so ate 85, entao mais da metade dos quadros
    # amostrados cairiam num trecho parado, repetindo a mesma pose.
    if bpy.data.actions:
        start = int(min(a.frame_range[0] for a in bpy.data.actions))
        end = int(max(a.frame_range[1] for a in bpy.data.actions))
    else:
        start, end = 1, 1
    has_anim = end > start
    if not has_anim:
        frames = 1

    # Quadrado o suficiente para caber criatura larga (a aranha ocupa quase 3x a
    # largura de um humano); o recorte final acontece depois, no empacotador.
    scene.render.resolution_y = CELL_H
    scene.render.resolution_x = CELL_H
    scene.render.resolution_percentage = 100

    radius = span * 2.2

    # ⚠️ ESCALA DA LUZ. Estes modelos vem em unidades malucas (o corpser tem 20 de
    # altura, o moldman 2). Watt e potencia absoluta: a mesma lampada a 45 unidades
    # entrega 1% do que entrega a 4.5. Sem amarrar a energia ao raio, modelo grande
    # sai preto e modelo pequeno sai estourado.
    key.data.energy = 55.0 * radius * radius
    fill.data.energy = 14.0 * radius * radius
    key.data.size = span * 0.8
    fill.data.size = span * 1.2
    folder = os.path.join(OUT, nick)
    os.makedirs(folder, exist_ok=True)

    for f in range(frames):
        if has_anim:
            scene.frame_set(int(start + (end - start) * f / max(1, frames)))

        for a in range(angles):
            theta = math.radians(spin + 360.0 * a / angles)

            cam.location = center + Vector((math.cos(theta) * radius,
                                            math.sin(theta) * radius,
                                            0.0))
            aim(cam, center)

            # As luzes acompanham a camera para que o lado iluminado seja sempre o
            # lado que o jogador ve — de novo: o sprite nao sabe onde fica o sol.
            key.location = center + Vector((math.cos(theta + 0.6) * radius,
                                            math.sin(theta + 0.6) * radius,
                                            height * 1.6))
            aim(key, center)
            fill.location = center + Vector((math.cos(theta - 1.2) * radius,
                                             math.sin(theta - 1.2) * radius,
                                             height * 0.2))
            aim(fill, center)

            scene.render.filepath = os.path.join(folder, f"f{f:02d}_a{a:02d}.png")
            bpy.ops.render.render(write_still=True)

    print(f"@@ {nick}: {frames} quadros x {angles} angulos -> {folder}")


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    wanted = argv if argv else list(MODELS)

    for nick in wanted:
        if nick not in MODELS:
            print("@@ desconhecido:", nick)
            continue
        rel, spin = MODELS[nick]
        render_one(nick, rel, spin)


main()
