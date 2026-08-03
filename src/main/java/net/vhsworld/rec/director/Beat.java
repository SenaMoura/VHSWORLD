package net.vhsworld.rec.director;

/**
 * UMA BATIDA: qualquer coisa que o mod queira fazer com o jogador.
 *
 * O mod inteiro sempre teve batidas — a criatura que nasce, o som que vem do nada — mas
 * cada uma tinha o proprio relogio e nenhuma sabia da outra. O resultado e o defeito que
 * este pacote existe para corrigir: tres coisas acontecendo no mesmo minuto e vinte
 * minutos em que nada acontece, sem que ninguem tenha decidido isso. Terror e RITMO, e
 * ritmo e uma coisa que so existe se alguem estiver contando o compasso.
 *
 * Agora ninguem acontece sozinho: toda batida PEDE ao Diretor. Estes tres numeros sao a
 * conversa inteira.
 */
public enum Beat {

    /**
     * O som que vem do nada.
     *
     * Piso baixo e teto alto: e a batida barata, a que pode acontecer com o jogador ja
     * meio tenso, porque som nao cobra nada dele — so cobra atencao.
     */
    NOISE(25, 0.75f, 0.10f, 0.025f),

    /**
     * A AUSENCIA: o mundo tira uma coisa que o jogador poe.
     *
     * A tocha que ele fincou nao esta mais la. A porta que ele fechou esta aberta. Nao
     * ha criatura, nao ha som, nao ha nada para apontar — e por isso ela e a batida mais
     * barata do mod e provavelmente a que mais assusta por linha escrita.
     *
     * ⚠️ ELA NAO E UM SUSTO, E UMA SEMENTE. O custo baixo diz isso: ela quase nao gasta
     * o silencio, porque o efeito dela nao acontece no instante em que ela acontece —
     * acontece minutos depois, quando o jogador repara. E a unica batida do mod cujo
     * tempo de detonacao quem escolhe e o jogador.
     */
    ABSENCE(120, 0.50f, 0.15f, 0.010f),

    /**
     * Uma criatura do mod quer nascer perto do jogador.
     *
     * ⚠️ O TETO BAIXO (0.35) E A CORRECAO MAIS IMPORTANTE DESTE ARQUIVO. Antes, o spawn
     * era vanilla puro: o jogo sorteia um ponto valido e poe o bicho la, e nao existe no
     * jogo nenhuma nocao de "ja tem um te perseguindo agora". Por isso as criaturas se
     * empilhavam — e monstro empilhado nao assusta, vira multidao, e multidao e um
     * problema de combate, nao de terror. Com este teto, enquanto houver qualquer coisa
     * nossa perto de voce, NENHUMA outra nasce. Uma de cada vez, sempre.
     */
    SPAWN(90, 0.35f, 0.55f, 0.005f),

    /**
     * A TRILHA — agora um acontecimento, nao um tapete.
     *
     * ⚠️ POR QUE A MUSICA VIROU BATIDA. Ate a v1.76.3 os biomas do mod tinham
     * `min_delay: 0, max_delay: 0` — no overworld e nas quinze dimensoes. Isso faz o
     * MusicManager emendar uma faixa na outra para sempre, e foi medido no jogo: "quando
     * uma acabava a outra comecava".
     *
     * O estrago nao e a musica ser demais. E que o PRODUTO DO DIRETOR E SILENCIO
     * FABRICADO: ele nega batidas para comprar dois minutos de vazio, e a trilha enchia
     * cada segundo desse vazio. O trabalho inteiro dele era inaudivel — as batidas
     * chegavam, o intervalo entre elas nao. E o silencio e o quarto estado das leis de
     * dimensao, escrito pelo proprio mod e desligado por configuracao desde a v1.69.0.
     *
     * Piso longo e TETO BAIXISSIMO (0.20): musica so em calmaria de verdade. E ela nao
     * custa pressao (0.0) porque ouvir uma faixa nao deixa ninguem tenso — ela so ocupa
     * o proprio relogio.
     */
    MUSIC(240, 0.20f, 0.0f, 0.006f);

    private final int floorSeconds;
    private final float ceiling;
    private final float cost;
    private final float urgePerSecond;

    Beat(int floorSeconds, float ceiling, float cost, float urgePerSecond) {
        this.floorSeconds = floorSeconds;
        this.ceiling = ceiling;
        this.cost = cost;
        this.urgePerSecond = urgePerSecond;
    }

    /**
     * O PISO DE SILENCIO: quantos ticks tem que ter passado desde a ultima batida
     * QUALQUER antes desta poder acontecer.
     *
     * E o piso que compra o vazio. Sem ele o Diretor viraria so um sorteador melhor, e o
     * problema nunca foi o sorteio — era nao existir intervalo obrigatorio nenhum.
     */
    public int floorTicks() {
        return floorSeconds * 20;
    }

    /**
     * Acima desta pressao o Diretor NEGA esta batida.
     *
     * Negar e a metade do trabalho dele, e a metade que nao existia em lugar nenhum do
     * mod. Quando o jogador ja esta com medo, a coisa mais assustadora que se pode fazer
     * e nada.
     */
    public float ceiling() {
        return ceiling;
    }

    /** Quanta pressao esta batida deixa no jogador depois de acontecer. */
    public float cost() {
        return cost;
    }

    /**
     * ⚠️ COMO AS BATIDAS SE ATRAPALHAM: pela PRESSAO, nunca pelo relogio.
     *
     * Cada batida espera so o proprio piso (o relogio dela, ver Tension.quiet). O que
     * impede duas coisas de acontecerem juntas e o TETO: depois de um spawn a pressao vai
     * a 0.55, que passa do teto da ausencia (0.50), e a ausencia espera sozinha ate a
     * criatura ir embora e a pressao cair.
     *
     * Isso parece um detalhe e custou tres versoes erradas, todas com o mesmo sintoma
     * ("nao acontece nada") e nenhuma apontando para o Diretor. A historia inteira esta em
     * Tension.quiet(Beat) — leia antes de acrescentar batida nova.
     */
    public float weight() {
        return cost;
    }

    /**
     * Chance POR SEGUNDO de o Diretor iniciar esta batida sozinho, ja com o piso vencido.
     * Sobe conforme o silencio passa do piso (ver Director.wants) — e a curva que garante
     * que a calmaria sempre quebra, sem que a hora seja previsivel.
     *
     * ⚠️ ZERO QUER DIZER "ESTA BATIDA NUNCA PARTE DO DIRETOR", e a distincao aqui e um
     * erro de unidade que quase entrou. NOISE o Diretor inicia: ele acorda duas vezes por
     * segundo e sorteia, entao a chance so faz sentido como taxa por tempo.
     *
     * ⚠️ O SPAWN ERA ZERO ATE A v1.79.0, E O MOTIVO DE TER DEIXADO DE SER IMPORTA. O
     * argumento antigo estava certo: quem tentava era o motor do vanilla, na hora que ele
     * quisesse, e sortear aqui faria a frequencia das criaturas depender de quantas vezes o
     * vanilla resolveu tentar naquele minuto — um numero que ninguem controla. O que mudou
     * nao foi o argumento, foi o fato: com o Staging, o Diretor tambem COLOCA criatura por
     * conta propria, e batida que ele inicia precisa de taxa.
     *
     * As duas portas continuam separadas e e por isso que isto e seguro: o motor do vanilla
     * so passa por `allow` (piso e teto, sem dado), e so a colocacao do Diretor consulta
     * `wants`. O vanilla nunca vira dono da frequencia. E as duas dividem ESTE relogio e
     * ESTA pressao de proposito — se cada uma tivesse a sua, o Diretor poderia colocar um
     * bicho a vinte blocos logo depois de o vanilla ter posto outro, que e o empilhamento
     * que o teto baixo existe para impedir.
     */
    public float urgePerSecond() {
        return urgePerSecond;
    }
}
