package hmggvcmob.military.client;

import hmggvcmob.military.EntityTraderMarker;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class RenderTraderMarker extends Render {
    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partial) {
        EntityTraderMarker marker = (EntityTraderMarker)entity;
        float time = entity.ticksExisted + partial;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glTranslated(x, y + Math.sin(time * .11) * .10, z);
            GL11.glRotatef(time * 4, 0, 1, 0);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            Tessellator mesh = Tessellator.instance;
            mesh.startDrawingQuads();
            for (int side : new int[]{-1, 1}) {
                mesh.setColorOpaque_I(0xE9AD43);
                quad(mesh, -.18, -.24, .18, .24, side * .05);
                quad(mesh, -.24, -.18, .24, .18, side * .05);
                mesh.setColorOpaque_I(0x744627);
                quad(mesh, -.15, -.16, .15, .16, side * .051);
                mesh.setColorOpaque_I(0xFFE2A0);
                quad(mesh, -.09, .10, .09, .13, side * .052);
                quad(mesh, -.09, -.13, .09, -.10, side * .052);
                quad(mesh, -.025, -.10, .025, .10, side * .052);
            }
            mesh.setColorOpaque_I(0xA46928);
            edge(mesh, -.18, .24, .18, .24);
            edge(mesh, .18, .24, .24, .18);
            edge(mesh, .24, .18, .24, -.18);
            edge(mesh, .24, -.18, .18, -.24);
            edge(mesh, .18, -.24, -.18, -.24);
            edge(mesh, -.18, -.24, -.24, -.18);
            edge(mesh, -.24, -.18, -.24, .18);
            edge(mesh, -.24, .18, -.18, .24);
            mesh.draw();
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
        if (renderManager.livingPlayer != null && entity.getDistanceSqToEntity(renderManager.livingPlayer) <= 64) {
            int seconds = marker.secondsRemaining();
            renderCaption(x, y + .48, z, seconds);
        }
    }

    private void renderCaption(double x, double y, double z, int seconds) {
        FontRenderer font = getFontRendererFromRenderManager();
        String title = I18n.format("gvc.military.marker.title");
        String time = seconds == 0 ? I18n.format("gvc.military.marker.pending")
                : String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
        int half = Math.max(font.getStringWidth(title), font.getStringWidth(time)) / 2;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
        try {
            GL11.glTranslated(x, y, z);
            GL11.glRotatef(-renderManager.playerViewY, 0, 1, 0);
            GL11.glRotatef(renderManager.playerViewX, 1, 0, 0);
            GL11.glScalef(-.011F, -.011F, .011F);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDepthMask(false);
            Gui.drawRect(-half - 3, -11, half + 3, 8, 0xB3221C14);
            font.drawString(title, -font.getStringWidth(title) / 2, -9, 0xFFE8D8);
            font.drawString(time, -font.getStringWidth(time) / 2, 0, 0xFFD383);
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    private void quad(Tessellator mesh, double x1, double y1, double x2, double y2, double z) {
        mesh.addVertex(x1, y1, z);
        mesh.addVertex(x2, y1, z);
        mesh.addVertex(x2, y2, z);
        mesh.addVertex(x1, y2, z);
    }

    private void edge(Tessellator mesh, double x1, double y1, double x2, double y2) {
        mesh.addVertex(x1, y1, -.05);
        mesh.addVertex(x2, y2, -.05);
        mesh.addVertex(x2, y2, .05);
        mesh.addVertex(x1, y1, .05);
    }

    @Override protected ResourceLocation getEntityTexture(Entity entity) { return null; }
}
