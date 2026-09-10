package handmadeguns.client.animation;

import handmadeguns.animation.AnimationDefinition;
import handmadeguns.animation.AnimationLoader;
import handmadeguns.client.render.HMGGunParts;
import handmadeguns.client.render.PartsRender_Gun;
import java.io.File;
import java.io.IOException;
import java.util.*;

/** External pack files are CPU-only assets. No mesh invalidation or OpenGL work occurs here. */
public final class AnimationResources {
    private static final AnimationLoader LOADER = new AnimationLoader();
    private static final Map<PartsRender_Gun, Binding> BINDINGS = new WeakHashMap<PartsRender_Gun, Binding>();

    private AnimationResources() { }

    public static void bind(PartsRender_Gun renderer, File gunFile, String reference) {
        renderer.animationDefinition = null;
        BINDINGS.remove(renderer);
        if (renderer.model instanceof handmadeguns.client.modelLoader.blockbench.BlockbenchModel) {
            renderer.animationDefinition = ((handmadeguns.client.modelLoader.blockbench.BlockbenchModel)renderer.model).project.animations;
            if (reference != null) System.err.println("[HMG Blockbench] " + gunFile + " | Embedded animations take precedence over Animations JSON");
            return;
        }
        if (reference == null || reference.isEmpty()) return;
        try {
            File root = gunFile.getCanonicalFile().getParentFile().getParentFile();
            File file = new File(root, reference).getCanonicalFile();
            if (!file.toPath().startsWith(root.toPath()) || file.equals(root))
                throw new IOException("Animation path must stay inside pack root: " + reference);
            Set<String> parts = new LinkedHashSet<String>();
            collect(renderer.partslist, parts);
            Binding binding = new Binding(gunFile.getCanonicalFile(), file, parts);
            BINDINGS.put(renderer, binding);
            renderer.animationDefinition = load(binding);
        } catch (IOException failure) {
            System.err.println("[HMG Animation] Gun " + gunFile + " | " + failure.getMessage() + " | Using legacy motions");
        }
    }

    private static void collect(List<HMGGunParts> parts, Set<String> names) {
        for (HMGGunParts part : parts) {
            names.add(part.partsname);
            collect(part.childs, names);
            if (part.reticleChild != null) collect(part.reticleChild, names);
        }
    }

    private static AnimationDefinition load(Binding binding) {
        try {
            AnimationDefinition definition = LOADER.load(binding.file);
            definition.validateParts(binding.parts);
            return definition;
        } catch (IOException | IllegalArgumentException failure) {
            System.err.println("[HMG Animation] Gun " + binding.gunFile + " | " + failure.getMessage() + " | Using legacy motions");
            return null;
        }
    }

    public static void reloadAll() {
        LOADER.clear();
        AnimationClient.clearPlayback();
        for (Map.Entry<PartsRender_Gun, Binding> entry : BINDINGS.entrySet())
            entry.getKey().animationDefinition = load(entry.getValue());
    }

    /** Called before the existing targeted model/pack reparse; shared animation users also refresh. */
    public static void invalidateSource(File gunFile) {
        Set<File> files = new HashSet<File>();
        try {
            File source = gunFile.getCanonicalFile();
            for (Binding binding : BINDINGS.values()) if (binding.gunFile.equals(source)) files.add(binding.file);
            for (File file : files) LOADER.invalidate(file);
            for (Map.Entry<PartsRender_Gun, Binding> entry : BINDINGS.entrySet())
                if (files.contains(entry.getValue().file)) entry.getKey().animationDefinition = load(entry.getValue());
        } catch (IOException failure) {
            System.err.println("[HMG Animation] " + gunFile + " | Cannot invalidate animation: " + failure.getMessage());
        }
        AnimationClient.clearPlayback();
    }

    private static final class Binding {
        final File gunFile, file;
        final Set<String> parts;
        Binding(File gunFile, File file, Set<String> parts) { this.gunFile = gunFile; this.file = file; this.parts = parts; }
    }
}
