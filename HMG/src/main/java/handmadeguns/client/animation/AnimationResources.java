package handmadeguns.client.animation;

import handmadeguns.animation.AnimationDefinition;
import handmadeguns.animation.AnimationLoader;
import handmadeguns.client.render.HMGGunParts;
import handmadeguns.client.render.PartsRender_Gun;
import handmadeguns.pack.HMGPackAssetResolver;
import java.io.File;
import java.io.IOException;
import java.util.*;

/** External pack files are CPU-only assets. No mesh invalidation or OpenGL work occurs here. */
public final class AnimationResources {
    private static final AnimationLoader LOADER = new AnimationLoader();
    private static final handmadeguns.client.modelLoader.blockbench.BedrockAnimationLoader BEDROCK_LOADER =
            new handmadeguns.client.modelLoader.blockbench.BedrockAnimationLoader();
    private static final handmadeguns.client.modelLoader.blockbench.BedrockAnimationLoader NATIVE_BEDROCK_LOADER =
            new handmadeguns.client.modelLoader.blockbench.BedrockAnimationLoader(true);
    private static final Map<PartsRender_Gun, Binding> BINDINGS = new WeakHashMap<PartsRender_Gun, Binding>();

    private AnimationResources() { }

    public static void bind(PartsRender_Gun renderer, File gunFile, String reference) {
        bind(renderer, gunFile, reference == null ? Collections.<String>emptyList()
                : Collections.singletonList(reference));
    }

    /** Sources are ordered from local/highest priority to shared fallback. */
    public static void bind(PartsRender_Gun renderer, File gunFile, List<String> references) {
        renderer.animationDefinition = null;
        BINDINGS.remove(renderer);
        AnimationDefinition embedded = renderer.model instanceof handmadeguns.client.modelLoader.blockbench.BlockbenchModel
                ? ((handmadeguns.client.modelLoader.blockbench.BlockbenchModel)renderer.model).project.animations : null;
        if (embedded != null && (references == null || references.isEmpty())) {
            renderer.animationDefinition = embedded;
            return;
        }
        if (references == null || references.isEmpty()) return;
        try {
            File root = handmadeguns.HandmadeGunsCore.gunPackRoot(gunFile);
            HMGPackAssetResolver resolver = new HMGPackAssetResolver(root);
            List<File> files = new ArrayList<File>();
            for (String reference : references) if (reference != null && !reference.isEmpty()) {
                try { files.add(resolver.resolve(HMGPackAssetResolver.Type.ANIMATION, reference)); }
                catch (IOException failure) {
                    System.err.println("[HMG Animation] Gun " + gunFile + " | " + reference + " | "
                            + failure.getMessage() + " | Skipping this animation source");
                }
            }
            Set<String> parts = new LinkedHashSet<String>();
            collect(renderer.partslist, parts);
            if (files.isEmpty()) { renderer.animationDefinition = embedded; return; }
            boolean nativeBedrock = renderer.model instanceof handmadeguns.client.modelLoader.blockbench.BlockbenchModel
                    && ((handmadeguns.client.modelLoader.blockbench.BlockbenchModel)renderer.model).project.bedrock;
            Binding binding = new Binding(gunFile.getCanonicalFile(), files, parts, embedded, nativeBedrock);
            BINDINGS.put(renderer, binding);
            renderer.animationDefinition = load(binding);
        } catch (IOException failure) {
            renderer.animationDefinition = embedded;
            String fallback = embedded == null ? "Using legacy motions" : "Using embedded Blockbench animations";
            System.err.println("[HMG Animation] Gun " + gunFile + " | " + failure.getMessage() + " | " + fallback);
        }
    }

    private static void collect(List<HMGGunParts> parts, Set<String> names) {
        for (HMGGunParts part : parts) {
            names.add(part.partsname);
            names.add(part.animationKey());
            collect(part.childs, names);
            if (part.reticleChild != null) collect(part.reticleChild, names);
        }
    }

    private static AnimationDefinition load(Binding binding) {
        AnimationDefinition definition = binding.embedded;
        for (File file : binding.files) {
            try {
                boolean bedrock = file.getName().toLowerCase(Locale.ROOT).endsWith(".animation.json");
                AnimationDefinition loaded = bedrock ? (binding.nativeBedrock ? NATIVE_BEDROCK_LOADER : BEDROCK_LOADER).load(file) : LOADER.load(file);
                if (bedrock) {
                    Set<String> ignored = new LinkedHashSet<String>();
                    loaded = handmadeguns.client.modelLoader.blockbench.BedrockAnimationLoader
                            .retainKnownParts(loaded, binding.parts, ignored);
                    if (!ignored.isEmpty()) System.err.println("[HMG Animation] Gun " + binding.gunFile + " | "
                            + file + " | Ignoring optional tracks absent from selected geometry: " + ignored);
                }
                loaded.validateParts(binding.parts);
                definition = merge(definition, loaded);
            } catch (IOException | IllegalArgumentException failure) {
                String retained = definition == null ? "No earlier animation source retained"
                        : "Retaining earlier animation sources";
                System.err.println("[HMG Animation] Gun " + binding.gunFile + " | " + file + " | "
                        + failure.getMessage() + " | " + retained);
            }
        }
        return definition;
    }

    public static void reloadAll() {
        LOADER.clear();
        BEDROCK_LOADER.clear();
        NATIVE_BEDROCK_LOADER.clear();
        AnimationClient.clearPlayback();
        for (Map.Entry<PartsRender_Gun, Binding> entry : BINDINGS.entrySet())
            entry.getKey().animationDefinition = load(entry.getValue());
    }

    /** Called before the existing targeted model/pack reparse; shared animation users also refresh. */
    public static void invalidateSource(File gunFile) {
        Set<File> files = new HashSet<File>();
        try {
            File source = gunFile.getCanonicalFile();
            for (Binding binding : BINDINGS.values()) if (binding.gunFile.equals(source)) files.addAll(binding.files);
            for (File file : files) LOADER.invalidate(file);
            for (File file : files) BEDROCK_LOADER.invalidate(file);
            for (File file : files) NATIVE_BEDROCK_LOADER.invalidate(file);
            for (Map.Entry<PartsRender_Gun, Binding> entry : BINDINGS.entrySet())
                if (!Collections.disjoint(files, entry.getValue().files))
                    entry.getKey().animationDefinition = load(entry.getValue());
        } catch (IOException failure) {
            System.err.println("[HMG Animation] " + gunFile + " | Cannot invalidate animation: " + failure.getMessage());
        }
        AnimationClient.clearPlayback();
    }

    /** Embedded project clips win; an external definition supplies only names the project omitted. */
    private static AnimationDefinition merge(AnimationDefinition primary, AnimationDefinition fallback) {
        if (primary == null) return fallback;
        return primary.withFallback(fallback);
    }

    private static final class Binding {
        final File gunFile;
        final List<File> files;
        final Set<String> parts;
        final AnimationDefinition embedded;
        final boolean nativeBedrock;
        Binding(File gunFile, List<File> files, Set<String> parts, AnimationDefinition embedded, boolean nativeBedrock) {
            this.nativeBedrock = nativeBedrock;
            this.gunFile = gunFile;
            this.files = Collections.unmodifiableList(new ArrayList<File>(files));
            this.parts = parts; this.embedded = embedded;
        }
    }
}
