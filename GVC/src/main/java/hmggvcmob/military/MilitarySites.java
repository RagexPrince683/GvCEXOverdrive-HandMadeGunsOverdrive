package hmggvcmob.military;

import com.google.gson.Gson;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.IWorldGenerator;
import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraft.world.World;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class MilitarySites implements IWorldGenerator {
    private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<>();
    private static final Map<String, List<Definition>> POOLS = new LinkedHashMap<>();
    private static final Map<Integer, Map<Long, List<FixedSite>>> FIXED = new LinkedHashMap<>();
    private static Overrides overrides = new Overrides();
    private static final long SITE_SALT = 0x456D67B883C30A41L;
    private static final long REGION_SALT = 0xD8933B2EF804615DL;

    static void load(Path directory) {
        DEFINITIONS.clear();
        POOLS.clear();
        FIXED.clear();
        Definition[] entries = read(directory, "sites.json", Definition[].class, new Definition[0], true);
        Definition[] fieldworks = read(directory, "fieldworks.json", Definition[].class, new Definition[0], true);
        addDefinitions(entries);
        addDefinitions(fieldworks);
        overrides = read(directory, "regions.json", Overrides.class, new Overrides(), true);
        if (overrides.regions == null) overrides.regions = new RegionOverride[0];
        if (overrides.sites == null) overrides.sites = new FixedSite[0];
        overrides.regions = Arrays.stream(overrides.regions).limit(512).filter(value -> value != null && value.valid())
                .toArray(RegionOverride[]::new);
        overrides.sites = Arrays.stream(overrides.sites).limit(2048).filter(value -> value != null && value.valid())
                .toArray(FixedSite[]::new);
        for (FixedSite fixed : overrides.sites) {
            int half = DEFINITIONS.get(fixed.site).width / 2;
            Map<Long, List<FixedSite>> chunks = FIXED.computeIfAbsent(fixed.dimension, dimension -> new LinkedHashMap<>());
            for (int cx = (fixed.x - half) >> 4; cx <= (fixed.x + half) >> 4; cx++) {
                for (int cz = (fixed.z - half) >> 4; cz <= (fixed.z + half) >> 4; cz++) {
                    chunks.computeIfAbsent(ChunkCoordIntPair.chunkXZ2Int(cx, cz), chunk -> new ArrayList<>()).add(fixed);
                }
            }
        }
        FMLLog.info("[GVC military] Loaded %d regional site definitions, %d authored regions and %d fixed sites",
                DEFINITIONS.size(), overrides.regions.length, overrides.sites.length);
    }

    private static void addDefinitions(Definition[] entries) {
        for (Definition site : entries) {
            if (site == null || !site.valid() || DEFINITIONS.containsKey(site.id)) {
                FMLLog.warning("[GVC military] Ignored invalid or duplicate site: %s", site == null ? "null" : site.id);
                continue;
            }
            DEFINITIONS.put(site.id, site);
            POOLS.computeIfAbsent(site.faction, key -> new ArrayList<>()).add(site);
        }
    }

    static <T> T read(Path directory, String file, Class<T> type, T fallback, boolean bundled) {
        Path path = directory.resolve(file);
        String resource = "/assets/gvcmob/military/" + file;
        try {
            if (Files.notExists(path)) {
                if (!bundled) return fallback;
                try (InputStream input = MilitarySites.class.getResourceAsStream(resource)) {
                    if (input == null) throw new IOException("Missing default " + file);
                    Files.copy(input, path);
                }
            }
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                T parsed = new Gson().fromJson(reader, type);
                if (parsed == null) throw new IllegalArgumentException("Empty definition");
                return parsed;
            }
        } catch (IOException | RuntimeException failure) {
            FMLLog.warning("[GVC military] Cannot load %s: %s", path, failure.getMessage());
            if (bundled) {
                try (InputStream input = MilitarySites.class.getResourceAsStream(resource)) {
                    if (input != null) return new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), type);
                } catch (IOException | RuntimeException invalidDefault) {
                    FMLLog.severe("[GVC military] Cannot read bundled %s: %s", file, invalidDefault.getMessage());
                }
            }
            return fallback;
        }
    }

    @Override
    public void generate(Random ignored, int chunkX, int chunkZ, World world, IChunkProvider generator, IChunkProvider loaded) {
        if (world.isRemote) return;
        int dimension = world.provider.dimensionId;
        Map<Long, List<FixedSite>> fixedChunks = FIXED.get(dimension);
        List<FixedSite> fixedSites = fixedChunks == null ? null : fixedChunks.get(ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ));
        if (fixedSites != null) for (FixedSite fixed : fixedSites) {
            Definition site = DEFINITIONS.get(fixed.site);
            place(world, fixed.x, fixed.z, fixed.rotation, site, fixed.faction == null ? site.faction : fixed.faction);
        }
        if (dimension != 0 || !MilitaryConfig.regionalSites) return;
        MilitaryFrontlines.generate(world, chunkX, chunkZ);
        MilitaryRoads.chunkReady(world, chunkX, chunkZ);
        generateRegional(world, chunkX, chunkZ);
        if (MilitaryConfig.fieldworks && MilitaryConfig.factionRoads) {
            for (Occupation post : WorldSavedData_Flag.get(world).near(chunkX * 16 + 8, chunkZ * 16 + 8, 121)) {
                satellite(world, post, chunkX, chunkZ, false);
            }
        }
    }

    private static void generateRegional(World world, int chunkX, int chunkZ) {
        int grid = MilitaryConfig.siteSpacing;
        int cellX = Math.floorDiv(chunkX, grid), cellZ = Math.floorDiv(chunkZ, grid);
        Random random = random(world, cellX, cellZ, SITE_SALT);
        if (random.nextInt(100) >= MilitaryConfig.siteDensity) return;
        int centerChunkX = cellX * grid + 1 + random.nextInt(grid - 2);
        int centerChunkZ = cellZ * grid + 1 + random.nextInt(grid - 2);
        if (Math.abs(centerChunkX - chunkX) > 1 || Math.abs(centerChunkZ - chunkZ) > 1) return;
        int x = centerChunkX * 16 + 8, z = centerChunkZ * 16 + 8;
        RegionOverride authored = regionOverride(world, x, z);
        if (authored != null && !authored.procedural) return;
        String biome = biome(world, x, z);
        List<String> factions = regionalFactions(world, x, z, biome);
        boolean useRegion = random.nextInt(100) < MilitaryConfig.influenceStrength;
        String preferred = factions.get(useRegion && random.nextInt(100) < 78 ? 0 : 1);
        String faction = authored != null && authored.faction != null ? authored.faction : preferred;
        if (!useRegion && authored == null) faction = chooseFaction(world, x, z, biome, random, null);
        if (MilitaryConfig.abandonedSites && random.nextInt(100) < 17 && authored == null) faction = "";
        if (faction.isEmpty() && !MilitaryConfig.abandonedSites) return;
        MilitaryRelations.Faction profile = MilitaryRelations.definition(faction);
        if (authored == null && profile != null && profile.initialReputation < profile.hostileBelow
                && protectedStartingArea(world, x, z)) return;
        Definition site = chooseSite(faction, biome, random, regionalDepth(x, z));
        if (site != null && withinFootprint(chunkX, chunkZ, x, z, site.width / 2)) {
            int rotation = random.nextInt(4);
            Occupation post = place(world, x, z, rotation, site, faction, site.layout.startsWith("TUNNEL_"));
            if (post != null && MilitaryConfig.fieldworks && MilitaryConfig.factionRoads) satellite(world, post, 0, 0, true);
        }
    }

    private static void satellite(World world, Occupation hub, int chunkX, int chunkZ, boolean anyChunk) {
        if (hub.classification() != Occupation.Classification.SETTLEMENT || hub.entrance.length != 3 || hub.tenure > 0) return;
        String id;
        switch (hub.faction()) {
            case "union": id = "corps_field_checkpoint"; break;
            case "pmc": id = "company_security_checkpoint"; break;
            case "allies": id = "irregular_track_post"; break;
            default: return;
        }
        Definition site = DEFINITIONS.get(id);
        if (site == null) return;
        Random random = random(world, hub.flagPos[0], hub.flagPos[2], SITE_SALT ^ 0x40F756124C7AB573L);
        int choice = random.nextInt(100);
        if (choice >= ("allies".equals(hub.faction()) ? 45 : 78)) return;
        int distance = 56 + random.nextInt(18), drift = random.nextInt(25) - 12;
        int rotation = hub.approach;
        int half = site.width / 2;
        for (int side = -1; side <= 1; side++) {
            int dx = distance + (side == 0 ? 12 : 0), along = drift + side * 21;
            int x = hub.entrance[0] + (rotation == 1 ? dx : rotation == 3 ? -dx : along);
            int z = hub.entrance[2] + (rotation == 2 ? dx : rotation == 0 ? -dx : along);
            if (!anyChunk && !withinFootprint(chunkX, chunkZ, x, z, half)) continue;
            if (!proceduralAt(world, x, z) || !matches(site, biome(world, x, z), hub.faction())) continue;
            List<String> influence = regionalFactions(world, x, z);
            if (!influence.contains(hub.faction()) && MilitaryRelations.factionRelation(influence.get(0), hub.faction()) < 0) continue;
            if (place(world, x, z, rotation, site, hub.faction()) != null) return;
        }
    }

    private static boolean withinFootprint(int chunkX, int chunkZ, int x, int z, int half) {
        return chunkX >= ((x - half) >> 4) && chunkX <= ((x + half) >> 4)
                && chunkZ >= ((z - half) >> 4) && chunkZ <= ((z + half) >> 4);
    }

    static Random random(World world, int x, int z, long salt) {
        long mixed = world.getSeed() ^ salt ^ (x * 0x62A9D9ED799705F5L) ^ (z * 0xCB24D0A5C88C35B3L);
        mixed = (mixed ^ mixed >>> 30) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ mixed >>> 27) * 0x94D049BB133111EBL;
        return new Random(mixed ^ mixed >>> 31);
    }

    public static List<String> regionalFactions(World world, int x, int z) {
        return regionalFactions(world, x, z, biome(world, x, z));
    }


    private static List<String> regionalFactions(World world, int x, int z, String biome) {
        RegionOverride authored = regionOverride(world, x, z);
        int blocks = MilitaryConfig.regionSize * 16;
        int rx = Math.floorDiv(x, blocks), rz = Math.floorDiv(z, blocks);
        Random random = random(world, rx, rz, REGION_SALT);
        int centerX = rx * blocks + blocks / 2, centerZ = rz * blocks + blocks / 2;
        String regionalBiome = biome(world, centerX, centerZ);
        String primary = authored != null && authored.faction != null ? authored.faction
                : chooseFaction(world, centerX, centerZ, regionalBiome, random, null);
        String secondary = chooseFaction(world, x, z, biome, random, primary);
        return Arrays.asList(primary, secondary.isEmpty() && !MilitaryConfig.abandonedSites ? primary : secondary);
    }

    private static int regionalDepth(int x, int z) {
        int size = MilitaryConfig.regionSize * 16;
        int dx = Math.floorMod(x, size), dz = Math.floorMod(z, size);
        return Math.min(Math.min(dx, size - 1 - dx), Math.min(dz, size - 1 - dz)) * 100 / size;
    }

    static boolean proceduralAt(World world, int x, int z) {
        RegionOverride region = regionOverride(world, x, z);
        return region == null || region.procedural;
    }

    private static RegionOverride regionOverride(World world, int x, int z) {
        for (RegionOverride region : overrides.regions) {
            if (world.provider.dimensionId == region.dimension && x >= region.minX && x <= region.maxX
                    && z >= region.minZ && z <= region.maxZ) return region;
        }
        return null;
    }

    private static String chooseFaction(World world, int x, int z, String biome, Random random, String exclude) {
        List<String> candidates = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = MilitaryConfig.abandonedSites && !"".equals(exclude) ? 5 : 0;
        for (String id : MilitaryRelations.factions()) {
            if (id.equals(exclude) || !eligible(id, biome)) continue;
            MilitaryRelations.Faction faction = MilitaryRelations.definition(id);
            if (faction.initialReputation < faction.hostileBelow && protectedStartingArea(world, x, z)) continue;
            int weight = faction.regionalWeight * (Arrays.asList(faction.biomes).contains(biome) ? 3 : 1);
            if (weight == 0) continue;
            candidates.add(id);
            weights.add(weight);
            total += weight;
        }
        if (total == 0) return "";
        int draw = random.nextInt(total);
        for (int i = 0; i < candidates.size(); i++) {
            draw -= weights.get(i);
            if (draw < 0) return candidates.get(i);
        }
        return "";
    }

    static boolean protectedStartingArea(int x, int z) {
        long limit = MilitaryConfig.spawnBuffer;
        return (long)x * x + (long)z * z < limit * limit;
    }

    static boolean protectedStartingArea(World world, int x, int z) {
        return protectedStartingArea(x, z) || protectedStartingArea(x - world.getWorldInfo().getSpawnX(),
                z - world.getWorldInfo().getSpawnZ());
    }

    private static boolean eligible(String faction, String biome) {
        if (!MilitaryRelations.available(faction)) return false;
        List<Definition> candidates = POOLS.get(faction);
        if (candidates == null) return false;
        for (Definition site : candidates) if (matches(site, biome, faction)) return true;
        return false;
    }

    private static boolean matches(Definition site, String biome, String faction) {
        MilitaryRelations.Faction profile = MilitaryRelations.definition(faction);
        return site.weight > 0 && !site.borderOnly && (!site.fieldwork || MilitaryConfig.fieldworks)
                && (profile == null || profile.structures.length == 0 || Arrays.asList(profile.structures).contains(site.id))
                && Arrays.asList(site.biomes).contains(biome);
    }

    private static int siteWeight(Definition site, String biome, String faction, int depth) {
        if (!matches(site, biome, faction)) return 0;
        if (site.classification == Occupation.Classification.SETTLEMENT) return site.weight * (depth >= 22 ? 3 : 1);
        return site.weight * (depth < 12 && site.fieldwork ? 3 : 2);
    }

    private static Definition chooseSite(String faction, String biome, Random random, int depth) {
        List<Definition> options = POOLS.get(faction);
        if (options == null) return null;
        int weight = 0;
        for (Definition site : options) weight += siteWeight(site, biome, faction, depth);
        if (weight == 0) return null;
        int draw = random.nextInt(weight);
        for (Definition site : options) if ((draw -= siteWeight(site, biome, faction, depth)) < 0) return site;
        return null;
    }

    static String biome(World world, int x, int z) {
        BiomeGenBase at = world.getWorldChunkManager().getBiomeGenAt(x, z);
        if (BiomeDictionary.isBiomeOfType(at, Type.OCEAN)) return "ocean";
        if (BiomeDictionary.isBiomeOfType(at, Type.BEACH)) return "coast";
        if (BiomeDictionary.isBiomeOfType(at, Type.SNOWY)) return "snow";
        if (BiomeDictionary.isBiomeOfType(at, Type.SANDY) && BiomeDictionary.isBiomeOfType(at, Type.DRY)) return "desert";
        if (BiomeDictionary.isBiomeOfType(at, Type.SWAMP)) return "swamp";
        if (BiomeDictionary.isBiomeOfType(at, Type.MOUNTAIN) || BiomeDictionary.isBiomeOfType(at, Type.HILLS)) return "hills";
        if (BiomeDictionary.isBiomeOfType(at, Type.FOREST)) return "forest";
        return "plains";
    }

    /** Places an authored site on already loaded, populated and suitable terrain from the server thread. */
    public static Occupation placeAuthored(World world, String site, String faction, int x, int z, int rotation) {
        MilitarySystem.requireServerThread();
        Definition definition = DEFINITIONS.get(site);
        if (definition == null || world.isRemote || rotation < 0 || rotation > 3) return null;
        return place(world, x, z, rotation, definition, faction == null ? definition.faction : faction);
    }

    private static Occupation place(World world, int x, int z, int rotation, Definition site, String faction) {
        return place(world, x, z, rotation, site, faction, false);
    }

    private static Occupation place(World world, int x, int z, int rotation, Definition site, String faction, boolean tryOtherEntrances) {
        if (site == null || !faction.isEmpty() && !MilitaryRelations.available(faction)) return null;
        int half = site.width / 2;
        if (!populated(world, x - half, z - half, x + half, z + half)) return null;
        WorldSavedData_Flag data = WorldSavedData_Flag.get(world);
        if (!data.near(x, z, Math.max(36, half * 2)).isEmpty()) return null;
        MilitarySiteLayout layout = survey(world, x, z, rotation, site);
        for (int retry = 1; tryOtherEntrances && layout == null && retry < 4; retry++) {
            layout = survey(world, x, z, (rotation + retry) & 3, site);
        }
        return layout == null ? null : complete(layout, faction);
    }

    static boolean populated(World world, int x1, int z1, int x2, int z2) {
        IChunkProvider provider = world.getChunkProvider();
        for (int cx = x1 >> 4; cx <= x2 >> 4; cx++) for (int cz = z1 >> 4; cz <= z2 >> 4; cz++) {
            if (!provider.chunkExists(cx, cz) || !provider.provideChunk(cx, cz).isTerrainPopulated) return false;
        }
        return true;
    }

    static MilitarySiteLayout survey(World world, int x, int z, int rotation, Definition site) {
        int half = site.width / 2;
        MilitarySiteLayout layout = MilitarySiteLayout.survey(world, site, x, z, rotation, biome(world, x, z));
        if (layout == null || world.villageCollectionObj != null
                && world.villageCollectionObj.findNearestVillage(x, layout.floor, z, half + 8) != null) return null;
        return layout;
    }

    static Occupation complete(MilitarySiteLayout layout, String faction) {
        World world = layout.world;
        int x = layout.x, z = layout.z;
        Definition site = layout.site;
        Occupation post = Occupation.generated(x, layout.anchorY(), z, faction, site, world.getTotalWorldTime());
        RegionOverride region = regionOverride(world, x, z);
        if (region != null) post.configureRegion(region.pricePercent, region.garrisonPercent, region.equipment, region.quests);
        layout.build(post, random(world, x, z, SITE_SALT ^ REGION_SALT));
        WorldSavedData_Flag.get(world).register(post);
        MilitaryRoads.siteRegistered(world, post);
        if (MilitaryConfig.diagnostics) FMLLog.info("[GVC military] Generated %s (%s) at %s %s %s dim=%s rotation=%s",
                site.id, faction, x, layout.anchorY(), z, world.provider.dimensionId, layout.turn);
        return post;
    }

    static Definition borderSite(String faction) {
        List<Definition> sites = POOLS.get(faction);
        if (sites != null) for (Definition site : sites) if (site.borderOnly) return site;
        return DEFINITIONS.get("frontier_entrenchment");
    }

    static final class Definition {
        String id, faction = "", layout, palette = "adaptive";
        String hierarchy = "local";
        String guardPreset = "";
        Occupation.Classification classification;
        String[] biomes = {}, loot = {}, equipment = {}, quests = {};
        Trader[] traders = {};
        int width = 13, weight = 10, slope = 5, radius = 24, patrolRadius = 12, garrison = 2, patrol = 0, scavengers;
        int pricePercent = 100;
        boolean capture = true, beacon = false;
        boolean fieldwork, borderOnly;

        boolean valid() {
            if (!MilitaryRelations.validId(id) || faction == null || !faction.isEmpty() && !MilitaryRelations.isFaction(faction)
                    || classification == null || !MilitarySiteLayout.supports(layout, palette, width) || biomes == null || loot == null
                    || !MilitaryRelations.validId(hierarchy) || !validPool(equipment) || !validPool(quests)
                    || guardPreset == null || !guardPreset.isEmpty() && !MilitaryRelations.validId(guardPreset)
                    || pricePercent < 25 || pricePercent > 400 || traders == null || traders.length > 8
                    || weight < 0 || weight > 1000 || slope < 0 || slope > 12 || radius < 8 || radius > 128
                    || patrolRadius < 4 || patrolRadius > radius || garrison < 0 || garrison > 16 || patrol < 0 || patrol > 16
                    || scavengers < 0 || scavengers > 4 || scavengers > 0 && classification != Occupation.Classification.ABANDONED
                    || classification == Occupation.Classification.ABANDONED && !faction.isEmpty()) return false;
            java.util.Set<String> keys = new java.util.HashSet<>();
            for (Trader trader : traders) {
                if (trader == null || !MilitaryRelations.validId(trader.key) || !keys.add(trader.key)
                        || !MilitaryRelations.validId(trader.role) || trader.x < -width / 2 || trader.x > width / 2
                        || trader.z < -width / 2 || trader.z > width / 2 || trader.x == 0 && trader.z == 0) return false;
            }
            return true;
        }
    }

    private static boolean validPool(String[] entries) {
        if (entries == null || entries.length > 128) return false;
        for (String entry : entries) if (entry == null || entry.isEmpty() || entry.length() > 160) return false;
        return true;
    }

    static final class Trader {
        String key, role = "quartermaster";
        int x, z;
        boolean neutral;
    }

    private static final class Overrides {
        RegionOverride[] regions = {};
        FixedSite[] sites = {};
    }

    private static final class RegionOverride {
        int dimension, minX, minZ, maxX, maxZ;
        boolean procedural = true;
        String faction;
        int pricePercent = 100, garrisonPercent = 100;
        String[] equipment = {}, quests = {};
        boolean valid() {
            return minX <= maxX && minZ <= maxZ && (faction == null || faction.isEmpty() || MilitaryRelations.isFaction(faction))
                    && pricePercent >= 25 && pricePercent <= 400 && garrisonPercent >= 0 && garrisonPercent <= 200
                    && validPool(equipment) && validPool(quests);
        }
    }

    private static final class FixedSite {
        int dimension, x, z, rotation;
        String site, faction;
        boolean valid() {
            return Math.abs((long)x) < 29999968 && Math.abs((long)z) < 29999968 && rotation >= 0 && rotation <= 3
                    && DEFINITIONS.containsKey(site) && (faction == null || faction.isEmpty() || MilitaryRelations.isFaction(faction));
        }
    }
}
