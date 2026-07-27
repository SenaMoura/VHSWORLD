package net.vhsworld.rec.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.CameraState;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.entity.AnomalyEntity;
import net.vhsworld.rec.entity.AnomalyType;
import net.vhsworld.rec.item.ModSounds;
import org.jetbrains.annotations.Nullable;

/**
 * O som CONTINUO das duas esculturas: a trilha de quem te caca e o zumbido de quem
 * so esta ali sendo enorme.
 *
 * Por que som em loop e um arquivo separado do EntitySightSounds: aquele toca um
 * susto e acaba. Estes dois duram enquanto uma condicao for verdade, e por isso cada
 * um e uma INSTANCIA viva que se pergunta a cada tick se ainda faz sentido existir —
 * e se para sozinha quando nao faz. Isso e importante: som em loop que perde o dono
 * (criatura morreu, jogador trocou de mundo, config desligou) fica tocando para
 * sempre, e e o tipo de bug que so aparece depois de meia hora de jogo.
 *
 * A CACADA (Cara Cinza): a trilha nao vem da criatura, vem do ouvido — e trilha de
 * filme, nao som do mundo. Ela e a unica coisa que informa ao jogador que ele foi
 * VISTO: a criatura nao grita, nao muda de pose e nao acende nada. Quando a musica
 * para, a perseguicao acabou. Ela sobe RAPIDO e desce DEVAGAR de proposito: o susto
 * chega inteiro, e o alivio custa alguns segundos de duvida.
 *
 * O OFANIM: o zumbido cresce sozinho conforme voce chega perto, porque a atenuacao
 * do Minecraft ja faz essa conta — o volume acima de 1.0 serve para ESTICAR o
 * alcance, e uma criatura de treze blocos tem que ser ouvida de longe. Ele existe
 * porque uma presenca que nunca se mexe nao tem outro jeito de pesar.
 *
 * ⚠️ Em multiplayer, a trilha toca para quem esta PERTO de uma cacada, nao
 * necessariamente para a presa (o cliente nao sabe quem e o alvo). Preferi isso a
 * inventar pacote so para isto — e, num jogo de terror, ouvir a trilha da caca ao
 * seu amigo tambem serve.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CreatureAudio {

    private CreatureAudio() {}

    /** Distancia em que o cliente ainda liga a trilha. O servidor tem o knob dele. */
    private static final double CHASE_EARSHOT = 48.0D;

    /** Alem disto o zumbido nem comeca (a atenuacao ja o teria matado). */
    private static final double DRONE_EARSHOT = 48.0D;

    @Nullable
    private static ChaseTrack chase;

    @Nullable
    private static PresenceDrone drone;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            // Trocou de mundo: as instancias morrem com ele. Nao dou stop() aqui
            // porque o proprio tick delas ja se encerra sem nivel; so solto a
            // referencia para o proximo mundo poder comecar do zero.
            chase = null;
            drone = null;
            return;
        }
        if (mc.isPaused()) return;
        if (!CameraState.audible()) return;

        if (RECConfig.CLIENT.chaseMusic.get() && (chase == null || chase.isStopped())
                && hunter(mc) != null) {
            chase = new ChaseTrack();
            mc.getSoundManager().play(chase);
        }

        if (RECConfig.CLIENT.ophanimDrone.get() && (drone == null || drone.isStopped())) {
            AnomalyEntity ophanim = ophanim(mc);
            if (ophanim != null) {
                drone = new PresenceDrone(ophanim);
                mc.getSoundManager().play(drone);
            }
        }
    }

    /** Existe um Cara Cinza cacando por aqui? */
    @Nullable
    private static AnomalyEntity hunter(Minecraft mc) {
        if (mc.level == null || mc.player == null) return null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            // ⚠️ O tipo entra no teste de proposito. O Vazio tambem e cacador, mas esta
            // trilha e do Cara Cinza; solta para "qualquer um que cace", os dois
            // chegariam com a mesma musica e o pack perderia a diferenca entre eles.
            if (entity instanceof AnomalyEntity anomaly
                    && anomaly.type() == AnomalyType.GREYFACE
                    && anomaly.isHunting()
                    && anomaly.isAlive()
                    && anomaly.distanceTo(mc.player) < CHASE_EARSHOT) {
                return anomaly;
            }
        }
        return null;
    }

    /** O Ofanim mais perto que os olhos alcancam. */
    @Nullable
    private static AnomalyEntity ophanim(Minecraft mc) {
        if (mc.level == null || mc.player == null) return null;

        AnomalyEntity best = null;
        double bestDistance = DRONE_EARSHOT;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof AnomalyEntity anomaly)) continue;
            if (anomaly.type() != AnomalyType.OPHANIM || !anomaly.isAlive()) continue;
            // Se a fita nao a revelou, ela nao zumbe: som saindo de um lugar vazio
            // entregaria a posicao de uma coisa que o jogo jura nao estar ali.
            if (!AnomalyVision.canSee(anomaly.type())) continue;

            double distance = anomaly.distanceTo(mc.player);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = anomaly;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ a trilha

    private static final class ChaseTrack extends AbstractTickableSoundInstance {

        /** 0 = calada, 1 = na cara. Existe para a trilha entrar e sair, nao ligar. */
        private float fade = 0.05F;

        private ChaseTrack() {
            super(ModSounds.GREYFACE_CHASE.get(), SoundSource.HOSTILE,
                    SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            // Colada no ouvido: e trilha, nao som que sai da criatura. O jogador nao
            // deve poder virar a cabeca para "localizar" a musica.
            this.attenuation = Attenuation.NONE;
            this.relative = true;
            this.x = 0.0D;
            this.y = 0.0D;
            this.z = 0.0D;
            this.volume = 0.05F;
        }

        /**
         * ⚠️ SEM ISTO A TRILHA NUNCA COMECA. O motor descarta na largada qualquer som
         * com volume zerado, e esta nasce quase muda de proposito (ela entra subindo).
         */
        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) {
                stop();
                return;
            }

            boolean chasing = RECConfig.CLIENT.chaseMusic.get()
                    && CameraState.audible()
                    && hunter(mc) != null;

            // Sobe rapido (0.20 por tick, ~1s para o topo), desce devagar (0.015,
            // ~3s). Cortar a musica no tick em que ela desiste soaria como bug de
            // audio; sumindo devagar, o alivio tem duracao — e o jogador continua
            // andando de costas por um tempo depois de estar a salvo.
            fade += ((chasing ? 1.0F : 0.0F) - fade) * (chasing ? 0.20F : 0.015F);

            if (!chasing && fade < 0.02F) {
                stop();
                return;
            }

            float base = RECConfig.CLIENT.chaseMusicVolume.get().floatValue();
            this.volume = Math.max(0.005F, CameraState.volume(base) * fade);
        }
    }

    // ------------------------------------------------------------------ o zumbido

    private static final class PresenceDrone extends AbstractTickableSoundInstance {

        private final AnomalyEntity source;

        private PresenceDrone(AnomalyEntity source) {
            super(ModSounds.OPHANIM_DRONE.get(), SoundSource.HOSTILE,
                    SoundInstance.createUnseededRandom());
            this.source = source;
            this.looping = true;
            this.delay = 0;
            this.attenuation = Attenuation.LINEAR;
            // Na metade da altura DO DESENHO, nao da caixa de colisao: numa coisa de
            // treze blocos, o som sairia dos pes dela.
            place();
            this.volume = volumeNow();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        private void place() {
            this.x = source.getX();
            this.y = source.getY() + source.type().height() * 0.5D;
            this.z = source.getZ();
        }

        /**
         * Volume acima de 1.0 nao "estoura": no Minecraft ele MULTIPLICA o alcance
         * (rolloff = volume x 16 blocos). E o unico jeito honesto de uma presenca
         * gigante ser ouvida de longe sem que a atenuacao normal desapareca — quem
         * faz o som crescer conforme voce chega perto continua sendo o motor.
         */
        private float volumeNow() {
            float base = RECConfig.CLIENT.ophanimDroneVolume.get().floatValue();
            return CameraState.volume(base * 2.0F);
        }

        @Override
        public void tick() {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null
                    || !source.isAlive() || source.isRemoved()
                    || !RECConfig.CLIENT.ophanimDrone.get()
                    || !CameraState.audible()
                    || !AnomalyVision.canSee(source.type())
                    || source.distanceTo(mc.player) > DRONE_EARSHOT + 16.0D) {
                stop();
                return;
            }
            place();
            this.volume = volumeNow();
        }
    }
}
