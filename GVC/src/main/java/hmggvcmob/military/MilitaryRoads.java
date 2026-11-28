package hmggvcmob.military;

import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Saved local links and chunk-driven scheduling. Terrain is read and written on the world thread. */
public final class MilitaryRoads extends WorldSavedData {
    private static final String NAME = "gvco_faction_roads";
    private static final int MAX_OFFERS = 8;
    private static final byte PENDING = 0, BUILT = 1, REJECTED = 2;
    private final Map<UUID, List<Link>> bySite = new HashMap<>();
    private final Map<Long, Set<Link>> waiting = new HashMap<>();
    private final Set<Link> ready = new LinkedHashSet<>();

    public MilitaryRoads(String name) { super(name); }

    private static MilitaryRoads data(World world) {
        MilitaryRoads result = (MilitaryRoads)world.perWorldStorage.loadData(MilitaryRoads.class, NAME);
        if (result == null) {
            result = new MilitaryRoads(NAME);
            world.perWorldStorage.setData(NAME, result);
        }
        return result;
    }

    private static boolean enabled(World world) {
        return MilitaryConfig.factionRoads && !world.isRemote && world.provider.dimensionId == 0
                && world.perWorldStorage != null;
    }

    static void siteRegistered(World world, Occupation post) {
        if (!enabled(world) || !usable(post)) return;
        List<Occupation> nearby = new ArrayList<>(WorldSavedData_Flag.get(world).near(
                post.flagPos[0], post.flagPos[2], MilitaryConfig.roadLength + post.footprint + 40));
        nearby.removeIf(other -> other == post || !usable(other) || !other.faction().equals(post.faction()));
        nearby.sort(Comparator.comparingInt((Occupation other) -> fortress(other) ? 0 : 1).thenComparingInt(other ->
                distance(post.entrance[0], post.entrance[2], other.entrance[0], other.entrance[2])));
        MilitaryRoads roads = data(world);
        for (Occupation other : nearby) roads.offer(post, other);
    }

    private void offer(Occupation a, Occupation b) {
        int gates = gates(a, b);
        int[] first = entrance(a, (gates & 1) != 0), second = entrance(b, (gates & 2) != 0);
        int length = distance(first[0], first[2], second[0], second[2]);
        int limit = rough(a.faction()) ? Math.min(248, MilitaryConfig.roadLength) : MilitaryConfig.roadLength;
        if (length < 12 || length > limit) return;
        List<Link> aLinks = bySite.get(a.id), bLinks = bySite.get(b.id);
        if (aLinks != null && (aLinks.size() >= MAX_OFFERS || aLinks.stream().anyMatch(link -> link.has(b.id)))
                || bLinks != null && bLinks.size() >= MAX_OFFERS) return;
        Link link = new Link(a.id, b.id);
        link.gates = gates;
        link.bounds(first, second);
        add(link);
        ready.add(link);
        markDirty();
    }

    private static boolean usable(Occupation post) {
        return post != null && post.entrance.length == 3 && post.occupied() && !post.faction().isEmpty()
                && post.classification() == Occupation.Classification.SETTLEMENT;
    }

    private static boolean fortress(Occupation post) {
        return "command".equals(post.hierarchy) || "corps_district_hq".equals(post.type);
    }

    static int[] entrance(Occupation site, boolean rear) {
        return rear && site.rearEntrance.length == 3 ? site.rearEntrance : site.entrance;
    }

    static int gates(Occupation a, Occupation b) {
        int mask = 0;
        int best = Integer.MAX_VALUE;
        for (int i = 0; i < (a.rearEntrance.length == 3 ? 2 : 1); i++) {
            for (int j = 0; j < (b.rearEntrance.length == 3 ? 2 : 1); j++) {
                int[] start = entrance(a, i != 0), end = entrance(b, j != 0);
                int score = distance(start[0], start[2], end[0], end[2]);
                score += approachPenalty((a.approach + 2 * i) & 3, start, end);
                score += approachPenalty((b.approach + 2 * j) & 3, end, start);
                if (score < best) {
                    best = score;
                    mask = i | (j << 1);
                }
            }
        }
        return mask;
    }

    private static int approachPenalty(int direction, int[] origin, int[] destination) {
        int dx = destination[0] - origin[0], dz = destination[2] - origin[2];
        int forward = direction == 0 ? -dz : direction == 1 ? dx : direction == 2 ? dz : -dx;
        return forward >= 0 ? 0 : 20;
    }

    static boolean rough(String faction) { return "allies".equals(faction) || "cinder".equals(faction); }

    private static int distance(int x, int z, int toX, int toZ) {
        return (int)Math.ceil(Math.hypot(toX - x, toZ - z));
    }

    private void add(Link link) {
        bySite.computeIfAbsent(link.a, unused -> new ArrayList<>()).add(link);
        bySite.computeIfAbsent(link.b, unused -> new ArrayList<>()).add(link);
        if (link.state != PENDING) return;
        for (int cx = link.x1; cx <= link.x2; cx++) for (int cz = link.z1; cz <= link.z2; cz++) {
            waiting.computeIfAbsent(ChunkCoordIntPair.chunkXZ2Int(cx, cz), unused -> new LinkedHashSet<>()).add(link);
        }
    }

    static void chunkReady(World world, int x, int z) {
        if (!enabled(world)) return;
        MilitaryRoads roads = data(world);
        Set<Link> links = roads.waiting.get(ChunkCoordIntPair.chunkXZ2Int(x, z));
        if (links != null) roads.ready.addAll(links);
    }

    static void tick(World world) {
        if (!enabled(world) || world.getTotalWorldTime() % 8 != 0) return;
        MilitaryRoads roads = data(world);
        if (roads.ready.isEmpty()) return;
        Link link = roads.ready.iterator().next();
        roads.ready.remove(link);
        roads.attempt(world, link);
    }

    private void attempt(World world, Link link) {
        WorldSavedData_Flag sites = WorldSavedData_Flag.get(world);
        Occupation a = sites.byId(link.a), b = sites.byId(link.b);
        if (!usable(a) || !usable(b) || !a.faction().equals(b.faction()) || atCapacity(a) || atCapacity(b)) {
            finish(link, REJECTED);
            return;
        }
        MilitaryRoadPlan plan = new MilitaryRoadPlan(world, a, b, sites, link.gates);
        MilitaryRoadPlan.Result result = plan.survey();
        if (result == MilitaryRoadPlan.Result.WAITING) return;
        if (result == MilitaryRoadPlan.Result.BLOCKED) {
            finish(link, REJECTED);
            return;
        }
        plan.build();
        sites.markDirty();
        finish(link, BUILT);
        if (MilitaryConfig.diagnostics) cpw.mods.fml.common.FMLLog.info(
                "[GVC military] Road %s to %s (%s) from %s %s to %s %s, length=%s crossing=%s",
                a.type, b.type, a.faction(), plan.gate(a)[0], plan.gate(a)[2], plan.gate(b)[0], plan.gate(b)[2],
                plan.length(), plan.crossingLength());
    }

    private boolean atCapacity(Occupation site) {
        int limit = fortress(site) ? MilitaryConfig.roadsPerHub : 2;
        return bySite.get(site.id).stream().filter(link -> link.state == BUILT).count() >= limit;
    }

    private void finish(Link link, byte state) {
        link.state = state;
        for (int cx = link.x1; cx <= link.x2; cx++) for (int cz = link.z1; cz <= link.z2; cz++) {
            long key = ChunkCoordIntPair.chunkXZ2Int(cx, cz);
            Set<Link> links = waiting.get(key);
            if (links == null) continue;
            links.remove(link);
            if (links.isEmpty()) waiting.remove(key);
        }
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        bySite.clear();
        waiting.clear();
        ready.clear();
        NBTTagList entries = tag.getTagList("Links", 10);
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            UUID a = new UUID(entry.getLong("AHigh"), entry.getLong("ALow"));
            UUID b = new UUID(entry.getLong("BHigh"), entry.getLong("BLow"));
            int[] bounds = entry.getIntArray("Bounds");
            byte state = entry.getByte("State");
            if (a.equals(b) || bounds.length != 4 || (long)bounds[2] - bounds[0] > 28
                    || (long)bounds[3] - bounds[1] > 28 || bounds[2] < bounds[0] || bounds[3] < bounds[1]
                    || state < PENDING || state > REJECTED) continue;
            Link link = new Link(a, b);
            link.x1 = bounds[0];
            link.z1 = bounds[1];
            link.x2 = bounds[2];
            link.z2 = bounds[3];
            link.state = state;
            link.gates = entry.getByte("Gates") & 3;
            add(link);
            if (state == PENDING) ready.add(link);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagList entries = new NBTTagList();
        for (Map.Entry<UUID, List<Link>> site : bySite.entrySet()) for (Link link : site.getValue()) {
            if (!link.a.equals(site.getKey())) continue;
            NBTTagCompound entry = new NBTTagCompound();
            entry.setLong("AHigh", link.a.getMostSignificantBits());
            entry.setLong("ALow", link.a.getLeastSignificantBits());
            entry.setLong("BHigh", link.b.getMostSignificantBits());
            entry.setLong("BLow", link.b.getLeastSignificantBits());
            entry.setIntArray("Bounds", new int[]{link.x1, link.z1, link.x2, link.z2});
            entry.setByte("State", link.state);
            entry.setByte("Gates", (byte)link.gates);
            entries.appendTag(entry);
        }
        tag.setTag("Links", entries);
    }

    private static final class Link {
        final UUID a, b;
        int x1, z1, x2, z2;
        int gates;
        byte state;

        Link(UUID a, UUID b) { this.a = a; this.b = b; }
        boolean has(UUID id) { return a.equals(id) || b.equals(id); }

        void bounds(int[] first, int[] second) {
            int margin = MilitaryRoadPlan.MARGIN + 2;
            x1 = (Math.min(first[0], second[0]) - margin) >> 4;
            x2 = (Math.max(first[0], second[0]) + margin) >> 4;
            z1 = (Math.min(first[2], second[2]) - margin) >> 4;
            z2 = (Math.max(first[2], second[2]) + margin) >> 4;
        }
    }
}
