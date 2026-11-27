
package hmggvcmob.military;

import cpw.mods.fml.common.FMLLog;
import hmggvcmob.GVCMobPlus;
import hmggvcmob.entity.util.PlatoonOBJ;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
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
        if (preset == null || !preset.recruitable || !post.faction().equals(preset.faction)
                || MilitaryRelations.playerRelation(player, post.faction()) < preset.requiredRelation) return "unavailable";
        MilitaryEconomy economy = MilitaryEconomy.get();
        MilitaryEconomy.Account account = economy.account(player.getUniqueID());
        if (account.activeCount() >= MilitaryConfig.playerLimit) return "limit";
        if (account.currency < preset.recruitmentCost) return "funds";

        EntityMilitaryUnit unit = new EntityMilitaryUnit(player.worldObj);
        ChunkCoordinates spot = MilitaryOccupation.findSpace(player.worldObj, post.traderX, post.traderY, post.traderZ, 1, 5, false, unit);
        if (spot == null) return "space";
        unit.configure(preset, player.getUniqueID(), null);
        if (!player.worldObj.spawnEntityInWorld(unit)) return "space";
        account.currency -= preset.recruitmentCost;
        unit.setPlatoon(PlatoonOBJ.create(unit));
        unit.getPlatoon().follow(player);
        economy.recruit(unit, preset);
        if (!player.inventory.hasItem(GVCMobPlus.defsetter)) {
            player.inventory.addItemStackToInventory(new ItemStack(GVCMobPlus.defsetter));
        }
        if (MilitaryConfig.diagnostics) FMLLog.info("[GVC military] %s hired %s for %d from %s",
                player.getCommandSenderName(), preset.id, preset.recruitmentCost, post.id);
        return "hired";
    }

    static boolean permitted(EntityPlayer player, EntityMilitaryTrader trader, Occupation post) {
        return post != null && MilitaryConfig.recruitment && trader.isEntityAlive() && trader.getUniqueID().equals(post.trader)
                && player.isEntityAlive() && player.getDistanceSqToEntity(trader) <= 64
                && player.canEntityBeSeen(trader) && post.permits(player);
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
