package handmadeguns.client.render;

import handmadeguns.client.HMGDroppedGunRenderHelper;
import handmadeguns.client.HMGCustomNpcRenderCompat;

import cpw.mods.fml.client.FMLClientHandler;
import handmadeguns.HandmadeGunsCore;
import handmadeguns.HMGGunSkinRegistry;
import handmadeguns.items.*;
import handmadeguns.items.guns.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.script.Invocable;
import javax.script.ScriptException;

import java.util.ArrayList;

import static handmadeguns.event.HMGEventZoom.setUp3DView;
import static java.lang.Math.abs;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.ENTITY;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED;
import static net.minecraftforge.client.IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON;
import static org.lwjgl.opengl.GL11.*;

public class HMGRenderItemGun_U implements IItemRenderer {
	//this is the bad renderer type, it's really buggy and SHIT and only used for things like shotguns and anti tank weapons for some reason.
	//it breaks when you don't have any ammo and go to ADS.

	public IModelCustom modeling;
	public ResourceLocation guntexture;
	private ResourceLocation gunSkinTexture;
	public static float smoothing;
	public ModelBiped modelBipedMain;
	public float modelscala;

	public float modely;

	public float modely0;
	public float modely1;
	public float modely2;

	public float modelx;

	public float modelx0;
	public float modelx1;
	public float modelx2;

	public float modelz;
	public float modelz0;
	public float modelz1;
	public float modelz2;

	public float rotationx;
	public float rotationx0;
	public float rotationx1;
	public float rotationx2;

	public float rotationy;
	public float rotationy0;
	public float rotationy1;
	public float rotationy2;

	public float rotationz;
	public float rotationz0;
	public float rotationz1;
	public float rotationz2;

	public boolean armtrue;
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

	public float nox;
	public float noy;
	public float noz;

	public float mat31posx;
	public float mat31posy;
	public float mat31posz;
	public float mat31rotex;
	public float mat31rotey;
	public float mat31rotez;

	public float mat32posx;
	public float mat32posy;
	public float mat32posz;
	public float mat32rotex;
	public float mat32rotey;
	public float mat32rotez;
	public float mat2offsetz = 0.4f;
	public float armoffsetscale = 1;
	
	public boolean mat22 = false;
	public float mat22rotationx = 90F;
	public float mat22rotationy = 0F;
	public float mat22rotationz = 0F;
	public float mat22offsetx = 0F;
	public float mat22offsety = 1.5F;
	public float mat22offsetz = 2F;
	
	public float mat25rotationx = 0F;
	public float mat25rotationy = 0F;
	public float mat25rotationz = -90F;
	public float mat25offsetx = 0F;
	public float mat25offsety = 0.75F;
	public float mat25offsetz = 1.1F;
	
	
	public float Sprintrotationx = 20F;
	public float Sprintrotationy = 60F;
	public float Sprintrotationz = 0F;
	public float Sprintoffsetx = 0.5F;
	public float Sprintoffsety = 0.0F;
	public float Sprintoffsetz = 0.5F;
	
	public float jump = 0;
	public boolean all_jump = false;
	public boolean cock_left = false;
	public boolean mat25 = false;
	public boolean mat2 = false;
	public boolean remat31 = true;
	public boolean remat3 = true;

	public boolean reloadanim = false;
	public ArrayList<Float[]> reloadanimation = new ArrayList<Float[]>();
	public boolean nodrawmat35 = false;

	public float barrelattachoffset[] = new float[3];
	public float barrelattachrotation[] = new float[3];
	public float sightattachoffset[] = new float[3];
	public float sightattachrotation[] = new float[3];
	public float gripattachoffset[] = new float[3];
	public float gripattachrotation[] = new float[3];
	public float lightattachoffset[] = new float[3];
	public float lightattachrotation[] = new float[3];
	public NBTTagCompound nbt;
	ItemStack[] items = new ItemStack[6];

	public HMGRenderItemGun_U(IModelCustom modelgun, ResourceLocation texture, float scala, float high, float high1,
							  float high2, float widthx0, float widthx1, float widthx2, float widthz0, float widthz1, float widthz2, float rotax0
			, float rotax1, float rotax2, float rotay0, float rotay1, float rotay2, float rotaz0, float rotaz1, float rotaz2,
							  boolean arm, float armrxr, float armryr, float armrzr, float offxr, float offyr, float offzr
			, float armrxl, float armryl, float armrzl, float offxl, float offyl, float offzl, float nx, float ny, float nz,
							  float m31px, float m31py, float m31pz, float m31rx, float m31ry, float m31rz
			, float m32px, float m32py, float m32pz, float m32rx, float m32ry, float m32rz) {
		modeling = modelgun;
		this.modelBipedMain = new ModelBiped(0.5F);

		this.modelscala = scala;
		this.modely = high;
		this.modely0 = high;
		this.modely1 = high1;
		this.modely2 = high2;

		this.modelx = widthx0;
		this.modelx0 = widthx0;
		this.modelx1 = widthx1;
		this.modelx2 = widthx2;
		this.modelz = widthz0;
		this.modelz0 = widthz0;
		this.modelz1 = widthz1;
		this.modelz2 = widthz2;

		this.rotationx = rotax0;
		this.rotationx0 = rotax0;
		this.rotationx1 = rotax1;
		this.rotationx2 = rotax2;
		this.rotationy = rotay0;
		this.rotationy0 = rotay0;
		this.rotationy1 = rotay1;
		this.rotationy2 = rotay2;
		this.rotationz = rotaz0;
		this.rotationz0 = rotaz0;
		this.rotationz1 = rotaz1;
		this.rotationz2 = rotaz2;

		this.armtrue = arm;
		this.armrotationxr = armrxr;
		this.armrotationyr = armryr;
		this.armrotationxr = armrzr;
		this.armoffsetxr = offxr;
		this.armoffsetyr = offyr;
		this.armoffsetzr = offzr;
		this.armrotationxl = armrxl;
		this.armrotationyl = armryl;
		this.armrotationxl = armrzl;
		this.armoffsetxl = offxl;
		this.armoffsetyl = offyl;
		this.armoffsetzl = offzl;

		this.nox = nx;
		this.noy = ny;
		this.noz = nz;

		this.mat31posx = m31px;
		this.mat31posy = m31py;
		this.mat31posz = m31pz;
		this.mat31rotex = m31rx;
		this.mat31rotez = m31rz;
		this.mat31rotey = m31ry;

		this.mat32posx = m32px;
		this.mat32posy = m32py;
		this.mat32posz = m32pz;
		this.mat32rotex = m32rx;
		this.mat32rotez = m32ry;
		this.mat32rotey = m32rz;

	}
	public HMGRenderItemGun_U(IModelCustom modelgun, ResourceLocation texture, float scala, float high, float high1,
							  float high2, float widthx0, float widthx1, float widthx2, float widthz0, float widthz1, float widthz2, float rotax0
			, float rotax1, float rotax2, float rotay0, float rotay1, float rotay2, float rotaz0, float rotaz1, float rotaz2,
							  boolean arm, float armrxr, float armryr, float armrzr, float offxr, float offyr, float offzr
			, float armrxl, float armryl, float armrzl, float offxl, float offyl, float offzl, float nx, float ny, float nz,
							  float m31px, float m31py, float m31pz, float m31rx, float m31ry, float m31rz
			, float m32px, float m32py, float m32pz, float m32rx, float m32ry, float m32rz , float armoffsetscalein) {
		guntexture = texture;
		modeling = modelgun;
		this.modelBipedMain = new ModelBiped(0.5F);

		this.modelscala = scala;
		this.modely = high;
		this.modely0 = high;
		this.modely1 = high1;
		this.modely2 = high2;

		this.modelx = widthx0;
		this.modelx0 = widthx0;
		this.modelx1 = widthx1;
		this.modelx2 = widthx2;
		this.modelz = widthz0;
		this.modelz0 = widthz0;
		this.modelz1 = widthz1;
		this.modelz2 = widthz2;

		this.rotationx = rotax0;
		this.rotationx0 = rotax0;
		this.rotationx1 = rotax1;
		this.rotationx2 = rotax2;
		this.rotationy = rotay0;
		this.rotationy0 = rotay0;
		this.rotationy1 = rotay1;
		this.rotationy2 = rotay2;
		this.rotationz = rotaz0;
		this.rotationz0 = rotaz0;
		this.rotationz1 = rotaz1;
		this.rotationz2 = rotaz2;

		this.armtrue = arm;
		this.armrotationxr = armrxr;
		this.armrotationyr = armryr;
		this.armrotationxr = armrzr;
		this.armoffsetxr = offxr;
		this.armoffsetyr = offyr;
		this.armoffsetzr = offzr;
		this.armrotationxl = armrxl;
		this.armrotationyl = armryl;
		this.armrotationxl = armrzl;
		this.armoffsetxl = offxl;
		this.armoffsetyl = offyl;
		this.armoffsetzl = offzl;

		this.nox = nx;
		this.noy = ny;
		this.noz = nz;

		this.mat31posx = m31px;
		this.mat31posy = m31py;
		this.mat31posz = m31pz;
		this.mat31rotex = m31rx;
		this.mat31rotez = m31rz;
		this.mat31rotey = m31ry;

		this.mat32posx = m32px;
		this.mat32posy = m32py;
		this.mat32posz = m32pz;
		this.mat32rotex = m32rx;
		this.mat32rotez = m32ry;
		this.mat32rotey = m32rz;

		this.armoffsetscale = armoffsetscalein;
	}

	//@Override
	//public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
	//	GunInfo info = GunInfo.getGunInfo(stack);
//
	//	if (type == ItemRenderType.INVENTORY) {
	//		if (info.useModelAsIcon && info.model != null) {
	//			renderGunModelAsIcon(info);
	//		} else {
	//			render2DIcon(info);
	//		}
	//		return;
	//	}
//
	//	// everything else (equipped, entity, etc)
	//	renderGunModel(info);
	//}
	//TODO none of this is real/erroring, we need to either add these methods somehow
	// or do different logic to add 3d model rendering for gun item icons
	
	public void setSomeParam(IModelCustom modelgun, ResourceLocation texture, float scala, float high, float high1,
	                          float high2, float widthx0, float widthx1, float widthx2, float widthz0, float widthz1, float widthz2, float rotax0
			, float rotax1, float rotax2, float rotay0, float rotay1, float rotay2, float rotaz0, float rotaz1, float rotaz2,
			                  boolean arm, float armrxr, float armryr, float armrzr, float offxr, float offyr, float offzr
			, float armrxl, float armryl, float armrzl, float offxl, float offyl, float offzl, float nx, float ny, float nz,
			                  float m31px, float m31py, float m31pz, float m31rx, float m31ry, float m31rz
			, float m32px, float m32py, float m32pz, float m32rx, float m32ry, float m32rz , float armoffsetscalein) {
		guntexture = texture;
		modeling = modelgun;
		this.modelBipedMain = new ModelBiped(0.5F);
		
		this.modelscala = scala;
		this.modely = high;
		this.modely0 = high;
		this.modely1 = high1;
		this.modely2 = high2;
		
		this.modelx = widthx0;
		this.modelx0 = widthx0;
		this.modelx1 = widthx1;
		this.modelx2 = widthx2;
		this.modelz = widthz0;
		this.modelz0 = widthz0;
		this.modelz1 = widthz1;
		this.modelz2 = widthz2;
		
		this.rotationx = rotax0;
		this.rotationx0 = rotax0;
		this.rotationx1 = rotax1;
		this.rotationx2 = rotax2;
		this.rotationy = rotay0;
		this.rotationy0 = rotay0;
		this.rotationy1 = rotay1;
		this.rotationy2 = rotay2;
		this.rotationz = rotaz0;
		this.rotationz0 = rotaz0;
		this.rotationz1 = rotaz1;
		this.rotationz2 = rotaz2;
		
		this.armtrue = arm;
		this.armrotationxr = armrxr;
		this.armrotationyr = armryr;
		this.armrotationxr = armrzr;
		this.armoffsetxr = offxr;
		this.armoffsetyr = offyr;
		this.armoffsetzr = offzr;
		this.armrotationxl = armrxl;
		this.armrotationyl = armryl;
		this.armrotationxl = armrzl;
		this.armoffsetxl = offxl;
		this.armoffsetyl = offyl;
		this.armoffsetzl = offzl;
		
		this.nox = nx;
		this.noy = ny;
		this.noz = nz;
		
		this.mat31posx = m31px;
		this.mat31posy = m31py;
		this.mat31posz = m31pz;
		this.mat31rotex = m31rx;
		this.mat31rotez = m31rz;
		this.mat31rotey = m31ry;
		
		this.mat32posx = m32px;
		this.mat32posy = m32py;
		this.mat32posz = m32pz;
		this.mat32rotex = m32rx;
		this.mat32rotez = m32ry;
		this.mat32rotey = m32rz;
		
		this.armoffsetscale = armoffsetscalein;
	}
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {

		switch (type) {
			case INVENTORY:
				// only return true (i.e. handle inventory rendering ourselves)
				// if this ItemStack is one of our guns and its GunInfo requests model-as-icon
				if (item != null && item.getItem() instanceof HMGItem_Unified_Guns) {
					HMGItem_Unified_Guns gun = (HMGItem_Unified_Guns) item.getItem();
					if (gun != null && gun.gunInfo != null && gun.gunInfo.useModelAsIcon && this.modeling != null) {
						return HMGInventoryIconManager.claimCachedIcon(item, this);
					}
				}
				return false;
			case FIRST_PERSON_MAP:
				return false;
			case EQUIPPED_FIRST_PERSON:
			case ENTITY:
			case EQUIPPED:
				return true;
			default:
				return false;
		}
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		switch (type) {
			// case 1:
			case INVENTORY:
			case FIRST_PERSON_MAP:
				return false;
			case EQUIPPED:
				if (helper == ItemRendererHelper.BLOCK_3D && HMGCustomNpcRenderCompat.isRenderingCustomNpc()) {
					return false;
				}
				return true;
			case EQUIPPED_FIRST_PERSON:
			case ENTITY:
				return true;
		}
		return false;
	}

	float rotez;
	float rotey;
	float rotex;
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		if (type == ItemRenderType.INVENTORY && !HMGInventoryIconManager.isCapturing()) {
			HMGInventoryIconManager.renderCachedIcon(item);
			return;
		}
		gunSkinTexture = HMGGunSkinTextures.available(HMGGunSkinRegistry.appliedTexture(item));
		GL11.glEnable(GL_BLEND);
		GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		GL11.glColor4f(1, 1, 1, 1F);
//		glMaterialf(GL_FRONT_AND_BACK,GL_SHININESS,120);
		float scala = this.modelscala;
		HMGItem_Unified_Guns gun;
		if (item.getItem() instanceof HMGItem_Unified_Guns)
			gun = (HMGItem_Unified_Guns) item.getItem();
		else {
			GL11.glDepthMask(true);
			GL11.glDisable(GL_BLEND);
			return;
		}
		if(!gun.gunInfo.userenderscript) {
			nbt = item.getTagCompound();
			if (nbt == null) gun.checkTags(item);
			nbt = item.getTagCompound();
			items[0] = null;
			items[1] = null;
			items[2] = null;
			items[3] = null;
			items[4] = null;
			items[5] = null;
			NBTTagList tags = (NBTTagList)nbt.getTag("Items");
			if(tags != null) {
				for (int i = 0; i < tags.tagCount(); i++)//133
				{
					NBTTagCompound tagCompound = tags.getCompoundTagAt(i);
					int slot = tagCompound.getByte("Slot");
					if (slot >= 0 && slot < items.length) {
						items[slot] = ItemStack.loadItemStackFromNBT(tagCompound);
					}
				}
			}
			rotex = nbt.getFloat("rotex");
			rotey = nbt.getFloat("rotey");
			rotez = nbt.getFloat("rotez");
		/*boolean cocking = nbt.getBoolean("Cocking");
		int cockingtime = nbt.getInteger("CockingTime");
		boolean recoiled = nbt.getBoolean("Recoiled");
					int mode = nbt.getInteger("HMGMode");
		int recoiledtime = nbt.getInteger("RecoiledTime");*/
			
			float reco;
			Minecraft minecraft = FMLClientHandler.instance().getClient();
			switch (type) {
				case INVENTORY:
					glMatrixForRenderInInventory();
					break;
				case EQUIPPED_FIRST_PERSON://first
				{

					setUp3DView(Minecraft.getMinecraft(),smoothing);

					EntityPlayer entityplayer = (EntityPlayer)Minecraft.getMinecraft().thePlayer;

					// --- FOV scaling ---
					float baseFOV = 95.0F;
					float playerFOV = Minecraft.getMinecraft().gameSettings.fovSetting;
					float fovScale = Math.max(0.7F, Math.min(1.3F, playerFOV / baseFOV));

					float f1 = entityplayer.distanceWalkedModified - entityplayer.prevDistanceWalkedModified;
					float f2 = -(entityplayer.distanceWalkedModified + f1 * smoothing) * fovScale;
					float f3 = (entityplayer.prevCameraYaw + (entityplayer.cameraYaw - entityplayer.prevCameraYaw) * smoothing) * fovScale;
					float f4 = (entityplayer.prevCameraPitch + (entityplayer.cameraPitch - entityplayer.prevCameraPitch) * smoothing);
					GL11.glTranslatef(MathHelper.sin(f2 * (float)Math.PI) * f3 * 0.5F, -Math.abs(MathHelper.cos(f2 * (float)Math.PI) * f3), 0.0F);
					GL11.glRotatef(MathHelper.sin(f2 * (float)Math.PI) * f3 * 3.0F, 0.0F, 0.0F, 1.0F);
					GL11.glRotatef(Math.abs(MathHelper.cos(f2 * (float)Math.PI - 0.2F) * f3) * 5.0F, 1.0F, 0.0F, 0.0F);
					GL11.glRotatef(f4, 1.0F, 0.0F, 0.0F);

					// --- model offsets ---
					modelx *= fovScale;
					modely *= fovScale;
					modelz *= fovScale;

//					boolean cocking = nbt.getBoolean("Cocking");
					float cockingtime = nbt.getInteger("CockingTime") + smoothing - 1;
					boolean recoiled = nbt.getBoolean("Recoiled");
					int mode = nbt.getInteger("HMGMode");
//					int recoiledtime = nbt.getInteger("RecoiledTime");
					byte boltprogress = nbt.getByte("Bolt");
//					System.out.println("" + boltprogress);
					boltprogress -= smoothing;
					int cycle = (int)gun.gunInfo.cycle;
					float boltoffsetcof;
					if (boltprogress <= cycle / 2)//�{���g��ޒ�
						boltoffsetcof = cycle - boltprogress;
					else
						boltoffsetcof = cycle - (cycle - boltprogress);//�{���g�O�i��
					if (boltoffsetcof < 0) boltoffsetcof = 0;
					GL11.glPushMatrix();//glstart1
					ItemStack itemstackSight =null;
					itemstackSight = items[1];
					if (itemstackSight == null) {
						modely = modely0;
						modelx = modelx0;
						modelz = modelz0;

						rotationx = rotationx0;
						rotationy = rotationy0;
						rotationz = rotationz0;
					} else {
						if(itemstackSight.getItem() instanceof HMGItemSightBase && ((HMGItemSightBase) itemstackSight.getItem()).needgunoffset){
							modelx =0.694f-(sightattachoffset[0] + ((HMGItemSightBase) itemstackSight.getItem()).gunoffset[0])*scala;
							modely =1.8f-(sightattachoffset[1] + ((HMGItemSightBase) itemstackSight.getItem()).gunoffset[1])*scala;
							modelz =-(sightattachoffset[2] + ((HMGItemSightBase) itemstackSight.getItem()).gunoffset[2])*scala;

							rotationx = rotationx0 + ((HMGItemSightBase) itemstackSight.getItem()).gunrotation[0];
							rotationy = rotationy0 + ((HMGItemSightBase) itemstackSight.getItem()).gunrotation[1];
							rotationz = rotationz0 + ((HMGItemSightBase) itemstackSight.getItem()).gunrotation[2];
						}else if ( itemstackSight.getItem() instanceof HMGItemAttachment_reddot) {
							modely = modely1;
							modelx = modelx1;
							modelz = modelz1;

							rotationx = rotationx1;
							rotationy = rotationy1;
							rotationz = rotationz1;
						} else if ( itemstackSight.getItem() instanceof HMGItemAttachment_scope) {
							modely = modely2;
							modelx = modelx2;
							modelz = modelz2;

							rotationx = rotationx2;
							rotationy = rotationy2;
							rotationz = rotationz2;
						}
					}
					if (HandmadeGunsCore.Key_ADS(entityplayer)) { //attachment scopes
						if(itemstackSight != null && itemstackSight.getItem() instanceof HMGItemSightBase) {
							if(((HMGItemSightBase)itemstackSight.getItem()).scopeonly){
								GL11.glPopMatrix();//glend1
								break;
							}else
							if (itemstackSight.getItem() instanceof HMGItemAttachment_reddot){
								if (!gun.gunInfo.zoomrer) {
									GL11.glPopMatrix();//glend1
									break;
								}
							} else
							if (itemstackSight.getItem() instanceof HMGItemAttachment_scope) {
								if (!gun.gunInfo.zoomres) {
									GL11.glPopMatrix();//glend1
									break;
								}
							}
						}else {
							if (!gun.gunInfo.zoomren) {
								GL11.glPopMatrix();//glend1
								break;
							}
						}
					}
					if (HandmadeGunsCore.Key_ADS(entityplayer)) { //todone, blame maybe?
						if (getremainingbullet(item)<=0) {
							if (!reloadanim)
								glMatrixForRenderInEquipped_reload();
							else {
								reco = -0.2F;
								glMatrixForRenderInEquipped(reco);
							}
						} else {
							//if (gun.recoilre)
							//if (gun.recoilreBolts(item))
							if (!recoiled) {
								glMatrixForRenderInEquippedADS(-1.4F - 0.03F * (1 - smoothing));
							} else {
								reco = -1.4F;
								glMatrixForRenderInEquippedADS(reco);
							}
						}

					} else {
						if (isentitysprinting(entityplayer) || (gun.gunInfo.needfix && !nbt.getBoolean("HMGfixed") ) ) {
							//if (!HandmadeGunsCore.Key_ADS(entityplayer)) {
								glMatrixForRenderInEquipped(0);
					/*if(item.getItem() instanceof HMGItemGun_HG){
					GL11.glRotatef(-60F, 1.0F, 0.0F, 0.0F);
					GL11.glTranslatef(0.5F, 0F, 0.5F);
					}else{
					GL11.glRotatef(90F, 0.0F, 1.0F, 0.0F);
					GL11.glTranslatef(0.5F, 0F, 0.5F);
					}*/
								GL11.glRotatef(Sprintrotationx, 1.0F, 0.0F, 0.0F);
								GL11.glRotatef(Sprintrotationy, 0.0F, 1.0F, 0.0F);
								GL11.glRotatef(Sprintrotationz, 0.0F, 0.0F, 1.0F);
								GL11.glTranslatef(Sprintoffsetx, Sprintoffsety, Sprintoffsetz);
								//System.out.println("TEST");
							//THIS IS LITERALLY THE OTHER RENDER. IT DOES NOT NEED TO BE MODIFIED.
							//}
						} else if (getremainingbullet(item)<=0) {
							if (!reloadanim)
								glMatrixForRenderInEquipped_reload();
							else {
								reco = -0.2F;
								glMatrixForRenderInEquipped(reco);
							}
						} else {
							//if (gun.recoilre)
							//if (gun.recoilreBolts(item))
							if (!recoiled) {
								glMatrixForRenderInEquipped(-0.2F /*- 0.05F * (1 - smoothing)*/);
							} else {
								glMatrixForRenderInEquipped(-0.2F);
							}
						}

					}
					if (!recoiled) {
						GL11.glRotatef(jump * (1 - smoothing), 1.0F, 0.0F, 0.0F);
					}

					if (all_jump) {
						if (cockingtime > 0){

							if (cockingtime > 0 && cockingtime < ((gun.gunInfo.cocktime + smoothing - 1) / 2)) {
								//GL11.glTranslatef(0F, 0F, -cockingtime*0.1F);
								GL11.glRotatef(-cockingtime * 1F, 1.0F, 0.0F, 0.0F);
							} else {
								//GL11.glTranslatef(0F, 0F, (cockingtime-(gun.cocktime + smoothing -1))*0.1F);
								GL11.glRotatef((cockingtime - (gun.gunInfo.cocktime + smoothing - 1)) * 1F, 1.0F, 0.0F, 0.0F);
							}
						}
					}
					//GL11.glPushMatrix();//glstart1

					Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);

					GL11.glScalef(scala, scala, scala);
					if (nbt.getBoolean("IsReloading")) {
						if (reloadanim && !nbt.getBoolean("WaitReloading")) {
							float reloadti = nbt.getInteger("RloadTime");
							Float[] tgt = new Float[]{0f,
									0f, 0f, 0f, 0f, 0f, 0f,

									0f,

									0f,
									0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,
									0f,
									0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,
									0f,
									0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f};
							Float[] pre = new Float[]{0f,
									0f, 0f, 0f, 0f, 0f, 0f,

									0f,

									0f,
									0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,
									0f,
									0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,
									0f,
									0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f};
							for (int i = 0; i < reloadanimation.size(); i++) {
								tgt = reloadanimation.get(i);
								if (tgt.length < 25) continue;
								if (tgt[0] > reloadti) {
									if (i > 0) pre = reloadanimation.get(i - 1);
									break;
								}
							}
							reloadti += smoothing;
							//�z��̓��e��
							//0.�w��̏��  �ɂȂ�܂ł̎���
							//�@��
							//1.�e�̉�]���� xyz,xzy,yxz,yzx,zxy,zyx
							//2 3 4.�e�̉�]���S(x,y,z)
							//5 6 7.�e�̉�]��
							//8.�}�K�W�����h�����Ă邩�i0 yet 1 already�j
							//9  10 11 12 13 14.����̈ʒu�A��]��
							//15 16 17 18 19 20.�E��̈ʒu�A��]��
							//21.�}�K�W���̉�]���� xyz,xzy,yxz,yzx,zxy,zyx
							//22 23 24 25 26 27 28 29 30.�}�K�W���ʒu�A��]���S�y�щ�]�ʁA�ړ����ĉ�]����
							//�e�̕`��J�n
							GL11.glTranslatef(tgt[2] + (pre[2] - tgt[2]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti), tgt[3] + (pre[3] - tgt[3]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti), tgt[4] + (pre[4] - tgt[4]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti));
							switch (pre[1].intValue()) {
								case 0:
									GL11.glRotatef(pre[5] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 1, 0, 0);
									GL11.glRotatef(pre[6] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 1, 0);
									GL11.glRotatef(pre[7] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 0, 1);
									break;
								case 1:
									GL11.glRotatef(pre[5] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 1, 0, 0);
									GL11.glRotatef(pre[6] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 0, 1);
									GL11.glRotatef(pre[7] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 1, 0);
									break;
								case 2:
									GL11.glRotatef(pre[5] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 1, 0);
									GL11.glRotatef(pre[6] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 1, 0, 0);
									GL11.glRotatef(pre[7] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 0, 1);
									break;
								case 3:
									GL11.glRotatef(pre[5] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 1, 0);
									GL11.glRotatef(pre[6] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 0, 1);
									GL11.glRotatef(pre[7] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 1, 0, 0);
									break;
								case 4:
									GL11.glRotatef(pre[5] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 0, 1);
									GL11.glRotatef(pre[6] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 1, 0, 0);
									GL11.glRotatef(pre[7] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 1, 0);
									break;
								case 5:
									GL11.glRotatef(pre[5] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 0, 1);
									GL11.glRotatef(pre[6] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 1, 0);
									GL11.glRotatef(pre[7] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 1, 0, 0);
									break;

							}
							switch (tgt[1].intValue()) {
								case 0:
									GL11.glRotatef(tgt[5] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 1, 0, 0);
									GL11.glRotatef(tgt[6] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 1, 0);
									GL11.glRotatef(tgt[7] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 0, 1);
									break;
								case 1:
									GL11.glRotatef(tgt[5] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 1, 0, 0);
									GL11.glRotatef(tgt[6] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 0, 1);
									GL11.glRotatef(tgt[7] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 1, 0);
									break;
								case 2:
									GL11.glRotatef(tgt[5] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 1, 0);
									GL11.glRotatef(tgt[6] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 1, 0, 0);
									GL11.glRotatef(tgt[7] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 0, 1);
									break;
								case 3:
									GL11.glRotatef(tgt[5] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 1, 0);
									GL11.glRotatef(tgt[6] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 0, 1);
									GL11.glRotatef(tgt[7] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 1, 0, 0);
									break;
								case 4:
									GL11.glRotatef(tgt[5] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 0, 1);
									GL11.glRotatef(tgt[6] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 1, 0, 0);
									GL11.glRotatef(tgt[7] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 1, 0);
									break;
								case 5:
									GL11.glRotatef(tgt[5] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 0, 1);
									GL11.glRotatef(tgt[6] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 1, 0);
									GL11.glRotatef(tgt[7] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 1, 0, 0);
									break;

							}
							GL11.glTranslatef(-tgt[2] - (pre[2] - tgt[2]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti), -tgt[3] - (pre[3] - tgt[3]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti), -tgt[4] - (pre[4] - tgt[4]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti));
							renderpartofmodel("mat1");
							renderpartofmodel("mats" + String.valueOf(mode));
							GL11.glPushMatrix();
							GL11.glRotatef(-minecraft.thePlayer.rotationPitch,1,0,0);
							renderpartofmodel("matbase");
							GL11.glPopMatrix();
							if (remat31) {
								renderpartofmodel("mat31");
							}
							renderpartofmodel("mat32");
							renderpartofmodel("mat24");
							GL11.glTranslatef(0.0F, 0.0F, -0.4F);
							renderpartofmodel("mat2");
							GL11.glTranslatef(0.0F, 0.0F, 0.4F);
							{
								GL11.glPushMatrix();//glstart11
								GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
								GL11.glRotatef(mat25rotationx, 1.0F, 0.0F, 0.0F);
								GL11.glRotatef(mat25rotationy, 0.0F, 1.0F, 0.0F);
								GL11.glRotatef(mat25rotationz, 0.0F, 0.0F, 1.0F);
								GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);
								GL11.glTranslatef(0F, 0F, -(gun.gunInfo.cocktime / 2) * 0.1F);
								renderpartofmodel("mat25");
								GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
								GL11.glRotatef(-mat25rotationx, 1.0F, 0.0F, 0.0F);
								GL11.glRotatef(-mat25rotationy, 0.0F, 1.0F, 0.0F);
								GL11.glRotatef(-mat25rotationz, 0.0F, 0.0F, 1.0F);
								GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);
								GL11.glTranslatef(0F, 0F, (gun.gunInfo.cocktime / 2) * 0.1F);
								GL11.glPopMatrix();//glend11
							}
							if (mat22) {
								GL11.glTranslatef(mat22offsetx, mat22offsety, mat22offsetz);
								GL11.glRotatef(mat22rotationx, 1.0F, 0.0F, 0.0F);
								GL11.glRotatef(mat22rotationy, 0.0F, 1.0F, 0.0F);
								GL11.glRotatef(mat22rotationz, 0.0F, 0.0F, 1.0F);
								GL11.glTranslatef(-mat22offsetx, -mat22offsety, -mat22offsetz);
								renderpartofmodel("mat22");
								GL11.glTranslatef(mat22offsetx, mat22offsety, mat22offsetz);
								GL11.glRotatef(-mat22rotationx, 1.0F, 0.0F, 0.0F);
								GL11.glRotatef(-mat22rotationy, 0.0F, 1.0F, 0.0F);
								GL11.glRotatef(-mat22rotationz, 0.0F, 0.0F, 1.0F);
								GL11.glTranslatef(-mat22offsetx, -mat22offsety, -mat22offsetz);
							} else {
								renderpartofmodel("mat22");
							}
							this.model(item,EQUIPPED_FIRST_PERSON,data);
							this.modelSight(item,EQUIPPED,data);

							if (tgt[8] == 1) {
								ItemStack magstack = items[5];
								if(magstack != null) {
									IItemRenderer magrender = MinecraftForgeClient.getItemRenderer(magstack, type);
									if (magrender instanceof HMGRenderItemCustom && magrender.handleRenderType(magstack, type)) {
										((HMGRenderItemCustom)magrender).renderaspart();
										Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
									}else renderpartofmodel("mat3");
								}else renderpartofmodel("mat3");
								renderpartofmodel("mat35");
							} else {
								//�ړ����ĉ�
								GL11.glTranslatef(tgt[22] + (pre[22] - tgt[22]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti), tgt[23] + (pre[23] - tgt[23]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti), tgt[24] + (pre[24] - tgt[24]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti));
								GL11.glTranslatef(tgt[25] + (pre[25] - tgt[25]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti), tgt[26] + (pre[26] - tgt[26]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti), tgt[27] + (pre[27] - tgt[27]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti));
//							GL11.glRotatef(tgt[23]+(pre[23]-tgt[23])/(tgt[0]- pre[0])*(tgt[0]-reloadti),1,0,0);
//							GL11.glRotatef(tgt[24]+(pre[24]-tgt[24])/(tgt[0]- pre[0])*(tgt[0]-reloadti),0,1,0);
//							GL11.glRotatef(tgt[25]+(pre[25]-tgt[25])/(tgt[0]- pre[0])*(tgt[0]-reloadti),0,0,1);
								switch (pre[21].intValue()) {
									case 0:
										GL11.glRotatef(pre[28] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 1, 0, 0);
										GL11.glRotatef(pre[29] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 1, 0);
										GL11.glRotatef(pre[30] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 0, 1);
										break;
									case 1:
										GL11.glRotatef(pre[28] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 1, 0, 0);
										GL11.glRotatef(pre[29] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 0, 1);
										GL11.glRotatef(pre[30] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 1, 0);
										break;
									case 2:
										GL11.glRotatef(pre[28] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 1, 0);
										GL11.glRotatef(pre[29] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 1, 0, 0);
										GL11.glRotatef(pre[30] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 0, 1);
										break;
									case 3:
										GL11.glRotatef(pre[28] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 1, 0);
										GL11.glRotatef(pre[29] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 0, 1);
										GL11.glRotatef(pre[30] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 1, 0, 0);
										break;
									case 4:
										GL11.glRotatef(pre[28] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 0, 1);
										GL11.glRotatef(pre[29] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 1, 0, 0);
										GL11.glRotatef(pre[30] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 1, 0);
										break;
									case 5:
										GL11.glRotatef(pre[28] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 0, 1);
										GL11.glRotatef(pre[29] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 0, 1, 0);
										GL11.glRotatef(pre[30] * (tgt[0] - reloadti) / (tgt[0] - pre[0]), 1, 0, 0);
										break;

								}
								switch (tgt[21].intValue()) {
									case 0:
										GL11.glRotatef(tgt[28] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 1, 0, 0);
										GL11.glRotatef(tgt[29] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 1, 0);
										GL11.glRotatef(tgt[30] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 0, 1);
										break;
									case 1:
										GL11.glRotatef(tgt[28] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 1, 0, 0);
										GL11.glRotatef(tgt[29] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 0, 1);
										GL11.glRotatef(tgt[30] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 1, 0);
										break;
									case 2:
										GL11.glRotatef(tgt[28] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 1, 0);
										GL11.glRotatef(tgt[29] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 1, 0, 0);
										GL11.glRotatef(tgt[30] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 0, 1);
										break;
									case 3:
										GL11.glRotatef(tgt[28] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 1, 0);
										GL11.glRotatef(tgt[29] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 0, 1);
										GL11.glRotatef(tgt[30] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 1, 0, 0);
										break;
									case 4:
										GL11.glRotatef(tgt[28] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 0, 1);
										GL11.glRotatef(tgt[29] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 1, 0, 0);
										GL11.glRotatef(tgt[30] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 1, 0);
										break;
									case 5:
										GL11.glRotatef(tgt[28] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 0, 1);
										GL11.glRotatef(tgt[29] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 0, 1, 0);
										GL11.glRotatef(tgt[30] * (1 - (tgt[0] - reloadti) / (tgt[0] - pre[0])), 1, 0, 0);
										break;

								}
//							GL11.glTranslatef(tgt[17]-pre[17]/(tgt[0]- pre[0])*(tgt[0]-reloadti), tgt[18]-pre[18]/(tgt[0]- pre[0])*(tgt[0]-reloadti), tgt[19]-pre[19]/(tgt[0]- pre[0])*(tgt[0]-reloadti));
//							GL11.glTranslatef(tgt[20]-pre[20]/(tgt[0]- pre[0])*(tgt[0]-reloadti), tgt[21]-pre[21]/(tgt[0]- pre[0])*(tgt[0]-reloadti), tgt[22]-pre[22]/(tgt[0]- pre[0])*(tgt[0]-reloadti));
//							GL11.glRotatef(tgt[23]-pre[23]/(tgt[0]- pre[0])*(tgt[0]-reloadti),1,0,0);
//							GL11.glRotatef(tgt[24]-pre[24]/(tgt[0]- pre[0])*(tgt[0]-reloadti),0,1,0);
//							GL11.glRotatef(tgt[25]-pre[25]/(tgt[0]- pre[0])*(tgt[0]-reloadti),0,0,1);

								ItemStack magstack = items[5];
								if(magstack != null) {
									IItemRenderer magrender = MinecraftForgeClient.getItemRenderer(magstack, type);
									if (magrender instanceof HMGRenderItemCustom && magrender.handleRenderType(magstack, type)) {
										((HMGRenderItemCustom)magrender).renderaspart();
										Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
									}else renderpartofmodel("mat3");
								}else renderpartofmodel("mat3");
							}
							GL11.glPopMatrix();
							GL11.glPushMatrix();
							if (isentitysprinting(entityplayer) || (gun.gunInfo.needfix && !nbt.getBoolean("HMGfixed")) ) { //TODO IF NOT ADS ALSO TODO: CHECK ALL SPRINTING CHECKS FOR SCOPED WEAPONS
								glMatrixForRenderInEquipped(0);
								GL11.glRotatef(Sprintrotationx, 1.0F, 0.0F, 0.0F);
								GL11.glRotatef(Sprintrotationy, 0.0F, 1.0F, 0.0F);
								GL11.glRotatef(Sprintrotationz, 0.0F, 0.0F, 1.0F);
								GL11.glTranslatef(Sprintoffsetx, Sprintoffsety, Sprintoffsetz);
							} else {
								reco = -0.2F;
								glMatrixForRenderInEquipped(reco);
							}
							ResourceLocation resourcelocation = this.getEntityTexture((AbstractClientPlayer) entityplayer);
							if (resourcelocation == null) {
								resourcelocation = AbstractClientPlayer.getLocationSkin("default");
							}
							Minecraft.getMinecraft().renderEngine.bindTexture(resourcelocation);
							GL11.glRotatef(180F, 1.0F, 0.0F, 0.0F);
							GL11.glTranslatef(0F, -0.5F, 0F);
							if (this.armtrue) {
								//��]���Ԃ���Ȃ������Ȃ���c�܂��ꉞ�c
								GL11.glPushMatrix();//glatrt3
								modelBipedMain.bipedRightArm.render(0.0625f);//0.0625
								modelBipedMain.bipedLeftArm.render(0.0625f);
								GL11.glPopMatrix();//glend3
								modelBipedMain.bipedLeftArm .rotateAngleX = this.armrotationxl + tgt[12] + (pre[12] - tgt[12]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti);//-0.8
								modelBipedMain.bipedLeftArm .rotateAngleY = this.armrotationyl + tgt[13] + (pre[13] - tgt[13]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti);//0
								modelBipedMain.bipedLeftArm .rotateAngleZ = this.armrotationzl + tgt[14] + (pre[14] - tgt[14]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti);//90
								modelBipedMain.bipedLeftArm .offsetX = this.armoffsetxl*armoffsetscale  + (tgt[9] + (pre[9] - tgt[9]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti))*armoffsetscale;//0.1
								modelBipedMain.bipedLeftArm .offsetY = this.armoffsetyl*armoffsetscale  + (tgt[10] + (pre[10] - tgt[10]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti))*armoffsetscale;//0.3
								modelBipedMain.bipedLeftArm .offsetZ = this.armoffsetzl*armoffsetscale  + (tgt[11] + (pre[11] - tgt[11]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti))*armoffsetscale;//-1


								modelBipedMain.bipedRightArm.rotateAngleX = this.armrotationxr + tgt[18] + (pre[18] - tgt[18]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti);//-1.57
								modelBipedMain.bipedRightArm.rotateAngleY = this.armrotationyr + tgt[19] + (pre[19] - tgt[19]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti);//0
								modelBipedMain.bipedRightArm.rotateAngleZ = this.armrotationzr + tgt[20] + (pre[20] - tgt[20]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti);//0
								modelBipedMain.bipedRightArm.offsetX = this.armoffsetxr*armoffsetscale  +(tgt[15] + (pre[15] - tgt[15]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti))*armoffsetscale;//0.5
								modelBipedMain.bipedRightArm.offsetY = this.armoffsetyr*armoffsetscale  +(tgt[16] + (pre[16] - tgt[16]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti))*armoffsetscale;//0.5
								modelBipedMain.bipedRightArm.offsetZ = this.armoffsetzr*armoffsetscale  +(tgt[17] + (pre[17] - tgt[17]) / (tgt[0] - pre[0]) * (tgt[0] - reloadti))*armoffsetscale;//0.5

							}
						} else {
							renderpartofmodel("mat1");
							renderpartofmodel("mats" + String.valueOf(mode));
							if (remat31) {
								renderpartofmodel("mat31");
							}
							renderpartofmodel("mat32");
							renderpartofmodel("mat24");
							GL11.glTranslatef(0.0F, 0.0F, -0.4F);
							renderpartofmodel("mat2");
							GL11.glTranslatef(0.0F, 0.0F, 0.4F);
							{
								GL11.glPushMatrix();//glstart11
								GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
								GL11.glRotatef(mat25rotationx, 1.0F, 0.0F, 0.0F);
								GL11.glRotatef(mat25rotationy, 0.0F, 1.0F, 0.0F);
								GL11.glRotatef(mat25rotationz, 0.0F, 0.0F, 1.0F);
								GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);
								GL11.glTranslatef(0F, 0F, -(gun.gunInfo.cocktime / 2) * 0.1F);
								renderpartofmodel("mat25");
								GL11.glPopMatrix();//glend11
							}
							if (mat22) {
								GL11.glTranslatef(mat22offsetx, mat22offsety, mat22offsetz);
								GL11.glRotatef(mat22rotationx, 1.0F, 0.0F, 0.0F);
								GL11.glRotatef(mat22rotationy, 0.0F, 1.0F, 0.0F);
								GL11.glRotatef(mat22rotationz, 0.0F, 0.0F, 1.0F);
								GL11.glTranslatef(-mat22offsetx, -mat22offsety, -mat22offsetz);
								renderpartofmodel("mat22");
								GL11.glTranslatef(mat22offsetx, mat22offsety, mat22offsetz);
								GL11.glRotatef(-mat22rotationx, 1.0F, 0.0F, 0.0F);
								GL11.glRotatef(-mat22rotationy, 0.0F, 1.0F, 0.0F);
								GL11.glRotatef(-mat22rotationz, 0.0F, 0.0F, 1.0F);
								GL11.glTranslatef(-mat22offsetx, -mat22offsety, -mat22offsetz);
							} else {
								renderpartofmodel("mat22");
							}
							GL11.glPushMatrix();
							GL11.glRotatef(-minecraft.thePlayer.rotationPitch,1,0,0);
							renderpartofmodel("matbase");
							GL11.glPopMatrix();
							this.model(item,EQUIPPED_FIRST_PERSON,data);

							this.modelSight(item,EQUIPPED,data);
						}
					} else {
						renderpartofmodel("mat1");
						renderpartofmodel("mats" + String.valueOf(mode));
						GL11.glPushMatrix();
						GL11.glRotatef(-minecraft.thePlayer.rotationPitch,1,0,0);
						renderpartofmodel("matbase");
						GL11.glPopMatrix();

						ItemStack magstack = items[5];
						if(gun.remain_Bullet(item)!= 0 || remat3) {
							if (magstack != null) {
								IItemRenderer magrender = MinecraftForgeClient.getItemRenderer(magstack, type);
								if (magrender instanceof HMGRenderItemCustom && magrender.handleRenderType(magstack, type)) {
									((HMGRenderItemCustom) magrender).renderaspart();
									Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
								} else renderpartofmodel("mat3");
							} else renderpartofmodel("mat3");
						}
						renderpartofmodel("mat22");
						if (nodrawmat35) {
							renderpartofmodel("mat35");
						}
						if (cockingtime <= 0) {
							renderpartofmodel("mat25");
						} else {
							GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
							GL11.glRotatef(mat25rotationx, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(mat25rotationy, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(mat25rotationz, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);
							if (cockingtime > 0 && cockingtime < ((gun.gunInfo.cocktime + smoothing - 1) / 2)) {
								GL11.glTranslatef(0F, 0F, -cockingtime * 0.1F);
							} else {
								GL11.glTranslatef(0F, 0F, (cockingtime - (gun.gunInfo.cocktime + smoothing - 1)) * 0.1F);
							}
							renderpartofmodel("mat25");
							if (cockingtime > 0 && cockingtime < ((gun.gunInfo.cocktime + smoothing - 1) / 2)) {
								GL11.glTranslatef(0F, 0F, cockingtime * 0.1F);
							} else {
								GL11.glTranslatef(0F, 0F, -(cockingtime - (gun.gunInfo.cocktime + smoothing - 1)) * 0.1F);
							}
							GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
							GL11.glRotatef(-mat25rotationx, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(-mat25rotationy, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(-mat25rotationz, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);
						}
						this.model(item,EQUIPPED_FIRST_PERSON,data);

						this.modelSight(item,EQUIPPED,data);
						if (!recoiled) {


//							System.out.println("" + boltoffsetcof);
							GL11.glTranslatef(0.0F, 0.0F, -mat2offsetz * boltoffsetcof * (1 - smoothing));
							renderpartofmodel("mat2");
							GL11.glTranslatef(0.0F, 0.0F, mat2offsetz * boltoffsetcof * (1 - smoothing));
							this.mat31mat32(true);
						} else {
							renderpartofmodel("mat2");
							renderpartofmodel("mat32");
							GL11.glTranslatef(mat31posx, mat31posy, mat31posz);//0,0.7,0
							GL11.glRotatef(rotey, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(rotex, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(rotez, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat31posx, -mat31posy, -mat31posz);
							renderpartofmodel("mat31");
							GL11.glTranslatef(mat31posx, mat31posy, mat31posz);//0,0.7,0
							GL11.glRotatef(-rotey, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(-rotex, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(-rotez, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat31posx, -mat31posy, -mat31posz);
						}
					}
					GL11.glPopMatrix();
					GL11.glPushMatrix();
					if (HandmadeGunsCore.Key_ADS(entityplayer)) { //reloading
						if (nbt.getBoolean("IsReloading")) {
							if (!reloadanim)
								glMatrixForRenderInEquipped_reload();
							else {
								glMatrixForRenderInEquipped(-0.2F);
							}
						} else {
							//if (gun.recoilre)
							//if (gun.recoilreBolts(item))
							if (!recoiled) {
								glMatrixForRenderInEquippedADS(-1.4F - 0.03F * (1 - smoothing));
							} else {
								glMatrixForRenderInEquippedADS(-1.4F);
							}
						}

					} else {
						if (isentitysprinting(entityplayer) || (gun.gunInfo.needfix && !nbt.getBoolean("HMGfixed")) ) {
							glMatrixForRenderInEquipped(0);
							GL11.glRotatef(Sprintrotationx, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(Sprintrotationy, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(Sprintrotationz, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(Sprintoffsetx, Sprintoffsety, Sprintoffsetz);
						} else if (nbt.getBoolean("IsReloading")) {
							if (!reloadanim)
								glMatrixForRenderInEquipped_reload();
							else {
								glMatrixForRenderInEquipped(-0.2F);
							}
						} else {
							//if (gun.recoilre)
							//if (gun.recoilreBolts(item))
							if (!recoiled) {
								glMatrixForRenderInEquipped(-0.2F - 0.05f * (1 - smoothing));
							} else {
								glMatrixForRenderInEquipped(-0.2F);
							}
						}

					}
					if (!recoiled) {
						GL11.glRotatef(jump * (1 - smoothing), 1.0F, 0.0F, 0.0F);
					}
					ResourceLocation resourcelocation = this.getEntityTexture((AbstractClientPlayer) entityplayer);
					if (resourcelocation == null) {
						resourcelocation = AbstractClientPlayer.getLocationSkin("default");
					}
					Minecraft.getMinecraft().renderEngine.bindTexture(resourcelocation);
					GL11.glRotatef(180F, 1.0F, 0.0F, 0.0F);
					GL11.glTranslatef(0F, -0.5F, 0F);
					if (all_jump) {
						if (cockingtime > 0){

							if (cockingtime > 0 && cockingtime < ((gun.gunInfo.cocktime + smoothing - 1) / 2)) {
								//GL11.glTranslatef(0F, 0F, -cockingtime*0.1F);
								GL11.glRotatef(-cockingtime * 1F, 1.0F, 0.0F, 0.0F);
							} else {
								//GL11.glTranslatef(0F, 0F, (cockingtime-(gun.cocktime + smoothing -1))*0.1F);
								GL11.glRotatef((cockingtime - (gun.gunInfo.cocktime + smoothing - 1)) * 1F, 1.0F, 0.0F, 0.0F);
							}
						}
					}
					if (this.armtrue) {
						if (nbt.getBoolean("IsReloading")){
							if (!reloadanim) {
								GL11.glPushMatrix();//glatrt3
								modelBipedMain.bipedRightArm.render(0.0625f);//0.0625
								modelBipedMain.bipedLeftArm.render(0.0625f);
								GL11.glPopMatrix();//glend3
								modelBipedMain.bipedLeftArm.rotateAngleX = this.armrotationxl;//-0.8
								modelBipedMain.bipedLeftArm.rotateAngleY = this.armrotationyl;//0
								modelBipedMain.bipedLeftArm.rotateAngleZ = this.armrotationzl;//90
								modelBipedMain.bipedLeftArm.offsetX = this.armoffsetxl*armoffsetscale;//0.1
								modelBipedMain.bipedLeftArm.offsetY = this.armoffsetyl*armoffsetscale;//0.3
								modelBipedMain.bipedLeftArm.offsetZ = this.armoffsetzl*armoffsetscale;//-1


								modelBipedMain.bipedRightArm.rotateAngleX = this.armrotationxr;//-1.57
								modelBipedMain.bipedRightArm.rotateAngleY = this.armrotationyr;//0
								modelBipedMain.bipedRightArm.rotateAngleZ = this.armrotationzr;//0
								modelBipedMain.bipedRightArm.offsetX = this.armoffsetxr*armoffsetscale;//0.5
								modelBipedMain.bipedRightArm.offsetY = this.armoffsetyr*armoffsetscale;//0.5
								modelBipedMain.bipedRightArm.offsetZ = this.armoffsetzr*armoffsetscale;//0.5

								if (cock_left) {
									if (cockingtime > 0){
										if (cockingtime > 0 && cockingtime < ((gun.gunInfo.cocktime + smoothing - 1) / 2)) {
											//GL11.glTranslatef(0F, 0F, -cockingtime*0.1F);
											modelBipedMain.bipedLeftArm.offsetZ = this.armoffsetzl*armoffsetscale - (-cockingtime * 0.1F) * scala;
										} else {
											//GL11.glTranslatef(0F, 0F, (cockingtime-(gun.cocktime + smoothing -1))*0.1F);
											modelBipedMain.bipedLeftArm.offsetZ = this.armoffsetzl*armoffsetscale - (cockingtime - (gun.gunInfo.cocktime + smoothing - 1)) * 0.1F * scala;
										}
									}
								}
							}
						} else {
							GL11.glPushMatrix();//glatrt3
							modelBipedMain.bipedRightArm.render(0.0625f);//0.0625
							modelBipedMain.bipedLeftArm.render(0.0625f);
							GL11.glPopMatrix();//glend3
							modelBipedMain.bipedLeftArm.rotateAngleX = this.armrotationxl;//-0.8
							modelBipedMain.bipedLeftArm.rotateAngleY = this.armrotationyl;//0
							modelBipedMain.bipedLeftArm.rotateAngleZ = this.armrotationzl;//90
							modelBipedMain.bipedLeftArm.offsetX = this.armoffsetxl*armoffsetscale;//0.1
							modelBipedMain.bipedLeftArm.offsetY = this.armoffsetyl*armoffsetscale;//0.3
							modelBipedMain.bipedLeftArm.offsetZ = this.armoffsetzl*armoffsetscale;//-1


							modelBipedMain.bipedRightArm.rotateAngleX = this.armrotationxr;//-1.57
							modelBipedMain.bipedRightArm.rotateAngleY = this.armrotationyr;//0
							modelBipedMain.bipedRightArm.rotateAngleZ = this.armrotationzr;//0
							modelBipedMain.bipedRightArm.offsetX = this.armoffsetxr*armoffsetscale;//0.5
							modelBipedMain.bipedRightArm.offsetY = this.armoffsetyr*armoffsetscale;//0.5
							modelBipedMain.bipedRightArm.offsetZ = this.armoffsetzr*armoffsetscale;//0.5

							if (cock_left) {
								if (cockingtime > 0){
									if (cockingtime > 0 && cockingtime < ((gun.gunInfo.cocktime + smoothing - 1) / 2)) {
										//GL11.glTranslatef(0F, 0F, -cockingtime*0.1F);
										modelBipedMain.bipedLeftArm.offsetZ = this.armoffsetzl*armoffsetscale - (-cockingtime * 0.1F) * scala;
									} else {
										//GL11.glTranslatef(0F, 0F, (cockingtime-(gun.cocktime + smoothing -1))*0.1F);
										modelBipedMain.bipedLeftArm.offsetZ = this.armoffsetzl*armoffsetscale - (cockingtime - (gun.gunInfo.cocktime + smoothing - 1)) * 0.1F * scala;
									}
								}
							}
						}
					}
					GL11.glPopMatrix();//glend2
					break;
				}
				case EQUIPPED: {//thrid
					int cockingtime = nbt.getInteger("CockingTime");
					boolean recoiled = nbt.getBoolean("Recoiled");
					int mode = nbt.getInteger("HMGMode");
					byte boltprogress = nbt.getByte("Bolt");
					boltprogress -= smoothing;
					int cycle = (int)gun.gunInfo.cycle;
					float boltoffsetcof;
					if (boltprogress < cycle / 2)//�{���g��ޒ�
						boltoffsetcof = cycle - boltprogress;
					else
						boltoffsetcof = cycle - (cycle - boltprogress);//�{���g�O�i��
					if (boltoffsetcof < 0) boltoffsetcof = 0;
					GL11.glPushMatrix();//glstart1
					GL11.glScalef(1f/2f, 1f/2f, 1f/2f);
					Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
					if (data[1] instanceof EntityPlayer) {
						glMatrixForRenderInEntityPlayer(-0.75f);
						GL11.glScalef(scala, scala, scala);
					} else {
						glMatrixForRenderInEntity(0);
						GL11.glScalef(-scala * 0.6f, scala * 0.6f, scala * 0.6f);
					}
					GL11.glScalef(gun.gunInfo.inworldScale, gun.gunInfo.inworldScale, gun.gunInfo.inworldScale);
					if (nbt.getBoolean("IsReloading")) {
						// modeling.renderAll();
						renderpartofmodel("mat1");
						renderpartofmodel("mats" + String.valueOf(mode));
						renderpartofmodel("mat24");
						GL11.glTranslatef(0.0F, 0.0F, -0.4F);
						renderpartofmodel("mat2");
						GL11.glTranslatef(0.0F, 0.0F, 0.4F);
						if (remat31) {
							renderpartofmodel("mat31");
						}
						renderpartofmodel("mat32");
						{
							GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
							GL11.glRotatef(mat25rotationx, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(mat25rotationy, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(mat25rotationz, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);
							GL11.glTranslatef(0F, 0F, -((gun.gunInfo.cocktime + smoothing - 1) / 2) * 0.1F);
							renderpartofmodel("mat25");
							GL11.glTranslatef(0F, 0F, ((gun.gunInfo.cocktime + smoothing - 1) / 2) * 0.1F);
							GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
							GL11.glRotatef(-mat25rotationx, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(-mat25rotationy, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(-mat25rotationz, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);
						}
						if (mat22) {
							GL11.glTranslatef(mat22offsetx, mat22offsety, mat22offsetz);
							GL11.glRotatef(mat22rotationx, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(mat22rotationy, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(mat22rotationz, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat22offsetx, -mat22offsety, -mat22offsetz);
							renderpartofmodel("mat22");
							GL11.glTranslatef(mat22offsetx, mat22offsety, mat22offsetz);
							GL11.glRotatef(-mat22rotationx, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(-mat22rotationy, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(-mat22rotationz, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat22offsetx, -mat22offsety, -mat22offsetz);
						} else {
							renderpartofmodel("mat22");
						}
						this.model(item, EQUIPPED, data);

						this.modelSight(item, EQUIPPED, data);
					} else {
						renderpartofmodel("mat1");
						renderpartofmodel("mats" + String.valueOf(mode));
						GL11.glPushMatrix();
						GL11.glRotatef(-minecraft.thePlayer.rotationPitch,1,0,0);
						renderpartofmodel("matbase");
						GL11.glPopMatrix();
						if (nodrawmat35) {
							renderpartofmodel("mat35");
						}

						ItemStack magstack = items[5];
						if (magstack != null) {
							IItemRenderer magrender = MinecraftForgeClient.getItemRenderer(magstack, type);
							if (magrender instanceof HMGRenderItemCustom && magrender.handleRenderType(magstack, type)) {
								((HMGRenderItemCustom) magrender).renderaspart();
								Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
							} else renderpartofmodel("mat3");
						} else renderpartofmodel("mat3");
						renderpartofmodel("mat22");
						if (cockingtime <= 0) {
							renderpartofmodel("mat25");
						} else {
							GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
							GL11.glRotatef(mat25rotationx, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(mat25rotationy, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(mat25rotationz, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);
							if (cockingtime > 0 && cockingtime < ((gun.gunInfo.cocktime + smoothing - 1) / 2)) {
								GL11.glTranslatef(0F, 0F, -cockingtime * 0.1F);
							} else {
								GL11.glTranslatef(0F, 0F, (cockingtime - (gun.gunInfo.cocktime + smoothing - 1)) * 0.1F);
							}
							renderpartofmodel("mat25");
							if (cockingtime > 0 && cockingtime < ((gun.gunInfo.cocktime + smoothing - 1) / 2)) {
								GL11.glTranslatef(0F, 0F, cockingtime * 0.1F);
							} else {
								GL11.glTranslatef(0F, 0F, -(cockingtime - (gun.gunInfo.cocktime + smoothing - 1)) * 0.1F);
							}
							GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
							GL11.glRotatef(-mat25rotationx, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(-mat25rotationy, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(-mat25rotationz, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);

						}
						this.model(item, EQUIPPED, data);

						this.modelSight(item, EQUIPPED, data);
						if (!recoiled) {
							GL11.glTranslatef(0.0F, 0.0F, -mat2offsetz * boltoffsetcof * (1 - smoothing));
							renderpartofmodel("mat2");
							GL11.glTranslatef(0.0F, 0.0F, mat2offsetz * boltoffsetcof * (1 - smoothing));
							this.mat31mat32(true);
						} else {
							renderpartofmodel("mat2");
							renderpartofmodel("mat32");
							GL11.glTranslatef(mat31posx, mat31posy, mat31posz);//0,0.7,0
							GL11.glRotatef(rotey, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(rotex, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(rotez, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat31posx, -mat31posy, -mat31posz);
							renderpartofmodel("mat31");
							GL11.glTranslatef(mat31posx, mat31posy, mat31posz);//0,0.7,0
							GL11.glRotatef(-rotey, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(-rotex, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(-rotez, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat31posx, -mat31posy, -mat31posz);
						}
					}
					GL11.glPopMatrix();//glend1
					break;
				}
				case ENTITY: {

					int pass = MinecraftForgeClient.getRenderPass();
					if(pass == 1) {
						glEnable(GL_BLEND);
						glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
						GL11.glDepthMask(false);
						glAlphaFunc(GL_LESS, 1);
					}else {
						GL11.glDepthMask(true);
						glAlphaFunc(GL_EQUAL, 1);
					}
					GL11.glEnable(GL12.GL_RESCALE_NORMAL);
					HMGDroppedGunRenderHelper.applyGroundTransform(data);
					int cockingtime = nbt.getInteger("CockingTime");
					boolean recoiled = nbt.getBoolean("Recoiled");
					int mode = nbt.getInteger("HMGMode");
					byte boltprogress = nbt.getByte("Bolt");
					boltprogress -= smoothing;
					int cycle = (int)gun.gunInfo.cycle;
					float boltoffsetcof;
					if (boltprogress < cycle / 2)//�{���g��ޒ�
						boltoffsetcof = cycle - boltprogress;
					else
						boltoffsetcof = cycle - (cycle - boltprogress);//�{���g�O�i��
					if (boltoffsetcof < 0) boltoffsetcof = 0;
					GL11.glPushMatrix();//glstart1
					Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
					GL11.glScalef(0.4f * scala * gun.gunInfo.inworldScale,0.4f * scala * gun.gunInfo.inworldScale,0.4f * scala * gun.gunInfo.inworldScale);
					if (nbt.getBoolean("IsReloading")) {
						// modeling.renderAll();
						renderpartofmodel("mat1");
						renderpartofmodel("mats" + String.valueOf(mode));
						GL11.glPushMatrix();
						GL11.glRotatef(-minecraft.thePlayer.rotationPitch,1,0,0);
						renderpartofmodel("matbase");
						GL11.glPopMatrix();
						renderpartofmodel("mat24");
						GL11.glTranslatef(0.0F, 0.0F, -0.4F);
						renderpartofmodel("mat2");
						GL11.glTranslatef(0.0F, 0.0F, 0.4F);
						if (remat31) {
							renderpartofmodel("mat31");
						}
						renderpartofmodel("mat32");
						{
							GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
							GL11.glRotatef(mat25rotationx, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(mat25rotationy, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(mat25rotationz, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);
							GL11.glTranslatef(0F, 0F, -((gun.gunInfo.cocktime + smoothing - 1) / 2) * 0.1F);
							renderpartofmodel("mat25");
							GL11.glTranslatef(0F, 0F, ((gun.gunInfo.cocktime + smoothing - 1) / 2) * 0.1F);
							GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
							GL11.glRotatef(-mat25rotationx, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(-mat25rotationy, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(-mat25rotationz, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);
						}
						if (mat22) {
							GL11.glTranslatef(mat22offsetx, mat22offsety, mat22offsetz);
							GL11.glRotatef(mat22rotationx, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(mat22rotationy, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(mat22rotationz, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat22offsetx, -mat22offsety, -mat22offsetz);
							renderpartofmodel("mat22");
							GL11.glTranslatef(mat22offsetx, mat22offsety, mat22offsetz);
							GL11.glRotatef(-mat22rotationx, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(-mat22rotationy, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(-mat22rotationz, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat22offsetx, -mat22offsety, -mat22offsetz);
						} else {
							renderpartofmodel("mat22");
						}
						this.model(item, ENTITY, data);

						this.modelSight(item, ENTITY, data);
					} else {
						renderpartofmodel("mat1");
						renderpartofmodel("mats" + mode);
						GL11.glPushMatrix();
						GL11.glRotatef(-minecraft.thePlayer.rotationPitch,1,0,0);
						renderpartofmodel("matbase");
						GL11.glPopMatrix();
						if (nodrawmat35) {
							renderpartofmodel("mat35");
						}

						ItemStack magstack = items[5];
						if (magstack != null) {
							IItemRenderer magrender = MinecraftForgeClient.getItemRenderer(magstack, type);
							if (magrender instanceof HMGRenderItemCustom && magrender.handleRenderType(magstack, type)) {
								((HMGRenderItemCustom) magrender).renderaspart();
								Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
							} else renderpartofmodel("mat3");
						} else renderpartofmodel("mat3");
						renderpartofmodel("mat22");
						if (cockingtime <= 0) {
							renderpartofmodel("mat25");
						} else {
							GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
							GL11.glRotatef(mat25rotationx, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(mat25rotationy, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(mat25rotationz, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);
							if (cockingtime > 0 && cockingtime < ((gun.gunInfo.cocktime + smoothing - 1) / 2)) {
								GL11.glTranslatef(0F, 0F, -cockingtime * 0.1F);
							} else {
								GL11.glTranslatef(0F, 0F, (cockingtime - (gun.gunInfo.cocktime + smoothing - 1)) * 0.1F);
							}
							renderpartofmodel("mat25");
							if (cockingtime > 0 && cockingtime < ((gun.gunInfo.cocktime + smoothing - 1) / 2)) {
								GL11.glTranslatef(0F, 0F, cockingtime * 0.1F);
							} else {
								GL11.glTranslatef(0F, 0F, -(cockingtime - (gun.gunInfo.cocktime + smoothing - 1)) * 0.1F);
							}
							GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
							GL11.glRotatef(-mat25rotationx, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(-mat25rotationy, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(-mat25rotationz, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);

						}
						this.model(item, ENTITY, data);

						this.modelSight(item, ENTITY, data);
						if (!recoiled) {
							GL11.glTranslatef(0.0F, 0.0F, -mat2offsetz * boltoffsetcof * (1 - smoothing));
							renderpartofmodel("mat2");
							GL11.glTranslatef(0.0F, 0.0F, mat2offsetz * boltoffsetcof * (1 - smoothing));
							this.mat31mat32(true);
						} else {
							renderpartofmodel("mat2");
							renderpartofmodel("mat32");
							GL11.glTranslatef(mat31posx, mat31posy, mat31posz);//0,0.7,0
							GL11.glRotatef(rotey, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(rotex, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(rotez, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat31posx, -mat31posy, -mat31posz);
							renderpartofmodel("mat31");
							GL11.glTranslatef(mat31posx, mat31posy, mat31posz);//0,0.7,0
							GL11.glRotatef(-rotey, 0.0F, 1.0F, 0.0F);
							GL11.glRotatef(-rotex, 1.0F, 0.0F, 0.0F);
							GL11.glRotatef(-rotez, 0.0F, 0.0F, 1.0F);
							GL11.glTranslatef(-mat31posx, -mat31posy, -mat31posz);
						}
					}
					GL11.glPopMatrix();//glend1
					break;
				}
				case FIRST_PERSON_MAP:
					break;
			}
		}else{
			nbt = item.getTagCompound();
			if (nbt == null) gun.checkTags(item);
			nbt = item.getTagCompound();
			Invocable invocable = (Invocable) gun.gunInfo.renderscript;
			
			Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
			switch (type) {
				case INVENTORY:
					glMatrixForRenderInInventory();
					break;
				case EQUIPPED_FIRST_PERSON://first
					try {
						invocable.invokeFunction("renderFirst",this,gun,item,modeling,data[1],type,data);
					} catch (ScriptException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					}
					break;
				case EQUIPPED://thrid
					try {
						invocable.invokeFunction("renderThird",this,gun,item,modeling,data[1],type,data);
					} catch (ScriptException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					}
					break;
				case FIRST_PERSON_MAP:
					break;
				case ENTITY:
					try {
						invocable.invokeFunction("renderThird",this,gun,item,modeling,data[1],type,data);
					} catch (ScriptException e) {
						e.printStackTrace();
					} catch (NoSuchMethodException e) {
						e.printStackTrace();
					}
					break;
			}
			GL11.glDepthMask(true);
			GL11.glDisable(GL_BLEND);
			return;
		}

		// If the gun explicitly requests the model to be used as the inventory icon,
		// and we actually have a loaded model (setSomeParam was called), render it here.
		if (type == ItemRenderType.INVENTORY) {
			try {
				if (gun != null && gun.gunInfo != null && gun.gunInfo.useModelAsIcon && this.modeling != null) {
					GL11.glPushMatrix();

					// avoid leftover GUI color tinting
					GL11.glDisable(GL11.GL_COLOR_MATERIAL);
					GL11.glColor4f(1f, 1f, 1f, 1f);

					//flip
					//GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
					//GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
					GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);

					// move into the inventory-slot space (tweak the translate/scale to taste)
					// common slot center offsets (Minecraft tends to place item origin at top-left of slot)
					// these values are a safe starting point; you can tweak translate/scale for perfect fit
					GL11.glTranslatef(-9.5F, -15.5F, -5.5F);

					// flip/scale to match your usual model orientation in GUI
					float iconScale = (gun.gunInfo.inventoryscale * 1 * modelscala) * gun.gunInfo.inworldScale;
					//gun.ModelScala / gun.InworldScale
					GL11.glScalef(-iconScale, iconScale, iconScale);

					// use the same inventory matrix you already had (keeps orientation familiar)
					try {
						glMatrixForRenderInInventory();
					} catch (Throwable ignored) {}

					// make sure lighting and normals are set up and color is white
					RenderHelper.enableStandardItemLighting();
					GL11.glDisable(GL11.GL_COLOR_MATERIAL);
					GL11.glColor4f(1f, 1f, 1f, 1f);

					// bind the model texture if available
					try {
						if (this.guntexture != null) {
							Minecraft.getMinecraft().getTextureManager().bindTexture(this.guntexture);
						}
					} catch (Throwable ignored) {}

					// render the model
					GL11.glEnable(GL12.GL_RESCALE_NORMAL);
					try {
						this.modeling.renderAll();
						if (gunSkinTexture != null) {
							GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_POLYGON_BIT);
							Minecraft.getMinecraft().renderEngine.bindTexture(gunSkinTexture);
							GL11.glEnable(GL11.GL_BLEND);
							GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
							GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);
							GL11.glDepthMask(false);
							GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
							GL11.glPolygonOffset(-1.0F, -1.0F);
							GL11.glColor4f(1, 1, 1, 1);
							this.modeling.renderAll();
							GL11.glPopAttrib();
							Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
						}
					} catch (Throwable t) {
						// fallback: do nothing - don't crash the GUI
						System.out.println("[HMG] model-as-icon render failed: " + t.getMessage());
					}
					GL11.glDisable(GL12.GL_RESCALE_NORMAL);

					RenderHelper.disableStandardItemLighting();

					GL11.glPopMatrix();
					// done — we handled the inventory draw
					return;
				} // else: fall through and let vanilla draw the 2D texture (handleRenderType returned false previously)
			} catch (Throwable t) {
				// if anything goes wrong we don't want to break the GUI
				System.out.println("[HMG] Inventory 3D icon render exception: " + t.getMessage());
				t.printStackTrace();
				// fallthrough to let vanilla draw the 2D icon
			}
		}


		GL11.glDepthMask(true);
		GL11.glDisable(GL_BLEND);
		GL11.glShadeModel(GL11.GL_SMOOTH);
	}
	public void renderatunder(ItemRenderType type, ItemStack item, Object... data){
		float scala = this.modelscala;
		HMGItem_Unified_Guns gun;
		if (item.getItem() instanceof HMGItem_Unified_Guns)
			gun = (HMGItem_Unified_Guns) item.getItem();
		else {
			return;
		}
		nbt = item.getTagCompound();
		if (nbt == null) gun.checkTags(item);
		nbt = item.getTagCompound();
		
		GL11.glPushMatrix();
		{int cockingtime = nbt.getInteger("CockingTime");
			boolean recoiled = nbt.getBoolean("Recoiled");
			int mode = nbt.getInteger("HMGMode");
			GL11.glScalef(scala, scala, scala);
			Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
			if (nbt.getBoolean("IsReloading")){
				{
					renderpartofmodel("mat1");
					renderpartofmodel("mats" + mode);
					if (remat31) {
						renderpartofmodel("mat31");
					}
					renderpartofmodel("mat32");
					renderpartofmodel("mat24");
					GL11.glTranslatef(0.0F, 0.0F, -0.4F);
					renderpartofmodel("mat2");
					GL11.glTranslatef(0.0F, 0.0F, 0.4F);
					GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
					GL11.glRotatef(mat25rotationx, 1.0F, 0.0F, 0.0F);
					GL11.glRotatef(mat25rotationy, 0.0F, 1.0F, 0.0F);
					GL11.glRotatef(mat25rotationz, 0.0F, 0.0F, 1.0F);
					GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);
					GL11.glTranslatef(0F, 0F, -gun.gunInfo.cocktime*0.1f);
					renderpartofmodel("mat25");
					GL11.glTranslatef(0F, 0F, gun.gunInfo.cocktime*0.1f);
					GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
					GL11.glRotatef(-mat25rotationx, 1.0F, 0.0F, 0.0F);
					GL11.glRotatef(-mat25rotationy, 0.0F, 1.0F, 0.0F);
					GL11.glRotatef(-mat25rotationz, 0.0F, 0.0F, 1.0F);
					GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);
					if (mat22) {
						GL11.glTranslatef(mat22offsetx, mat22offsety, mat22offsetz);
						GL11.glRotatef(mat22rotationx, 1.0F, 0.0F, 0.0F);
						GL11.glRotatef(mat22rotationy, 0.0F, 1.0F, 0.0F);
						GL11.glRotatef(mat22rotationz, 0.0F, 0.0F, 1.0F);
						GL11.glTranslatef(-mat22offsetx, -mat22offsety, -mat22offsetz);
						renderpartofmodel("mat22");
						GL11.glTranslatef(mat22offsetx, mat22offsety, mat22offsetz);
						GL11.glRotatef(-mat22rotationx, 1.0F, 0.0F, 0.0F);
						GL11.glRotatef(-mat22rotationy, 0.0F, 1.0F, 0.0F);
						GL11.glRotatef(-mat22rotationz, 0.0F, 0.0F, 1.0F);
						GL11.glTranslatef(-mat22offsetx, -mat22offsety, -mat22offsetz);
					} else {
						renderpartofmodel("mat22");
					}
					this.model(item,EQUIPPED,data);

					this.modelSight(item,EQUIPPED,data);
				}
			} else {
				//if (gun.recoilre)
				//if (gun.recoilreBolts(item))
				if (!recoiled) {
					renderpartofmodel("mat1");
					renderpartofmodel("mats" + mode);
					if (nodrawmat35) {
						renderpartofmodel("mat35");
					}

					ItemStack magstack = items[5];
					if(magstack != null) {
						IItemRenderer magrender = MinecraftForgeClient.getItemRenderer(magstack, type);
						if (magrender instanceof HMGRenderItemCustom && magrender.handleRenderType(magstack, type)) {
							((HMGRenderItemCustom)magrender).renderaspart();
							Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
						}else renderpartofmodel("mat3");
					}else renderpartofmodel("mat3");
					renderpartofmodel("mat22");
					GL11.glTranslatef(0.0F, 0.0F, -0.4F*(1-smoothing));
					renderpartofmodel("mat2");
					GL11.glTranslatef(0.0F, 0.0F, 0.4F*(1-smoothing));

					if (cockingtime <= 0) {
						renderpartofmodel("mat25");
					} else {
						GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
						GL11.glRotatef(mat25rotationx, 1.0F, 0.0F, 0.0F);
						GL11.glRotatef(mat25rotationy, 0.0F, 1.0F, 0.0F);
						GL11.glRotatef(mat25rotationz, 0.0F, 0.0F, 1.0F);
						GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);
						if (cockingtime < ((gun.gunInfo.cocktime + smoothing -1) / 2)) {
							GL11.glTranslatef(0F, 0F, -cockingtime * 0.1F);
						} else {
							GL11.glTranslatef(0F, 0F, (cockingtime - (gun.gunInfo.cocktime + smoothing -1)) * 0.1F);
						}
						renderpartofmodel("mat25");
						if (cockingtime < ((gun.gunInfo.cocktime + smoothing -1) / 2)) {
							GL11.glTranslatef(0F, 0F, cockingtime * 0.1F);
						} else {
							GL11.glTranslatef(0F, 0F, -(cockingtime - (gun.gunInfo.cocktime + smoothing -1)) * 0.1F);
						}
						GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
						GL11.glRotatef(-mat25rotationx, 1.0F, 0.0F, 0.0F);
						GL11.glRotatef(-mat25rotationy, 0.0F, 1.0F, 0.0F);
						GL11.glRotatef(-mat25rotationz, 0.0F, 0.0F, 1.0F);
						GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);
					}

					this.mat31mat32(true);
					this.model(item,EQUIPPED,data);

					this.modelSight(item,EQUIPPED,data);

				} else {
					renderpartofmodel("mat1");
					renderpartofmodel("mats" + mode);
					if (nodrawmat35) {
						renderpartofmodel("mat35");
					}
					renderpartofmodel("mat2");

					ItemStack magstack = items[5];
					if(magstack != null) {
						IItemRenderer magrender = MinecraftForgeClient.getItemRenderer(magstack, type);
						if (magrender instanceof HMGRenderItemCustom) {
							((HMGRenderItemCustom)magrender).renderaspart();
							Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
						}else renderpartofmodel("mat3");
					}else renderpartofmodel("mat3");
					renderpartofmodel("mat32");
					renderpartofmodel("mat22");

					if (cockingtime <= 0) {
						renderpartofmodel("mat25");
					} else {
						GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
						GL11.glRotatef(mat25rotationx, 1.0F, 0.0F, 0.0F);
						GL11.glRotatef(mat25rotationy, 0.0F, 1.0F, 0.0F);
						GL11.glRotatef(mat25rotationz, 0.0F, 0.0F, 1.0F);
						GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);
						if (cockingtime < ((gun.gunInfo.cocktime + smoothing -1) / 2)) {
							GL11.glTranslatef(0F, 0F, -cockingtime * 0.1F);
						} else {
							GL11.glTranslatef(0F, 0F, (cockingtime - (gun.gunInfo.cocktime + smoothing -1)) * 0.1F);
						}
						renderpartofmodel("mat25");
						GL11.glTranslatef(mat25offsetx, mat25offsety, mat25offsetz);
						GL11.glRotatef(-mat25rotationx, 1.0F, 0.0F, 0.0F);
						GL11.glRotatef(-mat25rotationy, 0.0F, 1.0F, 0.0F);
						GL11.glRotatef(-mat25rotationz, 0.0F, 0.0F, 1.0F);
						GL11.glTranslatef(-mat25offsetx, -mat25offsety, -mat25offsetz);
						if (cockingtime < ((gun.gunInfo.cocktime + smoothing -1) / 2)) {
							GL11.glTranslatef(0F, 0F, cockingtime * 0.1F);
						} else {
							GL11.glTranslatef(0F, 0F, -(cockingtime - (gun.gunInfo.cocktime + smoothing -1)) * 0.1F);
						}
					}

					GL11.glTranslatef(mat31posx, mat31posy, mat31posz);//0,0.7,0
					GL11.glRotatef(rotey, 0.0F, 1.0F, 0.0F);
					GL11.glRotatef(rotex, 1.0F, 0.0F, 0.0F);
					GL11.glRotatef(rotez, 0.0F, 0.0F, 1.0F);
					GL11.glTranslatef(-mat31posx, -mat31posy, -mat31posz);
					renderpartofmodel("mat31");
					GL11.glTranslatef(mat31posx, mat31posy, mat31posz);//0,0.7,0
					GL11.glRotatef(-rotey, 0.0F, 1.0F, 0.0F);
					GL11.glRotatef(-rotex, 1.0F, 0.0F, 0.0F);
					GL11.glRotatef(-rotez, 0.0F, 0.0F, 1.0F);
					GL11.glTranslatef(-mat31posx, -mat31posy, -mat31posz);
					this.model(item,EQUIPPED,data);
					this.modelSight(item,EQUIPPED,data);
				}
			}
				
		}
		GL11.glPopMatrix();
	}

	public void model(ItemStack item,ItemRenderType type,Object... data){
		if(item.getItem() instanceof HMGItem_Unified_Guns) {
			HMGItem_Unified_Guns gun = (HMGItem_Unified_Guns) item.getItem();
			ItemStack itemstacka = null;
			itemstacka = items[2];
			if (itemstacka != null && itemstacka.getItem() instanceof HMGItemAttachment_laser) {
				IItemRenderer attachrender = MinecraftForgeClient.getItemRenderer(itemstacka, type);
				if (attachrender instanceof HMGRenderItemCustom) {
					GL11.glPushMatrix();
					GL11.glTranslatef(lightattachoffset[0],lightattachoffset[1],lightattachoffset[2]);
					GL11.glRotatef(lightattachrotation[0],0,1,0);
					GL11.glRotatef(lightattachrotation[1],1,0,0);
					GL11.glRotatef(lightattachrotation[2],0,0,1);
					((HMGRenderItemCustom)attachrender).renderaspart();
					Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
					GL11.glPopMatrix();
				} else
					renderpartofmodel("mat6");
			} else if (itemstacka != null && itemstacka.getItem() instanceof HMGItemAttachment_light) {
				IItemRenderer attachrender = MinecraftForgeClient.getItemRenderer(itemstacka, type);
				if (attachrender instanceof HMGRenderItemCustom) {
					GL11.glPushMatrix();
					GL11.glTranslatef(lightattachoffset[0],lightattachoffset[1],lightattachoffset[2]);
					GL11.glRotatef(lightattachrotation[0],0,1,0);
					GL11.glRotatef(lightattachrotation[1],1,0,0);
					GL11.glRotatef(lightattachrotation[2],0,0,1);
					((HMGRenderItemCustom)attachrender).renderaspart();
					Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
					GL11.glPopMatrix();
				} else
					renderpartofmodel("mat7");
			} else if (itemstacka != null && itemstacka.getItem() instanceof HMGItemSwordBase) {
				renderpartofmodel("mat14");
			}
			Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
			itemstacka = items[3];
			if (itemstacka != null && itemstacka.getItem() instanceof HMGItemAttachment_Suppressor) {
				IItemRenderer attachrender = MinecraftForgeClient.getItemRenderer(itemstacka, type);
				if (attachrender instanceof HMGRenderItemCustom) {
					GL11.glPushMatrix();
					GL11.glTranslatef(barrelattachoffset[0],barrelattachoffset[1],barrelattachoffset[2]);
					GL11.glRotatef(barrelattachrotation[0],0,1,0);
					GL11.glRotatef(barrelattachrotation[1],1,0,0);
					GL11.glRotatef(barrelattachrotation[2],0,0,1);
					((HMGRenderItemCustom)attachrender).renderaspart();
					Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
					GL11.glPopMatrix();
				} else
					renderpartofmodel("mat8");
			}
			Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
			itemstacka = items[4];
			if (itemstacka != null) {
				IItemRenderer attachrender = MinecraftForgeClient.getItemRenderer(itemstacka, type);
				if(gun.gunInfo.useundergunsmodel) {
					if (itemstacka != item && itemstacka.getItem() instanceof HMGItem_Unified_Guns) {
						renderpartofmodel("mat12");
						GL11.glTranslatef(gun.gunInfo.underoffsetpx, gun.gunInfo.underoffsetpy, gun.gunInfo.underoffsetpz);
						GL11.glRotatef(gun.gunInfo.underrotationx, 1.0F, 0.0F, 0.0F);
						GL11.glRotatef(gun.gunInfo.underrotationy, 0.0F, 1.0F, 0.0F);
						GL11.glRotatef(gun.gunInfo.underrotationz, 0.0F, 0.0F, 1.0F);
						boolean flag1 = false;
						if (attachrender instanceof HMGRenderItemGun_U) {
							
							GL11.glScalef(1/(0.4f * modelscala),1/(0.4f * modelscala),1/(0.4f * modelscala));
							attachrender.renderItem(ItemRenderType.ENTITY, items[4], data);
							GL11.glScalef(modelscala, modelscala, modelscala);
						}
						GL11.glRotatef(-gun.gunInfo.underrotationx, 1.0F, 0.0F, 0.0F);
						GL11.glRotatef(-gun.gunInfo.underrotationy, 0.0F, 1.0F, 0.0F);
						GL11.glRotatef(-gun.gunInfo.underrotationz, 0.0F, 0.0F, 1.0F);
						GL11.glTranslatef(-gun.gunInfo.underoffsetpx, -gun.gunInfo.underoffsetpy, -gun.gunInfo.underoffsetpz);
					} else if (itemstacka.getItem() instanceof HMGXItemGun_Sword) {
						renderpartofmodel("mat13");
						GL11.glPushMatrix();
						GL11.glTranslatef(gun.gunInfo.underoffsetpx, gun.gunInfo.underoffsetpy, gun.gunInfo.underoffsetpz);
						GL11.glRotatef(90, 1.0F, 0.0F, 0.0F);
						GL11.glRotatef(gun.gunInfo.underrotationx, 1.0F, 0.0F, 0.0F);
						GL11.glRotatef(gun.gunInfo.underrotationy, 0.0F, 1.0F, 0.0F);
						GL11.glRotatef(gun.gunInfo.underrotationz, 0.0F, 0.0F, 1.0F);
						if (attachrender instanceof HMGRenderItemGun_S) {
							GL11.glScalef(1/(0.4f * modelscala),1/(0.4f * modelscala),1/(0.4f * modelscala));
							attachrender.renderItem(ItemRenderType.ENTITY, items[4], data);
						}
						GL11.glRotatef(-gun.gunInfo.underrotationx, 1.0F, 0.0F, 0.0F);
						GL11.glRotatef(-gun.gunInfo.underrotationy, 0.0F, 1.0F, 0.0F);
						GL11.glRotatef(-gun.gunInfo.underrotationz, 0.0F, 0.0F, 1.0F);
						GL11.glTranslatef(-gun.gunInfo.underoffsetpx, -gun.gunInfo.underoffsetpy, -gun.gunInfo.underoffsetpz);
						GL11.glPopMatrix();
					}
				} else {
					if (attachrender instanceof HMGRenderItemCustom) {
						GL11.glPushMatrix();
						GL11.glTranslatef(gripattachoffset[0], gripattachoffset[1], gripattachoffset[2]);
						GL11.glRotatef(gripattachrotation[0], 0, 1, 0);
						GL11.glRotatef(gripattachrotation[1], 1, 0, 0);
						GL11.glRotatef(gripattachrotation[2], 0, 0, 1);
						if (nbt.getBoolean("HMGfixed") || nbt.getBoolean("set_up") && itemstacka.getItem() instanceof HMGItemAttachment_grip && ((HMGItemAttachment_grip) itemstacka.getItem()).isbase) {
							if (data[1] == FMLClientHandler.instance().getClient().thePlayer)
								GL11.glRotatef(-FMLClientHandler.instance().getClient().thePlayer.rotationPitch, 1, 0, 0);
							else {
								if (data[1] != null)
									GL11.glRotatef(-((Entity) data[1]).rotationPitch, 1, 0, 0);
								else
									GL11.glRotatef(0, 1, 0, 0);
							}
						}
						((HMGRenderItemCustom) attachrender).renderaspart();
						Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
						GL11.glPopMatrix();
					} else if (itemstacka.getItem() instanceof HMGItemAttachment_grip) {
						renderpartofmodel("mat9");
					} else if (itemstacka.getItem() instanceof HMGItem_Unified_Guns) {
						HMGItem_Unified_Guns Ugun = (HMGItem_Unified_Guns) itemstacka.getItem();
						if (gun.gunInfo.guntype == 1) {
							renderpartofmodel("mat11");
						} else if (gun.gunInfo.guntype == 2) {
							renderpartofmodel("mat10");
						}
					} else if (itemstacka.getItem() instanceof HMGXItemGun_Sword) {
						renderpartofmodel("mat14");
					}
				}
			} else {
				renderpartofmodel("mat21");
			}
			Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
			itemstacka = items[1];
			if(itemstacka != null ){
				if (itemstacka.getItem() instanceof HMGItemSightBase) {
					IItemRenderer sightrender = MinecraftForgeClient.getItemRenderer(itemstacka, type);
					if (sightrender instanceof HMGRenderItemCustom) {
						GL11.glPushMatrix();
						GL11.glTranslatef(sightattachoffset[0],sightattachoffset[1],sightattachoffset[2]);
						((HMGRenderItemCustom)sightrender).renderaspart();
						Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
						GL11.glPopMatrix();
					}else if (itemstacka.getItem() instanceof HMGItemAttachment_reddot) {
						renderpartofmodel("mat41");
						renderpartofmodel("mat4");
					} else if (itemstacka.getItem() instanceof HMGItemAttachment_scope) {
						renderpartofmodel("mat5");
					}
				} else {
					renderpartofmodel("mat20");
				}
			} else {
				renderpartofmodel("mat20");
			}
		}
	}
	public void modelSight(ItemStack item,ItemRenderType type,Object... data){
	}

	public void mat31mat32(boolean reload){
		//if(rote < Math.PI * 2F)
		//if(rote < 6.2832F)
		if(rotez < 360F || rotex < 360F || rotey < 360F)
		{
			rotez = rotez - mat31rotez * (1-smoothing);//60F
			rotex = rotex - mat31rotex * (1-smoothing);
			rotey = rotey - mat31rotey * (1-smoothing);
			GL11.glTranslatef(mat31posx, mat31posy, mat31posz);//0,0.7,0
			GL11.glRotatef(rotey, 0.0F, 1.0F, 0.0F);
			GL11.glRotatef(rotex, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(rotez, 0.0F, 0.0F, 1.0F);
			GL11.glTranslatef(-mat31posx, -mat31posy, -mat31posz);
			if(reload){
				renderpartofmodel("mat31");
			}
			GL11.glTranslatef(mat31posx, mat31posy, mat31posz);//0,0.7,0
			GL11.glRotatef(-rotey, 0.0F, 1.0F, 0.0F);
			GL11.glRotatef(-rotex, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(-rotez, 0.0F, 0.0F, 1.0F);
			GL11.glTranslatef(-mat31posx, -mat31posy, -mat31posz);
			//rote = (float) (rote  + (Math.PI *(1/3) ));
			//rote = rote + 1.04720F;
			GL11.glTranslatef(mat32posx, mat32posy, mat32posz);//0,0.5,0
			GL11.glRotatef(mat32rotey*abs(0.5f-smoothing)*2, 0.0F, 1.0F, 0.0F);//90
			GL11.glRotatef(mat32rotez*abs(0.5f-smoothing)*2, 0.0F, 0.0F, 1.0F);
			GL11.glRotatef(mat32rotex*abs(0.5f-smoothing)*2, 1.0F, 0.0F, 0.0F);
			GL11.glTranslatef(-mat32posx, -mat32posy, -mat32posz);
			renderpartofmodel("mat32");
			GL11.glTranslatef(mat32posx, mat32posy, mat32posz);//0,0.5,0
			GL11.glRotatef(-mat32rotey*abs(0.5f-smoothing)*2, 0.0F, 1.0F, 0.0F);//90
			GL11.glRotatef(-mat32rotez*abs(0.5f-smoothing)*2, 0.0F, 0.0F, 1.0F);
			GL11.glRotatef(-mat32rotex*abs(0.5f-smoothing)*2, 1.0F, 0.0F, 0.0F);
			GL11.glTranslatef(-mat32posx, -mat32posy, -mat32posz);
			//	GL11.glRotatef(-mat32rotex, 0.0F, 0.0F, 1.0F);//90
			//	GL11.glRotatef(-mat32rotez, 1.0F, 0.0F, 0.0F);
			//	GL11.glRotatef(-mat32rotey, 0.0F, 1.0F, 0.0F);
		}else{
			rotez = 0;
			rotey = 0;
			rotex = 0;
		}
	}

	public void glMatrixForRenderInInventory() {
		GL11.glRotatef(15F, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(-30F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(1900F, 0.0F, 0.0F, 1.0F);
		GL11.glTranslatef(-0.8F, -1.2F, -0.1F);
	}

	public void glMatrixForRenderInEquipped(float reco) {
		GL11.glRotatef(180F, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(180F, 0.0F, 0.0F, 1.0F);
		GL11.glTranslatef(nox, noy, noz + 1.4f);// -0.2F//-0.7,0.7,0
	}

	public void glMatrixForRenderInEquipped_reload() {
		GL11.glRotatef(190F, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(180F, 0.0F, 0.0F, 1.0F);
		GL11.glTranslatef(nox, noy, noz + 1.4f);
	}

	//ADS
	public void glMatrixForRenderInEquippedADS(float reco) {
		GL11.glRotatef(180f, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(180f, 0.0F, 0.0F, 1.0F);
		GL11.glTranslatef(modelx, modely, modelz + reco + 1.4f);// 0.694,1.03,-1.0//-1.4F
		GL11.glRotatef(rotationy, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(rotationx, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(rotationz, 0.0F, 0.0F, 1.0F);
	}

	public void glMatrixForRenderInEntity(float reco) {
		GL11.glRotatef(190F, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(45F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(180F, 0.0F, 0.0F, 1.0F);
		GL11.glTranslatef(-1.3f, 1.65f, 0.1f+reco);//-0.4
		GL11.glScalef(1.0F, 1.0F, 1.0F);
	}

	public void glMatrixForRenderInEntityPlayer(float reco) {
		GL11.glRotatef(110F, 1.0F, 0.0F, 0.0F);// 90//110
		GL11.glRotatef(-0F, 0.0F, 1.0F, 0.0F);// 0
		GL11.glRotatef(120F, 0.0F, 0.0F, 1.0F);// 130
		GL11.glTranslatef(0.2F, -1.85F, -0.5f + reco);
		GL11.glRotatef(10F, 0.0F, 0.0F, 1.0F);
		GL11.glRotatef(10F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-10F, 1.0F, 0.0F, 0.0F);
	}

	public ResourceLocation getEntityTexture(AbstractClientPlayer p_110775_1_)
	{
		return p_110775_1_.getLocationSkin();
	}
	public Minecraft getminecraft(){
		return Minecraft.getMinecraft();
	}
	public float getfloatfromnbt(String name){
		return nbt.getFloat(name);
	}
	public int getintfromnbt(String name){
		return nbt.getInteger(name);
	}
	public boolean getbooleanfromnbt(String name){
		return nbt.getBoolean(name);
	}
	public boolean getbytefromnbt(String name){
		return nbt.getBoolean(name);
	}

	public float getSmoothing(){
		return smoothing;
	}

	public void bindPlayertexture(Entity entityplayer) {
		if (entityplayer instanceof AbstractClientPlayer) {
			ResourceLocation resourcelocation = this.getEntityTexture((AbstractClientPlayer) entityplayer);
			if (resourcelocation == null) {
				resourcelocation = AbstractClientPlayer.getLocationSkin("default");
			}
			Minecraft.getMinecraft().renderEngine.bindTexture(resourcelocation);
		}
	}
	public void bindGuntexture(Item gun){
		if(gun instanceof HMGItem_Unified_Guns) {
			Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
		}
	}
	public void renderarm(float armrotationxl,float armrotationyl,float armrotationzl,float armoffsetxl,float armoffsetyl,float armoffsetzl,
						  float armrotationxr,float armrotationyr,float armrotationzr,float armoffsetxr,float armoffsetyr,float armoffsetzr){
		GL11.glScalef(1/modelscala,1/modelscala,1/modelscala);
		// Keep non-HMG player model consumers (e.g. armor / other mods) stable by restoring arm state
		// after HMG applies custom arm offsets and rotations for first-person gun rendering.
		float prevLeftRotateX = modelBipedMain.bipedLeftArm.rotateAngleX;
		float prevLeftRotateY = modelBipedMain.bipedLeftArm.rotateAngleY;
		float prevLeftRotateZ = modelBipedMain.bipedLeftArm.rotateAngleZ;
		float prevLeftOffsetX = modelBipedMain.bipedLeftArm.offsetX;
		float prevLeftOffsetY = modelBipedMain.bipedLeftArm.offsetY;
		float prevLeftOffsetZ = modelBipedMain.bipedLeftArm.offsetZ;
		float prevRightRotateX = modelBipedMain.bipedRightArm.rotateAngleX;
		float prevRightRotateY = modelBipedMain.bipedRightArm.rotateAngleY;
		float prevRightRotateZ = modelBipedMain.bipedRightArm.rotateAngleZ;
		float prevRightOffsetX = modelBipedMain.bipedRightArm.offsetX;
		float prevRightOffsetY = modelBipedMain.bipedRightArm.offsetY;
		float prevRightOffsetZ = modelBipedMain.bipedRightArm.offsetZ;

		modelBipedMain.bipedLeftArm.rotateAngleX = armrotationxl;
		modelBipedMain.bipedLeftArm.rotateAngleY = armrotationyl;
		modelBipedMain.bipedLeftArm.rotateAngleZ = armrotationzl;
		modelBipedMain.bipedLeftArm.offsetX = armoffsetxl * armoffsetscale;
		modelBipedMain.bipedLeftArm.offsetY = armoffsetyl * armoffsetscale;
		modelBipedMain.bipedLeftArm.offsetZ = armoffsetzl * armoffsetscale;
		modelBipedMain.bipedRightArm.rotateAngleX = armrotationxr;
		modelBipedMain.bipedRightArm.rotateAngleY = armrotationyr;
		modelBipedMain.bipedRightArm.rotateAngleZ = armrotationzr;
		modelBipedMain.bipedRightArm.offsetX = armoffsetxr * armoffsetscale;
		modelBipedMain.bipedRightArm.offsetY = armoffsetyr * armoffsetscale;
		modelBipedMain.bipedRightArm.offsetZ = armoffsetzr * armoffsetscale;

		GL11.glPushMatrix();
		modelBipedMain.bipedRightArm.render(0.0625f);
		modelBipedMain.bipedLeftArm.render(0.0625f);
		GL11.glPopMatrix();

		modelBipedMain.bipedLeftArm.rotateAngleX = prevLeftRotateX;
		modelBipedMain.bipedLeftArm.rotateAngleY = prevLeftRotateY;
		modelBipedMain.bipedLeftArm.rotateAngleZ = prevLeftRotateZ;
		modelBipedMain.bipedLeftArm.offsetX = prevLeftOffsetX;
		modelBipedMain.bipedLeftArm.offsetY = prevLeftOffsetY;
		modelBipedMain.bipedLeftArm.offsetZ = prevLeftOffsetZ;
		modelBipedMain.bipedRightArm.rotateAngleX = prevRightRotateX;
		modelBipedMain.bipedRightArm.rotateAngleY = prevRightRotateY;
		modelBipedMain.bipedRightArm.rotateAngleZ = prevRightRotateZ;
		modelBipedMain.bipedRightArm.offsetX = prevRightOffsetX;
		modelBipedMain.bipedRightArm.offsetY = prevRightOffsetY;
		modelBipedMain.bipedRightArm.offsetZ = prevRightOffsetZ;
	}

	public int bindAttach_SightPosition_and_getSightType(Entity entity,ItemStack item) {
		ItemStack itemstackSight =null;
		NBTTagList tags = (NBTTagList)  item.getTagCompound().getTag("Items");
		if(tags != null) {
			{
				NBTTagCompound tagCompound = tags.getCompoundTagAt(0);
				itemstackSight = ItemStack.loadItemStackFromNBT(tagCompound);
			}
		}
		if (itemstackSight == null) {
			modely = modely0;
			modelx = modelx0;
			modelz = modelz0;

			rotationx = rotationx0;
			rotationy = rotationy0;
			rotationz = rotationz0;
			return 0;
		} else if (itemstackSight.getItem() instanceof HMGItemAttachment_reddot) {
			modely = modely1;
			modelx = modelx1;
			modelz = modelz1;

			rotationx = rotationx1;
			rotationy = rotationy1;
			rotationz = rotationz1;
			return 1;
		} else if (itemstackSight.getItem() instanceof HMGItemAttachment_scope) {
			modely = modely2;
			modelx = modelx2;
			modelz = modelz2;

			rotationx = rotationx2;
			rotationy = rotationy2;
			rotationz = rotationz2;
			return 2;
		}
		return -1;
	}
	public int getAttach_SightType(Entity entity,ItemStack item) {
		ItemStack itemstackSight =null;
		NBTTagList tags = (NBTTagList)  item.getTagCompound().getTag("Items");
		if(tags != null) {
			{
				NBTTagCompound tagCompound = tags.getCompoundTagAt(0);
				itemstackSight = ItemStack.loadItemStackFromNBT(tagCompound);
			}
		}
		if (itemstackSight == null) {
			return 0;
		} else if (itemstackSight.getItem() instanceof HMGItemAttachment_reddot) {
			return 1;
		} else if (itemstackSight.getItem() instanceof HMGItemAttachment_scope) {
			return 2;
		}
		return 0;
	}
	public String getSightAttachname(Entity entity,ItemStack item) {
		ItemStack itemstackSight =null;
		NBTTagList tags = (NBTTagList)  item.getTagCompound().getTag("Items");
		if(tags != null) {
			{
				NBTTagCompound tagCompound = tags.getCompoundTagAt(0);
				itemstackSight = ItemStack.loadItemStackFromNBT(tagCompound);
			}
		}
		if(itemstackSight != null){
			return itemstackSight.getUnlocalizedName();
		}
		return "null";
	}

	public int getUnderbarrelAttachType(Entity entity,ItemStack item) {
		ItemStack itemstacka =null;
		NBTTagList tags = (NBTTagList)  item.getTagCompound().getTag("Items");
		if(tags != null) {
			{
				NBTTagCompound tagCompound = tags.getCompoundTagAt(4);
				itemstacka = ItemStack.loadItemStackFromNBT(tagCompound);
			}
		}
		if (itemstacka != null && itemstacka.getItem() instanceof HMGItemAttachment_grip){
			return 0;
		} else if(itemstacka != null && itemstacka.getItem() instanceof HMGXItemGun_Sword){
			return 1;
		} else {
			return -1;
		}
	}
	public ItemStack getUnderbarrelAttachStack(Entity entity,ItemStack item) {
		ItemStack itemstacka =null;
		NBTTagList tags = (NBTTagList)  item.getTagCompound().getTag("Items");
		if(tags != null) {
			{
				NBTTagCompound tagCompound = tags.getCompoundTagAt(4);
				int slot = tagCompound.getByte("Slot");
				itemstacka = ItemStack.loadItemStackFromNBT(tagCompound);
			}
		}
		return itemstacka;
	}

	public String getAttachnameAtslot(Entity entity,ItemStack item,int slot) {
		ItemStack itemstacka =null;
		NBTTagList tags = (NBTTagList)  item.getTagCompound().getTag("Items");
		if(tags != null) {
			{
				NBTTagCompound tagCompound = tags.getCompoundTagAt(slot);
				itemstacka = ItemStack.loadItemStackFromNBT(tagCompound);
			}
		}
		return itemstacka != null ? itemstacka.getUnlocalizedName():"null";
	}
	public ItemStack getAttach_inSlot(Entity entity,ItemStack item,int slot) {
		ItemStack itemstacka =null;
		NBTTagList tags = (NBTTagList)  item.getTagCompound().getTag("Items");
		if(tags != null) {
			{
				NBTTagCompound tagCompound = tags.getCompoundTagAt(slot);
				itemstacka = ItemStack.loadItemStackFromNBT(tagCompound);
			}
		}
		return itemstacka;
	}

	public void underRend_useunderGunModel(HMGItem_Unified_Guns gun, ItemStack itemStack, ItemRenderType type, Object... data){
		if(itemStack != null &&( itemStack.getItem() instanceof HMGItem_Unified_Guns ||itemStack.getItem() instanceof HMGXItemGun_Sword)) {
			GL11.glTranslatef(gun.gunInfo.underoffsetpx, gun.gunInfo.underoffsetpy, gun.gunInfo.underoffsetpz);
			if(itemStack.getItem() instanceof HMGXItemGun_Sword)GL11.glRotatef(90, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(gun.gunInfo.underrotationx, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(gun.gunInfo.underrotationy, 0.0F, 1.0F, 0.0F);
			GL11.glRotatef(gun.gunInfo.underrotationz, 0.0F, 0.0F, 1.0F);
			IItemRenderer attachrender = MinecraftForgeClient.getItemRenderer(itemStack, type);
			GL11.glScalef(1 / modelscala, 1 / modelscala, 1 / modelscala);
			if (attachrender instanceof HMGRenderItemGun_S) {
				((HMGRenderItemGun_S) attachrender).renderatunder(type, itemStack, data);
			} else if (attachrender instanceof HMGRenderItemGun_U) {
				((HMGRenderItemGun_U) attachrender).renderatunder(type, itemStack, data);
			}
			GL11.glScalef(modelscala, modelscala, modelscala);
			GL11.glRotatef(-gun.gunInfo.underrotationx, 1.0F, 0.0F, 0.0F);
			GL11.glRotatef(-gun.gunInfo.underrotationy, 0.0F, 1.0F, 0.0F);
			GL11.glRotatef(-gun.gunInfo.underrotationz, 0.0F, 0.0F, 1.0F);
			GL11.glTranslatef(-gun.gunInfo.underoffsetpx, -gun.gunInfo.underoffsetpy, -gun.gunInfo.underoffsetpz);
		}
	}

	public void setLighting(float x,float y){
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,x,y);
	}
	public int[] getFirstpersonLighting(){
		Minecraft mc = this.getminecraft();
		Entity entityclientplayermp = mc.thePlayer;
		int i = mc.theWorld.getLightBrightnessForSkyBlocks(MathHelper.floor_double(entityclientplayermp.posX), MathHelper.floor_double(entityclientplayermp.posY), MathHelper.floor_double(entityclientplayermp.posZ), 0);
		int j = i % 65536;
		int k = i / 65536;
		return new int[]{j, k};
	}
	public int[] getThirdpersonLighting(Entity entity){
		Minecraft mc = this.getminecraft();
		int i = mc.theWorld.getLightBrightnessForSkyBlocks(MathHelper.floor_double(entity.posX), MathHelper.floor_double(entity.posY), MathHelper.floor_double(entity.posZ), 0);
		int j = i % 65536;
		int k = i / 65536;
		return new int[]{j, k};
	}

	public boolean is_entity_player(Entity entity){
		return entity instanceof EntityPlayer;
	}
	public boolean isentitysprinting(Entity entity){
		return entity.isSprinting() && !nbt.getBoolean("set_up"); //&& !HandmadeGunsCore.Key_ADS(entity)
	}
	public boolean isentitysneaking(Entity entity){
		return entity.isSneaking();
	}

	public int getremainingbullet(ItemStack itemStack){
		
		return ((HMGItem_Unified_Guns)itemStack.getItem()).remain_Bullet(itemStack);
	}
	public void renderpartofmodel(String name){
		modeling.renderPart(name);
		if (gunSkinTexture != null) {
			GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_POLYGON_BIT);
			Minecraft.getMinecraft().renderEngine.bindTexture(gunSkinTexture);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);
			GL11.glDepthMask(false);
			GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
			GL11.glPolygonOffset(-1.0F, -1.0F);
			GL11.glColor4f(1, 1, 1, 1);
			modeling.renderPart(name);
			GL11.glPopAttrib();
			Minecraft.getMinecraft().renderEngine.bindTexture(guntexture);
		}
		float lastBrightnessX = OpenGlHelper.lastBrightnessX;
		float lastBrightnessY = OpenGlHelper.lastBrightnessY;

		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
		modeling.renderPart(name + "light");
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)lastBrightnessX, (float)lastBrightnessY);
	}
}
