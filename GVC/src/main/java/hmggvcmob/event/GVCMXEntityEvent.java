package hmggvcmob.event;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import handmadeguns.items.guns.HMGItem_Unified_Guns;
import hmggvcmob.entity.EntityMGAX55;
import hmggvcmob.entity.IPlatoonable;
import hmggvcmob.entity.util.PlatoonOBJ;
import hmggvcutil.GVCUtils;
import hmggvcmob.entity.guerrilla.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;


import static hmggvcmob.GVCMobPlus.*;

public class GVCMXEntityEvent {
	@SubscribeEvent
	public void leaveChunk(ChunkEvent.Unload event) {
		if (event.world.isRemote) return;
		for (java.util.List section : event.getChunk().entityLists) {
			for (Object entry : section) {
				if (!(entry instanceof IPlatoonable) || !(entry instanceof EntityLiving)) continue;
				PlatoonOBJ platoon = ((IPlatoonable)entry).getPlatoon();
				if (platoon != null) platoon.removeMember((EntityLiving)entry, false);
			}
		}
	}

	@SubscribeEvent
	public void joinWorld(EntityJoinWorldEvent event) {
		if (!event.world.isRemote && event.entity instanceof IPlatoonable && event.entity instanceof EntityLiving) {
			PlatoonOBJ platoon = ((IPlatoonable)event.entity).getPlatoon();
			if (platoon != null) platoon.addMember((EntityLiving)event.entity);
		}
	}

	@SubscribeEvent
	public void targetevent(LivingSetAttackTargetEvent event){
		EntityLivingBase targeted = event.target;
		EntityLivingBase targeting = event.entityLiving;
		if(targeted==null){
			return;
		}
		if(targeted instanceof EntityPlayer) {
			if (targeting instanceof GVCEntityDrawn) {
				((EntityPlayer) targeted).addStat(unmanned_Craft,1);
			}else
			if (targeting instanceof GVCEntityGK) {
				((EntityPlayer) targeted).addStat(war_has_changed,1);
			}else
			if (targeting instanceof GVCEntityGuerrillaSkeleton) {
				((EntityPlayer) targeted).addStat(unending_war,1);
			}
			
			
			if (targeting instanceof EntityGBase) {
				((EntityPlayer) targeted).addStat(No_place_to_HIDE,1);
			}
		}
	}
	
	@SubscribeEvent
	public void deathEvent(LivingDeathEvent event){
		Entity killing = event.source.getEntity();
		EntityLivingBase dieing = event.entityLiving;
		if(dieing instanceof EntityPlayer) {
			if (killing instanceof EntityGBase) {
				((EntityPlayer) dieing).addStat(killedGuerrilla,1);
			}
		}
	}
	
	@SubscribeEvent
	public void pickItemEvent(EntityItemPickupEvent event){
		EntityLivingBase picking = event.entityPlayer;
		EntityItem picked = event.item;
		if(picking==null){
			return;
		}
		if(picked.getEntityItem().getItem() instanceof HMGItem_Unified_Guns) {
			if(picked.getEntityItem().getItem() == GVCUtils.type38) ((EntityPlayer) picking).addStat(Gun_of_the_Lost_Country,1);
		}
	}

	@SubscribeEvent
	public void entitydamaged(LivingHurtEvent event)
	{
		EntityLivingBase entity = event.entityLiving;

		if (VEHICLES_ENABLED && entity.ridingEntity instanceof EntityMGAX55) {
			event.ammount = 0;
			event.setCanceled(true);
		}
	}
}
