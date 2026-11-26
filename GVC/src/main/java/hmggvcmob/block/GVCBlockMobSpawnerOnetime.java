package hmggvcmob.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import hmggvcmob.tile.TileEntityMobSpawner_OneTime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemNameTag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;
//import net.minecraft.world.gen.structure.StructureStrongholdPieces;;


public class GVCBlockMobSpawnerOnetime extends BlockContainer
{

    public GVCBlockMobSpawnerOnetime() {
        super(Material.rock);
        setCreativeTab(CreativeTabs.tabMisc);
        setHardness(1.5F);
        setResistance(1.0F);
        setStepSound(Block.soundTypeStone);
        this.setTickRandomly(true);
    }

    /**
     * Returns a new instance of a block's tile entity class. Called on placing the block.
     */
    public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_)
    {
        TileEntityMobSpawner_OneTime tileEntityMobSpawner_oneTime = new TileEntityMobSpawner_OneTime();
        tileEntityMobSpawner_oneTime.mobName = "GVCMob.Guerrillarpg";
        tileEntityMobSpawner_oneTime.offset.y = 1;
        return tileEntityMobSpawner_oneTime;
    }

    public Item getItemDropped(int p_149650_1_, Random p_149650_2_, int p_149650_3_)
    {
        return null;
    }

    /**
     * Returns the quantity of items to drop on block destruction.
     */
    public int quantityDropped(Random p_149745_1_)
    {
        return 0;
    }

    /**
     * Drops the block items with a specified chance of dropping the specified items
     */
    public void dropBlockAsItemWithChance(World p_149690_1_, int p_149690_2_, int p_149690_3_, int p_149690_4_, int p_149690_5_, float p_149690_6_, int p_149690_7_)
    {
        super.dropBlockAsItemWithChance(p_149690_1_, p_149690_2_, p_149690_3_, p_149690_4_, p_149690_5_, p_149690_6_, p_149690_7_);
    }

    private Random rand = new Random();
    @Override
    public int getExpDrop(IBlockAccess world, int metadata, int fortune)
    {
        return 0;
    }

    /**
     * Is this block (a) opaque and (b) a full 1m cube?  This determines whether or not to render the shared face of two
     * adjacent blocks and also whether the player can attach torches, redstone wire, etc to this block.
     */
    public boolean isOpaqueCube()
    {
        return false;
    }

    /**
     * Gets an item for the block being called on. Args: world, x, y, z
     */
    @SideOnly(Side.CLIENT)
    public Item getItem(World p_149694_1_, int p_149694_2_, int p_149694_3_, int p_149694_4_)
    {
        return Item.getItemById(0);
    }


    public boolean onBlockActivated(World world, int posX, int posY, int posZ, EntityPlayer player, int p_149727_6_, float p_149727_7_, float p_149727_8_, float p_149727_9_)
    {
        if (world.isRemote) return true;
        TileEntity tile = world.getTileEntity(posX, posY, posZ);
        if (!(tile instanceof TileEntityMobSpawner_OneTime)) return false;
        TileEntityMobSpawner_OneTime spawner = (TileEntityMobSpawner_OneTime) tile;
        ItemStack held = player.getCurrentEquippedItem();
        if (held == null) {
            player.addChatComponentMessage(new ChatComponentTranslation("gvc.spawner.configured",
                    new ChatComponentTranslation("entity." + spawner.mobName + ".name")));
            return true;
        }
        if (held.getItem() instanceof ItemNameTag && held.hasDisplayName()
                && held.getDisplayName().startsWith("GVCMob.")
                && player.canPlayerEdit(posX, posY, posZ, p_149727_6_, held)) {
            spawner.mobName = held.getDisplayName();
            spawner.vehicleName = "";
            spawner.markDirty();
            return true;
        }
        return false;
    }
}
