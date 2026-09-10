package handmadeguns.client.modelLoader.blockbench;

import handmadeguns.animation.AnimationPose;
import org.lwjgl.opengl.GL11;

/** One conversion boundary from saved Blockbench project space to HMG model space.
 * C = rotationZ(180 degrees), so positions are [-x,-y,z] and Euler angles are
 * [-rx,-ry,rz]. Bedrock uses ZYX Euler and C preserves winding.
 *
 * Blockbench pixels use a 3/16 HMG unit baseline. The factor of three matches the
 * legacy HMG firearm baseline (legacy MQO coordinates are divided by 100) while
 * leaving ModelScala and Gunparts_offsetScale as author controls.
 * Origins in the project are absolute even when bones are nested; translations are parent-local.
 */
public final class BlockbenchTransform {
    private static final double HMG_UNITS_PER_PIXEL = 3.0 / 16.0;
    private static final float HMG_MODEL_NORMALIZATION = 3.0f;
    private BlockbenchTransform() { }
    public static double factor(String channel, int axis) {
        if ("scale".equals(channel)) return 1;
        return (axis == 2 ? 1 : -1) * ("position".equals(channel) ? HMG_UNITS_PER_PIXEL : 1);
    }
    /** Scale a non-Blockbench mesh placed inside the converted model, such as a player arm. */
    public static float modelNormalization() { return HMG_MODEL_NORMALIZATION; }
    public static float[] point(double[] position, double[] parentOrigin) {
        float[] result = new float[3];
        for (int i = 0; i < 3; i++) result[i] = (float)((position[i] - parentOrigin[i]) * factor("position", i));
        return result;
    }
    public static float[] rotation(double[] angles) {
        float[] result = new float[3];
        for (int i = 0; i < 3; i++) result[i] = (float)(angles[i] * factor("rotation", i));
        return result;
    }
    public static double[] rotate(double[] point, double[] origin, double[] degrees) {
        double x = point[0] - origin[0], y = point[1] - origin[1], z = point[2] - origin[2];
        double a = Math.toRadians(degrees[0]), b = Math.toRadians(degrees[1]), c = Math.toRadians(degrees[2]);
        double yy = y*Math.cos(a) - z*Math.sin(a), zz = y*Math.sin(a) + z*Math.cos(a);
        double xx = x*Math.cos(b) + zz*Math.sin(b); z = -x*Math.sin(b) + zz*Math.cos(b);
        x = xx*Math.cos(c) - yy*Math.sin(c); y = xx*Math.sin(c) + yy*Math.cos(c);
        return new double[]{x + origin[0], y + origin[1], z + origin[2]};
    }
    /** Geometry is local to the bone origin. Children remain in that local frame after this call. */
    public static void apply(float[] origin, float[] rest, AnimationPose.Transform pose, float units) {
        apply(origin,rest,pose.x,pose.y,pose.z,pose.rx,pose.ry,pose.rz,pose.sx,pose.sy,pose.sz,units);
    }
    public static void apply(float[] origin, float[] rest,
                             handmadeguns.client.render.HMGGunParts_Motion_PosAndRotation pose, float units) {
        if (pose == null) apply(origin,rest,AnimationPose.Transform.IDENTITY,units);
        else apply(origin,rest,pose.posX,pose.posY,pose.posZ,pose.rotationX,pose.rotationY,pose.rotationZ,
                pose.scaleX,pose.scaleY,pose.scaleZ,units);
    }
    private static void apply(float[] origin, float[] rest, float x,float y,float z,float rx,float ry,float rz,
                              float sx,float sy,float sz,float units) {
        GL11.glTranslatef((origin[0]+x)*units,(origin[1]+y)*units,(origin[2]+z)*units);
        GL11.glRotatef(rest[2]+rz,0,0,1); GL11.glRotatef(rest[1]+ry,0,1,0); GL11.glRotatef(rest[0]+rx,1,0,0);
        GL11.glScalef(sx,sy,sz);
    }
    /** TaCZ hand anchors rotate the player arm 180 degrees around local Z. */
    public static void playerArmFrame() {
        GL11.glRotatef(180,0,0,1);
    }
}
