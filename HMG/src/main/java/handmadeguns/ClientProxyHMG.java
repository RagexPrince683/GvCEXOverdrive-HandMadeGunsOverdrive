package handmadeguns;

import handmadeguns.client.HMGManualGunPickupClientHandler;
import handmadeguns.client.HMGCustomNpcRenderCompat;


import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;

import cpw.mods.fml.relauncher.ReflectionHelper;
import handmadeguns.client.audio.BulletSoundHMG;
import handmadeguns.client.audio.GunSoundHMG;
import handmadeguns.client.audio.MovingSoundHMG;
import handmadeguns.client.audio.ReloadSoundHMG;
import handmadeguns.client.modelLoader.emb_modelloader.MQO_ModelLoader;
import handmadeguns.client.modelLoader.obj_modelloaderMod.obj.HMGObjModelLoader;
import handmadeguns.client.modelLoader.obj_modelloaderMod.obj.HMGObjResourceReloadListener;
import handmadeguns.entity.*;
import handmadeguns.entity.bullets.*;
import handmadeguns.event.HMGFovHandler;
import handmadeguns.event.KillFeedHUD;
import handmadeguns.items.guns.HMGItem_Unified_Guns;
import handmadeguns.network.PacketSpawnParticle;
import handmadeguns.client.render.*;
import handmadeguns.client.modelLoader.tcn_modelloaderMod.TechneModelLoader;
import handmadeguns.tile.TileMounter;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.AdvancedModelLoader;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import paulscode.sound.SoundSystemConfig;

import static handmadeguns.HandmadeGunsCore.HMG_proxy;
import static handmadeguns.HandmadeGunsCore.cfg_ADS_Toggle;
import static handmadeguns.HandmadeGunsCore.cfg_SwapFireAndADSKeys;

public class ClientProxyHMG extends CommonSideProxyHMG {
	public static final KeyBinding_mod Reload					= new KeyBinding_mod("Reload Magazine", Keyboard.KEY_R, "HandmadeGuns");
	public static final KeyBinding_mod Fire_AttachedGun		= new KeyBinding_mod("Fire AttachedGun", Keyboard.KEY_F, "HandmadeGuns");
	public static final KeyBinding_mod ADS =
			new KeyBinding_mod("ADS_Key", -100, "HandmadeGuns"); //todone? Left click by default
	public static final KeyBinding_mod gunPrepare_modification = new KeyBinding_mod("Gun Prepare Modification Key", Keyboard.KEY_LMENU, "HandmadeGuns");
	public static final KeyBinding_mod Attachment				= new KeyBinding_mod("[Gun Preparation] Attachment GUI", Keyboard.KEY_X, "HandmadeGuns");
	public static final KeyBinding_mod ChangeMagazineType		= new KeyBinding_mod("[Gun Preparation] Change Magazine Type", Keyboard.KEY_B, "HandmadeGuns");
	//TODO: PRESS THIS KEY AUTOMATICALLY FOR THE PLAYER UNTIL THE RIGHT MAG IS SELECTED. ^
	// THIS DOES NOT NEED TO BE A FUCKING KEYBIND!!!
	public static final KeyBinding_mod Fix						= new KeyBinding_mod("[Gun Preparation] Fix Gun", Keyboard.KEY_H, "HandmadeGuns");

	public static final KeyBinding_mod gunSetting_modification = new KeyBinding_mod("Gun Settings Modification", Keyboard.KEY_NONE, "HandmadeGuns");
	public static final KeyBinding_mod El_Up					= new KeyBinding_mod("[Gun Settings] Zero in : Increase", Keyboard.KEY_Y, "HandmadeGuns");
	public static final KeyBinding_mod El_Reset				= new KeyBinding_mod("[Gun Settings] Zero in : Reset", Keyboard.KEY_H, "HandmadeGuns");
	public static final KeyBinding_mod El_Down					= new KeyBinding_mod("[Gun Settings] Zero in : Decrease", Keyboard.KEY_N, "HandmadeGuns");
	public static final KeyBinding_mod SeekerOpen_Close		= new KeyBinding_mod("[Gun Settings] Seeker Open/Close", Keyboard.KEY_C, "HandmadeGuns");
	public static final KeyBinding_mod Mode					= new KeyBinding_mod("[Gun Settings] Cycle Selector", Keyboard.KEY_F, "HandmadeGuns");


	private static final String trailtexture = ("handmadeguns:textures/entity/trail");
	private static final String lockonmarker = ("handmadeguns:textures/items/lockonmarker");
	static Field equippedProgress;
	static Field prevEquippedProgress;
	static Field itemToRender;
	static Field previousEquipment;
	static Field rightClickDelayTimer = null;

	static Field fovModifierHandPrev;
	static Field fovModifierHand;
	static Field prevCamFOV;
	static Field camFOV;

	static int beforeSlot = -1;

	public static ArrayList<IModelCustom_HMG> modelList = new ArrayList<>();
//	public static final KeyBinding Fire2 = new KeyBinding("ADS_Key",-100 , "HandmadeGuns");
	//public static final KeyBinding Jump = new KeyBinding("Jump", Keyboard.KEY_X, "HandmadeGuns");

	//public static ModelBiped PlayerRender = new HMGPlayer();

	@Override
	public File ProxyFile(){
		return Minecraft.getMinecraft().mcDataDir;
	}

	@Override
	public void setuprender(){

		try {
//			System.setProperty("forge.forceDisplayStencil", "true");
//			Field stencilBits_F = ReflectionHelper.findField(ForgeHooksClient.class, "stencilBits");
//			Field modifiersField = Field.class.getDeclaredField("modifiers");
//			modifiersField.setAccessible(true);
//			modifiersField.setInt(stencilBits_F,
//					stencilBits_F.getModifiers() & ~Modifier.PRIVATE); // 更新対象アクセス用のFieldオブジェクトのmodifiersからprivateとfinalを外す。
//			stencilBits_F.set(null, 8);
			System.out.println("Debug stencil is	" + MinecraftForgeClient.getStencilBits());
			System.out.println("Debug stencil state " + Boolean.parseBoolean(System.getProperty("forge.forceDisplayStencil", "false")));
//			net.minecraftforge.client.ForgeHooksClient.createDisplay();

//			OpenGlHelper.initializeTextures();

//			Field framebufferMc_F = ReflectionHelper.findField(Minecraft.class, "framebufferMc");
//			framebufferMc_F.set(proxy.getMCInstance(),new Framebuffer(proxy.getMCInstance().displayWidth, proxy.getMCInstance().displayHeight, true));
//			proxy.getMCInstance().getFramebuffer().setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
		}catch (Exception e){
			e.printStackTrace();
		}


		SoundSystemConfig.setNumberNormalChannels(128);
		SoundSystemConfig.setNumberStreamingChannels(32);
		AdvancedModelLoader.registerModelHandler(new MQO_ModelLoader());
		AdvancedModelLoader.registerModelHandler(new TechneModelLoader());
	}
	@Override
	public void playsoundat(String sound, float soundLV, float soundSP, float tempsp, double posX, double posY, double posZ){
		Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(new ResourceLocation(sound),soundLV,soundSP*tempsp,(float)posX,(float)posY,(float)posZ));
	}
	@Override
	public void playsound_Gun(String sound, float soundLV, float soundSP,float maxdist,Entity attached,
							  double posX,
							  double posY,
							  double posZ){
		Minecraft.getMinecraft().getSoundHandler().playSound(new GunSoundHMG(attached,sound,soundLV,soundSP,maxdist,
				posX,
				posY,
				posZ));
	}
	@Override
	public void playsoundatEntity(String sound, float soundLV, float soundSP,Entity attached,boolean repeat,int time){
		Minecraft.getMinecraft().getSoundHandler().playSound(new MovingSoundHMG(attached,sound,repeat,soundLV,soundSP,time));
	}
	@Override
	public void playsoundatEntity_reload(String sound, float soundLV, float soundSP, Entity attached, boolean repeat){
		Minecraft.getMinecraft().getSoundHandler().playSound(new ReloadSoundHMG(attached,sound,repeat,soundLV,soundSP));
	}
	@Override
	public void playsoundatBullet(String sound, float soundLV, float soundSP,float mindspeed,float maxdist,Entity attached,boolean repeat){
		Minecraft.getMinecraft().getSoundHandler().playSound(new BulletSoundHMG(attached,sound,repeat,soundLV,soundSP,mindspeed,maxdist));
	}
	@Override
	public EntityPlayer getEntityPlayerInstance() {
			return Minecraft.getMinecraft().thePlayer;
		}
	@Override
	public Minecraft getMCInstance() {
		return Minecraft.getMinecraft();
	}
	@Override
	public void playGUISound(String sound,float speed){
		getMCInstance().getSoundHandler().playSound(PositionedSoundRecord.func_147673_a(new ResourceLocation(sound)));
	}
	@Override
	public void addKillFeedEntry(String attacker, String victim, ItemStack weapon) {
		KillFeedHUD.addEntry(attacker, victim, weapon);
	}

	@Override
	public World getCilentWorld(){
		return FMLClientHandler.instance().getClient().theWorld;
		}

	@Override
	public void registerClientInfo() {
		//ClientRegistry.registerKeyBinding(Speedreload);
	}

	@Override
	public void registerSomething(){
		//RenderManager rendermanager = new RenderManager(mc.renderEngine, mc.getRenderItem());
		//RenderItem renderitem = mc.getRenderItem();

		ClientRegistry.registerKeyBinding(Reload.keyBinding);
		ClientRegistry.registerKeyBinding(Fire_AttachedGun.keyBinding);
		if (cfg_SwapFireAndADSKeys && ADS.keyBinding.getKeyCode() == -100) {
			ADS.keyBinding.setKeyCode(-99);
			KeyBinding.resetKeyBindingArrayAndHash();
		}
		ClientRegistry.registerKeyBinding(ADS.keyBinding);
		ClientRegistry.registerKeyBinding(gunPrepare_modification.keyBinding);
		gunPrepare_modification.includeNull = true;
		ClientRegistry.registerKeyBinding(Attachment.keyBinding);
		ClientRegistry.registerKeyBinding(ChangeMagazineType.keyBinding);
		ClientRegistry.registerKeyBinding(Fix.keyBinding);
		ClientRegistry.registerKeyBinding(gunSetting_modification.keyBinding);
		gunSetting_modification.includeNull = true;
		ClientRegistry.registerKeyBinding(El_Up.keyBinding);
		ClientRegistry.registerKeyBinding(El_Reset.keyBinding);
		ClientRegistry.registerKeyBinding(El_Down.keyBinding);
		ClientRegistry.registerKeyBinding(SeekerOpen_Close.keyBinding);
		ClientRegistry.registerKeyBinding(Mode.keyBinding);
		ClientRegistry.registerKeyBinding(HMGManualGunPickupClientHandler.PICKUP_KEY);
		ClientRegistry.registerKeyBinding(handmadeguns.client.animation.AnimationClient.INSPECT);
		cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(new handmadeguns.client.animation.AnimationClient());
		HMGManualGunPickupClientHandler manualPickupHandler = new HMGManualGunPickupClientHandler();
		cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(manualPickupHandler);
		MinecraftForge.EVENT_BUS.register(manualPickupHandler);
		if (Minecraft.getMinecraft().getResourceManager() instanceof IReloadableResourceManager) {
			((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new HMGObjResourceReloadListener());
		}
		MinecraftForge.EVENT_BUS.register(new HMGParticles());
		MinecraftForge.EVENT_BUS.register(new HMGFovHandler());
		MinecraftForge.EVENT_BUS.register(new HMGCustomNpcRenderCompat());


		try {
			RenderingRegistry.registerEntityRenderingHandler(HMGEntityItemMount.class, new HMGRenderItemMount());
			RenderingRegistry.registerEntityRenderingHandler(HMGEntityItemMount2.class, new HMGRenderItemMount2());

			RenderingRegistry.registerEntityRenderingHandler(HMGEntityBullet.class, new HMGRenderBullet());
			RenderingRegistry.registerEntityRenderingHandler(HMGEntityFallingBlockModified.class, new RenderFallingBlockMod());
			RenderingRegistry.registerEntityRenderingHandler(HMGEntityBulletRocket.class, new HMGRenderBulletExplode());
			RenderingRegistry.registerEntityRenderingHandler(HMGEntityBulletExprode.class, new HMGRenderBulletExplode());
			RenderingRegistry.registerEntityRenderingHandler(HMGEntityBulletTorp.class, new HMGRenderBulletExplode());

			RenderingRegistry.registerEntityRenderingHandler(HMGEntityLight.class, new HMGRenderRight());
			RenderingRegistry.registerEntityRenderingHandler(HMGEntityLight2.class, new HMGRenderRight2());
			RenderingRegistry.registerEntityRenderingHandler(HMGEntityLaser.class, new HMGRenderLaser());

			RenderingRegistry.registerEntityRenderingHandler(HMGEntityBullet_AP.class, new HMGRenderBullet());
			RenderingRegistry.registerEntityRenderingHandler(HMGEntityBullet_Frag.class, new HMGRenderBullet());
			RenderingRegistry.registerEntityRenderingHandler(HMGEntityBullet_HE.class, new HMGRenderBullet());
			RenderingRegistry.registerEntityRenderingHandler(HMGEntityBullet_TE.class, new HMGRenderBulletExplode());
			RenderingRegistry.registerEntityRenderingHandler(HMGEntityBullet_AT.class, new HMGRenderBullet());
			RenderingRegistry.registerEntityRenderingHandler(HMGEntityBullet_Flame.class, new HMGRenderBullet());


			RenderingRegistry.registerEntityRenderingHandler(HMGEntityBulletCartridge.class, new HMGRenderBulletCartridge());
			RenderingRegistry.registerEntityRenderingHandler(PlacedGunEntity.class, new PlacedGun_Render());


			try {
				equippedProgress = ReflectionHelper.findField(HMG_proxy.getMCInstance().entityRenderer.itemRenderer.getClass(), "field_78454_c","equippedProgress");
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				prevEquippedProgress = ReflectionHelper.findField(HMG_proxy.getMCInstance().entityRenderer.itemRenderer.getClass(), "field_78451_d","prevEquippedProgress");
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				itemToRender = ReflectionHelper.findField(HMG_proxy.getMCInstance().entityRenderer.itemRenderer.getClass(), "field_78453_b","itemToRender");
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				previousEquipment = ReflectionHelper.findField(EntityLivingBase.class, "field_82180_bT","previousEquipment");
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				rightClickDelayTimer = ReflectionHelper.findField(HMG_proxy.getMCInstance().getClass(), "field_71467_ac","rightClickDelayTimer");
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				fovModifierHandPrev = ReflectionHelper.findField(EntityRenderer.class, "field_78506_S","fovModifierHandPrev");
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				fovModifierHand = ReflectionHelper.findField(EntityRenderer.class, "field_78507_R","fovModifierHand");
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				prevCamFOV = ReflectionHelper.findField(EntityRenderer.class, "field_78494_N","prevDebugCamFOV");
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				camFOV = ReflectionHelper.findField(EntityRenderer.class, "field_78493_M","debugCamFOV");
			} catch (Exception e) {
				e.printStackTrace();
			}

		}catch (Exception e){
			e.printStackTrace();
		}

	}

	@Override
	public void registerTileEntity() {
		RenderTileMounter renderTileMounter = new RenderTileMounter();
		ClientRegistry.registerTileEntity(TileMounter.class, "TileItemMounter", renderTileMounter);//������
		//GameRegistry.registerTileEntity(GVCTileEntityItemG36.class, "GVCTileEntitysample");
	}
	@Override
	public void force_render_item_position(ItemStack itemStack,int i){
		try {
			Object obj = itemToRender.get(HMG_proxy.getMCInstance().entityRenderer.itemRenderer);
			if(beforeSlot == i && obj instanceof ItemStack && ((ItemStack)obj).getItem() instanceof HMGItem_Unified_Guns) {
				equippedProgress.set(HMG_proxy.getMCInstance().entityRenderer.itemRenderer, 1);
				prevEquippedProgress.set(HMG_proxy.getMCInstance().entityRenderer.itemRenderer, 1);
				if (itemToRender.get(HMG_proxy.getMCInstance().entityRenderer.itemRenderer) != itemStack) {
					itemToRender.set(HMG_proxy.getMCInstance().entityRenderer.itemRenderer, itemStack);
				}
			}else {
				beforeSlot = i;
			}

		} catch (NullPointerException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void resetRightClickTimer(){
		try {
			rightClickDelayTimer.set(HMG_proxy.getMCInstance(), 0);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void setRightClickTimer(){
		try {
			ClientProxyHMG.rightClickDelayTimer.set(HMG_proxy.getMCInstance(), 10);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void InitRendering()
	{
		super.InitRendering();
		MinecraftForge.EVENT_BUS.register(new HMGParticles());
		//ClientRegistry.bindTileEntitySpecialRenderer(GVCTileEntityItemG36.class, new GVCRenderItemG36());
	}
	@Override
	public boolean seekerOpenClose(){
		return SeekerOpen_Close.isKeyDown_withStopper() && (gunSetting_modification).isKeyDown_noStop();
		//return false;
	}
	@Override
	public boolean seekerOpenClose_NonStop(){
		return SeekerOpen_Close.isKeyDown_noStop() && (gunSetting_modification).isKeyDown_noStop();
		//return false;
	}
	@Override
	public boolean fixkeydown(){
		return Fix.isKeyDown_withStopper() && (gunPrepare_modification).isKeyDown_noStop();
		//return false;
	}

	@Override
	public boolean upElevationKeyDown(){
		return El_Up.isKeyDown_withStopper() && (gunSetting_modification).isKeyDown_noStop();
	}
	@Override
	public boolean downElevationKeyDown(){
		return El_Down.isKeyDown_withStopper() && (gunSetting_modification).isKeyDown_noStop();
	}
	@Override
	public boolean resetElevationKeyDown(){
		return El_Reset.isKeyDown_withStopper() && (gunSetting_modification).isKeyDown_noStop();
	}
	@Override
	public boolean FClick(){
		return Fire_AttachedGun.isKeyDown_withStopper();
	}


	//TODO: CALL THIS FUCKING METHOD REPEATEDLY IF THE PLAYER EVEN JUST HAS THE RIGHT AMMO IN THEIR INVENTORY AND IS NOT NULL,
	// WHY DO WE NEED TO PRESS STUPID FUCKING KEYS TO CHANGE MAGAZINE TYPE???
	// oh my god... this SLOP is hardcoded.
	@Override
	public boolean ChangeMagazineTypeClick(){
		return ChangeMagazineType.isKeyDown_withStopper() && (gunPrepare_modification).isKeyDown_noStop();
	}
	@Override
	public boolean FClick_no_stopper(){
		return (Fire_AttachedGun).isKeyDown_noStop();
	}
	@Override
	public boolean ADSClick(){
//		System.out.println("debug");
		return cfg_ADS_Toggle ? ADS.isKeyDown_toggle() : ADS.isKeyDown_noStop();
	}
	@Override
	public boolean ReloadKey_isPressed(){
		return (Reload).isKeyDown_noStop();
	}

	@Override
	public boolean AttachmentKey_isPressed(){
		return (Attachment).isKeyDown_noStop() && (gunPrepare_modification).isKeyDown_noStop();
	}
	@Override
	public boolean ModeKey_isPressed(){
		return Mode.isKeyDown_withStopper() && gunSetting_modification.isKeyDown_noStop();
	}
//	@Override
//	public boolean Secondarykeyispressed(){
//		return keyDown(Fire2.getKeyCode());
//	}


//	public static boolean keyDown_except_null(KeyBinding_withStopper key)
//	{
//		return keyDown_except_null(key.keyBinding);
//	}
//
//	public static boolean keyDown_except_null(KeyBinding key)
//	{
//		boolean state = key.getKeyCode() != Keyboard.KEY_NONE;
//		if (HMG_proxy.getMCInstance().currentScreen == null || HMG_proxy.getMCInstance().currentScreen.allowUserInput) {
//			state = key.getIsKeyPressed();
//		}
//		return state;
//	}
//
//	public static boolean keyDown_include_null(KeyBinding_withStopper key)
//	{
//		return keyDown_include_null(key.keyBinding);
//	}
//
//	public static boolean keyDown_include_null(KeyBinding key)
//	{
//		boolean state = key.getKeyCode() == Keyboard.KEY_NONE;
//		if (HMG_proxy.getMCInstance().currentScreen == null || HMG_proxy.getMCInstance().currentScreen.allowUserInput) {
//			state = key.getIsKeyPressed();
//		}
//		return state;
//	}



	public void spawnParticles(PacketSpawnParticle message){
		try {
			HMGEntityParticles var10 = new HMGEntityParticles(getCilentWorld(),
					message.posx, message.posy, message.posz);
			if(message.name == null || message.name.equals("")) {
				switch (message.id % 100) {
					case 0:
						var10.setParticleIcon(HMGParticles.getInstance().getIcon("handmadeguns:fire"));
						var10.setIcon("handmadeguns:textures/items/fire");
						var10.isglow = message.id/100 ==1;
						var10.fuse = 1;
						break;
					case 1:
						var10.setParticleIcon(HMGParticles.getInstance().getIcon("handmadeguns:smoke"));
						var10.thisMotionX = message.motionX;
						var10.thisMotionY = message.motionY;
						var10.thisMotionZ = message.motionZ;
						var10.fuse = 5;
						var10.animationspeed = 2;
						var10.setIcon("handmadeguns:textures/items/smoke",10);
						var10.setParticleAlpha(1);
						var10.setParticleScale(3);
						break;
					case 2:

						var10.setParticleIcon(HMGParticles.getInstance().getIcon("handmadeguns:lockonmarker"));
						var10.setIcon(lockonmarker);
						var10.animationspeed = 2;
						var10.setParticleScale(1);
						var10.isglow = false;
						var10.fuse = 1;
						var10.fixedsize = true;
						var10.disable_DEPTH_TEST = true;
						var10.isrenderglow = true;
						message.id = 102;

						var10.prevPosX = message.posx;
						var10.prevPosY = message.posy;
						var10.prevPosZ = message.posz;
						break;
					case 3:
						var10.setParticleIcon(HMGParticles.getInstance().getIcon("handmadeguns:smoke"));
						var10.thisMotionX = message.motionX;
						var10.thisMotionY = message.motionY;
						var10.thisMotionZ = message.motionZ;
						var10.fuse = message.fuse;
						var10.animationspeed = 2;
						var10.setIcon(trailtexture,10);
						var10.setParticleAlpha(1);
						var10.istrail = true;
						var10.trailwidth = message.trailwidth;
						var10.setParticleScale(3);
						break;
					case 4:

						var10.setParticleIcon(HMGParticles.getInstance().getIcon("handmadeguns:lockonmarker"));
						var10.setIcon(lockonmarker);
						var10.setParticleScale(1);
						var10.isglow = false;
						var10.fuse = 0;
						var10.fixedsize = true;
						var10.disable_DEPTH_TEST = true;
						var10.isrenderglow = true;
						message.id = 102;

						var10.prevPosX = message.posx;
						var10.prevPosY = message.posy;
						var10.prevPosZ = message.posz;
						break;
//
//		bullet = message.bullet.setdata(bullet);
//		System.out.println("bullet "+ bullet);
				}
			}else {
				if(message.id % 100== 3 || message.id == 3){
					var10.setParticleIcon(HMGParticles.getInstance().getIcon("handmadeguns:smoke"));
					var10.thisMotionX = message.motionX;
					var10.thisMotionY = message.motionY;
					var10.thisMotionZ = message.motionZ;
					var10.fuse = message.fuse;
					var10.istrail = true;
					var10.trailwidth = message.trailwidth;
					var10.setIcon("handmadeguns:textures/items/" + message.name,10);
					var10.setParticleAlpha(1);
				}else if(message.id % 100== 1 || message.id == 1){
					var10.setParticleIcon(HMGParticles.getInstance().getIcon("handmadeguns:" + message.name));
					var10.rotationYaw = (float) message.motionX;
					var10.rotationPitch = (float) message.motionY;
					var10.setIcon("handmadeguns:textures/items/" + message.name, message.fuse+1);
					var10.isrenderglow = message.id/100 ==1;
					var10.setParticleScale(message.scale);
					var10.fuse = message.fuse;
					var10.is3d = message.is3d;
				}else {
					var10.setParticleIcon(HMGParticles.getInstance().getIcon("handmadeguns:" + message.name));
					var10.rotationYaw = (float) message.motionX;
					var10.rotationPitch = (float) message.motionY;
					var10.setIcon("handmadeguns:textures/items/" + message.name, message.fuse+1);
					var10.isglow = message.id/100 ==1;
					var10.setParticleScale(message.scale);
					var10.fuse = message.fuse;
					var10.is3d = message.is3d;
				}
			}

			var10.prevPosX = message.posx;
			var10.prevPosY = message.posy;
			var10.prevPosZ = message.posz;
			var10.isrenderglow = message.id/100 ==1;
			var10.animationspeed = message.animationspeed;

			FMLClientHandler.instance().getClient().effectRenderer.addEffect(var10);
		} catch (ClassCastException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void fixitemEquippedprogress(){
		getMCInstance().entityRenderer.itemRenderer.updateEquippedItem();
	}
	@Override
	public boolean rightClick(){
		return Minecraft.getMinecraft().gameSettings.keyBindUseItem.getIsKeyPressed();
		//return false;
	}

	@Override
	public boolean fireKeyDown(){
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.currentScreen != null && !mc.currentScreen.allowUserInput) {
			return false;
		}
		return mc.gameSettings.keyBindAttack.getIsKeyPressed();
	}

	public String getFixkey(){
		return GameSettings.getKeyDisplayString(Fix.keyBinding.getKeyCode()) + " + " + GameSettings.getKeyDisplayString(gunPrepare_modification.keyBinding.getKeyCode());
	}

	public float getFOVModifier(Minecraft mc,float p_78481_1_, boolean p_78481_2_)
	{
		try {
			EntityRenderer entityRenderer = mc.entityRenderer;
			if (entityRenderer.debugViewDirection > 0)
			{
				return 90.0F;
			}
			else
			{
				EntityLivingBase entityplayer = mc.renderViewEntity;
				float f1 = 70.0F;

				if (p_78481_2_)
				{
					f1 = mc.gameSettings.fovSetting;
					f1 *= fovModifierHandPrev.getFloat(entityRenderer) + (fovModifierHand.getFloat(entityRenderer) - fovModifierHandPrev.getFloat(entityRenderer)) * p_78481_1_;

				}

				if (entityplayer.getHealth() <= 0.0F)
				{
					float f2 = (float)entityplayer.deathTime + p_78481_1_;
					f1 /= (1.0F - 500.0F / (f2 + 500.0F)) * 2.0F + 1.0F;
				}

				Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(mc.theWorld, entityplayer, p_78481_1_);

				if (block.getMaterial() == Material.water)
				{
					f1 = f1 * 60.0F / 70.0F;
				}

				return f1 + prevCamFOV.getFloat(entityRenderer) + (camFOV.getFloat(entityRenderer) - prevCamFOV.getFloat(entityRenderer)) * p_78481_1_;
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public ItemStack[] getPrevEquippedItems(EntityLivingBase entityLivingBase){
		try {
			return (ItemStack[]) previousEquipment.get(entityLivingBase);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}


	public void setUpModels(){
		for(IModelCustom_HMG modelCustom_hmg : modelList){
			while(!modelCustom_hmg.isReady()){
				try {
					Thread.sleep(1L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
		// Model loading can finish during preInit, but rendering cannot safely begin
		// there. In particular, a VBO draw enters fixed-function shader emulation
		// before the first render frame has established its tracked GL state. Leave
		// GPU resource creation lazy; normal rendering compiles each model on the
		// client render thread when a real draw is requested.
		modelList.clear();
	}
	@Override
	public void invalidateReloadableModels(){
		// Commands execute on the client thread, so GPU objects cannot still be in
		// use by an in-progress render when they are released here.
		HMGGunMaker.clearCachedModels();
		HMGObjModelLoader.clearModelCache();
		MQO_ModelLoader.clearModelCache();
	}
	@Override
	public boolean reloadHeldItemModel(ItemStack heldItem){
		return HMGGunMaker.reloadModelsForItem(heldItem == null ? null : heldItem.getItem());
	}
	public void AddModel(Object o){
		modelList.add((IModelCustom_HMG) o);
	}
}
