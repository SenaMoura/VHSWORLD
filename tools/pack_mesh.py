"""
Fecha o que o Blender assou: monta o atlas de textura e escreve o `.mesh` do jar.

    python tools/pack_mesh.py ophanim greyface

Roda no Python do sistema (nao no do Blender) porque quem trabalha aqui e o PIL.

O ATLAS. A escultura chega com um material por parte — corpo, metal, bolsa, olho — cada
um com sua imagem de 4K. Seis imagens seriam seis trocas de textura para desenhar uma
criatura, e o caminho de entidade do Minecraft quer UMA textura por lote. Entao as
imagens viram celulas de uma folha so, e o UV de cada canto e reescrito para cair na
celula certa. 512 por celula: a 4K morre no grao de VHS de qualquer jeito.

O FORMATO. Tudo em big-endian, que e o que o DataInputStream do Java le sem ginastica:

    "RECM"          4 bytes
    versao          u8
    quadros         u8
    vertices        u16      pontos que se movem
    cantos          u32      = triangulos * 3
    qmin[3] qmax[3] float32  a caixa da quantizacao
    cantos:         para cada um -> vertice u16, u float32, v float32
    quadros:        para cada quadro, para cada vertice -> x,y,z u16 + nx,ny,nz i8

POR QUE u16 NA POSICAO: float32 dobraria o arquivo por um detalhe que ninguem ve. A
criatura tem 1 de altura nesta escala, entao 65.536 passos dao resolucao de ~0.00002 de
bloco — mais fino que um pixel de textura a queima-roupa.
"""

import json
import os
import struct
import sys

import numpy as np
from PIL import Image

MESH_IN = r"C:\Users\Hamilton\Downloads\vhsworldentities\3d\_mesh"
MODEL_ROOT = r"C:\Users\Hamilton\Downloads\vhsworldentities\3d"
ASSETS = r"C:\Users\Hamilton\Downloads\GitHub\VHSWORLD\src\main\resources\assets\recmod"

# onde procurar a textura de cada criatura quando o caminho gravado no modelo nao presta
MODEL_DIR = {
    "ophanim":  "ophanim-angel",
    "greyface": "greyface",
}

CELL = 512
VERSION = 1


def log(msg):
    print("[pack] " + msg, flush=True)


def _key(filename):
    """Nome comparavel: sem caixa, sem espaco, sem sublinhado."""
    stem = os.path.splitext(os.path.basename(filename))[0]
    return stem.lower().replace(" ", "").replace("_", "").replace("-", "")


def resolve(path, name):
    """
    Acha a textura de verdade no disco.

    O FBX do Ofanim guarda o caminho ABSOLUTO da maquina de quem modelou —
    "D:\\Downloads\\eye ball new textgure\\...". Esse disco nao existe aqui, entao todo
    material caia em cor chapada e a criatura sairia cinza lisa. As imagens estao na
    pasta do proprio modelo o tempo todo, so que renomeadas (espaco virou sublinhado
    quando o pacote foi zipado). Por isso a busca compara o nome NORMALIZADO, e nao o
    caminho: "eyeball Base Color.png" e "eyeball_Base_Color.png" sao a mesma imagem.

    A busca fica presa a pasta da criatura de proposito — solta, o "DefaultMaterial_
    BaseColor.png" do Ofanim casaria com o de qualquer outro modelo.
    """
    if path and os.path.exists(path):
        return path
    if not path:
        return ""

    root = os.path.join(MODEL_ROOT, MODEL_DIR.get(name, ""))
    want = _key(path)
    for dirpath, _, files in os.walk(root):
        for f in files:
            if f.lower().endswith((".png", ".jpg", ".jpeg", ".tga")) and _key(f) == want:
                found = os.path.join(dirpath, f)
                log("  resgatada: %s -> %s" % (os.path.basename(path), os.path.relpath(found, root)))
                return found
    log("  NAO ACHEI a textura %s" % os.path.basename(path))
    return ""


def build_atlas(mats, out_png, name):
    """
    Uma imagem por material vira uma celula. Devolve (colunas, linhas) para o remapeamento.

    Material sem imagem vira celula de cor chapada — o Ofanim tem dois assim. Antes de eu
    guardar a cor no manifesto eles sairiam como buraco branco no meio da criatura.
    """
    n = len(mats)
    cols = int(np.ceil(np.sqrt(n)))
    rows = int(np.ceil(n / cols))
    atlas = Image.new("RGBA", (cols * CELL, rows * CELL), (0, 0, 0, 255))

    for i, mat in enumerate(mats):
        cx, cy = (i % cols) * CELL, (i // cols) * CELL
        path = resolve(mat.get("image") or "", name)
        if path and os.path.exists(path):
            img = Image.open(path).convert("RGBA").resize((CELL, CELL), Image.LANCZOS)
            log("  celula %d: %s" % (i, os.path.basename(path)))
        else:
            rgba = mat.get("color") or [0.8, 0.8, 0.8, 1.0]
            # o Blender guarda cor em linear; a tela quer sRGB
            srgb = tuple(int(round(255 * (c ** (1 / 2.2)))) for c in rgba[:3]) + (255,)
            img = Image.new("RGBA", (CELL, CELL), srgb)
            log("  celula %d: cor chapada %s (%s)" % (i, srgb[:3], mat.get("name")))
        atlas.paste(img, (cx, cy))

    atlas.save(out_png)
    log("atlas %dx%d -> %s" % (atlas.width, atlas.height, os.path.basename(out_png)))
    return cols, rows


def remap_uvs(uvs, tri_loops, tri_mat, cols, rows):
    """
    Reescreve o UV de cada canto para dentro da celula do material dele.

    Duas armadilhas moram aqui. (1) O V do Blender cresce para CIMA e o da textura do
    Minecraft cresce para BAIXO — sem inverter, a criatura sai com a textura de cabeca
    para baixo, o que num rosto e imediatamente obvio. (2) UV fora de [0,1] quer dizer
    textura repetida; dentro de um atlas repetir nao existe (vazaria na celula vizinha),
    entao a parte fracionaria e o melhor que da para fazer.
    """
    corner_uv = np.zeros((tri_loops.size, 2), dtype=np.float32)
    flat_loops = tri_loops.reshape(-1)
    # material do canto = material do triangulo dele
    flat_mat = np.repeat(tri_mat, 3)

    uv = uvs[flat_loops]
    frac = uv - np.floor(uv)          # tira o azulejamento
    col = (flat_mat % cols).astype(np.float32)
    row = (flat_mat // cols).astype(np.float32)

    corner_uv[:, 0] = (col + frac[:, 0]) / cols
    corner_uv[:, 1] = (row + (1.0 - frac[:, 1])) / rows   # V invertido

    return corner_uv, flat_loops


# Criaturas pintadas por GEOMETRIA em vez de usarem a textura que vieram (ver paint_bone).
PAINT_BONE = {"void"}


def bone_ramp(w=256, h=64):
    """
    A paleta: corpo preto -> sangue -> osso. O eixo U e a CONVEXIDADE da superficie, o
    eixo V e quanto aquela regiao esta ensanguentada.
    """
    ramp = np.zeros((h, w, 4), dtype=np.uint8)
    x = np.linspace(0.0, 1.0, w)

    # Os pousos da rampa, em (posicao, cor seca, cor molhada). Como o U entra por POSTO
    # (ver paint_bone), a posicao aqui e literalmente a fracao do corpo: dois tercos dela
    # e vulto preto, uma faixa fina e a carne rompida, e o quinto mais saliente e osso.
    stops = [
        (0.00, (4, 4, 6),       (10, 3, 4)),        # o vazio: nao se ve nada ali
        (0.58, (9, 9, 11),      (26, 5, 6)),        # pele, ainda preta
        (0.68, (44, 12, 12),    (104, 12, 14)),     # a carne abrindo
        (0.75, (130, 32, 28),   (176, 24, 25)),     # sangue vivo na beirada do osso
        (0.82, (196, 176, 146), (204, 146, 128)),   # osso aflorando, ainda sujo
        (1.00, (238, 232, 214), (230, 204, 186)),   # osso limpo, na crista
    ]

    for row in range(h):
        wet = row / (h - 1.0)
        for i in range(w):
            u = x[i]
            for k in range(len(stops) - 1):
                a, b = stops[k], stops[k + 1]
                if a[0] <= u <= b[0]:
                    t = (u - a[0]) / max(b[0] - a[0], 1e-6)
                    dry = np.array(a[1]) * (1 - t) + np.array(b[1]) * t
                    wetc = np.array(a[2]) * (1 - t) + np.array(b[2]) * t
                    ramp[row, i, :3] = (dry * (1 - wet) + wetc * wet).astype(np.uint8)
                    break
            ramp[row, i, 3] = 255
    return Image.fromarray(ramp, "RGBA")


def paint_bone(positions, normals, tri_verts):
    """
    Pinta a criatura medindo a propria malha: onde a superficie se PROJETA, o osso fura.

    POR QUE ASSIM, e nao pintando o UV. A textura desta criatura nao serve (o FBX aponta
    para nomes que nao existem no disco) e, mesmo que servisse, pintar "osso e sangue" em
    espaco de UV as cegas e chute: nao ha como saber que ilha do mapa e a costela e qual e
    a barriga. A geometria sabe. Uma crista de costela e CONVEXA e uma dobra de carne e
    CONCAVA — entao eu meco convexidade em cada vertice e deixo o osso nascer onde ele ja
    estava, em relevo. Sangue vai no degrau logo antes do osso, que e onde a pele rompe.

    A conta da convexidade: para cada vertice, olho para onde os vizinhos estao em relacao
    ao plano da normal dele. Vizinho ATRAS do plano quer dizer que a superficie foge dali
    — ou seja, e um pico. A media disso, com sinal trocado, e a convexidade.

    Devolve o UV por vertice: U = convexidade, V = quanto sangue.
    """
    p = positions[0]
    n = normals[0]
    n = n / np.maximum(np.linalg.norm(n, axis=1, keepdims=True), 1e-6)

    # soma vetorial dos vizinhos, montada de uma vez pelas arestas dos triangulos
    acc = np.zeros_like(p)
    cnt = np.zeros(len(p))
    for a, b in ((0, 1), (1, 2), (2, 0)):
        i, j = tri_verts[:, a], tri_verts[:, b]
        d = p[j] - p[i]
        d /= np.maximum(np.linalg.norm(d, axis=1, keepdims=True), 1e-6)
        np.add.at(acc, i, d)
        np.add.at(cnt, i, 1.0)
        np.add.at(acc, j, -d)
        np.add.at(cnt, j, 1.0)

    cnt = np.maximum(cnt, 1.0)
    conv = -(acc / cnt[:, None] * n).sum(axis=1)

    # POSTO, e nao valor. Esticar a convexidade entre o minimo e o maximo parece certo e
    # nao e: a convexidade se amontoa no meio (quase toda superficie e quase plana), entao
    # quase nenhum vertice alcancava a faixa do osso e a criatura saiu inteira cor de
    # sangue. Ordenando e usando a POSICAO na fila, a distribuicao vira uniforme e os
    # pousos da rampa passam a significar fracao do corpo: "os 18% mais salientes sao
    # osso" e uma decisao que se le e se ajusta, em vez de um numero magico.
    order = np.argsort(conv)
    u = np.empty(len(conv), dtype=np.float64)
    u[order] = np.linspace(0.0, 1.0, len(conv))

    # Sangue em manchas COERENTES (marmore de baixa frequencia). Ruido por vertice
    # sairia como chuvisco: cada ponto de uma cor, sem nenhuma mancha se formando.
    q = (p - p.mean(axis=0)) * 9.0
    marble = np.sin(q[:, 0]) * np.sin(q[:, 1] * 0.8 + 1.7) * np.sin(q[:, 2] * 1.3)
    # e escorre para baixo: o que esta em cima pinga no que esta embaixo
    # (np.ptp e nao p.ptp(): o metodo saiu do ndarray no numpy 2)
    fall = 1.0 - (p[:, 1] - p[:, 1].min()) / max(float(np.ptp(p[:, 1])), 1e-6)
    v = np.clip(0.30 + 0.45 * marble + 0.45 * fall, 0.0, 1.0)

    return np.stack([u, v], axis=1).astype(np.float32)


# Criaturas de pose unica que ganham vida por deformacao procedural (ver animate_sway).
#
# amp    = quanto o topo se desloca, em fracao da altura (a malha chega com altura 1.0)
# power  = como o balanco se distribui: 2 = pes plantados e cabeca solta; 1 = tomba inteira
# dangle = balanco EXTRA para o que esta longe do eixo do corpo (braco, garra pendurada)
# tremor  = amplitude do chacoalho rapido nos membros (peso: longe do eixo E em cima)
# shakes   = quantos ciclos de tremor cabem numa volta. TEM que ser inteiro, senao o laco
#            emenda com um salto; e no maximo ~frames/5, senao o tremor e amostrado grosso
#            demais e vira serrote em vez de vibracao
# twist    = torcao em GRAUS no topo, em torno do eixo vertical (a cabeca se retorce)
# nod      = quanto a cabeca tomba junto, para a torcao nao virar so um giro de parafuso
SWAY = {
    "tall":         {"amp": 0.038, "power": 2.2, "dangle": 0.010, "bob": 0.006,
                     "tremor": 0.000, "shakes": 0, "twist": 0.0, "nod": 0.0, "frames": 16},
    "claws_scream": {"amp": 0.030, "power": 1.9, "dangle": 0.050, "bob": 0.005,
                     "tremor": 0.052, "shakes": 6, "twist": 52.0, "nod": 0.070, "frames": 32},
}

SWAY_FRAMES = 16


def animate_sway(positions, normals, cfg, frames=SWAY_FRAMES):
    """
    Da vida a uma escultura de pose unica, sem esqueleto.

    O Alto e a das Garras nasceram de PNG: nao ha animacao para assar, porque nunca houve.
    Riga-las seria o caminho caro; para o que elas fazem, e caro a toa. Elas nao vao a
    lugar nenhum — o trabalho delas e ESTAR ali. E o que separa "estatua" de "viva", nesse
    caso, e movimento lento e continuo, nao passada.

    Entao o corpo balanca como haste: o deslocamento cresce com a altura elevada a
    `power`, o que deixa os pes plantados e a cabeca solta. Em X e Z com fases
    diferentes, para o topo descrever uma elipse preguicosa em vez de um vaivem de
    metronomo — vaivem reto e lido como mecanico na hora.

    `dangle` acrescenta balanco a quem esta LONGE DO EIXO do corpo, com atraso de fase:
    e o que faz a garra pendurada da segunda criatura oscilar depois do tronco, em vez de
    junto. E o mais perto de "membro solto" que se consegue sem osso nenhum.

    ⚠️ As normais NAO sao recalculadas. A deformacao e de baixa frequencia e amplitude
    pequena (uns 3% da altura), entao o erro de iluminacao fica abaixo do que o grao de
    VHS ja esconde. Se um dia a amplitude crescer, isto passa a aparecer como sombra que
    nao acompanha o corpo.
    """
    p = positions[0]
    n = normals[0]

    y = p[:, 1]
    h = np.clip((y - y.min()) / max(float(np.ptp(y)), 1e-6), 0.0, 1.0)
    weight = h ** cfg["power"]

    # distancia ao eixo vertical do corpo, normalizada: 0 no tronco, 1 na ponta do braco
    radial = np.linalg.norm(p[:, [0, 2]] - p[:, [0, 2]].mean(axis=0), axis=1)
    radial = radial / max(radial.max(), 1e-6)

    # O tremor e da PONTA dos membros: longe do eixo e alto no corpo. Assim os bracos e as
    # garras chacoalham e os pes, que estao plantados, nao.
    limb = radial * np.clip((h - 0.25) / 0.55, 0.0, 1.0) ** 1.5

    # ⚠️ NADA AQUI PODE RASGAR A MALHA, e a garantia nao e cuidado, e a forma da conta:
    # todo deslocamento e uma funcao CONTINUA da posicao do vertice (altura, distancia ao
    # eixo) vezes uma funcao do tempo. Dois vertices vizinhos tem posicao quase igual,
    # logo recebem deslocamento quase igual, logo continuam vizinhos. O que rasga modelo e
    # peso que salta — "este vertice e do braco, aquele nao" — e aqui nao existe peso
    # binario nenhum. Amplitude grande deixa o braco BORRACHUDO, nunca solto do ombro.
    centre_xz = p[:, [0, 2]].mean(axis=0)

    out_p, out_n = [], []
    for f in range(frames):
        t = 2.0 * np.pi * f / frames
        s = 2.0 * np.pi * cfg.get("shakes", 0) * f / frames
        d = p.copy()

        d[:, 0] += cfg["amp"] * np.sin(t) * weight
        d[:, 2] += cfg["amp"] * 0.62 * np.sin(t + 1.9) * weight
        d[:, 1] -= cfg["bob"] * (1.0 - np.cos(2.0 * t)) * 0.5 * h

        # o membro solto chega atrasado
        d[:, 0] += cfg["dangle"] * np.sin(t - 0.9) * radial * h
        d[:, 2] += cfg["dangle"] * 0.7 * np.cos(t - 1.4) * radial * h

        if cfg.get("tremor", 0.0) > 0.0 and cfg.get("shakes", 0) > 0:
            # os dois eixos fora de fase: tremor num eixo so vira aceno, e nao tremedeira
            d[:, 0] += cfg["tremor"] * np.sin(s) * limb
            d[:, 2] += cfg["tremor"] * 0.8 * np.sin(s * 1.0 + 2.4) * limb
            d[:, 1] += cfg["tremor"] * 0.35 * np.sin(s * 2.0 + 1.1) * limb

        if cfg.get("twist", 0.0) != 0.0:
            # Torcao em torno do eixo do corpo, crescendo com a altura: o quadril fica, o
            # pescoco gira. E rotacao de verdade (nao empurrao lateral), senao a cabeca
            # ACHATA em vez de virar.
            ang = np.radians(cfg["twist"]) * np.sin(t * 2.0 + 0.7) * (h ** 2.6)
            ca, sa = np.cos(ang), np.sin(ang)
            rx = d[:, 0] - centre_xz[0]
            rz = d[:, 2] - centre_xz[1]
            d[:, 0] = centre_xz[0] + rx * ca - rz * sa
            d[:, 2] = centre_xz[1] + rx * sa + rz * ca
            # e tomba junto, para nao parecer parafuso
            d[:, 0] += cfg.get("nod", 0.0) * np.sin(t * 2.0 + 2.1) * (h ** 3.0)

        out_p.append(d.astype(np.float32))
        out_n.append(n)

    return np.array(out_p), np.array(out_n)


def pack(name):
    log("=== %s ===" % name)
    raw = np.load(os.path.join(MESH_IN, name + "_raw.npz"))
    with open(os.path.join(MESH_IN, name + "_mats.json"), encoding="utf-8") as fh:
        mats = json.load(fh)

    positions = raw["positions"]        # (quadros, vertices, 3)
    normals = raw["normals"]
    uvs = raw["uvs"]
    tri_loops = raw["tri_loops"]
    tri_verts = raw["tri_verts"]
    tri_mat = raw["tri_mat"]

    if name in SWAY and positions.shape[0] == 1:
        cfg = SWAY[name]
        positions, normals = animate_sway(positions, normals, cfg,
                                          frames=cfg.get("frames", SWAY_FRAMES))
        log("pose unica animada por balanco: %d poses" % positions.shape[0])

    frames, verts, _ = positions.shape
    tris = tri_loops.shape[0]
    if verts > 65535:
        raise SystemExit("vertices demais para indice u16: %d" % verts)
    if frames > 255:
        raise SystemExit("quadros demais: %d" % frames)

    tex_dir = os.path.join(ASSETS, "textures", "entity", "anomaly")
    os.makedirs(tex_dir, exist_ok=True)
    corner_vert = tri_verts.reshape(-1).astype(np.uint16)

    if name in PAINT_BONE:
        bone_ramp().save(os.path.join(tex_dir, name + "_mesh.png"))
        vert_uv = paint_bone(positions, normals, tri_verts)
        corner_uv = vert_uv[corner_vert]
        log("pintada pela geometria: rampa preto->sangue->osso (256x64)")
    else:
        cols, rows = build_atlas(mats, os.path.join(tex_dir, name + "_mesh.png"), name)
        corner_uv, _ = remap_uvs(uvs, tri_loops, tri_mat, cols, rows)

    # a caixa da quantizacao cobre TODOS os quadros: se cobrisse so o primeiro, o braco
    # que sobe no meio da animacao estouraria o alcance e grudaria na borda da caixa
    qmin = positions.reshape(-1, 3).min(axis=0)
    qmax = positions.reshape(-1, 3).max(axis=0)
    span = np.maximum(qmax - qmin, 1e-6)

    q = np.clip(np.round((positions - qmin) / span * 65535.0), 0, 65535).astype(np.uint16)
    nlen = np.linalg.norm(normals, axis=2, keepdims=True)
    nq = np.clip(np.round(normals / np.maximum(nlen, 1e-6) * 127.0), -127, 127).astype(np.int8)

    out = os.path.join(ASSETS, "meshes")
    os.makedirs(out, exist_ok=True)
    path = os.path.join(out, name + ".mesh")

    with open(path, "wb") as fh:
        fh.write(b"RECM")
        fh.write(struct.pack(">BBHI", VERSION, frames, verts, corner_vert.size))
        fh.write(struct.pack(">6f", *qmin, *qmax))

        corners = np.empty(corner_vert.size, dtype=[("v", ">u2"), ("u", ">f4"), ("t", ">f4")])
        corners["v"] = corner_vert
        corners["u"] = corner_uv[:, 0]
        corners["t"] = corner_uv[:, 1]
        fh.write(corners.tobytes())

        for f in range(frames):
            block = np.empty(verts, dtype=[("p", ">u2", 3), ("n", "i1", 3)])
            block["p"] = q[f]
            block["n"] = nq[f]
            fh.write(block.tobytes())

    log("%d quadros, %d vertices, %d triangulos -> %s (%.0f KB)"
        % (frames, verts, tris, os.path.basename(path), os.path.getsize(path) / 1024))


for arg in sys.argv[1:]:
    pack(arg)
