package handmadeguns.command;

import handmadeguns.HandmadeGunsCore;
import handmadeguns.HMGPacketHandler;
import handmadeguns.guide.PacketFieldManual;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;

public class HMG_CommandManual extends CommandBase {
    @Override
    public String getCommandName() {
        return "hmgmanual";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return sender instanceof EntityPlayerMP || super.canCommandSenderUseCommand(sender);
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/hmgmanual";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!HandmadeGunsCore.enableHMGGuideBook) {
            sender.addChatMessage(new ChatComponentTranslation("hmg.manual.disabled"));
        } else if (sender instanceof EntityPlayerMP) {
            HMGPacketHandler.INSTANCE.sendTo(new PacketFieldManual(), (EntityPlayerMP) sender);
        } else {
            sender.addChatMessage(new ChatComponentTranslation("hmg.manual.player_only"));
        }
    }
}
