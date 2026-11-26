package hmggvcmob.render.model;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import handmadeguns.items.guns.HMGItem_Unified_Guns;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

@SideOnly(Side.CLIENT)
public class GVCModelGuerrilla extends ModelBiped {
    public GVCModelGuerrilla() {
        this(0.0F, false);
    }

    protected GVCModelGuerrilla(float size, float y, int textureWidth, int textureHeight) {
        super(size, y, textureWidth, textureHeight);
    }

    public GVCModelGuerrilla(float size, boolean compactTexture) {
        super(size, 0.0F, 64, compactTexture ? 32 : 64);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbAmount, float partialTick) {
        isSneak = entity.isSneaking();
        super.setLivingAnimations(entity, limbSwing, limbAmount, partialTick);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbAmount, float age, float headYaw,
                                  float headPitch, float scale, Entity entity) {
        super.setRotationAngles(limbSwing, limbAmount, age, headYaw, headPitch, scale, entity);
        ItemStack held = ((EntityLivingBase)entity).getHeldItem();
        if (held == null || !(held.getItem() instanceof HMGItem_Unified_Guns)) return;

        boolean dead = !((EntityLivingBase)entity).isEntityAlive();
        boolean reload = !dead && held.hasTagCompound() && held.getTagCompound().getBoolean("IsReloading");
        boolean aim = !dead && !reload && entity.isSneaking() && !entity.isSprinting();
        float pitch = -1.57F;
        if (dead) pitch += 1.02F;
        else if (reload) pitch += 0.37F;
        else if (entity.isSprinting()) pitch += 0.75F;
        else if (!aim) pitch += 0.51F;
        float yaw = aim ? bipedHead.rotateAngleY : bipedHead.rotateAngleY * 0.25F;
        float sway = aim ? 0 : MathHelper.sin(limbSwing * 0.67F) * limbAmount * 0.08F;

        bipedRightArm.rotateAngleX = pitch + (aim ? bipedHead.rotateAngleX : sway);
        bipedRightArm.rotateAngleY = yaw;
        bipedRightArm.rotateAngleZ = 0;
        bipedLeftArm.rotateAngleX = bipedRightArm.rotateAngleX - (reload ? 0.35F : 0.13F);
        bipedLeftArm.rotateAngleY = yaw + (reload ? 0.52F : 0.56F);
        bipedLeftArm.rotateAngleZ = reload ? -0.12F : 0;
    }
}
