package net.vhsworld.rec.client;

import net.vhsworld.rec.client.sanity.SanityState;
import net.vhsworld.rec.config.RECConfig;

// Só é carregada no lado do cliente (via DistExecutor) para mexer no HUD da filmadora.
public class ClientBatteryHandler {

    // Quanto de carga (%) cada pilha devolve para a bateria da câmera.
    public static float rechargeAmount() {
        return RECConfig.CLIENT.batteryRechargeAmount.get().floatValue();
    }

    public static void recharge() {
        float amount = rechargeAmount();

        // A pilha nao repoe só a câmera: repõe o jogador junto.
        SanityState.get().restore(RECConfig.CLIENT.sanityPerBattery.get().floatValue());

        apply(amount);
    }

    /**
     * A pilha corrompida: enche a bateria (mais que a limpa) mas cobra do juizo, em vez
     * de devolver. Mesmo caminho client-side da pilha comum — por isso nao precisou de
     * rede nem de capability: quem ja mexia na sanidade daqui continua mexendo daqui.
     */
    public static void rechargeCorrupted() {
        if (!RECConfig.CLIENT.corruptedBattery.get()) return;

        float amount = RECConfig.CLIENT.corruptedBatteryRecharge.get().floatValue();

        SanityState.get().drain(RECConfig.CLIENT.corruptedBatterySanityCost.get().floatValue());

        apply(amount);
    }

    private static void apply(float amount) {
        // Se a câmera estava desligada (apagão), religa
        if (CamcorderOverlay.isBatteryDead) {
            CamcorderOverlay.isBatteryDead = false;
            CamcorderOverlay.miniGameProgress = 0.0f;
            CamcorderOverlay.batteryLevel = amount;
        } else {
            CamcorderOverlay.batteryLevel = Math.min(100.0f, CamcorderOverlay.batteryLevel + amount);
        }
    }
}
