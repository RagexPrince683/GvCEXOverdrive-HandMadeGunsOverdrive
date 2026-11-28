
package hmggvcmob.world;

import hmggvcmob.GVCMobPlus;
import hmggvcmob.camp.CampObj;
import hmggvcmob.camp.CampObjAndPos;
import hmggvcmob.military.Occupation;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorldSavedData_Flag extends WorldSavedData {
    private static final String NAME = GVCMobPlus.modid + "_FlagData";
    private final Map<ChunkCoordinates, Occupation> positions = new HashMap<>();
    private final Map<UUID, Occupation> settlements = new HashMap<>();
    private final Map<ChunkCoordIntPair, List<Occupation>> territory = new HashMap<>();

    /** Cached compatibility view of the post nearest to each chunk center. */
    @Deprecated public final HashMap<ChunkCoordIntPair, CampObjAndPos> campObjHashMap = new HashMap<>();

    public WorldSavedData_Flag(String name) {
        super(name);
    }

    public WorldSavedData_Flag() {
        this(NAME);
    }

    public static WorldSavedData_Flag get(World world) {
        WorldSavedData_Flag data = (WorldSavedData_Flag) world.perWorldStorage.loadData(WorldSavedData_Flag.class, NAME);
        if (data == null) {
            data = new WorldSavedData_Flag(NAME);
            world.perWorldStorage.setData(NAME, data);
        }
        return data;
    }

    public Occupation atBeacon(int x, int y, int z) {
        return positions.get(new ChunkCoordinates(x, y, z));
    }

    public Occupation byId(UUID id) {
        return settlements.get(id);
    }

    public Occupation getOrCreate(int x, int y, int z, CampObj profile, String type) {
        Occupation post = atBeacon(x, y, z);
        if (post != null) return post;
        post = new Occupation(x, y, z, profile, type);
        add(post);
        markDirty();
        return post;
    }

    public void remove(int x, int y, int z) {
        Occupation post = positions.remove(new ChunkCoordinates(x, y, z));
        if (post == null) return;
        settlements.remove(post.id);
        updateCoverage(post, false);
        markDirty();
    }

    public boolean register(Occupation post) {
        int[] pos = post.flagPos;
        if (positions.containsKey(new ChunkCoordinates(pos[0], pos[1], pos[2])) || settlements.containsKey(post.id)) return false;
        add(post);
        markDirty();
        return true;
    }

    public List<Occupation> near(int x, int z, int radius) {
        java.util.Set<Occupation> found = new java.util.LinkedHashSet<>();
        for (int cx = (x - radius) >> 4; cx <= (x + radius) >> 4; cx++) {
            for (int cz = (z - radius) >> 4; cz <= (z + radius) >> 4; cz++) {
                List<Occupation> posts = territory.get(new ChunkCoordIntPair(cx, cz));
                if (posts != null) found.addAll(posts);
            }
        }
        ArrayList<Occupation> results = new ArrayList<>(found);
        long limit = (long)radius * radius;
        results.removeIf(post -> distance(post, x, z) > limit);
        results.sort(java.util.Comparator.comparingLong((Occupation post) -> distance(post, x, z))
                .thenComparingInt(post -> post.flagPos[0]).thenComparingInt(post -> post.flagPos[2]));
        return results;
    }

    private static long distance(Occupation post, int x, int z) {
        long dx = (long)post.flagPos[0] - x, dz = (long)post.flagPos[2] - z;
        return dx * dx + dz * dz;
    }

    public Occupation atTerritory(double x, double z, boolean villageOnly) {
        return atTerritory(x, z, villageOnly, false);
    }

    public Occupation atOccupiedTerritory(double x, double z, boolean villageOnly) {
        return atTerritory(x, z, villageOnly, true);
    }

    private Occupation atTerritory(double x, double z, boolean villageOnly, boolean occupiedOnly) {
        ChunkCoordIntPair chunk = new ChunkCoordIntPair(MathHelper.floor_double(x) >> 4, MathHelper.floor_double(z) >> 4);
        return nearest(territory.get(chunk), x, z, villageOnly, occupiedOnly);
    }

    private static Occupation nearest(List<Occupation> posts, double x, double z, boolean villageOnly, boolean occupiedOnly) {
        if (posts == null) return null;
        Occupation best = null;
        double distance = Double.POSITIVE_INFINITY;
        for (Occupation post : posts) {
            if (villageOnly && !post.isVillage() || occupiedOnly && !post.occupied() || !post.contains(x, z)) continue;
            double dx = x - post.flagPos[0] - .5;
            double dz = z - post.flagPos[2] - .5;
            double score = dx * dx + dz * dz;
            if (score < distance || score == distance && post.id.compareTo(best.id) < 0) {
                best = post;
                distance = score;
            }
        }
        return best;
    }

    private void add(Occupation post) {
        positions.put(new ChunkCoordinates(post.flagPos[0], post.flagPos[1], post.flagPos[2]), post);
        settlements.put(post.id, post);
        updateCoverage(post, true);
    }

    private void updateCoverage(Occupation post, boolean add) {
        int x = post.flagPos[0];
        int z = post.flagPos[2];
        for (int cx = (x - post.radius) >> 4; cx <= (x + post.radius) >> 4; cx++) {
            for (int cz = (z - post.radius) >> 4; cz <= (z + post.radius) >> 4; cz++) {
                ChunkCoordIntPair chunk = new ChunkCoordIntPair(cx, cz);
                List<Occupation> covered = territory.computeIfAbsent(chunk, ignored -> new ArrayList<>());
                if (add) covered.add(post);
                else covered.remove(post);
                if (covered.isEmpty()) territory.remove(chunk);
                Occupation closest = nearest(covered, cx * 16 + 8, cz * 16 + 8, false, false);
                if (closest == null) campObjHashMap.remove(chunk);
                else campObjHashMap.put(chunk, closest);
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        positions.clear();
        settlements.clear();
        territory.clear();
        campObjHashMap.clear();
        if (tag.hasKey("Settlements")) {
            NBTTagList list = tag.getTagList("Settlements", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                Occupation post = Occupation.read(list.getCompoundTagAt(i));
                if (post != null && byId(post.id) == null && atBeacon(post.flagPos[0], post.flagPos[1], post.flagPos[2]) == null) add(post);
            }
            return;
        }
        // The legacy save repeats the same flag once for each claimed chunk.
        for (int i = 0; i < tag.getInteger("CampObjNum"); i++) {
            CampObj camp = GVCMobPlus.campsHash.get(tag.getString("CampObj" + i));
            if (camp == null) continue;
            getOrCreate(tag.getInteger("CampObjBlockX" + i), tag.getInteger("CampObjBlockY" + i),
                    tag.getInteger("CampObjBlockZ" + i), camp, camp.isRaidType ? "raid" : "base");
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList list = new NBTTagList();
        for (Occupation post : settlements.values()) list.appendTag(post.write());
        tag.setTag("Settlements", list);
    }
}
