package handmadeguns.items;

import handmadeguns.Util.PlaceGunShooterPosGetter;
import handmadeguns.entity.PlacedGunEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.vecmath.Vector3d;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static handmadeguns.HandmadeGunsCore.cfg_defaultknockback;
import static handmadeguns.HandmadeGunsCore.cfg_defaultknockbacky;

public class GunInfo {
	/** Optional real-world introduction/service year. Null preserves legacy unrestricted behavior. */
	public Integer techYear;
	/** Optional explicit tier override, stored as 0..10 half-steps; -1 means derive from techYear. */
	public int techTierHalfSteps = -1;
	
	public static final UUID field_110179_h = UUID.fromString("254F543F-8B6F-407F-931B-4B76FEB8BA0D");
	//wtf is this
	public int power;
	public int bulletRound = 30;
	public float speed;
	public boolean hasspreadDiffusionSettings = false;
	public float spreadDiffusionMax = 0;
	public float spreadDiffusionmin = 0;
	public float spreadDiffusionRate = 0;
	public float spreadDiffusionHeadRate = 0;
	public float spreadDiffusionWalkRate = 0;
	public float spreadDiffusionReduceRate = 0;
	public float spread_setting = 1;
	public float ads_spread_cof = 0.5f;
	public double recoil;
	public double recoil_sneak;
	public float onTurretScale = 1.0f;
	public boolean restrictTurretMoveSpeed;
	public float turretspeedY = -1;
	public float turretMotorAccelaY = -1;
	public float turretspeedP = -1;
	public float turretMotorAccelaP = -1;
	public float turreboxW = 0.8f;
	public float turreboxH = 0.8f;
	public int turretMaxHP = -1;
	public boolean restrictTurretAngle = false;
	public int canuseclass = -1;
	public int guntype = -1;
	public float cycle = 1;
	public float ex = 0.0F;
	public int rpm = 600;
	//template value
	public boolean destroyBlock = true;
	public String[] soundre= {"handmadeguns:handmadeguns.reload"};
	public float soundrelevel = 1.0f;
	public boolean canobj = true;
	public boolean renderMCcross = true;
	public boolean renderHMGcross = true;
	public String soundco = "handmadeguns:handmadeguns.cooking";
	public float scopezoombase = 1;
	public float scopezoomred = 1;
	public float scopezoomscope = 4;
	public float soundspeed = 1.0F;
	public String soundbase= "handmadeguns:handmadeguns.fire";
	public float soundbaselevel = 4.0f;
	public String soundsu= "handmadeguns:handmadeguns.supu";
	public float soundsuplevel = 1.0f;
	public String lockSound_entity = "handmadeguns:handmadeguns.lockon";
	public boolean lockSound_NoStop = false;
	public String lockSound_block = "handmadeguns:handmadeguns.lockon";
	public float lockpitch_entity = 1;
	public float lockpitch_block = 0.5f;
	public Item[] magazine = new Item[1];
	public int[] reloadTimes = new int[1];
	public int magazineItemCount = 1;
	public boolean perShellReload = false;
	public boolean perShellReloadStages = false;
	public int perShellReloadIntroTime = 0;
	public int perShellReloadEmptyIntroTime = 0;
	public boolean animationEventSounds = false;
	public final Map<String, String> animationSounds = new LinkedHashMap<String, String>();
	public String adstexture = "handmadeguns:textures/misc/ironsight";
	public String adstexturer = "handmadeguns:textures.misc.reddot";
	public String adstextures = "handmadeguns:textures.misc.scope";
	public ResourceLocation lockOnMarker = new ResourceLocation("handmadeguns:textures/items/lockonmarker0.png");
	public ResourceLocation predictMarker = new ResourceLocation("handmadeguns:textures/items/predictMarker.png");
	public boolean zoomren = true;
	public boolean zoomrer = true;
	public boolean zoomres = true;
	public boolean zoomrent = false;
	public boolean zoomrert = false;
	public boolean zoomrest = false;

	//'gun' 2d item texture
	public String texture;
	/** Model-derived inventory art is captured once; ordinary sprites remain the fallback. */
	public boolean useModelAsIcon = true;
	/** Source locations, not stale size/mtime fingerprints. Read lazily by the client cache. */
	public final java.util.List<java.io.File> iconSourceFiles = new java.util.ArrayList<java.io.File>();
	public final java.util.List<String> iconSourceResources = new java.util.ArrayList<String>();
	public String iconLoadedFingerprint;

	public double motion = 1D;
	public double weight = 1D;
	public boolean muzzleflash = true;
	public float soundrespeed = 1.0F;
	public int cocktime = 20;
	public boolean needcock = false;
	public boolean needFirstCock = false;
	public int pellet = 1;
	//01/27
	//02/14
	public int cartType = 1;
	public int magType = 5;
	public int magentityCnt = 1;
	public int cartentityCnt = 1;
	public boolean dropcart = true;
	public boolean cart_cocked = false;
	public boolean dropMagEntity = true;
	//0307
	public String soundunder_gl= "handmadeguns:handmadeguns.cooking";
	public String soundunder_sg= "handmadeguns:handmadeguns.cooking";
	public int under_gl_power = 20;
	public boolean under_gl_canbounce = true;
	public int under_gl_fuse = -1;
	public float under_gl_speed = 2;
	public float under_gl_bure = 5;
	public double under_gl_recoil = 5;
	public float under_gl_gra = 0.01F;
	public int under_sg_power = 4;
	public float under_sg_speed = 3;
	public float under_sg_bure = 20;
	public double under_sg_recoil = 5;
	public float under_sg_gra = 0.029F;
	public float attackDamage = 1;
	public float foruseattackDamage = 1;
	public boolean hasAttachRestriction = false;
	public ArrayList<String> attachwhitelist = new ArrayList<String>();
	public boolean useundergunsmodel = false;
	public float underoffsetpx;
	public float underoffsetpy;
	public float underoffsetpz;
	public float underrotationx;
	public float underrotationy;
	public float underrotationz;
	public float onunderoffsetpx;
	public float onunderoffsetpy;
	public float onunderoffsetpz;
	public float onunderrotationx;
	public float onunderrotationy;
	public float onunderrotationz;
	public float modelscale = 1;
	public float inworldScale = 1;
	/** Shared local anchor for attachments using attach3dmodel. */
	public boolean hasAttachmentLocation = false;
	public float attachmentLocationX = 0;
	public float attachmentLocationY = 0;
	public float attachmentLocationZ = 0;
	public float attachmentLocationRotation = 0;
	/** Slot-specific attachment locations. Index zero is intentionally unused. */
	public final boolean[] hasAttachmentLocations = new boolean[6];
	public final float[] attachmentLocationXs = new float[6];
	public final float[] attachmentLocationYs = new float[6];
	public final float[] attachmentLocationZs = new float[6];
	public final float[] attachmentLocationRotations = new float[6];

	public float inventoryscale = 1;
	/** Parsed on both sides; never inferred from a client renderer. */
	public boolean bedrockPresentation;
	public float inventoryOffsetX = 0.0F;
	public float inventoryOffsetY = 0.0F;
	public float inventoryOffsetZ = 0.0F;
	public boolean grenade = false;
	public boolean hascustombulletmodel = false;
	public boolean hascustomcartridgemodel = false;
	public boolean hascustommagemodel = false;
	public String bulletmodelN = "default";
	public String bulletmodelAR = "default";
	public String bulletmodelAP = "default";
	public String bulletmodelAT = "default";
	public String bulletmodelFrag = "default";
	public String bulletmodelHE = "default";
	public String bulletmodelTE = "default";
	public String bulletmodelGL = "default";
	public String bulletmodelRPG = "byfrou01_Rocket";
	public String bulletmodelMAG = null;
	public String bulletmodelCart = "default";
	public int fuse = 0;
	public double knockback = cfg_defaultknockback;
	public double knockbackY =cfg_defaultknockbacky;
	public double bulletStability =0;
	public float  bouncerate = 0.3f;
	public float  bouncelimit = 90;
	public float  resistance = 0.99f;
	public float  acceleration;
	public int accelerationDelay = 0;
	public int accelerationFuse = -1;
	public float gravity = 0.049F;
	public boolean canbounce = false;
	public ArrayList<Integer> burstcount = new ArrayList<Integer>(){
//		{
//			add(-1);
//		}
	};
	public ArrayList<Float> rates = new ArrayList<Float>(){
//		{
//			add(1);
//		}
	};

	public ArrayList<Float> elevationOffsets = new ArrayList<>();
	public ArrayList<String> elevationOffsets_info = null;
	public boolean userenderscript = false;
	public ScriptEngine renderscript;
	public ScriptEngine script;
	public ScriptEngine script_withGUI;
	public float mat31rotex;
	public float mat31rotey;
	public float mat31rotez;
	public boolean isOneuse = false;
	public boolean guerrila_can_use = true;
	public boolean canInRoot = true;
	public boolean soldiercanstorage = true;
	public boolean use_internal_secondary;
	public boolean canlock = false;
	public boolean canlockBlock = false;
	public boolean canlockEntity = false;
	public boolean displayPredict = false;
	public boolean displayPredict_MoveSight = true;
	public boolean displayPredict_ConsiderMyLooking = true;
	public double seekerSize = 60;
	public float seekerSize_bullet = 90;
	public boolean semiActive = false;
	public boolean SACLOS_Homing = false;
	public boolean chunkLoaderBullet = false;
	public boolean isActive = false;
	public boolean lock_to_Vehicle = false;
	public double lookDown = 1;
	public double radarRange = 1200 * 1200;
	public float induction_precision;
	public float lockOn_MaxSpeed = -1;
	public float lockOn_minSpeed = -1;
	public float lockOn_MaxThrottle = -1;
	public float lockOn_minThrottle = -1;
	public String flashname = null;
	public int flashfuse = 1;
	public float flashScale = 1;
	public boolean canfix;
	public boolean needfix;
	public boolean userOnBarrel = true;
	public boolean fixAsEntity;
	public float[] sightattachoffset = new float[3];
	public PlaceGunShooterPosGetter posGetter = new PlaceGunShooterPosGetter();;
	public float yoffset;
	public double[] sightPosN = new double[3];
	public double[] sightPosR = new double[3];
	public double[] sightPosS = new double[3];
	public Vector3d[] sightOffset_zeroIn = new Vector3d[]{new Vector3d()};
	/** Per-attachment-slot first-person ADS offsets. Index zero is intentionally unused. */
	public final boolean[] hasOpticShift = new boolean[6];
	public final float[] opticShiftX = new float[6];
	public final float[] opticShiftY = new float[6];
	public final float[] opticShiftZ = new float[6];
	public boolean canceler;
	public boolean chargeType;
	public boolean[] hasNightVision = new boolean[]{false,false,false};
	public float turretanglelimtPitchMax = 360;
	public float turretanglelimtPitchmin = -360;
	public float turretanglelimtYawMax = 360;
	public float turretanglelimtYawmin = -360;
	public double torpdraft;
	public float damagerange;

	public boolean hasVT   = false;
	public boolean forceVT   = false;
	public double  VTRange = 10;
	public double  VTWidth = 30;

	public float resistanceinWater;

	public boolean isHighAngleFire;

	public static Vec3 getLook(float p_70676_1_, Entity entity)
	{
	    float f1;
	    float f2;
	    float f3;
	    float f4;
	
	
	
	    f1 = MathHelper.cos(-(entity instanceof EntityLivingBase ? ((EntityLivingBase)entity).rotationYawHead : (entity instanceof PlacedGunEntity ?((PlacedGunEntity) entity).rotationYawGun:entity.rotationYaw)) * 0.017453292F - (float)Math.PI);
	    f2 = MathHelper.sin(-(entity instanceof EntityLivingBase ? ((EntityLivingBase)entity).rotationYawHead : (entity instanceof PlacedGunEntity ?((PlacedGunEntity) entity).rotationYawGun:entity.rotationYaw)) * 0.017453292F - (float)Math.PI);
	    f3 = -MathHelper.cos(-entity.rotationPitch * 0.017453292F);
	    f4 = MathHelper.sin(-entity.rotationPitch * 0.017453292F);
	    return Vec3.createVectorHelper((double)(f2 * f3), (double)f4, (double)(f1 * f3));
	}


	public void setmodelADSPosAndRotation(double px,double py,double pz){
		sightPosN = new double[]{(-px)*0.2 * inworldScale,(-py)*0.2 * inworldScale,-pz*0.2 * inworldScale};
	}
	public void setADSoffsetRed(double px,double py,double pz){
		sightPosR = new double[]{(-px)*0.2 * inworldScale,(-py)*0.2 * inworldScale,-pz*0.2 * inworldScale};
	}
	public void setADSoffsetScope(double px,double py,double pz){
		sightPosS = new double[]{(-px)*0.2 * inworldScale,(-py)*0.2 * inworldScale,-pz*0.2 * inworldScale};
	}
	public void setmodelADSPosAndRotation_ForVehicle(double px,double py,double pz){
		sightPosN = new double[]{(-px) * inworldScale,(-py) * inworldScale,-pz * inworldScale};
	}
	public void setADSoffsetRed_ForVehicle(double px,double py,double pz){
		sightPosR = new double[]{(-px) * inworldScale,(-py) * inworldScale,-pz * inworldScale};
	}
	public void setADSoffsetScope_ForVehicle(double px,double py,double pz){
		sightPosS = new double[]{(-px) * inworldScale,(-py) * inworldScale,-pz * inworldScale};
	}
}
