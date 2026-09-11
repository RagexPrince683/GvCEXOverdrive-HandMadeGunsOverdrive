package handmadeguns.loading;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

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

    private static void ensureVecmath(Map<String, Object> data) {
        ClassLoader loader = HMGJumpCorePlugin.class.getClassLoader();
        try {
            Class.forName("javax.vecmath.Vector3d", false, loader);
            return;
        } catch (ClassNotFoundException missing) {
            // The release jar carries a private fallback for environments without vecmath.
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

            if (!(loader instanceof URLClassLoader)) {
                throw new IllegalStateException("HMG requires a URL-capable Forge classloader for bundled vecmath");
            }
            Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrl.setAccessible(true);
            addUrl.invoke(loader, extracted.toURI().toURL());
            Class.forName("javax.vecmath.Vector3d", false, loader);
        } catch (Exception failure) {
            if (extracted != null) {
                extracted.delete();
            }
            throw new IllegalStateException("HMG could not load its bundled vecmath runtime", failure);
        }
    }
}
