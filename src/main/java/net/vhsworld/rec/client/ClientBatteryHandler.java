package net.vhsworld.rec.client;

// Só é carregada no lado do cliente (via DistExecutor) para mexer no HUD da filmadora.
public class ClientBatteryHandler {

    // Quanto de carga (%) cada pilha devolve para a bateria da câmera.
    public static final float RECHARGE_AMOUNT = 50.0f;

    public static void recharge() {
        // Se a câmera estava desligada (apagão), religa
        if (CamcorderOverlay.isBatteryDead) {
            CamcorderOverlay.isBatteryDead = false;
            CamcorderOverlay.miniGameProgress = 0.0f;
            CamcorderOverlay.batteryLevel = RECHARGE_AMOUNT;
        } else {
            CamcorderOverlay.batteryLevel = Math.min(100.0f, CamcorderOverlay.batteryLevel + RECHARGE_AMOUNT);
        }
    }
}
