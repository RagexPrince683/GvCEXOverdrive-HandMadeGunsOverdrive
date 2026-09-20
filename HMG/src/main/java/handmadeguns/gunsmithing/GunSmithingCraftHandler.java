package handmadeguns.gunsmithing;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import java.util.List;

public class GunSmithingCraftHandler {

    //gun craft handler
    public static void handleCraft(EntityPlayer player, int recipeIndex) {
        if (!canHandleRequest(player)) return;
        List<GunSmithRecipe> list = GunSmithRecipeRegistry.getGunRecipes();
        if (list == null || recipeIndex < 0 || recipeIndex >= list.size()) return;
        GunSmithRecipe entry = list.get(recipeIndex);
        if (entry == null || entry.getOutput() == null) return;

        craftTransaction(player, entry);
    }

    // ---------------- SERVER-SIDE AMMO CRAFT ----------------
    public static void handleAmmoCraft(EntityPlayer player, int recipeIndex) {
        if (!canHandleRequest(player)) return;

        List<GunSmithRecipe> ammoList = GunSmithRecipeRegistry.getAmmoRecipes();

        if (ammoList == null || recipeIndex < 0 || recipeIndex >= ammoList.size()) {
            return;
        }

        GunSmithRecipe entry = ammoList.get(recipeIndex);
        if (entry == null || entry.getOutput() == null) {
            return;
        }

        craftTransaction(player, entry);
    }

    private static boolean canHandleRequest(EntityPlayer player) {
        return player != null && !player.worldObj.isRemote && player.openContainer instanceof ContainerGunSmith;
    }

    /**
     * Revalidates, consumes, and delivers under one inventory lock.  The packet only
     * selects a server-owned recipe; no client-provided stack participates here.
     */
    private static boolean craftTransaction(EntityPlayer player,
                                            GunSmithRecipe entry) {
		if (!handmadeguns.tech.HMGTechTierManager.isUnlocked(entry.getOutput(), player, player.worldObj)) {
			handmadeguns.tech.HMGTechTierManager.notifyLocked(player, entry.getOutput());
			return false;
		}
        synchronized (player.inventory) {
            GunTableInventoryAllocator.AllocationResult allocation =
                    GunTableInventoryAllocator.allocate(player, entry.getIngredients());
            if (!allocation.success || !GunTableInventoryAllocator.consume(player, allocation)) return false;

            ItemStack remainder = entry.getOutput();
            player.inventory.addItemStackToInventory(remainder);
            if (remainder.stackSize > 0) {
                player.dropPlayerItemWithRandomChoice(remainder, false);
            }
            synchronize(player);
            return true;
        }
    }

    private static void synchronize(EntityPlayer player) {
        player.inventory.markDirty();
        if (player.inventoryContainer != null) player.inventoryContainer.detectAndSendChanges();
        if (player.openContainer != null) player.openContainer.detectAndSendChanges();
        if (player instanceof EntityPlayerMP) {
            EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
            // ContainerGunSmith has no slots of its own, so explicitly send the
            // player inventory container as well as detecting the open container.
            serverPlayer.sendContainerToPlayer(serverPlayer.inventoryContainer);
        }
    }
}
