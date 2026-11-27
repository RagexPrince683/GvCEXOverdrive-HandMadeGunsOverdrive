package hmggvcmob.entity.util;

import handmadeguns.entity.IFF;
import handmadevehicle.entity.parts.Modes;
import hmggvcmob.ai.AIInfantryTarget;
import hmggvcmob.entity.IGVCmob;
import hmggvcmob.entity.IPlatoonable;
import hmggvcmob.entity.friend.EntityPMCBase;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import javax.vecmath.Vector3d;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlatoonOBJ {
    public static final int MAX_MEMBERS = 8;
    private static final double SPACING = 2.4;
    private static final String SAVE_KEY = "GVCPlatoon";

    private final UUID id;
    private final Set<UUID> members = new HashSet<>();
    private final List<EntityAndPos> loadedMembers = new ArrayList<>();
    private final Data data;
    public EntityAndPos leader;
    private UUID leaderId;
    private UUID managedOwner, appointedLeader, designatedTarget;
    private int designatedEntityId = -1;
    private long nextTargetLookup;
    private boolean ceaseFire;
    private Formation formation = Formation.DOUBLE_COLUMN;
    private int leaderRank, capacity = MAX_MEMBERS, patrolCapacity;
    private UUID playerId;
    private String legacyPlayerName;
    private long nextUpdate;
    private boolean scheduled;
    private final Vector3d patrolOrigin = new Vector3d();
    private boolean patrolOutbound = true;
    private boolean currentCamp;
    private final Vector3d heading = new Vector3d();
    private boolean hasHeading;

    @Deprecated public Modes platoonMode = Modes.Wait;
    private PlatoonOrder currentOrder = PlatoonOrder.Wait;
    public final Vector3d PlatoonTargetPos = new Vector3d();

    private PlatoonOBJ(Data data, UUID id) {
        this.data = data;
        this.id = id;
    }

    public static PlatoonOBJ create(EntityLiving owner) {
        return create(owner, UUID.randomUUID());
    }

    public static PlatoonOBJ managed(EntityLiving member, UUID id, UUID owner) {
        PlatoonOBJ squad = find(member.worldObj, id);
        if (squad == null) {
            squad = create(member, id);
            squad.managedOwner = owner;
        }
        if (!owner.equals(squad.managedOwner)) throw new IllegalStateException("Unexpected managed platoon owner");
        return squad;
    }

    private static PlatoonOBJ create(EntityLiving owner, UUID id) {
        Data data = Data.get(owner.worldObj);
        PlatoonOBJ platoon = new PlatoonOBJ(data, id);
        platoon.PlatoonTargetPos.set(owner.posX, owner.boundingBox.minY, owner.posZ);
        platoon.patrolOrigin.set(platoon.PlatoonTargetPos);
        data.platoons.put(platoon.id, platoon);
        data.markDirty();
        return platoon;
    }

    public static void save(EntityLiving member, NBTTagCompound tag) {
        PlatoonOBJ platoon = ((IPlatoonable)member).getPlatoon();
        if (platoon != null) tag.setString(SAVE_KEY, platoon.id.toString());
        else if (member.getEntityData().hasKey(SAVE_KEY)) {
            tag.setString(SAVE_KEY, member.getEntityData().getString(SAVE_KEY));
        }
    }

    public static void read(EntityLiving member, NBTTagCompound tag) {
        if (tag.hasKey(SAVE_KEY)) member.getEntityData().setString(SAVE_KEY, tag.getString(SAVE_KEY));
        if (tag.getIntArray("platoonTargetPos").length == 3) {
            member.getEntityData().setIntArray("platoonTargetPos", tag.getIntArray("platoonTargetPos"));
        }
    }

    public static boolean pending(EntityLiving member) {
        return member.getEntityData().hasKey(SAVE_KEY) || member.getEntityData().hasKey("platoonTargetPos");
    }

    public static void restore(EntityLiving member) {
        IPlatoonable unit = (IPlatoonable)member;
        if (unit.getPlatoon() != null || !pending(member)) return;
        NBTTagCompound tag = member.getEntityData();
        UUID id = parseId(tag.getString(SAVE_KEY));
        PlatoonOBJ platoon = id == null ? null : Data.get(member.worldObj).platoons.get(id);
        int[] oldPosition = tag.getIntArray("platoonTargetPos");
        tag.removeTag(SAVE_KEY);
        tag.removeTag("platoonTargetPos");
        if (platoon != null && platoon.members.contains(member.getUniqueID())) {
            unit.setPlatoon(platoon);
        } else if (oldPosition.length == 3) {
            // Older saves did not store that unit's squad ID. Use its saved order without
            // merging neighboring squads just because they were moving toward the same flag.
            platoon = create(member);
            unit.setPlatoon(platoon);
            platoon.order(PlatoonOrder.Go, oldPosition[0] + 0.5, oldPosition[1], oldPosition[2] + 0.5);
            if (member instanceof EntityPMCBase) {
                String name = ((EntityPMCBase)member).leaderEntity_name;
                if (name != null && !name.isEmpty()) {
                    platoon.legacyPlayerName = name;
                    platoon.setOrder(PlatoonOrder.Follow);
                }
            }
        }
    }

    public boolean hasRoom() {
        return members.size() < (patrolCapacity > 0 ? patrolCapacity : capacity);
    }

    public void setPatrolCapacity(int limit) {
        patrolCapacity = limit;
        data.markDirty();
    }

    public int size() {
        return members.size();
    }

    public UUID id() {
        return id;
    }

    public UUID leaderId() {
        return leaderId;
    }

    public UUID managedOwner() {
        return managedOwner;
    }

    public boolean allMembersMatch(java.util.function.Predicate<UUID> test) {
        return members.stream().allMatch(test);
    }

    public boolean allLoadedMembersMatch(java.util.function.Predicate<EntityLiving> test) {
        return loadedMembers.size() == members.size() && loadedMembers.stream().allMatch(member -> test.test(member.entity));
    }

    public void forgetUnloadedMembers(java.util.function.Predicate<UUID> keep) {
        if (managedOwner == null || !members.removeIf(id -> !keep.test(id)
                && loadedMembers.stream().noneMatch(member -> id.equals(member.entity.getUniqueID())))) return;
        if (!members.contains(leaderId)) {
            leaderId = null;
            leaderRank = 0;
            leader = null;
            electLeader();
        }
        if (!members.contains(appointedLeader)) appointedLeader = null;
        data.markDirty();
    }

    public void setAppointedLeader(UUID selected) {
        if (java.util.Objects.equals(appointedLeader, selected)) return;
        appointedLeader = selected;
        leader = null;
        electLeader();
        scheduled = false;
        data.markDirty();
    }

    public static PlatoonOBJ find(World world, UUID id) {
        return id == null ? null : Data.get(world).platoons.get(id);
    }

    public void addMember(EntityLiving entity) {
        for (EntityAndPos member : loadedMembers) {
            if (member.entity == entity) return;
        }
        if (members.add(entity.getUniqueID())) data.markDirty();
        EntityAndPos member = new EntityAndPos(entity);
        loadedMembers.add(member);
        ((IPlatoonable)entity).setPosObj(member);
        int rank = entity instanceof EntityPMCBase ? ((EntityPMCBase)entity).commandRank() : 0;
        if (entity.getUniqueID().equals(appointedLeader) || appointedLeader == null
                && (entity.getUniqueID().equals(leaderId) || leaderId == null || rank > leaderRank)) appoint(member);
        else if (leader == null) electLeader();
        scheduled = false;
    }

    public void removeMember(EntityLiving entity, boolean permanent) {
        loadedMembers.removeIf(member -> member.entity == entity);
        if (permanent && members.remove(entity.getUniqueID())) {
            if (entity.getUniqueID().equals(leaderId)) {
                leaderId = null;
                leaderRank = 0;
                capacity = MAX_MEMBERS;
            }
            if (members.isEmpty()) data.platoons.remove(id);
            data.markDirty();
        }
        if (leader != null && leader.entity == entity) leader = null;
        if (leader == null && !loadedMembers.isEmpty()) electLeader();
        else if (leader != null && leaderId == null) appoint(leader);
        scheduled = false;
    }

    private void electLeader() {
        for (EntityAndPos member : loadedMembers) {
            if (!member.entity.isEntityAlive()) continue;
            if (member.entity.getUniqueID().equals(appointedLeader != null ? appointedLeader : leaderId)) {
                leader = member;
                return;
            }
            if (leader == null || rank(member) > rank(leader)) leader = member;
        }
        if (leaderId == null && leader != null) appoint(leader);
    }

    private int rank(EntityAndPos member) {
        return member.entity instanceof EntityPMCBase ? ((EntityPMCBase)member.entity).commandRank() : 0;
    }

    private void appoint(EntityAndPos member) {
        leader = member;
        UUID selected = member.entity.getUniqueID();
        int rank = rank(member);
        int limit = member.entity instanceof EntityPMCBase ? ((EntityPMCBase)member.entity).commandSize() : MAX_MEMBERS;
        if (selected.equals(leaderId) && leaderRank == rank && capacity == limit) return;
        leaderId = selected;
        leaderRank = rank;
        capacity = limit;
        data.markDirty();
    }

    public PlatoonOrder getOrder() {
        // Preserve writes by callers of the old GVC field and Hasmode interface.
        if (platoonMode != currentOrder.legacyMode) {
            setOrder(PlatoonOrder.fromLegacy(platoonMode));
            platoonMode = currentOrder.legacyMode;
        }
        return currentOrder;
    }

    public void setMode(Modes mode) {
        setOrder(PlatoonOrder.fromLegacy(mode));
    }

    public void setOrder(PlatoonOrder order) {
        platoonMode = order.legacyMode;
        if (currentOrder == order) return;
        currentOrder = order;
        scheduled = false;
        data.markDirty();
    }

    public void order(Modes mode, double x, double y, double z) {
        order(PlatoonOrder.fromLegacy(mode), x, y, z);
    }

    public void order(PlatoonOrder mode, double x, double y, double z) {
        order(mode, x, y, z, null);
    }

    public void order(PlatoonOrder mode, double x, double y, double z, UUID target) {
        if (getOrder() == mode && PlatoonTargetPos.x == x && PlatoonTargetPos.y == y
                && PlatoonTargetPos.z == z && java.util.Objects.equals(designatedTarget, target)) return;
        if (mode == PlatoonOrder.Patrol && leader != null) {
            patrolOrigin.set(leader.entity.posX, leader.entity.boundingBox.minY, leader.entity.posZ);
            patrolOutbound = true;
        }
        playerId = null;
        legacyPlayerName = null;
        designatedTarget = target;
        designatedEntityId = -1;
        nextTargetLookup = 0;
        PlatoonTargetPos.set(x, y, z);
        setOrder(mode);
        scheduled = false;
        data.markDirty();
    }

    public void designate(EntityLivingBase target) {
        if (target != null && target.getUniqueID().equals(designatedTarget)) designatedEntityId = target.getEntityId();
    }

    public void follow(EntityPlayer player) {
        playerId = player.getUniqueID();
        legacyPlayerName = null;
        designatedTarget = null;
        setOrder(PlatoonOrder.Follow);
        scheduled = false;
        data.markDirty();
    }

    public boolean hasCurrentCamp() {
        return currentCamp;
    }

    public void setCurrentCamp(boolean currentCamp) {
        this.currentCamp = currentCamp;
        data.markDirty();
    }

    public boolean allowsEngagement(EntityLivingBase target) {
        if (ceaseFire) return false;
        if (getOrder() == PlatoonOrder.Defend) {
            return target.getDistanceSq(PlatoonTargetPos.x, PlatoonTargetPos.y, PlatoonTargetPos.z) <= 24 * 24;
        }
        return true;
    }

    public boolean ceasesFire() { return ceaseFire; }

    public void setCeaseFire(boolean value) {
        ceaseFire = value;
        if (value) for (EntityAndPos member : loadedMembers) member.entity.setAttackTarget(null);
        data.markDirty();
    }

    public void setFormation(Formation value) {
        formation = value;
        scheduled = false;
        data.markDirty();
    }

    public enum Formation { DOUBLE_COLUMN, LINE, COLUMN, LOOSE }

    public void update(EntityLiving caller) {
        PlatoonOrder order = getOrder();
        long tick = caller.worldObj.getTotalWorldTime();
        if (scheduled && tick < nextUpdate) return;
        nextUpdate = tick + 10 + (id.hashCode() & 7);
        scheduled = true;
        if (leader == null || !leader.entity.isEntityAlive()) {
            leader = null;
            electLeader();
        }
        if (leader == null) return;

        EntityLiving captain = leader.entity;
        boolean hold = order == PlatoonOrder.Wait || order == PlatoonOrder.Land || order == PlatoonOrder.Pause;
        Vector3d anchor = order == PlatoonOrder.Patrol && !patrolOutbound ? patrolOrigin : PlatoonTargetPos;
        if (order == PlatoonOrder.Attack && designatedTarget != null) {
            net.minecraft.entity.Entity registered = captain.worldObj.getEntityByID(designatedEntityId);
            EntityLivingBase marked = registered instanceof EntityLivingBase && designatedTarget.equals(registered.getUniqueID())
                    ? (EntityLivingBase) registered : null;
            if (marked == null && tick >= nextTargetLookup) {
                nextTargetLookup = tick + 100;
                for (Object entry : captain.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, captain.boundingBox.expand(128, 40, 128))) {
                    EntityLivingBase entity = (EntityLivingBase) entry;
                    if (designatedTarget.equals(entity.getUniqueID())) {
                        marked = entity;
                        designatedEntityId = marked.getEntityId();
                        break;
                    }
                }
            }
            if (marked != null && marked.isEntityAlive() && !((IFF) captain).is_this_entity_friend(marked)) {
                anchor = new Vector3d(marked.posX, marked.boundingBox.minY, marked.posZ);
                for (EntityAndPos member : loadedMembers) {
                    EntityLiving soldier = member.entity;
                    double range = soldier.getEntityAttribute(SharedMonsterAttributes.followRange).getAttributeValue();
                    if (AIInfantryTarget.canEngage(soldier, marked, range) && soldier.getEntitySenses().canSee(marked)) {
                        soldier.setAttackTarget(marked);
                    }
                }
            }
        }
        if (order == PlatoonOrder.Patrol && horizontalDistance(captain, anchor) < 9) {
            patrolOutbound = !patrolOutbound;
            anchor = patrolOutbound ? PlatoonTargetPos : patrolOrigin;
            data.markDirty();
        }
        if (order == PlatoonOrder.Follow) {
            EntityPlayer player = playerId == null ? null : caller.worldObj.func_152378_a(playerId);
            if (player == null && legacyPlayerName != null) {
                player = caller.worldObj.getPlayerEntityByName(legacyPlayerName);
                if (player != null) follow(player);
            }
            if (player == null || !player.isEntityAlive()) {
                hold = true;
            } else {
                anchor = new Vector3d(player.posX, player.boundingBox.minY, player.posZ);
            }
        }

        double dx = anchor.x - captain.posX;
        double dz = anchor.z - captain.posZ;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length > 3 && !hold) {
            heading.set(dx / length, 0, dz / length);
            hasHeading = true;
        } else if (!hasHeading) {
            double yaw = Math.toRadians(captain.renderYawOffset);
            heading.set(-Math.sin(yaw), 0, Math.cos(yaw));
            hasHeading = true;
        }
        double forwardX = heading.x;
        double forwardZ = heading.z;
        double stopRange = order == PlatoonOrder.Attack ? 4 : order == PlatoonOrder.Follow ? 5 : 2;
        if (hold || (length < stopRange && Math.abs(anchor.y - captain.boundingBox.minY) < 1.1)) {
            leader.set(captain.posX, captain.boundingBox.minY, captain.posZ);
        } else {
            double offset = length < stopRange + 1 ? 0 : stopRange;
            leader.set(anchor.x - forwardX * offset, anchor.y, anchor.z - forwardZ * offset);
        }

        int slot = 0;
        for (EntityAndPos member : loadedMembers) {
            if (member == leader) continue;
            if (order == PlatoonOrder.Pause) {
                member.set(member.entity.posX, member.entity.boundingBox.minY, member.entity.posZ);
                continue;
            }
            int row = 1 + slot / 2;
            double side = (slot++ % 2 == 0 ? -1 : 1) * SPACING;
            EntityLiving soldier = member.entity;
            double behind = row * SPACING;
            if (formation == Formation.LINE) {
                behind = 0;
                side = (slot % 2 == 0 ? 1 : -1) * ((slot + 1) / 2) * SPACING;
            } else if (formation == Formation.COLUMN) {
                behind = slot * SPACING;
                side = 0;
            } else if (formation == Formation.LOOSE) {
                behind *= 1.7;
                side *= 1.7;
            }
            double x = captain.posX - forwardX * behind + forwardZ * side;
            double z = captain.posZ - forwardZ * behind - forwardX * side;
            if (((IGVCmob)soldier).getMoveToPositionMng().isBlocked()) {
                // A narrow entrance may not admit the formation abreast.
                x = captain.posX - forwardX * slot * 1.7;
                z = captain.posZ - forwardZ * slot * 1.7;
            }
            member.set(x, captain.boundingBox.minY, z);
        }

        EntityLivingBase contact = null;
        for (EntityAndPos member : loadedMembers) {
            EntityLivingBase target = member.entity.getAttackTarget();
            if (target != null && target.isEntityAlive() && member.entity.getEntitySenses().canSee(target)) {
                contact = target;
                break;
            }
        }
        if (contact == null) return;
        for (EntityAndPos member : loadedMembers) {
            EntityLiving soldier = member.entity;
            if (soldier.getAttackTarget() != null || !soldier.isEntityAlive()
                    || ((IFF)soldier).is_this_entity_friend(contact)) continue;
            double range = soldier.getEntityAttribute(SharedMonsterAttributes.followRange).getAttributeValue();
            if (AIInfantryTarget.canEngage(soldier, contact, range) && soldier.getEntitySenses().canSee(contact)) {
                soldier.setAttackTarget(contact);
            }
        }
    }

    private static double horizontalDistance(EntityLiving entity, Vector3d point) {
        double x = entity.posX - point.x;
        double z = entity.posZ - point.z;
        return x * x + z * z;
    }

    private static UUID parseId(String value) {
        if (value.isEmpty()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static class Data extends WorldSavedData {
        private static final String NAME = "GVC_Platoons";
        private final Map<UUID, PlatoonOBJ> platoons = new HashMap<>();

        public Data(String name) {
            super(name);
        }

        private static Data get(World world) {
            Data data = (Data)world.perWorldStorage.loadData(Data.class, NAME);
            if (data == null) {
                data = new Data(NAME);
                world.perWorldStorage.setData(NAME, data);
            }
            return data;
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            platoons.clear();
            NBTTagList list = tag.getTagList("Platoons", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound entry = list.getCompoundTagAt(i);
                UUID id = parseId(entry.getString("Id"));
                if (id == null) continue;
                PlatoonOBJ squad = new PlatoonOBJ(this, id);
                squad.leaderId = parseId(entry.getString("Leader"));
                squad.managedOwner = parseId(entry.getString("ManagedOwner"));
                squad.appointedLeader = parseId(entry.getString("AppointedLeader"));
                squad.designatedTarget = parseId(entry.getString("DesignatedTarget"));
                squad.ceaseFire = entry.getBoolean("CeaseFire");
                int formation = entry.getInteger("Formation");
                if (formation >= 0 && formation < Formation.values().length) squad.formation = Formation.values()[formation];
                squad.leaderRank = entry.getInteger("LeaderRank");
                squad.patrolCapacity = entry.getInteger("PatrolCapacity");
                squad.capacity = entry.hasKey("Capacity") ? Math.max(1, Math.min(33, entry.getInteger("Capacity"))) : MAX_MEMBERS;
                squad.playerId = parseId(entry.getString("Player"));
                squad.legacyPlayerName = entry.hasKey("PlayerName") ? entry.getString("PlayerName") : null;
                PlatoonOrder order = PlatoonOrder.fromId(entry.getInteger("Mode"));
                if (order != null) {
                    squad.currentOrder = order;
                    squad.platoonMode = order.legacyMode;
                }
                squad.PlatoonTargetPos.set(entry.getDouble("X"), entry.getDouble("Y"), entry.getDouble("Z"));
                squad.patrolOrigin.set(entry.getDouble("PatrolX"), entry.getDouble("PatrolY"), entry.getDouble("PatrolZ"));
                squad.patrolOutbound = entry.getBoolean("Outbound");
                squad.currentCamp = entry.getBoolean("Camp");
                NBTTagList roster = entry.getTagList("Members", 8);
                for (int j = 0; j < roster.tagCount() && j < (squad.managedOwner == null ? 33 : 64); j++) {
                    UUID member = parseId(roster.getStringTagAt(j));
                    if (member != null) squad.members.add(member);
                }
                if (!squad.members.isEmpty()) platoons.put(id, squad);
            }
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            NBTTagList list = new NBTTagList();
            for (PlatoonOBJ squad : platoons.values()) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setString("Id", squad.id.toString());
                if (squad.leaderId != null) entry.setString("Leader", squad.leaderId.toString());
                if (squad.managedOwner != null) entry.setString("ManagedOwner", squad.managedOwner.toString());
                if (squad.appointedLeader != null) entry.setString("AppointedLeader", squad.appointedLeader.toString());
                if (squad.designatedTarget != null) entry.setString("DesignatedTarget", squad.designatedTarget.toString());
                entry.setBoolean("CeaseFire", squad.ceaseFire);
                entry.setInteger("Formation", squad.formation.ordinal());
                entry.setInteger("LeaderRank", squad.leaderRank);
                entry.setInteger("PatrolCapacity", squad.patrolCapacity);
                entry.setInteger("Capacity", squad.capacity);
                if (squad.playerId != null) entry.setString("Player", squad.playerId.toString());
                if (squad.legacyPlayerName != null) entry.setString("PlayerName", squad.legacyPlayerName);
                entry.setInteger("Mode", squad.getOrder().id);
                entry.setDouble("X", squad.PlatoonTargetPos.x);
                entry.setDouble("Y", squad.PlatoonTargetPos.y);
                entry.setDouble("Z", squad.PlatoonTargetPos.z);
                entry.setDouble("PatrolX", squad.patrolOrigin.x);
                entry.setDouble("PatrolY", squad.patrolOrigin.y);
                entry.setDouble("PatrolZ", squad.patrolOrigin.z);
                entry.setBoolean("Outbound", squad.patrolOutbound);
                entry.setBoolean("Camp", squad.currentCamp);
                NBTTagList roster = new NBTTagList();
                for (UUID member : squad.members) roster.appendTag(new NBTTagString(member.toString()));
                entry.setTag("Members", roster);
                list.appendTag(entry);
            }
            tag.setTag("Platoons", list);
        }
    }
}
