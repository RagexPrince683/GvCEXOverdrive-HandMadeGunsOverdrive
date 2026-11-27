package hmggvcmob.render;

import cpw.mods.fml.client.FMLClientHandler;
import handmadeguns.client.render.ModelSetAndData;
import handmadeguns.client.render.IModelCustom_HMG;
import hmggvcmob.tile.TileEntityFlag;
import hmggvcmob.camp.CampObj;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import static java.lang.StrictMath.atan2;
import static java.lang.StrictMath.toDegrees;
import static org.lwjgl.opengl.GL11.*;

public class RenderTileFlag extends TileEntitySpecialRenderer {
    
    private final Map<CampObj, ModelSetAndData> models = new HashMap<>();

    public void renderTileEntityAt(TileEntityFlag tileEntity, double v, double v1, double v2, float v3) {
        if (tileEntity.campObj != null) {
            ModelSetAndData flag = models.computeIfAbsent(tileEntity.campObj,
                    camp -> new ModelSetAndData(camp.campBlockObjModel, camp.campBlockTextureModel));
            if (flag.model instanceof IModelCustom_HMG && !((IModelCustom_HMG)flag.model).isReady()) return;
            bindTexture(flag.texture);
            GL11.glDepthMask(true);
            GL11.glPushMatrix();
            GL11.glTranslatef((float) v + 0.5f, (float) v1 + 1f, (float) v2 + 0.5f);
            GL11.glScalef(4, 4, 4);
            GL11.glRotatef((float) toDegrees(atan2(tileEntity.xCoord, tileEntity.zCoord)) + 90, 0, 1, 0);
            flag.model.renderPart("obj2");
            flag.model.renderPart("obj4");
            flag.model.renderPart("obj5");
            flag.model.renderPart("obj5a");
            GL11.glScalef(1f/4, 1f/4, 1f/4);
            GL11.glTranslatef(0.7f, 0.8f, 0.5f);
            GL11.glRotatef(-90, 0, 1, 0);
            GL11.glRotatef(180, 0, 0, 1);
            GL11.glScalef(1f/128, 1f/128, 1f/128);
            renderString(StatCollector.translateToLocalFormatted("gvc.flag.next_spawn", tileEntity.respawncycle / 20));
            GL11.glTranslatef(0, -16, 0);
            renderString(StatCollector.translateToLocalFormatted("gvc.flag.capture",
                    tileEntity.campObj.maxFlagHeight - tileEntity.flagHeight, tileEntity.campObj.maxFlagHeight));
            GL11.glTranslatef(0, -16, 0);
            renderString(StatCollector.translateToLocal(tileEntity.campObj.getTranslationKey()));
            GL11.glPopMatrix();

        }
    
    
    }

    @Override
    public void renderTileEntityAt(TileEntity tileEntity, double v, double v1, double v2, float v3) {
        this.renderTileEntityAt((TileEntityFlag)tileEntity,v,v1,v2,v3);
    }

    public void renderString(String renderObj){
        GL11.glDisable(GL_BLEND);
        GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        Minecraft minecraft = FMLClientHandler.instance().getClient();
        FontRenderer fontrenderer = minecraft.fontRenderer;
        Tessellator tessellator = Tessellator.instance;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        int width = fontrenderer.getStringWidth(renderObj) / 2;
        GL11.glEnable(GL11.GL_BLEND);
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.25F);
        tessellator.addVertex((double)(-width - 1), -1.0D, 0.0D);
        tessellator.addVertex((double)(-width - 1), 8.0D, 0.0D);
        tessellator.addVertex((double)(width + 1), 8.0D, 0.0D);
        tessellator.addVertex((double)(width + 1), -1.0D, 0.0D);
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        fontrenderer.drawString(renderObj,-width,0,0xFFFFFF);
    }
}
