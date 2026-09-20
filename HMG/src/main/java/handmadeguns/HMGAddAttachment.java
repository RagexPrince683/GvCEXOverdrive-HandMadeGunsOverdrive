package handmadeguns;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import handmadeguns.gunsmithing.GunSmithRecipeRegistry;
import handmadeguns.gunsmithing.GunSmithRecipeCategory;
import handmadeguns.gunsmithing.DeferredHMGRecipes;
import handmadeguns.gunsmithing.GunSmithRecipe;
import handmadeguns.items.*;
import handmadeguns.pack.HMGPackAssetResolver;
import handmadeguns.client.render.HMGRenderItemCustom;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.IModelCustom;

import static handmadeguns.HandmadeGunsCore.tabshmg;
//import static handmadeguns.client.render.HMGRenderItemGun_U_NEW.isentitysprinting;
import static java.lang.Integer.parseInt;

public class HMGAddAttachment
{
	public static List Attach = new ArrayList();
	public static List<Item> Magazines = new ArrayList<Item>();

	public static void load( boolean isClient, File file1)
	{
		String GunName = null;
		String Namegun = null;
		boolean cosume_onCraft = true;
		int kazu = 1;
		String texture = "null";
		String hud = null;
		String ads = "null";
		float zoom = -1;
		boolean isnightvision = false;
		boolean textureOnly = false;
		float damagemodify = 1;
		
		
		float slowdownrate = 1;
		float speedmodify = 1;


		int fuse = -1;
		Integer fuseOverride = null;
		boolean blockdestroyex = true;
		boolean autoDestroy = true;
		boolean hasRoundOption = false;
		int round = 0;
		boolean hasReloadOption = false;
		int reloadTime = 0;
		int bullettype = -1;
		float explosionlevel = -1;
		
		double knockback = Double.NaN;
		double knockbackY = Double.NaN;
		float  bouncerate = Float.NaN;
		float  bouncelimit = Float.NaN;
		float  resistance = Float.NaN;
		float  acceleration = Float.NaN;
		float  gra = Float.NaN;
		Integer powerOverride = null, pelletOverride = null, accelerationDelayOverride = null, accelerationFuseOverride = null;
		Float speedOverride = null, spreadOverride = null, damageRangeOverride = null, resistanceInWaterOverride = null;
		Double bulletStabilityOverride = null;
		Boolean canBounceOverride = null;
		Boolean blockDestroyOverride = null;
		Boolean canDoorBreachOverride = null;
		String rawPowerOverride = null, rawSpeedOverride = null;
		
		
		String bulletItemName = null;
		String cartItemName = null;
		String bulletModelName = null;
		String cartridgeModelName = null;
		float gunoffset[] = new float[3];
		float gunrotation[] = new float[3];
		boolean needgunoffset = false;

		boolean canobj = false;
		String  objmodel = null;
		String objtexture = "null";
		String attach3dmodel = null;
		String model3dTexture = null;
		String[] attach3dmodels = new String[6];
		String[] model3dTextures = new String[6];
		float inventoryScale = 1.0F;
		float inventoryOffsetX = 0.0F;
		float inventoryOffsetY = 0.0F;
		float inventoryOffsetZ = 0.0F;
		List<HMGItemAttachmentBase> pendingAttachments = new ArrayList<HMGItemAttachmentBase>();
		Item itema = null;
		Item itemb = null;
		Item itemc = null;
		Item itemd = null;
		Item iteme = null;
		Item itemf = null;
		Item itemg = null;
		Item itemh = null;
		Item itemi = null;
		ItemStack[] legacyRecipeIngredients = new ItemStack[GunSmithRecipe.SLOT_COUNT];
		String re1 = "   ";
		String re2 = "   ";
		String re3 = "   ";


		float reduceRecoilLevel = 1f;
		float reduceRecoilLevel_ADS = 1f;
		float reduceSpreadLevel = 1f;
		float reduceSpreadLevel_ADS = 1f;
		boolean isbase = false;
		String tabname = null;
		boolean gunSkin = false;
		String skinTexture = null;
		try {
			File file = file1;
			HMGPackAssetResolver resolver = new HMGPackAssetResolver(file1.getParentFile().getParentFile());
			//File file = new File(configfile,"hmg_handmadeguns.txt");
			if (checkBeforeReadfile(file))
			{

				try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file),"Shift-JIS"))) {
					String str;
					while((str = br.readLine()) != null){  // 1行ずつ読み込む
					//System.out.println(str);
					String[] type = HMGConfigLineParser.parseAttachmentExtensionLine(str);

					int guntype = 0;






					if (type.length != 0)
					{//1
						// Match gun-TXT whitespace handling for ballistic overrides only;
						// attachment/recipe tokens retain their legacy untrimmed semantics.
						String trimmedKey = type[0].trim();
						if ("BulletSpread".equals(trimmedKey) || "BlletSpread".equals(trimmedKey)
								|| "bulletFuse".equals(trimmedKey) || "fuse".equals(trimmedKey)) {
							type[0] = trimmedKey;
						}
						switch (type[0]) {
							case "Texture":
								try { texture = resolver.itemTextureName(type[1]); }
								catch (FileNotFoundException missing) { texture = type[1]; }
								break;
							case "Stack":
								kazu = Integer.parseInt(type[1]);
								break;
							case "Name":
								Namegun = type[1];
								break;
							case "ScopeTexture":
								hud = type[1];
								break;
							case "Zoom":
								zoom = Float.parseFloat(type[1]);
								break;
							case "isnightvision":
								isnightvision = Boolean.parseBoolean(type[1]);
								break;
							case "cosume_onCraft":
								cosume_onCraft = Boolean.parseBoolean(type[1]);
								break;
							case "ZoomRender":
								textureOnly = Boolean.parseBoolean(type[1]);
								break;
							case "ScopeOnly":
								textureOnly = Boolean.parseBoolean(type[1]);
								break;
							case "Model":
								canobj = Boolean.parseBoolean(type[1]);
								break;
							case "ObjModel":
								objmodel = type[1];
								break;
							case "ObjTexture":
							case "ModelTexture":
								objtexture = type[1];
								break;
							case "attach3dmodel":
								attach3dmodel = type.length > 1 ? type[1].trim() : null;
								break;
							case "3dmodeltex":
								model3dTexture = type.length > 1 ? type[1].trim() : null;
								break;
							case "attach3dmodel1": case "attach3dmodel2": case "attach3dmodel3":
							case "attach3dmodel4": case "attach3dmodel5":
								attach3dmodels[type[0].charAt(type[0].length() - 1) - '0'] = type.length > 1 ? type[1].trim() : null;
								break;
							case "3dmodeltex1": case "3dmodeltex2": case "3dmodeltex3":
							case "3dmodeltex4": case "3dmodeltex5":
								model3dTextures[type[0].charAt(type[0].length() - 1) - '0'] = type.length > 1 ? type[1].trim() : null;
								break;
							case "InventoryScale":
								inventoryScale = parseInventoryScale(type, file1);
								break;
							case "InventoryOffset":
								float[] inventoryOffset = parseInventoryOffset(type, file1);
								inventoryOffsetX = inventoryOffset[0];
								inventoryOffsetY = inventoryOffset[1];
								inventoryOffsetZ = inventoryOffset[2];
								break;
							case "ReduceRecoilLevel":
								reduceRecoilLevel = Float.parseFloat(type[1]);
								break;
							case "AntiRecoil":
								reduceRecoilLevel = Float.parseFloat(type[1]);
								break;
							case "AntiRecoil_ADS":
								reduceRecoilLevel_ADS = Float.parseFloat(type[1]);
								break;
							case "AntiBure":
								reduceSpreadLevel = Float.parseFloat(type[1]);
								break;
							case "AntiBure_ADS":
								reduceSpreadLevel_ADS = Float.parseFloat(type[1]);
								break;
							case "AntiSpread":
								reduceSpreadLevel = Float.parseFloat(type[1]);
								break;
							case "AntiSpread_ADS":
								reduceSpreadLevel_ADS = Float.parseFloat(type[1]);
								break;
							case "isBase":
								isbase = Boolean.parseBoolean(type[1]);
								break;
							case "Slowdown":
								slowdownrate = Float.parseFloat(type[1]);
								break;
							case "BulletRound":
								hasRoundOption = true;
								round = Integer.parseInt(type[1]);
								break;
							case "ReloadTimeOption":
								hasReloadOption = true;
								reloadTime = Integer.parseInt(type[1]);
								break;
							case "BulletType":
								bullettype = Integer.parseInt(type[1]);
								break;
							case "BulletPower":
								rawPowerOverride = type[1];
								powerOverride = (int)(Integer.parseInt(type[1]) * HMGGunMaker.damageCof);
								break;
							case "BulletSpeed":
								rawSpeedOverride = type[1];
								speedOverride = Float.parseFloat(type[1]) * HMGGunMaker.speedCof * 2;
								break;
							case "BulletGravity":
							case "gravity":
								gra = Float.parseFloat(type[1]);
								break;
							case "BulletSpread":
							case "BlletSpread":
								spreadOverride = Float.parseFloat(type[1].trim());
								break;
							case "ShotGun_Pellet":
							case "PerFireRound":
								pelletOverride = Math.max(1, Integer.parseInt(type[1]));
								break;
							case "BulletStability": bulletStabilityOverride = Double.parseDouble(type[1]); break;
							case "damageRange": damageRangeOverride = Float.parseFloat(type[1]); break;
							case "ResistanceInWater": resistanceInWaterOverride = Float.parseFloat(type[1]); break;
							case "CanBounce": case "canBounce": canBounceOverride = Boolean.parseBoolean(type[1]); break;
							case "AccelerationDelay": case "accelerationDelay": accelerationDelayOverride = Integer.parseInt(type[1]); break;
							case "AccelerationFuse": case "accelerationFuse": accelerationFuseOverride = Integer.parseInt(type[1]); break;
							case "fuse": case "bulletFuse":
								fuseOverride = Integer.valueOf(type[1].trim());
								fuse = fuseOverride;
								break;
							case "Explosion": case "ExplosionRadius": explosionlevel = Float.parseFloat(type[1]); break;
							case "Explosionlevel":
								explosionlevel = Float.parseFloat(type[1]);
								break;
								
							case "KnockBack":
							case "knockback":
								knockback = Double.parseDouble(type[1]);
								if(type.length > 2) knockbackY = Double.parseDouble(type[2]);
								break;
							case "KnockBackY": case "knockbackY": knockbackY = Double.parseDouble(type[1]); break;
							case "BounceRate":
							case "bouncerate":
								bouncerate = Float.parseFloat(type[1]);
								break;
							case "BounceLimit":
							case "bouncelimit":
								bouncelimit = Float.parseFloat(type[1]);
								break;
							case "Resistance":
							case "resistance":
								resistance = Float.parseFloat(type[1]);
								break;
							case "Acceleration":
							case "acceleration":
								acceleration = Float.parseFloat(type[1]);
								break;

							case "BlockDestroy":
							case "Blockdestroy":
								blockdestroyex = Boolean.parseBoolean(type[1]);
								blockDestroyOverride = blockdestroyex;
								break;
							case "AutoDestroy":
								autoDestroy = Boolean.parseBoolean(type[1]);
								break;
							case "candoorbreach":
								canDoorBreachOverride = Boolean.parseBoolean(type[1]);
								break;
							case "BulletItemName":
								bulletItemName = (type[1]);
								break;
							case "CartItemName":
								cartItemName = (type[1]);
								break;
							case "BulletModelName":
							case "Bulletmodel":
								bulletModelName = (type[1]);
								break;
							case "CartModelName":
								cartridgeModelName = (type[1]);
								break;
							case "CenterPoint":
								for (int i = 0; i < 3; i++)
									gunoffset[i] = Float.parseFloat(type[i + 1]);
								needgunoffset = true;
								break;
							case "Tabname":
								tabname = type[1];
								break;
							case "GunSkin":
								gunSkin = Boolean.parseBoolean(type[1]);
								break;
							case "SkinTexture":
								if (type[1].indexOf(':') >= 0) skinTexture = type[1];
								else {
									try { skinTexture = resolver.resourceLocation(HMGPackAssetResolver.Type.MODEL_TEXTURE, type[1]); }
									catch (FileNotFoundException missing) { skinTexture = type[1]; }
								}
								break;
							case "SkinTarget":
								// Deprecated compatibility key. Skins are universal, so targets are ignored.
								break;
							case "GunRotation":
								for (int i = 0; i < 3; i++)
									gunrotation[i] = Float.parseFloat(type[i + 1]);
								needgunoffset = true;
								break;
						}
						Item newitem = null;
						if(type[0].equals("GunSkinItem") && gunSkin && skinTexture != null){
							GunName = type[1];
							newitem = new HMGItemGunSkin(GunName, skinTexture).setUnlocalizedName(GunName)
									.setTextureName("handmadeguns:" + texture);
							if(Namegun != null) LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							else LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
						}else if(type[0].equals("Model_Sight")){
							GunName = type[1];
							newitem	= new HMGItemSightBase().setUnlocalizedName(GunName)
									.setTextureName("handmadeguns:"+ texture).setCreativeTab(HandmadeGunsCore.tabhmg);
							((HMGItemSightBase)newitem).needgunoffset = needgunoffset;
							((HMGItemSightBase)newitem).gunoffset = gunoffset;
							((HMGItemSightBase)newitem).gunrotation = gunrotation;
							//if (!isentitysprinting() ) {
								((HMGItemSightBase) newitem).zoomlevel = zoom;
							//}
							((HMGItemSightBase)newitem).isnightvision = isnightvision;

							if(hud != null)((HMGItemSightBase)newitem).scopetexture = resolveTexture(resolver, HMGPackAssetResolver.Type.MISC_TEXTURE, hud, "handmadeguns:textures/misc/");
							((HMGItemSightBase)newitem).scopeonly = textureOnly;
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
//							System.out.println("" + GunName);
							Attach.add(newitem);
						}else if(type[0].equals("RedDot")){
							GunName = type[1];
							newitem	= new HMGItemAttachment_reddot().setUnlocalizedName(GunName)
									.setTextureName("handmadeguns:"+ texture).setCreativeTab(HandmadeGunsCore.tabhmg);
							((HMGItemSightBase)newitem).zoomlevel = zoom;
							((HMGItemSightBase)newitem).isnightvision = isnightvision;
							if(hud != null)((HMGItemSightBase)newitem).scopetexture = resolveTexture(resolver, HMGPackAssetResolver.Type.MISC_TEXTURE, hud, "handmadeguns:textures/misc/");
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
//							System.out.println("" + GunName);
							Attach.add(newitem);
						}else if(type[0].equals("SCOPE")){
							GunName = type[1];
							newitem	= new HMGItemAttachment_scope().setUnlocalizedName(GunName)
									.setTextureName("handmadeguns:"+texture).setCreativeTab(HandmadeGunsCore.tabhmg);
							((HMGItemSightBase)newitem).zoomlevel = zoom;
							((HMGItemSightBase)newitem).isnightvision = isnightvision;
							if(hud != null)((HMGItemSightBase)newitem).scopetexture = resolveTexture(resolver, HMGPackAssetResolver.Type.MISC_TEXTURE, hud, "handmadeguns:textures/misc/");
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
//							System.out.println("" + GunName);
							Attach.add(newitem);
						}else if(type[0].equals("Suppressor")){
							GunName = type[1];
							newitem	= new HMGItemAttachment_Suppressor().setUnlocalizedName(GunName)
									.setTextureName("handmadeguns:"+texture).setCreativeTab(HandmadeGunsCore.tabhmg);
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
//							System.out.println("" + GunName);
							Attach.add(newitem);
						}else if(type[0].equals("Laser")){
							GunName = type[1];
							newitem	= new HMGItemAttachment_laser().setUnlocalizedName(GunName)
									.setTextureName("handmadeguns:"+texture).setCreativeTab(HandmadeGunsCore.tabhmg);
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
//							System.out.println("" + GunName);
							Attach.add(newitem);
						}else if(type[0].equals("Model_Laser")){
							GunName = type[1];
							newitem	= new HMGItemAttachment_laser().setUnlocalizedName(GunName)
									.setTextureName("handmadeguns:"+texture).setCreativeTab(HandmadeGunsCore.tabhmg);
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
//							System.out.println("" + GunName);
							Attach.add(newitem);
						}else if(type[0].equals("Right")){
							GunName = type[1];
							newitem	= new HMGItemAttachment_light().setUnlocalizedName(GunName)
									.setTextureName("handmadeguns:"+texture).setCreativeTab(HandmadeGunsCore.tabhmg);
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
//							System.out.println("" + GunName);
							Attach.add(newitem);
						}else if(type[0].equals("Light")){
							GunName = type[1];
							newitem	= new HMGItemAttachment_light().setUnlocalizedName(GunName)
									.setTextureName("handmadeguns:"+texture).setCreativeTab(HandmadeGunsCore.tabhmg);
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
//							System.out.println("" + GunName);
							Attach.add(newitem);
						}else if(type[0].equals("Model_Light")){
							GunName = type[1];
							newitem	= new HMGItemAttachment_light().setUnlocalizedName(GunName)
									.setTextureName("handmadeguns:"+texture).setCreativeTab(HandmadeGunsCore.tabhmg);
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
//							System.out.println("" + GunName);
							Attach.add(newitem);
						}else if(type[0].equals("Grip")){
							GunName = type[1];
							newitem	= new HMGItemAttachment_grip().setUnlocalizedName(GunName)
									.setTextureName("handmadeguns:"+texture).setCreativeTab(HandmadeGunsCore.tabhmg);
							((HMGItemAttachment_grip)newitem).reduceRecoilLevel = reduceRecoilLevel;
							((HMGItemAttachment_grip)newitem).reduceRecoilLevel_ADS= reduceRecoilLevel_ADS;
							((HMGItemAttachment_grip)newitem).reduceSpreadLevel= reduceSpreadLevel;
							((HMGItemAttachment_grip)newitem).reduceSpreadLevel_ADS= reduceSpreadLevel_ADS;
							((HMGItemAttachmentBase)newitem).slowdownrate = slowdownrate;
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
//							System.out.println("" + GunName);
							Attach.add(newitem);
						}else if(type[0].equals("Model_Grip")){
							GunName = type[1];
							newitem	= new HMGItemAttachment_grip().setUnlocalizedName(GunName)
									.setTextureName("handmadeguns:"+texture).setCreativeTab(HandmadeGunsCore.tabhmg);
							((HMGItemAttachment_grip)newitem).reduceRecoilLevel = reduceRecoilLevel;
							((HMGItemAttachment_grip)newitem).reduceRecoilLevel_ADS= reduceRecoilLevel_ADS;
							((HMGItemAttachment_grip)newitem).reduceSpreadLevel = reduceSpreadLevel;
							((HMGItemAttachment_grip)newitem).reduceSpreadLevel_ADS= reduceSpreadLevel_ADS;
							((HMGItemAttachment_grip)newitem).isbase= isbase;
							((HMGItemAttachmentBase)newitem).slowdownrate = slowdownrate;
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
//							System.out.println("" + GunName);
							Attach.add(newitem);
						}
						else if(type[0].equals("Magazine")){
							GunName = type[1];
							newitem	= new HMGItemBullet().setUnlocalizedName(GunName).setMaxStackSize(kazu)
									.setTextureName("handmadeguns:"+texture);
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
//							System.out.println("" + GunName);
							Magazines.add(newitem);
						}
						else if(type[0].equals("CustomMagazine")){
							GunName = type[1];
							newitem	= new HMGItemCustomMagazine().setUnlocalizedName(GunName).setMaxStackSize(kazu)
									.setTextureName("handmadeguns:"+texture);
							
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
							Magazines.add(newitem);
						}
						else if(type[0].equals("SimpleMaterial")){
							GunName = type[1];
							newitem	= new HMG_simpleMaterial().setUnlocalizedName(GunName).setMaxStackSize(kazu)
									.setTextureName("handmadeguns:"+texture);
							if(!cosume_onCraft){
								newitem.setContainerItem(newitem);
							}
							((HMG_simpleMaterial)newitem).cosume_onCraft = cosume_onCraft;
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
						}
						else if(type[0].equals("BulletAP")){
							GunName = type[1];
							newitem	= new HMGItemBullet_AP().setUnlocalizedName(GunName).setMaxStackSize(kazu)
									.setTextureName("handmadeguns:"+texture);
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
						}
						else if(type[0].equals("BulletAT")){
							GunName = type[1];
							newitem	= new HMGItemBullet_AT().setUnlocalizedName(GunName).setMaxStackSize(kazu)
									.setTextureName("handmadeguns:"+texture);
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
						}
						else if(type[0].equals("BulletDart")){
							GunName = type[1];
							newitem	= new HMGItemBullet_AP().setUnlocalizedName(GunName).setMaxStackSize(kazu)
									.setTextureName("handmadeguns:"+texture);
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
						}
						else if(type[0].equals("BulletFrag")){
							GunName = type[1];
							newitem	= new HMGItemBullet_Frag().setUnlocalizedName(GunName).setMaxStackSize(kazu)
									.setTextureName("handmadeguns:"+texture);
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
							//Namegun = null;
						}
						else if(type[0].equals("BulletTE")){
							GunName = type[1];
							//	Name = type[2];
							newitem	= new HMGItemBullet_TE(texture).setUnlocalizedName(GunName).setMaxStackSize(kazu)
									//.setTextureName("minecraft:"+"mods" + File.separatorChar + "handmadeguns/attachment/texture/"+texture)
									.setTextureName("handmadeguns:"+texture);
							if(Namegun != null){
								LanguageRegistry.instance().addNameForObject(newitem, "jp_JP", Namegun);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun);
							}else{
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", GunName);
							}
							//Namegun = null;
						}

						if (newitem != null) {
							// Definitions are loaded in source order. Reconfigure an existing
							// compatible item so an external pack can intentionally override a
							// bundled magazine/attachment without attempting a second registry entry.
							Item registered = GameRegistry.findItem("HandmadeGuns", GunName);
							if (registered != null) {
								if (!registered.getClass().equals(newitem.getClass())) {
									System.err.println("[HMG] Ignoring incompatible item override for " + GunName);
									continue;
								}
								newitem = registered;
								newitem.setMaxStackSize(kazu).setTextureName("handmadeguns:" + texture);
								LanguageRegistry.instance().addNameForObject(newitem, "en_US", Namegun != null ? Namegun : GunName);
							}
							if (newitem instanceof HMGItemAttachmentBase) pendingAttachments.add((HMGItemAttachmentBase)newitem);
							try {
								if (canobj && isClient && !(newitem instanceof HMGItemAttachmentBase)) {
//									System.out.println("" + objmodel);
									IModelCustom attach = HMGGunMaker.getCachedModel(resolver.resourceLocation(HMGPackAssetResolver.Type.MODEL, objmodel));
									//todo gun skins here

									ResourceLocation attachtexture = HMGGunMaker.getCachedResourceLocation(resolver.resourceLocation(HMGPackAssetResolver.Type.MODEL_TEXTURE, objtexture));
									MinecraftForgeClient.registerItemRenderer(newitem, new HMGRenderItemCustom(attach, attachtexture));
								}
							}catch (Throwable e){
								System.err.println("[HMG] Unable to load attachment model for " + GunName + " ("
										+ attach3dmodel + "): " + e.getMessage());
							}
							if(tabname == null) newitem.setCreativeTab(HandmadeGunsCore.tabhmg);
							else if(tabshmg.containsKey(tabname)){
								newitem.setCreativeTab(tabshmg.get(tabname));
							}
							if(newitem instanceof HMGItemCustomMagazine){
								((HMGItemCustomMagazine)newitem).damagemodify = damagemodify;
								((HMGItemCustomMagazine)newitem).speedmodify = speedmodify;
								((HMGItemCustomMagazine)newitem).slowdownrate = slowdownrate;
								((HMGItemCustomMagazine)newitem).bullettype = bullettype;
								((HMGItemCustomMagazine)newitem).hasRoundOption = hasRoundOption;
								((HMGItemCustomMagazine)newitem).round = round;
								((HMGItemCustomMagazine)newitem).hasReloadOption = hasReloadOption;
								((HMGItemCustomMagazine)newitem).reloadTime = reloadTime;
								if(hasRoundOption){
									((HMGItemCustomMagazine)newitem).setMaxDamage(round);
								}
								((HMGItemCustomMagazine)newitem).fuse = fuse;
								((HMGItemCustomMagazine)newitem).fuseOverride = fuseOverride;
								((HMGItemCustomMagazine)newitem).blockdestroyex = blockdestroyex;
								((HMGItemCustomMagazine)newitem).autoDestroy = autoDestroy;
								((HMGItemCustomMagazine)newitem).explosionlevel = explosionlevel;
								
								((HMGItemCustomMagazine)newitem).bulletItemName = bulletItemName;
								((HMGItemCustomMagazine)newitem).cartridgeItemName = cartItemName;
								
								((HMGItemCustomMagazine)newitem).bulletmodel = bulletModelName;
								((HMGItemCustomMagazine)newitem).cartridgeModelName = cartridgeModelName;
								((HMGItemCustomMagazine)newitem).magmodel = objmodel;
								((HMGItemCustomMagazine)newitem).knockback = knockback;
								((HMGItemCustomMagazine)newitem).knockbackY = knockbackY;
								((HMGItemCustomMagazine)newitem).bouncerate = bouncerate;
								((HMGItemCustomMagazine)newitem).bouncelimit = bouncelimit;
								((HMGItemCustomMagazine)newitem).resistance = resistance;
								((HMGItemCustomMagazine)newitem).acceleration = acceleration;
								((HMGItemCustomMagazine)newitem).gra = gra;
								((HMGItemCustomMagazine)newitem).powerOverride = powerOverride;
								((HMGItemCustomMagazine)newitem).speedOverride = speedOverride;
								((HMGItemCustomMagazine)newitem).spreadOverride = spreadOverride;
								((HMGItemCustomMagazine)newitem).pelletOverride = pelletOverride;
								((HMGItemCustomMagazine)newitem).bulletStabilityOverride = bulletStabilityOverride;
								((HMGItemCustomMagazine)newitem).damageRangeOverride = damageRangeOverride;
								((HMGItemCustomMagazine)newitem).resistanceInWaterOverride = resistanceInWaterOverride;
								((HMGItemCustomMagazine)newitem).canBounceOverride = canBounceOverride;
								((HMGItemCustomMagazine)newitem).blockDestroyOverride = blockDestroyOverride;
								((HMGItemCustomMagazine)newitem).canDoorBreachOverride = canDoorBreachOverride;
								((HMGItemCustomMagazine)newitem).accelerationDelayOverride = accelerationDelayOverride;
								((HMGItemCustomMagazine)newitem).accelerationFuseOverride = accelerationFuseOverride;
								if(powerOverride != null || speedOverride != null || spreadOverride != null || pelletOverride != null || fuseOverride != null) {
									HandmadeGunsCore.Debug("[AmmoDebug] ammo=%s source=%s rawPower=%s effectivePower=%s rawSpeed=%s effectiveSpeed=%s damageCof=%s speedCof=%s spread=%s pellet=%s fuse=%s bulletType=%s",
											GunName, file1.getPath(), rawPowerOverride, powerOverride, rawSpeedOverride, speedOverride,
											HMGGunMaker.damageCof, HMGGunMaker.speedCof, spreadOverride, pelletOverride, fuseOverride, bullettype);
								}
							}
							if (registered == null) GameRegistry.registerItem(newitem, GunName);
							if (newitem instanceof HMGItemGunSkin)
								HMGGunSkinRegistry.register((HMGItemGunSkin)newitem);
						}













						if(type[0].equals("addRecipe")){
							Item additem = GameRegistry.findItem("HandmadeGuns", type[1]);
							if(additem != null) {
								int kazu1 = Integer.parseInt(type[2]);
								re1 = type[3];
								re2 = type[4];
								re3 = type[5];
								int ia = Integer.parseInt(type[6]);
								int ib = Integer.parseInt(type[7]);
								int ic = Integer.parseInt(type[8]);
								int id = Integer.parseInt(type[9]);
								int ie = Integer.parseInt(type[10]);
								int ief = Integer.parseInt(type[11]);
								int ig = Integer.parseInt(type[12]);
								int ih = Integer.parseInt(type[13]);
								int ii = Integer.parseInt(type[14]);


								itema = Item.getItemById(ia);
								itemb = Item.getItemById(ib);
								itemc = Item.getItemById(ic);
								itemd = Item.getItemById(id);
								iteme = Item.getItemById(ie);
								itemf = Item.getItemById(ief);
								itemg = Item.getItemById(ig);
								itemh = Item.getItemById(ih);
								itemi = Item.getItemById(ii);


								DeferredHMGRecipes.queueLegacyRecipe(new ItemStack(additem, kazu1), new String[] { re1, re2, re3 },
                                        new Item[] { itema, itemb, itemc, itemd, iteme, itemf, itemg, itemh, itemi });
								itema = null;
								itemb = null;
								itemc = null;
								itemd = null;
								iteme = null;
								itemf = null;
								itemg = null;
								itemh = null;
								itemi = null;
							}


						}
						else if(type[0].equals("addSmelting")){
							Item additem = GameRegistry.findItem("HandmadeGuns", type[1]);
							if(additem != null) {
								float xp = Float.parseFloat(type[2]);

								int ia = Integer.parseInt(type[3]);
								itema = Item.getItemById(ia);


								if(itema != null && additem != null)
								DeferredHMGRecipes.queueSmelting(itema, new ItemStack(additem), xp);
								itema = null;
							}
						}


						if(type[0].equals("Recipe1")){
							re1 = type[1];
						}
						if(type[0].equals("Recipe2")){
							re2 = type[1];
						}
						if(type[0].equals("Recipe3")){
							re3 = type[1];
						}
						if (type[0].length() == 5 && type[0].startsWith("Item")
								&& type[0].charAt(4) >= 'A' && type[0].charAt(4) <= 'I') {
							int ingredientIndex = type[0].charAt(4) - 'A';
							legacyRecipeIngredients[ingredientIndex] = type.length > 1 && !type[1].equals("null")
									? resolveLegacyRecipeIngredient(type, file1) : null;
						}
						//'attachments' (MOST AMMO IS UNDER THIS TAB)
						if (type[0].equals("addNewRecipe")) {

							try {
								Item additem = GameRegistry.findItem(type[1], type[2]);

								if (additem == null) {
									System.out.println("[HMG] ERROR: Item not found for recipe output -> Mod: "
											+ type[1] + " Item: " + type[2]);
									throw new IllegalArgumentException("recipe output was not found");
								}

								int kazu1  = parseInt(type[3]);

								// Attachment-pack addNewRecipe entries populate the Ammo page of
								// the Gun Smithing Table; NEI reads this same registration directly.
								ItemStack output = new ItemStack(additem, kazu1);

								String[] rows = new String[] { re1, re2, re3 };
								DeferredHMGRecipes.queueContentPackRecipe(output, rows, legacyRecipeIngredients, file1, GunSmithRecipeCategory.AMMO);

							} catch (Exception e) {
								System.out.println("[HMG] ERROR: Failed to register crafting recipe for -> "
										+ type[1] + ":" + type[2]);
								e.printStackTrace();
							} finally {
								legacyRecipeIngredients = new ItemStack[GunSmithRecipe.SLOT_COUNT];
								re1 = "   ";
								re2 = "   ";
								re3 = "   ";
							}
						}



					}//1





				}
				for (HMGItemAttachmentBase attachment : pendingAttachments) {
					attachment.attach3dmodel = attach3dmodel;
					attachment.model3dTexture = model3dTexture;
					System.arraycopy(attach3dmodels, 0, attachment.attach3dmodels, 0, attach3dmodels.length);
					System.arraycopy(model3dTextures, 0, attachment.model3dTextures, 0, model3dTextures.length);
					attachment.inventoryScale = inventoryScale;
					attachment.inventoryOffsetX = inventoryOffsetX;
					attachment.inventoryOffsetY = inventoryOffsetY;
					attachment.inventoryOffsetZ = inventoryOffsetZ;
					if (isClient && attachment.getStandalone3dModelSlot() >= 0) registerModelAttachment(attachment, attachment.getUnlocalizedName(), resolver);
					else if (isClient && canobj) {
						IModelCustom model = HMGGunMaker.getCachedModel(resolver.resourceLocation(HMGPackAssetResolver.Type.MODEL, objmodel));
						ResourceLocation tex = HMGGunMaker.getCachedResourceLocation(resolver.resourceLocation(HMGPackAssetResolver.Type.MODEL_TEXTURE, objtexture));
						MinecraftForgeClient.registerItemRenderer(attachment, new HMGRenderItemCustom(model, tex));
					}
					}
				}
			}
			else
			{

			}
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private static ItemStack resolveLegacyRecipeIngredient(String[] type, File sourceFile) {
		if (type.length < 3) {
			HandmadeGunsCore.Debug("[HMG] ERROR: Invalid %s ingredient in %s", type[0], sourceFile.getPath());
			return null;
		}
		Item item = GameRegistry.findItem(type[1], type[2]);
		if (item == null) {
			Block block = GameRegistry.findBlock(type[1], type[2]);
			if (block != null) item = Item.getItemFromBlock(block);
		}
		if (item == null) HandmadeGunsCore.Debug("[HMG] ERROR: Could not resolve legacy ingredient %s:%s (%s) in %s",
				type[1], type[2], type[0], sourceFile.getPath());
		return item == null ? null : new ItemStack(item);
	}

	private static ResourceLocation resolveTexture(HMGPackAssetResolver resolver, HMGPackAssetResolver.Type type,
			String reference, String legacyPrefix) throws IOException {
		try {
			return HMGGunMaker.getCachedResourceLocation(resolver.resourceLocation(type, reference));
		} catch (FileNotFoundException missing) {
			return HMGGunMaker.getCachedResourceLocation(legacyPrefix + reference);
		}
	}

	private static void registerModelAttachment(HMGItemAttachmentBase attachment, String registryName, HMGPackAssetResolver resolver) {
		try {
			IModelCustom[] models = new IModelCustom[6];
			ResourceLocation[] textures = new ResourceLocation[6];
			for (int slot = 0; slot <= 5; slot++) {
				String configuredModel = slot == 0 ? attachment.attach3dmodel : attachment.get3dModel(slot);
				if (configuredModel == null || configuredModel.length() == 0) continue;
				String configuredTexture = slot == 0 ? attachment.model3dTexture : attachment.get3dModelTexture(slot);
				String modelName = HMGGunMaker.resolveAttachmentModel(configuredModel);
				String textureName = HMGGunMaker.resolveAttachmentTexture(configuredTexture, modelName, registryName);
				if (slot == 0) { attachment.attach3dmodel = modelName; attachment.model3dTexture = textureName; }
				else {
					if (attachment.attach3dmodels[slot] != null && attachment.attach3dmodels[slot].length() > 0)
						attachment.attach3dmodels[slot] = modelName;
					if (attachment.model3dTextures[slot] != null && attachment.model3dTextures[slot].length() > 0)
						attachment.model3dTextures[slot] = textureName;
				}
				models[slot] = HMGGunMaker.getCachedModel(resolver.resourceLocation(HMGPackAssetResolver.Type.MODEL, modelName));
				textures[slot] = HMGGunMaker.getCachedResourceLocation(resolver.resourceLocation(HMGPackAssetResolver.Type.MODEL_TEXTURE, textureName));
			}
			MinecraftForgeClient.registerItemRenderer(attachment, HMGRenderItemCustom.forAttachment(models, textures,
					attachment.getStandalone3dModelSlot()));
		} catch (Throwable error) {
			System.err.println("[HMG] Unable to load attachment model for " + registryName + " ("
					+ attachment.attach3dmodel + "): " + error.getMessage());
		}
	}

	/** Re-reads model/render settings for registered items without item or recipe registration. */
	public static void reloadAttachmentSettings(File file) {
		String attach3dmodel = null;
		String model3dTexture = null;
		String[] attach3dmodels = new String[6];
		String[] model3dTextures = new String[6];
		float inventoryScale = 1.0F;
		float[] inventoryOffset = new float[] {0.0F, 0.0F, 0.0F};
		List<HMGItemAttachmentBase> registeredAttachments = new ArrayList<HMGItemAttachmentBase>();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "Shift-JIS"));
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					String[] values = HMGConfigLineParser.parseAttachmentExtensionLine(line);
					if (values.length == 0) continue;
					String key = values[0].trim();
					if ("attach3dmodel".equals(key))
						attach3dmodel = values.length > 1 ? values[1].trim() : null;
					else if ("3dmodeltex".equals(key))
						model3dTexture = values.length > 1 ? values[1].trim() : null;
					else if (key.matches("attach3dmodel[1-5]"))
						attach3dmodels[key.charAt(key.length() - 1) - '0'] = values.length > 1 ? values[1].trim() : null;
					else if (key.matches("3dmodeltex[1-5]"))
						model3dTextures[key.charAt(key.length() - 1) - '0'] = values.length > 1 ? values[1].trim() : null;
					else if ("InventoryScale".equals(key)) inventoryScale = parseInventoryScale(values, file);
					else if ("InventoryOffset".equals(key)) inventoryOffset = parseInventoryOffset(values, file);
					else if (isAttachmentDeclaration(key) && values.length > 1) {
						Item registered = GameRegistry.findItem("HandmadeGuns", values[1].trim());
						if (registered instanceof HMGItemAttachmentBase
								&& !registeredAttachments.contains(registered)) {
							registeredAttachments.add((HMGItemAttachmentBase)registered);
						}
					}
				}
			} finally {
				reader.close();
			}
			for (HMGItemAttachmentBase attachment : registeredAttachments) {
				attachment.attach3dmodel = attach3dmodel;
				attachment.model3dTexture = model3dTexture;
				System.arraycopy(attach3dmodels, 0, attachment.attach3dmodels, 0, attach3dmodels.length);
				System.arraycopy(model3dTextures, 0, attachment.model3dTextures, 0, model3dTextures.length);
				attachment.inventoryScale = inventoryScale;
				attachment.inventoryOffsetX = inventoryOffset[0];
				attachment.inventoryOffsetY = inventoryOffset[1];
				attachment.inventoryOffsetZ = inventoryOffset[2];
				if (attachment.getStandalone3dModelSlot() >= 0)
					registerModelAttachment(attachment, attachment.getUnlocalizedName(),
							new HMGPackAssetResolver(file.getParentFile().getParentFile()));
			}
		} catch (IOException error) {
			System.err.println("[HMG] Unable to reload attachment settings from " + file.getPath()
					+ ": " + error.getMessage());
		}
	}

	private static boolean isAttachmentDeclaration(String key) {
		return "Grip".equals(key) || "Laser".equals(key) || "Light".equals(key)
				|| "Model_Grip".equals(key) || "Model_Laser".equals(key)
				|| "Model_Light".equals(key) || "Model_Sight".equals(key)
				|| "RedDot".equals(key) || "Right".equals(key) || "SCOPE".equals(key)
				|| "Suppressor".equals(key);
	}

	private static float parseInventoryScale(String[] values, File file) {
		String invalidValue = values.length > 1 ? values[1] : "<missing>";
		try {
			float value = Float.parseFloat(invalidValue);
			if (!Float.isNaN(value) && !Float.isInfinite(value) && value > 0.0F) return value;
		} catch (NumberFormatException ignored) {
		}
		System.err.println("[HMG] Invalid InventoryScale in attachment file " + file.getPath()
				+ ": " + invalidValue + "; using 1.0");
		return 1.0F;
	}

	private static float[] parseInventoryOffset(String[] values, File file) {
		String invalidValue = values.length > 1 ? joinValues(values, 1) : "<missing>";
		try {
			if (values.length != 4) throw new NumberFormatException("expected three values");
			float x = Float.parseFloat(values[1]);
			float y = Float.parseFloat(values[2]);
			float z = Float.parseFloat(values[3]);
			if (Float.isNaN(x) || Float.isInfinite(x) || Float.isNaN(y) || Float.isInfinite(y)
					|| Float.isNaN(z) || Float.isInfinite(z)) throw new NumberFormatException("non-finite value");
			return new float[] {x, y, z};
		} catch (NumberFormatException ignored) {
			System.err.println("[HMG] Invalid InventoryOffset in attachment file " + file.getPath()
					+ ": " + invalidValue + "; using 0,0,0");
			return new float[] {0.0F, 0.0F, 0.0F};
		}
	}

	private static String joinValues(String[] values, int start) {
		StringBuilder result = new StringBuilder();
		for (int i = start; i < values.length; i++) {
			if (i > start) result.append(',');
			result.append(values[i]);
		}
		return result.toString();
	}

	private static boolean checkBeforeReadfile(File file){
		if (file.exists()){
			if (file.isFile() && file.canRead()){
				return true;
			}
		}

		return false;
	}
}
