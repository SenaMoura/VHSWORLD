# Harness headless das dimensoes

Confere as dimensoes **sem abrir o Minecraft**. Existe porque entrar no jogo para
descobrir que uma peca ficou 1 bloco fora de lugar custa 5 minutos por tentativa, e
porque varios defeitos desta parte do mod nao dao erro nenhum — dao um buraco no chao
que so se acha pisando nele.

## O que cada um mede

| arquivo | o que mede |
|---|---|
| `DimCheck.java` | as pecas de verdade, lidas do `.bin`: se cabem, se encostam, se o pulo e pulavel, se o spawn tem chao |
| `FieldCheck.java` | passa cada `dimension_type/*.json` pelo **codec do proprio jogo** |
| `BiomeCheck.java` | passa cada `worldgen/biome/*.json` pelo **codec do proprio jogo** |
| `../check_dimensions.py` | se cada dimensao esta ligada nas 6 pontas (stem, type, bioma, codec, fita, aba) |

## Como rodar

```bash
# 1. os recursos tem que estar no build, senao o PieceSet nao acha os .bin
./gradlew processResources --offline

# 2. despeja o classpath de runtime (init script, nao mexe no build.gradle)
./gradlew -I tools/harness/classpath.gradle dumpRuntimeClasspath --offline

# 3. compila e roda (JDK 17 — o `java` do PATH pode ser 8)
JDK="$LOCALAPPDATA/Programs/Eclipse Adoptium/jdk-17.0.18.8-hotspot/bin"
CP=$(cat build/runtime-classpath.txt)
"$JDK/javac.exe" -encoding UTF-8 -nowarn -cp "$CP" -d build/harness tools/harness/*.java
"$JDK/java.exe" -Dfile.encoding=UTF-8 -cp "build/harness;$CP" DimCheck
```

## As pegadinhas que custaram tempo

- **A receita do bootstrap**: `SharedConstants.setVersion(DetectedVersion.BUILT_IN)` e
  depois `Bootstrap.bootStrap()` **dentro de try/catch**. Ele enche os registros e SO
  DEPOIS chama `NetworkHooks.init`, que morre fora do launcher — quando explodir ali, os
  blocos ja estao todos de pe. Conferir com `Blocks.STONE.defaultBlockState().isAir()`.
- **`processResources` antes de tudo.** Sem ele o `PieceSet` le do classpath e nao acha os
  `.bin` novos; os 18 testes reprovam por um motivo que nao tem nada a ver com eles.
- **`-encoding UTF-8` no javac E `-Dfile.encoding=UTF-8` no java.** Sem o primeiro, os
  comentarios com acento nao compilam no Windows.
- **`java` do PATH e 8.** Usar o do Adoptium 17, o mesmo do `javac`.
- **`./gradlew runServer` NAO funciona neste repo** (falha antes de ligar, em
  `MixinInitialisationError: recmod.mixins.json`). Nao e das dimensoes — e anterior a
  elas. Entao o teste de ponta a ponta continua sendo entrar no jogo.

## ⚠️ A licao de metodo

Dois testes deste harness **passaram medindo a coisa errada**, e os dois passaram:

1. "a porta esta na parede leste" procurava o vao na borda da PECA. A borda da peca e a
   cerca do quintal, que e quase toda ar — ele achava "porta" em z=1 e aprovava. A parede
   da casa e `x=13`, e a porta esta em `z=4..5`.
2. "ha bloco para se ficar de pe" no topo da torre do submarino achou bloco: um
   **alcapao**. Fechado e uma laje de tres pixels; aberto nao ha nada ali. "Tem bloco" nao
   era a pergunta; "e chao" era.

Antes de acreditar num numero de teste, olhar o que ele mediu.
