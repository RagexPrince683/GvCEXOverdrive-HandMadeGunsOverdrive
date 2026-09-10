package handmadeguns.animation;

/** Mutable cursor owned by exactly one controller, never by an animation definition. */
public final class AnimationPlayback {
    public interface EventSink {
        void onEvent(AnimationClip clip, AnimationEvent event, long generation, long cycle);
    }

    public final AnimationClip clip;
    public final long generation;
    public final int direction;
    public final AnimationClip.Loop loop;
    private double elapsed;
    private long cycle;
    private boolean started;

    public AnimationPlayback(AnimationClip clip, long generation, int direction, AnimationClip.Loop loop) {
        if (direction != 1 && direction != -1) throw new IllegalArgumentException("Direction must be 1 or -1");
        if (loop == AnimationClip.Loop.LOOP && clip.duration == 0) throw new IllegalArgumentException("Cannot loop zero duration");
        this.clip = clip; this.generation = generation; this.direction = direction; this.loop = loop;
    }

    public double time() { return direction == 1 ? elapsed : clip.duration - elapsed; }
    public double progress() { return clip.duration == 0 ? 1 : elapsed / clip.duration; }
    public double remaining() { return Math.max(0, clip.duration - elapsed); }
    public boolean finished() { return started && loop == AnimationClip.Loop.ONCE && elapsed >= clip.duration; }

    public void advance(double delta, EventSink sink) {
        if (!Double.isFinite(delta) || delta < 0) throw new IllegalArgumentException("Invalid elapsed seconds");
        if (!started) { started = true; emitEndpoint(time(), sink); }
        while (delta > 0 && clip.duration > 0) {
            double step = Math.min(delta, remaining());
            double before = time();
            elapsed += step;
            emitRange(before, time(), sink);
            delta -= step;
            if (elapsed >= clip.duration) {
                if (loop != AnimationClip.Loop.LOOP) break;
                elapsed = 0;
                cycle++;
                emitEndpoint(time(), sink);
            }
        }
    }

    private void emitEndpoint(double time, EventSink sink) {
        if (sink != null) for (AnimationEvent event : clip.events)
            if (event.time == time) sink.onEvent(clip, event, generation, cycle);
    }

    private void emitRange(double from, double to, EventSink sink) {
        if (sink == null || from == to) return;
        if (direction == 1) {
            for (AnimationEvent event : clip.events) if (event.time > from && event.time <= to)
                sink.onEvent(clip, event, generation, cycle);
        } else {
            // Reverse timestamp order; retain declaration order within each timestamp.
            int end = clip.events.size();
            while (end > 0) {
                int start = end - 1;
                double timestamp = clip.events.get(start).time;
                while (start > 0 && clip.events.get(start - 1).time == timestamp) start--;
                if (timestamp < from && timestamp >= to) for (int i = start; i < end; i++)
                    sink.onEvent(clip, clip.events.get(i), generation, cycle);
                end = start;
            }
        }
    }
}
