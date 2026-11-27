
package hmggvcmob.military;

import cpw.mods.fml.common.FMLLog;
import hmggvcmob.entity.IflagBattler;
import hmggvcmob.entity.util.PlatoonOBJ;
import hmggvcmob.entity.util.PlatoonOrder;
import hmggvcmob.tile.TileEntityFlag;
import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MilitaryOccupation {
    private MilitaryOccupation() {}

    public static void update(TileEntityFlag flag, Occupation post, World world) {
        long now = world.getTotalWorldTime();
        long phase = now + flag.xCoord * 11L + flag.zCoord * 7L;
        if (Math.floorMod(phase, MilitaryConfig.occupationInterval) != 0) return;
        WorldSavedData_Flag data = WorldSavedData_Flag.get(world);
        if (post.occupied() && MilitaryConfig.recruitment && post.trader == null && post.nextTrader <= now) {
            if (spawnTrader(world, post)) data.markDirty();
            else post.nextTrader = now + 100;
        }
        if (!MilitaryConfig.occupation) return;
        boolean wasContested = post.attacked;
        boolean simulated = world.difficultySetting != EnumDifficulty.PEACEFUL;
        if (post.occupied() && simulated && Math.floorMod(phase, MilitaryConfig.occupationInterval * 2) == 0) {
            capture(world, post, data);
        }
        int previous = flag.flagHeight;
        int previousCountdown = flag.respawncycle;
        flag.flagHeight = post.occupied() ? post.control * post.campObj.maxFlagHeight / MilitaryConfig.captureStrength : 0;
        flag.campObj = post.campObj;
        if (post.occupied() && simulated && post.nextReinforcement <= now && (!post.attacked || MilitaryConfig.reinforceContested)
                && world.getClosestPlayer(flag.xCoord + .5, flag.yCoord + .5, flag.zCoord + .5, MilitaryConfig.reinforcementDistance) != null) {
            post.nextReinforcement = now + MilitaryConfig.reinforcementInterval;
            reinforce(world, post, false);
            reinforce(world, post, true);
            data.markDirty();
        }
        flag.respawncycle = post.occupied() ? (int) MathHelper.clamp_double(post.nextReinforcement - now, 0, MilitaryConfig.reinforcementInterval) : 0;
        if (previous != flag.flagHeight || previousCountdown / 100 != flag.respawncycle / 100
                || wasContested != post.attacked) {
            flag.markDirty();
            world.markBlockForUpdate(flag.xCoord, flag.yCoord, flag.zCoord);
        }
    }

    private static void capture(World world, Occupation post, WorldSavedData_Flag data) {
        int[] pos = post.flagPos;
        int range = Math.min(20, post.radius);
        AxisAlignedBB area = AxisAlignedBB.getBoundingBox(pos[0] - range, pos[1] - 8, pos[2] - range,
                pos[0] + range + 1, pos[1] + 9, pos[2] + range + 1);
        int friendly = 0;
        Map<Claimant, Integer> enemies = new HashMap<>();
        for (Object entry : world.getEntitiesWithinAABB(EntityPlayer.class, area)) {
            EntityPlayer player = (EntityPlayer) entry;
            if (!player.isEntityAlive() || player.capabilities.disableDamage) continue;
            if (supporting(post, player.getUniqueID(), "pmc")) friendly += 6;
            else if (MilitaryRelations.hostileToPlayer(player.getUniqueID(), post.faction())) enemies.merge(new Claimant(player.getUniqueID()), 6, Integer::sum);
        }
        for (Object entry : world.getEntitiesWithinAABB(EntityLiving.class, area)) {
            if (!(entry instanceof IflagBattler) || !((EntityLiving) entry).isEntityAlive()) continue;
            IflagBattler unit = (IflagBattler) entry;
            String faction = MilitaryRelations.faction((EntityLiving) entry);
            UUID owner = unit instanceof EntityMilitaryUnit ? ((EntityMilitaryUnit)unit).owner() : null;
            if (supporting(post, owner, faction)) friendly++;
            else if (owner != null && MilitaryRelations.hostileToPlayer(owner, post.faction())) {
                enemies.merge(new Claimant(owner), 1, Integer::sum);
            }
        }
        Claimant lead = null;
        int pressure = 0;
        for (Map.Entry<Claimant, Integer> contender : enemies.entrySet()) {
            if (contender.getValue() > pressure || contender.getValue() == pressure
                    && contender.getKey().key().compareTo(lead.key()) < 0) {
                lead = contender.getKey();
                pressure = contender.getValue();
            }
        }
        boolean contested = lead != null;
        int previous = post.control;
        boolean wasContested = post.attacked;
        post.attacked = contested;
        post.state = contested ? Occupation.State.CONTESTED : post.tenure > 0 && post.owner == null
                ? Occupation.State.FACTION_CONTROLLED : Occupation.State.ACTIVE;
        if (contested) {
            int competing = enemies.values().stream().mapToInt(Integer::intValue).sum() - pressure;
            post.control = Math.max(0, Math.min(MilitaryConfig.captureStrength, post.control + (friendly + competing - pressure) * 10));
        } else {
            post.control = Math.min(MilitaryConfig.captureStrength, post.control + Math.max(1, friendly) * 5);
        }
        if (lead != null && post.control == 0) {
            String allegiance = MilitaryRelations.allegiance(lead.owner);
            boolean claimPossible = canClaim(lead.owner, post);
            boolean canNeutralize = MilitaryConfig.neutralize && (MilitaryConfig.alignedNeutralize || allegiance.isEmpty() || !claimPossible);
            deactivate(world, post, lead.owner, canNeutralize ? Occupation.State.NEUTRALIZED : Occupation.State.DISABLED);
        } else if (previous != post.control || wasContested != contested) {
            data.markDirty();
        }
    }

    private static boolean supporting(Occupation post, UUID owner, String faction) {
        if (owner != null) return owner.equals(post.owner) || !MilitaryRelations.hostileToPlayer(owner, post.faction())
                && (MilitaryRelations.reputation(owner, post.faction()) > 0 || post.faction().equals(MilitaryRelations.allegiance(owner)));
        return MilitaryRelations.factionRelation(faction, post.faction()) > 0;
    }

    public static void transfer(World world, Occupation post, String faction, UUID claimant) {
        post.setFaction(faction);
        post.owner = claimant;
        post.control = MilitaryConfig.captureStrength;
        post.attacked = false;
        post.state = Occupation.State.FACTION_CONTROLLED;
        post.defeatedBy = null;
        post.nextReinforcement = world.getTotalWorldTime() + MilitaryConfig.reinforcementInterval;
        resetGarrison(post);
        updateBeacon(world, post);
        if (MilitaryConfig.diagnostics) FMLLog.info("[GVC military] Settlement %s now belongs to %s (%s)", post.id, faction, claimant);
    }

    private static void deactivate(World world, Occupation post, UUID player, Occupation.State state) {
        post.state = state;
        post.defeatedBy = player;
        post.owner = null;
        post.attacked = false;
        post.control = 0;
        post.trader = null;
        post.nextTrader = world.getTotalWorldTime() + MilitaryConfig.occupationInterval;
        resetGarrison(post);
        updateBeacon(world, post);
    }

    private static void resetGarrison(Occupation post) {
        post.tenure++;
        post.garrison = null;
        post.patrol = null;
    }

    private static void updateBeacon(World world, Occupation post) {
        WorldSavedData_Flag.get(world).markDirty();
        TileEntityFlag flag = getFlag(world, post);
        if (flag != null) {
            flag.campObj = post.campObj;
            flag.flagHeight = post.occupied() ? flag.campObj.maxFlagHeight : 0;
            if (!post.occupied()) flag.respawncycle = 0;
            flag.markDirty();
            world.markBlockForUpdate(flag.xCoord, flag.yCoord, flag.zCoord);
        }
        MilitarySystem.closeHostileTrade();
    }

    static NBTTagCompound nearby(EntityPlayer player) {
        NBTTagCompound result = new NBTTagCompound();
        Occupation post = nearbyPost(player);
        if (post == null) return result;
        result.setString("Id", post.id.toString());
        MilitaryRelations.Faction faction = MilitaryRelations.definition(post.faction());
        result.setString("Faction", faction == null ? post.faction() : faction.name);
        result.setString("Type", post.type);
        result.setString("State", post.state.name());
        result.setBoolean("Claim", !post.occupied() && canClaim(player.getUniqueID(), post) && mayResolve(player, post));
        result.setBoolean("Neutralize", post.state == Occupation.State.DISABLED && mayResolve(player, post)
                && (MilitaryConfig.alignedNeutralize || !canClaim(player.getUniqueID(), post)));
        return result;
    }

    static String resolve(EntityPlayer player, String id, boolean claim) {
        Occupation post = nearbyPost(player);
        if (post == null || !post.id.toString().equals(id) || post.occupied() || !mayResolve(player, post)) return "capture_denied";
        if (claim && canClaim(player.getUniqueID(), post)) {
            transfer(player.worldObj, post, MilitaryRelations.allegiance(player.getUniqueID()), null);
            return "occupied";
        }
        if (!claim && post.state == Occupation.State.DISABLED
                && (MilitaryConfig.alignedNeutralize || !canClaim(player.getUniqueID(), post))) {
            deactivate(player.worldObj, post, player.getUniqueID(), Occupation.State.NEUTRALIZED);
            return "neutralized";
        }
        return "capture_denied";
    }

    private static Occupation nearbyPost(EntityPlayer player) {
        if (!MilitaryConfig.occupation) return null;
        TileEntityFlag flag = TileEntityFlag.findNearest(player.worldObj, player.posX, player.posY, player.posZ, 12, false,
                tile -> !tile.vehicleBaseType && !tile.campObj.isOneTime && !tile.campObj.isRaidType);
        return flag == null ? null : flag.occupation();
    }

    private static boolean mayResolve(EntityPlayer player, Occupation post) {
        return player.getUniqueID().equals(post.defeatedBy) || MilitaryRelations.hostileToPlayer(player.getUniqueID(), post.faction());
    }

    private static boolean canClaim(UUID player, Occupation post) {
        String allegiance = MilitaryRelations.allegiance(player);
        MilitaryRelations.Faction faction = MilitaryRelations.definition(allegiance);
        return MilitaryConfig.factionCapture && faction != null && faction.settlementCapture && !allegiance.equals(post.faction())
                && !MilitaryRelations.hostileToPlayer(player, allegiance);
    }

    private static TileEntityFlag getFlag(World world, Occupation post) {
        int[] pos = post.flagPos;
        if (!world.getChunkProvider().chunkExists(pos[0] >> 4, pos[2] >> 4)) return null;
        net.minecraft.tileentity.TileEntity tile = world.getTileEntity(pos[0], pos[1], pos[2]);
        return tile instanceof TileEntityFlag ? (TileEntityFlag) tile : null;
    }

    private static boolean spawnTrader(World world, Occupation post) {
        EntityMilitaryTrader trader = new EntityMilitaryTrader(world);
        ChunkCoordinates spot = findSpace(world, post.flagPos[0], post.flagPos[1], post.flagPos[2], 1, 6, true, trader);
        if (spot == null) return false;
        post.traderX = spot.posX;
        post.traderY = spot.posY;
        post.traderZ = spot.posZ;
        trader.bind(post);
        post.trader = trader.getUniqueID();
        if (world.spawnEntityInWorld(trader)) return true;
        post.trader = null;
        return false;
    }

    private static void reinforce(World world, Occupation post, boolean patrol) {
        int limit = patrol ? MilitaryConfig.patrolSize : MilitaryConfig.garrisonSize;
        if (post.isVillage()) limit = patrol ? MilitaryConfig.villagePatrol : MilitaryConfig.villageGarrison;
        int rate = patrol ? MilitaryConfig.patrolRate : MilitaryConfig.garrisonRate;
        if (limit == 0 || rate == 0) return;
        PlatoonOBJ team = PlatoonOBJ.find(world, patrol ? post.patrol : post.garrison);
        int count = Math.min(rate, limit - (team == null ? 0 : team.size()));
        if (count <= 0) return;
        String faction = post.faction();
        List<UnitPresets.Preset> presets = UnitPresets.pool(faction, true);
        int weight = presets.stream().mapToInt(preset -> preset.reinforcementWeight).sum();
        if (weight <= 0) return;

        for (int i = 0; i < count; i++) {
            EntityMilitaryUnit unit = new EntityMilitaryUnit(world);
            ChunkCoordinates spot = findSpace(world, post.flagPos[0], post.flagPos[1], post.flagPos[2], 2, 8, false, unit);
            if (spot == null) break;
            UnitPresets.Preset preset = choosePreset(presets, world.rand.nextInt(weight));
            unit.configure(preset, null, post);
            ChunkCoordinates waypoint = team == null && patrol
                    ? findSpace(world, post.flagPos[0], post.flagPos[1], post.flagPos[2],
                            Math.max(4, post.patrolRadius / 2), post.patrolRadius, false, unit) : null;
            unit.setLocationAndAngles(spot.posX + .5, spot.posY, spot.posZ + .5, unit.rotationYaw, 0);
            if (!world.spawnEntityInWorld(unit)) continue;
            if (team == null) {
                team = PlatoonOBJ.create(unit);
                team.setPatrolCapacity(limit);
                if (patrol) post.patrol = team.id();
                else post.garrison = team.id();
                unit.setPlatoon(team);
                if (waypoint == null) team.order(PlatoonOrder.Defend, spot.posX + .5, spot.posY, spot.posZ + .5);
                else team.order(PlatoonOrder.Patrol, waypoint.posX + .5, waypoint.posY, waypoint.posZ + .5);
            } else {
                unit.setPlatoon(team);
            }
        }
    }

    private static UnitPresets.Preset choosePreset(List<UnitPresets.Preset> presets, int draw) {
        for (UnitPresets.Preset preset : presets) {
            draw -= preset.reinforcementWeight;
            if (draw < 0) return preset;
        }
        throw new IllegalArgumentException("Invalid reinforcement selection");
    }

    static ChunkCoordinates findSpace(World world, int x, int y, int z, int inner, int outer,
                                      boolean sameChunk, EntityLiving entity) {
        for (int i = 0; i < 36; i++) {
            int dx = world.rand.nextInt(outer * 2 + 1) - outer;
            int dz = world.rand.nextInt(outer * 2 + 1) - outer;
            int distance = dx * dx + dz * dz;
            if (distance < inner * inner || distance > outer * outer) continue;
            int sx = x + dx;
            int sz = z + dz;
            if ((sameChunk && ((sx >> 4) != (x >> 4) || (sz >> 4) != (z >> 4)))
                    || !world.getChunkProvider().chunkExists(sx >> 4, sz >> 4)) continue;
            for (int level = 0; level < 9; level++) {
                int dy = (level + 1) / 2 * (level % 2 == 0 ? -1 : 1);
                int sy = y + dy;
                if (sy < 1 || sy > 253 || !world.isSideSolid(sx, sy - 1, sz, ForgeDirection.UP)) continue;
                entity.setLocationAndAngles(sx + .5, sy, sz + .5, entity.rotationYaw, 0);
                if (!world.checkNoEntityCollision(entity.boundingBox) || !world.getCollidingBoundingBoxes(entity, entity.boundingBox).isEmpty()
                        || world.isAnyLiquid(entity.boundingBox)) continue;
                return new ChunkCoordinates(sx, sy, sz);
            }
        }
        return null;
    }

    private static final class Claimant {
        final UUID owner;

        Claimant(UUID owner) { this.owner = owner; }

        String key() {
            return owner.toString();
        }

        @Override public int hashCode() { return owner.hashCode(); }

        @Override public boolean equals(Object value) {
            if (!(value instanceof Claimant)) return false;
            Claimant other = (Claimant) value;
            return owner.equals(other.owner);
        }
    }
}
