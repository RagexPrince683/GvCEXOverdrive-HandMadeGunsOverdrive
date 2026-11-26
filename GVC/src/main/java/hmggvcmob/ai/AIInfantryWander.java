package hmggvcmob.ai;

import hmggvcmob.entity.IGVCmob;
import hmggvcmob.entity.IPlatoonable;
import hmggvcmob.entity.util.MoveToPositionMng;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.Vec3;

public class AIInfantryWander extends EntityAIBase {
    private final EntityCreature soldier;
    private final MoveToPositionMng movement;
    private final double speed;
    private int nextWander;
    private int until;
    private Vec3 destination;

    public AIInfantryWander(EntityCreature soldier, double speed) {
        this.soldier = soldier;
        this.speed = speed;
        movement = ((IGVCmob)soldier).getMoveToPositionMng();
        nextWander = 40 + soldier.getRNG().nextInt(80);
        setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (!isIdle() || soldier.ticksExisted < nextWander) return false;
        nextWander = soldier.ticksExisted + 80 + soldier.getRNG().nextInt(120);
        destination = RandomPositionGenerator.findRandomTarget(soldier, 8, 3);
        return destination != null;
    }

    @Override
    public void startExecuting() {
        until = soldier.ticksExisted + 90;
        movement.moveTo(destination.xCoord, destination.yCoord, destination.zCoord, speed);
    }

    @Override
    public boolean continueExecuting() {
        return isIdle() && soldier.ticksExisted < until && !movement.isBlocked();
    }

    private boolean isIdle() {
        return soldier.ridingEntity == null && soldier.getAttackTarget() == null && ((IPlatoonable)soldier).isFree();
    }

    @Override
    public void resetTask() {
        movement.stop();
    }
}
