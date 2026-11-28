package hmggvcmob.military;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cpw.mods.fml.common.FMLLog;
import hmggvcmob.GVCMobPlus;
import hmggvcmob.camp.CampObj;
import hmggvcmob.entity.IflagBattler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

public final class MilitaryRelations {
    public static final int MIN_REPUTATION = -1000000, MAX_REPUTATION = 1000000;
    private static final int MAX_FACTIONS = 32;
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]{1,48}");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Faction> FACTIONS = new LinkedHashMap<>();
    private static final Map<String, Map<String, Integer>> RELATIONS = new HashMap<>();
    private static final Map<String, List<String>> OPPOSITION = new HashMap<>();
    private static final NavigableMap<Integer, Standing> STANDINGS = new TreeMap<>();
    private static List<String> ids = Collections.emptyList();

    private MilitaryRelations() {}

    static void load(Path directory, String[] entries) {
        FACTIONS.clear();
        RELATIONS.clear();
        OPPOSITION.clear();
        loadStandings();
        addBuiltin("union", "Defense Corps", "Regular infantry defending fortified outposts and villages.", 10, "union", 0x87AD87);
        addBuiltin("allies", "Insurgents", "Irregular fighters operating from camps and occupied villages.", -150, "allies", 0xD88774);
        addBuiltin("pmc", "Free Company", "Independent soldiers available for hire.", 10, "pmc", 0xD8BA72);
        addBuiltin("cinder", "Cinder Raiders", "Predatory bands that raid supply lines and occupy ruined infrastructure.", -180, "allies", 0xA26391);
        addBuiltin("crossroads", "Crossroads Cooperative", "Independent market towns protected by local watch patrols.", 0, "pmc", 0x7CAA9E);
        addBuiltin("dune", "Dunewatch", "Desert communities that maintain relay stations and guarded water stops.", 0, "union", 0xD3A066);
        addBuiltin("northline", "Northline Rangers", "Winter patrols that provision isolated lodges and high-country waystations.", 10, "union", 0x91AFD9);
        identity("union", "defense", "standardized", "conventional", 10, 100, "plains", "forest");
        identity("allies", "insurgents", "improvised", "irregular", 9, 88, "forest", "hills", "swamp");
        identity("pmc", "company", "industrial", "contract", 7, 112, "plains", "coast", "ocean");
        identity("cinder", "raiders", "salvaged", "raiding", 5, 100, "forest", "hills", "swamp");
        identity("crossroads", "cooperative", "civilian", "local watch", 12, 94, "plains", "forest", "coast");
        identity("dune", "dune", "sandstone", "relay patrol", 14, 100, "desert");
        identity("northline", "northline", "alpine", "winter patrol", 14, 102, "snow");
        definition("cinder").pledging = false;
        definition("cinder").settlementCapture = false;
        definition("cinder").memberInfrastructure = false;
        definition("crossroads").pledging = false;
        definition("crossroads").settlementCapture = false;
        definition("union").loot = new String[]{"minecraft:bread", "minecraft:torch"};
        definition("allies").loot = new String[]{"minecraft:bread", "minecraft:coal"};
        definition("pmc").loot = new String[]{"minecraft:iron_ingot", "GVCGuns:HealthPack"};
        definition("cinder").loot = new String[]{"minecraft:gold_nugget", "minecraft:coal", "minecraft:leather"};
        definition("crossroads").loot = new String[]{"minecraft:bread", "minecraft:wheat"};
        definition("dune").loot = new String[]{"minecraft:paper", "minecraft:bread"};
        definition("northline").loot = new String[]{"minecraft:coal", "minecraft:cooked_beef"};
        definition("union").firearmClasses = new String[]{"ASSAULT_RIFLE", "MACHINE_GUN", "SHOTGUN", "SPECIAL"};
        definition("allies").firearmClasses = new String[]{"ASSAULT_RIFLE", "SHOTGUN", "SPECIAL"};
        definition("dune").firearmClasses = new String[]{"ASSAULT_RIFLE", "SHOTGUN", "SPECIAL"};
        definition("northline").firearmClasses = new String[]{"ASSAULT_RIFLE", "SNIPER_RIFLE", "SHOTGUN"};
        Path file = directory.resolve("factions.json");
        try {
            if (Files.notExists(file)) {
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    JSON.toJson(FACTIONS.values().toArray(new Faction[0]), writer);
                }
            }
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonElement definitions = new JsonParser().parse(reader);
                if (!definitions.isJsonArray()) throw new IllegalArgumentException("Expected a faction array");
                for (JsonElement entry : definitions.getAsJsonArray()) {
                    if (!entry.isJsonObject()) continue;
                    JsonObject fields = entry.getAsJsonObject();
                    JsonElement key = fields.get("id");
                    Faction builtin = key != null && key.isJsonPrimitive() ? FACTIONS.get(key.getAsString()) : null;
                    JsonObject merged = builtin == null ? new JsonObject() : JSON.toJsonTree(builtin).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> field : fields.entrySet()) merged.add(field.getKey(), field.getValue());
                    Faction definition;
                    try {
                        definition = JSON.fromJson(merged, Faction.class);
                    } catch (RuntimeException invalid) {
                        FMLLog.warning("[GVC military] Ignored invalid faction entry in %s: %s", file, invalid.getMessage());
                        continue;
                    }
                    if (definition.valid()
                            && (FACTIONS.size() < MAX_FACTIONS || FACTIONS.containsKey(definition.id))) {
                        definition.camp = builtinCamp(definition.id) ? definition.id : definition.camp;
                        if ("pmc".equals(definition.id) && definition.entryTier == null) definition.entryTier = 2;
                        FACTIONS.put(definition.id, definition);
                    } else {
                        FMLLog.warning("[GVC military] Ignored invalid or excess faction entry in %s", file);
                    }
                }
            }
        } catch (IOException | RuntimeException failure) {
            FMLLog.warning("[GVC military] Cannot load factions from %s: %s", file, failure.getMessage());
        }
        ids = Collections.unmodifiableList(new ArrayList<>(FACTIONS.keySet()));
        for (String id : ids) RELATIONS.put(id, new HashMap<>());
        setRelation("union", "pmc", 30);
        setRelation("union", "allies", -50);
        setRelation("pmc", "allies", -50);
        setRelation("union", "northline", 30);
        for (String id : ids) setRelation("cinder", id, -80);
        for (Faction faction : FACTIONS.values()) {
            if (faction.relations == null) continue;
            for (Map.Entry<String, Integer> relation : faction.relations.entrySet()) {
                if (relation.getValue() != null) setRelation(faction.id, relation.getKey(), relation.getValue());
            }
        }
        for (String entry : entries) {
            String[] fields = entry.trim().split("[=,]", -1);
            if (fields.length != 3) continue;
            try {
                setRelation(fields[0].trim(), fields[1].trim(), Integer.parseInt(fields[2].trim()));
            } catch (NumberFormatException invalid) {
                FMLLog.warning("[GVC military] Invalid faction relation: %s", entry);
            }
        }
        for (String id : ids) {
            List<String> enemies = new ArrayList<>();
            for (String candidate : ids) if (available(candidate) && factionRelation(id, candidate) < 0) enemies.add(candidate);
            OPPOSITION.put(id, Collections.unmodifiableList(enemies));
        }
    }

    private static void loadStandings() {
        STANDINGS.clear();
        STANDINGS.put(MIN_REPUTATION, new Standing("Hostile", 0, 0));
        for (String entry : MilitaryConfig.militaryStandings) {
            String[] fields = entry.split(",", -1);
            if (fields.length != 4) continue;
            try {
                int minimum = clamp(Integer.parseInt(fields[0].trim()));
                String label = fields[1].trim();
                int tier = Integer.parseInt(fields[2].trim());
                int discount = Integer.parseInt(fields[3].trim());
                if (!label.isEmpty() && label.length() <= 32 && tier >= 0 && tier <= 10 && discount >= 0 && discount <= 90) {
                    STANDINGS.put(minimum, new Standing(label, tier, discount));
                }
            } catch (NumberFormatException invalid) {
                FMLLog.warning("[GVC military] Invalid standing: %s", entry);
            }
        }
    }

    private static void addBuiltin(String id, String name, String description, int reputation, String camp, int color) {
        Faction faction = new Faction();
        faction.id = id;
        faction.name = name;
        faction.description = description;
        faction.initialReputation = reputation;
        faction.camp = camp;
        faction.color = color;
        faction.entryTier = "pmc".equals(id) ? 2 : 1;
        FACTIONS.put(id, faction);
    }

    private static boolean builtinCamp(String id) {
        return "union".equals(id) || "allies".equals(id) || "pmc".equals(id);
    }

    private static void identity(String id, String insignia, String architecture, String doctrine,
                                 int regionalWeight, int price, String... biomes) {
        Faction faction = definition(id);
        faction.insignia = insignia;
        faction.architecture = architecture;
        faction.doctrine = doctrine;
        faction.regionalWeight = regionalWeight;
        faction.price = price;
        faction.biomes = biomes;
    }

    private static void setRelation(String first, String second, int score) {
        if (!isFaction(first) || !isFaction(second) || first.equals(second)) return;
        int relation = Math.max(-100, Math.min(100, score));
        RELATIONS.get(first).put(second, relation);
        RELATIONS.get(second).put(first, relation);
    }

    public static List<String> factions() { return ids; }
    static List<String> opponents(String faction) { return OPPOSITION.getOrDefault(faction, Collections.emptyList()); }
    static Faction definition(String id) { return FACTIONS.get(id); }
    public static String displayName(String id) {
        Faction faction = definition(id);
        return faction == null ? id : faction.name;
    }
    public static String insignia(String id) {
        Faction faction = definition(id);
        return faction == null ? "independent" : faction.insignia;
    }

    public static int color(String id) {
        Faction faction = definition(id);
        return faction == null ? 0x899CA8 : faction.color;
    }

    public static String textureProfile(String id) {
        Faction faction = definition(id);
        return faction == null ? "pmc" : faction.camp;
    }
    static boolean isFaction(String id) { return FACTIONS.containsKey(id); }
    static boolean available(String id) {
        Faction faction = definition(id);
        return faction != null && faction.enabled;
    }
    static boolean validId(String id) { return id != null && ID.matcher(id).matches(); }

    public static String faction(CampObj camp) {
        if (camp != null && (camp == GVCMobPlus.guerrillas || camp == GVCMobPlus.guerrillas_AirBase)) return "allies";
        if (camp != null && (camp == GVCMobPlus.soldiers || camp == GVCMobPlus.soldiers_AirBase)) return "union";
        return "pmc";
    }

    public static CampObj camp(String id) {
        Faction faction = definition(id);
        String profile = faction == null ? id : faction.camp;
        if ("allies".equals(profile)) return GVCMobPlus.guerrillas;
        if ("union".equals(profile)) return GVCMobPlus.soldiers;
        return GVCMobPlus.forPlayer;
    }

    public static String faction(Entity entity) {
        if (entity instanceof EntityMilitaryUnit) return ((EntityMilitaryUnit) entity).faction();
        if (entity instanceof EntityMilitaryTrader) return ((EntityMilitaryTrader) entity).faction();
        if (entity instanceof IflagBattler) return faction(((IflagBattler) entity).getCampObj());
        return null;
    }

    public static int factionRelation(String first, String second) {
        if (first == null || !isFaction(first) || !isFaction(second)) return 0;
        return first.equals(second) ? 100 : RELATIONS.get(first).getOrDefault(second, 0);
    }

    public static int reputation(UUID player, String id) {
        Faction faction = definition(id);
        if (player == null || faction == null) throw new IllegalArgumentException("Player and known faction required");
        MilitaryEconomy.Account account = MilitaryEconomy.get().findAccount(player);
        return account == null ? faction.initialReputation : account.relations.getOrDefault(id, faction.initialReputation);
    }

    public static void setReputation(UUID player, String faction, int score) {
        if (player == null || !isFaction(faction)) throw new IllegalArgumentException("Player and known faction required");
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(player);
        int value = clamp(score);
        if (!account.relations.containsKey(faction) || reputation(player, faction) != value) {
            account.relations.put(faction, value);
            economy.changed(account);
        }
    }

    public static String allegiance(UUID player) {
        MilitaryEconomy.Account account = MilitaryEconomy.get().findAccount(player);
        return account != null && isFaction(account.allegiance) ? account.allegiance : "";
    }

    static String pledge(EntityPlayer player, String faction) {
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(player.getUniqueID());
        String old = account.allegiance;
        Faction next = definition(faction);
        long now = MilitaryEconomy.now();
        if (faction.equals(old) || !faction.isEmpty() && (next == null || !next.enabled || !next.pledging
                || reputation(account.owner, faction) < next.allegianceRequired || hostileToPlayer(account.owner, faction))) return "pledge_locked";
        if (now < account.allegianceAfter) return "pledge_cooldown";
        if (isFaction(old)) MilitaryReputation.adjust(account, old, -MilitaryConfig.allegiancePenalty);
        account.allegiance = faction;
        account.allegianceAfter = now + MilitaryConfig.allegianceCooldown;
        economy.changed(account);
        if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            MilitaryNetwork.syncAllegiance((net.minecraft.entity.player.EntityPlayerMP)player);
        }
        return faction.isEmpty() ? "renounced" : "pledged";
    }

    public static int playerRelation(EntityPlayer player, String faction) {
        Faction definition = definition(faction);
        return definition == null ? 0 : player.worldObj.isRemote ? definition.initialReputation : reputation(player.getUniqueID(), faction);
    }

    public static boolean hostileToPlayer(UUID player, String faction) {
        Faction definition = definition(faction);
        return definition != null && reputation(player, faction) < definition.hostileBelow;
    }

    static boolean permitsTrade(EntityPlayer player, String faction) {
        Faction definition = definition(faction);
        return definition != null && definition.enabled && playerRelation(player, faction) >= Math.max(definition.tradeRequired, definition.hostileBelow);
    }

    static UUID owner(Entity entity) {
        return entity instanceof EntityMilitaryUnit ? ((EntityMilitaryUnit) entity).owner() : null;
    }

    public static boolean hostile(Entity source, Entity other) {
        String faction = faction(source);
        if (!isFaction(faction) || other == null) return false;
        UUID owner = owner(source);
        if (other instanceof EntityPlayer) {
            UUID player = other.getUniqueID();
            if (player.equals(owner)) return false;
            if (MilitaryReputation.retaliating(source, other)) return true;
            return playerRelation((EntityPlayer) other, faction) < definition(faction).hostileBelow;
        }
        if (MilitaryReputation.retaliating(source, other)) return true;
        String opposing = faction(other);
        if (!isFaction(opposing)) return false;
        UUID otherOwner = owner(other);
        if (owner != null && owner.equals(otherOwner)) return false;
        if (!source.worldObj.isRemote && (owner != null || otherOwner != null)) {
            return owner != null && hostileToPlayer(owner, opposing) || otherOwner != null && hostileToPlayer(otherOwner, faction);
        }
        return factionRelation(faction, opposing) < 0;
    }

    static int clamp(int score) { return Math.max(MIN_REPUTATION, Math.min(MAX_REPUTATION, score)); }

    static Standing standing(int score) { return STANDINGS.floorEntry(clamp(score)).getValue(); }

    static int recruitmentTier(String id, int score) {
        Faction faction = definition(id);
        int tier = standing(score).tier;
        if (!faction.enabled || score < faction.recruitRequired || score < faction.hostileBelow || tier == 0) return 0;
        return Math.max(tier, faction.entryTier == null ? 1 : faction.entryTier);
    }

    static int requirement(UnitPresets.Preset preset) {
        Faction faction = definition(preset.faction);
        int min = Math.max(Math.max(faction.recruitRequired, faction.tradeRequired), Math.max(faction.hostileBelow, preset.requiredRelation));
        if (preset.minimumReputation != null) return Math.max(min, preset.minimumReputation);
        for (Map.Entry<Integer, Standing> entry : STANDINGS.entrySet()) {
            Integer next = STANDINGS.higherKey(entry.getKey());
            int candidate = Math.max(min, entry.getKey());
            if ((next == null || candidate < next) && recruitmentTier(preset.faction, candidate) >= preset.tier) return candidate;
        }
        return MAX_REPUTATION + 1;
    }

    static int discount(UUID player, String id) {
        int discount = standing(reputation(player, id)).discount;
        if (id.equals(allegiance(player))) discount += definition(id).allegianceDiscount;
        return Math.min(90, discount);
    }

    static void writeFactions(NBTTagCompound snapshot, UUID player) {
        NBTTagList rows = new NBTTagList();
        String pledged = allegiance(player);
        snapshot.setString("Allegiance", pledged);
        snapshot.setLong("AllegianceWait", Math.max(0, MilitaryEconomy.get().account(player).allegianceAfter - MilitaryEconomy.now()));
        snapshot.setInteger("AllegiancePenalty", MilitaryConfig.allegiancePenalty);
        for (String id : ids) {
            Faction faction = definition(id);
            if (!faction.enabled && !id.equals(pledged)) continue;
            int score = reputation(player, id);
            Standing rank = standing(score);
            Integer next = STANDINGS.higherKey(score);
            NBTTagCompound row = new NBTTagCompound();
            row.setString("Id", id);
            row.setString("Name", faction.name);
            row.setString("Description", faction.description);
            row.setString("Insignia", faction.insignia);
            row.setString("Doctrine", faction.doctrine);
            row.setString("Architecture", faction.architecture);
            row.setInteger("Color", faction.color);
            row.setInteger("Score", score);
            row.setString("Standing", rank.label);
            row.setInteger("Tier", recruitmentTier(id, score));
            row.setInteger("Discount", discount(player, id));
            row.setInteger("Next", next == null ? score : next);
            row.setInteger("Previous", STANDINGS.floorKey(score));
            row.setBoolean("Hostile", score < faction.hostileBelow);
            row.setBoolean("Trade", score >= Math.max(faction.hostileBelow, faction.tradeRequired));
            row.setBoolean("Pledging", faction.pledging);
            row.setInteger("PledgeRequired", faction.allegianceRequired);
            row.setBoolean("CanPledge", faction.pledging && score >= Math.max(faction.allegianceRequired, faction.hostileBelow));
            row.setInteger("PledgeDiscount", faction.allegianceDiscount);
            row.setBoolean("Capture", faction.settlementCapture && MilitaryConfig.factionCapture);
            row.setBoolean("Infrastructure", faction.memberInfrastructure);
            row.setString("Allies", relatedNames(id, 1));
            row.setString("Enemies", relatedNames(id, -1));
            row.setString("Neutral", relatedNames(id, 0));
            MilitaryQuests.stats(row, MilitaryEconomy.get().account(player), id);
            rows.appendTag(row);
        }
        snapshot.setTag("Factions", rows);
    }

    private static String relatedNames(String faction, int disposition) {
        List<String> names = new ArrayList<>();
        for (String id : ids) {
            if (id.equals(faction) || !available(id)) continue;
            int relation = factionRelation(faction, id);
            if (Integer.signum(relation) == disposition) names.add(definition(id).name);
        }
        return String.join(", ", names);
    }

    static final class Standing {
        final String label;
        final int tier, discount;

        Standing(String label, int tier, int discount) {
            this.label = label;
            this.tier = tier;
            this.discount = discount;
        }
    }

    static final class Faction {
        String id, name, description = "", camp = "pmc";
        String insignia = "independent", architecture = "", doctrine = "";
        String[] biomes = {}, structures = {}, equipment = {}, firearmClasses = {}, recruitment = {}, quests = {}, loot = {};
        Map<String, Integer> relations = new LinkedHashMap<>();
        int regionalWeight = 8, price = 100;
        int initialReputation = 0, hostileBelow = -100, tradeRequired = -100, recruitRequired = 0;
        int allegianceRequired = 1000, allegianceDiscount = 10, color = 0xD8BA72;
        Integer entryTier;
        double gainMultiplier = 1, lossMultiplier = 1;
        boolean enabled = true, pledging = true, settlementCapture = true, memberInfrastructure = true;

        boolean valid() {
            return validId(id) && name != null && !name.trim().isEmpty() && name.length() <= 48
                    && description != null && description.length() <= 256 && builtinCamp(camp)
                    && validId(insignia) && architecture != null && architecture.length() <= 64
                    && doctrine != null && doctrine.length() <= 64
                    && biomes != null && structures != null && equipment != null && firearmClasses != null
                    && recruitment != null && quests != null && loot != null
                    && regionalWeight >= 0 && regionalWeight <= 1000 && price >= 25 && price <= 400
                    && initialReputation == clamp(initialReputation) && hostileBelow == clamp(hostileBelow)
                    && tradeRequired == clamp(tradeRequired) && recruitRequired == clamp(recruitRequired)
                    && allegianceRequired == clamp(allegianceRequired) && allegianceDiscount >= 0 && allegianceDiscount <= 90
                    && (entryTier == null || entryTier >= 1 && entryTier <= 10)
                    && gainMultiplier >= 0 && gainMultiplier <= 100 && lossMultiplier >= 0 && lossMultiplier <= 100;
        }
    }
}
