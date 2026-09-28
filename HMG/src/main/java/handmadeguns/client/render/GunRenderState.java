package handmadeguns.client.render;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import org.lwjgl.opengl.GL11;

/** Restores actual caller state, including lightmap bookkeeping and the projection HMG replaces. */
public final class GunRenderState implements AutoCloseable {
    private final float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
    private final int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    private final int modelDepth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
    private final int projectionDepth = GL11.glGetInteger(GL11.GL_PROJECTION_STACK_DEPTH);
    private final int attributeDepth = GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH);

    public GunRenderState(ItemRenderType type, Object[] data) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_NORMALIZE);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        if ((type == ItemRenderType.EQUIPPED_FIRST_PERSON || type == ItemRenderType.EQUIPPED)
                && data != null && data.length > 1 && data[1] instanceof Entity) {
            int light = ((Entity)data[1]).getBrightnessForRender(handmadeguns.HandmadeGunsCore.smooth);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light & 65535, light >>> 16);
        }
    }

    @Override public void close() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        while (GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) > modelDepth) GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        while (GL11.glGetInteger(GL11.GL_PROJECTION_STACK_DEPTH) > projectionDepth) GL11.glPopMatrix();
        while (GL11.glGetInteger(GL11.GL_ATTRIB_STACK_DEPTH) > attributeDepth) GL11.glPopAttrib();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
        GL11.glMatrixMode(matrixMode);
    }
}
