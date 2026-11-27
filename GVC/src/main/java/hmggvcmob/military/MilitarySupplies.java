
package hmggvcmob.military;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.registry.GameRegistry;
import hmggvcmob.GVCMobPlus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemSoup;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MilitarySupplies {
    private static final Item[] COINS = new Item[4];
    private static final Map<String, Integer> FOOD_OVERRIDES = new HashMap<>();
    private static final Set<String> BLACKLIST = new HashSet<>();
    private static final Map<Item, FoodAdapter> ADAPTERS = new HashMap<>();
    private static int[] optimalCoins;
    private static byte[] optimalLastCoin;

    public interface FoodAdapter {
        int value(ItemStack food);
        ItemStack remainder(ItemStack food);
    }

    private MilitarySupplies() {}

    static void registerItems() {
        for (int i = 0; i < COINS.length; i++) {
            Item coin = new Item().setUnlocalizedName("gvc_military_coin_" + i)
                    .setTextureName("gvcmob:military_coin_" + i).setCreativeTab(GVCMobPlus.tabgvcm);
            COINS[i] = coin;
            GameRegistry.registerItem(coin, "MilitaryCoin" + i);
        }
    }

    static void load() {
        FOOD_OVERRIDES.clear();
        BLACKLIST.clear();
        for (String value : MilitaryConfig.foodBlacklist) BLACKLIST.add(value.trim());
        for (String entry : MilitaryConfig.foodOverrides) {
            int split = entry.lastIndexOf('=');
            if (split > 0) {
                try {
                    int value = Integer.parseInt(entry.substring(split + 1).trim());
                    if (value >= 0 && value <= 1000000) {
                        FOOD_OVERRIDES.put(entry.substring(0, split).trim(), value);
                        continue;
                    }
                } catch (NumberFormatException ignored) {}
            }
            FMLLog.warning("[GVC military] Invalid food override %s", entry);
        }
        optimalCoins = new int[MilitaryConfig.maxWithdrawal + 1];
        optimalLastCoin = new byte[optimalCoins.length];
        calculateChange(MilitaryConfig.denominations, optimalCoins, optimalLastCoin);
    }

    public static void registerFoodAdapter(Item item, FoodAdapter adapter) {
        if (item == null || adapter == null) throw new IllegalArgumentException("Item and food adapter required");
        ADAPTERS.put(item, adapter);
    }

    public static Item coinItem(int index) {
        return COINS[index];
    }

    static int coinIndex(ItemStack stack) {
        if (stack == null || stack.getItemDamage() != 0 || stack.hasTagCompound()) return -1;
        for (int i = 0; i < COINS.length; i++) if (stack.getItem() == COINS[i]) return i;
        return -1;
    }

    public static int foodValue(ItemStack stack) {
        if (stack == null) return 0;
        String item = Item.itemRegistry.getNameForObject(stack.getItem());
        String variant = item + "@" + stack.getItemDamage();
        if (BLACKLIST.contains(item) || BLACKLIST.contains(variant)) return 0;
        Integer override = FOOD_OVERRIDES.get(variant);
        if (override == null) override = FOOD_OVERRIDES.get(item);
        if (override != null) return override;
        FoodAdapter adapter = ADAPTERS.get(stack.getItem());
        if (adapter != null) return Math.max(0, Math.min(1000000, adapter.value(single(stack))));
        if (!(stack.getItem() instanceof ItemFood)) return 0;
        ItemFood food = (ItemFood) stack.getItem();
        int hunger = Math.max(0, food.func_150905_g(stack));
        float saturation = Math.max(0, food.func_150906_h(stack)) * hunger * 2F;
        return (int) Math.min(1000000L, Math.round(hunger * (double) MilitaryConfig.hungerMultiplier
                + saturation * (double) MilitaryConfig.saturationMultiplier));
    }

    private static ItemStack remainder(ItemStack stack) {
        ItemStack input = single(stack);
        FoodAdapter adapter = ADAPTERS.get(input.getItem());
        if (adapter != null) return adapter.remainder(input);
        if (input.getItem().hasContainerItem(input)) return input.getItem().getContainerItem(input);
        return input.getItem() instanceof ItemSoup ? new ItemStack(Items.bowl) : null;
    }

    private static ItemStack single(ItemStack stack) {
        ItemStack result = stack.copy();
        result.stackSize = 1;
        return result;
    }

    private static void calculateChange(int[] values, int[] counts, byte[] last) {
        for (int i = 1; i < counts.length; i++) {
            counts[i] = i + 1;
            for (int denomination = values.length - 1; denomination >= 0; denomination--) {
                int remainder = i - values[denomination];
                if (remainder >= 0 && counts[remainder] + 1 < counts[i]) {
                    counts[i] = counts[remainder] + 1;
                    last[i] = (byte) denomination;
                }
            }
        }
    }

    static int[] change(int amount) {
        if (amount <= 0 || amount >= optimalCoins.length) return null;
        return unpack(amount, MilitaryConfig.denominations, optimalLastCoin);
    }

    public static int[] previewChange(int amount, int[] values) {
        if (amount <= 0 || amount > 1000000 || values.length != 4 || values[0] != 1
                || values[1] <= 1 || values[2] <= values[1] || values[3] <= values[2]) return null;
        int[] counts = new int[amount + 1];
        byte[] last = new byte[amount + 1];
        calculateChange(values, counts, last);
        return unpack(amount, values, last);
    }

    private static int[] unpack(int amount, int[] values, byte[] last) {
        int[] result = new int[values.length];
        while (amount > 0) {
            int denomination = last[amount];
            result[denomination]++;
            amount -= values[denomination];
        }
        return result;
    }

    static String withdraw(EntityPlayer player, int amount) {
        int[] change = change(amount);
        if (change == null) return "amount";
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(player.getUniqueID());
        if (account.currency < amount) return "funds";
        ItemStack[] result = copyInventory(player);
        for (int i = change.length - 1; i >= 0; i--) {
            if (change[i] > 0 && !insert(result, new ItemStack(COINS[i], change[i]))) return "space";
        }
        commit(player, result);
        account.currency -= amount;
        economy.changed(account);
        return "withdrawn";
    }

    static String deposit(EntityPlayer player, MilitaryContainer container, boolean food, int amount) {
        int slot = food ? 1 : 0;
        ItemStack source = container.deposit.getStackInSlot(slot);
        if (source == null || amount <= 0 || amount > source.stackSize) return "amount";
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(player.getUniqueID());
        int index = coinIndex(source);
        int value = food ? foodValue(source) : index < 0 ? 0 : MilitaryConfig.denominations[index];
        if (value <= 0) return "invalid";
        long addition = (long) value * amount;
        if ((food ? account.food : account.currency) > MilitaryEconomy.MAX_BALANCE - addition) return "overflow";
        if (food) {
            ItemStack[] result = copyInventory(player);
            for (int i = 0; i < amount; i++) {
                ItemStack returned = remainder(source);
                if (returned != null && returned.stackSize > 0 && (!returned.isItemStackDamageable()
                        || returned.getItemDamage() <= returned.getMaxDamage()) && !insert(result, returned)) return "space";
            }
            commit(player, result);
            account.food += addition;
        } else {
            account.currency += addition;
        }
        container.deposit.decrStackSize(slot, amount);
        economy.changed(account);
        return food ? "supplied" : "deposited";
    }

    static String depositAllCoins(EntityPlayer player, MilitaryContainer container) {
        ItemStack[] inventory = copyInventory(player);
        ItemStack deposit = container.deposit.getStackInSlot(0);
        int denomination = coinIndex(deposit);
        long total = denomination < 0 ? 0 : (long) MilitaryConfig.denominations[denomination] * deposit.stackSize;
        for (int i = 0; i < inventory.length; i++) {
            int coin = coinIndex(inventory[i]);
            if (coin < 0) continue;
            total += (long) MilitaryConfig.denominations[coin] * inventory[i].stackSize;
            inventory[i] = null;
        }
        if (total == 0) return "no_coins";
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(player.getUniqueID());
        if (account.currency > MilitaryEconomy.MAX_BALANCE - total) return "overflow";
        commit(player, inventory);
        container.deposit.setInventorySlotContents(0, null);
        account.currency += total;
        economy.changed(account);
        return "deposited";
    }

    private static ItemStack[] copyInventory(EntityPlayer player) {
        return Arrays.stream(player.inventory.mainInventory).map(stack -> stack == null ? null : stack.copy()).toArray(ItemStack[]::new);
    }

    private static void commit(EntityPlayer player, ItemStack[] result) {
        System.arraycopy(result, 0, player.inventory.mainInventory, 0, result.length);
        player.inventory.markDirty();
    }

    private static boolean insert(ItemStack[] inventory, ItemStack source) {
        int remaining = source.stackSize;
        int limit = source.getMaxStackSize();
        for (ItemStack existing : inventory) {
            if (remaining == 0) return true;
            if (existing == null || !existing.isItemEqual(source) || !ItemStack.areItemStackTagsEqual(source, existing)) continue;
            int moved = Math.min(remaining, Math.max(0, limit - existing.stackSize));
            existing.stackSize += moved;
            remaining -= moved;
        }
        for (int i = 0; i < inventory.length && remaining > 0; i++) {
            if (inventory[i] != null) continue;
            inventory[i] = source.copy();
            inventory[i].stackSize = Math.min(limit, remaining);
            remaining -= inventory[i].stackSize;
        }
        return remaining == 0;
    }
}
