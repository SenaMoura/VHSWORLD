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
    //
    // ⚠️ UM EVENTO, VARIAS FAIXAS. Cada `music_*` daqui aponta para um evento do
    // sounds.json que tem N arquivos dentro, e o jogo SORTEIA um a cada vez que
    // toca. Foi isto que evitou escrever um tocador de playlist: com o bioma
    // pedindo `music_maze` e delay 0, as tres faixas da MAZE se revezam sozinhas
    // para sempre. Para acrescentar uma faixa nova a uma dimensao NAO se mexe
    // aqui — poe o .ogg em sounds/music/ e adiciona a linha no sounds.json.
    //
    // Os arquivos e a divisao por dimensao sao gerados de
    // "Downloads/SOUNDTRACK vhsworld/dimension" por tools/build_sounds.py.

    /** BIBLIOTECA: o salao de estantes no breu. */
    public static final RegistryObject<SoundEvent> MUSIC_BIBLIOTECA =
            SOUND_EVENTS.register("music_biblioteca", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_biblioteca")));

    /** CHUNKS: os pedacos de mundo e as pontes. */
    public static final RegistryObject<SoundEvent> MUSIC_CHUNKS =
            SOUND_EVENTS.register("music_chunks", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_chunks")));

    /** DATA: os corredores de andesito. */
    public static final RegistryObject<SoundEvent> MUSIC_DATA =
            SOUND_EVENTS.register("music_data", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_data")));

    /** ESCRITORIO: as torres de baias no vazio. */
    public static final RegistryObject<SoundEvent> MUSIC_ESCRITORIO =
            SOUND_EVENTS.register("music_escritorio", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_escritorio")));

    /** FLORESTA: a taiga branca de neblina, e o celeiro. */
    public static final RegistryObject<SoundEvent> MUSIC_FLORESTA =
            SOUND_EVENTS.register("music_floresta", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_floresta")));

    /** GRASSROOMS: o liminal branco com grama crescendo dentro. */
    public static final RegistryObject<SoundEvent> MUSIC_GRASSROOMS =
            SOUND_EVENTS.register("music_grassrooms", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_grassrooms")));

    /** INSIDIOUS: os saloes sem teto sobre o vazio. */
    public static final RegistryObject<SoundEvent> MUSIC_INSIDIOUS =
            SOUND_EVENTS.register("music_insidious", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_insidious")));

    /**
     * MALL: o shopping infinito.
     *
     * A unica cujo pool nao e dela: o Pedro so mandou um stinger de 23s para a
     * mall, e stinger nao e trilha. Enquanto nao vier faixa propria, o evento
     * aponta para o pool de "Sons Gerais" — e a troca depois e uma linha no
     * sounds.json, sem tocar em Java.
     */
    public static final RegistryObject<SoundEvent> MUSIC_MALL =
            SOUND_EVENTS.register("music_mall", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_mall")));

    /** MAZE: o labirinto de parede de 163 blocos. */
    public static final RegistryObject<SoundEvent> MUSIC_MAZE =
            SOUND_EVENTS.register("music_maze", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_maze")));

    /** PARKOURLAND: a unica finita, e a unica de que se cai para fora. */
    public static final RegistryObject<SoundEvent> MUSIC_PARKOURLAND =
            SOUND_EVENTS.register("music_parkourland", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_parkourland")));

    /** PIPE TUNELS: os tuneis de concreto com os canos na parede. */
    public static final RegistryObject<SoundEvent> MUSIC_PIPE_TUNELS =
            SOUND_EVENTS.register("music_pipe_tunels", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_pipe_tunels")));

    /** STONELAND: a silhueta do overworld feita so de pedregulho. */
    public static final RegistryObject<SoundEvent> MUSIC_STONELAND =
            SOUND_EVENTS.register("music_stoneland", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_stoneland")));

    /** TRAIN: a linha reta sobre o vazio. */
    public static final RegistryObject<SoundEvent> MUSIC_TRAIN =
            SOUND_EVENTS.register("music_train", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_train")));

    /** UNDER PRESSURE: os 92 blocos de agua. */
    public static final RegistryObject<SoundEvent> MUSIC_UNDER_PRESSURE =
            SOUND_EVENTS.register("music_under_pressure", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_under_pressure")));

    /** VILLAGE: a mesma casa, para sempre. */
    public static final RegistryObject<SoundEvent> MUSIC_VILLAGE =
            SOUND_EVENTS.register("music_village", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_village")));

    /**
     * OVERWORLD: o unico que nao pode ser pendurado num bioma nosso.
     *
     * O overworld do mod roda em biomas VANILLA (ver AlphaBiomeSource), e datapack
     * de mod nao reescreve bioma de outro namespace. Quem pendura este evento la e
     * o OverworldMusicModifier — ver a classe.
     */
    public static final RegistryObject<SoundEvent> MUSIC_OVERWORLD =
            SOUND_EVENTS.register("music_overworld", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "music_overworld")));

    // ------------------------------------------------------------------ sustos
    //
    // Faixa curta nao e trilha. Se o chute de bumbo de 1,75s da PARKOURLAND caisse
    // no sorteio da musica do bioma, o jogador ouviria 1,75s de bumbo e o jogo
    // sortearia de novo — viraria uma bateria, nao um susto. Entao tudo abaixo de
    // 40 segundos foi separado para ca, e quem toca e o DimensionAmbience, em
    // intervalos longos e aleatorios.

    /** BIBLIOTECA. */
    public static final RegistryObject<SoundEvent> STING_BIBLIOTECA =
            SOUND_EVENTS.register("sting_biblioteca", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "sting_biblioteca")));

    /** CHUNKS. */
    public static final RegistryObject<SoundEvent> STING_CHUNKS =
            SOUND_EVENTS.register("sting_chunks", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "sting_chunks")));

    /** MALL. */
    public static final RegistryObject<SoundEvent> STING_MALL =
            SOUND_EVENTS.register("sting_mall", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "sting_mall")));

    /** PARKOURLAND. */
    public static final RegistryObject<SoundEvent> STING_PARKOURLAND =
            SOUND_EVENTS.register("sting_parkourland", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "sting_parkourland")));

    /** PIPE TUNELS. */
    public static final RegistryObject<SoundEvent> STING_PIPE_TUNELS =
            SOUND_EVENTS.register("sting_pipe_tunels", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "sting_pipe_tunels")));

    /** UNDER PRESSURE. */
    public static final RegistryObject<SoundEvent> STING_UNDER_PRESSURE =
            SOUND_EVENTS.register("sting_under_pressure", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "sting_under_pressure")));

    /**
     * O pool que toca em TODA dimensao, inclusive no overworld: os cinco arquivos
     * de "Sons Gerais/random sounds effect" (passos distantes, sussurro, grito).
     */
    public static final RegistryObject<SoundEvent> STING_GERAL =
            SOUND_EVENTS.register("sting_geral", () -> SoundEvent.createVariableRangeEvent(
                    ResourceLocation.fromNamespaceAndPath(RECMod.MOD_ID, "sting_geral")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}