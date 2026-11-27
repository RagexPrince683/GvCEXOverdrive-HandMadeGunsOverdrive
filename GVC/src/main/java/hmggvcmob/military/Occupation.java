
package hmggvcmob.military;

import hmggvcmob.camp.CampObj;
import hmggvcmob.camp.CampObjAndPos;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;

import java.util.UUID;

public final class Occupation extends CampObjAndPos {
    public enum State { ACTIVE, DISABLED, CONTESTED, NEUTRALIZED, FACTION_CONTROLLED }

    public final UUID id;
    public final String type;
    public final int radius, patrolRadius;
    public UUID owner, trader, garrison, patrol;
    public int traderX, traderY, traderZ;
    public long nextTrader, nextReinforcement;
    public int control, tenure;
    public boolean established;
    private String factionId;
    public State state = State.ACTIVE;
    UUID defeatedBy;

    public Occupation(int x, int y, int z, CampObj camp, String type) {
        this(UUID.randomUUID(), x, y, z, camp, type,
                "village".equals(type) ? MilitaryConfig.villageRadius : MilitaryConfig.territoryRadius, MilitaryConfig.patrolRadius);
        control = MilitaryConfig.captureStrength;
    }

    private Occupation(UUID id, int x, int y, int z, CampObj camp, String type, int radius, int patrolRadius) {
        super(new int[]{x, y, z}, camp);
        this.id = id;
        this.type = type;
        this.radius = MathHelper.clamp_int(radius, 16, 128);
        this.patrolRadius = MathHelper.clamp_int(patrolRadius, 8, Math.min(64, this.radius));
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
        if (trader != null) tag.setString("Trader", trader.toString());
        if (garrison != null) tag.setString("Garrison", garrison.toString());
        if (patrol != null) tag.setString("Patrol", patrol.toString());
        tag.setIntArray("TraderPosition", new int[]{traderX, traderY, traderZ});
        tag.setLong("NextTrader", nextTrader);
        tag.setLong("NextReinforcement", nextReinforcement);
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
        if (MilitaryRelations.validId(faction)) result.factionId = faction;
        result.state = result.attacked ? State.CONTESTED : State.ACTIVE;
        if (tag.hasKey("BeaconState")) {
            try { result.state = State.valueOf(tag.getString("BeaconState")); }
            catch (IllegalArgumentException ignored) {}
        }
        result.defeatedBy = MilitaryEconomy.parseUuid(tag.getString("DefeatedBy"));
        result.established = tag.getBoolean("Established");
        result.owner = MilitaryEconomy.parseUuid(tag.getString("Owner"));
        result.trader = MilitaryEconomy.parseUuid(tag.getString("Trader"));
        result.garrison = MilitaryEconomy.parseUuid(tag.getString("Garrison"));
        result.patrol = MilitaryEconomy.parseUuid(tag.getString("Patrol"));
        int[] traderPos = tag.getIntArray("TraderPosition");
        if (traderPos.length == 3) {
            result.traderX = traderPos[0];
            result.traderY = traderPos[1];
            result.traderZ = traderPos[2];
        }
        result.nextTrader = tag.getLong("NextTrader");
        result.nextReinforcement = tag.getLong("NextReinforcement");
        return result;
    }
}
