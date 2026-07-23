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

    // ------------------------------------------------------------------ CLIENT

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    public static final class Client {

        // --- camera ---
        public final ForgeConfigSpec.BooleanValue effectsOnlyWhenHolding;

        public final ForgeConfigSpec.BooleanValue viewfinder;
        public final ForgeConfigSpec.DoubleValue viewfinderOpacity;

        public final ForgeConfigSpec.BooleanValue fisheye;
        public final ForgeConfigSpec.DoubleValue fisheyeStrength;

        public final ForgeConfigSpec.BooleanValue handShake;
        public final ForgeConfigSpec.DoubleValue handShakeStrength;

        // --- hud ---
        public final ForgeConfigSpec.BooleanValue showHud;
        public final ForgeConfigSpec.BooleanValue showRecBlink;

        // --- flash ---
        public final ForgeConfigSpec.BooleanValue screenFlash;
        public final ForgeConfigSpec.DoubleValue flashFadeSpeed;
        public final ForgeConfigSpec.IntValue flashChargeTicks;

        // --- bateria / apagao ---
        public final ForgeConfigSpec.BooleanValue batteryDrains;
        public final ForgeConfigSpec.DoubleValue batteryDrainPerTick;
        public final ForgeConfigSpec.DoubleValue batteryRechargeAmount;
        public final ForgeConfigSpec.IntValue pressesToRecharge;
        public final ForgeConfigSpec.DoubleValue blackoutDrainPerTick;
        public final ForgeConfigSpec.IntValue secondsUntilScream;
        public final ForgeConfigSpec.BooleanValue blackoutFootsteps;

        // --- audio ---
        public final ForgeConfigSpec.DoubleValue horrorVolume;

        Client(ForgeConfigSpec.Builder b) {
            b.comment("Visual da filmadora. Tudo aqui e client-side: nao muda regra de jogo,",
                      "so como a tela se comporta no teu PC.")
             .push("camera");

            effectsOnlyWhenHolding = b
                    .comment("Se true, moldura/fisheye/tremida so aparecem com a filmadora na mao.",
                             "Se false (padrao), a camera fica sempre ligada, como nas versoes antigas.")
                    .define("effectsOnlyWhenHolding", false);

            viewfinder = b
                    .comment("Moldura preta do visor (vignette) por cima do mundo.")
                    .define("viewfinder", true);

            viewfinderOpacity = b
                    .comment("Opacidade da moldura. 0.0 = invisivel, 1.0 = cheia.")
                    .defineInRange("viewfinderOpacity", 1.0D, 0.0D, 1.0D);

            fisheye = b
                    .comment("Distorcao de lente (aumento do FOV) enquanto a camera esta ativa.")
                    .define("fisheye", true);

            fisheyeStrength = b
                    .comment("Multiplicador do FOV. 1.0 = sem distorcao, 1.35 = padrao.",
                             "Valores altos causam enjoo em muita gente; nao passe de 1.6.")
                    .defineInRange("fisheyeStrength", 1.35D, 1.0D, 2.0D);

            handShake = b
                    .comment("Balanco de mao (a camera respira junto com o jogador).")
                    .define("handShake", true);

            handShakeStrength = b
                    .comment("Amplitude do balanco, em graus. 0.0 desliga na pratica.")
                    .defineInRange("handShakeStrength", 0.18D, 0.0D, 1.0D);

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
                    .comment("Ticks segurando R para carregar o flash por completo (20 ticks = 1s).")
                    .defineInRange("flashChargeTicks", 60, 1, 600);

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
