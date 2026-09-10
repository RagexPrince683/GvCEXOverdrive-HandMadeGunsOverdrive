package handmadeguns.animation;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small client-independent pose mixer. Call advanceTo once per clock value, sample as often as needed. */
public final class AnimationController {
    public enum Layer { BASE, ACTION, ADDITIVE }
    private final AnimationDefinition definition;
    private final EnumMap<Layer, AnimationPlayback> layers = new EnumMap<Layer, AnimationPlayback>(Layer.class);
    private AnimationPose legacy = AnimationPose.EMPTY, lastPose = AnimationPose.EMPTY, transitionFrom;
    private double clock = Double.NaN, transitionElapsed, transitionDuration;
    private long generation;

    public AnimationController(AnimationDefinition definition) { this.definition = definition; }

    public boolean play(Layer layer, String name) { return play(layer, name, false, 1, null); }

    public boolean play(Layer layer, String name, boolean restart, int direction, AnimationClip.Loop loop) {
        AnimationClip clip = definition.requireClip(name);
        AnimationPlayback old = layers.get(layer);
        if (old != null) {
            if (!restart && old.clip == clip) return true;
            if (!old.clip.interruptible || clip.priority < old.clip.priority) return false;
        }
        AnimationPlayback next = new AnimationPlayback(clip, ++generation, direction, loop == null ? clip.loop : loop);
        beginTransition(clip.fadeIn);
        layers.put(layer, next);
        return true;
    }

    /** Explicit owner cancellation (e.g. authoritative reload ended) bypasses request priority. */
    public void stop(Layer layer) {
        AnimationPlayback old = layers.get(layer);
        if (old != null) { beginTransition(old.clip.fadeOut); layers.remove(layer); }
    }

    public boolean active(String name) {
        for (AnimationPlayback playback : layers.values()) if (playback.clip.name.equals(name)) return true;
        return false;
    }

    public String current(Layer layer) { return layers.containsKey(layer) ? layers.get(layer).clip.name : null; }
    public double progress(Layer layer) { return layers.containsKey(layer) ? layers.get(layer).progress() : 0; }
    public boolean transitioning() { return transitionFrom != null; }

    private void beginTransition(double duration) {
        transitionFrom = duration == 0 ? null : lastPose;
        transitionDuration = duration;
        transitionElapsed = 0;
    }

    public void advanceTo(double seconds, AnimationPlayback.EventSink sink) {
        if (!Double.isFinite(seconds)) throw new IllegalArgumentException("Invalid animation clock");
        if (Double.isNaN(clock)) clock = seconds;
        if (seconds < clock) throw new IllegalArgumentException("Animation clock moved backwards");
        double remaining = seconds - clock;
        clock = seconds;
        // Segment at non-looping completions so stalls spend the correct time in the exit fade.
        do {
            double step = remaining;
            for (AnimationPlayback playback : layers.values())
                if (playback.loop == AnimationClip.Loop.ONCE) step = Math.min(step, playback.remaining());
            for (AnimationPlayback playback : layers.values()) playback.advance(step, sink);
            transitionElapsed += step;
            if (transitionFrom != null && transitionElapsed >= transitionDuration) transitionFrom = null;
            lastPose = compose();
            boolean completed = false;
            double fade = 0;
            for (AnimationPlayback playback : layers.values()) if (playback.finished()) {
                completed = true; fade = Math.max(fade, playback.clip.fadeOut);
            }
            if (completed) {
                beginTransition(fade);
                for (Layer layer : Layer.values()) {
                    AnimationPlayback playback = layers.get(layer);
                    if (playback != null && playback.finished()) layers.remove(layer);
                }
            }
            remaining -= step;
            if (step == 0 && !completed) break;
        } while (remaining > 0);
    }

    /** Captures the actual blended pose, including the legacy baseline, for the next interruption. */
    public AnimationPose sample(AnimationPose legacyPose) {
        legacy = legacyPose;
        lastPose = compose();
        return lastPose;
    }

    private AnimationPose compose() {
        Map<String, AnimationPose.Transform> result = new LinkedHashMap<String, AnimationPose.Transform>(legacy.parts);
        for (Map.Entry<Layer, AnimationPlayback> entry : layers.entrySet()) {
            AnimationPlayback playback = entry.getValue();
            for (Map.Entry<String, AnimationTrack> track : playback.clip.tracks.entrySet()) {
                AnimationPose.Transform value = track.getValue().sample(playback.time());
                if (entry.getKey() == Layer.ADDITIVE) {
                    AnimationPose.Transform base = result.get(track.getKey());
                    if (base != null) value = base.add(value);
                } else value = track.getValue().overlay(result.get(track.getKey()), value);
                result.put(track.getKey(), value);
            }
        }
        AnimationPose target = new AnimationPose(result);
        return transitionFrom == null ? target : AnimationPose.blend(transitionFrom, target,
                transitionDuration == 0 ? 1 : transitionElapsed / transitionDuration);
    }
}
