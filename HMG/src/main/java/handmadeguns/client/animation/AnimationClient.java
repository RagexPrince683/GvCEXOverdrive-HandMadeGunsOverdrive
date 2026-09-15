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
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
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
    private static final Map<ItemStack, Integer> LEGACY_RELOAD_EVENTS = new WeakHashMap<ItemStack, Integer>();
    private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<Scope>();
    private static Object world;
    private static ItemStack held;
    private static ItemStack heldIdentity;
    private static int heldSlot = -1;
    private static long ticks;
    private static double seconds;

    public static void clearPlayback() { INSTANCES.clear(); LEGACY_RELOAD_EVENTS.clear(); }

    /** Receives one server-authorized reload presentation event on the client thread. */
    public static boolean reloadStarted(int eventId, int slot, int itemId, boolean empty) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.inventory.currentItem != slot) return false;
        ItemStack stack = mc.thePlayer.getHeldItem();
        if (stack == null || Item.getIdFromItem(stack.getItem()) != itemId) return false;
        IItemRenderer.ItemRenderType context = IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON;
        IItemRenderer itemRenderer = MinecraftForgeClient.getItemRenderer(stack, context);
        if (!(itemRenderer instanceof HMGRenderItemGun_U_NEW)) return false;
        ReloadAnimationBridge.StartEvent event = new ReloadAnimationBridge.StartEvent(eventId, slot, itemId, empty);
        if (!ReloadAnimationBridge.matches(event, mc.thePlayer.inventory.currentItem,
                Item.getIdFromItem(stack.getItem()))) return false;
        AnimationDefinition definition = ((HMGRenderItemGun_U_NEW)itemRenderer).partsRender_gun.animationDefinition;
        if (definition == null) {
            Integer previous = LEGACY_RELOAD_EVENTS.get(stack);
            if (previous != null && previous == eventId) return false;
            LEGACY_RELOAD_EVENTS.put(stack, eventId);
            if (!(stack.getItem() instanceof HMGItem_Unified_Guns)) return false;
            HMGItem_Unified_Guns gun = (HMGItem_Unified_Guns)stack.getItem();
            gun.checkTags(stack);
            NBTTagCompound tag = stack.getTagCompound();
            // The server has already accepted this reload. Reconcile only the legacy
            // client presentation timer that the original renderer consumes.
            tag.setBoolean("IsReloading", true);
            tag.setBoolean("WaitReloading", false);
            tag.setInteger("RloadTime", 0);
            tag.setInteger("CockingTime", 0);
            return true;
        }
        Entry entry = entry(stack, mc.thePlayer, context, 0, definition);
        ReloadAnimationBridge.Request request = entry.reloadBridge.accept(
                event,
                mc.thePlayer.inventory.currentItem, Item.getIdFromItem(stack.getItem()), definition.clips.keySet());
        if (request == null) return false;
        entry.reloadRequest = request;
        entry.reloadPlaybackEndedAt = Double.NaN;
        return true;
    }

    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (world != mc.theWorld) {
            world = mc.theWorld; clearPlayback(); ticks = 0; seconds = 0; held = null;
        }
        if (mc.theWorld != null && !mc.isGamePaused()) ticks++;
        ItemStack next = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
        updateHeld(mc, next);
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
        return request(stack, owner, context, clip, layer, false);
    }

    /** Explicit retrigger for discrete actions; ordinary state requests are idempotent. */
    public static boolean request(ItemStack stack, Entity owner, IItemRenderer.ItemRenderType context,
                                  String clip, AnimationController.Layer layer, boolean restart) {
        IItemRenderer renderer = MinecraftForgeClient.getItemRenderer(stack, context);
        if (!(renderer instanceof HMGRenderItemGun_U_NEW)) return false;
        AnimationDefinition definition = ((HMGRenderItemGun_U_NEW)renderer).partsRender_gun.animationDefinition;
        if (definition == null || !definition.clips.containsKey(clip)) return false;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && (tag.getBoolean("IsReloading") || tag.getInteger("CockingTime") > 0)) return false;
        Entry entry = entry(stack, owner, context, 0, definition);
        // Defer until the render scope has the real legacy baseline and event recipient.
        entry.request = clip; entry.requestLayer = layer; entry.requestRestart = restart;
        return true;
    }

    public static Scope begin(PartsRender_Gun renderer, ItemStack stack, IItemRenderer.ItemRenderType type,
                              Object[] data, boolean under, boolean placed) {
        Object owner = data != null && data.length > 0 ? data[0] : null;
        if (data != null) for (Object value : data) if (value instanceof Entity) { owner = value; break; }
        // Inventory/attachment preview scopes are isolated from held/world scopes, including preview copies.
        if (type == IItemRenderer.ItemRenderType.INVENTORY) owner = data != null && data.length > 0 ? data[0] : null;
        ItemStack identity = stack;
        ItemStack stateStack = stack;
        int flags = placed ? 2 : 0;
        Scope parent = ACTIVE.get();
        if (under && parent != null) {
            // Installed under-gun stacks are decoded from NBT each pass. Use the parent instance + slot path.
            identity = parent.identity; owner = parent.owner; type = parent.context;
            flags = (parent.entry == null ? 0 : parent.entry.flags) * 4 + 1;
        }
        if (type == IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON && owner == Minecraft.getMinecraft().thePlayer
                && flags == 0) {
            ItemStack live = Minecraft.getMinecraft().thePlayer == null ? null : Minecraft.getMinecraft().thePlayer.getHeldItem();
            // ItemRenderer draws its cached itemToRender. Its NBT can trail the selected
            // hotbar stack even though both represent the same equipped gun, so playback
            // state must come from the live slot while retaining the stable render identity.
            if (live != null && stack != null && live.getItem() == stack.getItem()) stateStack = live;
        }
        Entry entry = renderer.animationDefinition == null ? null : entry(identity, owner, type, flags, renderer.animationDefinition);
        Scope scope = new Scope(parent, renderer, identity, stack, stateStack, owner, type, entry);
        ACTIVE.set(scope);
        return scope;
    }

    public static boolean scoped(PartsRender_Gun renderer) { return ACTIVE.get() != null && ACTIVE.get().renderer == renderer; }

    public static boolean scopedFor(PartsRender_Gun renderer, ItemStack stack) {
        return scoped(renderer) && ACTIVE.get().stack == stack;
    }

    /** One render snapshot: an accepted imported ACTION outranks asynchronously replicated gameplay state. */
    public static boolean reloadState(PartsRender_Gun renderer, boolean fallback) {
        Scope scope = ACTIVE.get();
        if (scope == null || scope.renderer != renderer || scope.entry == null) return fallback;
        validateReloadIdentity(scope);
        scope.entry.settleReloadCompletion();
        return scope.entry.reloadBridge.presentationReload(authoritativeReload(scope, fallback));
    }

    public static boolean ownsReload(PartsRender_Gun renderer) {
        Scope scope = ACTIVE.get();
        if (scope == null || scope.renderer != renderer || scope.entry == null) return false;
        validateReloadIdentity(scope);
        scope.entry.settleReloadCompletion();
        return scope.entry.reloadBridge.ownsAction();
    }

    /** Stable preview identity while the GUI supplies a fresh, presentation-only NBT copy each frame. */
    public static Scope beginPreview(PartsRender_Gun renderer, ItemStack identity, ItemStack preview, Object gui) {
        Entry entry = renderer.animationDefinition == null ? null : entry(identity, gui,
                IItemRenderer.ItemRenderType.INVENTORY, 0, renderer.animationDefinition);
        Scope scope = new Scope(ACTIVE.get(), renderer, identity, preview, preview, gui,
                IItemRenderer.ItemRenderType.INVENTORY, entry);
        ACTIVE.set(scope);
        return scope;
    }

    private static Entry entry(ItemStack stack, Object owner, IItemRenderer.ItemRenderType context, int flags,
                               AnimationDefinition definition) {
        Minecraft mc = Minecraft.getMinecraft();
        if (context == IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON && owner == mc.thePlayer && flags == 0) {
            updateHeld(mc, mc.thePlayer == null ? null : mc.thePlayer.getHeldItem());
            // Vanilla slot synchronization replaces ItemStack objects as gun NBT changes.
            // Playback belongs to this equipped slot session, not each packet's object.
            if (held != null && stack.getItem() == held.getItem()) stack = heldIdentity;
        }
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

    private static void updateHeld(Minecraft mc, ItemStack next) {
        int slot = mc.thePlayer == null ? -1 : mc.thePlayer.inventory.currentItem;
        if (slot != heldSlot || held == null || next == null || held.getItem() != next.getItem()) {
            if (heldIdentity != null) INSTANCES.remove(heldIdentity);
            heldIdentity = next;
        }
        held = next;
        heldSlot = slot;
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
        NBTTagCompound tag = scope.stateStack.getTagCompound();
        if (scope.stateStack != scope.stack && scope.stateStack.getItem() instanceof HMGItem_Unified_Guns)
            ammunition = ((HMGItem_Unified_Guns)scope.stateStack.getItem()).remain_Bullet(scope.stateStack);
        entry.settleReloadCompletion();
        boolean reload = entry.reloadBridge.presentationReload(authoritativeReload(scope, false));
        int cock = tag == null ? 0 : tag.getInteger("CockingTime");
        int bolt = tag == null ? 0 : tag.getByte("Bolt");
        boolean recoil = states[0] == GunState.Recoil;
        boolean shot = recoil && (!entry.recoil || bolt > entry.bolt || ammunition < entry.ammunition);
        if (!entry.initialized) {
            entry.play(AnimationController.Layer.BASE, "idle", false);
            if (scope.context == IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON)
                entry.play(AnimationController.Layer.ACTION, "draw", false);
        }
        if (entry.reloadRequest != null) {
            entry.controller.stop(AnimationController.Layer.ACTION);
            entry.reloadBridge.started(entry.play(entry.reloadRequest.layer, entry.reloadRequest.clip,
                    entry.reloadRequest.restart));
            entry.reloadRequest = null;
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
            if (!reload && cock == 0 && !shot) entry.play(entry.requestLayer, entry.request, entry.requestRestart);
            entry.request = null;
        }
        updateLocomotion(scope, entry, states, tag, reload, cock);
        entry.initialized = true; entry.cock = cock; entry.recoil = recoil;
        entry.bolt = bolt; entry.ammunition = ammunition;
        // Zero-time markers fire now, once; repeated GL passes neither advance nor re-dispatch.
        entry.controller.advanceTo(seconds, sink);
        entry.observeReloadCompletion();
        entry.pose = entry.controller.sample(legacy);
        for (HMGAnimationEvent marker : markers) MinecraftForge.EVENT_BUS.post(marker);
    }

    private static void updateLocomotion(Scope scope, Entry entry, GunState[] states,
                                         NBTTagCompound tag, boolean reload, int cock) {
        boolean equipped = scope.context == IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON
                || scope.context == IItemRenderer.ItemRenderType.EQUIPPED;
        EntityLivingBase entity = scope.owner instanceof EntityLivingBase ? (EntityLivingBase)scope.owner : null;
        boolean aiming = false;
        for (GunState state : states) if (state == GunState.ADS) { aiming = true; break; }
        double horizontal = entity == null ? 0 : entity.motionX * entity.motionX + entity.motionZ * entity.motionZ;
        boolean moving = entity != null && (Math.abs(entity.moveForward) > 0.01F
                || Math.abs(entity.moveStrafing) > 0.01F || horizontal > 0.0001);
        LocomotionAnimationBridge.Direction direction = LocomotionAnimationBridge.Direction.FORWARD;
        if (entity != null && entity.moveForward < -0.01F)
            direction = LocomotionAnimationBridge.Direction.BACKWARD;
        else if (entity != null && Math.abs(entity.moveStrafing) > Math.abs(entity.moveForward))
            direction = LocomotionAnimationBridge.Direction.SIDEWAY;
        boolean triggered = tag != null && tag.getBoolean("IsTriggered");
        boolean sprinting = entity != null && entity.isSprinting() && !reload && cock == 0 && !triggered;
        LocomotionAnimationBridge.Request request = entry.locomotion.update(
                new LocomotionAnimationBridge.Input(equipped && !reload && cock == 0
                        && entry.controller.current(AnimationController.Layer.ACTION) == null, moving, sprinting,
                        entity != null && entity.onGround, aiming, direction),
                entry.definition.clips.keySet(), entry.controller.current(AnimationController.Layer.MOVEMENT));
        if (request == null) return;
        if (request.stop) entry.controller.stop(AnimationController.Layer.MOVEMENT);
        else entry.controller.play(AnimationController.Layer.MOVEMENT, request.clip,
                request.restart, 1, request.loop);
    }

    private static boolean authoritativeReload(Scope scope, boolean fallback) {
        if (scope == null || scope.stateStack == null) return fallback;
        NBTTagCompound tag = scope.stateStack.getTagCompound();
        return tag == null ? fallback : tag.getBoolean("IsReloading");
    }

    private static void validateReloadIdentity(Scope scope) {
        Minecraft mc = Minecraft.getMinecraft();
        if (scope.context != IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON
                || scope.entry.flags != 0 || scope.owner != mc.thePlayer) return;
        ItemStack live = mc.thePlayer == null ? null : mc.thePlayer.getHeldItem();
        int slot = mc.thePlayer == null ? -1 : mc.thePlayer.inventory.currentItem;
        int itemId = live == null ? -1 : Item.getIdFromItem(live.getItem());
        scope.entry.invalidateReloadIdentity(slot, itemId);
    }

    public static HMGGunParts_Motion_PosAndRotation pose(PartsRender_Gun renderer, HMGGunParts part,
                                                         HMGGunParts_Motion_PosAndRotation legacy) {
        Scope scope = ACTIVE.get();
        if (scope == null || scope.renderer != renderer || scope.entry == null) return legacy;
        String key = scope.entry.pose.parts.containsKey(part.animationKey()) ? part.animationKey() : part.partsname;
        return LegacyMotionAdapter.apply(scope.entry.pose, key, legacy);
    }

    public static final class Scope implements AutoCloseable {
        final Scope previous;
        final PartsRender_Gun renderer;
        final ItemStack identity, stack, stateStack;
        final Object owner;
        final IItemRenderer.ItemRenderType context;
        final Entry entry;
        Scope(Scope previous, PartsRender_Gun renderer, ItemStack identity, ItemStack stack, ItemStack stateStack,
              Object owner, IItemRenderer.ItemRenderType context, Entry entry) {
            this.previous = previous; this.renderer = renderer; this.identity = identity; this.stack = stack;
            this.stateStack = stateStack; this.owner = owner;
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
        double lastSeen = seconds, preparedAt = Double.NaN, reloadPlaybackEndedAt = Double.NaN;
        AnimationPose pose = AnimationPose.EMPTY;
        final ReloadAnimationBridge.State reloadBridge = new ReloadAnimationBridge.State();
        final LocomotionAnimationBridge.State locomotion = new LocomotionAnimationBridge.State();
        boolean initialized, recoil, requestRestart;
        int cock, bolt, ammunition;
        ReloadAnimationBridge.Request reloadRequest;
        String request;
        AnimationController.Layer requestLayer;
        Entry(Object owner, IItemRenderer.ItemRenderType context, int flags, AnimationDefinition definition) {
            this.owner = new WeakReference<Object>(owner); hasOwner = owner != null;
            this.context = context; this.flags = flags; this.definition = definition;
            controller = new AnimationController(definition);
        }
        boolean play(AnimationController.Layer layer, String clip, boolean restart) {
            return definition.clips.containsKey(clip) && controller.play(layer, clip, restart, 1, null);
        }
        void observeReloadCompletion() {
            if (reloadBridge.ownsAction() && reloadBridge.playbackStarted()
                    && !reloadBridge.clip().equals(controller.current(AnimationController.Layer.ACTION))
                    && Double.isNaN(reloadPlaybackEndedAt)) reloadPlaybackEndedAt = seconds;
        }
        void settleReloadCompletion() {
            // Preserve one snapshot across every render pass at the clock where ACTION naturally ended.
            if (!Double.isNaN(reloadPlaybackEndedAt) && seconds > reloadPlaybackEndedAt) {
                reloadBridge.finishNaturally();
                reloadPlaybackEndedAt = Double.NaN;
            }
        }
        void invalidateReloadIdentity(int slot, int itemId) {
            String invalidated = reloadBridge.invalidateIfIdentityChanged(slot, itemId);
            if (invalidated == null) return;
            reloadRequest = null;
            reloadPlaybackEndedAt = Double.NaN;
            if (controller.active(invalidated)) controller.stop(AnimationController.Layer.ACTION);
        }
    }
}
