
package hmggvcmob.military;

import hmggvcmob.tile.TileEntityFlag;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class MilitaryCommand extends CommandBase {
    private final boolean admin;

    MilitaryCommand(boolean admin) {
        this.admin = admin;
    }

    @Override public String getCommandName() { return admin ? "militaryadmin" : "military"; }
    @Override public int getRequiredPermissionLevel() { return admin ? 2 : 0; }
    @Override public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return !admin && sender instanceof EntityPlayerMP || super.canCommandSenderUseCommand(sender);
    }
    @Override public String getCommandUsage(ICommandSender sender) { return "gvc.military.command." + (admin ? "admin" : "player"); }

    @Override
    public void processCommand(ICommandSender sender, String[] arguments) {
        if (arguments.length == 0 && !admin) {
            MilitaryNetwork.open(getCommandSenderAsPlayer(sender));
            return;
        }
        if (arguments.length == 0) throw new WrongUsageException(getCommandUsage(sender));
        if (admin) {
            administer(sender, arguments);
            return;
        }
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        switch (arguments[0]) {
            case "manual":
                if (arguments.length == 1) {
                    MilitaryNetwork.send(player, MilitaryNetwork.MANUAL_WINDOW, new net.minecraft.nbt.NBTTagCompound());
                    return;
                }
                break;
            case "hire":
                if (arguments.length == 3) {
                    reply(player, MilitaryRecruitment.hire(player, parseInt(sender, arguments[1]), arguments[2]));
                    return;
                }
                break;
            case "assign":
                if (arguments.length == 3) {
                    reply(player, MilitaryRecruitment.assign(player, uuid(arguments[1]), uuid(arguments[2])) ? "assigned" : "assign_failed");
                    return;
                }
                break;
            case "roster":
                roster(player, arguments.length > 1 ? parseIntBounded(sender, arguments[1], 1, 100) - 1 : 0);
                return;
            case "order":
                if (arguments.length >= 2 && arguments.length <= 3) {
                    order(player, arguments[1], arguments.length == 3 ? arguments[2] : "");
                    return;
                }
                break;
            case "relation":
                for (String faction : MilitaryRelations.factions()) sender.addChatMessage(new ChatComponentTranslation(
                        "gvc.military.command.relation", MilitaryRelations.displayName(faction), MilitaryRelations.reputation(player.getUniqueID(), faction)));
                return;
            default:
                break;
        }
        throw new WrongUsageException(getCommandUsage(sender));
    }

    private void administer(ICommandSender sender, String[] arguments) {
        if (arguments[0].equals("structure")) {
            structure(sender, arguments);
            return;
        }
        if (arguments[0].equals("balance") && (arguments.length == 2 || arguments.length == 3)) {
            UUID target = playerId(sender, arguments[1]);
            if (arguments.length == 3) {
                long delta;
                try { delta = Long.parseLong(arguments[2]); }
                catch (NumberFormatException invalid) { throw new WrongUsageException(getCommandUsage(sender)); }
                if (!MilitaryEconomy.adjustBalance(target, delta)) {
                    reply(sender, "overflow");
                    return;
                }
            }
            sender.addChatMessage(new ChatComponentTranslation("gvc.military.command.balance", arguments[1], MilitaryEconomy.balance(target)));
            return;
        }
        if (arguments[0].equals("relation") && arguments.length == 4 && MilitaryRelations.isFaction(arguments[2])) {
            int score = parseIntBounded(sender, arguments[3], MilitaryRelations.MIN_REPUTATION, MilitaryRelations.MAX_REPUTATION);
            MilitaryRelations.setReputation(playerId(sender, arguments[1]), arguments[2], score);
            MilitarySystem.closeHostileTrade();
            sender.addChatMessage(new ChatComponentTranslation("gvc.military.command.relation", arguments[2], score));
            return;
        }
        if (arguments[0].equals("occupy") && arguments.length >= 2 && arguments.length <= 3 && MilitaryRelations.isFaction(arguments[1])) {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            TileEntityFlag flag = TileEntityFlag.findNearest(player.worldObj, player.posX, player.posY, player.posZ, 32, false,
                    tile -> tile.occupation() != null && !tile.campObj.isRaidType);
            if (flag == null) reply(sender, "no_post");
            else {
                MilitaryOccupation.transfer(player.worldObj, flag.occupation(), arguments[1],
                        arguments.length == 3 ? playerId(sender, arguments[2]) : null);
                MilitarySystem.closeHostileTrade();
                reply(sender, "occupied");
            }
            return;
        }
        throw new WrongUsageException(getCommandUsage(sender));
    }

    private void structure(ICommandSender sender, String[] args) {
        if (args.length == 2 && "showcase".equals(args[1])
                && sender.getEntityWorld().getWorldInfo().getTerrainType() == net.minecraft.world.WorldType.FLAT) {
            sender.getEntityWorld().getGameRules().setOrCreateGameRule(MilitarySites.SHOWCASE_RULE, "true");
            sender.addChatMessage(new ChatComponentTranslation("gvc.military.structure.showcase"));
            return;
        }
        if (args.length < 5 || args.length > 6 || !("preview".equals(args[1]) || "live".equals(args[1]))) {
            throw new WrongUsageException("gvc.military.structure.command");
        }
        int x = parseIntBounded(sender, args[3], -29999871, 29999871);
        int z = parseIntBounded(sender, args[4], -29999871, 29999871);
        int rotation = args.length == 6 ? parseIntBounded(sender, args[5], 0, 3) : 0;
        boolean placed = MilitarySites.testPlacement(sender.getEntityWorld(), args[2], x, z, rotation, "preview".equals(args[1]));
        sender.addChatMessage(new ChatComponentTranslation("gvc.military.structure." + (placed ? "placed" : "rejected"), args[2], x, z));
    }

    private static void roster(EntityPlayerMP player, int page) {
        int index = 0;
        for (MilitaryEconomy.Personnel person : MilitaryEconomy.get().account(player.getUniqueID()).roster.values()) {
            if (!person.active()) continue;
            if (index >= page * 8 && index < (page + 1) * 8) {
                player.addChatComponentMessage(new ChatComponentTranslation("gvc.military.command.unit", person.name, person.id.toString()));
            }
            index++;
        }
        player.addChatComponentMessage(new ChatComponentTranslation("gvc.military.command.roster", page + 1, Math.max(1, (index + 7) / 8)));
    }

    private void order(EntityPlayerMP player, String name, String target) {
        ArmyOrders.Order order;
        try {
            order = ArmyOrders.Order.valueOf(name.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException invalid) {
            throw new WrongUsageException(getCommandUsage(player));
        }
        ArmyOrders.issue(player, order, target, false);
    }

    private static UUID uuid(String value) {
        UUID id = MilitaryEconomy.parseUuid(value);
        if (id == null) throw new WrongUsageException("gvc.military.command.invalid_id");
        return id;
    }

    private static UUID playerId(ICommandSender sender, String value) {
        UUID id = MilitaryEconomy.parseUuid(value);
        return id == null ? getPlayer(sender, value).getUniqueID() : id;
    }

    private static void reply(ICommandSender sender, String result) {
        if (result.equals("denied") && sender instanceof EntityPlayerMP) MilitaryRecruitment.refused((EntityPlayerMP)sender);
        else sender.addChatMessage(new ChatComponentTranslation("gvc.military.result." + result));
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, admin
                ? new String[]{"balance", "relation", "occupy", "structure"} : new String[]{"hire", "assign", "roster", "order", "relation", "manual"});
        if (admin && args[0].equals("structure")) {
            if (args.length == 2) return getListOfStringsMatchingLastWord(args, "preview", "live", "showcase");
            if (args.length == 3) return getListOfStringsMatchingLastWord(args, MilitarySites.ids().toArray(new String[0]));
        }
        if (args.length == 2 && args[0].equals("order")) return getListOfStringsMatchingLastWord(args,
                "follow", "hold", "wait", "move", "regroup", "patrol", "defend", "attack", "garrison",
                "cease-fire", "fire-at-will", "line", "column", "loose", "double-column");
        if (args.length == 2 && args[0].equals("occupy") || args.length == 3 && admin && args[0].equals("relation")) {
            return getListOfStringsMatchingLastWord(args, MilitaryRelations.factions().toArray(new String[0]));
        }
        return null;
    }
}
