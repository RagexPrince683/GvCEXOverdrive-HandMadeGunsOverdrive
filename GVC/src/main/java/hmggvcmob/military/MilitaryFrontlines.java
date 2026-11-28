package hmggvcmob.military;

import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import java.util.Random;

/** Sparse paired posts along shared procedural region edges. */
final class MilitaryFrontlines {
    private static final long BORDER_SALT = 0x7138D4A283538162L;
    private static final int SECTOR = 12, OFFSET = 16, SURVEY = 27;

    private MilitaryFrontlines() {}

    static void generate(World world, int chunkX, int chunkZ) {
        if (!MilitaryConfig.borderWorks || MilitaryConfig.borderPercent == 0) return;
        int size = MilitaryConfig.regionSize;
        int localX = Math.floorMod(chunkX, size), localZ = Math.floorMod(chunkZ, size);
        if (localX < 2 || localX >= size - 2) {
            int edge = chunkX - localX + (localX < 2 ? 0 : size);
            candidate(world, chunkX, chunkZ, edge, chunkZ, true);
        }
        if (localZ < 2 || localZ >= size - 2) {
            int edge = chunkZ - localZ + (localZ < 2 ? 0 : size);
            candidate(world, chunkX, chunkZ, edge, chunkX, false);
        }
    }

    private static void candidate(World world, int cx, int cz, int edge, int along, boolean eastWest) {
        int size = MilitaryConfig.regionSize;
        int regionStart = Math.floorDiv(along, size) * size;
        int sectorStart = regionStart + Math.floorDiv(along - regionStart, SECTOR) * SECTOR;
        int length = Math.min(SECTOR, regionStart + size - sectorStart);
        if (length < 6) return;
        Random random = MilitarySites.random(world, edge, sectorStart, eastWest ? BORDER_SALT : ~BORDER_SALT);
        if (random.nextInt(100) >= MilitaryConfig.borderPercent) return;
        int center = sectorStart + 2 + random.nextInt(length - 4);
        if (Math.abs(center - along) > 2) return;
        int x = eastWest ? edge * 16 : center * 16 + 8;
        int z = eastWest ? center * 16 + 8 : edge * 16;
        int dx = eastWest ? OFFSET : 0, dz = eastWest ? 0 : OFFSET;
        int rx = eastWest ? SURVEY : 12, rz = eastWest ? 12 : SURVEY;
        if (cx < ((x - rx) >> 4) || cx > ((x + rx) >> 4) || cz < ((z - rz) >> 4) || cz > ((z + rz) >> 4)
                || MilitarySites.protectedStartingArea(world, x, z)
                || !MilitarySites.populated(world, x - rx, z - rz, x + rx, z + rz)) return;
        if (!MilitarySites.proceduralAt(world, x, z) || !MilitarySites.proceduralAt(world, x - dx, z - dz)
                || !MilitarySites.proceduralAt(world, x + dx, z + dz)
                || !WorldSavedData_Flag.get(world).near(x, z, 44).isEmpty()) return;
        String first = MilitarySites.regionalFactions(world, x - dx, z - dz).get(0);
        String second = MilitarySites.regionalFactions(world, x + dx, z + dz).get(0);
        if (first.isEmpty() || second.isEmpty() || first.equals(second)
                || MilitaryRelations.factionRelation(first, second) >= 0 || MilitaryRelations.factionRelation(second, first) >= 0) return;
        MilitarySites.Definition firstSite = MilitarySites.borderSite(first), secondSite = MilitarySites.borderSite(second);
        if (firstSite == null || secondSite == null || firstSite.width > 21 || secondSite.width > 21) return;
        MilitarySiteLayout a = MilitarySites.survey(world, x - dx, z - dz, eastWest ? 1 : 2, firstSite);
        MilitarySiteLayout b = MilitarySites.survey(world, x + dx, z + dz, eastWest ? 3 : 0, secondSite);
        if (a == null || b == null || Math.abs(a.floor - b.floor) > 9
                || world.villageCollectionObj != null
                && world.villageCollectionObj.findNearestVillage(x, (a.floor + b.floor) / 2, z, 24) != null) return;
        MilitarySites.complete(a, first);
        MilitarySites.complete(b, second);
        scars(world, x, z, eastWest, random);
    }

    private static void scars(World world, int x, int z, boolean eastWest, Random random) {
        int[] craters = {-7, -3, 3, 8};
        for (int i = 0; i < craters.length; i++) {
            int across = random.nextInt(7) - 3;
            int along = craters[i] + random.nextInt(3) - 1;
            int px = x + (eastWest ? across : along), pz = z + (eastWest ? along : across);
            for (int ox = -3; ox <= 3; ox++) for (int oz = -3; oz <= 3; oz++) {
                int radial = ox * ox + oz * oz;
                if (radial <= 9) scar(world, px + ox, pz + oz, radial, random);
            }
        }
        for (int along = -10; along <= 10; along++) {
            if (Math.floorMod(along, 6) < 2) continue;
            int across = along < 0 ? -5 : 5;
            int px = x + (eastWest ? across : along), pz = z + (eastWest ? along : across);
            int y = scarGround(world, px, pz);
            if (y < 0 || world.getBlock(px, y + 1, pz) != Blocks.air) continue;
            int debris = Math.floorMod(along, 7);
            Block obstacle = debris == 0 ? Blocks.log : debris <= 3 ? MilitaryMaterials.BARBED_WIRE : MilitaryMaterials.BATTLE_RUBBLE;
            world.setBlock(px, y + 1, pz, obstacle, obstacle == Blocks.log ? 1 : 0, 2);
        }
    }

    private static void scar(World world, int x, int z, int distance, Random random) {
        int top = scarGround(world, x, z);
        if (top < 2) return;
        int depth = Math.min(top - 1, distance < 3 ? 2 : distance < 7 ? 1 : 0);
        for (int y = top + 1; y > top - depth; y--) {
            Block block = world.getBlock(x, y, z);
            if (!MilitarySiteLayout.vegetation(block) && !MilitarySiteLayout.natural(block,
                    world.getWorldChunkManager().getBiomeGenAt(x, z))) return;
        }
        Block ground = world.getBlock(x, top - depth, z);
        if (!MilitarySiteLayout.natural(ground, world.getWorldChunkManager().getBiomeGenAt(x, z))) return;
        for (int y = top + 1; y > top - depth; y--) world.setBlock(x, y, z, Blocks.air, 0, 2);
        world.setBlock(x, top - depth, z, random.nextInt(3) == 0 ? Blocks.gravel : Blocks.dirt, 0, 2);
        if (depth == 0 && random.nextInt(6) == 0) world.setBlock(x, top + 1, z, MilitaryMaterials.BATTLE_RUBBLE, 0, 2);
    }

    private static int scarGround(World world, int x, int z) {
        if (!MilitarySites.proceduralAt(world, x, z)) return -1;
        int top = MilitarySiteLayout.surfaceTop(world, x, z);
        int y = top;
        while (y > 1 && MilitarySiteLayout.vegetation(world.getBlock(x, y, z))) y--;
        if (top - y > 2 || !MilitarySiteLayout.natural(world.getBlock(x, y, z),
                world.getWorldChunkManager().getBiomeGenAt(x, z))) return -1;
        return y;
    }
}
