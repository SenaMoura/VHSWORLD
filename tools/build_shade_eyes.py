"""Gera a mascara emissiva do Anomalo da Sombra: os dois pontos brancos.

POR QUE ELA E DESENHADA AQUI, E NAO RECORTADA DA TEXTURA
--------------------------------------------------------
Os olhos deste bicho nao sao enfeite: sao a UNICA coisa que o jogador ve dele no
breu, que e exatamente quando ele anda. A camada emissiva e a mecanica aparecendo na
tela. Ela nao podia ficar dependendo da pintura.

E a pintura, hoje, nao tem olhos. `Shade_segment.png` ainda esta com os marcadores
coloridos do Blockbench (verde e salmao) por cima de um corpo bege — nao e a "coluna
preta rasgada com dois pontos brancos" do documento. Recortar os pixels mais claros
dela daria o corpo bege inteiro brilhando no escuro, que e o oposto do efeito.

Entao os dois pontos sao postos onde eles TEM que estar: na face da frente do cubo
da cabeca, achada pela conta de UV do proprio modelo. Quando o Pedro pintar a textura
de verdade, e so rodar de novo — ou apagar este arquivo, se os olhos ja vierem
pintados e bastar recortar.

A CONTA DA FACE DA FRENTE (box UV do Minecraft)
Para um cubo com texOffs(u, v) e tamanho (w, h, d), a face -Z (a que encara o
jogador) fica em (u + d, v + d), com w de largura e h de altura. E a mesma conta que
poe o rosto do Steve em (8,8) numa skin de cabeca 8x8x8 com texOffs(0,0).

Uso: python tools/build_shade_eyes.py
"""
import os

from PIL import Image

TEX = (r"C:\Users\Hamilton\Downloads\GitHub\VHSWORLD\src\main\resources\assets"
       r"\recmod\textures\entity\shade_segment.png")
OUT = (r"C:\Users\Hamilton\Downloads\GitHub\VHSWORLD\src\main\resources\assets"
       r"\recmod\textures\entity\shade_segment_eyes.png")

# do ShadeSegmentGeometry: texOffs(0, 22), addBox(..., 8.0F, 9.0F, 8.0F)
TEX_OFFS = (0, 22)
BOX = (8, 9, 8)          # largura, altura, profundidade
UV_SIZE = 128            # a resolucao de UV declarada no LayerDefinition


def main():
    src = Image.open(TEX)
    w, h = src.size

    # A textura pode ter resolucao maior que o espaco de UV (a do Pedro e 256 para
    # um UV de 128). A mascara TEM que sair no mesmo tamanho do arquivo original,
    # senao ela nao alinha com o modelo.
    scale = w // UV_SIZE
    mask = Image.new("RGBA", (w, h), (0, 0, 0, 0))

    u, v = TEX_OFFS
    bw, bh, bd = BOX
    face_u = u + bd
    face_v = v + bd

    # Dois pontos de 2x2 (em UV), no terco de cima da face e afastados do centro.
    # Altos e juntos: olho no meio do rosto le como bicho; olho no alto e junto le
    # como coisa que NAO tem rosto, que e o que o desenho pede.
    eye_y = face_v + 2
    for eye_x in (face_u + 2, face_u + bw - 4):
        for dx in range(2):
            for dy in range(2):
                px = (eye_x + dx) * scale
                py = (eye_y + dy) * scale
                for sx in range(scale):
                    for sy in range(scale):
                        mask.putpixel((px + sx, py + sy), (255, 255, 255, 255))

    mask.save(OUT)
    print("escrito: %s  (%dx%d, face da frente em UV %d,%d)"
          % (os.path.basename(OUT), w, h, face_u, face_v))


if __name__ == "__main__":
    main()
