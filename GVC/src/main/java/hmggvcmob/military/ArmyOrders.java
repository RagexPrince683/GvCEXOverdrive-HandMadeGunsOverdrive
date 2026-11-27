package hmggvcmob.military;

import hmggvcmob.entity.friend.EntityPMCBase;
import hmggvcmob.entity.util.PlatoonOBJ;
import hmggvcmob.entity.util.PlatoonOrder;
import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

public final class ArmyOrders {
    public enum Order {
        FOLLOW, MOVE, HOLD, WAIT, REGROUP, ATTACK, DEFEND, PATROL,
        CEASE_FIRE, FIRE_AT_WILL, GARRISON, LINE, COLUMN, LOOSE, DOUBLE_COLUMN;

        public String key() { return "gvc.army.order." + name().toLowerCase(java.util.Locale.ROOT); }
    }

    private ArmyOrders() {}

    static boolean eligible(EntityPlayer player, EntityMilitaryUnit unit) {
        double range = MilitaryConfig.commandRange;
        return unit != null && unit.worldObj == player.worldObj && unit.canBeCommandedBy(player)
                && unit.getDistanceSqToEntity(player) <= range * range;
    }

    private static boolean permitsGroup(EntityPlayer player, MilitaryEconomy.Account account, MilitaryGroups.Group group) {
        if (group == null) return true;
        MilitaryEconomy.Personnel leader = account.roster.get(group.leader);
        return (!MilitaryConfig.requireOfficer || leader != null && eligible(player, MilitaryEconomy.get().loaded(group.leader)))
                && (!MilitaryConfig.enforceCapacity || group.leader == null
                    || MilitaryGroups.size(account, group.id) <= MilitaryGroups.capacity(leader));
    }

    static void menu(EntityPlayerMP player) {
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(player.getUniqueID());
        NBTTagCompound data = new NBTTagCompound();
        NBTTagList rows = new NBTTagList();
        Map<UUID, Integer> counts = new LinkedHashMap<>();
        Map<UUID, Boolean> allowed = new LinkedHashMap<>();
        int all = 0;
        for (MilitaryEconomy.Personnel member : account.roster.values()) {
            if (!member.active() || !eligible(player, economy.loaded(member.id))) continue;
            UUID group = account.groups.containsKey(member.group) ? member.group : null;
            if (!allowed.computeIfAbsent(group, id -> permitsGroup(player, account, account.groups.get(id)))) continue;
            counts.merge(group, 1, Integer::sum);
            all++;
        }
        rows.appendTag(menuRow(MilitaryGroups.ALL, "", 0, all));
        rows.appendTag(menuRow(MilitaryGroups.UNGROUPED, "", 9, counts.getOrDefault(null, 0)));
        for (MilitaryGroups.Group group : account.groups.values()) {
            rows.appendTag(menuRow(group.id.toString(), group.name, group.slot, counts.getOrDefault(group.id, 0)));
        }
        Hit hit = crosshair(player);
        data.setBoolean("Position", hit.point != null);
        data.setBoolean("Target", hit.target != null);
        Occupation post = WorldSavedData_Flag.get(player.worldObj).atTerritory(player.posX, player.posZ, false);
        data.setBoolean("Garrison", post != null && post.permits(player));
        data.setTag("Groups", rows);
        MilitaryNetwork.send(player, MilitaryNetwork.COMMAND_MENU_WINDOW, data);
    }

    private static NBTTagCompound menuRow(String id, String name, int slot, int count) {
        NBTTagCompound row = new NBTTagCompound();
        row.setString("Id", id);
        row.setString("Name", name);
        row.setInteger("Slot", slot);
        row.setInteger("Count", count);
        return row;
    }

    static void issue(EntityPlayerMP player, Order order, String selection, boolean battlefield) {
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(player.getUniqueID());
        long now = MilitaryEconomy.now();
        if (now - account.lastOrder < MilitaryConfig.commandCooldown) return;
        account.lastOrder = now;
        UUID id = MilitaryEconomy.parseUuid(selection);
        boolean all = MilitaryGroups.ALL.equals(selection), ungrouped = MilitaryGroups.UNGROUPED.equals(selection);
        MilitaryGroups.Group group = account.groups.get(id);
        boolean individual = !battlefield && !all && !ungrouped && group == null;
        if (!all && !ungrouped && group == null && !individual) {
            feedback(player, order, "no_squad", 0, null);
            return;
        }

        Hit destination = destination(player, order, !battlefield);
        if (destination == null) return;
        Map<PlatoonOBJ, EntityMilitaryUnit> squads = new LinkedHashMap<>();
        Map<UUID, Boolean> allowed = new LinkedHashMap<>();
        EntityMilitaryUnit nearest = null;
        for (MilitaryEconomy.Personnel member : account.roster.values()) {
            if (!member.active()) continue;
            EntityMilitaryUnit unit = economy.loaded(member.id);
            MilitaryGroups.Group assigned = account.groups.get(member.group);
            if (!eligible(player, unit) || !allowed.computeIfAbsent(member.group, key -> permitsGroup(player, account, assigned))) continue;
            if (individual) {
                if (id != null && id.equals(member.id) || selection.isEmpty()
                        && (nearest == null || player.getDistanceSqToEntity(unit) < player.getDistanceSqToEntity(nearest))) nearest = unit;
                continue;
            }
            if (!all && !(ungrouped ? assigned == null : group == assigned)) continue;
            add(player, account, member, unit, squads, destination.target);
        }
        if (nearest != null) add(player, account, account.roster.get(nearest.getUniqueID()), nearest, squads, destination.target);
        int count = 0;
        for (Map.Entry<PlatoonOBJ, EntityMilitaryUnit> entry : squads.entrySet()) {
            apply(player, entry.getKey(), entry.getValue(), order, destination);
            count += entry.getKey().size();
            economy.updateAssignment(entry.getValue());
        }
        feedback(player, order, count == 0 ? "no_squad" : "accepted", count, destination.point);
    }

    private static void add(EntityPlayer player, MilitaryEconomy.Account account, MilitaryEconomy.Personnel member,
                            EntityMilitaryUnit unit, Map<PlatoonOBJ, EntityMilitaryUnit> squads, EntityLivingBase target) {
        if (target != null && unit.is_this_entity_friend(target)) return;
        MilitaryGroups.sync(unit, account, member);
        PlatoonOBJ squad = unit.getPlatoon();
        if (squad != null && squads.containsKey(squad)) return;
        Predicate<UUID> sameGroup = id -> {
            MilitaryEconomy.Personnel other = account.roster.get(id);
            return other != null && other.active() && Objects.equals(member.group, other.group);
        };
        if (squad != null) squad.forgetUnloadedMembers(sameGroup);
        if (squad == null || !squad.allMembersMatch(sameGroup)) {
            if (squad != null && squad.managedOwner() != null) return;
            squad = PlatoonOBJ.create(unit);
            unit.setPlatoon(squad);
        }
        squads.putIfAbsent(squad, unit);
    }

    public static void legacy(EntityPlayer player, EntityPMCBase unit, Order order) {
        if (!MilitaryConfig.legacyDevice || !(player instanceof EntityPlayerMP) || !unit.canBeCommandedBy(player)) return;
        EntityPlayerMP sender = (EntityPlayerMP) player;
        if (unit instanceof EntityMilitaryUnit) {
            issue(sender, order, unit.getUniqueID().toString(), false);
            return;
        }
        Hit destination = destination(sender, order, true);
        if (destination == null) return;
        PlatoonOBJ.restore(unit);
        if (unit.getPlatoon() == null) unit.makePlatoon();
        PlatoonOBJ squad = unit.getPlatoon();
        Predicate<net.minecraft.entity.EntityLiving> authorized = entity -> entity instanceof EntityPMCBase
                && !(entity instanceof EntityMilitaryUnit) && ((EntityPMCBase) entity).canBeCommandedBy(player)
                && (((EntityPMCBase) entity).leaderEntity_name == null || ((EntityPMCBase) entity).leaderEntity_name.isEmpty()
                    || player.getCommandSenderName().equalsIgnoreCase(((EntityPMCBase) entity).leaderEntity_name));
        if (squad == null || !squad.allLoadedMembersMatch(authorized)) {
            feedback(sender, order, "no_squad", 0, null);
            return;
        }
        apply(player, squad, unit, order, destination);
        feedback(sender, order, "accepted", squad.size(), destination.point);
    }

    private static Hit destination(EntityPlayerMP player, Order order, boolean legacyPositionAttack) {
        Hit hit = new Hit(null, null);
        switch (order) {
            case MOVE: case DEFEND: case PATROL: case ATTACK:
                hit = crosshair(player);
                boolean attack = order == Order.ATTACK;
                if (attack && hit.target == null && (!legacyPositionAttack || hit.point == null)
                        || !attack && hit.point == null) {
                    feedback(player, order, attack ? "no_target" : "no_position", 0, null);
                    return null;
                }
                if (attack && hit.target != null) hit = new Hit(Vec3.createVectorHelper(hit.target.posX,
                        hit.target.boundingBox.minY, hit.target.posZ), hit.target);
                if (!attack) hit = new Hit(hit.point, null);
                break;
            case GARRISON:
                Occupation post = WorldSavedData_Flag.get(player.worldObj).atTerritory(player.posX, player.posZ, false);
                if (post == null || !post.permits(player)) {
                    feedback(player, order, "no_post", 0, null);
                    return null;
                }
                hit = new Hit(Vec3.createVectorHelper(post.flagPos[0] + .5, post.flagPos[1] + 1, post.flagPos[2] + .5), null);
                break;
            case REGROUP:
                hit = new Hit(Vec3.createVectorHelper(player.posX, player.boundingBox.minY, player.posZ), null);
                break;
            default:
                break;
        }
        return hit;
    }

    private static void apply(EntityPlayer player, PlatoonOBJ squad, EntityPMCBase unit, Order order, Hit hit) {
        switch (order) {
            case FOLLOW: squad.follow(player); return;
            case WAIT: squad.setOrder(PlatoonOrder.Pause); return;
            case HOLD:
                EntityLivingBase leader = squad.leader == null ? unit : squad.leader.entity;
                squad.order(PlatoonOrder.Hold, leader.posX, leader.boundingBox.minY, leader.posZ);
                return;
            case CEASE_FIRE: squad.setCeaseFire(true); return;
            case FIRE_AT_WILL: squad.setCeaseFire(false); return;
            case LINE: case COLUMN: case LOOSE: case DOUBLE_COLUMN:
                squad.setFormation(PlatoonOBJ.Formation.valueOf(order.name()));
                return;
            default:
                PlatoonOrder mode = order == Order.MOVE ? PlatoonOrder.Go : order == Order.ATTACK ? PlatoonOrder.Attack
                        : order == Order.PATROL ? PlatoonOrder.Patrol : order == Order.REGROUP ? PlatoonOrder.Rally : PlatoonOrder.Defend;
                squad.order(mode, hit.point.xCoord, hit.point.yCoord, hit.point.zCoord,
                        hit.target == null ? null : hit.target.getUniqueID());
                squad.designate(hit.target);
        }
    }

    private static Hit crosshair(EntityPlayer player) {
        int range = MilitaryConfig.targetingRange;
        Vec3 look = player.getLook(1);
        Vec3 eye = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 end = eye.addVector(look.xCoord * range, look.yCoord * range, look.zCoord * range);
        // The 1.7.10 block trace advances its mutable start vector through each block.
        Vec3 blockStart = Vec3.createVectorHelper(eye.xCoord, eye.yCoord, eye.zCoord);
        MovingObjectPosition block = player.worldObj.rayTraceBlocks(blockStart, end);
        double closest = block == null ? range : block.hitVec.distanceTo(eye);
        EntityLivingBase target = null;
        for (Object entry : player.worldObj.getEntitiesWithinAABB(EntityLivingBase.class,
                player.boundingBox.addCoord(look.xCoord * range, look.yCoord * range, look.zCoord * range).expand(1, 1, 1))) {
            EntityLivingBase entity = (EntityLivingBase) entry;
            if (entity == player || !entity.isEntityAlive() || !entity.canBeCollidedWith()
                    || entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.disableDamage) continue;
            MovingObjectPosition intercept = entity.boundingBox.expand(.15, .15, .15).calculateIntercept(eye, end);
            if (intercept == null) continue;
            double distance = intercept.hitVec.distanceTo(eye);
            if (distance < closest) {
                closest = distance;
                target = entity;
            }
        }
        Vec3 point = block == null ? null : Vec3.createVectorHelper(block.blockX + .5, block.blockY + 1, block.blockZ + .5);
        return new Hit(point, target);
    }

    private static void feedback(EntityPlayerMP player, Order order, String result, int count, Vec3 point) {
        player.addChatComponentMessage(new ChatComponentTranslation("gvc.army.feedback." + result,
                new ChatComponentTranslation(order.key()), count));
        if (count <= 0 || point == null) return;
        NBTTagCompound marker = new NBTTagCompound();
        marker.setDouble("X", point.xCoord);
        marker.setDouble("Y", point.yCoord);
        marker.setDouble("Z", point.zCoord);
        marker.setString("Order", order.key());
        MilitaryNetwork.send(player, MilitaryNetwork.COMMAND_MARKER_WINDOW, marker);
    }

    private static final class Hit {
        final Vec3 point;
        final EntityLivingBase target;

        Hit(Vec3 point, EntityLivingBase target) {
            this.point = point;
            this.target = target;
        }
    }
}
