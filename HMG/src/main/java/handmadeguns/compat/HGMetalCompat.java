package handmadeguns.compat;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import handmadeguns.items.HGBaseItems;
import handmadeguns.items.HGMetalBlocks;
import handmadeguns.items.HGMetalItems;
import net.minecraft.item.Item;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraft.item.ItemStack;

public class HGMetalCompat {
    // Recipe alternatives, never industrial commodity aliases. Forge recipes retain these live lists.
    public static final String STEEL_INPUT = "hmgInputSteel";
    public static final String POLYMER_INPUT = "hmgInputPolymer";

    public static boolean industrialProviderLoaded() {
        // RealSim and upstream HBM share this ID. Do not link either mod's classes.
        return Loader.isModLoaded("hbm");
    }

    public static void initializeRecipeInputs() {
        // Explicit full ingots only: no dusted steel, alloy grades, plates, rubber or raw fluids.
        addProvider(STEEL_INPUT, "ingot_steel");
        addProvider(POLYMER_INPUT, "ingot_polymer");
        addProvider(POLYMER_INPUT, "ingot_bakelite");
        addProvider(POLYMER_INPUT, "ingot_pc");
        addInput(STEEL_INPUT, new ItemStack(HGMetalItems.steelIngot));
        addInput(POLYMER_INPUT, new ItemStack(HGBaseItems.polymer));
        // Existing third-party ordinary steel is a local weapon ingredient, not a conversion output.
        for (ItemStack stack : OreDictionary.getOres("ingotSteel")) {
            if (stack != null && stack.getItem() != null && !stack.hasTagCompound()
                    && stack.getItemDamage() != OreDictionary.WILDCARD_VALUE) {
                String id = String.valueOf(Item.itemRegistry.getNameForObject(stack.getItem()));
                // HBM grades are accepted only by the explicit mapping above.
                if (!id.startsWith("hbm:") && !id.startsWith("mcheli:")) addInput(STEEL_INPUT, stack);
            }
        }
    }

    private static void addProvider(String key, String name) {
        Item item = GameRegistry.findItem("hbm", "item." + name);
        if (item != null) addInput(key, new ItemStack(item));
    }

    private static void addInput(String key, ItemStack stack) {
        for (ItemStack old : OreDictionary.getOres(key)) {
            if (old.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(old, stack)) return;
        }
        ItemStack copy = stack.copy();
        copy.stackSize = 1;
        OreDictionary.registerOre(key, copy);
    }

    /** Overrides automatic first-ore selection for HMG's private synthetic materials. */
    public static String recipeInput(ItemStack stack) {
        if (stack.getItemDamage() != 0 || stack.hasTagCompound()) return null;
        if (stack.getItem() == HGMetalItems.steelIngot) return STEEL_INPUT;
        if (stack.getItem() == HGBaseItems.polymer) return POLYMER_INPUT;
        return null;
    }

    public static boolean externalSteelExists() {
        for (ItemStack stack : OreDictionary.getOres(STEEL_INPUT)) {
            if (stack != null && stack.getItem() != HGMetalItems.steelIngot) {
                return true;
            }
        }
        return false;
    }
    public static boolean externalCopperExists() {
        return externalOreExists("oreCopper", Item.getItemFromBlock(HGMetalBlocks.copperOre));
    }

    public static boolean externalAluminumExists() {
        Item own = Item.getItemFromBlock(HGMetalBlocks.aluminumOre);
        return externalOreExists("oreAluminum", own) || externalOreExists("oreAluminium", own);
    }

    private static boolean externalOreExists(String key, Item own) {
        for (ItemStack stack : OreDictionary.getOres(key)) {
            if (stack != null && stack.getItem() != null && stack.getItem() != own) return true;
        }
        return false;
    }
}

