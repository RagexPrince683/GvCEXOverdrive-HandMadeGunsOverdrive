
package hmggvcmob.military.client;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import hmggvcmob.military.MilitaryNetwork;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraftforge.client.event.GuiScreenEvent;

import java.lang.reflect.Field;

public final class MilitaryClientEvents {
    private static final Field LEFT = ReflectionHelper.findField(GuiContainer.class, "guiLeft", "field_147003_i");
    private static final Field TOP = ReflectionHelper.findField(GuiContainer.class, "guiTop", "field_147009_r");
    private static final Field WIDTH = ReflectionHelper.findField(GuiContainer.class, "xSize", "field_146999_f");

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void addButton(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiInventory) && !(event.gui instanceof GuiContainerCreative)) return;
        int left, top, panelWidth;
        try {
            left = LEFT.getInt(event.gui);
            top = TOP.getInt(event.gui);
            panelWidth = WIDTH.getInt(event.gui);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException(failure);
        }
        int x = left + panelWidth + 2;
        int buttonWidth = Math.min(55, event.gui.width - 2 - x);
        if (buttonWidth < 20) {
            x = left + panelWidth - 20;
            buttonWidth = 18;
        }
        event.buttonList.add(new MilitaryButton(x, top + 5, buttonWidth));
    }

    @SubscribeEvent
    public void open(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.button instanceof MilitaryButton) {
            event.setCanceled(true);
            MilitaryNetwork.request(0, MilitaryNetwork.OPEN, 0);
        }
    }

    private static final class MilitaryButton extends GuiButton {
        MilitaryButton(int x, int y, int width) {
            super(714, x, y, width, 20, width < 46 ? "GVC" : MilitaryScreen.label("button"));
        }
    }
}
