package hmggvcmob.military.client;

import handmadeguns.guide.FieldManualScreen;
import hmggvcmob.military.MilitarySupplies;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public final class MilitaryFieldManual {
    private MilitaryFieldManual() {}

    public static void open(Minecraft client) {
        if (client.thePlayer == null) return;
        client.thePlayer.closeScreen();
        client.displayGuiScreen(new FieldManualScreen("gvc.manual.title", 0xFF3A4733,
                page("start", new ItemStack(Items.book)),
                page("recruit", new ItemStack(MilitarySupplies.coinItem(0))),
                page("upkeep", new ItemStack(Items.bread)),
                page("groups", new ItemStack(Items.leather_helmet)),
                page("orders", new ItemStack(Items.compass), ArmyCommandScreen.wheelKey(), ArmyCommandScreen.quickKey()),
                page("formation", new ItemStack(Items.map)),
                page("standing", new ItemStack(Items.paper)),
                page("allegiance", new ItemStack(Items.emerald)),
                page("capture", new ItemStack(Blocks.fence))));
    }

    private static FieldManualScreen.Page page(String id, ItemStack icon, Object... arguments) {
        return new FieldManualScreen.Page("gvc.manual." + id, icon, arguments);
    }
}
