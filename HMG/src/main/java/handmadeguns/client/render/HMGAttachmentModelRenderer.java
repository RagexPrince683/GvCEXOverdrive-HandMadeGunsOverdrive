package handmadeguns.client.render;

import handmadeguns.items.HMGItemAttachmentBase;
import handmadeguns.items.guns.HMGItem_Unified_Guns;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;

/** Shared local-space renderer for model-based attachments in every gun view. */
public final class HMGAttachmentModelRenderer {
    private HMGAttachmentModelRenderer() {}

    public static void renderInstalled(HMGItem_Unified_Guns gun, ItemStack[] slots, int pass,
                                       float gunPartsScale, ResourceLocation gunTexture) {
        for (int slot = 1; slot <= 5; slot++) renderInstalledSlot(gun, slots, pass, gunPartsScale, gunTexture, slot);
    }

    public static void renderInstalledSlot(HMGItem_Unified_Guns gun, ItemStack[] slots, int pass,
                                           float gunPartsScale, ResourceLocation gunTexture, int slot) {
        if (gun == null || slots == null || slot < 1 || slot > 5 || slot >= slots.length) return;
            ItemStack stack = slots[slot];
            if (stack == null || !(stack.getItem() instanceof HMGItemAttachmentBase)) return;
            HMGItemAttachmentBase attachment = (HMGItemAttachmentBase) stack.getItem();
            if (!attachment.has3dModel(slot)) return;
            IItemRenderer renderer = MinecraftForgeClient.getItemRenderer(stack, IItemRenderer.ItemRenderType.EQUIPPED);
            if (!(renderer instanceof HMGRenderItemCustom)) return;

            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
                    | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_TEXTURE_BIT
                    | GL11.GL_STENCIL_BUFFER_BIT | GL11.GL_CURRENT_BIT);
            try {
                GL11.glPushMatrix();
                try {
                // PartsRender expresses every gun-local position in scaled model units.
                boolean numbered = gun.gunInfo.hasAttachmentLocations[slot];
                if (numbered || gun.gunInfo.hasAttachmentLocation) GL11.glTranslatef(
                        (numbered ? gun.gunInfo.attachmentLocationXs[slot] : gun.gunInfo.attachmentLocationX) * gunPartsScale,
                        (numbered ? gun.gunInfo.attachmentLocationYs[slot] : gun.gunInfo.attachmentLocationY) * gunPartsScale,
                        (numbered ? gun.gunInfo.attachmentLocationZs[slot] : gun.gunInfo.attachmentLocationZ) * gunPartsScale);
                // Existing attachment rotations use Y first; the optional scalar follows that convention.
                if (numbered || gun.gunInfo.hasAttachmentLocation) GL11.glRotatef(
                        numbered ? gun.gunInfo.attachmentLocationRotations[slot] : gun.gunInfo.attachmentLocationRotation, 0, 1, 0);
                ((HMGRenderItemCustom) renderer).renderaspart(pass, slot);
                } finally { GL11.glPopMatrix(); }
            } finally {
                GL11.glPopAttrib();
                // Texture attributes are restored above; bind explicitly for subsequent gun parts.
                Minecraft.getMinecraft().renderEngine.bindTexture(gunTexture);
            }
    }
}
