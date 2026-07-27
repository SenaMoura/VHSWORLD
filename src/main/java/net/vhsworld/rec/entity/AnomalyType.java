package net.vhsworld.rec.entity;

import net.minecraft.resources.ResourceLocation;
import net.vhsworld.rec.RECMod;

/**
 * O catalogo das anomalias 2D e — mais importante — COMO cada uma se deixa ver.
 *
 * A ideia que manda aqui: a anomalia esta SEMPRE no mundo. Ela nao aparece e some;
 * quem aparece e some e a sua capacidade de ve-la. Isso resolve de um jeito honesto
 * um problema que era feio de outro jeito: o album, o registro e a sanidade sao todos
 * client-side, e o mod nao tem canal de rede — se a "manifestacao" dependesse de o
 * servidor saber quantas fotos voce revelou, seria preciso inventar pacote e
 * capability so para isso. Aqui o servidor cuida do corpo e o cliente decide o que os
 * olhos alcancam, cada um com o que ja tem na mao.
 *
 * E, de quebra, e o que a lore diz: ela sempre esteve ali.
 */
public enum AnomalyType {

    /**
     * O ALTO. Anda no mundo como qualquer coisa que se ve. E o unico que o jogador
     * pode encontrar sem camera nenhuma — o mod precisa de pelo menos uma criatura
     * que exista para quem nunca fotografou nada.
     */
    TALL("tall", Visibility.ALWAYS, 5.2F, 0.320F, 180.0F),

    /**
     * A ARANHA. So existe na fita — ate voce insistir. Cada revelacao dela aproxima
     * o dia em que ela vai estar na sua frente sem camera nenhuma.
     *
     * Este e o coracao do sistema: o jogador aprende "essa aqui e inofensiva, so sai
     * na foto", relaxa, e continua revelando porque revelar e o verbo do jogo. A
     * curiosidade dele e o que a traz. Quando ela finalmente aparece, a culpa tem
     * dono — e por isso que o susto gruda, em vez de virar reclamacao de aleatorio.
     */
    SPIDER("spider_smile", Visibility.TAPE_THEN_REAL, 4.4F, 0.602F),

    /**
     * A DAS GARRAS. Anda no mundo como o Alto.
     *
     * Ela era TAPE_ONLY — a que nunca saia da fita, por mais que voce insistisse — e
     * existia como contrapeso: se TODA anomalia acabasse aparecendo, "so na fita" nao
     * significaria nada. O Pedro passou ela para ALWAYS em 2026-07-26, depois que ela
     * ganhou corpo 3D.
     *
     * ⚠️ Com isso NENHUMA anomalia e mais TAPE_ONLY. Quem sustenta a ideia de "existe
     * coisa que so a camera pega" passa a ser so a Aranha, que e TAPE_THEN_REAL — ou
     * seja, ate ela acaba saindo. Se um dia a fita precisar voltar a valer alguma coisa,
     * e aqui que se conserta: basta uma anomalia teimar em TAPE_ONLY.
     */
    CLAWS("claws_scream", Visibility.ALWAYS, 4.6F, 0.430F, 180.0F),

    // ------------------------------------------------------------------ as esculturas
    //
    // Pre-renderizadas no Blender a partir das malhas de verdade (tools/render_creatures.py):
    // 8 vistas em volta x 8 quadros de animacao numa folha so. E a tecnica do Doom, e ela
    // existe porque estas malhas tem dezenas de milhares de triangulos e o Minecraft nao
    // tem skinning de malha com ossos — vivas, nenhuma delas entraria.
    //
    // Eram seis; sobraram duas. As outras quatro sairam na v1.42.0 por escolha do Pedro:
    // criatura demais dilui o terror (licao 3 das autopsias das versoes antigas), e um
    // elenco pequeno e bem definido vale mais que um catalogo.

    /**
     * O CARA CINZA. A unica que CACA.
     *
     * Ela persegue de verdade — anda atras de voce e bate. Continua sem poder ser
     * morta, e isso e de proposito: e o pilar do mod, do qual nao se mata, se foge.
     * A saida nao e a espada, e sobreviver ate ela ir embora.
     */
    GREYFACE("greyface", Visibility.ALWAYS, Behavior.HUNTER, 3.8F, 8, 8, 3, 315.0F, 0.52F,
            -135.0F, 0x000000, 0.0F),

    /**
     * O OFANIM. Presenca, e a que mais se mexe: 228 quadros de animacao no original.
     * Fica onde nasceu, girando, ate voce chegar perto demais e ela nao estar mais la.
     *
     * GIGANTE (13 blocos, sete jogadores empilhados). Ela nao persegue, nao encosta e
     * nao faz nada — o tamanho E o ataque dela. Uma presenca da altura do jogador se
     * confunde com bicho; nesta escala ela nao cabe na tela quando voce esta perto, e
     * e isso que faz o jogador andar para tras sem pensar. A caixa de colisao segue
     * pequena (ver ModEntities); quem cresce e o cartaz.
     */
    // ⚠️ 0.30F escrito na mao, e nao a constante WALK: constante declarada depois das
    // constantes do enum e "illegal forward reference" — nao compila.
    OPHANIM("ophanim", Visibility.ALWAYS, Behavior.PRESENCE, 13.0F, 8, 8, 3, 90.0F, 0.30F,
            92.0F, 0xFFFFFF, 30.0F),

    /**
     * O VAZIO. A ultima escultura do elenco.
     *
     * Aranha esqueletica: costelas abertas, coluna que vira cauda, oito pernas compridas.
     * Nao usa a textura com que veio — ela foi PINTADA pela propria geometria
     * (tools/pack_mesh.py, `paint_bone`): o corpo e preto, e o osso aflora exatamente
     * onde a superficie ja se projetava, com o sangue na beirada em que a carne rompe.
     *
     * CACADORA por decisao do Pedro (2026-07-26), e a segunda do mod. Fica MUDA: o passo
     * e a trilha de perseguicao estao presos ao Cara Cinza, senao as duas chegariam com o
     * mesmo som e deixariam de ser criaturas diferentes.
     *
     * ⚠️ ALTURA, aqui, e so a vertical. Ela e uma aranha: o corpo dela e **1.86 vezes
     * mais comprido que alto**, entao 4.5 de altura sao mais de 9 blocos da cabeca a
     * ponta da cauda. Subir este numero cresce nas duas direcoes, rapido.
     *
     * ⚠️ Nao tem folha de sprites de reserva: se o .mesh sumir do jar ela fica sem
     * textura, em vez de virar cartaz como as outras.
     */
    VOID("void", Visibility.ALWAYS, Behavior.HUNTER, 4.5F, 1, 1, 1, 0.0F, 0.30F,
            0.0F, 0xFFFFFF, 0.0F);

    /** O que a criatura FAZ — separado do que ela deixa ver. */
    public enum Behavior {
        /** Fica onde nasceu. Nao persegue, nao toca, se desfaz se voce avancar. */
        PRESENCE,
        /** Persegue e bate. Nao se mata; escapa-se dela. */
        HUNTER
    }

    /** As tres maneiras de uma anomalia se deixar ver. */
    public enum Visibility {
        /** Sempre visivel, como qualquer criatura. */
        ALWAYS,
        /** So no clarao do flash e na foto revelada. Nunca a olho nu. */
        TAPE_ONLY,
        /** Comeca so na fita; passa a existir a olho nu depois de tantas revelacoes. */
        TAPE_THEN_REAL
    }

    /**
     * Velocidade de quem nao anda. Serve so para o atributo nascer com algum valor:
     * presenca nenhuma recebe goal, entao nenhuma delas usa isto.
     */
    private static final float WALK = 0.30F;

    private final String id;
    private final Visibility visibility;
    private final float height;
    private final float aspect;

    private final Behavior behavior;

    /**
     * Velocidade DESTA criatura.
     *
     * Existe porque o atributo MOVEMENT_SPEED vale para o EntityType inteiro, e as
     * seis anomalias sao o mesmo tipo. Cada uma grava o proprio numero na base do
     * atributo no primeiro tick (ver AnomalyEntity), quando o tipo dela ja e conhecido
     * — no construtor ainda nao e.
     */
    private final float speed;

    /** Colunas da folha: quantas vistas em volta da criatura. 1 = imagem unica. */
    private final int angles;

    /** Linhas da folha: quadros da animacao. 1 = parada. */
    private final int frames;

    /** Ticks que cada quadro fica na tela. */
    private final int ticksPerFrame;

    /**
     * Giro, em graus, entre a "frente" da criatura no jogo e a coluna 0 da folha.
     *
     * Existe porque cada escultura foi modelada encarando um lado diferente. Zerado,
     * as duas apareciam de COSTAS — a coluna 0 do render nao e a frente de nenhuma
     * delas. Achei o numero medindo a folha em vez de chutar: contei os pixels claros
     * na regiao da cabeca de cada coluna, porque o rosto palido e muito mais claro que
     * a nuca. Greyface: face nas colunas 6-7-0, centro em 7 -> 315 graus. Ophanim nao
     * tem frente (e um anel simetrico), mas nas colunas 0 e 4 o anel aparece de perfil
     * e vira uma barra chata; a vista cheia e a 2 -> 90 graus.
     *
     * Mudar aqui nao obriga a renderizar nada de novo.
     */
    private final float angleOffset;

    /**
     * Giro que poe a ESCULTURA encarando para onde a criatura anda. NaN = nao tem
     * escultura, e ela e desenhada como cartaz.
     *
     * Nao e o mesmo numero do angleOffset: aquele escolhe a coluna da folha de sprites,
     * este gira geometria. Os dois convivem porque a folha continua sendo a reserva —
     * se o .mesh sumir do jar, a criatura volta a ser cartaz em vez de sumir do mundo.
     *
     * COMO ESTES NUMEROS FORAM ACHADOS, e o que nao funcionou. A primeira tentativa
     * mediu o BRILHO da silhueta ("o rosto e a parte clara da cabeca") e errou feio: a
     * nuca do Cara Cinza e uma coroa de espetos palidos, mais clara que a cara dele, e
     * ele foi para o jogo perseguindo o jogador de costas. Olhar as vistas lado a lado
     * tambem nao resolveu — a cabeca dele e um emaranhado e 0 e 180 sao parecidos.
     *
     * O que resolveu foi parar de olhar para a imagem e medir a GEOMETRIA
     * (tools/preview_mesh.py):
     *   - `--feet`: para onde a animacao CAMINHA (a raiz se desloca) e para que lado o
     *     dedo do pe passa da canela. Duas testemunhas independentes, -135 e -113 graus.
     *   - `--axis` com PREVIEW_PUPIL: no Ofanim isola a PUPILA (a mancha escura dentro
     *     da textura do olho) e tira a media das normais dela — o olhar, literalmente.
     *     A esfera inteira nao serviria: as normais dela se cancelam.
     * O Minecraft espera o modelo encarando -Z, entao meshYaw = atan2(fx, -fz).
     */
    private final float meshYaw;

    /**
     * Cor que MULTIPLICA a textura da escultura. 0xFFFFFF deixa como esta.
     *
     * O Cara Cinza vai a 0x000000 por escolha do Pedro: preto puro apaga a pele, o metal
     * e a calca roxa e sobra a SILHUETA. Ganha-se com isso — o modelo realista, visto de
     * perto e com luz, entrega que e um boneco; a silhueta nao entrega nada, e ainda casa
     * com a fita VHS, onde o que assusta e sempre a forma que a camera nao resolveu.
     *
     * Multiplicar e melhor que repintar o atlas: a textura fica no jar intacta e voltar
     * atras (ou parar no meio do caminho, tipo 0x202020) e trocar um numero aqui.
     */
    private final int meshTint;

    /**
     * A quantos blocos ACIMA DO CHAO ela nasce. 0 = pisa no chao como todo mundo.
     *
     * O Ofanim vai a 30. Nao e enfeite: uma presenca de 13 blocos plantada no chao ainda
     * e uma coisa no seu caminho, que se contorna. La em cima ela deixa de ser obstaculo
     * e vira ceu — voce nao esbarra nela, voce OLHA PRA CIMA e ela esta ali. E a unica
     * posicao em que o tamanho dela trabalha o tempo todo, em vez de so quando voce chega
     * perto.
     */
    private final float hoverBlocks;

    /**
     * @param height altura do cartaz em blocos (o jogador tem 1.8 — elas sao TORRES)
     * @param aspect largura/altura DA TEXTURA. A largura nao se escolhe: ela sai
     *               daqui. Na primeira versao eu chutei os dois numeros e as tres
     *               sairam esticadas na horizontal — o Alto, que e um risco fino de
     *               0.32, estava desenhado com quase meia altura de largura.
     */
    AnomalyType(String id, Visibility visibility, float height, float aspect) {
        this(id, visibility, Behavior.PRESENCE, height, aspect, 1, 1, 1, 0.0F, WALK,
                Float.NaN, 0xFFFFFF, 0.0F);
    }

    /**
     * Anomalia que NASCEU EM PNG e ganhou corpo (tools/inflate_png.py).
     *
     * A silhueta continua sendo o desenho original — ela nao foi remodelada, foi inflada:
     * a distancia de cada pixel ate a borda virou a espessura naquele ponto. Por isso o
     * meshYaw e sempre 180: a camada da frente e construida em +Z, e o Minecraft espera
     * o modelo encarando -Z. Nao ha o que medir aqui.
     */
    AnomalyType(String id, Visibility visibility, float height, float aspect, float meshYaw) {
        this(id, visibility, Behavior.PRESENCE, height, aspect, 1, 1, 1, 0.0F, WALK,
                meshYaw, 0xFFFFFF, 0.0F);
    }

    /**
     * Criatura de FOLHA: pre-renderizada em varias vistas e varios quadros.
     *
     * A celula e quadrada (o empacotador recorta assim), entao aqui o aspecto e 1 e a
     * largura do cartaz e igual a altura — a margem transparente do proprio quadro se
     * encarrega da silhueta.
     */
    AnomalyType(String id, Visibility visibility, Behavior behavior, float height,
                int angles, int frames, int ticksPerFrame, float angleOffset, float speed) {
        this(id, visibility, behavior, height, 1.0F, angles, frames, ticksPerFrame, angleOffset,
                speed, Float.NaN, 0xFFFFFF, 0.0F);
    }

    /** Criatura de ESCULTURA: a folha continua declarada, como reserva. */
    AnomalyType(String id, Visibility visibility, Behavior behavior, float height,
                int angles, int frames, int ticksPerFrame, float angleOffset, float speed,
                float meshYaw, int meshTint, float hoverBlocks) {
        this(id, visibility, behavior, height, 1.0F, angles, frames, ticksPerFrame, angleOffset,
                speed, meshYaw, meshTint, hoverBlocks);
    }

    AnomalyType(String id, Visibility visibility, Behavior behavior, float height, float aspect,
                int angles, int frames, int ticksPerFrame, float angleOffset, float speed,
                float meshYaw, int meshTint, float hoverBlocks) {
        this.id = id;
        this.visibility = visibility;
        this.behavior = behavior;
        this.height = height;
        this.aspect = aspect;
        this.angles = angles;
        this.frames = frames;
        this.ticksPerFrame = ticksPerFrame;
        this.angleOffset = angleOffset;
        this.speed = speed;
        this.meshYaw = meshYaw;
        this.meshTint = meshTint;
        this.hoverBlocks = hoverBlocks;
    }

    public int meshTintRed() {
        return (this.meshTint >> 16) & 0xFF;
    }

    public int meshTintGreen() {
        return (this.meshTint >> 8) & 0xFF;
    }

    public int meshTintBlue() {
        return this.meshTint & 0xFF;
    }

    /**
     * Quantas poses da animacao passam por tick.
     *
     * Fica em metodo, e nao em mais uma coluna do enum, porque as linhas ja carregam doze
     * numeros e a decima terceira deixaria de ser lida. E porque quase ninguem precisa
     * dela: o padrao pelo comportamento cobre todo mundo — quem caca anda (ciclo de passo
     * de ~3s) e quem e presenca so respira.
     *
     * A das Garras e a excecao. O corpo dela nao balanca: TREME. Tremor precisa de mais
     * poses (32) rodando mais rapido, senao os seis ciclos de chacoalho que estao
     * gravados na malha saem em camera lenta e viram aceno.
     */
    public float animSpeed() {
        if (this == CLAWS) return 0.75F;
        return hunts() ? 0.28F : 0.12F;
    }

    /**
     * Ela aparece SOZINHA no mundo, ou so onde alguma dimensao a chama?
     *
     * Fica em metodo pelo mesmo motivo do animSpeed logo acima: as linhas do catalogo
     * ja carregam doze numeros, e a decima terceira coluna deixaria de ser lida.
     *
     * O OFANIM SAIU DO MUNDO ABERTO (escolha do Pedro, 2026-07-27). Ele nascia no
     * overworld pelo sorteio do spawn natural, junto com as outras — e isso passou a
     * ser errado no dia em que ele virou a criatura da CHUNKS. Duas razoes:
     *
     * 1. Ele e o ASSUNTO daquela dimensao. Uma coisa que se encontra a toa no campo
     *    deixa de ser o motivo para atravessar as colunas.
     * 2. A mecanica dele nao cabe no overworld. Ali ha parede, teto, caverna e casa —
     *    baixar os olhos e andar nao custa nada quando existe um lugar coberto a vinte
     *    passos. O verbo da fuga so tem preco onde o chao acaba a cada dezesseis
     *    blocos, e esse lugar e um so.
     *
     * ⚠️ Isto vale para o sorteio do spawn NATURAL. O ovo de anomalia e o /summon
     * continuam podendo invoca-lo em qualquer lugar — sao ato deliberado de quem esta
     * testando, e nao "aparecer".
     */
    public boolean wild() {
        return this != OPHANIM;
    }

    /**
     * O elenco do spawn natural: as que podem ser sorteadas no mundo aberto.
     *
     * Calculada uma vez. Se um dia todas sairem daqui, quem sorteia devolve a primeira
     * do catalogo em vez de estourar — ver AnomalyEntity.roll.
     */
    private static final java.util.List<AnomalyType> WILD =
            java.util.Arrays.stream(values()).filter(AnomalyType::wild).toList();

    public static java.util.List<AnomalyType> wildTypes() {
        return WILD;
    }

    /** Ela nasce no ar? */
    public boolean hovers() {
        return this.hoverBlocks > 0.0F;
    }

    public float hoverBlocks() {
        return this.hoverBlocks;
    }

    /** Ela tem escultura de verdade, ou e cartaz? */
    public boolean hasMesh() {
        return !Float.isNaN(this.meshYaw);
    }

    public float meshYaw() {
        return this.meshYaw;
    }

    /** O binario com as poses (tools/pack_mesh.py). */
    public ResourceLocation meshPath() {
        return new ResourceLocation(RECMod.MOD_ID, "meshes/" + this.id + ".mesh");
    }

    /**
     * O atlas da escultura — separado da textura do cartaz de proposito.
     *
     * Um arquivo so nao daria: a folha de sprites e a criatura ja DESENHADA de oito lados,
     * e o atlas e a pele dela em pedacos. Guardando os dois, a reserva continua de pe.
     */
    public ResourceLocation meshTexture() {
        return new ResourceLocation(RECMod.MOD_ID, "textures/entity/anomaly/" + this.id + "_mesh.png");
    }

    /** Velocidade base desta criatura (o atributo e por tipo; ver AnomalyEntity). */
    public float speed() {
        return this.speed;
    }

    public int angles() {
        return this.angles;
    }

    public int frames() {
        return this.frames;
    }

    public int ticksPerFrame() {
        return this.ticksPerFrame;
    }

    public float angleOffset() {
        return this.angleOffset;
    }

    /** Ela tem vistas por angulo? Se nao, o cartaz so encara a camera. */
    public boolean directional() {
        return this.angles > 1;
    }

    public String id() {
        return this.id;
    }

    public Visibility visibility() {
        return this.visibility;
    }

    public Behavior behavior() {
        return this.behavior;
    }

    /** Ela vem atras de voce? */
    public boolean hunts() {
        return this.behavior == Behavior.HUNTER;
    }

    /** Largura do cartaz, em blocos — derivada da altura pela proporcao da textura. */
    public float width() {
        return this.height * this.aspect;
    }

    /** Altura do cartaz, em blocos. */
    public float height() {
        return this.height;
    }

    public ResourceLocation texture() {
        return new ResourceLocation(RECMod.MOD_ID, "textures/entity/anomaly/" + this.id + ".png");
    }

    /** O nome que sai na foto ("CAPTURADO: ..."), via lang. */
    public String translationKey() {
        return "entity.recmod.anomaly." + this.id;
    }

    public static AnomalyType byIndex(int index) {
        AnomalyType[] all = values();
        return all[Math.floorMod(index, all.length)];
    }

    public static AnomalyType byId(String id) {
        for (AnomalyType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return null;
    }
}
