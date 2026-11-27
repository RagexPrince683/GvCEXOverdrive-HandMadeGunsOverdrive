package hmggvcmob.event;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import hmggvcmob.entity.IflagBattler;
import hmggvcmob.military.Occupation;
import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;

public class GVCMSpawnEvent {
    @SubscribeEvent
    public void checkSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (event.world.isRemote || event.getResult() != Event.Result.DEFAULT) return;
        Occupation claimed = WorldSavedData_Flag.get(event.world).atOccupiedTerritory(event.x, event.z, false);
        if (claimed != null && claimed.isVillage() && !(event.entityLiving instanceof IflagBattler)) return;
        if (claimed != null && (!(event.entityLiving instanceof IflagBattler)
                || ((IflagBattler) event.entityLiving).isThisIgnoreSpawnCamp(claimed.campObj))) {
            event.setResult(Event.Result.DENY);
        }
    }
}
