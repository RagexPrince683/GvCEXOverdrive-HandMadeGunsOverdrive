package handmadeguns.Handler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import handmadeguns.network.PacketReloadAnimation;

import static handmadeguns.HandmadeGunsCore.HMG_proxy;

public final class MessageCatcher_ReloadAnimation implements IMessageHandler<PacketReloadAnimation, IMessage> {
    @Override public IMessage onMessage(PacketReloadAnimation message, MessageContext ctx) {
        HMG_proxy.handleReloadAnimation(message.eventId, message.slot, message.itemId, message.empty);
        return null;
    }
}
