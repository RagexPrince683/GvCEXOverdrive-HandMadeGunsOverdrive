
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
    public static boolean legacyDevice, radialMenu, quickCommands, requireOfficer, enforceCapacity;
    public static int groupLimit, groupSize, commandRange, targetingRange, markerTicks, commandTimeout, commandCooldown;
    public static boolean neutralize, factionCapture, alignedNeutralize;
    public static int occupationInterval, tradeInterval;
    public static boolean attributePlayer, attributeHired, attributeSquad, attributeVehicle, attributeIndirect, shareReputation;
    public static boolean rewardOwned, rewardReinforcements, rewardTraders, reputationNotices;
    public static int reputationLoss, ownFactionPenalty, reputationWindow, reputationKillLimit, assistTicks, noticeTicks;
    public static int allegianceCooldown, allegiancePenalty, retaliationTicks;
    public static String[] militaryStandings, classificationValues;
    public static boolean regionalSites, abandonedSites, traderMarkers, legacySurfaceCamps;
    public static int regionSize, siteSpacing, siteDensity, influenceStrength, spawnBuffer, reinforcementPercent;
    public static boolean fieldworks, borderWorks, factionRoads;
    public static int borderPercent, roadLength, roadsPerHub;
    public static int weaponLoot, ammunitionLoot, supplyLoot, reinforcementLootCooldown;

    private MilitaryConfig() {}

    public static void load(Configuration config, int legacyInterval, int legacyStrength) {
        legacyRecipes = config.getBoolean("legacyRecipes", RECRUITMENT, false, "Restore recipes for legacy soldier eggs and deployment devices.");
        legacyHiring = config.getBoolean("legacyHiring", RECRUITMENT, legacyRecipes, "Restore survival use of old recruitment items. Creative use remains available.");
        String command = "Military.Command";
        legacyDevice = config.getBoolean("legacyDevice", command, false, "Enable squad control device use and crafting.");
        radialMenu = config.getBoolean("radialMenu", command, true, "Enable the radial command screen on this client.");
        quickCommands = config.getBoolean("quickCommands", command, true, "Enable the contextual keyboard command screen on this client.");
        commandTimeout = config.getInt("quickTimeoutTicks", command, 160, 20, 1200, "Idle time before quick command mode closes.");
        markerTicks = config.getInt("markerTicks", command, 100, 0, 600, "Lifetime of the most recent accepted order marker on this client.");
        groupLimit = config.getInt("groupLimit", command, 24, 1, 48, "Maximum custom groups per player.");
        groupSize = config.getInt("groupSize", command, 32, 1, 64, "Maximum personnel assigned to one group.");
        requireOfficer = config.getBoolean("requireOfficerForGroupOrders", command, false, "Require a deployed ranked group leader to relay orders.");
        enforceCapacity = config.getBoolean("enforceLeaderCapacity", command, false, "Reject group assignments and orders exceeding an assigned leader's capacity.");
        commandRange = config.getInt("commandRange", command, 96, 16, 256, "Maximum command distance to personnel in blocks.");
        targetingRange = config.getInt("targetRange", command, 80, 8, 128, "Maximum server crosshair targeting distance in blocks.");
        commandCooldown = config.getInt("orderCooldownTicks", command, 4, 1, 40, "Minimum time between accepted battlefield order requests.");
        recruitment = config.getBoolean("enabled", RECRUITMENT, true, "Enable military traders and currency-based hiring.");
        invulnerableTraders = config.getBoolean("invulnerableTraders", RECRUITMENT, false, "Reject all incoming damage to military traders.");
        traderMarkers = config.getBoolean("traderReplacementMarkers", RECRUITMENT, true, "Show a floating coin at a vacant trader station.");
        traderReplacement = config.getInt("traderReplacementTicks", RECRUITMENT, 12000, 0, 1200000, "Ticks after a trader dies before replacement. Zero disables replacement.");
        playerLimit = config.getInt("playerPersonnelLimit", RECRUITMENT, 64, 1, 512, "Maximum active hired personnel per player.");
        reputationPerKill = config.getInt("reputationPerHostileKill", RECRUITMENT, 1, 0, 20,
                "Base reputation earned with factions that oppose the defeated unit's faction.");
        String reputation = "Military.Reputation";
        reputationLoss = config.getInt("lossPerKill", reputation, 4, 0, 100, "Base reputation lost with the victim's faction.");
        ownFactionPenalty = config.getInt("allegianceFriendlyFirePenalty", reputation, 6, 0, 100, "Additional reputation lost for killing a member of your pledged faction.");
        attributePlayer = config.getBoolean("attributePlayerKills", reputation, true, "Attribute direct player damage and projectiles.");
        attributeHired = config.getBoolean("attributeHiredKills", reputation, true, "Attribute player-owned soldier kills.");
        attributeSquad = config.getBoolean("attributeSubordinateKills", reputation, true, "Attribute kills by soldiers assigned to a custom group or subordinate squad.");
        attributeVehicle = config.getBoolean("attributeVehicleKills", reputation, true, "Attribute vehicle and mounted gun damage to an available driver or gunner.");
        attributeIndirect = config.getBoolean("attributeIndirectKills", reputation, false, "Attribute environmental deaths to the most recent recorded player damage.");
        shareReputation = config.getBoolean("shareParticipation", reputation, false, "Share credit with players who damaged a victim within the attribution window.");
        rewardOwned = config.getBoolean("rewardPlayerOwnedVictims", reputation, false, "Allow gains from killing hired units. Losses still apply.");
        rewardReinforcements = config.getBoolean("rewardReinforcements", reputation, false, "Allow gains from repeated garrison and patrol replacements.");
        rewardTraders = config.getBoolean("rewardTraderKills", reputation, false, "Allow gains from killing military traders.");
        reputationWindow = config.getInt("rewardWindowTicks", reputation, 1200, 20, 72000, "Duration of the per-player gain budget.");
        reputationKillLimit = config.getInt("rewardedKillsPerWindow", reputation, 16, 0, 1024, "Number of eligible deaths that can grant positive reputation per window. Zero removes the limit.");
        assistTicks = config.getInt("attributionWindowTicks", reputation, 200, 20, 1200, "How far back to consider actual damage participation.");
        retaliationTicks = config.getInt("retaliationTicks", reputation, 160, 0, 1200, "How long a directly attacked soldier remembers a local attacker.");
        reputationNotices = config.getBoolean("notifications", reputation, true, "Show aggregated reputation and unlock notifications.");
        noticeTicks = config.getInt("notificationInterval", reputation, 80, 20, 1200, "Ticks between reputation notice batches.");
        allegianceCooldown = config.getInt("allegianceCooldownTicks", reputation, 1200, 0, 72000, "Cooldown on pledge, switch or renunciation.");
        allegiancePenalty = config.getInt("allegianceLeavingPenalty", reputation, 25, 0, 10000, "Reputation lost with the departing faction when renouncing or switching.");
        militaryStandings = config.getStringList("standings", reputation, new String[]{
                "-1000000,Hostile,0,0", "-100,Unfriendly,0,0", "0,Friendly,1,0", "100,Trusted,2,5",
                "300,Respected,3,10", "600,Honored,4,15", "1000,Distinguished,10,20"
        }, "Each entry is minimum reputation, standing label, maximum recruitment tier, discount percent.");
        classificationValues = config.getStringList("classificationModifiers", reputation, new String[]{
                "heavy=1.25", "marksman=1.25", "anti_armor=1.5"
        }, "Positive point multiplier per preset classification, in classification=multiplier form.");

        occupation = config.getBoolean("enabled", OCCUPATION, true, "Enable faction occupation and preset-based garrisons and patrols.");
        String regions = "Military.Regions";
        regionalSites = config.getBoolean("proceduralSites", regions, true, "Generate regional sites in newly populated Overworld chunks.");
        legacySurfaceCamps = config.getBoolean("legacySurfaceCamps", regions, false, "Also generate the previous Overworld flag camps while regional sites are enabled.");
        abandonedSites = config.getBoolean("abandonedSites", regions, true, "Include unoccupied ruins and abandoned industrial sites.");
        regionSize = config.getInt("regionSizeChunks", regions, 48, 12, 512, "Width of a deterministic faction influence region.");
        siteSpacing = config.getInt("siteGridChunks", regions, 9, 5, 48, "Width of each grid cell eligible for a site.");
        siteDensity = config.getInt("candidatePercent", regions, 90, 0, 100, "Percentage of site cells considered before terrain checks.");
        influenceStrength = config.getInt("influencePercent", regions, 74, 0, 100, "Chance a site uses one of its region's leading factions.");
        spawnBuffer = config.getInt("hostileSpawnBufferBlocks", regions, 256, 0, 2048,
                "Keep hostile procedural sites this far from world spawn and the origin.");
        reinforcementPercent = config.getInt("reinforcementPercent", regions, 100, 0, 200, "Population capacity of newly generated sites relative to their definitions.");
        fieldworks = config.getBoolean("fieldworks", regions, true, "Add terrain-blended fortifications and tunnel sites to the regional pools.");
        borderWorks = config.getBoolean("frontlines", regions, true, "Generate opposing field positions where hostile regions meet.");
        borderPercent = config.getInt("frontlineCandidatePercent", regions, 42, 0, 100, "Chance per eligible border sector before the terrain survey.");
        factionRoads = config.getBoolean("factionRoads", regions, true, "Link nearby faction sites with roads and trails on loaded terrain.");
        roadLength = config.getInt("maximumRoadLength", regions, 288, 48, 384, "Maximum distance between supply road gates.");
        roadsPerHub = config.getInt("roadsPerHub", regions, 3, 1, 6, "Maximum direct roads from a regional seat or settlement.");
        reinforceContested = config.getBoolean("reinforceWhileContested", OCCUPATION, false, "Allow an occupied post to receive reinforcements during an attack.");
        neutralize = config.getBoolean("neutralizeOnDefeat", OCCUPATION, true, "Neutralize a defeated post before an explicit claim. Otherwise the defeated post becomes disabled and stops reinforcing.");
        factionCapture = config.getBoolean("factionCapture", OCCUPATION, true, "Allow pledged players to claim defeated settlements for their faction.");
        alignedNeutralize = config.getBoolean("allowAlignedNeutralization", OCCUPATION, true, "Allow pledged players to leave a defeated post neutralized.");
        occupationInterval = config.getInt("updateInterval", OCCUPATION, 20, 5, 100, "Ticks between staggered occupation updates.");
        tradeInterval = config.getInt("tradeCheckInterval", OCCUPATION, 20, 1, 100, "Ticks between checks of open civilian trade windows.");
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
        outpostWidth = config.getInt("outpostWidth", VILLAGES, 21, 11, 29, "Square occupation office width; even values are rounded down.");
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
        weaponLoot = config.getInt("weaponLootPercent", ECONOMY, 22, 0, 100, "Chance to salvage a hostile military unit's weapon.");
        ammunitionLoot = config.getInt("ammunitionLootPercent", ECONOMY, 45, 0, 100, "Chance to recover compatible ammunition from a hostile military unit.");
        supplyLoot = config.getInt("supplyLootPercent", ECONOMY, 30, 0, 100, "Chance to salvage a faction supply from a hostile military unit.");
        reinforcementLootCooldown = config.getInt("reinforcementLootCooldownTicks", ECONOMY, 600, 0, 72000,
                "Minimum time between supply-bearing casualties from the same reinforcing site.");
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
