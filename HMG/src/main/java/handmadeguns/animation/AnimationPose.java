package handmadeguns.animation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable local part transforms, independent of models, Minecraft and JSON. */
public final class AnimationPose {
    public static final AnimationPose EMPTY = new AnimationPose(Collections.<String, Transform>emptyMap());
    public final Map<String, Transform> parts;

    public AnimationPose(Map<String, Transform> parts) {
        this.parts = Collections.unmodifiableMap(new LinkedHashMap<String, Transform>(parts));
    }

    public Transform get(String part) {
        Transform value = parts.get(part);
        return value == null ? Transform.IDENTITY : value;
    }

    public static AnimationPose blend(AnimationPose from, AnimationPose to, double weight) {
        if (weight >= 1) return to;
        if (weight <= 0) return from;
        Map<String, Transform> result = new LinkedHashMap<String, Transform>();
        for (String part : from.parts.keySet()) result.put(part, from.get(part).blend(to.get(part), weight));
        for (String part : to.parts.keySet()) if (!result.containsKey(part))
            result.put(part, from.get(part).blend(to.get(part), weight));
        return new AnimationPose(result);
    }

    public static final class Transform {
        public static final Transform IDENTITY = new Transform(0, 0, 0, 0, 0, 0);
        public final float x, y, z, rx, ry, rz, sx, sy, sz;

        public Transform(float x, float y, float z, float rx, float ry, float rz) {
            this(x, y, z, rx, ry, rz, 1, 1, 1);
        }

        public Transform(float x, float y, float z, float rx, float ry, float rz, float sx, float sy, float sz) {
            this.x = finite(x); this.y = finite(y); this.z = finite(z);
            this.rx = finite(rx); this.ry = finite(ry); this.rz = finite(rz);
            this.sx = finite(sx); this.sy = finite(sy); this.sz = finite(sz);
        }

        private static float finite(float value) {
            if (Float.isNaN(value) || Float.isInfinite(value)) throw new IllegalArgumentException("Non-finite transform");
            return value;
        }

        public Transform blend(Transform other, double weight) {
            return new Transform((float)(x + ((double)other.x - x) * weight),
                    (float)(y + ((double)other.y - y) * weight), (float)(z + ((double)other.z - z) * weight),
                    (float)(rx + ((double)other.rx - rx) * weight), (float)(ry + ((double)other.ry - ry) * weight),
                    (float)(rz + ((double)other.rz - rz) * weight),
                    (float)(sx + ((double)other.sx - sx) * weight),
                    (float)(sy + ((double)other.sy - sy) * weight),
                    (float)(sz + ((double)other.sz - sz) * weight));
        }

        /** Component addition, not matrix multiplication; angles intentionally do not wrap. */
        public Transform add(Transform other) {
            return new Transform(x + other.x, y + other.y, z + other.z,
                    rx + other.rx, ry + other.ry, rz + other.rz, sx * other.sx, sy * other.sy, sz * other.sz);
        }
    }
}
