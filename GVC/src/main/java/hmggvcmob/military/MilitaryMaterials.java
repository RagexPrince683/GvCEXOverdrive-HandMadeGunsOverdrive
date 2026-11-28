package hmggvcmob.military;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/** Common-side registration for the materials placed by regional sites. */
final class MilitaryMaterials {
    static final Block SANDBAGS = new CoverBlock(Material.cloth)
            .setHardness(1.1F).setResistance(8F).setStepSound(Block.soundTypeCloth);
    static final Block CONCRETE = new CoverBlock(Material.rock)
            .setHardness(3F).setResistance(14F).setStepSound(Block.soundTypeStone);
    static final Block CORRUGATED = new CoverBlock(Material.iron)
            .setHardness(2F).setResistance(6F).setStepSound(Block.soundTypeMetal);

    private MilitaryMaterials() {}

    static void register() {
        register(SANDBAGS, "sandbag_wall");
        register(CONCRETE, "field_concrete");
        register(CORRUGATED, "corrugated_steel");
        CONCRETE.setHarvestLevel("pickaxe", 1);
        CORRUGATED.setHarvestLevel("pickaxe", 0);
        GameRegistry.addRecipe(new ItemStack(SANDBAGS, 4), "WSW", "SWS", 'W', Items.string, 'S', Blocks.sand);
        GameRegistry.addRecipe(new ItemStack(CONCRETE, 4), "SG", "GC", 'S', Blocks.sand, 'G', Blocks.gravel, 'C', Items.clay_ball);
        GameRegistry.addRecipe(new ItemStack(CORRUGATED, 4), "III", 'I', Items.iron_ingot);
    }

    private static void register(Block block, String id) {
        block.setBlockName("gvcmob." + id).setBlockTextureName("gvcmob:" + id).setCreativeTab(CreativeTabs.tabBlock);
        GameRegistry.registerBlock(block, id);
    }

    private static final class CoverBlock extends Block {
        CoverBlock(Material material) { super(material); }
    }
}
