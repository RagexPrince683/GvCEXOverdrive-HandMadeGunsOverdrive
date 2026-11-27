package hmggvcmob.item;

import hmggvcmob.entity.friend.EntityPMCBase;
import hmggvcmob.entity.util.PlatoonOrder;
import hmggvcmob.military.ArmyOrders;
import hmggvcmob.military.MilitaryConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

import static hmggvcmob.GVCMobPlus.tabgvcm;
import static hmggvcutil.GVCUtils.platoonMatched;

public class GVCItemPMCDefSetter extends Item {
    private static final String ORDER_MODE = "GVCOrderMode";
    private static final PlatoonOrder[] ORDERS = {PlatoonOrder.Go, PlatoonOrder.Follow, PlatoonOrder.Patrol, PlatoonOrder.Defend, PlatoonOrder.Attack};
    private static final ArmyOrders.Order[] COMMANDS = {ArmyOrders.Order.MOVE, ArmyOrders.Order.FOLLOW,
            ArmyOrders.Order.PATROL, ArmyOrders.Order.DEFEND, ArmyOrders.Order.ATTACK};

    public GVCItemPMCDefSetter() {
        if (MilitaryConfig.legacyDevice) setCreativeTab(tabgvcm);
        setMaxStackSize(64);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote || !MilitaryConfig.legacyDevice) return stack;
        NBTTagCompound tag = stack.getTagCompound();
        int index = tag == null ? 0 : tag.getInteger(ORDER_MODE);
        // The previous index 2 meant vehicle landing.
        if (tag == null || !tag.getBoolean("GVCInfantryOrders") || index < 0 || index >= ORDERS.length) {
            index = index == 1 ? 1 : 0;
        }
        if (player.isSneaking()) {
            if (tag == null) {
                tag = new NBTTagCompound();
                stack.setTagCompound(tag);
            }
            index = (index + 1) % ORDERS.length;
            tag.setBoolean("GVCInfantryOrders", true);
            tag.setInteger(ORDER_MODE, index);
            player.addChatComponentMessage(new ChatComponentTranslation("gvc.order.selected", title(ORDERS[index])));
            return stack;
        }

        EntityPMCBase unit = searchEntity(stack, player, world);
        if (unit == null) {
            player.addChatComponentMessage(new ChatComponentTranslation("gvc.order.no_squad"));
            return stack;
        }
        if (stack.hasDisplayName()) unit.platoonName = stack.getDisplayName();
        ArmyOrders.legacy(player, unit, COMMANDS[index]);
        return stack;
    }

    private static ChatComponentTranslation title(PlatoonOrder order) {
        return new ChatComponentTranslation("gvc.order.mode." + order.name());
    }

    public EntityPMCBase searchEntity(ItemStack stack, EntityPlayer player, World world) {
        if (world.isRemote) return null;
        String name = stack.hasDisplayName() ? stack.getDisplayName() : null;
        double best = 80 * 80;
        EntityPMCBase nearest = null;
        for (Object nearby : world.getEntitiesWithinAABB(EntityPMCBase.class, player.boundingBox.expand(80, 16, 80))) {
            EntityPMCBase unit = (EntityPMCBase)nearby;
            double distance = player.getDistanceSqToEntity(unit);
            if (unit.canBeCommandedBy(player) && unit.isEntityAlive() && unit.ridingEntity == null
                    && distance < best && platoonMatched(name, unit)) {
                best = distance;
                nearest = unit;
            }
        }
        return nearest;
    }
}
