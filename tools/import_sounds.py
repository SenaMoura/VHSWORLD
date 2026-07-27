"""
Traz os sons do Pedro para dentro do mod.

⚠️ MOTIVO DE EXISTIR: os arquivos que ele separou em SOUNDTRACK vhsworld/entitys sao
**.mp3**, e o Minecraft nao toca mp3 — so **.ogg** (Vorbis). Colar o mp3 no jar nao
da erro de build: da um som que simplesmente nunca sai, e se perde meia hora
procurando o defeito no codigo. Entao a conversao e obrigatoria e mora aqui.

Tambem corta o que e longo demais: um "susto" de 40 segundos nao e susto, e trilha —
e um arquivo de 1 MB por criatura incha o jar a toa.

Rodar: python tools/import_sounds.py
"""

import os
import subprocess

import imageio_ffmpeg
import numpy as np

SRC = r"C:\Users\Hamilton\Downloads\SOUNDTRACK vhsworld"
DEST = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "recmod", "sounds")

# destino.ogg -> (caminho relativo na pasta do Pedro, segundos maximos)
#
# AVISTAMENTO: um por criatura. Curto, seco, reconhecivel — o jogador tem que
# aprender a associar o som aquela coisa especifica, senao vira barulho generico.
SIGHT = {
    "sight_tall":     ("entitys/hgoliya08-scary-sound-effect-298866.mp3", 5),
    "sight_spider":   ("entitys/dragon-studio-glitch-effect-1-397982.mp3", 4),
    "sight_claws":    ("entitys/freesound_community-crazy-distorted-screaming-sound-39350.mp3", 5),
    "sight_stoneman": ("entitys/freesoundsxx-loud-bang-horror-268572.mp3", 4),
    # As duas esculturas nao tinham som nenhum — o EntitySightSounds caia no
    # `default: return null` para elas.
    "sight_greyface": ("Chapters/Chapter 3/freesound_community-monster-appearing-sound-41450.mp3", 3),
    "sight_ophanim":  ("dimension/hellfamez-horror-metallic-screeches-183236.mp3", 5),
}

# FUNDO: tocam sozinhos, de reipente, sem criatura nenhuma por perto. Mais longos e
# mais vagos de proposito — o que assusta aqui e nao saber se tinha alguma coisa.
DREAD = {
    "dread_speech": ("entitys/freesound_community-garbled-speech-32862.mp3", 8),
    "dread_broken": ("entitys/freesound_community-broken-audio-file-distortion-104395.mp3", 8),
    "dread_glitch": ("entitys/soundreality-glitch-sfx-479202.mp3", 6),
    "dread_steps":  ("entitys/monster-footsteps.mp3", 8),
    "dread_burnt":  ("entitys/yo-tu-burnt-out-232187.mp3", 8),
    "dread_flesh":  ("entitys/tanweraman-flesh-growing-horror-392360.mp3", 6),
}


# ---------------------------------------------------------------------------- loops
#
# Som que toca EM LOOP nao pode ser recortado a olho: o silencio das pontas vira um
# buraco a cada volta, e o corte seco vira um clique que denuncia a costura. Estes
# passam por numpy porque as duas coisas se resolvem medindo — apara o silencio de
# verdade (por energia) e cruza a cauda com a cabeca.
#
# nome -> (arquivo, comeco em segundos, duracao maxima, crossfade em segundos)
LOOPS = {
    # A TRILHA DA CACADA. Toca enquanto o Cara Cinza esta atras de voce, e para
    # quando ele desiste — e o unico jeito de o jogador saber que foi visto.
    # O arquivo tem 0.7s de silencio na frente e 1.3s no fim; em loop isso seria
    # uma pausa de dois segundos no meio da perseguicao.
    "greyface_chase": ("entitys/Hunt.mp3", 0.0, 12.0, 0.35),

    # O ZUMBIDO DO OFANIM. Escolhido MEDINDO cinco candidatos: este e o unico com
    # envelope plano (variacao 0.02 e cabeca/cauda 0.98) — os outros tem curva de
    # trilha, e trilha em loop se entrega na primeira volta.
    "ophanim_drone": ("dimension/freesound_community-creepy-distortion-29539.mp3", 1.0, 16.0, 0.80),
}

# O PASSO. Um so, isolado do meio de uma gravacao com varios.
STEP = ("entitys/monster-footsteps.mp3", 0.62, 1.25)


# ------------------------------------------------------------------- dimensoes
#
# A TRILHA DE CADA DIMENSAO. O Pedro separou uma pasta por dimensao em
# `SOUNDTRACK vhsworld/dimension`; cada uma vira a musica de fundo do bioma
# correspondente (campo `music` do bioma), com pausa entre as repeticoes.
#
# Estes NAO passam pelo tratamento dos sustos: nada de mono, nada de corte. Musica
# de fundo e estereo e toca inteira — encolher para 8 segundos transformaria a
# trilha num efeito sonoro repetitivo.
#
# nome -> caminho relativo na pasta do Pedro
DIM_MUSIC = {
    "music_data": "dimension/Data/universfield-scary-horror-atmosphere-176754.mp3",
    "music_chunks": "dimension/Chunks/universfield-dark-horror-suspense-30s-355836.mp3",
    "music_insidious": "dimension/Insidious/universfield-scary-horror-atmosphere-176754.mp3",
}

RATE = 44100


def decode(rel):
    """O arquivo do Pedro em float mono, para poder medir antes de cortar."""
    src = os.path.join(SRC, rel.replace("/", os.sep))
    if not os.path.exists(src):
        print("  !! sumiu:", rel)
        return None
    p = subprocess.run([imageio_ffmpeg.get_ffmpeg_exe(), "-v", "quiet", "-i", src,
                        "-ac", "1", "-ar", str(RATE), "-f", "s16le", "-"],
                       capture_output=True, check=True)
    return np.frombuffer(p.stdout, dtype="<i2").astype(np.float32) / 32768.0


def encode(name, x):
    out = os.path.join(DEST, name + ".ogg")
    raw = (np.clip(x, -1.0, 1.0) * 32767.0).astype("<i2").tobytes()
    subprocess.run([imageio_ffmpeg.get_ffmpeg_exe(), "-y", "-v", "quiet",
                    "-f", "s16le", "-ar", str(RATE), "-ac", "1", "-i", "pipe:0",
                    "-c:a", "libvorbis", "-q:a", "3", out],
                   input=raw, check=True)
    print(f"  {name+'.ogg':22s} {os.path.getsize(out)/1024:6.1f} KB  {len(x)/RATE:5.2f}s")


def trim_silence(x, floor=0.02):
    """Corta as pontas mudas POR ENERGIA — o numero nao vem do olho na forma de onda."""
    loud = np.abs(x) > floor
    if not loud.any():
        return x
    return x[np.argmax(loud):len(x) - np.argmax(loud[::-1])]


def seamless(x, fade):
    """
    Cruza a cauda com a cabeca e joga a cauda fora: o fim entrega no comeco sem
    clique. E o mesmo truque do tape_static, que foi feito para tocar sem parar.
    """
    n = int(fade * RATE)
    if n <= 0 or len(x) < 2 * n:
        return x
    ramp = np.linspace(0.0, 1.0, n, dtype=np.float32)
    y = x[:-n].copy()
    y[:n] = x[-n:] * (1.0 - ramp) + x[:n] * ramp
    return y


def build_loop(name, rel, start, seconds, fade):
    x = decode(rel)
    if x is None:
        return
    x = trim_silence(x)[int(start * RATE):][:int(seconds * RATE)]
    encode(name, seamless(x, fade))


def build_step():
    """
    O passo ALTO do Cara Cinza. A gravacao tem varios passos em cima de ruido; medi
    o envelope e ela tem uma batida forte de verdade (pico 0.62, entre 0.68s e
    1.12s) e uma fraca. Recorto a forte com folga, e o peso vem do pitch baixo no
    jogo — no arquivo o som fica natural, para poder ajustar sem re-exportar.
    """
    name, start, end = "greyface_step", STEP[1], STEP[2]
    x = decode(STEP[0])
    if x is None:
        return
    x = x[int(start * RATE):int(end * RATE)].copy()
    # Fade curtissimo na entrada (so mata o clique) e mais folgado na saida.
    a, b = int(0.005 * RATE), int(0.06 * RATE)
    x[:a] *= np.linspace(0.0, 1.0, a, dtype=np.float32)
    x[-b:] *= np.linspace(1.0, 0.0, b, dtype=np.float32)
    peak = float(np.max(np.abs(x)))
    if peak > 0:
        x *= 0.95 / peak          # passo tem que ser ALTO: normaliza no teto
    encode(name, x)


def convert(name, rel, seconds):
    src = os.path.join(SRC, rel.replace("/", os.sep))
    if not os.path.exists(src):
        print("  !! sumiu:", rel)
        return False

    out = os.path.join(DEST, name + ".ogg")
    cmd = [
        imageio_ffmpeg.get_ffmpeg_exe(), "-y", "-i", src,
        "-t", str(seconds),
        "-ac", "1",            # mono: som posicional no jogo e mono de qualquer jeito
        "-ar", "44100",
        "-c:a", "libvorbis", "-q:a", "3",
        # Evita o corte seco no fim de um arquivo que foi truncado no meio.
        "-af", "afade=t=out:st=%.2f:d=0.35" % max(0.1, seconds - 0.35),
        out,
    ]
    subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    print(f"  {name+'.ogg':22s} {os.path.getsize(out)/1024:6.1f} KB  <- {os.path.basename(src)}")
    return True


def convert_music(name, rel):
    """Trilha de dimensao: estereo, inteira, so re-embalada em Vorbis."""
    src = os.path.join(SRC, rel.replace("/", os.sep))
    if not os.path.exists(src):
        print("  !! sumiu:", rel)
        return False
    out = os.path.join(DEST, name + ".ogg")
    subprocess.run([imageio_ffmpeg.get_ffmpeg_exe(), "-y", "-v", "quiet", "-i", src,
                    "-ac", "2", "-ar", "44100", "-c:a", "libvorbis", "-q:a", "4", out],
                   check=True)
    print(f"  {name+'.ogg':22s} {os.path.getsize(out)/1024:6.1f} KB  <- {os.path.basename(src)}")
    return True


def main():
    os.makedirs(DEST, exist_ok=True)
    print("avistamento:")
    for name, (rel, secs) in SIGHT.items():
        convert(name, rel, secs)
    print("\nfundo:")
    for name, (rel, secs) in DREAD.items():
        convert(name, rel, secs)

    print("\nloops (trilha da cacada e zumbido do ofanim):")
    for name, (rel, start, secs, fade) in LOOPS.items():
        build_loop(name, rel, start, secs, fade)

    print("\npasso:")
    build_step()

    print("\ntrilha das dimensoes:")
    for name, rel in DIM_MUSIC.items():
        convert_music(name, rel)


if __name__ == "__main__":
    main()
