package hmggvcmob.camp;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.Team;

import static hmggvcmob.GVCMobPlus.cfg_beaconSpawnInterval;
import static hmggvcmob.GVCMobPlus.cfg_beaconSpawnLevel;

public class CampObj extends Team {
	public String campName;
	public String translationKey;

	public String getTranslationKey() {
		return translationKey == null ? campName : translationKey;
	}
	public boolean isOneTime = false;
	public boolean isRaidType = false;
	public String campBlockObjModel = "gvcmob:textures/model/flagmodel.mqo";
	public String campBlockTextureModel = "gvcmob:textures/model/flagtexture.png";
	public String campBlockTextureItem;

	public int flagWidth = 2;
	public int spawnEntitiesAve = cfg_beaconSpawnLevel;
	public int spawnRaiderEntitiesAve = 8;
	public int raiderGroupNum = 8;
	public int maxFlagHeight = 10000;

	public boolean playerIsFriend = false;

	public int flagSpawnInterval = cfg_beaconSpawnInterval;

	public CampObj[] raiderIgnoreList;

	public Block campsBlock;

	public Class<? extends Entity>[] teamEntityClasses;
	public Class<? extends Entity>[] raiderEntityClasses;

	@Override
	public String getRegisteredName() {
		return campName;
	}

	@Override
	public String formatString(String p_142053_1_) {
		return campName;
	}

	@Override
	public boolean func_98297_h() {
		return false;
	}

	@Override
	public boolean getAllowFriendlyFire() {
		return true;
	}
}
