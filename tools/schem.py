# -*- coding: utf-8 -*-
"""Leitor de .schem (Sponge Schematic v2/v3) do WorldEdit.

O Pedro salva as pecas das dimensoes pelo WorldEdit (`//copy` + `//schem save`),
que grava .schem — NAO o .nbt de bloco de estrutura. Os dois sao NBT gzipado, mas
o miolo e diferente: o .schem guarda os blocos num array de bytes achatado com
paleta de STRING->int (varint), enquanto o .nbt guarda uma lista de posicoes.
Este modulo faz a ponte, e e a base do import_dimension.py.

Uso:
    from schem import Schem
    s = Schem.load(path)
    s.get(x, y, z)  -> "minecraft:polished_andesite" (com [props] se tiver)
"""
import gzip
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from nbtlib import Reader

AIR = "minecraft:air"


def _varints(data):
    """A paleta do .schem indexa um fluxo de varint, nao um byte por bloco.

    Ate 127 blocos distintos cada indice cabe em 1 byte e da pra ler direto — foi
    por isso que a primeira versao disto funcionou nas pecas de andesito e quebrou
    na data_hub (24 estados). Com paleta grande o indice vira multi-byte e ler
    byte a byte desalinha o resto da peca inteira.
    """
    out = []
    value = 0
    shift = 0
    for b in data:
        value |= (b & 0x7F) << shift
        if b & 0x80:
            shift += 7
            continue
        out.append(value)
        value = 0
        shift = 0
    return out


class Schem:
    def __init__(self, width, height, length, palette, indices, offset=(0, 0, 0)):
        self.width = width
        self.height = height
        self.length = length
        self.palette = palette          # lista: indice -> nome do bloco
        self.indices = indices          # achatado, ordem y*W*L + z*W + x
        self.offset = offset

    @classmethod
    def load(cls, path):
        raw = gzip.open(path, "rb").read()
        r = Reader(raw)
        tag = r.u(">b", 1)
        r.string()
        root = r.payload(tag)
        # v3 embrulha tudo num "Schematic"; v2 poe na raiz.
        s = root.get("Schematic", root)

        width, height, length = s["Width"], s["Height"], s["Length"]

        # v3: Blocks{Palette, Data}. v2: Palette + BlockData na raiz.
        blocks = s.get("Blocks")
        if blocks is not None:
            pal_map, data = blocks["Palette"], blocks["Data"]
        else:
            pal_map, data = s["Palette"], s["BlockData"]

        palette = [AIR] * (max(pal_map.values()) + 1)
        for name, idx in pal_map.items():
            palette[idx] = name

        off = s.get("Offset") or [0, 0, 0]
        return cls(width, height, length, palette, _varints(bytes(data)),
                   (off[0], off[1], off[2]))

    def get(self, x, y, z):
        if not (0 <= x < self.width and 0 <= y < self.height and 0 <= z < self.length):
            return AIR
        i = y * self.width * self.length + z * self.width + x
        if i >= len(self.indices):
            return AIR
        return self.palette[self.indices[i]]

    def is_air(self, x, y, z):
        return self.get(x, y, z).split("[")[0] == AIR

    @property
    def size(self):
        return (self.width, self.height, self.length)

    def solid_cells(self):
        """Itera (x, y, z, nome) so dos blocos que nao sao ar."""
        for y in range(self.height):
            for z in range(self.length):
                for x in range(self.width):
                    name = self.get(x, y, z)
                    if name.split("[")[0] != AIR:
                        yield x, y, z, name
