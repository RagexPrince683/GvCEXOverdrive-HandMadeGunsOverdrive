package hmggvcmob.ai;

import handmadeguns.entity.IFF;
import handmadevehicle.entity.parts.Modes;
import hmggvcmob.entity.IflagBattler;
import hmggvcmob.tile.TileEntityFlag;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;

public class AITargetFlag extends EntityAIBase {
    private final IflagBattler battler;
    private final EntityLiving entity;
    private int cooldown;
    private boolean checkedNearbySquad;

    public AITargetFlag(IflagBattler battler, EntityLiving entity, IFF iff) {
        this.battler = battler;
        this.entity = entity;
        cooldown = entity.getRNG().nextInt(600);
    }

    @Override
    public boolean shouldExecute() {
        if (--cooldown > 0 || !(battler.isPlatoonLeader() || battler.isFree())) return false;
        cooldown = 30 + entity.getRNG().nextInt(30);
        TileEntityFlag flag = TileEntityFlag.findNearest(entity.worldObj, entity.posX, entity.posY,
                entity.posZ, 128, false, camp -> battler.isThisAttackAbleCamp(camp.campObj)
                        || (camp.isAttacked() && battler.isThisFriendCamp(camp.campObj)));
        if (flag == null) {
            if (battler.isFree() && !checkedNearbySquad) {
                checkedNearbySquad = true;
                for (Object entry : entity.worldObj.getEntitiesWithinAABB(EntityLiving.class,
                        entity.boundingBox.expand(8, 3, 8))) {
                    if (entry == entity || !(entry instanceof IflagBattler)) continue;
                    IflagBattler ally = (IflagBattler)entry;
                    if (ally.getCampObj() == battler.getCampObj() && ally.isFree()
                            && ((EntityLiving)entry).isEntityAlive()) {
                        battler.makePlatoon();
                        break;
                    }
                }
            }
            if (battler.getPlatoon() != null && battler.getPlatoon().hasCurrentCamp()) {
                battler.getPlatoon().setMode(Modes.Wait);
                battler.getPlatoon().setCurrentCamp(false);
            }
            return false;
        }
        if (battler.isFree()) battler.makePlatoon();
        Modes order = battler.isThisAttackAbleCamp(flag.campObj) ? Modes.Attack : Modes.Defend;
        battler.getPlatoon().order(order, flag.xCoord + 0.5, flag.yCoord + 1, flag.zCoord + 0.5);
        if (!battler.getPlatoon().hasCurrentCamp()) battler.getPlatoon().setCurrentCamp(true);
        return true;
    }
}
