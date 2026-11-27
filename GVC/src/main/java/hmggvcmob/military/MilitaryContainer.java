
package hmggvcmob.military;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class MilitaryContainer extends Container {
    public static final int VISIBLE_ROWS = 6;
    public static final int TREASURY = 2, SUPPLIES = 3, FACTIONS = 4;
    private final EntityPlayer owner;
    private final int traderId;
    final InventoryBasic deposit = new InventoryBasic("Military deposits", false, 2);
    private int offset, filter, view, sentRoster = -1, lastTransaction, acknowledgement;
    private boolean grouping;
    private long sentRevision = -1, sentCarried = -1;
    private String sentFaction = "";
    private String groupFilter = MilitaryGroups.ALL;
    private NBTTagCompound settlement = new NBTTagCompound();
    private long settlementCheck = -1;
    private boolean sentCooldown;
    private ItemStack lastCurrency, lastFood;
    private String result = "";

    public MilitaryContainer(EntityPlayer owner, int traderId) {
        this.owner = owner;
        this.traderId = traderId;
        if (isRecruitment()) return;
        addSlotToContainer(new Slot(deposit, 0, 17, 84) {
            @Override public boolean isItemValid(ItemStack stack) { return MilitarySupplies.coinIndex(stack) >= 0; }
        });
        addSlotToContainer(new Slot(deposit, 1, 17, 84) {
            @Override public boolean isItemValid(ItemStack stack) {
                return owner.worldObj.isRemote || MilitarySupplies.foodValue(stack) > 0;
            }
        });
        for (int row = 0; row < 3; row++) for (int x = 0; x < 9; x++) {
            addSlotToContainer(new Slot(owner.inventory, x + row * 9 + 9, 61 + x * 18, 145 + row * 18));
        }
        for (int x = 0; x < 9; x++) addSlotToContainer(new Slot(owner.inventory, x, 61 + x * 18, 203));
    }

    public boolean isRecruitment() { return traderId >= 0; }

    public void positionInventory(int tab) {
        if (isRecruitment()) return;
        view = tab;
        boolean visible = tab == TREASURY || tab == SUPPLIES;
        for (int i = 0; i < inventorySlots.size(); i++) {
            Slot slot = getSlot(i);
            if (!visible || i == 0 && tab != TREASURY || i == 1 && tab != SUPPLIES) slot.xDisplayPosition = -1024;
            else slot.xDisplayPosition = i < 2 ? 17 : 61 + (i - 2) % 9 * 18;
        }
    }

    @Override
    public ItemStack slotClick(int slot, int button, int mode, EntityPlayer player) {
        if (isRecruitment() || slot >= inventorySlots.size()
                || slot >= 0 && (view != TREASURY && view != SUPPLIES || slot < 2 && view - TREASURY != slot)) return null;
        return super.slotClick(slot, button, mode, player);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        if (player != owner || !player.isEntityAlive()) return false;
        if (!isRecruitment() || player.worldObj.isRemote) return true;
        Entity entity = player.worldObj.getEntityByID(traderId);
        return entity instanceof EntityMilitaryTrader
                && MilitaryRecruitment.permitted(player, (EntityMilitaryTrader)entity, ((EntityMilitaryTrader)entity).post());
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (isRecruitment() || !canInteractWith(player)) return null;
        Slot slot = getSlot(index);
        if (slot == null || !slot.getHasStack()) return null;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index < 2) {
            if (!mergeItemStack(stack, 2, inventorySlots.size(), true)) return null;
        } else if (view == TREASURY && MilitarySupplies.coinIndex(stack) >= 0) {
            if (!mergeItemStack(stack, 0, 1, false)) return null;
        } else if (view == SUPPLIES && MilitarySupplies.foodValue(stack) > 0) {
            if (!mergeItemStack(stack, 1, 2, false)) return null;
        } else return null;
        if (stack.stackSize == 0) slot.putStack(null);
        else slot.onSlotChanged();
        return original;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (player.worldObj.isRemote || isRecruitment()) return;
        for (int i = 0; i < deposit.getSizeInventory(); i++) {
            ItemStack stack = deposit.getStackInSlotOnClosing(i);
            if (stack != null && !player.inventory.addItemStackToInventory(stack)) player.dropPlayerItemWithRandomChoice(stack, false);
        }
    }

    void action(int type, int value, int transaction, String preset) {
        if (!canInteractWith(owner)) return;
        if (type == MilitaryNetwork.ROSTER) {
            offset = value & 1023;
            filter = value >> 10 & 3;
            grouping = (value & 4096) != 0;
        } else if (type == MilitaryNetwork.TAB) {
            view = Math.max(0, Math.min(FACTIONS, value));
            sentRevision = -1;
        } else if (type == MilitaryNetwork.GROUP_FILTER && !isRecruitment()) {
            MilitaryEconomy.Account account = MilitaryEconomy.get().account(owner.getUniqueID());
            if (MilitaryGroups.ALL.equals(preset) || MilitaryGroups.UNGROUPED.equals(preset)
                    || account.groups.containsKey(MilitaryEconomy.parseUuid(preset))) {
                groupFilter = preset;
                offset = 0;
                grouping = false;
                sentRevision = -1;
            }
        } else if (transaction > lastTransaction) {
            lastTransaction = acknowledgement = transaction;
            result = "invalid";
            if (type == MilitaryNetwork.HIRE && isRecruitment()) {
                result = MilitaryRecruitment.hire(owner, traderId, preset);
            } else if (type == MilitaryNetwork.PLEDGE && preset.isEmpty()) {
                result = MilitaryRelations.pledge(owner, "");
            } else if (type == MilitaryNetwork.PLEDGE && isRecruitment()) {
                result = MilitaryRecruitment.pledge(owner, traderId, preset);
            } else if (!isRecruitment()) {
                if (type == MilitaryNetwork.WITHDRAW) result = MilitarySupplies.withdraw(owner, value);
                else if (type == MilitaryNetwork.GROUP_EDIT && view == 1) result = MilitaryGroups.edit(owner, value, preset);
                else if (type == MilitaryNetwork.CAPTURE && view == FACTIONS) {
                    result = MilitaryOccupation.resolve(owner, preset, value == 1);
                    settlementCheck = -1;
                }
                else if (type == MilitaryNetwork.ALL_COINS) result = MilitarySupplies.depositAllCoins(owner, this);
                else if (type == MilitaryNetwork.DEPOSIT_COINS || type == MilitaryNetwork.DEPOSIT_FOOD) {
                    boolean food = type == MilitaryNetwork.DEPOSIT_FOOD;
                    ItemStack stack = deposit.getStackInSlot(food ? 1 : 0);
                    int count = value == -1 && stack != null ? stack.stackSize : value;
                    result = MilitarySupplies.deposit(owner, this, food, count);
                }
            }
        }
        detectAndSendChanges();
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (!(owner instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP)owner;
        if (isRecruitment() && !canInteractWith(player)) {
            if (player.openContainer == this) {
                player.closeScreen();
                MilitaryRecruitment.refused(player);
            }
            return;
        }
        Entity entity = isRecruitment() ? player.worldObj.getEntityByID(traderId) : null;
        Occupation post = entity instanceof EntityMilitaryTrader ? ((EntityMilitaryTrader)entity).post() : null;
        String faction = post == null ? "" : post.faction();
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(owner.getUniqueID());
        long now = MilitaryEconomy.now();
        boolean cooldown = account.allegianceAfter > now;
        boolean settlementChanged = false;
        if (view == FACTIONS && (settlementCheck < 0 || now >= settlementCheck || sentRevision != account.revision)) {
            NBTTagCompound local = MilitaryOccupation.nearby(owner);
            settlementChanged = !settlement.equals(local);
            settlement = local;
            settlementCheck = now + MilitaryConfig.occupationInterval;
        }
        ItemStack currency = deposit.getStackInSlot(0);
        ItemStack food = deposit.getStackInSlot(1);
        long carried = view == TREASURY ? carriedCoins() : 0;
        int selection = offset | filter << 10 | (grouping ? 4096 : 0);
        if (sentRevision == account.revision && sentRoster == selection && result.isEmpty() && !settlementChanged
                && sentCooldown == cooldown
                && sentCarried == carried && sentFaction.equals(faction)
                && ItemStack.areItemStacksEqual(lastCurrency, currency) && ItemStack.areItemStacksEqual(lastFood, food)) return;

        long wage = 0, ration = 0;
        int active = 0, warnings = 0, deployed = 0;
        for (MilitaryEconomy.Personnel person : account.roster.values()) {
            if (!person.active()) continue;
            active++;
            if (economy.loaded(person.id) != null) deployed++;
            wage += person.wage();
            ration += person.ration();
            if (person.missedWage > 0 || person.missedFood > 0) warnings++;
        }

        NBTTagCompound snapshot = new NBTTagCompound();
        snapshot.setLong("Currency", account.currency);
        snapshot.setLong("Food", account.food);
        snapshot.setLong("Wage", wage);
        snapshot.setLong("Ration", ration);
        snapshot.setInteger("Interval", MilitaryConfig.upkeepInterval);
        snapshot.setInteger("Active", active);
        snapshot.setInteger("Deployed", deployed);
        snapshot.setInteger("Limit", MilitaryConfig.playerLimit);
        snapshot.setInteger("Warnings", warnings);
        snapshot.setInteger("View", view);
        snapshot.setInteger("MaxWithdrawal", MilitaryConfig.maxWithdrawal);
        snapshot.setIntArray("Denominations", MilitaryConfig.denominations);
        int coin = MilitarySupplies.coinIndex(currency);
        snapshot.setInteger("CoinDeposit", coin < 0 ? 0 : MilitaryConfig.denominations[coin]);
        snapshot.setLong("CarriedCoins", carried);
        snapshot.setInteger("FoodDeposit", MilitarySupplies.foodValue(food));
        snapshot.setInteger("Ack", acknowledgement);
        snapshot.setString("Result", result);
        if (post != null) {
            writeOffers(snapshot, post);
            MilitaryRelations.writeFactions(snapshot, account.owner);
        } else if (view == FACTIONS) {
            MilitaryRelations.writeFactions(snapshot, account.owner);
            snapshot.setTag("Settlement", settlement);
        } else if (view == 1) {
            writeGroups(snapshot, economy, account);
            writeRoster(snapshot, economy, account);
        }

        MilitaryNetwork.send(player, windowId, snapshot);
        sentRevision = account.revision;
        sentRoster = offset | filter << 10 | (grouping ? 4096 : 0);
        sentCarried = carried;
        sentFaction = faction;
        sentCooldown = cooldown;
        lastCurrency = currency == null ? null : currency.copy();
        lastFood = food == null ? null : food.copy();
        result = "";
    }

    private long carriedCoins() {
        long total = 0;
        for (ItemStack stack : owner.inventory.mainInventory) {
            int coin = MilitarySupplies.coinIndex(stack);
            if (coin >= 0) total += (long) stack.stackSize * MilitaryConfig.denominations[coin];
        }
        return total;
    }

    private void writeOffers(NBTTagCompound snapshot, Occupation post) {
        List<UnitPresets.Preset> offers = UnitPresets.pool(post.faction(), false);
        offers.sort(Comparator.comparingInt((UnitPresets.Preset p) -> p.tier).thenComparing(p -> p.name));
        snapshot.setString("FactionId", post.faction());
        snapshot.setString("FactionLabel", MilitaryRelations.definition(post.faction()).name);
        snapshot.setInteger("Reputation", MilitaryRelations.playerRelation(owner, post.faction()));
        boundOffset(snapshot, offers.size());
        NBTTagList rows = new NBTTagList();
        for (int index = offset; index < Math.min(offers.size(), offset + VISIBLE_ROWS); index++) {
            UnitPresets.Preset preset = offers.get(index);
            NBTTagCompound row = new NBTTagCompound();
            row.setString("Id", preset.id);
            row.setString("Name", preset.name);
            row.setInteger("Tier", preset.tier);
            row.setInteger("Required", MilitaryRelations.requirement(preset));
            row.setInteger("Price", MilitaryRecruitment.price(owner.getUniqueID(), preset));
            row.setString("Lock", MilitaryRecruitment.lock(owner, preset));
            row.setLong("Wage", Math.round(preset.currencyUpkeep * (double) MilitaryConfig.wageMultiplier));
            row.setLong("Ration", Math.round(preset.foodConsumption * (double) MilitaryConfig.rationMultiplier));
            writePreset(row, preset, preset.previewWeapon());
            rows.appendTag(row);
        }
        snapshot.setTag("Rows", rows);
    }

    private void writeRoster(NBTTagCompound snapshot, MilitaryEconomy economy, MilitaryEconomy.Account account) {
        UUID group = MilitaryEconomy.parseUuid(groupFilter);
        if (group != null && !account.groups.containsKey(group)) {
            groupFilter = MilitaryGroups.ALL;
            offset = 0;
        }
        snapshot.setString("GroupFilter", groupFilter);
        List<MilitaryEconomy.Personnel> personnel = new ArrayList<>(account.roster.values());
        personnel.sort(Comparator.comparing((MilitaryEconomy.Personnel p) -> !p.active())
                .thenComparing(p -> Objects.toString(p.squad, "")).thenComparing(p -> -p.rank).thenComparing(p -> p.name));
        Map<String, RosterEntry> entries = new LinkedHashMap<>();
        for (MilitaryEconomy.Personnel person : personnel) {
            UUID assigned = account.groups.containsKey(person.group) ? person.group : null;
            if (!MilitaryGroups.ALL.equals(groupFilter) && !Objects.equals(group, assigned)) continue;
            boolean deployed = economy.loaded(person.id) != null;
            if (filter == 1 && (!person.active() || !deployed) || filter == 2 && (!person.active() || deployed)
                    || filter == 3 && person.active()) continue;
            String key = grouping ? person.preset + "|" + person.state + "|" + deployed + "|" + person.assignment
                    : person.id.toString();
            RosterEntry row = entries.computeIfAbsent(key, ignored -> new RosterEntry(person, deployed));
            row.count++;
            row.wage += person.active() ? person.wage() : 0;
            row.ration += person.active() ? person.ration() : 0;
        }

        boundOffset(snapshot, entries.size());
        NBTTagList rows = new NBTTagList();
        int index = -1;
        for (RosterEntry entry : entries.values()) {
            if (++index < offset) continue;
            if (index >= offset + VISIBLE_ROWS) break;
            MilitaryEconomy.Personnel person = entry.person;
            NBTTagCompound row = person.write();
            row.setLong("Wage", entry.wage);
            row.setLong("Ration", entry.ration);
            row.setInteger("Count", entry.count);
            row.setBoolean("Loaded", entry.deployed);
            MilitaryEconomy.Personnel leader = account.roster.get(person.leader);
            if (entry.count == 1 && leader != null && leader != person) row.setString("LeaderName", leader.name);
            UnitPresets.Preset preset = UnitPresets.get(person.preset);
            if (preset != null) writePreset(row, preset, person.weapon.isEmpty() ? preset.previewWeapon() : UnitPresets.Preset.item(person.weapon));
            EntityMilitaryUnit unit = economy.loaded(person.id);
            if (unit != null && entry.count == 1) row.setFloat("CurrentHealth", unit.getHealth());
            rows.appendTag(row);
        }
        snapshot.setTag("Rows", rows);
    }

    private void writeGroups(NBTTagCompound snapshot, MilitaryEconomy economy, MilitaryEconomy.Account account) {
        NBTTagList rows = new NBTTagList();
        Map<UUID, GroupSummary> summaries = new LinkedHashMap<>();
        for (MilitaryEconomy.Personnel person : account.roster.values()) {
            if (!person.active()) continue;
            UUID group = account.groups.containsKey(person.group) ? person.group : null;
            GroupSummary summary = summaries.computeIfAbsent(group, ignored -> new GroupSummary());
            summary.count++;
            if (economy.loaded(person.id) != null) summary.deployed++;
            summary.wage += person.wage();
            summary.ration += person.ration();
            String order = person.assignment;
            if (!order.isEmpty()) summary.orders.add(order);
            UnitPresets.Preset preset = UnitPresets.get(person.preset);
            summary.composition.merge(preset == null ? "infantry" : preset.classification, 1, Integer::sum);
        }
        NBTTagCompound ungrouped = summaries.getOrDefault(null, new GroupSummary()).tag();
        ungrouped.setString("Id", MilitaryGroups.UNGROUPED);
        rows.appendTag(ungrouped);
        for (MilitaryGroups.Group group : account.groups.values()) {
            NBTTagCompound row = summaries.getOrDefault(group.id, new GroupSummary()).tag();
            row.setString("Id", group.id.toString());
            row.setString("Name", group.name);
            row.setInteger("Slot", group.slot);
            MilitaryEconomy.Personnel leader = account.roster.get(group.leader);
            if (leader != null && leader.active()) {
                row.setString("Leader", leader.name);
                row.setInteger("Capacity", MilitaryGroups.capacity(leader));
                row.setBoolean("OfficerDeployed", economy.loaded(leader.id) != null);
            }
            rows.appendTag(row);
        }
        snapshot.setTag("Groups", rows);
        snapshot.setBoolean("RequireOfficer", MilitaryConfig.requireOfficer);
        snapshot.setInteger("GroupLimit", MilitaryConfig.groupLimit);
    }

    private static final class GroupSummary {
        final Map<String, Integer> composition = new LinkedHashMap<>();
        final java.util.Set<String> orders = new java.util.LinkedHashSet<>();
        int count, deployed;
        long wage, ration;

        NBTTagCompound tag() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Count", count);
            tag.setInteger("Deployed", deployed);
            tag.setLong("Wage", wage);
            tag.setLong("Ration", ration);
            NBTTagList roles = new NBTTagList();
            for (Map.Entry<String, Integer> role : composition.entrySet()) {
                NBTTagCompound row = new NBTTagCompound();
                row.setString("Classification", role.getKey());
                row.setInteger("Count", role.getValue());
                roles.appendTag(row);
            }
            tag.setTag("Composition", roles);
            tag.setString("Orders", String.join(",", orders));
            return tag;
        }
    }

    private void boundOffset(NBTTagCompound snapshot, int size) {
        offset = Math.max(0, Math.min(offset, size - VISIBLE_ROWS));
        snapshot.setInteger("Offset", offset);
        snapshot.setInteger("Total", size);
        snapshot.setInteger("Filter", filter);
        snapshot.setBoolean("Grouping", grouping);
    }

    private static void writePreset(NBTTagCompound row, UnitPresets.Preset preset, ItemStack weapon) {
        row.setString("Faction", preset.faction);
        row.setString("Classification", preset.classification);
        row.setString("Description", preset.description);
        row.setFloat("Health", preset.health);
        row.setInteger("Armor", preset.armor);
        row.setFloat("Resistance", preset.resistance);
        row.setInteger("Range", preset.range);
        row.setInteger("Capacity", preset.capacity);
        if (weapon != null) row.setTag("Weapon", weapon.writeToNBT(new NBTTagCompound()));
    }

    private static final class RosterEntry {
        final MilitaryEconomy.Personnel person;
        final boolean deployed;
        int count;
        long wage, ration;

        RosterEntry(MilitaryEconomy.Personnel person, boolean deployed) {
            this.person = person;
            this.deployed = deployed;
        }
    }
}
