package hmggvcmob.military;

import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class MilitaryQuests {
    private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<>();
    private static Settings settings = new Settings();
    private static final int LIMIT = 8;

    private MilitaryQuests() {}

    private enum Type {
        HOSTILES, FACTION, UNIT, DELIVER, RECOVER, CLEAR, CAPTURE, LIBERATE, DISCOVER, VISIT;

        boolean needsSite() {
            return this == CLEAR || this == CAPTURE || this == LIBERATE || this == DISCOVER || this == VISIT;
        }
    }

    static void load(Path directory) {
        DEFINITIONS.clear();
        settings = MilitarySites.read(directory, "quests.json", Settings.class, new Settings(), true);
        settings.maxActive = Math.max(1, Math.min(LIMIT, settings.maxActive));
        settings.discoveryRadius = Math.max(8, Math.min(48, settings.discoveryRadius));
        settings.searchRadius = Math.max(32, Math.min(512, settings.searchRadius));
        if (settings.definitions == null) return;
        for (Definition definition : settings.definitions) {
            if (definition != null && definition.valid() && DEFINITIONS.size() < 128) {
                DEFINITIONS.putIfAbsent(definition.id, definition);
            }
        }
    }

    private static String faction(Entity provider) {
        String faction = MilitaryRelations.faction(provider);
        return faction == null ? "" : faction;
    }

    static String role(Entity provider) {
        if (provider instanceof EntityMilitaryTrader) {
            Occupation.TraderLocation location = ((EntityMilitaryTrader)provider).location();
            return location == null ? "" : location.role;
        }
        if (provider instanceof EntityMilitaryUnit) return ((EntityMilitaryUnit)provider).commandRank() > 0 ? "officer" : "soldier";
        return "";
    }

    static boolean permits(EntityPlayer player, Entity provider) {
        if (!settings.enabled || provider == null || provider.worldObj != player.worldObj || !MilitaryRelations.available(faction(provider))) return false;
        if (provider instanceof EntityMilitaryTrader) {
            EntityMilitaryTrader trader = (EntityMilitaryTrader)provider;
            return MilitaryCommerce.offersQuests(role(provider)) && MilitaryRecruitment.permitted(player, trader, trader.post());
        }
        return provider instanceof EntityMilitaryUnit && ((EntityMilitaryUnit)provider).owner() == null
                && ((EntityMilitaryUnit)provider).getAttackTarget() == null
                && provider.isEntityAlive() && player.isEntityAlive() && player.getDistanceSqToEntity(provider) <= 64
                && player.canEntityBeSeen(provider) && !MilitaryRelations.hostile(provider, player)
                && MilitaryRelations.permitsTrade(player, faction(provider));
    }

    static boolean providersEnabled() { return settings.enabled; }

    static boolean open(EntityPlayer player, EntityMilitaryUnit provider) {
        if (!permits(player, provider)) return false;
        if (player instanceof EntityPlayerMP) {
            MilitaryNetwork.openProvider((EntityPlayerMP)player, provider);
            provider.talkWith(player);
        }
        return true;
    }

    static void writeJournal(NBTTagCompound snapshot, EntityPlayer player, Entity provider, Map<String, Contract> offers) {
        MilitaryEconomy.Account account = MilitaryEconomy.get().account(player.getUniqueID());
        NBTTagList active = new NBTTagList();
        long now = MilitaryEconomy.now();
        String faction = faction(provider);
        for (Contract contract : account.contracts.values()) {
            NBTTagCompound row = contract.display(player, now);
            row.setBoolean("TurnIn", faction.equals(contract.faction) && row.getBoolean("Complete") && !row.getBoolean("Invalid"));
            active.appendTag(row);
        }
        snapshot.setTag("Contracts", active);
        snapshot.setInteger("QuestLimit", settings.maxActive);
        snapshot.setBoolean("QuestsEnabled", settings.enabled);
        if (provider == null || !permits(player, provider)) return;
        NBTTagList available = new NBTTagList();
        Map<String, Contract> previous = new LinkedHashMap<>(offers);
        offers.clear();
        if (account.contracts.size() < settings.maxActive) {
            List<Occupation> nearby = null;
            for (Definition definition : DEFINITIONS.values()) {
                if (!eligible(account, provider, definition, now)) continue;
                if (definition.type.needsSite() && nearby == null) {
                    nearby = WorldSavedData_Flag.get(player.worldObj).near(MathHelper.floor_double(provider.posX),
                            MathHelper.floor_double(provider.posZ), settings.searchRadius);
                }
                Contract contract = offer(player, provider, definition, nearby);
                if (contract == null) continue;
                for (Contract former : previous.values()) {
                    if (former.definition.equals(contract.definition) && former.faction.equals(contract.faction)
                            && former.targetFaction.equals(contract.targetFaction) && java.util.Objects.equals(former.site, contract.site)) {
                        contract = former;
                        break;
                    }
                }
                offers.put(contract.id.toString(), contract);
                available.appendTag(contract.display(player, now));
                if (offers.size() == LIMIT) break;
            }
        }
        snapshot.setTag("QuestOffers", available);
    }

    private static boolean eligible(MilitaryEconomy.Account account, Entity provider, Definition definition, long now) {
        String faction = faction(provider);
        String role = role(provider);
        MilitaryRelations.Faction profile = MilitaryRelations.definition(faction);
        Occupation site = provider instanceof EntityMilitaryTrader ? ((EntityMilitaryTrader)provider).post() : null;
        if (profile == null || profile.quests.length > 0 && !Arrays.asList(profile.quests).contains(definition.id)
                || site != null && site.quests.length > 0 && !Arrays.asList(site.quests).contains(definition.id)
                || definition.factions.length > 0 && !Arrays.asList(definition.factions).contains(faction)
                || definition.roles.length > 0 && !Arrays.asList(definition.roles).contains(role)
                || MilitaryRelations.reputation(account.owner, faction) < definition.reputation
                || definition.allegiance && !faction.equals(account.allegiance)) return false;
        String key = faction + ":" + definition.id;
        if (account.questCooldowns.getOrDefault(key, 0L) > now || !definition.repeatable && account.finishedQuests.containsKey(key)) return false;
        for (Contract contract : account.contracts.values()) if (key.equals(contract.key())) return false;
        return true;
    }

    private static Contract offer(EntityPlayer player, Entity provider, Definition definition, List<Occupation> nearby) {
        MilitaryEconomy.Account account = MilitaryEconomy.get().account(player.getUniqueID());
        String faction = faction(provider);
        Contract quest = new Contract(UUID.randomUUID(), definition.id, definition.type, faction);
        quest.provider = provider instanceof EntityMilitaryUnit
                ? ((EntityMilitaryUnit)provider).getCustomNameTag() : role(provider);
        quest.providerRole = provider instanceof EntityMilitaryTrader;
        quest.amount = definition.amount;
        quest.reward = definition.currency;
        quest.reputation = definition.rewardReputation;
        quest.rewardItems = definition.rewards;
        quest.cooldown = definition.cooldown;
        quest.duration = definition.duration;
        quest.dimension = player.dimension;
        if (definition.type == Type.DELIVER || definition.type == Type.RECOVER) {
            quest.item = definition.item;
            return UnitPresets.Preset.item(quest.item) != null ? quest : null;
        }
        List<String> enemies = MilitaryRelations.opponents(faction);
        if (definition.type == Type.HOSTILES) return enemies.isEmpty() ? null : quest;
        if (definition.type == Type.FACTION || definition.type == Type.UNIT) {
            for (String enemy : enemies) {
                if (!definition.target.isEmpty() && !definition.target.equals(enemy)) continue;
                for (UnitPresets.Preset preset : UnitPresets.pool(enemy, true)) {
                    if (definition.type == Type.UNIT && !definition.classification.equals(preset.classification)) continue;
                    quest.targetFaction = enemy;
                    quest.classification = definition.type == Type.UNIT ? definition.classification : "";
                    return quest;
                }
            }
            return null;
        }
        for (Occupation post : nearby) {
            if (post.id.equals(account.lastVisited) || post.contains(provider.posX, provider.posZ)) continue;
            if (!definition.site.isEmpty() && !definition.site.equals(post.type)) continue;
            boolean canContest = MilitaryRelations.hostileToPlayer(account.owner, post.faction());
            boolean fit;
            switch (definition.type) {
                case CLEAR: fit = canContest && post.classification() == Occupation.Classification.INFLUENCE && hostileSite(faction, post); break;
                case CAPTURE: fit = post.classification() == Occupation.Classification.SETTLEMENT && hostileSite(faction, post)
                        && canContest && faction.equals(account.allegiance) && MilitaryConfig.factionCapture
                        && MilitaryRelations.definition(faction).settlementCapture; break;
                case LIBERATE: fit = canContest && post.isVillage() && hostileSite(faction, post); break;
                case DISCOVER: fit = !account.discovered.containsKey(post.id); break;
                case VISIT: fit = post.occupied() && post.classification() == Occupation.Classification.SETTLEMENT
                        && MilitaryRelations.available(post.faction()) && MilitaryRelations.factionRelation(faction, post.faction()) >= 0; break;
                default: fit = false;
            }
            if (fit) {
                quest.site = post.id;
                quest.targetType = post.type;
                quest.targetFaction = post.faction();
                quest.x = post.flagPos[0]; quest.y = post.flagPos[1]; quest.z = post.flagPos[2];
                quest.amount = 1;
                return quest;
            }
        }
        return null;
    }

    private static boolean hostileSite(String faction, Occupation site) {
        return MilitaryConfig.occupation && site.occupied() && site.capturable() && MilitaryRelations.available(site.faction())
                && MilitaryRelations.factionRelation(faction, site.faction()) < 0;
    }

    static String accept(EntityPlayer player, Entity provider, Contract contract) {
        long now = MilitaryEconomy.now();
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(player.getUniqueID());
        if (contract == null || !permits(player, provider) || !faction(provider).equals(contract.faction)
                || account.contracts.size() >= settings.maxActive) return "quest_invalid";
        Definition definition = DEFINITIONS.get(contract.definition);
        if (definition == null || !eligible(account, provider, definition, now) || contract.invalid(player, now)) return "quest_invalid";
        contract.expires = contract.duration > 0 ? now + contract.duration : 0;
        account.contracts.put(contract.id, contract);
        economy.changed(account);
        return "quest_accepted";
    }

    static String finish(EntityPlayer player, Entity provider, UUID id, boolean abandon) {
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(player.getUniqueID());
        Contract contract = account.contracts.get(id);
        if (contract == null || !settings.enabled) return "quest_invalid";
        long now = MilitaryEconomy.now();
        if (!abandon) {
            if (!permits(player, provider) || !faction(provider).equals(contract.faction) || contract.invalid(player, now)
                    || contract.progress(player) < contract.amount || contract.reward > MilitaryEconomy.MAX_BALANCE - account.currency) {
                return "quest_invalid";
            }
            MilitaryInventory inventory = new MilitaryInventory(player.inventory);
            if (contract.delivery() && !inventory.take(UnitPresets.Preset.item(contract.item), contract.amount)) return "quest_invalid";
            for (String entry : contract.rewardItems) {
                ItemStack stack = rewardStack(entry);
                if (stack == null) return "quest_invalid";
                if (!inventory.add(stack, stack.stackSize)) return "trade_inventory";
            }
            inventory.commit();
            account.currency += contract.reward;
            increment(account.finishedQuests, contract.key());
            MilitaryReputation.adjust(account, contract.faction, contract.reputation);
        }
        account.contracts.remove(id);
        if (contract.cooldown > 0) account.questCooldowns.put(contract.key(), now + contract.cooldown);
        account.questCooldowns.values().removeIf(until -> until <= now);
        economy.changed(account);
        return abandon ? "quest_abandoned" : "quest_completed";
    }

    private static ItemStack rewardStack(String entry) {
        if (entry == null) return null;
        int split = entry.lastIndexOf('*');
        ItemStack stack = UnitPresets.Preset.item(split < 0 ? entry : entry.substring(0, split));
        if (stack == null) return null;
        try {
            stack.stackSize = split < 0 ? 1 : Integer.parseInt(entry.substring(split + 1));
            return stack.stackSize >= 1 && stack.stackSize <= 256 ? stack : null;
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    static void combat(UUID player, EntityLivingBase victim, String defeated) {
        if (!settings.enabled || victim instanceof EntityMilitaryTrader || MilitaryRelations.owner(victim) != null
                || !MilitaryRelations.available(defeated)) return;
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(player);
        boolean changed = MilitaryRelations.hostileToPlayer(player, defeated), notice = false;
        if (changed) increment(account.defeated, defeated);
        for (Contract quest : account.contracts.values()) {
            if (quest.progress >= quest.amount || quest.expired(MilitaryEconomy.now())
                    || MilitaryRelations.factionRelation(quest.faction, defeated) >= 0) continue;
            if (quest.type == Type.HOSTILES || quest.type == Type.FACTION && defeated.equals(quest.targetFaction)
                    || quest.type == Type.UNIT && defeated.equals(quest.targetFaction)
                    && victim instanceof EntityMilitaryUnit && ((EntityMilitaryUnit)victim).preset() != null
                    && quest.classification.equals(((EntityMilitaryUnit)victim).preset().classification)) {
                quest.progress++;
                changed = true;
                notice |= quest.progress == quest.amount;
            }
        }
        if (changed) economy.changed(account);
        if (notice && victim.worldObj.func_152378_a(player) instanceof EntityPlayerMP) {
            notice(victim.worldObj.func_152378_a(player), account);
        }
    }

    static void control(World world, Occupation post, UUID player, String former, boolean own) {
        if (!settings.enabled || player == null) return;
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(player);
        if (!former.isEmpty()) increment(own ? account.captured : account.neutralized, former);
        boolean notice = false;
        long now = MilitaryEconomy.now();
        for (Contract quest : account.contracts.values()) {
            if (!post.id.equals(quest.site) || quest.progress > 0 || quest.dimension != world.provider.dimensionId
                    || quest.expired(now) || MilitaryRelations.factionRelation(quest.faction, quest.targetFaction) >= 0) continue;
            if (own ? quest.type == Type.CAPTURE && quest.faction.equals(post.faction())
                    : former.equals(quest.targetFaction) && (quest.type == Type.CLEAR || quest.type == Type.LIBERATE)) {
                quest.progress = 1;
                notice = true;
            }
        }
        economy.changed(account);
        EntityPlayer participant = world.func_152378_a(player);
        if (notice && participant != null) notice(participant, account);
    }

    static void visit(EntityPlayerMP player) {
        if (!settings.enabled || (player.ticksExisted + player.getEntityId()) % 20 != 0) return;
        MilitaryEconomy economy = MilitaryEconomy.get();
        Occupation post = WorldSavedData_Flag.get(player.worldObj).atTerritory(player.posX, player.posZ, false);
        if (post == null || player.getDistanceSq(post.flagPos[0] + .5, post.flagPos[1], post.flagPos[2] + .5)
                > settings.discoveryRadius * settings.discoveryRadius) {
            MilitaryEconomy.Account existing = economy.findAccount(player.getUniqueID());
            if (existing != null) existing.lastVisited = null;
            return;
        }
        MilitaryEconomy.Account account = economy.account(player.getUniqueID());
        if (post.id.equals(account.lastVisited)) return;
        account.lastVisited = post.id;
        boolean changed = false, notice = false;
        if (account.discovered.size() < 2048 && !account.discovered.containsKey(post.id)) {
            account.discovered.put(post.id, post.faction());
            changed = true;
        }
        for (Contract quest : account.contracts.values()) {
            if ((quest.type == Type.DISCOVER || quest.type == Type.VISIT && post.occupied()
                    && MilitaryRelations.factionRelation(quest.faction, post.faction()) >= 0)
                    && post.id.equals(quest.site) && quest.progress == 0 && quest.dimension == player.dimension
                    && !quest.expired(MilitaryEconomy.now())) {
                quest.progress = 1;
                changed = notice = true;
            }
        }
        if (changed) economy.changed(account);
        if (notice) notice(player, account);
    }

    private static void notice(EntityPlayer player, MilitaryEconomy.Account account) {
        long now = MilitaryEconomy.now();
        if (settings.notifications && now >= account.questNotice) {
            account.questNotice = now + 100;
            player.addChatComponentMessage(new ChatComponentTranslation("gvc.military.quest.ready"));
        }
    }

    static void stats(NBTTagCompound row, MilitaryEconomy.Account account, String faction) {
        int completed = 0, discovered = 0;
        for (Map.Entry<String, Integer> entry : account.finishedQuests.entrySet()) {
            if (entry.getKey().startsWith(faction + ":")) completed = (int)Math.min(Integer.MAX_VALUE, (long)completed + entry.getValue());
        }
        for (String owner : account.discovered.values()) if (faction.equals(owner)) discovered++;
        row.setInteger("CompletedQuests", completed);
        row.setInteger("Defeated", account.defeated.getOrDefault(faction, 0));
        row.setInteger("Captured", account.captured.getOrDefault(faction, 0));
        row.setInteger("Neutralized", account.neutralized.getOrDefault(faction, 0));
        row.setInteger("Discovered", discovered);
    }

    private static void increment(Map<String, Integer> counts, String key) {
        if (counts.containsKey(key) || counts.size() < 512) {
            counts.merge(key, 1, (old, one) -> old == Integer.MAX_VALUE ? old : old + one);
        }
    }

    static void save(MilitaryEconomy.Account account, NBTTagCompound tag) {
        NBTTagList quests = new NBTTagList();
        for (Contract quest : account.contracts.values()) quests.appendTag(quest.write());
        tag.setTag("Contracts", quests);
        NBTTagCompound cooldowns = new NBTTagCompound(), sites = new NBTTagCompound();
        for (Map.Entry<String, Long> value : account.questCooldowns.entrySet()) cooldowns.setLong(value.getKey(), value.getValue());
        for (Map.Entry<UUID, String> value : account.discovered.entrySet()) sites.setString(value.getKey().toString(), value.getValue());
        tag.setTag("QuestCooldowns", cooldowns);
        tag.setTag("DiscoveredSites", sites);
        writeCounts(tag, "FinishedContracts", account.finishedQuests);
        writeCounts(tag, "DefeatedPersonnel", account.defeated);
        writeCounts(tag, "CapturedSites", account.captured);
        writeCounts(tag, "NeutralizedSites", account.neutralized);
    }

    static void load(MilitaryEconomy.Account account, NBTTagCompound tag) {
        NBTTagList quests = tag.getTagList("Contracts", 10);
        for (int i = 0; i < Math.min(LIMIT, quests.tagCount()); i++) {
            Contract contract = Contract.read(quests.getCompoundTagAt(i));
            if (contract != null) account.contracts.putIfAbsent(contract.id, contract);
        }
        NBTTagCompound cooldowns = tag.getCompoundTag("QuestCooldowns"), sites = tag.getCompoundTag("DiscoveredSites");
        for (Object key : cooldowns.func_150296_c()) {
            String id = (String)key;
            if (id.length() <= 97 && account.questCooldowns.size() < 512) account.questCooldowns.put(id, cooldowns.getLong(id));
        }
        for (Object key : sites.func_150296_c()) {
            String id = (String)key;
            UUID site = MilitaryEconomy.parseUuid(id);
            String faction = sites.getString(id);
            if (site != null && account.discovered.size() < 2048 && (faction.isEmpty() || MilitaryRelations.validId(faction))) {
                account.discovered.put(site, faction);
            }
        }
        readCounts(tag, "FinishedContracts", account.finishedQuests);
        readCounts(tag, "DefeatedPersonnel", account.defeated);
        readCounts(tag, "CapturedSites", account.captured);
        readCounts(tag, "NeutralizedSites", account.neutralized);
    }

    private static void writeCounts(NBTTagCompound tag, String name, Map<String, Integer> counts) {
        NBTTagCompound section = new NBTTagCompound();
        for (Map.Entry<String, Integer> value : counts.entrySet()) section.setInteger(value.getKey(), value.getValue());
        tag.setTag(name, section);
    }

    private static void readCounts(NBTTagCompound tag, String name, Map<String, Integer> counts) {
        NBTTagCompound section = tag.getCompoundTag(name);
        for (Object key : section.func_150296_c()) {
            String id = (String)key;
            int count = section.getInteger(id);
            if (id.length() <= 97 && count > 0 && counts.size() < 512) counts.put(id, count);
        }
    }

    static final class Contract {
        final UUID id;
        final Type type;
        final String definition, faction;
        UUID site;
        String provider = "", targetFaction = "", classification = "", targetType = "", item = "";
        String[] rewardItems = {};
        boolean providerRole;
        int amount, progress, reputation, cooldown, duration, dimension, x, y, z;
        long expires, reward;

        Contract(UUID id, String definition, Type type, String faction) {
            this.id = id;
            this.definition = definition;
            this.type = type;
            this.faction = faction;
        }

        String key() { return faction + ":" + definition; }
        boolean expired(long now) { return expires > 0 && now >= expires; }
        boolean delivery() { return type == Type.DELIVER || type == Type.RECOVER; }

        int progress(EntityPlayer player) {
            if (!delivery()) return progress;
            ItemStack requested = UnitPresets.Preset.item(item);
            if (requested == null) return 0;
            int count = 0;
            for (ItemStack stack : player.inventory.mainInventory) {
                if (stack != null && stack.isItemEqual(requested) && ItemStack.areItemStackTagsEqual(stack, requested)) count += stack.stackSize;
            }
            return Math.min(amount, count);
        }

        boolean invalid(EntityPlayer player, long now) {
            if (!MilitaryRelations.available(faction) || expired(now)) return true;
            if (delivery()) return UnitPresets.Preset.item(item) == null;
            if (progress >= amount) return false;
            if (type == Type.HOSTILES) return MilitaryRelations.opponents(faction).isEmpty();
            if (type == Type.FACTION || type == Type.UNIT) {
                return !MilitaryRelations.available(targetFaction) || MilitaryRelations.factionRelation(faction, targetFaction) >= 0;
            }
            World world = player.dimension == dimension ? player.worldObj
                    : net.minecraftforge.common.DimensionManager.getWorld(dimension);
            if (world == null) return false;
            Occupation target = WorldSavedData_Flag.get(world).byId(site);
            if (target == null) return true;
            if (type == Type.VISIT) return !target.occupied() || MilitaryRelations.factionRelation(faction, target.faction()) < 0;
            if (type == Type.CLEAR || type == Type.LIBERATE) return !hostileSite(faction, target);
            return type == Type.CAPTURE && (!MilitaryConfig.factionCapture || !target.capturable()
                    || MilitaryRelations.factionRelation(faction, target.faction()) >= 0 || target.occupied() && !hostileSite(faction, target));
        }

        NBTTagCompound display(EntityPlayer player, long now) {
            NBTTagCompound tag = write();
            tag.setString("FactionName", MilitaryRelations.displayName(faction));
            tag.setString("TargetFactionName", MilitaryRelations.displayName(targetFaction));
            tag.setInteger("Progress", progress(player));
            tag.setBoolean("Complete", progress(player) >= amount);
            tag.setBoolean("Invalid", invalid(player, now));
            tag.setLong("Remaining", expires == 0 ? -1 : Math.max(0, expires - now));
            ItemStack required = UnitPresets.Preset.item(item);
            if (required != null) tag.setTag("Required", required.writeToNBT(new NBTTagCompound()));
            if (rewardItems.length > 0) {
                NBTTagList rewards = new NBTTagList();
                for (String entry : rewardItems) {
                    ItemStack rewardItem = rewardStack(entry);
                    if (rewardItem == null) continue;
                    NBTTagCompound row = rewardItem.writeToNBT(new NBTTagCompound());
                    row.setInteger("RewardCount", rewardItem.stackSize);
                    rewards.appendTag(row);
                }
                tag.setTag("RewardItems", rewards);
            }
            return tag;
        }

        NBTTagCompound write() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Id", id.toString());
            tag.setString("Definition", definition);
            tag.setString("Type", type.name());
            tag.setString("Faction", faction);
            tag.setString("Provider", provider);
            tag.setBoolean("ProviderRole", providerRole);
            tag.setString("TargetFaction", targetFaction);
            tag.setString("TargetType", targetType);
            tag.setString("Classification", classification);
            tag.setString("Item", item);
            tag.setString("RewardDefinitions", String.join(",", rewardItems));
            tag.setInteger("Amount", amount);
            tag.setInteger("Progress", progress);
            tag.setInteger("ReputationReward", reputation);
            tag.setInteger("Cooldown", cooldown);
            tag.setLong("Expires", expires);
            tag.setLong("Reward", reward);
            if (site != null) {
                tag.setString("Site", site.toString());
                tag.setIntArray("Location", new int[]{x, y, z});
                tag.setInteger("Dimension", dimension);
            }
            return tag;
        }

        static Contract read(NBTTagCompound tag) {
            UUID id = MilitaryEconomy.parseUuid(tag.getString("Id"));
            String definition = tag.getString("Definition"), faction = tag.getString("Faction");
            if (id == null || !MilitaryRelations.validId(definition) || !MilitaryRelations.validId(faction)) return null;
            Type type;
            try { type = Type.valueOf(tag.getString("Type")); }
            catch (IllegalArgumentException invalid) { return null; }
            Contract quest = new Contract(id, definition, type, faction);
            quest.provider = tag.getString("Provider");
            quest.providerRole = tag.getBoolean("ProviderRole");
            quest.targetFaction = tag.getString("TargetFaction");
            quest.targetType = tag.getString("TargetType");
            quest.classification = tag.getString("Classification");
            quest.item = tag.getString("Item");
            String rewards = tag.getString("RewardDefinitions");
            quest.rewardItems = rewards.isEmpty() ? new String[0] : rewards.split(",", 9);
            if (quest.rewardItems.length > 8) return null;
            quest.amount = MathHelper.clamp_int(tag.getInteger("Amount"), 1, 256);
            quest.progress = MathHelper.clamp_int(tag.getInteger("Progress"), 0, quest.amount);
            quest.reputation = MathHelper.clamp_int(tag.getInteger("ReputationReward"), 0, 10000);
            quest.cooldown = MathHelper.clamp_int(tag.getInteger("Cooldown"), 0, 720000);
            quest.expires = Math.max(0, tag.getLong("Expires"));
            quest.reward = Math.max(0, Math.min(1000000, tag.getLong("Reward")));
            quest.site = MilitaryEconomy.parseUuid(tag.getString("Site"));
            if (quest.site != null) {
                int[] pos = tag.getIntArray("Location");
                if (pos.length != 3) return null;
                quest.x = pos[0]; quest.y = pos[1]; quest.z = pos[2];
                quest.dimension = tag.getInteger("Dimension");
            }
            return quest;
        }
    }

    private static final class Settings {
        boolean enabled = true, notifications = true;
        int maxActive = 4, discoveryRadius = 28, searchRadius = 384;
        Definition[] definitions = {};
    }

    private static final class Definition {
        String id, item = "", classification = "", target = "", site = "";
        String[] factions = {}, roles = {}, rewards = {};
        Type type;
        int amount = 1, reputation, rewardReputation = 5, cooldown = 6000, duration;
        long currency = 15;
        boolean allegiance, repeatable = true;

        boolean valid() {
            if (!MilitaryRelations.validId(id) || type == null || item == null || classification == null
                    || target == null || site == null || factions == null || roles == null || rewards == null || rewards.length > 8
                    || amount < 1 || amount > 256 || reputation != MilitaryRelations.clamp(reputation)
                    || rewardReputation < 0 || rewardReputation > 10000 || currency < 0 || currency > 1000000
                    || cooldown < 0 || cooldown > 720000 || duration < 0 || duration > 720000) return false;
            if ((type == Type.DELIVER || type == Type.RECOVER) && UnitPresets.Preset.item(item) == null) return false;
            if (type == Type.UNIT && classification.isEmpty()) return false;
            for (String entry : rewards) if (rewardStack(entry) == null) return false;
            return true;
        }
    }
}
