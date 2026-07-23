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

        // --- audio ---
        public final ForgeConfigSpec.DoubleValue horrorVolume;

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
                    .comment("Se true, qualquer mob na frente da lente conta como revelacao.",
                             "Padrao false: so as criaturas do proprio mod aparecem no filme,",
                             "para uma vaca no pasto nao virar o susto da foto.")
                    .define("photoCatchesAnyMob", false);

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

            b.comment("Volume dos sons de terror do mod.").push("audio");

            horrorVolume = b
                    .comment("Multiplicador dos sons do mod (grito, passos, camera).",
                             "0.0 silencia so o REC, sem mexer no volume do Minecraft.")
                    .defineInRange("horrorVolume", 1.0D, 0.0D, 2.0D);

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
