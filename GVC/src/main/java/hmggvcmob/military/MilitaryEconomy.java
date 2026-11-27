
package hmggvcmob.military;

import cpw.mods.fml.common.FMLLog;
import hmggvcmob.entity.util.PlatoonOBJ;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

public final class MilitaryEconomy extends WorldSavedData {
    public static final long MAX_BALANCE = 9_000_000_000_000_000L;
    private static final String NAME = "GVC_MilitaryAccounts";
    private static final int HISTORY_LIMIT = 32;
    private final Map<UUID, Account> accounts = new HashMap<>();
    private final Map<UUID, EntityMilitaryUnit> loaded = new HashMap<>();
    private final PriorityQueue<Personnel> due = new PriorityQueue<>(Comparator
            .comparingLong((Personnel member) -> member.nextDue)
            .thenComparing(Comparator.comparingInt((Personnel member) -> member.rank).reversed())
            .thenComparing(member -> member.id));

    public MilitaryEconomy(String name) {
        super(name);
    }

    static MilitaryEconomy get() {
        MilitarySystem.requireServerThread();
        WorldServer overworld = MinecraftServer.getServer().worldServerForDimension(0);
        MilitaryEconomy data = (MilitaryEconomy) overworld.mapStorage.loadData(MilitaryEconomy.class, NAME);
        if (data == null) {
            data = new MilitaryEconomy(NAME);
            overworld.mapStorage.setData(NAME, data);
        }
        return data;
    }

    public static long balance(UUID player) {
        Account account = get().accounts.get(player);
        return account == null ? 0 : account.currency;
    }

    /** Applies an exact signed delta. Server-thread callers receive false when it would underflow or overflow. */
    public static boolean adjustBalance(UUID player, long delta) {
        if (player == null || delta < -MAX_BALANCE || delta > MAX_BALANCE) return false;
        MilitaryEconomy data = get();
        Account account = data.account(player);
        if (delta < -account.currency || delta > MAX_BALANCE - account.currency) return false;
        if (delta != 0) {
            account.currency += delta;
            data.changed(account);
        }
        return true;
    }

    static long now() {
        return MinecraftServer.getServer().worldServerForDimension(0).getTotalWorldTime();
    }

    Account account(UUID owner) {
        return accounts.computeIfAbsent(owner, Account::new);
    }

    void changed(Account account) {
        account.revision++;
        markDirty();
    }

    void recruit(EntityMilitaryUnit unit, UnitPresets.Preset preset) {
        Account account = account(unit.owner());
        Personnel member = new Personnel(unit.getUniqueID(), account.owner);
        member.preset = preset.id;
        member.name = preset.name;
        member.tier = preset.tier;
        member.rank = preset.rank;
        member.wage = preset.currencyUpkeep;
        member.ration = preset.foodConsumption;
        if (unit.getHeldItem() != null) member.weapon = net.minecraft.item.Item.itemRegistry.getNameForObject(unit.getHeldItem().getItem());
        member.nextDue = now() + MilitaryConfig.upkeepInterval;
        account.roster.put(member.id, member);
        due.add(member);
        loaded.put(member.id, unit);
        updateAssignment(unit);
        changed(account);
    }

    boolean attach(EntityMilitaryUnit unit) {
        Account account = accounts.get(unit.owner());
        Personnel member = account == null ? null : account.roster.get(unit.getUniqueID());
        if (member == null || !member.active()) return false;
        EntityMilitaryUnit existing = loaded.get(member.id);
        if (existing != null && existing != unit && !existing.isDead) return false;
        if (existing != unit) {
            loaded.put(member.id, unit);
            account.revision++;
        }
        unit.setSupplyPenalty(penalized(member), member.missedFood > MilitaryConfig.graceIntervals && MilitaryConfig.penalizeHungry);
        return true;
    }

    EntityMilitaryUnit loaded(UUID id) {
        EntityMilitaryUnit unit = loaded.get(id);
        return unit != null && unit.isEntityAlive() ? unit : null;
    }

    void unload(EntityMilitaryUnit unit) {
        if (loaded.remove(unit.getUniqueID(), unit)) {
            Account account = accounts.get(unit.owner());
            if (account != null) account.revision++;
        }
    }

    void lost(EntityMilitaryUnit unit) {
        if (loaded.get(unit.getUniqueID()) != unit) return;
        loaded.remove(unit.getUniqueID());
        Account account = accounts.get(unit.owner());
        Personnel member = account == null ? null : account.roster.get(unit.getUniqueID());
        if (member != null && member.active()) retire(account, member, "casualty");
    }

    void updateAssignment(EntityMilitaryUnit unit) {
        Account account = accounts.get(unit.owner());
        Personnel member = account == null ? null : account.roster.get(unit.getUniqueID());
        if (member == null || !member.active()) return;
        PlatoonOBJ squad = unit.getPlatoon();
        String status = squad == null ? "unassigned" : squad.getOrder().name();
        UUID group = squad == null ? null : squad.id();
        UUID leader = squad == null ? null : squad.leaderId();
        int dimension = unit.worldObj.provider.dimensionId;
        if (status.equals(member.assignment) && java.util.Objects.equals(group, member.squad)
                && java.util.Objects.equals(leader, member.leader) && dimension == member.dimension) return;
        member.assignment = status;
        member.squad = group;
        member.leader = leader;
        member.dimension = dimension;
        changed(account);
    }

    void processUpkeep(long now) {
        if (due.isEmpty() || due.peek().nextDue > now) return;
        Map<UUID, EntityPlayerMP> online = new HashMap<>();
        for (Object entry : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) entry;
            online.put(player.getUniqueID(), player);
        }
        while (!due.isEmpty() && due.peek().nextDue <= now) {
            Personnel member = due.remove();
            if (!member.active()) continue;
            Account account = accounts.get(member.owner);
            EntityMilitaryUnit entity = loaded(member.id);
            EntityPlayerMP player = online.get(member.owner);
            member.nextDue = now + MilitaryConfig.upkeepInterval;
            markDirty();
            if ((player != null || MilitaryConfig.upkeepOffline) && (entity != null || MilitaryConfig.upkeepUnloaded)) {
                long wage = member.wage();
                long ration = member.ration();
                boolean paid = account.currency >= wage;
                boolean fed = account.food >= ration;
                if (paid) account.currency -= wage;
                if (fed) account.food -= ration;
                member.missedWage = paid ? 0 : Math.min(member.missedWage + 1, 1000);
                member.missedFood = fed ? 0 : Math.min(member.missedFood + 1, 1000);
                int missed = Math.max(member.missedWage, member.missedFood);
                if (player != null && (missed == 1 || missed == MilitaryConfig.graceIntervals + 1)) {
                    player.addChatComponentMessage(new ChatComponentTranslation("gvc.military.upkeep.warning",
                            member.name, paid ? "" : new ChatComponentTranslation("gvc.military.upkeep.pay"),
                            fed ? "" : new ChatComponentTranslation("gvc.military.upkeep.food")));
                }
                if (MilitaryConfig.desertion && missed >= MilitaryConfig.graceIntervals + MilitaryConfig.desertionIntervals) {
                    retire(account, member, "deserted");
                    loaded.remove(member.id);
                    if (entity != null) entity.setDead();
                    if (player != null) player.addChatComponentMessage(new ChatComponentTranslation("gvc.military.upkeep.deserted", member.name));
                    continue;
                }
                if (entity != null) entity.setSupplyPenalty(penalized(member),
                        member.missedFood > MilitaryConfig.graceIntervals && MilitaryConfig.penalizeHungry);
                changed(account);
            }
            due.add(member);
        }
    }

    private static boolean penalized(Personnel member) {
        return member.missedWage > MilitaryConfig.graceIntervals && MilitaryConfig.penalizeUnpaid
                || member.missedFood > MilitaryConfig.graceIntervals && MilitaryConfig.penalizeHungry;
    }

    private void retire(Account account, Personnel member, String state) {
        member.state = state;
        int remaining = 0;
        for (Personnel entry : account.roster.values()) if (!entry.active()) remaining++;
        Iterator<Personnel> entries = account.roster.values().iterator();
        while (remaining > HISTORY_LIMIT && entries.hasNext()) {
            if (!entries.next().active()) {
                entries.remove();
                remaining--;
            }
        }
        changed(account);
    }

    @Override
    public void readFromNBT(NBTTagCompound root) {
        accounts.clear();
        due.clear();
        loaded.clear();
        NBTTagList list = root.getTagList("Accounts", 10);
        for (int index = 0; index < list.tagCount(); index++) {
            NBTTagCompound tag = list.getCompoundTagAt(index);
            UUID owner = parseUuid(tag.getString("Owner"));
            if (owner == null) continue;
            Account account = account(owner);
            account.currency = bound(tag.getLong("Currency"));
            account.food = bound(tag.getLong("Food"));
            for (String faction : MilitaryRelations.factions()) {
                if (tag.getCompoundTag("Relations").hasKey(faction)) {
                    account.relations.put(faction, MilitaryRelations.clamp(tag.getCompoundTag("Relations").getInteger(faction)));
                }
            }
            NBTTagList roster = tag.getTagList("Roster", 10);
            for (int i = 0; i < roster.tagCount(); i++) {
                Personnel member = Personnel.read(owner, roster.getCompoundTagAt(i));
                if (member == null || account.roster.containsKey(member.id)) continue;
                account.roster.put(member.id, member);
                if (member.active()) due.add(member);
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound root) {
        NBTTagList list = new NBTTagList();
        for (Account account : accounts.values()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Owner", account.owner.toString());
            tag.setLong("Currency", account.currency);
            tag.setLong("Food", account.food);
            NBTTagCompound relations = new NBTTagCompound();
            for (Map.Entry<String, Integer> value : account.relations.entrySet()) {
                relations.setInteger(value.getKey(), value.getValue());
            }
            tag.setTag("Relations", relations);
            NBTTagList roster = new NBTTagList();
            for (Personnel member : account.roster.values()) roster.appendTag(member.write());
            tag.setTag("Roster", roster);
            list.appendTag(tag);
        }
        root.setTag("Accounts", list);
    }

    private static long bound(long value) {
        return Math.max(0, Math.min(MAX_BALANCE, value));
    }

    public static UUID parseUuid(String value) {
        try {
            return value.isEmpty() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException badId) {
            return null;
        }
    }

    static final class Account {
        final UUID owner;
        long currency, food, revision;
        final Map<UUID, Personnel> roster = new LinkedHashMap<>();
        final Map<String, Integer> relations = new HashMap<>();

        Account(UUID owner) {
            this.owner = owner;
        }

        int activeCount() {
            int count = 0;
            for (Personnel member : roster.values()) if (member.active()) count++;
            return count;
        }
    }

    static final class Personnel {
        final UUID id, owner;
        String preset, name, weapon = "", state = "active", assignment = "unassigned";
        UUID squad, leader;
        long nextDue;
        int tier, rank, wage, ration, missedWage, missedFood, dimension;

        Personnel(UUID id, UUID owner) {
            this.id = id;
            this.owner = owner;
        }

        boolean active() {
            return state.equals("active");
        }

        long wage() {
            return Math.max(0L, Math.round(wage * (double) MilitaryConfig.wageMultiplier));
        }

        long ration() {
            return Math.max(0L, Math.round(ration * (double) MilitaryConfig.rationMultiplier));
        }

        NBTTagCompound write() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Id", id.toString());
            tag.setString("Preset", preset);
            tag.setString("Name", name);
            tag.setString("WeaponId", weapon);
            tag.setString("State", state);
            tag.setString("Assignment", assignment);
            if (squad != null) tag.setString("Squad", squad.toString());
            if (leader != null) tag.setString("Leader", leader.toString());
            tag.setLong("Due", nextDue);
            tag.setInteger("Tier", tier);
            tag.setInteger("Rank", rank);
            tag.setInteger("Wage", wage);
            tag.setInteger("Ration", ration);
            tag.setInteger("MissedWage", missedWage);
            tag.setInteger("MissedFood", missedFood);
            tag.setInteger("Dimension", dimension);
            return tag;
        }

        static Personnel read(UUID owner, NBTTagCompound tag) {
            UUID id = parseUuid(tag.getString("Id"));
            if (id == null) return null;
            Personnel member = new Personnel(id, owner);
            member.preset = tag.getString("Preset");
            member.name = tag.getString("Name");
            member.weapon = tag.getString("WeaponId");
            member.state = tag.getString("State");
            member.assignment = tag.getString("Assignment");
            member.squad = parseUuid(tag.getString("Squad"));
            member.leader = parseUuid(tag.getString("Leader"));
            member.nextDue = tag.getLong("Due");
            member.tier = tag.getInteger("Tier");
            member.rank = tag.getInteger("Rank");
            member.wage = tag.getInteger("Wage");
            member.ration = tag.getInteger("Ration");
            member.missedWage = tag.getInteger("MissedWage");
            member.missedFood = tag.getInteger("MissedFood");
            member.dimension = tag.getInteger("Dimension");
            return member;
        }
    }
}
