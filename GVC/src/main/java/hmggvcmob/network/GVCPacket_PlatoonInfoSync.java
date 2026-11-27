package hmggvcmob.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.ByteBufUtils;
import hmggvcmob.entity.IPlatoonable;
import hmggvcmob.entity.util.PlatoonInfoData;
import hmggvcmob.entity.util.PlatoonOrder;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import static hmggvcmob.GVCMobPlus.proxy;

public class GVCPacket_PlatoonInfoSync implements IMessage , IMessageHandler<GVCPacket_PlatoonInfoSync, IMessage> {
	boolean isLeader;
	boolean isOnPlatoon;
	double[] target;
	int targetEntity;
	PlatoonOrder order;
	String name;
	public GVCPacket_PlatoonInfoSync(){

	}
	public GVCPacket_PlatoonInfoSync(PlatoonInfoData platoonInfoData, Entity targetEntity){
		isLeader = platoonInfoData.isLeader;
		isOnPlatoon = platoonInfoData.isOnPlatoon;
		target = platoonInfoData.target;
		order = platoonInfoData.order == null ? PlatoonOrder.fromLegacy(platoonInfoData.mode) : platoonInfoData.order;
		name = platoonInfoData.platoonName == null ? "" : platoonInfoData.platoonName;
		this.targetEntity = targetEntity.getEntityId();
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		isLeader = buf.readBoolean();
		isOnPlatoon = buf.readBoolean();
		target = new double[]{buf.readDouble(),buf.readDouble(),buf.readDouble()};
		order = PlatoonOrder.fromId(buf.readInt());
		targetEntity = buf.readInt();
		name = ByteBufUtils.readUTF8String(buf);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(isLeader);
		buf.writeBoolean(isOnPlatoon);
		buf.writeDouble(target[0]);
		buf.writeDouble(target[1]);
		buf.writeDouble(target[2]);
		buf.writeInt(order.id);
		buf.writeInt(targetEntity);
		ByteBufUtils.writeUTF8String(buf, name);

	}

	public static void send(PlatoonInfoData data, Entity entity) {
		GVCMPacketHandler.INSTANCE.sendToAllAround(new GVCPacket_PlatoonInfoSync(data, entity),
				new TargetPoint(entity.dimension, entity.posX, entity.posY, entity.posZ, 128));
	}

	@Override
	public IMessage onMessage(GVCPacket_PlatoonInfoSync message, MessageContext ctx) {
		if (ctx.side == Side.CLIENT && message.order != null) {
			proxy.scheduleClientPacket(world -> {
				Entity entity = world.getEntityByID(message.targetEntity);
				if (entity instanceof IPlatoonable) {
					PlatoonInfoData info = ((IPlatoonable) entity).getPlatoonMemberInfo();
					if (info == null) return;
					info.platoonName = message.name;
					info.isLeader = message.isLeader;
					info.isOnPlatoon = message.isOnPlatoon;
					info.target = message.target;
					info.order = message.order;
					info.mode = message.order.legacyMode;
				}
			});
		}
		return null;
	}
}
