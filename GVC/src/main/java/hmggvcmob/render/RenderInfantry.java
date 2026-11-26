package hmggvcmob.render;

import handmadeguns.client.render.HMGBipedGunRender;
import hmggvcmob.render.model.GVCModelGuerrilla;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.EntityLiving;

public abstract class RenderInfantry extends RenderBiped {
    protected RenderInfantry() {
        super(new GVCModelGuerrilla(), 0.5F);
    }

    @Override
    protected void renderEquippedItems(EntityLiving entity, float partialTick) {
        HMGBipedGunRender.withBipedHand(entity, () -> super.renderEquippedItems(entity, partialTick));
    }
}
