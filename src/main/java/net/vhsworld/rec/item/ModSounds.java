package net.vhsworld.rec.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.vhsworld.rec.RECMod;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RECMod.MOD_ID);

    public static final RegistryObject<SoundEvent> ENTITY_APPROACHING =
            SOUND_EVENTS.register("entity_approaching",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "entity_approaching")));

    public static final RegistryObject<SoundEvent> ENTITY_SCREAM =
            SOUND_EVENTS.register("entity_scream",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "entity_scream")));

    public static final RegistryObject<SoundEvent> FLASH =
            SOUND_EVENTS.register("flash",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "flash")));

    public static final RegistryObject<SoundEvent> FLASHLIGHT_CLICK =
            SOUND_EVENTS.register("flashlight_click",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "flashlight_click")));

    public static final RegistryObject<SoundEvent> CAMERA_OFF =
            SOUND_EVENTS.register("camera_off",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "camera_off")));

    public static final RegistryObject<SoundEvent> CAMERA_ON =
            SOUND_EVENTS.register("camera_on",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "camera_on")));

    public static final RegistryObject<SoundEvent> HORROR_AMBIENCE =
            SOUND_EVENTS.register("horror_ambience",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "horror_ambience")));

    /** O estalo que acompanha o susto da revelacao. */
    public static final RegistryObject<SoundEvent> BONE_BREAKING =
            SOUND_EVENTS.register("bone_breaking",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "bone_breaking")));

    /** Clique seco ao abrir album e registro. */
    public static final RegistryObject<SoundEvent> MENU_BUTTON =
            SOUND_EVENTS.register("menu_button",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "menu_button")));

    /** O momento em que a sanidade acaba. Toca uma vez, na virada. */
    public static final RegistryObject<SoundEvent> HORROR_SANITY =
            SOUND_EVENTS.register("horror_sanity",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "horror_sanity")));

    /** O aparelho engolindo a fita: abertura do jogo. */
    public static final RegistryObject<SoundEvent> TAPE_PLAYER =
            SOUND_EVENTS.register("tape_player",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "tape_player")));

    /** O baque do coracao dos localizadores. Acelera perto do alvo. */
    public static final RegistryObject<SoundEvent> HEARTBEAT =
            SOUND_EVENTS.register("heartbeat",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "heartbeat")));

    /** Chiado de fita em loop. Hoje: o fundo da tela de dificuldade. */
    public static final RegistryObject<SoundEvent> TAPE_STATIC =
            SOUND_EVENTS.register("tape_static",
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "tape_static")));


    // ---------------------------------------------------------------- criaturas
    //
    // Sons que o Pedro separou. ⚠️ Eles chegaram como .mp3 e foram convertidos para
    // .ogg pelo tools/import_sounds.py — o Minecraft nao toca mp3, e o sintoma disso
    // e um som que simplesmente nunca sai, sem erro nenhum no log.

    /** Um por criatura: o jogador precisa aprender a associar o som AQUELA coisa. */
    public static final RegistryObject<SoundEvent> SIGHT_TALL =
            SOUND_EVENTS.register("sight_tall", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "sight_tall")));

    public static final RegistryObject<SoundEvent> SIGHT_SPIDER =
            SOUND_EVENTS.register("sight_spider", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "sight_spider")));

    public static final RegistryObject<SoundEvent> SIGHT_CLAWS =
            SOUND_EVENTS.register("sight_claws", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "sight_claws")));

    public static final RegistryObject<SoundEvent> SIGHT_STONEMAN =
            SOUND_EVENTS.register("sight_stoneman", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "sight_stoneman")));

    /** Fundo: tocam do nada, sem criatura nenhuma por perto. */
    public static final RegistryObject<SoundEvent> DREAD_SPEECH =
            SOUND_EVENTS.register("dread_speech", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "dread_speech")));

    public static final RegistryObject<SoundEvent> DREAD_BROKEN =
            SOUND_EVENTS.register("dread_broken", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "dread_broken")));

    public static final RegistryObject<SoundEvent> DREAD_GLITCH =
            SOUND_EVENTS.register("dread_glitch", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "dread_glitch")));

    public static final RegistryObject<SoundEvent> DREAD_STEPS =
            SOUND_EVENTS.register("dread_steps", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "dread_steps")));

    public static final RegistryObject<SoundEvent> DREAD_BURNT =
            SOUND_EVENTS.register("dread_burnt", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "dread_burnt")));

    public static final RegistryObject<SoundEvent> DREAD_FLESH =
            SOUND_EVENTS.register("dread_flesh", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "dread_flesh")));

    /** As duas esculturas nao tinham som de avistamento nenhum. Agora tem. */
    public static final RegistryObject<SoundEvent> SIGHT_GREYFACE =
            SOUND_EVENTS.register("sight_greyface", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "sight_greyface")));

    public static final RegistryObject<SoundEvent> SIGHT_OPHANIM =
            SOUND_EVENTS.register("sight_ophanim", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "sight_ophanim")));

    /**
     * A TRILHA DA CACADA. Toca em loop enquanto o Cara Cinza esta atras de voce.
     *
     * Ela nao e enfeite: e o unico jeito de o jogador saber que foi VISTO. A criatura
     * nao muda de pose nem grita para avisar — quem avisa e a musica, e quando ela
     * para, a perseguicao acabou.
     */
    public static final RegistryObject<SoundEvent> GREYFACE_CHASE =
            SOUND_EVENTS.register("greyface_chase", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "greyface_chase")));

    /** O passo dela. Alto de proposito: da para ouvir de onde ela vem sem ver. */
    public static final RegistryObject<SoundEvent> GREYFACE_STEP =
            SOUND_EVENTS.register("greyface_step", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "greyface_step")));

    /** O zumbido do Ofanim: presenca gigante nao precisa se mover para pesar. */
    public static final RegistryObject<SoundEvent> OPHANIM_DRONE =
            SOUND_EVENTS.register("ophanim_drone", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "ophanim_drone")));

    // ---------------------------------------------------------------- dimensoes
    //
    // Uma trilha por dimensao, pendurada no campo `music` do bioma: o jogo a toca
    // sozinho, com pausa entre as voltas, e ela some quando o jogador sai. Nao
    // precisa de codigo nenhum do nosso lado — so o evento existir.

    /** DATA: os corredores de andesito. */
    public static final RegistryObject<SoundEvent> MUSIC_DATA =
            SOUND_EVENTS.register("music_data", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_data")));

    /** CHUNKS: os pedacos de mundo e as pontes. */
    public static final RegistryObject<SoundEvent> MUSIC_CHUNKS =
            SOUND_EVENTS.register("music_chunks", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_chunks")));

    /** INSIDIOUS: os saloes sem teto sobre o vazio. */
    public static final RegistryObject<SoundEvent> MUSIC_INSIDIOUS =
            SOUND_EVENTS.register("music_insidious", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_insidious")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}