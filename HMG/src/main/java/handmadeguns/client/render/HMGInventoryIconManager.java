package handmadeguns.client.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import handmadeguns.items.GunInfo;
import handmadeguns.items.guns.HMGItem_Unified_Guns;

/** Model capture produces persistent pixels; ready inventory icons never draw geometry. */
public final class HMGInventoryIconManager implements IResourceManagerReloadListener {

    public static final int ICON_CACHE_VERSION = 4;
    private static final int SIZE = 128;
    private static final int CAPTURE_SIZE = 512;
    private static final int TARGET_LONG_AXIS = Math.round(SIZE * 0.80F);
    private static final int CAPTURE_EDGE_GUARD = 4;
    private static final int MAX_CAPTURE_ATTEMPTS = 4;
    private static final float INITIAL_CAPTURE_SPAN = 32.0F;
    private static final long INTERVAL_MS = 350L;
    private static final Map<Object, Entry> ENTRIES = new IdentityHashMap<Object, Entry>();
    private static final LinkedHashMap<Object, Entry> QUEUE = new LinkedHashMap<Object, Entry>();
    private static final java.util.concurrent.atomic.AtomicLong writes = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong writeFailures = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong writeNanos = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong corruptFiles = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.ThreadPoolExecutor WRITER = new java.util.concurrent.ThreadPoolExecutor(
            0, 1, 5L, java.util.concurrent.TimeUnit.SECONDS,
            new java.util.concurrent.ArrayBlockingQueue<Runnable>(32), new java.util.concurrent.ThreadFactory() {
        public Thread newThread(Runnable job) {
            Thread thread = new Thread(job, "hmg icon PNG writer");
            // A submitted PNG is allowed to finish even if ordinary client threads stop.
            // This worker exits after five idle seconds and is also bounded by the shutdown hook.
            thread.setDaemon(false);
            return thread;
        }
    }, new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
    private static Framebuffer framebuffer;
    private static final ByteBuffer PIXELS = BufferUtils.createByteBuffer(CAPTURE_SIZE * CAPTURE_SIZE * 4);
    private static long nextWork, lastReport, requests, duplicates, prebaked, hits, misses, captures, failures, loads;
    private static long captureNanos, loadNanos;
    private static int writerHighWater;
    private static java.util.Iterator bakeItems;
    private static boolean bakeStarted;
    private static boolean capturing;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                WRITER.shutdown();
                try { WRITER.awaitTermination(10L, java.util.concurrent.TimeUnit.SECONDS); }
                catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }
        }, "hmg icon cache shutdown"));
    }

    public static boolean isCapturing() { return capturing; }

    private static Entry request(Object identity, ItemStack stack, IItemRenderer renderer) {
        Entry entry = ENTRIES.get(identity);
        if(entry != null && entry.appearance != appearance(identity)) {
            release(entry);
            ENTRIES.remove(identity);
            QUEUE.remove(identity);
            entry = null;
        }
        if(entry != null) {
            if(entry.state == State.QUEUED || entry.state == State.GENERATING) ++duplicates;
            return entry;
        }
        entry = new Entry(identity, new ItemStack(stack.getItem(), 1, 0), renderer);
        ENTRIES.put(identity, entry);
        QUEUE.put(identity, entry);
        ++requests;
        return entry;
    }

    /** One queued resolution/capture per interval; never called from an item callback. */
    public static void onRenderFrame() {
        long now = Minecraft.getSystemTime();
        report(now);
        if(now < nextWork) return;
        nextWork = now + INTERVAL_MS;
        if(Boolean.getBoolean("hmg.bakeIcons")) {
            if(!bakeStarted) { bakeItems = Item.itemRegistry.iterator(); bakeStarted = true; }
            // Bounded registry walk, using precisely the normal request/generation path.
            for(int i = 0; i < 16 && bakeItems.hasNext(); ++i) bake((Item)bakeItems.next());
        }
        if(QUEUE.isEmpty()) return;
        Entry entry = QUEUE.values().iterator().next();
        QUEUE.remove(entry.identity);
        entry.state = State.GENERATING;
        try {
            refreshChangedSources(entry);
            entry.key = cacheKey(entry);
            long started = System.nanoTime();
            BufferedImage image = loadPrebaked(entry.key);
            boolean shipped = image != null;
            if(shipped) ++prebaked;
            File file = new File(directory(), entry.key + ".png");
            if(image == null) {
                image = read(file);
                if(image != null) ++hits;
                else ++misses;
            }
            loadNanos += System.nanoTime() - started;
            if(image != null) {
                ++loads;
                upload(entry, image);
                cacheEvent(entry, shipped ? "prebaked=HIT state=READY" : "disk=HIT state=READY");
                if(Boolean.getBoolean("hmg.bakeIcons")) save(entry, image, exportFile(entry.key));
                return;
            }
            cacheEvent(entry, "disk=MISS capture=QUEUED");
            started = System.nanoTime();
            try {
                prepare(entry);
                image = capture(entry);
                ++captures;
            } finally {
                captureNanos += System.nanoTime() - started;
            }
            // Upload before submitting disk work: persistence never gates this session's icon.
            upload(entry, image);
            save(entry, image, file);
            if(Boolean.getBoolean("hmg.bakeIcons")) save(entry, image, exportFile(entry.key));
        } catch(Exception failure) {
            entry.state = State.FAILED;
            ++failures;
            System.err.println("[hmg icons] Failed " + entry.stack.getUnlocalizedName() + ": " + failure);
        }
    }

    private static BufferedImage loadPrebaked(String key) {
        try (java.io.InputStream stream = Minecraft.getMinecraft().getResourceManager()
                .getResource(new ResourceLocation("handmadeguns", "textures/icons/" + key + ".png")).getInputStream()) {
            return valid(ImageIO.read(stream));
        } catch(IOException unavailable) { return null; }
    }

    private static BufferedImage read(File file) {
        if(!file.isFile()) return null;
        BufferedImage image = null;
        try { image = valid(ImageIO.read(file)); }
        catch(IOException corrupt) {}
        if(image == null) {
            corruptFiles.incrementAndGet();
            try { Files.deleteIfExists(file.toPath()); } catch(IOException ignored) {}
        }
        return image;
    }

    private static BufferedImage valid(BufferedImage image) {
        return image != null && image.getWidth() == SIZE && image.getHeight() == SIZE ? image : null;
    }

    private static void upload(Entry entry, BufferedImage image) {
        entry.texture = Minecraft.getMinecraft().getTextureManager()
                .getDynamicTextureLocation("hmg_inventory_icon", new DynamicTexture(image));
        entry.state = State.READY;
    }

    private static void save(final Entry entry, final BufferedImage image, final File destination) {
        try {
            WRITER.execute(new Runnable() {
                public void run() {
                    long started = System.nanoTime();
                    File temporary = null;
                    try {
                        File parent = destination.getParentFile();
                        if(!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create " + parent);
                        temporary = new File(destination.getPath() + ".tmp");
                        if(!ImageIO.write(image, "png", temporary)) throw new IOException("PNG encoder unavailable");
                        if(!temporary.isFile() || temporary.length() <= 0L)
                            throw new IOException("PNG encoder produced no data");
                        try {
                            Files.move(temporary.toPath(), destination.toPath(),
                                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                        } catch(IOException atomicFailure) {
                            try {
                                Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            } catch(IOException fallbackFailure) {
                                fallbackFailure.addSuppressed(atomicFailure);
                                throw fallbackFailure;
                            }
                        }
                        if(!destination.isFile() || destination.length() <= 0L)
                            throw new IOException("Final PNG is missing or empty");
                        writes.incrementAndGet();
                        cacheEvent(entry, "write=OK path=" + destination.getAbsolutePath());
                    } catch(Exception failure) {
                        writeFailures.incrementAndGet();
                        cacheEvent(entry, "write=FAILED path=" + destination.getAbsolutePath()
                                + " reason=" + failure.getClass().getSimpleName() + ":" + failure.getMessage());
                    } finally {
                        writeNanos.addAndGet(System.nanoTime() - started);
                        if(temporary != null) temporary.delete();
                    }
                }
            });
            writerHighWater = Math.max(writerHighWater, WRITER.getQueue().size());
        } catch(java.util.concurrent.RejectedExecutionException full) {
            // Keep READY. A full writer must never block the render thread.
            writeFailures.incrementAndGet();
            cacheEvent(entry, "write=FAILED path=" + destination.getAbsolutePath() + " reason=writer-queue-full");
        }
    }

    public static void resetForReload() {
        for(Entry entry : ENTRIES.values()) release(entry);
        ENTRIES.clear();
        QUEUE.clear();
        // Completed images are immutable and fingerprinted, so queued disk writes remain valid.
        if(framebuffer != null) {
            int previous = GL11.glGetInteger(0x8CA6);
            framebuffer.deleteFramebuffer();
            OpenGlHelper.func_153171_g(0x8D40, previous);
            framebuffer = null;
        }
        nextWork = 0L;
        bakeStarted = false;
    }

    private static void release(Entry entry) {
        if(entry != null && entry.texture != null)
            Minecraft.getMinecraft().getTextureManager().deleteTexture(entry.texture);
    }

    private static void draw(Entry entry) {
        if(entry == null || entry.state != State.READY) return;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        try {
            Minecraft.getMinecraft().getTextureManager().bindTexture(entry.texture);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.01F);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1, 1, 1, 1);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            t.addVertexWithUV(0, 16, 0, 0, 1);
            t.addVertexWithUV(16, 16, 0, 1, 1);
            t.addVertexWithUV(16, 0, 0, 1, 0);
            t.addVertexWithUV(0, 0, 0, 0, 0);
            t.draw();
        } finally { GL11.glPopAttrib(); }
    }

    private static BufferedImage capture(Entry entry) throws IOException {
        if(!OpenGlHelper.isFramebufferEnabled()) throw new IOException("Framebuffer capture unavailable/disabled");
        int previousFramebuffer = GL11.glGetInteger(0x8CA6);
        int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        int previousActiveTexture = GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE);
        float previousLightX = OpenGlHelper.lastBrightnessX, previousLightY = OpenGlHelper.lastBrightnessY;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushClientAttrib(-1 /* GL_CLIENT_ALL_ATTRIB_BITS */);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            if(framebuffer == null) framebuffer = new Framebuffer(CAPTURE_SIZE, CAPTURE_SIZE, true);
            framebuffer.bindFramebuffer(true);
            framebuffer.checkFramebufferComplete();
            GL11.glViewport(0, 0, CAPTURE_SIZE, CAPTURE_SIZE);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
            float captureSpan = INITIAL_CAPTURE_SPAN;
            Bounds bounds = null;
            for(int attempt = 0; attempt < MAX_CAPTURE_ATTEMPTS; ++attempt) {
                BufferedImage image = renderCapture(entry, captureSpan);
                bounds = findBounds(image);
                if(bounds != null && !bounds.touchesEdge(CAPTURE_SIZE, CAPTURE_EDGE_GUARD))
                    return normalize(image, bounds);
                captureSpan *= 2.0F;
            }
            if(bounds == null) throw new IOException("Empty model capture");
            throw new IOException("Model capture still reaches the framebuffer edge after adaptive framing");
        } finally {
            capturing = false;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, previousLightX, previousLightY);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            GL11.glPopMatrix();
            GL11.glPopClientAttrib();
            GL11.glPopAttrib();
            OpenGlHelper.func_153171_g(0x8D40, previousFramebuffer);
            OpenGlHelper.setActiveTexture(previousActiveTexture);
            GL11.glMatrixMode(previousMatrixMode);
        }
    }

    private static BufferedImage renderCapture(Entry entry, float captureSpan) {
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClearDepth(1);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        projection(captureSpan);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.01F);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_NORMALIZE);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glColor4f(1, 1, 1, 1);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
        net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();
        capturing = true;
        renderCanonical(entry);
        capturing = false;
        PIXELS.clear();
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glReadPixels(0, 0, CAPTURE_SIZE, CAPTURE_SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, PIXELS);
        // The framebuffer blend stores premultiplied RGB. Populate an ARGB_PRE raster directly so
        // Java2D does not multiply translucent edge colors a second time during normalization.
        BufferedImage image = new BufferedImage(CAPTURE_SIZE, CAPTURE_SIZE, BufferedImage.TYPE_INT_ARGB_PRE);
        int[] imagePixels = ((java.awt.image.DataBufferInt)image.getRaster().getDataBuffer()).getData();
        for(int y = 0; y < CAPTURE_SIZE; ++y) for(int x = 0; x < CAPTURE_SIZE; ++x) {
            int p = (x + y * CAPTURE_SIZE) * 4;
            imagePixels[x + (CAPTURE_SIZE - y - 1) * CAPTURE_SIZE] = (PIXELS.get(p + 3) & 255) << 24
                    | (PIXELS.get(p) & 255) << 16 | (PIXELS.get(p + 1) & 255) << 8 | PIXELS.get(p + 2) & 255;
        }
        return image;
    }

    private static Bounds findBounds(BufferedImage source) {
        int minX = source.getWidth(), minY = source.getHeight(), maxX = -1, maxY = -1;
        for(int y = 0; y < source.getHeight(); ++y) for(int x = 0; x < source.getWidth(); ++x) {
            if((source.getRGB(x, y) >>> 24) != 0) {
                minX = Math.min(minX, x); minY = Math.min(minY, y);
                maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
            }
        }
        return maxX < minX ? null : new Bounds(minX, minY, maxX, maxY);
    }

    private static BufferedImage normalize(BufferedImage source, Bounds bounds) {
        int sourceWidth = bounds.maxX - bounds.minX + 1;
        int sourceHeight = bounds.maxY - bounds.minY + 1;
        double scale = TARGET_LONG_AXIS / (double)Math.max(sourceWidth, sourceHeight);
        int targetWidth = Math.max(1, (int)Math.round(sourceWidth * scale));
        int targetHeight = Math.max(1, (int)Math.round(sourceHeight * scale));
        int targetX = (SIZE - targetWidth) / 2;
        int targetY = (SIZE - targetHeight) / 2;

        // Filtering premultiplied colors prevents transparent texels from contributing dark fringes.
        BufferedImage cropped = new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_INT_ARGB_PRE);
        java.awt.Graphics2D crop = cropped.createGraphics();
        try {
            crop.setComposite(java.awt.AlphaComposite.Src);
            crop.drawImage(source, 0, 0, sourceWidth, sourceHeight,
                    bounds.minX, bounds.minY, bounds.maxX + 1, bounds.maxY + 1, null);
        } finally { crop.dispose(); }

        BufferedImage output = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB_PRE);
        java.awt.Graphics2D g = output.createGraphics();
        try {
            g.setComposite(java.awt.AlphaComposite.Src);
            g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(java.awt.RenderingHints.KEY_ALPHA_INTERPOLATION, java.awt.RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(cropped, targetX, targetY, targetX + targetWidth, targetY + targetHeight,
                    0, 0, sourceWidth, sourceHeight, null);
        } finally { g.dispose(); }
        return output;
    }

    private static final class Bounds {
        final int minX, minY, maxX, maxY;
        Bounds(int minX, int minY, int maxX, int maxY) {
            this.minX = minX; this.minY = minY; this.maxX = maxX; this.maxY = maxY;
        }
        boolean touchesEdge(int size, int guard) {
            return minX < guard || minY < guard || maxX >= size - guard || maxY >= size - guard;
        }
    }

    private static void resourceHash(MessageDigest digest, ResourceLocation location) throws IOException {
        bytes(digest, location.toString());
        try(java.io.InputStream in = Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream()) {
            streamHash(digest, in);
        }
    }

    private static void streamHash(MessageDigest digest, java.io.InputStream in) throws IOException {
        byte[] buffer = new byte[16384];
        int read;
        while((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
    }

    private static void bytes(MessageDigest digest, String value) throws IOException {
        digest.update(value.getBytes("UTF-8"));
        digest.update((byte)0);
    }

    private static String hex(byte[] value) {
        StringBuilder result = new StringBuilder();
        for(byte b : value) result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return result.toString();
    }

    private static File directory() { return new File(Minecraft.getMinecraft().mcDataDir, "cache/hmg/icons"); }
    private static File exportFile(String key) {
        return new File(directory(), "export/assets/handmadeguns/textures/icons/" + key + ".png");
    }

    private static void report(long now) {
        if(!diagnostics() || now - lastReport < 10000L) return;
        lastReport = now;
        System.out.println(String.format(java.util.Locale.ROOT,
                "[HMG IconCache] prebaked=%d diskHits=%d diskMisses=%d corrupt=%d requests=%d deduplicated=%d captures=%d failures=%d loads=%d writes=%d writeFailures=%d queue=%d writerQueue=%d/%d avgCaptureMs=%.2f totalCaptureMs=%.2f avgResolveLoadMs=%.2f avgWriteMs=%.2f",
                prebaked, hits, misses, corruptFiles.get(), requests, duplicates, captures, failures, loads,
                writes.get(), writeFailures.get(), QUEUE.size(), WRITER.getQueue().size(), writerHighWater,
                captureNanos / 1e6 / Math.max(1, captures + failures), captureNanos / 1e6,
                loadNanos / 1e6 / Math.max(1, hits + misses + prebaked),
                writeNanos.get() / 1e6 / Math.max(1, writes.get() + writeFailures.get())));
    }


    private static long appearance(Object identity) {
        GunInfo info = (GunInfo)identity;
        long value = Float.floatToIntBits(info.modelscale);
        value = value * 31 + Float.floatToIntBits(info.inventoryscale);
        value = value * 31 + Float.floatToIntBits(info.inworldScale);
        value = value * 31 + Float.floatToIntBits(info.inventoryOffsetX);
        value = value * 31 + Float.floatToIntBits(info.inventoryOffsetY);
        return value * 31 + Float.floatToIntBits(info.inventoryOffsetZ);
    }

    private enum State { QUEUED, GENERATING, READY, FAILED }
    private static final class Entry {
        Object identity;
        final ItemStack stack;
        final long appearance;
        final String contentId;
        IItemRenderer renderer;
        String key;
        String sourceFingerprint;
        ResourceLocation texture;
        State state = State.QUEUED;
        Entry(Object identity, ItemStack stack, IItemRenderer renderer) {
            this.identity = identity; this.stack = stack; this.renderer = renderer;
            this.appearance = appearance(identity);
            this.contentId = String.valueOf(Item.itemRegistry.getNameForObject(stack.getItem()));
        }
    }

    private static final HMGInventoryIconManager INSTANCE = new HMGInventoryIconManager();
    public static HMGInventoryIconManager instance() { return INSTANCE; }
    public void onResourceManagerReload(IResourceManager manager) { resetForReload(); }
    public static boolean claimCachedIcon(ItemStack stack, IItemRenderer renderer) {
        if(capturing) return true;
        if(stack == null || !(stack.getItem() instanceof HMGItem_Unified_Guns)) return false;
        GunInfo info = ((HMGItem_Unified_Guns)stack.getItem()).gunInfo;
        return info != null && info.useModelAsIcon && request(info, stack, renderer).state == State.READY;
    }
    public static boolean renderCachedIcon(ItemStack stack) {
        if(stack == null || !(stack.getItem() instanceof HMGItem_Unified_Guns)) return false;
        Entry entry = ENTRIES.get(((HMGItem_Unified_Guns)stack.getItem()).gunInfo);
        draw(entry);
        return entry != null && entry.state == State.READY;
    }
    private static void bake(Item item) {
        if(item instanceof HMGItem_Unified_Guns)
            net.minecraftforge.client.MinecraftForgeClient.getItemRenderer(new ItemStack(item), IItemRenderer.ItemRenderType.INVENTORY);
    }
    private static boolean diagnostics() { return handmadeguns.HandmadeGunsCore.debugGunIconCache; }
    private static void cacheEvent(Entry entry, String message) {
        if(!diagnostics()) return;
        String key = entry != null && entry.key != null ? entry.key : "pending";
        String id = entry != null ? entry.contentId : "unknown";
        System.out.println("[HMG IconCache] " + id + " key=" + key + " " + message);
    }
    private static void projection(float span) {
        float half = span * 0.5F;
        GL11.glOrtho(8.0F - half, 8.0F + half, 8.0F + half, 8.0F - half, -1000, 1000);
    }
    private static void prepare(Entry entry) { ((HMGItem_Unified_Guns)entry.stack.getItem()).checkTags(entry.stack); }
    private static int capturePass;
    public static int capturePass() { return capturePass; }
    private static void renderCanonical(Entry entry) {
        boolean under = HMGRenderItemGun_U_NEW.isUnder, placed = HMGRenderItemGun_U_NEW.isPlacedGun;
        try {
            HMGRenderItemGun_U_NEW.isUnder = false;
            HMGRenderItemGun_U_NEW.isPlacedGun = false;
            int passes = entry.renderer instanceof HMGRenderItemGun_U_NEW ? 2 : 1;
            for(capturePass = 0; capturePass < passes; ++capturePass) {
                GL11.glPushMatrix();
                try { entry.renderer.renderItem(IItemRenderer.ItemRenderType.INVENTORY, entry.stack); }
                finally { GL11.glPopMatrix(); }
            }
        } finally {
            HMGRenderItemGun_U_NEW.isUnder = under;
            HMGRenderItemGun_U_NEW.isPlacedGun = placed;
        }
    }
    private static String cacheKey(Entry entry) throws Exception {
        GunInfo info = (GunInfo)entry.identity;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String id = String.valueOf(Item.itemRegistry.getNameForObject(entry.stack.getItem()));
        bytes(digest, "hmg-canonical-renderer-3|" + ICON_CACHE_VERSION + "|" + id + "|default-unskinned-unattached-t0|"
                + info.modelscale + "|" + info.inventoryscale + "|" + info.inworldScale + "|"
                + info.inventoryOffsetX + "|" + info.inventoryOffsetY + "|" + info.inventoryOffsetZ);
        bytes(digest, entry.renderer.getClass().getName());
        bytes(digest, entry.sourceFingerprint);
        // The digest already contains the stable registry ID and source fingerprints. Keeping the
        // final component compact avoids legacy Windows path failures in deeply nested instances.
        return "v" + ICON_CACHE_VERSION + "-" + hex(digest.digest());
    }
    /** Read source content, never file timestamps. Also used when the native renderer is bound. */
    public static String sourceFingerprint(GunInfo info) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        if(info.iconSourceFiles.isEmpty()) throw new IOException("Missing gun source identity");
        for(File file : info.iconSourceFiles) {
            bytes(digest, file.getName());
            try(java.io.InputStream in = new java.io.FileInputStream(file)) { streamHash(digest, in); }
            if(file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".bbmodel")) hashExternalProjectTextures(digest, file);
        }
        for(String resource : info.iconSourceResources) resourceHash(digest, new ResourceLocation(resource));
        return hex(digest.digest());
    }
    private static void refreshChangedSources(Entry entry) throws Exception {
        GunInfo info = (GunInfo)entry.identity;
        String current = sourceFingerprint(info);
        entry.sourceFingerprint = current;
        if(info.iconLoadedFingerprint != null && !info.iconLoadedFingerprint.equals(current)) {
            // Generic resource reload deliberately retains HMG model objects. Only reparse
            // a changed gun, through its established targeted reload, before capturing it.
            if(!handmadeguns.HMGGunMaker.reloadModelsForItem(entry.stack.getItem()))
                throw new IOException("Changed gun sources require a successful model reload");
            ENTRIES.remove(entry.identity);
            entry.identity = ((HMGItem_Unified_Guns)entry.stack.getItem()).gunInfo;
            entry.sourceFingerprint = sourceFingerprint((GunInfo)entry.identity);
            entry.renderer = net.minecraftforge.client.MinecraftForgeClient.getItemRenderer(entry.stack, IItemRenderer.ItemRenderType.EQUIPPED);
            if(entry.renderer == null) throw new IOException("Missing reloaded gun renderer");
            ENTRIES.put(entry.identity, entry);
        }
    }
    private static void hashExternalProjectTextures(MessageDigest digest, File file) throws IOException {
        try(java.io.Reader reader = new java.io.InputStreamReader(new java.io.FileInputStream(file), "UTF-8")) {
            com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(reader).getAsJsonObject();
            com.google.gson.JsonArray textures = json.getAsJsonArray("textures");
            if(textures == null) return;
            for(com.google.gson.JsonElement value : textures) {
                com.google.gson.JsonObject texture = value.getAsJsonObject();
                String source = texture.has("source") ? texture.get("source").getAsString() : "";
                if(source.startsWith("data:image/png;base64,")) continue;
                String path = texture.has("relative_path") ? texture.get("relative_path").getAsString() : "";
                if(path.isEmpty()) path = texture.has("name") ? texture.get("name").getAsString() : "";
                File image = new File(file.getParentFile(), path).getCanonicalFile();
                try(java.io.InputStream in = new java.io.FileInputStream(image)) { streamHash(digest, in); }
            }
        }
    }
}
