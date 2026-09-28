package handmadeguns.client.modelLoader.emb_modelloader;

import java.util.ArrayList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import handmadeguns.client.modelLoader.obj_modelloaderMod.obj.HMGGroupObject;
import handmadeguns.client.modelLoader.HMGModelTessellator;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

@SideOnly(Side.CLIENT)
public class MQO_GroupObject extends HMGGroupObject
{
	public String name;
	public ArrayList[] faces_PerMat;
	public int glDrawingMode;

	public MQO_Material currentMaterial;
	public MQO_MetasequoiaObject mqo_metasequoiaObject;
	private int displayList = -1;
	private boolean cpuDataReleased = false;

	public MQO_GroupObject()
	{
		this("",null);
	}

	public MQO_GroupObject(String name, MQO_MetasequoiaObject mqo_metasequoiaObject)
	{
		this(name, -1);
		this.mqo_metasequoiaObject = mqo_metasequoiaObject;
	}

	public MQO_GroupObject(String name, int glDrawingMode)
	{
		this.name = name;
		this.glDrawingMode = glDrawingMode;
	}


	public void initDisplay(){
		this.displayList = GLAllocation.generateDisplayLists(1);
		GL11.glNewList(this.displayList, GL11.GL_COMPILE);
		render_init();
		GL11.glEndList();
		releaseCpuRenderData();
	}

	private void releaseCpuRenderData()
	{
		if (!cpuDataReleased && faces_PerMat != null)
		{
			for (ArrayList faces : faces_PerMat)
			{
				if (faces != null)
				{
					faces.clear();
				}
			}
			faces_PerMat = null;
			cpuDataReleased = true;
		}
	}

	public void render()
	{
		if(displayList == -1)initDisplay();
		else if(displayList != 0) GL11.glCallList(this.displayList);
		else initDisplay();
	}

	public void releaseDisplayList()
	{
		if (displayList > 0)
		{
			GLAllocation.deleteDisplayLists(displayList);
			displayList = -1;
		}
	}

	public void render_init()
	{
		if (faces_PerMat == null) return;
		for(int i = 0; i < faces_PerMat.length; i++) {
			if (faces_PerMat[i].size() > 0) {
				currentMaterial = null;
				if(mqo_metasequoiaObject.materials != null)currentMaterial = mqo_metasequoiaObject.materials[i];
				Tessellator tessellator = HMGModelTessellator.create();
				GL11.glPushAttrib(GL11.GL_LIGHTING_BIT | GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
				tessellator.startDrawing(glDrawingMode);
				try {
					for (MQO_Face face : (ArrayList<MQO_Face>) faces_PerMat[i]) {
						face.addFaceForRender(tessellator);
					}

				if (currentMaterial != null) {
					GL11.glMaterial(GL_FRONT_AND_BACK, GL11.GL_DIFFUSE, currentMaterial.dif_Buf);
					GL11.glMaterial(GL_FRONT_AND_BACK, GL11.GL_AMBIENT, currentMaterial.amb_Buf);
					GL11.glMaterial(GL_FRONT_AND_BACK, GL11.GL_SPECULAR, currentMaterial.spc_Buf);
					GL11.glMaterial(GL_FRONT_AND_BACK, GL11.GL_EMISSION, currentMaterial.emi_Buf);
					GL11.glMaterialf(GL_FRONT_AND_BACK, GL11.GL_SHININESS, currentMaterial.power);
					GL11.glDisable(GL11.GL_COLOR_MATERIAL);
				}
				} finally {
					try { tessellator.draw(); } finally { GL11.glPopAttrib(); }
				}
			}
		}
	}
}
