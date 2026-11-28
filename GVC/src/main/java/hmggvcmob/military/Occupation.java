
package hmggvcmob.military;

import hmggvcmob.camp.CampObj;
import hmggvcmob.camp.CampObjAndPos;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.MathHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class Occupation extends CampObjAndPos {
    public enum State { ACTIVE, DISABLED, CONTESTED, NEUTRALIZED, FACTION_CONTROLLED }
    public enum Classification { SETTLEMENT, INFLUENCE, ABANDONED }

    public final UUID id;
    public final String type;
    public final int radius, patrolRadius;
    public UUID owner, garrison, patrol;
    public long nextReinforcement;
    long nextSpoils;
    public int control, tenure;
    public boolean established;
    private String factionId;
    public State state = State.ACTIVE;
    UUID defeatedBy;
    private final Map<String, TraderLocation> traders = new LinkedHashMap<>();
    private Classification classification = Classification.SETTLEMENT;
    private boolean capture = true, beacon = true;
    private int garrisonLimit = -1, patrolLimit = -1;
    private int sitePrice = 100;
    String hierarchy = "local";
    String[] equipment = {}, quests = {};
    int[] entrance = {}, patrolPoint = {}, guardPoint = {};
    String guardPreset = "";
    int approach;
    int footprint;
    boolean underground;

    public Occupation(int x, int y, int z, CampObj camp, String type) {
        this(UUID.randomUUID(), x, y, z, camp, type,
                "village".equals(type) ? MilitaryConfig.villageRadius : MilitaryConfig.territoryRadius, MilitaryConfig.patrolRadius);
        control = MilitaryConfig.captureStrength;
        traders.put("main", new TraderLocation("main", "quartermaster"));
    }

    static Occupation generated(int x, int y, int z, String faction, MilitarySites.Definition site, long time) {
        Occupation post = new Occupation(UUID.randomUUID(), x, y, z, MilitaryRelations.camp(faction), site.id,
                site.radius, site.patrolRadius);
        post.setFaction(faction);
        post.classification = site.classification;
        post.capture = site.capture;
        post.beacon = site.beacon;
        post.garrisonLimit = site.garrison;
        post.patrolLimit = site.patrol;
        post.sitePrice = site.pricePercent;
        post.hierarchy = site.hierarchy;
        post.equipment = site.equipment;
        post.quests = site.quests;
        post.guardPreset = site.guardPreset;
        post.established = true;
        post.state = faction.isEmpty() ? State.NEUTRALIZED : State.ACTIVE;
        post.control = faction.isEmpty() ? 0 : MilitaryConfig.captureStrength;
        post.nextReinforcement = time + MilitaryConfig.reinforcementInterval;
        return post;
    }

    private Occupation(UUID id, int x, int y, int z, CampObj camp, String type, int radius, int patrolRadius) {
        super(new int[]{x, y, z}, camp);
        this.id = id;
        this.type = type;
        this.radius = MathHelper.clamp_int(radius, 8, 128);
        this.patrolRadius = MathHelper.clamp_int(patrolRadius, 4, Math.min(64, this.radius));
        factionId = MilitaryRelations.faction(camp);
    }

    public String faction() {
        return factionId;
    }

    void setFaction(String faction) {
        factionId = faction;
        campObj = MilitaryRelations.camp(faction);
    }

    public boolean occupied() { return state != State.NEUTRALIZED && state != State.DISABLED; }
    public Classification classification() { return classification; }
    public boolean isRegionalSite() { return garrisonLimit >= 0; }
    int pricePercent() { return sitePrice; }
    void configureRegion(int price, int staffing, String[] equipment, String[] quests) {
        sitePrice = sitePrice * price / 100;
        garrisonLimit = Math.min(16, (garrisonLimit * staffing + 50) / 100);
        patrolLimit = Math.min(16, (patrolLimit * staffing + 50) / 100);
        if (equipment.length > 0) this.equipment = equipment;
        if (quests.length > 0) this.quests = quests;
    }
    public boolean hasBeaconService() { return beacon; }
    public boolean isUnderground() { return underground; }
    boolean capturable() { return capture; }
    int personnelLimit(boolean patrolling) { return patrolling ? patrolLimit : garrisonLimit; }
    Collection<TraderLocation> traders() { return Collections.unmodifiableCollection(traders.values()); }
    TraderLocation trader(String key) { return traders.get(key); }
    void addTrader(TraderLocation location) { traders.put(location.key, location); }

    String traderFaction(TraderLocation location) {
        if (occupied() && MilitaryRelations.available(factionId)) return factionId;
        return location.neutral && MilitaryRelations.available("crossroads") ? "crossroads" : "";
    }

    public boolean isVillage() {
        return "village".equals(type);
    }

    public boolean contains(double x, double z) {
        double dx = x - flagPos[0] - .5;
        double dz = z - flagPos[2] - .5;
        return dx * dx + dz * dz <= radius * radius;
    }

    public boolean permits(net.minecraft.entity.player.EntityPlayer player) {
        String allegiance = player.worldObj.isRemote ? "" : MilitaryRelations.allegiance(player.getUniqueID());
        MilitaryRelations.Faction definition = MilitaryRelations.definition(faction());
        return permitsTrade(player) && (MilitaryRelations.playerRelation(player, faction()) >= 0
                || faction().equals(allegiance) && definition != null && definition.memberInfrastructure);
    }

    boolean permitsTrade(net.minecraft.entity.player.EntityPlayer player) {
        return occupied() && (owner == null || owner.equals(player.getUniqueID())) && MilitaryRelations.permitsTrade(player, faction());
    }

    public NBTTagCompound write() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Id", id.toString());
        tag.setIntArray("Position", flagPos);
        tag.setString("Camp", campObj.campName);
        tag.setString("Faction", factionId);
        tag.setString("BeaconState", state.name());
        if (defeatedBy != null) tag.setString("DefeatedBy", defeatedBy.toString());
        tag.setString("Type", type);
        tag.setInteger("Radius", radius);
        tag.setInteger("PatrolRadius", patrolRadius);
        tag.setInteger("Control", control);
        tag.setInteger("Tenure", tenure);
        tag.setBoolean("Contested", attacked);
        tag.setBoolean("Established", established);
        if (owner != null) tag.setString("Owner", owner.toString());
        if (garrison != null) tag.setString("Garrison", garrison.toString());
        if (patrol != null) tag.setString("Patrol", patrol.toString());
        tag.setString("Classification", classification.name());
        tag.setBoolean("Capturable", capture);
        tag.setBoolean("BeaconService", beacon);
        tag.setInteger("GarrisonLimit", garrisonLimit);
        tag.setInteger("PatrolLimit", patrolLimit);
        tag.setInteger("SitePrice", sitePrice);
        tag.setString("Hierarchy", hierarchy);
        tag.setTag("EquipmentPool", writePool(equipment));
        tag.setTag("QuestPool", writePool(quests));
        if (entrance.length == 3) {
            tag.setIntArray("Entrance", entrance);
            tag.setInteger("Approach", approach);
        }
        if (patrolPoint.length == 3) tag.setIntArray("RoadPatrolPoint", patrolPoint);
        if (guardPoint.length == 3) tag.setIntArray("GuardPoint", guardPoint);
        if (footprint > 0) tag.setInteger("Footprint", footprint);
        if (underground) tag.setBoolean("Underground", true);
        if (!guardPreset.isEmpty()) tag.setString("GuardPreset", guardPreset);
        NBTTagList locations = new NBTTagList();
        for (TraderLocation location : traders.values()) locations.appendTag(location.write());
        tag.setTag("TraderLocations", locations);
        tag.setLong("NextReinforcement", nextReinforcement);
        tag.setLong("NextSpoils", nextSpoils);
        return tag;
    }

    public static Occupation read(NBTTagCompound tag) {
        UUID id = MilitaryEconomy.parseUuid(tag.getString("Id"));
        int[] pos = tag.getIntArray("Position");
        CampObj camp = hmggvcmob.GVCMobPlus.campsHash.get(tag.getString("Camp"));
        if (id == null || camp == null || pos.length != 3) return null;
        Occupation result = new Occupation(id, pos[0], pos[1], pos[2], camp, tag.getString("Type"),
                tag.getInteger("Radius"), tag.getInteger("PatrolRadius"));
        result.control = tag.getInteger("Control");
        result.tenure = tag.getInteger("Tenure");
        result.attacked = tag.getBoolean("Contested");
        String faction = tag.getString("Faction");
        if (tag.hasKey("Faction") && (faction.isEmpty() || MilitaryRelations.validId(faction))) result.factionId = faction;
        result.state = result.attacked ? State.CONTESTED : State.ACTIVE;
        if (tag.hasKey("BeaconState")) {
            try { result.state = State.valueOf(tag.getString("BeaconState")); }
            catch (IllegalArgumentException ignored) {}
        }
        result.defeatedBy = MilitaryEconomy.parseUuid(tag.getString("DefeatedBy"));
        result.established = tag.getBoolean("Established");
        result.owner = MilitaryEconomy.parseUuid(tag.getString("Owner"));
        result.garrison = MilitaryEconomy.parseUuid(tag.getString("Garrison"));
        result.patrol = MilitaryEconomy.parseUuid(tag.getString("Patrol"));
        if (tag.hasKey("Classification")) {
            try { result.classification = Classification.valueOf(tag.getString("Classification")); }
            catch (IllegalArgumentException ignored) {}
        }
        if (tag.hasKey("Capturable")) result.capture = tag.getBoolean("Capturable");
        if (tag.hasKey("BeaconService")) result.beacon = tag.getBoolean("BeaconService");
        if (tag.hasKey("GarrisonLimit")) result.garrisonLimit = MathHelper.clamp_int(tag.getInteger("GarrisonLimit"), -1, 16);
        if (tag.hasKey("PatrolLimit")) result.patrolLimit = MathHelper.clamp_int(tag.getInteger("PatrolLimit"), -1, 16);
        if (tag.hasKey("SitePrice")) result.sitePrice = MathHelper.clamp_int(tag.getInteger("SitePrice"), 6, 1600);
        if (MilitaryRelations.validId(tag.getString("Hierarchy"))) result.hierarchy = tag.getString("Hierarchy");
        result.equipment = readPool(tag.getTagList("EquipmentPool", 8));
        result.quests = readPool(tag.getTagList("QuestPool", 8));
        result.entrance = readPoint(tag, "Entrance");
        result.approach = tag.getInteger("Approach") & 3;
        result.patrolPoint = readPoint(tag, "RoadPatrolPoint");
        result.guardPoint = readPoint(tag, "GuardPoint");
        result.footprint = MathHelper.clamp_int(tag.getInteger("Footprint"), 0, 23);
        result.underground = tag.getBoolean("Underground");
        if (MilitaryRelations.validId(tag.getString("GuardPreset"))) result.guardPreset = tag.getString("GuardPreset");
        if (tag.hasKey("TraderLocations", 9)) {
            NBTTagList list = tag.getTagList("TraderLocations", 10);
            for (int i = 0; i < Math.min(8, list.tagCount()); i++) {
                TraderLocation location = TraderLocation.read(list.getCompoundTagAt(i));
                if (location != null) result.addTrader(location);
            }
        } else {
            TraderLocation location = new TraderLocation("main", "quartermaster");
            location.entity = MilitaryEconomy.parseUuid(tag.getString("Trader"));
            location.position(tag.getIntArray("TraderPosition"));
            location.nextTrader = tag.getLong("NextTrader");
            location.waiting = location.entity == null && location.y > 0 && location.nextTrader > 0;
            result.addTrader(location);
        }
        result.nextReinforcement = tag.getLong("NextReinforcement");
        result.nextSpoils = tag.getLong("NextSpoils");
        return result;
    }

    private static int[] readPoint(NBTTagCompound tag, String key) {
        int[] point = tag.getIntArray(key);
        return point.length == 3 && point[1] > 0 && point[1] < 254 ? point : new int[0];
    }

    private static NBTTagList writePool(String[] pool) {
        NBTTagList list = new NBTTagList();
        for (String value : pool) list.appendTag(new NBTTagString(value));
        return list;
    }

    private static String[] readPool(NBTTagList list) {
        java.util.List<String> pool = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(128, list.tagCount()); i++) {
            String value = list.getStringTagAt(i);
            if (!value.isEmpty() && value.length() <= 160) pool.add(value);
        }
        return pool.toArray(new String[0]);
    }

    static final class TraderLocation {
        final String key, role;
        int x, y, z;
        boolean neutral, waiting;
        UUID entity;
        long nextTrader, retry, commerceEpoch = Long.MIN_VALUE, liquidityUsed;
        final Map<String, Integer> stockUsed = new LinkedHashMap<>();

        TraderLocation(String key, String role) {
            this.key = key;
            this.role = role;
        }

        void position(int[] pos) {
            if (pos.length != 3 || pos[1] < 1 || pos[1] > 253) return;
            x = pos[0];
            y = pos[1];
            z = pos[2];
        }

        NBTTagCompound write() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Key", key);
            tag.setString("Role", role);
            tag.setIntArray("Position", new int[]{x, y, z});
            tag.setBoolean("Neutral", neutral);
            tag.setBoolean("Waiting", waiting);
            tag.setLong("NextTrader", nextTrader);
            tag.setLong("CommerceEpoch", commerceEpoch);
            tag.setLong("LiquidityUsed", liquidityUsed);
            NBTTagCompound stock = new NBTTagCompound();
            for (Map.Entry<String, Integer> entry : stockUsed.entrySet()) stock.setInteger(entry.getKey(), entry.getValue());
            tag.setTag("StockUsed", stock);
            if (entity != null) tag.setString("Entity", entity.toString());
            return tag;
        }

        static TraderLocation read(NBTTagCompound tag) {
            String key = tag.getString("Key"), role = tag.getString("Role");
            if (!MilitaryRelations.validId(key) || !MilitaryRelations.validId(role)) return null;
            TraderLocation location = new TraderLocation(key, role);
            location.position(tag.getIntArray("Position"));
            location.entity = MilitaryEconomy.parseUuid(tag.getString("Entity"));
            location.neutral = tag.getBoolean("Neutral");
            location.waiting = tag.getBoolean("Waiting");
            location.nextTrader = tag.getLong("NextTrader");
            if (tag.hasKey("CommerceEpoch")) location.commerceEpoch = tag.getLong("CommerceEpoch");
            location.liquidityUsed = Math.max(0, tag.getLong("LiquidityUsed"));
            NBTTagCompound stock = tag.getCompoundTag("StockUsed");
            for (Object entry : stock.func_150296_c()) {
                String id = (String)entry;
                int used = stock.getInteger(id);
                if (id.length() <= 160 && used > 0) location.stockUsed.put(id, used);
            }
            return location;
        }
    }
}
