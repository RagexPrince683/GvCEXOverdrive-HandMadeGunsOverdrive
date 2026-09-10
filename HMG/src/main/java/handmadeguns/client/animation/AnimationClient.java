package handmadeguns.client.animation;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import handmadeguns.animation.*;
import handmadeguns.client.render.*;
import handmadeguns.items.guns.HMGItem_Unified_Guns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import java.lang.ref.WeakReference;
import java.util.*;

/** Client-thread registry. Neither shared item renderers nor definitions own playback cursors. */
public final class AnimationClient {
    public static final KeyBinding INSPECT = new KeyBinding("Inspect HMG animation", Keyboard.KEY_NONE, "HandmadeGuns");
    private static final Map<ItemStack, List<Entry>> INSTANCES = new WeakHashMap<ItemStack, List<Entry>>();
    private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<Scope>();
    private static Object world;
    private static ItemStack held;
    private static long ticks;
    private static double seconds;

    public static void clearPlayback() { INSTANCES.clear(); }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (world != mc.theWorld) {
            world = mc.theWorld; clearPlayback(); ticks = 0; seconds = 0; held = null;
        }
        if (mc.theWorld != null && !mc.isGamePaused()) ticks++;
        ItemStack next = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
        if (held != next) { if (held != null) INSTANCES.remove(held); held = next; }
        if (ticks % 40 == 0) {
            for (List<Entry> entries : INSTANCES.values()) {
                Iterator<Entry> iterator = entries.iterator();
                while (iterator.hasNext()) {
                    Entry entry = iterator.next();
                    if (seconds - entry.lastSeen > 2 || (entry.hasOwner && entry.owner.get() == null)) iterator.remove();
                }
            }
        }
    }

    @SubscribeEvent public void frame(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START && world != null && !Minecraft.getMinecraft().isGamePaused())
            seconds = Math.max(seconds, (ticks + event.renderTickTime) / 20.0);
    }

    @SubscribeEvent public void key(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!INSPECT.isPressed() || mc.currentScreen != null || mc.thePlayer == null) return;
        ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack == null || !(stack.getItem() instanceof HMGItem_Unified_Guns)) return;
        IItemRenderer renderer = MinecraftForgeClient.getItemRenderer(stack, IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON);
        if (!(renderer instanceof HMGRenderItemGun_U_NEW)) return;
        AnimationDefinition definition = ((HMGRenderItemGun_U_NEW)renderer).partsRender_gun.animationDefinition;
        if (definition == null) return;
        String clip = ((HMGItem_Unified_Guns)stack.getItem()).remain_Bullet(stack) == 0
                && definition.clips.containsKey("inspect_empty") ? "inspect_empty" : "inspect";
        request(stack, mc.thePlayer, IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON, clip, AnimationController.Layer.ACTION);
    }

    /** Queues a custom presentation clip; the next root render performs the final priority/state check. */
    public static boolean request(ItemStack stack, Entity owner, IItemRenderer.ItemRenderType context,
                                  String clip, AnimationController.Layer layer) {
        IItemRenderer renderer = MinecraftForgeClient.getItemRenderer(stack, context);
        if (!(renderer instanceof HMGRenderItemGun_U_NEW)) return false;
        AnimationDefinition definition = ((HMGRenderItemGun_U_NEW)renderer).partsRender_gun.animationDefinition;
        if (definition == null || !definition.clips.containsKey(clip)) return false;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && (tag.getBoolean("IsReloading") || tag.getInteger("CockingTime") > 0)) return false;
        Entry entry = entry(stack, owner, context, 0, definition);
        // Defer until the render scope has the real legacy baseline and event recipient.
        entry.request = clip; entry.requestLayer = layer;
        return true;
    }

    public static Scope begin(PartsRender_Gun renderer, ItemStack stack, IItemRenderer.ItemRenderType type,
                              Object[] data, boolean under, boolean placed) {
        Object owner = data != null && data.length > 0 ? data[0] : null;
        if (data != null) for (Object value : data) if (value instanceof Entity) { owner = value; break; }
        // Inventory/attachment preview scopes are isolated from held/world scopes, including preview copies.
        if (type == IItemRenderer.ItemRenderType.INVENTORY) owner = data != null && data.length > 0 ? data[0] : null;
        ItemStack identity = stack;
        int flags = placed ? 2 : 0;
        Scope parent = ACTIVE.get();
        if (under && parent != null) {
            // Installed under-gun stacks are decoded from NBT each pass. Use the parent instance + slot path.
            identity = parent.identity; owner = parent.owner; type = parent.context;
            flags = (parent.entry == null ? 0 : parent.entry.flags) * 4 + 1;
        }
        Entry entry = renderer.animationDefinition == null ? null : entry(identity, owner, type, flags, renderer.animationDefinition);
        Scope scope = new Scope(parent, renderer, identity, stack, owner, type, entry);
        ACTIVE.set(scope);
        return scope;
    }

    public static boolean scoped(PartsRender_Gun renderer) { return ACTIVE.get() != null && ACTIVE.get().renderer == renderer; }

    public static boolean scopedFor(PartsRender_Gun renderer, ItemStack stack) {
        return scoped(renderer) && ACTIVE.get().stack == stack;
    }

    /** Stable preview identity while the GUI supplies a fresh, presentation-only NBT copy each frame. */
    public static Scope beginPreview(PartsRender_Gun renderer, ItemStack identity, ItemStack preview, Object gui) {
        Entry entry = renderer.animationDefinition == null ? null : entry(identity, gui,
                IItemRenderer.ItemRenderType.INVENTORY, 0, renderer.animationDefinition);
        Scope scope = new Scope(ACTIVE.get(), renderer, identity, preview, gui, IItemRenderer.ItemRenderType.INVENTORY, entry);
        ACTIVE.set(scope);
        return scope;
    }

    private static Entry entry(ItemStack stack, Object owner, IItemRenderer.ItemRenderType context, int flags,
                               AnimationDefinition definition) {
        List<Entry> entries = INSTANCES.get(stack);
        if (entries == null) { entries = new ArrayList<Entry>(); INSTANCES.put(stack, entries); }
        Iterator<Entry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (seconds - entry.lastSeen > 2) { iterator.remove(); continue; }
            if (entry.owner.get() == owner && entry.context == context && entry.flags == flags) {
                if (entry.definition != definition) { iterator.remove(); continue; }
                entry.lastSeen = seconds; return entry;
            }
        }
        Entry entry = new Entry(owner, context, flags, definition);
        entries.add(entry);
        return entry;
    }

    /** Runs only at the root part traversal, once at a given animation clock value. */
    public static void prepare(PartsRender_Gun renderer, GunState[] states, float legacyTime, int ammunition) {
        Scope scope = ACTIVE.get();
        if (scope == null || scope.renderer != renderer || scope.entry == null) return;
        Entry entry = scope.entry;
        if (entry.preparedAt == seconds) return;
        entry.preparedAt = seconds;
        AnimationPose legacy = LegacyMotionAdapter.sample(renderer.partslist, entry.definition.partNames, states, legacyTime);
        if (!entry.initialized) entry.controller.sample(legacy);
        final List<HMGAnimationEvent> markers = new ArrayList<HMGAnimationEvent>();
        AnimationPlayback.EventSink sink = new AnimationPlayback.EventSink() {
            @Override public void onEvent(AnimationClip clip, AnimationEvent event, long generation, long cycle) {
                markers.add(new HMGAnimationEvent(scope.stack, scope.owner instanceof Entity ? (Entity)scope.owner : null,
                        scope.context, entry.definition.source, clip.name, event, generation, cycle));
            }
        };
        entry.controller.advanceTo(seconds, sink);
        NBTTagCompound tag = scope.stack.getTagCompound();
        boolean reload = tag != null && tag.getBoolean("IsReloading");
        int cock = tag == null ? 0 : tag.getInteger("CockingTime");
        int bolt = tag == null ? 0 : tag.getByte("Bolt");
        boolean recoil = states[0] == GunState.Recoil;
        boolean shot = recoil && (!entry.recoil || bolt > entry.bolt || ammunition < entry.ammunition);
        if (!entry.initialized) {
            entry.play(AnimationController.Layer.BASE, "idle", false);
            if (scope.context == IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON)
                entry.play(AnimationController.Layer.ACTION, "draw", false);
        }
        if (reload && !entry.reload) {
            entry.controller.stop(AnimationController.Layer.ACTION);
            String variant = ammunition == 0 ? "reload_empty" : "reload_tactical";
            entry.reloadClip = entry.definition.clips.containsKey(variant) ? variant : "reload";
            entry.play(AnimationController.Layer.ACTION, entry.reloadClip, true);
        } else if (!reload && entry.reload) {
            if (entry.controller.active(entry.reloadClip)) entry.controller.stop(AnimationController.Layer.ACTION);
        }
        if (!reload && cock > 0 && entry.cock == 0) {
            entry.controller.stop(AnimationController.Layer.ACTION);
            entry.play(AnimationController.Layer.ACTION, entry.definition.clips.containsKey("cock") ? "cock" : "bolt", true);
        }
        if (shot && !reload) {
            String action = entry.controller.current(AnimationController.Layer.ACTION);
            if ("inspect".equals(action) || "inspect_empty".equals(action)) entry.controller.stop(AnimationController.Layer.ACTION);
            entry.play(AnimationController.Layer.ADDITIVE, "fire", true);
        }
        if (entry.request != null) {
            if (!reload && cock == 0 && !shot) entry.play(entry.requestLayer, entry.request, true);
            entry.request = null;
        }
        entry.initialized = true; entry.reload = reload; entry.cock = cock; entry.recoil = recoil;
        entry.bolt = bolt; entry.ammunition = ammunition;
        // Zero-time markers fire now, once; repeated GL passes neither advance nor re-dispatch.
        entry.controller.advanceTo(seconds, sink);
        entry.pose = entry.controller.sample(legacy);
        for (HMGAnimationEvent marker : markers) MinecraftForge.EVENT_BUS.post(marker);
    }

    public static HMGGunParts_Motion_PosAndRotation pose(PartsRender_Gun renderer, HMGGunParts part,
                                                         HMGGunParts_Motion_PosAndRotation legacy) {
        Scope scope = ACTIVE.get();
        if (scope == null || scope.renderer != renderer || scope.entry == null) return legacy;
        return LegacyMotionAdapter.apply(scope.entry.pose, part.partsname, legacy);
    }

    public static final class Scope implements AutoCloseable {
        final Scope previous;
        final PartsRender_Gun renderer;
        final ItemStack identity, stack;
        final Object owner;
        final IItemRenderer.ItemRenderType context;
        final Entry entry;
        Scope(Scope previous, PartsRender_Gun renderer, ItemStack identity, ItemStack stack, Object owner, IItemRenderer.ItemRenderType context, Entry entry) {
            this.previous = previous; this.renderer = renderer; this.identity = identity; this.stack = stack; this.owner = owner;
            this.context = context; this.entry = entry;
        }
        @Override public void close() { if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous); }
    }

    private static final class Entry {
        final WeakReference<Object> owner;
        final boolean hasOwner;
        final IItemRenderer.ItemRenderType context;
        final int flags;
        final AnimationDefinition definition;
        final AnimationController controller;
        double lastSeen = seconds, preparedAt = Double.NaN;
        AnimationPose pose = AnimationPose.EMPTY;
        boolean initialized, reload, recoil;
        int cock, bolt, ammunition;
        String reloadClip, request;
        AnimationController.Layer requestLayer;
        Entry(Object owner, IItemRenderer.ItemRenderType context, int flags, AnimationDefinition definition) {
            this.owner = new WeakReference<Object>(owner); hasOwner = owner != null;
            this.context = context; this.flags = flags; this.definition = definition;
            controller = new AnimationController(definition);
        }
        void play(AnimationController.Layer layer, String clip, boolean restart) {
            if (definition.clips.containsKey(clip)) controller.play(layer, clip, restart, 1, null);
        }
    }
}
