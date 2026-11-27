
package hmggvcmob.military;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import hmggvcmob.GVCMobPlus;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MilitaryNetwork {
    public static final int OPEN = 0, ROSTER = 1, WITHDRAW = 2, DEPOSIT_COINS = 3, DEPOSIT_FOOD = 4,
            TAB = 5, ALL_COINS = 6, HIRE = 7;
    static final int GUI = 21;
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("GVCMilitary");
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private MilitaryNetwork() {}

    static void init() {
        CHANNEL.registerMessage(RequestHandler.class, Request.class, 0, Side.SERVER);
        CHANNEL.registerMessage(SnapshotHandler.class, Snapshot.class, 1, Side.CLIENT);
    }

    public static void request(int window, int action, int value) {
        request(window, action, value, 0, "");
    }

    public static void request(int window, int action, int value, int transaction, String preset) {
        CHANNEL.sendToServer(new Request(window, action, value, transaction, preset));
    }

    static void send(EntityPlayerMP player, int window, NBTTagCompound snapshot) {
        CHANNEL.sendTo(new Snapshot(window, snapshot), player);
    }

    static void drain() {
        if (PENDING.isEmpty()) return;
        for (Object playerEntry : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) playerEntry;
            Pending batch = PENDING.remove(player.getUniqueID());
            if (batch == null || batch.sender != player || !player.isEntityAlive()) continue;
            if (batch.open != null && player.openContainer == player.inventoryContainer) open(player);
            if (player.openContainer instanceof MilitaryContainer) {
                deliver((MilitaryContainer)player.openContainer, batch.tab);
                deliver((MilitaryContainer)player.openContainer, batch.roster);
                deliver((MilitaryContainer)player.openContainer, batch.transaction);
            }
        }
        // Drop requests from sessions that disconnected before the game thread handled them.
        PENDING.entrySet().removeIf(entry -> !MinecraftServer.getServer().getConfigurationManager().playerEntityList.contains(entry.getValue().sender));
    }

    private static void deliver(MilitaryContainer container, Request request) {
        if (request != null && container.windowId == request.window) {
            container.action(request.action, request.value, request.transaction, request.preset);
        }
    }

    private static final class Pending {
        final EntityPlayerMP sender;
        Request open, tab, roster, transaction;

        Pending(EntityPlayerMP sender) { this.sender = sender; }

        Pending enqueue(Request request) {
            if (request.action == OPEN) open = request;
            else if (request.action == TAB) tab = request;
            else if (request.action == ROSTER) roster = request;
            else if (transaction == null) transaction = request;
            return this;
        }
    }

    static void open(EntityPlayerMP player) {
        player.openGui(GVCMobPlus.INSTANCE, GUI, player.worldObj, -1, 0, 0);
    }

    static void openRecruitment(EntityPlayerMP player, EntityMilitaryTrader trader) {
        player.openGui(GVCMobPlus.INSTANCE, GUI, player.worldObj, trader.getEntityId(), 0, 0);
    }

    public static final class Request implements IMessage {
        int window, action, value, transaction;
        String preset;
        public Request() {}
        Request(int window, int action, int value, int transaction, String preset) {
            this.window = window;
            this.action = action;
            this.value = value;
            this.transaction = transaction;
            this.preset = preset;
        }
        @Override public void fromBytes(ByteBuf buffer) {
            window = buffer.readInt();
            action = buffer.readUnsignedByte();
            value = buffer.readInt();
            transaction = buffer.readInt();
            preset = action == HIRE ? ByteBufUtils.readUTF8String(buffer) : "";
        }
        @Override public void toBytes(ByteBuf buffer) {
            buffer.writeInt(window).writeByte(action).writeInt(value).writeInt(transaction);
            if (action == HIRE) ByteBufUtils.writeUTF8String(buffer, preset);
        }
    }

    public static final class RequestHandler implements IMessageHandler<Request, IMessage> {
        @Override public IMessage onMessage(Request request, MessageContext context) {
            if (request.action < OPEN || request.action > HIRE
                    || request.action == HIRE && !request.preset.matches("[a-z0-9_.-]{1,48}")) return null;
            EntityPlayerMP player = context.getServerHandler().playerEntity;
            PENDING.compute(player.getUniqueID(), (id, previous) ->
                    (previous == null || previous.sender != player ? new Pending(player) : previous).enqueue(request));
            return null;
        }
    }

    public static final class Snapshot implements IMessage {
        int window;
        NBTTagCompound data;
        public Snapshot() {}
        Snapshot(int window, NBTTagCompound data) {
            this.window = window;
            this.data = data;
        }
        @Override public void fromBytes(ByteBuf buffer) {
            window = buffer.readInt();
            data = ByteBufUtils.readTag(buffer);
        }
        @Override public void toBytes(ByteBuf buffer) {
            buffer.writeInt(window);
            ByteBufUtils.writeTag(buffer, data);
        }
    }

    public static final class SnapshotHandler implements IMessageHandler<Snapshot, IMessage> {
        @Override public IMessage onMessage(Snapshot message, MessageContext context) {
            GVCMobPlus.proxy.handleMilitarySnapshot(message.window, message.data);
            return null;
        }
    }
}
