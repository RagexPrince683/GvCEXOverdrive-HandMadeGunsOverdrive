package hmggvcmob.military.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public enum FactionInsignia {
    DEFENSE(0x74996D, 0xECDCB5, new String[]{
            "..#######..",
            ".#aaaaaaa#.",
            "#aaabbbaaa#",
            "#aaabbbaaa#",
            "#aabbbbbaa#",
            "#aaabbbaaa#",
            "#aaabbbaaa#",
            ".#aaaaaaa#.",
            "..#aaaaa#..",
            "...#aaa#...",
            "....###...."
    }),
    INSURGENTS(0xB85C4E, 0xF2D39C, new String[]{
            ".....#.....",
            "....#a#....",
            "....#a#....",
            "...#aba#...",
            "...#bba#...",
            "..#abbaa#..",
            "..#aabba#..",
            ".#aaabaaa#.",
            ".#abbbbaa#.",
            "#aaaaaaaaa#",
            "###########"
    }),
    COMPANY(0xD3AB63, 0xF6E0AC, new String[]{
            ".....#.....",
            "....#a#....",
            "...#aba#...",
            "..#abbaa#..",
            ".#abbabba#.",
            "#aabbabbaa#",
            ".#abbabba#.",
            "..#ababa#..",
            "...#aba#...",
            "....#a#....",
            ".....#....."
    }),
    INDEPENDENT(0x899CA8, 0xEEEBDD, new String[]{
            "..#######..",
            ".#aaaaaaa#.",
            "#aabaaabaa#",
            "#aabaaabaa#",
            "#aabbbbbaa#",
            "#aabaaabaa#",
            "#aabaaabaa#",
            ".#aaaaaaa#.",
            "..#######.."
    });

    private static final int OUTLINE = 0x272B22;
    private final int color, accent;
    private final String[] pixels;

    FactionInsignia(int color, int accent, String[] pixels) {
        this.color = color;
        this.accent = accent;
        this.pixels = pixels;
    }

    public static FactionInsignia of(String faction) {
        if (faction == null) return null;
        switch (faction) {
            case "union": return DEFENSE;
            case "allies": return INSURGENTS;
            case "pmc": return COMPANY;
            default: return INDEPENDENT;
        }
    }

    public void draw(int x, int y) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        Tessellator mesh = Tessellator.instance;
        mesh.startDrawingQuads();
        for (int row = 0; row < pixels.length; row++) for (int col = 0; col < pixels[row].length(); col++) {
            char pixel = pixels[row].charAt(col);
            if (pixel == '.') continue;
            mesh.setColorOpaque_I(pixel == 'a' ? color : pixel == 'b' ? accent : OUTLINE);
            int left = x + col, top = y + row;
            mesh.addVertex(left, top + 1, 0);
            mesh.addVertex(left + 1, top + 1, 0);
            mesh.addVertex(left + 1, top, 0);
            mesh.addVertex(left, top, 0);
        }
        mesh.draw();
        GL11.glPopAttrib();
    }

    public void drawBand(ModelRenderer arm, boolean armored) {
        float pixel = 1F / 16F, padding = armored ? .62F : .08F;
        float left = (-1 - padding) * pixel, right = (3 + padding) * pixel, depth = (2 + padding) * pixel;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        try {
            arm.postRender(pixel);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_CULL_FACE);
            Tessellator mesh = Tessellator.instance;
            mesh.startDrawingQuads();
            strip(mesh, left, right, depth, -.4F * pixel, .5F * pixel, accent);
            strip(mesh, left, right, depth, .5F * pixel, 3.6F * pixel, color);
            strip(mesh, left, right, depth, 3.6F * pixel, 4.15F * pixel, OUTLINE);
            mesh.draw();
        } finally {
            GL11.glPopAttrib();
            GL11.glPopMatrix();
        }
    }

    private static void strip(Tessellator mesh, float left, float right, float z, float top, float bottom, int color) {
        mesh.setColorOpaque_I(color);
        face(mesh, left, -z, right, -z, top, bottom, 0, -1);
        face(mesh, right, -z, right, z, top, bottom, 1, 0);
        face(mesh, right, z, left, z, top, bottom, 0, 1);
        face(mesh, left, z, left, -z, top, bottom, -1, 0);
    }

    private static void face(Tessellator mesh, float x1, float z1, float x2, float z2, float top, float bottom, float nx, float nz) {
        mesh.setNormal(nx, 0, nz);
        mesh.addVertex(x1, top, z1);
        mesh.addVertex(x1, bottom, z1);
        mesh.addVertex(x2, bottom, z2);
        mesh.addVertex(x2, top, z2);
    }
}
