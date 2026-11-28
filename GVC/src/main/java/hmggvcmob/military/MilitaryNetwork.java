
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
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MilitaryNetwork {
    public static final int OPEN = 0, ROSTER = 1, WITHDRAW = 2, DEPOSIT_COINS = 3, DEPOSIT_FOOD = 4,
            TAB = 5, ALL_COINS = 6, HIRE = 7, GROUP_EDIT = 8, GROUP_FILTER = 9,
            COMMAND_MENU = 10, COMMAND_ORDER = 11, PLEDGE = 12, CAPTURE = 13, TRADE_QUERY = 14, TRADE = 15,
            QUEST = 16;
    public static final int COMMAND_MENU_WINDOW = -1, COMMAND_MARKER_WINDOW = -2, MANUAL_WINDOW = -3;
    public static final int ALLEGIANCE_WINDOW = -4;
    static final int GUI = 21;
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("GVCMilitary");
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, NBTTagCompound> HUD_SNAPSHOTS = new HashMap<>();

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

    static void syncAllegiance(EntityPlayerMP player) {
        updateReputationHud(player, true);
    }

    static void updateReputationHud(EntityPlayerMP player, boolean force) {
        UUID id = player.getUniqueID();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Allegiance", MilitaryRelations.allegiance(id));
        tag.setBoolean("Showcase", MilitarySites.isShowcase(player.worldObj));
        NBTTagList factions = new NBTTagList();
        for (String faction : MilitaryRelations.factions()) {
            if (!MilitaryRelations.available(faction)) continue;
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("Id", faction);
            entry.setString("Name", MilitaryRelations.displayName(faction));
            entry.setInteger("Color", MilitaryRelations.color(faction));
            int score = MilitaryRelations.reputation(id, faction);
            entry.setInteger("Reputation", score);
            entry.setString("Standing", MilitaryRelations.standing(score).label);
            factions.appendTag(entry);
        }
        tag.setTag("Factions", factions);
        if (force || !tag.equals(HUD_SNAPSHOTS.get(id))) {
            HUD_SNAPSHOTS.put(id, tag);
            send(player, ALLEGIANCE_WINDOW, tag);
        }
    }

    static void removeReputationHud(UUID player) { HUD_SNAPSHOTS.remove(player); }

    static void drain() {
        if (PENDING.isEmpty()) return;
        for (Object playerEntry : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) playerEntry;
            Pending batch = PENDING.remove(player.getUniqueID());
            if (batch == null || batch.sender != player || !player.isEntityAlive()) continue;
            if (player.openContainer == player.inventoryContainer) {
                if (batch.menu != null) {
                    MilitaryEconomy.Account account = MilitaryEconomy.get().account(player.getUniqueID());
                    long now = MilitaryEconomy.now();
                    if (now - account.lastMenu >= 4) {
                        account.lastMenu = now;
                        ArmyOrders.menu(player);
                    }
                }
                if (batch.order != null) ArmyOrders.issue(player, ArmyOrders.Order.values()[batch.order.value], batch.order.preset, true);
                if (batch.open != null) open(player);
            }
            if (player.openContainer instanceof MilitaryContainer) {
                deliver((MilitaryContainer)player.openContainer, batch.tab);
                deliver((MilitaryContainer)player.openContainer, batch.roster);
                deliver((MilitaryContainer)player.openContainer, batch.group);
                deliver((MilitaryContainer)player.openContainer, batch.trade);
                deliver((MilitaryContainer)player.openContainer, batch.transaction);
            }
        }
        // Drop requests from sessions that disconnected before the game thread handled them.
        PENDING.entrySet().removeIf(entry -> !MinecraftServer.getServer().getConfigurationManager().playerEntityList.contains(entry.getValue().sender));
    }

    static void clear() {
        PENDING.clear();
        HUD_SNAPSHOTS.clear();
    }

    private static void deliver(MilitaryContainer container, Request request) {
        if (request != null && container.windowId == request.window) {
            container.action(request.action, request.value, request.transaction, request.preset);
        }
    }

    private static final class Pending {
        final EntityPlayerMP sender;
        Request open, tab, roster, group, trade, transaction, menu, order;

        Pending(EntityPlayerMP sender) { this.sender = sender; }

        Pending enqueue(Request request) {
            if (request.action == OPEN) open = request;
            else if (request.action == TAB) tab = request;
            else if (request.action == ROSTER) roster = request;
            else if (request.action == GROUP_FILTER) group = request;
            else if (request.action == TRADE_QUERY) trade = request;
            else if (request.action == COMMAND_MENU) menu = request;
            else if (request.action == COMMAND_ORDER) {
                if (order == null) order = request;
            }
            else if (transaction == null) transaction = request;
            return this;
        }
    }

    private static boolean hasText(int action) {
        return action == HIRE || action == GROUP_EDIT || action == GROUP_FILTER || action == COMMAND_ORDER
                || action == PLEDGE || action == CAPTURE || action == TRADE_QUERY || action == TRADE || action == QUEST;
    }

    static void open(EntityPlayerMP player) {
        player.openGui(GVCMobPlus.INSTANCE, GUI, player.worldObj, -1, 0, 0);
    }

    static void openRecruitment(EntityPlayerMP player, EntityMilitaryTrader trader) {
        openProvider(player, trader);
    }

    static void openProvider(EntityPlayerMP player, net.minecraft.entity.Entity provider) {
        player.openGui(GVCMobPlus.INSTANCE, GUI, player.worldObj, provider.getEntityId(), 0, 0);
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
            preset = hasText(action) ? ByteBufUtils.readUTF8String(buffer) : "";
        }
        @Override public void toBytes(ByteBuf buffer) {
            buffer.writeInt(window).writeByte(action).writeInt(value).writeInt(transaction);
            if (hasText(action)) ByteBufUtils.writeUTF8String(buffer, preset);
        }
    }

    public static final class RequestHandler implements IMessageHandler<Request, IMessage> {
        @Override public IMessage onMessage(Request request, MessageContext context) {
            if (request.action < OPEN || request.action > QUEST || request.preset.length() > 20000
                    || request.action == QUEST && (request.preset.length() != 36 || request.value < 0 || request.value > 2)
                    || request.action == TRADE_QUERY && request.preset.length() > 64
                    || request.action == TRADE && request.preset.length() > 20
                    || request.action == HIRE && !MilitaryRelations.validId(request.preset)
                    || request.action == PLEDGE && !request.preset.isEmpty() && !MilitaryRelations.validId(request.preset)
                    || request.action == COMMAND_ORDER && (request.value < 0 || request.value >= ArmyOrders.Order.values().length
                        || request.preset.length() > 36)
                    || (request.action == CAPTURE || request.action == GROUP_FILTER) && request.preset.length() > 36) return null;
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
