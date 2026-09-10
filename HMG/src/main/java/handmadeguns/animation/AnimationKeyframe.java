package handmadeguns.animation;

public final class AnimationKeyframe {
    public final double time;
    public final AnimationPose.Transform transform;

    public AnimationKeyframe(double time, AnimationPose.Transform transform) {
        if (!Double.isFinite(time) || time < 0 || transform == null) throw new IllegalArgumentException("Invalid keyframe");
        this.time = time;
        this.transform = transform;
    }
}
