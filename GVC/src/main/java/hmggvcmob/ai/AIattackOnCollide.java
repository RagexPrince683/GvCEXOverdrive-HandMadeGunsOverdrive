package hmggvcmob.ai;

import handmadeguns.items.guns.HMGItem_Unified_Guns;
import hmggvcmob.entity.IGVCmob;
import hmggvcmob.entity.util.MoveToPositionMng;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;

public class AIattackOnCollide extends EntityAIBase {
    private final EntityCreature attacker;
    private final MoveToPositionMng movement;
    private int nextAttack;

    public AIattackOnCollide(EntityCreature attacker) {
        this.attacker = attacker;
        movement = ((IGVCmob)attacker).getMoveToPositionMng();
        setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase target = attacker.getAttackTarget();
        ItemStack held = attacker.getHeldItem();
        return target != null && target.isEntityAlive() && attacker.ridingEntity == null
                && (held == null || !(held.getItem() instanceof HMGItem_Unified_Guns));
    }

    @Override
    public void startExecuting() {
        attacker.getNavigator().clearPathEntity();
    }

    @Override
    public void updateTask() {
        EntityLivingBase target = attacker.getAttackTarget();
        if (target == null || attacker.isPotionActive(Potion.blindness)
                || !attacker.getEntitySenses().canSee(target)) {
            movement.stop();
            return;
        }

        double reach = attacker.width * 2;
        if (attacker.getDistanceSqToEntity(target) <= reach * reach + target.width) {
            movement.stop();
            attacker.getLookHelper().setLookPositionWithEntity(target, 25, 25);
            if (attacker.ticksExisted >= nextAttack) {
                attacker.swingItem();
                attacker.attackEntityAsMob(target);
                nextAttack = attacker.ticksExisted + 20;
            }
        } else {
            movement.moveTo(target.posX, target.boundingBox.minY, target.posZ, 1);
            if (movement.isBlocked()) {
                movement.disengage(target);
                attacker.setAttackTarget(null);
            }
        }
    }

    @Override
    public void resetTask() {
        movement.stop();
    }
}
