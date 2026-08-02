"""Converte .bbmodel em geometria Java (LayerDefinition), sem passar pelo .java exportado.

POR QUE NAO USAR O .java DO BLOCKBENCH
--------------------------------------
A geometria vinha transcrita do "modded entity" .java que o Blockbench exporta. A
geometria de la e correta — foi conferida cubo a cubo contra o .bbmodel nos quatro
modelos desta leva. O problema sao os NOMES, e a lista cresce a cada leva:

  1. nome de parte com espaco ("Right Arm") -> o .java nao compila;
  2. namespace placeholder "modid:" -> tem que virar "recmod:";
  3. classe/layer com nome de outra entidade (o corrupted_4 saiu como corrupted_3);
  4. nome de layer com espaco e traco -> ResourceLocation invalido;
  5. ⚠️ o Crawler_void tem "Arms_3" E "Arms3". Em snake_case os dois viram a mesma
     coisa, e `addOrReplaceChild` com nome repetido SUBSTITUI sem reclamar: o bicho
     perde membros e nao aparece erro nenhum, nem em compilacao nem em execucao.

Consertar isso no texto ja pronto e remendo que so cresce. Aqui a geometria e gerada
do .bbmodel, onde as partes ainda tem identidade propria (uuid) e a colisao de nome
simplesmente nao existe — o gerador desambigua na hora de escrever.

A conversao e simples:

  espaco do Blockbench -> espaco de entidade do Minecraft
      x -> -x        (o render da entidade tem scale(-1,-1,1))
      y -> 24 - y    (24 e o chao; no Minecraft o Y cresce para BAIXO)
      z ->  z

  Como o mapa e um giro de 180 graus em Z, uma rotacao (rx, ry, rz) do Blockbench
  vira (-rx, -ry, rz) no Minecraft. (Conferido no Static_Watcher: o cubo girado 15
  graus em X saiu do exportador como -0.2618 rad.)

  Grupo  -> PartDefinition, com offset = origem do grupo menos a origem do PAI.
  Cubo   -> addBox em coordenadas relativas a origem do grupo.
  Cubo COM rotacao -> nao existe cubo girado no Minecraft, entao ele vira uma parte
            filha propria ("<nome>_r1") com o pivo e a rotacao dele. E exatamente o
            que o exportador do Blockbench chama de cube_rN.

Uso: python tools/bbmodel_to_geometry.py
"""
import json
import math
import os
import re

OUT_DIR = (r"C:\Users\Hamilton\Downloads\GitHub\VHSWORLD\src\main\java\net\vhsworld"
           r"\rec\client\entity\geom")

SRC = r"C:\Users\Hamilton\Downloads\vhsworldentities"

# bbmodel -> (classe Java, nome da parte raiz usada pelo modelo)
JOBS = [
    (r"Static_Watcher\Static_Watcher.bbmodel", "StaticWatcherGeometry"),
    (r"Shade_Segment\Shade_Segment.bbmodel", "ShadeSegmentGeometry"),
    (r"Inverted_silhoutte\Inverted_silhoutte.bbmodel", "InvertedSilhouetteGeometry"),
    (r"Crawler_void\Crawler_Void.bbmodel", "CrawlerVoidGeometry"),
]

GROUND = 24.0


def snake(name):
    s = re.sub(r"[^0-9a-zA-Z]+", "_", name.strip()).strip("_").lower()
    return s or "part"


def camel(name):
    bits = snake(name).split("_")
    return "".join(w.capitalize() if i else w for i, w in enumerate(bits))


class Namer:
    """Nomes unicos, de parte E de variavel.

    Os dois precisam ser garantidos, e por motivos diferentes:
    - parte: `addOrReplaceChild` com nome repetido SUBSTITUI em silencio, e o bicho
      perde membros sem erro nenhum (o Crawler_void tem "Arms_3" e "Arms3");
    - variavel: "Arms_3" e "Arms3" viram os dois `arms3` em camelCase, e ai o Java
      nem compila — ou pior, o cubo seguinte entra na parte errada.
    """

    def __init__(self):
        self.parts = set()
        self.vars = set()

    @staticmethod
    def _free(taken, want):
        if want not in taken:
            taken.add(want)
            return want
        n = 2
        while "%s_%d" % (want, n) in taken:
            n += 1
        out = "%s_%d" % (want, n)
        taken.add(out)
        return out

    def __call__(self, want):
        """Devolve (nome da parte, nome da variavel), ambos livres."""
        part = self._free(self.parts, want)
        var = self._free(self.vars, camel(part))
        return part, var


def f(v):
    """Float no formato do Java, sem -0.0 e sem cauda de ponto flutuante."""
    v = round(v + 0.0, 4)
    if v == 0:
        v = 0.0
    return "%sF" % ("%.4f" % v).rstrip("0").rstrip(".") if v != int(v) else "%.1fF" % v


def cube_java(el, origin):
    """O trecho .texOffs(..).addBox(..) de um cubo, relativo a `origin` (em bb)."""
    fr, to = el["from"], el["to"]
    size = [to[i] - fr[i] for i in range(3)]

    x = origin[0] - to[0]
    y = origin[1] - to[1]
    z = fr[2] - origin[2]

    uv = el.get("uv_offset", [0, 0])
    infl = el.get("inflate", 0) or 0

    box = "addBox(%s, %s, %s, %s, %s, %s, new CubeDeformation(%s))" % (
        f(x), f(y), f(z), f(size[0]), f(size[1]), f(size[2]), f(infl))

    # mirror() troca o lado da textura; tem que ser desligado logo depois, senao
    # contamina todos os cubos seguintes do mesmo CubeListBuilder.
    if el.get("mirror_uv"):
        box = "mirror().%s.mirror(false)" % box

    return ".texOffs(%d, %d).%s" % (int(uv[0]), int(uv[1]), box)


def build(path, class_name):
    doc = json.load(open(path, encoding="utf-8"))
    els = {e["uuid"]: e for e in doc["elements"] if "from" in e}
    grps = {g["uuid"]: g for g in doc.get("groups", [])}
    res = doc.get("resolution", {"width": 64, "height": 64})

    namer = Namer()
    lines = []
    roots = []

    def emit(node, parent_var, parent_origin, depth):
        """Escreve um grupo e desce. Devolve o nome da parte."""
        grp = grps.get(node["uuid"], {})
        origin = grp.get("origin", [0.0, 0.0, 0.0])
        rot = grp.get("rotation", [0.0, 0.0, 0.0]) or [0.0, 0.0, 0.0]

        part, var = namer(snake(grp.get("name", "part")))

        if parent_var is None:
            # A raiz nasce no chao: o +24 do mapa de coordenadas entra aqui, uma
            # vez so. Nos filhos ele se cancela na subtracao.
            off = [-origin[0], GROUND - origin[1], origin[2]]
        else:
            off = [parent_origin[0] - origin[0],
                   parent_origin[1] - origin[1],
                   origin[2] - parent_origin[2]]

        # os cubos SEM rotacao vao direto neste grupo
        cubes = []
        for child in node.get("children", []):
            if isinstance(child, str) and child in els and not any(
                    els[child].get("rotation") or []):
                cubes.append(cube_java(els[child], origin))

        builder = "CubeListBuilder.create()" + "".join(cubes)
        if len(builder) > 100 and cubes:
            builder = ("CubeListBuilder.create()"
                       + ("\n" + " " * 8).join([""] + cubes))

        pose = pose_java(off, rot)
        target = "root" if parent_var is None else parent_var
        lines.append("        PartDefinition %s = %s.addOrReplaceChild(\"%s\", %s, %s);\n"
                     % (var, target, part, builder, pose))
        if parent_var is None:
            roots.append(part)

        # os cubos COM rotacao viram parte propria, com o pivo deles
        for child in node.get("children", []):
            if isinstance(child, str) and child in els and any(
                    els[child].get("rotation") or []):
                emit_rotated(els[child], var, origin, part)

        for child in node.get("children", []):
            if isinstance(child, dict):
                emit(child, var, origin, depth + 1)

        return part

    def emit_rotated(el, parent_var, parent_origin, parent_part):
        pivot = el.get("origin", [0.0, 0.0, 0.0])
        rot = el.get("rotation", [0.0, 0.0, 0.0])

        part, var = namer("%s_r" % snake(el.get("name") or "cube"))

        off = [parent_origin[0] - pivot[0],
               parent_origin[1] - pivot[1],
               pivot[2] - parent_origin[2]]

        lines.append("        PartDefinition %s = %s.addOrReplaceChild(\"%s\", "
                     "CubeListBuilder.create()%s, %s);\n"
                     % (var, parent_var, part, cube_java(el, pivot), pose_java(off, rot)))

    def pose_java(off, rot):
        if any(abs(a) > 1e-6 for a in rot):
            # o mapa espelha X e Y, entao os giros em X e Y trocam de sinal
            return "PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)" % (
                f(off[0]), f(off[1]), f(off[2]),
                f(-math.radians(rot[0])), f(-math.radians(rot[1])),
                f(math.radians(rot[2])))
        return "PartPose.offset(%s, %s, %s)" % (f(off[0]), f(off[1]), f(off[2]))

    # cubos soltos na raiz do outliner (sem grupo nenhum)
    for node in doc["outliner"]:
        if isinstance(node, dict):
            emit(node, None, [0.0, 0.0, 0.0], 0)

    body = "".join(lines)
    src = TEMPLATE % dict(cls=class_name, body=body,
                          w=int(res["width"]), h=int(res["height"]),
                          model=os.path.basename(path),
                          roots=", ".join('"%s"' % r for r in roots))
    dest = os.path.join(OUT_DIR, class_name + ".java")
    open(dest, "w", encoding="utf-8").write(src)
    return dest, roots


TEMPLATE = '''package net.vhsworld.rec.client.entity.geom;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * ARQUIVO GERADO — nao editar a mao.
 * Fonte: %(model)s
 * Gerador: tools/bbmodel_to_geometry.py
 *
 * A geometria vem do .bbmodel, e nao do .java que o Blockbench exporta: o export tem
 * historico de nome de parte invalido em Java e, no Crawler_void, de duas partes com
 * o mesmo nome — e nome repetido no addOrReplaceChild apaga a anterior em silencio.
 * A animacao fica na classe de modelo, nao aqui.
 *
 * Partes na raiz: %(roots)s
 */
public final class %(cls)s {

    private %(cls)s() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

%(body)s
        return LayerDefinition.create(mesh, %(w)d, %(h)d);
    }
}
'''


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for rel, cls in JOBS:
        dest, roots = build(os.path.join(SRC, rel), cls)
        print("%-30s -> %s  (raizes: %s)" % (os.path.basename(rel),
                                             os.path.basename(dest), ", ".join(roots)))


if __name__ == "__main__":
    main()
