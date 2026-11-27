
package hmggvcmob.military;

import hmggvcmob.entity.util.PlatoonOBJ;
import hmggvcmob.entity.util.PlatoonOrder;
import hmggvcmob.tile.TileEntityFlag;
import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class MilitaryCommand extends CommandBase {
    private final boolean admin;

    MilitaryCommand(boolean admin) {
        this.admin = admin;
    }

    @Override public String getCommandName() { return admin ? "militaryadmin" : "military"; }
    @Override public int getRequiredPermissionLevel() { return admin ? 2 : 0; }
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
                        "gvc.military.command.relation", faction, MilitaryRelations.reputation(player.getUniqueID(), faction)));
                return;
            default:
                break;
        }
        throw new WrongUsageException(getCommandUsage(sender));
    }

    private void administer(ICommandSender sender, String[] arguments) {
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
            int score = parseIntBounded(sender, arguments[3], -100, 100);
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
        PlatoonOrder order;
        boolean garrison = name.equalsIgnoreCase("garrison");
        switch (name.toLowerCase(Locale.ROOT)) {
            case "garrison":
            case "defend": order = PlatoonOrder.Defend; break;
            case "follow": order = PlatoonOrder.Follow; break;
            case "hold": order = PlatoonOrder.Wait; break;
            case "move": order = PlatoonOrder.Go; break;
            case "patrol": order = PlatoonOrder.Patrol; break;
            case "attack": order = PlatoonOrder.Attack; break;
            default: throw new WrongUsageException(getCommandUsage(player));
        }
        Vec3 point = null;
        if (garrison) {
            Occupation post = WorldSavedData_Flag.get(player.worldObj).atTerritory(player.posX, player.posZ, false);
            if (post == null || !post.permits(player)) {
                reply(player, "no_post");
                return;
            }
            point = Vec3.createVectorHelper(post.flagPos[0] + .5, post.flagPos[1] + 1, post.flagPos[2] + .5);
        } else if (order != PlatoonOrder.Wait && order != PlatoonOrder.Follow) {
            Vec3 look = player.getLook(1);
            Vec3 origin = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
            MovingObjectPosition hit = player.worldObj.rayTraceBlocks(origin, origin.addVector(look.xCoord * 80, look.yCoord * 80, look.zCoord * 80));
            if (hit != null) point = Vec3.createVectorHelper(hit.blockX + .5, hit.blockY + 1, hit.blockZ + .5);
            if (point == null) {
                player.addChatComponentMessage(new ChatComponentTranslation("gvc.order.no_destination"));
                return;
            }
        }
        Set<PlatoonOBJ> groups = new HashSet<>();
        boolean all = "all".equals(target);
        boolean hasCommander = false;
        EntityMilitaryUnit selected = null;
        double nearest = 96 * 96;
        MilitaryEconomy economy = MilitaryEconomy.get();
        for (UUID id : economy.account(player.getUniqueID()).roster.keySet()) {
            EntityMilitaryUnit unit = economy.loaded(id);
            if (unit == null || unit.worldObj != player.worldObj || !unit.canBeCommandedBy(player)
                    || player.getDistanceSqToEntity(unit) >= 96 * 96 || unit.getPlatoon() == null) continue;
            if (all) {
                groups.add(unit.getPlatoon());
                hasCommander |= unit.commandRank() >= 2;
            } else if (id.toString().equals(target)) {
                selected = unit;
                break;
            } else if (target.isEmpty() && player.getDistanceSqToEntity(unit) < nearest) {
                selected = unit;
                nearest = player.getDistanceSqToEntity(unit);
            }
        }
        if (all && !hasCommander) groups.clear();
        if (selected != null) groups.add(selected.getPlatoon());
        if (groups.isEmpty()) {
            reply(player, "no_squad");
            return;
        }
        for (PlatoonOBJ group : groups) {
            if (order == PlatoonOrder.Follow) group.follow(player);
            else if (order == PlatoonOrder.Wait) group.setOrder(order);
            else group.order(order, point.xCoord, point.yCoord, point.zCoord);
        }
        reply(player, "ordered");
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
                ? new String[]{"balance", "relation", "occupy"} : new String[]{"hire", "assign", "roster", "order", "relation"});
        if (args.length == 2 && args[0].equals("order")) return getListOfStringsMatchingLastWord(args,
                "follow", "hold", "move", "patrol", "defend", "attack", "garrison");
        if (args.length == 2 && args[0].equals("occupy") || args.length == 3 && admin && args[0].equals("relation")) {
            return getListOfStringsMatchingLastWord(args, MilitaryRelations.factions().toArray(new String[0]));
        }
        return null;
    }
}
