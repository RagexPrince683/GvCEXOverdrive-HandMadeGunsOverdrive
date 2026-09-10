package handmadeguns.client.modelLoader.obj_modelloaderMod.obj;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;

/** Keeps generic resource reloads separate from explicit HMG pack-model reloads. */
@SideOnly(Side.CLIENT)
public class HMGObjResourceReloadListener implements IResourceManagerReloadListener {
    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
		// Animation files contain no GL resources and can safely refresh existing bindings.
		handmadeguns.client.animation.AnimationResources.reloadAll();
        // SimpleReloadableResourceManager invokes a newly registered listener
        // immediately.  HMG registers this listener during init, after gun and
        // vehicle packs have already bound their initial models during pre-init.
        // Releasing those models here leaves the renderers pointing at deleted
        // VBO/display-list data, with no pack reparse to install replacements.
        //
        // Generic resource reloads do not re-read HMG's external pack settings,
        // so they cannot safely invalidate model objects.  The explicit HMG/HMV
        // reload commands own that destructive operation: they clear the caches,
        // reparse the packs, replace renderer references, and compile the new
        // models on the client thread.
    }
}
