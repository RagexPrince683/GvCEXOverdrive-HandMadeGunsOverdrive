package handmadeguns.pack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Validates the checked-in filesystem packs without consulting legacy fallback paths. */
public final class HMGRepositoryPackAssetTests {
    private static final Charset PACK_CHARSET = Charset.forName("Shift_JIS");
    private static final Set<String> EXPECTED_MISSING = new HashSet<String>(Arrays.asList(
            "Addfixing|Texture|bipod2",
            "Addfixing|Texture|bullet_shell_hmg",
            "Addfixing|Texture|grip2",
            "Addfixing|Texture|laser2",
            "Addfixing|Texture|reddot2",
            "Addfixing|Texture|right2",
            "Addfixing|Texture|scope2",
            "Addfixing|Texture|Suppressor2",
            "aww2pack|ScopeTexture|holosight.png",
            "aww2pack|ScopeTexture|leupold_itm.png",
            "aww2pack|ScopeTexture|mildot2.png",
            "aww2pack|Texture|ger20mm",
            "GVCguns|ScopeTexture|mildot2.png",
            "VehicleWeapons|Texture|AIM-54",
            "VehicleWeapons|Texture|bullet_7.5cm_KwK_40_Ammo",
            "VehicleWeapons|Texture|FAB-100",
            "VehicleWeapons|Texture|HJ-9",
            "VehicleWeapons|Texture|Kh-25ML"));

    private static int checks;
    private static int knownMissing;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException("Expected HMG pack roots");
        for (String argument : args) {
            File root = new File(argument).getCanonicalFile();
            if (!root.isDirectory()) throw new IOException("Pack root is not a directory: " + root);

            File[] packs = root.listFiles(File::isDirectory);
            if (packs == null) throw new IOException("Cannot enumerate pack root: " + root);
            Arrays.sort(packs, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
            for (File pack : packs) validatePack(root, pack);
            validateStg44TextureCategories(root);
        }

        System.out.println("HMG repository pack asset tests passed: " + checks
                + " checks (" + knownMissing + " pre-existing unresolved logical references)");
    }

    private static void validatePack(File root, File pack) throws Exception {
        check(pack.getCanonicalFile().getParentFile().equals(root), "pack escaped root: " + pack);
        HMGPackAssetResolver resolver = new HMGPackAssetResolver(pack);
        validateLegacyTree(pack);
        validateTextureLayout(pack);

        List<Path> definitions = new ArrayList<Path>();
        Files.walk(pack.toPath()).forEach(path -> {
            if (Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".txt"))
                definitions.add(path);
        });
        Collections.sort(definitions);
        for (Path definition : definitions) validateDefinition(pack.getName(), definition.toFile(), resolver);
    }

    private static void validateLegacyTree(File pack) throws IOException {
        Path packPath = pack.getCanonicalFile().toPath();
        Path assets = packPath.resolve("assets").resolve("handmadeguns");
        if (!Files.isDirectory(assets)) return;
        Files.walk(assets).forEach(path -> {
            if (!Files.isRegularFile(path)) return;
            String relative = assets.relativize(path).toString().replace('\\', '/');
            if (!"sounds.json".equalsIgnoreCase(relative))
                throw new IllegalStateException("authored pack asset remains under legacy resource tree: " + path);
        });
    }

    private static void validateTextureLayout(File pack) throws IOException {
        File textures = new File(pack, "textures");
        if (!textures.isDirectory()) return;
        File[] entries = textures.listFiles();
        if (entries == null) throw new IOException("Cannot enumerate texture directory: " + textures);
        for (File entry : entries) {
            String name = entry.getName();
            check(entry.isDirectory() && (name.equals("models") || name.equals("items") || name.equals("misc")),
                    "clean texture layout requires textures/models, textures/items, or textures/misc: " + entry);
        }
    }

    /** Regression for aww2pack/Ger_stg44: its item icon and OBJ texture share a basename but not a category. */
    private static void validateStg44TextureCategories(File root) throws Exception {
        File pack = new File(root, "aww2pack");
        HMGPackAssetResolver resolver = new HMGPackAssetResolver(pack);
        File item = resolver.resolve(HMGPackAssetResolver.Type.ITEM_TEXTURE, "items/stg44.png");
        File model = resolver.resolve(HMGPackAssetResolver.Type.MODEL_TEXTURE, "stg44.png");
        check(relative(pack, item).equals("textures/items/stg44.png"), "STG44 item icon resolved outside textures/items");
        check(relative(pack, model).equals("textures/models/stg44.png"), "STG44 model texture resolved outside textures/models");
        check(!item.equals(model), "STG44 item icon and model texture must remain separate assets");
        check(resolver.itemTextureName("items/stg44.png").equals("stg44"), "STG44 item icon did not resolve as an item texture");
        try {
            resolver.itemTextureName("models/stg44.png");
            throw new AssertionError("STG44 model texture was accepted as an item texture");
        } catch (IOException expected) {
            checks++;
        }
    }

    private static void validateDefinition(String packName, File definition,
                                           HMGPackAssetResolver resolver) throws Exception {
        for (String line : readPackLines(definition)) {
            String trimmed = line.trim();
            if (trimmed.length() == 0 || trimmed.startsWith("//") || trimmed.startsWith("#")) continue;
            String[] values = trimmed.split(",", -1);
            if (values.length < 2) continue;
            String key = values[0].trim();
            if (key.equals("Texture")) {
                validateReference(packName, key, values[1].trim(), resolver, HMGPackAssetResolver.Type.ITEM_TEXTURE, false);
            } else if (key.equals("ObjTexture") || key.equals("ModelTexture")) {
                validateReference(packName, key, values[1].trim(), resolver, HMGPackAssetResolver.Type.MODEL_TEXTURE, false);
            } else if (key.equals("ScopeTexture")) {
                for (int i = 1; i < Math.min(values.length, 4); i++)
                    validateReference(packName, key, values[i].trim(), resolver, HMGPackAssetResolver.Type.MISC_TEXTURE, false);
            } else if (key.equals("SkinTexture")) {
                validateReference(packName, key, values[1].trim(), resolver, HMGPackAssetResolver.Type.MODEL_TEXTURE, false);
            } else if (key.equals("Animations")) {
                for (int i = 1; i < values.length; i++)
                    validateReference(packName, key, values[i].trim(), resolver, HMGPackAssetResolver.Type.ANIMATION, false);
            } else if (key.equals("BedrockModel")) {
                validateReference(packName, key, values[1].trim(), resolver, HMGPackAssetResolver.Type.BEDROCK_MODEL, false);
            } else if (key.equals("ObjModel") || key.equals("BlockbenchModel")
                    || (key.equals("Model") && !values[1].trim().equalsIgnoreCase("true")
                    && !values[1].trim().equalsIgnoreCase("false"))) {
                validateReference(packName, key, values[1].trim(), resolver, HMGPackAssetResolver.Type.MODEL, false);
            } else if (key.matches("attach3dmodel[1-5]?")) {
                validateReference(packName, key, values[1].trim(), resolver, HMGPackAssetResolver.Type.MODEL, true);
            } else if (key.matches("3dmodeltex[1-5]?")) {
                validateReference(packName, key, values[1].trim(), resolver, HMGPackAssetResolver.Type.MODEL_TEXTURE, true);
            }
        }
    }

    private static List<String> readPackLines(File definition) throws IOException {
        // A few historical pack notes mix encodings; replacement decoding is safe here
        // because validation only consumes ASCII directive keys and asset references.
        String text = new String(Files.readAllBytes(definition.toPath()), PACK_CHARSET);
        return Arrays.asList(text.split("\\r\\n|\\r|\\n", -1));
    }

    private static void validateReference(String packName, String key, String reference,
                                          HMGPackAssetResolver resolver,
                                          HMGPackAssetResolver.Type type,
                                          boolean inferExtension) throws Exception {
        if (reference.length() == 0 || reference.equalsIgnoreCase("null") || reference.equalsIgnoreCase("null.png")) return;
        if (reference.toLowerCase().contains("assets/handmadeguns")
                || reference.toLowerCase().contains("textures/model/"))
            throw new AssertionError("deprecated asset path remains: " + packName + "|" + key + "|" + reference);

        File resolved = null;
        IOException failure = null;
        String[] candidates = inferExtension && reference.lastIndexOf('.') <= reference.lastIndexOf('/')
                ? new String[]{reference + (type == HMGPackAssetResolver.Type.MODEL ? ".mqo" : ".png"),
                        reference + (type == HMGPackAssetResolver.Type.MODEL ? ".obj" : ".png"), reference}
                : new String[]{reference};
        for (String candidate : candidates) {
            try {
                resolved = resolver.resolve(type, candidate);
                break;
            } catch (IOException missing) {
                failure = missing;
            }
        }
        if (resolved == null) {
            String missingKey = packName + "|" + key + "|" + reference;
            if (EXPECTED_MISSING.contains(missingKey)) {
                knownMissing++;
                System.out.println("[known pre-existing unresolved] " + missingKey);
                return;
            }
            throw new AssertionError("missing migrated asset " + missingKey, failure);
        }
        assertExactCase(resolver.getPackRoot(), resolved);
        checks++;
    }

    private static void assertExactCase(File pack, File asset) throws IOException {
        Path root = pack.getCanonicalFile().toPath();
        Path current = root;
        Path relative = root.relativize(asset.getCanonicalFile().toPath());
        for (Path segment : relative) {
            File[] entries = current.toFile().listFiles();
            if (entries == null) throw new IOException("Cannot enumerate asset directory: " + current);
            String expected = segment.toString();
            File exact = null;
            for (File entry : entries) if (entry.getName().equals(expected)) exact = entry;
            if (exact == null) throw new AssertionError("asset reference case mismatch: " + asset);
            current = exact.toPath();
        }
        check(current.toFile().isFile(), "resolved asset is not a file: " + asset);
    }

    private static String relative(File pack, File file) throws IOException {
        return pack.getCanonicalFile().toPath().relativize(file.getCanonicalFile().toPath())
                .toString().replace('\\', '/');
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
