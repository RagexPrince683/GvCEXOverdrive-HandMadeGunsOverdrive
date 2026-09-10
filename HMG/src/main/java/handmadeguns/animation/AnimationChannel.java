package handmadeguns.animation;

import java.util.*;

/** Numeric vector channel with discontinuous keys and independently timed cubic interpolation. */
public final class AnimationChannel {
    public enum Interpolation { LINEAR, STEP, CATMULLROM, BEZIER }
    public static final class Key {
        public final double time;
        public final Interpolation interpolation;
        private final double[] before, after, leftTime, leftValue, rightTime, rightValue;
        public final boolean split;
        public Key(double time, double[] before, double[] after, Interpolation interpolation,
                   double[] leftTime, double[] leftValue, double[] rightTime, double[] rightValue) {
            if (!Double.isFinite(time) || time < 0) throw new IllegalArgumentException("Invalid channel time");
            this.time = time; this.interpolation = Objects.requireNonNull(interpolation);
            this.before = vector(before); this.after = vector(after == null ? before : after);
            split = after != null;
            this.leftTime = vector(leftTime); this.leftValue = vector(leftValue);
            this.rightTime = vector(rightTime); this.rightValue = vector(rightValue);
        }
        private static double[] vector(double[] values) {
            if (values == null) return new double[3];
            if (values.length != 3) throw new IllegalArgumentException("Expected three channel components");
            for (double value : values) if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite channel");
            return values.clone();
        }
    }
    public final List<Key> keys;
    private final boolean loopTangents;
    public AnimationChannel(List<Key> keys, boolean loopTangents) {
        List<Key> sorted = new ArrayList<Key>(keys);
        Collections.sort(sorted, new Comparator<Key>() {
            @Override public int compare(Key a, Key b) { return Double.compare(a.time, b.time); }
        });
        double previous = -1;
        for (Key key : sorted) {
            if (key.time <= previous) throw new IllegalArgumentException("Duplicate channel key time: " + key.time);
            previous = key.time;
        }
        if (sorted.isEmpty()) throw new IllegalArgumentException("Empty channel");
        this.keys = Collections.unmodifiableList(sorted); this.loopTangents = loopTangents;
    }
    public double sample(double time, int axis) {
        int last = keys.size() - 1;
        if (time <= keys.get(0).time) return keys.get(0).before[axis];
        if (time > keys.get(last).time) return time - keys.get(last).time < 1.0/1200
                ? keys.get(last).before[axis] : keys.get(last).after[axis];
        int low = 0, high = last;
        while (high - low > 1) {
            int mid = (low + high) >>> 1;
            if (keys.get(mid).time < time) low = mid; else high = mid;
        }
        Key a = keys.get(low), b = keys.get(high);
        if (time - a.time < 1.0/1200) return a.before[axis];
        if (b.time - time < 1.0/1200) return b.before[axis];
        if (a.interpolation == Interpolation.STEP || low == high) return a.after[axis];
        double t = (time - a.time) / (b.time - a.time), p = a.after[axis], q = b.before[axis];
        if (a.interpolation == Interpolation.CATMULLROM || b.interpolation == Interpolation.CATMULLROM) {
            Key previous = low > 0 ? keys.get(low - 1) : loopTangents && last >= 2 ? keys.get(last - 1) : null;
            Key next = high < last ? keys.get(high + 1) : loopTangents && last >= 2 ? keys.get(1) : null;
            // Match the editor's SplineCurve parameterization, including its split-key neighbor indexing.
            // The reference retains the preceding-key parameter offset even when a split omits that point.
            boolean includePrevious = previous != null && !a.split, includeNext = next != null && !b.split;
            double parameter = t + (previous != null ? 1 : 0);
            int segment = (int)parameter;
            double p0 = splinePoint(segment-1,includePrevious,includeNext,previous,a,b,next,axis);
            double p1 = splinePoint(segment,includePrevious,includeNext,previous,a,b,next,axis);
            double p2 = splinePoint(segment+1,includePrevious,includeNext,previous,a,b,next,axis);
            double p3 = splinePoint(segment+2,includePrevious,includeNext,previous,a,b,next,axis);
            return cubic(p1,p2,(p2-p0)*0.5,(p3-p1)*0.5,parameter-segment);
        }
        if (a.interpolation == Interpolation.BEZIER || b.interpolation == Interpolation.BEZIER) {
            double gap = b.time - a.time;
            double x1 = Math.max(0, Math.min(gap, a.rightTime[axis])) / gap;
            double x2 = 1 + Math.max(-gap, Math.min(0, b.leftTime[axis])) / gap;
            double lo = 0, hi = 1;
            for (int i = 0; i < 32; i++) {
                double u = (lo + hi) * 0.5;
                if (bezier(0, x1, x2, 1, u) < t) lo = u; else hi = u;
            }
            return bezier(p, p + a.rightValue[axis], q + b.leftValue[axis], q, (lo + hi) * 0.5);
        }
        return p + (q - p) * t;
    }
    private static double splinePoint(int index, boolean pre, boolean post, Key previous, Key a, Key b, Key next, int axis) {
        int length = 2 + (pre ? 1 : 0) + (post ? 1 : 0);
        index = Math.max(0,Math.min(length-1,index));
        if (pre) { if (index == 0) return previous.after[axis]; index--; }
        if (index == 0) return a.after[axis];
        if (index == 1) return b.before[axis];
        return next.before[axis];
    }
    private static double cubic(double p, double q, double m, double n, double t) {
        return (2*t*t*t - 3*t*t + 1)*p + (t*t*t - 2*t*t + t)*m
                + (-2*t*t*t + 3*t*t)*q + (t*t*t - t*t)*n;
    }
    private static double bezier(double a, double b, double c, double d, double t) {
        double s = 1 - t;
        return s*s*s*a + 3*s*s*t*b + 3*s*t*t*c + t*t*t*d;
    }
}
