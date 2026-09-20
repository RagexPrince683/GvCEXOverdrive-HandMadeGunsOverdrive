package handmadeguns.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import handmadeguns.tech.HMGTechTierManager;
import io.netty.buffer.ByteBuf;

public final class PacketTechTierSync implements IMessage {
    private boolean enabled;
    private int unlockedHalfSteps;
    private int[] maxYears = new int[11];

    public PacketTechTierSync() {}
    public PacketTechTierSync(boolean enabled, int unlockedHalfSteps, int[] maxYears) {
        this.enabled = enabled;
        this.unlockedHalfSteps = unlockedHalfSteps;
        System.arraycopy(maxYears, 0, this.maxYears, 0, Math.min(11, maxYears.length));
    }

    @Override public void fromBytes(ByteBuf buf) {
        enabled = buf.readBoolean();
        unlockedHalfSteps = buf.readByte();
        for (int i = 0; i < maxYears.length; i++) maxYears[i] = buf.readInt();
    }

    @Override public void toBytes(ByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeByte(unlockedHalfSteps);
        for (int value : maxYears) buf.writeInt(value);
    }

    public static final class Handler implements IMessageHandler<PacketTechTierSync, IMessage> {
        @Override public IMessage onMessage(PacketTechTierSync message, MessageContext context) {
            HMGTechTierManager.acceptClientState(message.enabled, message.unlockedHalfSteps, message.maxYears);
            return null;
        }
    }
}
