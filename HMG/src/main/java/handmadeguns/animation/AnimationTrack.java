package handmadeguns.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AnimationTrack {
    public final List<AnimationKeyframe> keyframes;

    public AnimationTrack(List<AnimationKeyframe> frames) {
        if (frames.isEmpty()) throw new IllegalArgumentException("Empty track");
        double previous = -1;
        for (AnimationKeyframe frame : frames) {
            if (frame.time <= previous) throw new IllegalArgumentException("Keyframe times must increase strictly");
            previous = frame.time;
        }
        keyframes = Collections.unmodifiableList(new ArrayList<AnimationKeyframe>(frames));
    }

    public AnimationPose.Transform sample(double time) {
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
}
