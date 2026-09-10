package handmadeguns.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Uses Minecraft's existing Gson. Strict syntax, tolerant unknown fields, no client/GL linkage. */
public final class AnimationLoader {
    private final Map<File, AnimationDefinition> cache = new HashMap<File, AnimationDefinition>();
    private final Map<File, String> failures = new HashMap<File, String>();

    public AnimationDefinition load(File file) throws IOException {
        File key = file.getCanonicalFile();
        if (failures.containsKey(key)) throw new IOException(failures.get(key));
        AnimationDefinition definition = cache.get(key);
        if (definition != null) return definition;
        try (Reader reader = new InputStreamReader(new FileInputStream(key), StandardCharsets.UTF_8)) {
            definition = parse(reader, key.getPath());
            cache.put(key, definition);
            return definition;
        } catch (IOException | IllegalArgumentException failure) {
            String message = "[HMG Animation] " + key + " | " + failure.getMessage();
            failures.put(key, message);
            throw new IOException(message, failure);
        }
    }

    public void invalidate(File file) throws IOException { File key = file.getCanonicalFile(); cache.remove(key); failures.remove(key); }
    public void clear() { cache.clear(); failures.clear(); }

    public AnimationDefinition parse(Reader input, String source) throws IOException {
        JsonReader reader = new JsonReader(input);
        reader.setLenient(false);
        try {
            JsonObject root = object(readValue(reader, 0), "root");
            if (reader.peek() != JsonToken.END_DOCUMENT) throw bad("root", "Trailing JSON data");
            if (number(required(root, "formatVersion", "root"), "formatVersion") != 1)
                throw bad("formatVersion", "Expected version 1");
            JsonObject clips = object(required(root, "clips", "root"), "clips");
            Map<String, AnimationClip> definitions = new LinkedHashMap<String, AnimationClip>();
            for (Map.Entry<String, JsonElement> entry : clips.entrySet()) {
                String name = entry.getKey(), at = "Clip: " + name;
                if (name.isEmpty()) throw bad(at, "Empty clip name");
                JsonObject clip = object(entry.getValue(), at);
                double duration = number(required(clip, "duration", at), at + " | duration");
                if (duration < 0) throw bad(at, "duration must be non-negative");
                AnimationClip.Loop loop = AnimationClip.Loop.ONCE;
                if (clip.has("loop")) {
                    JsonElement value = clip.get("loop");
                    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean())
                        loop = value.getAsBoolean() ? AnimationClip.Loop.LOOP : AnimationClip.Loop.ONCE;
                    else if ("hold".equals(string(value, at + " | loop"))) loop = AnimationClip.Loop.HOLD;
                    else throw bad(at, "loop must be true, false, or 'hold'");
                }
                if (duration == 0 && loop == AnimationClip.Loop.LOOP) throw bad(at, "Zero-duration clips cannot loop");
                Map<String, AnimationTrack> tracks = new LinkedHashMap<String, AnimationTrack>();
                JsonObject parts = clip.has("parts") ? object(clip.get("parts"), at + " | parts") : new JsonObject();
                for (Map.Entry<String, JsonElement> part : parts.entrySet()) {
                    String partAt = at + " | Part: " + part.getKey();
                    JsonArray frames = array(part.getValue(), partAt);
                    if (frames.size() == 0) throw bad(partAt, "Track needs at least one keyframe");
                    List<AnimationKeyframe> keys = new ArrayList<AnimationKeyframe>();
                    double previous = -1;
                    for (int i = 0; i < frames.size(); i++) {
                        String keyAt = partAt + " | Keyframe: " + i;
                        JsonObject key = object(frames.get(i), keyAt);
                        double time = number(required(key, "time", keyAt), keyAt + " | time");
                        if (time < 0 || time > duration || time <= previous)
                            throw bad(keyAt, "time must be within duration and strictly increasing");
                        previous = time;
                        float[] pos = vector(key, "position", keyAt), rot = vector(key, "rotation", keyAt);
                        keys.add(new AnimationKeyframe(time, new AnimationPose.Transform(pos[0], pos[1], pos[2], rot[0], rot[1], rot[2])));
                    }
                    tracks.put(part.getKey(), new AnimationTrack(keys));
                }
                List<AnimationEvent> events = new ArrayList<AnimationEvent>();
                if (clip.has("events")) {
                    JsonArray markers = array(clip.get("events"), at + " | events");
                    for (int i = 0; i < markers.size(); i++) {
                        String eventAt = at + " | Event: " + i;
                        JsonObject marker = object(markers.get(i), eventAt);
                        double time = number(required(marker, "time", eventAt), eventAt + " | time");
                        if (time < 0 || time > duration) throw bad(eventAt, "time must be within duration");
                        String event = string(required(marker, "event", eventAt), eventAt + " | event");
                        if (event.isEmpty()) throw bad(eventAt, "event must not be empty");
                        events.add(new AnimationEvent(time, event, marker.has("data") ? string(marker.get("data"), eventAt + " | data") : null));
                    }
                }
                double fadeIn = .1, fadeOut = .1;
                if (clip.has("transition")) {
                    JsonObject transition = object(clip.get("transition"), at + " | transition");
                    if (transition.has("in")) fadeIn = number(transition.get("in"), at + " | transition.in");
                    if (transition.has("out")) fadeOut = number(transition.get("out"), at + " | transition.out");
                    if (fadeIn < 0 || fadeOut < 0) throw bad(at, "Transition times must be non-negative");
                }
                double priority = clip.has("priority") ? number(clip.get("priority"), at + " | priority") : 0;
                if (priority != Math.rint(priority) || priority < Integer.MIN_VALUE || priority > Integer.MAX_VALUE)
                    throw bad(at, "priority must be an integer");
                boolean interruptible = true;
                if (clip.has("interruptible")) {
                    JsonElement value = clip.get("interruptible");
                    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw bad(at, "interruptible must be boolean");
                    interruptible = value.getAsBoolean();
                }
                definitions.put(name, new AnimationClip(name, duration, loop, tracks, events, fadeIn, fadeOut, (int)priority, interruptible));
            }
            return new AnimationDefinition(source, definitions);
        } catch (IllegalArgumentException | IllegalStateException failure) {
            throw new IOException("[HMG Animation] " + source + " | " + failure.getMessage(), failure);
        } catch (IOException failure) {
            throw new IOException("[HMG Animation] " + source + " | JSON: " + failure.getMessage(), failure);
        }
    }

    // JsonParser in Gson 2.2.4 temporarily enables leniency. Read the tree ourselves to retain strict JSON.
    private static JsonElement readValue(JsonReader reader, int depth) throws IOException {
        if (depth > 64) throw new IOException("JSON nesting exceeds 64");
        switch (reader.peek()) {
            case BEGIN_OBJECT:
                JsonObject object = new JsonObject(); reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (object.has(name)) throw new IOException("Duplicate JSON field: " + name);
                    object.add(name, readValue(reader, depth + 1));
                }
                reader.endObject(); return object;
            case BEGIN_ARRAY:
                JsonArray array = new JsonArray(); reader.beginArray();
                while (reader.hasNext()) array.add(readValue(reader, depth + 1));
                reader.endArray(); return array;
            case STRING: return new JsonPrimitive(reader.nextString());
            case NUMBER: return new JsonPrimitive(new BigDecimal(reader.nextString()));
            case BOOLEAN: return new JsonPrimitive(reader.nextBoolean());
            case NULL: reader.nextNull(); return JsonNull.INSTANCE;
            default: throw new IOException("Expected JSON value, found " + reader.peek());
        }
    }

    private static IllegalArgumentException bad(String at, String message) { return new IllegalArgumentException(at + " | " + message); }
    private static JsonElement required(JsonObject object, String key, String at) {
        if (!object.has(key)) throw bad(at, "Missing '" + key + "'"); return object.get(key);
    }
    private static JsonObject object(JsonElement value, String at) {
        if (!value.isJsonObject()) throw bad(at, "Expected object"); return value.getAsJsonObject();
    }
    private static JsonArray array(JsonElement value, String at) {
        if (!value.isJsonArray()) throw bad(at, "Expected array"); return value.getAsJsonArray();
    }
    private static String string(JsonElement value, String at) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw bad(at, "Expected string");
        return value.getAsString();
    }
    private static double number(JsonElement value, String at) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw bad(at, "Expected finite number");
        double result = value.getAsDouble();
        if (!Double.isFinite(result)) throw bad(at, "Expected finite number"); return result;
    }
    private static float[] vector(JsonObject key, String field, String at) {
        float[] result = new float[3];
        if (!key.has(field)) return result;
        JsonArray vector = array(key.get(field), at + " | " + field);
        if (vector.size() != 3) throw bad(at, field + " must contain exactly three numbers");
        for (int i = 0; i < 3; i++) {
            result[i] = (float)number(vector.get(i), at + " | " + field + "[" + i + "]");
            if (!Float.isFinite(result[i])) throw bad(at + " | " + field + "[" + i + "]", "Expected finite float");
        }
        return result;
    }
}
