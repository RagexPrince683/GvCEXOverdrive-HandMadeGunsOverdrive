package handmadeguns.Util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

/** Server-owned ammunition entitlement; loaded rounds still cycle normally. */
public final class HMGAmmoPolicy {
    private static final String GLOBAL_RULE = "hmgInfiniteAmmo";
    private static final String SUPPLIED = "HMGInfiniteSupply";

    private HMGAmmoPolicy() { }

    public static boolean hasInfiniteAmmo(EntityPlayer player) {
        return player != null && !player.worldObj.isRemote
                && (player.capabilities.isCreativeMode
                || isGlobalInfiniteAmmo());
    }

    public static boolean hasInfiniteAmmo(Entity user) {
        return hasInfiniteAmmo(user instanceof EntityPlayer ? (EntityPlayer) user
                : user != null && user.riddenByEntity instanceof EntityPlayer ? (EntityPlayer) user.riddenByEntity : null);
    }

    /** One saved-world setting for every dimension, including future logins. */
    public static boolean isGlobalInfiniteAmmo() {
        return MinecraftServer.getServer().worldServerForDimension(0).getGameRules()
                .getGameRuleBooleanValue(GLOBAL_RULE);
    }

    public static void setGlobalInfiniteAmmo(boolean enabled) {
        MinecraftServer.getServer().worldServerForDimension(0).getGameRules()
                .setOrCreateGameRule(GLOBAL_RULE, Boolean.toString(enabled));
    }

    public static ItemStack suppliedMagazine(Item item) {
        ItemStack stack = new ItemStack(item, 1);
        markSupplied(stack);
        return stack;
    }

    public static void markSupplied(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setBoolean(SUPPLIED, true);
    }

    public static boolean isSupplied(ItemStack stack) {
        return stack != null && stack.hasTagCompound() && stack.getTagCompound().getBoolean(SUPPLIED);
    }
}
