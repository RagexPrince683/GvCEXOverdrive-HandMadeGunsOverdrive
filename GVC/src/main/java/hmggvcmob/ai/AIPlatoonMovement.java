package hmggvcmob.ai;

import hmggvcmob.entity.IGVCmob;
import hmggvcmob.entity.IPlatoonable;
import hmggvcmob.entity.util.MoveToPositionMng;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;

import javax.vecmath.Vector3d;

public class AIPlatoonMovement extends EntityAIBase {
    private final EntityLiving member;
    private final IPlatoonable unit;
    private final MoveToPositionMng movement;

    public AIPlatoonMovement(EntityLiving member) {
        this.member = member;
        unit = (IPlatoonable)member;
        movement = ((IGVCmob)member).getMoveToPositionMng();
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        return unit.getPlatoon() != null && member.ridingEntity == null && member.getAttackTarget() == null;
    }

    @Override
    public void startExecuting() {
        member.getNavigator().clearPathEntity();
    }

    @Override
    public void updateTask() {
        Vector3d position = unit.getMoveToPos();
        movement.moveTo(position.x, position.y, position.z, 0.85);
    }

    @Override
    public void resetTask() {
        movement.stop();
    }
}
