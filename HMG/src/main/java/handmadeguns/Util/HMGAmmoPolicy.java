package handmadeguns.Util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Server-owned ammunition entitlement; loaded rounds still cycle normally. */
public final class HMGAmmoPolicy {
    private static final String ENABLED = "HMGInfiniteAmmo";
    private static final String SUPPLIED = "HMGInfiniteSupply";

    private HMGAmmoPolicy() { }

    public static boolean hasInfiniteAmmo(EntityPlayer player) {
        return player != null && !player.worldObj.isRemote
                && (player.capabilities.isCreativeMode
                || player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).getBoolean(ENABLED));
    }

    public static boolean hasInfiniteAmmo(Entity user) {
        return hasInfiniteAmmo(user instanceof EntityPlayer ? (EntityPlayer) user
                : user != null && user.riddenByEntity instanceof EntityPlayer ? (EntityPlayer) user.riddenByEntity : null);
    }

    public static void setInfiniteAmmo(EntityPlayer player, boolean enabled) {
        if (player.worldObj.isRemote) return;
        NBTTagCompound persisted = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        persisted.setBoolean(ENABLED, enabled);
        player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
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
