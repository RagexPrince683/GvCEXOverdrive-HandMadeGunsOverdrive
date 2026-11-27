package handmadeguns.guide;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import handmadeguns.ClientProxyHMG;
import handmadeguns.HandmadeGunsCore;
import handmadeguns.client.HMGManualGunPickupClientHandler;
import handmadeguns.items.HGBaseItems;
import handmadeguns.items.HGGunItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@SideOnly(Side.CLIENT)
public final class HMGFieldManual {
    private HMGFieldManual() {}

    public static void open(Minecraft client) {
        client.thePlayer.closeScreen();
        client.displayGuiScreen(new FieldManualScreen("hmg.manual.title", 0xFF304853,
                page("start", Items.book),
                page("smithing", HGBaseItems.polymer),
                page("ammunition", Items.arrow, key(ClientProxyHMG.ChangeMagazineType.keyBinding)),
                page("reload", Items.gold_nugget, key(ClientProxyHMG.Reload.keyBinding)),
                page("controls", Items.compass, key(ClientProxyHMG.Mode.keyBinding),
                        key(HMGManualGunPickupClientHandler.PICKUP_KEY)),
                page("attachments", HGGunItems.opticMount, key(ClientProxyHMG.gunPrepare_modification.keyBinding),
                        key(ClientProxyHMG.Attachment.keyBinding), key(ClientProxyHMG.Fire_AttachedGun.keyBinding)),
                page("trouble", Items.flint, key(ClientProxyHMG.Fix.keyBinding)),
                page("packs", HGGunItems.precisionComponents)));
    }

    private static FieldManualScreen.Page page(String name, Item icon, Object... args) {
        return new FieldManualScreen.Page("hmg.manual." + name, new ItemStack(icon == null ? Items.book : icon), args);
    }

    private static String key(KeyBinding key) { return GameSettings.getKeyDisplayString(key.getKeyCode()); }
}
