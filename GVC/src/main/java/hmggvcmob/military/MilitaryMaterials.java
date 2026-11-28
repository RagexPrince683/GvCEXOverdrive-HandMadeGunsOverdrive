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
    static final Block BLAST_WALL = stone(4F, 18F);
    static final Block STEEL_PLATE = new CoverBlock(Material.iron).setHardness(3F).setResistance(12F)
            .setStepSound(Block.soundTypeMetal);
    static final Block CAMOUFLAGE = new CoverBlock(Material.cloth).setHardness(.65F).setStepSound(Block.soundTypeCloth);
    static final Block REVETMENT = new CoverBlock(Material.wood).setHardness(1.5F).setStepSound(Block.soundTypeWood);
    static final Block TREADWAY = stone(1.5F, 7F);
    static final Block BATTLE_RUBBLE = stone(.9F, 4F);
    static final Block BARBED_WIRE = new BlockMilitaryWire().setHardness(1.3F).setResistance(4F).setStepSound(Block.soundTypeMetal);

    private MilitaryMaterials() {}

    private static Block stone(float hardness, float resistance) {
        return new CoverBlock(Material.rock).setHardness(hardness).setResistance(resistance).setStepSound(Block.soundTypeStone);
    }

    static void register() {
        register(SANDBAGS, "sandbag_wall");
        register(CONCRETE, "field_concrete");
        register(CORRUGATED, "corrugated_steel");
        register(BLAST_WALL, "blast_wall");
        register(STEEL_PLATE, "steel_plate");
        register(CAMOUFLAGE, "camouflage_canvas");
        register(REVETMENT, "timber_revetment");
        register(TREADWAY, "military_treadway");
        register(BATTLE_RUBBLE, "battle_rubble");
        register(BARBED_WIRE, "barbed_wire");
        CONCRETE.setHarvestLevel("pickaxe", 1);
        CORRUGATED.setHarvestLevel("pickaxe", 0);
        BLAST_WALL.setHarvestLevel("pickaxe", 1);
        STEEL_PLATE.setHarvestLevel("pickaxe", 0);
        GameRegistry.addRecipe(new ItemStack(SANDBAGS, 4), "WSW", "SWS", 'W', Items.string, 'S', Blocks.sand);
        GameRegistry.addRecipe(new ItemStack(CONCRETE, 4), "SG", "GC", 'S', Blocks.sand, 'G', Blocks.gravel, 'C', Items.clay_ball);
        GameRegistry.addRecipe(new ItemStack(CORRUGATED, 4), "III", 'I', Items.iron_ingot);
        GameRegistry.addShapelessRecipe(new ItemStack(BLAST_WALL, 2), CONCRETE, CONCRETE, Items.iron_ingot);
        GameRegistry.addShapelessRecipe(new ItemStack(STEEL_PLATE, 2), CORRUGATED, CORRUGATED, Items.iron_ingot);
        GameRegistry.addShapelessRecipe(new ItemStack(CAMOUFLAGE, 3), Blocks.wool, Blocks.leaves, Items.string);
        GameRegistry.addShapelessRecipe(new ItemStack(REVETMENT, 4), Blocks.log, Blocks.planks, Items.iron_ingot);
        GameRegistry.addShapelessRecipe(new ItemStack(TREADWAY, 3), CONCRETE, Blocks.gravel, Blocks.gravel);
        GameRegistry.addShapelessRecipe(new ItemStack(BATTLE_RUBBLE, 2), Blocks.cobblestone, Blocks.gravel);
        GameRegistry.addRecipe(new ItemStack(BARBED_WIRE, 6), "ISI", "SIS", 'I', Items.iron_ingot, 'S', Items.string);
    }

    private static void register(Block block, String id) {
        block.setBlockName("gvcmob." + id).setBlockTextureName("gvcmob:" + id).setCreativeTab(CreativeTabs.tabBlock);
        GameRegistry.registerBlock(block, id);
    }

    private static final class CoverBlock extends Block {
        CoverBlock(Material material) { super(material); }
    }

}
