package handmadeguns.command;

import handmadeguns.Util.HMGAmmoPolicy;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import java.util.List;

public class HMG_Command extends CommandBase {
    @Override public String getCommandName() { return "hmg"; }
    @Override public int getRequiredPermissionLevel() { return 2; }
    @Override public String getCommandUsage(ICommandSender sender) {
        return "/hmg infiniteammo <player> <true|false>";
    }

    @Override public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 3 || !"infiniteammo".equals(args[0])
                || !("true".equals(args[2]) || "false".equals(args[2]))) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        EntityPlayerMP player = getPlayer(sender, args[1]);
        boolean enabled = Boolean.parseBoolean(args[2]);
        HMGAmmoPolicy.setInfiniteAmmo(player, enabled);
        sender.addChatMessage(new ChatComponentText("HMG infinite ammo for " + player.getCommandSenderName()
                + ": " + enabled + (player.capabilities.isCreativeMode ? " (Creative still grants infinite ammo)" : "")));
    }

    @Override public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "infiniteammo");
        if (args.length == 2) return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
        if (args.length == 3) return getListOfStringsMatchingLastWord(args, "true", "false");
        return null;
    }

    @Override public boolean isUsernameIndex(String[] args, int index) { return index == 1; }
}
