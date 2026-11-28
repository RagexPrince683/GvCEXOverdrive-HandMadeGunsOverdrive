package hmggvcmob.event;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import hmggvcmob.entity.IPlatoonable;
import hmggvcmob.entity.util.PlatoonInfoData;
import hmggvcmob.entity.util.PlatoonOrder;
import hmggvcmob.military.EntityMilitaryUnit;
import hmggvcmob.military.MilitaryRelations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderLivingEvent;
import org.lwjgl.opengl.GL11;

public final class EventRenderPlatoonInfo {
    private static World allegianceWorld;
    private static String allegiance = "";

    public static void updateAllegiance(World world, String faction) {
        allegianceWorld = world;
        allegiance = faction;
    }

    @SubscribeEvent
    public void unload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        if (event.world == allegianceWorld) updateAllegiance(null, "");
    }

    @SubscribeEvent
    public void renderEntity(RenderLivingEvent.Post event) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.thePlayer == null || client.gameSettings.hideGUI || event.entity.isInvisibleToPlayer(client.thePlayer)
                || !(event.entity instanceof IPlatoonable) || event.entity.getDistanceSqToEntity(client.thePlayer) > 48 * 48) return;
        String pledged = client.theWorld == allegianceWorld ? allegiance : "";
        if (event.entity instanceof EntityMilitaryUnit) {
            if (!((EntityMilitaryUnit)event.entity).showsOrdersTo(client.thePlayer.getUniqueID(), pledged)) return;
        } else if (pledged.isEmpty() || !pledged.equals(MilitaryRelations.faction(event.entity))) return;
        PlatoonInfoData info = ((IPlatoonable) event.entity).getPlatoonMemberInfo();
        if (info == null || !info.isOnPlatoon) return;

        RenderManager view = RenderManager.instance;
        FontRenderer font = client.fontRenderer;
        PlatoonOrder order = info.order == null ? PlatoonOrder.fromLegacy(info.mode) : info.order;
        String status = StatCollector.translateToLocal("gvc.order.mode." + order.name());
        if (info.isLeader) status += "  ·  " + StatCollector.translateToLocal("gvc.order.leader");

        float lightX = OpenGlHelper.lastBrightnessX;
        float lightY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glPushMatrix();
        try {
            // RenderLivingEvent positions already include entity and render-view interpolation.
            GL11.glTranslated(event.x, event.y + event.entity.height + .68, event.z);
            GL11.glRotatef(-view.playerViewY, 0, 1, 0);
            float pitch = client.gameSettings.thirdPersonView == 2 ? -view.playerViewX : view.playerViewX;
            GL11.glRotatef(pitch, 1, 0, 0);
            GL11.glScalef(-.019F, -.019F, .019F);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(false);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            line(font, status, 0, 0xE7CE87);
            if (info.platoonName != null && !info.platoonName.isEmpty()) line(font, info.platoonName, -10, 0xEEEBDD);
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private static void line(FontRenderer font, String text, int y, int color) {
        int width = font.getStringWidth(text) / 2;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        Tessellator mesh = Tessellator.instance;
        mesh.startDrawingQuads();
        mesh.setColorRGBA_F(.09F, .12F, .09F, .72F);
        mesh.addVertex(-width - 2, y + 9, 0);
        mesh.addVertex(width + 2, y + 9, 0);
        mesh.addVertex(width + 2, y - 1, 0);
        mesh.addVertex(-width - 2, y - 1, 0);
        mesh.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        font.drawStringWithShadow(text, -width, y, color);
    }
}
