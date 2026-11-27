package hmggvcmob.military.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import hmggvcmob.military.ArmyOrders.Order;
import hmggvcmob.military.MilitaryConfig;
import hmggvcmob.military.MilitaryNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public final class ArmyCommandScreen extends GuiScreen {
    private static final CommandInput INPUT = new CommandInput();
    private static final int GROUPS_PER_PAGE = 3;
    private static final int IVORY = 0xF2EDDE, MUTED = 0xB0B8AF, GOLD = 0xF2D38E;
    private static final String[] CATEGORIES = {"movement", "combat", "formation"};
    private static final Order[][] ORDERS = {
            {Order.FOLLOW, Order.MOVE, Order.HOLD, Order.WAIT, Order.REGROUP, Order.GARRISON},
            {Order.ATTACK, Order.DEFEND, Order.PATROL, Order.CEASE_FIRE, Order.FIRE_AT_WILL},
            {Order.LINE, Order.COLUMN, Order.LOOSE, Order.DOUBLE_COLUMN}
    };
    private final boolean radial;
    private NBTTagCompound data = new NBTTagCompound(), selected;
    private int category = -1, page, idle;
    private boolean browsing;

    private ArmyCommandScreen(boolean radial) { this.radial = radial; }

    public static void register() {
        ClientRegistry.registerKeyBinding(INPUT.radial);
        ClientRegistry.registerKeyBinding(INPUT.quick);
        FMLCommonHandler.instance().bus().register(INPUT);
        MinecraftForge.EVENT_BUS.register(INPUT);
    }

    public static boolean snapshot(int window, NBTTagCompound tag) {
        Minecraft client = Minecraft.getMinecraft();
        if (window == MilitaryNetwork.COMMAND_MENU_WINDOW) {
            if (client.currentScreen instanceof ArmyCommandScreen) ((ArmyCommandScreen) client.currentScreen).data = tag;
        } else if (window == MilitaryNetwork.COMMAND_MARKER_WINDOW) {
            INPUT.marker = tag;
            INPUT.markerWorld = client.theWorld;
            INPUT.markerTime = MilitaryConfig.markerTicks;
        } else return false;
        return true;
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    @Override public void updateScreen() {
        if (radial && !down(INPUT.radial) || !radial && ++idle >= MilitaryConfig.commandTimeout) close();
    }

    @Override public void handleKeyboardInput() {
        if (radial && !down(INPUT.radial)) {
            close();
            return;
        }
        super.handleKeyboardInput();
    }

    @Override public void handleMouseInput() {
        if (radial && !down(INPUT.radial)) {
            close();
            return;
        }
        if (radial && selected == null && Mouse.getEventDWheel() != 0) {
            turnPage(Mouse.getEventDWheel() < 0 ? 1 : -1);
        }
        super.handleMouseInput();
    }

    @Override protected void keyTyped(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE || !radial && key == INPUT.quick.getKeyCode()) {
            close();
            return;
        }
        if (radial || Keyboard.isRepeatEvent()) return;
        idle = 0;
        if (key == Keyboard.KEY_BACK) {
            back();
        } else if (selected == null && key == Keyboard.KEY_TAB) {
            browsing = !browsing;
            page = 0;
        } else if (selected == null && browsing && (key == Keyboard.KEY_PRIOR || key == Keyboard.KEY_NEXT)) {
            turnPage(key == Keyboard.KEY_NEXT ? 1 : -1);
        } else if (key >= Keyboard.KEY_1 && key <= Keyboard.KEY_0) {
            int digit = key == Keyboard.KEY_0 ? 0 : key - Keyboard.KEY_1 + 1;
            for (Choice choice : choices()) if (choice.key == digit) {
                choose(choice);
                break;
            }
        }
    }

    @Override protected void mouseClicked(int x, int y, int button) {
        if (button == 1) {
            back();
        } else if (radial && button == 0) {
            List<Choice> choices = choices();
            int slot = hovered(x, y, choices.size());
            if (slot >= 0) choose(choices.get(slot));
        }
    }

    private void back() {
        if (category >= 0) category = -1;
        else if (selected != null) selected = null;
        else if (!radial) close();
    }

    private void close() {
        if (mc.currentScreen != this) return;
        mc.displayGuiScreen(null);
        if (mc.inGameHasFocus) {
            GameSettings settings = mc.gameSettings;
            KeyBinding[] movement = {settings.keyBindForward, settings.keyBindBack, settings.keyBindLeft, settings.keyBindRight,
                    settings.keyBindJump, settings.keyBindSneak, settings.keyBindSprint};
            for (KeyBinding key : movement) KeyBinding.setKeyBindState(key.getKeyCode(), down(key));
        }
    }

    private void choose(Choice choice) {
        if (!choice.enabled) return;
        if (choice.row != null) {
            selected = choice.row;
        } else if (choice.order != null) {
            MilitaryNetwork.request(0, MilitaryNetwork.COMMAND_ORDER, choice.order.ordinal(), 0, selected.getString("Id"));
            if (radial) {
                category = -1;
                selected = null;
            } else close();
        } else if (choice.page != 0) {
            turnPage(choice.page);
        } else {
            category = choice.category;
        }
    }

    private void turnPage(int direction) {
        int pages = Math.max(1, (Math.max(0, groups().tagCount() - 2) + GROUPS_PER_PAGE - 1) / GROUPS_PER_PAGE);
        page = (page + direction + pages) % pages;
    }

    private NBTTagList groups() { return data.getTagList("Groups", 10); }

    private List<Choice> choices() {
        List<Choice> result = new ArrayList<>();
        if (selected == null) {
            NBTTagList groups = groups();
            if (groups.tagCount() < 2) return result;
            result.add(new Choice(groups.getCompoundTagAt(0), 0));
            NBTTagCompound ungrouped = groups.getCompoundTagAt(1);
            if (ungrouped.getInteger("Count") > 0) result.add(new Choice(ungrouped, 9));
            if (radial || browsing) {
                int first = 2 + page * GROUPS_PER_PAGE;
                for (int i = first; i < Math.min(first + GROUPS_PER_PAGE, groups.tagCount()); i++) {
                    result.add(new Choice(groups.getCompoundTagAt(i), i - first + 1));
                }
                if (radial && groups.tagCount() > 2 + GROUPS_PER_PAGE) result.add(new Choice(1));
            } else {
                for (int slot = 1; slot <= 8; slot++) for (int index = 2; index < groups.tagCount(); index++) {
                    NBTTagCompound row = groups.getCompoundTagAt(index);
                    if (row.getInteger("Slot") == slot) result.add(new Choice(row, slot));
                }
            }
        } else if (category < 0) {
            for (int index = 0; index < CATEGORIES.length; index++) result.add(new Choice(index, CATEGORIES[index]));
        } else {
            for (Order order : ORDERS[category]) result.add(new Choice(order, result.size() + 1, available(order)));
        }
        return result;
    }

    private boolean available(Order order) {
        switch (order) {
            case MOVE: case DEFEND: case PATROL: return data.getBoolean("Position");
            case ATTACK: return data.getBoolean("Target");
            case GARRISON: return data.getBoolean("Garrison");
            default: return true;
        }
    }

    private static String label(String key, Object... args) { return I18n.format("gvc.army." + key, args); }
    private static String groupName(NBTTagCompound row) {
        String id = row.getString("Id");
        return "all".equals(id) ? label("everyone") : "-".equals(id) ? label("ungrouped") : row.getString("Name");
    }

    private int radius() { return Math.min(100, Math.min(width / 2 - 18, height / 2 - 34)); }

    private int hub() { return Math.max(30, (int) (radius() * .44)); }

    private int hovered(int x, int y, int count) {
        int dx = x - width / 2, dy = y - height / 2;
        double distance = Math.hypot(dx, dy);
        if (count == 0 || distance < hub() + 3 || distance > radius() + 12) return -1;
        double arc = 2 * Math.PI / count;
        return (int) Math.floor((Math.atan2(dy, dx) + Math.PI / 2 + arc / 2 + 2 * Math.PI) / arc) % count;
    }

    @Override public void drawScreen(int x, int y, float partialTicks) {
        List<Choice> choices = choices();
        String heading = selected == null ? label(radial ? "wheel_title" : "select")
                : label("selected", groupName(selected), selected.getInteger("Count"));
        if (radial) drawWheel(x, y, heading, choices);
        else drawQuick(heading, choices);
    }

    private void drawWheel(int x, int y, String heading, List<Choice> choices) {
        int cx = width / 2, cy = height / 2;
        int radius = radius(), hub = hub();
        drawRect(0, 0, width, height, 0x63101617);
        plate(heading, cx, Math.max(17, cy - radius - 15), GOLD);
        plate(label("wheel_help"), cx, Math.min(height - 31, cy + radius + 8), MUTED);
        if (choices.isEmpty()) {
            plate(label("loading"), cx, cy - 4, IVORY);
            return;
        }
        int pointer = hovered(x, y, choices.size());
        double arc = 2 * Math.PI / choices.size();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT
                | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        disc(cx, cy, radius + 5, 0x64101414, 0x32101414);
        disc(cx, cy, radius + 1, 0xF3151F1D, 0xF5708074);
        disc(cx, cy, radius - 1, 0xF3151F1D, 0xF5151F1D);
        for (int i = 0; i < choices.size(); i++) {
            double angle = i * arc - Math.PI / 2;
            Choice choice = choices.get(i);
            boolean active = i == pointer && choice.enabled;
            drawWedge(cx, cy, hub, radius, angle, arc, choice.enabled, active);
            int iconX = iconX(cx, radius, angle), iconY = iconY(cy, radius, angle);
            disc(iconX, iconY, 9, 0xFF28392E, active ? 0xFFEDD18B : 0xFF759184);
            disc(iconX, iconY, 8, choice.enabled ? 0xFF23332E : 0xFF242A27, choice.enabled ? 0xFF3D5147 : 0xFF2C342F);
            choice.icon.draw(iconX - 4, iconY - 4, !choice.enabled ? 0x859087 : active ? GOLD : IVORY);
        }
        disc(cx, cy, hub + 1, 0xFF18201F, 0xFF778178);
        disc(cx, cy, hub - 1, 0xFF27342E, 0xFF121A19);
        GL11.glPopAttrib();
        for (int i = 0; i < choices.size(); i++) {
            double angle = i * arc - Math.PI / 2;
            Choice choice = choices.get(i);
            int limit = choices.size() > 4 ? 57 : 72;
            drawCenteredString(fontRendererObj, fit(choice.wheelText, limit), iconX(cx, radius, angle), iconY(cy, radius, angle) + 11,
                    !choice.enabled ? 0xA3ADA4 : i == pointer ? GOLD : IVORY);
        }
        drawHub(cx, cy, hub, pointer < 0 ? null : choices.get(pointer));
    }

    private static void drawWedge(int x, int y, int hub, int radius, double angle, double arc, boolean enabled, boolean active) {
        double start = angle - arc / 2 + .022, end = angle + arc / 2 - .022;
        sector(x, y, hub + 3, radius - 2, start, end,
                active ? 0xFFB99A62 : 0xFF506359, active ? 0xFFF2D38E : 0xFF55665E);
        sector(x, y, hub + 4, radius - (active ? 5 : 3), start + .013, end - .013,
                active ? 0xFF394740 : enabled ? 0xFF202D2B : 0xFF202726,
                active ? 0xFF687754 : enabled ? 0xFF40544B : 0xFF333B37);
        if (active) sector(x, y, radius - 2, radius + 2, angle - arc * .31, angle + arc * .31, 0xFFEDD18B, 0xFFBD9F62);
    }

    private void drawHub(int x, int y, int radius, Choice highlighted) {
        String title = highlighted != null ? highlighted.title
                : selected == null ? label("wheel_pick_group") : category < 0 ? label("wheel_pick_category") : label(CATEGORIES[category]);
        List<String> lines = fontRendererObj.listFormattedStringToWidth(title, radius * 2 - 12);
        int top = y - Math.min(lines.size(), 2) * 5 - 3;
        for (int i = 0; i < Math.min(lines.size(), 2); i++) drawCenteredString(fontRendererObj, lines.get(i), x, top + i * 10, IVORY);
        String hint = highlighted == null ? label("wheel_hover")
                : !highlighted.enabled ? label("unavailable")
                : highlighted.row != null ? label("wheel_count", highlighted.row.getInteger("Count"))
                : label(highlighted.order == null ? "wheel_open" : "wheel_issue");
        drawCenteredString(fontRendererObj, fit(hint, radius * 2 - 14), x, y + 13,
                highlighted == null || !highlighted.enabled ? MUTED : GOLD);
    }

    private void plate(String text, int center, int y, int tint) {
        text = fit(text, width - 22);
        int half = fontRendererObj.getStringWidth(text) / 2 + 7;
        drawRect(center - half, y - 3, center + half, y + 11, 0xDE1B2523);
        drawRect(center - half, y + 10, center + half, y + 11, 0xFF65736A);
        drawCenteredString(fontRendererObj, text, center, y, tint);
    }

    private String fit(String text, int width) {
        return fontRendererObj.getStringWidth(text) <= width ? text : fontRendererObj.trimStringToWidth(text, width - 7) + "…";
    }

    private static int iconX(int center, int radius, double angle) {
        return center + (int) (Math.cos(angle) * (radius * .73));
    }

    private static int iconY(int center, int radius, double angle) {
        return center + (int) (Math.sin(angle) * (radius * .73)) - 5;
    }

    private static void disc(int x, int y, int radius, int inside, int outside) {
        sector(x, y, 0, radius, 0, 2 * Math.PI, inside, outside);
    }

    private static void sector(int x, int y, int inner, int outer, double from, double to, int innerColor, int outerColor) {
        Tessellator mesh = Tessellator.instance;
        mesh.startDrawing(GL11.GL_TRIANGLE_STRIP);
        int steps = (int) Math.max(8, Math.ceil(64 * (to - from) / (2 * Math.PI)));
        for (int i = 0; i <= steps; i++) {
            double angle = from + i * (to - from) / steps;
            double cos = Math.cos(angle), sin = Math.sin(angle);
            mesh.setColorRGBA_I(innerColor & 0xFFFFFF, innerColor >>> 24);
            mesh.addVertex(x + inner * cos, y + inner * sin, 0);
            mesh.setColorRGBA_I(outerColor & 0xFFFFFF, outerColor >>> 24);
            mesh.addVertex(x + outer * cos, y + outer * sin, 0);
        }
        mesh.draw();
    }



    private void drawQuick(String heading, List<Choice> choices) {
        int x = 12, y = 24, right = Math.min(213, width - 12);
        drawRect(x, y, right, y + 31 + choices.size() * 13, 0xE0293229);
        drawRect(x, y, x + 2, y + 31 + choices.size() * 13, 0xFFCDAD62);
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(heading, right - x - 16), x + 8, y + 7, 0xE7CE87);
        y += 20;
        for (Choice choice : choices) {
            fontRendererObj.drawString(Integer.toString(choice.key), x + 9, y, 0xE7CE87);
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(choice.text, right - x - 26),
                    x + 22, y, choice.enabled ? 0xEEEBDD : 0x888E84);
            y += 13;
        }
        String help = selected == null ? label(browsing ? "quick_browse" : "quick_groups") : label("quick_back");
        fontRendererObj.drawString(help, x + 7, y, 0xBABDAC);
    }

    private static boolean down(KeyBinding key) {
        int code = key.getKeyCode();
        if (code < 0) return Mouse.isCreated() && code >= -100 && code + 100 < Mouse.getButtonCount() && Mouse.isButtonDown(code + 100);
        return code > 0 && code < Keyboard.KEYBOARD_SIZE && Keyboard.isCreated() && Keyboard.isKeyDown(code);
    }

    static String wheelKey() { return GameSettings.getKeyDisplayString(INPUT.radial.getKeyCode()); }
    static String quickKey() { return GameSettings.getKeyDisplayString(INPUT.quick.getKeyCode()); }

    private static final class Choice {
        final String text, wheelText, title;
        final boolean enabled;
        final int key, category, page;
        final CommandGlyph icon;
        final NBTTagCompound row;
        final Order order;

        Choice(NBTTagCompound row, int key) {
            this(groupName(row), groupName(row), key, groupIcon(row.getString("Id")), -1, 0, row, null,
                    row.getInteger("Count") > 0);
        }

        Choice(int category, String name) {
            this(label(name), label(name), category + 1,
                    category == 0 ? CommandGlyph.MOVEMENT : category == 1 ? CommandGlyph.ENGAGEMENT : CommandGlyph.FORMATION,
                    category, 0, null, null, true);
        }

        Choice(int page) {
            this(label("next_groups"), label("next_groups"), 0, CommandGlyph.NEXT, -1, page, null, null, true);
        }

        Choice(Order order, int key, boolean enabled) {
            this(I18n.format(order.key()), wheelLabel(order), key, CommandGlyph.valueOf(order.name()), -1, 0, null, order, enabled);
        }

        private Choice(String title, String wheelText, int key, CommandGlyph icon, int category, int page,
                       NBTTagCompound row, Order order, boolean enabled) {
            this.title = title;
            this.text = row == null ? title : title + "  " + row.getInteger("Count");
            this.wheelText = wheelText;
            this.enabled = enabled;
            this.key = key;
            this.icon = icon;
            this.category = category;
            this.page = page;
            this.row = row;
            this.order = order;
        }

        private static CommandGlyph groupIcon(String id) {
            return "all".equals(id) ? CommandGlyph.EVERYONE : "-".equals(id) ? CommandGlyph.UNGROUPED : CommandGlyph.GROUP;
        }

        private static String wheelLabel(Order order) {
            String key = order.key() + ".short";
            String text = I18n.format(key);
            return text.equals(key) ? I18n.format(order.key()) : text;
        }
    }



    public static final class CommandInput {
        final KeyBinding radial = new KeyBinding("gvc.army.key.wheel", Keyboard.KEY_LBRACKET, "gvc.army.key.category");
        final KeyBinding quick = new KeyBinding("gvc.army.key.quick", Keyboard.KEY_RBRACKET, "gvc.army.key.category");
        boolean lastRadial, lastQuick;
        NBTTagCompound marker;
        World markerWorld;
        int markerTime;

        @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;
            Minecraft mc = Minecraft.getMinecraft();
            boolean wheelPressed = down(radial), quickPressed = down(quick);
            if (mc.thePlayer != null && mc.inGameHasFocus && mc.currentScreen == null) {
                boolean wheel = MilitaryConfig.radialMenu && wheelPressed && !lastRadial;
                if (wheel || MilitaryConfig.quickCommands && quickPressed && !lastQuick) {
                    mc.displayGuiScreen(new ArmyCommandScreen(wheel));
                    MilitaryNetwork.request(0, MilitaryNetwork.COMMAND_MENU, 0);
                }
            }
            lastRadial = wheelPressed;
            lastQuick = quickPressed;
            if (markerTime > 0) markerTime--;
            if (markerTime <= 0 || markerWorld != mc.theWorld) {
                marker = null;
                markerWorld = null;
            }
        }

        @SubscribeEvent public void marker(RenderWorldLastEvent event) {
            Minecraft client = Minecraft.getMinecraft();
            EntityLivingBase view = client.renderViewEntity;
            if (marker == null || view == null || markerWorld != client.theWorld || client.gameSettings.hideGUI) return;
            double t = event.partialTicks;
            double x = marker.getDouble("X") - (view.lastTickPosX + (view.posX - view.lastTickPosX) * t);
            double y = marker.getDouble("Y") - (view.lastTickPosY + (view.posY - view.lastTickPosY) * t);
            double z = marker.getDouble("Z") - (view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * t);
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(false);
            GL11.glLineWidth(2);
            Tessellator mesh = Tessellator.instance;
            int alpha = Math.min(220, markerTime * 12);
            mesh.startDrawing(GL11.GL_LINE_LOOP);
            mesh.setColorRGBA_I(0xE7CE87, alpha);
            for (int i = 0; i < 32; i++) {
                double angle = 2 * Math.PI * i / 32;
                mesh.addVertex(x + .7 * Math.cos(angle), y + .045, z + .7 * Math.sin(angle));
            }
            mesh.draw();
            mesh.startDrawing(GL11.GL_LINES);
            mesh.setColorRGBA_I(0xE7CE87, alpha);
            mesh.addVertex(x, y + .045, z);
            mesh.addVertex(x, y + 2.0, z);
            mesh.draw();
            GL11.glPopAttrib();
        }
    }
}
