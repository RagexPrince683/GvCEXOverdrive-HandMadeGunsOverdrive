package hmggvcmob.event;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import hmggvcmob.camp.CampObjAndPos;
import hmggvcmob.entity.IflagBattler;
import hmggvcmob.world.WorldSavedData_Flag;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;

public class GVCMSpawnEvent {
    @SubscribeEvent
    public void checkSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (event.world.isRemote || event.getResult() != Event.Result.DEFAULT) return;
        int chunkX = MathHelper.floor_double(event.x) >> 4;
        int chunkZ = MathHelper.floor_double(event.z) >> 4;
        CampObjAndPos claimed = WorldSavedData_Flag.get(event.world).campObjHashMap.get(new ChunkCoordIntPair(chunkX, chunkZ));
        if (claimed != null && (!(event.entityLiving instanceof IflagBattler)
                || ((IflagBattler) event.entityLiving).isThisIgnoreSpawnCamp(claimed.campObj))) {
            event.setResult(Event.Result.DENY);
        }
    }
}
