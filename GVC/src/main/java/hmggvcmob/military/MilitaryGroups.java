package hmggvcmob.military;

import hmggvcmob.entity.util.PlatoonOBJ;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

final class MilitaryGroups {
    static final int CREATE = 0, RENAME = 1, DELETE = 2, ASSIGN = 3, LEADER = 4, MOVE = 5, SLOT = 6;
    static final String ALL = "all", UNGROUPED = "-";

    private MilitaryGroups() {}

    static String edit(EntityPlayer owner, int action, String payload) {
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(owner.getUniqueID());
        int separator = payload.indexOf('|');
        String id = separator < 0 ? payload : payload.substring(0, separator);
        String argument = separator < 0 ? "" : payload.substring(separator + 1);
        Group group = account.groups.get(MilitaryEconomy.parseUuid(id));
        if (action == CREATE) {
            if (!validName(payload) || account.groups.size() >= MilitaryConfig.groupLimit) return "group_invalid";
            group = new Group(UUID.randomUUID(), payload.trim());
            account.groups.put(group.id, group);
        } else if (action == ASSIGN) {
            if (group == null && !UNGROUPED.equals(id)) return "group_invalid";
            if (!assign(owner, economy, account, group, argument)) return "group_invalid";
        } else if (group == null) {
            return "group_invalid";
        } else if (action == RENAME) {
            if (!validName(argument)) return "group_invalid";
            group.name = argument.trim();
            for (MilitaryEconomy.Personnel member : account.roster.values()) {
                EntityMilitaryUnit unit = group.id.equals(member.group) ? economy.loaded(member.id) : null;
                if (unit != null) unit.platoonName = group.name;
            }
        } else if (action == DELETE) {
            account.groups.remove(group.id);
            for (MilitaryEconomy.Personnel member : account.roster.values()) {
                if (group.id.equals(member.group)) {
                    member.group = null;
                    refresh(owner, economy, member);
                }
            }
        } else if (action == LEADER) {
            UUID leader = MilitaryEconomy.parseUuid(argument);
            MilitaryEconomy.Personnel member = account.roster.get(leader);
            if (!argument.isEmpty() && (member == null || !member.active() || member.rank == 0
                    || !group.id.equals(member.group)
                    || MilitaryConfig.enforceCapacity && size(account, group.id) > capacity(member))) return "group_invalid";
            group.leader = leader;
            for (MilitaryEconomy.Personnel person : account.roster.values()) {
                if (group.id.equals(person.group)) refresh(owner, economy, person);
            }
        } else if (action == MOVE) {
            List<Group> list = new ArrayList<>(account.groups.values());
            int old = list.indexOf(group);
            int next = old + ("up".equals(argument) ? -1 : "down".equals(argument) ? 1 : 0);
            if (old == next || next < 0 || next >= list.size()) return "group_invalid";
            java.util.Collections.swap(list, old, next);
            account.groups.clear();
            for (Group entry : list) account.groups.put(entry.id, entry);
        } else if (action == SLOT) {
            if (!argument.matches("[0-8]")) return "group_invalid";
            int slot = Integer.parseInt(argument);
            if (slot != 0) for (Group entry : account.groups.values()) {
                if (entry.slot == slot) entry.slot = 0;
            }
            group.slot = slot;
        } else {
            return "group_invalid";
        }
        economy.changed(account);
        return "group_saved";
    }

    private static boolean assign(EntityPlayer owner, MilitaryEconomy economy, MilitaryEconomy.Account account,
                                  Group group, String argument) {
        UUID destination = group == null ? null : group.id;
        Set<MilitaryEconomy.Personnel> selected = new LinkedHashSet<>();
        int count = size(account, destination);
        for (String raw : argument.split(",", -1)) {
            MilitaryEconomy.Personnel member = account.roster.get(MilitaryEconomy.parseUuid(raw));
            if (member == null || !member.active()) return false;
            if (selected.add(member) && !Objects.equals(destination, member.group)) count++;
        }
        if (group != null && (count > MilitaryConfig.groupSize || MilitaryConfig.enforceCapacity
                && group.leader != null && count > capacity(account.roster.get(group.leader)))) return false;
        for (MilitaryEconomy.Personnel member : selected) {
            if (Objects.equals(destination, member.group)) continue;
            Group previous = account.groups.get(member.group);
            if (previous != null && member.id.equals(previous.leader)) previous.leader = null;
            member.group = destination;
            refresh(owner, economy, member);
        }
        return true;
    }

    private static void refresh(EntityPlayer owner, MilitaryEconomy economy, MilitaryEconomy.Personnel member) {
        EntityMilitaryUnit unit = economy.loaded(member.id);
        if (unit != null) {
            sync(unit, economy.account(owner.getUniqueID()), member);
            economy.updateAssignment(unit);
        }
    }

    static void sync(EntityMilitaryUnit unit, MilitaryEconomy.Account account, MilitaryEconomy.Personnel member) {
        PlatoonOBJ.restore(unit);
        PlatoonOBJ previous = unit.getPlatoon();
        Group group = account.groups.get(member.group);
        if (group != null) {
            PlatoonOBJ squad = PlatoonOBJ.managed(unit, group.id, account.owner);
            unit.setPlatoon(squad);
            squad.setAppointedLeader(group.leader);
            unit.platoonName = group.name;
        } else if (previous != null && previous.managedOwner() != null) {
            unit.setPlatoon(PlatoonOBJ.create(unit));
            unit.platoonName = null;
        }
    }

    static void retired(MilitaryEconomy.Account account, MilitaryEconomy.Personnel member) {
        Group group = account.groups.get(member.group);
        if (group != null && member.id.equals(group.leader)) group.leader = null;
    }

    static int size(MilitaryEconomy.Account account, UUID id) {
        int count = 0;
        for (MilitaryEconomy.Personnel member : account.roster.values()) {
            if (member.active() && Objects.equals(id, member.group)) count++;
        }
        return count;
    }

    static int capacity(MilitaryEconomy.Personnel leader) {
        UnitPresets.Preset preset = leader == null ? null : UnitPresets.get(leader.preset);
        return preset == null ? 0 : preset.capacity + 1;
    }

    private static boolean validName(String name) {
        return !name.trim().isEmpty() && name.length() <= 32 && name.chars().noneMatch(c -> c < 32 || c == 127 || c == 167);
    }

    static final class Group {
        final UUID id;
        String name;
        UUID leader;
        int slot;

        Group(UUID id, String name) {
            this.id = id;
            this.name = name;
        }

        NBTTagCompound write() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Id", id.toString());
            tag.setString("Name", name);
            tag.setInteger("Slot", slot);
            if (leader != null) tag.setString("Leader", leader.toString());
            return tag;
        }

        static void readAll(MilitaryEconomy.Account account, NBTTagList list) {
            Set<Integer> slots = new LinkedHashSet<>();
            for (int i = 0; i < list.tagCount() && i < 48; i++) {
                NBTTagCompound tag = list.getCompoundTagAt(i);
                UUID id = MilitaryEconomy.parseUuid(tag.getString("Id"));
                String name = tag.getString("Name");
                if (id == null || !validName(name) || account.groups.containsKey(id)) continue;
                Group group = new Group(id, name);
                group.leader = MilitaryEconomy.parseUuid(tag.getString("Leader"));
                int slot = tag.getInteger("Slot");
                if (slot > 0 && slot <= 8 && slots.add(slot)) group.slot = slot;
                account.groups.put(id, group);
            }
        }
    }
}
