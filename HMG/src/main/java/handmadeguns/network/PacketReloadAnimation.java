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
    public ReloadAnimationBridge.Stage stage = ReloadAnimationBridge.Stage.STANDARD;

    public PacketReloadAnimation() { }

    public PacketReloadAnimation(ReloadAnimationBridge.StartEvent event) {
        eventId = event.eventId;
        slot = event.slot;
        itemId = event.itemId;
        empty = event.empty;
        stage = event.stage;
    }

    @Override public void fromBytes(ByteBuf buf) {
        eventId = buf.readInt();
        slot = buf.readByte();
        itemId = buf.readInt();
        empty = buf.readBoolean();
        stage = ReloadAnimationBridge.Stage.fromNetwork(buf.readByte());
    }

    @Override public void toBytes(ByteBuf buf) {
        buf.writeInt(eventId);
        buf.writeByte(slot);
        buf.writeInt(itemId);
        buf.writeBoolean(empty);
        buf.writeByte(stage.ordinal());
    }
}
