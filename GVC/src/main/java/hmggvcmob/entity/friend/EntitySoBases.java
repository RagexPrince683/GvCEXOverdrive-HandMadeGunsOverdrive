package hmggvcmob.entity.friend;

import hmggvcmob.ai.AIPlatoonMovement;
import hmggvcmob.ai.AIInfantryTarget;
import hmggvcmob.ai.AIInfantryWander;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import handmadeguns.Util.GunsUtils;
import handmadeguns.entity.IFF;
import handmadeguns.entity.PlacedGunEntity;
import handmadeguns.items.guns.HMGItem_Unified_Guns;
import handmadevehicle.entity.EntityDummy_rider;
import handmadevehicle.entity.EntityVehicle;
//import handmadevehicle.entity.parts.IDriver;
import handmadevehicle.entity.parts.Modes;
import handmadevehicle.entity.parts.logics.BaseLogic;
import handmadevehicle.entity.parts.turrets.TurretObj;
import handmadevehicle.entity.parts.turrets.WeaponCategory;
import handmadevehicle.entity.prefab.Prefab_Seat;
//import handmadevehicle.entity.prefab.Prefab_Vehicle_Base;
import hmggvcmob.GVCMobPlus;
import hmggvcmob.ai.newai.AIAttackEntityByGun;
import hmggvcmob.ai.newai.AIAttackEntityByTank;
import hmggvcmob.ai.newai.AIAttackManager;
import hmggvcmob.entity.*;
import hmggvcmob.ai.*;
import hmggvcmob.entity.guerrilla.EntityGBase;
import hmggvcmob.entity.guerrilla.EntityGBases;
import hmggvcmob.entity.util.EntityAndPos;
import hmggvcmob.entity.util.MoveToPositionMng;
import hmggvcmob.entity.util.PlatoonOBJ;
import littleMaidMobX.LMM_EntityLittleMaid;
import net.minecraft.block.Block;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

import javax.vecmath.Vector3d;
import java.util.List;

import static handmadeguns.HandmadeGunsCore.HMG_proxy;
import static handmadeguns.HandmadeGunsCore.islmmloaded;
import static handmadeguns.Util.GunsUtils.getmovingobjectPosition_forBlock;
import static handmadevehicle.entity.EntityVehicle.EntityVehicle_spawnByMob;
import static hmggvcmob.GVCMobPlus.*;
import static java.lang.Math.*;

public abstract class EntitySoBases extends EntityCreature implements INpc ,  IFF, IGVCmob, IPlatoonable {
	private EntityBodyHelper_modified bodyHelper;
	public String summoningVehicle = null;

	public MoveToPositionMng moveToPositionMng;


	
	public float viewWide = 1.04f;
	public double movespeed = 0.3d;
	public float spread = 1;
	public EntityAISwimming aiSwimming;
	public AIAttackEntityByGun aiAttackGun;
	public AITargetFlag aiTargetFlag;
	public boolean canDespawn = true;

	public int rideCool = 0;

	public EntitySoBases(World par1World) {
		super(par1World);
		this.bodyHelper = new EntityBodyHelper_modified(this);
		renderDistanceWeight = Double.MAX_VALUE;

		this.moveToPositionMng = new MoveToPositionMng(this);

		this.getNavigator().setBreakDoors(true);
		this.getNavigator().setAvoidsWater(true);
		this.tasks.addTask(0, aiSwimming = new EntityAISwimming(this));
		this.tasks.addTask(2, new AIAttackManager(this));
		if (!(this instanceof hmggvcmob.entity.IIRVING)) this.tasks.addTask(3, new AIattackOnCollide(this));
		this.tasks.addTask(4, new AIPlatoonMovement(this));
		this.targetTasks.addTask(1, new AIInfantryTarget(this));
//		this.tasks.addTask(3, new AIDriveTank(this, null, worldForPathfind));
		this.tasks.addTask(6, new EntityAIOpenDoor(this, true));
		this.tasks.addTask(8, new AIInfantryWander(this, 1.0D));
		this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
		this.tasks.addTask(8, new EntityAILookIdle(this));
		
	}

	protected float func_110146_f(float p_110146_1_, float p_110146_2_)
	{
		if (this.isAIEnabled())
		{
			this.bodyHelper.func_75664_a();
			return p_110146_2_;
		}
		else
		{
			return super.func_110146_f(p_110146_1_, p_110146_2_);
		}
	}

	protected void entityInit() {
		super.entityInit();
	}
	
    public void readEntityFromNBT(NBTTagCompound p_70037_1_)
    {
        super.readEntityFromNBT(p_70037_1_);
	    this.moveToPositionMng.getMoveToPos().set(this.posX,this.posY
			    ,this.posZ);
		seatID = p_70037_1_.getInteger("seatID");
	    if (p_70037_1_.hasKey("canDespawn")) canDespawn = p_70037_1_.getBoolean("canDespawn");
		PlatoonOBJ.read(this, p_70037_1_);
    }
    public void writeEntityToNBT(NBTTagCompound p_70014_1_)
    {
        super.writeEntityToNBT(p_70014_1_);
	    p_70014_1_.setBoolean("canDespawn",canDespawn);
		p_70014_1_.setInteger("seatID", seatID);
		PlatoonOBJ.save(this, p_70014_1_);
    }
	
    public IEntityLivingData onSpawnWithEgg(IEntityLivingData par1EntityLivingData)
    {
        par1EntityLivingData = super.onSpawnWithEgg(par1EntityLivingData);
        {
            this.addRandomArmor();
            this.enchantEquipment();
        }

        this.setCanPickUpLoot(this.rand.nextFloat() < 0.55F * this.worldObj.func_147462_b(this.posX, this.posY, this.posZ));
        return par1EntityLivingData;
    }
	
	
	
	@SideOnly(Side.CLIENT)
    public int getBrightnessForRender(float par1)
    {
        return super.getBrightnessForRender(par1);
    }
    
	public void setDead(){
		if (!worldObj.isRemote && platoonOBJ != null) setPlatoon(null);
		super.setDead();
	}
    public boolean canAttackClass(Class par1Class)
    {
        return EntityCreature.class != par1Class;
    }

    protected String getHurtSound()
    {
        return "gvcmob:gvcmob.pmcHurt";
    }

    protected String getDeathSound()
    {
	    return "gvcmob:gvcmob.pmcDeath";
    }

    protected void func_145780_a(int p_145780_1_, int p_145780_2_, int p_145780_3_, Block p_145780_4_) {
        this.playSound("gvcmob:gvcmob.pmcWalk", 1F, 1.0F);
    }
    
    protected boolean canDespawn()
    {
        return false;
    }
    
    public boolean isAIEnabled()
    {
        return true;
    }

	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
	}

	public void addRandomArmor() {
	}

	public boolean isCreatureType(EnumCreatureType type, boolean forSpawnCount)
	{
		return type.getCreatureClass() == EntitySoBases.class;
	}
	
	public Vec3 getLookVec()
	{
		return this.getLook(1.0F);
	}

	/**
	 * interpolated look vector
	 */
	public Vec3 getLook(float p_70676_1_)
	{
		float f1;
		float f2;
		float f3;
		float f4;

		if (p_70676_1_ == 1.0F)
		{
			f1 = MathHelper.cos(-this.rotationYawHead * 0.017453292F - (float)Math.PI);
			f2 = MathHelper.sin(-this.rotationYawHead * 0.017453292F - (float)Math.PI);
			f3 = -MathHelper.cos(-this.rotationPitch * 0.017453292F);
			f4 = MathHelper.sin(-this.rotationPitch * 0.017453292F);
			return Vec3.createVectorHelper((double)(f2 * f3), (double)f4, (double)(f1 * f3));
		}
		else
		{
			f1 = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * p_70676_1_;
			f2 = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * p_70676_1_;
			f3 = MathHelper.cos(-f2 * 0.017453292F - (float)Math.PI);
			f4 = MathHelper.sin(-f2 * 0.017453292F - (float)Math.PI);
			float f5 = -MathHelper.cos(-f1 * 0.017453292F);
			float f6 = MathHelper.sin(-f1 * 0.017453292F);
			return Vec3.createVectorHelper((double)(f4 * f5), (double)f6, (double)(f3 * f5));
		}
	}
	private int reArmCnt;
	public void onUpdate() {
		if (!worldObj.isRemote) {
			PlatoonOBJ.restore(this);
			hmggvcutil.GVCUtils.migrateNpcGun(this);
			if (platoonOBJ != null) platoonOBJ.update(this);
		}

		if(currentMainWeapon != null && currentMainWeapon.linkedBaseLogic.mc_Entity != null){
			if(getAttackTarget() == null){
				aimPos = null;
				if(reArmCnt > 0)reArmCnt--;
			}else {
				reArmCnt = 20;
			}

			if(reArmCnt <= 0) {
				//Prefab_Vehicle_Base prefab_vehicle = ((EntityVehicle) currentMainWeapon.linkedBaseLogic.mc_Entity).getBaseLogic().prefab_vehicle;
				//for (int slotID = 0; slotID < prefab_vehicle.weaponSlotNum; slotID++) {
				//	if (prefab_vehicle.weaponSlot_linkedTurret_StackWhiteList.get(slotID) != null) {
				//		int randUsingSlot = rand.nextInt(prefab_vehicle.weaponSlot_linkedTurret_StackWhiteList.get(slotID).length);
				//		String whiteList = prefab_vehicle.weaponSlot_linkedTurret_StackWhiteList.get(slotID)[randUsingSlot];
//				//			System.out.println("" + whiteList);
				//		Item check = GameRegistry.findItem("HandmadeGuns", whiteList);
				//		if (check instanceof HMGItem_Unified_Guns && ((HMGItem_Unified_Guns) check).gunInfo.guerrila_can_use) {
				//			((EntityVehicle) currentMainWeapon.linkedBaseLogic.mc_Entity).getBaseLogic().inventoryVehicle.setInventorySlotContents(slotID, new ItemStack(check));
				//		}
				//	} else {
				//		int randUsingSlot = rand.nextInt(GVCMobPlus.Guns_CanUse.size());
				//		Item choosenGun = GVCMobPlus.Guns_CanUse.get(randUsingSlot);
				//		((EntityVehicle) currentMainWeapon.linkedBaseLogic.mc_Entity).getBaseLogic().inventoryVehicle.setInventorySlotContents(slotID, new ItemStack(choosenGun));
				//	}
				//}
				reArmCnt = 20;
			}
		}

		super.onUpdate();
		if(VEHICLES_ENABLED && !worldObj.isRemote && summoningVehicle != null) {

			int var12 = MathHelper.floor_double((rotationYaw - 22.5)*8 / 360.0F)*45 + 45;
//			System.out.println(summoningVehicle);
			EntityVehicle bespawningEntity = EntityVehicle_spawnByMob(worldObj,summoningVehicle);
			bespawningEntity.noDrop = true;
			bespawningEntity.setLocationAndAngles(this.posX, this.posY, this.posZ, var12 , 0.0F);if((this.worldObj.canBlockSeeTheSky(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY), MathHelper.floor_double(this.posZ)))) {
				if(bespawningEntity.pickupEntity(this,0)){
					this.setCurrentItemOrArmor(0,null);
				}
				bespawningEntity.canUseByMob = true;
				bespawningEntity.canDespawn = canDespawn;
				//Prefab_Vehicle_Base prefab_vehicle = bespawningEntity.getBaseLogic().prefab_vehicle;
				//for(int slotID = 0 ;slotID < prefab_vehicle.weaponSlotNum;slotID++) {
				//	if(prefab_vehicle.weaponSlot_linkedTurret_StackWhiteList.get(slotID) != null) {
				//		int randUsingSlot = rand.nextInt(prefab_vehicle.weaponSlot_linkedTurret_StackWhiteList.get(slotID).length);
				//		String whiteList = prefab_vehicle.weaponSlot_linkedTurret_StackWhiteList.get(slotID)[randUsingSlot];
//				//		System.out.println("" + whiteList);
				//		Item check = GameRegistry.findItem("HandmadeGuns", whiteList);
				//		if (check instanceof HMGItem_Unified_Guns && ((HMGItem_Unified_Guns) check).gunInfo.guerrila_can_use) {
				//			bespawningEntity.getBaseLogic().inventoryVehicle.setInventorySlotContents(slotID, new ItemStack(check));
				//		}
				//	}else{
				//		int randUsingSlot = rand.nextInt(GVCMobPlus.Guns_CanUse.size());
				//		Item choosenGun = GVCMobPlus.Guns_CanUse.get(randUsingSlot);
				//		bespawningEntity.getBaseLogic().inventoryVehicle.setInventorySlotContents(slotID, new ItemStack(choosenGun));
				//	}
				//}
				//if(!prefab_vehicle.T_Land_F_Plane) {
				//	bespawningEntity.setLocationAndAngles(this.posX, this.posY + 128, this.posZ, var12, 0.0F);
				//	bespawningEntity.getBaseLogic().throttle = prefab_vehicle.throttle_Max;
				//}
				if(!bespawningEntity.checkObstacle())bespawningEntity.setDead();
				for (int cnt = 0;cnt < bespawningEntity.getBaseLogic().riddenByEntities.length;cnt++) {
					GVCEntitySoldier entity = new GVCEntitySoldier(worldObj);
					entity.setCanDespawn(canDespawn);
					entity.copyLocationAndAnglesFrom(this);
					entity.onSpawnWithEgg(null);
					if(bespawningEntity.pickupEntity(entity,cnt)) {
						Prefab_Seat sittingSeat = bespawningEntity.getBaseLogic().seatObjects[((EntityDummy_rider)entity.ridingEntity).linkedSeatID].prefab_seat;
						if(sittingSeat.isBlindedSeat || sittingSeat.hasGun){
							this.setCurrentItemOrArmor(0,null);
						}
						worldObj.spawnEntityInWorld(entity);
					}
				}
				for(TurretObj turretObj : bespawningEntity.getBaseLogic().allturrets){
					if(turretObj.gunItem != null && turretObj.gunStack != null){
						turretObj.gunItem.checkTags(turretObj.gunStack);
						turretObj.gunItem.resetReload(turretObj.gunStack,worldObj,this,0);
					}
				}
				worldObj.spawnEntityInWorld(bespawningEntity);
			}
			summoningVehicle = null;
		}
		if(!worldObj.isRemote && this.getAttackTarget() != null && (this.getAttackTarget().isDead || is_this_entity_friend(this.getAttackTarget())))this.setAttackTarget(null);
		if(this.getAttackTarget() != null && (this.getAttackTarget() instanceof EntitySoBases || this.getAttackTarget() instanceof EntityPlayer || (islmmloaded && this.getAttackTarget() instanceof LMM_EntityLittleMaid)))this.setAttackTarget(null);
		if(this.getHeldItem() != null) {
			if(this.getHeldItem().hasTagCompound()){
				if(this.getHeldItem().getItem() instanceof HMGItem_Unified_Guns)((HMGItem_Unified_Guns)this.getHeldItem().getItem()).checkTags(this.getHeldItem());
			}
			float backRP = rotationPitch;
			float backRY = rotationYaw;
			this.getHeldItem().getItem().onUpdate(this.getHeldItem(), worldObj, this, 0, true);
			rotationPitch = backRP;
			rotationYaw = backRY;
			if (this.getEntityData().getBoolean("HMGisUsingItem")) {
				this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(movespeed / 4);
			} else {
				this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(movespeed);
			}
		}
		if(ridingEntity instanceof PlacedGunEntity){
			if(this.getEntityData().getBoolean("HMGisUsingItem")){
				((PlacedGunEntity) ridingEntity).firing = true;
			}else {
				((PlacedGunEntity) ridingEntity).firing = false;
			}
		}
		if(ridingEntity == null){
			rideCool --;
		}
		this.getEntityData().setBoolean("HMGisUsingItem",false);

		moveToPositionMng.update();

	}


	public void onLivingUpdate()
	{
		this.updateArmSwingProgress();
		float f = this.getBrightness(1.0F);

		if (f > 0.5F)
		{
			this.entityAge += 2;
		}
		
		super.onLivingUpdate();
	}
	
	protected void updateAITasks()
	{
		super.updateAITasks();
	}
	public float moveToDir = 0;
	public void setAIMoveSpeed(float p_70659_1_)
	{
		super.setAIMoveSpeed(p_70659_1_);
		this.setMoveForward(p_70659_1_);
		moveToDir = this.rotationYaw;
	}
	public void moveEntityWithHeading(float p_70612_1_, float p_70612_2_){
		this.rotationYaw = moveToDir;
		if(!worldObj.isRemote){
			super.moveEntityWithHeading(p_70612_1_,p_70612_2_);
		}else {
			double motionXBackUp = motionX;
			double motionYBackUp = motionY;
			double motionZBackUp = motionZ;
			double posXBackUp = posX;
			double posYBackUp = posY;
			double posZBackUp = posZ;
			super.moveEntityWithHeading(p_70612_1_,p_70612_2_);
			motionX = motionXBackUp;
			motionY = motionYBackUp;
			motionZ = motionZBackUp;
			posX = posXBackUp;
			posY = posYBackUp;
			posZ = posZBackUp;
		}
	}
	
	public void moveFlying(float p_70060_1_, float p_70060_2_, float p_70060_3_)
	{
		float f3 = p_70060_1_ * p_70060_1_ + p_70060_2_ * p_70060_2_;

		if (f3 >= 1.0E-4F)
		{
			f3 = MathHelper.sqrt_float(f3);

			if (f3 < 1.0F)
			{
				f3 = 1.0F;
			}
			f3 = p_70060_3_ / f3;
			f3 = abs(f3);
			p_70060_1_ *= f3;
			p_70060_2_ *= f3;
			float f4 = MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F);
			float f5 = MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F);
			this.motionX += (double)(-p_70060_2_ * f4 + p_70060_1_ * f5);
			this.motionZ += (double)( p_70060_2_ * f5 + p_70060_1_ * f4);
		}
	}
	
	public boolean shouldDismountInWater(Entity entity){
		return false;
	}


    public void setVehicleName(String string) {
        summoningVehicle = string;
    }

    @Override
	public boolean is_this_entity_friend(Entity entity) {
		if(entity instanceof EntityPlayer){
			return true;
		}else if(entity instanceof EntitySoBases) {
			return true;
		}else if(islmmloaded && entity instanceof LMM_EntityLittleMaid){
			return true;
		}
		return false;
	}
	
	@Override
	public float getviewWide() {
		return viewWide;
	}

	@Override
	public boolean canSeeTarget(Entity target) {
		if(VEHICLES_ENABLED && this.ridingEntity instanceof EntityDummy_rider){
			//if(!((EntityDummy_rider) this.ridingEntity).linkedBaseLogic.prefab_vehicle.T_Land_F_Plane)return true;
		}
		boolean flag;
		flag = canhearsound(target);
		if (!flag) {
			Vec3 lookVec = getLookVec();
			Vec3 toTGTvec = Vec3.createVectorHelper(target.posX - posX, target.posY + target.getEyeHeight() - (posY + getEyeHeight()), target.posZ - posZ);
			toTGTvec = toTGTvec.normalize();
			double angle = acos(lookVec.dotProduct(toTGTvec));
		return angle < getviewWide();
		}else
			return true;
	}

	@Override
	public boolean canEntityBeSeen(Entity p_70685_1_)
	{

		if(VEHICLES_ENABLED && ridingEntity instanceof EntityDummy_rider){
			BaseLogic connected = ((EntityDummy_rider) ridingEntity).linkedBaseLogic;
			if(connected.seatObjects[((EntityDummy_rider) ridingEntity).linkedSeatID].prefab_seat.isBlindedSeat)return false;
		}

		Vec3 vec3 = Vec3.createVectorHelper(this.posX, this.posY + this.getEyeHeight(), this.posZ);
		Vec3 vec31 = Vec3.createVectorHelper(p_70685_1_.posX, p_70685_1_.posY + p_70685_1_.getEyeHeight(), p_70685_1_.posZ);

		return GunsUtils.getMovingObjectPosition_forBlock_CheckEmpty(worldObj,vec3, vec31,10,new Block[] {Blocks.glass,Blocks.glass_pane,Blocks.stained_glass,Blocks.stained_glass_pane})
				&& (p_70685_1_ == getAttackTarget() || getDistanceSqToEntity(p_70685_1_) <= 16 || canSeeTarget(p_70685_1_));
	}

	@Override
	public boolean canhearsound(Entity target) {
		boolean flag;
		double dist = getDistanceToEntity(target);
		flag = dist < hmggvcmob.util.GVCUtil.audibleGunshotLevel(target) * 14;
		return flag;
	}

	@Override
	public void setCanDespawn(boolean canDespawn) {
		this.canDespawn = canDespawn;
	}

	public boolean canuseAlreadyPlacedGun = true;
	public int placing;
	protected void collideWithNearbyEntities()
	{
		if(!worldObj.isRemote) {
			List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox.expand(0.20000000298023224D, 0.0D, 0.20000000298023224D));

			if (list != null && !list.isEmpty()) {
				for (int i = 0; i < list.size(); ++i) {
					Entity entity = (Entity) list.get(i);

					if (entity.canBePushed()) {
						this.collideWithEntity(entity);
					}

					if (this.ridingEntity == null && rideCool < 0 && !entity.isDead) {
						if (VEHICLES_ENABLED && entity instanceof EntityVehicle && ((EntityVehicle) entity).canUseByMob) {
							Entity pilot = ((EntityVehicle) entity).getBaseLogic().getRiddenEntityList()[((EntityVehicle) entity).getpilotseatid()];
							if ((pilot == null || is_this_entity_friend(pilot)) && !((EntityVehicle) entity).getBaseLogic().isRidingEntity(this)) {
//                            System.out.println("" + );
								rideCool = 200;
								((EntityVehicle) entity).pickupEntity(this, seatID);
							}
							((EntityVehicle) entity).getBaseLogic().health += 0.01;
						}else {
							if(cfg_guerrillacanusePlacedGun && canuseAlreadyPlacedGun && !worldObj.isRemote && this.getAttackTarget() != null) {
								if (entity.riddenByEntity == null && entity instanceof PlacedGunEntity) {//TODO プレイヤー指示で搭乗するようにしたい
									placing++;
									if (placing > 60) {
										placing = 0;
										rideCool = 200;
										this.mountEntity((PlacedGunEntity) entity);
										if (ridingEntity == entity) setCurrentItemOrArmor(0, null);
									}
								}
							}
						}
					}
					if(entity instanceof EntityLivingBase && is_this_entity_friend(entity)){
						((EntityLivingBase) entity).heal(1f);
					}
				}
			}
		}
	}

	public int seatID = 0;
	public WeaponCategory currentMainWeapon;
	public WeaponCategory currentSubWeapon;
    public void setSeatID(int id) {
        seatID = id;
    }

    public void setWeaponMain(WeaponCategory turret) {
        currentMainWeapon = turret;
    }

    public void setWeaponSub(WeaponCategory turret) {
        currentSubWeapon = turret;
    }

    public int getSeatID() {
        return seatID;
    }

    public WeaponCategory getWeaponMain() {
        return currentMainWeapon;
    }

    public WeaponCategory getWeaponSub() {
        return currentSubWeapon;
    }


	protected void despawnEntity()
	{
		Event.Result result = null;
		if (this.isNoDespawnRequired())
		{
			this.entityAge = 0;
		}
		else if ((this.entityAge & 0x1F) == 0x1F && (result = ForgeEventFactory.canEntityDespawn(this)) != Event.Result.DEFAULT)
		{
			if (result == Event.Result.DENY || this.ridingEntity instanceof EntityDummy_rider)
			{
				this.entityAge = 0;
			}
			else
			{
				this.setDead();
			}
		}
		else
		{
			EntityPlayer entityplayer = this.worldObj.getClosestPlayerToEntity(this, -1.0D);

			if (entityplayer != null)
			{
				double d0 = entityplayer.posX - this.posX;
				double d1 = entityplayer.posY - this.posY;
				double d2 = entityplayer.posZ - this.posZ;
				double d3 = d0 * d0 + d2 * d2;

				if (this.canDespawn() && d3 > 16384.0D)
				{
					if(VEHICLES_ENABLED && this.ridingEntity instanceof EntityDummy_rider){
						((EntityDummy_rider) this.ridingEntity).linkedBaseLogic.mc_Entity.despawnEntity();
					}
					this.setDead();
				}
			}
		}
	}



	PlatoonOBJ platoonOBJ;
    EntityAndPos myPos;

	@Override
	public void setPlatoon(PlatoonOBJ platoon) {
		if (worldObj.isRemote || (isDead && platoon != null) || platoonOBJ == platoon) return;
		if (platoonOBJ != null) platoonOBJ.removeMember(this, true);
		platoonOBJ = platoon;
		myPos = null;
		if (platoonOBJ != null) platoonOBJ.addMember(this);
	}

	@Override
	public PlatoonOBJ getPlatoon() {
		return this.platoonOBJ;
	}

	@Override
	public void setPosObj(EntityAndPos entityAndPos) {
		myPos = entityAndPos;
	}

	@Override
	public double[] getTargetpos() {
		return new double[]{platoonOBJ.PlatoonTargetPos.x,platoonOBJ.PlatoonTargetPos.y,platoonOBJ.PlatoonTargetPos.z};
	}

	@Override
	public Vector3d getMoveToPos() {
		return myPos == null ? null : myPos.getPos();
	}
	Vector3d aimPos;
	public void setAimPos(Vector3d aimPos){
		this.aimPos = aimPos;
	}
	public Vector3d getAimPos(){
		return this.aimPos;
	}

	BaseLogic linkedLogic;
	public void setLinkedVehicle(BaseLogic baseLogic) {
		linkedLogic = baseLogic;
	}

	public BaseLogic getLinkedVehicle() {
		return linkedLogic;
	}

	@Override
	public void extraprocessInMGFire(){
		if(ridingEntity instanceof PlacedGunEntity && getAimPos() != null){
			double[] yp = ((PlacedGunEntity) ridingEntity).getAngleToTarget(getAimPos());
			this.rotationPitch = (float) yp[1];
			this.rotationYaw = (float) yp[0];
			this.rotationYawHead = (float) yp[0];
		}
	}


	@Override
	public MoveToPositionMng getMoveToPositionMng() {
		return moveToPositionMng;
	}

	public void setLocationAndAngles(double posX,double posY,double posZ,float yaw,float pitch){
		super.setLocationAndAngles(posX,posY,posZ,yaw,pitch);
		moveToPositionMng.getMoveToPos().set(posX,posY,posZ);
	}
}
