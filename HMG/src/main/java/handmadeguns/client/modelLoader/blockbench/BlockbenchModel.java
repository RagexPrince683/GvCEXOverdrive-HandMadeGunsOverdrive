package handmadeguns.client.modelLoader.blockbench;

import handmadeguns.animation.AnimationPose;
import handmadeguns.client.modelLoader.obj_modelloaderMod.obj.*;
import handmadeguns.client.render.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import java.util.*;
import java.util.concurrent.ExecutorService;

/** Uses HMG's existing mesh batches, VBO/display-list fallback and part traversal. No playback state. */
public final class BlockbenchModel implements IModelCustom_HMG {
    private static final ResourceLocation DEFERRED_TEXTURE = new ResourceLocation("missingno");
    private static final HMGGroupObject NO_GEOMETRY = new HMGGroupObject() {
        @Override public void render() { }
    };
    public final BlockbenchProject project;
    public final ArrayList<HMGGunParts> parts = new ArrayList<HMGGunParts>();
    private final Map<String, Part> byId = new LinkedHashMap<String, Part>();
    private final Map<String, List<Part>> byName = new LinkedHashMap<String, List<Part>>();
    private final ResourceLocation[] textures;
    private HMGGroupObject current = NO_GEOMETRY;
    private static final net.minecraft.client.model.ModelBiped HANDS = new net.minecraft.client.model.ModelBiped(0);

    public static final class Part extends HMGGunParts {
        public final float[] localOrigin, restRotation;
        public final boolean visible;
        public final boolean bedrock;
        Part(BlockbenchProject.Node node, boolean bedrock) {
            super(node.name);
            this.bedrock = bedrock;
            animationId = node.uuid;
            localOrigin = BlockbenchTransform.point(node.origin, node.parent == null ? new double[3] : node.parent.origin);
            restRotation = BlockbenchTransform.rotation(node.rotation);
            visible = node.visible;
            rendering_Def = rendering_Ads = rendering_Recoil = rendering_Cock = rendering_Reload = true;
            AddRenderinfDef(0,0,0,0,0,0); AddRenderinfADS(0,0,0,0,0,0);
            AddRenderinfRecoil(0,0,0,0,0,0); AddRenderinfCock(0,0,0,0,0,0); AddRenderinfReload(0,0,0,0,0,0);
            currentGroup_reticlePlate = currentGroup_reticle = currentGroup_light = NO_GEOMETRY;
            initialized = true;
        }
    }
    public BlockbenchModel(BlockbenchProject project) {
        this.project = project;
        textures = new ResourceLocation[project.textures.size()];
        for (BlockbenchProject.Node node : project.roots) parts.add(part(node, null));
    }
    private Part part(BlockbenchProject.Node node, Part parent) {
        Part part = new Part(node, project.bedrock); part.mother = parent;
        part.partsID = byId.size(); byId.put(node.uuid,part);
        List<Part> names = byName.get(node.name);
        if (names == null) { names = new ArrayList<Part>(); byName.put(node.name,names); }
        names.add(part);
        part.currentGroup_parts = new Mesh(node);
        for (BlockbenchProject.Node child : node.children) part.childs.add(part(child,part));
        return part;
    }
    private ResourceLocation texture(int index) {
        if (textures[index] == null) textures[index] = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(
                "hmg_bbmodel", new DynamicTexture(project.textures.get(index).image));
        return textures[index];
    }
    /** HMG parses guns during FML pre-init, before Minecraft constructs its TextureManager. */
    public ResourceLocation texture() {
        if (textures.length == 0 || Minecraft.getMinecraft().getTextureManager() == null) return DEFERRED_TEXTURE;
        return texture(0);
    }
    public void release() {
        for (Part part : byId.values()) ((Mesh)part.currentGroup_parts).releaseVbo();
        TextureManager manager = Minecraft.getMinecraft().getTextureManager();
        if (manager != null) for (ResourceLocation texture : textures) if (texture != null) manager.deleteTexture(texture);
        Arrays.fill(textures,null);
    }
    private final class Mesh extends HMGGroupObject {
        final Map<Integer,HMGGroupObject> batches = new LinkedHashMap<Integer,HMGGroupObject>();
        final boolean handPlaceholder;
        Mesh(BlockbenchProject.Node node) {
            super(node.uuid);
            handPlaceholder = "lefthand_pos".equals(node.name) || "righthand_pos".equals(node.name);
            for (BlockbenchProject.Face face : node.faces) {
                HMGGroupObject batch = batches.get(face.texture);
                if (batch == null) {
                    batch = new HMGGroupObject(node.uuid + "/" + face.texture, GL11.GL_QUADS);
                    batch.setModelKey(project.file.toString()); batches.put(face.texture,batch);
                }
                HMGFace polygon = new HMGFace();
                polygon.vertices = new HMGVertex[4]; polygon.HMGTextureCoordinates = new HMGTextureCoordinate[4];
                for (int i=0;i<4;i++) {
                    // Project faces are a screen-space clockwise UV ring; GL needs outward winding.
                    int corner = (4-i)%4;
                    float[] p = face.vertices[corner], uv = face.uv[corner];
                    polygon.vertices[i] = new HMGVertex(p[0],p[1],p[2]);
                    polygon.HMGTextureCoordinates[i] = new HMGTextureCoordinate(uv[0],uv[1]);
                }
                polygon.faceNormal = polygon.calculateFaceNormal();
                batch.faces.add(polygon);
            }
        }
        @Override public void render() { renderTextured(null); }
        public void renderTextured(ResourceLocation override) {
            if (handPlaceholder || batches.isEmpty()) return;
            GL11.glPushAttrib(GL11.GL_TEXTURE_BIT | GL11.GL_ENABLE_BIT);
            try {
                GL11.glEnable(GL11.GL_NORMALIZE);
                for (Map.Entry<Integer,HMGGroupObject> entry : batches.entrySet()) {
                    Minecraft.getMinecraft().getTextureManager().bindTexture(override == null ? texture(entry.getKey()) : override);
                    entry.getValue().render();
                }
            } finally { GL11.glPopAttrib(); }
        }
        @Override public void releaseVbo() { for (HMGGroupObject batch : batches.values()) batch.releaseVbo(); }
    }
    public void renderGeometry(HMGGunParts part, float units, ResourceLocation texture) {
        GL11.glPushMatrix();
        try {
            GL11.glScalef(units,units,units);
            ((Mesh)part.currentGroup_parts).renderTextured(texture);
        } finally { GL11.glPopMatrix(); }
    }
    public void renderHand(HMGGunParts part, float units) {
        boolean left = "lefthand_pos".equals(part.partsname);
        if (!left && !"righthand_pos".equals(part.partsname)) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        GL11.glPushMatrix(); GL11.glPushAttrib(GL11.GL_TEXTURE_BIT);
        try {
            float scale = units * BlockbenchTransform.modelNormalization();
            GL11.glScalef(scale,scale,scale);
            BlockbenchTransform.playerArmFrame();
            mc.getTextureManager().bindTexture(mc.thePlayer.getLocationSkin());
            (left ? HANDS.bipedLeftArm : HANDS.bipedRightArm).render(1.0f/16);
        } finally { GL11.glPopAttrib(); GL11.glPopMatrix(); }
    }
    public boolean hasHandLocator(boolean left) {
        List<Part> candidates = byName.get(left ? "lefthand_pos" : "righthand_pos");
        if (candidates == null) return false;
        for (Part candidate : candidates) {
            Part current = candidate;
            while (current != null && current.visible) current = (Part)current.mother;
            if (current == null) return true;
        }
        return false;
    }
    /** TaCZ first-person placement is authored by inverse camera/view nodes. */
    public void applyFirstPersonPosition(float ads, float units) {
        List<Part> idle = path("idle_view");
        if (idle == null) idle = path("camera");
        List<Part> aiming = path("iron_view");
        BlockbenchTransform.applyPositioning(idle, aiming, ads, units);
    }
    /** TaCZ third-person item placement aligns this authored node with the hand origin. */
    public void applyThirdPersonPosition(float units) {
        BlockbenchTransform.applyPositioning(path("thirdperson_hand"), null, 0, units);
    }
    private List<Part> path(String name) {
        List<Part> candidates = byName.get(name);
        if (candidates == null || candidates.isEmpty()) return null;
        LinkedList<Part> path = new LinkedList<Part>();
        Part part = candidates.get(0);
        while (part != null) { path.addFirst(part); part = (Part)part.mother; }
        return path;
    }
    @Override public String getType() { return "bbmodel"; }
    @Override public boolean isReady() { return true; }
    @Override public ExecutorService getLoadThread() { return null; }
    @Override public HMGGroupObject renderPart_getInstance() { return current; }
    @Override public void renderPart(String name) {
        Part id = byId.get(name);
        current = id == null ? NO_GEOMETRY : id.currentGroup_parts;
        if (id != null) current.render();
        else if (byName.containsKey(name)) for (Part part : byName.get(name)) { current = part.currentGroup_parts; current.render(); }
    }
    @Override public void renderOnly(String... names) { for (String name : names) renderPart(name); }
    @Override public void renderAll() { for (HMGGunParts part : parts) renderTree((Part)part, Collections.<String>emptySet()); }
    @Override public void renderAllExcept(String... names) {
        Set<String> excluded = new HashSet<String>(Arrays.asList(names));
        for (HMGGunParts part : parts) renderTree((Part)part,excluded);
    }
    private void renderTree(Part part, Set<String> excluded) {
        if (!part.visible || excluded.contains(part.partsname) || excluded.contains(part.animationId)) return;
        GL11.glPushMatrix();
        try {
            BlockbenchTransform.apply(part.localOrigin,part.restRotation,AnimationPose.Transform.IDENTITY,1);
            part.currentGroup_parts.render();
            for (HMGGunParts child : part.childs) renderTree((Part)child,excluded);
        } finally { GL11.glPopMatrix(); }
    }
}
