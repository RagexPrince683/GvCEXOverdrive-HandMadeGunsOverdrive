package handmadeguns.client.modelLoader.blockbench;

import com.google.gson.*;
import handmadeguns.pack.HMGPackAssetResolver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Reads the audited cube-only TaCZ Bedrock geometry subset into the native Blockbench runtime model. */
public final class BedrockGeometryLoader {
    private static final String[] SIDES = {"east", "west", "up", "down", "south", "north"};
    // FaceUVsItem.getFace maps Java directions to the exported Bedrock face names.
    private static final String[] PER_FACE_SIDES = {"west", "east", "down", "up", "south", "north"};
    // TaCZ BedrockCubePerFace vertex order in Java cube coordinates (Y points down).
    private static final int[][] CORNERS = {{5,1,3,7},{0,4,6,2},{3,2,6,7},{5,4,0,1},{4,5,7,6},{1,0,2,3}};

    public static BlockbenchProject load(File gunFile, String geometryReference,
                                         String textureReference) throws IOException {
        File root = handmadeguns.HandmadeGunsCore.gunPackRoot(gunFile);
        try {
            HMGPackAssetResolver resolver = new HMGPackAssetResolver(root);
            File geometry = resolver.resolve(HMGPackAssetResolver.Type.BEDROCK_MODEL, geometryReference);
            File texture = resolver.resolve(HMGPackAssetResolver.Type.MODEL_TEXTURE, textureReference);
            return load(geometry, texture);
        } catch (IOException | RuntimeException failure) {
            throw new IOException("[HMG Bedrock] " + gunFile + " | " + geometryReference + " | "
                    + failure.getMessage(), failure);
        }
    }

    public static BlockbenchProject load(File geometry, File texture) throws IOException {
        BufferedImage image = ImageIO.read(texture);
        if (image == null) throw new IOException("Cannot decode Bedrock texture " + texture);
        try (Reader reader = new InputStreamReader(new FileInputStream(geometry), StandardCharsets.UTF_8)) {
            return parse(reader, image, geometry, texture.getName());
        }
    }

    public static BlockbenchProject parse(Reader reader, BufferedImage image, File source,
                                          String textureName) throws IOException {
        try {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            String version = string(root, "format_version", "");
            if (!"1.12.0".equals(version) && !"1.21.0".equals(version))
                throw new IOException("Unsupported Bedrock geometry version " + version
                        + "; audited TaCZ geometry uses 1.12.0 or 1.21.0");
            JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
            if (geometries == null || geometries.size() != 1)
                throw new IOException("Expected exactly one minecraft:geometry entry");
            JsonObject geometry = geometries.get(0).getAsJsonObject();
            rejectAdvanced(geometry, "geometry");
            JsonObject description = geometry.getAsJsonObject("description");
            if (description == null) throw new IOException("Missing Bedrock geometry description");
            double textureWidth = positive(description, "texture_width");
            double textureHeight = positive(description, "texture_height");
            JsonArray bones = geometry.getAsJsonArray("bones");
            if (bones == null || bones.size() == 0) throw new IOException("Missing Bedrock bones");

            BlockbenchProject project = BlockbenchProject.geometry(source);
            project.textures.add(new BlockbenchProject.Texture("0", textureName, image,
                    textureWidth, textureHeight));
            LinkedHashMap<String, JsonObject> definitions = new LinkedHashMap<String, JsonObject>();
            for (JsonElement value : bones) {
                JsonObject bone = value.getAsJsonObject();
                rejectAdvanced(bone, "bone");
                if (!bone.has("name") || bone.get("name").isJsonNull())
                    throw new IOException("Bedrock bone without name");
                String name = string(bone, "name", "");
                if (definitions.put(name, bone) != null)
                    throw new IOException("Duplicate Bedrock bone name " + name);
            }
            for (Map.Entry<String, JsonObject> entry : definitions.entrySet()) {
                String parent = string(entry.getValue(), "parent", "");
                if (!parent.isEmpty() && !definitions.containsKey(parent))
                    throw new IOException("Unknown parent '" + parent + "' for bone " + entry.getKey());
            }
            Set<String> visiting = new LinkedHashSet<String>();
            for (String name : definitions.keySet()) build(name, definitions, project, visiting, 0);
            return project;
        } catch (JsonParseException | IllegalStateException | IllegalArgumentException failure) {
            throw new IOException(source + ": invalid Bedrock geometry JSON: " + failure.getMessage(), failure);
        }
    }

    private static BlockbenchProject.Node build(String name, Map<String, JsonObject> definitions,
                                                BlockbenchProject project, Set<String> visiting,
                                                int depth) throws IOException {
        BlockbenchProject.Node existing = project.nodes.get(name);
        if (existing != null) return existing;
        if (depth > 128) throw new IOException("Bedrock hierarchy exceeds 128 levels at " + name);
        if (!visiting.add(name)) throw new IOException("Cyclic Bedrock hierarchy: " + visiting + " -> " + name);
        JsonObject json = definitions.get(name);
        String parentName = string(json, "parent", "");
        BlockbenchProject.Node parent = parentName.isEmpty() ? null
                : build(parentName, definitions, project, visiting, depth + 1);
        BlockbenchProject.Node node = new BlockbenchProject.Node(name, name,
                projectPosition(vector(json, "pivot", 3, 0)), projectRotation(vector(json, "rotation", 3, 0)), parent,
                baseVisible(name, parent));
        if (name.isEmpty()) project.warnings.add("Unnamed Bedrock bone retained with an empty part identifier");
        project.nodes.put(name, node);
        if (parent == null) project.roots.add(node); else parent.children.add(node);
        JsonArray cubes = json.getAsJsonArray("cubes");
        if (cubes != null) for (JsonElement value : cubes) cube(value.getAsJsonObject(), node,
                bool(json, "mirror", false), project);
        visiting.remove(name);
        if ("camera".equals(name) || "constraint".equals(name))
            project.warnings.add("TaCZ camera/constraint bones are retained as parts; their animated camera effects are not applied");
        return node;
    }

    /** TaCZ's ordinary, attachment-free gun state. Dynamic attachment selection remains HMG-owned. */
    private static boolean baseVisible(String name, BlockbenchProject.Node parent) {
        if (parent != null && "attachment_adapter".equals(parent.name)) return false;
        return !("mount".equals(name) || "sight_folded".equals(name)
                || "mag_extended_1".equals(name) || "mag_extended_2".equals(name)
                || "mag_extended_3".equals(name) || "additional_magazine".equals(name)
                || "handguard_tactical".equals(name) || "muzzle_pos".equals(name)
                || "stock_pos".equals(name) || "grip_pos".equals(name)
                || "laser_pos".equals(name) || "extended_mag_pos".equals(name));
    }

    private static void cube(JsonObject cube, BlockbenchProject.Node node, boolean boneMirror,
                             BlockbenchProject project) throws IOException {
        rejectAdvanced(cube, "cube");
        double[] from = vector(cube, "origin", 3, Double.NaN);
        double[] size = vector(cube, "size", 3, Double.NaN);
        double[] to = new double[3];
        for (int i=0;i<3;i++) to[i] = from[i] + size[i];
        double[] rotation = vector(cube, "rotation", 3, 0);
        double[] pivot = vector(cube, "pivot", 3, 0);
        boolean rotates = rotation[0] != 0 || rotation[1] != 0 || rotation[2] != 0;
        if (rotates && !cube.has("pivot")) throw new IOException("Rotated Bedrock cube without pivot in " + node.name);
        double inflate = number(cube, "inflate", 0);
        boolean mirror = cube.has("mirror") ? bool(cube, "mirror", false) : boneMirror;
        JsonElement uv = cube.get("uv");
        if (uv == null || uv.isJsonNull()) throw new IOException("Bedrock cube without UV in " + node.name);
        if (uv.isJsonArray()) boxFaces(node, from, to, pivot, rotation, inflate,
                vector(uv.getAsJsonArray(), 2, Double.NaN), size, mirror, project);
        else if (uv.isJsonObject()) perFace(node, from, to, pivot, rotation, inflate,
                uv.getAsJsonObject(), mirror, project);
        else throw new IOException("Bedrock cube UV must be an array or object in " + node.name);
        project.recordCube();
    }

    private static void perFace(BlockbenchProject.Node node, double[] from, double[] to,
                                double[] pivot, double[] rotation, double inflate,
                                JsonObject faces, boolean mirror, BlockbenchProject project) throws IOException {
        // TaCZ's reference renderer intentionally does not apply cube/bone mirror to per-face UV cubes.
        if (mirror) project.warnings.add("Per-face cube mirror follows TaCZ behavior and is not applied: " + node.name);
        for (int side=0;side<SIDES.length;side++) {
            JsonObject face = faces.getAsJsonObject(PER_FACE_SIDES[side]);
            if (face == null) continue;
            double[] start = vector(face, "uv", 2, Double.NaN);
            double[] extent = vector(face, "uv_size", 2, Double.NaN);
            addFace(node, side, from, to, pivot, rotation, inflate,
                    new double[]{start[0], start[1], start[0] + extent[0], start[1] + extent[1]}, false, project);
        }
    }

    private static void boxFaces(BlockbenchProject.Node node, double[] from, double[] to,
                                 double[] pivot, double[] rotation, double inflate,
                                 double[] offset, double[] size, boolean mirror,
                                 BlockbenchProject project) {
        // TaCZ's box-UV renderer truncates dimensions to integers, including negative dimensions.
        double x = (int)size[0], y = (int)size[1], z = (int)size[2];
        double[][] boxes = {{z+x,z,2*z+x,z+y},{0,z,z,z+y},{z+x,z,z+2*x,0},{z,0,z+x,z},
                {2*z+x,z,2*z+2*x,z+y},{z,z,z+x,z+y}};
        for (int side=0;side<SIDES.length;side++) {
            double[] uv = boxes[side];
            addFace(node, side, from, to, pivot, rotation, inflate,
                    new double[]{uv[0]+offset[0],uv[1]+offset[1],uv[2]+offset[0],uv[3]+offset[1]}, mirror, project);
        }
    }

    private static void addFace(BlockbenchProject.Node node, int side, double[] from, double[] to,
                                double[] pivot, double[] rotation, double inflate,
                                double[] uv, boolean mirror, BlockbenchProject project) {
        BlockbenchProject.Texture texture = project.textures.get(0);
        float[][] vertices = new float[4][], coords = new float[4][2];
        double[][] uvCorners = {{uv[2],uv[1]},{uv[0],uv[1]},{uv[0],uv[3]},{uv[2],uv[3]}};
        for (int i=0;i<4;i++) {
            // Runtime reverses project rings. Box mirror swaps X bounds and reverses winding.
            int index = mirror ? i : 3-i;
            int corner = CORNERS[side][index] ^ 2 ^ (mirror ? 1 : 0);
            double[] point = new double[3];
            for (int axis=0;axis<3;axis++)
                point[axis] = (corner & (1 << axis)) == 0 ? from[axis]-inflate : to[axis]+inflate;
            vertices[i] = BlockbenchTransform.point(BlockbenchTransform.rotate(projectPosition(point),
                    projectPosition(pivot), projectRotation(rotation)), node.origin);
            // Reorder the vertex/UV pair together. Y-bound conversion changes positions,
            // not the UV assigned by BedrockPolygon to that Java corner.
            int uvIndex = index;
            coords[i][0] = (float)(uvCorners[uvIndex][0] / texture.width);
            coords[i][1] = (float)(uvCorners[uvIndex][1] / texture.height);
        }
        node.faces.add(new BlockbenchProject.Face(0, vertices, coords));
    }

    // Exported Bedrock and native project coordinates differ. Convert only at this boundary.
    private static double[] projectPosition(double[] value) {
        // TaCZ roots use ModelRenderer's 24-pixel Y origin. Applying this to every
        // absolute point preserves child and cube-local differences.
        return new double[]{-value[0], value[1] - 24, value[2]};
    }
    private static double[] projectRotation(double[] value) {
        return new double[]{-value[0], -value[1], value[2]};
    }

    private static void rejectAdvanced(JsonObject object, String context) throws IOException {
        for (String field : new String[]{"poly_mesh", "texture_meshes", "locators", "binding", "bedrock_binding"})
            if (object.has(field) && !object.get(field).isJsonNull())
                throw new IOException("Unsupported Bedrock " + field + " in " + context);
    }

    private static String string(JsonObject object, String key, String def) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : def;
    }
    private static boolean bool(JsonObject object, String key, boolean def) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsBoolean() : def;
    }
    private static double positive(JsonObject object, String key) throws IOException {
        double value = number(object, key, Double.NaN);
        if (value <= 0) throw new IOException("Invalid Bedrock " + key);
        return value;
    }
    private static double number(JsonObject object, String key, double def) throws IOException {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            if (Double.isNaN(def)) throw new IOException("Missing Bedrock " + key);
            return def;
        }
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value)) throw new IOException("Non-finite Bedrock " + key);
        return value;
    }
    private static double[] vector(JsonObject object, String key, int size, double def) throws IOException {
        JsonArray array = object.has(key) && !object.get(key).isJsonNull() ? object.getAsJsonArray(key) : null;
        if (array == null) {
            if (Double.isNaN(def)) throw new IOException("Missing Bedrock " + key);
            double[] values = new double[size]; Arrays.fill(values, def); return values;
        }
        return vector(array, size, def);
    }
    private static double[] vector(JsonArray array, int size, double def) throws IOException {
        if (array.size() != size) throw new IOException("Expected " + size + " Bedrock vector components");
        double[] values = new double[size];
        for (int i=0;i<size;i++) {
            values[i] = array.get(i).getAsDouble();
            if (!Double.isFinite(values[i])) throw new IOException("Non-finite Bedrock vector");
        }
        return values;
    }

    /** Pack-author audit entry point; optional animation files are parsed and checked against the bone set. */
    public static void main(String[] args) throws IOException {
        if (args.length < 2) throw new IOException("Usage: BedrockGeometryLoader <model_geo.json> <texture.png> [animation.json ...]");
        BlockbenchProject project = load(new File(args[0]), new File(args[1]));
        project.audit(System.out);
        BedrockAnimationLoader animationLoader = new BedrockAnimationLoader(true);
        for (int i=2;i<args.length;i++) {
            handmadeguns.animation.AnimationDefinition definition = animationLoader.load(new File(args[i]));
            Set<String> ignored = new LinkedHashSet<String>();
            definition = BedrockAnimationLoader.retainKnownParts(definition, project.nodes.keySet(), ignored);
            definition.validateParts(project.nodes.keySet());
            System.out.println("animation source " + args[i] + " clips=" + definition.clips.size()
                    + " validated; ignored optional parts=" + ignored);
        }
    }

    private BedrockGeometryLoader() { }
}
