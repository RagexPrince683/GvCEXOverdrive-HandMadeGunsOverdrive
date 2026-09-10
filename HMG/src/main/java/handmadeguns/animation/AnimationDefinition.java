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

    public void validateParts(Set<String> knownParts) {
        for (AnimationClip clip : clips.values()) for (String part : clip.tracks.keySet())
            if (!knownParts.contains(part)) throw new IllegalArgumentException("[HMG Animation] " + source
                    + " | Clip: " + clip.name + " | Part: " + part + " | Unknown part. Known parts: " + knownParts);
    }
}
