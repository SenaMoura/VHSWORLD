package net.vhsworld.rec.client.codex;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.client.ClientWorldData;
import net.vhsworld.rec.init.ModItems;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * O registro dos itens do mod.
 *
 * Nada aqui nasce sabido: cada ficha comeca trancada e so abre quando o jogador
 * fotografa o item com o flash. O item conta como fotografado se estava na mao ou
 * jogado no chao dentro do enquadramento, no instante do disparo.
 *
 * O motivo de existir: a camera deixa de ser so um brinquedo de terror e vira a
 * ferramenta de descoberta do jogo. Quem quer saber o que uma coisa faz, aponta e
 * dispara — que e a mesma coisa que o jogo pede para fazer com as criaturas.
 */
public final class Codex {

    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Type SET_TYPE = new TypeToken<LinkedHashSet<String>>() {}.getType();

    /** Alcance e cone para reconhecer um item jogado no chao. */
    private static final double RANGE = 12.0;
    private static final double CONE = 0.55;

    private static final Map<Item, CodexEntry> ENTRIES = new LinkedHashMap<>();

    private static Codex instance;

    private final Path file;
    private final Set<String> unlocked = new LinkedHashSet<>();

    static {
        // --- CAMERA: a camera e o que a alimenta ---
        register(new CodexEntry(ModItems.BATTERY.get(), "battery",
                new ResourceLocation(RECMod.MOD_ID, "battery"), CodexCategory.CAMERA));
        register(new CodexEntry(ModItems.ALUMINUM_INGOT.get(), "aluminum", null,
                CodexCategory.CAMERA));

        // As engenhocas de camera.
        register(new CodexEntry(ModItems.INFRARED_LENS.get(), "infrared_lens",
                new ResourceLocation(RECMod.MOD_ID, "infrared_lens"), CodexCategory.CAMERA));
        register(new CodexEntry(ModItems.CORRUPTED_BATTERY.get(), "corrupted_battery",
                new ResourceLocation(RECMod.MOD_ID, "corrupted_battery"), CodexCategory.CAMERA));
        register(new CodexEntry(ModItems.BLANK_TAPE.get(), "blank_tape",
                new ResourceLocation(RECMod.MOD_ID, "blank_tape"), CodexCategory.CAMERA));
        register(new CodexEntry(ModItems.VIDEOCASSETTE.get(), "videocassette",
                new ResourceLocation(RECMod.MOD_ID, "videocassette"), CodexCategory.CAMERA));
        register(new CodexEntry(ModItems.TRIPOD_ITEM.get(), "tripod",
                new ResourceLocation(RECMod.MOD_ID, "tripod"), CodexCategory.CAMERA));

        // --- SURVIVE: a cadeia de materiais e os dispositivos de fuga/orientacao ---
        // O ferro prensado nao tem receita para animar porque nao sai de bancada
        // nenhuma — a ficha dele explica a bigorna por texto.
        register(new CodexEntry(ModItems.CORRUPTED_STONE_ITEM.get(), "corrupted_stone", null,
                CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.IRON_STICK.get(), "iron_stick",
                new ResourceLocation(RECMod.MOD_ID, "iron_stick"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.PRESSED_IRON.get(), "pressed_iron", null,
                CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.BLACK_GOO.get(), "black_goo", null,
                CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.CORRUPTED_COMPASS.get(), "corrupted_compass",
                new ResourceLocation(RECMod.MOD_ID, "corrupted_compass"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.ANCHOR.get(), "anchor",
                new ResourceLocation(RECMod.MOD_ID, "anchor"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.LURE_CLOCK.get(), "lure_clock",
                new ResourceLocation(RECMod.MOD_ID, "lure_clock"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.ORE_TRACKER.get(), "ore_tracker",
                new ResourceLocation(RECMod.MOD_ID, "ore_tracker"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.STRUCTURE_LOCATOR.get(), "structure_locator",
                new ResourceLocation(RECMod.MOD_ID, "structure_locator"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.REALITY_TEAR.get(), "reality_tear", null,
                CodexCategory.SURVIVE));

        // --- SURVIVE / REBOOT: a cadeia de sucata eletronica e a bancada do mod ---
        // Tudo passa por aqui para nenhum item nascer com o tooltip preso em "use o flash"
        // sem nunca destravar. A bancada e os Tier 1/2/3 do documento de itens finais.
        register(new CodexEntry(ModItems.RF_RECEIVER_ITEM.get(), "rf_receiver",
                new ResourceLocation(RECMod.MOD_ID, "rf_receiver"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.CIRCUIT_SCRAP.get(), "circuit_scrap",
                new ResourceLocation(RECMod.MOD_ID, "circuit_scrap"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.MAGNETIZED_COPPER_TAPE.get(), "magnetized_copper_tape",
                new ResourceLocation(RECMod.MOD_ID, "magnetized_copper_tape"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.BLANK_MAGNETIC_TAPE.get(), "blank_magnetic_tape",
                new ResourceLocation(RECMod.MOD_ID, "blank_magnetic_tape"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.STATIC_RESIDUE.get(), "static_residue",
                new ResourceLocation(RECMod.MOD_ID, "static_residue"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.STATIC_CONDENSER.get(), "static_condenser",
                new ResourceLocation(RECMod.MOD_ID, "static_condenser"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.BUSTED_TUNER.get(), "busted_tuner",
                new ResourceLocation(RECMod.MOD_ID, "busted_tuner"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.CRT_TUBE.get(), "crt_tube",
                new ResourceLocation(RECMod.MOD_ID, "crt_tube"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.REINFORCED_SYRINGE.get(), "reinforced_syringe",
                new ResourceLocation(RECMod.MOD_ID, "reinforced_syringe"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.CONTAMINATED_BLOOD.get(), "contaminated_blood",
                new ResourceLocation(RECMod.MOD_ID, "contaminated_blood"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.CORRUPTED_CRT_TUBE.get(), "corrupted_crt_tube",
                new ResourceLocation(RECMod.MOD_ID, "corrupted_crt_tube"), CodexCategory.SURVIVE));

        // --- KILL: o que se empunha para ferir ---
        register(new CodexEntry(ModItems.HAMMER.get(), "hammer",
                new ResourceLocation(RECMod.MOD_ID, "hammer"), CodexCategory.KILL));
        register(new CodexEntry(ModItems.SHARP_SCISSORS.get(), "sharp_scissors",
                new ResourceLocation(RECMod.MOD_ID, "sharp_scissors"), CodexCategory.KILL));

        // O kit corrompido divide UMA ficha entre as cinco pecas: a historia e do
        // material, nao de cada ferramenta. Cinco fichas repetindo o mesmo texto
        // so fariam o registro parecer maior do que e.
        register(new CodexEntry(ModItems.CORRUPTED_SWORD.get(), "corrupted_kit",
                new ResourceLocation(RECMod.MOD_ID, "corrupted_sword"), CodexCategory.KILL));
        register(new CodexEntry(ModItems.CORRUPTED_PICKAXE.get(), "corrupted_kit",
                new ResourceLocation(RECMod.MOD_ID, "corrupted_pickaxe"), CodexCategory.KILL));
        register(new CodexEntry(ModItems.CORRUPTED_AXE.get(), "corrupted_kit",
                new ResourceLocation(RECMod.MOD_ID, "corrupted_axe"), CodexCategory.KILL));
        register(new CodexEntry(ModItems.CORRUPTED_SHOVEL.get(), "corrupted_kit",
                new ResourceLocation(RECMod.MOD_ID, "corrupted_shovel"), CodexCategory.KILL));
        register(new CodexEntry(ModItems.CORRUPTED_HOE.get(), "corrupted_kit",
                new ResourceLocation(RECMod.MOD_ID, "corrupted_hoe"), CodexCategory.KILL));
        register(new CodexEntry(ModItems.CORRUPTED_DIAMOND_PICKAXE.get(), "corrupted_diamond_pickaxe",
                new ResourceLocation(RECMod.MOD_ID, "corrupted_diamond_pickaxe"), CodexCategory.KILL));
        register(new CodexEntry(ModItems.FRACTURE.get(), "fracture",
                new ResourceLocation(RECMod.MOD_ID, "fracture"), CodexCategory.KILL));
        register(new CodexEntry(ModItems.FRACTURE_PICKAXE.get(), "fracture_pickaxe",
                new ResourceLocation(RECMod.MOD_ID, "fracture_pickaxe"), CodexCategory.KILL));
        register(new CodexEntry(ModItems.FRACTURE_AXE.get(), "fracture_axe",
                new ResourceLocation(RECMod.MOD_ID, "fracture_axe"), CodexCategory.KILL));
        register(new CodexEntry(ModItems.FRACTURE_SHOVEL.get(), "fracture_shovel",
                new ResourceLocation(RECMod.MOD_ID, "fracture_shovel"), CodexCategory.KILL));
        register(new CodexEntry(ModItems.FRACTURE_HELMET.get(), "fracture_suit",
                new ResourceLocation(RECMod.MOD_ID, "fracture_helmet"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.FRACTURE_CHESTPLATE.get(), "fracture_suit",
                new ResourceLocation(RECMod.MOD_ID, "fracture_chestplate"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.FRACTURE_LEGGINGS.get(), "fracture_suit",
                new ResourceLocation(RECMod.MOD_ID, "fracture_leggings"), CodexCategory.SURVIVE));
        register(new CodexEntry(ModItems.FRACTURE_BOOTS.get(), "fracture_suit",
                new ResourceLocation(RECMod.MOD_ID, "fracture_boots"), CodexCategory.SURVIVE));
    }

    private Codex() {
        this.file = ClientWorldData.worldDir().resolve("codex.json");
        load();
    }

    public static Codex get() {
        if (instance == null) instance = new Codex();
        return instance;
    }

    /** Trocou de mundo: esquece o registro carregado; o proximo get() le a pasta certa. */
    public static void reset() {
        instance = null;
    }

    public static void register(CodexEntry entry) {
        ENTRIES.put(entry.item, entry);
    }

    /** Todas as fichas, na ordem em que foram registradas. */
    public static List<CodexEntry> entries() {
        return new ArrayList<>(ENTRIES.values());
    }

    /** As fichas de uma categoria, na ordem de registro. */
    public static List<CodexEntry> entries(CodexCategory category) {
        List<CodexEntry> list = new ArrayList<>();
        for (CodexEntry e : ENTRIES.values()) {
            if (e.category == category) list.add(e);
        }
        return list;
    }

    public static CodexEntry entryFor(Item item) {
        return ENTRIES.get(item);
    }

    public boolean isUnlocked(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id != null && unlocked.contains(id.toString());
    }

    public boolean unlock(Item item) {
        if (!ENTRIES.containsKey(item)) return false;

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null || !unlocked.add(id.toString())) return false;

        save();
        return true;
    }

    /**
     * Chamado no disparo do flash: destranca o que estava enquadrado.
     *
     * @return quantas fichas novas abriram
     */
    public static int unlockFromFlash(Minecraft mc) {
        if (mc.player == null || mc.level == null) return 0;

        Codex codex = get();
        int opened = 0;

        // Na mao: o jeito obvio de "olhar melhor" uma coisa é segurá-la e disparar.
        if (codex.unlock(mc.player.getMainHandItem().getItem())) opened++;
        if (codex.unlock(mc.player.getOffhandItem().getItem())) opened++;

        // No chao, dentro do enquadramento.
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof ItemEntity drop)) continue;

            Vec3 delta = entity.position().add(0.0, 0.25, 0.0).subtract(eye);
            double distance = delta.length();
            if (distance < 0.01 || distance > RANGE) continue;
            if (delta.normalize().dot(look) < CONE) continue;

            ItemStack stack = drop.getItem();
            if (!stack.isEmpty() && codex.unlock(stack.getItem())) opened++;
        }

        return opened;
    }

    // ------------------------------------------------------------------ disco

    private void load() {
        if (!Files.isRegularFile(file)) return;
        try {
            Set<String> read = GSON.fromJson(Files.readString(file), SET_TYPE);
            if (read != null) unlocked.addAll(read);
        } catch (Exception e) {
            LOG.error("vhsworld_codex.json ilegivel; registro comeca vazio", e);
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(unlocked));
        } catch (Exception e) {
            LOG.error("Nao consegui salvar o registro", e);
        }
    }
}
