package handmadeguns.items.guns;

import com.google.common.collect.Multimap;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import handmadeguns.HMGPacketHandler;
import handmadeguns.HandmadeGunsCore;
import handmadeguns.animation.ReloadAnimationBridge;
import handmadeguns.Util.GunsUtils;
import handmadeguns.Util.StackAndSlot;
import handmadeguns.entity.HMGEntityLaser;
import handmadeguns.entity.HMGEntityLight;
import handmadeguns.entity.PlacedGunEntity;
import handmadeguns.entity.bullets.*;
import handmadeguns.compat.HMGPointOfAimBridge;
import handmadeguns.event.AmmoHUDRenderer;
import handmadeguns.items.*;
import handmadeguns.network.*;
import handmadevehicle.Utils;
import handmadevehicle.entity.EntityDummy_rider;
import handmadevehicle.entity.EntityVehicle;
import handmadevehicle.entity.parts.turrets.TurretObj;
import littleMaidMobX.LMM_EntityLittleMaid;
import littleMaidMobX.LMM_IEntityLittleMaidAvatarBase;
import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import javax.script.Invocable;
import javax.script.ScriptException;
import javax.vecmath.Vector3d;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static handmadeguns.HandmadeGunsCore.islmmloaded;
import static handmadeguns.HandmadeGunsCore.HMG_proxy;
import static handmadevehicle.Utils.*;
import static java.lang.Math.*;
import static java.lang.StrictMath.toRadians;
import static net.minecraft.util.MathHelper.wrapAngleTo180_double;
import static net.minecraft.util.MathHelper.wrapAngleTo180_float;
import static net.minecraft.world.World.MAX_ENTITY_RADIUS;

public class HMGItem_Unified_Guns extends Item {
	private static final String PER_SHELL_INTERRUPT_TIME_TAG = "PerShellInterruptTime";
	private static final String PER_SHELL_PHASE_TAG = "PerShellReloadPhase";
	private static final String PER_SHELL_INTRO_TIME_TAG = "PerShellReloadIntroTime";
	private static final String PER_SHELL_PRESENTATION_ACTIVE_TAG = "PerShellPresentationActive";
	private static final String PER_SHELL_STARTED_EMPTY_TAG = "PerShellStartedEmpty";
	private static final int PER_SHELL_PHASE_NONE = 0;
	private static final int PER_SHELL_PHASE_INTRO = 1;
	private static final int PER_SHELL_PHASE_INSERT = 2;
	private static final int DEFAULT_PER_SHELL_INTERRUPT_TICKS = 4;


	//the main GUN item class.

	public GunInfo gunInfo = new GunInfo();
	public GunTemp guntemp = new GunTemp();
	public FireTemp firetemp;
	public HMGItem_Unified_Guns(){
	}

	/** Creative-tab stacks carry a real, full magazine state before the first tick. */
	@Override
	public void getSubItems(Item item, CreativeTabs tab, List stacks) {
		ItemStack stack = new ItemStack(item, 1, 0);
		checkTags(stack);
		initializeInfiniteAmmo(stack);
		stacks.add(stack);
	}

	//oh my god this class is fucking spaghetti hell



	public void addInformation(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4){
		checkTags(par1ItemStack);
		if (gunInfo.techYear != null) {
			par3List.add(EnumChatFormatting.GRAY + "Introduced: " + gunInfo.techYear);
		}
		if (gunInfo.techYear != null || gunInfo.techTierHalfSteps >= 0) {
			float requiredTier = handmadeguns.tech.HMGTechTierManager.resolveRequiredTier(gunInfo);
			par3List.add(EnumChatFormatting.GRAY + "Required Tech Tier: " + requiredTier
					+ (gunInfo.techTierHalfSteps >= 0 ? " (override)" : ""));
			if (par2EntityPlayer != null && !handmadeguns.tech.HMGTechTierManager.isUnlocked(
					par1ItemStack, par2EntityPlayer, par2EntityPlayer.worldObj)) {
				par3List.add(EnumChatFormatting.RED + "LOCKED — Server Tech Tier: "
						+ handmadeguns.tech.HMGTechTierManager.getUnlockedTier(par2EntityPlayer.worldObj));
			}
		}
		{
			String powor = String
					.valueOf(gunInfo.power + EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, par1ItemStack));
			String speed = String.valueOf(gunInfo.speed);
			String bure = String.valueOf(gunInfo.spread_setting);
			String recoil = String.valueOf(gunInfo.recoil);
			String exp = String.valueOf(gunInfo.ex);
			String rpm = String.valueOf(gunInfo.rpm);
			String damagerange = String.valueOf(gunInfo.damagerange);
			NBTTagCompound nbt = par1ItemStack.getTagCompound();
			int selecting = nbt.getInteger("get_selectingMagazine");
			if (selecting >= gunInfo.reloadTimes.length) selecting = 0;
			String retime = String.valueOf(gunInfo.reloadTimes[selecting]);
			String nokori = String.valueOf(getMaxDamage() - par1ItemStack.getItemDamage());

			par3List.add(EnumChatFormatting.RED + "Magazine Rounds " + StatCollector.translateToLocal(nokori));
			par3List.add(EnumChatFormatting.WHITE + "Damage " + "+" + StatCollector.translateToLocal(powor));
			par3List.add(EnumChatFormatting.WHITE + "Bullet Speed " + "+" + StatCollector.translateToLocal(speed));
			par3List.add(EnumChatFormatting.WHITE + "Bullet Spread " + "+" + StatCollector.translateToLocal(bure));
			par3List.add(EnumChatFormatting.WHITE + "Recoil " + "+" + StatCollector.translateToLocal(recoil));
			par3List.add(EnumChatFormatting.YELLOW + "Reload Time " + "+" + StatCollector.translateToLocal(retime));
			// Show explosive radius separately from BulletPower damage.
			if (gunInfo.ex > 0.0f) {
				par3List.add(EnumChatFormatting.RED + "Explosion Radius " + "+" + StatCollector.translateToLocal(exp));
			}
			if (gunInfo.damagerange > 0.0f) { //I guess this works as a non null check too idk im sure it's fine
				par3List.add(EnumChatFormatting.RED + "Damage Range " + "+" + StatCollector.translateToLocal(damagerange));
			}
			// par3List.add(EnumChatFormatting.YELLOW + "MagazimeType " +
			// StatCollector.translateToLocal("ARMagazine"));
			if (!(gunInfo.scopezoombase == 1.0f)) {
				//todo: traceback
				String scopezoom = String.valueOf(gunInfo.scopezoombase);
				par3List.add(EnumChatFormatting.WHITE + "Scope Zoom " + "x" + StatCollector.translateToLocal(scopezoom));
			}
			if (gunInfo.needfix && gunInfo.canfix) {
				par3List.add(EnumChatFormatting.WHITE + "You cannot shoot this handheld.");
				par3List.add(EnumChatFormatting.WHITE + "Press " + HMG_proxy.getFixkey() + " while pointing at a block.");
			} else if (gunInfo.canfix) {
				par3List.add(EnumChatFormatting.WHITE + "You can place this on a block.");
				par3List.add(EnumChatFormatting.WHITE + "Press " + HMG_proxy.getFixkey() + " while pointing at a block.");
			} else if (gunInfo.needfix) {
				par3List.add(EnumChatFormatting.WHITE + "You can not shoot this gun by hand.");
				par3List.add(EnumChatFormatting.WHITE + "You can use this gun with a vehicle.");
			}

			if (!gunInfo.rates.isEmpty() && gunInfo.rates.get(0) > 0) {
				float delayTicks = gunInfo.rates.get(0); // primary fire mode
				gunInfo.rpm = (int)(1200.0f / delayTicks);
				par3List.add(EnumChatFormatting.GRAY + "RPM " + "+" + StatCollector.translateToLocal(rpm));
			} else {
				gunInfo.rpm = 0;
			}

		}{
			if (gunInfo.magazine != null) {
				par3List.add(EnumChatFormatting.YELLOW + "Ammunition:");
				for (Item magazine : gunInfo.magazine)
					if(magazine != null)par3List.add(EnumChatFormatting.WHITE + magazine.getItemStackDisplayName(new ItemStack(magazine)));
			}
			if(!gunInfo.attachwhitelist.isEmpty()){
				par3List.add(EnumChatFormatting.YELLOW + "Valid attachments:");
				for (String attachment : gunInfo.attachwhitelist)
					par3List.add(EnumChatFormatting.WHITE + getAttachmentDisplayName(attachment));

			}
		}
	}
	private String getAttachmentDisplayName(String attachmentName) {
		Item attachment = GameRegistry.findItem("HandmadeGuns", attachmentName);
		if (attachment != null) {
			return attachment.getItemStackDisplayName(new ItemStack(attachment));
		}
		return attachmentName;
	}
	public void onUpdate_fromTurret(ItemStack itemstack, World world, Entity entity, int i, boolean flag, TurretObj turretObj){
		synchronized (this) {
			guntemp = new GunTemp();
			guntemp.currentConnectedTurret = turretObj;
			checkTags(itemstack);
			NBTTagCompound nbt = itemstack.getTagCompound();
			guntemp.connectedTurret = nbt.getBoolean("IsTurretStack");
			nbt.setBoolean("HMGfixed", true);
			try {
				gunProcess(itemstack, world, entity, i, flag);
			} catch (Exception e) {
				e.printStackTrace();
			}
			nbt.setBoolean("HMGfixed", false);
		}
	}
	public void onUpdate(ItemStack itemstack, World world, Entity entity, int i, boolean flag){
		if (gunInfo.bedrockPresentation && !flag && itemstack.hasTagCompound()) {
			itemstack.getTagCompound().removeTag("HMGSprintRecovery");
			itemstack.getTagCompound().removeTag("HMGSprintTrigger");
		}

		synchronized (this) {
			guntemp = new GunTemp();
			guntemp.invocable = (Invocable) gunInfo.script;

			gunProcess(itemstack, world, entity, i, flag);
		}
	}

	//helpers
	private int findBestMagazineIndex(EntityPlayer player, ItemStack gunStack) {
		if (player == null || gunStack == null) return -1;
		if (this.gunInfo == null || this.gunInfo.magazine == null) return -1;

		int len = this.gunInfo.magazine.length;
		if (len == 0) return -1;

		int bestIndex = -1;
		int bestRounds = 0;

		// Get loaded mags from the gun (these are ItemStack[])
		ItemStack[] loadedMags = this.get_loadedMagazineStack(gunStack);

		// Walk through each magazine type index and sum rounds from loaded + player inventory
		for (int idx = 0; idx < len; idx++) {
			Object magDescriptor = this.gunInfo.magazine[idx];
			Item magItemCandidate = extractItemFromDescriptor(magDescriptor);

			int totalRoundsForThisType = 0;

			// 1) Count rounds in loaded magazines that match this type
			if (loadedMags != null) {
				for (ItemStack loaded : loadedMags) {
					if (loaded == null) continue;
					if (itemsMatchDescriptor(loaded, magDescriptor, magItemCandidate)) {
						// your HUD uses (maxDamage - itemDamage) * stackSize -> replicate that
						int perStack = (loaded.getMaxDamage() - loaded.getItemDamage());
						totalRoundsForThisType += perStack * loaded.stackSize;
					}
				}
			}

			// 2) Count rounds in player's inventory (mainInventory)
			InventoryPlayer inv = player.inventory;
			if (inv != null && inv.mainInventory != null) {
				for (ItemStack stack : inv.mainInventory) {
					if (stack == null) continue;
					if (itemsMatchDescriptor(stack, magDescriptor, magItemCandidate)) {
						int perStack = (stack.getMaxDamage() - stack.getItemDamage());
						totalRoundsForThisType += perStack * stack.stackSize;
					}
				}
			}

			// pick best
			if (totalRoundsForThisType > bestRounds) {
				bestRounds = totalRoundsForThisType;
				bestIndex = idx;
			}
		}

		return bestIndex;
	}

	/** Try to pull an Item out of the magazine descriptor. If descriptor is an Item or ItemStack we return the Item; otherwise null. */
	private Item extractItemFromDescriptor(Object magDescriptor) {
		try {
			if (magDescriptor instanceof Item) {
				return (Item) magDescriptor;
			} else if (magDescriptor instanceof ItemStack) {
				return ((ItemStack) magDescriptor).getItem();
			} else if (magDescriptor instanceof String) {
				// no direct Item mapping available here
				return null;
			}
		} catch (Throwable t) { }
		return null;
	}

	/** Robust matching: if we have the concrete Item, use it; otherwise try to match by unlocalized name or toString fallback. */
	private boolean itemsMatchDescriptor(ItemStack stack, Object magDescriptor, Item magItemCandidate) {
		if (stack == null) return false;
		if (magItemCandidate != null) {
			return stack.getItem() == magItemCandidate;
		}

		// If descriptor is ItemStack, compare items
		if (magDescriptor instanceof ItemStack) {
			try {
				return stack.getItem() == ((ItemStack) magDescriptor).getItem();
			} catch (Throwable t) { }
		}

		// If descriptor is a String identifier, try to match unlocalized name or display name
		if (magDescriptor instanceof String) {
			String id = (String) magDescriptor;
			try {
				if (stack.getUnlocalizedName() != null && stack.getUnlocalizedName().toLowerCase().contains(id.toLowerCase())) return true;
				if (stack.getDisplayName() != null && stack.getDisplayName().toLowerCase().contains(id.toLowerCase())) return true;
			} catch (Throwable t) { }
		}

		// Last-ditch fallback: check if the stack's unlocalized name contains "mag" (crude)
		try {
			String u = stack.getUnlocalizedName();
			if (u != null && u.toLowerCase().contains("mag")) return true;
		} catch (Throwable ignored) { }

		return false;
	}

	public void gunProcess(ItemStack itemstack, World world, Entity entity, int i, boolean flag){
		try {
			if (islmmloaded && entity instanceof LMM_IEntityLittleMaidAvatarBase) {
				return;
			}
			if (entity != null && flag) {
				if (HandmadeGunsCore.cfg_Flash) {
					int xTile = (int) entity.lastTickPosX - 1;
					int yTile = (int) entity.lastTickPosY - 1;
					int zTile = (int) entity.lastTickPosZ - 1;
//					world.func_147451_t(xTile, yTile, zTile);
//					world.func_147451_t(xTile - 1, yTile, zTile);
//					world.func_147451_t(xTile + 1, yTile, zTile);
//					world.func_147451_t(xTile, yTile - 1, zTile);
//					world.func_147451_t(xTile, yTile + 1, zTile);
//					world.func_147451_t(xTile, yTile, zTile - 1);
//					world.func_147451_t(xTile, yTile, zTile + 1);
				}
				checkTags(itemstack);
				if (!world.isRemote && handmadeguns.Util.HMGAmmoPolicy.hasInfiniteAmmo(entity)) {
					initializeInfiniteAmmo(itemstack);
				}
				NBTTagCompound nbt = itemstack.getTagCompound();



				guntemp.readPropertyFromNBT(gunInfo,nbt,HandmadeGunsCore.Key_ADS(entity) || guntemp.connectedTurret,world);
//                if (guntemp.connectedTurret = nbt.getBoolean("IsTurretStack") && guntemp.currentConnectedTurret == null) {
//                    return;
//                }
//                guntemp.tempspread = gunInfo.spread_setting;
//                if (HandmadeGunsCore.Key_ADS(entity)) {
//                    guntemp.tempspread = guntemp.tempspread * gunInfo.ads_spread_cof;
//                }
//                guntemp.tempspreadDiffusion = nbt.getFloat("Diffusion");
//                if (guntemp.tempspreadDiffusion > gunInfo.spreadDiffusionMax)
//                    guntemp.tempspreadDiffusion = gunInfo.spreadDiffusionMax;
//                guntemp.tempspreadDiffusion -= gunInfo.spreadDiffusionReduceRate;
//                if (guntemp.tempspreadDiffusion < gunInfo.spreadDiffusionmin)
//                    guntemp.tempspreadDiffusion = gunInfo.spreadDiffusionmin;
//                guntemp.tempspread += gunInfo.spread_setting * guntemp.tempspreadDiffusion;
//
//
//                guntemp.sound = gunInfo.soundbase;
//                guntemp.soundlevel = gunInfo.soundbaselevel;
//                guntemp.muzzle = gunInfo.muzzleflash;
//                guntemp.selectingMagazine = nbt.getInteger("get_selectingMagazine");
//                guntemp.currentMgazine = nbt.getInteger("getcurrentMagazine");
//
//
//                guntemp.islockingentity = nbt.getBoolean("islockedentity");
//                guntemp.TGT = world.getEntityByID(nbt.getInteger("TGT"));
//                guntemp.islockingblock = nbt.getBoolean("islockedblock");
//                guntemp.LockedPosX = nbt.getInteger("LockedPosX");
//                guntemp.LockedPosY = nbt.getInteger("LockedPosY");
//                guntemp.LockedPosZ = nbt.getInteger("LockedPosZ");
//                guntemp.currentElevation = nbt.getInteger("currentElevation");
//                guntemp.selector = nbt.getInteger("HMGMode");


				if (i != -10) nbt.setBoolean("IsTurretStack", false);


				gunInfo.posGetter.curretnSightPos = gunInfo.sightPosN;//tempへの移動めんD

				bindattaches(itemstack, world, entity);
				boolean canFixflag = gunInfo.canfix;
				try {
					if (guntemp.items != null && guntemp.items[4] != null && guntemp.items[4].getItem() instanceof HMGItemAttachment_grip) {
						canFixflag |= ((HMGItemAttachment_grip) guntemp.items[4].getItem()).isbase;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (!world.isRemote && guntemp.currentConnectedTurret == null) {
					float walkedDist = entity.distanceWalkedModified - nbt.getFloat("prevdistanceWalkedModified");
					float headShakeDist;
					if (entity instanceof EntityLivingBase) {
						headShakeDist = abs(wrapAngleTo180_float(nbt.getFloat("prevRotationYawHead") - entity.getRotationYawHead()))
								+ abs(wrapAngleTo180_float(nbt.getFloat("prevRotationPitch") - entity.rotationPitch));
//                        System.out.println("debug" + headShakeDist);
						nbt.setFloat("prevRotationYawHead", entity.getRotationYawHead());
						nbt.setFloat("prevRotationPitch", entity.rotationPitch);
					} else {
						headShakeDist = abs(wrapAngleTo180_float(nbt.getFloat("prevRotationYawHead") - entity.rotationYaw))
								+ abs(wrapAngleTo180_float(nbt.getFloat("prevRotationPitch") - entity.rotationPitch));
						nbt.setFloat("prevRotationYawHead", entity.rotationYaw);
						nbt.setFloat("prevRotationPitch", entity.rotationPitch);
					}
					guntemp.tempspreadDiffusion += headShakeDist * gunInfo.spreadDiffusionHeadRate;
					if(guntemp.currentConnectedTurret != null)guntemp.tempspreadDiffusion += walkedDist * gunInfo.spreadDiffusionWalkRate;
					if (!canFixflag || (entity.distanceWalkedModified != nbt.getFloat("prevdistanceWalkedModified"))) {
						nbt.setFloat("prevdistanceWalkedModified", entity.distanceWalkedModified);
						nbt.setBoolean("HMGfixed", false);
					}
				}
				if (entity instanceof EntityPlayer) {
					gunInfo.canceler = false;
					try {
						if (guntemp.invocable != null)
							guntemp.invocable.invokeFunction("update_onplayer", this, itemstack, nbt, entity);
					} catch (ScriptException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					}
					if (!gunInfo.canceler) {
						if (HandmadeGunsCore.Key_ADS(entity) || nbt.getBoolean("HMGfixed")) {
							nbt.setBoolean("set_up", true);
							nbt.setInteger("set_up_cnt", 3);
						}
						if (world.isRemote && (i != -1) && i != -10 && ((EntityPlayer) entity).getHeldItem() == itemstack) {
							if(!guntemp.connectedTurret){
								HMG_proxy.force_render_item_position(itemstack, i);
							}
							if (HandmadeGunsCore.cfg_SwapFireAndADSKeys && HMG_proxy.fireKeyDown()) {
								triggerHeldGun(itemstack);
								HMGPacketHandler.INSTANCE.sendToServer(new PacketTriggerHeld(entity.getEntityId()));
							}
							if (!gunInfo.isOneuse && HMG_proxy.ReloadKey_isPressed()) {
								HMGPacketHandler.INSTANCE.sendToServer(new PacketreturnMgazineItem(entity.getEntityId()));
								// Only an accepted reload starts the timer; holding the key must not rewind it.
							}
							if(((EntityPlayer) entity).inventory.getStackInSlot(i) != null && ((EntityPlayer) entity).inventory.getStackInSlot(i).getItem() instanceof HMGItem_Unified_Guns) {
								if (HMG_proxy.AttachmentKey_isPressed()) {
									HMGPacketHandler.INSTANCE.sendToServer(new PacketOpenGui(0, entity.getEntityId()));
								}

								boolean elevation_changed = false;
								if (HMG_proxy.resetElevationKeyDown()) {
									guntemp.currentElevation = 0;
									elevation_changed = true;
								} else if (HMG_proxy.upElevationKeyDown()) {
									guntemp.currentElevation++;
									elevation_changed = true;
								} else if (HMG_proxy.downElevationKeyDown()) {
									guntemp.currentElevation--;
									elevation_changed = true;
								}
								if (guntemp.currentElevation >= gunInfo.elevationOffsets.size()) {
									guntemp.currentElevation = gunInfo.elevationOffsets.size() - 1;
								} else if (guntemp.currentElevation < 0) {
									guntemp.currentElevation = 0;
								}
								if(elevation_changed) {//サーバーにパケット送信
									HMGPacketHandler.INSTANCE.sendToServer(new PacketSetElevation(entity.getEntityId(),i,guntemp.currentElevation));
								}
							}
							if ( HMG_proxy.seekerOpenClose()) {
								nbt.setBoolean("SeekerOpened", !nbt.getBoolean("SeekerOpened"));
								HMGPacketHandler.INSTANCE.sendToServer(new PacketSeekerOpen(entity.getEntityId()));
							}
							if (canFixflag && HMG_proxy.fixkeydown()) {
								HMGPacketHandler.INSTANCE.sendToServer(new PacketFixGun(entity.getEntityId()));
							}
							try {
								if (guntemp.items != null && guntemp.items[4] != null && guntemp.items[4].getItem() instanceof HMGItem_Unified_Guns) {
									checkTags(guntemp.items[4]);
									if (((HMGItem_Unified_Guns) guntemp.items[4].getItem()).getburstCount(guntemp.items[4].getTagCompound().getInteger("HMGMode")) != -1) {
										if (HMG_proxy.FClick())
											HMGPacketHandler.INSTANCE.sendToServer(new PacketTriggerUnder(entity.getEntityId()));
									} else if (HMG_proxy.FClick_no_stopper()) {
										HMGPacketHandler.INSTANCE.sendToServer(new PacketTriggerUnder(entity.getEntityId()));
									}
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
							if (HMG_proxy.ModeKey_isPressed()) {
								guntemp.selector++;
								if (guntemp.selector >= gunInfo.burstcount.size() || guntemp.selector >= gunInfo.rates.size()) {
									guntemp.selector = 0;
								}
								nbt.setInteger("HMGMode", guntemp.selector);
								HMGPacketHandler.INSTANCE.sendToServer(new PacketChangeModeHeldItem(entity, guntemp.selector));
							}
							//if (HMG_proxy.ChangeMagazineTypeClick()) {
							//	int selecting = nbt.getInteger("get_selectingMagazine");
							//	selecting++;
							//	if (selecting >= gunInfo.magazine.length) {
							//		selecting = 0;
							//	}
							//	nbt.setInteger("get_selectingMagazine", selecting);
							//	HMGPacketHandler.INSTANCE.sendToServer(new PacketChangeMagazineType(entity, selecting));
							//} GOODBYE
							//TODO this is more heckin bullshit for the keybind crap to change le heckin magazine to work

							//automated shit

							// run only on client ? oh my god why
							if (entity instanceof EntityPlayer && entity.worldObj.isRemote) {
								EntityPlayer player = (EntityPlayer) entity;

								int selecting = nbt.getInteger("get_selectingMagazine");
								// CALL THE INSTANCE METHOD ON THIS and pass the actual itemstack param
								int best = this.findBestMagazineIndex(player, itemstack);

								if (best >= 0 && best != selecting) {
									selecting = best;
									nbt.setInteger("get_selectingMagazine", selecting);
									HMGPacketHandler.INSTANCE.sendToServer(new PacketChangeMagazineType(entity, selecting));
								}
								// else if best == selecting or best == -1: do nothing (keeps existing selection)
							}


						} else {
							if(guntemp.currentConnectedTurret == null && (((EntityPlayer) entity).getHeldItem() == itemstack || i == -1)){
								if (gunInfo.canlock && nbt.getBoolean("SeekerOpened")) {
									lockon(itemstack, world, entity, nbt);
								} else if (guntemp.TGT != null) {
									Vector3d frontVec = getLockOnDirection(entity);
									guntemp.TGT = canContinueLock(itemstack, world, entity, frontVec);
								}
							}
						}
					}
				} else if (islmmloaded && entity instanceof LMM_EntityLittleMaid) {
					gunInfo.canceler = false;
					try {
						if (guntemp.invocable != null)
							guntemp.invocable.invokeFunction("update_onmaid", this, itemstack, nbt, entity);
					} catch (ScriptException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					}
					if (!gunInfo.canceler && (((LMM_EntityLittleMaid) entity).isUsingItem()))
						nbt.setBoolean("IsTriggered", true);
				}
				if (entity instanceof EntityLiving) {
					gunInfo.canceler = false;
					try {
						if (guntemp.invocable != null)
							guntemp.invocable.invokeFunction("update_onliving", this, itemstack, nbt, entity);
					} catch (ScriptException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					}
					if (!gunInfo.canceler) {
						nbt.setBoolean("SeekerOpened", false);
						if (gunInfo.canlock) {
							guntemp.TGT = null;
							if(((EntityLiving) entity).getAttackTarget() != null) {
								lockon(itemstack, world, entity, nbt);
							}
						}
					}
				}
				if (entity instanceof PlacedGunEntity) {
					gunInfo.canceler = false;
					try {
						if (guntemp.invocable != null)
							guntemp.invocable.invokeFunction("update_onplacedGun", this, itemstack, nbt, entity);
					} catch (ScriptException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					}

					if(world.isRemote && entity.riddenByEntity != null && entity.riddenByEntity == HMG_proxy.getEntityPlayerInstance()){
						if (!gunInfo.isOneuse && HMG_proxy.ReloadKey_isPressed()) {
							HMGPacketHandler.INSTANCE.sendToServer(new PacketreturnMgazineItem(entity.riddenByEntity.getEntityId()));
							// The accepted server reload owns timer initialization.
						}
						{

							if (HMG_proxy.ModeKey_isPressed()) {
								guntemp.selector++;
								if (guntemp.selector >= gunInfo.burstcount.size() || guntemp.selector >= gunInfo.rates.size()) {
									guntemp.selector = 0;
								}
								nbt.setInteger("HMGMode", guntemp.selector);
								HMGPacketHandler.INSTANCE.sendToServer(new PacketChangeModeHeldItem(entity.riddenByEntity, guntemp.selector));
							}
							if (HMG_proxy.AttachmentKey_isPressed()) {
								HMGPacketHandler.INSTANCE.sendToServer(new PacketOpenGui(0, entity.riddenByEntity.getEntityId()));
							}

							boolean elevation_changed = false;
							if (HMG_proxy.resetElevationKeyDown()) {
								guntemp.currentElevation = 0;
								elevation_changed = true;
							} else if (HMG_proxy.upElevationKeyDown()) {
								guntemp.currentElevation++;
								elevation_changed = true;
							} else if (HMG_proxy.downElevationKeyDown()) {
								guntemp.currentElevation--;
								elevation_changed = true;
							}
							if (guntemp.currentElevation >= gunInfo.elevationOffsets.size()) {
								guntemp.currentElevation = gunInfo.elevationOffsets.size() - 1;
							} else if (guntemp.currentElevation < 0) {
								guntemp.currentElevation = 0;
							}
							if(elevation_changed) {//サーバーにパケット送信
								HMGPacketHandler.INSTANCE.sendToServer(new PacketSetElevation(entity.riddenByEntity.getEntityId(),i,guntemp.currentElevation));
							}
						}
					}

					if (!gunInfo.canceler) {
						if (gunInfo.canlock && nbt.getBoolean("SeekerOpened")) {
							lockon(itemstack, world, entity, nbt);
						}
					}
				}
				try {
					if (guntemp.invocable != null)
						guntemp.invocable.invokeFunction("update_all", this, itemstack, nbt, entity);
				} catch (ScriptException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
				{
					if (!gunInfo.rates.isEmpty() && gunInfo.rates.size() > guntemp.selector)
						gunInfo.cycle = gunInfo.rates.get(guntemp.selector);
					boolean cocking = nbt.getBoolean("Cocking");
					int cockingtime = nbt.getInteger("CockingTime");
					if (!cocking && gunInfo.needcock) {
						try {
							if (guntemp.invocable != null)
								guntemp.invocable.invokeFunction("proceedcock", this, itemstack, nbt, entity);
						} catch (ScriptException e) {
							e.printStackTrace();
						} catch (NoSuchMethodException e) {
							e.printStackTrace();
						}
						if (cockingtime == 0) {
							world.playSoundEffect(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ, gunInfo.soundco, 1.0F, 1.0f);
						}
						++cockingtime;
						nbt.setInteger("CockingTime", cockingtime);
						if (cockingtime >= gunInfo.cocktime) {
							nbt.setInteger("CockingTime", 0);
							nbt.setBoolean("Cocking", true);
							if (HandmadeGunsCore.cfg_canEjectCartridge && gunInfo.cart_cocked) {
								dropCartridge(world, entity, itemstack);
							}
						}
					} else nbt.setBoolean("Cocking", true);
					boolean is_Bolt_shooting_position = cycleBolt(itemstack) && (!gunInfo.needcock || nbt.getBoolean("Cocking"));
					boolean fireReady = true;
					if (gunInfo.bedrockPresentation && entity instanceof EntityPlayer
							&& ((EntityPlayer)entity).getHeldItem() == itemstack && guntemp.currentConnectedTurret == null) {
						// Runs on the normal gun tick, including the authoritative server tick.
						boolean lowered = entity.isSprinting() && !nbt.getBoolean("set_up")
								&& !nbt.getBoolean("IsTriggered") && !nbt.getBoolean("IsReloading")
								&& !HandmadeGunsCore.Key_ADS(entity) && cockingtime == 0;
						handmadeguns.animation.LocomotionAnimationBridge.FireRecovery recovery =
								handmadeguns.animation.LocomotionAnimationBridge.updateFireRecovery(
										nbt.getInteger("HMGSprintRecovery"), nbt.getBoolean("HMGSprintTrigger"),
										lowered, nbt.getBoolean("IsTriggered"), nbt.getBoolean("IsReloading"));
						nbt.setInteger("HMGSprintRecovery", recovery.ticks);
						nbt.setBoolean("IsTriggered", recovery.triggered);
						if (recovery.queuedTrigger) nbt.setBoolean("HMGSprintTrigger", true);
						else nbt.removeTag("HMGSprintTrigger");
						fireReady = recovery.ready;
					}
					boolean isbulletremaining = remain_Bullet(itemstack) > 0;
					if (isbulletremaining && nbt.getBoolean("IsReloading") && isPerShellReload(itemstack) && shouldInterruptPerShellReload(entity, nbt)) {
						finishPerShellPresentation(itemstack, world, entity, nbt);
						nbt.setBoolean("IsReloading", false);
						nbt.setBoolean("WaitReloading", false);
						nbt.setInteger("RloadTime", 0);
						nbt.setInteger(PER_SHELL_PHASE_TAG, PER_SHELL_PHASE_NONE);
						nbt.setInteger(PER_SHELL_INTRO_TIME_TAG, 0);
					}
					Entity SACLOSCheck = world.getEntityByID(entity.getEntityData().getInteger("SACLOS_HOMING"));

					if (fireReady && nbt.getBoolean("IsTriggered") && !isPerShellInsertionInProgress(itemstack, nbt) && !(SACLOSCheck instanceof HMGEntityBulletBase && ((HMGEntityBulletBase) SACLOSCheck).SACLOS_Homing && this.gunInfo.SACLOS_Homing)) {
//						System.out.println("debug");
						if (!gunInfo.needfix || nbt.getBoolean("HMGfixed")) {
							if (!nbt.getBoolean("TriggerBacked")) {
								if (getburstCount(guntemp.selector) == 0) {
									nbt.setBoolean("Bursting", false);
								} else {
									if (getburstCount(guntemp.selector) != -1 && !gunInfo.chargeType) {
										if (is_Bolt_shooting_position && !nbt.getBoolean("Bursting")) {
											nbt.setBoolean("Bursting", true);
											nbt.setInteger("RemainBurstround", getburstCount(guntemp.selector));
										}
										nbt.setBoolean("TriggerBacked", true);
									}
									if (getburstCount(guntemp.selector) != -1 && gunInfo.chargeType) {
										nbt.setBoolean("TriggerBacked", true);
									}
									if (!gunInfo.chargeType && !nbt.getBoolean("Bursting") && is_Bolt_shooting_position && isbulletremaining) {
										fireProcess(itemstack, world, entity, nbt, i);
										resetBolt(itemstack);
										nbt.setBoolean("Cocking", false);
									}
								}
							}
						}
					} else if (fireReady) {
						if (nbt.getBoolean("TriggerBacked") && gunInfo.chargeType) {
							if (is_Bolt_shooting_position && !nbt.getBoolean("Bursting")) {
								nbt.setBoolean("Bursting", true);
								nbt.setInteger("RemainBurstround", getburstCount(guntemp.selector));
							}
						}
						nbt.setBoolean("TriggerBacked", false);
					}
					if (fireReady && is_Bolt_shooting_position && isbulletremaining && nbt.getBoolean("Bursting")) {
						nbt.setInteger("RemainBurstround", nbt.getInteger("RemainBurstround") - 1);
						if (nbt.getInteger("RemainBurstround") < 0) {
							nbt.setBoolean("Bursting", false);
							nbt.setBoolean("Cocking", false);
						} else {
							fireProcess(itemstack, world, entity, nbt, i);
							resetBolt(itemstack);
						}
					}
				}
				boolean perShellInterruptWindow = isPerShellReload(itemstack) && nbt.getInteger(PER_SHELL_INTERRUPT_TIME_TAG) > 0;
				boolean reloadRequested = nbt.getBoolean("IsReloading");
				boolean canAutoReload = !requiresManualReload(entity);
				if (reloadRequested || perShellInterruptWindow || (canAutoReload && remain_Bullet(itemstack) <= 0)) {
					nbt.setInteger("CockingTime", 0);
					nbt.setBoolean("Cocking", true);
					if (!perShellInterruptWindow) {
						try {
							if (guntemp.invocable != null)
								guntemp.invocable.invokeFunction("startreload", this, itemstack, nbt, entity);
						} catch (ScriptException e) {
							e.printStackTrace();
						} catch (NoSuchMethodException e) {
							e.printStackTrace();
						}
						if (!nbt.getBoolean("IsReloading")) nbt.setBoolean("IsReloading", true);
						if (!isPerShellReload(itemstack) && !nbt.getBoolean("detached")) returnInternalMagazines(itemstack, entity);
					}
					proceedreload(itemstack, world, entity, nbt, i);
				}
				nbt.setBoolean("IsTriggered", false);
				if (nbt.getInteger("set_up_cnt") > 0) {
					nbt.setInteger("set_up_cnt", nbt.getInteger("set_up_cnt") - 1);
				} else {
					nbt.setBoolean("set_up", false);
				}
//                if(entity instanceof EntityPlayerMP) {
//                    updateCheckinghSlot((EntityPlayerMP) entity, itemstack);
//                }
				if (!world.isRemote && gunInfo.canlock && nbt.getBoolean("SeekerOpened")) {
					nbt.setBoolean("islockedentity", guntemp.islockingentity);
					if (guntemp.TGT != null) nbt.setInteger("TGT", guntemp.TGT.getEntityId());
					else nbt.setInteger("TGT", -1);
					if (guntemp.islockingblock) {
						nbt.setInteger("LockedPosX", guntemp.LockedPosX);
						nbt.setInteger("LockedPosY", guntemp.LockedPosY);
						nbt.setInteger("LockedPosZ", guntemp.LockedPosZ);
					}
					nbt.setBoolean("islockedblock", guntemp.islockingblock);
				} else {
					if (guntemp.TGT == null || guntemp.TGT.isDead) {
						nbt.setInteger("TGT", -1);
					}
				}
				try {
					if (guntemp.items != null) {
//                        for (int i1 = 0; i1 < guntemp.items.length; i1++) {
//                            if (guntemp.items[i1] != null && guntemp.items[i1].getItemDamage() > guntemp.items[i1].getMaxDamage()) {
//                                guntemp.items[i1].stackSize--;
//                            }
//                            if (guntemp.items[i1] != null && guntemp.items[i1].stackSize <= 0) {
//                                guntemp.items[i1] = null;
//                            }
//                        }
						if (!world.isRemote) {
							NBTTagList tags = (NBTTagList) nbt.getTag("Items");
							int compressedID = 0;
							if (tags != null) {
								for (int itemid = 0; itemid < guntemp.items.length; itemid++) {
									if (guntemp.items[itemid] != null && guntemp.items[itemid].getItem() != null) {
										NBTTagCompound compound = new NBTTagCompound();
										compound.setByte("Slot", (byte) itemid);
										guntemp.items[itemid].writeToNBT(compound);
										tags.func_150304_a(compressedID, compound);
										compressedID++;
									}
//                                    if (items[itemid] != null && items[itemid].getItem() != null) {
//                                        NBTTagCompound compound = new NBTTagCompound();
//                                        compound.setByte("Slot", (byte) itemid);
//                                        items[itemid].writeToNBT(compound);
//                                        if(tags.tagCount() >= 6) {
//                                            if(itemid == 4 && items[4] != null && items[4].getItem() instanceof HMGItem_Unified_Guns){
//                                                System.out.println("" + items[4].getItemDamage());
//                                            }
//                                            tags.func_150304_a(itemid, compound);
//                                        }else {
//                                            tags.appendTag(compound);
//                                        }
//                                    }
								}
								if (compressedID > 6 && compressedID < tags.tagCount()) {
									for (int removeid = compressedID; removeid < tags.tagCount(); removeid++) {
										System.out.println("debug" + compressedID);
										tags.removeTag(removeid);
									}
								}
								nbt.setTag("Items", tags);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				nbt.setFloat("Diffusion", guntemp.tempspreadDiffusion);
			} else if (itemstack != null) {
				checkTags(itemstack);
				NBTTagCompound tagCompound = itemstack.getTagCompound();
				tagCompound.setInteger("RloadTime", 0);//持っていなければリロード初期化
			}
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	//end of gun process
	public void lockon(ItemStack itemstack, World worldObj, Entity entity, NBTTagCompound nbt){
		if(guntemp.currentConnectedTurret != null){
			guntemp.TGT = guntemp.currentConnectedTurret.target;
			if(guntemp.TGT != null)
				guntemp.islockingentity = true;
		}else {
			if(gunInfo.lockSound_NoStop){
				guntemp.TGT = null;
				guntemp.islockingentity = false;
			}
			try {
				HMGPointOfAimBridge.AimRay aimRay = entity instanceof EntityPlayer
						? HMGPointOfAimBridge.getAuthoritativeRay((EntityPlayer) entity) : null;
				Vector3d frontVec = aimRay != null ? getjavaxVecObj(aimRay.direction)
						: getjavaxVecObj(getLook(1,entity.getRotationYawHead(),entity.rotationPitch));
				frontVec.scale(-1);
				Entity prevTarget = guntemp.TGT;
				guntemp.TGT = null;
				guntemp.TGT = lockOn(itemstack,worldObj,entity,frontVec);
				if(guntemp.TGT != null){
					guntemp.islockingentity = true;
					guntemp.islockingblock = false;
					if(guntemp.TGT.ridingEntity != null)guntemp.TGT = guntemp.TGT.ridingEntity;
					if(guntemp.TGT instanceof EntityDummy_rider){
						guntemp.TGT = ((EntityDummy_rider) guntemp.TGT).linkedBaseLogic.mc_Entity;
					}
					guntemp.TGT.getEntityData().setBoolean("behome",true);
				}
				if(guntemp.TGT != null && (gunInfo.lockSound_NoStop || guntemp.TGT != prevTarget)){
					entity.worldObj.playSoundAtEntity(entity, gunInfo.lockSound_entity, 1f, gunInfo.lockpitch_entity);
				}
				if(gunInfo.canlockBlock){
					Vec3 vec3 = aimRay != null ? aimRay.origin
							: Vec3.createVectorHelper(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
					Vec3 playerlook = getMinecraftVecObj(frontVec);

					playerlook.xCoord *= -1;
					playerlook.yCoord *= -1;
					playerlook.zCoord *= -1;

					playerlook = Vec3.createVectorHelper(playerlook.xCoord * 256, playerlook.yCoord * 256, playerlook.zCoord * 256);

					Vec3 vec31 = Vec3.createVectorHelper(vec3.xCoord + playerlook.xCoord,
							vec3.yCoord + playerlook.yCoord, vec3.zCoord + playerlook.zCoord);
					MovingObjectPosition movingobjectposition = GunsUtils.getmovingobjectPosition_forBlock(worldObj,vec3, vec31);//衝突するブロックを調べる
					if(movingobjectposition != null && movingobjectposition.hitVec != null){
						guntemp.LockedPosX = movingobjectposition.blockX;
						guntemp.LockedPosY = movingobjectposition.blockY;
						guntemp.LockedPosZ = movingobjectposition.blockZ;
						guntemp.islockingblock = true;
						guntemp.islockingentity = false;
						guntemp.TGT = null;
					}
				}
			} catch (Exception e) {
			}
//            Vec3 vec3 = Vec3.createVectorHelper(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
//            Vec3 playerlook = entity instanceof EntityLivingBase ? ((EntityLivingBase) entity).getLook(1.0f) : entity instanceof PlacedGunEntity ? ((PlacedGunEntity) entity).getLook(1.0f) : null;
//            playerlook = Vec3.createVectorHelper(playerlook.xCoord * 256, playerlook.yCoord * 256, playerlook.zCoord * 256);
//            Vec3 vec31 = Vec3.createVectorHelper(entity.posX + playerlook.xCoord, entity.posY + entity.getEyeHeight() + playerlook.yCoord, entity.posZ + playerlook.zCoord);
//            MovingObjectPosition movingobjectposition = entity.worldObj.func_147447_a(vec3, vec31, false, true, false);
//            Block hitblock;
//            Random rand = new Random();
//            while (movingobjectposition != null) {
//                hitblock = entity.worldObj.getBlock(movingobjectposition.blockX, movingobjectposition.blockY, movingobjectposition.blockZ);
//                if ((hitblock.getMaterial() == Material.plants) || ((
//                        hitblock.getMaterial() == Material.glass ||
//                                hitblock instanceof BlockFence ||
//                                hitblock instanceof BlockFenceGate ||
//                                hitblock == Blocks.iron_bars) && rand.nextInt(5) <= 1)) {
//                    Vec3 penerater = movingobjectposition.hitVec.normalize();
//                    vec3 = Vec3.createVectorHelper(movingobjectposition.hitVec.xCoord + penerater.xCoord, movingobjectposition.hitVec.yCoord + penerater.yCoord, movingobjectposition.hitVec.zCoord + penerater.zCoord);
//                    vec31 = Vec3.createVectorHelper(entity.posX + playerlook.xCoord, entity.posY + entity.getEyeHeight() + playerlook.yCoord, entity.posZ + playerlook.zCoord);
//                    movingobjectposition = entity.worldObj.func_147447_a(vec3, vec31, false, true, false);
//                } else {
//                    break;
//                }
//            }
//            vec3 = Vec3.createVectorHelper(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
//            vec31 = Vec3.createVectorHelper(entity.posX + playerlook.xCoord, entity.posY + entity.getEyeHeight() + playerlook.yCoord, entity.posZ + playerlook.zCoord);
//            if (movingobjectposition != null) {
//                vec31 = Vec3.createVectorHelper(movingobjectposition.hitVec.xCoord, movingobjectposition.hitVec.yCoord, movingobjectposition.hitVec.zCoord);
//            }
//            Entity rentity = null;
//            List list = entity.worldObj.getEntitiesWithinAABBExcludingEntity(entity, entity.boundingBox.addCoord(playerlook.xCoord, playerlook.yCoord, playerlook.zCoord).expand(1.0D, 1.0D, 1.0D));
//            double d0 = 0.0D;
//            double d1 = 0;
//            for (int i1 = 0; i1 < list.size(); ++i1) {
//                Entity entity1 = (Entity) list.get(i1);
//                if (entity1.canBeCollidedWith() && (entity1 != entity)) {
//                    float f = 0.5F;
//                    AxisAlignedBB axisalignedbb = entity1.boundingBox.expand((double) f, (double) f, (double) f);
//                    MovingObjectPosition movingobjectposition1 = axisalignedbb.calculateIntercept(vec3, vec31);
//
//                    if (movingobjectposition1 != null) {
//                        d1 = vec3.distanceTo(movingobjectposition1.hitVec);
//
//                        if (d1 < d0 || d0 == 0.0D) {
//                            rentity = entity1;
//                            d0 = d1;
//                        }
//                    }
//                }
//            }
//
//            if (rentity != null) {
//                d1 = vec3.distanceTo(vec31);
//                vec3.xCoord = vec3.xCoord + (vec31.xCoord - vec3.xCoord) * d0 / d1;
//                vec3.yCoord = vec3.yCoord + (vec31.yCoord - vec3.yCoord) * d0 / d1;
//                vec3.zCoord = vec3.zCoord + (vec31.zCoord - vec3.zCoord) * d0 / d1;
//
//                movingobjectposition = new MovingObjectPosition(rentity);
//                movingobjectposition.hitVec = vec3;
//            }
//            if (movingobjectposition != null) {
//                if (gunInfo.canlockEntity && movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && movingobjectposition.entityHit != null) {
//                    if (!guntemp.islockingentity || (entity.ridingEntity == null || entity.ridingEntity != movingobjectposition.entityHit) && guntemp.TGT != movingobjectposition.entityHit) {
//                        entity.worldObj.playSoundAtEntity(entity, gunInfo.lockSound_entity, 1f, gunInfo.lockpitch_entity);
//                        guntemp.TGT = movingobjectposition.entityHit;
//                        guntemp.islockingentity = true;
//                        guntemp.islockingblock = false;
//                    }
//                } else if (gunInfo.canlockBlock && movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && (
//                        guntemp.LockedPosX != movingobjectposition.blockX || guntemp.LockedPosY != movingobjectposition.blockY || guntemp.LockedPosZ != movingobjectposition.blockZ)) {
//                    entity.worldObj.playSoundAtEntity(entity, gunInfo.lockSound_block, 1f, gunInfo.lockpitch_block);
//                    guntemp.LockedPosX = movingobjectposition.blockX;
//                    guntemp.LockedPosY = movingobjectposition.blockY;
//                    guntemp.LockedPosZ = movingobjectposition.blockZ;
//                    guntemp.islockingblock = true;
//                    guntemp.islockingentity = false;
//                    guntemp.TGT = null;
//                }
//            }
//
//            if (gunInfo.canlock) {
////                if (guntemp.islockingblock) {
////                    HMG_proxy.spawnParticles(new PacketSpawnParticle(guntemp.LockedPosX + 0.5, guntemp.LockedPosY + 0.5, guntemp.LockedPosZ + 0.5, 2));
////                }
////                if (guntemp.islockingentity && guntemp.TGT != null) {
////                    HMG_proxy.spawnParticles(new PacketSpawnParticle(guntemp.TGT.posX, guntemp.TGT.posY + guntemp.TGT.height / 2, guntemp.TGT.posZ, 2));
////                }
//            }
		}
	}
	private Vector3d getLockOnDirection(Entity entity) {
		if (entity instanceof EntityPlayer) {
			HMGPointOfAimBridge.AimRay aimRay = HMGPointOfAimBridge.getAuthoritativeRay((EntityPlayer) entity);
			if (aimRay != null) return getjavaxVecObj(aimRay.direction);
		}
		return getjavaxVecObj(getLook(1, entity.getRotationYawHead(), entity.rotationPitch));
	}
	public Entity lockOn(ItemStack itemstack,World worldObj,Entity entity,Vector3d frontVec){
		double predeg = -1;
		Entity target = null;
		if(gunInfo.canlockEntity && remain_Bullet(itemstack)>0) {
			List loadedEntityList = (List) ((ArrayList<?>)worldObj.loadedEntityList).clone();
			Iterator loadedEntityListIterator = loadedEntityList.iterator();
			while (loadedEntityListIterator.hasNext()) {
				Object obj = loadedEntityListIterator.next();
				Entity aEntity = (Entity) obj;

				if (!aEntity.isDead) {
					if ((!gunInfo.lock_to_Vehicle || aEntity.width >= 2)) {
//                        System.out.println("debug" + aEntity);
						double distsq = entity.getDistanceSqToEntity(aEntity);
						if (distsq < 16777216) {
							Vector3d totgtvec = new Vector3d(entity.posX - aEntity.posX, entity.posY - aEntity.posY, entity.posZ - aEntity.posZ);
							if (totgtvec.length() > 1) {
								totgtvec.normalize();
								if (totgtvec.y < gunInfo.lookDown) {
									double deg = wrapAngleTo180_double(toDegrees(totgtvec.angle(frontVec)));
									if (canLock(entity, aEntity) && gunInfo.seekerSize > abs(deg) && (abs(deg) < predeg || predeg == -1)) {
										predeg = deg;
										target = aEntity;
									}
								}
							}
						}
					}
				}
			}
		}
		return target;
	}
	public Entity canContinueLock(ItemStack itemstack,World worldObj,Entity entity,Vector3d frontVec){
		Entity target = guntemp.TGT;
		if(gunInfo.canlockEntity && target != null) {
			Entity aEntity = target;
			target = null;
			if (!aEntity.isDead) {
				if (aEntity.canBeCollidedWith() &&
						(!gunInfo.lock_to_Vehicle || aEntity.width >= 1.5)) {
					double distsq = entity.getDistanceSqToEntity(aEntity);
					if (distsq < 16777216) {
						Vector3d totgtvec = new Vector3d(entity.posX - aEntity.posX, entity.posY - aEntity.posY, entity.posZ - aEntity.posZ);
						if (totgtvec.length() > 1) {
							totgtvec.normalize();
							if (totgtvec.y < gunInfo.lookDown) {
								double deg = wrapAngleTo180_double(toDegrees(totgtvec.angle(frontVec)));
								if (canLock(entity, aEntity) && gunInfo.seekerSize > abs(deg)) {
									target = aEntity;
								}
							}
						}
					}
				}
			}
		}
		return target;
	}
	public boolean canLock(Entity my,Entity entity){
		if(!Utils.iscandamageentity(my,entity))return false;
		if(my instanceof EntityLivingBase && !((EntityLivingBase) my).canEntityBeSeen(entity))return false;
		if(my instanceof PlacedGunEntity && my.riddenByEntity instanceof EntityLivingBase && !((EntityLivingBase) my.riddenByEntity).canEntityBeSeen(entity))return false;
		double targetEntitySpeed = getEntitySpeedSQ(entity);
		return (gunInfo.lockOn_minSpeed == -1 || targetEntitySpeed > gunInfo.lockOn_minSpeed) && (gunInfo.lockOn_MaxSpeed == -1 || targetEntitySpeed < gunInfo.lockOn_MaxSpeed) &&
				(!(entity instanceof EntityVehicle) || ((gunInfo.lockOn_minThrottle == -1 || abs(((EntityVehicle) entity).getBaseLogic().throttle) > gunInfo.lockOn_minThrottle) && (gunInfo.lockOn_MaxThrottle == -1 || abs(((EntityVehicle) entity).getBaseLogic().throttle) < gunInfo.lockOn_MaxThrottle)));
	}
	public double getEntitySpeedSQ(Entity entity){
		return entity.motionX * entity.motionX + entity.motionY * entity.motionY + entity.motionZ * entity.motionZ;
	}
	public void fireProcess(ItemStack itemstack, World world, Entity entity, NBTTagCompound nbt, int inventorySlot){
		EntityPlayer firingPlayer = entity instanceof EntityPlayer ? (EntityPlayer) entity : null;
		if (!handmadeguns.tech.HMGTechTierManager.isUnlocked(itemstack, firingPlayer, world)) {
			handmadeguns.tech.HMGTechTierManager.notifyLocked(firingPlayer, itemstack);
			return;
		}
		int fireLoop = 1;
		boolean shotCommitted = false;
		ItemStack oneUseState = gunInfo.isOneuse && !world.isRemote ? itemstack.copy() : null;
		if(guntemp.currentConnectedTurret != null){
			fireLoop = guntemp.currentConnectedTurret.getSyncroFireNum();
		}
		for(int fired = 0;fired < fireLoop;fired ++) {
			if (!world.isRemote) {
				guntemp.tempspreadDiffusion += gunInfo.spreadDiffusionRate * (guntemp.currentConnectedTurret == null && entity instanceof EntityLiving && entity.rotationPitch < 0 ? sin(entity.rotationPitch):1);
				try {
					if (guntemp.invocable != null)
						guntemp.invocable.invokeFunction("prefire", this, itemstack, nbt, entity);
				} catch (ScriptException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
				entity.getEntityData().setFloat("GunshotLevel", guntemp.soundlevel);
				HMG_proxy.playerSounded(entity);
//            world.playSoundEffect(entity.posX,entity.posY,entity.posZ, sound, soundlevel, soundspeed);


				// Resolve before consumption: this exact stack supplies this shot's profile.
				ItemStack ammunitionStack = resolveNextAmmunitionStack(itemstack);
				HMGEntityBulletBase[] bullet = null;
				//メソッド一個追加して、弾種類で分けながらfor回したほうが絶対効率良さそう<-んな訳あるか
				int currentBulletType = gunInfo.guntype;
				if (gunInfo.guntype < 5 && guntemp.items != null && guntemp.items[5] != null) {
					try {
						switch (gunInfo.guntype) {
							case 0:
							case 1:
								if (guntemp.items[5].getItem() instanceof HMGItemBullet_AP) {
									currentBulletType = 6;
								} else if (guntemp.items[5].getItem() instanceof HMGItemBullet_Frag) {
									currentBulletType = 7;
								} else if (guntemp.items[5].getItem() instanceof HMGItemBullet_AT) {
									currentBulletType = 8;
								}
								break;
							case 2:
							case 3:
								if (guntemp.items[5].getItem() instanceof HMGItemBullet_TE) {
									currentBulletType = 9;
								}
								break;
							case 4:
								if (guntemp.items[5].getItem() instanceof HMGItemBullet_AP) {
									currentBulletType = 6;
								} else if (guntemp.items[5].getItem() instanceof HMGItemBullet_Frag) {
									currentBulletType = 10;
								}
								break;
						}
					} catch (NullPointerException e) {
						e.printStackTrace();
					}
				}
				firetemp = new FireTemp(gunInfo);
				firetemp.applyMagOption(ammunitionStack);
				if(firetemp.bulletType != -1) currentBulletType = firetemp.bulletType;
				float gunBaseSpread = gunInfo.spread_setting;
				int gunBaseFuse = gunInfo.fuse;
				Float ammoSpread = null;
				Integer ammoFuse = null;
				if(ammunitionStack != null && ammunitionStack.getItem() instanceof HMGItemCustomMagazine) {
					HMGItemCustomMagazine ammunition = (HMGItemCustomMagazine) ammunitionStack.getItem();
					ammoSpread = ammunition.spreadOverride;
					ammoFuse = ammunition.fuseOverride;
				}
				// GunTemp records the normal ADS, diffusion and attachment factor. Apply
				// it to this round's base directly, without dividing by the gun base.
				firetemp.spread *= guntemp.spreadMultiplier;
				if(HandmadeGunsCore.isDebugMessage) {
					ItemStack loadedFront = ItemStack.loadItemStackFromNBT(itemstack.getTagCompound().getCompoundTag("LoadedMagazine0"));
					Item selected = getcurrentMagazine(itemstack);
					HandmadeGunsCore.Debug("[AmmoDebug] SHOT gun=%s resolved=%s itemDamage=%s maxDamage=%s selected=%s loaded0=%s power=%s speed=%s gunBaseSpread=%s ammoSpread=%s finalSpread=%s pellet=%s gunBaseFuse=%s ammoFuse=%s finalFuse=%s bulletType=%s gravity=%s resistance=%s stability=%s damageRange=%s",
							getUnlocalizedName(), debugItem(ammunitionStack), ammunitionStack == null ? -1 : ammunitionStack.getItemDamage(), ammunitionStack == null ? -1 : ammunitionStack.getMaxDamage(),
							selected == null ? "null" : selected.getUnlocalizedName(), debugItem(loadedFront), firetemp.power, firetemp.speed,
							gunBaseSpread, ammoSpread, firetemp.spread, firetemp.pellet, gunBaseFuse, ammoFuse, firetemp.fuse,
							currentBulletType, firetemp.gra, firetemp.resistance, firetemp.bulletStability, firetemp.damageRange);
				}
				damageMagazine(itemstack, entity);
				shotCommitted = true;
				float backUpRotationPitch = entity.rotationPitch;
				float backUpRotationYaw = entity.rotationYaw;
				if(!gunInfo.elevationOffsets.isEmpty() && !(entity instanceof PlacedGunEntity)){
					entity.rotationPitch += gunInfo.elevationOffsets.get(guntemp.currentElevation);
				}
				if(entity.rotationPitch>90 || entity.rotationPitch<-90){
					entity.rotationYaw += 180;
					entity.rotationPitch = (180 - abs(entity.rotationPitch)) * (entity.rotationPitch>0 ? 1 : -1);
				}
				switch (currentBulletType) {
					case 0:
					case 1:
					case 4:
						bullet = getBullet(world, entity);
						break;
					case 2:
						bullet = FireBulletGL(world, entity);
						break;
					case 3:
						bullet = FireBulletRPG(world, entity);
						break;
					case 5:
						bullet = FireBulletFrame(world, entity);
						break;
					case 6:
						bullet = getBulletAP(world, entity);
						break;
					case 7:
						bullet = FireBulletFrag(world, entity);
						break;
					case 8:
						bullet = FireBulletAT(world, entity);
						break;
					case 9:
						bullet = FireBulletTE(world, entity);
						break;
					case 10:
						bullet = FireBulletHE(world, entity);
						break;
					case 11:
						bullet = FireBulletTorp(world, entity);
						break;
				}

				if (bullet != null) {
					if (guntemp.currentConnectedTurret != null) {
						guntemp.currentConnectedTurret.setBulletsPos(bullet);
					}else {
						if (HandmadeGunsCore.cfg_canEjectCartridge && gunInfo.dropcart) {
							dropCartridge(world, entity, itemstack);
						}
						this.Flash(itemstack, world, entity, nbt);
					}
					int pelletIndex = 0;
					for (HMGEntityBulletBase bulletBase : bullet) {
						bulletBase.knockbackXZ = firetemp.knockback;
						bulletBase.knockbackY = firetemp.knockbackY;
						bulletBase.bulletStability = firetemp.bulletStability;
						bulletBase.gra = firetemp.gra;
						bulletBase.bouncerate = firetemp.bouncerate;
						bulletBase.bouncelimit = firetemp.bouncelimit;
						bulletBase.fuse = firetemp.fuse;
						if(HandmadeGunsCore.isDebugMessage && pelletIndex == 0)
							HandmadeGunsCore.Debug("[AmmoDebug] PROJECTILE_CONFIG class=%s finalSpread=%s spawnedFuse=%s", bulletBase.getClass().getName(), firetemp.spread, bulletBase.fuse);
						bulletBase.canbounce = firetemp.canbounce;
						bulletBase.resistance = firetemp.resistance;
						bulletBase.acceleration = firetemp.acceleration;
						bulletBase.accelerationDelay = firetemp.accelerationDelay;
						bulletBase.accelerationFuse = firetemp.accelerationFuse;
						bulletBase.canex = firetemp.destroyBlock;
						bulletBase.canDoorBreach = firetemp.canDoorBreach;
						if(bulletBase instanceof HMGEntityBulletTorp)((HMGEntityBulletTorp) bulletBase).draft = gunInfo.torpdraft;
						bulletBase.damageRange = firetemp.damageRange;
						bulletBase.hasVT   = gunInfo.hasVT  ;
						bulletBase.forceVT   = gunInfo.forceVT  ;
						bulletBase.VTRange = gunInfo.VTRange;
						bulletBase.VTWidth = gunInfo.VTWidth;
						bulletBase.seekerwidth = gunInfo.seekerSize_bullet;
						bulletBase.resistanceinwater = firetemp.resistanceInWater;
						if (guntemp.currentConnectedTurret == null && !bulletBase.authoritativePlayerAim) {
							bulletBase.prevRotationYaw = bulletBase.rotationYaw = wrapAngleTo180_float(entity.getRotationYawHead());
							bulletBase.prevRotationPitch = bulletBase.rotationPitch = entity.rotationPitch;
						}
						bulletBase.SACLOS_Homing = gunInfo.SACLOS_Homing;
						bulletBase.chunkLoaderBullet = gunInfo.chunkLoaderBullet;

						if (guntemp.currentConnectedTurret == null) {
							if (guntemp.islockingentity) {
								bulletBase.homingEntity = guntemp.TGT;
								bulletBase.isSemiActive = gunInfo.semiActive;
								bulletBase.isActive = gunInfo.isActive;
								bulletBase.induction_precision = gunInfo.induction_precision;
							} else if (guntemp.islockingblock) {
								bulletBase.lockedBlockPos = Vec3.createVectorHelper(guntemp.LockedPosX, guntemp.LockedPosY, guntemp.LockedPosZ);
								bulletBase.induction_precision = gunInfo.induction_precision;
							}
							if (entity instanceof PlacedGunEntity) {
								setBulletPos_PlacedGun(entity, bulletBase, nbt);
							}
						}else {
							if(gunInfo.canlock) {
								if (guntemp.islockingentity) {
//                                    System.out.println("" + bulletBase.homingEntity);
//                                    bulletBase.induction_precision = gunInfo.induction_precision;
								} else if (guntemp.islockingblock) {
//                                    bulletBase.lockedBlockPos = guntemp.currentConnectedTurret.lockedBlockPos;
//                                    bulletBase.induction_precision = gunInfo.induction_precision;
								}
							}
						}
						if(pelletIndex == 0) bulletBase.debugSpawn(pelletIndex);
						world.spawnEntityInWorld(bulletBase);
						pelletIndex++;
					}
				}else {
					if (HandmadeGunsCore.cfg_canEjectCartridge && gunInfo.dropcart) {
						dropCartridge(world, entity, itemstack);
					}
				}
				entity.rotationPitch = backUpRotationPitch;
				entity.rotationYaw = backUpRotationYaw;
				HMGPacketHandler.INSTANCE.sendToAll(new PacketPlaysound(entity, guntemp.sound, gunInfo.soundspeed, guntemp.soundlevel,entity.posX,entity.posY,entity.posZ));
			}

			if (!nbt.getBoolean("HMGfixed"))
				if (entity instanceof EntityPlayerMP)
					HMGPacketHandler.INSTANCE.sendTo(new PacketRecoil(), (EntityPlayerMP) entity);
			try {
				if (guntemp.invocable != null)
					guntemp.invocable.invokeFunction("fireout", this, itemstack, nbt, entity);
			} catch (ScriptException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
		}
		if (shotCommitted && gunInfo.isOneuse) {
			consumeOneUseWeapon(itemstack, oneUseState, entity, inventorySlot);
		}
	}
	public void setBulletPos_PlacedGun(Entity entity,HMGEntityBulletBase bulletBase,NBTTagCompound nbt){
		if (gunInfo.posGetter.multi_barrelpos == null) {
			Vec3 vec = Vec3.createVectorHelper(gunInfo.posGetter.barrelpos[0], gunInfo.posGetter.barrelpos[1], -gunInfo.posGetter.barrelpos[2]);
			vec = vec.addVector(-gunInfo.posGetter.turretRotationPitchPoint[0], -gunInfo.posGetter.turretRotationPitchPoint[1], -gunInfo.posGetter.turretRotationPitchPoint[2]);
			vec.rotateAroundX(-(float) toRadians(entity.rotationPitch));
			vec = vec.addVector(gunInfo.posGetter.turretRotationPitchPoint[0], gunInfo.posGetter.turretRotationPitchPoint[1], gunInfo.posGetter.turretRotationPitchPoint[2]);
			vec = vec.addVector(-gunInfo.posGetter.turretRotationYawPoint[0], -gunInfo.posGetter.turretRotationYawPoint[1], -gunInfo.posGetter.turretRotationYawPoint[2]);
			vec.rotateAroundY(-(float) toRadians(((PlacedGunEntity) entity).rotationYawGun - ((PlacedGunEntity) entity).baserotationYaw));
			vec = vec.addVector(gunInfo.posGetter.turretRotationYawPoint[0], gunInfo.posGetter.turretRotationYawPoint[1], gunInfo.posGetter.turretRotationYawPoint[2]);
			vec.rotateAroundY(-(float) toRadians(((PlacedGunEntity) entity).baserotationYaw));
			double ix = 0;
			double iy = 0;
			double iz = 0;
			ix = vec.xCoord;
			iy = vec.yCoord;
			iz = vec.zCoord;
			if (entity.riddenByEntity != null) bulletBase.thrower = entity.riddenByEntity;
			bulletBase.setLocationAndAngles(entity.posX + ix, entity.posY + entity.getEyeHeight() + iy, entity.posZ + iz, ((PlacedGunEntity) entity).rotationYawGun, entity.rotationPitch);
		} else {
			int barrelId = nbt.getInteger("barrelId");
			barrelId++;
			if (barrelId >= gunInfo.posGetter.multi_barrelpos.length) barrelId = 0;
			Vec3 vec = Vec3.createVectorHelper(gunInfo.posGetter.multi_barrelpos[barrelId][0], gunInfo.posGetter.multi_barrelpos[barrelId][1], gunInfo.posGetter.multi_barrelpos[barrelId][2]);
			vec = vec.addVector(-gunInfo.posGetter.turretRotationPitchPoint[0], -gunInfo.posGetter.turretRotationPitchPoint[1], -gunInfo.posGetter.turretRotationPitchPoint[2]);
			vec.rotateAroundX(-(float) toRadians(entity.rotationPitch));
			vec = vec.addVector(gunInfo.posGetter.turretRotationPitchPoint[0], gunInfo.posGetter.turretRotationPitchPoint[1], gunInfo.posGetter.turretRotationPitchPoint[2]);
			vec = vec.addVector(-gunInfo.posGetter.turretRotationYawPoint[0], -gunInfo.posGetter.turretRotationYawPoint[1], -gunInfo.posGetter.turretRotationYawPoint[2]);
			vec.rotateAroundY(-(float) toRadians(((PlacedGunEntity) entity).rotationYawGun - ((PlacedGunEntity) entity).baserotationYaw));
			vec = vec.addVector(gunInfo.posGetter.turretRotationYawPoint[0], gunInfo.posGetter.turretRotationYawPoint[1], gunInfo.posGetter.turretRotationYawPoint[2]);
			vec.rotateAroundY(-(float) toRadians(((PlacedGunEntity) entity).baserotationYaw));
			double ix = 0;
			double iy = 0;
			double iz = 0;
			ix = vec.xCoord;
			iy = vec.yCoord;
			iz = vec.zCoord;
			if (entity.riddenByEntity != null) bulletBase.thrower = entity.riddenByEntity;
			bulletBase.setLocationAndAngles(entity.posX + ix, entity.posY + entity.getEyeHeight() + iy, entity.posZ + iz, ((PlacedGunEntity) entity).rotationYawGun, entity.rotationPitch);
			nbt.setInteger("barrelId", barrelId);
		}
	}
	public void dropCartridge(World world,Entity entity,ItemStack itemStack){
		HMGEntityBulletCartridge var8;
		for(int i=0;i < gunInfo.cartentityCnt;i++) {
			String currentMagCart = currentMagazine_cartridgeModelName(itemStack);
			if(currentMagCart == null){
				if (!gunInfo.hascustomcartridgemodel)
					var8 = new HMGEntityBulletCartridge(world, entity, gunInfo.cartType);
				else {
					var8 = new HMGEntityBulletCartridge(world, entity, -1, gunInfo.bulletmodelCart);
				}
			}else
				var8 = new HMGEntityBulletCartridge(world, entity, -1, currentMagCart);
			var8.rotationYaw += 90;
			Item cartItem = currentMagazine_cartridgeItem(itemStack);
			if(cartItem != null)var8.itemStack = new ItemStack(cartItem);
			world.spawnEntityInWorld(var8);
		}
	}
	public HMGEntityBulletBase[] getBullet(World par2World, Entity par3Entity){
		HMGEntityBulletBase[] bulletinstances = new HMGEntityBulletBase[firetemp.pellet];
		for(int i = 0; i < firetemp.pellet; i++){
			bulletinstances[i] = new HMGEntityBullet(par2World, par3Entity,
					firetemp.power, firetemp.speed, firetemp.spread, firetemp.model != null?firetemp.model:gunInfo.bulletmodelN);
		}
		return bulletinstances;
	}
	public HMGEntityBulletBase[] getBulletAP(World par2World, Entity par3Entity){
		HMGEntityBulletBase[] bulletinstances = new HMGEntityBulletBase[firetemp.pellet];
		for(int i = 0; i < firetemp.pellet; i++){
			bulletinstances[i] = new HMGEntityBullet_AP(par2World, par3Entity,
					firetemp.power, firetemp.speed, firetemp.spread, firetemp.model != null?firetemp.model:gunInfo.bulletmodelAP);
		}
		return bulletinstances;
	}
	public HMGEntityBulletBase[] FireBulletFrag(World par2World, Entity par3Entity){
		if(gunInfo.guntype == 1) {
			HMGEntityBulletBase[] bulletinstances = new HMGEntityBulletBase[1];
			bulletinstances[0] = new HMGEntityBullet_Frag(par2World, par3Entity,
					firetemp.power, firetemp.speed, firetemp.spread, firetemp.model != null?firetemp.model:gunInfo.bulletmodelFrag);

			return bulletinstances;
		}else {
			HMGEntityBulletBase[] bulletinstances = new HMGEntityBulletBase[firetemp.pellet];
			for(int i = 0; i < firetemp.pellet; i++){
				bulletinstances[i] = new HMGEntityBullet_Frag(par2World, par3Entity,
						firetemp.power, firetemp.speed, firetemp.spread, firetemp.model != null?firetemp.model:gunInfo.bulletmodelFrag);
			}
			return bulletinstances;
		}
	}
	public HMGEntityBulletBase[] FireBulletAT(World par2World, Entity par3Entity){
		HMGEntityBulletBase[] bulletinstances = new HMGEntityBulletBase[firetemp.pellet];
		for(int i = 0; i < firetemp.pellet; i++){
			bulletinstances[i] = new HMGEntityBullet_AT(par2World, par3Entity,
					firetemp.power, firetemp.speed, firetemp.spread, firetemp.model != null?firetemp.model:gunInfo.bulletmodelAT);
		}
		return bulletinstances;
	}



	public HMGEntityBulletBase[] FireBulletGL(World par2World, Entity par3Entity){
		HMGEntityBulletBase[] bulletinstances = new HMGEntityBulletBase[firetemp.pellet];
		for(int i = 0; i < firetemp.pellet; i++){
			bulletinstances[i] = new HMGEntityBulletExprode(par2World, par3Entity,
					firetemp.power, firetemp.speed, firetemp.spread,firetemp.exlevel,firetemp.destroyBlock, firetemp.model != null?firetemp.model:gunInfo.bulletmodelGL);
		}
		return bulletinstances;
	}
	public HMGEntityBulletBase[] FireBulletTE(World par2World, Entity par3Entity){
		HMGEntityBulletBase[] bulletinstances = new HMGEntityBulletBase[firetemp.pellet];
		for(int i = 0; i < firetemp.pellet; i++){
			bulletinstances[i] = new HMGEntityBullet_TE(par2World, par3Entity,
					firetemp.power, firetemp.speed, firetemp.spread,firetemp.exlevel,firetemp.destroyBlock, firetemp.model != null?firetemp.model:gunInfo.bulletmodelTE);
		}
		return bulletinstances;
	}
	public HMGEntityBulletBase[] FireBulletRPG(World par2World, Entity par3Entity){
		//dawhg I just want to fix the MCH BS
		HMGEntityBulletBase[] bulletinstances = new HMGEntityBulletBase[firetemp.pellet];
		for(int i = 0; i < firetemp.pellet; i++){
			bulletinstances[i] = new HMGEntityBulletRocket(par2World, par3Entity,
					firetemp.power, firetemp.speed, firetemp.spread,firetemp.exlevel,firetemp.destroyBlock, firetemp.model != null?firetemp.model:gunInfo.bulletmodelRPG);
		}
		return bulletinstances;
	}
	public HMGEntityBulletBase[] FireBulletHE(World par2World, Entity par3Entity){
		HMGEntityBulletBase[] bulletinstances = new HMGEntityBulletBase[firetemp.pellet];
		for(int i = 0; i < firetemp.pellet; i++){
			bulletinstances[i] = new HMGEntityBullet_HE(par2World, par3Entity,
					firetemp.power, firetemp.speed, firetemp.spread, firetemp.model != null?firetemp.model: gunInfo.bulletmodelHE);
		}
		return bulletinstances;
	}
	public HMGEntityBulletBase[] FireBulletFrame(World par2World, Entity par3Entity){
		HMGEntityBulletBase[] bulletinstances = new HMGEntityBulletBase[firetemp.pellet];
		for(int i = 0; i < firetemp.pellet; i++){
			bulletinstances[i] = new HMGEntityBullet_Flame(par2World, par3Entity,
					firetemp.power, firetemp.speed, firetemp.spread, firetemp.model != null?firetemp.model: gunInfo.bulletmodelN);
		}
		return bulletinstances;
	}
	public HMGEntityBulletBase[] FireBulletTorp(World par2World, Entity par3Entity){
		HMGEntityBulletBase[] bulletinstances = new HMGEntityBulletBase[firetemp.pellet];
		for(int i = 0; i < firetemp.pellet; i++){
			bulletinstances[i] = new HMGEntityBulletTorp(par2World, par3Entity,
					firetemp.power, firetemp.speed, firetemp.spread,firetemp.exlevel,firetemp.destroyBlock, firetemp.model != null?firetemp.model:gunInfo.bulletmodelRPG);
		}
		return bulletinstances;
	}
	public ItemStack onItemRightClick(ItemStack par1ItemStack, World par2World, EntityPlayer par3EntityPlayer) {
		if (HandmadeGunsCore.cfg_SwapFireAndADSKeys) {
			return par1ItemStack;
		}
		checkTags(par1ItemStack);
		if (HandmadeGunsCore.cfg_SwapFireAndADSKeys) {
			par1ItemStack.getTagCompound().setBoolean("IsTriggered", false);
			return par1ItemStack;
		}
		triggerHeldGun(par1ItemStack);
		return par1ItemStack;
	}

	public void triggerHeldGun(ItemStack itemStack) {
		checkTags(itemStack);
		NBTTagCompound nbt = itemStack.getTagCompound();
		if(!gunInfo.needfix || nbt.getBoolean("HMGfixed")) {
			HMG_proxy.resetRightClickTimer();
			nbt.setBoolean("IsTriggered", true);
			nbt.setBoolean("set_up", true);
			nbt.setInteger("set_up_cnt", 10);
		}else {
			nbt.setBoolean("set_up", false);
			nbt.setInteger("set_up_cnt", 3);
		}
	}



	public void onPlayerStoppedUsing(ItemStack par1ItemStack, World par2World, EntityPlayer par3EntityPlayer, int par4) {
	}
	public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ)
	{
		return false;
	}
	public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack)
	{
		return HandmadeGunsCore.cfg_SwapFireAndADSKeys;
	}
	public int getMaxItemUseDuration(ItemStack p_77626_1_)
	{
		return 7200;
	}

	public float func_150893_a(ItemStack p_150893_1_, Block p_150893_2_)
	{
		return 0.8F;
	}
	public boolean isFull3D()
	{
		return true;
	}
	public EnumAction getItemUseAction(ItemStack par1ItemStack) {
		// Compatibility: external armour renderers (e.g. Flan's custom armour models)
		// key off EnumAction.bow + itemInUseCount to drive aimed arm transforms.
		return EnumAction.bow;
	}
	public boolean func_150897_b(Block p_150897_1_)
	{
		return p_150897_1_ == Blocks.web;
	}
	public boolean isWeaponReload(ItemStack itemstack, EntityPlayer entityplayer) {
		return remain_Bullet(itemstack) <= 0;
	}
	public boolean isWeaponFullAuto(ItemStack itemstack) {
		return false;
	}





	public void Flash(ItemStack par1ItemStack, World par2World, Entity entity,NBTTagCompound nbt){

		double ix = 0;
		double iy = 0;
		double iz = 0;
		if(entity instanceof PlacedGunEntity) {
			if(gunInfo.posGetter.multi_barrelpos == null) {
				Vec3 vec = Vec3.createVectorHelper(gunInfo.posGetter.barrelpos[0], gunInfo.posGetter.barrelpos[1], -gunInfo.posGetter.barrelpos[2]);
				vec = vec.addVector(-gunInfo.posGetter.turretRotationPitchPoint[0], -gunInfo.posGetter.turretRotationPitchPoint[1], -gunInfo.posGetter.turretRotationPitchPoint[2]);
				vec.rotateAroundX(-(float) toRadians(entity.rotationPitch));
				vec = vec.addVector(gunInfo.posGetter.turretRotationPitchPoint[0], gunInfo.posGetter.turretRotationPitchPoint[1], gunInfo.posGetter.turretRotationPitchPoint[2]);
				vec = vec.addVector(-gunInfo.posGetter.turretRotationYawPoint[0], -gunInfo.posGetter.turretRotationYawPoint[1], -gunInfo.posGetter.turretRotationYawPoint[2]);
				vec.rotateAroundY(-(float) toRadians(((PlacedGunEntity) entity).rotationYawGun - ((PlacedGunEntity) entity).baserotationYaw));
				vec = vec.addVector(gunInfo.posGetter.turretRotationYawPoint[0], gunInfo.posGetter.turretRotationYawPoint[1], gunInfo.posGetter.turretRotationYawPoint[2]);
				vec.rotateAroundY(-(float) toRadians(((PlacedGunEntity) entity).baserotationYaw));
				ix = vec.xCoord;
				iy = vec.yCoord + entity.getEyeHeight();
				iz = vec.zCoord;
			}else {
				int barrelId = nbt.getInteger("barrelId");
				if(barrelId >= gunInfo.posGetter.multi_barrelpos.length)barrelId = 0;
				Vec3 vec = Vec3.createVectorHelper(gunInfo.posGetter.multi_barrelpos[barrelId][0], gunInfo.posGetter.multi_barrelpos[barrelId][1], -gunInfo.posGetter.multi_barrelpos[barrelId][2]);
				vec = vec.addVector(-gunInfo.posGetter.turretRotationPitchPoint[0], -gunInfo.posGetter.turretRotationPitchPoint[1], -gunInfo.posGetter.turretRotationPitchPoint[2]);
				vec.rotateAroundX(-(float) toRadians(entity.rotationPitch));
				vec = vec.addVector(gunInfo.posGetter.turretRotationPitchPoint[0], gunInfo.posGetter.turretRotationPitchPoint[1], gunInfo.posGetter.turretRotationPitchPoint[2]);
				vec = vec.addVector(-gunInfo.posGetter.turretRotationYawPoint[0], -gunInfo.posGetter.turretRotationYawPoint[1], -gunInfo.posGetter.turretRotationYawPoint[2]);
				vec.rotateAroundY(-(float) toRadians(((PlacedGunEntity) entity).rotationYawGun - ((PlacedGunEntity) entity).baserotationYaw));
				vec = vec.addVector(gunInfo.posGetter.turretRotationYawPoint[0], gunInfo.posGetter.turretRotationYawPoint[1], gunInfo.posGetter.turretRotationYawPoint[2]);
				vec.rotateAroundY(-(float) toRadians(((PlacedGunEntity) entity).baserotationYaw));
				ix = vec.xCoord;
				iy = vec.yCoord + entity.getEyeHeight();
				iz = vec.zCoord;
			}
		}else {
			float f1 = (entity instanceof EntityLivingBase ? ((EntityLivingBase) entity).rotationYawHead : entity.rotationYaw) * (2 * (float) Math.PI / 360);
			float f2 = entity.rotationPitch * (2 * (float) Math.PI / 360);
			//if (entity.isSneaking())
			if (!HandmadeGunsCore.Key_ADS(entity)) {
				ix -= MathHelper.sin(f1) * MathHelper.cos(f2) + MathHelper.cos(-f1) * 0.2;
				iy = entity.getEyeHeight() - 0.1 - MathHelper.sin(f2);
				iz += MathHelper.cos(f1) * MathHelper.cos(f2) + MathHelper.sin(-f1) * 0.2;
			} else {
				ix -= MathHelper.sin(f1) * MathHelper.cos(f2);
				iy = entity.getEyeHeight() - 0.1 - MathHelper.sin(f2);
				iz += MathHelper.cos(f1) * MathHelper.cos(f2);
			}
		}
		if (HandmadeGunsCore.cfg_muzzleflash && guntemp.muzzle && gunInfo.muzzleflash) {
			if(!par2World.isRemote) {
				PacketSpawnParticle packet;
				if(gunInfo.flashname != null){
					packet = new PacketSpawnParticle(entity.posX + ix, entity.posY + iy, entity.posZ + iz, (entity instanceof EntityLivingBase ? ((EntityLivingBase)entity).rotationYawHead : entity.rotationYaw),entity.rotationPitch,0, gunInfo.flashname,true);
					packet.id = 100;
				}else {
					packet = new PacketSpawnParticle(entity.posX + ix, entity.posY + iy, entity.posZ + iz,
							(entity instanceof EntityLivingBase ? ((EntityLivingBase)entity).rotationYawHead : entity.rotationYaw),
							entity.rotationPitch,0, 100);
				}
				packet.scale = gunInfo.flashScale;
				packet.fuse = gunInfo.flashfuse;
				HMGPacketHandler.INSTANCE.sendToAll(packet);
			}
		}
	}
	public boolean checkTags(ItemStack pitemstack) {
		if (pitemstack.hasTagCompound()) {
				/*NBTTagCompound lnbt = pitemstack.getTagCompound();
				byte lre = lnbt.getByte("RloadTime");
				if(lre != 0){
					return false;
				}else{

				}*/
			return true;
		}
		NBTTagCompound ltags = new NBTTagCompound();
		pitemstack.setTagCompound(ltags);
		ltags.setBoolean("IsReloading",false);
		ltags.setInteger("Reload", 0x0000);
		ltags.setBoolean("SeekerOpened",true);
		ltags.setTag("Items", new NBTTagList());
		ltags.setBoolean("HMGInfiniteAmmoInitialized", false);
		int defaultMode = getDefaultFireMode();
		ltags.setInteger("HMGMode", defaultMode);
		ltags.setInteger("RemainBurstround", getburstCount(defaultMode));
		return false;
	}

	private int getDefaultFireMode() {
		int modeCount = min(gunInfo.burstcount.size(), gunInfo.rates.size());
		for (int mode = 0; mode < modeCount; mode++) {
			if (gunInfo.burstcount.get(mode) == -1) return mode;
		}
		return 0;
	}

	/**
	 * Creates the normal loaded-magazine NBT once for an infinite-ammo owner.
	 * Firing still damages those stacks; only the normal reload commit supplies a
	 * fresh marked magazine later.
	 */
	private void initializeInfiniteAmmo(ItemStack gunStack) {
		checkTags(gunStack);
		NBTTagCompound nbt = gunStack.getTagCompound();
		if (nbt.getBoolean("HMGInfiniteAmmoInitialized")) return;
		Item selectedMagazine = get_selectingMagazine(gunStack);
		if (selectedMagazine != null) {
			ItemStack[] magazines = new ItemStack[gunInfo.magazineItemCount];
			for (int slot = 0; slot < magazines.length; slot++) {
				magazines[slot] = handmadeguns.Util.HMGAmmoPolicy.suppliedMagazine(selectedMagazine);
			}
			set_loadedMagazineStack(gunStack, magazines);
		}
		if (!currentMagzine_has_roundOption(gunStack)) gunStack.setItemDamage(0);
		nbt.setInteger("getcurrentMagazine", nbt.getInteger("get_selectingMagazine"));
		nbt.setBoolean("HMGInfiniteAmmoInitialized", true);
	}
	protected boolean cycleBolt(ItemStack pItemstack) {
		NBTTagCompound lnbt = pItemstack.getTagCompound();
		byte lb = lnbt.getByte("Bolt");
		lb--;
		if (lb <= 0) {
			lnbt.setBoolean("Recoiled", true);
			lnbt.setByte("Bolt", (byte) 0);
			return true;
		} else {
			lnbt.setBoolean("Recoiled", false);
			lnbt.setByte("Bolt", lb);
			return false;
		}
	}
	protected void resetBolt(ItemStack pItemstack) {
		NBTTagCompound nbt = pItemstack.getTagCompound();
		nbt.setFloat("rotex",nbt.getFloat("rotex") + gunInfo.mat31rotex);
		nbt.setFloat("rotey",nbt.getFloat("rotey") + gunInfo.mat31rotey);
		nbt.setFloat("rotez",nbt.getFloat("rotez") + gunInfo.mat31rotez);
		pItemstack.getTagCompound().setBoolean("Recoiled", false);
		pItemstack.getTagCompound().setByte("Bolt", getCycleCount(pItemstack));
	}
	public byte getCycleCount(ItemStack pItemstack) {
		boolean addPlusOne = gunInfo.cycle%1 > itemRand.nextFloat();
		byte temp = (byte)((gunInfo.cycle - gunInfo.cycle%1) + (addPlusOne?1:0));
		if(temp == 0){
			temp = 1;
		}
		return temp;
	}
	public void proceedreload(ItemStack itemstack , World world , Entity entity , NBTTagCompound nbt, int i){
		nbt.setInteger("getcurrentMagazine", nbt.getInteger("get_selectingMagazine"));
		boolean perShellReload = isPerShellReload(itemstack);
		if (perShellReload && handlePerShellInterruptWindow(itemstack, world, entity, nbt)) {
			return;
		}

		try {
			if(guntemp.invocable != null) {
				guntemp.invocable.invokeFunction("proceedreload", this, itemstack, nbt, entity);
			}
		} catch (ScriptException | NoSuchMethodException e) {
			e.printStackTrace();
		}

		if (perShellReload && gunInfo.perShellReloadStages
				&& nbt.getInteger(PER_SHELL_PHASE_TAG) == PER_SHELL_PHASE_INTRO) {
			int introDuration = nbt.getBoolean(PER_SHELL_STARTED_EMPTY_TAG)
					? gunInfo.perShellReloadEmptyIntroTime : gunInfo.perShellReloadIntroTime;
			if (!nbt.getBoolean("IsReloading") || remain_Bullet(itemstack) >= max_Bullet(itemstack)
					|| !canreloadBullets(itemstack, world, entity)) {
				finishPerShellPresentation(itemstack, world, entity, nbt);
				nbt.setBoolean("IsReloading", false);
				nbt.setBoolean("WaitReloading", true);
				nbt.setInteger(PER_SHELL_PHASE_TAG, PER_SHELL_PHASE_NONE);
				nbt.setInteger(PER_SHELL_INTRO_TIME_TAG, 0);
				return;
			}
			int introTime = nbt.getInteger(PER_SHELL_INTRO_TIME_TAG) + 1;
			nbt.setBoolean("WaitReloading", false);
			if (introTime >= Math.max(0, introDuration)) {
				nbt.setInteger(PER_SHELL_PHASE_TAG, PER_SHELL_PHASE_INSERT);
				nbt.setInteger(PER_SHELL_INTRO_TIME_TAG, 0);
				nbt.setInteger("RloadTime", 0);
				sendReloadPresentation(itemstack, world, entity, nbt, ReloadAnimationBridge.Stage.INSERT);
			} else {
				nbt.setInteger(PER_SHELL_INTRO_TIME_TAG, introTime);
			}
			return;
		}

		int reloadti = nbt.getInteger("RloadTime");

		// Advance only an explicit/manual reload for players; non-player users may still be auto-started by gunProcess.
		if (nbt.getBoolean("IsReloading") && (remain_Bullet(itemstack) < max_Bullet(itemstack) || gunInfo.isOneuse)) {
			if (canreloadBullets(itemstack, world, entity)) {
				if (!world.isRemote && reloadti == 0 && !gunInfo.isOneuse && !gunInfo.animationEventSounds) {
					HMGPacketHandler.INSTANCE.sendToAll(new PacketPlaysound(entity, gunInfo.soundre.length > nbt.getInteger("getcurrentMagazine") ? gunInfo.soundre[nbt.getInteger("getcurrentMagazine")] : gunInfo.soundre[0], gunInfo.soundrespeed, gunInfo.soundrelevel, true));
				}

				// Reload time countdown
				++reloadti;
				nbt.setBoolean("WaitReloading", false);
				
			} else {
				if (perShellReload) finishPerShellPresentation(itemstack, world, entity, nbt);
				nbt.setBoolean("IsReloading", false);
				nbt.setBoolean("WaitReloading", true);
				nbt.setInteger(PER_SHELL_PHASE_TAG, PER_SHELL_PHASE_NONE);
			}
		} else {
			reloadti = 0;
		}

		// Perform reload logic when reload time reaches its limit
		if (!world.isRemote) {
			if (reloadti >= reloadTime(itemstack)) {
				boolean shellCommitted = false;
				if (perShellReload) {
					shellCommitted = commitOnePerShellReload(itemstack, world, entity);
					syncCurrentGunStack(itemstack, entity, i);
				} else {
					resetReload(itemstack, world, entity, i);
				}

				boolean hasNextPerShellReload = perShellReload
						&& shellCommitted
						&& remain_Bullet(itemstack) < max_Bullet(itemstack)
						&& canreloadBullets(itemstack, world, entity);
				if (perShellReload) {
					if (hasNextPerShellReload) {
						nbt.setInteger(PER_SHELL_PHASE_TAG, PER_SHELL_PHASE_NONE);
					} else {
						finishPerShellPresentation(itemstack, world, entity, nbt);
						nbt.setInteger(PER_SHELL_PHASE_TAG, PER_SHELL_PHASE_NONE);
					}
				}

				// Reset cocking state after reload
				if (gunInfo.needFirstCock) {
					nbt.setBoolean("cocking", false);
				} else {
					nbt.setBoolean("cocking", true);
				}

				nbt.setBoolean("IsReloading", false);
				nbt.setBoolean("WaitReloading", false);
				nbt.setInteger("RloadTime", 0);
				nbt.setInteger(PER_SHELL_INTERRUPT_TIME_TAG, hasNextPerShellReload ? getPerShellInterruptTicks(nbt) : 0);
				nbt.setBoolean("Bursting", false);
				nbt.setInteger("RemainBurstround", getburstCount(nbt.getInteger("HMGMode")));
			} else if (nbt.getBoolean("IsReloading")) {
				nbt.setInteger("RloadTime", reloadti);
			}
		} else {
			nbt.setInteger("RloadTime", reloadti);
		}
	}

	public boolean startPerShellReloadFromKey(ItemStack itemstack, World world, Entity entity) {
		return startReloadFromKey(itemstack, world, entity);
	}

	public boolean startReloadFromKey(ItemStack itemstack, World world, Entity entity) {
		checkTags(itemstack);
		NBTTagCompound nbt = itemstack.getTagCompound();
		if (isPerShellReload(itemstack) && nbt.getInteger(PER_SHELL_INTERRUPT_TIME_TAG) > 0) {
			finishPerShellPresentation(itemstack, world, entity, nbt);
			nbt.setInteger(PER_SHELL_INTERRUPT_TIME_TAG, 0);
			nbt.setBoolean("IsReloading", false);
			nbt.setBoolean("WaitReloading", false);
			nbt.setInteger("RloadTime", 0);
			return true;
		}
		if (nbt.getBoolean("IsReloading") || remain_Bullet(itemstack) >= max_Bullet(itemstack) || !canreloadBullets(itemstack, world, entity)) {
			return false;
		}

		nbt.setBoolean("IsReloading", true);
		nbt.setBoolean("WaitReloading", false);
		nbt.setInteger("RloadTime", 0);
		nbt.setInteger(PER_SHELL_INTERRUPT_TIME_TAG, 0);
		if (isPerShellReload(itemstack) && gunInfo.perShellReloadStages) {
			boolean empty = remain_Bullet(itemstack) == 0;
			int introDuration = empty ? gunInfo.perShellReloadEmptyIntroTime : gunInfo.perShellReloadIntroTime;
			nbt.setInteger(PER_SHELL_PHASE_TAG,
					introDuration > 0 ? PER_SHELL_PHASE_INTRO : PER_SHELL_PHASE_INSERT);
			nbt.setInteger(PER_SHELL_INTRO_TIME_TAG, 0);
			nbt.setBoolean(PER_SHELL_PRESENTATION_ACTIVE_TAG, true);
			nbt.setBoolean(PER_SHELL_STARTED_EMPTY_TAG, empty);
		} else {
			nbt.setInteger(PER_SHELL_PHASE_TAG, PER_SHELL_PHASE_NONE);
		}
		nbt.setBoolean("Bursting", false);
		nbt.setInteger("RemainBurstround", getburstCount(nbt.getInteger("HMGMode")));
		return true;
	}

	public ReloadAnimationBridge.Stage reloadPresentationStage(ItemStack itemstack) {
		if (!isPerShellReload(itemstack) || !gunInfo.perShellReloadStages || itemstack.getTagCompound() == null)
			return ReloadAnimationBridge.Stage.STANDARD;
		return itemstack.getTagCompound().getInteger(PER_SHELL_PHASE_TAG) == PER_SHELL_PHASE_INTRO
				? ReloadAnimationBridge.Stage.INTRO : ReloadAnimationBridge.Stage.INSERT;
	}

	private boolean requiresManualReload(Entity entity) {
		return entity instanceof EntityPlayer || entity.riddenByEntity instanceof EntityPlayer;
	}

	private boolean isPerShellReload(ItemStack itemStack) {
		return itemStack != null
				&& gunInfo.perShellReload
				&& gunInfo.magazineItemCount > 1
				&& get_selectingMagazine(itemStack) != null;
	}

	private boolean shouldInterruptPerShellReload(Entity entity, NBTTagCompound nbt) {
		return shouldInterruptPerShellReload(entity, nbt, false);
	}

	private boolean shouldInterruptPerShellReload(Entity entity, NBTTagCompound nbt, boolean shellInsertionComplete) {
		return nbt.getBoolean("IsTriggered")
				&& (shellInsertionComplete || nbt.getInteger("RloadTime") == 0)
				&& (entity instanceof EntityPlayer || entity.riddenByEntity instanceof EntityPlayer);
	}

	private boolean isPerShellInsertionInProgress(ItemStack itemstack, NBTTagCompound nbt) {
		return isPerShellReload(itemstack) && nbt.getBoolean("IsReloading") && nbt.getInteger("RloadTime") > 0;
	}

	private boolean handlePerShellInterruptWindow(ItemStack itemstack, World world, Entity entity, NBTTagCompound nbt) {
		int interruptTime = nbt.getInteger(PER_SHELL_INTERRUPT_TIME_TAG);
		if (interruptTime <= 0) {
			return false;
		}
		nbt.setBoolean("IsReloading", false);
		nbt.setBoolean("WaitReloading", false);
		nbt.setInteger("RloadTime", 0);
		if (shouldInterruptPerShellReload(entity, nbt, true)) {
			finishPerShellPresentation(itemstack, world, entity, nbt);
			nbt.setInteger(PER_SHELL_INTERRUPT_TIME_TAG, 0);
			nbt.setInteger(PER_SHELL_PHASE_TAG, PER_SHELL_PHASE_NONE);
			return true;
		}
		interruptTime--;
		nbt.setInteger(PER_SHELL_INTERRUPT_TIME_TAG, interruptTime);
		if (interruptTime <= 0 && remain_Bullet(itemstack) < max_Bullet(itemstack) && canreloadBullets(itemstack, world, entity)) {
			nbt.setBoolean("IsReloading", true);
			nbt.setBoolean("WaitReloading", false);
			nbt.setInteger("RloadTime", 0);
			if (gunInfo.perShellReloadStages) {
				nbt.setInteger(PER_SHELL_PHASE_TAG, PER_SHELL_PHASE_INSERT);
				sendReloadPresentation(itemstack, world, entity, nbt, ReloadAnimationBridge.Stage.INSERT);
			}
		} else if (interruptTime <= 0) {
			finishPerShellPresentation(itemstack, world, entity, nbt);
			nbt.setInteger(PER_SHELL_PHASE_TAG, PER_SHELL_PHASE_NONE);
		}
		return true;
	}

	private void finishPerShellPresentation(ItemStack itemstack, World world, Entity entity, NBTTagCompound nbt) {
		if (!gunInfo.perShellReloadStages || !nbt.getBoolean(PER_SHELL_PRESENTATION_ACTIVE_TAG)) return;
		sendReloadPresentation(itemstack, world, entity, nbt, ReloadAnimationBridge.Stage.END);
		nbt.setBoolean(PER_SHELL_PRESENTATION_ACTIVE_TAG, false);
	}

	private void sendReloadPresentation(ItemStack itemstack, World world, Entity entity, NBTTagCompound nbt,
									ReloadAnimationBridge.Stage stage) {
		if (world.isRemote) return;
		EntityPlayerMP player = entity instanceof EntityPlayerMP ? (EntityPlayerMP)entity
				: entity.riddenByEntity instanceof EntityPlayerMP ? (EntityPlayerMP)entity.riddenByEntity : null;
		if (player == null || player.getHeldItem() != itemstack) return;
		ReloadAnimationBridge.StartEvent event = ReloadAnimationBridge.nextEvent(player.inventory.currentItem,
				Item.getIdFromItem(itemstack.getItem()), nbt.getBoolean(PER_SHELL_STARTED_EMPTY_TAG), stage);
		HMGPacketHandler.INSTANCE.sendTo(new PacketReloadAnimation(event), player);
	}

	private int getPerShellInterruptTicks(NBTTagCompound nbt) {
		if (nbt.hasKey("PerShellInterruptTicks")) {
			return Math.max(0, nbt.getInteger("PerShellInterruptTicks"));
		}
		return DEFAULT_PER_SHELL_INTERRUPT_TICKS;
	}

	private void syncCurrentGunStack(ItemStack itemstack, Entity entity, int slot) {
		IInventory inventory = getInventory_fromEntity(entity);
		if (inventory != null) {
			if (slot >= 0 && slot < inventory.getSizeInventory()) {
				inventory.setInventorySlotContents(slot, itemstack);
			}
			inventory.markDirty();
		}
	}

	public boolean commitOnePerShellReload(ItemStack itemstack, World world, Entity entity) {
		if (world.isRemote) return false;
		int currentLoadedAmmo = remain_Bullet(itemstack);
		if (currentLoadedAmmo >= max_Bullet(itemstack)) {
			return false;
		}
		if (handmadeguns.Util.HMGAmmoPolicy.hasInfiniteAmmo(entity) && get_selectingMagazine(itemstack) != null) {
			return commitOnePerShellReload(itemstack, world, null, currentLoadedAmmo);
		}

		IInventory inventory = getInventory_VehicleCheck(entity);
		if (inventory != null && commitOnePerShellReload(itemstack, world, inventory, currentLoadedAmmo)) {
			return true;
		}

		inventory = getInventory_fromEntity(entity);
		return inventory != null && commitOnePerShellReload(itemstack, world, inventory, currentLoadedAmmo);
	}

	private boolean commitOnePerShellReload(ItemStack itemstack, World world, IInventory inventory, int currentLoadedAmmo) {
		StackAndSlot reserveShell = inventory == null
				? new StackAndSlot(-1, handmadeguns.Util.HMGAmmoPolicy.suppliedMagazine(get_selectingMagazine(itemstack)))
				: searchMagazines(itemstack, world, inventory);
		if (reserveShell == null || reserveShell.stack == null || reserveShell.stack.stackSize <= 0) {
			return false;
		}

		ItemStack[] magazines = null;
		int loadedMagazineSlot = -1;
		if (currentMagzine_has_roundOption(itemstack)) {
			magazines = get_loadedMagazineStack(itemstack);
			Item selectedMagazine = get_selectingMagazine(itemstack);
			for (int magazineSlot = 0; magazineSlot < magazines.length; magazineSlot++) {
				ItemStack magazine = magazines[magazineSlot];
				if (magazine != null && magazine.getItem() == selectedMagazine && magazine.getItemDamage() > 0) {
					loadedMagazineSlot = magazineSlot;
					break;
				}
			}
			if (loadedMagazineSlot < 0) {
				for (int magazineSlot = 0; magazineSlot < magazines.length; magazineSlot++) {
					if (magazines[magazineSlot] == null) {
						loadedMagazineSlot = magazineSlot;
						break;
					}
				}
			}
			if (loadedMagazineSlot < 0) {
				return false;
			}
		}

		if (inventory != null) {
			reserveShell.stack.stackSize--;
			if (reserveShell.stack.stackSize <= 0) {
				inventory.setInventorySlotContents(reserveShell.slot, null);
			} else {
				inventory.setInventorySlotContents(reserveShell.slot, reserveShell.stack);
			}
			inventory.markDirty();
		}

		if (currentMagzine_has_roundOption(itemstack)) {
			ItemStack loadedMagazine = magazines[loadedMagazineSlot];
			if (loadedMagazine == null) {
				loadedMagazine = new ItemStack(get_selectingMagazine(itemstack), 1);
				if (inventory == null) handmadeguns.Util.HMGAmmoPolicy.markSupplied(loadedMagazine);
				loadedMagazine.setItemDamage(loadedMagazine.getMaxDamage() - 1);
				magazines[loadedMagazineSlot] = loadedMagazine;
			} else {
				loadedMagazine.setItemDamage(loadedMagazine.getItemDamage() - 1);
			}
			set_loadedMagazineStack(itemstack, magazines);
		} else {
			itemstack.setItemDamage(itemstack.getMaxDamage() - (currentLoadedAmmo + 1));
		}
		itemstack.getTagCompound().setInteger("getcurrentMagazine", itemstack.getTagCompound().getInteger("get_selectingMagazine"));
		return true;
	}

	public void resetReload(ItemStack par1ItemStack, World par2World, Entity entity, int i) {
		if(guntemp != null)guntemp.items[5] = null;//撃ち終わりに特殊弾を消去
		if(par1ItemStack.stackSize > 0){
			reloadBullets(par1ItemStack, par2World, entity);
		}
//        int l;
//        for(l = 0; l< gunInfo.magazineItemCount; l++) {
//            if( entity instanceof EntityPlayer) {
//                if(!((EntityPlayer) entity).inventory.consumeInventoryItem(getcurrentMagazine(par1ItemStack))) {
//                    break;
//                }
////                    System.out.println("debug" + this.getMaxDamage() * (l - 1) / magazineCount);
//            } else if( entity.riddenByEntity instanceof EntityPlayer) {
//                if(!((EntityPlayer) entity.riddenByEntity).inventory.consumeInventoryItem(getcurrentMagazine(par1ItemStack))) {
//                    break;
//                }
////                    System.out.println("debug" + this.getMaxDamage() * (l - 1) / magazineCount);
//            } else if(islmmloaded && entity instanceof LMM_EntityLittleMaid){
//                if(!((LMM_EntityLittleMaid) entity).maidInventory.consumeInventoryItem(getcurrentMagazine(par1ItemStack))) {
//                    break;
//                }
//            }
//        }

	}

	private void consumeOneUseWeapon(ItemStack firedStack, ItemStack stateBeforeUse, Entity entity, int inventorySlot) {
		IInventory vehicleInventory = getInventory_VehicleCheck(entity);
		if (consumeExactStack(firedStack, stateBeforeUse, vehicleInventory, inventorySlot)) {
			return;
		}

		IInventory owningInventory = getInventory_fromEntity(entity);
		if (owningInventory != vehicleInventory && consumeExactStack(firedStack, stateBeforeUse, owningInventory, inventorySlot)) {
			return;
		}

		// Placed guns and inventory-less mounts own the stack directly rather than
		// exposing it through IInventory.
		decrementOneUseStack(firedStack, stateBeforeUse);
		if (entity instanceof PlacedGunEntity && firedStack.stackSize <= 0) {
			((PlacedGunEntity) entity).gunStack = null;
			((PlacedGunEntity) entity).gunItem = null;
		}
	}

	private boolean consumeExactStack(ItemStack firedStack, ItemStack stateBeforeUse, IInventory inventory, int preferredSlot) {
		if (inventory == null) {
			return false;
		}
		if (preferredSlot >= 0 && preferredSlot < inventory.getSizeInventory()
				&& inventory.getStackInSlot(preferredSlot) == firedStack) {
			decrementOneUseStackInInventory(firedStack, stateBeforeUse, inventory, preferredSlot);
			return true;
		}
		for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
			if (inventory.getStackInSlot(slot) == firedStack) {
				decrementOneUseStackInInventory(firedStack, stateBeforeUse, inventory, slot);
				return true;
			}
		}
		return false;
	}

	private void decrementOneUseStackInInventory(ItemStack firedStack, ItemStack stateBeforeUse, IInventory inventory, int slot) {
		decrementOneUseStack(firedStack, stateBeforeUse);
		inventory.setInventorySlotContents(slot, firedStack.stackSize > 0 ? firedStack : null);
		inventory.markDirty();
	}

	private void decrementOneUseStack(ItemStack firedStack, ItemStack stateBeforeUse) {
		firedStack.stackSize--;
		if (firedStack.stackSize > 0 && stateBeforeUse != null) {
			// ItemStack damage and loaded-magazine NBT belong to the whole stack. Put
			// the unthrown disposable items back into their pre-shot ammunition state.
			firedStack.setItemDamage(stateBeforeUse.getItemDamage());
			for (int magazineSlot = 0; magazineSlot < gunInfo.magazineItemCount; magazineSlot++) {
				String tagName = "LoadedMagazine" + magazineSlot;
				if (stateBeforeUse.getTagCompound().hasKey(tagName)) {
					firedStack.getTagCompound().setTag(tagName, stateBeforeUse.getTagCompound().getTag(tagName).copy());
				} else {
					firedStack.getTagCompound().removeTag(tagName);
				}
			}
		}
	}

	public double getTerminalspeed(){
		if(gunInfo.acceleration > 0){
			//A*0.9 + 0.1 = A
			//(1.0 - 0.9) * A = 0.1
			//A = 0.1 * (1.0 - 0.9)
			//Terminal = acceleration * (1 - resistance)
			return gunInfo.acceleration/(1-gunInfo.resistance);
		}else
			return gunInfo.speed;
	}

	public boolean consumeInventoryItem(Item p_146026_1_,IInventory iInventory,int i)
	{
		if(iInventory != null) {
			if(i>0){
				ItemStack stackOnSlot =  iInventory.getStackInSlot(i);
				if(stackOnSlot != null && stackOnSlot.getItem() == p_146026_1_) {
					stackOnSlot.stackSize--;
					if (stackOnSlot.stackSize <= 0) {
						iInventory.setInventorySlotContents(i, null);
					}
					return true;
				}
			}
			int i2 = this.func_146029_c(p_146026_1_, iInventory);

			if (i2 < 0) {
				return false;
			} else {
				iInventory.getStackInSlot(i2).stackSize--;
				if (iInventory.getStackInSlot(i2).stackSize <= 0) {
					iInventory.setInventorySlotContents(i2, null);
				}

				return true;
			}
		}
		return false;
	}
	private int func_146029_c(Item p_146029_1_,IInventory iInventory)
	{
		for (int i = 0; i < iInventory.getSizeInventory(); ++i)
		{
			if (iInventory.getStackInSlot(i) != null && iInventory.getStackInSlot(i).getItem() == p_146029_1_)
			{
				return i;
			}
		}

		return -1;
	}
	public int remain_Bullet(ItemStack gunStack){
		if(!currentMagzine_has_roundOption(gunStack)){
			return this.getMaxDamage() - gunStack.getItemDamage();
		}else {
			ItemStack[] magazines = get_loadedMagazineStack(gunStack);
			int remainBullets = 0;
			for(ItemStack magazinestack : magazines){
				if(magazinestack != null){
					remainBullets += magazinestack.getMaxDamage() - magazinestack.getItemDamage();
				}
			}
			return remainBullets;
		}

	}
	public int max_Bullet(ItemStack gunStack){
		if(!currentMagzine_has_roundOption(gunStack)){
			return this.getMaxDamage();
		}else {
			return getcurrentMagazine(gunStack).getMaxDamage() * gunInfo.magazineItemCount;

		}

	}
	public void reloadBullets(ItemStack itemstack, World world, Entity entity){
		itemstack.getTagCompound().setBoolean("detached",false);
		if (handmadeguns.Util.HMGAmmoPolicy.hasInfiniteAmmo(entity) && get_selectingMagazine(itemstack) != null) {
			if (isPerShellReload(itemstack)) commitOnePerShellReload(itemstack, world, entity);
			else consumeAndSetMagazine(itemstack, world, null, true);
			return;
		}

		if(isPerShellReload(itemstack)){
			boolean loadedShell = false;
			IInventory inventory = getInventory_VehicleCheck(entity);
			if(inventory != null){
				loadedShell = consumeAndSetMagazine(itemstack,world,inventory);
			}
			if(!loadedShell){
				inventory = getInventory_fromEntity(entity);
				if(inventory != null){
					loadedShell = consumeAndSetMagazine(itemstack,world,inventory);
				}
			}
			if(!loadedShell){
				NBTTagCompound nbt = itemstack.getTagCompound();
				nbt.setBoolean("IsReloading", false);
				nbt.setBoolean("WaitReloading", false);
				nbt.setInteger("RloadTime", 0);
			}
			return;
		}

		IInventory inventory = getInventory_VehicleCheck(entity);
		boolean flag = false;
		if(inventory != null && searchMagazines(itemstack,world,inventory)!= null){
			if(get_selectingMagazine(itemstack) != null)flag = consumeAndSetMagazine(itemstack,world,inventory);
			else {flag = true ; setMagazine(itemstack,world);}
		}
		inventory = getInventory_fromEntity(entity);
		if(inventory != null && !flag){
			if(get_selectingMagazine(itemstack) != null)consumeAndSetMagazine(itemstack,world,inventory);
			else setMagazine(itemstack,world);
		}else {
			setMagazine(itemstack,world);
		}


	}
	public boolean canreloadBullets(ItemStack itemstack, World world, Entity entity){
		Entity SACLOSCheck = world.getEntityByID(entity.getEntityData().getInteger("SACLOS_HOMING"));
		if(SACLOSCheck instanceof HMGEntityBulletBase && ((HMGEntityBulletBase) SACLOSCheck).SACLOS_Homing && this.gunInfo.SACLOS_Homing){
			return false;
		}
		if(guntemp.currentConnectedTurret != null && !guntemp.currentConnectedTurret.prefab_turret.canReloadAirBone && !guntemp.currentConnectedTurret.motherEntity.onGround){
			return false;
		}
		if(get_selectingMagazine(itemstack) == null)return true;
		if(handmadeguns.Util.HMGAmmoPolicy.hasInfiniteAmmo(entity))return true;
		IInventory inventory = getInventory_VehicleCheck(entity);
		if(inventory != null && searchMagazines(itemstack,world,inventory)!= null){
			return true;
		}
		inventory = getInventory_fromEntity(entity);
		if(inventory != null)return searchMagazines(itemstack,world,inventory) != null;
		else return !(entity instanceof PlacedGunEntity) || !(entity.riddenByEntity == null || entity.riddenByEntity instanceof EntityPlayer);
	}
	public IInventory getInventory_VehicleCheck(Entity entity){
		if(!(entity instanceof EntityLiving) && guntemp != null && guntemp.currentConnectedTurret != null && !guntemp.currentConnectedTurret.prefab_turret.useVehicleInventory && guntemp.currentConnectedTurret.connectedInventory != null){
			return guntemp.currentConnectedTurret.connectedInventory;
		}
		return null;
	}
	public IInventory getInventory_fromEntity(Entity entity){
		if(!(entity instanceof EntityLiving) && guntemp != null && guntemp.currentConnectedTurret != null && guntemp.currentConnectedTurret.prefab_turret.useVehicleInventory && guntemp.currentConnectedTurret.connectedInventory != null){
			return guntemp.currentConnectedTurret.connectedInventory;
		}else
		if (entity instanceof EntityPlayer) {
			return ((EntityPlayer) entity).inventory;
		} else if (entity.riddenByEntity instanceof EntityPlayer){
			return ((EntityPlayer) entity.riddenByEntity).inventory;
		} else if(islmmloaded && entity instanceof LMM_EntityLittleMaid){
			return ((LMM_EntityLittleMaid) entity).maidInventory;
		} else if(islmmloaded && entity.riddenByEntity instanceof LMM_EntityLittleMaid){
			return ((LMM_EntityLittleMaid) entity.riddenByEntity).maidInventory;
		}
		return null;
	}
	public void setMagazine(ItemStack gunStack, World world){
		ItemStack[] mgazines = get_loadedMagazineStack(gunStack);
		for (int magazine_slot = 0;magazine_slot < gunInfo.magazineItemCount; magazine_slot++) {
			ItemStack stack = new ItemStack(get_selectingMagazine(gunStack),1);
			mgazines[magazine_slot] = stack;
		}

		if(!currentMagzine_has_roundOption(gunStack))gunStack.setItemDamage(0);
		gunStack.getTagCompound().setInteger("getcurrentMagazine", gunStack.getTagCompound().getInteger("get_selectingMagazine"));
		set_loadedMagazineStack(gunStack,mgazines);
	}
	public boolean consumeAndSetMagazine(ItemStack gunStack, World world, IInventory inventory){
		return inventory != null && consumeAndSetMagazine(gunStack, world, inventory, false);
	}
	private boolean consumeAndSetMagazine(ItemStack gunStack, World world, IInventory inventory, boolean infinite) {
		if (world.isRemote) return false;
		ItemStack[] magazines = get_loadedMagazineStack(gunStack);
		StackAndSlot[] stackAndSlots = new StackAndSlot[magazines.length];
		int magazine_cnt = countLoadedMagazines(magazines);
		int originalMagazineCount = magazine_cnt;
		boolean perShellReload = isPerShellReload(gunStack);
		if(perShellReload) {
			ItemStack[] compacted = new ItemStack[magazines.length];
			int next = 0;
			for(ItemStack magazine : magazines) if(magazine != null) compacted[next++] = magazine;
			magazines = compacted;
			magazine_cnt = next;
		}
		int currentLoadedAmmo = remain_Bullet(gunStack);
		int maxLoadedAmmo = max_Bullet(gunStack);
		StackAndSlot prevStackAndSlot = null;
		int cnt_useStackSlot = 0;
		int loadLimit = perShellReload ? Math.min(gunInfo.magazineItemCount, magazine_cnt + 1) : gunInfo.magazineItemCount;
		for (;magazine_cnt < loadLimit; magazine_cnt++) {
			if (perShellReload && currentLoadedAmmo >= maxLoadedAmmo) {
				break;
			}
			StackAndSlot stackAndSlot = infinite
					? new StackAndSlot(-1, handmadeguns.Util.HMGAmmoPolicy.suppliedMagazine(get_selectingMagazine(gunStack)))
					: searchMagazines(gunStack, world, inventory);
			if(stackAndSlot != null && stackAndSlot.stack.stackSize>0) {
				magazines[magazine_cnt] = stackAndSlot.stack.copy();
				magazines[magazine_cnt].stackSize = 1;
				if (infinite) continue;
				stackAndSlot.stack.stackSize--;
				if(prevStackAndSlot == null || prevStackAndSlot.slot != stackAndSlot.slot) {
					stackAndSlots[cnt_useStackSlot] = stackAndSlot;
					cnt_useStackSlot++;
				}
				prevStackAndSlot = stackAndSlot;
				inventory.markDirty();
			}else break;
		}
		for(StackAndSlot stackAndSlot:stackAndSlots) {
			if(stackAndSlot != null) {
				if (stackAndSlot.stack.stackSize > 0) {
					inventory.setInventorySlotContents(stackAndSlot.slot, stackAndSlot.stack);
				} else {
					inventory.setInventorySlotContents(stackAndSlot.slot, null);
				}
				inventory.markDirty();
			}
		}
		set_loadedMagazineStack(gunStack,magazines);
		gunStack.getTagCompound().setInteger("getcurrentMagazine", gunStack.getTagCompound().getInteger("get_selectingMagazine"));

		boolean consumedReserveAmmo = magazine_cnt > originalMagazineCount;
		if(perShellReload && consumedReserveAmmo) {
			gunStack.setItemDamage(getMaxDamage() - (currentLoadedAmmo + 1));
		} else if(!currentMagzine_has_roundOption(gunStack)) {
			gunStack.setItemDamage((int)((this.getMaxDamage() - this.getMaxDamage() / (float) gunInfo.magazineItemCount * (float)magazine_cnt)));
		}
		return magazine_cnt != 0;
	}
	private int countLoadedMagazines(ItemStack[] magazines) {
		int magazine_cnt = 0;
		for (ItemStack magazine : magazines) {
			if (magazine != null) magazine_cnt++;
		}
		return magazine_cnt;
	}

	public StackAndSlot searchMagazines(ItemStack gunStack, World world, IInventory inventory){
		int size = inventory.getSizeInventory();
		Item magItem = get_selectingMagazine(gunStack);
		boolean hasRoundOption = currentMagzine_has_roundOption(gunStack);
		for(int slot = 0;slot < size;slot++) {
			ItemStack itemStack = inventory.getStackInSlot(slot);
			if(itemStack != null && (!hasRoundOption || itemStack.getItemDamage() < itemStack.getMaxDamage()) && itemStack.stackSize>0){
				Item item = itemStack.getItem();
				if (item == magItem) {
					return new StackAndSlot(slot, itemStack);
				}
				inventory.markDirty();
			}
		}
		return null;
	}
	public void bindattaches(ItemStack itemstack, World world, Entity entity){
		//�e�C���x���g���̃A�b�v�f�[�g
		try {
			gunInfo.foruseattackDamage = gunInfo.attackDamage;
			NBTTagList tags = (NBTTagList) itemstack.getTagCompound().getTag("Items");
			if (tags != null) {

				guntemp.items = new ItemStack[6];
				for (int i = 0; i < tags.tagCount(); i++)//133
				{
					NBTTagCompound tagCompound = tags.getCompoundTagAt(i);
					int slot = tagCompound.getByte("Slot");
					if (slot >= 0 && slot < guntemp.items.length && guntemp.items[slot] == null) {
						guntemp.items[slot] = ItemStack.loadItemStackFromNBT(tagCompound);
					}
				}
				ItemStack itemstackattach;
				itemstackattach = guntemp.items[1];
				if(itemstackattach == null) {
					gunInfo.posGetter.curretnSightPos = gunInfo.sightPosN;
					if(gunInfo.hasNightVision[0]){
						if(entity instanceof EntityLivingBase && HandmadeGunsCore.Key_ADS(entity))((EntityLivingBase) entity).addPotionEffect(new PotionEffect(Potion.nightVision.id, 1, 1));
					}
				}else {
					if (itemstackattach.getItem() instanceof HMGItemSightBase){
						if(((HMGItemSightBase) itemstackattach.getItem()).needgunoffset) {
							float onads_modelPosX = 0;
							float onads_modelPosY = 0;
							float onads_modelPosZ = 0;
							if (itemstackattach.getItem() instanceof HMGItemSightBase) {
								onads_modelPosX = (gunInfo.sightattachoffset[0] + ((HMGItemSightBase) itemstackattach.getItem()).gunoffset[0]) * gunInfo.modelscale * gunInfo.inworldScale * gunInfo.onTurretScale;
								onads_modelPosY = (gunInfo.sightattachoffset[1] + ((HMGItemSightBase) itemstackattach.getItem()).gunoffset[1]) * gunInfo.modelscale * gunInfo.inworldScale * gunInfo.onTurretScale;
								onads_modelPosZ = (gunInfo.sightattachoffset[2] + ((HMGItemSightBase) itemstackattach.getItem()).gunoffset[2]) * gunInfo.modelscale * gunInfo.inworldScale * gunInfo.onTurretScale;
							}
							gunInfo.posGetter.curretnSightPos = new double[]{onads_modelPosX, onads_modelPosY, onads_modelPosZ};
							if(((HMGItemSightBase) itemstackattach.getItem()).isnightvision){
								if(entity instanceof EntityLivingBase && HandmadeGunsCore.Key_ADS(entity))((EntityLivingBase) entity).addPotionEffect(new PotionEffect(Potion.nightVision.id, 1, 1));
							}
						}else if(itemstackattach.getItem() instanceof HMGItemAttachment_reddot){
							if(gunInfo.hasNightVision[1]){
								if(entity instanceof EntityLivingBase && HandmadeGunsCore.Key_ADS(entity))((EntityLivingBase) entity).addPotionEffect(new PotionEffect(Potion.nightVision.id, 1, 1));
							}
							gunInfo.posGetter.curretnSightPos = gunInfo.sightPosR;
						}else if(itemstackattach.getItem() instanceof HMGItemAttachment_scope){
							if(gunInfo.hasNightVision[2]){
								if(entity instanceof EntityLivingBase && HandmadeGunsCore.Key_ADS(entity))((EntityLivingBase) entity).addPotionEffect(new PotionEffect(Potion.nightVision.id, 1, 1));
							}
							gunInfo.posGetter.curretnSightPos = gunInfo.sightPosS;
						}
					}
				}
				itemstackattach = guntemp.items[2];
				if (itemstackattach != null && itemstackattach.getItem() instanceof HMGItemAttachment_laser) {
					if (world.isRemote) {
						Vec3 vec3 = Vec3.createVectorHelper(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
						Vec3 playerlook = GunInfo.getLook(1.0f,entity);
						playerlook = Vec3.createVectorHelper(playerlook.xCoord * 256, playerlook.yCoord * 256, playerlook.zCoord * 256);
						Vec3 vec31 = Vec3.createVectorHelper(entity.posX + playerlook.xCoord, entity.posY + entity.getEyeHeight() + playerlook.yCoord, entity.posZ + playerlook.zCoord);
						MovingObjectPosition movingobjectposition = GunsUtils.getmovingobjectPosition_forBlock(world,vec3, vec31);//衝突するブロックを調べる

						vec3 = Vec3.createVectorHelper(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
						vec31 = Vec3.createVectorHelper(entity.posX + playerlook.xCoord, entity.posY + entity.getEyeHeight() + playerlook.yCoord, entity.posZ + playerlook.zCoord);
						if (movingobjectposition != null) {
							vec31 = Vec3.createVectorHelper(movingobjectposition.hitVec.xCoord, movingobjectposition.hitVec.yCoord, movingobjectposition.hitVec.zCoord);
						}
						Entity rentity = null;
						List list = getEntitiesWithinAABBExcludingEntity(entity, entity.boundingBox.addCoord(playerlook.xCoord, playerlook.yCoord, playerlook.zCoord).expand(1.0D, 1.0D, 1.0D));
						double d0 = 0.0D;
						double d1 = 0;
						for (Object o : list) {
							Entity entity1 = (Entity) o;
							if (entity1.canBeCollidedWith() && (entity1 != entity)) {
								float f = 0.3F;
								AxisAlignedBB axisalignedbb = entity1.boundingBox.expand((double) f, (double) f, (double) f);
								MovingObjectPosition movingobjectposition1 = axisalignedbb.calculateIntercept(vec3, vec31);

								if (movingobjectposition1 != null) {
									d1 = vec3.distanceTo(movingobjectposition1.hitVec);

									if (d1 < d0 || d0 == 0.0D) {
										rentity = entity1;
										d0 = d1;
									}
								}
							}
						}

						if (rentity != null) {
							d1 = vec3.distanceTo(vec31);
							vec3.xCoord = vec3.xCoord + (vec31.xCoord - vec3.xCoord) * d0 / d1;
							vec3.yCoord = vec3.yCoord + (vec31.yCoord - vec3.yCoord) * d0 / d1;
							vec3.zCoord = vec3.zCoord + (vec31.zCoord - vec3.zCoord) * d0 / d1;

							movingobjectposition = new MovingObjectPosition(rentity);
							movingobjectposition.hitVec = vec3;
						}
						if (movingobjectposition != null) {
							if (movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && movingobjectposition.entityHit != null) {
								if (entity.ridingEntity == null || entity.ridingEntity != movingobjectposition.entityHit) {
									HMGEntityLaser var8 = new HMGEntityLaser(world,entity, 0.01F);
									var8.posX = movingobjectposition.hitVec.xCoord;
									var8.posY = movingobjectposition.hitVec.yCoord;
									var8.posZ = movingobjectposition.hitVec.zCoord;
									world.spawnEntityInWorld(var8);
								}
							} else {
								if (movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
//									System.out.println("debug" + movingobjectposition);
									HMGEntityLaser var8 = new HMGEntityLaser(world,entity, 0.01F);
									var8.posX = movingobjectposition.hitVec.xCoord;
									var8.posY = movingobjectposition.hitVec.yCoord;
									var8.posZ = movingobjectposition.hitVec.zCoord;
									world.spawnEntityInWorld(var8);
								}
							}
						}
					}

				} else if (itemstackattach != null && itemstackattach.getItem() instanceof HMGItemAttachment_light) {
					if (world.isRemote) {
						//spawn light
						HMGEntityLight var8 = new HMGEntityLight(world, entity, true);
						world.spawnEntityInWorld(var8);
					}
				}
				//somehow made this break, I think its making two lights so this just fucks itself
				//else {
				//	HMGEntityLight var8 = new HMGEntityLight(world, entity, false);
				//	var8.setGunIsHeld(false); // Reset or change the value as required
				//}
				itemstackattach = guntemp.items[3];
				if (itemstackattach != null && itemstackattach.getItem() instanceof HMGItemAttachment_Suppressor) {
					guntemp.sound = gunInfo.soundsu;
					guntemp.soundlevel = gunInfo.soundsuplevel;
					guntemp.muzzle = false;
				} else if (itemstackattach != null && itemstackattach.getItem() instanceof HMGXItemGun_Sword) {
					gunInfo.foruseattackDamage = ((HMGXItemGun_Sword) itemstackattach.getItem()).attackDamage;
					guntemp.sound = gunInfo.soundbase;
					guntemp.soundlevel = gunInfo.soundbaselevel;
					guntemp.muzzle = gunInfo.muzzleflash;
				}else {
					guntemp.sound = gunInfo.soundbase;
					guntemp.soundlevel = gunInfo.soundbaselevel;
					guntemp.muzzle = gunInfo.muzzleflash;
				}
				if (guntemp.items[4] != null && guntemp.items[4].getItem() instanceof HMGItemAttachment_grip) {
					if(HandmadeGunsCore.Key_ADS(entity) || (entity instanceof EntityPlayer && entity.boundingBox.maxY - entity.boundingBox.minY < 1.5)){
						float gripSpread = ((HMGItemAttachment_grip) guntemp.items[4].getItem()).reduceSpreadLevel_ADS;
						guntemp.tempspread *= gripSpread;
						guntemp.spreadMultiplier *= gripSpread;
					}else {
						float gripSpread = ((HMGItemAttachment_grip) guntemp.items[4].getItem()).reduceSpreadLevel;
						guntemp.tempspread *= gripSpread;
						guntemp.spreadMultiplier *= gripSpread;
					}
				} else if(guntemp.items[4] != null && guntemp.items[4].getItem() instanceof HMGItem_Unified_Guns) {
					double ix = 0;
					double iy = 0;
					double iz = 0;
					float f1 = entity.getRotationYawHead() * (2 * (float) Math.PI / 360);
					float f2 = entity.rotationPitch * (2 * (float) Math.PI / 360);
					if (!HandmadeGunsCore.Key_ADS(entity)) {
						ix -= MathHelper.sin(f1) * MathHelper.cos(f2) * gunInfo.underoffsetpz / 4 + MathHelper.cos(-f1) * (-gunInfo.underoffsetpx / 4);
						iy += -MathHelper.sin(f2) * gunInfo.underoffsetpz / 4 + MathHelper.cos(f2) * gunInfo.underoffsetpy / 4;
						iz += MathHelper.cos(f1) * MathHelper.cos(f2) * gunInfo.underoffsetpz / 4 + MathHelper.sin(-f1) * (-gunInfo.underoffsetpx / 4);
					} else {
						ix -= MathHelper.sin(f1) * MathHelper.cos(f2) * gunInfo.underoffsetpz / 4 + MathHelper.cos(-f1) * (-gunInfo.underoffsetpx / 4);
						iy += -MathHelper.sin(f2) * gunInfo.underoffsetpz / 4 + MathHelper.cos(f2) * gunInfo.underoffsetpy / 4;
						iz += MathHelper.cos(f1) * MathHelper.cos(f2) * gunInfo.underoffsetpz / 4 + MathHelper.sin(-f1) * (-gunInfo.underoffsetpx / 4);
					}
					(entity).posX += ix;
					(entity).posY += iy;
					(entity).posZ += iz;
					guntemp.items[4].getItem().onUpdate(guntemp.items[4], world, entity, -1, true);
					( entity).posX -= ix;
					( entity).posY -= iy;
					( entity).posZ -= iz;
				} else if (guntemp.items[4] != null && guntemp.items[4].getItem() instanceof HMGXItemGun_Sword) {
					gunInfo.foruseattackDamage = ((HMGXItemGun_Sword) guntemp.items[4].getItem()).attackDamage;
				}
			}
		}catch (NullPointerException e){
			e.printStackTrace();
		}

		try {
			if (guntemp.items != null) {
//                        for (int i1 = 0; i1 < guntemp.items.length; i1++) {
//                            if (guntemp.items[i1] != null && guntemp.items[i1].getItemDamage() > guntemp.items[i1].getMaxDamage()) {
//                                guntemp.items[i1].stackSize--;
//                            }
//                            if (guntemp.items[i1] != null && guntemp.items[i1].stackSize <= 0) {
//                                guntemp.items[i1] = null;
//                            }
//                        }
				NBTTagCompound nbt = itemstack.getTagCompound();
				if (!world.isRemote) {
					NBTTagList tags = (NBTTagList) nbt.getTag("Items");
					int compressedID = 0;
					if (tags != null) {
						for (int itemid = 0; itemid < guntemp.items.length; itemid++) {
							if (guntemp.items[itemid] != null && guntemp.items[itemid].getItem() != null) {
								NBTTagCompound compound = new NBTTagCompound();
								compound.setByte("Slot", (byte) itemid);
								guntemp.items[itemid].writeToNBT(compound);
								tags.func_150304_a(compressedID, compound);
								compressedID++;
							}
//                                    if (items[itemid] != null && items[itemid].getItem() != null) {
//                                        NBTTagCompound compound = new NBTTagCompound();
//                                        compound.setByte("Slot", (byte) itemid);
//                                        items[itemid].writeToNBT(compound);
//                                        if(tags.tagCount() >= 6) {
//                                            if(itemid == 4 && items[4] != null && items[4].getItem() instanceof HMGItem_Unified_Guns){
//                                                System.out.println("" + items[4].getItemDamage());
//                                            }
//                                            tags.func_150304_a(itemid, compound);
//                                        }else {
//                                            tags.appendTag(compound);
//                                        }
//                                    }
						}
						if (compressedID > 6 && compressedID < tags.tagCount()) {
							for (int removeid = compressedID; removeid < tags.tagCount(); removeid++) {
								System.out.println("debug" + compressedID);
								tags.removeTag(removeid);
							}
						}
						nbt.setTag("Items", tags);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public int getburstCount(int mode){
		if(!gunInfo.burstcount.isEmpty() && gunInfo.burstcount.size()>mode) {
			return gunInfo.burstcount.get(mode);
		}else{
			return -1;
		}
	}
	public float getDiffusion(ItemStack itemStack){
		checkTags(itemStack);
		NBTTagCompound nbt = itemStack.getTagCompound();
		return nbt.getFloat("Diffusion");
	}

	public void damageMagazine(ItemStack par1ItemStack , Entity par3Entity){
		boolean hasPhysicalRound = false;
		for(int i = 0; i < gunInfo.magazineItemCount; i++) {
			ItemStack loaded = ItemStack.loadItemStackFromNBT(par1ItemStack.getTagCompound().getCompoundTag("LoadedMagazine" + i));
			if(loaded != null && loaded.getItemDamage() < loaded.getMaxDamage()) {
				hasPhysicalRound = true;
				break;
			}
		}
		if(hasPhysicalRound) damage_LoadedMagazine(par1ItemStack);
		else if(!currentMagzine_has_roundOption(par1ItemStack)){
			par1ItemStack.setItemDamage(par1ItemStack.getItemDamage() + 1);
			if(gunInfo.magazineItemCount > 1)destroy_LoadedMagazine(par1ItemStack);
			if(par1ItemStack.getItemDamage() == par1ItemStack.getMaxDamage())destroy_LoadedMagazine(par1ItemStack);
		}
		else damage_LoadedMagazine(par1ItemStack);
	}

	public Multimap getAttributeModifiers(ItemStack itemstack)
	{

		if (itemstack != null && itemstack.getItem() instanceof HMGItem_Unified_Guns) {

			if (itemstack.getTagCompound() != null) try {
				guntemp = new GunTemp();
				guntemp.items = new ItemStack[6];
				gunInfo.foruseattackDamage = gunInfo.attackDamage;
				NBTTagList tags = (NBTTagList) itemstack.getTagCompound().getTag("Items");
				if (tags != null) {
					for (int i = 0; i < tags.tagCount(); i++)//133
					{
						NBTTagCompound tagCompound = tags.getCompoundTagAt(i);
						int slot = tagCompound.getByte("Slot");
						if (slot >= 0 && slot < guntemp.items.length) {
							guntemp.items[slot] = ItemStack.loadItemStackFromNBT(tagCompound);
						}
					}
					ItemStack itemstackattach;
					itemstackattach = guntemp.items[3];
					if (itemstackattach != null && itemstackattach.getItem() instanceof HMGXItemGun_Sword) {
						gunInfo.foruseattackDamage = ((HMGXItemGun_Sword) itemstackattach.getItem()).attackDamage;
					}
					itemstackattach = guntemp.items[4];
					if (itemstackattach != null && itemstackattach.getItem() instanceof HMGXItemGun_Sword) {
						gunInfo.foruseattackDamage = ((HMGXItemGun_Sword) itemstackattach.getItem()).attackDamage;
					}
				}
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
			AttributeModifier gunMoveFactor = new AttributeModifier(GunInfo.field_110179_h, "GunMoveFactor", gunInfo.motion - 1, 1);
			Multimap multimap = super.getItemAttributeModifiers();
			multimap.put(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(), new AttributeModifier(field_111210_e, "Weapon modifier", (double) gunInfo.foruseattackDamage, 0));
			multimap.put(SharedMonsterAttributes.movementSpeed.getAttributeUnlocalizedName(), gunMoveFactor);

			return multimap;
		}
		return super.getAttributeModifiers(itemstack);
	}
	public void setmodelADSPosAndRotation(double px,double py,double pz){
		gunInfo.setmodelADSPosAndRotation(px,py,pz);
	}
	public void setADSoffsetRed(double px,double py,double pz){
		gunInfo.setADSoffsetRed(px,py,pz);
	}
	public void setADSoffsetScope(double px,double py,double pz){
		gunInfo.setADSoffsetScope(px,py,pz);
	}
	public double[] getSightPos(ItemStack itemStack){
		if(guntemp == null)guntemp = new GunTemp();
		guntemp.items = new ItemStack[6];
		try {
			NBTTagList tags = (NBTTagList) itemStack.getTagCompound().getTag("Items");
			if (tags != null) {
				for (int i = 0; i < tags.tagCount(); i++)//133
				{
					NBTTagCompound tagCompound = tags.getCompoundTagAt(i);
					int slot = tagCompound.getByte("Slot");
					if (slot >= 0 && slot < guntemp.items.length) {
						guntemp.items[slot] = ItemStack.loadItemStackFromNBT(tagCompound);
					}
				}
				ItemStack itemstack_attach;
				itemstack_attach = guntemp.items[1];
				if (itemstack_attach == null) {
					return gunInfo.sightPosN;
				} else {
					if (itemstack_attach.getItem() instanceof HMGItemSightBase && ((HMGItemSightBase) itemstack_attach.getItem()).needgunoffset) {
						double onads_modelPosX = 0;
						double onads_modelPosY = 0;
						double onads_modelPosZ = 0;
						if (itemstack_attach.getItem() instanceof HMGItemSightBase) {
							onads_modelPosX = (gunInfo.sightattachoffset[0] + ((HMGItemSightBase) itemstack_attach.getItem()).gunoffset[0]) * gunInfo.modelscale * gunInfo.inworldScale * 0.4;
							onads_modelPosY = (gunInfo.sightattachoffset[1] + ((HMGItemSightBase) itemstack_attach.getItem()).gunoffset[1]) * gunInfo.modelscale * gunInfo.inworldScale * 0.4;
							onads_modelPosZ = (gunInfo.sightattachoffset[2] + ((HMGItemSightBase) itemstack_attach.getItem()).gunoffset[2]) * gunInfo.modelscale * gunInfo.inworldScale * 0.4;
						}
						return new double[]{onads_modelPosX, onads_modelPosY, onads_modelPosZ};
					} else if (itemstack_attach.getItem() instanceof HMGItemAttachment_reddot) {
						return gunInfo.sightPosR;
					} else if (itemstack_attach.getItem() instanceof HMGItemAttachment_scope) {
						return gunInfo.sightPosS;
					}
				}
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		return gunInfo.sightPosN;
	}

	public boolean currentMagzine_has_roundOption(ItemStack itemStack){
		Item currentmagazine = getcurrentMagazine(itemStack);
		if(currentmagazine instanceof HMGItemCustomMagazine){
			return ((HMGItemCustomMagazine) currentmagazine).hasRoundOption;
		}
		return false;
	}
	/** Returns the physical round at the chamber end, not the currently selected reload type. */
	public ItemStack resolveNextAmmunitionStack(ItemStack gunStack){
		checkTags(gunStack);
		for(int i = 0; i < gunInfo.magazineItemCount; i++) {
			ItemStack stack = ItemStack.loadItemStackFromNBT(gunStack.getTagCompound().getCompoundTag("LoadedMagazine" + i));
			if(stack != null && stack.getItemDamage() < stack.getMaxDamage()) return stack;
		}
		Item selected = getcurrentMagazine(gunStack);
		return selected == null ? null : new ItemStack(selected, 1);
	}
	private String debugItem(ItemStack stack) {
		return stack == null || stack.getItem() == null ? "null" : stack.getItem().getUnlocalizedName();
	}
	public int get_Type_Option_of_currentMagzine_and_apply_magazine_Option(ItemStack itemStack){
		Item currentmagazine = getcurrentMagazine(itemStack);
		if(currentmagazine instanceof HMGItemCustomMagazine){
			firetemp.applyMagOption((HMGItemCustomMagazine) currentmagazine);


			return ((HMGItemCustomMagazine) currentmagazine).bullettype;
		}
		return -1;
	}
	public int reloadTime(ItemStack itemStack){
		try {
			if(guntemp == null)return 0;
			Item currentmagazine = getcurrentMagazine(itemStack);
			if(currentmagazine instanceof HMGItemCustomMagazine &&  ((HMGItemCustomMagazine) currentmagazine).hasReloadOption)return ((HMGItemCustomMagazine) currentmagazine).reloadTime;
			return guntemp.selectingMagazine < gunInfo.reloadTimes.length ? gunInfo.reloadTimes[guntemp.selectingMagazine]:gunInfo.reloadTimes[0];
		}catch (Exception e){
			e.printStackTrace();
		}
		return 0;
	}
	//    public boolean selectingMagazine_has_roundOption(ItemStack itemStack){
//        Item selectingmagazine = get_selectingMagazine(itemStack);
//        if(selectingmagazine instanceof HMGItemCustomMagazine){
//            return ((HMGItemCustomMagazine) selectingmagazine).hasRoundOption;
//        }
//        return false;
//    }
	public boolean currentMagazine_is_autoDestroy(ItemStack itemStack){
		Item currentMagazine = getcurrentMagazine(itemStack);
		if(currentMagazine instanceof HMGItemCustomMagazine){
			return ((HMGItemCustomMagazine) currentMagazine).autoDestroy;
		}
		return false;
	}
	public String currentMagazine_magazineModel(ItemStack itemStack){
		String magmodel = null;
		Item currentMagazine = getcurrentMagazine(itemStack);
		if(currentMagazine instanceof HMGItemCustomMagazine){
			magmodel = ((HMGItemCustomMagazine) currentMagazine).magmodel;
		}
		return magmodel != null ? magmodel:gunInfo.bulletmodelMAG;
	}
	public String currentMagazine_cartridgeModelName(ItemStack itemStack){
		Item currentMagazine = getcurrentMagazine(itemStack);
		if(currentMagazine instanceof HMGItemCustomMagazine){
			return ((HMGItemCustomMagazine) currentMagazine).cartridgeModelName;
		}
		return null;
	}
	public Item currentMagazine_cartridgeItem(ItemStack itemStack){
		Item currentMagazine = getcurrentMagazine(itemStack);
		if(currentMagazine instanceof HMGItemCustomMagazine){
			return ((HMGItemCustomMagazine) currentMagazine).getCartridgeItem();
		}
		return null;
	}
	public Item getcurrentMagazine(ItemStack itemStack){
		if(gunInfo.magazine == null)return null;
		int selectingMagazine;
		if(itemStack == null){
			selectingMagazine = 0;
		}else {
			checkTags(itemStack);
			NBTTagCompound nbt = itemStack.getTagCompound();
			selectingMagazine = nbt.getInteger("getcurrentMagazine");
		}
		if(selectingMagazine >=0 && selectingMagazine < gunInfo.magazine.length){
			return gunInfo.magazine[selectingMagazine];
		}else {
			if(itemStack !=null) {
				NBTTagCompound nbt = itemStack.getTagCompound();
				nbt.setInteger("getcurrentMagazine", 0);
			}
			return gunInfo.magazine[0];
		}
	}
	public Item get_selectingMagazine(ItemStack itemStack){
		if(gunInfo.magazine == null)return null;
		int selectingMagazine;
		if(itemStack == null){
			selectingMagazine = 0;
		}else {
			checkTags(itemStack);
			NBTTagCompound nbt = itemStack.getTagCompound();
			selectingMagazine = nbt.getInteger("get_selectingMagazine");
		}
		if(selectingMagazine >=0 && selectingMagazine < gunInfo.magazine.length){
			return gunInfo.magazine[selectingMagazine];
		}else {
			if(itemStack !=null) {
				NBTTagCompound nbt = itemStack.getTagCompound();
				nbt.setInteger("get_selectingMagazine", 0);
			}
			return gunInfo.magazine[0];
		}
	}
	public ItemStack[] get_loadedMagazineStack(ItemStack itemStack){
		if(currentMagzine_has_roundOption(itemStack)){
			checkTags(itemStack);
			NBTTagCompound nbt = itemStack.getTagCompound();
			ItemStack[] itemStacks = new ItemStack[gunInfo.magazineItemCount];
			for(int i = 0;i < gunInfo.magazineItemCount; i++) {
				NBTTagCompound tag = nbt.getCompoundTag("LoadedMagazine" + i);
				if(tag != null)itemStacks[i] = ItemStack.loadItemStackFromNBT(tag);
			}
			return itemStacks;
		}else{
			NBTTagCompound nbt = itemStack.getTagCompound();
			int willReturnNum = remain_Bullet(itemStack)/max_Bullet(itemStack);
			ItemStack[] itemStacks = new ItemStack[gunInfo.magazineItemCount];
			for(int i = 0;i < willReturnNum; i++) {
				NBTTagCompound tag = nbt.getCompoundTag("LoadedMagazine" + i);
				if(tag != null)itemStacks[i] = ItemStack.loadItemStackFromNBT(tag);
			}
			return itemStacks;
		}
	}
	public void set_loadedMagazineStack(ItemStack itemStack,ItemStack[] itemStacks){
		checkTags(itemStack);
		NBTTagCompound stackTagCompound = itemStack.getTagCompound();
		for(int i = 0;i < gunInfo.magazineItemCount; i++) {
			NBTTagCompound nbttagcompound = new NBTTagCompound();
			if(itemStacks[i] != null) {
				itemStacks[i].writeToNBT(nbttagcompound);
			}else {
			}
			stackTagCompound.setTag("LoadedMagazine" + i, nbttagcompound);
		}
	}
	public void destroy_LoadedMagazine(ItemStack itemStack){
		ItemStack[] magazines = get_loadedMagazineStack(itemStack);
		int cnt = 0;
		for(ItemStack a_itemStack: magazines){
			if(a_itemStack != null){
				magazines[cnt] = null;
				break;
			}
			cnt ++ ;
		}
		set_loadedMagazineStack(itemStack,magazines);
	}
	public void damage_LoadedMagazine(ItemStack itemStack){
		ItemStack[] magazines = new ItemStack[gunInfo.magazineItemCount];
		for(int i = 0; i < magazines.length; i++)
			magazines[i] = ItemStack.loadItemStackFromNBT(itemStack.getTagCompound().getCompoundTag("LoadedMagazine" + i));
		int cnt = 0;
		for(ItemStack a_itemStack: magazines){
			if(a_itemStack != null && a_itemStack.getMaxDamage() > a_itemStack.getItemDamage()){
				a_itemStack.setItemDamage(a_itemStack.getItemDamage() + 1);
				if(a_itemStack.getMaxDamage() <= a_itemStack.getItemDamage() &&
						(!(a_itemStack.getItem() instanceof HMGItemCustomMagazine) || ((HMGItemCustomMagazine)a_itemStack.getItem()).autoDestroy)) magazines[cnt] = null;
				break;
			}
			cnt ++ ;
		}
		if(isPerShellReload(itemStack)) {
			// Compact only tube-fed ammunition: firing is FIFO and loading appends.
			ItemStack[] compacted = new ItemStack[magazines.length];
			int next = 0;
			for(ItemStack magazine : magazines) if(magazine != null) compacted[next++] = magazine;
			magazines = compacted;
		}
		set_loadedMagazineStack(itemStack,magazines);
	}
	public void detach_LoadedMagazine(ItemStack itemStack){
		ItemStack[] magazines = new ItemStack[gunInfo.magazineItemCount];
		set_loadedMagazineStack(itemStack,magazines);
		if(!currentMagzine_has_roundOption(itemStack))
			itemStack.setItemDamage(itemStack.getMaxDamage());
		itemStack.getTagCompound().setBoolean("detached",true);
	}
	public void returnInternalMagazines(ItemStack gunstack,Entity shooter){
		if(!shooter.worldObj.isRemote){
			int returnmagazineCount = (int)((float) this.gunInfo.magazineItemCount);
//                            System.out.println("debug" + returnmagazineCount);
			ItemStack[] itemStacks =  this.get_loadedMagazineStack(gunstack);
			for(int i= 0;i<returnmagazineCount;i++) {
				if (itemStacks[i] == null || handmadeguns.Util.HMGAmmoPolicy.isSupplied(itemStacks[i])) {
					// Virtual magazines never become inventory items or ejected entities.
					continue;
				}
				{
					if (gunInfo.dropMagEntity && HandmadeGunsCore.cfg_canEjectCartridge) {
						HMGEntityBulletCartridge var8;
						String magmodel = currentMagazine_magazineModel(gunstack);
						if (magmodel == null) {
							var8 = new HMGEntityBulletCartridge(shooter.worldObj, shooter, gunInfo.magType);
						} else {
							var8 = new HMGEntityBulletCartridge(shooter.worldObj, shooter, -1, magmodel);
						}
						var8.itemStack = itemStacks[i];
						shooter.worldObj.spawnEntityInWorld(var8);
					} else {
						shooter.worldObj.spawnEntityInWorld(new EntityItem(shooter.worldObj, shooter.posX, shooter.posY, shooter.posZ, itemStacks[i]));
					}
				}
			}
			this.detach_LoadedMagazine(gunstack);
		}
	}

	public List getEntitiesWithinAABBExcludingEntity(Entity p_72839_1_, AxisAlignedBB p_72839_2_)
	{
		return this.getEntitiesWithinAABBExcludingEntity(p_72839_1_, p_72839_2_, (IEntitySelector)null);
	}

	public List getEntitiesWithinAABBExcludingEntity(Entity p_94576_1_, AxisAlignedBB p_94576_2_, IEntitySelector p_94576_3_)
	{
		ArrayList arraylist = new ArrayList();
		int i = MathHelper.floor_double((p_94576_2_.minX - MAX_ENTITY_RADIUS) / 16.0D);
		int j = MathHelper.floor_double((p_94576_2_.maxX + MAX_ENTITY_RADIUS) / 16.0D);
		int k = MathHelper.floor_double((p_94576_2_.minZ - MAX_ENTITY_RADIUS) / 16.0D);
		int l = MathHelper.floor_double((p_94576_2_.maxZ + MAX_ENTITY_RADIUS) / 16.0D);

		for (int i1 = i; i1 <= j; ++i1)
		{
			for (int j1 = k; j1 <= l; ++j1)
			{
				if (p_94576_1_.worldObj.getChunkProvider().chunkExists(i1, j1))
				{
					getEntitiesWithinAABBForEntity(p_94576_1_.worldObj.getChunkFromChunkCoords(i1, j1),p_94576_1_, p_94576_2_, arraylist, p_94576_3_);
				}
			}
		}

		return arraylist;
	}

	public void getEntitiesWithinAABBForEntity(Chunk chunk, Entity p_76588_1_, AxisAlignedBB p_76588_2_, List p_76588_3_, IEntitySelector p_76588_4_)
	{
		int i = MathHelper.floor_double((p_76588_2_.minY - World.MAX_ENTITY_RADIUS) / 16.0D);
		int j = MathHelper.floor_double((p_76588_2_.maxY + World.MAX_ENTITY_RADIUS) / 16.0D);
		i = MathHelper.clamp_int(i, 0, chunk.entityLists.length - 1);
		j = MathHelper.clamp_int(j, 0, chunk.entityLists.length - 1);

		for (int k = i; k <= j; ++k)
		{
			List list1 = chunk.entityLists[k];

			for (int l = 0; l < list1.size(); ++l)
			{
				Entity entity1 = (Entity)list1.get(l);

				if (entity1 != p_76588_1_ && !(entity1 instanceof HMGEntityBulletBase) && entity1.boundingBox.intersectsWith(p_76588_2_) && (p_76588_4_ == null || p_76588_4_.isEntityApplicable(entity1)))
				{
					p_76588_3_.add(entity1);
					Entity[] aentity = entity1.getParts();

					if (aentity != null)
					{
						for (int i1 = 0; i1 < aentity.length; ++i1)
						{
							entity1 = aentity[i1];

							if (entity1 != p_76588_1_ && entity1.boundingBox.intersectsWith(p_76588_2_) && (p_76588_4_ == null || p_76588_4_.isEntityApplicable(entity1)))
							{
								p_76588_3_.add(entity1);
							}
						}
					}
				}
			}
		}
	}

	static Field genericAttribute_field;
	public static double computeMoveSpeed_WithoutGunModifier(ModifiableAttributeInstance inst)
	{
		if (inst == null) return 0.0D;

		double base = inst.getBaseValue();
		AttributeModifier mod;

		// Operation 0 (ADD_NUMBER)
		Iterator it0 = inst.getModifiersByOperation(0).iterator();
		while (it0.hasNext())
		{
			mod = (AttributeModifier) it0.next();

			if (GunInfo.field_110179_h != null &&
					GunInfo.field_110179_h.equals(mod.getID()))
				continue;

			base += mod.getAmount();
		}

		double result = base;

		// Operation 1 (MULTIPLY_BASE)
		Iterator it1 = inst.getModifiersByOperation(1).iterator();
		while (it1.hasNext())
		{
			mod = (AttributeModifier) it1.next();

			if (GunInfo.field_110179_h != null &&
					GunInfo.field_110179_h.equals(mod.getID()))
				continue;

			result += base * mod.getAmount();
		}

		// Operation 2 (MULTIPLY_TOTAL)
		Iterator it2 = inst.getModifiersByOperation(2).iterator();
		while (it2.hasNext())
		{
			mod = (AttributeModifier) it2.next();

			if (GunInfo.field_110179_h != null &&
					GunInfo.field_110179_h.equals(mod.getID()))
				continue;

			result *= (1.0D + mod.getAmount());
		}

		// Clamp like vanilla
		if (genericAttribute_field == null)
		{
			genericAttribute_field = ReflectionHelper.findField(
					ModifiableAttributeInstance.class,
					"field_111136_b",
					"genericAttribute"
			);
		}

		try
		{
			IAttribute genericAttribute =
					(IAttribute) genericAttribute_field.get(inst);

			return genericAttribute.clampValue(result);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return result;
	}

}
