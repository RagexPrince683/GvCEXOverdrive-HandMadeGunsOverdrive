package hmggvcmob.block;

import hmggvcmob.camp.CampObj;
import hmggvcmob.tile.TileEntityFlag;
import hmggvcmob.military.MilitaryRespawn;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.Random;

import static hmggvcmob.GVCMobPlus.beacon_defensive;
import static hmggvcmob.GVCMobPlus.fn_PlayerFlag;

public class GVCBlockFlag extends BlockContainer {
    public CampObj campObj;
    public GVCBlockFlag(CampObj campObj) {
        super(Material.rock);
        setCreativeTab(CreativeTabs.tabMisc);
        setHardness(1.5F);
        setResistance(2000.0F);
        setStepSound(Block.soundTypeStone);
        this.setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
        this.setTickRandomly(true);
        this.campObj = campObj;
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
     * If this block doesn't render as an ordinary block it will return False (examples: signs, buttons, stairs, etc)
     */
    public boolean renderAsNormalBlock()
    {
        return false;
    }

    public int tickRate(World p_149738_1_)
    {
        return 1;
    }
    @Override
    public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_)
    {
        if(this == fn_PlayerFlag && !p_149915_1_.isRemote){
            for(Object obj : p_149915_1_.playerEntities){
                if(obj instanceof EntityPlayer){
                    EntityPlayer entityPlayer = (EntityPlayer) obj;
                    entityPlayer.addStat(beacon_defensive,1);
                }
            }
        }
        return new TileEntityFlag(campObj);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player,
                                    int side, float hitX, float hitY, float hitZ) {
        if (!player.isSneaking()) return false;
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof TileEntityFlag && MilitaryRespawn.bind(player, (TileEntityFlag) tile);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        if (!world.isRemote && this == fn_PlayerFlag && placer instanceof EntityPlayer) {
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof TileEntityFlag) {
                hmggvcmob.military.Occupation post = ((TileEntityFlag) tile).occupation();
                if (post != null) {
                    post.owner = placer.getUniqueID();
                    hmggvcmob.world.WorldSavedData_Flag.get(world).markDirty();
                }
            }
        }
    }




    public void breakBlock(World p_149749_1_, int x, int y, int z, Block p_149749_5_, int p_149749_6_)
    {
        TileEntity tile = p_149749_1_.getTileEntity(x, y, z);
        if (!p_149749_1_.isRemote && tile instanceof TileEntityFlag) {
            ((TileEntityFlag) tile).removeFlagData();
        }
        super.breakBlock(p_149749_1_, x, y, z, p_149749_5_, p_149749_6_);
    }


    public Item getItemDropped(int p_149650_1_, Random p_149650_2_, int p_149650_3_)
    {
        return Item.getItemFromBlock(this);
    }
}
