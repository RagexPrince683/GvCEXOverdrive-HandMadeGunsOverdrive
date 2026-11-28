package hmggvcmob.military;

import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.village.Village;
import net.minecraft.village.VillageDoorInfo;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/** Bounded route search. A route is surveyed completely before it can modify terrain. */
final class MilitaryRoadPlan {
    enum Result { READY, WAITING, BLOCKED }
    static final int MARGIN = 16;
    private static final int MAX_VISITS = 10000;
    private static final int[] DX = {0, 1, 0, -1}, DZ = {-1, 0, 1, 0};
    private static final Column OBSTACLE = new Column(0, 0, -1, false);
    private final World world;
    private final Occupation a, b;
    private final List<Occupation> sites;
    private final IChunkProvider chunks;
    private final boolean trail;
    private final int x1, z1, x2, z2;
    private final Map<Long, Boolean> populations = new HashMap<>();
    private final Map<Long, Column> terrain = new HashMap<>();
    private final Map<Long, Column> paving = new LinkedHashMap<>();
    private List<Column> path;
    private boolean waiting;

    MilitaryRoadPlan(World world, Occupation a, Occupation b, WorldSavedData_Flag sites) {
        this.world = world;
        this.a = a;
        this.b = b;
        trail = MilitaryRoads.rough(a.faction());
        int ax = a.entrance[0], az = a.entrance[2], bx = b.entrance[0], bz = b.entrance[2];
        x1 = Math.min(ax, bx) - MARGIN;
        z1 = Math.min(az, bz) - MARGIN;
        x2 = Math.max(ax, bx) + MARGIN;
        z2 = Math.max(az, bz) + MARGIN;
        this.sites = sites.near((ax + bx) / 2, (az + bz) / 2, (Math.abs(ax - bx) + Math.abs(az - bz)) / 2 + 50);
        chunks = world.getChunkProvider();
    }

    Result survey() {
        List<Column> start = approach(a), goal = approach(b);
        if (start == null || goal == null) return waiting ? Result.WAITING : Result.BLOCKED;
        List<Column> route = search(start.get(start.size() - 1), goal.get(goal.size() - 1));
        if (route == null) return waiting ? Result.WAITING : Result.BLOCKED;
        path = new ArrayList<>(start);
        path.addAll(route.subList(1, route.size()));
        Collections.reverse(goal);
        path.addAll(goal.subList(1, goal.size()));
        for (int i = 1; i < path.size() - 1; i++) {
            Column center = path.get(i);
            paving.put(key(center.x, center.z), center);
            if (trail) continue;
            Column previous = path.get(i - 1), next = path.get(i + 1);
            addShoulders(center, center.x - previous.x, center.z - previous.z);
            if (next.x - center.x != center.x - previous.x || next.z - center.z != center.z - previous.z) {
                addShoulders(center, next.x - center.x, next.z - center.z);
            }
        }
        return Result.READY;
    }

    private List<Column> approach(Occupation site) {
        int[] gate = site.entrance;
        if (!populated(gate[0], gate[2])) return null;
        if (!clear(gate[0], gate[1], gate[2]) || !clear(gate[0], gate[1] + 1, gate[2])
                || !world.getBlock(gate[0], gate[1] - 1, gate[2]).getMaterial().isSolid()) return null;
        List<Column> result = new ArrayList<>(4);
        Column last = new Column(gate[0], gate[2], gate[1] - 1, false);
        result.add(last);
        for (int i = 1; i <= 3; i++) {
            Column next = column(gate[0] + DX[site.approach] * i, gate[2] + DZ[site.approach] * i);
            if (next == OBSTACLE || next.water) return null;
            int grade = Math.max(last.y - 1, Math.min(last.y + 1, next.y));
            if (grade != next.y) {
                if (grade < next.y - 1 || grade > next.y + 2 || !natural(next.x, next.y, next.z)
                        || !clear(next.x, grade + 2, next.z) || grade < next.y
                        && !natural(next.x, grade, next.z)) return null;
                next = new Column(next.x, next.z, grade, false, next.y);
            }
            result.add(next);
            last = next;
        }
        return result;
    }

    private List<Column> search(Column start, Column goal) {
        PriorityQueue<Step> open = new PriorityQueue<>(Comparator.comparingInt((Step step) -> step.estimate)
                .thenComparingInt(step -> step.remaining).thenComparingLong(step -> key(step.column.x, step.column.z)));
        Map<Long, Step> best = new HashMap<>();
        Step root = new Step(start, null, 0, goal);
        best.put(key(start.x, start.z), root);
        open.add(root);
        int visits = 0;
        while (!open.isEmpty() && visits++ < MAX_VISITS) {
            Step current = open.poll();
            Column at = current.column;
            if (best.get(key(at.x, at.z)) != current) continue;
            if (at.x == goal.x && at.z == goal.z) {
                List<Column> result = new ArrayList<>();
                for (Step step = current; step != null; step = step.previous) result.add(step.column);
                Collections.reverse(result);
                return result;
            }
            for (int direction = 0; direction < 4; direction++) {
                Column next = column(at.x + DX[direction], at.z + DZ[direction]);
                if (next == OBSTACLE || Math.abs(at.y - next.y) > 1) continue;
                if (at.water && next.water && (current.waterRun >= 9 || current.previous != null
                        && current.previous.column.water
                        && (at.x - current.previous.column.x != DX[direction] || at.z - current.previous.column.z != DZ[direction]))) continue;
                int cost = current.cost + 10 + (next.y == at.y ? 0 : 3) + (next.water ? 17 : 0);
                long key = key(next.x, next.z);
                Step existing = best.get(key);
                if (existing != null && existing.cost <= cost) continue;
                Step step = new Step(next, current, cost, goal);
                best.put(key, step);
                open.add(step);
            }
        }
        return null;
    }

    private void addShoulders(Column center, int dx, int dz) {
        for (int side : new int[]{-1, 1}) {
            Column edge = column(center.x + dz * side, center.z - dx * side);
            if (edge != OBSTACLE && edge.water == center.water && Math.abs(edge.y - center.y) <= 1) {
                paving.putIfAbsent(key(edge.x, edge.z), edge);
            }
        }
    }

    private Column column(int x, int z) {
        if (x < x1 || x > x2 || z < z1 || z > z2) return OBSTACLE;
        return terrain.computeIfAbsent(key(x, z), unused -> inspect(x, z));
    }

    private Column inspect(int x, int z) {
        if (!populated(x, z) || !MilitarySites.proceduralAt(world, x, z)) return OBSTACLE;
        for (Occupation site : sites) {
            int radius = site.footprint > 0 ? site.footprint : 8;
            if (Math.abs(site.flagPos[0] - x) <= radius && Math.abs(site.flagPos[2] - z) <= radius
                    && (site != a && site != b || !outsideGate(site, x, z))) return OBSTACLE;
        }
        int top = world.getHeightValue(x, z) - 1;
        if (top < 1 || top + 4 >= world.getActualHeight()) return OBSTACLE;
        if (nearVillageDoor(x, top, z)) return OBSTACLE;
        int surface = top;
        while (surface > 1 && replaceable(x, surface, z)) surface--;
        if (top - surface > 4 || !clear(x, surface + 1, z) || !clear(x, surface + 2, z)) return OBSTACLE;
        Block block = world.getBlock(x, surface, z);
        if (block.getMaterial() == Material.water) {
            for (int depth = 1; depth <= 5; depth++) {
                if (natural(x, surface - depth, z)) return new Column(x, z, surface + 1, true);
            }
            return OBSTACLE;
        }
        return natural(x, surface, z) || block == MilitaryMaterials.CONCRETE || block == Blocks.cobblestone
                ? new Column(x, z, surface, false) : OBSTACLE;
    }

    private boolean populated(int x, int z) {
        boolean present = populations.computeIfAbsent(key(x >> 4, z >> 4), unused ->
                chunks.chunkExists(x >> 4, z >> 4) && chunks.provideChunk(x >> 4, z >> 4).isTerrainPopulated);
        waiting |= !present;
        return present;
    }

    private static boolean outsideGate(Occupation site, int x, int z) {
        int dx = x - site.entrance[0], dz = z - site.entrance[2];
        return dx * DX[site.approach] + dz * DZ[site.approach] > 0 && Math.abs(dx) + Math.abs(dz) <= 6;
    }

    private static boolean nearGate(Occupation site, int x, int z) {
        return Math.abs(x - site.entrance[0]) + Math.abs(z - site.entrance[2]) < 12;
    }

    private boolean nearVillageDoor(int x, int y, int z) {
        if (world.villageCollectionObj == null) return false;
        Village village = world.villageCollectionObj.findNearestVillage(x, y, z, 3);
        if (village == null) return false;
        for (Object entry : village.getVillageDoorInfoList()) {
            VillageDoorInfo door = (VillageDoorInfo)entry;
            if (Math.abs(x - door.posX) <= 2 && Math.abs(z - door.posZ) <= 2) return true;
        }
        return false;
    }

    private boolean clear(int x, int y, int z) {
        return world.getBlock(x, y, z) == Blocks.air || replaceable(x, y, z);
    }

    private boolean replaceable(int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        Material material = block.getMaterial();
        return material == Material.plants || material == Material.vine || material == Material.leaves
                || block == Blocks.snow_layer || block == Blocks.tallgrass || block == Blocks.double_plant;
    }

    private boolean natural(int x, int y, int z) {
        return MilitarySiteLayout.natural(world.getBlock(x, y, z), world.getWorldChunkManager().getBiomeGenAt(x, z));
    }

    void build() {
        String faction = a.faction();
        for (Column c : paving.values()) {
            int variation = Math.floorMod(c.x * 41 + c.z * 13, 9);
            Block road = c.water ? Blocks.planks : trail ? (variation < 2 ? Blocks.gravel : Blocks.dirt)
                    : "pmc".equals(faction) ? (variation < 2 ? Blocks.cobblestone : MilitaryMaterials.CONCRETE)
                    : variation == 0 ? Blocks.cobblestone : Blocks.gravel;
            if (road == Blocks.gravel && !world.getBlock(c.x, c.y - 1, c.z).getMaterial().isSolid()) road = Blocks.cobblestone;
            for (int y = c.surface + 1; y < c.y; y++) world.setBlock(c.x, y, c.z, Blocks.cobblestone, 0, 2);
            if (c.surface > c.y) world.setBlock(c.x, c.surface, c.z, Blocks.air, 0, 2);
            world.setBlock(c.x, c.y, c.z, road, 0, 2);
            for (int y = c.y + 1; y <= c.y + 2; y++) {
                if (replaceable(c.x, y, c.z)) world.setBlock(c.x, y, c.z, Blocks.air, 0, 2);
            }
        }
        if (a.patrolPoint.length == 0) approachPatrol(a, 5);
        if (b.patrolPoint.length == 0) approachPatrol(b, path.size() - 6);
        if (path.size() >= 28) marker();
    }

    int length() { return path.size() - 1; }
    long crossingLength() { return path.stream().filter(column -> column.water).count(); }

    private void approachPatrol(Occupation site, int index) {
        if (index < 1 || index >= path.size() - 1) return;
        Column c = path.get(index);
        if (!c.water) site.patrolPoint = new int[]{c.x, c.y + 1, c.z};
    }

    private void marker() {
        int mid = path.size() / 2;
        for (int index = mid - 4; index <= mid + 4; index++) {
            Column at = path.get(index), next = path.get(index + 1);
            if (at.water) continue;
            int x = at.x + (next.z - at.z) * 2, z = at.z - (next.x - at.x) * 2;
            if (paving.containsKey(key(x, z))) continue;
            Column niche = column(x, z);
            if (niche == OBSTACLE || niche.water || Math.abs(niche.y - at.y) > 1
                    || nearGate(a, x, z) || nearGate(b, x, z)) continue;
            world.setBlock(x, niche.y + 1, z, trail ? Blocks.mossy_cobblestone : MilitaryMaterials.SANDBAGS, 0, 2);
            if (!trail && world.getBlock(x, niche.y + 2, z) == Blocks.air) {
                world.setBlock(x, niche.y + 2, z, Blocks.torch, 5, 2);
            }
            return;
        }
    }

    private static long key(int x, int z) { return ChunkCoordIntPair.chunkXZ2Int(x, z); }

    private static final class Column {
        final int x, z, y, surface;
        final boolean water;

        Column(int x, int z, int y, boolean water) {
            this(x, z, y, water, y);
        }

        Column(int x, int z, int y, boolean water, int surface) {
            this.x = x;
            this.z = z;
            this.y = y;
            this.water = water;
            this.surface = surface;
        }
    }

    private static final class Step {
        final Column column;
        final Step previous;
        final int cost, remaining, estimate, waterRun;

        Step(Column column, Step previous, int cost, Column goal) {
            this.column = column;
            this.previous = previous;
            this.cost = cost;
            remaining = Math.abs(column.x - goal.x) + Math.abs(column.z - goal.z);
            estimate = cost + remaining * 10;
            waterRun = column.water ? (previous == null ? 0 : previous.waterRun) + 1 : 0;
        }
    }
}
