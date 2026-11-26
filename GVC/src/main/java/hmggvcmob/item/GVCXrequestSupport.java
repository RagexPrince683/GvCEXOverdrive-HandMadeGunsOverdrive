package hmggvcmob.item;

import hmggvcmob.entity.EntitySupportTGT;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import static hmggvcmob.GVCMobPlus.tabgvcm;
import static hmggvcmob.GVCMobPlus.VEHICLES_ENABLED;

public class GVCXrequestSupport extends Item {
    public int type;
    public GVCXrequestSupport(int t){
        super();
        this.maxStackSize = 64;
        type = t;
        if (t == 0 || VEHICLES_ENABLED) this.setCreativeTab(tabgvcm);
    }
    public ItemStack onItemRightClick(ItemStack p_77659_1_, World world, EntityPlayer entityplayer)
    {
        if (!world.isRemote && (type == 0 || VEHICLES_ENABLED)) {
            EntitySupportTGT var8 = new EntitySupportTGT(world, (EntityLivingBase) entityplayer, 20.0F);
            var8.type = type;
            hmggvcmob.util.GVCUtil.spawnFromItem(world, var8, entityplayer, p_77659_1_);
        }
        return p_77659_1_;
    }
}
