"""Mede a caixa que cada export do Blockbench REALMENTE ocupa, em espaco de entidade.

Serve para escolher o `sized(largura, altura)` do EntityType sem chutar. Chutar da
errado dos dois lados: caixa maior que o bicho faz ele empacar em porta que caberia,
caixa menor faz ele sumir da tela quando o centro sai do enquadramento.

⚠️ Nao medir pelo .bbmodel. La as coisas estao no espaco de tela do Blockbench, e a
relacao com o espaco da entidade depende de como o export normalizou a raiz (um saiu
com raiz em -15, outro em +24). A conta certa e no .java, que e o que o jogo desenha.
No espaco da entidade o Y cresce PARA BAIXO: o pe fica no maior Y.

Uso: python tools/measure_entity_models.py [arquivo.java ...]
"""
import os
import sys

import bbexport

SRC = r"C:\Users\Hamilton\Downloads\vhsworldentities"

DEFAULT = [
    r"Static_Watcher\Static_Watcher.java",
    r"Shade_Segment\Shade_Segment.java",
    r"Inverted_silhoutte\Inverted_silhoutte.java",
    r"Crawler_void\Crawler_Void.java",
]


def main(files):
    for f in files:
        path = f if os.path.isabs(f) else os.path.join(SRC, f)
        report = []
        mn, mx = bbexport.bounds(path, report)
        w = max(mx[0] - mn[0], mx[2] - mn[2]) / 16.0
        h = (mx[1] - mn[1]) / 16.0
        print(os.path.basename(path))
        print("   x %7.1f..%7.1f   y %7.1f..%7.1f   z %7.1f..%7.1f  (px)"
              % (mn[0], mx[0], mn[1], mx[1], mn[2], mx[2]))
        print("   largura %.2f  altura %.2f  profundidade %.2f  (blocos)"
              % ((mx[0] - mn[0]) / 16.0, h, (mx[2] - mn[2]) / 16.0))
        print("   sugestao sized(%.2fF, %.2fF)   pe em y=%.1f   textura %dx%d"
              % (w, h, mx[1], *bbexport.texture_size(path)))
        for line in report:
            print("   !! " + line)


if __name__ == "__main__":
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    main(sys.argv[1:] or DEFAULT)
