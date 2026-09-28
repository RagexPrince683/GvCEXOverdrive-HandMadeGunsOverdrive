package handmadeguns.client.modelLoader.obj_modelloaderMod.obj;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import handmadeguns.HandmadeGunsCore;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.util.ArrayList;

/** VBO representation of one OBJ group/object part. */
@SideOnly(Side.CLIENT)
final class HMGVboMeshGroup {
    private static final int FLOATS_PER_VERTEX = 8;
    private static final int BYTES_PER_FLOAT = 4;
    private static final int STRIDE_BYTES = FLOATS_PER_VERTEX * BYTES_PER_FLOAT;
    private static final long POSITION_OFFSET = 0L;
    private static final long NORMAL_OFFSET = 3L * BYTES_PER_FLOAT;
    private static final long UV_OFFSET = 6L * BYTES_PER_FLOAT;

    private final String key;
    private int bufferId;
    private int vertexCount;
    private int glDrawingMode;
    private boolean compileFailed;
    private boolean loggedFallback;

    HMGVboMeshGroup(String key) {
        this.key = key;
    }

    boolean isCompiled() {
        return bufferId != 0;
    }

    boolean hasFailed() {
        return compileFailed;
    }

    boolean compile(ArrayList<HMGFace> faces, int drawingMode) {
        if (compileFailed || bufferId != 0) {
            return bufferId != 0;
        }
        if (faces == null || faces.isEmpty() || drawingMode == -1) {
            compileFailed = true;
            logFallback("empty or unknown OBJ group");
            return false;
        }

        try {
            int vertices = 0;
            for (HMGFace face : faces) {
                if (face == null || face.vertices == null || face.vertices.length == 0) {
                    compileFailed = true;
                    logFallback("invalid face data");
                    return false;
                }
                vertices += face.vertices.length;
            }

            FloatBuffer data = BufferUtils.createFloatBuffer(vertices * FLOATS_PER_VERTEX);
            for (HMGFace face : faces) {
                putFace(data, face);
            }
            data.flip();

            bufferId = GL15.glGenBuffers();
            if (bufferId == 0) {
                compileFailed = true;
                logFallback("glGenBuffers returned 0");
                return false;
            }

            int previousArrayBuffer = HMGVboModelCache.getArrayBufferBinding();
            try {
                HMGVboModelCache.bindArrayBuffer(bufferId);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
            } finally {
                HMGVboModelCache.bindArrayBuffer(previousArrayBuffer);
            }

            vertexCount = vertices;
            glDrawingMode = drawingMode;
            HMGVboModelCache.register(this);
            System.out.println("HandmadeGuns-compiled OBJ VBO group " + key + " (" + vertexCount + " vertices)");
            return true;
        } catch (Throwable t) {
            release();
            compileFailed = true;
            logFallback(t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
    }

    void render() {
        if (bufferId == 0 || vertexCount <= 0) {
            return;
        }

        int previousMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        int previousArrayBuffer = HMGVboModelCache.getArrayBufferBinding();
        boolean clientStatePushed = false;
        boolean textureMatrixPushed = false;
        try {
            // Vertex-array enables, pointers and VBO bindings belong to the caller.
            // In particular, Celeritas keeps its chunk-build buffer bound while
            // invoking main-thread render work.  Resetting that binding to zero
            // after every gun part detached its raw chunk buffer and made the next
            // copyRawBuffer call fail.  Save the complete client-array state rather
            // than assuming Minecraft entered with all arrays disabled.
            GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
            clientStatePushed = true;

            if (HandmadeGunsCore.textureOffsetU != 0.0F || HandmadeGunsCore.textureOffsetV != 0.0F) {
                GL11.glMatrixMode(GL11.GL_TEXTURE);
                GL11.glPushMatrix();
                GL11.glTranslatef(HandmadeGunsCore.textureOffsetU, HandmadeGunsCore.textureOffsetV, 0.0F);
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                textureMatrixPushed = true;
            }

            HMGVboModelCache.bindArrayBuffer(bufferId);
            // Meshes supply position, normals and base UVs only. Inherited color or
            // lightmap arrays would read the caller's chunk data instead of world light.
            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glVertexPointer(3, GL11.GL_FLOAT, STRIDE_BYTES, POSITION_OFFSET);
            GL11.glNormalPointer(GL11.GL_FLOAT, STRIDE_BYTES, NORMAL_OFFSET);
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, STRIDE_BYTES, UV_OFFSET);
            GL11.glDrawArrays(glDrawingMode, 0, vertexCount);
        } finally {
            if (clientStatePushed) {
                GL11.glPopClientAttrib();
            }
            // GL_CLIENT_VERTEX_ARRAY_BIT includes this binding in OpenGL 1.5.
            // Restore it explicitly as well for fixed-function compatibility
            // layers which only emulate the array enable/pointer portion.
            HMGVboModelCache.bindArrayBuffer(previousArrayBuffer);
            if (textureMatrixPushed) {
                GL11.glMatrixMode(GL11.GL_TEXTURE);
                GL11.glPopMatrix();
            }
            GL11.glMatrixMode(previousMatrixMode);
        }
    }

    boolean release() {
        if (bufferId == 0) {
            return false;
        }
        try {
            HMGVboModelCache.deleteBuffer(bufferId);
        } catch (Throwable ignored) {
            // Context may already be gone during shutdown.  Forget the id to avoid reusing it.
        }
        bufferId = 0;
        vertexCount = 0;
        HMGVboModelCache.unregister(this);
        return true;
    }

    private void putFace(FloatBuffer data, HMGFace face) {
        HMGVertex faceNormal = face.faceNormal != null ? face.faceNormal : face.calculateFaceNormal();
        float averageU = 0.0F;
        float averageV = 0.0F;
        boolean hasUv = face.HMGTextureCoordinates != null && face.HMGTextureCoordinates.length > 0;

        if (hasUv) {
            for (int i = 0; i < face.HMGTextureCoordinates.length; i++) {
                averageU += face.HMGTextureCoordinates[i].u;
                averageV += face.HMGTextureCoordinates[i].v;
            }
            averageU /= face.HMGTextureCoordinates.length;
            averageV /= face.HMGTextureCoordinates.length;
        }

        for (int i = 0; i < face.vertices.length; i++) {
            HMGVertex vertex = face.vertices[i];
            HMGVertex normal = faceNormal;
            if (face.HMGVertexNormals != null && i < face.HMGVertexNormals.length && face.HMGVertexNormals[i] != null) {
                normal = face.HMGVertexNormals[i];
            }

            float u = 0.0F;
            float v = 0.0F;
            if (hasUv && i < face.HMGTextureCoordinates.length && face.HMGTextureCoordinates[i] != null) {
                HMGTextureCoordinate tex = face.HMGTextureCoordinates[i];
                float offsetU = tex.u > averageU ? -0.0005F : 0.0005F;
                float offsetV = tex.v > averageV ? -0.0005F : 0.0005F;
                u = tex.u + offsetU;
                v = tex.v + offsetV;
            }

            data.put(vertex.x).put(vertex.y).put(vertex.z);
            data.put(normal.x).put(normal.y).put(normal.z);
            data.put(u).put(v);
        }
    }

    private void logFallback(String reason) {
        if (!loggedFallback) {
            System.out.println("HandmadeGuns-OBJ VBO fallback for " + key + ": " + reason);
            loggedFallback = true;
        }
    }
}
