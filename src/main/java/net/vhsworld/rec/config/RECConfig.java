package net.vhsworld.rec.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Configuracao do REC.
 *
 * REGRA DO PROJETO: toda mecanica que mexe na tela, no som ou no controle do jogador
 * nasce com um botao aqui. As versoes 1.3 e 1.6 do VHSWORLD apanharam exatamente disso
 * (fog travada, shader pesado sem ajuste fino), e a solucao veio sempre tarde, como
 * remendo. Aqui vem antes.
 *
 * Sao dois arquivos, gerados na pasta "config" do jogo:
 *   config/recmod-client.toml -> visual, audio e o "feel" da filmadora (so o teu PC)
 *   config/recmod-common.toml -> regras de mundo (vale para o servidor tambem)
 */
public final class RECConfig {

    /** Onde ficam as barras pretas que recortam a imagem. */
    public enum LetterboxMode {
        SIDES,
        TOP_BOTTOM,
        BOTH
    }

    // ------------------------------------------------------------------ CLIENT

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    public static final class Client {

        // --- camera ---
        public final ForgeConfigSpec.BooleanValue letterbox;
        public final ForgeConfigSpec.EnumValue<LetterboxMode> letterboxMode;
        public final ForgeConfigSpec.DoubleValue letterboxThickness;

        public final ForgeConfigSpec.BooleanValue fisheye;
        public final ForgeConfigSpec.DoubleValue fisheyeStrength;

        public final ForgeConfigSpec.BooleanValue handShake;
        public final ForgeConfigSpec.DoubleValue handShakeStrength;

        // --- fita vhs ---
        public final ForgeConfigSpec.BooleanValue vhsEffect;
        public final ForgeConfigSpec.BooleanValue scanlines;
        public final ForgeConfigSpec.DoubleValue scanlineOpacity;
        public final ForgeConfigSpec.IntValue scanlineSpacing;
        public final ForgeConfigSpec.BooleanValue staticNoise;
        public final ForgeConfigSpec.DoubleValue staticAmount;
        public final ForgeConfigSpec.BooleanValue trackingBar;
        public final ForgeConfigSpec.IntValue trackingPeriodSeconds;
        public final ForgeConfigSpec.BooleanValue degradeWithBattery;
        public final ForgeConfigSpec.BooleanValue lensPostShader;

        // --- hud ---
        public final ForgeConfigSpec.BooleanValue showHud;
        public final ForgeConfigSpec.BooleanValue showRecBlink;

        public final ForgeConfigSpec.BooleanValue vhsTooltip;
        public final ForgeConfigSpec.BooleanValue tooltipTracking;
        public final ForgeConfigSpec.BooleanValue tooltipHeader;

        // --- flash ---
        public final ForgeConfigSpec.BooleanValue screenFlash;
        public final ForgeConfigSpec.DoubleValue flashFadeSpeed;
        public final ForgeConfigSpec.IntValue flashChargeTicks;
        public final ForgeConfigSpec.BooleanValue flashLights;
        public final ForgeConfigSpec.DoubleValue flashLightBoost;
        public final ForgeConfigSpec.DoubleValue flashLightSeconds;

        // --- bateria / apagao ---
        public final ForgeConfigSpec.BooleanValue batteryDrains;
        public final ForgeConfigSpec.DoubleValue batteryDrainPerTick;
        public final ForgeConfigSpec.DoubleValue batteryRechargeAmount;
        public final ForgeConfigSpec.IntValue pressesToRecharge;
        public final ForgeConfigSpec.DoubleValue blackoutDrainPerTick;
        public final ForgeConfigSpec.IntValue secondsUntilScream;
        public final ForgeConfigSpec.BooleanValue blackoutFootsteps;

        // --- fotos ---
        public final ForgeConfigSpec.BooleanValue photos;
        public final ForgeConfigSpec.IntValue maxPhotos;
        public final ForgeConfigSpec.IntValue photoDevelopSeconds;
        public final ForgeConfigSpec.DoubleValue photoFadeSeconds;
        public final ForgeConfigSpec.BooleanValue photoCatchesAnyMob;
        public final ForgeConfigSpec.BooleanValue codex;
        public final ForgeConfigSpec.BooleanValue autoEnableBetaTextures;

        // --- sanidade ---
        public final ForgeConfigSpec.BooleanValue sanity;
        public final ForgeConfigSpec.BooleanValue sanityBar;
        public final ForgeConfigSpec.IntValue sanityBarMargin;
        public final ForgeConfigSpec.BooleanValue sanityShakeOnHostile;
        public final ForgeConfigSpec.DoubleValue sanityLossPerSighting;
        public final ForgeConfigSpec.DoubleValue sanityRegenPerMinute;
        public final ForgeConfigSpec.DoubleValue sanityPerBattery;
        public final ForgeConfigSpec.DoubleValue sanityShakeSeconds;
        public final ForgeConfigSpec.DoubleValue sanityShakeStrength;
        public final ForgeConfigSpec.DoubleValue sanityThreshold;
        public final ForgeConfigSpec.BooleanValue sanityCorruptsTape;
        public final ForgeConfigSpec.BooleanValue sanityDrainsBattery;
        public final ForgeConfigSpec.BooleanValue sanityBlackouts;
        public final ForgeConfigSpec.BooleanValue sanityPhantomSounds;

        // --- engenhocas de camera ---
        public final ForgeConfigSpec.BooleanValue infraredLens;
        public final ForgeConfigSpec.BooleanValue infraredTint;
        public final ForgeConfigSpec.IntValue infraredRange;
        public final ForgeConfigSpec.DoubleValue infraredDrainMultiplier;
        public final ForgeConfigSpec.BooleanValue corruptedBattery;
        public final ForgeConfigSpec.DoubleValue corruptedBatteryRecharge;
        public final ForgeConfigSpec.DoubleValue corruptedBatterySanityCost;
        public final ForgeConfigSpec.BooleanValue tapes;
        public final ForgeConfigSpec.IntValue tapeSeconds;
        public final ForgeConfigSpec.IntValue tapeFrameEveryTicks;
        public final ForgeConfigSpec.IntValue tapeMaxReels;
        public final ForgeConfigSpec.BooleanValue tripod;
        public final ForgeConfigSpec.IntValue tripodRange;
        public final ForgeConfigSpec.BooleanValue tripodHidesHud;
        public final ForgeConfigSpec.BooleanValue tripodSeesHostiles;
        public final ForgeConfigSpec.BooleanValue tripodBeep;

        // --- localizadores (rastreador de minerio / localizador de estrutura) ---
        public final ForgeConfigSpec.BooleanValue oreTracker;
        public final ForgeConfigSpec.BooleanValue structureLocator;
        public final ForgeConfigSpec.IntValue trackerScanRadius;
        public final ForgeConfigSpec.BooleanValue trackerMarker;
        public final ForgeConfigSpec.BooleanValue trackerHeartbeat;

        // --- dificuldade ---
        public final ForgeConfigSpec.BooleanValue difficultyPrompt;
        public final ForgeConfigSpec.DoubleValue hardSanityMultiplier;
        public final ForgeConfigSpec.DoubleValue hardBatteryMultiplier;
        public final ForgeConfigSpec.DoubleValue hardHauntingMultiplier;
        public final ForgeConfigSpec.IntValue hardThresholdBonus;

        // --- transicao ---
        public final ForgeConfigSpec.BooleanValue inkTransition;
        public final ForgeConfigSpec.DoubleValue inkSeconds;

        // --- neblina ---
        public final ForgeConfigSpec.BooleanValue oldFog;
        public final ForgeConfigSpec.DoubleValue oldFogDensity;

        // --- audio ---
        public final ForgeConfigSpec.DoubleValue horrorVolume;

        // --- criaturas (so o que a tela faz; a REGRA fica no COMMON) ---
        public final ForgeConfigSpec.BooleanValue stonemanSurgeShake;
        public final ForgeConfigSpec.BooleanValue stonemanSurgeStatic;
        public final ForgeConfigSpec.BooleanValue anomalies;
        public final ForgeConfigSpec.IntValue anomalyRevealsToManifest;
        public final ForgeConfigSpec.BooleanValue sightSounds;
        public final ForgeConfigSpec.IntValue sightSoundRest;
        public final ForgeConfigSpec.BooleanValue ambientDread;
        public final ForgeConfigSpec.DoubleValue ambientDreadVolume;
        public final ForgeConfigSpec.BooleanValue absenceEvidence;
        public final ForgeConfigSpec.BooleanValue directorMusic;
        public final ForgeConfigSpec.BooleanValue dimensionStings;
        public final ForgeConfigSpec.IntValue dimensionStingMinSeconds;
        public final ForgeConfigSpec.IntValue dimensionStingMaxSeconds;
        public final ForgeConfigSpec.DoubleValue dimensionStingVolume;
        public final ForgeConfigSpec.BooleanValue chaseMusic;
        public final ForgeConfigSpec.DoubleValue chaseMusicVolume;
        public final ForgeConfigSpec.BooleanValue ophanimDrone;
        public final ForgeConfigSpec.DoubleValue ophanimDroneVolume;
        public final ForgeConfigSpec.BooleanValue ophanimVertigo;
        public final ForgeConfigSpec.DoubleValue ophanimVertigoStrength;
        public final ForgeConfigSpec.BooleanValue ophanimGazeStatic;

        Client(ForgeConfigSpec.Builder b) {
            b.comment("Visual da filmadora. Tudo aqui e client-side: nao muda regra de jogo,",
                      "so como a tela se comporta no teu PC.")
             .push("camera");

            letterbox = b
                    .comment("Barras pretas recortando a imagem. Substituiu a antiga moldura",
                             "curvada do visor, que comia a tela toda.")
                    .define("letterbox", true);

            letterboxMode = b
                    .comment("Onde ficam as barras:",
                             "  SIDES       = laterais, esquerda e direita (padrao)",
                             "  TOP_BOTTOM  = em cima e embaixo, estilo cinema",
                             "  BOTH        = moldura fechada nos quatro lados")
                    .defineEnum("letterboxMode", LetterboxMode.SIDES);

            letterboxThickness = b
                    .comment("Espessura da barra, como fracao da tela. 0.03 = fina (padrao),",
                             "0.10 ja e uma tarja grossa. Nas laterais conta sobre a largura;",
                             "em cima/embaixo, sobre a altura.")
                    .defineInRange("letterboxThickness", 0.03D, 0.0D, 0.30D);

            fisheye = b
                    .comment("Distorcao de lente (aumento do FOV) enquanto a camera esta ativa.")
                    .define("fisheye", true);

            fisheyeStrength = b
                    .comment("Multiplicador do FOV. 1.0 = sem distorcao.",
                             "1.7 = padrao, bem aberto, cara de lente de camcorder.",
                             "Acima de 2.2 embrulha o estomago de muita gente.")
                    .defineInRange("fisheyeStrength", 1.7D, 1.0D, 2.5D);

            handShake = b
                    .comment("Balanco de mao (a camera respira junto com o jogador).")
                    .define("handShake", true);

            handShakeStrength = b
                    .comment("Amplitude do balanco, em graus. 0.0 desliga na pratica.")
                    .defineInRange("handShakeStrength", 0.18D, 0.0D, 1.0D);

            b.pop();

            b.comment("A cara da fita VHS: scanlines, chiado e a barra de tracking.",
                      "Tudo isto e desenhado POR CIMA da tela, na camada de GUI. Nao toca no",
                      "framebuffer do mundo, entao nao briga com shaderpack (Oculus/Iris) —",
                      "que foi exatamente o que quebrou o render na v1.0.0.")
             .push("vhs");

            vhsEffect = b
                    .comment("Chave geral do efeito de fita.")
                    .define("vhsEffect", true);

            scanlines = b
                    .comment("Linhas horizontais de varredura da TV.")
                    .define("scanlines", true);

            scanlineOpacity = b
                    .comment("Opacidade das linhas. 0.22 = padrao, bem visivel. 0.10 mal aparece.")
                    .defineInRange("scanlineOpacity", 0.22D, 0.0D, 1.0D);

            scanlineSpacing = b
                    .comment("Distancia entre linhas, em pixels. Menor = tela mais riscada e",
                             "mais linhas desenhadas por frame (custa um pouco mais).")
                    .defineInRange("scanlineSpacing", 3, 2, 16);

            staticNoise = b
                    .comment("Chiado/estatica da fita: pontinhos brancos por toda a imagem.",
                             "DESLIGADO por padrao — na tela cheia vira poluicao visual e",
                             "atrapalha enxergar o mundo, que e o oposto do que a fita deveria",
                             "fazer. Ligue so se quiser a tela realmente suja.")
                    .define("staticNoise", false);

            staticAmount = b
                    .comment("Quantidade de chiado. 0.0 nenhum, 1.0 tempestade de areia.")
                    .defineInRange("staticAmount", 0.50D, 0.0D, 1.0D);

            trackingBar = b
                    .comment("Aquela faixa clara que sobe/desce na imagem (tracking da fita).",
                             "DESLIGADA por padrao — do jeito que ficou, ela rouba a atencao da",
                             "cena inteira toda vez que passa. Efeito de fita tem que ser textura",
                             "de fundo, nao protagonista.")
                    .define("trackingBar", false);

            trackingPeriodSeconds = b
                    .comment("Segundos que a faixa leva para atravessar a tela.")
                    .defineInRange("trackingPeriodSeconds", 12, 1, 300);

            degradeWithBattery = b
                    .comment("A fita piora conforme a bateria cai: mais chiado, mais tracking.")
                    .define("degradeWithBattery", true);

            lensPostShader = b
                    .comment("LEGADO — a lente de verdade da v1.0.0 (post-shader que entorta o",
                             "mundo inteiro, com scanline e ruido no proprio frame).",
                             "Foi o que corrompeu o render das chunks distantes quando havia",
                             "shaderpack ligado: dois donos para o mesmo framebuffer.",
                             "Se voce ligar isto, o mod AINDA se recusa a carregar enquanto um",
                             "shaderpack estiver em uso. Sem shaderpack, roda.")
                    .define("lensPostShader", false);

            b.pop();

            b.comment("Textos do visor: REC, timer, bateria.").push("hud");

            showHud = b
                    .comment("Mostra o HUD da filmadora (REC / PLAY SP / timer / bateria).")
                    .define("showHud", true);

            showRecBlink = b
                    .comment("Piscar do indicador REC vermelho.")
                    .define("showRecBlink", true);

            b.pop();

            b.comment("A caixinha que aparece quando o mouse para em cima de um item.",
                      "Desligar aqui devolve o tooltip normal do jogo, sem perder nada de",
                      "conteudo: so muda a moldura, nunca o texto.")
             .push("tooltip");

            vhsTooltip = b
                    .comment("Veste o tooltip com a moldura do mod (caixa preta reta, cantos de",
                             "enquadramento, scanlines e regua vermelha sob o nome).")
                    .define("vhsTooltip", true);

            tooltipTracking = b
                    .comment("Linha de tracking descendo por dentro da caixa, como fita rodando.",
                             "Desligue se distrair na hora de ler.")
                    .define("tooltipTracking", true);

            tooltipHeader = b
                    .comment("Ficha no topo do tooltip: icone do item num encaixe, nome ao lado",
                             "e a raridade embaixo. Desligado, o nome volta a ser so a primeira",
                             "linha de texto, como no jogo normal.")
                    .define("tooltipHeader", true);

            b.pop();

            b.comment("Flash da filmadora (tecla R).").push("flash");

            screenFlash = b
                    .comment("Clarao branco na tela ao disparar o flash.",
                             "Desligue em caso de sensibilidade a luz.")
                    .define("screenFlash", true);

            flashFadeSpeed = b
                    .comment("Velocidade com que o clarao some. Maior = some mais rapido.")
                    .defineInRange("flashFadeSpeed", 0.05D, 0.01D, 1.0D);

            flashChargeTicks = b
                    .comment("Ticks segurando R para carregar o flash por completo (20 ticks = 1s).",
                             "O flash SO dispara em 100%: soltar antes perde a carga.")
                    .defineInRange("flashChargeTicks", 60, 1, 600);

            flashLights = b
                    .comment("O flash ilumina o mundo de verdade, e nao so pinta a tela.",
                             "Como a foto e tirada no mesmo instante, ela sai iluminada por ele.")
                    .define("flashLights", true);

            flashLightBoost = b
                    .comment("Quanto o flash clareia. 6.0 enxerga bem uma caverna escura;",
                             "valores altos deixam o mundo lavado.",
                             "OBS: com shaderpack (Oculus) o efeito e menor, porque o shader",
                             "manda na iluminacao final.")
                    .defineInRange("flashLightBoost", 6.0D, 0.0D, 20.0D);

            flashLightSeconds = b
                    .comment("Duracao da luz. Um flash e um estouro: 0.4s ja e generoso.")
                    .defineInRange("flashLightSeconds", 0.4D, 0.05D, 5.0D);

            b.pop();

            b.comment("Bateria da camera e o apagao (mini-game do ESPACO).").push("bateria");

            batteryDrains = b
                    .comment("Se false, a bateria nunca acaba e o apagao nunca acontece.")
                    .define("batteryDrains", true);

            batteryDrainPerTick = b
                    .comment("Quanto de carga (%) a bateria perde por tick. 20 ticks = 1 segundo.",
                             "0.012 da cerca de 7 minutos de camera ligada.")
                    .defineInRange("batteryDrainPerTick", 0.012D, 0.0D, 5.0D);

            batteryRechargeAmount = b
                    .comment("Quanto de carga (%) cada pilha devolve.")
                    .defineInRange("batteryRechargeAmount", 50.0D, 1.0D, 100.0D);

            pressesToRecharge = b
                    .comment("Quantos apertos de ESPACO para religar a camera durante o apagao.")
                    .defineInRange("pressesToRecharge", 25, 1, 200);

            blackoutDrainPerTick = b
                    .comment("Quanto a barra do mini-game vaza por tick (dificuldade).",
                             "0.0 = a barra nao vaza, so vai enchendo.")
                    .defineInRange("blackoutDrainPerTick", 0.8D, 0.0D, 10.0D);

            secondsUntilScream = b
                    .comment("Segundos de escuridao ate o grito.")
                    .defineInRange("secondsUntilScream", 5, 0, 120);

            blackoutFootsteps = b
                    .comment("Passos se aproximando depois do grito (cada vez mais rapidos e altos).")
                    .define("blackoutFootsteps", true);

            b.pop();

            b.comment("Fotografia: o flash (R) tira uma foto, e o album abre no C.").push("fotos");

            photos = b
                    .comment("Liga o sistema de fotografia. Desligado, o R so da o clarao.")
                    .define("photos", true);

            maxPhotos = b
                    .comment("Quantas fotos o album guarda. Passou disto, a mais antiga e",
                             "apagada do disco. Cada foto ocupa uns 200 KB.")
                    .defineInRange("maxPhotos", 64, 1, 512);

            photoDevelopSeconds = b
                    .comment("Segundos para revelar uma foto. A espera e proposital: e nela",
                             "que o jogador fica imaginando o que vai aparecer.")
                    .defineInRange("photoDevelopSeconds", 6, 0, 300);

            photoFadeSeconds = b
                    .comment("Segundos que a imagem leva para nascer por cima do filme velado",
                             "quando a revelacao chega a 100%. 0.0 faz aparecer de estalo.")
                    .defineInRange("photoFadeSeconds", 1.5D, 0.0D, 10.0D);

            photoCatchesAnyMob = b
                    .comment("Qualquer mob na frente da lente conta como revelacao.",
                             "LIGADO por enquanto: sem criatura propria no mod, deixar isto",
                             "desligado faria a fotografia inteira nao ter o que revelar.",
                             "Vira false quando as entidades do VHSWORLD existirem — ai uma",
                             "vaca no pasto nao pode mais custar sanidade.")
                    .define("photoCatchesAnyMob", true);

            codex = b
                    .comment("Registro dos itens (tecla G). Cada ficha comeca trancada e so",
                             "abre quando o jogador fotografa o item com o flash — na mao ou",
                             "jogado no chao dentro do enquadramento.",
                             "Desligado, some tambem a frase no tooltip dos itens.")
                    .define("codex", true);

            autoEnableBetaTextures = b
                    .comment("Liga sozinho, UMA UNICA VEZ, os packs de textura beta que ja",
                             "estiverem instalados (Golden Days e afins). Nada e redistribuido:",
                             "so ativamos o que ja esta na maquina do jogador.",
                             "Depois de ligado, fica registrado em disco e nunca mais insiste —",
                             "se voce desligar o pack, ele fica desligado.")
                    .define("autoEnableBetaTextures", true);

            b.pop();

            b.comment("Sanidade. Hoje ela so cai de um jeito: revelando uma foto e",
                      "descobrindo que alguma coisa estava ali com voce.")
             .push("sanidade");

            sanity = b
                    .comment("Liga o sistema de sanidade.")
                    .define("sanity", true);

            sanityBar = b
                    .comment("Mostra a barra com o cerebro na lateral esquerda.")
                    .define("sanityBar", true);

            sanityBarMargin = b
                    .comment("Distancia da barra ate a borda, em pixels. A barra preta do",
                             "letterbox ja e descontada: isto conta a partir do fim dela.")
                    .defineInRange("sanityBarMargin", 14, 0, 400);

            sanityShakeOnHostile = b
                    .comment("Andaime de teste, DESLIGADO: a tela treme ao ver um mob hostil,",
                             "sem tirar sanidade. Serviu para sentir a camera antes de haver",
                             "criatura propria. O susto de verdade e o da foto revelada, e",
                             "espalhar tremor por qualquer slime so gastaria o efeito.")
                    .define("sanityShakeOnHostile", false);

            sanityLossPerSighting = b
                    .comment("Quanto de sanidade (%) cai a cada revelacao com algo na foto.",
                             "18 = cinco ou seis avistamentos ate o fundo do poco.")
                    .defineInRange("sanityLossPerSighting", 18.0D, 0.0D, 100.0D);

            sanityRegenPerMinute = b
                    .comment("Quanto volta por minuto, so passando o tempo. 0.0 (padrao) =",
                             "nao volta sozinha: o caminho de volta e a pilha, nao a espera.")
                    .defineInRange("sanityRegenPerMinute", 0.0D, 0.0D, 100.0D);

            sanityPerBattery = b
                    .comment("Quanto de sanidade (%) cada pilha usada devolve.",
                             "12 contra 18 perdidos por avistamento: da para se recuperar,",
                             "mas custa mais pilha do que o susto rendeu. E o unico caminho",
                             "de volta, e ele passa por um recurso finito.")
                    .defineInRange("sanityPerBattery", 12.0D, 0.0D, 100.0D);

            sanityShakeSeconds = b
                    .comment("Duracao do tremor da tela ao ver a criatura na foto.")
                    .defineInRange("sanityShakeSeconds", 3.0D, 0.0D, 30.0D);

            sanityShakeStrength = b
                    .comment("Forca do tremor. 1.0 = padrao; acima de 2.0 fica dificil de olhar.")
                    .defineInRange("sanityShakeStrength", 1.0D, 0.0D, 3.0D);

            b.comment("Abaixo do limiar a fita comeca a virar contra o jogador, e em zero",
                      "ela esta de vez do outro lado. Nao existe morte por sanidade: o preco",
                      "de ter olhado demais e a camera deixar de ser um lugar seguro.")
             .push("sanidade_baixa");

            sanityThreshold = b
                    .comment("Fracao de sanidade em que a fita comeca a apodrecer. 0.4 = 40%.",
                             "Acima disso nada acontece; de la ate zero o efeito cresce.")
                    .defineInRange("sanityThreshold", 0.4D, 0.0D, 1.0D);

            sanityCorruptsTape = b
                    .comment("Chiado e tracking voltam sozinhos, mesmo desligados no config,",
                             "e as scanlines pesam. A imagem some junto com o juizo.")
                    .define("sanityCorruptsTape", true);

            sanityDrainsBattery = b
                    .comment("A bateria dura ate metade do normal quando a sanidade acaba.")
                    .define("sanityDrainsBattery", true);

            sanityBlackouts = b
                    .comment("A camera apaga sozinha de vez em quando, sem a bateria ter",
                             "acabado. Cai direto no apagao e no mini-game.")
                    .define("sanityBlackouts", true);

            sanityPhantomSounds = b
                    .comment("Passos e gritos ao redor sem nada por perto. Nao ha entidade",
                             "nenhuma: e a fita mentindo para voce.")
                    .define("sanityPhantomSounds", true);

            b.pop();

            b.comment("As engenhocas de camera: lente, pilha corrompida, fita/videocassete",
                      "e tripe. Cada uma mexe em tela, som ou recurso, entao cada uma nasce",
                      "com o seu botao aqui — a mesma regra que salvou o resto do mod.")
             .push("engenhocas");

            infraredLens = b
                    .comment("Lente infravermelha: enquanto na mao, revela os Rasgos da Realidade",
                             "no alcance sem precisar do flash. Nao cobra sanidade — so enxerga.")
                    .define("infraredLens", true);

            infraredTint = b
                    .comment("Tinta verde de visao noturna na tela enquanto a lente esta na mao.")
                    .define("infraredTint", true);

            infraredRange = b
                    .comment("Alcance da revelacao da lente, em blocos.")
                    .defineInRange("infraredRange", 12, 1, 48);

            infraredDrainMultiplier = b
                    .comment("Quanto a lente acelera a descarga da bateria enquanto na mao.",
                             "2.0 = o dobro do normal. O preco de enxergar o invisivel.")
                    .defineInRange("infraredDrainMultiplier", 2.0D, 1.0D, 10.0D);

            corruptedBattery = b
                    .comment("Pilha corrompida: recarrega a camera de vez, mas come sanidade.")
                    .define("corruptedBattery", true);

            corruptedBatteryRecharge = b
                    .comment("Quanto de carga (%) a pilha corrompida devolve. 100 = enche tudo.")
                    .defineInRange("corruptedBatteryRecharge", 100.0D, 1.0D, 100.0D);

            corruptedBatterySanityCost = b
                    .comment("Quanto de sanidade (%) a pilha corrompida cobra por uso.",
                             "15 contra os 12 que a pilha limpa devolve: da para usar na emergencia,",
                             "mas quem vive dela afunda.")
                    .defineInRange("corruptedBatterySanityCost", 15.0D, 0.0D, 100.0D);

            tapes = b
                    .comment("Fita virgem + videocassete: grava alguns segundos de imagem e so",
                             "deixa rever depois, no videocassete. O terror e o atraso.")
                    .define("tapes", true);

            tapeSeconds = b
                    .comment("Duracao da gravacao de uma fita, em segundos.")
                    .defineInRange("tapeSeconds", 10, 1, 60);

            tapeFrameEveryTicks = b
                    .comment("Um quadro a cada tantos ticks (20 = 1s). 10 = dois quadros por",
                             "segundo, cara de fita velha. Menor = mais quadros e mais disco.")
                    .defineInRange("tapeFrameEveryTicks", 10, 2, 40);

            tapeMaxReels = b
                    .comment("Quantas fitas gravadas o videocassete guarda. Passou disto, a mais",
                             "antiga e apagada do disco.")
                    .defineInRange("tapeMaxReels", 12, 1, 64);

            tripod = b
                    .comment("Tripe: plantado no chao, filma por voce. Chegar perto esconde o teu",
                             "HUD e liga o monitor; ele avisa quando algo se mexe no raio.")
                    .define("tripod", true);

            tripodRange = b
                    .comment("Raio de vigilancia do tripe, em blocos. Vale tanto para ligar o",
                             "monitor quanto para o que ele enxerga.")
                    .defineInRange("tripodRange", 16, 2, 64);

            tripodHidesHud = b
                    .comment("Com um tripe por perto, o HUD normal da camera (REC/bateria) some:",
                             "voce esta atras da camera agora, nao na frente.")
                    .define("tripodHidesHud", true);

            tripodSeesHostiles = b
                    .comment("O tripe tambem marca MOTION para mobs hostis do jogo, nao so para",
                             "Rasgos e criaturas do mod. Bom para testar antes de haver anomalia.")
                    .define("tripodSeesHostiles", true);

            tripodBeep = b
                    .comment("O bipe do monitor quando o tripe ve movimento.")
                    .define("tripodBeep", true);

            b.pop();

            b.comment("Os localizadores: depois do rito, 6 min de ponto vermelho atravessando",
                      "parede + o coracao que acelera perto do alvo. A duracao e o rito vivem no",
                      "codigo (server-safe); aqui ficam so os botoes de tela e som.")
             .push("localizadores");

            oreTracker = b
                    .comment("Rastreador de Minerios: marca o minerio valioso mais proximo",
                             "(diamante, esmeralda, ouro, escombro antigo, aluminio e pedra corrompida).")
                    .define("oreTracker", true);

            structureLocator = b
                    .comment("Localizador de Estruturas: marca no horizonte a estrutura mais",
                             "proxima da tag recmod:locatable (vila, mansao, monumento...).")
                    .define("structureLocator", true);

            trackerScanRadius = b
                    .comment("Alcance, em blocos, da varredura do Rastreador de Minerios.",
                             "Tambem e o alcance em que o coracao dele comeca a bater. Maior = mais",
                             "caro de varrer no teu PC enquanto o item esta ativo.")
                    .defineInRange("trackerScanRadius", 24, 4, 64);

            trackerMarker = b
                    .comment("Desenha o ponto vermelho atraves da parede. Desligado, sobra so o coracao.")
                    .define("trackerMarker", true);

            trackerHeartbeat = b
                    .comment("O som do coracao que acelera conforme voce chega perto do alvo.")
                    .define("trackerHeartbeat", true);

            b.pop();

            b.comment("A escolha de dificuldade que abre ao entrar no mundo pela primeira vez.",
                      "A escolha e guardada POR MUNDO. O dificil nao adiciona castigo novo: ele",
                      "aperta o que ja existe. Em NENHUMA das duas se morre de sanidade.")
             .push("dificuldade");

            difficultyPrompt = b
                    .comment("Mostra a tela dos dois cards ao entrar num mundo em que ainda nao",
                             "se escolheu. Desligado, o mundo roda no NORMAL sem perguntar.")
                    .define("difficultyPrompt", true);

            hardSanityMultiplier = b
                    .comment("DIFICIL: quanto revelar uma foto custa de sanidade, vezes isto.",
                             "1.55 leva os 18 pontos do normal para ~28.")
                    .defineInRange("hardSanityMultiplier", 1.55D, 1.0D, 4.0D);

            hardBatteryMultiplier = b
                    .comment("DIFICIL: quanto a bateria gasta por tick, vezes isto.",
                             "1.55 faz a pilha durar cerca de dois tercos do que dura no normal.")
                    .defineInRange("hardBatteryMultiplier", 1.55D, 1.0D, 4.0D);

            hardHauntingMultiplier = b
                    .comment("DIFICIL: frequencia dos apagoes espontaneos e dos sons fantasma da",
                             "sanidade baixa, vezes isto.")
                    .defineInRange("hardHauntingMultiplier", 2.60D, 1.0D, 8.0D);

            hardThresholdBonus = b
                    .comment("DIFICIL: quantos pontos percentuais o limiar da sanidade sobe. O",
                             "limiar e onde a fita comeca a apodrecer — subir faz a degradacao",
                             "comecar antes, com o medidor ainda cheio. NAO tira sanidade.")
                    .defineInRange("hardThresholdBonus", 15, 0, 60);

            b.pop();

            b.comment("A mancha preta que come a tela quando o terreno termina de carregar.",
                      "E a transicao de entrada: sob o preto a escolha de dificuldade abre,",
                      "ou a mancha se recolhe e o mundo ja esta ali.")
             .push("transicao");

            inkTransition = b
                    .comment("Liga a mancha. Desligada, o mundo entra seco, como no vanilla.")
                    .define("inkTransition", true);

            inkSeconds = b
                    .comment("Quanto tempo a mancha leva para comer a tela (e, depois, para se",
                             "recolher). Ela tem que ser RAPIDA: acima de ~1s vira espera.")
                    .defineInRange("inkSeconds", 0.60D, 0.15D, 3.0D);

            b.pop();

            b.comment("A neblina antiga (alpha / r1.6.4) DENTRO das dimensoes do mod.",
                      "No overworld quem faz isto e o NostalgicTweaks, mas ele so trata",
                      "overworld e nether — dimensao de mod ficava com a neblina moderna e o",
                      "jogador via o mundo trocar de linguagem visual ao atravessar a fita.",
                      "Nao mexe na COR: essa vem do bioma de cada dimensao.")
             .push("neblina");

            oldFog = b
                    .comment("Liga a neblina antiga nas dimensoes recmod:*. Desligada, elas voltam",
                             "a usar a neblina do jogo (nitido de perto, corte seco no fim).")
                    .define("oldFog", true);

            oldFogDensity = b
                    .comment("Aperta ou solta a bruma. 1.0 = a curva original do alpha.",
                             "ABAIXO de 1.0 fecha mais perto (0.6 e bem sufocante); acima abre.",
                             "Isto multiplica so a distancia FINAL — o inicio e sempre zero, que",
                             "e o que da o ar de foto velha em vez de parede de neblina.")
                    .defineInRange("oldFogDensity", 1.0D, 0.2D, 2.0D);

            b.pop();

            b.comment("Volume dos sons de terror do mod.").push("audio");

            horrorVolume = b
                    .comment("Multiplicador dos sons do mod (grito, passos, camera).",
                             "0.0 silencia so o REC, sem mexer no volume do Minecraft.")
                    .defineInRange("horrorVolume", 1.0D, 0.0D, 2.0D);

            b.pop();

            b.comment("O que a TELA faz quando uma criatura quebra a propria regra.",
                      "Aqui so mora o aviso; quando e com que frequencia ela quebra fica",
                      "no COMMON, senao cliente e servidor discordariam do jogo.")
             .push("criaturas");

            stonemanSurgeShake = b
                    .comment("Homem de Pedra: a tela treme quando ele anda sendo olhado.")
                    .define("stonemanSurgeShake", true);

            stonemanSurgeStatic = b
                    .comment("Homem de Pedra: chiado por cima da imagem durante o surto.",
                             "Vale mesmo com o chiado normal desligado — este e o aviso de",
                             "que a regra caiu, nao enfeite de fita.")
                    .define("stonemanSurgeStatic", true);

            anomalies = b
                    .comment("Desenha as anomalias 2D. Isto e CLIENT de proposito: elas",
                             "existem no mundo de qualquer jeito, e o que muda aqui e se os",
                             "teus olhos as alcancam. Desligado, o mundo fica com criaturas",
                             "que voce nunca ve — inclusive nas fotos.")
                    .define("anomalies", true);

            anomalyRevealsToManifest = b
                    .comment("Quantas fotos de uma anomalia voce precisa REVELAR para ela",
                             "passar a aparecer a olho nu (so vale para as que sao 'so na",
                             "fita ate voce insistir'). Conta por mundo. Baixo demais e o",
                             "truque queima na primeira noite; alto demais e o jogador nunca",
                             "descobre que insistir tem preco.")
                    .defineInRange("anomalyRevealsToManifest", 3, 1, 64);

            sightSounds = b
                    .comment("Cada criatura tem um som proprio quando entra no teu campo",
                             "de visao. E o que faz o jogador aprender a associar aquele",
                             "ruido AQUELA coisa — depois de um tempo o som sozinho ja",
                             "carrega o medo, sem precisar ver de novo.")
                    .define("sightSounds", true);

            sightSoundRest = b
                    .comment("Segundos ate a MESMA criatura poder assustar de novo. Baixo",
                             "demais e olhar fixo vira metralhadora de susto e o efeito",
                             "morre em dez segundos.")
                    .defineInRange("sightSoundRest", 45, 1, 3600);

            ambientDread = b
                    .comment("Ruidos que tocam do nada. A maior parte do medo acontece",
                             "quando NAO esta acontecendo nada; isto e o que ocupa esse",
                             "silencio.",
                             "⚠️ QUANDO eles tocam nao se ajusta mais aqui: quem decide e o",
                             "Diretor, no lado do servidor ([diretor] no recmod-common).",
                             "O antigo par de min/max saiu justamente por isso — era um",
                             "segundo relogio, e som sorteado sem motivo nenhum e o que",
                             "fazia o jogador parar de escutar. Isto aqui e so o botao de",
                             "desligar, para quem nao aguenta.")
                    .define("ambientDread", true);

            ambientDreadVolume = b
                    .comment("Volume dos ruidos de fundo. Baixo de proposito: tem que caber",
                             "a duvida de ter ouvido mesmo.")
                    .defineInRange("ambientDreadVolume", 0.55D, 0.0D, 1.0D);

            absenceEvidence = b
                    .comment("O VISOR: com a lente infravermelha na mao, o lugar onde havia",
                             "uma coisa sua que sumiu aparece marcado.",
                             "⚠️ Desligar isto NAO desliga a ausencia — o mundo continua",
                             "mexendo nas suas tochas, voce so perde o unico jeito de",
                             "provar. Que e, honestamente, a versao mais cruel do mod.",
                             "Para o mundo parar de mexer, e `absence` no recmod-common.")
                    .define("absenceEvidence", true);

            directorMusic = b
                    .comment("Deste lado, a trilha obedecer ao Diretor quer dizer CALAR quem",
                             "tentar tocar musica por conta propria — a do mod, a do vanilla",
                             "e a de outros mods.",
                             "⚠️ Sem este corte, abrir os intervalos nos JSON nao bastaria: o",
                             "MusicManager nasce com 100 ticks no relogio, entao a primeira",
                             "faixa entra cinco segundos depois de carregar o mundo por mais",
                             "alto que seja o min_delay. Enquanto houver uma segunda pessoa",
                             "decidindo quando ha musica, o silencio do Diretor e so um",
                             "intervalo entre faixas.",
                             "Disco de jukebox nao e afetado: fazer barulho de proposito e",
                             "decisao do jogador.")
                    .define("directorMusic", true);

            dimensionStings = b
                    .comment("Sustos avulsos da DIMENSAO em que voce esta: o grito da",
                             "biblioteca, o rangido de metal dos tuneis de cano, o bumbo",
                             "distorcido da parkourland. Sao as faixas curtas da trilha",
                             "que o Pedro separou por pasta — curtas demais para servirem",
                             "de musica, entao tocam soltas.")
                    .define("dimensionStings", true);

            dimensionStingMinSeconds = b
                    .comment("Menor espera entre dois sustos de dimensao.")
                    .defineInRange("dimensionStingMinSeconds", 120, 5, 3600);

            dimensionStingMaxSeconds = b
                    .comment("Maior espera entre dois sustos de dimensao. Mais espacado que o",
                             "ambientDread de proposito: sao dois sistemas tocando no mesmo",
                             "silencio, e juntos e facil virar barulho constante.")
                    .defineInRange("dimensionStingMaxSeconds", 420, 5, 3600);

            dimensionStingVolume = b
                    .comment("Volume dos sustos de dimensao.")
                    .defineInRange("dimensionStingVolume", 0.70D, 0.0D, 1.0D);

            chaseMusic = b
                    .comment("A trilha que toca enquanto o Cara Cinza esta atras de voce, e",
                             "para quando ele desiste. Ela nao e enfeite: e o unico jeito de",
                             "saber que foi VISTO — a criatura nao grita nem muda de pose.",
                             "Desligada, a perseguicao vira silenciosa (mais cruel, e uma",
                             "escolha valida).")
                    .define("chaseMusic", true);

            chaseMusicVolume = b
                    .comment("Volume da trilha da cacada. Ela toca colada no ouvido (nao vem",
                             "da criatura), como trilha de filme, entao um pouco abaixo do",
                             "teto ja e alto.")
                    .defineInRange("chaseMusicVolume", 0.75D, 0.0D, 1.0D);

            ophanimDrone = b
                    .comment("O zumbido do Ofanim, que cresce conforme voce se aproxima. E o",
                             "que da PESO a uma criatura que nunca se mexe do lugar; sem ele",
                             "uma coisa de treze blocos de altura fica muda como cenario.")
                    .define("ophanimDrone", true);

            ophanimDroneVolume = b
                    .comment("Volume do zumbido no ponto mais perto. Ele ja cai com a",
                             "distancia sozinho.")
                    .defineInRange("ophanimDroneVolume", 0.85D, 0.0D, 1.0D);

            ophanimVertigo = b
                    .comment("O horizonte entorta enquanto o Ofanim esta com os olhos em",
                             "voce, e da um estouro quando ele cobra. Isto e o AVISO da",
                             "mecanica dele: desligado, a criatura continua igual, mas o",
                             "julgamento passa a chegar sem nenhum sinal antes — o que e",
                             "mais cruel e bem menos justo.")
                    .define("ophanimVertigo", true);

            ophanimVertigoStrength = b
                    .comment("Quanto a camera entorta. E o knob de quem passa mal com",
                             "movimento de tela: em 0.3 o efeito ainda se percebe, e a",
                             "informacao (ele esta te olhando) continua chegando.")
                    .defineInRange("ophanimVertigoStrength", 1.0D, 0.0D, 3.0D);

            ophanimGazeStatic = b
                    .comment("A fita suja quando o medidor de olhar dele passa dos 60%.",
                             "Vale mesmo com o chiado normal desligado, pelo mesmo motivo do",
                             "Homem de Pedra: aqui o chiado nao e enfeite, e o relogio.")
                    .define("ophanimGazeStatic", true);

            b.pop();
        }
    }

    // ------------------------------------------------------------------ COMMON

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    public static final class Common {

        public final ForgeConfigSpec.BooleanValue scatterBatteries;
        public final ForgeConfigSpec.IntValue scatterIntervalTicks;
        public final ForgeConfigSpec.DoubleValue scatterChance;
        public final ForgeConfigSpec.IntValue scatterMinRadius;
        public final ForgeConfigSpec.IntValue scatterMaxRadius;
        public final ForgeConfigSpec.IntValue scatterMaxNearby;

        // --- o Diretor: quem conta o compasso do mod inteiro ---
        public final ForgeConfigSpec.BooleanValue director;
        public final ForgeConfigSpec.IntValue directorLongSilenceSeconds;
        public final ForgeConfigSpec.DoubleValue directorNoiseTellRange;
        public final ForgeConfigSpec.BooleanValue absence;
        public final ForgeConfigSpec.BooleanValue directorMusic;
        public final ForgeConfigSpec.DoubleValue directorMusicCutPressure;

        // --- criaturas ---
        public final ForgeConfigSpec.DoubleValue stonemanWatchRange;
        public final ForgeConfigSpec.BooleanValue stonemanSurge;
        public final ForgeConfigSpec.IntValue stonemanSurgeIntervalMin;
        public final ForgeConfigSpec.IntValue stonemanSurgeIntervalMax;
        public final ForgeConfigSpec.DoubleValue stonemanStareBreakSeconds;
        public final ForgeConfigSpec.DoubleValue stonemanSurgeMinSeconds;
        public final ForgeConfigSpec.DoubleValue stonemanSurgeMaxSeconds;
        public final ForgeConfigSpec.DoubleValue stonemanAmbushRange;
        public final ForgeConfigSpec.DoubleValue stonemanAmbushDamage;
        public final ForgeConfigSpec.BooleanValue stonemanBuilds;
        public final ForgeConfigSpec.IntValue stonemanBuildMaxBlocks;
        public final ForgeConfigSpec.IntValue stonemanBuildCooldown;
        public final ForgeConfigSpec.IntValue anomalyLifetimeSeconds;
        public final ForgeConfigSpec.DoubleValue anomalyVanishRange;
        public final ForgeConfigSpec.DoubleValue greyfaceSpeed;
        public final ForgeConfigSpec.DoubleValue greyfaceChaseRange;
        public final ForgeConfigSpec.BooleanValue greyfaceSteps;
        public final ForgeConfigSpec.DoubleValue greyfaceStepVolume;

        // --- a leva das quatro (Observador, Sombra, Silhueta, Rastejo) ---
        public final ForgeConfigSpec.DoubleValue staticWatcherRange;
        public final ForgeConfigSpec.DoubleValue staticWatcherOffSeconds;
        public final ForgeConfigSpec.DoubleValue staticWatcherStep;
        public final ForgeConfigSpec.DoubleValue staticWatcherMinDistance;
        public final ForgeConfigSpec.DoubleValue staticWatcherStrikeRange;
        public final ForgeConfigSpec.IntValue shadeSegmentDarkLevel;
        public final ForgeConfigSpec.DoubleValue shadeSegmentWatchRange;
        public final ForgeConfigSpec.DoubleValue silhouetteRange;
        public final ForgeConfigSpec.DoubleValue silhouetteKeepDistance;
        public final ForgeConfigSpec.IntValue silhouetteCorneredTicks;
        public final ForgeConfigSpec.DoubleValue crawlerWatchRange;
        public final ForgeConfigSpec.DoubleValue crawlerPounceRange;

        // --- o Ofanim: o olhar reciproco ---
        public final ForgeConfigSpec.BooleanValue ophanimInChunks;
        public final ForgeConfigSpec.BooleanValue ophanimGaze;
        public final ForgeConfigSpec.DoubleValue ophanimGazeSeconds;
        public final ForgeConfigSpec.DoubleValue ophanimReleaseFactor;
        public final ForgeConfigSpec.DoubleValue ophanimGazeRange;
        public final ForgeConfigSpec.DoubleValue ophanimApproach;
        public final ForgeConfigSpec.DoubleValue ophanimHoverAbove;
        public final ForgeConfigSpec.DoubleValue ophanimContactRange;
        public final ForgeConfigSpec.BooleanValue ophanimEscalates;
        public final ForgeConfigSpec.DoubleValue ophanimVertigoSeconds;
        public final ForgeConfigSpec.DoubleValue ophanimBlindSeconds;
        public final ForgeConfigSpec.DoubleValue ophanimSanityCost;
        public final ForgeConfigSpec.DoubleValue ophanimReturnSeconds;
        public final ForgeConfigSpec.DoubleValue ophanimSwarmChance;
        public final ForgeConfigSpec.IntValue ophanimSwarmSize;
        public final ForgeConfigSpec.BooleanValue ophanimFlashBurn;
        public final ForgeConfigSpec.DoubleValue ophanimFlashRange;
        public final ForgeConfigSpec.DoubleValue ophanimFlashPushback;
        public final ForgeConfigSpec.DoubleValue ophanimFlashBlindSeconds;

        Common(ForgeConfigSpec.Builder b) {
            b.comment("Pilhas espalhadas pelo chao perto dos jogadores, para achar explorando.")
             .push("pilhas");

            scatterBatteries = b
                    .comment("Liga o espalhamento automatico de pilhas.",
                             "Os bau de estrutura (loot modifiers) continuam funcionando mesmo com isto false.")
                    .define("scatterBatteries", true);

            scatterIntervalTicks = b
                    .comment("Intervalo entre tentativas, em ticks (600 = 30 segundos).")
                    .defineInRange("scatterIntervalTicks", 600, 20, 72000);

            scatterChance = b
                    .comment("Chance por jogador em cada tentativa.")
                    .defineInRange("scatterChance", 0.30D, 0.0D, 1.0D);

            scatterMinRadius = b
                    .comment("Distancia minima do jogador, em blocos.")
                    .defineInRange("scatterMinRadius", 6, 1, 128);

            scatterMaxRadius = b
                    .comment("Distancia maxima do jogador, em blocos.")
                    .defineInRange("scatterMaxRadius", 20, 2, 128);

            scatterMaxNearby = b
                    .comment("Nao gera mais pilhas se ja existirem tantas soltas na area.")
                    .defineInRange("scatterMaxNearby", 3, 1, 64);

            b.pop();

            b.comment("As criaturas do mod. Isto e COMMON (vale no servidor): entidade nunca",
                      "pode ler config de CLIENT, senao derruba servidor dedicado.")
             .push("criaturas");

            stonemanWatchRange = b
                    .comment("Homem de Pedra: de quantos blocos o olhar de um jogador ainda o",
                             "congela. Sem teto, alguem do outro lado do mapa apontado por acaso",
                             "na direcao dele o prenderia para sempre. Parede corta o olhar.")
                    .defineInRange("stonemanWatchRange", 24.0D, 4.0D, 128.0D);

            stonemanSurge = b
                    .comment("Liga o SURTO: de vez em quando ele anda mesmo sendo olhado.",
                             "Desligado, ele volta a ser uma estatua honesta que sempre para",
                             "sob o olhar — mais justo, e bem menos assustador.")
                    .define("stonemanSurge", true);

            stonemanSurgeIntervalMin = b
                    .comment("Menor espera entre surtos espontaneos, em segundos.",
                             "O relogio so corre com um jogador por perto: o susto e do",
                             "encontro, nao do bicho sozinho no meio do mato.")
                    .defineInRange("stonemanSurgeIntervalMin", 15, 1, 3600);

            stonemanSurgeIntervalMax = b
                    .comment("Maior espera entre surtos espontaneos, em segundos.",
                             "A distancia entre os dois numeros e a mecanica inteira: se forem",
                             "iguais, o jogador aprende a contar e o medo vira cronometro.")
                    .defineInRange("stonemanSurgeIntervalMax", 45, 1, 3600);

            stonemanStareBreakSeconds = b
                    .comment("Encarar sem piscar por tantos segundos FORCA um surto.",
                             "E o que impede a solucao chata de fitar o bicho para sempre:",
                             "olhar prende, mas olhar demais quebra a regra.")
                    .defineInRange("stonemanStareBreakSeconds", 5.0D, 0.5D, 120.0D);

            stonemanSurgeMinSeconds = b
                    .comment("Duracao minima do surto, em segundos.")
                    .defineInRange("stonemanSurgeMinSeconds", 2.0D, 0.2D, 30.0D);

            stonemanSurgeMaxSeconds = b
                    .comment("Duracao maxima do surto, em segundos. Curto de proposito: o",
                             "terror e a regra falhar por um instante, nao ele virar mob comum.")
                    .defineInRange("stonemanSurgeMaxSeconds", 4.0D, 0.2D, 30.0D);

            stonemanAmbushRange = b
                    .comment("A que distancia, em blocos, ele da a paulada imediata quando se",
                             "solta — ao surtar colado em voce ou quando voce desvia os olhos",
                             "com ele ja em cima. Fora disso ele so persegue.")
                    .defineInRange("stonemanAmbushRange", 3.0D, 0.0D, 16.0D);

            stonemanAmbushDamage = b
                    .comment("Multiplicador do dano da emboscada sobre o golpe normal.")
                    .defineInRange("stonemanAmbushDamage", 1.75D, 0.5D, 5.0D);

            stonemanBuilds = b
                    .comment("Ele empilha pedregulho para subir atras de voce e poe chao",
                             "para atravessar buraco. Desligado, a torre de terra volta a",
                             "ser um esconderijo perfeito — e ele, um problema resolvido.",
                             "Obedece TAMBEM ao gamerule mobGriefing.")
                    .define("stonemanBuilds", true);

            stonemanBuildMaxBlocks = b
                    .comment("Quantos blocos ele poe de uma vez antes de desistir e",
                             "descansar. E o teto que impede a obra infinita atras de quem",
                             "esta voando ou fora de alcance.")
                    .defineInRange("stonemanBuildMaxBlocks", 12, 1, 256);

            stonemanBuildCooldown = b
                    .comment("Ticks entre um bloco e o proximo (20 = 1 segundo). Baixo",
                             "demais e a torre nasce num piscar; a graca e voce voltar a",
                             "olhar e ela ja estar mais alta.")
                    .defineInRange("stonemanBuildCooldown", 8, 1, 200);

            anomalyLifetimeSeconds = b
                    .comment("Quanto tempo uma anomalia fica no mundo antes de se apagar.",
                             "Elas sao efemeras de proposito: anomalia que espera parada de",
                             "ser examinada vira estatua de museu.")
                    .defineInRange("anomalyLifetimeSeconds", 180, 5, 3600);

            anomalyVanishRange = b
                    .comment("A que distancia, em blocos, ela se desfaz quando voce avanca.",
                             "Nunca deixe o jogador ALCANCAR uma anomalia: de perto, o cartaz",
                             "2D se entrega e o medo acaba junto.")
                    .defineInRange("anomalyVanishRange", 4.0D, 0.0D, 32.0D);

            greyfaceSpeed = b
                    .comment("Velocidade do Cara Cinza, a unica que caca (0.23 = zumbi,",
                             "0.30 = aranha, 0.42 = lobo). Ela precisa ser mais rapida que",
                             "o jogador andando e por volta do jogador correndo: se der para",
                             "fugir andando, ela nao assusta; se for rapida demais, correr",
                             "deixa de ser uma saida e ai nao ha jogo, so castigo.")
                    .defineInRange("greyfaceSpeed", 0.52D, 0.05D, 1.0D);

            greyfaceChaseRange = b
                    .comment("A que distancia, em blocos, a perseguicao conta como perseguicao",
                             "— e a trilha toca. Alem disto ela pode continuar te procurando,",
                             "mas em silencio.")
                    .defineInRange("greyfaceChaseRange", 40.0D, 4.0D, 128.0D);

            greyfaceSteps = b
                    .comment("Passos altos do Cara Cinza. Servem de radar honesto: da para",
                             "saber de onde ela vem sem ter visto nada. Isto e COMMON porque o",
                             "som sai do servidor — passo pesado todo mundo ouve.")
                    .define("greyfaceSteps", true);

            greyfaceStepVolume = b
                    .comment("Volume do passo. Acima de 1.0 o Minecraft tambem AUMENTA O",
                             "ALCANCE do som, e e isso que se quer aqui: ouvir de longe.")
                    .defineInRange("greyfaceStepVolume", 1.8D, 0.0D, 4.0D);

            // ---------------- O Observador Estatico ----------------

            staticWatcherRange = b
                    .comment("Observador Estatico: de quantos blocos o seu olhar ainda o apaga",
                             "— e ate onde ele procura para onde voltar. Alto de proposito: o",
                             "bicho e feito para ser notado LA LONGE, no alto de um morro. De",
                             "perto ele ja perdeu a graca.")
                    .defineInRange("staticWatcherRange", 64.0D, 8.0D, 192.0D);

            staticWatcherOffSeconds = b
                    .comment("Quantos segundos ele fica apagado antes de tentar voltar.",
                             "Curto demais e ele pisca na sua cara e vira efeito visual;",
                             "longo demais e voce esquece que ele existe — e o susto mora",
                             "justamente em lembrar dele quando ja e tarde.")
                    .defineInRange("staticWatcherOffSeconds", 4.0D, 0.5D, 120.0D);

            staticWatcherStep = b
                    .comment("Quanto da distancia atual ele fecha a cada volta (0.25 = um",
                             "quarto). E o preco de cada piscada sua. Como e proporcional, a",
                             "aproximacao comeca larga e vai ficando fina — ele nunca chega",
                             "por um salto, sempre por uma conta que voce podia ter feito.")
                    .defineInRange("staticWatcherStep", 0.25D, 0.02D, 0.9D);

            staticWatcherMinDistance = b
                    .comment("Ele nunca reaparece mais perto que isto, em blocos. Sem o piso,",
                             "a aproximacao proporcional acabaria colocando ele DENTRO de voce.")
                    .defineInRange("staticWatcherMinDistance", 3.0D, 1.0D, 32.0D);

            staticWatcherStrikeRange = b
                    .comment("A que distancia olhar para ele deixa de apaga-lo e passa a",
                             "custar caro: ele bate, cega por 3 segundos e recomeca de longe.",
                             "E o unico dano que ele da no jogo inteiro. Sem isso a aproximacao",
                             "nao teria consequencia nenhuma e ele seria so um enfeite que anda.")
                    .defineInRange("staticWatcherStrikeRange", 3.5D, 0.0D, 16.0D);

            // ---------------- O Anomalo da Sombra ----------------

            shadeSegmentDarkLevel = b
                    .comment("Anomalo da Sombra: ate este nivel de luz ele consegue se soltar.",
                             "A luz e medida NO BLOCO DELE, nao no seu — por isso jogar uma",
                             "tocha em cima dele o prende mesmo com voce no breu. 7 e o mesmo",
                             "limiar que o Minecraft usa para nascer monstro; abaixo de 4 ele",
                             "so anda em breu quase total.")
                    .defineInRange("shadeSegmentDarkLevel", 4, 0, 15);

            shadeSegmentWatchRange = b
                    .comment("De quantos blocos o seu olhar tambem o prende, alem da luz. Esta",
                             "segunda trava existe para quem foi pego sem tocha: virar a cabeca",
                             "ainda compra um segundo. Sem ela, ficar sem carvao no escuro",
                             "seria morte anunciada, e o jogo viraria gerencia de inventario.")
                    .defineInRange("shadeSegmentWatchRange", 20.0D, 0.0D, 96.0D);

            // ---------------- A Silhueta Invertida ----------------

            silhouetteRange = b
                    .comment("Silhueta Invertida: de quantos blocos ela repara em voce. Fora",
                             "disso ela fica parada, parecendo um jogador no horizonte — que e",
                             "o estado em que ela passa a maior parte da vida.")
                    .defineInRange("silhouetteRange", 48.0D, 8.0D, 128.0D);

            silhouetteKeepDistance = b
                    .comment("A distancia, em blocos, que ela nao deixa voce furar. Ela recua",
                             "de frente, sem virar as costas, para manter exatamente isto.",
                             "Perto demais o disfarce de jogador cai; longe demais voce nunca",
                             "chega perto o bastante para desconfiar que aquilo nao e gente.")
                    .defineInRange("silhouetteKeepDistance", 12.0D, 2.0D, 64.0D);

            silhouetteCorneredTicks = b
                    .comment("Quantos ticks ela pode querer recuar sem sair do lugar antes de",
                             "simplesmente deixar de estar la (20 = 1 segundo). E o que impede",
                             "de encurralar ela num canto: alcancar a silhueta responderia a",
                             "unica pergunta que ela existe para nao responder.")
                    .defineInRange("silhouetteCorneredTicks", 30, 5, 600);

            // ---------------- O Rastreador do Rastejo ----------------

            crawlerWatchRange = b
                    .comment("Rastreador do Rastejo: de quantos blocos o seu olhar o faz parar.",
                             "Ele so avanca enquanto voce esta ocupado — de costas, minerando,",
                             "ou com um bau aberto na tela.")
                    .defineInRange("crawlerWatchRange", 32.0D, 4.0D, 128.0D);

            crawlerPounceRange = b
                    .comment("A partir daqui ele ignora o seu olhar e vem assim mesmo: e bote,",
                             "nao mais aproximacao. Sem isto ele congelaria a um passo de voce",
                             "e viraria brinquedo — bastaria encarar para nunca mais apanhar.")
                    .defineInRange("crawlerPounceRange", 6.0D, 0.0D, 32.0D);

            ophanimInChunks = b
                    .comment("O Ofanim mora no ceu da dimensao CHUNKS, mantido em UM por um",
                             "Diretor. Desligado, a CHUNKS volta a ser paisagem vazia — o que",
                             "e uma escolha valida se voce so quer explorar as colunas.",
                             "NAO ligue spawn natural no lugar disto: num bioma onde ela e o",
                             "unico monstro, a anomalia enche o teto de spawn do jogo sozinha",
                             "(foi assim que a DATA chegou a mais de cem e 1 fps).")
                    .define("ophanimInChunks", true);

            ophanimGaze = b
                    .comment("A REGRA DO OFANIM: ele so se mexe enquanto voce esta olhando",
                             "para ele, e olhar demais tem preco. Desligado, ele volta a ser",
                             "a presenca parada que era antes da 1.63.0 — continua enorme e",
                             "continua zumbindo, mas nao vem, nao cobra e nao tem saida",
                             "porque nao tem ameaca.")
                    .define("ophanimGaze", true);

            ophanimGazeSeconds = b
                    .comment("Segundos de olhar CONTINUO ate ele cobrar. E o cronometro que o",
                             "jogador aprende a sentir sem nunca ver: baixo demais e olhar",
                             "para o ceu vira roleta, alto demais e da para encarar a coisa a",
                             "vontade e a fuga deixa de existir.")
                    .defineInRange("ophanimGazeSeconds", 8.0D, 1.0D, 120.0D);

            ophanimReleaseFactor = b
                    .comment("Quao mais devagar o medidor DESCE quando voce desvia os olhos.",
                             "0.5 = leva o dobro do tempo para esvaziar do que levou para",
                             "encher. Isto e o coracao do balanceamento: em 1.0 a jogada otima",
                             "vira piscar em ritmo e a criatura vira um jogo de compasso; em",
                             "0.5 desviar alivia, mas voce continua devendo o que ja deu.")
                    .defineInRange("ophanimReleaseFactor", 0.5D, 0.05D, 4.0D);

            ophanimGazeRange = b
                    .comment("De quantos blocos o olhar dele ainda alcanca voce. Coluna no meio",
                             "do caminho corta — e, na CHUNKS, coluna e o unico esconderijo.")
                    .defineInRange("ophanimGazeRange", 96.0D, 8.0D, 256.0D);

            ophanimApproach = b
                    .comment("Blocos por tick que ele desliza na sua direcao ENQUANTO esta",
                             "sendo olhado (0.09 = 1.8 blocos por segundo, pouco mais lento",
                             "que um jogador andando). Ele atravessa o vazio em linha reta,",
                             "porque o vazio nao e obstaculo para quem voa.")
                    .defineInRange("ophanimApproach", 0.09D, 0.0D, 1.0D);

            ophanimHoverAbove = b
                    .comment("Quantos blocos acima dos SEUS OLHOS ele mira ao se aproximar.",
                             "Chegando por cima, quanto mais perto mais voce precisa levantar",
                             "a cabeca — e levantar a cabeca e continuar olhando. A propria",
                             "aproximacao dele cobra mais medidor.")
                    .defineInRange("ophanimHoverAbove", 9.0D, 0.0D, 64.0D);

            ophanimContactRange = b
                    .comment("Deixar ele chegar a esta distancia cobra na hora, com o medidor",
                             "pela metade ou vazio. Sem isto, correr para debaixo dele seria",
                             "uma forma de escapar do julgamento — e deixar a coisa encostar",
                             "tem que ser o pior desfecho, nao o melhor.")
                    .defineInRange("ophanimContactRange", 6.0D, 0.0D, 32.0D);

            ophanimEscalates = b
                    .comment("O castigo SOBE a cada julgamento: vertigem, depois cegueira,",
                             "depois ele te leva para outra coluna — e volta ao comeco.",
                             "Desligado, e sempre vertigem. Sempre no maximo tambem nao seria",
                             "melhor: o jogador aprenderia a nunca mais chegar perto, e a",
                             "criatura viraria uma parede em vez de um encontro.")
                    .define("ophanimEscalates", true);

            ophanimVertigoSeconds = b
                    .comment("Duracao da VERTIGEM (o primeiro castigo). A tela perde o prumo,",
                             "mas voce continua enxergando tudo — inclusive onde acaba o chao.")
                    .defineInRange("ophanimVertigoSeconds", 9.0D, 0.5D, 60.0D);

            ophanimBlindSeconds = b
                    .comment("Duracao da CEGUEIRA (o segundo castigo). Cinco segundos parado",
                             "no meio de uma ponte de oito blocos e o castigo exato desta",
                             "dimensao: ele nao te mata, voce e que pode se matar. Subir muito",
                             "isto passa de tensao para castigo puro.")
                    .defineInRange("ophanimBlindSeconds", 5.0D, 0.5D, 60.0D);

            ophanimSanityCost = b
                    .comment("Sanidade que um julgamento custa (o terceiro estagio cobra o",
                             "dobro do primeiro).",
                             "⚠️ ATE A 1.62.0 REVELAR UMA FOTO ERA A UNICA FONTE DE PERDA DE",
                             "SANIDADE NO MOD. Esta e a segunda, e ela obedece ao mesmo",
                             "principio: so cobra de quem ESCOLHEU olhar. Em 0 a mecanica",
                             "inteira continua de pe sem tocar na sanidade.")
                    .defineInRange("ophanimSanityCost", 6.0D, 0.0D, 100.0D);

            ophanimReturnSeconds = b
                    .comment("Segundos que ele fica fora depois de cobrar. E o PISO DE",
                             "SILENCIO: sem ele o Ofanim voltaria no mesmo minuto e o castigo",
                             "perderia o peso de ter acabado. O terror mora no intervalo.")
                    .defineInRange("ophanimReturnSeconds", 45.0D, 0.0D, 600.0D);

            ophanimSwarmChance = b
                    .comment("Chance de o Diretor mandar um CERCO em vez de um Ofanim so:",
                             "tres de uma vez, um em cada lado do jogador, e mais perto.",
                             "No cerco, OLHAR PARA UM FAZ OS TRES ANDAREM — mas so o que voce",
                             "olhou enche o medidor. A saida continua sendo a mesma (nao olhar",
                             "para nenhum), so que agora ela custa de verdade.",
                             "Raro de proposito: se todo encontro fosse cerco, o Ofanim",
                             "solitario deixaria de assustar e o cerco viraria a rotina.")
                    .defineInRange("ophanimSwarmChance", 0.15D, 0.0D, 1.0D);

            ophanimSwarmSize = b
                    .comment("Quantos vem no cerco. Tres e o numero que cerca sem entupir o",
                             "ceu — sao treze blocos de cartaz cada um. Se a planta nao tiver",
                             "coluna que sirva para todos, entram menos.")
                    .defineInRange("ophanimSwarmSize", 3, 2, 6);

            ophanimFlashBurn = b
                    .comment("O CLARAO QUEIMA O OLHO DELE: zera o medidor, empurra ele para",
                             "longe e o deixa cego por uns segundos. E a unica coisa no mod",
                             "que empurra uma anomalia, e existe para a camera poder AGIR, e",
                             "nao so olhar. Desligado, a unica saida volta a ser baixar os",
                             "olhos e andar.")
                    .define("ophanimFlashBurn", true);

            ophanimFlashRange = b
                    .comment("De quantos blocos o flash ainda o alcanca. Ele precisa estar",
                             "ENQUADRADO: clarao de costas nao queima nada.")
                    .defineInRange("ophanimFlashRange", 64.0D, 4.0D, 256.0D);

            ophanimFlashPushback = b
                    .comment("Quantos blocos ele recua com o clarao. Recua na linha do teu",
                             "olhar e mantendo a altura — o que se quer e ve-lo ficar PEQUENO.")
                    .defineInRange("ophanimFlashPushback", 28.0D, 0.0D, 128.0D);

            ophanimFlashBlindSeconds = b
                    .comment("Segundos em que ele fica cego depois do clarao: nao ganha medidor",
                             "e nao anda. E a janela em que da para atravessar a ponte olhando",
                             "para a frente. O preco ja foi pago na bateria.")
                    .defineInRange("ophanimFlashBlindSeconds", 6.0D, 0.0D, 60.0D);

            b.pop();

            // ------------------------------------------------------------ diretor

            b.comment("O DIRETOR: quem decide QUANDO qualquer coisa acontece com o",
                      "jogador. Antes nao existia — cada mecanica tinha o proprio",
                      "relogio e nenhuma sabia da outra, e era isso que produzia tres",
                      "sustos num minuto e vinte minutos de nada. Ver Director.java.")
             .push("diretor");

            director = b
                    .comment("Liga o Diretor. Desligado, o mod volta ao que era: spawn",
                             "vanilla puro e som ambiente solto. Nao e um modo mais facil,",
                             "e um modo SEM RITMO — o medo fica por conta da sorte.")
                    .define("director", true);

            directorLongSilenceSeconds = b
                    .comment("Depois de tantos segundos sem NADA acontecer, o Diretor pode",
                             "quebrar a calmaria com um ruido mentiroso — som sem nenhuma",
                             "criatura por tras.",
                             "⚠️ A MENTIRA E O QUE FAZ A VERDADE FUNCIONAR. Se o ruido so",
                             "tocasse com bicho por perto, o jogador aprenderia em uma noite",
                             "que som = ameaca, e o som viraria um radar util em vez de uma",
                             "duvida. Ele precisa mentir as vezes para nunca ser confiavel.")
                    .defineInRange("directorLongSilenceSeconds", 150, 20, 3600);

            directorNoiseTellRange = b
                    .comment("De quantos blocos uma criatura nossa ainda DENUNCIA a posicao",
                             "dela pelo ruido ambiente. Dentro disso, o som sai da direcao",
                             "dela (torta de proposito, e nunca na distancia real: ele diz",
                             "mais ou menos ONDE, jamais O QUE nem QUAO PERTO).")
                    .defineInRange("directorNoiseTellRange", 40.0D, 8.0D, 128.0D);

            absence = b
                    .comment("A AUSENCIA: o mundo mexe, pelas suas costas, em coisas que",
                             "VOCE colocou. A tocha que voce fincou nao esta mais la; a",
                             "porta que voce fechou esta aberta.",
                             "So mexe em tocha e porta, so no que voce mesmo colocou, so",
                             "fora do seu campo de visao e so entre 8 e 64 blocos. Nunca",
                             "encosta em bau nem em construcao: perda de progresso nao e",
                             "medo, e raiva.",
                             "⚠️ Sem a lente na mao nao ha como provar que aconteceu — e e",
                             "esse o ponto. Ver o visor em AbsenceEvidence.")
                    .define("absence", true);

            directorMusic = b
                    .comment("O DIRETOR MANDA NA TRILHA. Silencio vira o estado padrao e a",
                             "musica so entra quando ele decide — e some quando a pressao",
                             "sobe.",
                             "⚠️ Desligado, a trilha volta ao que era ate a v1.76.3: os",
                             "biomas tem min_delay/max_delay ZERO, entao uma faixa emenda na",
                             "outra sem parar. Isso nao e 'mais musica', e o Diretor ficar",
                             "inaudivel: ele nega batidas para comprar dois minutos de vazio",
                             "e a trilha enche esse vazio inteiro. O silencio e o quarto",
                             "estado das leis de dimensao; com isto desligado ele nao existe.")
                    .define("directorMusic", true);

            directorMusicCutPressure = b
                    .comment("Acima desta pressao a faixa e CORTADA na hora, sem esperar piso",
                             "nem nada. A musica sumir sozinha quando algo se aproxima e a",
                             "ferramenta de terror mais barata que este mod tem — o jogador",
                             "nao sabe por que parou, so sabe que parou.",
                             "Baixo demais e a trilha nunca toca; alto demais e ela insiste",
                             "por cima do encontro, que e trilha de filme e nao terror.")
                    .defineInRange("directorMusicCutPressure", 0.45D, 0.0D, 1.0D);

            b.pop();
        }
    }

    // ------------------------------------------------------------------ setup

    static {
        Pair<Client, ForgeConfigSpec> client = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = client.getLeft();
        CLIENT_SPEC = client.getRight();

        Pair<Common, ForgeConfigSpec> common = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = common.getLeft();
        COMMON_SPEC = common.getRight();
    }

    private RECConfig() {}
}
