package handmadeguns.guide;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import handmadeguns.HandmadeGunsCore;
import io.netty.buffer.ByteBuf;

public final class PacketFieldManual implements IMessage {
    @Override public void fromBytes(ByteBuf data) {}
    @Override public void toBytes(ByteBuf data) {}

    public static final class Handler implements IMessageHandler<PacketFieldManual, IMessage> {
        @Override public IMessage onMessage(PacketFieldManual message, MessageContext context) {
            HandmadeGunsCore.HMG_proxy.openFieldManual();
            return null;
        }
    }
}
