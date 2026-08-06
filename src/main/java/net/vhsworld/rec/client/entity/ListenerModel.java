package net.vhsworld.rec.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.entity.mesh.SkinnedMesh;
import net.vhsworld.rec.entity.ListenerEntity;
import org.joml.Matrix4f;

/**
 * O ESCUTADOR, animado POR CONTA — nao ha um keyframe neste arquivo.
 *
 * <h3>A anatomia (referencia do Pedro: SCP-939)</h3>
 * Quadrupede baixo e arqueado, membros compridos que saem para OS LADOS e nao para baixo,
 * garras longas, cerdas nas costas. E a cabeca e uma BOCA: uma fenda vertical de dentes
 * ocupando a frente inteira, sem olho nenhum. A coincidencia com a mecanica nao e sorte —
 * o 939 tambem e cego e tambem caca por som; a referencia e a criatura concordam.
 *
 * ⚠️ A CABECA-BOCA E O UNICO ROSTO QUE ELE PODE TER. Qualquer coisa parecida com olho, por
 * menor que fosse, faria o jogador testar o olhar — e ele passaria dez minutos encarando a
 * criatura esperando que ela congelasse, como as outras seis fazem. O corpo tem que dizer
 * "olhar aqui nao serve" antes de o jogador tentar.
 *
 * <h3>Por que procedural e nao pose assada</h3>
 * O mod ja sabe assar animacao do Blender (BakedMesh: dezesseis poses interpoladas). Aquilo
 * serve para a anomalia, que faz sempre a mesma coisa. Nao serve para este bicho: o andar
 * dele TEM que responder ao chao, ao aperto e ao tamanho do barulho que ele ouviu, porque e
 * por essas tres coisas que o jogador le o que esta acontecendo sem nenhuma interface. Pose
 * assada e ciclo fechado — da para toca-la mais rapido e mais nada.
 *
 * ⚠️ E O RIG E QUE E O TRABALHO, NAO A GEOMETRIA. As caixas aqui sao provisorias, ate a
 * malha em pecas articuladas vir do Blender; quando ela chegar, troca-se de onde os
 * vertices vem e este arquivo continua valendo palavra por palavra, porque ele so mexe em
 * angulo de junta. Rig e o ativo; caixa e aluguel.
 */
public class ListenerModel extends EntityModel<ListenerEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(RECMod.MOD_ID, "listener"), "main");

    /**
     * ⚠️ AS POSES DE REPOUSO SAO DADO, NAO NUMERO SOLTO. Toda animacao aqui e SOMADA a
     * elas, e nunca escrita por cima: se o passo escrevesse `zRot = ...`, o braco perderia
     * a abertura para o lado no primeiro tick e o bicho viraria um cachorro comum. E a
     * abertura e a silhueta inteira da referencia.
     */
    /**
     * ⚠️ ESTES ANGULOS FORAM CALCULADOS PARA A MAO POUSAR NO CHAO, e nao escolhidos no
     * olho. A primeira versao usava 0.62/0.85 e a ponta da mao caia CATORZE PIXELS ABAIXO
     * do chao — o bicho ficava enterrado ate o pulso. Com ombro a 13px de altura, braco de
     * 12 e antebraco de 20, a unica cadeia que fecha e esta: o braco sobe e abre (o
     * cotovelo vai parar ACIMA das costas, como na referencia) e o antebraco DESCE quase
     * reto ate o chao.
     *
     * ⚠️ Repare que o sinal do segundo elo VOLTA (−2.18 no braco, +2.08 no antebraco). E a
     * armadilha nº4 deste projeto, ja paga uma vez: repetir o sinal no elo seguinte empilha
     * o arco e o membro sai pelo teto — foi assim que um infectado passou de seis blocos.
     */
    private static final float FRONT_UPPER_Z = 2.18F;   // braco: para cima e para fora
    private static final float FRONT_FORE_Z = 2.08F;    // antebraco: volta, e desce reto
    private static final float FRONT_HAND_Z = 0.10F;    // e a mao fica plana no chao
    private static final float BACK_THIGH_X = 2.00F;    // coxa: para cima e para tras
    private static final float BACK_SHIN_X = -2.10F;    // canela: volta, e desce
    private static final float BACK_FOOT_X = 0.10F;

    /**
     * A PELE, se existir.
     *
     * ⚠️ A malha low poly (tools/build_listener_mesh.py) substitui as caixas NOS MESMOS
     * OSSOS: mesmos pivos, mesmos angulos, mesma animacao. As caixas continuam no arquivo e
     * continuam sendo montadas porque sao a rede de seguranca — se o .pmesh faltar num
     * resource pack, a criatura fica com cara velha em vez de sumir do mundo. E a mesma
     * doutrina do MeshLibrary, e ja poupou este mod de crashar de proposito uma vez.
     */
    private static final ResourceLocation MESH =
            new ResourceLocation(RECMod.MOD_ID, "meshes/listener.smesh");

    private final ModelPart spine;
    private final ModelPart chest;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart hip;

    private final ModelPart[] frontUpper = new ModelPart[2];
    private final ModelPart[] frontFore = new ModelPart[2];
    private final ModelPart[] frontHand = new ModelPart[2];
    private final ModelPart[] backThigh = new ModelPart[2];
    private final ModelPart[] backShin = new ModelPart[2];
    private final ModelPart[] backFoot = new ModelPart[2];

    public ListenerModel(ModelPart root) {
        this.spine = root.getChild("spine");
        this.chest = this.spine.getChild("chest");
        this.neck = this.chest.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.hip = this.spine.getChild("hip");

        String[] side = {"right", "left"};
        for (int i = 0; i < 2; i++) {
            this.frontUpper[i] = this.chest.getChild("front_upper_" + side[i]);
            this.frontFore[i] = this.frontUpper[i].getChild("front_fore_" + side[i]);
            this.frontHand[i] = this.frontFore[i].getChild("front_hand_" + side[i]);

            this.backThigh[i] = this.hip.getChild("back_thigh_" + side[i]);
            this.backShin[i] = this.backThigh[i].getChild("back_shin_" + side[i]);
            this.backFoot[i] = this.backShin[i].getChild("back_foot_" + side[i]);
        }
    }

    public static LayerDefinition createBody() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // A raiz fica a 11px do chao (o chao e y=24). Ele e baixo: o dorso mal passa da
        // cintura do jogador, e e por isso que encontra-lo num corredor da a impressao de
        // que a coisa esta vindo POR BAIXO.
        PartDefinition spine = root.addOrReplaceChild("spine",
                CubeListBuilder.create().texOffs(0, 40)
                        .addBox(-4.0F, -4.0F, -6.0F, 8.0F, 7.0F, 12.0F),
                PartPose.offset(0.0F, 13.0F, 0.0F));

        // As cerdas: a crista de espinhos das costas, na nuca. Peca fixa, so silhueta.
        spine.addOrReplaceChild("ridge",
                CubeListBuilder.create().texOffs(60, 40)
                        .addBox(-0.5F, -9.0F, -6.0F, 1.0F, 6.0F, 13.0F),
                PartPose.rotation(-0.18F, 0.0F, 0.0F));

        PartDefinition chest = spine.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(0, 60)
                        .addBox(-4.5F, -4.5F, -7.0F, 9.0F, 8.0F, 7.0F),
                PartPose.offset(0.0F, 0.0F, -6.0F));

        // O pescoco desce um pouco: a cabeca fica abaixo da linha dos ombros, como na
        // referencia, e e o que faz a boca chegar antes do resto do corpo.
        //
        // ⚠️ ERA 0.30 E CAIU PARA 0.12 depois do teste de frente: a cabeca ficava POR BAIXO
        // do peito e, de frente, o bicho nao tinha rosto — so dorso e pernas. A cabeca e a
        // unica coisa que identifica esta criatura; ela tem que cortar a silhueta do corpo,
        // nao se esconder nela.
        PartDefinition neck = chest.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(34, 60)
                        .addBox(-2.5F, -2.0F, -5.0F, 5.0F, 5.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 1.5F, -6.5F, 0.12F, 0.0F, 0.0F));

        // A CABECA: uma cunha comprida sem nada em cima. A boca e a fenda entre ela e a
        // mandibula — nao ha olho, nao ha narina, nao ha o que encarar.
        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -4.0F, -13.0F, 8.0F, 6.0F, 13.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, -4.0F, -0.06F, 0.0F, 0.0F));

        head.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(0, 20)
                        .addBox(-3.5F, 0.0F, -12.0F, 7.0F, 4.0F, 12.0F),
                PartPose.offset(0.0F, 2.0F, 0.0F));

        PartDefinition hip = spine.addOrReplaceChild("hip",
                CubeListBuilder.create().texOffs(0, 80)
                        .addBox(-4.0F, -3.5F, 0.0F, 8.0F, 7.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 6.0F));

        float[] sign = {-1.0F, 1.0F};
        String[] side = {"right", "left"};

        for (int i = 0; i < 2; i++) {
            // ⚠️ MEMBRO QUE SOBE ABRE COM Z NEGATIVO E O QUE DESCE COM Z POSITIVO (armadilha
            // de eixo ja paga duas vezes neste projeto). Aqui o braco SOBE, entao o lado
            // direito leva z negativo — e o antebraco, que desce, inverte de volta.
            PartDefinition upper = chest.addOrReplaceChild("front_upper_" + side[i],
                    CubeListBuilder.create().texOffs(56, 0).mirror(i == 1)
                            .addBox(-1.5F, -1.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                    PartPose.offsetAndRotation(sign[i] * 4.0F, -2.0F, -4.0F,
                            0.0F, 0.0F, -sign[i] * FRONT_UPPER_Z));

            PartDefinition fore = upper.addOrReplaceChild("front_fore_" + side[i],
                    CubeListBuilder.create().texOffs(70, 0).mirror(i == 1)
                            .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 18.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, 11.0F, 0.0F,
                            0.0F, 0.0F, sign[i] * FRONT_FORE_Z));

            PartDefinition hand = fore.addOrReplaceChild("front_hand_" + side[i],
                    CubeListBuilder.create().texOffs(82, 0).mirror(i == 1)
                            .addBox(-2.0F, 0.0F, -4.0F, 4.0F, 2.0F, 5.0F),
                    PartPose.offsetAndRotation(0.0F, 17.5F, 0.0F,
                            0.0F, 0.0F, sign[i] * FRONT_HAND_Z));

            // As garras: tres por mao, longas. Elas sao metade da leitura da criatura de
            // perto — e a unica parte clara do corpo inteiro.
            for (int c = 0; c < 3; c++) {
                hand.addOrReplaceChild("claw_" + side[i] + "_" + c,
                        CubeListBuilder.create().texOffs(100, 0)
                                .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F),
                        PartPose.offsetAndRotation((c - 1) * 1.4F, 1.0F, -3.2F,
                                1.35F, (c - 1) * 0.22F, 0.0F));
            }

            // A perna traseira e a mesma ideia no eixo X: quadril alto, coxa para cima e
            // para tras, canela descendo quase reta. E a perna dobrada de bicho que
            // ARRANCA — a referencia inteira e um corpo pronto para o bote.
            PartDefinition thigh = hip.addOrReplaceChild("back_thigh_" + side[i],
                    CubeListBuilder.create().texOffs(56, 20).mirror(i == 1)
                            .addBox(-2.0F, -1.0F, -2.0F, 4.0F, 10.0F, 4.0F),
                    PartPose.offsetAndRotation(sign[i] * 3.5F, -1.0F, 3.0F,
                            BACK_THIGH_X, 0.0F, 0.0F));

            PartDefinition shin = thigh.addOrReplaceChild("back_shin_" + side[i],
                    CubeListBuilder.create().texOffs(74, 20).mirror(i == 1)
                            .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 15.0F, 3.0F),
                    PartPose.offsetAndRotation(0.0F, 9.0F, 0.0F, BACK_SHIN_X, 0.0F, 0.0F));

            shin.addOrReplaceChild("back_foot_" + side[i],
                    CubeListBuilder.create().texOffs(88, 20).mirror(i == 1)
                            .addBox(-1.5F, 0.0F, -5.0F, 3.0F, 2.0F, 6.0F),
                    PartPose.offsetAndRotation(0.0F, 14.5F, 0.0F, BACK_FOOT_X, 0.0F, 0.0F));
        }

        return LayerDefinition.create(mesh, 128, 128);
    }

    // ------------------------------------------------------------------ a animacao

    @Override
    public void setupAnim(ListenerEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        byte posture = entity.posture();
        float alert = entity.alertClient();
        float ground = entity.groundNoiseClient();

        rest();

        if (posture == ListenerEntity.POSTURE_FROZEN) {
            frozen(netHeadYaw, headPitch, alert);
            return;
        }

        gait(limbSwing, limbSwingAmount, ground, alert);
        head(ageInTicks, posture, alert, netHeadYaw, headPitch);
    }

    /** Volta tudo para a pose de repouso antes de somar qualquer coisa. */
    private void rest() {
        this.spine.y = 13.0F;
        this.spine.xRot = 0.0F;
        this.spine.zRot = 0.0F;
        this.chest.xRot = 0.0F;
        this.neck.xRot = 0.12F;
        this.head.xRot = -0.06F;
        this.head.yRot = 0.0F;
        this.head.zRot = 0.0F;
        this.jaw.xRot = 0.0F;
        this.hip.xRot = 0.0F;

        float[] sign = {-1.0F, 1.0F};
        for (int i = 0; i < 2; i++) {
            this.frontUpper[i].xRot = 0.0F;
            this.frontUpper[i].zRot = -sign[i] * FRONT_UPPER_Z;
            this.frontFore[i].xRot = 0.0F;
            this.frontFore[i].zRot = sign[i] * FRONT_FORE_Z;
            this.frontHand[i].xRot = 0.0F;
            this.frontHand[i].zRot = sign[i] * FRONT_HAND_Z;

            this.backThigh[i].xRot = BACK_THIGH_X;
            this.backShin[i].xRot = BACK_SHIN_X;
            this.backFoot[i].xRot = BACK_FOOT_X;
        }
    }

    /**
     * TRAVADO — nem respiracao, nem balanco, nem peso trocando de perna.
     *
     * ⚠️ A tentacao de por um idle sutil aqui e forte e esta errada. O congelamento e a
     * unica coisa que diz "ele acabou de te ouvir", e o valor dele esta em ser ABSOLUTO:
     * qualquer movimento residual devolve a duvida que o instante existe para tirar.
     *
     * A UNICA coisa que se mexe e a boca, que ABRE. E o gesto que da nome a criatura: no
     * momento em que ele te ouve, a boca escancara na sua direcao — e como ele nao tem
     * olho, e a boca que aponta. O jogador entende sozinho que aquilo e o sentido dele.
     */
    private void frozen(float netHeadYaw, float headPitch, float alert) {
        this.head.yRot = netHeadYaw * (Mth.PI / 180.0F) * 0.35F;
        this.head.xRot += headPitch * (Mth.PI / 180.0F) * 0.30F;

        // ⚠️ E AQUI ELA ESCANCARA — o unico movimento do congelamento. Com a boca ja aberta
        // no tateio, o gesto passou a ser o SALTO: de meio palmo para a boca inteira, de uma
        // vez, virada para voce. Como ele nao tem olho, e a boca que aponta.
        this.jaw.xRot = 1.15F + 0.30F * alert;

        // O corpo se abaixa e recolhe: ele agacha para ouvir.
        this.spine.y = 13.5F;
        this.spine.xRot = 0.06F;
    }

    /**
     * A ANDADURA — passo diagonal de quadrupede, construida do apoio e nao de um seno no
     * dorso.
     *
     * Os quatro membros em DUAS fases cruzadas (dianteiro direito com traseiro esquerdo):
     * e o que qualquer bicho de quatro faz, e sem isso ele anda como um cavalinho de
     * balanco. Os tres numeros que variam sao os mesmos tres que o jogador le de longe:
     *
     * <b>amplitude</b> cresce com o alerta — ele ouviu uma porta a trinta blocos: passo
     * curto, quase curioso; ouviu voce quebrar pedra a dez: passada inteira. A mesma
     * animacao conta as duas coisas, e o jogador sabe o tamanho do erro que cometeu antes
     * de a criatura chegar.
     *
     * <b>altura do pe</b> cresce com o chao barulhento (folha, cascalho): ele levanta mais
     * e demora mais a pousar, porque ele TAMBEM escuta. E a regra dele aplicada a ele
     * mesmo, e o jogador tira a conclusao certa sozinho.
     *
     * <b>o dorso ondula</b> no dobro da frequencia. Sem isso ele desliza sobre rodas — e um
     * bicho que desliza nao faz barulho de passo, o que seria mentira exatamente na
     * criatura cujo assunto e o barulho.
     */
    private void gait(float limbSwing, float limbSwingAmount, float ground, float alert) {
        float amount = Math.min(limbSwingAmount, 1.0F);
        if (amount < 0.01F) return;

        float cycle = limbSwing * (1.0F - 0.25F * ground);
        float reach = (0.40F + 0.45F * alert) * amount;
        float lift = (0.30F + 0.50F * ground) * amount;

        for (int i = 0; i < 2; i++) {
            // Fase cruzada: dianteiro direito (i=0) anda junto com o traseiro esquerdo.
            float front = Mth.cos(cycle + (i == 0 ? 0.0F : Mth.PI));
            float back = Mth.cos(cycle + (i == 0 ? Mth.PI : 0.0F));

            this.frontUpper[i].xRot += front * reach;
            // O cotovelo recolhe so na volta — dobrar nos dois sentidos e o erro que faz o
            // membro parecer de borracha e tira o peso do passo.
            this.frontFore[i].xRot += Math.max(0.0F, -front) * lift * 1.6F;
            this.frontHand[i].xRot += Math.max(0.0F, front) * 0.35F;

            this.backThigh[i].xRot += back * reach * 0.9F;
            this.backShin[i].xRot += Math.max(0.0F, -back) * lift * 1.4F;
            this.backFoot[i].xRot += Math.max(0.0F, back) * 0.30F;
        }

        this.spine.y = 13.0F + Mth.cos(cycle * 2.0F) * 0.6F * amount;
        this.spine.xRot = Mth.cos(cycle * 2.0F) * 0.05F * amount;
        this.spine.zRot = Mth.cos(cycle) * 0.045F * amount;
        this.hip.xRot = -Mth.cos(cycle * 2.0F) * 0.07F * amount;
    }

    /**
     * A CABECA — tateando ou no rumo.
     *
     * ⚠️ Parado, ele nao fica em idle: ele PROCURA. A cabeca varre o ar em arcos curtos e
     * para, de boca FECHADA. A diferenca entre isto e um idle comum e que aqui o jogador ve
     * uma criatura fazendo alguma coisa COM ELE — e o que ela faz e tentar achar onde ele
     * esta, sem mostrar com o que.
     *
     * As varreduras usam frequencias que nao fecham (0.09 e 0.023): o movimento nunca se
     * repete igual e nao ha ciclo para o olho decorar. Ciclo decorado vira maquina, e
     * maquina nao assusta.
     */
    private void head(float ageInTicks, byte posture, float alert,
                      float netHeadYaw, float headPitch) {

        if (posture == ListenerEntity.POSTURE_SEEKING) {
            // Indo atras: a duvida acabou. A cabeca fica no rumo, baixa, e a boca vai
            // abrindo conforme o barulho era grande — mas parte de QUASE FECHADA, para
            // ainda haver o que revelar quando ele chegar.
            this.head.yRot = netHeadYaw * (Mth.PI / 180.0F) * 0.45F
                    + Mth.sin(ageInTicks * 0.17F) * 0.05F;
            this.head.xRot += headPitch * (Mth.PI / 180.0F) * 0.35F + 0.10F;
            this.neck.xRot += 0.10F * alert;
            this.jaw.xRot = 0.45F + 0.40F * alert + Mth.sin(ageInTicks * 0.23F) * 0.05F;
            return;
        }

        // Parado, sem som: a varredura.
        float sweep = Mth.sin(ageInTicks * 0.09F) * 0.60F + Mth.sin(ageInTicks * 0.023F) * 0.30F;
        this.head.yRot = sweep;
        this.head.xRot += Mth.sin(ageInTicks * 0.041F) * 0.20F;
        this.head.zRot = Mth.sin(ageInTicks * 0.031F) * 0.12F;
        this.neck.xRot += Mth.sin(ageInTicks * 0.037F) * 0.08F;

        // ⚠️ A BOCA FICA ABERTA — decisao do Pedro depois de ver no jogo, e ela derruba o
        // argumento que eu tinha feito.
        //
        // Eu havia fechado a boca no tateio para que abri-la fosse um acontecimento: o bicho
        // seria um vulto sem cara ate te ouvir. A logica era boa e o resultado no jogo foi
        // pior — <b>de frente, sem os dentes, a cabeca vira um borrao escuro sem leitura
        // nenhuma</b>. A silhueta e o que a criatura tem, e os dentes SAO a silhueta da
        // cabeca dela; escondendo-os, ela deixa de parecer uma coisa.
        //
        // A licao que fica: revelacao so vale quando o estado escondido ainda comunica
        // alguma coisa. Aqui nao comunicava. O contraste continua existindo, so mudou de
        // lugar — agora e entre a boca ABERTA e a boca ESCANCARADA (ver frozen).
        this.jaw.xRot = 0.42F + Mth.sin(ageInTicks * 0.045F) * 0.10F;

        // E os bracos dianteiros tateiam o chao, cada um no seu tempo. Juntos pareceria
        // ginastica; defasados parece procura.
        this.frontUpper[0].xRot += Mth.sin(ageInTicks * 0.055F) * 0.16F;
        this.frontUpper[1].xRot += Mth.sin(ageInTicks * 0.047F + 1.7F) * 0.16F;
        this.frontHand[0].xRot += Mth.sin(ageInTicks * 0.061F) * 0.22F;
        this.frontHand[1].xRot += Mth.sin(ageInTicks * 0.053F + 0.9F) * 0.22F;
    }

    // ------------------------------------------------------------------ o desenho

    /**
     * ⚠️ A MALHA E DOBRADA PELO PROPRIO ESQUELETO DO ModelPart. Cada osso entrega duas
     * matrizes — a de agora e a de repouso — e o SkinnedMesh combina as duas com os pesos.
     * Nao existe hierarquia paralela para divergir da animacao: as duas saem do mesmo rig,
     * a de repouso pelos `getInitialPose` e a de agora pelos campos que a animacao acabou
     * de escrever.
     *
     * Sem o arquivo, cai nas caixas. Ver SkinnedMesh.
     */
    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               float r, float g, float b, float a) {

        SkinnedMesh mesh = SkinnedMesh.get(MESH);
        if (mesh == null) {
            this.spine.render(pose, buffer, light, overlay, r, g, b, a);
            return;
        }

        mesh.draw(pose.last(), buffer, skinMatrices(mesh), light, overlay, r, g, b, a);
    }

    // ------------------------------------------------------------------ o esqueleto

    /** Um osso do skinning: a parte, e quem e o pai dela na cadeia. */
    private record Bone(String name, ModelPart part, int parent) {}

    private Bone[] rig;
    private Matrix4f[] bindInverse;
    private Matrix4f[] current;
    private Matrix4f[] skin;

    /**
     * A ordem importa: PAI ANTES DO FILHO, senao a matriz do pai ainda nao existe quando o
     * filho a le. E a mesma ordem em que a hierarquia foi montada.
     */
    private void buildRig() {
        java.util.List<Bone> bones = new java.util.ArrayList<>();
        bones.add(new Bone("spine", this.spine, -1));
        bones.add(new Bone("chest", this.chest, 0));
        bones.add(new Bone("neck", this.neck, 1));
        bones.add(new Bone("head", this.head, 2));
        bones.add(new Bone("jaw", this.jaw, 3));
        bones.add(new Bone("hip", this.hip, 0));

        for (int i = 0; i < 2; i++) {
            String side = i == 0 ? "right" : "left";
            int upper = bones.size();
            bones.add(new Bone("front_upper_" + side, this.frontUpper[i], 1));
            bones.add(new Bone("front_fore_" + side, this.frontFore[i], upper));
            bones.add(new Bone("front_hand_" + side, this.frontHand[i], upper + 1));

            int thigh = bones.size();
            bones.add(new Bone("back_thigh_" + side, this.backThigh[i], 5));
            bones.add(new Bone("back_shin_" + side, this.backShin[i], thigh));
            bones.add(new Bone("back_foot_" + side, this.backFoot[i], thigh + 1));
        }

        this.rig = bones.toArray(new Bone[0]);

        // ⚠️ A POSE DE REPOUSO SAI DO `getInitialPose`, e nao do arquivo. O Blender e o Java
        // teriam que concordar sobre ela, e duas fontes para a mesma verdade e como se produz
        // criatura torta que ninguem explica. A inversa e guardada porque nao muda nunca.
        this.bindInverse = new Matrix4f[this.rig.length];
        Matrix4f[] bind = new Matrix4f[this.rig.length];

        for (int i = 0; i < this.rig.length; i++) {
            Bone bone = this.rig[i];
            PartPose rest = bone.part().getInitialPose();

            Matrix4f m = bone.parent() < 0 ? new Matrix4f() : new Matrix4f(bind[bone.parent()]);
            m.translate(rest.x, rest.y, rest.z);
            if (rest.zRot != 0.0F) m.rotateZ(rest.zRot);
            if (rest.yRot != 0.0F) m.rotateY(rest.yRot);
            if (rest.xRot != 0.0F) m.rotateX(rest.xRot);

            bind[i] = m;
            this.bindInverse[i] = new Matrix4f(m).invert();
        }

        this.current = new Matrix4f[this.rig.length];
        this.skin = new Matrix4f[this.rig.length];
        for (int i = 0; i < this.rig.length; i++) {
            this.current[i] = new Matrix4f();
            this.skin[i] = new Matrix4f();
        }
    }

    /**
     * "agora × repouso⁻¹" para cada osso, na ordem em que o ARQUIVO nomeia os ossos.
     *
     * ⚠️ A ordem do arquivo e a do armature do Blender e nao tem por que ser a nossa, entao a
     * ligacao e feita por NOME. Casar por indice funcionaria hoje e quebraria no dia em que
     * alguem acrescentasse um osso no meio — com o sintoma mais divertido possivel: a cabeca
     * andando com a perna.
     */
    private Matrix4f[] skinMatrices(SkinnedMesh mesh) {
        if (this.rig == null) buildRig();

        for (int i = 0; i < this.rig.length; i++) {
            Bone bone = this.rig[i];
            ModelPart part = bone.part();

            Matrix4f m = this.current[i];
            if (bone.parent() < 0) {
                m.identity();
            } else {
                m.set(this.current[bone.parent()]);
            }
            m.translate(part.x, part.y, part.z);
            if (part.zRot != 0.0F) m.rotateZ(part.zRot);
            if (part.yRot != 0.0F) m.rotateY(part.yRot);
            if (part.xRot != 0.0F) m.rotateX(part.xRot);

            this.skin[i].set(m).mul(this.bindInverse[i]);
        }

        String[] names = mesh.bones();
        Matrix4f[] out = new Matrix4f[names.length];
        for (int i = 0; i < names.length; i++) {
            for (int j = 0; j < this.rig.length; j++) {
                if (this.rig[j].name().equals(names[i])) {
                    out[i] = this.skin[j];
                    break;
                }
            }
        }
        return out;
    }
}
