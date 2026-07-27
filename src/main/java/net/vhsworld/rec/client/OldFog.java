package net.vhsworld.rec.client;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.config.RECConfig;

/**
 * A neblina antiga (alpha / r1.6.4) dentro das dimensoes do mod.
 *
 * POR QUE ISTO EXISTE: o overworld ja tem a neblina antiga, mas ela vem do
 * NostalgicTweaks — e o NT trava o efeito em "estou no overworld OU no nether"
 * (GameUtil.isInOverworld / isInNether, conferido no jar dele). Dimensao registrada
 * por mod nao e nem uma coisa nem outra, entao ela recebia a neblina MODERNA e o
 * jogador atravessava a fita e via o mundo mudar de linguagem visual junto.
 * Aqui a mesma curva e refeita para o nosso lado, sem depender do NT estar instalado.
 *
 * O QUE MUDA, na pratica: a neblina moderna comeca colada no fim da distancia de
 * render (start = far - clamp(rd/10, 4, 64)) e tem forma de CILINDRO — ou seja, tudo
 * nitido e um corte repentino no fim. A antiga comeca em ZERO e e uma ESFERA: a bruma
 * cresce desde a camera, e o longe vira sugestao em vez de recorte. E dai que vem a
 * cara de foto velha; o numero do fim importa menos que o inicio ser zero.
 *
 * A COR nao e mexida aqui de proposito. Ela vem do bioma de cada dimensao
 * (effects.fog_color), que e coisa autorada por nos arquivo a arquivo — o preto da
 * DATA e escolha, nao acidente.
 *
 * 🔑 A pegadinha que faz isto funcionar esta no setCanceled do fim do metodo.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OldFog {

    private OldFog() {}

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!RECConfig.CLIENT.oldFog.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // So as dimensoes do mod, e a conta e pelo NAMESPACE, nao por uma lista de
        // nomes: as 21 dimensoes planejadas nascem todas como "recmod:<nome>", entao
        // cada uma nova ja entra com a neblina certa sem ninguem precisar lembrar de
        // voltar neste arquivo.
        if (!RECMod.MOD_ID.equals(mc.level.dimension().location().getNamespace())) return;

        // Dentro de agua, lava ou po de neve o jogo ja poe a propria neblina, e ela e
        // MUITO mais apertada que esta. Sobrepor a nossa por cima devolveria visao de
        // longe debaixo d'agua, que e o contrario do que se quer.
        if (event.getType() != FogType.NONE) return;

        // Cegueira e escuridao tambem tem neblina propria, e ela e o efeito inteiro:
        // se a gente reescrever a distancia, o jogador cego enxerga.
        if (mc.player != null
                && (mc.player.hasEffect(MobEffects.BLINDNESS) || mc.player.hasEffect(MobEffects.DARKNESS))) {
            return;
        }

        int rd = mc.options.getEffectiveRenderDistance();

        // O plano de fundo e dobrado abaixo de 29 chunks para a bruma poder terminar
        // ALEM da ultima chunk desenhada. Sem isso a neblina fecharia antes do fim do
        // mundo carregado e apareceria a borda seca do terreno dentro dela.
        float far = rd * 16.0f * (rd <= 28 ? 2.0f : 1.0f);
        float scale = event.getMode() == FogRenderer.FogMode.FOG_SKY ? horizon(rd) : terrain(rd);
        float density = RECConfig.CLIENT.oldFogDensity.get().floatValue();

        event.setNearPlaneDistance(0.0f);
        event.setFarPlaneDistance(far * scale * density);
        event.setFogShape(FogShape.SPHERE);

        // 🔑 SEM ESTA LINHA NADA ACONTECE. O Forge so le os valores do evento quando o
        // post volta CANCELADO: em ForgeHooksClient.onFogRender, se ninguem cancelou,
        // o metodo simplesmente da return e o que vale e o que o vanilla ja tinha
        // escrito. Cancelar aqui nao desliga a neblina — e o unico jeito de dizer
        // "os numeros que valem sao os meus".
        event.setCanceled(true);
    }

    /**
     * A curva do CHAO, copiada da tabela do alpha r1.6.4.
     *
     * Ela desce junto com a distancia de render por um motivo: com pouca chunk
     * carregada a bruma precisa fechar cedo para esconder a borda do mundo; com muita,
     * fechar cedo do mesmo jeito seria so jogar fora o terreno que o PC ja desenhou.
     */
    private static float terrain(int rd) {
        if (rd <= 3) return 0.48f;
        if (rd <= 5) return 0.55f;
        if (rd <= 8) return 0.65f;
        if (rd <= 11) return 0.70f;
        return 0.80f;
    }

    /**
     * A curva do CEU. Fecha antes da do chao de proposito: se as duas fossem iguais, o
     * ceu limpo apareceria por cima da bruma e entregaria onde ela termina.
     */
    private static float horizon(int rd) {
        if (rd <= 3) return 0.20f;
        if (rd <= 5) return 0.50f;
        if (rd <= 9) return 0.65f;
        return 0.80f;
    }
}
