package net.vhsworld.rec.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.entity.OphanimGazeFx;
import net.vhsworld.rec.client.entity.StonemanSurgeFx;
import net.vhsworld.rec.client.sanity.SanityState;
import net.vhsworld.rec.config.RECConfig;

@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CameraDistortionEvent {

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!RECConfig.CLIENT.fisheye.get() || !CameraState.isActive()) return;

        // Efeito Fisheye no FOV
        event.setNewFovModifier(event.getFovModifier() * RECConfig.CLIENT.fisheyeStrength.get().floatValue());
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double time = mc.player.level().getGameTime() + event.getPartialTick();

        // 1. Balanço suave de mão (a câmera respira)
        if (RECConfig.CLIENT.handShake.get() && CameraState.isActive()) {
            float amplitude = RECConfig.CLIENT.handShakeStrength.get().floatValue();
            if (amplitude > 0.0f) {
                event.setPitch(event.getPitch() + (float) Math.sin(time * 0.15) * amplitude);
                event.setYaw(event.getYaw() + (float) Math.cos(time * 0.2) * amplitude);
            }
        }

        // 2. Tremor do susto — independe da câmera estar ligada. Quem viu, viu.
        float panic = SanityState.get().shakeAmount();
        if (panic > 0.0f) {
            float fast = (float) Math.sin(time * 1.7) + (float) Math.sin(time * 2.9) * 0.6f;
            float other = (float) Math.cos(time * 2.3) + (float) Math.cos(time * 3.7) * 0.5f;

            event.setPitch(event.getPitch() + fast * panic * 1.6f);
            event.setYaw(event.getYaw() + other * panic * 1.6f);
            // O roll é o que dá o enjoo: o horizonte deixa de ser confiável.
            event.setRoll(event.getRoll() + fast * panic * 0.9f);
        }

        // 3. O chão treme: alguma coisa de pedra se soltou perto de você.
        // Tremor mais grosso e mais lento que o do susto — não é o teu pulso, é peso
        // caminhando. E entra sem pedir sanidade nem câmera ligada: é o mundo.
        float quake = StonemanSurgeFx.shakeAmount();
        if (quake > 0.0f) {
            float heavy = (float) Math.sin(time * 0.9) + (float) Math.sin(time * 1.4) * 0.5f;
            float side = (float) Math.cos(time * 1.1);

            event.setPitch(event.getPitch() + heavy * quake * 1.1f);
            event.setYaw(event.getYaw() + side * quake * 0.8f);
            event.setRoll(event.getRoll() + side * quake * 0.6f);
        }

        // 4. O horizonte ENTORTA: o Ofanim está com os olhos em você.
        //
        // Este é o único dos quatro que mora quase todo no ROLL, e de propósito. Os
        // outros tremem — pulso, susto, peso batendo no chão — e tremor o jogador lê
        // como "algo aconteceu". Entortar devagar não é um acontecimento: é o mundo
        // deixando de estar de pé, e é exatamente isso que se sente antes de cair.
        // Numa dimensão que é só abismo e ponte de oito blocos, o horizonte torto vale
        // mais que qualquer dano.
        float tilt = OphanimGazeFx.tiltAmount();
        if (tilt > 0.0f) {
            float slow = (float) Math.sin(time * 0.11) + (float) Math.sin(time * 0.19) * 0.5f;
            float drift = (float) Math.cos(time * 0.07);

            event.setRoll(event.getRoll() + slow * tilt * 9.0f);
            event.setPitch(event.getPitch() + drift * tilt * 1.2f);
        }
    }
}