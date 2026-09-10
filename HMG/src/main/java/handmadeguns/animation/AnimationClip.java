package handmadeguns.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AnimationClip {
    public enum Loop { ONCE, LOOP, HOLD }
    public final String name;
    public final double duration, fadeIn, fadeOut;
    public final Loop loop;
    public final int priority;
    public final boolean interruptible;
    public final Map<String, AnimationTrack> tracks;
    public final List<AnimationEvent> events;

    public AnimationClip(String name, double duration, Loop loop, Map<String, AnimationTrack> tracks,
                         List<AnimationEvent> events, double fadeIn, double fadeOut, int priority, boolean interruptible) {
        if (name == null || name.isEmpty() || !Double.isFinite(duration) || duration < 0 || loop == null
                || (duration == 0 && loop == Loop.LOOP) || !Double.isFinite(fadeIn) || fadeIn < 0
                || !Double.isFinite(fadeOut) || fadeOut < 0) throw new IllegalArgumentException("Invalid clip metadata");
        for (AnimationTrack track : tracks.values()) for (AnimationKeyframe frame : track.keyframes)
            if (frame.time > duration) throw new IllegalArgumentException("Keyframe after duration");
        // Independent channels may retain control keys after the clip end (e.g. cubic end tangents).
        for (AnimationEvent event : events) if (event.time > duration) throw new IllegalArgumentException("Event after duration");
        this.name = name; this.duration = duration; this.loop = loop;
        this.fadeIn = fadeIn; this.fadeOut = fadeOut; this.priority = priority; this.interruptible = interruptible;
        this.tracks = Collections.unmodifiableMap(new LinkedHashMap<String, AnimationTrack>(tracks));
        List<AnimationEvent> sorted = new ArrayList<AnimationEvent>(events);
        // Stable sort preserves author order for simultaneous markers.
        Collections.sort(sorted, new Comparator<AnimationEvent>() {
            @Override public int compare(AnimationEvent a, AnimationEvent b) { return Double.compare(a.time, b.time); }
        });
        this.events = Collections.unmodifiableList(sorted);
    }
}
