package net.vhsworld.rec.escape;

/**
 * Como se sai de uma dimensao.
 *
 * ============================ A REGRA QUE ISTO CRIA ============================
 *
 * Ate a v1.69.0 a fita era a ida E a volta: usar a mesma fita dentro da dimensao
 * devolvia o jogador. Isso acabou. A fita agora e SO IDA, e cada dimensao tem uma saida
 * propria, que e o unico caminho de volta.
 *
 * ⚠️ E UMA INVERSAO DO JOGO INTEIRO, e vale entender o que ela compra. Com a fita no
 * bolso, entrar numa dimensao nao custava nada: era uma visita, e dava para desistir a
 * qualquer segundo. O medo tinha botao de pausa. Sem ela, entrar e uma APOSTA — o jogador
 * so sai de la sabendo alguma coisa sobre o lugar que ele nao sabia ao entrar.
 *
 * ⚠️ E O QUE ELA COBRA: uma dimensao sem saida montada e um jogador preso para sempre.
 * Nao existe mais o "volta no lugar errado, que e melhor do que ficar preso" que o
 * DimensionTapeItem podia se dar ao luxo de ter. Por isso `exitAnchor` nao tem
 * implementacao padrao no `DimSpawn`: gerador que nao souber dizer onde cabe a saida NAO
 * COMPILA. O compilador e a unica coisa que nao esquece.
 *
 * ============================ A MUDANCA DE PLANOS (v1.72.0) ============================
 *
 * ⚠️ ERAM CINCO METODOS. Sao dois.
 *
 * O `fuga.rtf` trazia quatro (radio, videocassete, espelho, camara escura) e a TRAIN
 * ganhou um quinto (a porta na linha). Estavam todos implementados e funcionando quando o
 * Pedro modelou a entidade do ESPELHO — e a entidade mudou o problema. Um espelho de dois
 * metros e meio por oito, preto, com tres olhos desenhados, que fica encarando de volta,
 * nao e mais um aparelho numa sala: e uma CRIATURA que por acaso e a saida.
 *
 * A decisao dele foi que ela vale mais do que os outros quatro metodos somados, e ela
 * vale. O que os quatro tinham de bom era variedade; o que se perdia era identidade — a
 * saida de uma dimensao de terror nao devia ser um quebra-cabeca de radio, devia ser uma
 * coisa que a gente tem medo de olhar.
 *
 * RADIO, EJECT e DARKROOM foram removidos, junto com as salas, os aparelhos e os itens
 * deles. Nao ha codigo morto sobrando de proposito: bloco registrado que nada gera vira
 * enfeite no criativo que promete uma mecanica que nao existe.
 */
public enum ExitMethod {

    /**
     * O ESPELHO. Treze das quinze dimensoes.
     *
     * Ele fica parado num ponto fixo, e a regra e nao encara-lo. Quem olha perde sanidade
     * depressa, leva o susto na tela e recebe um aviso; quem insiste e devolvido ao inicio
     * da dimensao. Para sair, e preciso chegar nele SEM olhar — e encostar.
     *
     * ⚠️ E A UNICA SAIDA DO MOD QUE PUNE A CURIOSIDADE. Todo o resto do VHSWORLD ensina o
     * jogador a OLHAR: a camera e o verbo do mod, o flash revela, a foto cataloga, e o
     * Ofanim so anda quando nao se olha para ele. Este e o unico lugar em que olhar e o
     * erro — e e por isso que ele funciona como fim de jornada. A dimensao inteira treinou
     * o jogador a fazer exatamente a coisa que agora o manda de volta ao comeco.
     */
    MIRROR,

    /**
     * A PORTA NA LINHA. TRAIN e PARKOURLAND.
     *
     * Anda-se. Depois de um tanto de caminhada — e so caminhada conta, ficar parado nao
     * adianta — uma porta nasce sozinha mais adiante, no caminho, na direcao em que se
     * estava indo.
     *
     * ⚠️ E O UNICO METODO QUE NAO TEM LUGAR. O espelho esta em algum ponto do mapa e tem
     * que ser ACHADO; este nao esta em lugar nenhum ate existir. E isso e o que o torna
     * certo para as duas dimensoes que o usam, que sao as duas em que nao ha o que
     * procurar: a TRAIN e uma reta entre paredoes intransponiveis e a PARKOURLAND e uma
     * torre fechada e finita. Nas duas, a unica coisa que se pode fazer e avancar — e o
     * metodo cobra exatamente essa coisa.
     */
    DOOR
}
