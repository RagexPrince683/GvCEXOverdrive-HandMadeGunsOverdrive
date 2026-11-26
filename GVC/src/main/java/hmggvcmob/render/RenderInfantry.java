package hmggvcmob.render;

import handmadeguns.client.render.HMGBipedGunRender;
import hmggvcmob.render.model.GVCModelGuerrilla;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.EntityLiving;

public abstract class RenderInfantry extends RenderBiped {
    private static final boolean HAS_HMG_HAND_FRAME = hasHmgHandFrame();

    private static boolean hasHmgHandFrame() {
        try {
            Class.forName("handmadeguns.client.render.HMGBipedGunRender", false, RenderInfantry.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException absent) {
            return false;
        }
    }

    protected RenderInfantry() {
        super(new GVCModelGuerrilla(), 0.5F);
    }

    @Override
    protected void renderEquippedItems(EntityLiving entity, float partialTick) {
        if (HAS_HMG_HAND_FRAME) {
            HmgHandFrame.render(entity, () -> super.renderEquippedItems(entity, partialTick));
        } else {
            super.renderEquippedItems(entity, partialTick);
        }
    }

    private static class HmgHandFrame {
        static void render(EntityLiving entity, Runnable equipment) {
            HMGBipedGunRender.withBipedHand(entity, equipment);
        }
    }
}
