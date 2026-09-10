package handmadeguns.command;

import handmadeguns.Util.HMGAmmoPolicy;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import java.util.List;

public class HMG_Command extends CommandBase {
    @Override public String getCommandName() { return "hmg"; }
    @Override public int getRequiredPermissionLevel() { return 2; }
    @Override public String getCommandUsage(ICommandSender sender) {
        return "/hmg infiniteammo [true|false]";
    }

    @Override public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2 || !"infiniteammo".equals(args[0])
                || (args.length == 2 && !("true".equals(args[1]) || "false".equals(args[1])))) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        boolean enabled = args.length == 1 ? !HMGAmmoPolicy.isGlobalInfiniteAmmo()
                : Boolean.parseBoolean(args[1]);
        HMGAmmoPolicy.setGlobalInfiniteAmmo(enabled);
        sender.addChatMessage(new ChatComponentText("HMG infinite ammo for all players: " + enabled
                + " (Creative players always have infinite ammo)"));
    }

    @Override public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "infiniteammo");
        if (args.length == 2 && "infiniteammo".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, "true", "false");
        }
        return null;
    }
}
