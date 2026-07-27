"""
Recorta e monta os sprites 2D das anomalias do VHSWORLD.

O material bruto vem em duas familias, e cada uma pede uma chave diferente:

  CORPOS  — arte escura sobre fundo BRANCO. O alfa sai da distancia ate o branco.
  ROSTOS  — rosto pálido sobre fundo PRETO, com degradê. O alfa sai da LUMINANCIA,
            e isso e melhor do que recortar: o rosto continua nascendo do escuro,
            sem borda dura. E o mesmo efeito que a gente quer no jogo.

Nos dois casos o recorte e feito "des-misturando" a cor de fundo (unpremultiply):
sem isso sobra halo — branco em volta do corpo, cinza em volta do rosto.

Rodar: python tools/build_anomaly_sprites.py
Saida: Downloads/vhsworldentities/Entitys_PNG/recortes/
"""

import os
from collections import deque

import numpy as np
from PIL import Image, ImageFilter

SRC = r"C:\Users\Hamilton\Downloads\vhsworldentities\Entitys_PNG"
OUT = os.path.join(SRC, "recortes")
ASSETS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                      "src", "main", "resources", "assets", "recmod",
                      "textures", "entity", "anomaly")

# id do AnomalyType -> qual recorte/montagem vira a textura do jogo
DEPLOY = {
    "tall": "body_tall",
    "spider_smile": "anomaly_spider_smile",
    "claws_scream": "anomaly_claws_scream",
}

# Corte da "mancha solida" por arte, quando o padrao nao da conta. O claws vem com
# uma sombra de chao PINTADA, mais escura que a dos outros e encostada no pe: com
# 0.60 ela ainda conta como corpo e vem junto.
BLOB_THRESH = {"body_claws": 0.80}

# nome de arquivo -> (apelido, familia)
CATALOG = {
    "d9aa8819cafec305630b92f4205208a8.jpg": ("body_spider", "white"),
    "77032c8c96db2ccd4a4a89ba5b8f9ad7.jpg": ("body_tall", "white"),
    "b55f54fdeb03617304cd5163d25827fd.jpg": ("body_claws", "white"),
    "4b1efa98-6638-4b31-a78a-a8731a2f71c3.png": ("face_smile", "black"),
    "852c6503-88b7-4ac9-a178-06ae524ceb10.png": ("face_rings", "black"),
    "image_8e771f.png": ("face_skull", "black"),
    "image_8e7a65.png": ("face_scream", "black"),
}


def load(path):
    return np.asarray(Image.open(path).convert("RGB"), dtype=np.float32) / 255.0


def luma(rgb):
    return rgb[..., 0] * 0.299 + rgb[..., 1] * 0.587 + rgb[..., 2] * 0.114


def unpremultiply(rgb, alpha, bg):
    """Tira a cor do fundo de dentro do pixel, senao sobra halo na borda."""
    a = np.clip(alpha, 1e-3, 1.0)[..., None]
    out = (rgb - bg * (1.0 - a)) / a
    return np.clip(out, 0.0, 1.0)


def key_white(rgb, solid=0.70, ceil=0.88):
    """
    Fundo branco: quanto mais escuro, mais opaco.

    ⚠️ A rampa e CURTA de proposito. A primeira versao ia de 0.06 a 0.82, e o
    resultado era um corpo semitransparente: qualquer cinza de sombreado — as
    costelas do Alto, o volume das pernas — virava alfa 0.6 e o cenario aparecia
    ATRAVES da criatura. Corpo tem que ser corpo. Aqui tudo abaixo de `solid` e
    opaco, e a transicao mora so na borda, entre `solid` e `ceil`, que e onde a
    arte encosta no fundo de verdade.
    """
    a = (ceil - luma(rgb)) / (ceil - solid)
    return np.clip(a, 0.0, 1.0), np.array([1.0, 1.0, 1.0], dtype=np.float32)


def key_black(rgb, floor=0.05, ceil=0.62, gamma=0.75):
    """
    Fundo preto: quanto mais claro, mais opaco. O degradê vira o proprio fade.

    O gamma < 1 segura o meio-tom, que e onde mora o volume do rosto. Sem ele so
    o brilho da testa e do nariz sobrevive e a cara vira mancha.
    """
    a = (luma(rgb) - floor) / (ceil - floor)
    return np.clip(a, 0.0, 1.0) ** gamma, np.array([0.0, 0.0, 0.0], dtype=np.float32)


def largest_blob(alpha, thresh=0.35):
    """
    Fica so com a mancha principal.

    E o que joga fora marca d'agua, respingo de tinta e sombra solta no chao sem
    eu precisar adivinhar coordenada. Busca em largura, iterativa — recursao nao
    aguenta uma imagem de 1000px.
    """
    solid = alpha > thresh
    h, w = solid.shape
    seen = np.zeros_like(solid)
    best, best_size = None, 0

    for sy in range(h):
        for sx in range(w):
            if not solid[sy, sx] or seen[sy, sx]:
                continue

            blob = []
            q = deque([(sy, sx)])
            seen[sy, sx] = True
            while q:
                y, x = q.popleft()
                blob.append((y, x))
                for ny, nx in ((y - 1, x), (y + 1, x), (y, x - 1), (y, x + 1)):
                    if 0 <= ny < h and 0 <= nx < w and solid[ny, nx] and not seen[ny, nx]:
                        seen[ny, nx] = True
                        q.append((ny, nx))

            if len(blob) > best_size:
                best, best_size = blob, len(blob)

    if best is None:
        return alpha

    keep = np.zeros_like(solid)
    for y, x in best:
        keep[y, x] = True
    # Dilata um pouco para nao comer o meio-tom da borda, que e o que segura a
    # silhueta bonita depois de reduzir.
    keep = np.asarray(Image.fromarray((keep * 255).astype(np.uint8))
                      .filter(ImageFilter.MaxFilter(5)), dtype=np.float32) / 255.0
    return alpha * keep


def drop_floor_shadow(alpha, band=0.15, factor=1.4):
    """
    Apaga a sombra de chao que as artes trazem pintada.

    Ela nao sai pelo filtro de mancha porque ENCOSTA no pe — e a mesma mancha. Mas
    ela se entrega na largura: as pernas ocupam ~10 pixels por linha e a sombra
    salta para 40+. Entao a regra e essa, e nao uma coordenada chutada: no rodape,
    linha que e larga demais para ser perna e chao, nao criatura.

    Vale para qualquer arte nova com sombra — nao e remendo de uma imagem so.
    """
    solid = alpha > 0.3
    widths = solid.sum(axis=1)
    h = alpha.shape[0]

    # Referencia: o meio das pernas, entre 60% e 85% da altura.
    legs = widths[int(h * 0.60):int(h * 0.85)]
    legs = legs[legs > 0]
    if len(legs) == 0:
        return alpha

    limit = np.median(legs) * factor
    out = alpha.copy()
    for y in range(int(h * (1.0 - band)), h):
        if widths[y] > limit:
            out[y, :] = 0.0
    return out


def trim(rgba, pad=2):
    """Corta o vazio em volta. Sprite com margem desperdica resolucao no jogo."""
    a = rgba[..., 3]
    ys, xs = np.where(a > 0.02)
    if len(ys) == 0:
        return rgba
    y0, y1 = max(0, ys.min() - pad), min(a.shape[0], ys.max() + pad + 1)
    x0, x1 = max(0, xs.min() - pad), min(a.shape[1], xs.max() + pad + 1)
    return rgba[y0:y1, x0:x1]


def to_image(rgba):
    return Image.fromarray((np.clip(rgba, 0, 1) * 255).astype(np.uint8), "RGBA")


def cut(path, family, blob_thresh=0.60):
    rgb = load(path)

    if family == "white":
        # Arte escura sobre branco: aqui o halo E branco, entao tem que sair de
        # dentro do pixel — senao cada perna fina ganha um contorno leitoso.
        alpha, bg = key_white(rgb)
        # Corte alto (0.6) e nao 0.35: a SOMBRA do chao das artes originais tem alfa
        # ~0.45 e encosta no pe, entao com corte baixo ela entra na mesma mancha da
        # criatura e vem junto — uma poca cinza flutuando no ar debaixo dela. Acima de
        # 0.6 so o corpo de verdade e "solido", e a sombra fica de fora.
        alpha = largest_blob(alpha, thresh=blob_thresh)
        alpha = drop_floor_shadow(alpha)
        color = unpremultiply(rgb, alpha, bg)
    else:
        # Rosto sobre preto: a cor fica COMO ESTA, de proposito.
        # Desfazer a pre-multiplicacao aqui empurra todo pixel para o branco (dividir
        # um cinza por um alfa baixo estoura), e o rosto vira um fantasma lavado — foi
        # exatamente o que aconteceu na primeira leva. Com a cor crua, o escuro fica
        # transparente e o claro fica claro: o rosto nasce do breu, que e o efeito.
        # ⚠️ E NADA de largest_blob aqui. Num rosto que nasce do escuro, o olho e a
        # boca sao BURACOS de alfa baixo — a "maior mancha conexa" joga fora metade da
        # cara. Ja aconteceu; o rosto sorridente voltou vazado no meio.
        alpha, _ = key_black(rgb)
        color = rgb

    return trim(np.dstack([color, alpha]))


# ------------------------------------------------------------------ montagem

# apelido -> (corpo, rosto, altura do rosto em fracao da altura do corpo,
#             centro do rosto em fracao da largura, em fracao da altura)
COMPOSITES = {
    "anomaly_spider_smile": ("body_spider", "face_smile", 0.20, 0.50, 0.11),
    "anomaly_claws_scream": ("body_claws", "face_scream", 0.17, 0.50, 0.07),
}


def compose(body_name, face_name, face_h, cx, cy):
    body = Image.open(os.path.join(OUT, body_name + ".png")).convert("RGBA")
    face = Image.open(os.path.join(OUT, face_name + ".png")).convert("RGBA")

    h = max(8, int(body.height * face_h))
    face = face.resize((max(8, int(face.width * h / face.height)), h), Image.LANCZOS)

    out = body.copy()
    x = int(body.width * cx - face.width / 2)
    y = int(body.height * cy - face.height / 2)
    # alpha_composite em vez de paste: o rosto tem meio-tom nas bordas, e paste com
    # mascara so escolhe um dos dois — a borda do rosto ficaria recortada a faca.
    layer = Image.new("RGBA", out.size, (0, 0, 0, 0))
    layer.paste(face, (x, y))
    return Image.alpha_composite(out, layer)


def to_game(img, height=128, grain=0.0, seed=7):
    """
    Baixa para a resolucao do jogo.

    ⚠️ SEM GRAO POR PADRAO, e por dois motivos. Primeiro: o mod ja passa grao de
    fita na tela inteira (client/VHSEffectOverlay) — grao no sprite e grao em
    dobro, e o do overlay e o que acompanha a bateria e a sanidade. Segundo, o que
    me pegou na primeira tentativa: ruido ADITIVO num corpo quase preto so tem para
    onde ir para cima, entao ele acende pontinhos claros soltos e esfarrapa a
    silhueta — que e a unica coisa que o jogador le a 20 blocos de distancia.

    O parametro fica aqui para o caso de uma anomalia querer o proprio chuvisco,
    e nesse caso ele e MULTIPLICATIVO: modula o que ja existe, nao inventa luz.
    """
    w = max(8, int(img.width * height / img.height))
    small = img.resize((w, height), Image.LANCZOS)
    if grain <= 0.0:
        return small

    a = np.asarray(small, dtype=np.float32) / 255.0
    rng = np.random.default_rng(seed)
    factor = 1.0 + rng.normal(0.0, grain, size=(height, w, 1))
    a[..., :3] = np.clip(a[..., :3] * factor, 0.0, 1.0)
    return to_image(a)


def main():
    os.makedirs(OUT, exist_ok=True)
    for filename, (nick, family) in CATALOG.items():
        src = os.path.join(SRC, filename)
        if not os.path.exists(src):
            print("  !! sumiu:", filename)
            continue
        img = to_image(cut(src, family, BLOB_THRESH.get(nick, 0.60)))
        img.save(os.path.join(OUT, nick + ".png"))
        print(f"  {nick:14s} {img.width}x{img.height}  <- {filename}")

    print()
    for nick, args in COMPOSITES.items():
        full = compose(*args)
        full.save(os.path.join(OUT, nick + ".png"))
        game = to_game(full)
        game.save(os.path.join(OUT, nick + "_game.png"))
        print(f"  {nick:22s} {full.width}x{full.height}  -> jogo {game.width}x{game.height}")

    # E entrega no assets do mod, com o nome que o AnomalyType espera. Fazer isto
    # aqui e nao a mao e o que garante que a arte do jogo e SEMPRE a saida deste
    # script: mexeu no recorte, roda de novo e o mod ja esta com a versao nova.
    print()
    os.makedirs(ASSETS, exist_ok=True)
    for anomaly_id, source in DEPLOY.items():
        img = Image.open(os.path.join(OUT, source + ".png")).convert("RGBA")
        game = to_game(img)
        game.save(os.path.join(ASSETS, anomaly_id + ".png"))
        print(f"  assets/{anomaly_id}.png  {game.width}x{game.height}")


if __name__ == "__main__":
    main()
