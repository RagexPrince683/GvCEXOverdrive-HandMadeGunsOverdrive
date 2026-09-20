package handmadeguns.Handler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import handmadeguns.items.guns.HMGItem_Unified_Guns;
import handmadeguns.network.PacketTriggerHeld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import static handmadeguns.HandmadeGunsCore.HMG_proxy;

public class MessageCatcher_TriggerHeld implements IMessageHandler<PacketTriggerHeld, IMessage> {
    @Override
    public IMessage onMessage(PacketTriggerHeld message, MessageContext ctx) {
        World world;
        if (ctx.side.isServer()) {
            world = ctx.getServerHandler().playerEntity.worldObj;
        } else {
            world = HMG_proxy.getCilentWorld();
        }
        Entity entity = world.getEntityByID(message.playerid);
        if(entity instanceof EntityPlayer){
            ItemStack itemStack = ((EntityPlayer) entity).getHeldItem();
            if(itemStack != null && itemStack.getItem() instanceof HMGItem_Unified_Guns){
				if (!handmadeguns.tech.HMGTechTierManager.isUnlocked(itemStack, (EntityPlayer) entity, world)) {
					handmadeguns.tech.HMGTechTierManager.notifyLocked((EntityPlayer) entity, itemStack);
					return null;
				}
                ((HMGItem_Unified_Guns) itemStack.getItem()).triggerHeldGun(itemStack);
            }
        }
        return null;
    }
}
