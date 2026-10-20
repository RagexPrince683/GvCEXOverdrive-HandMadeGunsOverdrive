package handmadeguns.items;

import cpw.mods.fml.common.registry.GameRegistry;
import handmadeguns.compat.HGMetalCompat;
import net.minecraft.block.BlockOre;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.BlockCompressed;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public class HGMetalBlocks {

    public static Block copperOre;
    public static Block aluminumOre;

    public static Block copperBlock;
    public static Block aluminumBlock;

    public static void init() {

        // ===== ORES =====

        copperOre = new BlockOre()
                .setBlockName("copperOre")
                .setBlockTextureName("handmadeguns:copper_ore")
                .setHardness(3.0F)
                .setResistance(5.0F)
                .setCreativeTab(HGMetalItems.tabHmgOres);
        GameRegistry.registerBlock(copperOre, "copperOre");

        aluminumOre = new BlockOre()
                .setBlockName("aluminumOre")
                .setBlockTextureName("handmadeguns:aluminum_ore")
                .setHardness(3.0F)
                .setResistance(5.0F)
                .setCreativeTab(HGMetalItems.tabHmgOres);
        GameRegistry.registerBlock(aluminumOre, "aluminumOre");

        // ===== STORAGE BLOCKS =====

        copperBlock = new BlockCompressed(MapColor.adobeColor)
                .setBlockName("copperBlock")
                .setBlockTextureName("handmadeguns:copper_block")
                .setHardness(5.0F)
                .setResistance(10.0F)
                .setStepSound(Block.soundTypeMetal)
                .setCreativeTab(HGMetalItems.tabHmgOres);
        GameRegistry.registerBlock(copperBlock, "copperBlock");

        aluminumBlock = new BlockCompressed(MapColor.lightBlueColor)
                .setBlockName("aluminumBlock")
                .setBlockTextureName("handmadeguns:aluminum_block")
                .setHardness(4.0F)
                .setResistance(8.0F)
                .setStepSound(Block.soundTypeMetal)
                .setCreativeTab(HGMetalItems.tabHmgOres);
        GameRegistry.registerBlock(aluminumBlock, "aluminumBlock");

        // ===== OREDICT =====

        OreDictionary.registerOre("oreCopper", copperOre);
        OreDictionary.registerOre("oreAluminum", aluminumOre);

        OreDictionary.registerOre("blockCopper", copperBlock);
        OreDictionary.registerOre("blockAluminum", aluminumBlock);
    }

    public static void registerRecipes() {
        GameRegistry.addSmelting(copperOre, new ItemStack(HGMetalItems.copperIngot), 0.7f);
        // RealSim owns aluminum refining, including ore left in older worlds.
        if (!HGMetalCompat.industrialProviderLoaded()) {
            GameRegistry.addSmelting(aluminumOre, new ItemStack(HGMetalItems.aluminumIngot), 0.7f);
        }

        GameRegistry.addRecipe(new ItemStack(copperBlock),
                "III",
                "III",
                "III",
                'I', HGMetalItems.copperIngot
        );
        GameRegistry.addRecipe(new ItemStack(aluminumBlock),
                "III",
                "III",
                "III",
                'I', HGMetalItems.aluminumIngot
        );

        GameRegistry.addShapelessRecipe(new ItemStack(HGMetalItems.copperIngot, 9),
                new ItemStack(copperBlock)
        );
        GameRegistry.addShapelessRecipe(new ItemStack(HGMetalItems.aluminumIngot, 9),
                new ItemStack(aluminumBlock)
        );

    }
}



