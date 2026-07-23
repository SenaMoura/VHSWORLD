"""Escritor de NBT + montador de estruturas do Minecraft."""
import gzip, struct

def _str(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b

def _tag(t, name, payload):
    return struct.pack(">b", t) + _str(name) + payload

def _int(v):    return struct.pack(">i", v)
def _list(tid, items): return struct.pack(">b", tid) + _int(len(items)) + b"".join(items)

def _compound(pairs):
    return b"".join(pairs) + b"\x00"

def _c_str(name, v):  return _tag(8, name, _str(v))
def _c_int(name, v):  return _tag(3, name, _int(v))
def _c_comp(name, pairs): return _tag(10, name, _compound(pairs))
def _c_list(name, tid, items): return _tag(9, name, _list(tid, items))


class Structure:
    """Grade de blocos que vira um .nbt de estrutura."""

    def __init__(self, sx, sy, sz, data_version=3465):
        self.size = (sx, sy, sz)
        self.dv = data_version
        self.palette = []          # (name, props_tuple)
        self.blocks = {}           # (x,y,z) -> (state_index, nbt_pairs|None)

    def state(self, name, props=None):
        key = (name, tuple(sorted((props or {}).items())))
        if key not in self.palette:
            self.palette.append(key)
        return self.palette.index(key)

    def set(self, x, y, z, name, props=None, nbt=None):
        self.blocks[(x, y, z)] = (self.state(name, props), nbt)

    def fill(self, x1, y1, z1, x2, y2, z2, name, props=None):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, name, props)

    def name_at(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return None if b is None else self.palette[b[0]][0]

    def jigsaw(self, x, y, z, orientation, name, target, pool, final_state, joint="rollable"):
        self.set(x, y, z, "minecraft:jigsaw", {"orientation": orientation}, [
            _c_str("joint", joint),
            _c_str("final_state", final_state),
            _c_str("name", name),
            _c_str("pool", pool),
            _c_str("id", "minecraft:jigsaw"),
            _c_str("target", target),
        ])

    def to_bytes(self):
        pal = []
        for name, props in self.palette:
            pairs = [_c_str("Name", name)]
            if props:
                pairs.insert(0, _c_comp("Properties", [_c_str(k, v) for k, v in props]))
            pal.append(_compound(pairs))

        blocks = []
        for (x, y, z), (state, nbt) in sorted(self.blocks.items()):
            pairs = [
                _c_list("pos", 3, [_int(x), _int(y), _int(z)]),
                _c_int("state", state),
            ]
            if nbt:
                pairs.append(_c_comp("nbt", nbt))
            blocks.append(_compound(pairs))

        root = _compound([
            _c_int("DataVersion", self.dv),
            _c_list("size", 3, [_int(v) for v in self.size]),
            _c_list("palette", 10, pal),
            _c_list("blocks", 10, blocks),
            _c_list("entities", 10, []),
        ])
        return _tag(10, "", root)

    def save(self, path):
        with gzip.open(path, "wb") as f:
            f.write(self.to_bytes())
