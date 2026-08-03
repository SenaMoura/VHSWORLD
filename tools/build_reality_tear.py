"""Gerador do RASGO DA REALIDADE: 16x128, 8 quadros, na paleta e na luz da familia FRACTURE.

O que ele partilha com a fracture, e de proposito: a MESMA rampa de 5 cores e a MESMA
onda de brilho diagonal (wave/energy). Os dois sao a mesma materia — o rasgo e de onde
o FRACTURE e arrancado, entao eles tem que parecer a mesma coisa quando estiverem na
tela juntos.

O que ele NAO partilha, tambem de proposito: a silhueta. A espada e um talho diagonal de
lamina; o rasgo e uma FENDA VERTICAL rasgada, que afunila ate sumir nas duas pontas e tem
as bordas irregulares e DIFERENTES uma da outra. Nada de retangulo — nenhuma linha do
sprite tem a largura da linha de cima.

E ela RESPIRA: a fenda abre, segura e fecha. O nucleo branco (o vazio atras da realidade)
so aparece quando ela esta aberta — nos quadros fechados sobra so o contorno. Esse e o
unico acontecimento que uma textura de 16 pixels consegue ter.
"""
import math
import os

from PIL import Image

RES = r"C:\Users\Hamilton\Downloads\GitHub\VHSWORLD\src\main\resources\assets\recmod"
TEX = os.path.join(RES, "textures", "item")

S = 16
FRAMES = 8

# Paleta lida do fracture.png (mesma rampa, mesma ordem)
RAMP = [
    (86, 14, 56, 150),     # 0 penumbra
    (156, 28, 96, 220),    # 1 borda
    (232, 72, 150, 255),   # 2 corpo
    (255, 176, 214, 255),  # 3 realce
    (255, 255, 255, 255),  # 4 nucleo
]

# A ESPINHA: onde fica o centro da fenda em cada linha. Escrita a mao em vez de sorteada
# — em dezesseis pixels, ruido aleatorio vira sujeira, nao rasgo. Ela desce tortuosa da
# direita para a esquerda, com joelhos: costura de tecido rasgado, nunca uma reta.
SPINE = [9.4, 9.0, 9.1, 8.3, 8.0, 8.4, 7.6, 7.3,
         7.6, 6.9, 6.6, 7.1, 6.4, 6.2, 6.6, 6.3]

# AS DUAS BORDAS SAO DIFERENTES. Rasgo real nao e simetrico: um lado abre mais que o
# outro. Simetria aqui daria uma folha, um olho — coisa desenhada, nao coisa rompida.
#
# ⚠️ A VARIACAO E MENOR DO QUE PARECE QUE DEVERIA (0.72-1.18, nao 0.5-1.3). Com o
# intervalo largo o contorno ganhava dentes de um pixel e a silhueta lia como "minhoca
# encaracolada", nao como corte. Rasgo e uma linha que se abriu: o serrilhado tem que ser
# perceptivel de perto e sumir de longe.
RAGGED_L = [0.74, 1.02, 0.85, 1.14, 0.90, 1.18, 0.80, 1.10,
            0.88, 1.16, 0.78, 1.06, 0.94, 1.12, 0.80, 0.72]
RAGGED_R = [0.80, 0.88, 1.16, 0.92, 1.18, 0.84, 1.12, 0.95,
            1.18, 0.80, 1.10, 0.88, 1.14, 0.78, 0.92, 0.74]

# A RESPIRACAO: abre, segura, fecha. Copia a forma da cobertura da fracture (sobe, segura
# no alto, desce) para as duas animacoes baterem o mesmo compasso.
PULSE = [0.20, 0.50, 0.78, 1.00, 1.00, 0.88, 0.62, 0.34]

# ⚠️ O INTERVALO E LARGO DE PROPOSITO. Num teste com 1.45-2.55 a cobertura ia so de 38% a
# 46% e a animacao NAO LIA: parecia textura parada com chiado. E o nucleo branco quase
# nunca abria, entao o unico acontecimento da textura sumia junto.
AMP_MIN = 1.55
AMP_MAX = 3.05


def wave(x, y, f, scale=9.0, amp=1.15):
    """Onda de brilho que atravessa a peca na diagonal, uma volta a cada ciclo."""
    return math.sin(((x + y) / scale - f / FRAMES) * 2 * math.pi) * amp


def energy(level, x, y, f, amp=1.15):
    i = int(round(max(0.0, min(4.0, level + wave(x, y, f, amp=amp)))))
    return RAMP[i]


def half_width(y, amp, side):
    """Meia largura da fenda nesta linha. Zero nas pontas, cheia no meio."""
    t = (y + 0.5) / S
    # sin^0.45: afunila nas pontas e engorda no miolo, que e o que faz a coisa parecer
    # RASGADA em vez de recortada. Expoente baixo de proposito — com 0.55 o corpo so
    # existia no meio e as duas pontas viravam risquinho sem cor.
    taper = math.sin(math.pi * t) ** 0.45
    ragged = RAGGED_L[y] if side < 0 else RAGGED_R[y]
    return amp * taper * ragged


def frame(index):
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    amp = AMP_MIN + (AMP_MAX - AMP_MIN) * PULSE[index]

    for y in range(S):
        for x in range(S):
            offset = (x + 0.5) - SPINE[y]
            w = half_width(y, amp, -1 if offset < 0 else 1)

            d = abs(offset)

            # ⚠️ O ANEL ESCURO E FIXO, E ELE E O QUE FAZ A COISA TER FORMA. Na primeira
            # versao a onda de brilho corria pelo sprite INTEIRO, borda inclusive: o
            # branco vazava ate o limite e cada quadro virava um borrao claro sem
            # silhueta. A fracture nunca faz isso — ela tem contorno plum fechado em
            # volta de tudo, e e o contorno que segura o desenho enquanto a luz anda.
            # Entao a onda so vale DENTRO do corpo; contorno e halo nao a escutam.
            if d <= w:
                # Miolo: 4 na espinha, 2 na borda do corpo, mais a onda diagonal — e ela
                # que faz o nucleo aparecer em MANCHAS QUE ANDAM, do jeito da espada, em
                # vez de um risco branco solido. O piso 2 impede que a luz, ao passar,
                # abra buraco no corpo e coma o contorno por dentro.
                # ⚠️ O PICO E 3.6, NAO 4.0, e a diferenca de 0.4 e o que separa "vazio
                # que passa" de "borrao branco". Com 4.0 no centro, a espinha inteira ja
                # estava no nucleo antes de a onda chegar, e a onda so podia clarear o
                # que ja era branco — o efeito ficava parado e a fenda florescia. Com
                # 3.6, o branco PRECISA da onda para existir: ele nasce, atravessa e
                # apaga, que e o unico jeito de o vazio parecer estar se mexendo la atras.
                level = 2.0 + 1.6 * (1.0 - d / max(w, 0.001))
                i = int(round(max(2.0, min(4.0, level + wave(x, y, index)))))
                img.putpixel((x, y), RAMP[i])
            elif d <= w + 0.95:
                img.putpixel((x, y), RAMP[1])   # contorno
            elif d <= w + 1.85:
                img.putpixel((x, y), RAMP[0])   # penumbra

    return img


def main():
    sheet = Image.new("RGBA", (S, S * FRAMES), (0, 0, 0, 0))
    for f in range(FRAMES):
        sheet.paste(frame(f), (0, f * S))

    out = os.path.join(TEX, "reality_tear.png")
    sheet.save(out)
    print("gravado:", out, sheet.size)

    px = sheet.load()
    for f in range(FRAMES):
        n = sum(1 for y in range(f * S, (f + 1) * S)
                for x in range(S) if px[x, y][3] > 0)
        core = sum(1 for y in range(f * S, (f + 1) * S)
                   for x in range(S) if px[x, y] == RAMP[4])
        print(f"  quadro {f}: {n} px ({100 * n / (S * S):.0f}%), nucleo {core}")

    for f in (0, 3):
        print(f"\nQUADRO {f}:")
        for y in range(f * S, (f + 1) * S):
            row = ""
            for x in range(S):
                c = px[x, y]
                row += "." if c[3] == 0 else ",o+*#"[RAMP.index(c)]
            print("  " + row)


if __name__ == "__main__":
    main()
