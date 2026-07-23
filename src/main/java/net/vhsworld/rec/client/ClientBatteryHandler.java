package net.vhsworld.rec.client;

import net.vhsworld.rec.config.RECConfig;

// Só é carregada no lado do cliente (via DistExecutor) para mexer no HUD da filmadora.
public class ClientBatteryHandler {

    // Quanto de carga (%) cada pilha devolve para a bateria da câmera.
    public static float rechargeAmount() {
        return RECConfig.CLIENT.batteryRechargeAmount.get().floatValue();
    }

    public static void recharge() {
        float amount = rechargeAmount();

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
