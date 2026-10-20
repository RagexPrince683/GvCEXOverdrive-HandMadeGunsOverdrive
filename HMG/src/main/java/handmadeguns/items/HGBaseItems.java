package handmadeguns.items;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class HGBaseItems {

    // ===================
    // ITEMS
    // ===================
    public static Item polymer;

    // ===================
    // CREATIVE TAB
    // ===================
    public static final CreativeTabs tabHMGCrafting = new CreativeTabs("HMGCrafting") {
        @Override
        public Item getTabIconItem() {
            return polymer;
        }
    };

    public static void init() {

        polymer = new Item()
                .setUnlocalizedName("polymer")
                .setTextureName("handmadeguns:polymer")
                .setCreativeTab(tabHMGCrafting);



        GameRegistry.registerItem(polymer, "polymer");

        // Synthetic gun furniture has no verified industrial polymer identity.
    }

    public static void registerRecipes() {
        GameRegistry.addShapelessRecipe(
                new ItemStack(polymer, 16),
                new ItemStack(Items.slime_ball),
                new ItemStack(Items.reeds)
        );
    }
}
