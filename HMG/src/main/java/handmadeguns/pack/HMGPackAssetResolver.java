package handmadeguns.pack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;

/** Deterministic asset lookup rooted in one active HMG content pack. */
public final class HMGPackAssetResolver {
    public enum Type {
        GUN_DEFINITION("guns"),
        MODEL("models", "addmodel", "assets/handmadeguns/textures/model", "assets/handmadeguns/textures/models"),
        /** Textures rendered on OBJ, MQO, or Blockbench geometry. */
        MODEL_TEXTURE("textures/models", "textures", "addmodel",
                "assets/handmadeguns/textures/model", "assets/handmadeguns/textures/models"),
        /** Textures used by Item.setTextureName for inventory and hotbar icons. */
        ITEM_TEXTURE("textures/items", "addtexture", "assets/handmadeguns/textures/items"),
        /** Reticles, scope overlays, and other non-model textures. */
        MISC_TEXTURE("textures/misc", "addsighttex", "assets/handmadeguns/textures/misc"),
        BLOCKBENCH_MODEL("models", "addmodel", "assets/handmadeguns/textures/model", "assets/handmadeguns/textures/models"),
        ANIMATION("animations"),
        SOUND("sounds", "addsounds", "assets/handmadeguns/sounds"),
        ATTACHMENT_DEFINITION("attachments", "attachment"),
        MAGAZINE_DEFINITION("magazines"),
        BULLET_DEFINITION("bullets");

        final String cleanDirectory;
        final String[] legacyDirectories;

        Type(String cleanDirectory, String... legacyDirectories) {
            this.cleanDirectory = cleanDirectory;
            this.legacyDirectories = legacyDirectories;
        }
    }

    private static final Comparator<File> FILE_NAME_COMPARATOR = new Comparator<File>() {
        @Override public int compare(File left, File right) {
            return left.getName().compareToIgnoreCase(right.getName());
        }
    };

    private final File packRoot;
    private final Path packPath;

    public HMGPackAssetResolver(File packRoot) throws IOException {
        if (packRoot == null) throw new IOException("Missing HMG pack root");
        this.packRoot = packRoot.getCanonicalFile();
        if (!this.packRoot.isDirectory()) throw new IOException("HMG pack root is not a directory: " + packRoot);
        this.packPath = this.packRoot.toPath();
    }

    public File getPackRoot() {
        return packRoot;
    }

    public File resolve(Type type, String reference) throws IOException {
        if (type == null) throw new IOException("Missing HMG asset type");
        String normalized = normalizeReference(reference);
        List<String> variants = referenceVariants(type, normalized);
        Set<File> candidates = new LinkedHashSet<File>();

        for (String variant : variants) {
            String cleanRelative = stripCleanDirectoryPrefix(type, variant);
            candidates.add(inside(new File(new File(packRoot, type.cleanDirectory), cleanRelative), reference));
        }
        for (String legacyDirectory : type.legacyDirectories) {
            for (String variant : variants) {
                String legacyRelative = stripDirectoryPrefix(variant, legacyDirectory);
                candidates.add(inside(new File(new File(packRoot, legacyDirectory), legacyRelative), reference));
            }
        }

        for (File candidate : candidates) if (candidate.isFile()) return candidate;
        throw new FileNotFoundException("No " + type.name().toLowerCase() + " '" + reference
                + "' in HMG pack " + packRoot.getName());
    }

    /** Lists direct definition files, merging clean and legacy folders with clean-name precedence. */
    public List<File> listDefinitions(Type type) throws IOException {
        if (type != Type.GUN_DEFINITION && type != Type.ATTACHMENT_DEFINITION
                && type != Type.MAGAZINE_DEFINITION && type != Type.BULLET_DEFINITION)
            throw new IOException(type + " is not a definition type");
        TreeMap<String, File> files = new TreeMap<String, File>(String.CASE_INSENSITIVE_ORDER);
        addDefinitionDirectory(files, type.cleanDirectory);
        for (String legacyDirectory : type.legacyDirectories) addDefinitionDirectory(files, legacyDirectory);
        ArrayList<File> result = new ArrayList<File>(files.values());
        Collections.sort(result, FILE_NAME_COMPARATOR);
        return result;
    }

    /** Maps a resolved model/texture file to the resource path exposed by pack staging. */
    public String resourceLocation(Type type, String reference) throws IOException {
        File file = resolve(type, reference);
        String relative = relative(file);
        String assetsPrefix = "assets/handmadeguns/";
        if (relative.startsWith(assetsPrefix)) return "handmadeguns:" + relative.substring(assetsPrefix.length());
        if (relative.startsWith("models/")) return "handmadeguns:textures/model/" + relative.substring("models/".length());
        if (relative.startsWith("textures/models/")) return "handmadeguns:textures/model/" + relative.substring("textures/models/".length());
        if (relative.startsWith("textures/items/")) return "handmadeguns:textures/items/" + relative.substring("textures/items/".length());
        if (relative.startsWith("textures/misc/")) return "handmadeguns:textures/misc/" + relative.substring("textures/misc/".length());
        if (relative.startsWith("textures/")) return "handmadeguns:textures/model/" + relative.substring("textures/".length());
        if (relative.startsWith("addmodel/")) return "handmadeguns:textures/model/" + relative.substring("addmodel/".length());
        if (relative.startsWith("addtexture/")) return "handmadeguns:textures/items/" + relative.substring("addtexture/".length());
        if (relative.startsWith("addsighttex/")) return "handmadeguns:textures/misc/" + relative.substring("addsighttex/".length());
        throw new IOException("Asset has no HMG resource mapping: " + file);
    }

    /** Returns the value expected by Item.setTextureName after resolving a pack texture. */
    public String itemTextureName(String reference) throws IOException {
        File file = resolve(Type.ITEM_TEXTURE, reference);
        String relative = relative(file);
        String value;
        if (relative.startsWith("textures/items/")) value = relative.substring("textures/items/".length());
        else if (relative.startsWith("addtexture/")) value = relative.substring("addtexture/".length());
        else {
            String prefix = "assets/handmadeguns/textures/items/";
            if (!relative.startsWith(prefix)) throw new IOException("Texture is not an item texture: " + file);
            value = relative.substring(prefix.length());
        }
        if (value.toLowerCase().endsWith(".png")) value = value.substring(0, value.length() - 4);
        return value.replace('\\', '/');
    }

    /**
     * Mirrors author-friendly resource folders into Minecraft's registered resource tree.
     * Legacy mirrors run first so the recommended clean layout wins on a same-path collision.
     */
    public int stageResources() throws IOException {
        int copied = 0;
        copied += stageTree("addmodel", "assets/handmadeguns/textures/model");
        copied += stageTree("addtexture", "assets/handmadeguns/textures/items");
        copied += stageTree("addsighttex", "assets/handmadeguns/textures/misc");
        copied += stageTree("addsounds", "assets/handmadeguns/sounds");
        copied += stageTree("models", "assets/handmadeguns/textures/model");
        copied += stageLegacyCleanModelTextureTree();
        copied += stageTree("textures/models", "assets/handmadeguns/textures/model");
        copied += stageTree("textures/items", "assets/handmadeguns/textures/items");
        copied += stageTree("textures/misc", "assets/handmadeguns/textures/misc");
        copied += stageTree("sounds", "assets/handmadeguns/sounds");
        return copied;
    }

    private void addDefinitionDirectory(TreeMap<String, File> files, String directory) throws IOException {
        File folder = inside(new File(packRoot, directory), directory);
        File[] entries = folder.listFiles();
        if (entries == null) return;
        for (File entry : entries) {
            File canonical = inside(entry, entry.getName());
            if (canonical.isFile() && !files.containsKey(canonical.getName())) files.put(canonical.getName(), canonical);
        }
    }

    private int stageTree(String sourceDirectory, String targetDirectory) throws IOException {
        File source = inside(new File(packRoot, sourceDirectory), sourceDirectory);
        if (!source.isDirectory()) return 0;
        return stageDirectory(source, inside(new File(packRoot, targetDirectory), targetDirectory));
    }

    /** Supports the previous clean root texture folder without staging its typed branches. */
    private int stageLegacyCleanModelTextureTree() throws IOException {
        File source = inside(new File(packRoot, "textures"), "textures");
        if (!source.isDirectory()) return 0;
        return stageDirectory(source, inside(new File(packRoot, "assets/handmadeguns/textures/model"),
                "assets/handmadeguns/textures/model"), true);
    }

    private int stageDirectory(File source, File target) throws IOException {
        return stageDirectory(source, target, false);
    }

    private int stageDirectory(File source, File target, boolean skipLogicalTextureBranches) throws IOException {
        File[] entries = source.listFiles();
        if (entries == null) return 0;
        int copied = 0;
        for (File entry : entries) {
            if (skipLogicalTextureBranches && ("models".equals(entry.getName())
                    || "items".equals(entry.getName()) || "misc".equals(entry.getName()))) continue;
            File safeSource = inside(entry, entry.getPath());
            File safeTarget = inside(new File(target, entry.getName()), entry.getName());
            if (safeSource.isDirectory()) {
                copied += stageDirectory(safeSource, safeTarget, false);
            } else if (safeSource.isFile() && !safeSource.equals(safeTarget)) {
                File parent = safeTarget.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory())
                    throw new IOException("Cannot create HMG resource directory: " + parent);
                Files.copy(safeSource.toPath(), safeTarget.toPath(), StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
                copied++;
            }
        }
        return copied;
    }

    private File inside(File candidate, String reference) throws IOException {
        File canonical = candidate.getCanonicalFile();
        if (canonical.equals(packRoot) || !canonical.toPath().startsWith(packPath))
            throw new IOException("HMG asset path must stay inside active pack: " + reference);
        return canonical;
    }

    private String relative(File file) {
        return packPath.relativize(file.toPath()).toString().replace('\\', '/');
    }

    private static String normalizeReference(String reference) throws IOException {
        if (reference == null || reference.trim().length() == 0) throw new IOException("Empty HMG asset reference");
        String normalized = reference.trim().replace('\\', '/');
        if (new File(normalized).isAbsolute() || normalized.startsWith("/") || normalized.indexOf(':') >= 0)
            throw new IOException("HMG asset reference must be pack-relative: " + reference);
        String[] segments = normalized.split("/");
        for (String segment : segments) if ("..".equals(segment))
            throw new IOException("HMG asset path must stay inside active pack: " + reference);
        while (normalized.startsWith("./")) normalized = normalized.substring(2);
        return normalized;
    }

    private static List<String> referenceVariants(Type type, String reference) {
        ArrayList<String> variants = new ArrayList<String>();
        variants.add(reference);
        if ((type == Type.MODEL_TEXTURE || type == Type.ITEM_TEXTURE || type == Type.MISC_TEXTURE)
                && !hasTextureExtension(reference))
            variants.add(reference + ".png");
        return variants;
    }

    private static boolean hasTextureExtension(String reference) {
        String lower = reference.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".pdn") || lower.endsWith(".xcf")
                || lower.endsWith(".ace") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    private static String stripDirectoryPrefix(String reference, String directory) {
        String prefix = directory.replace('\\', '/') + "/";
        return reference.startsWith(prefix) ? reference.substring(prefix.length()) : reference;
    }

    private static String stripCleanDirectoryPrefix(Type type, String reference) {
        if (type == Type.MODEL_TEXTURE && reference.startsWith("models/"))
            return reference.substring("models/".length());
        if (type == Type.ITEM_TEXTURE && reference.startsWith("items/"))
            return reference.substring("items/".length());
        if (type == Type.MISC_TEXTURE && reference.startsWith("misc/"))
            return reference.substring("misc/".length());
        return stripDirectoryPrefix(reference, type.cleanDirectory);
    }
}
