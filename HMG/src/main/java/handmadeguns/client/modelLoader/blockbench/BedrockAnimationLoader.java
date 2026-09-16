package handmadeguns.client.modelLoader.blockbench;

import com.google.gson.*;
import handmadeguns.animation.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Reads TaCZ/Bedrock animation JSON into the same immutable clips used by native bbmodel projects. */
public final class BedrockAnimationLoader {
    private final Map<String, CacheEntry> cache = new HashMap<String, CacheEntry>();
    private final boolean nativeGeometry;

    public BedrockAnimationLoader() { this(false); }
    /** Separate loader instances keep coordinate conventions out of each other's caches. */
    public BedrockAnimationLoader(boolean nativeGeometry) { this.nativeGeometry = nativeGeometry; }

    public AnimationDefinition load(File file) throws IOException {
        File canonical = file.getCanonicalFile();
        String key = canonical.getPath();
        long modified = canonical.lastModified(), length = canonical.length();
        CacheEntry old = cache.get(key);
        if (old != null && old.modified == modified && old.length == length) return old.definition;
        AnimationDefinition definition;
        try (Reader reader = new InputStreamReader(new FileInputStream(canonical), StandardCharsets.UTF_8)) {
            definition = parse(reader, canonical.toString());
        }
        cache.put(key, new CacheEntry(modified, length, definition));
        return definition;
    }

    public AnimationDefinition parse(Reader reader, String source) throws IOException {
        try {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            JsonObject animations = root.getAsJsonObject("animations");
            if (animations == null) throw new IOException(source + ": missing Bedrock 'animations' object");
            Map<String, AnimationClip> clips = new LinkedHashMap<String, AnimationClip>();
            boolean authoredIdle = animations.has("idle");
            for (Map.Entry<String, JsonElement> entry : animations.entrySet())
                clips.put(entry.getKey(), clip(entry.getKey(), entry.getValue().getAsJsonObject(), source));
            alias(clips, "idle", "static_idle", false);
            // TaCZ installs static_idle as a persistent pose, even when its export omits loop.
            if (!authoredIdle && clips.containsKey("idle")) {
                AnimationClip idle = clips.get("idle");
                if (idle.loop == AnimationClip.Loop.ONCE)
                    clips.put("idle", new AnimationClip("idle", idle.duration, AnimationClip.Loop.HOLD,
                            idle.tracks, idle.events, idle.fadeIn, idle.fadeOut, idle.priority, idle.interruptible));
            }
            alias(clips, "fire", "shoot", true);
            alias(clips, "reload_empty", "reload_dry", true);
            alias(clips, "reload", "reload_tactical", true);
            alias(clips, "reload", "reload_empty", true);
            if (authoredIdle && hasLocomotion(clips)) alias(clips, "movement_idle", "idle", false);
            return new AnimationDefinition(source, clips);
        } catch (JsonParseException | IllegalStateException | IllegalArgumentException failure) {
            throw new IOException(source + ": invalid Bedrock animation JSON: " + failure.getMessage(), failure);
        }
    }

    public void clear() { cache.clear(); }

    /** Standard TaCZ ammo nodes plus bones explicitly hidden by the authored empty static pose. */
    public static Set<String> ammunitionBones(AnimationDefinition definition) {
        Set<String> result = new HashSet<String>(Arrays.asList("bullet_in_barrel", "bullet_in_mag", "bullet_chain"));
        AnimationClip empty = definition.clips.get("static_bolt_caught");
        if (empty != null) for (Map.Entry<String, AnimationTrack> entry : empty.tracks.entrySet()) {
            AnimationTrack track = entry.getValue();
            if (track.scale == null) continue;
            boolean hidden = !track.scale.keys.isEmpty();
            for (AnimationChannel.Key key : track.scale.keys) {
                AnimationPose.Transform value = track.sample(key.time);
                hidden &= value.sx == 0 && value.sy == 0 && value.sz == 0;
            }
            if (hidden) result.add(entry.getKey());
        }
        return Collections.unmodifiableSet(result);
    }

    public static boolean ammunitionVisible(Set<String> bones, String part, int ammunition, AnimationClip action) {
        return !bones.contains(part) || ammunition > 0 || (action != null
                && action.name.startsWith("reload") && action.tracks.containsKey(part));
    }

    public void invalidate(File file) {
        try { cache.remove(file.getCanonicalPath()); }
        catch (IOException ignored) { }
    }

    /** TaCZ animations may retain optional variant tracks omitted by the selected exported geometry. */
    public static AnimationDefinition retainKnownParts(AnimationDefinition definition, Set<String> knownParts,
                                                        Set<String> ignoredParts) {
        Map<String, AnimationClip> clips = new LinkedHashMap<String, AnimationClip>();
        for (AnimationClip clip : definition.clips.values()) {
            Map<String, AnimationTrack> tracks = new LinkedHashMap<String, AnimationTrack>();
            for (Map.Entry<String, AnimationTrack> track : clip.tracks.entrySet()) {
                if (knownParts.contains(track.getKey())) tracks.put(track.getKey(), track.getValue());
                else ignoredParts.add(track.getKey());
            }
            clips.put(clip.name, new AnimationClip(clip.name, clip.duration, clip.loop, tracks, clip.events,
                    clip.fadeIn, clip.fadeOut, clip.priority, clip.interruptible));
        }
        return new AnimationDefinition(definition.source, clips);
    }

    private AnimationClip clip(String name, JsonObject json, String source) throws IOException {
        boolean declaredDuration = json.has("animation_length");
        double duration = number(json, "animation_length", 0);
        AnimationClip.Loop loop = loop(json.get("loop"));
        Map<String, AnimationTrack> tracks = new LinkedHashMap<String, AnimationTrack>();
        JsonObject bones = json.getAsJsonObject("bones");
        if (bones != null) for (Map.Entry<String, JsonElement> bone : bones.entrySet()) {
            JsonObject channels = bone.getValue().getAsJsonObject();
            AnimationChannel position = channel(channels.get("position"), "position", loop == AnimationClip.Loop.LOOP, source, name, bone.getKey());
            AnimationChannel rotation = channel(channels.get("rotation"), "rotation", loop == AnimationClip.Loop.LOOP, source, name, bone.getKey());
            AnimationChannel scale = channel(channels.get("scale"), "scale", loop == AnimationClip.Loop.LOOP, source, name, bone.getKey());
            if (position != null || rotation != null || scale != null)
                tracks.put(bone.getKey(), new AnimationTrack(position, rotation, scale));
        }
        List<AnimationEvent> events = new ArrayList<AnimationEvent>();
        readEvents(json.getAsJsonObject("sound_effects"), "bedrock_sound", declaredDuration ? duration : Double.MAX_VALUE, events);
        readEvents(json.getAsJsonObject("particle_effects"), "bedrock_particle", declaredDuration ? duration : Double.MAX_VALUE, events);
        if (!declaredDuration) {
            for (AnimationTrack track : tracks.values()) for (AnimationChannel channel :
                    new AnimationChannel[]{track.position, track.rotation, track.scale})
                if (channel != null) duration = Math.max(duration, channel.keys.get(channel.keys.size()-1).time);
            for (AnimationEvent event : events) duration = Math.max(duration, event.time);
        }
        if (duration == 0 && loop == AnimationClip.Loop.LOOP) loop = AnimationClip.Loop.HOLD;
        double fade = name.equals("idle") || name.startsWith("walk") || name.startsWith("run")
                || name.startsWith("sprint") ? 0.12 : 0;
        return new AnimationClip(name, duration, loop, tracks, events, fade, fade, 0, true);
    }

    private AnimationChannel channel(JsonElement value, String kind, boolean loop,
                                            String source, String clip, String bone) throws IOException {
        if (value == null || value.isJsonNull()) return null;
        List<AnimationChannel.Key> keys = new ArrayList<AnimationChannel.Key>();
        if (value.isJsonArray() || value.isJsonPrimitive() || vectorObject(value)) {
            double[] point = vector(value, kind);
            keys.add(key(0, point, null, AnimationChannel.Interpolation.LINEAR));
        } else {
            for (Map.Entry<String, JsonElement> frame : value.getAsJsonObject().entrySet()) {
                double time;
                try { time = Double.parseDouble(frame.getKey()); }
                catch (NumberFormatException failure) {
                    throw new IOException(source + " | Clip: " + clip + " | Bone: " + bone
                            + " | Invalid key time " + frame.getKey());
                }
                JsonElement data = frame.getValue();
                double[] before, after = null;
                AnimationChannel.Interpolation interpolation = AnimationChannel.Interpolation.LINEAR;
                if (data.isJsonObject() && (data.getAsJsonObject().has("pre") || data.getAsJsonObject().has("post"))) {
                    JsonObject split = data.getAsJsonObject();
                    JsonElement pre = split.get("pre"), post = split.get("post");
                    before = vector(pre != null ? pre : post, kind);
                    if (pre != null && post != null) after = vector(post, kind);
                    String mode = string(split, "lerp_mode", "linear");
                    if ("catmullrom".equalsIgnoreCase(mode)) interpolation = AnimationChannel.Interpolation.CATMULLROM;
                    else if ("step".equalsIgnoreCase(mode)) interpolation = AnimationChannel.Interpolation.STEP;
                } else before = vector(data, kind);
                keys.add(key(time, before, after, interpolation));
            }
        }
        return new AnimationChannel(keys, loop);
    }

    private static AnimationChannel.Key key(double time, double[] before, double[] after,
                                            AnimationChannel.Interpolation interpolation) {
        return new AnimationChannel.Key(time, before, after, interpolation,
                new double[3], new double[3], new double[3], new double[3]);
    }

    private double[] vector(JsonElement value, String kind) throws IOException {
        double def = "scale".equals(kind) ? 1 : 0;
        double[] result = new double[3];
        if (value == null || value.isJsonNull()) Arrays.fill(result, def);
        else if (value.isJsonArray()) {
            JsonArray values = value.getAsJsonArray();
            if (values.size() != 3) throw new IOException("Expected three " + kind + " components");
            for (int i=0;i<3;i++) result[i] = numeric(values.get(i), def);
        } else if (value.isJsonObject()) {
            JsonObject values = value.getAsJsonObject();
            String[] axes = {"x", "y", "z"};
            for (int i=0;i<3;i++) result[i] = values.has(axes[i]) ? numeric(values.get(axes[i]), def) : def;
        } else {
            double scalar = numeric(value, def);
            Arrays.fill(result, scalar);
        }
        for (int i=0;i<3;i++) {
            double factor = BlockbenchTransform.factor(kind, i);
            // TaCZ ModelTranslateListener flips only Y; rotation keeps all authored signs.
            if (nativeGeometry && (("position".equals(kind) && i == 0)
                    || ("rotation".equals(kind) && i < 2))) factor = -factor;
            result[i] *= factor;
        }
        return result;
    }

    private static double numeric(JsonElement value, double def) throws IOException {
        if (value == null || value.isJsonNull()) return def;
        String text = value.getAsString().trim();
        if (text.isEmpty()) return 0;
        try {
            double result = Double.parseDouble(text);
            if (!Double.isFinite(result)) throw new NumberFormatException();
            return result;
        } catch (NumberFormatException failure) {
            throw new IOException("Unsupported Bedrock/Molang numeric expression: " + text);
        }
    }

    private static boolean vectorObject(JsonElement value) {
        if (!value.isJsonObject()) return false;
        JsonObject object = value.getAsJsonObject();
        return object.has("x") || object.has("y") || object.has("z");
    }

    private static void readEvents(JsonObject values, String type, double duration,
                                   List<AnimationEvent> events) throws IOException {
        if (values == null) return;
        for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
            double time;
            try { time = Double.parseDouble(entry.getKey()); }
            catch (NumberFormatException failure) { throw new IOException("Invalid Bedrock event time " + entry.getKey()); }
            if (time <= duration) events.add(new AnimationEvent(time, type, entry.getValue().toString()));
        }
    }

    private static AnimationClip.Loop loop(JsonElement value) throws IOException {
        if (value == null || value.isJsonNull()) return AnimationClip.Loop.ONCE;
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean())
            return value.getAsBoolean() ? AnimationClip.Loop.LOOP : AnimationClip.Loop.ONCE;
        String name = value.getAsString();
        if ("hold_on_last_frame".equals(name) || "hold".equals(name)) return AnimationClip.Loop.HOLD;
        if ("true".equalsIgnoreCase(name) || "loop".equalsIgnoreCase(name)) return AnimationClip.Loop.LOOP;
        if ("false".equalsIgnoreCase(name) || "once".equalsIgnoreCase(name)) return AnimationClip.Loop.ONCE;
        throw new IOException("Unsupported Bedrock loop mode " + name);
    }

    private static boolean hasLocomotion(Map<String, AnimationClip> clips) {
        for (String name : clips.keySet()) if (name.startsWith("walk") || name.startsWith("run")
                || name.startsWith("sprint")) return true;
        return false;
    }

    private static void alias(Map<String, AnimationClip> clips, String target, String source, boolean once) {
        AnimationClip clip = clips.get(source);
        if (clips.containsKey(target) || clip == null) return;
        clips.put(target, new AnimationClip(target, clip.duration, once ? AnimationClip.Loop.ONCE : clip.loop,
                clip.tracks, clip.events, clip.fadeIn, clip.fadeOut, clip.priority, clip.interruptible));
    }

    private static String string(JsonObject object, String key, String def) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : def;
    }

    private static double number(JsonObject object, String key, double def) throws IOException {
        if (!object.has(key)) return def;
        double result = numeric(object.get(key), def);
        if (result < 0) throw new IOException("Negative " + key);
        return result;
    }

    private static final class CacheEntry {
        final long modified, length;
        final AnimationDefinition definition;
        CacheEntry(long modified, long length, AnimationDefinition definition) {
            this.modified = modified; this.length = length; this.definition = definition;
        }
    }
}
