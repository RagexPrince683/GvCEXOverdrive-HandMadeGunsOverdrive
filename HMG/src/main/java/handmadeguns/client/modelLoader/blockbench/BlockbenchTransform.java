package handmadeguns.client.modelLoader.blockbench;

import handmadeguns.animation.AnimationPose;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import java.nio.FloatBuffer;
import java.util.List;

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
    private static final ThreadLocal<FloatBuffer> MATRIX = new ThreadLocal<FloatBuffer>() {
        @Override protected FloatBuffer initialValue() { return BufferUtils.createFloatBuffer(16); }
    };
    private BlockbenchTransform() { }
    public static double factor(String channel, int axis) {
        if ("scale".equals(channel)) return 1;
        return (axis == 2 ? 1 : -1) * ("position".equals(channel) ? HMG_UNITS_PER_PIXEL : 1);
    }
    /** Scale a non-Blockbench mesh placed inside the converted model, such as a player arm. */
    public static float modelNormalization() { return HMG_MODEL_NORMALIZATION; }
    /** HMG presentation expects Ry(180) project axes; imported bones use Rz(180).
     * Ry(180) * inverse(Rz(180)) = Rx(180). Apply once outside the root hierarchy
     * for equipped, dropped, GUI and nested models, never once per bone.
     */
    public static void presentationFrame() { GL11.glRotatef(180,1,0,0); }

    /** Apply TaCZ's positioning-node inverse in the already-corrected presentation frame.
     * View translation is lerped and view rotation is slerped, matching TaCZ's rigid
     * matrix blend. Positioning nodes deliberately ignore model-deformation scale.
     */
    public static void applyPositioning(List<BlockbenchModel.Part> from,
                                        List<BlockbenchModel.Part> to, float blend, float units) {
        if (from == null || from.isEmpty()) return;
        float[] a = inversePath(from,units);
        float[] selected = a;
        if (to != null && !to.isEmpty() && blend > 0) {
            float[] b = inversePath(to,units);
            float weight = Math.max(0, Math.min(1, blend));
            selected = interpolateRigid(a,b,weight);
        }
        // The root renderer applies Q afterwards. Conjugating here gives
        // (Q * node^-1 * Q^-1) * Q * model = Q * node^-1 * model.
        float[] q = rotationX(180);
        float[] matrix = multiply(multiply(q, selected), q);
        applyMatrix(matrix);
    }

    /** HMG's GUI supplies its standard preview angle; TaCZ fixed nodes supply the anchor only. */
    public static void applyPositioningTranslation(List<BlockbenchModel.Part> path, float units) {
        if (path == null || path.isEmpty()) return;
        float[] selected = inversePath(path, units);
        float[] q = rotationX(180);
        float[] positioned = multiply(multiply(q, selected), q);
        applyMatrix(translation(positioned[12], positioned[13], positioned[14]));
    }

    /** Forge 1.7's equipped-item hand frame has the opposite depth convention to TaCZ. */
    public static void applyThirdPersonPositioning(List<BlockbenchModel.Part> path, float units) {
        if (path == null || path.isEmpty()) return;
        float[] selected = inversePath(path, units);
        float[] q = rotationX(180);
        float[] positioned = multiply(multiply(q, selected), q);
        positioned[14] = -positioned[14];
        applyMatrix(positioned);
    }

    private static void applyMatrix(float[] matrix) {
        FloatBuffer buffer = MATRIX.get();
        buffer.clear(); buffer.put(matrix); buffer.flip();
        GL11.glMultMatrix(buffer);
    }

    private static float[] inversePath(List<BlockbenchModel.Part> path, float units) {
        float[] matrix = identity();
        for (int i=path.size()-1;i>=0;i--) {
            BlockbenchModel.Part part = path.get(i);
            matrix = multiply(matrix,rotationX(-part.restRotation[0]));
            matrix = multiply(matrix,rotationY(-part.restRotation[1]));
            matrix = multiply(matrix,rotationZ(-part.restRotation[2]));
            matrix = multiply(matrix,translation(-part.localOrigin[0]*units,-part.localOrigin[1]*units,-part.localOrigin[2]*units));
        }
        return matrix;
    }

    private static float[] identity() {
        float[] matrix = new float[16];
        matrix[0]=matrix[5]=matrix[10]=matrix[15]=1;
        return matrix;
    }
    private static float[] translation(float x,float y,float z) {
        float[] matrix=identity(); matrix[12]=x; matrix[13]=y; matrix[14]=z; return matrix;
    }
    private static float[] rotationX(float degrees) {
        float[] matrix=identity(); double angle=Math.toRadians(degrees); float c=(float)Math.cos(angle),s=(float)Math.sin(angle);
        matrix[5]=c; matrix[6]=s; matrix[9]=-s; matrix[10]=c; return matrix;
    }
    private static float[] rotationY(float degrees) {
        float[] matrix=identity(); double angle=Math.toRadians(degrees); float c=(float)Math.cos(angle),s=(float)Math.sin(angle);
        matrix[0]=c; matrix[2]=-s; matrix[8]=s; matrix[10]=c; return matrix;
    }
    private static float[] rotationZ(float degrees) {
        float[] matrix=identity(); double angle=Math.toRadians(degrees); float c=(float)Math.cos(angle),s=(float)Math.sin(angle);
        matrix[0]=c; matrix[1]=s; matrix[4]=-s; matrix[5]=c; return matrix;
    }
    private static float[] multiply(float[] left,float[] right) {
        float[] result=new float[16];
        for(int column=0;column<4;column++) for(int row=0;row<4;row++)
            for(int k=0;k<4;k++) result[column*4+row]+=left[k*4+row]*right[column*4+k];
        return result;
    }
    private static float[] interpolateRigid(float[] from,float[] to,float weight) {
        float[] q=slerp(quaternion(from),quaternion(to),weight);
        float x=q[0],y=q[1],z=q[2],w=q[3];
        float[] result=identity();
        result[0]=1-2*(y*y+z*z); result[1]=2*(x*y+z*w); result[2]=2*(x*z-y*w);
        result[4]=2*(x*y-z*w); result[5]=1-2*(x*x+z*z); result[6]=2*(y*z+x*w);
        result[8]=2*(x*z+y*w); result[9]=2*(y*z-x*w); result[10]=1-2*(x*x+y*y);
        result[12]=from[12]+(to[12]-from[12])*weight;
        result[13]=from[13]+(to[13]-from[13])*weight;
        result[14]=from[14]+(to[14]-from[14])*weight;
        return result;
    }
    private static float[] quaternion(float[] matrix) {
        float trace=matrix[0]+matrix[5]+matrix[10];
        float x,y,z,w;
        if(trace>0) {
            float s=(float)Math.sqrt(trace+1)*2; w=.25f*s;
            x=(matrix[6]-matrix[9])/s; y=(matrix[8]-matrix[2])/s; z=(matrix[1]-matrix[4])/s;
        } else if(matrix[0]>matrix[5] && matrix[0]>matrix[10]) {
            float s=(float)Math.sqrt(1+matrix[0]-matrix[5]-matrix[10])*2;
            w=(matrix[6]-matrix[9])/s; x=.25f*s; y=(matrix[4]+matrix[1])/s; z=(matrix[8]+matrix[2])/s;
        } else if(matrix[5]>matrix[10]) {
            float s=(float)Math.sqrt(1+matrix[5]-matrix[0]-matrix[10])*2;
            w=(matrix[8]-matrix[2])/s; x=(matrix[4]+matrix[1])/s; y=.25f*s; z=(matrix[9]+matrix[6])/s;
        } else {
            float s=(float)Math.sqrt(1+matrix[10]-matrix[0]-matrix[5])*2;
            w=(matrix[1]-matrix[4])/s; x=(matrix[8]+matrix[2])/s; y=(matrix[9]+matrix[6])/s; z=.25f*s;
        }
        return new float[]{x,y,z,w};
    }
    private static float[] slerp(float[] from,float[] to,float weight) {
        float dot=from[0]*to[0]+from[1]*to[1]+from[2]*to[2]+from[3]*to[3];
        if(dot<0) { dot=-dot; to=new float[]{-to[0],-to[1],-to[2],-to[3]}; }
        if(dot>.9995f) {
            float[] result=new float[4]; float length=0;
            for(int i=0;i<4;i++){result[i]=from[i]+(to[i]-from[i])*weight;length+=result[i]*result[i];}
            length=(float)Math.sqrt(length); for(int i=0;i<4;i++)result[i]/=length; return result;
        }
        double theta=Math.acos(Math.max(-1,Math.min(1,dot))),sin=Math.sin(theta);
        float a=(float)(Math.sin((1-weight)*theta)/sin),b=(float)(Math.sin(weight*theta)/sin);
        return new float[]{from[0]*a+to[0]*b,from[1]*a+to[1]*b,from[2]*a+to[2]*b,from[3]*a+to[3]*b};
    }
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

    /** Exported TaCZ bones compose rest ZYX with animation ZYX, rather than adding Euler angles. */
    public static void applyBedrock(float[] origin, float[] rest,
                                   handmadeguns.client.render.HMGGunParts_Motion_PosAndRotation pose, float units) {
        GL11.glTranslatef((origin[0] + (pose == null ? 0 : pose.posX))*units,
                (origin[1] + (pose == null ? 0 : pose.posY))*units,
                (origin[2] + (pose == null ? 0 : pose.posZ))*units);
        GL11.glRotatef(rest[2],0,0,1); GL11.glRotatef(rest[1],0,1,0); GL11.glRotatef(rest[0],1,0,0);
        if (pose != null) {
            GL11.glRotatef(pose.rotationZ,0,0,1); GL11.glRotatef(pose.rotationY,0,1,0); GL11.glRotatef(pose.rotationX,1,0,0);
            GL11.glScalef(pose.scaleX,pose.scaleY,pose.scaleZ);
        }
    }
    /** TaCZ uses a whole-player hand-render frame, not a shoulder-centered mesh.
     * Vanilla 1.7's right/left shoulders are (-/+5,2,0) pixels; their wrist
     * centers are (-/+6,12,0). Rz(180) maps those to (+/-6,-12,0) in this
     * locator frame. Keep ModelRenderer's shoulder translation: cancelling it
     * or treating the locator origin as the wrist would displace both hands.
     * Left-arm mirror changes box winding/UVs, not this proper rotation.
     * The outer presentationFrame applies to the locator and gun together.
     */
    public static void playerArmFrame() {
        GL11.glRotatef(180,0,0,1);
    }
}
