package handmadeguns.Util;

import handmadeguns.compat.HMGPointOfAimBridge;
import handmadeguns.entity.bullets.HMGEntityBulletBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/** Impact geometry is local to this damage attempt, never received in a packet. */
public final class HMGImpactDamageSource extends EntityDamageSourceIndirect {
    private final Entity victim;
    private final boolean headshot;

    public HMGImpactDamageSource(String type, HMGEntityBulletBase bullet, MovingObjectPosition hit) {
        super(type, bullet, bullet.getThrower());
        setProjectile();
        victim = hit.entityHit;
        headshot = !bullet.worldObj.isRemote && isHeadImpact(victim, hit.hitVec);
    }

    public boolean isHeadshot(EntityLivingBase target) {
        return target == victim && !target.worldObj.isRemote && headshot;
    }

    public static boolean isHeadImpact(Entity target, Vec3 impact) {
        if (!(target instanceof EntityLivingBase) || impact == null || target.boundingBox == null) return false;
        AxisAlignedBB bounds = target.boundingBox;
        double height = bounds.maxY - bounds.minY;
        double width = Math.min(bounds.maxX - bounds.minX, bounds.maxZ - bounds.minZ);
        if (height <= 0.0D || width <= 0.0D) return false;

        // Use the accepted pose eye, including Combatives' physical scale/posture.
        double eyeY = bounds.minY + target.getEyeHeight();
        if (target instanceof EntityPlayer) {
            HMGPointOfAimBridge.AimRay ray = HMGPointOfAimBridge.getAuthoritativeRay((EntityPlayer) target);
            if (ray != null) eyeY = ray.origin.yCoord;
        }
        eyeY = Math.max(bounds.minY, Math.min(bounds.maxY, eyeY));
        double headBottom = Math.max(bounds.minY, eyeY - Math.min(width * 0.2D, height * 0.2D));
        // HMG intersects a 0.1-expanded entity AABB. Test that exact impact envelope,
        // but never expand the head downward into shoulders/the upper torso.
        double margin = (double) 0.1F + 1.0E-7D;
        return impact.xCoord >= bounds.minX - margin && impact.xCoord <= bounds.maxX + margin
                && impact.zCoord >= bounds.minZ - margin && impact.zCoord <= bounds.maxZ + margin
                && impact.yCoord >= headBottom && impact.yCoord <= bounds.maxY + margin;
    }
}
