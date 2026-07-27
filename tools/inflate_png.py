"""
Transforma o recorte 2D de uma anomalia em malha 3D, inflando a silhueta.

    python tools/inflate_png.py body_tall body_claws

A IDEIA. Estas criaturas sao vultos: o desenho inteiro delas e a silhueta. Entao em vez
de tentar adivinhar anatomia, eu dou VOLUME ao que ja existe. Para cada pixel do recorte
eu mido a distancia ate a borda mais proxima; essa distancia vira a espessura ali. No meio
do tronco a distancia e grande e ele fica gordo; num dedo de garra ela e de dois pixels e
o dedo fica fino. O resultado e um corpo redondo cuja silhueta, de frente, e exatamente a
do PNG original — porque ela nao foi redesenhada, foi inflada.

POR QUE NAO UM MODELO DE IA (TripoSR/TRELLIS): aqueles inventam as costas do bicho, e se
saem mal justamente em geometria fina e ramificada — garra, dedo, perna de aranha. E o
mesmo defeito que a gente ja mediu ao voxelizar o corpser. Aqui nada e inventado: o que
voce ve de frente e o desenho do Pedro, intacto.

O QUE ELE NAO FAZ: nao existe informacao de profundidade num PNG, entao a frente e a
costa saem SIMETRICAS. Para um vulto preto isso e invisivel. Para uma criatura com rosto
so de um lado (o Alto tem), a nuca sai com o mesmo rosto — ver `--flatten-back`.

Sai um .obj + .mtl que entram no mesmo bake_mesh.py das esculturas.
"""

import os
import sys

import numpy as np
from PIL import Image
from scipy.ndimage import distance_transform_edt, gaussian_filter

SRC = r"C:\Users\Hamilton\Downloads\vhsworldentities\Entitys_PNG\recortes"
OUT = r"C:\Users\Hamilton\Downloads\vhsworldentities\3d\_inflated"

# Largura da grade. Nao e a resolucao final (o Blender decima depois): e quanto detalhe
# de CONTORNO entra. Baixo demais e a garra some antes de comecar.
GRID_W = 130

# Quanto a espessura vale, em relacao a distancia ate a borda. 1.0 = membro redondo de
# verdade (um braco de 6 px de meia-largura fica com 6 px de profundidade). Abaixo de 1
# achata o bicho, o que ajuda quando ele e para ser visto quase sempre de frente.
DEPTH = 0.85

# Suaviza a espessura. Sem isto a secao do membro sai em losango (a distancia cresce em
# rampa reta), com uma quina no meio das costas.
SMOOTH = 1.6


def log(msg):
    print("[inflate] " + msg, flush=True)


def build(name, flatten_back=False):
    img = Image.open(os.path.join(SRC, name + ".png")).convert("RGBA")
    w0, h0 = img.size

    gw = GRID_W
    gh = max(4, int(round(h0 * gw / float(w0))))
    small = img.resize((gw, gh), Image.LANCZOS)
    alpha = np.array(small)[:, :, 3].astype(np.float32) / 255.0
    mask = alpha > 0.5

    if not mask.any():
        raise SystemExit("recorte vazio: " + name)

    # Uma moldura de fundo em volta garante que a distancia caia a zero na borda da
    # imagem — sem ela, um pe encostado no limite do PNG sairia com a espessura cortada
    # no meio, deixando o corpo aberto por baixo.
    pad = np.zeros((gh + 2, gw + 2), dtype=bool)
    pad[1:-1, 1:-1] = mask

    dist = distance_transform_edt(pad)[1:-1, 1:-1]
    height = gaussian_filter(dist, SMOOTH) * DEPTH
    height[~mask] = 0.0     # a silhueta tem que continuar exata: fora dela, espessura zero

    log("%s: grade %dx%d, %d pixels de corpo, espessura maxima %.1f px"
        % (name, gw, gh, int(mask.sum()), height.max()))

    # ---------------------------------------------------------------- monta a malha
    # Vertice por pixel, em duas camadas (frente em +Z, costas em -Z). Onde a espessura
    # e zero as duas camadas se encostam, e o casco fecha sozinho na silhueta — sem
    # precisar costurar borda nenhuma.
    cx, cy = (gw - 1) * 0.5, (gh - 1) * 0.5
    verts, uvs = [], []
    for side in (1.0, -1.0):
        z = side * (0.0 if (flatten_back and side < 0) else 1.0)
        for i in range(gh):
            for j in range(gw):
                verts.append((j - cx, (gh - 1 - i) - cy, height[i, j] * z))
                uvs.append((j / (gw - 1.0), 1.0 - i / (gh - 1.0)))

    faces = []
    layer = gh * gw
    for i in range(gh - 1):
        for j in range(gw - 1):
            a, b = i * gw + j, i * gw + j + 1
            c, d = (i + 1) * gw + j + 1, (i + 1) * gw + j
            if not (mask[i, j] or mask[i, j + 1] or mask[i + 1, j + 1] or mask[i + 1, j]):
                continue
            faces.append((a, b, c))
            faces.append((a, c, d))
            # costas com a volta invertida, para a normal apontar para fora
            faces.append((layer + a, layer + c, layer + b))
            faces.append((layer + a, layer + d, layer + c))

    os.makedirs(OUT, exist_ok=True)
    obj = os.path.join(OUT, name + ".obj")

    tex, tex_back = name + ".png", name + "_back.png"
    img.save(os.path.join(OUT, tex))

    # A NUCA nao pode ser o rosto. Frente e costas compartilham a mesma silhueta, entao
    # compartilhariam a mesma textura — e o Alto tem uma cara palida que apareceria
    # IDENTICA atras da cabeca, dando duas caras ao bicho. Um material separado para as
    # costas, com o desenho escurecido, resolve: por tras ele e so vulto. (Na anomalia
    # das garras, que ja e preta inteira, nao muda nada — mas custa 512 pixels e evita a
    # armadilha no proximo recorte que tiver rosto.)
    back = np.array(img).astype(np.float32)
    back[:, :, :3] *= 0.12
    Image.fromarray(back.astype(np.uint8)).save(os.path.join(OUT, tex_back))

    with open(os.path.join(OUT, name + ".mtl"), "w") as fh:
        fh.write("newmtl front\nKd 1 1 1\nmap_Kd %s\nmap_d %s\n" % (tex, tex))
        fh.write("newmtl back\nKd 1 1 1\nmap_Kd %s\nmap_d %s\n" % (tex_back, tex_back))

    with open(obj, "w") as fh:
        fh.write("mtllib %s.mtl\n" % name)
        for v in verts:
            fh.write("v %.4f %.4f %.4f\n" % v)
        for t in uvs:
            fh.write("vt %.5f %.5f\n" % t)

        def emit(group, which):
            fh.write("usemtl %s\n" % which)
            for f in group:
                fh.write("f %d/%d %d/%d %d/%d\n"
                         % (f[0] + 1, f[0] + 1, f[1] + 1, f[1] + 1, f[2] + 1, f[2] + 1))

        emit([f for f in faces if max(f) < layer], "front")
        emit([f for f in faces if max(f) >= layer], "back")

    log("%s: %d vertices, %d triangulos -> %s"
        % (name, len(verts), len(faces), os.path.basename(obj)))


args = [a for a in sys.argv[1:] if not a.startswith("--")]
flat = "--flatten-back" in sys.argv
for arg in args:
    build(arg, flat)
