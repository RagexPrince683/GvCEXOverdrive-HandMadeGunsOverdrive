
package hmggvcmob.render;

import net.minecraft.entity.EntityLiving; 
import net.minecraft.util.ResourceLocation; 

 

public class GVCRenderPMCSP extends RenderInfantry
{ 
	
	private static final ResourceLocation skeletonTextures = new ResourceLocation("gvcmob:textures/mob/pmcsp.png"); 

 
 	public GVCRenderPMCSP() 
	{ 
 		
        super();
	} 
 
 
 	
 	@Override 
 	protected ResourceLocation getEntityTexture(EntityLiving par1EntityLiving) 
 	{ 
 		return this.skeletonTextures; 
 	} 
 } 

 