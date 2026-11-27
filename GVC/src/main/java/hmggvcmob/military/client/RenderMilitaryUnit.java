
package hmggvcmob.military.client;

import hmggvcmob.military.EntityMilitaryUnit;
import hmggvcmob.render.RenderInfantry;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

public final class RenderMilitaryUnit extends RenderInfantry {
    private static final ResourceLocation PMC = new ResourceLocation("gvcmob:textures/mob/pmc.png");
    private static final ResourceLocation UNION = new ResourceLocation("gvcmob:textures/mob/solider.png");
    private static final ResourceLocation ALLIES = new ResourceLocation("gvcmob:textures/mob/guerrilla.png");

    @Override
    protected ResourceLocation getEntityTexture(EntityLiving entity) {
        String faction = ((EntityMilitaryUnit) entity).faction();
        return "union".equals(faction) ? UNION : "allies".equals(faction) ? ALLIES : PMC;
    }
}
