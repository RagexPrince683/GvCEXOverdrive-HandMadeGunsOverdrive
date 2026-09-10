package handmadeguns.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import handmadeguns.event.HMGJumpHandler;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;

/** Server gun mobility values; clients never read local packs for jump policy. */
public final class PacketJumpPolicy implements IMessage {
    private Map<Integer, Double> policy;

    public PacketJumpPolicy() { }

    public PacketJumpPolicy(Map<Integer, Double> policy) {
        this.policy = policy;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        if (count < 0 || count > 65536 || count > buf.readableBytes() / 12) {
            throw new IllegalArgumentException("Invalid HMG jump policy size");
        }
        policy = new HashMap<>();
        for (int i = 0; i < count; i++) policy.put(buf.readInt(), buf.readDouble());
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(policy.size());
        for (Map.Entry<Integer, Double> entry : policy.entrySet()) {
            buf.writeInt(entry.getKey());
            buf.writeDouble(entry.getValue());
        }
    }

    public static final class Handler implements IMessageHandler<PacketJumpPolicy, IMessage> {
        @Override
        public IMessage onMessage(PacketJumpPolicy message, MessageContext ctx) {
            HMGJumpHandler.receivePolicy(ctx.netHandler, message.policy);
            return null;
        }
    }
}
