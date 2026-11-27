package hmggvcmob.render;

import handmadeguns.client.render.HMGBipedGunRender;
import hmggvcmob.render.model.GVCModelGuerrilla;
import hmggvcmob.military.MilitaryRelations;
import hmggvcmob.military.client.FactionInsignia;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.EntityLiving;

public abstract class RenderInfantry extends RenderBiped {
    private static final boolean HAS_HMG_HAND_FRAME = hasHmgHandFrame();
    private final GVCModelGuerrilla infantryModel;

    private static boolean hasHmgHandFrame() {
        try {
            Class.forName("handmadeguns.client.render.HMGBipedGunRender", false, RenderInfantry.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException absent) {
            return false;
        }
    }

    protected RenderInfantry() {
        this(new GVCModelGuerrilla());
    }

    private RenderInfantry(GVCModelGuerrilla model) {
        super(model, 0.5F);
        infantryModel = model;
    }

    @Override
    protected void renderEquippedItems(EntityLiving entity, float partialTick) {
        if (HAS_HMG_HAND_FRAME) {
            HmgHandFrame.render(entity, () -> super.renderEquippedItems(entity, partialTick));
        } else {
            super.renderEquippedItems(entity, partialTick);
        }
        if (!entity.isInvisible() && !entity.isChild()) {
            FactionInsignia badge = FactionInsignia.of(MilitaryRelations.faction(entity));
            if (badge != null) badge.drawBand(infantryModel.bipedLeftArm, entity.getEquipmentInSlot(3) != null);
        }
    }

    private static class HmgHandFrame {
        static void render(EntityLiving entity, Runnable equipment) {
            HMGBipedGunRender.withBipedHand(entity, equipment);
        }
    }
}
