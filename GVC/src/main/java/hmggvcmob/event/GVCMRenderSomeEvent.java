package hmggvcmob.event;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import hmggvcmob.tile.TileEntityFlag;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.world.WorldEvent;

public class GVCMRenderSomeEvent {
    private TileEntityFlag nearestCamp;
    private int distance;
    private int ticksUntilSearch;
    private World world;

    @SubscribeEvent
    public void update(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getMinecraft();
        if (client.theWorld != world) {
            world = client.theWorld;
            nearestCamp = null;
            ticksUntilSearch = 0;
        }
        EntityPlayer player = client.thePlayer;
        if (player == null || world == null || --ticksUntilSearch > 0) return;
        ticksUntilSearch = 20;
        nearestCamp = TileEntityFlag.findNearest(world, player.posX, player.posY, player.posZ,
                128, false, flag -> flag.showsOnHud(player.posY));
        if (nearestCamp != null) {
            distance = (int) Math.sqrt(nearestCamp.getDistanceFrom(player.posX, player.posY, player.posZ));
        }
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (event.world == world) {
            world = null;
            nearestCamp = null;
        }
    }

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (world == null || event.type != RenderGameOverlayEvent.ElementType.TEXT
                || player == null || nearestCamp == null || nearestCamp.isInvalid()
                || !nearestCamp.showsOnHud(player.posY)) return;
        String faction = nearestCamp.militaryFactionName();
        if (faction.isEmpty()) faction = net.minecraft.util.StatCollector.translateToLocal(nearestCamp.campObj.getTranslationKey());
        String text = net.minecraft.util.StatCollector.translateToLocalFormatted(
                nearestCamp.isMilitarySiteMarker() ? "gvc.military.hud.site" : "gvc.military.hud.beacon", faction, distance);
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(text, 5, 5, 0xEEEBDD);
    }
}
