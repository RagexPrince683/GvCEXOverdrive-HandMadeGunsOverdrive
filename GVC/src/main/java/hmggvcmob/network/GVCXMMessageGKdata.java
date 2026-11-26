package hmggvcmob.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import hmggvcmob.GVCMobPlus;
import hmggvcmob.entity.guerrilla.GVCEntityGK;
import net.minecraft.entity.Entity;

public class GVCXMMessageGKdata implements IMessageHandler<GVCXMPacketSyncGKdata, IMessage> {
    @Override
    public IMessage onMessage(GVCXMPacketSyncGKdata message, MessageContext context) {
        if (context.side == Side.CLIENT && message.data != null) {
            GVCMobPlus.proxy.scheduleClientPacket(world -> {
                Entity entity = world.getEntityByID(message.entityid);
                if (entity instanceof GVCEntityGK) {
                    GVCEntityGK irving = (GVCEntityGK) entity;
                    irving.combattask_2 = message.data.combattask_2;
                    irving.combattask_4 = message.data.combattask_4;
                    irving.kickprogeress = message.data.cooltime_3;
                }
            });
        }
        return null;
    }
}
