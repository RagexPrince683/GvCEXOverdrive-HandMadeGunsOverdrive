
package hmggvcmob.military;

import cpw.mods.fml.common.FMLLog;
import hmggvcmob.entity.IflagBattler;
import hmggvcmob.entity.util.PlatoonOBJ;
import hmggvcmob.entity.util.PlatoonOrder;
import hmggvcmob.tile.TileEntityFlag;
import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class MilitaryOccupation {
    private MilitaryOccupation() {}

    public static void update(TileEntityFlag flag, Occupation post, World world) {
        long now = world.getTotalWorldTime();
        if (Math.floorMod(now + flag.xCoord * 11L + flag.zCoord * 7L, 20) != 0) return;
        WorldSavedData_Flag data = WorldSavedData_Flag.get(world);
        if (MilitaryConfig.recruitment && post.trader == null && post.nextTrader <= now) {
            if (spawnTrader(world, post)) data.markDirty();
            else post.nextTrader = now + 100;
        }
        if (!MilitaryConfig.occupation || world.difficultySetting == EnumDifficulty.PEACEFUL) return;
        boolean wasContested = post.attacked;
        if (Math.floorMod(now + flag.xCoord * 11L + flag.zCoord * 7L, 40) == 0) {
            capture(world, post, data);
        }
        int previous = flag.flagHeight;
        int previousCountdown = flag.respawncycle;
        flag.flagHeight = post.control * post.campObj.maxFlagHeight / MilitaryConfig.captureStrength;
        flag.campObj = post.campObj;
        if (post.nextReinforcement <= now && (!post.attacked || MilitaryConfig.reinforceContested)
                && world.getClosestPlayer(flag.xCoord + .5, flag.yCoord + .5, flag.zCoord + .5, MilitaryConfig.reinforcementDistance) != null) {
            post.nextReinforcement = now + MilitaryConfig.reinforcementInterval;
            reinforce(world, post, false);
            reinforce(world, post, true);
            data.markDirty();
        }
        flag.respawncycle = (int) MathHelper.clamp_double(post.nextReinforcement - now, 0, MilitaryConfig.reinforcementInterval);
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
            else if (MilitaryRelations.playerRelation(player, post.faction()) < 0) enemies.merge(new Claimant("pmc", player.getUniqueID()), 6, Integer::sum);
        }
        for (Object entry : world.getEntitiesWithinAABB(EntityLiving.class, area)) {
            if (!(entry instanceof IflagBattler) || !((EntityLiving) entry).isEntityAlive()) continue;
            IflagBattler unit = (IflagBattler) entry;
            String faction = MilitaryRelations.faction(unit.getCampObj());
            UUID owner = unit instanceof EntityMilitaryUnit ? ((EntityMilitaryUnit)unit).owner() : null;
            if (supporting(post, owner, faction)) friendly++;
            else if (owner == null ? MilitaryRelations.factionRelation(faction, post.faction()) < 0
                    : MilitaryRelations.reputation(owner, post.faction()) < 0) {
                enemies.merge(new Claimant(owner == null ? faction : "pmc", owner), 1, Integer::sum);
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
        if (contested) {
            int competing = enemies.values().stream().mapToInt(Integer::intValue).sum() - pressure;
            post.control = Math.max(0, Math.min(MilitaryConfig.captureStrength, post.control + (friendly + competing - pressure) * 10));
        } else {
            post.control = Math.min(MilitaryConfig.captureStrength, post.control + Math.max(1, friendly) * 5);
        }
        if (lead != null && post.control == 0) {
            transfer(world, post, lead.faction, lead.owner);
        } else if (previous != post.control || wasContested != contested) {
            data.markDirty();
        }
    }

    private static boolean supporting(Occupation post, UUID owner, String faction) {
        if (owner != null) return owner.equals(post.owner) || MilitaryRelations.reputation(owner, post.faction()) > 0;
        return MilitaryRelations.factionRelation(faction, post.faction()) > 0;
    }

    public static void transfer(World world, Occupation post, String faction, UUID claimant) {
        post.campObj = MilitaryRelations.camp(faction);
        post.owner = claimant;
        post.control = MilitaryConfig.captureStrength;
        post.attacked = false;
        post.tenure++;
        post.garrison = null;
        post.patrol = null;
        post.nextReinforcement = world.getTotalWorldTime() + MilitaryConfig.reinforcementInterval;
        WorldSavedData_Flag.get(world).markDirty();
        TileEntityFlag flag = getFlag(world, post);
        if (flag != null) {
            flag.campObj = post.campObj;
            flag.flagHeight = flag.campObj.maxFlagHeight;
            flag.markDirty();
            world.markBlockForUpdate(flag.xCoord, flag.yCoord, flag.zCoord);
        }
        MilitarySystem.closeHostileTrade();
        if (MilitaryConfig.diagnostics) FMLLog.info("[GVC military] Settlement %s now belongs to %s (%s)", post.id, faction, claimant);
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
        final String faction;
        final UUID owner;

        Claimant(String faction, UUID owner) {
            this.faction = faction;
            this.owner = owner;
        }

        String key() {
            return faction + ":" + owner;
        }

        @Override public int hashCode() { return Objects.hash(faction, owner); }

        @Override public boolean equals(Object value) {
            if (!(value instanceof Claimant)) return false;
            Claimant other = (Claimant) value;
            return faction.equals(other.faction) && Objects.equals(owner, other.owner);
        }
    }
}
