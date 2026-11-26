package hmggvcmob.entity.friend;

import handmadevehicle.entity.EntityDummy_rider;
import hmggvcmob.GVCMobPlus;
import hmggvcmob.ai.AITargetFlag;
import hmggvcmob.entity.util.PlatoonOBJ;
import hmggvcmob.camp.CampObj;
import hmggvcmob.entity.IflagBattler;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

import static handmadevehicle.Utils.canMoveEntity;
import static hmggvcmob.GVCMobPlus.*;

public class EntitySoBase extends EntitySoBases implements IflagBattler {


	public EntitySoBase(World par1World) {
		super(par1World);
		this.targetTasks.addTask(4, aiTargetFlag = new AITargetFlag(this,this,this));
	}
	
	public boolean getCanSpawnHere()
	{
		return super.getCanSpawnHere() && this.worldObj.difficultySetting != EnumDifficulty.PEACEFUL
				&& worldObj.getEntitiesWithinAABB(EntitySoBase.class, boundingBox.expand(128, 128, 128)).size() < 40
				&& this.isValidLightLevel();
	}

	protected boolean isValidLightLevel() {
		if (hmggvcmob.tile.TileEntityFlag.findNearest(worldObj, posX, posY, posZ, 96, true,
				flag -> flag.campObj == forPlayer) != null) return false;
		int i = MathHelper.floor_double(this.posX);
		int j = MathHelper.floor_double(this.boundingBox.minY);
		int k = MathHelper.floor_double(this.posZ);


		if (this.worldObj.getSavedLightValue(EnumSkyBlock.Sky, i, j, k) <= this.rand.nextInt(32))
		{
			return false;
		}
		else
		{
			int l = this.worldObj.getSavedLightValue(EnumSkyBlock.Block,i, j, k);
			int l2= this.worldObj.getBlockLightValue(i, j, k);

			if (this.worldObj.isThundering())
			{
				int i1 = this.worldObj.skylightSubtracted;
				this.worldObj.skylightSubtracted = 10;
				l2 = this.worldObj.getBlockLightValue(i, j, k);
				this.worldObj.skylightSubtracted = i1;
			}

			return l < this.rand.nextInt(6) && l2>9 + this.rand.nextInt(8);
		}
    }

	public int getVerticalFaceSpeed()
	{
		return 90;
	}


	protected boolean canDespawn()
	{
		return canDespawn && getAttackTarget() == null && !isPlatoonLeader();
	}
	protected boolean func_146066_aG()
	{
		return true;
	}

	@Override
	public CampObj getCampObj() {
		return soldiers;
	}

	@Override
	public boolean isThisAttackAbleCamp(CampObj campObj) {
		return campObj == guerrillas;
	}

	@Override
	public boolean isThisFriendCamp(CampObj campObj) {
		return campObj == forPlayer || campObj == soldiers;
	}

	@Override
	public boolean isThisIgnoreSpawnCamp(CampObj campObj) {
		return campObj == forPlayer || campObj == guerrillas;
	}

	@Override
	public void makePlatoon() {
		if (worldObj.isRemote || getPlatoon() != null) return;
		setPlatoon(PlatoonOBJ.create(this));
		enlistPlatoon(false);
	}

	@Override
	public void makePlatoon_OnLoading() {
		if (!worldObj.isRemote) PlatoonOBJ.restore(this);
	}

	@Override
	public void enlistPlatoon(boolean force) {
		if (worldObj.isRemote || platoonOBJ == null || !platoonOBJ.hasRoom()) return;
		for (Object nearby : worldObj.getEntitiesWithinAABB(EntitySoBase.class, boundingBox.expand(18, 7, 18))) {
			EntitySoBase candidate = (EntitySoBase)nearby;
			if (!candidate.isEntityAlive() || !canMoveEntity(candidate) || candidate.getPlatoon() != null
					|| PlatoonOBJ.pending(candidate)) continue;
			candidate.setPlatoon(platoonOBJ);
			if (!platoonOBJ.hasRoom()) break;
		}
	}

}
