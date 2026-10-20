package handmadeguns.items;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import handmadeguns.compat.HGMetalCompat;

public class HGMetalRecipes {

    public static void init() {

        // === Existing fallback steel logic ===
        if (!HGMetalCompat.industrialProviderLoaded() && !HGMetalCompat.externalSteelExists()) {

            // Iron Infused Coal
            GameRegistry.addShapelessRecipe(
                    new ItemStack(HGMetalItems.ironInfusedCoal, 1),
                    new ItemStack(Items.iron_ingot, 1),
                    new ItemStack(Items.coal, 1)
            );

            // Smelt to steel
            GameRegistry.addSmelting(
                    HGMetalItems.ironInfusedCoal,
                    new ItemStack(HGMetalItems.steelIngot, 1),
                    0.3f
            );
        }

        // Equivalent copper/aluminum remain ore ingredients; never mint another provider's item.
    }
}
