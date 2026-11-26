package hmggvcmob.ai;

import handmadeguns.entity.IFF;
import hmggvcmob.entity.IGVCmob;
import hmggvcmob.entity.IPlatoonable;
import hmggvcmob.entity.friend.EntitySoBases;
import hmggvcmob.entity.friend.GVCEntityFlag;
import hmggvcmob.entity.guerrilla.EntityGBases;
import hmggvcmob.entity.util.MoveToPositionMng;
import hmggvcmob.entity.util.PlatoonOBJ;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.monster.EntityGolem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;

import static hmggvcmob.GVCMobPlus.cfg_infantryAggression;
import static hmggvcmob.GVCMobPlus.ignoreSoTargetEntity;

public class AIInfantryTarget extends EntityAIBase {
    private final EntityLiving soldier;
    private final MoveToPositionMng movement;
    private int nextScan;
    private int lastRevenge;
    private int unseenTicks;

    public AIInfantryTarget(EntityLiving soldier) {
        this.soldier = soldier;
        movement = ((IGVCmob)soldier).getMoveToPositionMng();
        nextScan = soldier.getRNG().nextInt(20);
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        return soldier.isEntityAlive();
    }

    @Override
    public void updateTask() {
        EntityLivingBase current = soldier.getAttackTarget();
        double range = soldier.getEntityAttribute(SharedMonsterAttributes.followRange).getAttributeValue();
        int revenge = soldier.func_142015_aE();
        if (revenge != lastRevenge) {
            lastRevenge = revenge;
            EntityLivingBase attacker = soldier.getAITarget();
            if (canEngage(soldier, attacker, range) && !soldier.isPotionActive(Potion.blindness)) {
                soldier.setAttackTarget(attacker);
                current = attacker;
                unseenTicks = 0;
            }
        }
        if (current != null) {
            if (!canEngage(soldier, current, range)) {
                soldier.setAttackTarget(null);
            } else if (!soldier.isPotionActive(Potion.blindness) && soldier.getEntitySenses().canSee(current)) {
                unseenTicks = 0;
                return;
            } else if (++unseenTicks < 100) {
                return;
            } else {
                movement.disengage(current);
                soldier.setAttackTarget(null);
            }
            unseenTicks = 0;
        }
        if (soldier.ticksExisted < nextScan || soldier.isPotionActive(Potion.blindness)) return;
        nextScan = soldier.ticksExisted + 13 + soldier.getRNG().nextInt(12);
        range *= cfg_infantryAggression;
        EntityLivingBase nearest = null;
        double best = range * range;
        for (Object entry : soldier.worldObj.getEntitiesWithinAABB(EntityLivingBase.class,
                soldier.boundingBox.expand(range, 8, range))) {
            EntityLivingBase target = (EntityLivingBase)entry;
            if (!isOrdinaryTarget(soldier, target) || !canEngage(soldier, target, range)) continue;
            double distance = soldier.getDistanceSqToEntity(target);
            if (distance < best && soldier.getEntitySenses().canSee(target)) {
                best = distance;
                nearest = target;
            }
        }
        if (nearest != null) soldier.setAttackTarget(nearest);
    }

    public static boolean canEngage(EntityLiving soldier, EntityLivingBase target, double range) {
        if (target == null || target == soldier || !target.isEntityAlive() || soldier.worldObj != target.worldObj
                || soldier.isOnSameTeam(target) || ((IFF)soldier).is_this_entity_friend(target)
                || ((IGVCmob)soldier).getMoveToPositionMng().isDisengagedFrom(target)
                || soldier.getDistanceSqToEntity(target) > range * range) return false;
        if (target instanceof EntityPlayer && ((EntityPlayer)target).capabilities.disableDamage) return false;
        if (soldier instanceof EntitySoBases && ignoreSoTargetEntity.containsKey(EntityList.getEntityString(target))) return false;
        PlatoonOBJ platoon = ((IPlatoonable)soldier).getPlatoon();
        return platoon == null || platoon.allowsEngagement(target);
    }

    private static boolean isOrdinaryTarget(EntityLiving soldier, EntityLivingBase target) {
        if (soldier instanceof EntityGBases) {
            return target instanceof EntityPlayer || target instanceof EntitySoBases
                    || target instanceof EntityVillager || target instanceof EntityGolem
                    || target instanceof GVCEntityFlag;
        }
        return target instanceof EntityGBases || target instanceof IMob;
    }
}
