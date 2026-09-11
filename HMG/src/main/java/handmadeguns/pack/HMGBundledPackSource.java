package handmadeguns.pack;

import handmadeguns.HandmadeGunsCore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Presents official packs shipped in the mod archive as ordinary, immutable pack
 * sources. The legacy parsers require {@link File}s, so a private instance cache
 * mirrors the archive before it is handed to the existing pack pipeline.
 */
public final class HMGBundledPackSource {
    public static final String RESOURCE_ROOT = "hmg_packs/";
    public static final String CACHE_DIRECTORY = "handmadeguns_builtin";

    private HMGBundledPackSource() { }

    public static File materialize(File instanceDirectory) throws IOException {
        File target = new File(instanceDirectory, CACHE_DIRECTORY).getCanonicalFile();
        if (!target.isDirectory() && !target.mkdirs() && !target.isDirectory())
            throw new IOException("Cannot create bundled HMG pack cache: " + target);

        CodeSource source = HandmadeGunsCore.class.getProtectionDomain().getCodeSource();
        int copied = source == null ? 0 : materializeFromLocation(source.getLocation(), target);
        if (copied == 0) {
            URL root = HandmadeGunsCore.class.getClassLoader().getResource(RESOURCE_ROOT);
            copied = materializeFromLocation(root, target);
        }
        if (copied == 0) throw new IOException("Cannot locate bundled HMG packs in the mod archive");
        return target;
    }

    /** Supports ordinary code-source URLs and Forge/LaunchWrapper jar URLs without parsing them. */
    static int materializeFromLocation(URL location, File target) throws IOException {
        if (location == null) return 0;
        if ("jar".equalsIgnoreCase(location.getProtocol())) {
            URLConnection connection = location.openConnection();
            if (!(connection instanceof JarURLConnection))
                throw new IOException("Unsupported HMG JAR connection: " + connection.getClass().getName());
            connection.setUseCaches(false);
            JarFile jar = ((JarURLConnection) connection).getJarFile();
            try {
                return copyFromArchive(jar, target);
            } finally {
                jar.close();
            }
        }
        if (!"file".equalsIgnoreCase(location.getProtocol())) return 0;

        final File source;
        try {
            URI uri = location.toURI();
            source = new File(uri);
        } catch (Exception invalidLocation) {
            throw new IOException("Cannot resolve the HMG code-source location", invalidLocation);
        }
        if (source.isFile()) {
            JarFile jar = new JarFile(source);
            try {
                return copyFromArchive(jar, target);
            } finally {
                jar.close();
            }
        }
        if (!source.isDirectory()) return 0;
        File resourceRoot = source.getName().equals("hmg_packs") ? source : new File(source, RESOURCE_ROOT);
        return resourceRoot.isDirectory() ? copyDirectory(resourceRoot, target) : 0;
    }

    private static int copyFromArchive(JarFile jar, File target) throws IOException {
        int copied = 0;
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().startsWith(RESOURCE_ROOT)) continue;
            File output = outputFile(target, entry.getName().substring(RESOURCE_ROOT.length()));
            copy(jar.getInputStream(entry), output);
            copied++;
        }
        return copied;
    }

    private static int copyDirectory(File source, File target) throws IOException {
        File[] entries = source.listFiles();
        if (entries == null) return 0;
        int copied = 0;
        for (File entry : entries) {
            File output = outputFile(target, entry.getName());
            if (entry.isDirectory()) copied += copyDirectory(entry, output);
            else {
                copy(new FileInputStream(entry), output);
                copied++;
            }
        }
        return copied;
    }

    private static File outputFile(File target, String relative) throws IOException {
        File output = new File(target, relative).getCanonicalFile();
        if (!output.toPath().startsWith(target.toPath()))
            throw new IOException("Bundled HMG pack entry escapes cache: " + relative);
        return output;
    }

    private static void copy(InputStream input, File output) throws IOException {
        File parent = output.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory())
            throw new IOException("Cannot create bundled HMG pack directory: " + parent);
        try {
            Files.copy(input, output.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            input.close();
        }
    }
}
