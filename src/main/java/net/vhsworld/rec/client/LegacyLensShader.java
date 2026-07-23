package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;

import java.lang.reflect.Method;

/**
 * A lente de verdade da v1.0.0 (post-shader recmod:fisheye), de volta — mas com trava.
 *
 * O BUG ORIGINAL: um post-shader vanilla assume o framebuffer principal do jogo.
 * Oculus/Iris tambem assume. Com os dois ligados o frame saia corrompido, e o sintoma
 * mais visivel eram as chunks distantes quebradas. Na v1.0.3 o efeito foi simplesmente
 * removido.
 *
 * A TRAVA: este handler so carrega o efeito quando NENHUM shaderpack esta em uso.
 * Se o jogador ligar um shaderpack com a lente ativa, o efeito e descarregado sozinho
 * no mesmo tick. A checagem e por reflexao — o mod nao depende do Iris para compilar,
 * e se a API nao responder assumimos o pior (nao carrega).
 *
 * Por isso este knob nasce DESLIGADO. O visual de fita do dia a dia vem do
 * VHSEffectOverlay, que e seguro por construcao.
 */
public final class LegacyLensShader {

    private static final ResourceLocation EFFECT =
            new ResourceLocation(RECMod.MOD_ID, "shaders/post/fisheye.json");

    private static boolean loaded = false;

    // Reflexao resolvida uma vez so.
    private static boolean irisChecked = false;
    private static Object irisApi = null;
    private static Method isShaderPackInUse = null;

    public static void tick(Minecraft mc) {
        if (mc.level == null) {
            loaded = false;
            return;
        }

        boolean wanted = RECConfig.CLIENT.lensPostShader.get()
                && CameraState.isActive()
                && !shaderPackInUse();

        if (wanted && !loaded) {
            mc.gameRenderer.loadEffect(EFFECT);
            loaded = true;
        } else if (!wanted && ownsCurrentEffect(mc)) {
            mc.gameRenderer.shutdownEffect();
            loaded = false;
        } else if (!wanted) {
            loaded = false;
        }
    }

    /** Só desliga efeito que é nosso — o jogo usa post-shaders proprios (creeper, spider...). */
    private static boolean ownsCurrentEffect(Minecraft mc) {
        return mc.gameRenderer.currentEffect() != null
                && mc.gameRenderer.currentEffect().getName().contains(RECMod.MOD_ID);
    }

    /** true se houver shaderpack ativo — ou se nao der para ter certeza. */
    private static boolean shaderPackInUse() {
        if (!irisChecked) {
            irisChecked = true;
            boolean present = ModList.get().isLoaded("oculus") || ModList.get().isLoaded("iris");
            if (present) {
                try {
                    Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                    irisApi = api.getMethod("getInstance").invoke(null);
                    isShaderPackInUse = api.getMethod("isShaderPackInUse");
                } catch (Throwable ignored) {
                    // API mudou de lugar: preferimos perder o efeito a corromper o render.
                    irisApi = null;
                    isShaderPackInUse = null;
                }
            }
        }

        boolean present = ModList.get().isLoaded("oculus") || ModList.get().isLoaded("iris");
        if (!present) return false;                       // sem Iris/Oculus, caminho livre
        if (isShaderPackInUse == null) return true;       // instalado mas ilegivel: nao arrisca

        try {
            return (boolean) isShaderPackInUse.invoke(irisApi);
        } catch (Throwable ignored) {
            return true;
        }
    }

    private LegacyLensShader() {}
}
