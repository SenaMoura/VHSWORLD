# -*- coding: utf-8 -*-
"""
Pega o sound_manifest.json e costura as tres pontas:
  1. assets/recmod/sounds.json  - os eventos e suas variantes
  2. ModSounds.java             - os RegistryObject
  3. worldgen/biome/<dim>.json  - o campo effects.music

O truque que faz isso ficar barato: um evento de som do Minecraft aceita VARIAS
entradas em "sounds", e ele sorteia uma a cada vez que toca. Entao "a trilha da
MAZE" nao precisa de codigo de playlist nenhum — e um evento so, `music_maze`,
com tres arquivos dentro. O bioma pede `music_maze` com delay 0 e o jogo fica
sorteando entre as tres para sempre, de graca.
"""
import json, os, re, collections

ROOT = r'C:/Users/Hamilton/Downloads/GitHub/VHSWORLD/src/main/resources'
SOUNDS_JSON = os.path.join(ROOT, 'assets/recmod/sounds.json')
BIOME_DIR = os.path.join(ROOT, 'data/recmod/worldgen/biome')

MANIFEST = json.load(open('sound_manifest.json'))

# dimensoes que ganham musica de bioma (overworld e geral sao tratados a parte)
DIMS = ['biblioteca', 'chunks', 'data', 'escritorio', 'floresta', 'grassrooms',
        'insidious', 'mall', 'maze', 'parkourland', 'pipe_tunels', 'stoneland',
        'train', 'under_pressure', 'village']

# Dimensao sem trilha propria cai no pool "Sons Gerais". Hoje so a mall.
FALLBACK = 'geral'


def music_pool(dim):
    pool = MANIFEST.get(dim, {}).get('music', [])
    return pool if pool else MANIFEST[FALLBACK]['music']


def sting_pool(dim):
    return MANIFEST.get(dim, {}).get('sting', [])


def main():
    # ------------------------------------------------------------ sounds.json
    data = json.load(open(SOUNDS_JSON, encoding='utf-8'),
                     object_pairs_hook=collections.OrderedDict)

    # tira as tres antigas de musica; vao ser reescritas com as variantes
    for k in list(data):
        if k.startswith('music_') or k.startswith('sting_'):
            del data[k]

    def entry(paths, category, stream):
        return collections.OrderedDict([
            ('category', category),
            ('sounds', [collections.OrderedDict([('name', 'recmod:' + p),
                                                 ('stream', stream)]) for p in paths]),
        ])

    for dim in DIMS + ['overworld']:
        data['music_' + dim] = entry(music_pool(dim), 'music', True)

    # sustos: curtos, entao nao precisam de stream (stream custa uma thread de IO)
    for dim in DIMS + ['overworld']:
        p = sting_pool(dim)
        if p:
            data['sting_' + dim] = entry(p, 'ambient', False)
    data['sting_geral'] = entry(MANIFEST['geral_sfx']['sting'], 'ambient', False)

    json.dump(data, open(SOUNDS_JSON, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
    print('sounds.json: %d eventos' % len(data))

    # ------------------------------------------------------------ biomas
    for dim in DIMS:
        f = os.path.join(BIOME_DIR, dim + '.json')
        if not os.path.exists(f):
            print('  (bioma %s ainda nao existe - a dimensao nova cria depois)' % dim)
            continue
        b = json.load(open(f, encoding='utf-8'), object_pairs_hook=collections.OrderedDict)
        b['effects']['music'] = collections.OrderedDict([
            ('sound', 'recmod:music_' + dim),
            ('min_delay', 0),
            ('max_delay', 0),
            ('replace_current_music', True),
        ])
        json.dump(b, open(f, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
        print('  bioma %-16s -> music_%s (%d faixas)' % (dim, dim, len(music_pool(dim))))

    # ------------------------------------------------------------ Java
    lines = []
    for dim in DIMS + ['overworld']:
        lines.append('    public static final RegistryObject<SoundEvent> MUSIC_%s =' % dim.upper())
        lines.append('            SOUND_EVENTS.register("music_%s", () -> SoundEvent.createVariableRangeEvent(' % dim)
        lines.append('                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_%s")));' % dim)
        lines.append('')
    for dim in DIMS + ['overworld']:
        if sting_pool(dim):
            lines.append('    public static final RegistryObject<SoundEvent> STING_%s =' % dim.upper())
            lines.append('            SOUND_EVENTS.register("sting_%s", () -> SoundEvent.createVariableRangeEvent(' % dim)
            lines.append('                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "sting_%s")));' % dim)
            lines.append('')
    lines.append('    public static final RegistryObject<SoundEvent> STING_GERAL =')
    lines.append('            SOUND_EVENTS.register("sting_geral", () -> SoundEvent.createVariableRangeEvent(')
    lines.append('                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "sting_geral")));')
    open('modsounds_snippet.java', 'w').write('\n'.join(lines))
    print('\nsnippet Java em modsounds_snippet.java (%d linhas)' % len(lines))


if __name__ == '__main__':
    main()
