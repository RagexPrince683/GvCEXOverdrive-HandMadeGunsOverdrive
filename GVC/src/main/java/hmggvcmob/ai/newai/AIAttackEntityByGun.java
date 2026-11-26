package hmggvcmob.ai.newai;

import handmadeguns.entity.IFF;
import handmadeguns.entity.PlacedGunEntity;
import handmadeguns.items.WeaponClass;
import handmadeguns.items.guns.HMGItem_Unified_Guns;
import hmggvcmob.entity.IGVCmob;
import hmggvcmob.entity.guerrilla.GVCEntityGuerrillaMG;
import hmggvcmob.entity.guerrilla.GVCEntityGuerrillaSG;
import hmggvcmob.entity.guerrilla.GVCEntityGuerrillaSP;
import hmggvcmob.entity.friend.GVCEntityPMCMG;
import hmggvcmob.entity.friend.GVCEntityPMCSP;
import hmggvcmob.entity.friend.GVCEntitySoldierMG;
import hmggvcmob.entity.friend.GVCEntitySoldierSP;
import hmggvcmob.entity.util.MoveToPositionMng;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import javax.vecmath.Vector3d;

import static hmggvcmob.GVCMobPlus.cfg_infantryAimError;
import static hmggvcmob.GVCMobPlus.cfg_infantryReactionTicks;

public class AIAttackEntityByGun extends AIAttackToEntity {
    private enum Role {
        RIFLE(8, 34, 12, 15, 1.0),
        SUPPORT(11, 42, 26, 24, 1.2),
        MARKSMAN(13, 58, 1, 28, 0.6),
        CLOSE(3, 17, 9, 12, 1.4);

        final int minimum, maximum, burst, pause;
        final double error;

        Role(int minimum, int maximum, int burst, int pause, double error) {
            this.minimum = minimum;
            this.maximum = maximum;
            this.burst = burst;
            this.pause = pause;
            this.error = error;
        }
    }

    private final MoveToPositionMng movement;
    private ItemStack weapon;
    private Role role;
    private boolean visible;
    private int nextMovement;
    private int reaction;
    private int nextAimSample;
    private int shotPause;
    private int burstRemaining;
    private int blockedTicks;
    private double aimOffsetX, aimOffsetY, aimOffsetZ;
    private final Vector3d lastSeenFeet = new Vector3d();
    private boolean hasLastSeen;
    private boolean firingLaneBlocked;

    public AIAttackEntityByGun(EntityLiving shooter, AIAttackManager manager) {
        super(shooter, manager);
        movement = ((IGVCmob)shooter).getMoveToPositionMng();
        rnd = shooter.getRNG();
    }

    private ItemStack getWeapon() {
        return shooter.ridingEntity instanceof PlacedGunEntity
                ? ((PlacedGunEntity)shooter.ridingEntity).gunStack : shooter.getHeldItem();
    }

    @Override
    public boolean shouldExecute() {
        ItemStack held = getWeapon();
        return held != null && held.getItem() instanceof HMGItem_Unified_Guns && super.shouldExecute();
    }

    @Override
    public boolean continueExecuting() {
        return target == shooter.getAttackTarget() && target != null && target.isEntityAlive()
                && weapon == getWeapon() && !((IFF)shooter).is_this_entity_friend(target);
    }

    @Override
    public void startExecuting() {
        weapon = getWeapon();
        HMGItem_Unified_Guns gun = (HMGItem_Unified_Guns)weapon.getItem();
        role = roleFor(gun);
        movement.stop();
        shooter.getNavigator().clearPathEntity();
        reaction = cfg_infantryReactionTicks + rnd.nextInt(8);
        nextAimSample = 0;
        nextMovement = shooter.ticksExisted;
        shotPause = 0;
        blockedTicks = 0;
        hasLastSeen = false;
        firingLaneBlocked = false;
        gun.checkTags(weapon);
        NBTTagCompound tag = weapon.getTagCompound();
        int mode = tag.getInteger("HMGMode");
        int selected = -1;
        int preferred = role == Role.MARKSMAN ? Integer.MAX_VALUE : 0;
        for (int i = 0; i < gun.gunInfo.burstcount.size(); i++) {
            int count = gun.getburstCount(i);
            if (count == -1 && role != Role.MARKSMAN) {
                selected = i;
                break;
            }
            if (count > 0 && (role == Role.MARKSMAN ? count < preferred : count > preferred)) {
                selected = i;
                preferred = count;
            }
        }
        if (selected >= 0 && selected != mode) tag.setInteger("HMGMode", selected);
    }

    private Role roleFor(HMGItem_Unified_Guns gun) {
        WeaponClass type = gun.gunInfo.weaponClass;
        if (type == WeaponClass.SHOTGUN || type == WeaponClass.SUBMACHINE_GUN || type == WeaponClass.HANDGUN
                || shooter instanceof GVCEntityGuerrillaSG) return Role.CLOSE;
        if (type == WeaponClass.MACHINE_GUN || shooter instanceof GVCEntityGuerrillaMG
                || shooter instanceof GVCEntityPMCMG || shooter instanceof GVCEntitySoldierMG) return Role.SUPPORT;
        if (type == WeaponClass.SNIPER_RIFLE || type == WeaponClass.BOLT_ACTION_RIFLE || type == WeaponClass.LAUNCHER
                || shooter instanceof GVCEntityGuerrillaSP || shooter instanceof GVCEntityPMCSP
                || shooter instanceof GVCEntitySoldierSP) return Role.MARKSMAN;
        return Role.RIFLE;
    }

    @Override
    public void updateTask() {
        if (target != shooter.getAttackTarget() || target == null || !target.isEntityAlive() || weapon != getWeapon()) {
            resetTask();
            return;
        }
        visible = !shooter.isPotionActive(Potion.blindness) && shooter.getEntitySenses().canSee(target);
        if (visible) {
            lastSeenFeet.set(target.posX, target.boundingBox.minY, target.posZ);
            hasLastSeen = true;
        } else {
            reaction = Math.max(reaction, 6);
        }
        aimTarget();
        fireWeapon();
        movePosition();
        if (movement.isBlocked() || (!visible && hasLastSeen && movement.getMovingSpeed() == 0)) blockedTicks++;
        else blockedTicks = 0;
        if (blockedTicks > 75 && (!visible || firingLaneBlocked || shooter.getDistanceSqToEntity(target) > role.maximum * role.maximum)) {
            movement.disengage((EntityLivingBase)target);
            shooter.setAttackTarget(null);
            resetTask();
        }
    }

    @Override
    public void aimTarget() {
        IGVCmob infantry = (IGVCmob)shooter;
        if (!visible) {
            infantry.setAimPos(null);
            shooter.setSneaking(false);
            return;
        }
        NBTTagCompound tag = weapon.getTagCompound();
        shooter.setSneaking(!tag.getBoolean("IsReloading") && shooter.getDistanceSqToEntity(target) <= role.maximum * role.maximum);
        if (--nextAimSample <= 0) {
            nextAimSample = 7 + rnd.nextInt(5);
            double error = Math.min(0.8, Math.sqrt(shooter.getDistanceSqToEntity(target)) * 0.013)
                    * cfg_infantryAimError * role.error;
            aimOffsetX = (rnd.nextDouble() - 0.5) * error;
            aimOffsetY = (rnd.nextDouble() - 0.5) * error;
            aimOffsetZ = (rnd.nextDouble() - 0.5) * error;
        }
        Vector3d aim = aiAttackManager.createAimingPoint();
        aim.set(target.posX + aimOffsetX, target.boundingBox.minY + target.height * 0.72 + aimOffsetY, target.posZ + aimOffsetZ);
        infantry.setAimPos(aim);
        shooter.getLookHelper().setLookPosition(aim.x, aim.y, aim.z, 12F, 10F);
    }

    @Override
    public void movePosition() {
        if (shooter.ridingEntity != null || !hasLastSeen || shooter.ticksExisted < nextMovement) return;
        nextMovement = shooter.ticksExisted + 10 + rnd.nextInt(7);
        double dx = lastSeenFeet.x - shooter.posX;
        double dz = lastSeenFeet.z - shooter.posZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (!visible) {
            if (distance < 1.8) movement.stop();
            else movement.moveTo(lastSeenFeet.x, lastSeenFeet.y, lastSeenFeet.z, 0.92);
            return;
        }
        if (distance > role.maximum) {
            movement.moveTo(lastSeenFeet.x, lastSeenFeet.y, lastSeenFeet.z, 1);
        } else if ((distance < role.minimum || firingLaneBlocked) && distance > 0.1) {
            double side = (shooter.getEntityId() & 1) == 0 ? 2.5 : -2.5;
            double retreat = distance < role.minimum ? 3 : 0;
            movement.moveTo(shooter.posX - (dx * retreat + dz * side) / distance, shooter.boundingBox.minY,
                    shooter.posZ + (dx * side - dz * retreat) / distance, 0.85);
        } else {
            movement.stop();
        }
    }

    @Override
    public void fireWeapon() {
        firingLaneBlocked = false;
        HMGItem_Unified_Guns gun = (HMGItem_Unified_Guns)weapon.getItem();
        NBTTagCompound tag = weapon.getTagCompound();
        if (shotPause > 0) shotPause--;
        Vector3d aim = aiAttackManager.getAimingPoint();
        if (!visible || aim == null || shooter.getDistanceSqToEntity(target) > role.maximum * role.maximum
                || tag.getBoolean("IsReloading") || gun.remain_Bullet(weapon) <= 0 || --reaction > 0) {
            cancelTrigger(tag);
            return;
        }
        Vec3 origin = Vec3.createVectorHelper(shooter.posX, shooter.posY + shooter.getEyeHeight(), shooter.posZ);
        Vec3 toAim = Vec3.createVectorHelper(aim.x - origin.xCoord, aim.y - origin.yCoord, aim.z - origin.zCoord);
        Vec3 direction = shooter.getLook(1);
        if (!(shooter.ridingEntity instanceof PlacedGunEntity) && direction.dotProduct(toAim.normalize()) < 0.997) {
            cancelTrigger(tag);
            return;
        }
        // Check the firing ray as well as the visual target, which can be seen through glass.
        if (shooter.ridingEntity instanceof PlacedGunEntity) direction = toAim.normalize();
        Vec3 end = origin.addVector(direction.xCoord * toAim.lengthVector(),
                direction.yCoord * toAim.lengthVector(), direction.zCoord * toAim.lengthVector());
        if (shooter.worldObj.rayTraceBlocks(origin, end) != null || friendlyInLane(origin, end)) {
            firingLaneBlocked = true;
            cancelTrigger(tag);
            return;
        }
        if (shotPause > 0) return;
        boolean automatic = gun.getburstCount(tag.getInteger("HMGMode")) == -1;
        if (!automatic && tag.getBoolean("Bursting")) return;
        if (burstRemaining == 0) burstRemaining = role.burst + (role.burst > 1 ? rnd.nextInt(6) : 0);
        tag.setBoolean("IsTriggered", true);
        shooter.getEntityData().setBoolean("HMGisUsingItem", true);
        if (!automatic || --burstRemaining <= 0) {
            burstRemaining = 0;
            shotPause = role.pause + rnd.nextInt(7);
        }
    }

    private boolean friendlyInLane(Vec3 start, Vec3 end) {
        AxisAlignedBB search = AxisAlignedBB.getBoundingBox(Math.min(start.xCoord, end.xCoord),
                Math.min(start.yCoord, end.yCoord), Math.min(start.zCoord, end.zCoord),
                Math.max(start.xCoord, end.xCoord), Math.max(start.yCoord, end.yCoord),
                Math.max(start.zCoord, end.zCoord)).expand(0.9, 0.9, 0.9);
        for (Object entry : shooter.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, search)) {
            EntityLivingBase entity = (EntityLivingBase)entry;
            if (entity == shooter || entity == target || !entity.isEntityAlive()
                    || (!((IFF)shooter).is_this_entity_friend(entity) && !shooter.isOnSameTeam(entity))) continue;
            AxisAlignedBB hitBox = entity.boundingBox.expand(0.30, 0.24, 0.30);
            if (hitBox.isVecInside(start) || hitBox.calculateIntercept(start, end) != null) return true;
        }
        return false;
    }

    private void cancelTrigger(NBTTagCompound tag) {
        tag.setBoolean("IsTriggered", false);
        tag.setBoolean("TriggerBacked", false);
        tag.setBoolean("Bursting", false);
        burstRemaining = 0;
    }

    @Override
    public void resetTask() {
        if (weapon != null && weapon.hasTagCompound()) cancelTrigger(weapon.getTagCompound());
        shooter.getEntityData().setBoolean("HMGisUsingItem", false);
        shooter.setSneaking(false);
        ((IGVCmob)shooter).setAimPos(null);
        movement.stop();
        weapon = null;
    }
}
