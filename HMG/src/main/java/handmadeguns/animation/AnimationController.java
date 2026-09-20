package handmadeguns.animation;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small client-independent pose mixer. Call advanceTo once per clock value, sample as often as needed. */
public final class AnimationController {
    public enum Layer { BASE, MOVEMENT, ACTION, ADDITIVE }
    private final AnimationDefinition definition;
    private final EnumMap<Layer, AnimationPlayback> layers = new EnumMap<Layer, AnimationPlayback>(Layer.class);
    private AnimationPose legacy = AnimationPose.EMPTY, transitionFrom;
    private double clock = Double.NaN, transitionElapsed, transitionDuration;
    private long generation;
    private AnimationPose movementFrom;
    private double movementElapsed, movementDuration;

    public AnimationController(AnimationDefinition definition) { this.definition = definition; }

    public boolean play(Layer layer, String name) { return play(layer, name, false, 1, null); }

    public boolean play(Layer layer, String name, boolean restart, int direction, AnimationClip.Loop loop) {
        AnimationClip clip = definition.requireClip(name);
        AnimationPlayback old = layers.get(layer);
        if (old != null) {
            if (!restart && old.clip == clip) return true;
            if (!old.clip.interruptible || clip.priority < old.clip.priority) return false;
        }
        AnimationClip.Loop playbackLoop = loop == null ? clip.loop : loop;
        // Locomotion requests normally loop. Some authored Bedrock idle aliases are
        // zero-duration static poses, which the importer correctly represents as HOLD.
        // Preserve that authored hold when a generic caller requests LOOP.
        if (clip.duration == 0 && playbackLoop == AnimationClip.Loop.LOOP)
            playbackLoop = clip.loop == AnimationClip.Loop.LOOP ? AnimationClip.Loop.HOLD : clip.loop;
        AnimationPlayback next = new AnimationPlayback(clip, ++generation, direction, playbackLoop);
        if (layer == Layer.MOVEMENT) beginMovementTransition(clip.fadeIn);
        else beginTransition(clip.fadeIn);
        layers.put(layer, next);
        return true;
    }

    /** Explicit owner invalidation bypasses request priority. Natural one-shot completion needs no stop. */
    public void stop(Layer layer) {
        AnimationPlayback old = layers.get(layer);
        if (old != null) {
            if (layer == Layer.MOVEMENT) beginMovementTransition(old.clip.fadeOut);
            else beginTransition(old.clip.fadeOut);
            layers.remove(layer);
        }
    }

    public boolean active(String name) {
        for (AnimationPlayback playback : layers.values()) if (playback.clip.name.equals(name)) return true;
        return false;
    }

    public String current(Layer layer) { return layers.containsKey(layer) ? layers.get(layer).clip.name : null; }
    public double progress(Layer layer) { return layers.containsKey(layer) ? layers.get(layer).progress() : 0; }
    public boolean transitioning() { return transitionFrom != null; }

    private void beginTransition(double duration) {
        // Evaluate before changing layers. Each pose is immutable, so subsequent
        // samples cannot change this source (including back-to-back requests).
        transitionFrom = duration == 0 ? null : compose();
        transitionDuration = duration;
        transitionElapsed = 0;
    }

    private void beginMovementTransition(double duration) {
        movementFrom = duration == 0 ? null : movementPose();
        movementDuration = duration;
        movementElapsed = 0;
    }

    private AnimationPose movementPose() {
        Map<String, AnimationPose.Transform> parts = new LinkedHashMap<String, AnimationPose.Transform>();
        AnimationPlayback playback = layers.get(Layer.MOVEMENT);
        if (playback != null) for (Map.Entry<String, AnimationTrack> track : playback.clip.tracks.entrySet())
            parts.put(track.getKey(), track.getValue().sample(playback.time()));
        AnimationPose target = new AnimationPose(parts);
        return movementFrom == null ? target : AnimationPose.blend(movementFrom, target,
                movementDuration == 0 ? 1 : movementElapsed / movementDuration);
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
            movementElapsed += step;
            if (movementFrom != null && movementElapsed >= movementDuration) movementFrom = null;
            if (transitionFrom != null && transitionElapsed >= transitionDuration) transitionFrom = null;
            boolean completed = false;
            double fade = 0;
            boolean visibleCompletion = false;
            for (Map.Entry<Layer, AnimationPlayback> entry : layers.entrySet()) if (entry.getValue().finished()) {
                completed = true;
                if (entry.getKey() == Layer.MOVEMENT) beginMovementTransition(entry.getValue().clip.fadeOut);
                else {
                    visibleCompletion = true;
                    fade = Math.max(fade, entry.getValue().clip.fadeOut);
                }
            }
            if (completed) {
                if (visibleCompletion) beginTransition(fade);
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
        return compose();
    }

    private AnimationPose compose() {
        Map<String, AnimationPose.Transform> result = new LinkedHashMap<String, AnimationPose.Transform>(legacy.parts);
        for (Layer layer : Layer.values()) {
            if (layer == Layer.MOVEMENT) {
                // Blend movement alone: its exit must never capture/replay an ACTION pose.
                if (!layers.containsKey(Layer.ACTION)) for (Map.Entry<String, AnimationPose.Transform> track : movementPose().parts.entrySet()) {
                    AnimationPose.Transform base = result.get(track.getKey());
                    result.put(track.getKey(), base == null ? track.getValue() : base.add(track.getValue()));
                }
                continue;
            }
            AnimationPlayback playback = layers.get(layer);
            if (playback == null) continue;
            // Actions own the whole pose, including channels/bones omitted by a sparse clip.
            for (Map.Entry<String, AnimationTrack> track : playback.clip.tracks.entrySet()) {
                AnimationPose.Transform value = track.getValue().sample(playback.time());
                if (layer == Layer.ADDITIVE) {
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
