package handmadeguns.loading;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import java.util.Map;

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
    public void injectData(Map<String, Object> data) { }

    @Override
    public String getAccessTransformerClass() { return null; }
}
