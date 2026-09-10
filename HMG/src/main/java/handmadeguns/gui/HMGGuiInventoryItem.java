package handmadeguns.gui;
 
import handmadeguns.client.render.HMGRenderItemGun_U;
import handmadeguns.client.render.HMGRenderItemGun_U_NEW;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import static net.minecraft.util.MathHelper.wrapAngleTo180_float;

public class HMGGuiInventoryItem extends GuiContainer
{
    private static final float DEFAULT_PREVIEW_SCALE = 60.0F;
    private static final float HORIZONTAL_DRAG_SENSITIVITY = 0.6F;
    private static final float VERTICAL_DRAG_SENSITIVITY = 0.6F;
    private static final float MIN_PREVIEW_PITCH = -80.0F;
    private static final float MAX_PREVIEW_PITCH = 80.0F;
    private static final float ZOOM_STEP = 0.1F;
    private static final float MIN_PREVIEW_ZOOM = 0.5F;
    private static final float MAX_PREVIEW_ZOOM = 2.0F;
    private static final int PREVIEW_LEFT = 8;
    private static final int PREVIEW_TOP = 55;
    private static final int PREVIEW_RIGHT = 168;
    private static final int PREVIEW_BOTTOM = 132;

    private float previewYaw;
    private float previewPitch;
    private float previewZoom = 1.0F;
    private boolean rotatingPreview;
    private int previousMouseX;
    private int previousMouseY;
    //private static final ResourceLocation texture = new ResourceLocation("textures/gui/container/generic_54.png");
    private static final ResourceLocation texture = new ResourceLocation("handmadeguns:textures/gui/AR.png");
 
    public HMGGuiInventoryItem(InventoryPlayer inventoryPlayer, ItemStack itemstack)
    {
        super(new HMGContainerInventoryItem(inventoryPlayer, itemstack));
        this.ySize = 222;
    }

	/*
        ChestとかInventoryとか文字を描画する
     */
    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
    {
        // text (unchanged)
        this.fontRendererObj.drawString("Attachments", 8, 6, 4210752);
        this.fontRendererObj.drawString("Inventory", 8, this.ySize - 96 + 2, 4210752);
        this.fontRendererObj.drawString("Sight/Support/Muzzle/Under/SP bullets", 8, 24, 4210752);

        // ===== compute true screen center, convert to GUI-local coords =====
        ScaledResolution sr = new ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
        int screenCenterX = sr.getScaledWidth() / 2;
        int screenCenterY = sr.getScaledHeight() / 2;

        // convert screen center into coordinates relative to the GUI origin (what foreground expects)
        float posX = (float)screenCenterX - (float)this.guiLeft;
        float posY = (float)screenCenterY - (float)this.guiTop;

        float scale = DEFAULT_PREVIEW_SCALE * previewZoom;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
        GL11.glPushMatrix();
        float prevViewY = RenderManager.instance.playerViewY;
        try {
            // Viewer rotations precede the legacy model transform, keeping the model origin at the existing pivot.
            GL11.glTranslatef(posX, posY, 120.0F);
            GL11.glRotatef(previewPitch, 1.0F, 0.0F, 0.0F);
            GL11.glRotatef(previewYaw, 0.0F, 1.0F, 0.0F);
            GL11.glScalef(-scale, scale, scale);
            GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
            GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
            GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);

            RenderHelper.enableStandardItemLighting();
            GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);
            RenderManager.instance.playerViewY = 180.0F;

            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_CULL_FACE);

            ItemStack currentItem = ((HMGContainerInventoryItem)inventorySlots).inventory.currentItem;
            if (currentItem != null)
            {
                // The renderer needs live attachment NBT, but the preview must never modify the held firearm.
                ItemStack previewItem = currentItem.copy();
                if (previewItem.getTagCompound() == null)
                    previewItem.setTagCompound(new NBTTagCompound());

                NBTTagList tagList = new NBTTagList();
                for (int i = 0; i < ((HMGContainerInventoryItem)inventorySlots).inventory.items.length; i++)
                {
                    ItemStack stack = ((HMGContainerInventoryItem)inventorySlots).inventory.items[i];
                    if (stack != null)
                    {
                        NBTTagCompound compound = new NBTTagCompound();
                        compound.setByte("Slot", (byte)i);
                        stack.writeToNBT(compound);
                        tagList.appendTag(compound);
                    }
                }
                previewItem.getTagCompound().setTag("Items", tagList);

                GL11.glEnable(GL12.GL_RESCALE_NORMAL);

                IItemRenderer gunrender = MinecraftForgeClient.getItemRenderer(
                        previewItem, IItemRenderer.ItemRenderType.EQUIPPED);

                if (gunrender instanceof HMGRenderItemGun_U_NEW ||
                        gunrender instanceof HMGRenderItemGun_U)
                {
                    if (gunrender instanceof HMGRenderItemGun_U_NEW
                            && ((HMGRenderItemGun_U_NEW)gunrender).partsRender_gun.animationDefinition != null) {
                        try (handmadeguns.client.animation.AnimationClient.Scope scope =
                                handmadeguns.client.animation.AnimationClient.beginPreview(
                                        ((HMGRenderItemGun_U_NEW)gunrender).partsRender_gun, currentItem, previewItem, this)) {
                            gunrender.renderItem(IItemRenderer.ItemRenderType.ENTITY, previewItem);
                        }
                    } else {
                        gunrender.renderItem(IItemRenderer.ItemRenderType.ENTITY, previewItem);
                    }
                }

                GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            }
        } finally {
            RenderManager.instance.playerViewY = prevViewY;
            RenderHelper.disableStandardItemLighting();
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        }
    }

    private boolean isInsidePreview(int mouseX, int mouseY)
    {
        int localX = mouseX - this.guiLeft;
        int localY = mouseY - this.guiTop;
        return localX >= PREVIEW_LEFT && localX < PREVIEW_RIGHT
                && localY >= PREVIEW_TOP && localY < PREVIEW_BOTTOM;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (mouseButton == 0 && isInsidePreview(mouseX, mouseY))
        {
            rotatingPreview = true;
            previousMouseX = mouseX;
            previousMouseY = mouseY;
            // Do not pass a preview press to GuiContainer, where it could begin an item drag.
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick)
    {
        if (rotatingPreview && mouseButton == 0)
        {
            int deltaX = mouseX - previousMouseX;
            int deltaY = mouseY - previousMouseY;
            previewYaw = wrapAngleTo180_float(previewYaw + deltaX * HORIZONTAL_DRAG_SENSITIVITY);
            previewPitch = Math.max(MIN_PREVIEW_PITCH, Math.min(MAX_PREVIEW_PITCH,
                    previewPitch + deltaY * VERTICAL_DRAG_SENSITIVITY));
            previousMouseX = mouseX;
            previousMouseY = mouseY;
            return;
        }
        super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int mouseButton)
    {
        if (mouseButton == 0 && rotatingPreview)
        {
            rotatingPreview = false;
            return;
        }
        super.mouseMovedOrUp(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput()
    {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0)
            return;

        // LWJGL reports display pixels; convert them to the same scaled coordinates as GuiContainer.
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        if (isInsidePreview(mouseX, mouseY))
        {
            float zoomDelta = wheel > 0 ? ZOOM_STEP : -ZOOM_STEP;
            previewZoom = Math.max(MIN_PREVIEW_ZOOM, Math.min(MAX_PREVIEW_ZOOM,
                    previewZoom + zoomDelta));
        }
    }



    /*
        背景の描画
     */
    @Override
    protected void drawGuiContainerBackgroundLayer(float p_146976_1_, int p_146976_2_, int p_146976_3_)
    {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(texture);
        int k = (this.width - this.xSize) / 2;
        int l = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(k, l, 0, 0, this.xSize, this.ySize);
    }
}
