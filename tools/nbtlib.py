"""Leitor/escritor minimo de NBT, so o que o formato de estrutura usa."""
import gzip, struct

TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG = 0, 1, 2, 3, 4
TAG_FLOAT, TAG_DOUBLE, TAG_BYTE_ARRAY, TAG_STRING = 5, 6, 7, 8
TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY, TAG_LONG_ARRAY = 9, 10, 11, 12


class Reader:
    def __init__(self, data):
        self.d, self.i = data, 0

    def u(self, fmt, n):
        v = struct.unpack_from(fmt, self.d, self.i)[0]
        self.i += n
        return v

    def string(self):
        n = self.u(">H", 2)
        s = self.d[self.i:self.i + n].decode("utf-8", "replace")
        self.i += n
        return s

    def payload(self, t):
        if t == TAG_BYTE:   return self.u(">b", 1)
        if t == TAG_SHORT:  return self.u(">h", 2)
        if t == TAG_INT:    return self.u(">i", 4)
        if t == TAG_LONG:   return self.u(">q", 8)
        if t == TAG_FLOAT:  return self.u(">f", 4)
        if t == TAG_DOUBLE: return self.u(">d", 8)
        if t == TAG_STRING: return self.string()
        if t == TAG_BYTE_ARRAY:
            n = self.u(">i", 4); v = self.d[self.i:self.i+n]; self.i += n; return v
        if t == TAG_INT_ARRAY:
            n = self.u(">i", 4); return [self.u(">i", 4) for _ in range(n)]
        if t == TAG_LONG_ARRAY:
            n = self.u(">i", 4); return [self.u(">q", 8) for _ in range(n)]
        if t == TAG_LIST:
            et = self.u(">b", 1); n = self.u(">i", 4)
            return [self.payload(et) for _ in range(n)]
        if t == TAG_COMPOUND:
            out = {}
            while True:
                tt = self.u(">b", 1)
                if tt == TAG_END: return out
                name = self.string()
                out[name] = self.payload(tt)
        raise ValueError("tag desconhecida %d" % t)


def read(path):
    with gzip.open(path, "rb") as f:
        data = f.read()
    r = Reader(data)
    t = r.u(">b", 1)
    r.string()
    return r.payload(t)
