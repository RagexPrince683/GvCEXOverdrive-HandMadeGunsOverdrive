package handmadeguns.Handler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import handmadeguns.entity.PlacedGunEntity;
import handmadeguns.HMGPacketHandler;
import handmadeguns.animation.ReloadAnimationBridge;
import handmadeguns.items.guns.HMGItem_Unified_Guns;
import handmadeguns.network.HMGServerTaskQueue;
import handmadeguns.network.PacketReloadAnimation;
import handmadeguns.network.PacketreturnMgazineItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class MessageCatcher_returnMagazineItem implements IMessageHandler<PacketreturnMgazineItem, IMessage> {
    @Override
    public IMessage onMessage(final PacketreturnMgazineItem message, MessageContext ctx) {
        final EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
        HMGServerTaskQueue.enqueue(new Runnable() {
            @Override public void run() { handleReload(message.entityid, sender); }
        });
        return null;
    }

    private static void handleReload(int requestedEntityId, EntityPlayerMP player) {
        if (player == null || player.worldObj == null || player.getEntityId() != requestedEntityId) return;
        ItemStack itemStack = player.getHeldItem();
        if (itemStack != null && itemStack.getItem() instanceof HMGItem_Unified_Guns) {
            HMGItem_Unified_Guns gun = (HMGItem_Unified_Guns)itemStack.getItem();
            gun.checkTags(itemStack);
            boolean wasReloading = itemStack.getTagCompound().getBoolean("IsReloading");
            boolean empty = gun.remain_Bullet(itemStack) == 0;
            boolean accepted = gun.startReloadFromKey(itemStack, player.worldObj, player)
                    && !wasReloading && itemStack.getTagCompound().getBoolean("IsReloading");
            ReloadAnimationBridge.Stage stage = gun.reloadPresentationStage(itemStack);
            ReloadAnimationBridge.StartEvent event = accepted ? ReloadAnimationBridge.nextEvent(
                    player.inventory.currentItem, Item.getIdFromItem(itemStack.getItem()), empty, stage) : null;
            if (event != null) HMGPacketHandler.INSTANCE.sendTo(new PacketReloadAnimation(event), player);
            return;
        }
        if (player.ridingEntity instanceof PlacedGunEntity) {
            PlacedGunEntity placed = (PlacedGunEntity)player.ridingEntity;
            if (placed.gunItem != null && placed.gunStack != null)
                placed.gunItem.startReloadFromKey(placed.gunStack, player.worldObj, player);
        }
    }
}
