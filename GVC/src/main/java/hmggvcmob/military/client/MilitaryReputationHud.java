package hmggvcmob.military.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import hmggvcmob.event.EventRenderPlatoonInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;

public final class MilitaryReputationHud {
    private static final MilitaryReputationHud INSTANCE = new MilitaryReputationHud();
    private final KeyBinding previous = new KeyBinding("gvc.reputation.key.previous", Keyboard.KEY_LEFT, "gvc.army.key.category");
    private final KeyBinding next = new KeyBinding("gvc.reputation.key.next", Keyboard.KEY_RIGHT, "gvc.army.key.category");
    private final KeyBinding pledged = new KeyBinding("gvc.reputation.key.pledged", Keyboard.KEY_UP, "gvc.army.key.category");
    private final KeyBinding visibility = new KeyBinding("gvc.reputation.key.toggle", Keyboard.KEY_DOWN, "gvc.army.key.category");
    private World world;
    private NBTTagList factions = new NBTTagList();
    private String allegiance = "";
    private int index;
    private boolean visible = true;
    private boolean showcase;

    private MilitaryReputationHud() {}

    public static void register() {
        for (KeyBinding key : new KeyBinding[]{INSTANCE.previous, INSTANCE.next, INSTANCE.pledged, INSTANCE.visibility}) {
            ClientRegistry.registerKeyBinding(key);
        }
        FMLCommonHandler.instance().bus().register(INSTANCE);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    public static void accept(World world, NBTTagCompound snapshot) {
        INSTANCE.update(world, snapshot);
    }

    public static int nearestSiteY(int screenWidth, int textWidth) {
        if (!INSTANCE.draws(Minecraft.getMinecraft()) || BossStatus.statusBarTime > 0) return 5;
        int left = (screenWidth - cardWidth(screenWidth)) / 2;
        return 5 + textWidth >= left ? 36 : 5;
    }

    private static int cardWidth(int screenWidth) { return Math.min(190, screenWidth - 32); }

    private boolean draws(Minecraft client) {
        return visible && !client.gameSettings.hideGUI && client.currentScreen == null && client.thePlayer != null
                && client.theWorld == world && factions.tagCount() != 0 && !showcase;
    }

    private void update(World current, NBTTagCompound snapshot) {
        String selected = world == current && factions.tagCount() > 0 ? factions.getCompoundTagAt(index).getString("Id") : "";
        world = current;
        factions = snapshot.getTagList("Factions", 10);
        allegiance = snapshot.getString("Allegiance");
        showcase = snapshot.getBoolean("Showcase");
        EventRenderPlatoonInfo.updateAllegiance(current, allegiance);
        int saved = find(selected.isEmpty() ? allegiance : selected);
        index = Math.max(0, saved);
    }

    private int find(String id) {
        for (int i = 0; i < factions.tagCount(); i++) {
            if (factions.getCompoundTagAt(i).getString("Id").equals(id)) return i;
        }
        return -1;
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getMinecraft();
        int step = 0;
        while (previous.isPressed()) step--;
        while (next.isPressed()) step++;
        boolean selectPledged = false, toggle = false;
        while (pledged.isPressed()) selectPledged = true;
        while (visibility.isPressed()) toggle = !toggle;
        if (client.currentScreen != null || client.theWorld != world || factions.tagCount() == 0) return;
        if (toggle) visible = !visible;
        if (step != 0) {
            index = Math.floorMod(index + step, factions.tagCount());
            visible = true;
        }
        if (selectPledged && find(allegiance) >= 0) {
            index = find(allegiance);
            visible = true;
        }
    }

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.Post event) {
        Minecraft client = Minecraft.getMinecraft();
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT || !draws(client)) return;
        NBTTagCompound faction = factions.getCompoundTagAt(index);
        FontRenderer font = client.fontRenderer;
        int width = cardWidth(event.resolution.getScaledWidth());
        int x = (event.resolution.getScaledWidth() - width) / 2;
        int y = BossStatus.statusBarTime > 0 ? 27 : 6;
        int color = 0xFF000000 | faction.getInteger("Color");
        int score = faction.getInteger("Reputation");
        String value = (score > 0 ? "+" : "") + score;
        int right = x + width - 12 - font.getStringWidth(value);
        Gui.drawRect(x, y, x + width, y + 27, 0xD51A211F);
        Gui.drawRect(x, y, x + width, y + 1, color);
        font.drawStringWithShadow("<", x + 4, y + 4, 0xAAB9B1);
        font.drawStringWithShadow(">", x + width - 9, y + 4, 0xAAB9B1);
        font.drawStringWithShadow(font.trimStringToWidth(faction.getString("Name"), Math.max(0, right - x - 19)), x + 13, y + 4, color);
        font.drawStringWithShadow(value, right, y + 4, score < 0 ? 0xE5A08E : 0xF0F4EA);
        font.drawString(font.trimStringToWidth(faction.getString("Standing"), width - 70), x + 13, y + 15, 0xD9E1D8);
        int meterRight = x + width - 12, meterLeft = meterRight - 48, middle = meterLeft + 24;
        int fill = (int)Math.round(24 * Math.min(1, Math.log1p(Math.abs((double)score)) / Math.log1p(10000)));
        Gui.drawRect(meterLeft, y + 18, meterRight, y + 21, 0xFF41504A);
        if (score < 0) Gui.drawRect(middle - fill, y + 18, middle, y + 21, 0xFFE5A08E);
        else if (score > 0) Gui.drawRect(middle, y + 18, middle + fill, y + 21, color);
        Gui.drawRect(middle, y + 17, middle + 1, y + 22, 0xFFCBD9CE);
    }
}
