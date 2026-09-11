package handmadeguns.client.render;

import handmadeguns.HandmadeGunsCore;
import handmadeguns.items.*;
import handmadeguns.items.guns.HMGItemSwordBase;
import handmadeguns.items.guns.HMGItem_Unified_Guns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;

import static handmadeguns.HandmadeGunsCore.HMG_proxy;
import static handmadeguns.client.render.HMGRenderItemGun_U_NEW.*;
import static org.lwjgl.opengl.GL11.glTranslatef;

public class PartsRender_Gun extends PartsRender {
	/** Immutable opt-in data only; playback belongs to AnimationClient's instance registry. */
	public handmadeguns.animation.AnimationDefinition animationDefinition;
	public ArrayList<HMGGunParts> partslist = new ArrayList<HMGGunParts>();
	public static Entity curretnEntity;
	public GunTemp guntemp = new GunTemp();//TODO readPropertyFromNBTで銃の状態に同期
	public float modelscala;
	private static ModelBiped modelBipedMain = new ModelBiped(0.5F);;
	
	public float muzzleattachoffset[] = new float[3];
	public float muzzleattachrotation[] = new float[3];
	public float sightattachoffset[] = new float[3];
	public float sightattachrotation[] = new float[3];
	public float gripattachoffset[] = new float[3];
	public float gripattachrotation[] = new float[3];
	public float overbarrelattachoffset[] = new float[3];
	public float overbarrelattachrotation[] = new float[3];
	public boolean useLegacyInventoryScale = false;
	/** ModelArm controls imported locator arms; legacy OBJ/MQO part behavior is unchanged. */
	public boolean blockbenchArmsEnabled = false;
	/** Set by the gun-part parser when this model has a SetAttachmentAttach anchor. */
	public boolean hasAttachmentAnchor = false;
	public final boolean[] hasNumberedAttachmentAnchor = new boolean[6];
	private final boolean[] attachmentRendered = new boolean[6];
	
	public void partSidentification(Object... data){
		guntemp.readPropertyFromNBT(gunitem.gunInfo,nbt, HandmadeGunsCore.Key_ADS(curretnEntity),HMG_proxy.getCilentWorld());
		GunState[] states;
		float flame;
		int remainbullets;
		ArrayList<HMGGunParts> partslist_temp = partslist;
		boolean rootRender = data.length <= 3;
		if (rootRender) for (int slot = 1; slot <= 5; slot++) attachmentRendered[slot] = false;
		if(data.length >3){
			partslist_temp = (ArrayList<HMGGunParts>) data[0];
			if(data[1] instanceof GunState[])states = (GunState[]) data[1];
			else states = new GunState[]{(GunState) data[1]};
			flame = (float) data[2];
			remainbullets = (int) data[3];
		}else {
			states = (GunState[]) data[0];
			flame = (float) data[1];
			remainbullets = (int) data[2];
		}
		
		if (rootRender && animationDefinition != null)
			handmadeguns.client.animation.AnimationClient.prepare(this, states, flame, remainbullets);
		boolean importedRoot = rootRender && model instanceof handmadeguns.client.modelLoader.blockbench.BlockbenchModel;
		if (importedRoot) {
			GL11.glPushMatrix();
			handmadeguns.client.modelLoader.blockbench.BlockbenchTransform.presentationFrame();
		}
		try {
		for (HMGGunParts parts : partslist_temp) {
			for (GunState state : states) {
				if(checkState2(state,parts,flame,remainbullets))break;
			}
		}
		} finally { if (importedRoot) GL11.glPopMatrix(); }
		if (rootRender && isfirstperson && pass != 1 && blockbenchArmsEnabled
				&& model instanceof handmadeguns.client.modelLoader.blockbench.BlockbenchModel) {
			handmadeguns.client.modelLoader.blockbench.BlockbenchModel imported =
					(handmadeguns.client.modelLoader.blockbench.BlockbenchModel)model;
			// Authored locators are primary. Existing ModelArm offsets/rotations remain the
			// fallback for either hand when its locator is absent or hidden.
			if (!imported.hasHandLocator(true)) renderarmL();
			if (!imported.hasHandLocator(false)) renderarmR();
		}
		if (rootRender) for (int slot = 1; slot <= 5; slot++)
			if (!attachmentRendered[slot] && !hasNumberedAttachmentAnchor[slot] && !hasAttachmentAnchor)
				renderAttachmentSlot(slot);
	}

	@Override
	protected void renderPartHook(HMGGunParts parts, GunState state, float flame, int remainbullets,
	                              HMGGunParts_Motion_PosAndRotation offsetAndRotation) {
		if (isfirstperson && pass != 1 && blockbenchArmsEnabled
				&& model instanceof handmadeguns.client.modelLoader.blockbench.BlockbenchModel)
			((handmadeguns.client.modelLoader.blockbench.BlockbenchModel)model).renderHand(parts,gunPartsScale);
		for (int slot = 1; slot <= 5; slot++) if (parts.attachmentAttachSlots[slot]) renderAttachmentSlot(slot);
		if (parts.attachmentAttach) for (int slot = 1; slot <= 5; slot++)
			if (!hasNumberedAttachmentAnchor[slot]) renderAttachmentSlot(slot);
	}

	private void renderAttachmentSlot(int slot) {
		if (attachmentRendered[slot]) return;
		attachmentRendered[slot] = true;
		HMGAttachmentModelRenderer.renderInstalledSlot(gunitem, items, pass, gunPartsScale, texture, slot);
	}

	/** Returns the live installed-item state for the existing attachment GUI/NBT slot. */
	protected boolean isAttachmentSlotOccupied(int slot) {
		return items != null && slot >= 1 && slot <= 5 && slot < items.length && items[slot] != null;
	}

	@Override
	protected void applyPartExtraTransform(HMGGunParts parts) {
		float moveX = 0;
		float moveY = 0;
		float moveZ = 0;
		for (int slot = 1; slot <= 5; slot++) {
			if (parts.moveIfAttachPresent[slot] && isAttachmentSlotOccupied(slot)) {
				moveX += parts.moveIfAttachPresentX[slot];
				moveY += parts.moveIfAttachPresentY[slot];
				moveZ += parts.moveIfAttachPresentZ[slot];
			}
		}
		if (moveX != 0 || moveY != 0 || moveZ != 0) {
			GL11.glTranslatef(moveX * gunPartsScale, moveY * gunPartsScale, moveZ * gunPartsScale);
		}
	}

	@Override
	protected boolean shouldRenderPartGeometry(HMGGunParts parts) {
		for (int slot = 1; slot <= 5; slot++) {
			if (parts.removeIfAttachPresent[slot] && isAttachmentSlotOccupied(slot)) return false;
		}
		return true;
	}

	private boolean checkState2(GunState state,HMGGunParts parts,float flame,int remainbullets){
		switch (state) {
			case ADS:
				if (parts.rendering_Ads) {
					PartSidentification_Attach(parts, state, flame, remainbullets, parts.getRenderinfOfADS());
					return true;
				}
				break;
			case Default:
				if (parts.rendering_Def) {
					PartSidentification_Attach(parts, state, flame, remainbullets,parts.getRenderinf_None());
					return true;
				}
				break;
			case Recoil:
				if (parts.rendering_Recoil) {
					if (parts.hasMotionRecoil) {
						HMGGunParts_Motion_PosAndRotation OffsetAndRotation = parts.getRecoilmotion(flame);
						
						PartSidentification_Attach(parts, state, flame, remainbullets, OffsetAndRotation);
						return true;
					} else {
						HMGGunParts_Motion_PosAndRotation OffsetAndRotation = parts.getRenderinfOfRecoil();
						
						PartSidentification_Attach(parts, state, flame, remainbullets, OffsetAndRotation);
						return true;
					}
				}
				break;
			case Cock:
				if (parts.rendering_Cock) {
					if (parts.hasMotionCock) {
						HMGGunParts_Motion_PosAndRotation OffsetAndRotation = parts.getcockmotion(flame);
						
						PartSidentification_Attach(parts, state, flame, remainbullets, OffsetAndRotation);
						return true;
					} else {
						HMGGunParts_Motion_PosAndRotation OffsetAndRotation = parts.getRenderinfOfCock();
						
						PartSidentification_Attach(parts, state, flame, remainbullets, OffsetAndRotation);
						return true;
					}
				}
				break;
			case Reload:
				if (parts.rendering_Reload) {
					if (parts.hasMotionReload) {
						HMGGunParts_Motion_PosAndRotation OffsetAndRotation = parts.getReloadmotion(flame);
						
						PartSidentification_Attach(parts, state, flame, remainbullets, OffsetAndRotation);
						return true;
					} else {
						HMGGunParts_Motion_PosAndRotation OffsetAndRotation = parts.getRenderinfOfReload();
						
						PartSidentification_Attach(parts, state, flame, remainbullets, OffsetAndRotation);
						return true;
					}
				}
				break;
		}
		return false;
	}
	public void PartSidentification_Attach(HMGGunParts parts, GunState state, float flame, int remainbullets, HMGGunParts_Motion_PosAndRotation OffsetAndRotation){
		if (animationDefinition != null)
			OffsetAndRotation = handmadeguns.client.animation.AnimationClient.pose(this, parts, OffsetAndRotation);
		if(gunitem.gunInfo.magazine.length >1) {
			if (parts.current_magazineType != null) {
				int currentmagazineid = nbt.getInteger("getcurrentMagazine");
				if (!parts.current_magazineType.get(currentmagazineid)) return;
			}
			if (parts.select_magazineType != null) {
				int selectmagazineid = nbt.getInteger("get_selectingMagazine");
				if (!parts.select_magazineType.get(selectmagazineid)) return;
			}
		}
		if(OffsetAndRotation != null) {
			
			if (isPlacedGun && !parts.base && parts.carryingHandle) {
				return;
			}
			
			if (!isPlacedGun && !parts.carryingHandle && parts.base) {
				return;
			}
			if((parts.underOnly && isUnder) || !(parts.underOnly_not && isUnder))
			if (parts.isattachpart) {
				if (items[1] != null) {//sight
					if (parts.issightbase && items[1].getItem() instanceof HMGItemSightBase) {
						IItemRenderer attachrender = MinecraftForgeClient.getItemRenderer(items[1], IItemRenderer.ItemRenderType.EQUIPPED);
						if (attachrender instanceof HMGRenderItemCustom
								&& !((HMGItemAttachmentBase) items[1].getItem()).has3dModel(1)) {
							GunPart_Render_attach(parts, state, flame, remainbullets, OffsetAndRotation, sightattachoffset, sightattachrotation, ((HMGRenderItemCustom) attachrender));
							return;
						}
					} else if (parts.isscope && items[1].getItem() instanceof HMGItemAttachment_scope) {
						part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
						return;
					} else if (parts.isdot && items[1].getItem() instanceof HMGItemAttachment_reddot) {
						part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
						return;
					}
				} else if (parts.issight) {
					part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
					return;
				}
				if (items[2] != null) {
					if (parts.islight && items[2].getItem() instanceof HMGItemAttachment_light) {
						part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
						return;
					} else if (parts.islasersight && items[2].getItem() instanceof HMGItemAttachment_laser) {
						part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
						return;
					} else if (parts.isoverbarrelbase && items[2].getItem() instanceof HMGItemAttachmentBase) {
						IItemRenderer attachrender = MinecraftForgeClient.getItemRenderer(items[2], IItemRenderer.ItemRenderType.EQUIPPED);
						if (attachrender instanceof HMGRenderItemCustom
								&& !((HMGItemAttachmentBase) items[2].getItem()).has3dModel(2)) {
							GunPart_Render_attach(parts, state, flame, remainbullets, OffsetAndRotation, overbarrelattachoffset, overbarrelattachrotation, ((HMGRenderItemCustom) attachrender));
							return;
						}
					}
				}
				if (items[3] != null) {
					if (parts.ismuzzlepart && items[3].getItem() instanceof HMGItemAttachment_Suppressor) {
						part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
						return;
					} else if (parts.ismuzzulebase && items[3].getItem() instanceof HMGItemAttachmentBase) {
						IItemRenderer attachrender = MinecraftForgeClient.getItemRenderer(items[3], IItemRenderer.ItemRenderType.EQUIPPED);
						if (attachrender instanceof HMGRenderItemCustom
								&& !((HMGItemAttachmentBase) items[3].getItem()).has3dModel(3)) {
							GunPart_Render_attach(parts, state, flame, remainbullets, OffsetAndRotation, muzzleattachoffset, muzzleattachrotation, ((HMGRenderItemCustom) attachrender));
							return;
						}
					} else if (parts.issword && items[3].getItem() instanceof HMGItemSwordBase) {
						part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
					} else if (parts.isswordbase && items[3].getItem() instanceof HMGItemSwordBase) {
						IItemRenderer attachrender = MinecraftForgeClient.getItemRenderer(items[3], IItemRenderer.ItemRenderType.EQUIPPED);
						if (attachrender != null) {
							GL11.glPushMatrix();
							part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
							glTranslatef(gunitem.gunInfo.underoffsetpx * gunPartsScale, gunitem.gunInfo.underoffsetpy * gunPartsScale, gunitem.gunInfo.underoffsetpz * gunPartsScale);
							GL11.glRotatef(gunitem.gunInfo.underrotationy, 0, 1, 0);
							GL11.glRotatef(gunitem.gunInfo.underrotationx, 1, 0, 0);
							GL11.glRotatef(gunitem.gunInfo.underrotationz, 0, 0, 1);
							GL11.glScalef(1 / modelscala, 1 / modelscala, 1 / modelscala);
							GL11.glScalef(1 / (0.4f), 1 / (0.4f), 1 / (0.4f));
							attachrender.renderItem(IItemRenderer.ItemRenderType.ENTITY, items[3], datas);
							Minecraft.getMinecraft().renderEngine.bindTexture(texture);
							GL11.glPopMatrix();
						}
					}
				}
				if (items[4] != null) {
					if (parts.isgripBase && items[4].getItem() instanceof HMGItemAttachmentBase) {
						IItemRenderer attachrender = MinecraftForgeClient.getItemRenderer(items[4], IItemRenderer.ItemRenderType.EQUIPPED);
						if (attachrender instanceof HMGRenderItemCustom
								&& !((HMGItemAttachmentBase) items[4].getItem()).has3dModel(4)) {
							GunPart_Render_attach(parts, state, flame, remainbullets, OffsetAndRotation, gripattachoffset, gripattachrotation, ((HMGRenderItemCustom) attachrender));
						}
					} else if (parts.isgrip && items[4].getItem() instanceof HMGItemAttachment_grip) {
						part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
					} else if (parts.issword && items[4].getItem() instanceof HMGItemSwordBase) {
						part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
					} else if (parts.isswordbase && items[4].getItem() instanceof HMGItemSwordBase) {
						part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
						IItemRenderer attachrender = MinecraftForgeClient.getItemRenderer(items[4], IItemRenderer.ItemRenderType.EQUIPPED);
						if (attachrender != null) {
							GL11.glPushMatrix();
							part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
							glTranslatef(gunitem.gunInfo.underoffsetpx * gunPartsScale, gunitem.gunInfo.underoffsetpy * gunPartsScale, gunitem.gunInfo.underoffsetpz * gunPartsScale);
							GL11.glRotatef(gunitem.gunInfo.underrotationy, 0, 1, 0);
							GL11.glRotatef(gunitem.gunInfo.underrotationx, 1, 0, 0);
							GL11.glRotatef(gunitem.gunInfo.underrotationz, 0, 0, 1);
							GL11.glScalef(1 / modelscala, 1 / modelscala, 1 / modelscala);
							GL11.glScalef(1 / (0.4f), 1 / (0.4f), 1 / (0.4f));
							attachrender.renderItem(IItemRenderer.ItemRenderType.ENTITY, items[4], datas);
							Minecraft.getMinecraft().renderEngine.bindTexture(texture);
							GL11.glPopMatrix();
						}
					} else if (items[4].getItem() instanceof HMGItem_Unified_Guns) {
						if (parts.isunderGunbase) {
							IItemRenderer attachrender = MinecraftForgeClient.getItemRenderer(items[4], IItemRenderer.ItemRenderType.EQUIPPED);
							if (attachrender != null) {
								boolean backUp = isfirstperson;
								GL11.glPushMatrix();

								HMGGunParts_Motion_PosAndRotation rotationCenterAndRotation = parts.getRenderinfCenter();
								if(!OffsetAndRotation.renderOnOff)return;

								transformParts(rotationCenterAndRotation,OffsetAndRotation,parts);
								part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
								glTranslatef(gunitem.gunInfo.underoffsetpx * gunPartsScale, gunitem.gunInfo.underoffsetpy * gunPartsScale, gunitem.gunInfo.underoffsetpz * gunPartsScale);
								GL11.glRotatef(gunitem.gunInfo.underrotationy, 0, 1, 0);
								GL11.glRotatef(gunitem.gunInfo.underrotationx, 1, 0, 0);
								GL11.glRotatef(gunitem.gunInfo.underrotationz, 0, 0, 1);
								HMGItem_Unified_Guns undergun = (HMGItem_Unified_Guns) items[4].getItem();
								glTranslatef(undergun.gunInfo.onunderoffsetpx * gunPartsScale, undergun.gunInfo.onunderoffsetpy * gunPartsScale, undergun.gunInfo.onunderoffsetpz * gunPartsScale);
								GL11.glRotatef(gunitem.gunInfo.onunderrotationy, 0, 1, 0);
								GL11.glRotatef(gunitem.gunInfo.onunderrotationx, 1, 0, 0);
								GL11.glRotatef(gunitem.gunInfo.onunderrotationz, 0, 0, 1);
								if (attachrender instanceof HMGRenderItemGun_U_NEW) {
									((HMGRenderItemGun_U_NEW) attachrender).isUnder = true;
								}
								GL11.glScalef(1 / (0.4f * modelscala * gunitem.gunInfo.inworldScale), 1 / (0.4f * modelscala * gunitem.gunInfo.inworldScale), 1 / (0.4f * modelscala * gunitem.gunInfo.inworldScale));
								
								attachrender.renderItem(IItemRenderer.ItemRenderType.ENTITY, items[4], datas);

								if (attachrender instanceof HMGRenderItemGun_U_NEW) {
									((HMGRenderItemGun_U_NEW) attachrender).isUnder = false;
								}
								Minecraft.getMinecraft().renderEngine.bindTexture(texture);
								GL11.glPopMatrix();
								isfirstperson = backUp;
							}
							part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
						} else if (parts.isunderGL && gunitem.gunInfo.guntype == 2) {
							part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
						} else if (parts.isunderSG && gunitem.gunInfo.guntype == 1) {
							part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
						}
					}
				} else {
					if (parts.isgripcover) {
						part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
					}
				}
			} else {
				part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
			}
		}
	}
	public void GunPart_Render_attach(HMGGunParts parts, GunState state, float flame, int remainbullets, HMGGunParts_Motion_PosAndRotation OffsetAndRotation,float[] attachoffset,float[] attachrotation,HMGRenderItemCustom attachrender){
		GL11.glPushMatrix();
		glTranslatef(attachoffset[0] * gunPartsScale, attachoffset[1] * gunPartsScale, attachoffset[2] * gunPartsScale);
		GL11.glRotatef(attachrotation[0], 0, 1, 0);
		GL11.glRotatef(attachrotation[1], 1, 0, 0);
		GL11.glRotatef(attachrotation[2], 0, 0, 1);
		((HMGRenderItemCustom) attachrender).renderaspart(pass);
		GL11.glPopMatrix();
		HMG_proxy.getMCInstance().getTextureManager().bindTexture(texture);
		part_Render(parts, state, flame, remainbullets, OffsetAndRotation);
	}
	public boolean partModel_render(HMGGunParts parts, GunState state, float flame, int remainbullets, HMGGunParts_Motion_PosAndRotation OffsetAndRotation){
		HMGGunParts_Motion_PosAndRotation rotationCenterAndRotation = parts.getRenderinfCenter();
		if(shouldRenderPartGeometry(parts) && parts.isLarm){
			renderarmL();
		}else if(shouldRenderPartGeometry(parts) && parts.isRarm){
			renderarmR();
		}
		HMGGunParts_Motion_PosAndRotation elevationInfo = parts.getSomethingPositions(guntemp.currentElevation,0);
		if (elevationInfo != null) {
			if(!elevationInfo.renderOnOff){
				return true;
			}
			transformParts(rotationCenterAndRotation,elevationInfo,parts);
		}
		HMGGunParts_Motion_PosAndRotation selectorInfo = parts.getSomethingPositions(guntemp.selector,1);
		if (selectorInfo != null) {
			if(!selectorInfo.renderOnOff){
				return true;
			}
			transformParts(rotationCenterAndRotation,selectorInfo,parts);
		}
		
		if(isPlacedGun && parts.hasbaseYawInfo) {
			HMGGunParts_Motion_PosAndRotation baserotationCenterAndRotation = parts.getYawInfo(turretYaw);
			if (baserotationCenterAndRotation != null) {
				transformParts(rotationCenterAndRotation,baserotationCenterAndRotation,parts);
			}
		}
		if(isPlacedGun && parts.hasbasePitchInfo){
			HMGGunParts_Motion_PosAndRotation baserotationCenterAndRotation = parts.getPitchInfo(turretPitch);
			if (baserotationCenterAndRotation != null) {
				transformParts(rotationCenterAndRotation,baserotationCenterAndRotation,parts);
			}
		}
		return super.partModel_render(parts, state, flame, remainbullets, OffsetAndRotation);
	}
	
	public float armrotationxr;
	public float armrotationyr;
	public float armrotationzr;
	public float armoffsetxr;
	public float armoffsetyr;
	public float armoffsetzr;
	
	public float armrotationxl;
	public float armrotationyl;
	public float armrotationzl;
	public float armoffsetxl;
	public float armoffsetyl;
	public float armoffsetzl;
	public float armoffsetscale = 1;
	
	public void renderarmL(){
		if(isfirstperson) {
			GL11.glPushMatrix();
			ResourceLocation resourcelocation = this.getEntityTexture(HMG_proxy.getMCInstance().thePlayer);
			if (resourcelocation == null) {
				resourcelocation = AbstractClientPlayer.getLocationSkin("default");
			}
			Minecraft.getMinecraft().renderEngine.bindTexture(resourcelocation);
			GL11.glScalef(1 / modelscala, 1 / modelscala, 1 / modelscala);
			GL11.glRotatef(180F, 1.0F, 0.0F, 0.0F);
			GL11.glTranslatef(0F, -0.5F, 0F);
			modelBipedMain.bipedLeftArm.rotateAngleX = armrotationzl;
			modelBipedMain.bipedLeftArm.rotateAngleY = armrotationyl;
			modelBipedMain.bipedLeftArm.rotateAngleZ = 0;
			modelBipedMain.bipedLeftArm.offsetX = armoffsetxl * armoffsetscale;
			modelBipedMain.bipedLeftArm.offsetY = armoffsetyl * armoffsetscale;
			modelBipedMain.bipedLeftArm.offsetZ = armoffsetzl * armoffsetscale;
			modelBipedMain.bipedLeftArm.render(0.0625f);
			Minecraft.getMinecraft().renderEngine.bindTexture(texture);
			GL11.glScalef(modelscala, modelscala, modelscala);
			GL11.glPopMatrix();
		}
	}
	public void renderarmR(){
		if(isfirstperson) {
			GL11.glPushMatrix();
			ResourceLocation resourcelocation = this.getEntityTexture(HMG_proxy.getMCInstance().thePlayer);
			if (resourcelocation == null) {
				resourcelocation = AbstractClientPlayer.getLocationSkin("default");
			}
			Minecraft.getMinecraft().renderEngine.bindTexture(resourcelocation);
			GL11.glScalef(1 / modelscala, 1 / modelscala, 1 / modelscala);
			GL11.glRotatef(180F, 1.0F, 0.0F, 0.0F);
			GL11.glTranslatef(0F, -0.5F, 0F);
			modelBipedMain.bipedRightArm.rotateAngleX = armrotationzr;
			modelBipedMain.bipedRightArm.rotateAngleY = armrotationyr;
			modelBipedMain.bipedRightArm.rotateAngleZ = 0;
			modelBipedMain.bipedRightArm.offsetX = armoffsetxr * armoffsetscale;
			modelBipedMain.bipedRightArm.offsetY = armoffsetyr * armoffsetscale;
			modelBipedMain.bipedRightArm.offsetZ = armoffsetzr * armoffsetscale;
			modelBipedMain.bipedRightArm.render(0.0625f);
			Minecraft.getMinecraft().renderEngine.bindTexture(texture);
			GL11.glScalef(modelscala, modelscala, modelscala);
			GL11.glPopMatrix();
		}
	}
	public ResourceLocation getEntityTexture(AbstractClientPlayer p_110775_1_)
	{
		return p_110775_1_.getLocationSkin();
	}
}
