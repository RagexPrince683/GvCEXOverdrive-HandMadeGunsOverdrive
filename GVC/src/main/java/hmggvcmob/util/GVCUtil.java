package hmggvcmob.util;

import handmadeguns.entity.PlacedGunEntity;
import handmadevehicle.entity.EntityDummy_rider;
import handmadevehicle.entity.EntityVehicle;
import handmadevehicle.entity.parts.SeatObject;
import handmadevehicle.entity.parts.logics.BaseLogic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import static hmggvcmob.GVCMobPlus.VEHICLES_ENABLED;

public class GVCUtil {
	public static double audibleGunshotLevel(Entity entity) {
		if (!entity.getEntityData().hasKey("HMGGunshotTick")) return 0;
		long age = entity.worldObj.getTotalWorldTime() - entity.getEntityData().getLong("HMGGunshotTick");
		if (age < 0 || age > 200) return 0;
		return entity.getEntityData().getFloat("GunshotLevel") * Math.pow(0.95, age);
	}

	public static boolean spawnFromItem(World world, Entity entity, EntityPlayer player, ItemStack stack) {
		if (!world.spawnEntityInWorld(entity)) return false;
		if (!player.capabilities.isCreativeMode) stack.stackSize--;
		return true;
	}

	public static Entity getRidingEntity(Entity entity){
		if(VEHICLES_ENABLED && entity.ridingEntity instanceof EntityDummy_rider){
			return ((EntityDummy_rider) entity.ridingEntity).linkedBaseLogic.mc_Entity;
		}
		return entity.ridingEntity;
	}
	public static BaseLogic getRidingVehicle(Entity entity){//乗っている車両を返す
		if(VEHICLES_ENABLED && entity.ridingEntity instanceof EntityDummy_rider){
			return ((EntityDummy_rider) entity.ridingEntity).linkedBaseLogic;
		}
		return null;
	}
	public static SeatObject getRidingSeat(EntityDummy_rider ridingEntity){
		return ridingEntity.linkedBaseLogic.seatObjects[ridingEntity.linkedSeatID];
	}
	public static BaseLogic getDrivingVehicle(Entity entity){//操縦している車両を返す
		if(VEHICLES_ENABLED && entity.ridingEntity instanceof EntityDummy_rider && ((EntityDummy_rider) entity.ridingEntity).linkedBaseLogic.ispilot(entity)){
			return ((EntityDummy_rider) entity.ridingEntity).linkedBaseLogic;
		}
		return null;
	}
	public static boolean isCollectLocking(Entity currentLocking , Entity target){
		if(currentLocking == target)return true;
		if(currentLocking == getRidingEntity(target))return true;
		BaseLogic targetRiding = getRidingVehicle(target);
		if(targetRiding != null && currentLocking == targetRiding.mc_Entity)return true;
		return false;
	}

	public static boolean isOnGround(Entity entity){
		boolean flag = entity.posY<100 || entity.onGround;
		if(entity.ridingEntity != null){
			flag |= entity.ridingEntity.onGround;
			BaseLogic ridingVehicle = getRidingVehicle(entity);
			if(ridingVehicle != null){
				flag |= ridingVehicle.mc_Entity.onGround;
				//flag |= ridingVehicle.prefab_vehicle.T_Land_F_Plane;
			}
		}
		return flag;
	}

	public static EntityRidingState getRidingType(Entity entity){
		Entity ridingEntity = getRidingEntity(entity);
		if(ridingEntity == null){
			return EntityRidingState.None;
		}else {
			if(ridingEntity instanceof PlacedGunEntity){
				return EntityRidingState.Gun;
			}
			if(VEHICLES_ENABLED && ridingEntity instanceof EntityVehicle){
				BaseLogic ridingVehicle = getRidingVehicle(entity);
				if(ridingVehicle != null && ridingVehicle.ispilot(entity)) {
					//if (ridingVehicle.prefab_vehicle.T_Land_F_Plane) {
					//	return EntityRidingState.Tank;
					//}else
					//if (ridingVehicle.prefab_vehicle.type_F_Plane_T_Heli) {
					//	return EntityRidingState.Heli;
					//}else {
						return EntityRidingState.AirPlane;
					//}
				}else {
					if(getRidingSeat((EntityDummy_rider) entity.ridingEntity).hasAvailableWeapon())
					return EntityRidingState.Turret;
				}
			}
		}
		return EntityRidingState.None;
	}
}
