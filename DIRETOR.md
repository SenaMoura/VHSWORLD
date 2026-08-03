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

### A correção (v1.78.0)

**O gancho sensorial.** A tocha agora **se apaga com o som de apagar** (`Absence.hiss`). Você
ouve, vira, não há nada — mas agora *está olhando*. Três decisões dentro disso:

- **Som não é prova.** O comentário que estava no código dizia "qualquer efeito seria uma prova,
  e a mecânica depende de não haver prova". A frase estava certa e a conclusão estava errada.
  Partícula fica, drop fica, marca no chão fica — todos podem ser apontados depois, inclusive
  para outra pessoa. O som **já acabou quando você vira a cabeça**: ele não deixa nada além da
  sua palavra, que é o material desta mecânica. Por isso: som sim, partícula não.
- **Não é aviso.** Pela mesma razão que o visor não é detector. Aviso aponta pro mod e entrega
  tarefa. Uma chama morrendo é o som que aquele objeto faz — pode ter sido goteira, pode ter
  sido nada. Só que vem de um lugar onde agora não há mais nada.
- **Só o dono da fita ouve** (`ClientboundSoundPacket` direto, não `level.playSound`). Perguntar
  "você ouviu isso?" e receber "não" é melhor que qualquer efeito.
- ⚠️ **Volume sobe com a distância** porque no Minecraft volume *é* alcance (raio ≈ 16 × volume).
  Sem essa conta, tudo acima de 16 blocos sumiria em silêncio e o gancho existiria só no código
  — que é, palavra por palavra, o defeito original.

**O alvo único.** O `Collections.shuffle` + primeiro que servir achava um bloco *válido*; nunca
achava um bloco de que o jogador **sentiria falta**. Agora pontua por **solidão** (distância até
a coisa mais próxima que você colocou) e desempata por **idade** (`Mark.placedAt`):

- A solidão é também o único jeito honesto de a falta **carregar peso**: se era a única luz dali,
  sumir muda *o que você consegue fazer*. Tirar a 14ª de uma fileira não apaga nada.
- Idade porque marca precisa **envelhecer** para virar lembrança. Tocha de 30 s atrás ainda está
  na mão do jogador — sumir com ela não produz dúvida, produz a conclusão correta de que foi o mod.
- ⚠️ **Cama/baú/fornalha carregariam peso de verdade e continuam vetados.** A terceira trava
  ganha: perda de progresso vira raiva. O peso sai da **escolha** do alvo, nunca do valor dele.
- ⚠️ O piso de solidão (6 blocos) é **frouxo de propósito**. Entre os que passam, a ausência já
  escolhe o mais sozinho — o filtro só precisa matar a fileira colada. Apertar demais recria
  "não acontece nada", o único sintoma que nunca aponta para o Diretor.

**O teto de distância caiu de 64 para 28** (`absenceHearingRange`): ausência que o jogador não
pode *ouvir* acontecer volta a ser a versão que falhou. Batida gasta que não vira nada é pior que
espera.

⚠️ **Isto é um alerta para a direção da fita inteira.** A tese "a câmera existe porque o mundo
mente e você vai querer prova" pressupõe que o jogador **detecta** a mentira. A revisão de fita
depende da mesma suposição. Ela não é mais gratuita: toda mecânica dessa família precisa provar
que produz o momento de virar a cabeça, e nenhuma pode contar com a memória do jogador.

### O segundo teste (2026-08-03) — ✅ PASSOU

Mesma mecânica, mesmo jogador, um som de diferença: *"escutei o barulho de tocha apagar e tive a
sensação de fato de que algo estava errado."*

Duas conclusões, e a segunda vale mais que a primeira:

1. **A ausência está validada como batida.** Ela produz o momento — sem entidade, sem vulto, sem
   dano, com um `setBlock` e um pacote de som.
2. **★ O DIAGNÓSTICO ESTAVA CERTO E É GENERALIZÁVEL.** O que separou o teste que falhou do que
   passou não foi conteúdo novo, nem mais efeito, nem ajuste de número: foi a mecânica passar a
   ter um **acontecimento perceptível**. A v1.76 e a v1.78 fazem *a mesma coisa com o mundo*. A
   diferença inteira mora no que chega ao jogador.

   Daí a pergunta que toda mecânica futura tem que responder ANTES de ser escrita: **qual é o
   segundo em que o jogador percebe que isto aconteceu?** Se a resposta for "quando ele reparar",
   a mecânica já falhou — só ainda não foi testada. O resultado padrão dela é "nada aconteceu",
   e esse modo de falha é silencioso: no jogo ele é indistinguível de estar quebrado.

---

## 6. A COLOCAÇÃO — o Diretor deixa de ser só censor (v1.79.0)

O `SpawnGate` podia **negar** um spawn; não podia **pedir** um. Enquanto só existisse a negativa,
o Diretor conseguia impedir o encontro errado e não conseguia produzir o certo.

O número que forçou isso: o spawn natural nasce a **110–125 blocos** (medidos 114, 124, 116 — o
vanilla sorteia numa casca de ~24–128 e na prática usa o limite externo). A batida **mais cara**
do Diretor gastava o compasso inteiro com algo que o jogador não tinha como ver, ouvir nem
suspeitar — e que ainda teria que caminhar cem blocos, coisa que o Homem de Pedra (congela quando
olhado) e a anomalia (nem anda) não fazem nunca.

`Staging` coloca a criatura a **18–40 blocos, no arco de trás** (meia volta a partir do olhar,
±90°, calculado direto em vez de sortear e rejeitar). É a mesma lição da ausência aplicada à
batida pesada: perto o bastante para importar, atrás o bastante para não ser vista nascendo.

- **As regras de spawn continuam valendo** (`SpawnPlacements`). Furar isso não daria mais
  encontro, daria encontro sem sentido — o Observador espremido numa caverna é a criatura jogada
  fora.
- **A lista de quem pode aparecer vem do BIOMA**, não de uma lista própria. Os `biome_modifier` já
  decidem quem nasce onde, e as 15 dimensões vão continuar decidindo. Lista própria criaria uma
  segunda verdade que fura a primeira em silêncio. A colocação muda **onde e quando**, nunca
  **o que é permitido**.
- **`MobSpawnType.EVENT`**, não `NATURAL` — senão o Diretor pediria licença a si mesmo e seria
  negado pelo próprio teto que acabou de conferir.
- **Já virada para o jogador.** O susto é ela já estar te encarando quando você vira, não ela
  reparar em você depois.

### ★ O racionamento do elenco — a objeção do Pedro, e ela estava certa

*"O problema é que não temos criaturas suficientes pro mod."*

O mod tem **6 criaturas colocáveis**, e hoje isso não aparece porque **o defeito de cima esconde o
elenco**: bicho que nasce a 114 blocos e nunca chega não repete, porque não acontece. No instante
em que o Diretor passa a colocar a 20 blocos, o mesmo elenco é consumido de verdade e "poucas
criaturas" deixa de ser opinião e vira sintoma. Construir a colocação sem racionar seria trocar um
defeito invisível por um visível.

⚠️ **O racionamento NÃO diminui a frequência — ele força a variedade.** É a distinção que faz a
coisa valer. O Diretor continua colocando na mesma cadência; ele só não pode repetir o mesmo bicho
dentro da janela. Com 6 tipos e 10 min de janela cabem 6 colocações nesses 10 min, então o teto do
racionamento fica praticamente em cima do piso da batida (90 s) e quase nunca silencia o mod.

E o elenco é percorrido **por quem está há mais tempo sem aparecer**, não sorteado: sorteio com
poucos tipos produz repetição vizinha por acaso, e dois Homens de Pedra seguidos é exatamente o
resultado que isto existe para impedir.

O que se ganha é o que **nenhuma quantidade de criatura nova compra**: a mesma criatura não vira
fauna. Bicho que aparece toda noite deixa de ser aparição e vira animal do bioma — e aí o problema
passa a ser de combate, que o jogador sabe resolver.

⚠️ **Isso não fecha a questão do elenco, adia com juros.** Criatura nova continua valendo — mas só
**com verbo novo**, nunca com o mesmo olhar (ver §1). De 6 para 12 no mesmo eixo é exatamente o que
se chamou de slop.

---

## 7. Pendências

- **Testar a colocação in-game.** As perguntas: ela acha lugar (ver `colocacao sem lugar` no log)?
  Aparece atrás mesmo? 10 min de janela sufoca ou respira? E a que já sabemos que vai doer: **6
  criaturas bastam quando elas de fato aparecem?**
- **Quebrar a monocultura do olhar:** eixos novos (som que você faz, permanência, posse,
  memória) — cada criatura taxando uma coisa diferente que o jogador valoriza.
- **Auditoria de corte** das 15 dimensões.
- **`recmod.tear.sense` é um AVISO** (`RealityTearSense:95`) — "You feel something that you don't
  understand", na action bar, a cada 20 s enquanto houver um Rasgo de Realidade num raio de 10.
  É o oposto exato do que a ausência provou: aponta para o mod, chega sem ser pedida e entrega
  **objetivo** em vez de dúvida. Primeiro item da auditoria de corte — o gesto certo seria o do
  visor (a lente confirma se, e só se, o jogador desconfiar sozinho).
- **Ligar a sanidade ao Diretor** (hoje ela é client-side e ninguém lê).
- Tirar o **pulso** de diagnóstico quando o ritmo estiver ajustado.
