package handmadeguns.pack;

import handmadeguns.HandmadeGunsCore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.CRC32;

/**
 * Presents official packs shipped in the mod archive as ordinary, immutable pack
 * sources. The legacy parsers require {@link File}s, so a private instance cache
 * mirrors the archive before it is handed to the existing pack pipeline.
 */
public final class HMGBundledPackSource {
    public static final String RESOURCE_ROOT = "hmg_packs/";
    public static final String CACHE_DIRECTORY = "handmadeguns_builtin";

    private static final int BUFFER_SIZE = 8192;
    private static final int REPLACE_ATTEMPTS = 4;
    private static final long REPLACE_BACKOFF_MILLIS = 25L;
    private static final boolean WINDOWS = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT).startsWith("windows");

    private HMGBundledPackSource() { }

    public static File materialize(File instanceDirectory) throws IOException {
        File target = new File(instanceDirectory, CACHE_DIRECTORY).getCanonicalFile();
        if (!target.isDirectory() && !target.mkdirs() && !target.isDirectory())
            throw new IOException("Cannot create bundled HMG pack cache: " + target);

        CodeSource source = HandmadeGunsCore.class.getProtectionDomain().getCodeSource();
        int entries = source == null ? 0 : materializeFromLocation(source.getLocation(), target);
        if (entries == 0) {
            URL root = HandmadeGunsCore.class.getClassLoader().getResource(RESOURCE_ROOT);
            entries = materializeFromLocation(root, target);
        }
        if (entries == 0) throw new IOException("Cannot locate bundled HMG packs in the mod archive");
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
            try (JarFile jar = ((JarURLConnection) connection).getJarFile()) {
                return copyFromArchive(jar, target);
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
            try (JarFile jar = new JarFile(source)) {
                return copyFromArchive(jar, target);
            }
        }
        if (!source.isDirectory()) return 0;
        File resourceRoot = source.getName().equals("hmg_packs") ? source : new File(source, RESOURCE_ROOT);
        return resourceRoot.isDirectory() ? copyDirectory(resourceRoot, target, "") : 0;
    }

    private static int copyFromArchive(JarFile jar, File target) throws IOException {
        int entriesFound = 0;
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().startsWith(RESOURCE_ROOT)) continue;
            String relative = entry.getName().substring(RESOURCE_ROOT.length());
            File output = outputFile(target, relative);
            boolean unchanged;
            try {
                unchanged = sameArchiveEntry(entry, output);
            } catch (IOException failure) {
                throw materializationFailure(relative, output, "compare the existing file", failure);
            }
            if (!unchanged) {
                try {
                    replace(jar.getInputStream(entry), output);
                } catch (IOException failure) {
                    throw materializationFailure(relative, output, "replace", failure);
                }
            }
            entriesFound++;
        }
        return entriesFound;
    }

    private static int copyDirectory(File source, File target, String parentRelative) throws IOException {
        File[] entries = source.listFiles();
        if (entries == null) return 0;
        int entriesFound = 0;
        for (File entry : entries) {
            String relative = parentRelative.length() == 0 ? entry.getName()
                    : parentRelative + "/" + entry.getName();
            File output = outputFile(target, entry.getName());
            if (entry.isDirectory()) {
                entriesFound += copyDirectory(entry, output, relative);
            } else {
                boolean unchanged;
                try {
                    unchanged = sameFileContents(entry, output);
                } catch (IOException failure) {
                    throw materializationFailure(relative, output, "compare the existing file", failure);
                }
                if (!unchanged) {
                    try {
                        replace(new FileInputStream(entry), output);
                    } catch (IOException failure) {
                        throw materializationFailure(relative, output, "replace", failure);
                    }
                }
                entriesFound++;
            }
        }
        return entriesFound;
    }

    private static File outputFile(File target, String relative) throws IOException {
        File output = new File(target, relative).getCanonicalFile();
        if (!output.toPath().startsWith(target.toPath()))
            throw new IOException("Bundled HMG pack entry escapes cache: " + relative);
        return output;
    }

    private static boolean sameArchiveEntry(JarEntry entry, File output) throws IOException {
        if (!output.isFile() || entry.getSize() < 0L || entry.getCrc() < 0L
                || output.length() != entry.getSize()) return false;
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream input = new BufferedInputStream(new FileInputStream(output))) {
            int read;
            while ((read = input.read(buffer)) != -1) crc.update(buffer, 0, read);
        }
        return crc.getValue() == entry.getCrc();
    }

    private static boolean sameFileContents(File source, File output) throws IOException {
        if (!output.isFile() || source.length() != output.length()) return false;
        byte[] sourceBuffer = new byte[BUFFER_SIZE];
        byte[] outputBuffer = new byte[BUFFER_SIZE];
        try (InputStream sourceInput = new BufferedInputStream(new FileInputStream(source));
             InputStream outputInput = new BufferedInputStream(new FileInputStream(output))) {
            while (true) {
                int sourceRead = sourceInput.read(sourceBuffer);
                int outputRead = outputInput.read(outputBuffer);
                if (sourceRead != outputRead) return false;
                if (sourceRead == -1) return true;
                for (int index = 0; index < sourceRead; index++)
                    if (sourceBuffer[index] != outputBuffer[index]) return false;
            }
        }
    }

    private static void replace(InputStream input, File output) throws IOException {
        File parent = output.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory())
            throw new IOException("Cannot create bundled HMG pack directory: " + parent);

        Path temporary = null;
        try {
            try (InputStream source = input) {
                temporary = Files.createTempFile(parent.toPath(), "." + output.getName() + ".", ".hmg.tmp");
                try (OutputStream destination = new BufferedOutputStream(Files.newOutputStream(temporary,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int read;
                    while ((read = source.read(buffer)) != -1) destination.write(buffer, 0, read);
                }
            }
            replaceWithRetry(temporary, output.toPath());
        } catch (IOException failure) {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
    }

    private static void replaceWithRetry(Path temporary, Path output) throws IOException {
        IOException lastFailure = null;
        for (int attempt = 1; attempt <= REPLACE_ATTEMPTS; attempt++) {
            try {
                try {
                    Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            } catch (IOException failure) {
                lastFailure = failure;
                if (attempt == REPLACE_ATTEMPTS || !isRetryableWindowsReplaceFailure(failure)) throw failure;
                try {
                    Thread.sleep(REPLACE_BACKOFF_MILLIS * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw failure;
                }
            }
        }
        throw lastFailure;
    }

    private static boolean isRetryableWindowsReplaceFailure(IOException failure) {
        if (!WINDOWS) return false;
        return failure instanceof AccessDeniedException || failure.getClass() == FileSystemException.class;
    }

    private static IOException materializationFailure(String relative, File output, String operation,
                                                       IOException cause) {
        int separator = relative.indexOf('/');
        String pack = separator < 0 ? relative : relative.substring(0, separator);
        return new IOException("Failed to materialize bundled HMG pack '" + pack + "': could not "
                + operation + " " + relative + " at " + output, cause);
    }
}
