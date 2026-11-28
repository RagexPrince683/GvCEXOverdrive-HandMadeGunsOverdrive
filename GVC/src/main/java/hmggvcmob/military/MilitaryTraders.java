package hmggvcmob.military;

import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

final class MilitaryTraders {
    private MilitaryTraders() {}

    static boolean canRecruit(String role) {
        return MilitaryCommerce.recruits(role);
    }

    static void update(World world, Occupation post, long now) {
        for (Occupation.TraderLocation location : post.traders()) {
            // Occupation records can outlive both the NPC and the chunk that contains its station.
            if (location.y > 0 && !world.getChunkProvider().chunkExists(location.x >> 4, location.z >> 4)) continue;
            if (post.traderFaction(location).isEmpty()) {
                if (location.entity != null) {
                    location.entity = null;
                    WorldSavedData_Flag.get(world).markDirty();
                }
                continue;
            }
            if (location.entity != null) continue;
            if (now >= location.nextTrader && now >= location.retry) {
                if (spawn(world, post, location)) continue;
                location.retry = now + 100;
            }
            if (location.waiting && location.y > 0 && MilitaryConfig.traderMarkers && location.nextTrader != Long.MAX_VALUE) {
                ensureMarker(world, post, location);
            }
        }
    }

    private static boolean spawn(World world, Occupation post, Occupation.TraderLocation location) {
        EntityMilitaryTrader trader = new EntityMilitaryTrader(world);
        if (location.y == 0) {
            ChunkCoordinates spot = MilitaryOccupation.findSpace(world, post.flagPos[0], post.flagPos[1], post.flagPos[2],
                    1, 6, true, trader);
            if (spot == null) return false;
            location.position(new int[]{spot.posX, spot.posY, spot.posZ});
        }
        trader.setLocationAndAngles(location.x + .5, location.y, location.z + .5, 0, 0);
        if (!world.isSideSolid(location.x, location.y - 1, location.z, ForgeDirection.UP)
                || !world.checkNoEntityCollision(trader.boundingBox)
                || !world.getCollidingBoundingBoxes(trader, trader.boundingBox).isEmpty()
                || world.isAnyLiquid(trader.boundingBox)) return false;
        trader.bind(post, location);
        location.entity = trader.getUniqueID();
        if (!world.spawnEntityInWorld(trader)) {
            location.entity = null;
            return false;
        }
        location.waiting = false;
        location.nextTrader = 0;
        WorldSavedData_Flag.get(world).markDirty();
        return true;
    }

    private static void ensureMarker(World world, Occupation post, Occupation.TraderLocation location) {
        AxisAlignedBB area = AxisAlignedBB.getBoundingBox(location.x, location.y, location.z,
                location.x + 1, location.y + 2, location.z + 1);
        for (Object candidate : world.getEntitiesWithinAABB(EntityTraderMarker.class, area)) {
            if (((EntityTraderMarker)candidate).atStation(post.id, location.key)) return;
        }
        EntityTraderMarker marker = new EntityTraderMarker(world);
        marker.bind(post, location);
        world.spawnEntityInWorld(marker);
    }
}
