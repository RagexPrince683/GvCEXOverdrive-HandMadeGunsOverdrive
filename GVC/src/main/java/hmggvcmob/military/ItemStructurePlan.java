package hmggvcmob.military;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.List;

final class ItemStructurePlan extends Item {
    ItemStructurePlan() {
        setUnlocalizedName("gvcmob.structure_plan");
        setTextureName("gvcmob:structure_plan");
        setMaxStackSize(1);
        setCreativeTab(hmggvcmob.GVCMobPlus.tabgvcm);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void getSubItems(Item item, CreativeTabs tab, List variants) {
        for (String id : MilitarySites.ids()) {
            ItemStack stack = new ItemStack(item);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Site", id);
            stack.setTagCompound(tag);
            variants.add(stack);
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return site(stack).isEmpty() ? super.getItemStackDisplayName(stack)
                : super.getItemStackDisplayName(stack) + " — " + site(stack).replace('_', ' ');
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, List tips, boolean advanced) {
        tips.add(net.minecraft.util.StatCollector.translateToLocal("gvc.military.structure.plan_help"));
        tips.add(net.minecraft.util.StatCollector.translateToLocal("gvc.military.structure.plan_live"));
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z,
                             int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        if (!player.capabilities.isCreativeMode || !player.canCommandSenderUseCommand(2, "militaryadmin")) {
            player.addChatMessage(new ChatComponentTranslation("gvc.military.structure.permission"));
            return true;
        }
        boolean preview = !player.isSneaking();
        String site = site(stack);
        MilitarySites.Definition definition = MilitarySites.definition(site);
        if (definition == null) {
            player.addChatMessage(new ChatComponentTranslation("gvc.military.structure.invalid", site));
            return true;
        }
        int turn = MathHelper.floor_double(player.rotationYaw * 4 / 360F + .5) & 3;
        // The clicked block is the approach, so the player stands outside the construction area.
        int distance = definition.width / 2 + 3;
        x += turn == 1 ? -distance : turn == 3 ? distance : 0;
        z += turn == 0 ? distance : turn == 2 ? -distance : 0;
        boolean placed = MilitarySites.testPlacement(world, site, x, z, turn, preview);
        player.addChatMessage(new ChatComponentTranslation("gvc.military.structure." + (placed ? "placed" : "rejected"), site, x, z));
        return true;
    }

    private static String site(ItemStack stack) {
        return stack.hasTagCompound() ? stack.getTagCompound().getString("Site") : "";
    }
}
