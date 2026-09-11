package handmadeguns.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import handmadeguns.animation.ReloadAnimationBridge;

/** Server-authorized, presentation-only reload start for the owning client. */
public final class PacketReloadAnimation implements IMessage {
    public int eventId;
    public int slot;
    public int itemId;
    public boolean empty;

    public PacketReloadAnimation() { }

    public PacketReloadAnimation(ReloadAnimationBridge.StartEvent event) {
        eventId = event.eventId;
        slot = event.slot;
        itemId = event.itemId;
        empty = event.empty;
    }

    @Override public void fromBytes(ByteBuf buf) {
        eventId = buf.readInt();
        slot = buf.readByte();
        itemId = buf.readInt();
        empty = buf.readBoolean();
    }

    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(eventId);
        buf.writeByte(slot);
        buf.writeInt(itemId);
        buf.writeBoolean(empty);
    }
}
