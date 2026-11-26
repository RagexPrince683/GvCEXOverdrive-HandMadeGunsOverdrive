package hmggvcmob.ai.newai;

import hmggvcmob.entity.IGVCmob;
import hmggvcmob.util.EntityRidingState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;

import javax.vecmath.Vector3d;

import static hmggvcmob.util.GVCUtil.getRidingType;
import static hmggvcmob.GVCMobPlus.VEHICLES_ENABLED;

public class AIAttackManager extends EntityAIBase {
	private final EntityLiving shooter;

	private AIAttack currentAI;

	private Vector3d aimingPoint;

	private AIAttackEntityByGun toEntity_Inf;
	private AIAttackEntityByAirPlane toEntity_airPlane;
	private AIAttackEntityByHeli toEntity_Heli;
	private AIAttackEntityByTank toEntity_Tank;

	public AIAttackManager(EntityLiving shooter) {
		this.shooter = shooter;
		setMutexBits(3);
		toEntity_Inf = new AIAttackEntityByGun(shooter,this);
		if (VEHICLES_ENABLED) {
			toEntity_Tank = new AIAttackEntityByTank(shooter,this);
			toEntity_airPlane = new AIAttackEntityByAirPlane(shooter,this);
			toEntity_Heli = new AIAttackEntityByHeli(shooter,this);
		}
	}

	@Override
	public boolean shouldExecute() {
		if (!VEHICLES_ENABLED) {
			currentAI = toEntity_Inf;
			return currentAI.shouldExecute();
		}
		currentAI = null;
		EntityRidingState type = getRidingType(shooter);
		switch (type){
			case None:
			case Gun:
				currentAI = toEntity_Inf;
				break;
			case AirPlane:
				currentAI = toEntity_airPlane;
				break;
			case Heli:
				currentAI = toEntity_Heli;
				break;
			case Turret:
			case Tank:
				currentAI = toEntity_Tank;
				break;
		}
		if(currentAI != null){
			return currentAI.shouldExecute();
		}
		return false;
	}

	@Override
	public boolean continueExecuting() {
		return currentAI != null && currentAI.continueExecuting();
	}

	@Override
	public void startExecuting() {
		if (currentAI != null) currentAI.startExecuting();
	}

	public Vector3d getAimingPoint(){
		return this.aimingPoint;
	}

	public Vector3d createAimingPoint(){
		if(aimingPoint == null)aimingPoint = new Vector3d();
		return this.aimingPoint;
	}


	@Override
	public void updateTask(){
		if(currentAI != null)currentAI.updateTask();
	}
	@Override
	public void resetTask(){
		if(currentAI != null)currentAI.resetTask();
		aimingPoint = null;
	}
}
