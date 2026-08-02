# O DIRETOR — terror é ritmo, não conteúdo

> Documento de direção. Escrito em 2026-08-02, depois de o projeto ser parado com o
> diagnóstico de que estava "virando slop": muitas dimensões, entidades e trilha, e
> nenhum efeito de terror psicológico.

---

## 1. O diagnóstico, provado no código

**7 entidades, 1 input.** Homem de Pedra, Ofanim, Static Watcher, Silhueta Invertida,
Crawler Void, Shade Segment e Anomalias. As *intenções* de design são distintas e estão
escritas — o Observador é o avesso do Homem de Pedra, o Rastejo lê `containerMenu`/`swinging`
(reage a você estar **ocupado**), a Sombra é travada por **luz**. Mas as duas exceções não
substituem o olhar, empilham em cima dele:

```java
ShadeSegmentEntity.computeHeld()  →  luz > nível  OU  Gaze.seenByAny(...)
CrawlerVoidEntity.mayAdvance()    →  ocupado      OU  !Gaze.sees(...)
```

O que chega às **mãos do jogador** é sempre o mesmo gesto. Consequências:

1. **O repertório nunca cresce.** Da segunda criatura em diante não há verbo novo — a
   novidade fica por conta do modelo e da textura.
2. **As regras se anulam.** Olhar congela o Homem de Pedra, alimenta o Observador, para o
   Rastejo, prende a Sombra. O jogador não forma instinto: ele primeiro **identifica a
   silhueta** e depois aplica a regra decorada. O medo desaba num problema de reconhecimento.
3. **Nenhuma cobra nada.** Todas se resolvem virando a cabeça: 0,2 s, custo zero, sem escolha.
   A Sombra é a única que cobra recurso (tocha, carvão) — **e é por isso que é a melhor.**

**Nada conversava com nada.** Cada mecânica tinha relógio próprio: o som ambiente sorteava um
intervalo, o spawn era vanilla puro, cada criatura decidia sozinha. Daí 3 sustos num minuto e
20 minutos de nada, sem que ninguém tivesse decidido isso.

**A sanidade era enfeite.** Client-side, num JSON global do `.minecraft` (nem por mundo), caía
só ao revelar foto. O servidor não sabia que existia; nenhuma entidade lia.

---

## 2. A regra da casa

> **Nada no mod acontece sozinho com o jogador.**
> Pede-se `Director.allow(player, Beat)`, e quando acontece, avisa-se com `Director.report(...)`.

Mecânica que não pedir volta a ser um relógio solto e desfaz o sistema inteiro.

O Diretor faz duas coisas que nenhum sistema fazia:

- **NEGAR.** Recusa tudo quando o jogador já está tenso. A queda lenta da pressão em `Tension`
  (~80 s de 1.0 até 0.35) é o que **compra o vazio depois do susto**. Encurtar esse número é
  desfazer o Diretor.
- **CORRELACIONAR.** O som toca por motivo: ou há criatura por perto (vem da direção dela,
  torta, nunca na distância real) ou o silêncio passou do limite e o som é **mentira
  deliberada**. O jogador não distingue os dois — é isso que o faz voltar a escutar.
  *A mentira é o que faz a verdade funcionar.*

---

## 3. As armadilhas já pagas (leia antes de acrescentar batida)

### 3.1 A regra do silêncio — 3 versões erradas

| versão | regra | como morreu |
|---|---|---|
| v1 | um relógio só, zerado por qualquer batida | o ruído (25 s) matava o spawn (90 s): **nenhuma criatura nasceria nunca** |
| v2 | dois relógios, `heavy()` binário | a AUSÊNCIA é barata (0.15) mas espera 120 s — caiu no lado errado e morreu igual |
| v3 | relógio = mínimo entre batidas de peso ≥ | o SPAWN, o mais pesado, zerava o relógio de **todo mundo** |

**A regra certa:** cada batida lê **só o próprio relógio** (`Tension.quiet(Beat)`), e a
conversa entre elas acontece pela **pressão**. Relógio responde *"há quanto tempo isto não
acontece"*; pressão responde *"o jogador aguenta mais alguma coisa agora?"*.

⚠️ O sintoma no jogo é sempre o mesmo — **"não acontece nada"** — e nunca aponta para o Diretor.

### 3.2 O Diretor modela a EXPERIÊNCIA, não os eventos do mundo

Medido em jogo: um `shade_segment` nasceu a **114 blocos** e cobrou os 0.55 de pressão
inteiros — bloqueando a ausência (teto 0.50), cortando a trilha (corte em 0.45) e travando o
próximo spawn, tudo por causa de algo que o jogador não tinha como ver, ouvir ou suspeitar.

Corrigido com `Director.report(player, beat, costScale)` + `Director.perceived(distance)`.
⚠️ O **relógio zera inteiro mesmo assim**: cadência e pressão respondem perguntas diferentes,
e só a segunda depende de o jogador ter percebido.

### 3.3 `allow` não pode ter dado

Quem pergunta pelo SPAWN é o motor de spawn do vanilla, em ritmo próprio e incontrolável.
Sorteio no `allow` faria o vanilla virar dono da frequência. Permissão é determinística (piso
+ teto); o dado só existe em `wants`, para batida que o Diretor **inicia**
(`Beat.urgePerSecond`, taxa **por segundo**, convertida pelo `SAMPLE_TICKS`).

### 3.4 Instrumentação que não separa causas é um "não sei" mais comprido

`nao achou alvo (rastro=42)` foi tão inútil quanto o silêncio que veio substituir. Hoje conta
por motivo: `perto / longe / visao / mudou / outro`.

---

## 4. A trilha — o achado que ninguém procurava

Todos os biomas (overworld + as 15 dimensões) tinham:

```json
"min_delay": 0, "max_delay": 0, "replace_current_music": true
```

Uma faixa emendava na outra, para sempre. **O produto do Diretor é silêncio fabricado**, e a
trilha preenchia cada segundo dele — o trabalho inteiro dele era inaudível. As batidas
chegavam; o intervalo entre elas, não. E o silêncio é o **quarto estado** das leis de
dimensão, escrito pelo próprio projeto e desligado por configuração desde a v1.69.0.

Hoje a música é `Beat.MUSIC` (piso 240 s, teto 0.20) e **é cortada quando a pressão sobe**
(`directorMusicCutPressure`) — a faixa some porque algo se aproximou, e o jogador não sabe por
quê. É a ferramenta de terror mais barata do mod.

⚠️ **Abrir os intervalos no JSON não bastaria:** o `MusicManager` nasce com
`nextSongDelay = 100` ticks, então a 1ª faixa entra 5 s depois de carregar o mundo por mais
alto que seja o `min_delay`. Por isso `MusicDirector` cancela no `PlaySoundEvent` toda música
`SoundSource.MUSIC` que não seja a nossa. Jukebox (`RECORDS`) fica de fora: fazer barulho de
propósito é decisão do jogador.

---

## 5. A AUSÊNCIA — e o resultado do primeiro teste real

O mundo mexe, pelas costas, em coisa que **você** colocou: tocha some, porta abre. As três
travas que fazem ser medo e não sacanagem: **é SEU** (`PlacementTrace`), **é PELAS COSTAS**
(fora do cone, 8–64 blocos), **é BARATO DE DESFAZER** (só tocha e porta — nunca baú nem
construção; perda de progresso vira raiva, não medo).

A prova só existe pelo **visor** (lente na mão). Nada avisa: a marca fica muda para sempre se
o jogador não desconfiar sozinho — é o que separa isso de um detector de fantasma, que dá
objetivo.

### O teste (2026-08-02, criativo)

Funcionou tecnicamente de ponta a ponta — tocha removida a 22,5 blocos, marca no visor.

**E o jogador não notou nada.**

Duas leituras, ambas verdadeiras:

1. **O teste era incapaz de funcionar.** 42 tochas fincadas em sequência em criativo não são
   objetos, são confete. Sem identidade não há o que sentir falta.
2. **Mas o defeito é real: a ausência não tem ACONTECIMENTO.** Ela muda o estado do mundo e
   depende de o jogador lembrar espontaneamente de como o mundo estava — e jogador de
   Minecraft não inventaria bloco. O **resultado padrão da mecânica é "nada aconteceu"**: um
   modo de falha silencioso que é o desfecho mais provável.

### O que falta

- **Um gancho sensorial que faça virar.** Não um aviso — a tocha deve **se apagar com o som de
  apagar, atrás de você**. Você ouve, vira, não há nada. Mas agora *está olhando*. É a
  diferença entre um estado que mudou e uma coisa que aconteceu.
- **Alvo único e que carregue peso.** A tocha do corredor, a porta da base, a cama — coisa
  cuja falta muda **o que você consegue fazer**, não só o que você vê.

⚠️ **Isto é um alerta para a direção da fita inteira.** A tese "a câmera existe porque o mundo
mente e você vai querer prova" pressupõe que o jogador **detecta** a mentira. A revisão de fita
depende da mesma suposição. Ela não é mais gratuita: toda mecânica dessa família precisa provar
que produz o momento de virar a cabeça, e nenhuma pode contar com a memória do jogador.

---

## 6. Pendências

- **Gancho sensorial + alvo único** na ausência, e refazer o teste em survival.
- **Spawn natural nasce a 110–125 blocos** (medidos 114, 124, 116 — o vanilla sorteia numa
  casca de ~24–128 e usa o limite externo). Isso torna o `SPAWN` como batida de ritmo quase
  teórico: o bicho tem que andar 100+ blocos, e Homem de Pedra/anomalias não são feitos pra
  isso. Se encontro faz parte do compasso, o Diretor vai ter que **colocar** a criatura.
- **Quebrar a monocultura do olhar:** eixos novos (som que você faz, permanência, posse,
  memória) — cada criatura taxando uma coisa diferente que o jogador valoriza.
- **Auditoria de corte** das 15 dimensões.
- **Ligar a sanidade ao Diretor** (hoje ela é client-side e ninguém lê).
- Tirar o **pulso** de diagnóstico quando o ritmo estiver ajustado.
