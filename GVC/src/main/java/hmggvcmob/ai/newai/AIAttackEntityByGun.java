package hmggvcmob.ai.newai;

import cpw.mods.fml.common.FMLLog;
import handmadeguns.entity.IFF;
import handmadeguns.entity.PlacedGunEntity;
import handmadeguns.items.WeaponClass;
import handmadeguns.items.guns.HMGItem_Unified_Guns;
import hmggvcmob.entity.IGVCmob;
import hmggvcmob.entity.IPlatoonable;
import hmggvcmob.entity.guerrilla.GVCEntityGuerrillaMG;
import hmggvcmob.entity.guerrilla.GVCEntityGuerrillaSG;
import hmggvcmob.entity.guerrilla.GVCEntityGuerrillaSP;
import hmggvcmob.entity.friend.GVCEntityPMCMG;
import hmggvcmob.entity.friend.GVCEntityPMCSP;
import hmggvcmob.entity.friend.GVCEntitySoldierMG;
import hmggvcmob.entity.friend.GVCEntitySoldierSP;
import hmggvcmob.entity.util.MoveToPositionMng;
import hmggvcmob.entity.util.PlatoonOBJ;
import hmggvcmob.military.EntityMilitaryUnit;
import hmggvcmob.military.UnitPresets;
import hmggvcmob.military.WeaponCatalog;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import javax.vecmath.Vector3d;

import static hmggvcmob.GVCMobPlus.cfg_infantryAimError;
import static hmggvcmob.GVCMobPlus.cfg_infantryCombatDiagnostics;
import static hmggvcmob.GVCMobPlus.cfg_infantryReactionTicks;

public class AIAttackEntityByGun extends AIAttackToEntity {
    private enum FireState {
        NO_SIGHT, OUT_OF_RANGE, REACTION, AIMING, BLOCK, FRIENDLY, RELOADING, EMPTY, CYCLING, BETWEEN_BURSTS, TRIGGER
    }

    private enum MovementState { HOLD, PURSUE, SEARCH, FLANK, MOUNTED }

    private static final int RECOVERY_DELAY = 60;
    private static final int GIVE_UP_DELAY = 165;
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
    private Entity weaponUser;
    private Role role;
    private boolean visible;
    private int nextMovement;
    private int reaction;
    private int nextAimSample;
    private int shotPause;
    private int burstRemaining;
    private int lastAction;
    private int lastMovement;
    private int lostSightTicks;
    private int lastAmmo;
    private boolean lastReloading;
    private long lastShot;
    private int shots;
    private int flankAttempts;
    private int nextFlank;
    private final Vector3d progressPosition = new Vector3d();
    private double aimOffsetX, aimOffsetY, aimOffsetZ;
    private final Vector3d lastSeenFeet = new Vector3d();
    private boolean hasLastSeen;
    private FireState fireState;
    private MovementState movementState;
    private MovementState loggedMovement;
    private FireState loggedFire;
    private int stateSince;
    private int nextDiagnostic;
    private float targetPitch;
    private final EntityMilitaryUnit military;
    private int engagementRange;
    private double recoilError;

    public AIAttackEntityByGun(EntityLiving shooter, AIAttackManager manager) {
        super(shooter, manager);
        movement = ((IGVCmob)shooter).getMoveToPositionMng();
        rnd = shooter.getRNG();
        military = shooter instanceof EntityMilitaryUnit ? (EntityMilitaryUnit) shooter : null;
    }

    private ItemStack getWeapon() {
        return shooter.ridingEntity instanceof PlacedGunEntity
                ? ((PlacedGunEntity)shooter.ridingEntity).gunStack : shooter.getHeldItem();
    }

    private Entity getWeaponUser() {
        return shooter.ridingEntity instanceof PlacedGunEntity ? shooter.ridingEntity : shooter;
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
        UnitPresets.Preset preset = military == null ? null : military.preset();
        engagementRange = preset == null ? role.maximum : preset.range;
        if (preset != null && role == Role.CLOSE) engagementRange = Math.min(engagementRange, 24);
        movement.stop();
        shooter.getNavigator().clearPathEntity();
        reaction = (preset == null ? cfg_infantryReactionTicks : preset.reactionTicks) + rnd.nextInt(8);
        if (military != null && military.hasSupplyPenalty()) reaction += 8;
        recoilError = 0;
        nextAimSample = 0;
        nextMovement = shooter.ticksExisted;
        shotPause = 0;
        burstRemaining = 0;
        lastAction = lastMovement = shooter.ticksExisted;
        weaponUser = getWeaponUser();
        lastShot = weaponUser.getEntityData().getLong("HMGGunshotTick");
        shots = 0;
        lastAmmo = -1;
        lastReloading = false;
        lostSightTicks = 0;
        flankAttempts = 0;
        nextFlank = shooter.ticksExisted + 18;
        progressPosition.set(shooter.posX, shooter.posY, shooter.posZ);
        hasLastSeen = false;
        movementState = MovementState.HOLD;
        loggedMovement = null;
        loggedFire = null;
        stateSince = nextDiagnostic = shooter.ticksExisted;
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
        String classification = WeaponCatalog.classification(gun);
        UnitPresets.Preset preset = military == null ? null : military.preset();
        if (preset != null) {
            if ("close".equals(preset.behavior)) return Role.CLOSE;
            if ("support".equals(preset.behavior)) return Role.SUPPORT;
            if ("marksman".equals(preset.behavior) || "LAUNCHER".equals(classification)) return Role.MARKSMAN;
        }
        WeaponClass type = gun.gunInfo.weaponClass;
        if ("SHOTGUN".equals(classification) || "SUBMACHINE_GUN".equals(classification) || "HANDGUN".equals(classification)
                || type == WeaponClass.SHOTGUN || type == WeaponClass.SUBMACHINE_GUN || type == WeaponClass.HANDGUN
                || shooter instanceof GVCEntityGuerrillaSG) return Role.CLOSE;
        if ("MACHINE_GUN".equals(classification) || type == WeaponClass.MACHINE_GUN || shooter instanceof GVCEntityGuerrillaMG
                || shooter instanceof GVCEntityPMCMG || shooter instanceof GVCEntitySoldierMG) return Role.SUPPORT;
        if ("SNIPER_RIFLE".equals(classification) || "BOLT_ACTION_RIFLE".equals(classification) || "LAUNCHER".equals(classification)
                || type == WeaponClass.SNIPER_RIFLE || type == WeaponClass.BOLT_ACTION_RIFLE || type == WeaponClass.LAUNCHER
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
            if (lostSightTicks >= 12) reaction = Math.max(reaction, 6);
            lostSightTicks = 0;
            lastSeenFeet.set(target.posX, target.boundingBox.minY, target.posZ);
            hasLastSeen = true;
        } else {
            lostSightTicks++;
        }
        if (reaction > 0) reaction--;
        observeProgress();
        aimTarget();
        fireWeapon();
        movePosition();
        if (cfg_infantryCombatDiagnostics) logCombat(false);
        int tick = shooter.ticksExisted;
        HMGItem_Unified_Guns gun = (HMGItem_Unified_Guns)weapon.getItem();
        int actionLimit = lastReloading ? Math.max(GIVE_UP_DELAY, gun.reloadTime(weapon, weaponUser) + 60) : GIVE_UP_DELAY;
        if (tick - lastAction >= actionLimit && (tick - lastMovement >= RECOVERY_DELAY
                || movementState == MovementState.FLANK && flankAttempts >= 3)) {
            if (cfg_infantryCombatDiagnostics) logCombat(true);
            movement.disengage((EntityLivingBase)target);
            shooter.setAttackTarget(null);
            resetTask();
        }
    }

    private void observeProgress() {
        int tick = shooter.ticksExisted;
        if (shooter.getDistanceSq(progressPosition.x, progressPosition.y, progressPosition.z) >= 1) {
            lastMovement = tick;
            progressPosition.set(shooter.posX, shooter.posY, shooter.posZ);
        }
        HMGItem_Unified_Guns gun = (HMGItem_Unified_Guns)weapon.getItem();
        int ammo = gun.remain_Bullet(weapon);
        boolean reloading = isReloading(weapon.getTagCompound());
        if (lastAmmo >= 0 && ammo > lastAmmo) {
            lastAction = tick;
            if (cfg_infantryCombatDiagnostics) {
                FMLLog.info("[GVC combat] tick=%d npc=%d name=%s gun=%s reload=%d->%d/%d",
                        shooter.worldObj.getTotalWorldTime(), shooter.getEntityId(), shooter.getCommandSenderName(),
                        Item.itemRegistry.getNameForObject(gun), lastAmmo, ammo, gun.max_Bullet(weapon));
            }
        }
        if (lastReloading && !reloading) shotPause = 0;
        lastReloading = reloading;
        lastAmmo = ammo;
        Entity currentUser = getWeaponUser();
        long shot = currentUser.getEntityData().getLong("HMGGunshotTick");
        if (shotPause > 0) shotPause--;
        recoilError *= .72;
        if (currentUser != weaponUser) {
            weaponUser = currentUser;
            lastShot = shot;
        } else if (shot != lastShot) {
            lastShot = shot;
            shots++;
            if (military != null) recoilError = Math.min(.9, recoilError + .16 * military.recoilMultiplier(weapon));
            lastAction = tick;
            flankAttempts = 0;
            if (gun.getburstCount(weapon.getTagCompound().getInteger("HMGMode")) != -1) {
                shotPause = role.pause + rnd.nextInt(7);
            } else if (--burstRemaining <= 0) {
                burstRemaining = 0;
                shotPause = role.pause + rnd.nextInt(7);
            }
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
        shooter.setSneaking(!isReloading(tag) && movementState == MovementState.HOLD
                && shooter.getDistanceSqToEntity(target) <= engagementRange * engagementRange);
        if (--nextAimSample <= 0) {
            nextAimSample = 7 + rnd.nextInt(5);
            double error = Math.min(0.8, Math.sqrt(shooter.getDistanceSqToEntity(target)) * 0.013)
                    * cfg_infantryAimError * role.error;
            if (military != null && military.preset() != null) error *= military.preset().accuracy;
            if (military != null && military.hasSupplyPenalty()) error *= 1.5;
            aimOffsetX = (rnd.nextDouble() - 0.5) * error;
            aimOffsetY = (rnd.nextDouble() - 0.5) * error;
            aimOffsetZ = (rnd.nextDouble() - 0.5) * error;
        }
        Vector3d aim = aiAttackManager.createAimingPoint();
        aim.set(target.posX + aimOffsetX, target.boundingBox.minY + target.height * 0.72 + aimOffsetY + recoilError, target.posZ + aimOffsetZ);
        infantry.setAimPos(aim);
        double eyeY = shooter.posY + shooter.getEyeHeight();
        double horizontal = Math.hypot(aim.x - shooter.posX, aim.z - shooter.posZ);
        targetPitch = (float)-Math.toDegrees(Math.atan2(aim.y - eyeY, horizontal));
        float pitchStep = MathHelper.clamp_float(targetPitch - shooter.rotationPitch, -10F, 10F);
        double lookPitch = Math.toRadians(MathHelper.clamp_float(shooter.rotationPitch + pitchStep, -89.9F, 89.9F));
        double lookX = aim.x;
        double lookZ = aim.z;
        if (horizontal < 0.001) {
            double yaw = Math.toRadians(shooter.rotationYawHead);
            lookX = shooter.posX - Math.sin(yaw);
            lookZ = shooter.posZ + Math.cos(yaw);
            horizontal = 1;
        }
        // Vanilla resets pitch to zero on every look update, so send the already-limited angle.
        shooter.getLookHelper().setLookPosition(lookX, eyeY - horizontal * Math.tan(lookPitch), lookZ, 12F, 90F);
    }

    @Override
    public void movePosition() {
        if (shooter.ridingEntity != null) {
            movementState = MovementState.MOUNTED;
            return;
        }
        int tick = shooter.ticksExisted;
        if (!hasLastSeen) {
            movementState = MovementState.HOLD;
            return;
        }
        double dx = lastSeenFeet.x - shooter.posX;
        double dz = lastSeenFeet.z - shooter.posZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        boolean blockedLane = fireState == FireState.BLOCK || fireState == FireState.FRIENDLY;
        boolean stalledAim = (fireState == FireState.AIMING || fireState == FireState.TRIGGER || fireState == FireState.CYCLING)
                && tick - lastAction >= RECOVERY_DELAY;
        boolean atLastSeen = distance < 2 && Math.abs(lastSeenFeet.y - shooter.boundingBox.minY) < 2;
        if (movementState == MovementState.FLANK && tick < nextFlank
                && !movement.hasArrived() && fireState != FireState.TRIGGER) return;
        if ((blockedLane || stalledAim || !visible && (atLastSeen || movement.isBlocked()))
                && distance > 0.1 && tick >= nextFlank) {
            double side = ((shooter.getEntityId() + flankAttempts++) & 1) == 0 ? 3.5 : -3.5;
            double advance = fireState == FireState.FRIENDLY ? -0.5 : 1.4;
            if (distance < role.minimum) advance = -1.6;
            movement.moveTo(shooter.posX + (dx * advance - dz * side) / distance, shooter.boundingBox.minY,
                    shooter.posZ + (dz * advance + dx * side) / distance, 0.94);
            movementState = MovementState.FLANK;
            nextFlank = tick + 34 + rnd.nextInt(9);
            return;
        }
        if (tick < nextMovement) return;
        nextMovement = tick + 10 + rnd.nextInt(7);
        if (!visible) {
            movementState = MovementState.SEARCH;
            if (atLastSeen) movement.stop();
            else movement.moveTo(lastSeenFeet.x, lastSeenFeet.y, lastSeenFeet.z, 0.94);
        } else if (fireState == FireState.OUT_OF_RANGE
                || shooter.getDistanceSqToEntity(target) > engagementRange * engagementRange) {
            movementState = MovementState.PURSUE;
            movement.moveTo(lastSeenFeet.x, lastSeenFeet.y, lastSeenFeet.z, 1);
        } else {
            movementState = MovementState.HOLD;
            movement.stop();
        }
    }

    @Override
    public void fireWeapon() {
        HMGItem_Unified_Guns gun = (HMGItem_Unified_Guns)weapon.getItem();
        NBTTagCompound tag = weapon.getTagCompound();
        // Request a top-up once during a sustained break in contact. HMG owns every insertion.
        if (lostSightTicks == 24 && lastAmmo > 0 && lastAmmo <= gun.max_Bullet(weapon) / 2 && !lastReloading) {
            gun.startReloadFromKey(weapon, shooter.worldObj, weaponUser);
        }
        Vector3d aim = aiAttackManager.getAimingPoint();
        if (!visible || aim == null) fireState = FireState.NO_SIGHT;
        else if (shooter.getDistanceSqToEntity(target) > engagementRange * engagementRange) fireState = FireState.OUT_OF_RANGE;
        else if (isReloading(tag)) fireState = FireState.RELOADING;
        else if (lastAmmo <= 0) fireState = FireState.EMPTY;
        else if (reaction > 0) fireState = FireState.REACTION;
        else fireState = FireState.AIMING;
        if (fireState != FireState.AIMING) {
            cancelTrigger(tag);
            return;
        }
        if (shotPause > 0 && !tag.getBoolean("Bursting")) {
            fireState = FireState.BETWEEN_BURSTS;
            return;
        }
        Vec3 origin = Vec3.createVectorHelper(shooter.posX, shooter.posY + shooter.getEyeHeight(), shooter.posZ);
        Vec3 toAim = Vec3.createVectorHelper(aim.x - origin.xCoord, aim.y - origin.yCoord, aim.z - origin.zCoord);
        Vec3 direction = shooter.getLook(1);
        if (!(shooter.ridingEntity instanceof PlacedGunEntity) && direction.dotProduct(toAim.normalize()) < 0.992) {
            cancelTrigger(tag);
            return;
        }
        // Check the firing ray as well as the visual target, which can be seen through glass.
        if (shooter.ridingEntity instanceof PlacedGunEntity) direction = toAim.normalize();
        Vec3 end = origin.addVector(direction.xCoord * toAim.lengthVector(),
                direction.yCoord * toAim.lengthVector(), direction.zCoord * toAim.lengthVector());
        if (shooter.worldObj.rayTraceBlocks(origin, end) != null) fireState = FireState.BLOCK;
        else if (friendlyInLane(origin, end) || unsafeExplosiveShot(gun)) fireState = FireState.FRIENDLY;
        if (fireState != FireState.AIMING) {
            cancelTrigger(tag);
            return;
        }
        if (shotPause > 0) {
            fireState = FireState.BETWEEN_BURSTS;
            return;
        }
        if (tag.getBoolean("Bursting") || tag.getByte("Bolt") > 1
                || gun.gunInfo.needcock && !tag.getBoolean("Cocking")) {
            fireState = FireState.CYCLING;
            return;
        }
        fireState = FireState.TRIGGER;
        // Give HMG a release tick before requesting the next shot or completing a charge trigger.
        if (tag.getBoolean("TriggerBacked")) return;
        if (burstRemaining == 0) burstRemaining = role.burst + (role.burst > 1 ? rnd.nextInt(6) : 0);
        tag.setBoolean("IsTriggered", true);
        shooter.getEntityData().setBoolean("HMGisUsingItem", true);
    }

    private boolean isReloading(NBTTagCompound tag) {
        return tag.getBoolean("IsReloading") || tag.getInteger("PerShellInterruptTime") > 0;
    }

    private boolean unsafeExplosiveShot(HMGItem_Unified_Guns gun) {
        if (!"LAUNCHER".equals(WeaponCatalog.classification(gun)) && gun.gunInfo.weaponClass != WeaponClass.LAUNCHER) return false;
        if (shooter.getDistanceSqToEntity(target) < 10 * 10) return true;
        for (Object entry : shooter.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, target.boundingBox.expand(5, 4, 5))) {
            EntityLivingBase nearby = (EntityLivingBase) entry;
            if (nearby != target && nearby.isEntityAlive() && ((IFF)shooter).is_this_entity_friend(nearby)) return true;
        }
        return false;
    }

    private void logCombat(boolean givingUp) {
        int tick = shooter.ticksExisted;
        boolean changed = loggedFire != fireState || loggedMovement != movementState;
        boolean stalled = tick - lastAction >= RECOVERY_DELAY;
        if (!givingUp && (tick < nextDiagnostic || !changed && !stalled)) return;
        HMGItem_Unified_Guns gun = (HMGItem_Unified_Guns)weapon.getItem();
        NBTTagCompound tag = weapon.getTagCompound();
        PlatoonOBJ squad = ((IPlatoonable)shooter).getPlatoon();
        Vector3d destination = movement.getMovingSpeed() > 0 ? movement.getMoveToPos() : null;
        FMLLog.info("[GVC combat] tick=%d npc=%d name=%s target=%d gun=%s ammo=%d/%d reload=%s reloadTicks=%d mode=%d shots=%d lastShot=%d "
                        + "fire=%s move=%s destination=%s path=%s searches=%d failures=%d idle=%d stationary=%d "
                        + "previousStateTicks=%d pitch=%.1f/%.1f order=%s disengage=%s",
                shooter.worldObj.getTotalWorldTime(), shooter.getEntityId(), shooter.getCommandSenderName(), target.getEntityId(),
                Item.itemRegistry.getNameForObject(gun), lastAmmo, gun.max_Bullet(weapon), isReloading(tag), tag.getInteger("RloadTime"),
                tag.getInteger("HMGMode"), shots, lastShot, fireState, movementState, destination, movement.getPathStatus(),
                movement.getPathSearchCount(), movement.getFailures(), tick - lastAction, tick - lastMovement,
                tick - stateSince, shooter.rotationPitch, targetPitch, squad == null ? "none" : squad.getOrder(), givingUp);
        if (changed) stateSince = tick;
        loggedFire = fireState;
        loggedMovement = movementState;
        nextDiagnostic = tick + (stalled ? 35 : 15);
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
        weaponUser = null;
    }
}
