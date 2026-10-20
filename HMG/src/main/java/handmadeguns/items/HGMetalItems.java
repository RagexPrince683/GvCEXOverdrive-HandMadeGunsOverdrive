package handmadeguns.items;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.oredict.OreDictionary;

public class HGMetalItems {

    public static Item steelIngot;
    public static Item copperIngot;
    public static Item aluminumIngot;

    // Steel fallback production
    public static Item ironInfusedCoal;

    // New creative tab for ores & metals
    public static final CreativeTabs tabHmgOres = new CreativeTabs("HMG_Ores") {
        @Override
        public Item getTabIconItem() {
            return copperIngot; // tab icon
        }
    };

    public static void init() {

        steelIngot = new Item()
                .setUnlocalizedName("steelIngot")
                .setTextureName("handmadeguns:steel_ingot")
                .setCreativeTab(tabHmgOres);

        copperIngot = new Item()
                .setUnlocalizedName("copperIngot")
                .setTextureName("handmadeguns:copper_ingot")
                .setCreativeTab(tabHmgOres);

        aluminumIngot = new Item()
                .setUnlocalizedName("aluminumIngot")
                .setTextureName("handmadeguns:aluminum_ingot")
                .setCreativeTab(tabHmgOres);

        ironInfusedCoal = new Item()
                .setUnlocalizedName("ironInfusedCoal")
                .setTextureName("handmadeguns:iron_infused_coal")
                .setCreativeTab(tabHmgOres);

        GameRegistry.registerItem(steelIngot, "steelIngot");
        GameRegistry.registerItem(copperIngot, "copperIngot");
        GameRegistry.registerItem(aluminumIngot, "aluminumIngot");
        GameRegistry.registerItem(ironInfusedCoal, "ironInfusedCoal");

        // === OREDICT REGISTRATION ===
        // Fallback steel is a private weapon material, not a certified industrial steel grade.
        OreDictionary.registerOre("ingotCopper", copperIngot);
        OreDictionary.registerOre("ingotAluminum", aluminumIngot);
    }
}



