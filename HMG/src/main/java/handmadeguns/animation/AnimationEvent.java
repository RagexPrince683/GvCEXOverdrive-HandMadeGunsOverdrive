package handmadeguns.animation;

/** Presentation marker only. Payload is optional author-defined text. */
public final class AnimationEvent {
    public final double time;
    public final String name, data;

    public AnimationEvent(double time, String name, String data) {
        if (!Double.isFinite(time) || time < 0 || name == null || name.isEmpty())
            throw new IllegalArgumentException("Invalid animation event");
        this.time = time;
        this.name = name;
        this.data = data;
    }
}
