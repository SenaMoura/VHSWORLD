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

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}