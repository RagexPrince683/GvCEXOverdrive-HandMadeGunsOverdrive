
package hmggvcmob.render;

import net.minecraft.entity.EntityLiving; 
import net.minecraft.util.ResourceLocation; 

 

public class GVCRenderPMC extends RenderInfantry
{ 
	
	private static final ResourceLocation skeletonTextures = new ResourceLocation("gvcmob:textures/mob/pmc.png"); 

 
 	public GVCRenderPMC() 
	{ 
 		
        super();
	} 
 
 
 	
 	@Override 
 	protected ResourceLocation getEntityTexture(EntityLiving par1EntityLiving) 
 	{ 
 		return this.skeletonTextures; 
 	} 
 } 

 