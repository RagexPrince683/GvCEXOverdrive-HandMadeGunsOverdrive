package handmadeguns.client.render;

import cpw.mods.fml.client.FMLClientHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import handmadeguns.items.HMGItemAttachmentBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import net.minecraft.util.MathHelper;


import java.nio.FloatBuffer;

import static handmadeguns.HandmadeGunsCore.cfgRender_useStencil;
import static handmadeguns.client.render.PartsRender.FBO;
import static java.lang.Math.abs;
import static org.lwjgl.opengl.GL11.*;

public class HMGRenderItemCustom extends RenderItem implements IItemRenderer {
	private static final float ATTACHMENT_INVENTORY_CENTER_X = 8.0F;
	private static final float ATTACHMENT_INVENTORY_CENTER_Y = 8.0F;
	private static final float ATTACHMENT_INVENTORY_CENTER_Z = 0.0F;
	private static final float ATTACHMENT_INVENTORY_BASE_SCALE = 6.0F;
	private static final float ATTACHMENT_INVENTORY_BASE_OFFSET_Y = -0.25F;
	private IModelCustom modeling;
	private ResourceLocation texture;
	private final boolean renderInventory;
	private final boolean attachmentMode;
	private final IModelCustom[] attachmentModels;
	private final ResourceLocation[] attachmentTextures;
	private final int standaloneAttachmentSlot;
	public static float smoothing;
	public NBTTagCompound nbt;

	public HMGRenderItemCustom(IModelCustom modelgun, ResourceLocation texture) {
		this(modelgun, texture, false);
	}

	public HMGRenderItemCustom(IModelCustom modelgun, ResourceLocation texture, boolean renderInventory) {
		this(modelgun, texture, renderInventory, false);
	}

	private HMGRenderItemCustom(IModelCustom modelgun, ResourceLocation texture, boolean renderInventory,
			boolean attachmentMode) {
		modeling = modelgun;
		this.texture = texture;
		this.renderInventory = renderInventory;
		this.attachmentMode = attachmentMode;
		this.attachmentModels = null;
		this.attachmentTextures = null;
		this.standaloneAttachmentSlot = -1;
	}

	public static HMGRenderItemCustom forAttachment(IModelCustom model, ResourceLocation texture) {
		return new HMGRenderItemCustom(model, texture, true, true);
	}

	private HMGRenderItemCustom(IModelCustom[] models, ResourceLocation[] textures, int standaloneSlot) {
		modeling = models[standaloneSlot]; texture = textures[standaloneSlot];
		renderInventory = true; attachmentMode = true;
		attachmentModels = models; attachmentTextures = textures; standaloneAttachmentSlot = standaloneSlot;
	}

	public static HMGRenderItemCustom forAttachment(IModelCustom[] models, ResourceLocation[] textures, int standaloneSlot) {
		return new HMGRenderItemCustom(models, textures, standaloneSlot);
	}

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {

		switch (type) {
			// case 1: //entity third person
			case INVENTORY:
				return renderInventory;
			case FIRST_PERSON_MAP:
				return false;
			case EQUIPPED_FIRST_PERSON:
			case ENTITY:
			case EQUIPPED:
				return true;
		}
		return false;
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
		switch (type) {
			// case 1:
			case INVENTORY:
				return renderInventory;
			case FIRST_PERSON_MAP:
				return false;
			case EQUIPPED_FIRST_PERSON:
			case ENTITY:
			case EQUIPPED:
				return true;
		}
		return false;
	}
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		if (attachmentMode) GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT
				| GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_TEXTURE_BIT);
		try {
		GL11.glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		GL11.glColor4f(1, 1, 1, 1F);
		nbt = item.getTagCompound();
		switch (type) {
			case INVENTORY:
				GL11.glPushMatrix();
				try {
					if (attachmentMode) {
						HMGItemAttachmentBase attachment = item != null
								&& item.getItem() instanceof HMGItemAttachmentBase
								? (HMGItemAttachmentBase)item.getItem() : null;
						float inventoryScale = attachment != null ? attachment.inventoryScale : 1.0F;
						float inventoryOffsetX = attachment != null ? attachment.inventoryOffsetX : 0.0F;
						float inventoryOffsetY = attachment != null ? attachment.inventoryOffsetY : 0.0F;
						float inventoryOffsetZ = attachment != null ? attachment.inventoryOffsetZ : 0.0F;
						GL11.glTranslatef(ATTACHMENT_INVENTORY_CENTER_X, ATTACHMENT_INVENTORY_CENTER_Y,
								ATTACHMENT_INVENTORY_CENTER_Z);
						// Forge supplies inventory rendering in 16x16 GUI coordinates. Convert model units
						// to that space independently of the pack-authored scale multiplier.
						GL11.glScalef(ATTACHMENT_INVENTORY_BASE_SCALE, ATTACHMENT_INVENTORY_BASE_SCALE,
								ATTACHMENT_INVENTORY_BASE_SCALE);
						// Preserve the original attachment model centering. A zero author offset means
						// no extra displacement; it does not opt out of this renderer correction.
						GL11.glTranslatef(0.0F, ATTACHMENT_INVENTORY_BASE_OFFSET_Y, 0.0F);
						// Author offsets are model-space values converted by the fixed base scale, but
						// deliberately are not multiplied by InventoryScale.
						GL11.glTranslatef(inventoryOffsetX, inventoryOffsetY, inventoryOffsetZ);
						GL11.glScalef(inventoryScale, inventoryScale, inventoryScale);
						GL11.glRotatef(25F, 1, 0, 0);
						GL11.glRotatef(135F, 0, 1, 0);
					} else GL11.glRotatef(180F, 1, 0, 0);
					Minecraft.getMinecraft().renderEngine.bindTexture(texture);
					GL11.glColor4f(1F, 1F, 1F, 1F);
					modeling.renderAll();
				} finally {
					GL11.glPopMatrix();
				}
				break;
			case ENTITY:
				if (!renderInventory) break;
				GL11.glPushMatrix();
				try {
				if (attachmentMode) {
					GL11.glScalef(0.35F, 0.35F, 0.35F);
					GL11.glRotatef(180F, 1, 0, 0);
				} else GL11.glRotatef(180F, 1, 0, 0);
				Minecraft.getMinecraft().renderEngine.bindTexture(texture);
				modeling.renderAll();
				} finally { GL11.glPopMatrix(); }
				break;
			case EQUIPPED_FIRST_PERSON:
			{
				Minecraft mc = Minecraft.getMinecraft();

				// Player-configured FOV (vanilla default is 95)
				float playerFov = mc.gameSettings.fovSetting;

				// Authored at vanilla 95 FOV
				float fovScale = playerFov / 95.0f;

				// Clamp to avoid insanity
				fovScale = Math.max(0.7F, Math.min(1.3F, fovScale));

				GL11.glPushMatrix();
				try {

				// FOV compensation (ONE LINE FIX)
				GL11.glScalef(fovScale, fovScale, fovScale);

				GL11.glRotatef(180F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(50F, 0.0F, 1.0F, 0.0F);
				GL11.glRotatef(180F, 0.0F, 0.0F, 1.0F);

				mc.renderEngine.bindTexture(texture);
				modeling.renderAll();

				} finally { GL11.glPopMatrix(); }
				break;
			}
			case EQUIPPED://thrid
				GL11.glPushMatrix();
				try {
				GL11.glRotatef(180F, 1.0F, 0.0F, 0.0F);
				GL11.glRotatef(50F, 0.0F, 1.0F, 0.0F);
				GL11.glRotatef(180F, 0.0F, 0.0F, 1.0F);
				Minecraft.getMinecraft().renderEngine.bindTexture(texture);
				modeling.renderAll();
				} finally { GL11.glPopMatrix(); }
				break;

			case FIRST_PERSON_MAP:
				break;
		}

		GL11.glDepthMask(true);
		GL11.glDisable(GL_BLEND);
		} finally { if (attachmentMode) GL11.glPopAttrib(); }
	}

	public void renderaspart() {
		renderaspart(0);
		renderaspart(1);
	}
	public void renderaspart(int pass) {
		renderaspart(pass, 0);
	}
	public void renderaspart(int pass, int attachmentSlot) {
		IModelCustom selectedModel = modeling;
		ResourceLocation selectedTexture = texture;
		if (attachmentModels != null && attachmentSlot >= 1 && attachmentSlot <= 5) {
			int selectedSlot = attachmentModels[attachmentSlot] != null ? attachmentSlot : 0;
			selectedModel = attachmentModels[selectedSlot];
			selectedTexture = attachmentTextures[selectedSlot];
		}
		if (selectedModel == null || selectedTexture == null) return;
		glEnable(GL_ALPHA_TEST);
		glEnable(GL_DEPTH_TEST);
		if(pass == 1) {
			glEnable(GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			GL11.glDepthMask(false);
			glAlphaFunc(GL_LESS, 1.0F);
		}else {
			glDisable(GL_BLEND);
			GL11.glDepthMask(true);
			glAlphaFunc(GL_EQUAL, 1.0F);
		}
		GL11.glColor4f(1, 1, 1, 1F);
		Minecraft.getMinecraft().renderEngine.bindTexture(selectedTexture);
		GL11.glScalef(1, 1, 1);
		selectedModel.renderAllExcept("light");


		boolean lightingEnabled = GL11.glIsEnabled(GL11.GL_LIGHTING);
		float lastBrightnessX = OpenGlHelper.lastBrightnessX;
		float lastBrightnessY = OpenGlHelper.lastBrightnessY;
		try {
		GL11.glDisable(GL11.GL_LIGHTING);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
		selectedModel.renderPart("light");
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)lastBrightnessX, (float)lastBrightnessY);
		if (lightingEnabled) GL11.glEnable(GL11.GL_LIGHTING);
		else GL11.glDisable(GL11.GL_LIGHTING);


		if(cfgRender_useStencil && pass==1){
			//INSERT : �t���[���o�b�t�@�ɕ`��J�n
			//       : �ی���Matrix����w�[��
			//       : �e�̃e�N�X�`������bind
//			FBO.start();
//			GL11.glPushMatrix();
			FMLClientHandler.instance().getClient().getTextureManager().bindTexture(selectedTexture);
			glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			//INSERT-END

			glClear(GL_STENCIL_BUFFER_BIT);
			glEnable(GL_STENCIL_TEST);
			glStencilMask(1);

			glStencilFunc(
					GL_ALWAYS,   // GLenum func
					1,          // GLint ref
					~0);// GLuint mask
			glStencilOp(
					GL_KEEP,
					GL_KEEP,
					GL_REPLACE);

			GL11.glDepthMask(false);
			glAlphaFunc(GL_ALWAYS, 1);
			glColorMask(
					false,   // GLboolean red
					false,   // GLboolean green
					false,   // GLboolean blue
					false);
			glEnable(GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			GL11.glDepthMask(false);
			glAlphaFunc(GL_GREATER, 0);

			selectedModel.renderPart("plate");

			GL11.glDepthMask(true);
			glAlphaFunc(GL_EQUAL, 1);
			glColorMask(
					true,   // GLboolean red
					true,   // GLboolean green
					true,   // GLboolean blue
					true);

			glDisable(GL_DEPTH_TEST);


			glStencilFunc(
					GL_EQUAL,   // GLenum func
					1,          // GLint ref
					~0);// GLuint mask

			glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
			glAlphaFunc(GL_GREATER, 0);
			GL11.glDepthMask(false);

			GL11.glDepthFunc(GL11.GL_ALWAYS);//�����`��
			GL11.glDisable(GL11.GL_LIGHTING);
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
			selectedModel.renderPart("reticle_light");
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
			selectedModel.renderPart("reticle");
			GL11.glDepthFunc(GL11.GL_LEQUAL);
			GL11.glDepthMask(true);
			glDisable(GL_STENCIL_TEST);
			glEnable(GL_DEPTH_TEST);



//			//INSET : Matrix����w�֕���
//			//      : FBO����e�N�X�`��ID���擾
//			//      : ��ʂɏo�͂ł���悤��Matrix��ۑ���������
//			//      : �e�N�X�`����ViwerPort�ɏo��
//			//      : �ۑ�����Matrix���Ăі߂�
//			GL11.glPopMatrix();
//			int tex = FBO.end();
//
//			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
//
//			glPushMatrix();
//			glDisable(GL_DEPTH_TEST);
//
//			glMatrixMode(GL_PROJECTION);
//			FloatBuffer projectionMatrix = BufferUtils.createFloatBuffer(16);
//			glGetFloat(GL_PROJECTION_MATRIX, projectionMatrix);
//			glLoadIdentity();
//
//			glMatrixMode(GL_MODELVIEW);
//			FloatBuffer modelViewMatrix = BufferUtils.createFloatBuffer(16);
//			glGetFloat(GL_PROJECTION_MATRIX, modelViewMatrix);
//			glLoadIdentity();
//
//			glOrtho(0,1,1,0,-1,1);
//			glDisable(GL_CULL_FACE);
//			glBindTexture(GL_TEXTURE_2D, tex);
//			glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
//			glBegin(GL_QUADS);
//			glTexCoord2d(0.0D, 1.0D);glVertex2d(0.0D, 0.0D);
//			glTexCoord2d(0.0D, 0.0D);glVertex2d(0.0D, 1.0D);
//			glTexCoord2d(1.0D, 0.0D);glVertex2d(1.0D, 1.0D);
//			glTexCoord2d(1.0D, 1.0D);glVertex2d(1.0D, 0.0D);
//			glEnd();
//			glEnable(GL_CULL_FACE);
//
//			glMatrixMode(GL_PROJECTION);
//			glLoadMatrix(projectionMatrix);
//			glMatrixMode(GL_MODELVIEW);
//			glLoadMatrix(modelViewMatrix);
//			glPopMatrix();
//			//INSERT-END
			FMLClientHandler.instance().getClient().getTextureManager().bindTexture(selectedTexture);
			GL11.glDepthFunc(GL11.GL_LEQUAL);
			glEnable(GL_DEPTH_TEST);
			if(pass == 1) {
				glEnable(GL_BLEND);
				glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
				GL11.glDepthMask(false);
				glAlphaFunc(GL_LESS, 1.0F);
			}else {
				glDisable(GL_BLEND);
				GL11.glDepthMask(true);
				glAlphaFunc(GL_EQUAL, 1);
			}
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float)lastBrightnessX, (float)lastBrightnessY);
			if (lightingEnabled) GL11.glEnable(GL11.GL_LIGHTING);
			else GL11.glDisable(GL11.GL_LIGHTING);
		}
		if(pass == 1) {
			glEnable(GL_BLEND);
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			GL11.glDepthMask(false);
			glAlphaFunc(GL_LESS, 1.0F);
		}else {
			glDisable(GL_BLEND);
			GL11.glDepthMask(true);
			glAlphaFunc(GL_EQUAL, 1);
		}

		GL11.glDepthMask(true);
		} finally {
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
			if (lightingEnabled) GL11.glEnable(GL11.GL_LIGHTING);
			else GL11.glDisable(GL11.GL_LIGHTING);
		}
	}
}
