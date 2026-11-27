
package hmggvcmob.military;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import cpw.mods.fml.common.FMLLog;
import handmadeguns.items.WeaponClass;
import handmadeguns.items.guns.HMGItem_Unified_Guns;
import hmggvcmob.GVCMobPlus;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public final class WeaponCatalog {
    private static final Map<String, Entry> WEAPONS = new TreeMap<>();
    private static final Map<String, List<Entry>> POOLS = new TreeMap<>();

    private WeaponCatalog() {}

    public static void load(Path directory) {
        Map<String, Override> overrides = Collections.emptyMap();
        Path file = directory.resolve("weapon-overrides.json");
        try {
            if (!Files.exists(file)) Files.write(file, Collections.singletonList("{}"), StandardCharsets.UTF_8);
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Map<String, Override> parsed = new GsonBuilder().create().fromJson(reader,
                        new TypeToken<Map<String, Override>>() {}.getType());
                if (parsed != null) overrides = parsed;
            }
        } catch (IOException | RuntimeException failure) {
            FMLLog.warning("[GVC military] Cannot read %s: %s", file, failure.getMessage());
        }
        WEAPONS.clear();
        POOLS.clear();
        for (Object item : Item.itemRegistry) {
            if (!(item instanceof HMGItem_Unified_Guns)) continue;
            HMGItem_Unified_Guns gun = (HMGItem_Unified_Guns) item;
            String id = Item.itemRegistry.getNameForObject(gun);
            Entry entry = new Entry(gun, overrides.get(id));
            WEAPONS.put(id, entry);
            if (entry.canEquip()) POOLS.computeIfAbsent(entry.classification, ignored -> new ArrayList<>()).add(entry);
        }
        updateLegacyPools();
        for (String id : overrides.keySet()) {
            if (!WEAPONS.containsKey(id)) FMLLog.warning("[GVC military] Unknown weapon override %s", id);
        }
        try (Writer report = Files.newBufferedWriter(directory.resolve("weapon-compatibility.json"), StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(WEAPONS, report);
        } catch (IOException failure) {
            FMLLog.warning("[GVC military] Cannot write weapon compatibility report: %s", failure.getMessage());
        }
        long supported = WEAPONS.values().stream().filter(entry -> entry.status.equals("supported")).count();
        long unknown = WEAPONS.values().stream().filter(entry -> entry.status.equals("unclassified")).count();
        FMLLog.info("[GVC military] NPC firearms: %d supported, %d unclassified, %d unsupported",
                supported, unknown, WEAPONS.size() - supported - unknown);
    }

    private static void updateLegacyPools() {
        for (List<?> pool : new List<?>[]{GVCMobPlus.Guns_AR, GVCMobPlus.Guns_SG, GVCMobPlus.Guns_SR,
                GVCMobPlus.Guns_LMG, GVCMobPlus.Guns_RR, GVCMobPlus.Guns_FL, GVCMobPlus.Guns_GR,
                GVCMobPlus.Guns_HMG, GVCMobPlus.Guns_BoltR, GVCMobPlus.Guns_CanUse}) pool.clear();
        for (Entry entry : WEAPONS.values()) {
            if (!entry.canEquip()) continue;
            HMGItem_Unified_Guns gun = entry.gun;
            GVCMobPlus.Guns_CanUse.add(gun);
            switch (entry.classification) {
                case "ASSAULT_RIFLE": GVCMobPlus.Guns_AR.add(gun); break;
                case "SHOTGUN": GVCMobPlus.Guns_SG.add(gun); break;
                case "SNIPER_RIFLE":
                case "BOLT_ACTION_RIFLE": GVCMobPlus.Guns_SR.add(gun); break;
                case "MACHINE_GUN": GVCMobPlus.Guns_LMG.add(gun); break;
                case "LAUNCHER": GVCMobPlus.Guns_RR.add(gun); break;
                case "SPECIAL": if (entry.legacyClass == 5) GVCMobPlus.Guns_FL.add(gun); break;
                default: break;
            }
            if (entry.projectileType == 0 && gun.gunInfo.needcock) GVCMobPlus.Guns_BoltR.add(gun);
            if (entry.projectileType == 2) GVCMobPlus.Guns_GR.add(gun);
        }
    }

    static ItemStack choose(String id, String[] pools, Random random) {
        Entry exact = WEAPONS.get(id == null ? "" : id);
        if (exact != null && exact.canEquip()) return new ItemStack(exact.gun);
        if (pools == null || pools.length == 0) return null;
        for (String pool : pools) {
            if (pool == null) continue;
            List<Entry> eligible = POOLS.get(pool.toUpperCase(Locale.ROOT));
            if (eligible != null && !eligible.isEmpty()) return new ItemStack(eligible.get(random.nextInt(eligible.size())).gun);
        }
        return null;
    }

    public static String classification(HMGItem_Unified_Guns gun) {
        Entry entry = WEAPONS.get(Item.itemRegistry.getNameForObject(gun));
        return entry == null ? "" : entry.classification;
    }

    private static final class Override {
        String classification;
        Boolean enabled;
        String reason;
    }

    private static final class Entry {
        transient final HMGItem_Unified_Guns gun;
        final String classification, status, reason;
        final String hmgClass;
        final int legacyClass, projectileType;
        final boolean firstCock, mountedOnly, hmgNpcAllowed, oneUse, perShell, targetLock, manualGuidance, chargedTrigger;
        final int magazineSlots;
        final List<Integer> fireModes;
        final List<String> magazines = new ArrayList<>();

        Entry(HMGItem_Unified_Guns gun, Override override) {
            this.gun = gun;
            WeaponClass type = gun.gunInfo.weaponClass;
            hmgClass = type == null ? "" : type.name();
            legacyClass = gun.gunInfo.canuseclass;
            projectileType = gun.gunInfo.guntype;
            firstCock = gun.gunInfo.needFirstCock;
            mountedOnly = gun.gunInfo.needfix;
            oneUse = gun.gunInfo.isOneuse;
            perShell = gun.gunInfo.perShellReloadStages;
            magazineSlots = gun.gunInfo.magazineItemCount;
            targetLock = gun.gunInfo.canlockEntity || gun.gunInfo.canlockBlock;
            manualGuidance = gun.gunInfo.SACLOS_Homing;
            chargedTrigger = gun.gunInfo.chargeType;
            hmgNpcAllowed = gun.gunInfo.guerrila_can_use;
            fireModes = gun.gunInfo.burstcount == null ? Collections.emptyList() : new ArrayList<>(gun.gunInfo.burstcount);
            if (gun.gunInfo.magazine != null) {
                for (Item magazine : gun.gunInfo.magazine) {
                    if (magazine != null) magazines.add(Item.itemRegistry.getNameForObject(magazine));
                }
            }
            classification = override != null && override.classification != null && !override.classification.trim().isEmpty()
                    ? override.classification.trim().toUpperCase(Locale.ROOT)
                    : type == null || type == WeaponClass.SPECIAL ? legacyClass(legacyClass) : hmgClass;
            boolean manuallyEnabled = override != null && Boolean.TRUE.equals(override.enabled);
            String unsupported = mountedOnly ? "requires an emplacement" : null;
            if (unsupported == null && (oneUse || type == WeaponClass.VEHICLE_WEAPON)) unsupported = "requires disposable or vehicle handling";
            if (unsupported == null && fireModes.stream().noneMatch(mode -> mode != null && (mode == -1 || mode > 0))) {
                unsupported = "no enabled firing mode";
            }
            if (unsupported == null && (magazines.isEmpty() || magazineSlots < 1)) unsupported = "no resolved magazine";
            if (unsupported == null && override != null && Boolean.FALSE.equals(override.enabled)) unsupported = "disabled by override";
            if (unsupported == null && !hmgNpcAllowed && !manuallyEnabled) unsupported = "HMG pack opts out of NPC selection";
            if (unsupported != null) {
                status = "unsupported";
                reason = detail(unsupported, override);
            } else if (classification.isEmpty() || type == WeaponClass.SAMPLE_WEAPON && !manuallyEnabled) {
                status = "unclassified";
                reason = detail("requires manual classification or sample opt-in", override);
            } else {
                status = "supported";
                reason = detail("uses HMG handheld fire/reload API; runtime verification is separate", override);
            }
        }

        boolean canEquip() {
            return status.equals("supported");
        }

        private static String detail(String base, Override override) {
            return override == null || override.reason == null ? base : base + ". " + override.reason;
        }

        private static String legacyClass(int value) {
            switch (value) {
                case 0: return "ASSAULT_RIFLE";
                case 1: return "SHOTGUN";
                case 2: return "SNIPER_RIFLE";
                case 3: return "MACHINE_GUN";
                case 4: return "LAUNCHER";
                case 5: return "SPECIAL";
                default: return "";
            }
        }
    }
}
