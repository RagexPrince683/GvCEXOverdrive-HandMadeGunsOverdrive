package hmggvcmob.entity.util;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.MathHelper;

import javax.vecmath.Vector3d;

public class MoveToPositionMng {
    private static final int SEARCH_RANGE = 20;
    private static final int SEARCH_MARGIN = SEARCH_RANGE + 8;

    private final EntityLiving entity;
    private final Vector3d destination = new Vector3d();
    private final Vector3d plannedDestination = new Vector3d();
    private final Vector3d progressPosition = new Vector3d();
    private double speed;
    private PathEntity ownedPath;
    private int nextSearch;
    private int nextProgressCheck;
    private int failures;
    private boolean planned;
    private long searches;
    private int ignoredTargetId;
    private int ignoreTargetUntil;


    public MoveToPositionMng(EntityLiving entity) {
        this.entity = entity;
        nextSearch = entity.getRNG().nextInt(12);
    }

    public Vector3d getMoveToPos() {
        return destination;
    }

    public void setMovingSpeed(double speed) {
        if (speed == 0) stop();
        else this.speed = speed;
    }

    public double getMovingSpeed() {
        return speed;
    }

    public void moveTo(double x, double y, double z, double speed) {
        destination.set(x, y, z);
        this.speed = speed;
    }

    public void stop() {
        clearOwnedPath();
        speed = 0;
        planned = false;
        failures = 0;
    }

    public void disengage(EntityLivingBase target) {
        ignoredTargetId = target.getEntityId();
        ignoreTargetUntil = entity.ticksExisted + 140;
    }

    public boolean isDisengagedFrom(EntityLivingBase target) {
        return entity.ticksExisted < ignoreTargetUntil && target.getEntityId() == ignoredTargetId;
    }

    public boolean isBlocked() {
        return failures >= 3;
    }

    public long getPathSearchCount() {
        return searches;
    }

    public boolean hasArrived() {
        double dx = destination.x - entity.posX;
        double dz = destination.z - entity.posZ;
        return dx * dx + dz * dz < 0.81 && Math.abs(destination.y - entity.boundingBox.minY) < 1.1;
    }

    public int getFailures() {
        return failures;
    }

    public String getPathStatus() {
        if (speed == 0) return "idle";
        if (hasArrived()) return "arrived";
        if (ownedPath != null && entity.getNavigator().getPath() == ownedPath && !entity.getNavigator().noPath()) return "following";
        return failures > 0 ? "retry" : "pending";
    }

    private void clearOwnedPath() {
        if (ownedPath != null && entity.getNavigator().getPath() == ownedPath) {
            entity.getNavigator().clearPathEntity();
        }
        ownedPath = null;
    }

    public void update() {
        if (speed == 0 || entity.worldObj.isRemote || entity.ridingEntity != null || !entity.isEntityAlive()) return;
        PathNavigate navigator = entity.getNavigator();
        int tick = entity.ticksExisted;
        double dx = destination.x - entity.posX;
        double dy = destination.y - entity.boundingBox.minY;
        double dz = destination.z - entity.posZ;
        if (hasArrived()) {
            clearOwnedPath();
            failures = 0;
            return;
        }

        double goalX = destination.x - plannedDestination.x;
        double goalY = destination.y - plannedDestination.y;
        double goalZ = destination.z - plannedDestination.z;
        boolean changed = !planned || goalX * goalX + goalY * goalY + goalZ * goalZ >= 9;
        boolean following = ownedPath != null && navigator.getPath() == ownedPath && !navigator.noPath();
        if (following && tick >= nextProgressCheck) {
            double moved = entity.getDistanceSq(progressPosition.x, progressPosition.y, progressPosition.z);
            if (moved < 0.36) {
                clearOwnedPath();
                following = false;
                failures++;
            } else {
                progressPosition.set(entity.posX, entity.posY, entity.posZ);
                nextProgressCheck = tick + 32;
                failures = 0;
            }
        }
        if (following) {
            navigator.setSpeed(speed);
            if (!changed) return;
        }
        if (tick < nextSearch || (!entity.onGround && !entity.isInWater())) return;

        if (changed) failures = 0;
        planned = true;
        plannedDestination.set(destination);
        int x = MathHelper.floor_double(entity.posX);
        int y = MathHelper.floor_double(entity.boundingBox.minY);
        int z = MathHelper.floor_double(entity.posZ);
        // Vanilla creates a chunk cache around the search origin. Do not load terrain for an AI route.
        if (!entity.worldObj.checkChunksExist(x - SEARCH_MARGIN, y, z - SEARCH_MARGIN,
                x + SEARCH_MARGIN, y, z + SEARCH_MARGIN)) {
            nextSearch = tick + 45 + entity.getRNG().nextInt(15);
            return;
        }

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double scale = Math.min(1, 15 / distance);
        double targetX = entity.posX + dx * scale;
        double targetZ = entity.posZ + dz * scale;
        // After a partial or blocked route, try each side on separate ticks.
        if (failures > 0 && failures % 3 != 0) {
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            if (horizontal > 0.1) {
                double offset = (failures % 3 == 1 ? 3 : -3);
                targetX -= dz * offset / horizontal;
                targetZ += dx * offset / horizontal;
            }
        }
        int targetY = MathHelper.floor_double(entity.boundingBox.minY + dy * scale);
        searches++;
        PathEntity path = entity.worldObj.getEntityPathToXYZ(entity, MathHelper.floor_double(targetX),
                targetY, MathHelper.floor_double(targetZ), SEARCH_RANGE, true,
                navigator.getCanBreakDoors(), navigator.getAvoidsWater(), entity.isInWater());
        PathPoint end = path == null ? null : path.getFinalPathPoint();
        if (end != null && entity.getDistanceSq(end.xCoord + 0.5, end.yCoord, end.zCoord + 0.5) > 1.44
                && navigator.setPath(path, speed)) {
            ownedPath = navigator.getPath();
            progressPosition.set(entity.posX, entity.posY, entity.posZ);
            nextProgressCheck = tick + 32;
            nextSearch = tick + 12 + entity.getRNG().nextInt(10);
        } else {
            failures++;
            nextSearch = tick + Math.min(85, 9 + failures * 9) + entity.getRNG().nextInt(10);
        }
    }
}
