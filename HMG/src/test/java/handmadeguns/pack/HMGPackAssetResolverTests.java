package handmadeguns.pack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public final class HMGPackAssetResolverTests {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path temporary = Files.createTempDirectory("hmg-pack-resolver-");
        try {
            Path pack = Files.createDirectory(temporary.resolve("ActivePack"));
            Path otherPack = Files.createDirectory(temporary.resolve("OtherPack"));
            write(pack.resolve("models/akm.mqo"), "new");
            write(pack.resolve("textures/models/akm.png"), "model png");
            write(pack.resolve("textures/items/icon.png"), "item png");
            write(pack.resolve("textures/items/dotted.name.png"), "dotted item png");
            write(pack.resolve("textures/misc/scope.png"), "scope png");
            write(pack.resolve("animations/akm.json"), "{}");
            write(pack.resolve("attachments/new.txt"), "new");
            write(pack.resolve("attachment/new.txt"), "legacy duplicate");
            write(pack.resolve("attachment/legacy.txt"), "legacy");
            write(pack.resolve("assets/handmadeguns/textures/models/akm.mqo"), "legacy duplicate");
            write(pack.resolve("assets/handmadeguns/textures/models/legacy.obj"), "legacy");
            write(pack.resolve("assets/handmadeguns/textures/model/legacy.png"), "legacy texture");
            write(pack.resolve("assets/handmadeguns/textures/items/legacy_icon.png"), "legacy item texture");
            write(pack.resolve("assets/handmadeguns/textures/misc/legacy_scope.png"), "legacy misc texture");
            write(otherPack.resolve("models/foreign.obj"), "foreign");

            HMGPackAssetResolver resolver = new HMGPackAssetResolver(pack.toFile());
            same(pack.resolve("models/akm.mqo"), resolver.resolve(HMGPackAssetResolver.Type.MODEL, "akm.mqo"));
            same(pack.resolve("models/akm.mqo"), resolver.resolve(HMGPackAssetResolver.Type.MODEL, "models/akm.mqo"));
            same(pack.resolve("textures/models/akm.png"), resolver.resolve(HMGPackAssetResolver.Type.MODEL_TEXTURE, "akm"));
            same(pack.resolve("textures/models/akm.png"), resolver.resolve(HMGPackAssetResolver.Type.MODEL_TEXTURE, "models/akm"));
            same(pack.resolve("textures/items/dotted.name.png"), resolver.resolve(HMGPackAssetResolver.Type.ITEM_TEXTURE, "items/dotted.name"));
            same(pack.resolve("textures/misc/scope.png"), resolver.resolve(HMGPackAssetResolver.Type.MISC_TEXTURE, "scope"));
            same(pack.resolve("animations/akm.json"), resolver.resolve(HMGPackAssetResolver.Type.ANIMATION, "akm.json"));
            same(pack.resolve("assets/handmadeguns/textures/models/legacy.obj"),
                    resolver.resolve(HMGPackAssetResolver.Type.MODEL, "legacy.obj"));
            same(pack.resolve("assets/handmadeguns/textures/model/legacy.png"),
                    resolver.resolve(HMGPackAssetResolver.Type.MODEL_TEXTURE, "legacy.png"));
            same(pack.resolve("assets/handmadeguns/textures/items/legacy_icon.png"),
                    resolver.resolve(HMGPackAssetResolver.Type.ITEM_TEXTURE, "legacy_icon.png"));
            same(pack.resolve("assets/handmadeguns/textures/misc/legacy_scope.png"),
                    resolver.resolve(HMGPackAssetResolver.Type.MISC_TEXTURE, "legacy_scope.png"));
            equal("handmadeguns:textures/model/akm.mqo",
                    resolver.resourceLocation(HMGPackAssetResolver.Type.MODEL, "akm.mqo"));
            equal("handmadeguns:textures/model/akm.png",
                    resolver.resourceLocation(HMGPackAssetResolver.Type.MODEL_TEXTURE, "akm.png"));
            equal("handmadeguns:textures/items/icon.png",
                    resolver.resourceLocation(HMGPackAssetResolver.Type.ITEM_TEXTURE, "items/icon.png"));
            equal("handmadeguns:textures/misc/scope.png",
                    resolver.resourceLocation(HMGPackAssetResolver.Type.MISC_TEXTURE, "misc/scope.png"));
            equal("icon", resolver.itemTextureName("items/icon.png"));
            rejectedItemTexture(resolver, "akm.png");

            List<File> attachments = resolver.listDefinitions(HMGPackAssetResolver.Type.ATTACHMENT_DEFINITION);
            equal(2, attachments.size());
            same(pack.resolve("attachments/new.txt"), find(attachments, "new.txt"));
            same(pack.resolve("attachment/legacy.txt"), find(attachments, "legacy.txt"));

            rejected(resolver, HMGPackAssetResolver.Type.MODEL, "../OtherPack/models/foreign.obj");
            rejected(resolver, HMGPackAssetResolver.Type.MODEL, otherPack.resolve("models/foreign.obj").toString());
            rejected(resolver, HMGPackAssetResolver.Type.MODEL, "foreign.obj");
            rejected(resolver, HMGPackAssetResolver.Type.MODEL, "othermod:models/foreign.obj");

            int staged = resolver.stageResources();
            check(staged >= 3, "expected typed clean resources to be staged");
            check(Files.isRegularFile(pack.resolve("assets/handmadeguns/textures/model/akm.mqo")), "model was not staged");
            equal("new", new String(Files.readAllBytes(pack.resolve("assets/handmadeguns/textures/model/akm.mqo")), StandardCharsets.UTF_8));
            check(Files.isRegularFile(pack.resolve("assets/handmadeguns/textures/model/akm.png")), "model texture was not staged");
            check(!Files.exists(pack.resolve("assets/handmadeguns/textures/items/akm.png")), "model texture was staged as an item texture");
            check(!Files.exists(pack.resolve("assets/handmadeguns/textures/misc/akm.png")), "model texture was staged as a misc texture");
            check(Files.isRegularFile(pack.resolve("assets/handmadeguns/textures/items/icon.png")), "explicit item texture was not staged");
            check(Files.isRegularFile(pack.resolve("assets/handmadeguns/textures/misc/scope.png")), "explicit misc texture was not staged");

            System.out.println("HMG pack resolver tests passed: " + checks);
        } finally {
            Files.walk(temporary).sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); }
                catch (IOException failure) { throw new RuntimeException(failure); }
            });
        }
    }

    private static void write(Path path, String value) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, value.getBytes(StandardCharsets.UTF_8));
    }

    private static File find(List<File> files, String name) {
        for (File file : files) if (name.equals(file.getName())) return file;
        throw new AssertionError("Missing definition " + name);
    }

    private static void rejected(HMGPackAssetResolver resolver, HMGPackAssetResolver.Type type, String reference) throws Exception {
        try {
            resolver.resolve(type, reference);
            throw new AssertionError("Expected rejection for " + reference);
        } catch (IOException expected) {
            checks++;
        }
    }

    private static void rejectedItemTexture(HMGPackAssetResolver resolver, String reference) throws Exception {
        try {
            resolver.itemTextureName(reference);
            throw new AssertionError("Expected item texture rejection for " + reference);
        } catch (IOException expected) {
            checks++;
        }
    }

    private static void same(Path expected, File actual) throws IOException {
        equal(expected.toFile().getCanonicalFile(), actual.getCanonicalFile());
    }

    private static void equal(Object expected, Object actual) {
        check(expected == null ? actual == null : expected.equals(actual), "expected " + expected + " but got " + actual);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
