package hmggvcmob.military;

import handmadeguns.items.HMGItemAttachmentBase;
import handmadeguns.items.HMGItemBullet_with_Internal_Bullet;
import handmadeguns.items.HMGItemCustomMagazine;
import handmadeguns.items.guns.HMGItem_Unified_Guns;
import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.oredict.OreDictionary;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class MilitaryCommerce {
    static final int PAGE = 6;
    private static final String[] CATEGORIES = {"all", "firearms", "ammunition", "attachments", "equipment", "supplies", "resources"};
    private static final Map<String, Listing> BUY = new LinkedHashMap<>();
    private static final Map<String, Listing> CONFIGURED = new HashMap<>();
    private static final Map<String, Rule> ORES = new HashMap<>();
    private static Settings settings = new Settings();

    private MilitaryCommerce() {}

    static void load(Path directory) {
        settings = MilitarySites.read(directory, "commerce.json", Settings.class, new Settings(), true);
        BUY.clear();
        CONFIGURED.clear();
        ORES.clear();
        settings.maxQuantity = Math.max(1, Math.min(256, settings.maxQuantity));
        settings.discountCap = Math.max(0, Math.min(50, settings.discountCap));
        settings.saleBonus = Math.max(0, Math.min(100, settings.saleBonus));
        settings.restockTicks = Math.max(200, settings.restockTicks);
        if (settings.roles == null) settings.roles = new HashMap<>();
        if (settings.items == null) settings.items = new HashMap<>();
        if (settings.blacklist == null) settings.blacklist = new String[0];
        settings.roles.entrySet().removeIf(entry -> !MilitaryRelations.validId(entry.getKey())
                || entry.getValue() == null || !entry.getValue().valid());

        for (Map.Entry<String, Rule> entry : settings.items.entrySet()) {
            String id = entry.getKey();
            Rule rule = entry.getValue();
            if (rule == null || !rule.valid() || id.length() > 120) continue;
            if (id.startsWith("ore:")) {
                ORES.put(id.substring(4), rule);
                continue;
            }
            ItemStack stack = UnitPresets.Preset.item(id);
            if (stack == null || !validStack(stack)) continue;
            Listing listing = new Listing(id(stack), stack, rule);
            CONFIGURED.put(listing.id, listing);
        }
        for (Object entry : Item.itemRegistry) {
            if (!(entry instanceof Item)) continue;
            ItemStack stack = new ItemStack((Item)entry);
            Listing listing = automatic(stack);
            if (listing != null && listing.rule.buy > 0) BUY.put(listing.id, listing);
        }
        for (Listing listing : CONFIGURED.values()) {
            if (listing.rule.buy == 0) BUY.remove(listing.id);
            else BUY.put(listing.id, listing);
        }
    }

    static boolean enabled(String role, boolean sell) {
        Role definition = settings.roles.get(role);
        return settings.enabled && definition != null && (sell ? definition.sellsTo : definition.sellsFrom)
                && (definition.categories.length > 0 || definition.items.length > 0);
    }

    static boolean recruits(String role) {
        Role definition = settings.roles.get(role);
        return definition != null && definition.recruit != null ? definition.recruit
                : "quartermaster".equals(role) || "recruitment".equals(role) || "contract".equals(role);
    }

    static boolean offersQuests(String role) {
        Role definition = settings.roles.get(role);
        return definition == null || definition.quests;
    }

    private static boolean validStack(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0 || stack.stackSize > stack.getMaxStackSize()
                || stack.getItemDamage() < 0 || MilitarySupplies.coinIndex(stack) >= 0) return false;
        return !stack.isItemStackDamageable() || stack.getItemDamage() <= stack.getMaxDamage();
    }

    private static String id(ItemStack stack) {
        return Item.itemRegistry.getNameForObject(stack.getItem()) + (stack.getHasSubtypes() ? "@" + stack.getItemDamage() : "");
    }

    private static Listing automatic(ItemStack stack) {
        Item item = stack.getItem();
        String id = id(stack);
        Rule rule = new Rule();
        if (item instanceof HMGItem_Unified_Guns) {
            String kind = WeaponCatalog.classification((HMGItem_Unified_Guns)item);
            rule.category = "firearms";
            rule.buy = "LAUNCHER".equals(kind) ? 170 : "MACHINE_GUN".equals(kind) || "SNIPER_RIFLE".equals(kind) ? 115
                    : "SHOTGUN".equals(kind) ? 70 : 84;
            rule.sell = rule.buy / 4;
            rule.stock = 3;
            rule.reputation = rule.buy > 100 ? 100 : -99;
        } else if (item instanceof HMGItemCustomMagazine || item instanceof HMGItemBullet_with_Internal_Bullet) {
            rule.category = "ammunition";
            rule.buy = 6;
            rule.sell = 1;
            rule.stock = 48;
        } else if (item instanceof HMGItemAttachmentBase) {
            rule.category = "attachments";
            rule.buy = 28;
            rule.sell = 6;
            rule.stock = 6;
        } else if ((id.startsWith("GVCMob:") || id.startsWith("GVCGuns:")) && (item instanceof ItemArmor || item instanceof ItemFood)) {
            rule.category = item instanceof ItemFood ? "supplies" : "equipment";
            rule.buy = item instanceof ItemFood ? 6 : 40;
            rule.sell = item instanceof ItemFood ? 1 : 8;
        } else return null;
        return new Listing(id, stack, rule);
    }

    private static Listing selling(ItemStack stack) {
        if (!validStack(stack)) return null;
        String id = id(stack);
        Listing result = CONFIGURED.get(id);
        if (result == null) result = BUY.get(id);
        if (result == null && settings.oreDictionary) {
            int[] ids = OreDictionary.getOreIDs(stack);
            for (int code : ids) {
                Rule rule = ORES.get(OreDictionary.getOreName(code));
                if (rule != null && (result == null || result.rule.sell < rule.sell)) {
                    result = new Listing(id, stack, rule);
                }
            }
        }
        return result;
    }

    private static boolean permitted(Listing listing, String faction, Role role, Occupation post, boolean sell) {
        if (listing == null || role == null || Arrays.asList(settings.blacklist).contains(listing.id)
                || Arrays.asList(settings.blacklist).contains(Item.itemRegistry.getNameForObject(listing.stack.getItem()))
                || Arrays.asList(role.blacklist).contains(listing.id)) return false;
        if (sell && !settings.vanillaResources && listing.id.startsWith("minecraft:") && "resources".equals(listing.rule.category)) return false;
        boolean explicit = Arrays.asList(role.items).contains(listing.id);
        if (!explicit && !Arrays.asList(role.categories).contains(listing.rule.category)) return false;
        if (sell || "resources".equals(listing.rule.category) || "supplies".equals(listing.rule.category)) return true;
        MilitaryRelations.Faction profile = MilitaryRelations.definition(faction);
        String[] equipment = post.equipment.length > 0 ? post.equipment : profile.equipment;
        if (equipment.length > 0) return Arrays.asList(equipment).contains(listing.id);
        if (explicit || !(listing.stack.getItem() instanceof HMGItem_Unified_Guns) || profile.firearmClasses.length == 0) return true;
        String classification = WeaponCatalog.classification((HMGItem_Unified_Guns)listing.stack.getItem());
        return classification.isEmpty() || Arrays.asList(profile.firearmClasses).contains(classification);
    }

    private static int price(Listing listing, ItemStack stack, String faction, Role role, Occupation post, EntityPlayer player, boolean sell) {
        int discount = Math.min(settings.discountCap, MilitaryRelations.discount(player.getUniqueID(), faction));
        int factionPrice = MilitaryRelations.definition(faction).price;
        if (!sell) {
            int calculated = (int)(((long)listing.rule.buy * factionPrice * role.price * post.pricePercent()
                    * (100 - discount) + 99999999) / 100000000);
            return Math.max((listing.rule.buy + 2) / 3, calculated);
        }
        int bestSale = listing.rule.sell * (100 + Math.min(settings.saleBonus, discount)) / 100;
        if (stack.isItemStackDamageable() && !(stack.getItem() instanceof HMGItem_Unified_Guns) && stack.getMaxDamage() > 0) {
            bestSale = bestSale * (stack.getMaxDamage() - stack.getItemDamage()) / stack.getMaxDamage();
        }
        Listing purchase = BUY.get(listing.id);
        if (purchase != null) {
            bestSale = Math.min(bestSale, (purchase.rule.buy + 2) / 3 - 1);
        }
        return Math.max(0, bestSale);
    }

    static String transact(EntityPlayer player, EntityMilitaryTrader trader, Bid bid, int count) {
        if (bid == null || count <= 0 || count > settings.maxQuantity || !enabled(trader.location().role, bid.sell)) return "trade_invalid";
        Occupation.TraderLocation location = trader.location();
        Role role = settings.roles.get(location.role);
        String faction = trader.faction();
        Occupation post = trader.post();
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(player.getUniqueID());
        long epoch = epoch(player);
        long spent = location.commerceEpoch == epoch ? location.liquidityUsed : 0;
        ItemStack stack = bid.sell ? player.inventory.getStackInSlot(bid.slot) : bid.stack;
        if (bid.sell && !ItemStack.areItemStacksEqual(bid.stack, stack)) return "trade_changed";
        Listing listing = bid.sell ? selling(stack) : BUY.get(bid.id);
        if (!permitted(listing, faction, role, post, bid.sell)) return "trade_invalid";
        if (!bid.sell && MilitaryRelations.reputation(player.getUniqueID(), faction) < listing.rule.reputation) return "reputation_locked";
        int price = price(listing, stack, faction, role, post, player, bid.sell);
        if (price != bid.price || price <= 0 || count > bid.maximum) return "trade_changed";
        long total = (long)price * count;
        MilitaryInventory inventory = new MilitaryInventory(player.inventory);
        if (bid.sell) {
            if (stack.stackSize < count || total > MilitaryEconomy.MAX_BALANCE - account.currency) return "trade_invalid";
            if (role.liquidity > 0 && total > role.liquidity - spent) return "trade_liquidity";
            inventory.takeSlot(bid.slot, count);
        } else {
            if (count > available(location, listing, epoch)) return "trade_stock";
            if (total > account.currency) return "funds";
            if (!inventory.add(stack, count)) return "trade_inventory";
        }
        if (location.commerceEpoch != epoch) {
            location.commerceEpoch = epoch;
            location.liquidityUsed = 0;
            location.stockUsed.clear();
        }
        inventory.commit();
        account.currency += bid.sell ? total : -total;
        if (bid.sell && role.liquidity > 0) location.liquidityUsed += total;
        if (!bid.sell && listing.rule.stock > 0) location.stockUsed.merge(listing.id, count, Integer::sum);
        economy.changed(account);
        WorldSavedData_Flag.get(player.worldObj).markDirty();
        return bid.sell ? "trade_sold" : "trade_bought";
    }

    static long epoch(EntityPlayer player) { return player.worldObj.getTotalWorldTime() / settings.restockTicks; }

    private static int available(Occupation.TraderLocation location, Listing listing, long epoch) {
        return listing.rule.stock <= 0 ? settings.maxQuantity : Math.max(0, listing.rule.stock
                - (location.commerceEpoch == epoch ? location.stockUsed.getOrDefault(listing.id, 0) : 0));
    }

    static void snapshot(NBTTagCompound snapshot, EntityPlayer player, EntityMilitaryTrader trader, boolean sell,
                         int category, int offset, String query, int quote, Map<String, Bid> bids) {
        Occupation.TraderLocation location = trader.location();
        if (location == null || !enabled(location.role, sell)) return;
        Role role = settings.roles.get(location.role);
        String faction = trader.faction();
        Occupation post = trader.post();
        if (!MilitaryRelations.available(faction)) return;
        String filter = category >= 0 && category < CATEGORIES.length ? CATEGORIES[category] : "all";
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<Bid> offers = new ArrayList<>();
        long epoch = epoch(player);
        int reputation = MilitaryRelations.reputation(player.getUniqueID(), faction);
        NBTTagList carried = new NBTTagList();
        NBTTagList rows = new NBTTagList();
        for (int slot = 0; slot < player.inventory.mainInventory.length; slot++) {
            ItemStack stack = player.inventory.mainInventory[slot];
            if (stack == null) continue;
            NBTTagCompound column = item(stack);
            column.setInteger("Slot", slot);
            carried.appendTag(column);
            if (!sell) continue;
            Listing listing = selling(stack);
            if (!permitted(listing, faction, role, post, true)) continue;
            int price = price(listing, stack, faction, role, post, player, true);
            if (price == 0) continue;
            int maximum = Math.min(settings.maxQuantity, stack.stackSize);
            if (role.liquidity > 0) {
                long remaining = role.liquidity - (location.commerceEpoch == epoch ? location.liquidityUsed : 0);
                maximum = (int)Math.min(maximum, remaining / price);
            }
            Bid bid = new Bid(listing, stack.copy(), true, slot, price, Math.max(0, maximum));
            String key = quote + ".s" + slot;
            bids.put(key, bid);
            writeBid(column, bid, key, false);
            if (matches(listing, filter, needle)) offers.add(bid);
        }
        if (!sell) {
            for (Listing listing : BUY.values()) {
                if (!permitted(listing, faction, role, post, false) || !matches(listing, filter, needle)) continue;
                int count = reputation >= listing.rule.reputation ? Math.min(settings.maxQuantity, available(location, listing, epoch)) : 0;
                offers.add(new Bid(listing, listing.stack, false, -1, price(listing, listing.stack, faction, role, post, player, false), count));
            }
            offers.sort(Comparator.comparing((Bid bid) -> bid.category).thenComparing(bid -> bid.id, String.CASE_INSENSITIVE_ORDER));
        }
        int start = Math.max(0, Math.min(offset / PAGE * PAGE, (offers.size() - 1) / PAGE * PAGE));
        for (int i = start; i < Math.min(offers.size(), start + PAGE); i++) {
            Bid bid = offers.get(i);
            String key = quote + "." + (i - start);
            bids.put(key, bid);
            NBTTagCompound row = item(bid.stack);
            writeBid(row, bid, key, true);
            rows.appendTag(row);
        }
        snapshot.setTag("TradeRows", rows);
        snapshot.setTag("Carried", carried);
        snapshot.setInteger("TradeTotal", offers.size());
        snapshot.setInteger("TradeOffset", start);
        snapshot.setInteger("TradeDiscount", Math.min(settings.discountCap, MilitaryRelations.discount(player.getUniqueID(), faction)));
        snapshot.setInteger("TradeCategory", category);
        snapshot.setString("TradeQuery", query);
    }

    private static void writeBid(NBTTagCompound row, Bid bid, String key, boolean detailed) {
        row.setString("Key", key);
        row.setString("Id", bid.id);
        row.setInteger("Slot", bid.slot);
        row.setString("Category", bid.category);
        row.setInteger("Price", bid.price);
        row.setInteger("Max", bid.maximum);
        row.setInteger("Reputation", bid.reputation);
        if (detailed && bid.stack.getItem() instanceof HMGItem_Unified_Guns) {
            HMGItem_Unified_Guns gun = (HMGItem_Unified_Guns)bid.stack.getItem();
            row.setString("Classification", WeaponCatalog.classification(gun));
            Item magazine = gun.getcurrentMagazine(bid.sell ? bid.stack.copy() : null);
            if (magazine != null) row.setTag("Magazine", item(new ItemStack(magazine)).getCompoundTag("Stack"));
        }
    }

    private static boolean matches(Listing listing, String category, String needle) {
        return ("all".equals(category) || category.equals(listing.rule.category))
                && (needle.isEmpty() || listing.id.toLowerCase(Locale.ROOT).contains(needle)
                || listing.stack.getDisplayName().toLowerCase(Locale.ROOT).contains(needle));
    }

    private static NBTTagCompound item(ItemStack stack) {
        NBTTagCompound row = new NBTTagCompound();
        ItemStack icon = new ItemStack(stack.getItem(), 1, stack.getItemDamage());
        row.setTag("Stack", icon.writeToNBT(new NBTTagCompound()));
        row.setInteger("Count", stack.stackSize);
        if (stack.hasDisplayName()) {
            String display = stack.getDisplayName();
            row.setString("CustomName", display.substring(0, Math.min(96, display.length())));
        }
        return row;
    }

    static final class Bid {
        final String id, category;
        final ItemStack stack;
        final boolean sell;
        final int slot, price, maximum, reputation;

        Bid(Listing listing, ItemStack stack, boolean sell, int slot, int price, int maximum) {
            id = listing.id;
            category = listing.rule.category;
            reputation = listing.rule.reputation;
            this.stack = stack;
            this.sell = sell;
            this.slot = slot;
            this.price = price;
            this.maximum = maximum;
        }
    }

    private static final class Listing {
        final String id;
        final ItemStack stack;
        final Rule rule;

        Listing(String id, ItemStack stack, Rule rule) {
            this.id = id;
            this.stack = stack;
            this.rule = rule;
        }
    }

    private static final class Settings {
        boolean enabled = true, vanillaResources = true, oreDictionary = false;
        int maxQuantity = 64, discountCap = 30, saleBonus = 20, restockTicks = 12000;
        String[] blacklist = {};
        Map<String, Rule> items = new LinkedHashMap<>();
        Map<String, Role> roles = new LinkedHashMap<>();
    }

    private static final class Role {
        String[] categories = {}, items = {}, blacklist = {};
        int price = 100, liquidity;
        Boolean recruit;
        boolean sellsFrom = true, sellsTo = true, quests = true;
        boolean valid() {
            return categories != null && items != null && blacklist != null && price >= 25 && price <= 400
                    && liquidity >= 0 && liquidity <= 1000000;
        }
    }

    private static final class Rule {
        String category = "resources";
        int buy, sell, stock = 32, reputation = -100;
        boolean valid() {
            return Arrays.asList(CATEGORIES).contains(category) && buy >= 0 && buy <= 100000 && sell >= 0
                    && sell <= 100000 && stock >= 0 && stock <= 10000 && reputation == MilitaryRelations.clamp(reputation);
        }
    }
}
