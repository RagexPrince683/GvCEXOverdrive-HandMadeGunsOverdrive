package handmadeguns.command;

import handmadeguns.Util.HMGAmmoPolicy;
import handmadeguns.items.GunInfo;
import handmadeguns.tech.HMGTechTierManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.ChatComponentText;
import java.util.List;

public class HMG_Command extends CommandBase {
    @Override public String getCommandName() { return "hmg"; }
    @Override public int getRequiredPermissionLevel() { return 2; }
    @Override public String getCommandUsage(ICommandSender sender) {
        return "/hmg infiniteammo [true|false] | /hmg tier <get|set <0.0..5.0>|item>";
    }

    @Override public void processCommand(ICommandSender sender, String[] args) {
        if (args.length >= 2 && "tier".equalsIgnoreCase(args[0])) {
            processTier(sender, args);
            return;
        }
        if (args.length < 1 || args.length > 2 || !"infiniteammo".equals(args[0])
                || (args.length == 2 && !("true".equals(args[1]) || "false".equals(args[1]))))
            throw new WrongUsageException(getCommandUsage(sender));
        boolean enabled = args.length == 1 ? !HMGAmmoPolicy.isGlobalInfiniteAmmo()
                : Boolean.parseBoolean(args[1]);
        HMGAmmoPolicy.setGlobalInfiniteAmmo(enabled);
        sender.addChatMessage(new ChatComponentText("HMG infinite ammo for all players: " + enabled
                + " (Creative players always have infinite ammo)"));
    }

    private void processTier(ICommandSender sender, String[] args) {
        if ("get".equalsIgnoreCase(args[1]) && args.length == 2) {
            sender.addChatMessage(new ChatComponentText("HMG unlocked tech tier: "
                    + HMGTechTierManager.getUnlockedTier(sender.getEntityWorld())));
            return;
        }
        if ("set".equalsIgnoreCase(args[1]) && args.length == 3) {
            float tier;
            try { tier = Float.parseFloat(args[2]); }
            catch (NumberFormatException e) { throw new WrongUsageException(getCommandUsage(sender)); }
            if (!HMGTechTierManager.isValidTier(tier)) throw new WrongUsageException(getCommandUsage(sender));
            HMGTechTierManager.setUnlockedTier(sender.getEntityWorld(), tier);
            sender.addChatMessage(new ChatComponentText("HMG unlocked tech tier set to " + tier));
            return;
        }
        if ("item".equalsIgnoreCase(args[1]) && args.length == 2 && sender instanceof net.minecraft.entity.player.EntityPlayer) {
            net.minecraft.item.ItemStack stack = ((net.minecraft.entity.player.EntityPlayer) sender).getHeldItem();
            GunInfo info = HMGTechTierManager.getInfo(stack);
            if (info == null) {
                sender.addChatMessage(new ChatComponentText("Held item is not an HMG gun."));
            } else {
                float required = HMGTechTierManager.resolveRequiredTier(info);
                sender.addChatMessage(new ChatComponentText("HMG content=" + HMGTechTierManager.identify(stack)
                        + ", TechYear=" + (info.techYear == null ? "undefined" : info.techYear)
                        + ", explicit TechTier=" + (info.techTierHalfSteps < 0 ? "undefined" : HMGTechTierManager.fromHalfSteps(info.techTierHalfSteps))
                        + ", required=" + required + ", server=" + HMGTechTierManager.getUnlockedTier(sender.getEntityWorld())
                        + ", locked=" + !HMGTechTierManager.isUnlocked(stack, (net.minecraft.entity.player.EntityPlayer)sender, sender.getEntityWorld())));
            }
            return;
        }
        throw new WrongUsageException(getCommandUsage(sender));
    }

    @Override public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "infiniteammo", "tier");
        if (args.length == 2 && "infiniteammo".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, "true", "false");
        }
        if (args.length == 2 && "tier".equalsIgnoreCase(args[0]))
            return getListOfStringsMatchingLastWord(args, "get", "set", "item");
        if (args.length == 3 && "tier".equalsIgnoreCase(args[0]) && "set".equalsIgnoreCase(args[1]))
            return getListOfStringsMatchingLastWord(args, "0.0", "0.5", "1.0", "1.5", "2.0", "2.5", "3.0", "3.5", "4.0", "4.5", "5.0");
        return null;
    }
}
