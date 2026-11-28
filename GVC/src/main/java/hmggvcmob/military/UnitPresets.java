
package hmggvcmob.military;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cpw.mods.fml.common.FMLLog;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class UnitPresets {
    private static final Gson JSON = new GsonBuilder().create();
    private static final Map<String, Preset> PRESETS = new LinkedHashMap<>();

    private UnitPresets() {}

    public static void load(Path directory) {
        PRESETS.clear();
        load(directory, "unit-presets.json");
        load(directory, "regional-unit-presets.json");
        load(directory, "field-unit-presets.json");
        FMLLog.info("[GVC military] Loaded %d military presets", PRESETS.size());
    }

    private static void load(Path directory, String filename) {
        Path presets = directory.resolve(filename);
        try {
            if (!Files.exists(presets)) {
                try (InputStream defaults = defaults(filename)) {
                    Files.copy(defaults, presets);
                }
            }
            try (Reader reader = Files.newBufferedReader(presets, StandardCharsets.UTF_8)) {
                read(reader);
            }
        } catch (IOException | RuntimeException failure) {
            FMLLog.warning("[GVC military] Cannot load %s: %s. Using bundled presets.", presets, failure.getMessage());
            try (Reader reader = new InputStreamReader(defaults(filename), StandardCharsets.UTF_8)) {
                read(reader);
            } catch (IOException fallbackFailure) {
                throw new IllegalStateException("Bundled military presets unavailable", fallbackFailure);
            }
        }
    }

    private static InputStream defaults(String filename) {
        InputStream stream = UnitPresets.class.getResourceAsStream("/assets/gvcmob/military/" + filename);
        if (stream == null) throw new IllegalStateException("Missing bundled unit presets");
        return stream;
    }

    private static void read(Reader reader) {
        Preset[] entries = JSON.fromJson(reader, Preset[].class);
        if (entries == null) throw new IllegalArgumentException("Expected an array of presets");
        for (Preset preset : entries) {
            if (preset == null || !preset.valid() || PRESETS.containsKey(preset.id)) {
                FMLLog.warning("[GVC military] Invalid or duplicate preset: %s", preset == null ? "null" : preset.id);
                continue;
            }
            if (preset.requiresPrimary() && preset.primary(new Random(0)) == null) {
                FMLLog.warning("[GVC military] %s has no compatible primary weapon; unavailable for recruitment", preset.id);
                continue;
            }
            PRESETS.put(preset.id, preset);
        }
    }

    public static Preset get(String id) {
        return PRESETS.get(id);
    }

    public static Collection<Preset> all() {
        return Collections.unmodifiableCollection(PRESETS.values());
    }

    static List<Preset> pool(String faction, boolean reinforcement) {
        List<Preset> available = new ArrayList<>();
        if (!MilitaryRelations.available(faction)) return available;
        String[] recruitment = MilitaryRelations.definition(faction).recruitment;
        for (Preset preset : PRESETS.values()) {
            if (!faction.equals(preset.faction)) continue;
            if (!reinforcement && recruitment.length > 0 && !java.util.Arrays.asList(recruitment).contains(preset.id)) continue;
            if (reinforcement ? preset.reinforcementWeight > 0 : preset.recruitable) available.add(preset);
        }
        return available;
    }

    public static final class Preset {
        public String id, name, description = "", faction = "pmc", classification = "rifleman";
        public String primary = "", secondary = "", behavior = "rifle";
        public String[] primaryPools = new String[0], secondaryPools = new String[0], equipment = new String[0];
        public String ammunition = "";
        public int tier = 1, armor = 0, reactionTicks = 14, range = 34, rank = 0, capacity = 0;
        public int recruitmentCost = 20, currencyUpkeep = 1, foodConsumption = 3, requiredRelation = 0;
        public int reinforcementWeight = 0;
        public Integer minimumReputation;
        public float health = 20, resistance = 0, speed = .31F, accuracy = 1, dispersion = 1, recoil = 1, reload = 1;
        public boolean recruitable = true;
        public boolean allegianceRequired;

        boolean valid() {
            return id != null && id.matches("[a-z0-9_.-]{1,48}") && name != null && !name.isEmpty()
                    && name.length() <= 64 && description != null && description.length() <= 512
                    && MilitaryRelations.isFaction(faction) && classification != null && classification.length() <= 48
                    && ("rifle".equals(behavior) || "close".equals(behavior) || "marksman".equals(behavior)
                        || "support".equals(behavior) || "anti_armor".equals(behavior))
                    && tier >= 1 && tier <= 10 && armor >= 0 && armor <= 20 && rank >= 0 && rank <= 10
                    && capacity >= 0 && capacity <= 32 && reactionTicks >= 1 && reactionTicks <= 120
                    && range >= 6 && range <= 128 && recruitmentCost >= 0 && currencyUpkeep >= 0
                    && foodConsumption >= 0 && requiredRelation == MilitaryRelations.clamp(requiredRelation)
                    && (minimumReputation == null || minimumReputation == MilitaryRelations.clamp(minimumReputation))
                    && reinforcementWeight >= 0 && reinforcementWeight <= 1000
                    && health >= 1 && health <= 512 && resistance >= 0 && resistance <= .9F
                    && speed >= .05F && speed <= .6F && accuracy >= 0 && accuracy <= 5
                    && dispersion >= 0 && dispersion <= 5 && recoil >= 0 && recoil <= 5 && reload >= .1F && reload <= 5;
        }

        boolean requiresPrimary() {
            return primary != null && !primary.isEmpty() || primaryPools != null && primaryPools.length > 0;
        }

        ItemStack primary(Random random) {
            return WeaponCatalog.choose(primary, primaryPools, random);
        }

        ItemStack secondary(Random random) {
            return WeaponCatalog.choose(secondary, secondaryPools, random);
        }

        ItemStack previewWeapon() {
            return primary(new Random(id.hashCode()));
        }

        public void equipArmor(EntityMilitaryUnit unit) {
            if (equipment == null) return;
            for (String id : equipment) {
                ItemStack stack = item(id);
                if (stack != null && stack.getItem() instanceof ItemArmor) {
                    int slot = 4 - ((ItemArmor) stack.getItem()).armorType;
                    unit.setCurrentItemOrArmor(slot, stack);
                }
            }
        }

        static ItemStack item(String id) {
            if (id == null || id.isEmpty()) return null;
            int separator = id.lastIndexOf('@');
            try {
                String key = separator == -1 ? id : id.substring(0, separator);
                Object registered = Item.itemRegistry.getObject(key);
                if (!(registered instanceof Item)) return null;
                return new ItemStack((Item) registered, 1, separator == -1 ? 0 : Integer.parseInt(id.substring(separator + 1)));
            } catch (NumberFormatException invalid) {
                return null;
            }
        }
    }
}
