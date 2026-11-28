
package hmggvcmob.military;

import cpw.mods.fml.common.FMLLog;
import hmggvcmob.entity.util.PlatoonOBJ;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.entity.player.EntityPlayerMP;
import java.util.UUID;

final class MilitaryRecruitment {
    private MilitaryRecruitment() {}

    static void open(EntityPlayer player, EntityMilitaryTrader trader) {
        Occupation post = trader.post();
        if (!permitted(player, trader, post)) {
            refused(player);
            return;
        }
        if (player instanceof EntityPlayerMP) MilitaryNetwork.openRecruitment((EntityPlayerMP)player, trader);
    }

    static String hire(EntityPlayer player, int traderId, String presetId) {
        Entity entity = player.worldObj.getEntityByID(traderId);
        if (!(entity instanceof EntityMilitaryTrader)) return "trader";
        EntityMilitaryTrader trader = (EntityMilitaryTrader) entity;
        Occupation post = trader.post();
        if (!permitted(player, trader, post)) return "denied";
        UnitPresets.Preset preset = UnitPresets.get(presetId);
        if (preset == null || !UnitPresets.pool(trader.faction(), false).contains(preset)
                || !MilitaryTraders.canRecruit(trader.location().role)) return "unavailable";
        String lock = lock(player, preset);
        if (!lock.isEmpty()) return lock;
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(player.getUniqueID());
        if (account.activeCount() >= MilitaryConfig.playerLimit) return "limit";
        int price = price(player.getUniqueID(), preset);
        if (account.currency < price) return "funds";

        EntityMilitaryUnit unit = new EntityMilitaryUnit(player.worldObj);
        Occupation.TraderLocation location = trader.location();
        ChunkCoordinates spot = MilitaryOccupation.findSpace(player.worldObj, location.x, location.y, location.z, 1, 5, false, unit);
        if (spot == null) return "space";
        unit.configure(preset, player.getUniqueID(), null);
        if (!player.worldObj.spawnEntityInWorld(unit)) return "space";
        account.currency -= price;
        unit.setPlatoon(PlatoonOBJ.create(unit));
        unit.getPlatoon().follow(player);
        economy.recruit(unit, preset);
        if (MilitaryConfig.diagnostics) FMLLog.info("[GVC military] %s hired %s for %d from %s",
                player.getCommandSenderName(), preset.id, price, post.id);
        return "hired";
    }

    static boolean permitted(EntityPlayer player, EntityMilitaryTrader trader, Occupation post) {
        Occupation.TraderLocation location = trader.location();
        return post != null && location != null && MilitaryConfig.recruitment && trader.isEntityAlive()
                && trader.getUniqueID().equals(location.entity)
                && player.isEntityAlive() && player.getDistanceSqToEntity(trader) <= 64
                && player.canEntityBeSeen(trader) && (post.owner == null || post.owner.equals(player.getUniqueID()))
                && MilitaryRelations.permitsTrade(player, trader.faction());
    }

    static int price(UUID player, UnitPresets.Preset preset) {
        return (int) Math.max(1, ((long) preset.recruitmentCost * (100 - MilitaryRelations.discount(player, preset.faction)) + 99) / 100);
    }

    static String lock(EntityPlayer player, UnitPresets.Preset preset) {
        int score = MilitaryRelations.playerRelation(player, preset.faction);
        if (score < MilitaryRelations.requirement(preset) || preset.minimumReputation == null
                && MilitaryRelations.recruitmentTier(preset.faction, score) < preset.tier) return "reputation_locked";
        if (preset.allegianceRequired && !preset.faction.equals(MilitaryRelations.allegiance(player.getUniqueID()))) return "allegiance_locked";
        return "";
    }

    static String pledge(EntityPlayer player, int traderId, String faction) {
        Entity entity = player.worldObj.getEntityByID(traderId);
        if (!(entity instanceof EntityMilitaryTrader)) return "trader";
        EntityMilitaryTrader trader = (EntityMilitaryTrader) entity;
        Occupation post = trader.post();
        if (!permitted(player, trader, post) || !trader.faction().equals(faction)
                || !MilitaryTraders.canRecruit(trader.location().role)) return "denied";
        return MilitaryRelations.pledge(player, faction);
    }

    static void refused(EntityPlayer player) {
        player.addChatComponentMessage(MilitaryConfig.occupiedMessage.isEmpty()
                ? new ChatComponentTranslation("gvc.military.trade.denied") : new ChatComponentText(MilitaryConfig.occupiedMessage));
    }

    static boolean assign(EntityPlayer player, UUID followerId, UUID leaderId) {
        MilitaryEconomy economy = MilitaryEconomy.get();
        EntityMilitaryUnit follower = economy.loaded(followerId);
        EntityMilitaryUnit leader = economy.loaded(leaderId);
        if (follower == null || leader == null || follower == leader || !follower.canBeCommandedBy(player)
                || !leader.canBeCommandedBy(player) || leader.commandRank() <= follower.commandRank()
                || leader.worldObj != player.worldObj || follower.worldObj != player.worldObj
                || player.getDistanceSqToEntity(leader) > 96 * 96 || player.getDistanceSqToEntity(follower) > 96 * 96) return false;
        MilitaryEconomy.Account account = economy.account(player.getUniqueID());
        if (account.roster.get(followerId).group != null || account.roster.get(leaderId).group != null) return false;
        PlatoonOBJ platoon = leader.getPlatoon();
        if (platoon == null || !leaderId.equals(platoon.leaderId())) {
            platoon = PlatoonOBJ.create(leader);
            leader.setPlatoon(platoon);
            platoon.follow(player);
        }
        if (follower.getPlatoon() != platoon) {
            if (!platoon.hasRoom()) return false;
            follower.setPlatoon(platoon);
        }
        economy.updateAssignment(follower);
        economy.updateAssignment(leader);
        return true;
    }
}
