package handmadeguns.loading;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import net.minecraft.launchwrapper.LaunchClassLoader;

/** Uses Forge's existing ASM runtime; no Mixin or Combatives dependency. */
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions({"handmadeguns.loading."})
public final class HMGJumpCorePlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[] {"handmadeguns.loading.HMGJumpTransformer"};
    }

    @Override
    public String getModContainerClass() { return null; }

    @Override
    public String getSetupClass() { return null; }

    @Override
    public void injectData(Map<String, Object> data) {
        ensureVecmath(data);
    }

    @Override
    public String getAccessTransformerClass() { return null; }

    private static synchronized void ensureVecmath(Map<String, Object> data) {
        ClassLoader loader = HMGJumpCorePlugin.class.getClassLoader();
        // A failed load poisons LaunchClassLoader.invalidClasses, even after addURL.
        // Resource lookup does not populate that cache. Use HMG's defining loader
        // on both launch paths, never the context/system loader or a child loader.
        if (loader.getResource("javax/vecmath/Vector3d.class") != null) {
            verifyVecmath(loader);
            return;
        }
        if (!(loader instanceof LaunchClassLoader)) {
            throw new IllegalStateException("HMG cannot attach bundled vecmath: its defining loader is not LaunchClassLoader");
        }

        Object location = data.get("coremodLocation");
        if (!(location instanceof File)) {
            throw new IllegalStateException("HMG cannot locate its bundled vecmath runtime");
        }

        File extracted = null;
        try (JarFile coremod = new JarFile((File) location)) {
            ZipEntry entry = coremod.getEntry("META-INF/libraries/vecmath-1.5.2.jar");
            if (entry == null) {
                throw new IOException("META-INF/libraries/vecmath-1.5.2.jar is missing");
            }

            extracted = File.createTempFile("hmg-vecmath-", ".jar");
            extracted.deleteOnExit();
            try (InputStream input = coremod.getInputStream(entry);
                 FileOutputStream output = new FileOutputStream(extracted)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            }

            try (JarFile library = new JarFile(extracted)) {
                if (library.getJarEntry("javax/vecmath/Vector3d.class") == null) {
                    throw new IOException("Bundled vecmath jar does not contain javax/vecmath/Vector3d.class");
                }
            }
            ((LaunchClassLoader) loader).addURL(extracted.toURI().toURL());
            verifyVecmath(loader);
        } catch (Exception failure) {
            if (extracted != null) {
                extracted.delete();
            }
            throw new IllegalStateException("HMG could not extract or attach META-INF/libraries/vecmath-1.5.2.jar to its Forge classloader", failure);
        }
    }

    private static void verifyVecmath(ClassLoader loader) {
        try {
            Class.forName("javax.vecmath.Vector3d", false, loader);
        } catch (ClassNotFoundException | LinkageError failure) {
            throw new IllegalStateException("HMG's vecmath runtime is corrupt or inaccessible to its Forge classloader", failure);
        }
    }
}
