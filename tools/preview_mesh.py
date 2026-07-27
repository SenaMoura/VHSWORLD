"""
Desenha o `.mesh` fora do jogo, para conferir antes de compilar.

    python tools/preview_mesh.py ophanim greyface

POR QUE. Erro de malha e sempre o mesmo punhado: criatura de cabeca para baixo, textura
espelhada, escala errada, animacao que nao mexe. Descobrir isso dentro do Minecraft custa
um ciclo inteiro de build + abrir o jogo + achar a criatura. Aqui custa dez segundos, e o
leitor deste arquivo e o MESMO contrato que o Java vai ler — se o desenho sai certo, o
formato esta certo, e o que sobrar de errado esta no renderer.

Sai uma folha: colunas = angulos em volta, linhas = quadros da animacao.
"""

import os
import struct
import sys

import numpy as np
from PIL import Image, ImageDraw

ASSETS = r"C:\Users\Hamilton\Downloads\GitHub\VHSWORLD\src\main\resources\assets\recmod"
OUT = r"C:\Users\Hamilton\AppData\Local\Temp\claude\C--Users-Hamilton\8f76d0e0-21a8-41f6-ae17-4ddc98d35f58\scratchpad"

SIZE = int(os.environ.get("PREVIEW_SIZE", "260"))    # pixels por vista
VIEWS = [0, 90, 180, 270]
SHOW_FRAMES = [0, 4, 8, 12]


def read_mesh(path):
    with open(path, "rb") as fh:
        data = fh.read()
    if data[:4] != b"RECM":
        raise SystemExit("nao e um .mesh: " + path)
    version, frames, verts, corners = struct.unpack_from(">BBHI", data, 4)
    off = 12
    qmin = np.array(struct.unpack_from(">3f", data, off)); off += 12
    qmax = np.array(struct.unpack_from(">3f", data, off)); off += 12

    ctype = np.dtype([("v", ">u2"), ("u", ">f4"), ("t", ">f4")])
    c = np.frombuffer(data, dtype=ctype, count=corners, offset=off)
    off += ctype.itemsize * corners

    ftype = np.dtype([("p", ">u2", 3), ("n", "i1", 3)])
    pos, nrm = [], []
    for _ in range(frames):
        blk = np.frombuffer(data, dtype=ftype, count=verts, offset=off)
        off += ftype.itemsize * verts
        pos.append(qmin + blk["p"].astype(np.float64) / 65535.0 * (qmax - qmin))
        nrm.append(blk["n"].astype(np.float64) / 127.0)

    return {
        "frames": frames, "verts": verts,
        "idx": c["v"].astype(np.int32).reshape(-1, 3),
        "uv": np.stack([c["u"], c["t"]], axis=1).reshape(-1, 3, 2),
        "pos": np.array(pos), "nrm": np.array(nrm),
    }


def render(mesh, atlas, frame, yaw_deg, ambient=0.35):
    """Rasterizador de pobre: ordena triangulo por profundidade e pinta por cima."""
    img = Image.new("RGB", (SIZE, SIZE), (24, 24, 28))
    draw = ImageDraw.Draw(img)

    # A LINHA DO CHAO (y=0 da malha). Sem ela nao da para julgar se a criatura pisa ou
    # flutua: olhando so a silhueta no vazio, qualquer altura parece plausivel.
    ground_y = SIZE * 0.94
    draw.line([(0, ground_y), (SIZE, ground_y)], fill=(70, 60, 55), width=1)

    p = mesh["pos"][frame]
    n = mesh["nrm"][frame]
    a = np.radians(yaw_deg)
    rot = np.array([[np.cos(a), 0, np.sin(a)],
                    [0, 1, 0],
                    [-np.sin(a), 0, np.cos(a)]])
    pr = p @ rot.T
    nr = n @ rot.T

    # a criatura tem altura 1.0 e pes no zero (o assador normaliza assim)
    sc = SIZE * 0.82
    sx = SIZE * 0.5 + pr[:, 0] * sc
    sy = SIZE * 0.94 - pr[:, 1] * sc

    tri = mesh["idx"]
    depth = pr[tri, 2].mean(axis=1)
    order = np.argsort(depth)          # fundo primeiro

    aw, ah = atlas.size
    px = atlas.load()

    # Cor do triangulo = MEDIA DAS CORES DOS TRES CANTOS, e nao a cor do UV medio.
    #
    # Parece a mesma coisa e nao e. Numa criatura pintada por curvatura, uma crista de
    # osso e uma fileira fina de vertices claros entre vertices pretos; tirando a media
    # dos UVs primeiro, o ponto medio cai na faixa preta da rampa e o osso SOME do
    # desenho — foi o que me fez achar que a paleta estava escura demais. A GPU interpola
    # cor por pixel, entao a media das cores e a aproximacao honesta.
    uvs3 = mesh["uv"]
    face_n = nr[tri].mean(axis=1)
    face_n /= np.maximum(np.linalg.norm(face_n, axis=1, keepdims=True), 1e-6)
    light = np.clip(ambient + (1.0 - ambient) * np.clip(face_n @ np.array([0.4, 0.6, -0.7]), 0, 1), 0, 1)

    for t in order:
        r = g = b = 0
        for c in range(3):
            u = min(max(int(uvs3[t, c, 0] * aw), 0), aw - 1)
            v = min(max(int(uvs3[t, c, 1] * ah), 0), ah - 1)
            cr, cg, cb = px[u, v][:3]
            r += cr; g += cg; b += cb
        r, g, b = r / 3.0, g / 3.0, b / 3.0
        k = light[t]
        pts = [(sx[i], sy[i]) for i in tri[t]]
        draw.polygon(pts, fill=(int(r * k), int(g * k), int(b * k)))

    return img


def preview(name):
    mesh = read_mesh(os.path.join(ASSETS, "meshes", name + ".mesh"))
    atlas = Image.open(os.path.join(ASSETS, "textures", "entity", "anomaly", name + "_mesh.png")).convert("RGB")

    frames = [f for f in SHOW_FRAMES if f < mesh["frames"]]
    sheet = Image.new("RGB", (SIZE * len(VIEWS), SIZE * len(frames)))
    for r, f in enumerate(frames):
        for c, yaw in enumerate(VIEWS):
            sheet.paste(render(mesh, atlas, f, yaw), (c * SIZE, r * SIZE))

    out = os.path.join(OUT, "preview_" + name + ".png")
    sheet.save(out)
    print("[preview] %s: %d quadros, %d vertices, %d triangulos -> %s"
          % (name, mesh["frames"], mesh["verts"], len(mesh["idx"]), out), flush=True)


def facing(name):
    """
    De que lado esta a CARA da escultura, medindo em vez de chutar.

    Cada modelo foi esculpido encarando um eixo diferente, e ninguem anota qual. Se eu
    chutar, a chance de o cacador vir atras do jogador andando de costas e alta — e esse
    e o tipo de erro que so aparece com o bicho ja na sua frente, no meio do jogo.

    O metodo e o mesmo que usei para achar a coluna certa da folha de sprites: o rosto e
    a parte CLARA da cabeca (pele palida contra a nuca escura), entao eu giro a criatura
    de 10 em 10 graus e pergunto em qual angulo o quarto de cima da silhueta esta mais
    claro. O pico e a frente dela.
    """
    mesh = read_mesh(os.path.join(ASSETS, "meshes", name + ".mesh"))
    atlas = Image.open(os.path.join(ASSETS, "textures", "entity", "anomaly", name + "_mesh.png")).convert("RGB")

    best, scores = None, []
    for yaw in range(0, 360, 10):
        arr = np.asarray(render(mesh, atlas, 0, yaw)).astype(np.float64)
        head = arr[: int(SIZE * 0.30)]                     # so o quarto de cima
        lum = head.mean(axis=2)
        body = lum[lum > 32]                               # ignora o fundo
        score = body.mean() if body.size > 40 else 0.0
        scores.append((yaw, score))
        if best is None or score > best[1]:
            best = (yaw, score)

    top = sorted(scores, key=lambda s: -s[1])[:4]
    print("[facing] %s: frente em ~%d graus | melhores: %s"
          % (name, best[0], ", ".join("%d(%.1f)" % s for s in top)), flush=True)


def views(name):
    """
    Doze angulos, claros e com o numero escrito. Para eu OLHAR de que lado esta a cara.

    O modo --facing (medir o brilho do topo da silhueta) me enganou uma vez e custou uma
    rodada: no Cara Cinza a NUCA e uma coroa de espetos palidos, mais clara que o proprio
    rosto, entao o pico do brilho apontou exatamente para o lado errado e ele saiu no jogo
    perseguindo o jogador de costas. Contra isso nao existe heuristica melhor: e olhar.
    """
    mesh = read_mesh(os.path.join(ASSETS, "meshes", name + ".mesh"))
    atlas = Image.open(os.path.join(ASSETS, "textures", "entity", "anomaly", name + "_mesh.png")).convert("RGB")

    angles = [int(a) for a in os.environ.get("PREVIEW_ANGLES", "").split(",") if a.strip()] \
        or list(range(0, 360, 30))
    cols = min(len(angles), 6)
    rows = (len(angles) + cols - 1) // cols
    sheet = Image.new("RGB", (SIZE * cols, SIZE * rows))
    for i, yaw in enumerate(angles):
        tile = render(mesh, atlas, 0, yaw, ambient=0.75)
        ImageDraw.Draw(tile).text((6, 6), "%d" % yaw, fill=(255, 80, 80))
        sheet.paste(tile, ((i % cols) * SIZE, (i // cols) * SIZE))

    out = os.path.join(OUT, "views_" + name + ".png")
    sheet.save(out)
    print("[views] %s -> %s" % (name, out), flush=True)


def axis(name):
    """
    Para onde a CARA aponta, medido na geometria — o numero que vai no meshYaw.

    As duas tentativas anteriores olharam para a IMAGEM e erraram: brilho do topo da
    silhueta apontou para a nuca do Cara Cinza (a coroa de espetos e mais clara que o
    rosto), e a olho nu 0 e 180 sao parecidos porque a cabeca dele e uma mascara torta.
    Aqui nao se olha pixel de tela: pega-se as faces do ROSTO e tira-se a media das
    NORMAIS delas. Normal e para onde a superficie aponta — e a resposta literal.

    Como se acham as faces do rosto:
      - PREVIEW_CELL=n  -> as que usam a celula n do atlas. Exato quando o rosto tem
        material proprio: no Ofanim o olho gigante e a textura "eyeball", celula 2.
      - senao           -> as claras do alto da cabeca (o Cara Cinza tem a mascara palida
        contra o corpo escuro), acima de PREVIEW_TOP da altura.

    O Minecraft espera o modelo encarando -Z, entao meshYaw = atan2(fx, -fz).
    """
    mesh = read_mesh(os.path.join(ASSETS, "meshes", name + ".mesh"))
    atlas = Image.open(os.path.join(ASSETS, "textures", "entity", "anomaly", name + "_mesh.png")).convert("RGB")
    cols, rows = atlas.width // 512, atlas.height // 512

    p = mesh["pos"][0]
    n = mesh["nrm"][0]
    tri = mesh["idx"]
    uvc = mesh["uv"].mean(axis=1)
    cy = p[tri, 1].mean(axis=1)

    px = atlas.load()
    lum = np.array([sum(px[min(int(u * atlas.width), atlas.width - 1),
                          min(int(v * atlas.height), atlas.height - 1)][:3]) / 3.0
                    for u, v in uvc])

    cell = os.environ.get("PREVIEW_CELL")
    if cell is not None:
        col = np.floor(uvc[:, 0] * cols).astype(int)
        row = np.floor(uvc[:, 1] * rows).astype(int)
        pick = (row * cols + col) == int(cell)
        how = "celula %s do atlas" % cell
        if os.environ.get("PREVIEW_PUPIL"):
            # O olho gigante e uma ESFERA: a normal media da esfera inteira e zero, entao
            # a celula sozinha nao responde nada (e ainda por cima ela cobre todos os
            # olhinhos do anel, que se cancelam entre si). Quem tem direcao e a PUPILA —
            # a mancha escura da textura. Pegando so as faces escuras, sobra a calota que
            # olha para frente, e a media dela e o olhar.
            pick &= lum < np.percentile(lum[pick], 12)
            how += " + so a pupila (12% mais escuros)"
    else:
        top = float(os.environ.get("PREVIEW_TOP", "0.80"))
        high = cy > top
        if not high.any():
            raise SystemExit("nenhuma face acima de %.2f" % top)
        pick = high & (lum > np.percentile(lum[high], 60))
        how = "faces claras acima de %.2f da altura" % top

    # So o que olha para os LADOS. Sem isto a media vira o topo do cranio: a calota da
    # cabeca aponta para cima, tem muito mais area que o rosto, e domina a soma — foi
    # exatamente o que aconteceu na primeira medida (direcao saiu com Y=0.99).
    horiz = np.abs(n[tri].mean(axis=1)[:, 1]) < float(os.environ.get("PREVIEW_HORIZ", "0.55"))
    pick = pick & horiz

    if not pick.any():
        raise SystemExit("nenhuma face selecionada")

    # peso por area: triangulo grande manda mais que lasca
    v0, v1, v2 = p[tri[pick, 0]], p[tri[pick, 1]], p[tri[pick, 2]]
    area = np.linalg.norm(np.cross(v1 - v0, v2 - v0), axis=1) * 0.5
    fn = n[tri[pick]].mean(axis=1)
    fn /= np.maximum(np.linalg.norm(fn, axis=1, keepdims=True), 1e-6)
    d = (fn * area[:, None]).sum(axis=0)
    d /= max(np.linalg.norm(d), 1e-6)

    yaw = np.degrees(np.arctan2(d[0], -d[2]))
    print("[axis] %s: %d faces (%s) | direcao=(%.2f, %.2f, %.2f) -> meshYaw = %.0f"
          % (name, int(pick.sum()), how, d[0], d[1], d[2], yaw), flush=True)


def feet(name):
    """
    Para onde a criatura anda, medido pelos PES — sem depender de textura nenhuma.

    O rosto do Cara Cinza nao serve de referencia: a cabeca dele e um emaranhado de
    tentaculos e a medida pela mascara palida balanca de 168 a -5 conforme o limiar de
    altura. Anatomia nao balanca: o DEDO do pe se projeta na frente do tornozelo. Entao
    comparo o centro dos pes com o centro das canelas, e o vetor de um para o outro
    aponta para a frente.

    Media dos 16 quadros de proposito: num quadro solto do passo um pe esta na frente e o
    outro atras, e a leitura sairia enviesada pela perna que por acaso esta adiantada.
    """
    mesh = read_mesh(os.path.join(ASSETS, "meshes", name + ".mesh"))
    acc = np.zeros(3)
    drift = np.zeros(3)

    for f in range(mesh["frames"]):
        p = mesh["pos"][f]
        y = p[:, 1]
        lo, hi = y.min(), y.max()
        h = hi - lo
        foot = p[y < lo + 0.06 * h]
        shin = p[(y > lo + 0.10 * h) & (y < lo + 0.30 * h)]
        if len(foot) and len(shin):
            acc += foot.mean(axis=0) - shin.mean(axis=0)

    # A deriva e a TENDENCIA do centro de massa, tirada por ajuste de reta.
    #
    # ⚠️ Eu somava (centro_f - centro_0) quadro a quadro e chamava aquilo de deriva. Nao
    # e: aquela soma acumula, entao um corpo que so BALANCA no lugar devolve um numero
    # grande e uma direcao que e so a fase em que a maioria dos quadros calhou de estar.
    # Foi assim que a void-creature — que anda parada, com a cauda chicoteando — me deu
    # "deriva 0.716" e um sentido de frente que nao existia. A reta so cresce se o corpo
    # for de fato para algum lado.
    centre = np.array([mesh["pos"][f].mean(axis=0) for f in range(mesh["frames"])])
    if mesh["frames"] >= 3:
        n = mesh["frames"]
        f = np.arange(n, dtype=np.float64)
        for axis in range(3):
            slope = np.polyfit(f, centre[:, axis], 1)[0]
            drift[axis] = slope * (n - 1)

    # SO o plano horizontal. O pe fica embaixo da canela, entao o Y domina o vetor e
    # afogaria a unica componente que interessa — para onde o dedo aponta.
    def yaw_of(v):
        h = np.array([v[0], 0.0, v[2]])
        m = np.linalg.norm(h)
        return (np.degrees(np.arctan2(h[0], -h[2])), m)

    # a cabeca e a outra testemunha: para onde a parte de cima do corpo se projeta em
    # relacao ao resto. Serve para bicho sem pe (a void-creature nao tem canela nenhuma).
    p0 = mesh["pos"][0]
    y = p0[:, 1]
    head = p0[y > y.min() + 0.80 * float(np.ptp(y))]
    hy, hm = yaw_of(head.mean(axis=0) - p0.mean(axis=0))

    ty, tm = yaw_of(acc)
    dy, dm = yaw_of(drift)
    print("[feet] %s: dedo na frente da canela em %.0f graus (forca %.3f)" % (name, ty, tm), flush=True)
    print("[feet] %s: cabeca projetada para %.0f graus (forca %.3f)" % (name, hy, hm), flush=True)
    if dm > 0.02:
        # A animacao ANDA (a raiz se desloca). Esta e a melhor testemunha que existe:
        # para onde o corpo viaja e, por definicao, para onde ele esta virado.
        print("[feet] %s: a animacao caminha para %.0f graus (deriva %.3f) <== use este"
              % (name, dy, dm), flush=True)
    else:
        print("[feet] %s: animacao sem deslocamento de raiz (%.4f)" % (name, dm), flush=True)


def cells(name):
    """
    Quanto e onde cada celula do atlas ocupa. Serve para achar material intruso.

    Foi assim que localizei as "bolinhas brancas" que cobriam os olhos do Ofanim: o FBX
    trouxe dois materiais SEM imagem, que viraram celula de cor chapada clara, e eu so
    descobri o que eram comparando a POSICAO deles com a dos olhos — mesma altura, mesmo
    raio, um pouquinho maiores. Era a calota de vidro por cima do olho, que no material
    original era transparente e sem textura vira uma bola leitosa.
    """
    mesh = read_mesh(os.path.join(ASSETS, "meshes", name + ".mesh"))
    atlas = Image.open(os.path.join(ASSETS, "textures", "entity", "anomaly", name + "_mesh.png")).convert("RGB")
    cols, rows = atlas.width // 512, atlas.height // 512

    p = mesh["pos"][0]
    tri = mesh["idx"]
    uvc = mesh["uv"].mean(axis=1)
    col = np.floor(uvc[:, 0] * cols).astype(int)
    row = np.floor(uvc[:, 1] * rows).astype(int)
    idx = row * cols + col

    px = atlas.load()
    for c in range(cols * rows):
        m = idx == c
        if not m.any():
            print("[cells] %d: (vazia)" % c, flush=True)
            continue
        cen = p[tri[m]].reshape(-1, 3)
        # cor tipica da celula: um toque no meio dela
        u = int((c % cols + 0.5) * 512)
        v = int((c // cols + 0.5) * 512)
        print("[cells] %d: %4d tris | centro=(%.2f %.2f %.2f) tam=(%.2f %.2f %.2f) cor=%s"
              % (c, int(m.sum()),
                 *cen.mean(axis=0), *(cen.max(axis=0) - cen.min(axis=0)),
                 px[min(u, atlas.width - 1), min(v, atlas.height - 1)][:3]), flush=True)


def motion(name):
    """
    Quanto a criatura se MEXE, em fracao da propria altura.

    Serve para nao confundir "animei" com "animou". Uma animacao pode existir no arquivo e
    ainda assim ser invisivel no jogo, seja porque a amplitude e pequena demais, seja
    porque as 16 poses saidas de uma animacao longa calharam de cair todas no mesmo
    trecho parado. Aqui da para ver os dois casos de uma vez: o deslocamento maximo
    (a maior distancia que um vertice percorre) e o medio (se o corpo inteiro participa
    ou so a ponta de um dedo).
    """
    mesh = read_mesh(os.path.join(ASSETS, "meshes", name + ".mesh"))
    p = mesh["pos"]
    if mesh["frames"] < 2:
        print("[motion] %s: pose unica, nao anima" % name, flush=True)
        return

    h = float(np.ptp(p[0][:, 1]))
    base = p[0]
    worst, mean = 0.0, 0.0
    for f in range(1, mesh["frames"]):
        d = np.linalg.norm(p[f] - base, axis=1)
        worst = max(worst, float(d.max()))
        mean = max(mean, float(d.mean()))
    print("[motion] %s: %d poses | maior deslocamento %.1f%% da altura | media %.1f%%"
          % (name, mesh["frames"], 100.0 * worst / h, 100.0 * mean / h), flush=True)


mode = "preview"
args = sys.argv[1:]
if args and args[0] in ("--facing", "--views", "--axis", "--feet", "--cells", "--motion"):
    mode, args = args[0][2:], args[1:]

runner = {"facing": facing, "views": views, "preview": preview,
          "axis": axis, "feet": feet, "cells": cells, "motion": motion}[mode]
for arg in args:
    runner(arg)
