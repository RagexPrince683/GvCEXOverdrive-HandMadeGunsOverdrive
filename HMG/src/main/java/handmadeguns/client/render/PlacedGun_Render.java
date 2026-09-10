package handmadeguns.client.render;

import cpw.mods.fml.client.FMLClientHandler;
import handmadeguns.entity.PlacedGunEntity;
import handmadeguns.items.HMGItemAttachment_reddot;
import handmadeguns.items.HMGItemAttachment_scope;
import handmadeguns.items.HMGItemSightBase;
import handmadeguns.HandmadeGunsCore;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import static handmadeguns.HandmadeGunsCore.smooth;
import static net.minecraft.util.MathHelper.wrapAngleTo180_float;

public class PlacedGun_Render extends Render {

    //TODO: UNFUCK THIS ENTIRE GODDAMN CLASS HOLY FUCKING SHIT ITS SO ASS
    @Override
    public void doRender(Entity p_76986_1_, double p_76986_2_, double p_76986_4_, double p_76986_6_, float p_76986_8_, float p_76986_9_) {
        doRender((PlacedGunEntity)p_76986_1_,p_76986_2_,p_76986_4_,p_76986_6_,p_76986_8_,p_76986_9_);
    }

    public void doRender(PlacedGunEntity entity, double p_180551_2_, double p_180551_4_, double p_180551_6_, float p_180551_8_, float p_180551_9_) {
        float lastBrightnessX = OpenGlHelper.lastBrightnessX;
        float lastBrightnessY = OpenGlHelper.lastBrightnessY;

        // ADS visibility checks (keeps original behavior but safe against NPEs)
        if (entity.riddenByEntity == FMLClientHandler.instance().getClientPlayerEntity() && entity.gunStack != null && entity.gunItem != null) {
            ItemStack[] items = new ItemStack[6];
            NBTTagCompound nbt = entity.gunStack.getTagCompound();
            if (nbt == null) {
                try {
                    entity.gunItem.checkTags(entity.gunStack);
                } catch (Throwable ignored) {
                    // keep going — checkTags is best-effort
                }
            }
            nbt = entity.gunStack.getTagCompound();

            // default nulls (matching your original code)
            items[0] = null;
            items[1] = null;//サイト (sight)
            items[2] = null;//レーザーサイト他
            items[3] = null;//マズルアタッチメント
            items[4] = null;//アンダーバレル
            items[5] = null;//マガジン

            if (nbt != null && nbt.hasKey("Items")) {
                NBTTagList tags = (NBTTagList) nbt.getTag("Items");
                if (tags != null) {
                    for (int i = 0; i < tags.tagCount(); i++) {
                        NBTTagCompound tagCompound = tags.getCompoundTagAt(i);
                        if (tagCompound == null) continue;
                        int slot = tagCompound.getByte("Slot");
                        if (slot >= 0 && slot < items.length) {
                            try {
                                items[slot] = ItemStack.loadItemStackFromNBT(tagCompound);
                            } catch (Throwable ignored) { }
                        }
                    }
                }
            }

            ItemStack itemstackSight = items[1];
            // IMPORTANT: use the **rider's** sprint state rather than entity.isSprinting()
            //boolean riderSprinting = entity.riddenByEntity != null && entity.riddenByEntity.isSprinting();

            if (HandmadeGunsCore.Key_ADS(entity.riddenByEntity)) { // && !riderSprinting
                if (itemstackSight != null && itemstackSight.getItem() instanceof HMGItemSightBase) {
                    HMGItemSightBase sight = (HMGItemSightBase) itemstackSight.getItem();
                    if (sight.scopeonly) {
                        return;
                    } else if (itemstackSight.getItem() instanceof HMGItemAttachment_reddot) {
                        if (!entity.gunItem.gunInfo.zoomrer) {
                            return;
                        }
                    } else if (itemstackSight.getItem() instanceof HMGItemAttachment_scope) {
                        if (!entity.gunItem.gunInfo.zoomres) {
                            return;
                        }
                    }
                } else {
                    if (!entity.gunItem.gunInfo.zoomren) {
                        return;
                    }
                }
            }
        }

        GL11.glPushMatrix();
        GL11.glTranslatef((float) p_180551_2_, (float) p_180551_4_ + entity.gunyoffset, (float) p_180551_6_);

        if (entity.gunStack != null) {
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            IItemRenderer gunrender = MinecraftForgeClient.getItemRenderer(entity.gunStack, IItemRenderer.ItemRenderType.EQUIPPED);

            if (gunrender instanceof HMGRenderItemGun_U_NEW) {
                /*
                 * Critical: save previous static values, set our values, render, then restore
                 * and isolate GL transforms so nothing leaks to other draws.
                 */
                GL11.glPushMatrix();
                try {
                    GL11.glRotatef(-(entity.rotationYaw), 0.0F, 1.0F, 0.0F);

                    // Save old static state
                    synchronized (HMGRenderItemGun_U_NEW.class) {
                        boolean oldIsPlaced = HMGRenderItemGun_U_NEW.isPlacedGun;
                        float oldYaw = HMGRenderItemGun_U_NEW.turretYaw;
                        float oldPitch = HMGRenderItemGun_U_NEW.turretPitch;

                        try {
                            HMGRenderItemGun_U_NEW.isPlacedGun = true;
                            HMGRenderItemGun_U_NEW.turretYaw = wrapAngleTo180_float(entity.rotationYaw - (entity.prevrotationYawGun + (entity.rotationYawGun - entity.prevrotationYawGun) * smooth));
                            HMGRenderItemGun_U_NEW.turretPitch = wrapAngleTo180_float((entity.prevRotationPitch + wrapAngleTo180_float(entity.rotationPitch - entity.prevRotationPitch) * smooth));

                            GL11.glScalef(0.5f, 0.5f, 0.5f);
                            try {
                                if (((HMGRenderItemGun_U_NEW)gunrender).partsRender_gun.animationDefinition != null)
                                    gunrender.renderItem(IItemRenderer.ItemRenderType.ENTITY, entity.gunStack, entity);
                                else gunrender.renderItem(IItemRenderer.ItemRenderType.ENTITY, entity.gunStack);
                            } catch (Throwable t) {
                                // fail-safe: avoid breaking the game if a custom renderer throws
                                t.printStackTrace();
                            }
                        } finally {
                            // restore previous static state (no matter what)
                            HMGRenderItemGun_U_NEW.isPlacedGun = oldIsPlaced;
                            HMGRenderItemGun_U_NEW.turretYaw = oldYaw;
                            HMGRenderItemGun_U_NEW.turretPitch = oldPitch;
                        }
                    } // synchronized
                } finally {
                    GL11.glPopMatrix();
                }
            } else if (gunrender instanceof HMGRenderItemGun_U) {
                GL11.glPushMatrix();
                try {
                    // base は matbase を利用してそれらしく描画可能
                    GL11.glRotatef(-(entity.prevrotationYawGun + (entity.rotationYawGun - entity.prevrotationYawGun) * smooth), 0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(-(-entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * smooth), 1.0F, 0.0F, 0.0F);
                    GL11.glScalef(0.5f, 0.5f, 0.5f);
                    try {
                        gunrender.renderItem(IItemRenderer.ItemRenderType.ENTITY, entity.gunStack);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                } finally {
                    GL11.glPopMatrix();
                }
            }

            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        }

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)lastBrightnessX, (float)lastBrightnessY);
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
        return null;
    }
}
