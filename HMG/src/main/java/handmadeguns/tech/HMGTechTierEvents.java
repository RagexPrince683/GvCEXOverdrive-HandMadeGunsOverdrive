package handmadeguns.tech;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.entity.player.EntityPlayerMP;

public final class HMGTechTierEvents {
    @SubscribeEvent public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) HMGTechTierManager.sync((EntityPlayerMP) event.player);
    }

    @SubscribeEvent public void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (event.crafting != null && !event.player.worldObj.isRemote
                && !HMGTechTierManager.isUnlocked(event.crafting, event.player, event.player.worldObj)) {
            HMGTechTierManager.notifyLocked(event.player, event.crafting);
            event.crafting.stackSize = 0;
        }
    }
}
