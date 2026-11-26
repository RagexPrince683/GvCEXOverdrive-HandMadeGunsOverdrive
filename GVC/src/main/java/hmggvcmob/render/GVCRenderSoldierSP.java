
package hmggvcmob.render;

import hmggvcmob.GVCMobPlus;


import net.minecraft.entity.EntityLiving; 
import net.minecraft.util.ResourceLocation; 

 

public class GVCRenderSoldierSP extends RenderInfantry
{ 
	
	private static final ResourceLocation skeletonTextures = new ResourceLocation("gvcmob:textures/mob/solidersp.png");
	private static final ResourceLocation skeletonTextures2 = new ResourceLocation("gvcmob:textures/mob/guerrillasp.png"); 

 
 	public GVCRenderSoldierSP() 
	{ 
 		
        super();
	} 
 
 
 	
 	@Override 
 	protected ResourceLocation getEntityTexture(EntityLiving par1EntityLiving) 
 	{ 
 		if(GVCMobPlus.cfg_modeGorC){
 			return this.skeletonTextures; 
 		}else{
 			return this.skeletonTextures2; 
 		} 
 	} 
 } 

 