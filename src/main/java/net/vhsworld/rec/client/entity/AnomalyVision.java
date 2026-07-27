package net.vhsworld.rec.client.entity;

import net.vhsworld.rec.client.photo.AnomalySightings;
import net.vhsworld.rec.client.photo.PhotoCapture;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.entity.AnomalyType;

/**
 * Os seus olhos alcancam esta anomalia AGORA?
 *
 * Este arquivo e o sistema inteiro em quatro linhas de regra. O corpo da anomalia
 * esta sempre la, no servidor; aqui se decide se ele chega ate a tela.
 *
 * O TRUQUE DA FOTO: o PhotoCapture guarda o frame no AFTER_WEATHER, ou seja DEPOIS
 * que as entidades ja foram desenhadas. Entao, no frame em que o flash dispara,
 * basta deixar a anomalia ser desenhada — ela entra na foto por consequencia, e o
 * jogador ve um lampejo de UM frame. Aquele "acho que vi alguma coisa" nao foi
 * programado: e o mesmo frame que virou fotografia.
 */
public final class AnomalyVision {

    private AnomalyVision() {}

    /** Desenha? */
    public static boolean canSee(AnomalyType type) {
        if (!RECConfig.CLIENT.anomalies.get()) return false;

        switch (type.visibility()) {
            case ALWAYS:
                return true;

            case TAPE_ONLY:
                return flashing();

            case TAPE_THEN_REAL:
                return flashing() || manifested(type);

            default:
                return false;
        }
    }

    /** O frame do clarao — o unico em que a fita ve o que os olhos nao veem. */
    private static boolean flashing() {
        return PhotoCapture.isCapturing();
    }

    /**
     * Ela ja saiu da fita?
     *
     * Precisa de tantas revelacoes quantas o config pedir. Note que a conta e por
     * MUNDO e so sobe revelando: quem nunca abriu o album nunca a traz para ca, por
     * mais fotos que tire. Insistir e olhar de novo, nao clicar de novo.
     */
    private static boolean manifested(AnomalyType type) {
        int needed = RECConfig.CLIENT.anomalyRevealsToManifest.get();
        return AnomalySightings.get().count(type) >= needed;
    }
}
