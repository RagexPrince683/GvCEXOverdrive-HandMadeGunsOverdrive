package hmggvcmob.render;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import hmggvcmob.military.BlockMilitaryWire;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import java.util.ArrayList;
import java.util.List;

/** A galvanized coil baked into the chunk mesh. */
public final class RenderMilitaryWire implements ISimpleBlockRenderingHandler {
    private static final float[][][][] COIL = {mesh(false), mesh(true)};
    private final int renderId = RenderingRegistry.getNextAvailableRenderId();

    public static void register() {
        RenderMilitaryWire renderer = new RenderMilitaryWire();
        BlockMilitaryWire.setRenderId(renderer.renderId);
        RenderingRegistry.registerBlockHandler(renderer);
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int id, RenderBlocks renderer) {
        boolean acrossX = world.getBlock(x - 1, y, z) == block || world.getBlock(x + 1, y, z) == block;
        boolean acrossZ = world.getBlock(x, y, z - 1) == block || world.getBlock(x, y, z + 1) == block;
        Tessellator tess = Tessellator.instance;
        tess.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        IIcon icon = renderer.hasOverrideBlockTexture() ? renderer.overrideBlockTexture : block.getIcon(0, 0);
        if (acrossX || !acrossZ) emit(tess, icon, x, y, z, COIL[0], false);
        if (acrossZ) emit(tess, icon, x, y, z, COIL[1], false);
        return true;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int id, RenderBlocks renderer) {
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        emit(tess, block.getIcon(0, metadata), -.5, -.5, -.5, COIL[0], true);
        tess.draw();
    }

    private static void emit(Tessellator tess, IIcon icon, double x, double y, double z, float[][][] faces,
                             boolean inventory) {
        double minU = icon.getInterpolatedU(1);
        double maxU = icon.getInterpolatedU(15);
        double minV = icon.getInterpolatedV(1);
        double maxV = icon.getInterpolatedV(15);
        for (int i = 0; i < faces.length; i++) {
            float shade = .68F + (i & 3) * .10F;
            tess.setColorOpaque_F(shade, shade, shade);
            if (inventory) {
                float[] a = faces[i][0], b = faces[i][1], c = faces[i][2];
                float abY = b[1] - a[1], abZ = b[2] - a[2];
                float acX = c[0] - a[0], acY = c[1] - a[1], acZ = c[2] - a[2];
                float nx = abY * acZ - abZ * acY, ny = abZ * acX, nz = -abY * acX;
                float length = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
                tess.setNormal(nx / length, ny / length, nz / length);
            }
            for (int vertex = 0; vertex < 4; vertex++) {
                float[] p = faces[i][vertex];
                tess.addVertexWithUV(x + p[0], y + p[1], z + p[2],
                        vertex < 2 ? minU : maxU, (vertex == 0 || vertex == 3) ? minV : maxV);
            }
        }
    }

    private static float[][][] mesh(boolean rotate) {
        List<float[][]> faces = new ArrayList<>();
        float[] tail = point(0);
        for (int step = 1; step <= 24; step++) {
            float[] head = point(step);
            strand(faces, tail, head, .021F);
            if (step % 4 == 2) {
                float dy = (head[1] - .42F) * .24F;
                float dz = (head[2] - .5F) * .24F;
                strand(faces, new float[]{head[0] - .055F, head[1] - dy, head[2] - dz},
                        new float[]{head[0] + .055F, head[1] + dy, head[2] + dz}, .014F);
            }
            tail = head;
        }
        for (float z : new float[]{.25F, .75F}) {
            strand(faces, new float[]{0, .16F, z}, new float[]{1, .16F, z}, .017F);
        }
        if (rotate) {
            for (float[][] face : faces) for (float[] vertex : face) {
                float x = vertex[0];
                vertex[0] = 1 - vertex[2];
                vertex[2] = x;
            }
        }
        return faces.toArray(new float[0][][]);
    }

    private static float[] point(int step) {
        double angle = step * Math.PI / 6 + Math.PI;
        return new float[]{step / 24F, .42F + .32F * (float)Math.cos(angle), .5F + .34F * (float)Math.sin(angle)};
    }

    private static void strand(List<float[][]> faces, float[] from, float[] to, float radius) {
        // Longitudinal members use a diamond cross section to keep diagonals legible at a distance.
        float[][] offsets = {{radius, 0}, {0, radius}, {-radius, 0}, {0, -radius}};
        for (int i = 0; i < 4; i++) {
            float[] a = offsets[i], b = offsets[(i + 1) & 3];
            faces.add(new float[][]{vertex(from, a), vertex(from, b), vertex(to, b), vertex(to, a)});
        }
    }

    private static float[] vertex(float[] p, float[] offset) {
        return new float[]{p[0], p[1] + offset[0], p[2] + offset[1]};
    }

    @Override public boolean shouldRender3DInInventory(int id) { return true; }
    @Override public int getRenderId() { return renderId; }
}
