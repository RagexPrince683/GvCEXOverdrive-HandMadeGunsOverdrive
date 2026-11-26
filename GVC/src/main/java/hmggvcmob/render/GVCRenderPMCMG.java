
package hmggvcmob.render;

import net.minecraft.entity.EntityLiving; 
import net.minecraft.util.ResourceLocation; 

 

public class GVCRenderPMCMG extends RenderInfantry
{ 
	
	private static final ResourceLocation skeletonTextures = new ResourceLocation("gvcmob:textures/mob/pmcmg.png"); 

 
 	public GVCRenderPMCMG() 
	{ 
 		
        super();
	} 
 
 
 	
 	@Override 
 	protected ResourceLocation getEntityTexture(EntityLiving par1EntityLiving) 
 	{ 
 		return this.skeletonTextures; 
 	} 
 } 

 