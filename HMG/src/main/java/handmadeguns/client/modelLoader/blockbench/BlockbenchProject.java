package handmadeguns.client.modelLoader.blockbench;

import com.google.gson.*;
import handmadeguns.animation.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** CPU-only project reader. Independently implemented from the native project/preview semantics.
 * No project expressions, scripts or paths outside the pack are executed or resolved.
 */
public final class BlockbenchProject {
    public static final class Node {
        public final String uuid, name;
        public final double[] origin, rotation;
        public final boolean visible;
        public final Node parent;
        public final List<Node> children = new ArrayList<Node>();
        public final List<Face> faces = new ArrayList<Face>();
        Node(JsonObject json, Node parent) {
            uuid = string(json, "uuid", ""); name = string(json, "name", uuid);
            if (uuid.isEmpty()) throw new IllegalArgumentException("Bone without UUID");
            origin = vector(json, "origin", 0); rotation = vector(json, "rotation", 0);
            visible = bool(json, "visibility", true) && bool(json, "export", true);
            this.parent = parent;
        }
        Node(String uuid, String name, double[] origin, double[] rotation, Node parent) {
            if (uuid == null) throw new IllegalArgumentException("Bone without identifier");
            this.uuid = uuid; this.name = name == null ? uuid : name;
            this.origin = origin.clone(); this.rotation = rotation.clone();
            visible = true; this.parent = parent;
        }
    }
    public static final class Face {
        public final int texture;
        public final float[][] vertices;
        public final float[][] uv;
        Face(int texture, float[][] vertices, float[][] uv) { this.texture = texture; this.vertices = vertices; this.uv = uv; }
    }
    public static final class Texture {
        public final String uuid, name;
        public final BufferedImage image;
        public final double width, height;
        Texture(String uuid, String name, BufferedImage image, double width, double height) {
            this.uuid = uuid; this.name = name; this.image = image; this.width = width; this.height = height;
        }
    }
    public final File file;
    public final boolean bedrock;
    public final Map<String, Node> nodes = new LinkedHashMap<String, Node>();
    public final List<Node> roots = new ArrayList<Node>();
    public final List<Texture> textures = new ArrayList<Texture>();
    public final Set<String> warnings = new LinkedHashSet<String>();
    public final AnimationDefinition animations;
    private final Map<String, JsonObject> groups = new LinkedHashMap<String, JsonObject>();
    private final Map<String, JsonObject> elements = new LinkedHashMap<String, JsonObject>();
    private final Set<String> attached = new HashSet<String>();
    private final boolean boxUV;
    private int cubes, cameras;

    public static File resolve(File root, File directory, String reference) throws IOException {
        File path = new File(directory, reference).getCanonicalFile();
        if (!path.toPath().startsWith(root.getCanonicalFile().toPath()) || path.equals(root.getCanonicalFile()))
            throw new IOException("Path must stay inside pack: " + reference);
        return path;
    }
    public static BlockbenchProject load(File gunFile, String reference) throws IOException {
        File root = handmadeguns.HandmadeGunsCore.gunPackRoot(gunFile);
        try {
            File project = new handmadeguns.pack.HMGPackAssetResolver(root)
                    .resolve(handmadeguns.pack.HMGPackAssetResolver.Type.BLOCKBENCH_MODEL, reference);
            return new BlockbenchProject(project, root);
        }
        catch (IOException | RuntimeException failure) {
            throw new IOException("[HMG Blockbench] " + gunFile + " | " + reference + " | " + failure.getMessage(),failure);
        }
    }
    /** Empty common-runtime project populated by an alternate, directly inspectable geometry source. */
    static BlockbenchProject geometry(File file) throws IOException {
        return new BlockbenchProject(file);
    }
    private BlockbenchProject(File file) throws IOException {
        bedrock = true;
        this.file = file.getCanonicalFile();
        boxUV = false;
        animations = null;
    }
    void recordCube() { cubes++; }
    public BlockbenchProject(File file, File packRoot) throws IOException {
        bedrock = false;
        this.file = file.getCanonicalFile();
        JsonObject json;
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            json = new JsonParser().parse(reader).getAsJsonObject();
        }
        JsonObject meta = json.getAsJsonObject("meta");
        if (meta == null) throw new IOException("Missing bbmodel meta");
        String version = string(meta, "format_version", "0");
        if (!version.startsWith("4.") && !version.startsWith("5.")) throw new IOException("Unsupported bbmodel version " + version);
        String format = string(meta, "model_format", "");
        if (!"bedrock".equals(format) && !"bedrock_old".equals(format) && !"free".equals(format))
            throw new IOException("Unsupported project model_format: " + format);
        if (!string(json,"multi_file_ruleset","").isEmpty()) throw new IOException("Multi-file projects are unsupported");
        boxUV = bool(meta, "box_uv", false);
        JsonObject resolution = json.getAsJsonObject("resolution");
        double width = number(resolution, "width", 16), height = number(resolution, "height", 16);
        for (JsonElement value : array(json, "textures")) {
            JsonObject texture = value.getAsJsonObject();
            String source = string(texture, "source", "");
            BufferedImage image;
            if (source.startsWith("data:image/png;base64,")) {
                byte[] bytes = Base64.getDecoder().decode(source.substring(source.indexOf(',') + 1));
                image = ImageIO.read(new ByteArrayInputStream(bytes));
            } else {
                String path = string(texture, "relative_path", "");
                if (path.isEmpty()) path = string(texture, "name", "");
                image = ImageIO.read(resolve(packRoot, file.getCanonicalFile().getParentFile(), path));
            }
            if (image == null) throw new IOException("Cannot decode texture " + string(texture, "name", ""));
            // These formats use project UV resolution; per-texture dimensions belong to other formats/multi-file rigs.
            double tw = width, th = height;
            if (tw <= 0 || th <= 0) throw new IOException("Invalid texture UV resolution");
            textures.add(new Texture(string(texture, "uuid", ""), string(texture, "name", ""), image, tw, th));
            if (bool(texture, "layers_enabled", false)) warnings.add("Texture layers use the saved composite PNG");
            if (number(texture, "frame_time", 1) != 1 || number(texture, "frame_count", 1) > 1)
                warnings.add("Animated texture playback is unsupported; saved image used");
        }
        for (JsonElement value : array(json, "elements")) {
            JsonObject element = value.getAsJsonObject();
            String id = string(element, "uuid", "");
            if (id.isEmpty() || elements.put(id, element) != null) throw new IOException("Missing/duplicate element UUID " + id);
        }
        for (JsonElement value : array(json, "groups")) {
            JsonObject group = value.getAsJsonObject();
            if (groups.put(string(group, "uuid", ""), group) != null) throw new IOException("Duplicate group UUID");
        }
        for (JsonElement value : array(json, "outliner")) readNode(value, null, 0);
        for (String uuid : elements.keySet()) if (!attached.contains(uuid))
            throw new IOException("Element missing from outliner: " + uuid);
        for (String uuid : groups.keySet()) if (!nodes.containsKey(uuid))
            throw new IOException("Group missing from outliner: " + uuid);
        animations = readAnimations(json);
        for (String field : new String[]{"animation_controllers", "display"})
            if (json.has(field) && !json.get(field).toString().equals("[]") && !json.get(field).toString().equals("{}"))
                warnings.add(field + " is editor/export metadata; HMG keeps its existing presentation/action controller");
    }
    private void readNode(JsonElement value, Node parent, int depth) throws IOException {
        if (depth > 128) throw new IOException("Outliner hierarchy exceeds 128 levels");
        if (value.isJsonPrimitive()) {
            String id = value.getAsString();
            JsonObject element = elements.get(id);
            if (element == null || !attached.add(id)) throw new IOException("Unknown/repeated element " + id);
            String type = string(element, "type", "cube");
            if ("camera".equals(type) || "locator".equals(type)) {
                cameras++; warnings.add("Camera/locator elements are editor helpers; no camera or gameplay transform is applied"); return;
            }
            if (!"cube".equals(type)) throw new IOException("Unsupported geometry type " + type + " at " + id);
            cubes++;
            if (parent == null) {
                JsonObject synthetic = new JsonObject(); synthetic.addProperty("uuid", id); synthetic.addProperty("name", string(element, "name", id));
                parent = new Node(synthetic, null); nodes.put(id, parent); roots.add(parent);
            }
            if (bool(element, "visibility", true) && bool(element, "export", true)) cube(element, parent);
            return;
        }
        JsonObject entry = value.getAsJsonObject();
        String uuid = string(entry, "uuid", "");
        JsonObject data = groups.containsKey(uuid) ? groups.get(uuid) : entry;
        Node node = new Node(data, parent);
        if (nodes.put(node.uuid, node) != null) throw new IOException("Repeated group UUID " + uuid);
        if (parent == null) roots.add(node); else parent.children.add(node);
        if (!string(data, "bedrock_binding", "").isEmpty()) throw new IOException("Unsupported bone binding: " + node.name);
        if (bool(data,"reset",false)) throw new IOException("Unsupported reset bone: " + node.name);
        if ("camera".equals(node.name) || "constraint".equals(node.name))
            warnings.add("TaCZ camera/constraint bones are retained as parts; their animated camera effects are not applied");
        for (JsonElement child : array(entry, "children")) readNode(child, node, depth + 1);
    }
    // Top-left, top-right, bottom-right, bottom-left, outward winding (Three BoxGeometry faces).
    private static final String[] SIDES = {"east", "west", "up", "down", "south", "north"};
    private static final int[][] CORNERS = {{7,3,1,5},{2,6,4,0},{2,3,7,6},{4,5,1,0},{6,7,5,4},{3,2,0,1}};
    private void cube(JsonObject cube, Node node) throws IOException {
        double[] from = vector(cube, "from", 0), to = vector(cube, "to", 0), origin = vector(cube, "origin", 0);
        double[] rotation = vector(cube, "rotation", 0);
        double inflate = number(cube, "inflate", 0);
        double[] stretch = vector(cube,"stretch",1);
        for (int i=0;i<3;i++) if (stretch[i] != 1) throw new IOException("Cube stretch is unsupported: " + string(cube,"uuid",""));
        if (bool(cube, "rescale", false)) throw new IOException("Cube rescale is unsupported: " + string(cube, "uuid", ""));
        JsonObject faces = cube.getAsJsonObject("faces");
        if (faces == null) throw new IOException("Cube without faces");
        boolean box = bool(cube, "box_uv", boxUV);
        double x = Math.floor(to[0] - from[0] + 1e-7), y = Math.floor(to[1] - from[1] + 1e-7), z = Math.floor(to[2] - from[2] + 1e-7);
        double[][] boxes = {{0,z,z,z+y},{z+x,z,2*z+x,z+y},{z+x,z,z,0},{z+2*x,0,z+x,z},{2*z+x,z,2*z+2*x,z+y},{z,z,z+x,z+y}};
        if (bool(cube, "mirror_uv", false)) {
            for (double[] uv : boxes) { double t=uv[0]; uv[0]=uv[2]; uv[2]=t; }
            double[] t=boxes[0]; boxes[0]=boxes[1]; boxes[1]=t;
        }
        JsonArray offset = array(cube, "uv_offset");
        for (int side = 0; side < SIDES.length; side++) {
            JsonObject face = faces.getAsJsonObject(SIDES[side]);
            if (face == null || !face.has("texture") || face.get("texture").isJsonNull()) continue;
            int texture = textureIndex(face.get("texture"));
            Texture tex = textures.get(texture);
            double[] uv = box ? boxes[side].clone() : components(array(face, "uv"), 4, 0);
            if (box) for (int i=0;i<4;i++) uv[i] += offset.size() == 2 ? offset.get(i%2).getAsDouble() : 0;
            int turns = (int)number(face, "rotation", 0);
            if (turns % 90 != 0) throw new IOException("Non-quarter-turn face UV rotation");
            turns = ((turns / 90) % 4 + 4) % 4;
            float[][] vertices = new float[4][], coords = new float[4][2];
            double[][] uvCorners = {{uv[0],uv[1]},{uv[2],uv[1]},{uv[2],uv[3]},{uv[0],uv[3]}};
            for (int i=0;i<4;i++) {
                int corner = CORNERS[side][i];
                double[] p = new double[3];
                for (int axis=0;axis<3;axis++) p[axis] = (corner & (1 << axis)) == 0 ? from[axis]-inflate : to[axis]+inflate;
                vertices[i] = BlockbenchTransform.point(BlockbenchTransform.rotate(p, origin, rotation), node.origin);
                double[] cornerUV = uvCorners[(i - turns + 4) % 4];
                coords[i][0] = (float)(cornerUV[0]/tex.width); coords[i][1] = (float)(cornerUV[1]/tex.height);
            }
            node.faces.add(new Face(texture, vertices, coords));
        }
    }
    private int textureIndex(JsonElement ref) throws IOException {
        String id = ref.getAsString();
        for (int i=0;i<textures.size();i++) if (textures.get(i).uuid.equals(id)) return i;
        try { int index = Integer.parseInt(id); if (index >= 0 && index < textures.size()) return index; }
        catch (NumberFormatException ignored) { }
        throw new IOException("Unknown texture reference " + id);
    }
    private AnimationDefinition readAnimations(JsonObject project) throws IOException {
        Map<String, AnimationClip> clips = new LinkedHashMap<String, AnimationClip>();
        for (JsonElement value : array(project, "animations")) {
            JsonObject animation = value.getAsJsonObject();
            String name = string(animation, "name", "");
            double duration = number(animation, "length", 0);
            if (bool(animation,"override",false)) warnings.add("Animation override flag uses Phase A layer precedence, not Blockbench preview-stack resets");
            String loopName = string(animation, "loop", "once");
            AnimationClip.Loop loop;
            try { loop = AnimationClip.Loop.valueOf(loopName.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException failure) { throw new IOException("Unknown loop mode " + loopName); }
            for (String property : new String[]{"anim_time_update", "blend_weight", "start_delay", "loop_delay"})
                if (!string(animation, property, "").isEmpty()) throw new IOException("Unsupported animation " + property + " in " + name);
            Map<String, AnimationTrack> tracks = new LinkedHashMap<String, AnimationTrack>();
            List<AnimationEvent> events = new ArrayList<AnimationEvent>();
            JsonObject animators = animation.getAsJsonObject("animators");
            if (animators != null) for (Map.Entry<String, JsonElement> entry : animators.entrySet()) {
                JsonObject animator = entry.getValue().getAsJsonObject();
                if (bool(animator, "rotation_global", false) || bool(animator, "quaternion_interpolation", false))
                    throw new IOException("Unsupported global/quaternion interpolation: " + name + "/" + entry.getKey());
                Map<String, List<AnimationChannel.Key>> channels = new LinkedHashMap<String, List<AnimationChannel.Key>>();
                for (JsonElement keyValue : array(animator, "keyframes")) {
                    JsonObject key = keyValue.getAsJsonObject();
                    String channel = string(key, "channel", "");
                    double time = number(key, "time", 0);
                    JsonArray points = array(key, "data_points");
                    if ("sound".equals(channel) || "particle".equals(channel) || "timeline".equals(channel)) {
                        if (time <= duration) events.add(new AnimationEvent(time, "blockbench_" + channel, points.toString()));
                        else warnings.add("Effect keys past declared clip length are not dispatched");
                        warnings.add("Sound/particle/timeline keys are Phase A presentation markers; local audio paths/scripts are not executed");
                        continue;
                    }
                    if (!"position".equals(channel) && !"rotation".equals(channel) && !"scale".equals(channel))
                        throw new IOException("Unsupported animation channel " + channel + " in " + name);
                    if (!nodes.containsKey(entry.getKey())) throw new IOException("Unknown animated bone UUID " + entry.getKey());
                    if (points.size() < 1 || points.size() > 2) throw new IOException("Expected one/two key data points");
                    AnimationChannel.Interpolation interpolation;
                    try { interpolation = AnimationChannel.Interpolation.valueOf(string(key, "interpolation", "linear").toUpperCase(Locale.ROOT)); }
                    catch (IllegalArgumentException failure) { throw new IOException("Unsupported interpolation in " + name + ": " + key.get("interpolation")); }
                    if (key.has("easing")) throw new IOException("Plugin easing is unsupported: " + name);
                    List<AnimationChannel.Key> keys = channels.get(channel);
                    if (keys == null) { keys = new ArrayList<AnimationChannel.Key>(); channels.put(channel, keys); }
                    keys.add(new AnimationChannel.Key(time, point(points.get(0).getAsJsonObject(), channel),
                            points.size() == 2 ? point(points.get(1).getAsJsonObject(), channel) : null, interpolation,
                            vector(key, "bezier_left_time", -0.1), converted(vector(key, "bezier_left_value", 0), channel),
                            vector(key, "bezier_right_time", 0.1), converted(vector(key, "bezier_right_value", 0), channel)));
                }
                if (!channels.isEmpty()) tracks.put(entry.getKey(), new AnimationTrack(channel(channels, "position", loopName),
                        channel(channels, "rotation", loopName), channel(channels, "scale", loopName)));
            }
            // A zero-length looping static pose is a hold, not a cursor that repeatedly wraps at zero.
            if (duration == 0 && loop == AnimationClip.Loop.LOOP) loop = AnimationClip.Loop.HOLD;
            if (clips.put(name, new AnimationClip(name, duration, loop, tracks, events, 0, 0, 0, true)) != null)
                throw new IOException("Duplicate animation name " + name);
        }
        alias(clips, "idle", "static_idle", false);
        alias(clips, "fire", "shoot", true);
        alias(clips, "reload_empty", "reload_dry", true);
        alias(clips, "reload", "reload_tactical", true);
        alias(clips, "reload", "reload_empty", true);
        return new AnimationDefinition(file.toString(), clips);
    }
    private static void alias(Map<String, AnimationClip> clips, String target, String source, boolean once) {
        AnimationClip clip = clips.get(source);
        if (clips.containsKey(target) || clip == null) return;
        clips.put(target, new AnimationClip(target, clip.duration, once ? AnimationClip.Loop.ONCE : clip.loop,
                clip.tracks, clip.events, clip.fadeIn, clip.fadeOut, clip.priority, clip.interruptible));
    }
    private static AnimationChannel channel(Map<String, List<AnimationChannel.Key>> channels, String name, String loop) {
        return channels.containsKey(name) ? new AnimationChannel(channels.get(name), "loop".equals(loop)) : null;
    }
    private static double[] point(JsonObject point, String channel) {
        double def = "scale".equals(channel) ? 1 : 0;
        double[] values = new double[3];
        String[] axes = {"x","y","z"};
        for (int i=0;i<3;i++) {
            String expression = string(point,axes[i],Double.toString(def)).trim();
            // The AK contains newline-only values, including zero-scale visibility keys.
            // Blockbench's numeric/Molang evaluator treats an empty expression as zero.
            try { values[i] = expression.isEmpty() ? 0 : Double.parseDouble(expression); }
            catch (NumberFormatException failure) { throw new IllegalArgumentException("Unsupported Molang expression: " + expression); }
        }
        return converted(values, channel);
    }
    private static double[] converted(double[] value, String channel) {
        for (int i=0;i<3;i++) value[i] *= BlockbenchTransform.factor(channel,i);
        return value;
    }
    private static JsonArray array(JsonObject object, String key) { return object.has(key) ? object.getAsJsonArray(key) : new JsonArray(); }
    private static String string(JsonObject object, String key, String def) { return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : def; }
    private static boolean bool(JsonObject object, String key, boolean def) { return object.has(key) ? object.get(key).getAsBoolean() : def; }
    private static double number(JsonObject object, String key, double def) {
        double result = object != null && object.has(key) ? object.get(key).getAsDouble() : def;
        if (!Double.isFinite(result)) throw new IllegalArgumentException("Non-finite " + key);
        return result;
    }
    private static double[] vector(JsonObject object, String key, double def) { return components(array(object,key),3,def); }
    private static double[] components(JsonArray array, int size, double def) {
        if (array.size() != 0 && array.size() != size) throw new IllegalArgumentException("Expected " + size + " vector components");
        double[] values = new double[size];
        for (int i=0;i<size;i++) { values[i] = array.size() == 0 ? def : array.get(i).getAsDouble();
            if (!Double.isFinite(values[i])) throw new IllegalArgumentException("Non-finite vector"); }
        return values;
    }
    /** Pack-author audit entry point; requires no Minecraft instance or OpenGL context. */
    public void audit(PrintStream out) {
        out.println(file + " | bones=" + nodes.size() + " cubes=" + cubes + " helpers=" + cameras);
        for (Node root : roots) auditNode(out, root, "");
        for (Texture texture : textures) out.println("texture " + texture.name + " " + texture.image.getWidth() + "x" + texture.image.getHeight()
                + " UV=" + texture.width + "x" + texture.height);
        for (AnimationClip clip : animations == null
                ? Collections.<AnimationClip>emptyList() : animations.clips.values()) {
            Set<AnimationChannel.Interpolation> modes = new LinkedHashSet<AnimationChannel.Interpolation>();
            int position=0, rotation=0, scale=0, split=0;
            for (AnimationTrack track : clip.tracks.values()) {
                if (track.position != null) position += track.position.keys.size();
                if (track.rotation != null) rotation += track.rotation.keys.size();
                if (track.scale != null) scale += track.scale.keys.size();
                for (AnimationChannel channel : new AnimationChannel[]{track.position,track.rotation,track.scale})
                    if (channel != null) for (AnimationChannel.Key key : channel.keys) { modes.add(key.interpolation); if(key.split)split++; }
            }
            out.println("animation " + clip.name + " duration=" + clip.duration + " " + clip.loop + " keys(position/rotation/scale)="
                    + position + "/" + rotation + "/" + scale + " split=" + split + " modes=" + modes + " events=" + clip.events.size());
        }
        for (String warning : warnings) out.println("WARNING " + warning);
    }
    private static void auditNode(PrintStream out, Node node, String indent) {
        out.println(indent + node.name + " [" + node.uuid + "] origin=" + Arrays.toString(node.origin)
                + " rotation=" + Arrays.toString(node.rotation) + " visible=" + node.visible + " faces=" + node.faces.size());
        for (Node child : node.children) auditNode(out,child,indent + "  ");
    }
    public static void main(String[] args) throws IOException {
        File file = new File(args[0]).getCanonicalFile();
        new BlockbenchProject(file, file.getParentFile()).audit(System.out);
    }
}
