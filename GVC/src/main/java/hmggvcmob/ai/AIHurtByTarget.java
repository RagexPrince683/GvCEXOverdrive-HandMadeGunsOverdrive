package hmggvcmob.ai;

import handmadeguns.entity.IFF;
import hmggvcmob.entity.friend.EntitySoBases;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;

import java.util.Iterator;
import java.util.List;

import static hmggvcmob.GVCMobPlus.ignoreSoTargetEntity;

public class AIHurtByTarget extends EntityAITarget
{
    boolean entityCallsForHelp;
    private int field_142052_b;
    private static final String __OBFID = "CL_00001619";

    public AIHurtByTarget(EntityCreature p_i1660_1_, boolean p_i1660_2_)
    {
        super(p_i1660_1_, false);
        this.entityCallsForHelp = p_i1660_2_;
        this.setMutexBits(1);
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean shouldExecute()
    {
        int i = this.taskOwner.func_142015_aE();
        net.minecraft.entity.EntityLivingBase attacker = taskOwner.getAITarget();
        return attacker != null && i != field_142052_b && !taskOwner.isPotionActive(Potion.blindness)
                && (!(taskOwner instanceof IFF) || !((IFF) taskOwner).is_this_entity_friend(attacker))
                && (!(taskOwner instanceof EntitySoBases) || !ignoreSoTargetEntity.containsKey(EntityList.getEntityString(attacker)))
                && isSuitableTarget(attacker, false);
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void startExecuting()
    {
        this.field_142052_b = this.taskOwner.func_142015_aE();


        this.taskOwner.setAttackTarget(this.taskOwner.getAITarget());
        if (entityCallsForHelp) {
            AxisAlignedBB bounds = taskOwner.boundingBox.expand(16, 10, 16);
            for (Object nearby : taskOwner.worldObj.getEntitiesWithinAABB(taskOwner.getClass(), bounds)) {
                EntityCreature ally = (EntityCreature) nearby;
                if (ally != taskOwner && ally.getAttackTarget() == null
                        && !ally.isOnSameTeam(taskOwner.getAITarget())) {
                    ally.setAttackTarget(taskOwner.getAITarget());
                }
            }
        }

        super.startExecuting();
    }
    @Override
    public void resetTask()
    {
    }
}