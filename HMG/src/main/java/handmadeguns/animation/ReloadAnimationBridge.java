package handmadeguns.animation;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.nbt.NBTTagCompound;

/** Client-independent reload presentation state shared by the packet bridge and renderer. */
public final class ReloadAnimationBridge {
    private static final AtomicInteger EVENT_IDS = new AtomicInteger();

    private ReloadAnimationBridge() { }

    public enum Stage {
        STANDARD,
        INTRO,
        INSERT,
        END;

        public static Stage fromNetwork(int ordinal) {
            return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : STANDARD;
        }
    }

    /** Select both legacy reload fields together; never copy presentation state into a cached stack. */
    public static NBTTagCompound legacyTag(NBTTagCompound cached, NBTTagCompound live,
                                           boolean localFirstPerson, boolean sameItem) {
        return localFirstPerson && sameItem && live != null ? live : cached;
    }

    /** Legacy motion keys are in ticks, including the original final-frame snap. */
    public static float legacyProgress(NBTTagCompound tag, float partialTick, int duration) {
        float progress = tag.getInteger("RloadTime") + partialTick;
        return progress + partialTick >= duration - 1 ? duration : progress;
    }

    public static final class StartEvent {
        public final int eventId;
        public final int slot;
        public final int itemId;
        public final boolean empty;
        public final Stage stage;

        public StartEvent(int eventId, int slot, int itemId, boolean empty) {
            this(eventId, slot, itemId, empty, Stage.STANDARD);
        }

        public StartEvent(int eventId, int slot, int itemId, boolean empty, Stage stage) {
            this.eventId = eventId;
            this.slot = slot;
            this.itemId = itemId;
            this.empty = empty;
            this.stage = stage == null ? Stage.STANDARD : stage;
        }
    }

    public static StartEvent acceptedEvent(boolean accepted, int eventId, int slot, int itemId, boolean empty) {
        return acceptedEvent(accepted, eventId, slot, itemId, empty, Stage.STANDARD);
    }

    public static StartEvent acceptedEvent(boolean accepted, int eventId, int slot, int itemId,
                                           boolean empty, Stage stage) {
        return accepted ? new StartEvent(eventId, slot, itemId, empty, stage) : null;
    }

    public static StartEvent nextEvent(int slot, int itemId, boolean empty, Stage stage) {
        return new StartEvent(EVENT_IDS.incrementAndGet(), slot, itemId, empty, stage);
    }

    /** Shared identity gate for imported ACTION requests and legacy presentation state. */
    public static boolean matches(StartEvent event, int currentSlot, int currentItemId) {
        return event != null && event.slot == currentSlot && event.itemId == currentItemId;
    }

    public static final class Request {
        public final String clip;
        public final AnimationController.Layer layer;
        public final boolean restart;
        public final AnimationClip.Loop loop;
        public final boolean stop;

        private Request(String clip, Stage stage) {
            this.clip = clip;
            this.layer = AnimationController.Layer.ACTION;
            this.restart = true;
            this.loop = stage == Stage.INSERT ? AnimationClip.Loop.HOLD : null;
            this.stop = false;
        }

        private Request() {
            this.clip = null;
            this.layer = AnimationController.Layer.ACTION;
            this.restart = false;
            this.loop = null;
            this.stop = true;
        }
    }

    public static final class State {
        private int lastEventId = Integer.MIN_VALUE;
        private boolean active;
        private boolean started;
        private int slot;
        private int itemId;
        private String clip;
        private Stage stage = Stage.STANDARD;

        /** Returns the one clip request represented by this event, or null when it must be ignored. */
        public Request accept(StartEvent event, int currentSlot, int currentItemId, Set<String> availableClips) {
            if (!matches(event, currentSlot, currentItemId) || event.eventId == lastEventId) return null;
            lastEventId = event.eventId;
            String selected = selectClip(event, availableClips);
            if (selected == null) {
                if (event.stage != Stage.END) return null;
                finishNaturally();
                return new Request();
            }
            active = true;
            started = false;
            slot = event.slot;
            itemId = event.itemId;
            clip = selected;
            stage = event.stage;
            return new Request(selected, event.stage);
        }

        private String selectClip(StartEvent event, Set<String> availableClips) {
            String preferred;
            switch (event.stage) {
                case INTRO:
                    preferred = event.empty ? "reload_intro_empty" : "reload_intro";
                    if (availableClips.contains(preferred)) return preferred;
                    break;
                case INSERT:
                    if (availableClips.contains("reload_loop")) return "reload_loop";
                    break;
                case END:
                    return availableClips.contains("reload_end") ? "reload_end" : null;
                default:
                    break;
            }
            preferred = event.empty ? "reload_empty" : "reload_tactical";
            return availableClips.contains(preferred) ? preferred
                    : availableClips.contains("reload") ? "reload" : null;
        }

        public void started(boolean playbackStarted) {
            if (!active) return;
            if (playbackStarted) started = true;
            else invalidate();
        }

        public boolean presentationReload(boolean authoritativeReload) {
            return (active && stage != Stage.END) || authoritativeReload;
        }

        public boolean ownsAction() { return active; }

        public boolean playbackStarted() { return started; }

        public String clip() { return clip; }

        public boolean finishing() { return active && stage == Stage.END; }

        public void finishNaturally() {
            active = false;
            started = false;
            clip = null;
            stage = Stage.STANDARD;
        }

        public String invalidate() {
            String invalidated = clip;
            finishNaturally();
            return invalidated;
        }

        public String invalidateIfIdentityChanged(int currentSlot, int currentItemId) {
            return active && (slot != currentSlot || itemId != currentItemId) ? invalidate() : null;
        }
    }
}
