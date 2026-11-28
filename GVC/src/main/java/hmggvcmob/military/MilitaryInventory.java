package hmggvcmob.military;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

final class MilitaryInventory {
    private final InventoryPlayer inventory;
    private final ItemStack[] stacks;

    MilitaryInventory(InventoryPlayer inventory) {
        this.inventory = inventory;
        stacks = new ItemStack[inventory.mainInventory.length];
        for (int index = 0; index < stacks.length; index++) {
            ItemStack stack = inventory.mainInventory[index];
            stacks[index] = stack == null ? null : stack.copy();
        }
    }

    boolean add(ItemStack item, int quantity) {
        int capacity = Math.min(item.getMaxStackSize(), inventory.getInventoryStackLimit());
        for (ItemStack stack : stacks) {
            if (quantity == 0) break;
            if (stack == null || !same(item, stack)) continue;
            int count = Math.min(Math.max(0, capacity - stack.stackSize), quantity);
            stack.stackSize += count;
            quantity -= count;
        }
        for (int index = 0; quantity > 0 && index < stacks.length; index++) {
            if (stacks[index] != null) continue;
            ItemStack stack = item.copy();
            stack.stackSize = Math.min(capacity, quantity);
            stacks[index] = stack;
            quantity -= stack.stackSize;
        }
        return quantity == 0;
    }

    boolean take(ItemStack item, int count) {
        for (int index = 0; index < stacks.length && count > 0; index++) {
            ItemStack stack = stacks[index];
            if (stack == null || !same(item, stack)) continue;
            int amount = Math.min(count, stack.stackSize);
            takeSlot(index, amount);
            count -= amount;
        }
        return count == 0;
    }

    void takeSlot(int slot, int count) {
        stacks[slot].stackSize -= count;
        if (stacks[slot].stackSize == 0) stacks[slot] = null;
    }

    void commit() {
        for (int index = 0; index < stacks.length; index++) {
            if (!ItemStack.areItemStacksEqual(stacks[index], inventory.mainInventory[index])) {
                inventory.setInventorySlotContents(index, stacks[index]);
            }
        }
        inventory.markDirty();
    }

    private static boolean same(ItemStack a, ItemStack b) {
        return a.isItemEqual(b) && ItemStack.areItemStackTagsEqual(a, b);
    }
}
