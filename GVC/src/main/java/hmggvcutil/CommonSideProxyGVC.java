package hmggvcutil;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import hmggvcutil.Item.GVCItemForTranslate;
import net.minecraft.item.Item;
import net.minecraft.world.World;

import static hmggvcutil.GVCUtils.*;

public class CommonSideProxyGVC {

	public void registerClientInfo(){}

	public void IGuiHandler(){}

    public void reisterRenderers(){}

	public World getCilentWorld(){
		return null;}


	public void registerGuns(FMLPreInitializationEvent event) {
		fn_ak74 = registerLegacyGun("AK74");
		fn_rpk74 = registerLegacyGun("RPK74");
		fn_aks74u = registerLegacyGun("AKS74U");
		fn_m10 = registerLegacyGun("M10");
		fn_m1911 = registerLegacyGun("M1911");
		fn_m870 = registerLegacyGun("M870");
		fn_svd = registerLegacyGun("SVD");
		fn_pkm = registerLegacyGun("PKM");
		fn_m16a4 = registerLegacyGun("M16A4");
		fn_m4a1 = registerLegacyGun("M4A1");
		fn_m249 = registerLegacyGun("M249");
		fn_m9 = registerLegacyGun("M9");
		fn_g17 = registerLegacyGun("G17");
		fn_g18 = registerLegacyGun("G18C", "G18", "G18");
		fn_m110 = registerLegacyGun("M110");
		fn_m240b = registerLegacyGun("M240B");
		fn_r700 = registerLegacyGun("R700");
		fn_m82a3 = registerLegacyGun("M82A3");
		fn_g36 = registerLegacyGun("G36");
		fn_g36c = registerLegacyGun("G36C");
		fn_mg36 = registerLegacyGun("MG36");
		fn_rpg7 = registerLegacyGun("RPG7");
		fn_smaw = registerLegacyGun("SMAW", "SMAW", "smaw");
		fn_m134 = registerLegacyGun("M134");
		fn_m2f = registerLegacyGun("M2F");
		modelscala = 0.5F;
	}

	private Item registerLegacyGun(String name) {
		return registerLegacyGun(name, name, name);
	}

	private Item registerLegacyGun(String registryName, String name, String texture) {
		Item item = new GVCItemForTranslate().setUnlocalizedName(name)
				.setTextureName("gvcguns:" + texture).setMaxStackSize(1);
		GameRegistry.registerItem(item, registryName);
		return item;
	}

	public void registerTileEntity() {
		//GameRegistry.registerTileEntity(GVCTileEntityBlockIED.class, "GVCTileEntityBlockIED");
	}

	public boolean reload(){
		return false;
	}

}