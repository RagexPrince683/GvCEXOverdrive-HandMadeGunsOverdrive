package hmggvcmob.entity;

import handmadevehicle.entity.parts.Modes;
import hmggvcmob.camp.CampObj;

import static handmadevehicle.entity.parts.Modes.Go;
import static handmadevehicle.entity.parts.Modes.Wait;


public interface IflagBattler extends IPlatoonable {
    CampObj getCampObj();
    default int[] getTargetCampPosition(){
        if (getPlatoon() != null && (getPlatoon().platoonMode == Modes.Attack || getPlatoon().platoonMode == Modes.Defend)) return new int[]{
                (int) getPlatoon().PlatoonTargetPos.x,
                (int) getPlatoon().PlatoonTargetPos.y,
                (int) getPlatoon().PlatoonTargetPos.z};
        else return null;
    }
    default void setTargetCampPosition(int[] ints){
        if (getPlatoon() != null) getPlatoon().order(Modes.Attack, ints[0] + 0.5, ints[1] + 1, ints[2] + 0.5);
    }

    @Override
    default Modes getMobMode() {
        return getPlatoon() == null ? Wait : getPlatoon().platoonMode;
    }

    @Override
    default double[] getTargetpos() {
        int[] position = getTargetCampPosition();
        return position == null ? null : new double[]{position[0], position[1], position[2]};
    }

    boolean isThisAttackAbleCamp(CampObj campObj);
    boolean isThisFriendCamp(CampObj campObj);
    boolean isThisIgnoreSpawnCamp(CampObj campObj);
}
