package handmadeguns.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AnimationTrack {
    public final List<AnimationKeyframe> keyframes;
    public final AnimationChannel position, rotation, scale;

    /** Independent channel clocks; absent channels leave the underlying pose intact. */
    public AnimationTrack(AnimationChannel position, AnimationChannel rotation, AnimationChannel scale) {
        this.position = position; this.rotation = rotation; this.scale = scale;
        keyframes = Collections.emptyList();
        if (position == null && rotation == null && scale == null) throw new IllegalArgumentException("Empty track");
    }

    public AnimationTrack(List<AnimationKeyframe> frames) {
        position = rotation = scale = null;
        if (frames.isEmpty()) throw new IllegalArgumentException("Empty track");
        double previous = -1;
        for (AnimationKeyframe frame : frames) {
            if (frame.time <= previous) throw new IllegalArgumentException("Keyframe times must increase strictly");
            previous = frame.time;
        }
        keyframes = Collections.unmodifiableList(new ArrayList<AnimationKeyframe>(frames));
    }

    public AnimationPose.Transform sample(double time) {
        if (keyframes.isEmpty()) return new AnimationPose.Transform(
                value(position, time, 0, 0), value(position, time, 1, 0), value(position, time, 2, 0),
                value(rotation, time, 0, 0), value(rotation, time, 1, 0), value(rotation, time, 2, 0),
                value(scale, time, 0, 1), value(scale, time, 1, 1), value(scale, time, 2, 1));
        if (time <= keyframes.get(0).time) return keyframes.get(0).transform;
        int last = keyframes.size() - 1;
        if (time >= keyframes.get(last).time) return keyframes.get(last).transform;
        int low = 0, high = last;
        while (high - low > 1) {
            int mid = (low + high) >>> 1;
            if (keyframes.get(mid).time <= time) low = mid; else high = mid;
        }
        AnimationKeyframe a = keyframes.get(low), b = keyframes.get(high);
        return a.transform.blend(b.transform, (time - a.time) / (b.time - a.time));
    }

    private static float value(AnimationChannel channel, double time, int axis, float fallback) {
        return channel == null ? fallback : (float)channel.sample(time, axis);
    }

    public AnimationPose.Transform overlay(AnimationPose.Transform base, AnimationPose.Transform value) {
        if (!keyframes.isEmpty() || base == null) return value;
        return new AnimationPose.Transform(position == null ? base.x : value.x,
                position == null ? base.y : value.y, position == null ? base.z : value.z,
                rotation == null ? base.rx : value.rx, rotation == null ? base.ry : value.ry,
                rotation == null ? base.rz : value.rz, scale == null ? base.sx : value.sx,
                scale == null ? base.sy : value.sy, scale == null ? base.sz : value.sz);
    }
}
