package handmadeguns.animation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class AnimationDefinition {
    public final String source;
    public final Map<String, AnimationClip> clips;
    public final Set<String> partNames;

    public AnimationDefinition(String source, Map<String, AnimationClip> clips) {
        this.source = source;
        this.clips = Collections.unmodifiableMap(new LinkedHashMap<String, AnimationClip>(clips));
        Set<String> names = new LinkedHashSet<String>();
        for (AnimationClip clip : clips.values()) names.addAll(clip.tracks.keySet());
        partNames = Collections.unmodifiableSet(names);
    }

    public AnimationClip requireClip(String name) {
        AnimationClip clip = clips.get(name);
        if (clip == null) throw new IllegalArgumentException("[HMG Animation] " + source + ": unknown clip '" + name + "'");
        return clip;
    }

    /** A local reload family owns reload selection, not just one exact clip spelling. */
    public AnimationDefinition withFallback(AnimationDefinition fallback) {
        Map<String, AnimationClip> merged = new LinkedHashMap<String, AnimationClip>(clips);
        boolean localReload = false;
        for (String name : clips.keySet()) if (isReload(name)) localReload = true;
        for (Map.Entry<String, AnimationClip> entry : fallback.clips.entrySet())
            if (!merged.containsKey(entry.getKey()) && !(localReload && isReload(entry.getKey())))
                merged.put(entry.getKey(), entry.getValue());
        return new AnimationDefinition(source + " + fallback " + fallback.source, merged);
    }

    private static boolean isReload(String name) {
        return "reload".equals(name) || "reload_empty".equals(name)
                || "reload_dry".equals(name) || "reload_tactical".equals(name);
    }

    public void validateParts(Set<String> knownParts) {
        for (AnimationClip clip : clips.values()) for (String part : clip.tracks.keySet())
            if (!knownParts.contains(part)) throw new IllegalArgumentException("[HMG Animation] " + source
                    + " | Clip: " + clip.name + " | Part: " + part + " | Unknown part. Known parts: " + knownParts);
    }
}
