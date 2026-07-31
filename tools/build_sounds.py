# -*- coding: utf-8 -*-
"""
Converte a pasta SOUNDTRACK/dimension do Pedro para os .ogg do mod, e escreve
o manifesto que os geradores de sounds.json / ModSounds.java consomem.

Duas decisoes que o script toma sozinho e que valem saber:

1. DEDUPE POR CONTEUDO. O mesmo mp3 aparece em ate 5 pastas ("Background Music V"
   esta em Data, Escritorio, Florest, Insidious e Sons Gerais). Converter cinco
   vezes botaria o mesmo audio cinco vezes dentro do jar. O arquivo e convertido
   UMA vez, com nome derivado do conteudo, e as varias dimensoes so apontam para
   ele no sounds.json.

2. MUSICA vs STING, pelo relogio. Faixa curta nao e trilha: se um chute de bumbo
   de 1,75s cair no sorteio da musica do bioma, o jogador ouve 1,75s de bumbo e o
   jogo sorteia de novo. Entao tudo abaixo de MIN_MUSIC_SEC vira "sting" (susto
   avulso, tocado pelo DimensionAmbience) e o resto vira trilha do bioma.
"""
import os, re, json, hashlib, subprocess, collections
import imageio_ffmpeg

SRC = r'C:/Users/Hamilton/Downloads/SOUNDTRACK vhsworld/dimension'
DST = r'C:/Users/Hamilton/Downloads/GitHub/VHSWORLD/src/main/resources/assets/recmod/sounds/music'
FF = imageio_ffmpeg.get_ffmpeg_exe()

# Abaixo disso a faixa e susto, nao trilha.
MIN_MUSIC_SEC = 40.0

# Qualidade do vorbis. -q:a 1 ~ 80kbps estereo: para ambiencia de terror e
# transparente o bastante, e segura o jar num tamanho que ainda da para subir.
QUALITY = '1'

# pasta do Pedro -> id da dimensao no mod
FOLDER_TO_DIM = {
    'Biblioteca': 'biblioteca',
    'Chunks': 'chunks',
    'Data': 'data',
    'Escritorio': 'escritorio',
    'Florest': 'floresta',
    'grass_rooms': 'grassrooms',
    'Insidious': 'insidious',
    'mall': 'mall',
    'Maze': 'maze',
    'Overworld': 'overworld',
    'Parkourland': 'parkourland',
    'Pipe_Tunels': 'pipe_tunels',
    'Stoneland': 'stoneland',
    'Train_Rails': 'train',
    'Under_Pressure': 'under_pressure',
    'Village': 'village',
    'Sons  Gerais': 'geral',
}


def slug(name):
    s = os.path.splitext(os.path.basename(name))[0].lower()
    s = s.replace('&', ' and ')
    s = re.sub(r'[^a-z0-9]+', '_', s).strip('_')
    # nomes gerados por banco de som carregam um id numerico longo no fim; corta
    s = re.sub(r'_\d{5,}$', '', s)
    s = re.sub(r'^(freesound_community|universfield|thellywellyn|dragon_studio|jusatti890|hellfamez|u_\w+?)_', '', s)
    return s[:48].strip('_')


def probe_seconds(path):
    out = subprocess.run([FF, '-i', path], capture_output=True, text=True, errors='ignore').stderr
    for line in out.splitlines():
        if 'Duration:' in line:
            t = line.split('Duration:')[1].split(',')[0].strip()
            h, m, s = t.split(':')
            return int(h) * 3600 + int(m) * 60 + float(s)
    return 0.0


def main():
    os.makedirs(DST, exist_ok=True)

    # ---------------------------------------------------------- varre e agrupa
    tracks = {}                                   # hash -> {slug, seconds, src}
    dim_tracks = collections.defaultdict(list)    # dim  -> [hash]

    for root, dirs, files in os.walk(SRC):
        rel = os.path.relpath(root, SRC).replace('\\', '/')
        top = rel.split('/')[0]
        if top == '.':
            continue
        dim = FOLDER_TO_DIM.get(top)
        if dim is None:
            print('!! pasta sem dimensao:', top)
            continue
        # "Sons Gerais/random sounds effect" e o pool global de sustos
        is_global_sfx = 'random sounds effect' in rel

        for fn in sorted(files):
            if not fn.lower().endswith(('.mp3', '.ogg', '.wav', '.flac', '.m4a')):
                continue
            p = os.path.join(root, fn)
            h = hashlib.sha1(open(p, 'rb').read()).hexdigest()[:10]
            if h not in tracks:
                tracks[h] = {'slug': slug(fn), 'seconds': probe_seconds(p), 'src': p}
            key = 'geral_sfx' if is_global_sfx else dim
            if h not in dim_tracks[key]:
                dim_tracks[key].append(h)

    # slugs colidem? (dois "Background Music VII/VIII" tem o mesmo nome de arquivo
    # em pastas diferentes com conteudos diferentes) -> sufixo pelo hash
    seen = collections.Counter(t['slug'] for t in tracks.values())
    used = set()
    for h, t in tracks.items():
        s = t['slug']
        if seen[s] > 1 or s in used:
            s = '%s_%s' % (s, h[:4])
        t['slug'] = s
        used.add(s)

    # ------------------------------------------------------------- transcodifica
    for h, t in sorted(tracks.items(), key=lambda kv: kv[1]['slug']):
        out = os.path.join(DST, t['slug'] + '.ogg')
        t['ogg'] = 'music/' + t['slug']
        if os.path.exists(out) and os.path.getsize(out) > 1000:
            print('  ja existe', t['slug'])
            continue
        cmd = [FF, '-y', '-hide_banner', '-loglevel', 'error',
               '-i', t['src'], '-vn', '-map_metadata', '-1',
               '-c:a', 'libvorbis', '-q:a', QUALITY, '-ar', '44100', '-ac', '2', out]
        r = subprocess.run(cmd, capture_output=True, text=True, errors='ignore')
        if r.returncode != 0:
            print('ERRO', t['slug'], r.stderr[:300])
        else:
            print('  %-46s %6.1fs -> %5.1f MB' % (t['slug'], t['seconds'],
                                                  os.path.getsize(out) / 1e6))

    # ------------------------------------------------------------- manifesto
    manifest = {}
    for dim, hashes in sorted(dim_tracks.items()):
        music = [tracks[h]['ogg'] for h in hashes if tracks[h]['seconds'] >= MIN_MUSIC_SEC]
        sting = [tracks[h]['ogg'] for h in hashes if tracks[h]['seconds'] < MIN_MUSIC_SEC]
        manifest[dim] = {'music': sorted(music), 'sting': sorted(sting)}

    json.dump(manifest, open('sound_manifest.json', 'w'), indent=2)

    total = sum(os.path.getsize(os.path.join(DST, f)) for f in os.listdir(DST))
    print('\n%d faixas unicas, %.1f MB de ogg' % (len(tracks), total / 1e6))
    for dim, v in sorted(manifest.items()):
        print('  %-16s musica=%d  sting=%d' % (dim, len(v['music']), len(v['sting'])))


if __name__ == '__main__':
    main()
