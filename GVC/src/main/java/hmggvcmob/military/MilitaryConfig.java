
package hmggvcmob.military;

import cpw.mods.fml.common.FMLLog;
import net.minecraftforge.common.config.Configuration;

import java.util.Arrays;

public final class MilitaryConfig {
    public enum Respawn { VANILLA, HYBRID, BEACON_ONLY }

    private static final String RECRUITMENT = "Military.Recruitment";
    private static final String OCCUPATION = "Military.Occupation";
    private static final String VILLAGES = "Military.Villages";
    private static final String ECONOMY = "Military.Economy";
    private static final String UPKEEP = "Military.Upkeep";

    public static boolean legacyRecipes, legacyHiring, recruitment, invulnerableTraders;
    public static boolean occupation, reinforceContested, villages, restrictVillagers, blockBeaconOnlyBedSleep;
    public static boolean upkeepUnloaded, upkeepOffline, penalizeUnpaid, penalizeHungry, desertion, diagnostics;
    public static int playerLimit, traderReplacement, reinforcementInterval, reinforcementDistance, reputationPerKill;
    public static int garrisonSize, patrolSize, garrisonRate, patrolRate, territoryRadius, patrolRadius, captureStrength;
    public static int villageWeight, villageOccupation, villageRadius, villageGarrison, villagePatrol, outpostWidth;
    public static int upkeepInterval, upkeepProcessing, graceIntervals, desertionIntervals, maxWithdrawal, hostileCoinDrop;
    public static int[] denominations;
    public static float hungerMultiplier, saturationMultiplier, wageMultiplier, rationMultiplier;
    public static String initialVillageFaction, occupiedMessage;
    public static String[] foodOverrides, foodBlacklist;
    public static String[] factionRelations;
    public static Respawn respawn;
    public static int respawnDistance;

    private MilitaryConfig() {}

    public static void load(Configuration config, int legacyInterval, int legacyStrength) {
        legacyRecipes = config.getBoolean("legacyRecipes", RECRUITMENT, false, "Restore recipes for legacy soldier eggs and deployment devices.");
        legacyHiring = config.getBoolean("legacyHiring", RECRUITMENT, legacyRecipes, "Restore survival use of old recruitment items. Creative use remains available.");
        recruitment = config.getBoolean("enabled", RECRUITMENT, true, "Enable military traders and currency-based hiring.");
        invulnerableTraders = config.getBoolean("invulnerableTraders", RECRUITMENT, true, "Reject all incoming damage to military traders.");
        traderReplacement = config.getInt("traderReplacementTicks", RECRUITMENT, 12000, 0, 1200000, "Ticks after a trader dies before replacement. Zero disables replacement.");
        playerLimit = config.getInt("playerPersonnelLimit", RECRUITMENT, 64, 1, 512, "Maximum active hired personnel per player.");
        reputationPerKill = config.getInt("reputationPerHostileKill", RECRUITMENT, 1, 0, 20,
                "Reputation gained with friendly factions that oppose hostile unowned infantry slain by the player.");

        occupation = config.getBoolean("enabled", OCCUPATION, true, "Enable faction occupation and preset-based garrisons and patrols.");
        reinforceContested = config.getBoolean("reinforceWhileContested", OCCUPATION, false, "Allow an occupied post to receive reinforcements during an attack.");
        reinforcementInterval = config.getInt("reinforcementInterval", OCCUPATION, cap(legacyInterval, 20, 72000), 20, 72000, "Ticks between replacement waves for active settlements.");
        reinforcementDistance = config.getInt("reinforcementDistance", OCCUPATION, 96, 16, 256, "A player must be within this distance to activate reinforcements.");
        garrisonSize = config.getInt("garrisonSize", OCCUPATION, cap(legacyStrength / 2, 0, 16), 0, 16, "Maximum personnel in a base garrison.");
        patrolSize = config.getInt("patrolSize", OCCUPATION, cap(legacyStrength / 4, 0, 16), 0, 16, "Maximum personnel in a base patrol.");
        garrisonRate = config.getInt("garrisonReplacementRate", OCCUPATION, 2, 0, 16, "Maximum new garrison troops per wave.");
        patrolRate = config.getInt("patrolReplacementRate", OCCUPATION, 1, 0, 16, "Maximum new patrol troops per wave.");
        territoryRadius = config.getInt("territoryRadius", OCCUPATION, 48, 16, 128, "Horizontal radius of military territory in blocks.");
        patrolRadius = config.getInt("patrolRadius", OCCUPATION, 24, 8, 64, "Maximum patrol waypoint distance from the beacon.");
        captureStrength = config.getInt("captureStrength", OCCUPATION, 1000, 20, 100000, "Control points per settlement. Players contribute faster than individual troops.");
        factionRelations = config.getStringList("factionRelations", OCCUPATION,
                new String[]{"union,pmc=30", "union,allies=-50", "pmc,allies=-50"}, "Symmetric alliance scores: faction,faction=-100..100.");
        String mode = config.getString("playerRespawnMode", OCCUPATION, "VANILLA", "VANILLA, HYBRID, or BEACON_ONLY.");
        try {
            respawn = Respawn.valueOf(mode.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            FMLLog.warning("[GVC military] Unknown respawn mode %s. Using VANILLA.", mode);
            respawn = Respawn.VANILLA;
        }
        respawnDistance = config.getInt("respawnSearchRadius", OCCUPATION, 192, 16, 512, "Beacon Only search radius around the death or vanilla respawn position.");
        blockBeaconOnlyBedSleep = config.getBoolean("blockBeaconOnlyBedSleep", OCCUPATION, false, "Also prevent sleeping when Beacon Only is active.");

        villages = config.getBoolean("enabled", VILLAGES, true, "Allow fortified posts in newly generated vanilla villages.");
        restrictVillagers = config.getBoolean("restrictHostileTrade", VILLAGES, true, "Close civilian trade to hostile players within an occupied village.");
        occupiedMessage = config.getString("occupiedMessage", VILLAGES, "", "Custom refusal message. An empty value uses the built-in localized message.");
        villageWeight = config.getInt("outpostWeight", VILLAGES, 8, 0, 100, "Relative selection weight of the fortified outpost during village generation.");
        outpostWidth = config.getInt("outpostWidth", VILLAGES, 13, 11, 15, "Square outpost width; even values are rounded down.");
        if ((outpostWidth & 1) == 0) outpostWidth--;
        villageOccupation = config.getInt("hostileOccupationPercent", VILLAGES, 25, 0, 100, "Chance to start occupied by ALLIES instead of the initial faction.");
        initialVillageFaction = config.getString("initialFaction", VILLAGES, "union", "Normal starting faction: union, allies, or pmc.");
        villageRadius = config.getInt("territoryRadius", VILLAGES, 64, 16, 128, "Maximum radius of village occupation.");
        villageGarrison = config.getInt("garrisonSize", VILLAGES, 4, 0, 16, "Maximum village garrison size.");
        villagePatrol = config.getInt("patrolSize", VILLAGES, 3, 0, 16, "Maximum village patrol size.");

        denominations = config.get("denominations", ECONOMY, new int[]{1, 10, 100, 1000},
                "Values of coin items in ascending order. Require four distinct values with the smallest equal to 1.").getIntList();
        if (!validDenominations(denominations)) {
            FMLLog.warning("[GVC military] Invalid coin denominations %s. Using 1, 10, 100, 1000.", Arrays.toString(denominations));
            denominations = new int[]{1, 10, 100, 1000};
        }
        maxWithdrawal = config.getInt("maxWithdrawal", ECONOMY, 100000, 1, 1000000, "Maximum amount per withdrawal. Larger balances can be withdrawn in multiple transactions.");
        hostileCoinDrop = config.getInt("hostileCoinDrop", ECONOMY, 4, 0, 64, "Maximum base-denomination coins dropped by hostile infantry killed by a player. Zero disables drops.");
        hungerMultiplier = config.getFloat("hungerMultiplier", ECONOMY, 2, 0, 1000, "Supply points per hunger point.");
        saturationMultiplier = config.getFloat("saturationMultiplier", ECONOMY, 1, 0, 1000, "Supply points per restored saturation point.");
        foodOverrides = config.getStringList("foodOverrides", ECONOMY, new String[0], "Item-specific values: mod:item[@metadata]=points.");
        foodBlacklist = config.getStringList("foodBlacklist", ECONOMY, new String[]{"minecraft:rotten_flesh", "minecraft:poisonous_potato"},
                "Disallowed items: mod:item or mod:item@metadata.");

        upkeepInterval = config.getInt("interval", UPKEEP, 24000, 20, 1728000, "Elapsed world ticks between charges.");
        upkeepProcessing = config.getInt("processingInterval", UPKEEP, 100, 1, 1200, "Ticks between due-upkeep processing passes.");
        wageMultiplier = config.getFloat("currencyMultiplier", UPKEEP, 1, 0, 100, "Multiplier for preset wages.");
        rationMultiplier = config.getFloat("foodMultiplier", UPKEEP, 1, 0, 100, "Multiplier for preset supply consumption.");
        upkeepUnloaded = config.getBoolean("chargeUnloaded", UPKEEP, true, "Apply upkeep even when the unit is unloaded.");
        upkeepOffline = config.getBoolean("chargeOffline", UPKEEP, false, "Apply upkeep while the owner is offline.");
        graceIntervals = config.getInt("graceIntervals", UPKEEP, 2, 0, 365, "Consecutive failed charges before performance penalties begin.");
        penalizeUnpaid = config.getBoolean("penalizeUnpaid", UPKEEP, true, "Unpaid troops react and aim less effectively after the grace period.");
        penalizeHungry = config.getBoolean("penalizeHungry", UPKEEP, true, "Hungry troops move more slowly and react less effectively after the grace period.");
        desertion = config.getBoolean("desertion", UPKEEP, false, "Allow prolonged shortages to cause desertion.");
        desertionIntervals = config.getInt("desertionIntervals", UPKEEP, 7, 1, 365, "Consecutive failed charges needed for desertion, after the grace period.");
        diagnostics = config.getBoolean("debugLogging", "Military.Diagnostics", false, "Log occupation changes and recruitment transactions.");
    }

    private static boolean validDenominations(int[] values) {
        return values.length == 4 && values[0] == 1 && values[1] > 1 && values[2] > values[1]
                && values[3] > values[2] && values[3] <= 1000000;
    }

    private static int cap(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
