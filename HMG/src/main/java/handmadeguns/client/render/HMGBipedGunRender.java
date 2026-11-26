package handmadeguns.client.render;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

/** Shared hand frame for custom models drawn through Forge's RenderBiped path. */
@SideOnly(Side.CLIENT)
public final class HMGBipedGunRender {
    private static EntityLivingBase current;

    private HMGBipedGunRender() {
    }

    public static void withBipedHand(EntityLivingBase owner, Runnable renderEquipment) {
        EntityLivingBase previous = current;
        current = owner;
        try {
            renderEquipment.run();
        } finally {
            current = previous;
        }
    }

    public static boolean ownsHand(Object entity, ItemStack stack) {
        return current != null && current == entity && current.getHeldItem() == stack
                && stack.getItem().isFull3D();
    }

    public static void applyHandFrame() {
        // Undo EQUIPPED_BLOCK and RenderBiped's full-3D item rotation/scale. Keep
        // the already-posed arm, then place the grip at the center of its palm.
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        GL11.glRotatef(-45, 0, 1, 0);
        GL11.glRotatef(100, 1, 0, 0);
        GL11.glScalef(1.6F, -1.6F, 1.6F);
        GL11.glTranslatef(0, 0, -0.0625F);
        GL11.glRotatef(-90, 1, 0, 0);
        // Retain the legacy 0.625 * 0.5 * 0.6 model scale independently of the anchor.
        GL11.glScalef(0.1875F, 0.1875F, 0.1875F);
    }
}
