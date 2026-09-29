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

        /** Post-multiply a procedural TaCZ root rotation after its authored ZYX pose. */
        public Transform addBedrock(Transform other) {
            double[] a = quaternion(rx, ry, rz), b = quaternion(other.rx, other.ry, other.rz);
            double xq = a[3]*b[0] + a[0]*b[3] + a[1]*b[2] - a[2]*b[1];
            double yq = a[3]*b[1] - a[0]*b[2] + a[1]*b[3] + a[2]*b[0];
            double zq = a[3]*b[2] + a[0]*b[1] - a[1]*b[0] + a[2]*b[3];
            double wq = a[3]*b[3] - a[0]*b[0] - a[1]*b[1] - a[2]*b[2];
            double pitch = Math.asin(Math.max(-1, Math.min(1, 2*(wq*yq-zq*xq))));
            double roll, yaw;
            if (Math.abs(Math.cos(pitch)) > 0.00001) {
                roll = Math.atan2(2*(wq*xq+yq*zq), 1-2*(xq*xq+yq*yq));
                yaw = Math.atan2(2*(wq*zq+xq*yq), 1-2*(yq*yq+zq*zq));
            } else {
                roll = 0;
                yaw = Math.atan2(2*(wq*zq-xq*yq), 1-2*(xq*xq+zq*zq));
            }
            return new Transform(x+other.x, y+other.y, z+other.z,
                    (float)Math.toDegrees(roll), (float)Math.toDegrees(pitch), (float)Math.toDegrees(yaw),
                    sx*other.sx, sy*other.sy, sz*other.sz);
        }

        private static double[] quaternion(float x, float y, float z) {
            double ax = Math.toRadians(x)/2, ay = Math.toRadians(y)/2, az = Math.toRadians(z)/2;
            double cx = Math.cos(ax), cy = Math.cos(ay), cz = Math.cos(az);
            double sx = Math.sin(ax), sy = Math.sin(ay), sz = Math.sin(az);
            return new double[]{sx*cy*cz-cx*sy*sz, cx*sy*cz+sx*cy*sz,
                    cx*cy*sz-sx*sy*cz, cx*cy*cz+sx*sy*sz};
        }
    }
}
