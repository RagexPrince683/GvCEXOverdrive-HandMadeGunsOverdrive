package handmadeguns.animation;

import java.util.Set;

/** Dependency-free reload presentation state shared by the packet bridge and animation tests. */
public final class ReloadAnimationBridge {
    private ReloadAnimationBridge() { }

    public static final class StartEvent {
        public final int eventId;
        public final int slot;
        public final int itemId;
        public final boolean empty;

        public StartEvent(int eventId, int slot, int itemId, boolean empty) {
            this.eventId = eventId;
            this.slot = slot;
            this.itemId = itemId;
            this.empty = empty;
        }
    }

    public static StartEvent acceptedEvent(boolean accepted, int eventId, int slot, int itemId, boolean empty) {
        return accepted ? new StartEvent(eventId, slot, itemId, empty) : null;
    }

    /** Shared identity gate for imported ACTION requests and legacy presentation state. */
    public static boolean matches(StartEvent event, int currentSlot, int currentItemId) {
        return event != null && event.slot == currentSlot && event.itemId == currentItemId;
    }

    public static final class Request {
        public final String clip;
        public final AnimationController.Layer layer;
        public final boolean restart;

        private Request(String clip) {
            this.clip = clip;
            this.layer = AnimationController.Layer.ACTION;
            this.restart = true;
        }
    }

    public static final class State {
        private int lastEventId = Integer.MIN_VALUE;
        private boolean active;
        private boolean started;
        private int slot;
        private int itemId;
        private String clip;

        /** Returns the one clip request represented by this event, or null when it must be ignored. */
        public Request accept(StartEvent event, int currentSlot, int currentItemId, Set<String> availableClips) {
            if (!matches(event, currentSlot, currentItemId) || event.eventId == lastEventId) return null;
            lastEventId = event.eventId;
            String preferred = event.empty ? "reload_empty" : "reload_tactical";
            String selected = availableClips.contains(preferred) ? preferred
                    : availableClips.contains("reload") ? "reload" : null;
            if (selected == null) return null;
            active = true;
            started = false;
            slot = event.slot;
            itemId = event.itemId;
            clip = selected;
            return new Request(selected);
        }

        public void started(boolean playbackStarted) {
            if (!active) return;
            if (playbackStarted) started = true;
            else invalidate();
        }

        public boolean presentationReload(boolean authoritativeReload) { return active || authoritativeReload; }

        public boolean ownsAction() { return active; }

        public boolean playbackStarted() { return started; }

        public String clip() { return clip; }

        public void finishNaturally() {
            active = false;
            started = false;
            clip = null;
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
