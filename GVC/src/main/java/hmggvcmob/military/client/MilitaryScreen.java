
package hmggvcmob.military.client;

import hmggvcmob.military.EntityMilitaryUnit;
import hmggvcmob.military.MilitaryContainer;
import hmggvcmob.military.MilitaryNetwork;
import hmggvcmob.military.MilitarySupplies;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class MilitaryScreen extends GuiContainer {
    private static final int WIDTH = 288, HEIGHT = 234;
    private static final int INK = 0xEEEBDD, MUTED = 0xBABDAC, GOLD = 0xE7CE87;
    private static final int GREEN = 0x9BCD96, YELLOW = 0xEAC36F, RED = 0xF09885;
    private static final int FRAME = 0xFF242824, PANEL = 0xFF343C33, INSET = 0xFF292F29;
    private static final int LINE = 0xFF576354, PICKED = 0xFF4B5947;
    private static final int LIST_X = 11, LIST_Y = 65, LIST_W = 125, ROW_H = 23, SCROLL_X = 138;
    private static final int RIGHT = 150;
    private static final ResourceLocation ICONS = new ResourceLocation("gvcmob:textures/gui/military_icons.png");
    private static final int OVERVIEW = 0, PERSONNEL = 1, TREASURY = 2, SUPPLIES = 3;
    private static final String[] TABS = {"overview", "personnel", "treasury", "supplies"};
    private static final int[] TAB_LEFT = {9, 75, 147, 213};
    private static final int[] TAB_WIDTH = {65, 71, 65, 65};
    private static final String[] FILTERS = {"everyone", "deployed", "away", "losses"};
    private static final int[] QUICK = {100, 500, 1000};
    private static int lastTab;

    private final MilitaryContainer container;
    private final boolean recruiting;
    private NBTTagCompound snapshot = new NBTTagCompound();
    private NBTTagCompound selected;
    private String selectedId = "";
    private String feedback = "";
    private GuiTextField amount;
    private NumberFormat numbers;
    private int tab, filter, offset, ticks, transaction, pending, confirmAmount;
    private boolean grouping, dragging;
    private int previewAmount = -1;
    private int[] previewDenominations = {};
    private String preview = "", portraitId = "";
    private EntityMilitaryUnit portrait;
    private List<String> hover = Collections.emptyList();

    public MilitaryScreen(EntityPlayer player, int trader) {
        super(new MilitaryContainer(player, trader));
        container = (MilitaryContainer)inventorySlots;
        recruiting = container.isRecruitment();
        tab = lastTab;
        xSize = WIDTH;
        ySize = HEIGHT;
    }

    public void applySnapshot(int window, NBTTagCompound data) {
        if (inventorySlots.windowId != window || data == null) return;
        snapshot = data;
        offset = data.getInteger("Offset");
        if (data.getInteger("Ack") >= pending && pending > 0) {
            feedback = data.getString("Result");
            pending = 0;
            refreshControls();
        }
        reconcileSelection();
    }

    @Override public void initGui() {
        String input = amount == null ? "" : amount.getText();
        super.initGui();
        Locale locale = Locale.forLanguageTag(mc.getLanguageManager().getCurrentLanguage().getLanguageCode().replace('_', '-'));
        numbers = NumberFormat.getNumberInstance(locale);
        numbers.setMaximumFractionDigits(2);
        amount = new GuiTextField(fontRendererObj, guiLeft + 157, guiTop + 69, 113, 15);
        amount.setMaxStringLength(7);
        amount.setText(input);
        container.positionInventory(tab);
        refreshControls();
    }

    private void refreshControls() {
        buttonList.clear();
        if (recruiting) {
            control(20, 157, 199, 113, 20, label("hire"));
            return;
        }
        for (int i = 0; i < TABS.length; i++) {
            styled(i, TAB_LEFT[i], 28, TAB_WIDTH[i], 19, label(TABS[i]), i, tab == i);
        }
        if (tab == PERSONNEL) {
            control(10, 11, 50, 68, 13, label(FILTERS[filter]));
            control(11, 81, 50, 64, 13, label(grouping ? "grouped" : "individual"));
        } else if (tab == TREASURY) {
            control(12, 42, 84, 91, 16, label("deposit_stack"));
            control(13, 17, 108, 116, 16, label("deposit_all_coins"));
            for (int i = 0; i < QUICK.length; i++) control(30 + i, 157 + i * 38, 87, 36, 13, number(QUICK[i]));
            control(14, 157, 114, 113, 17, label("withdraw"));
        } else if (tab == SUPPLIES) {
            control(15, 17, 108, 56, 16, label("deposit_one"));
            control(16, 77, 108, 56, 16, label("full_stack"));
        }
    }

    private void control(int id, int x, int y, int width, int height, String title) {
        styled(id, x, y, width, height, title, -1, false);
    }

    private void styled(int id, int x, int y, int width, int height, String title, int icon, boolean active) {
        buttonList.add(new DeskButton(id, guiLeft + x, guiTop + y, width, height, title, icon, active));
    }

    private void switchTab(int selected) {
        if (tab == selected) return;
        tab = lastTab = selected;
        feedback = "";
        confirmAmount = 0;
        amount.setFocused(false);
        container.positionInventory(tab);
        request(MilitaryNetwork.TAB, tab);
        refreshControls();
    }

    @Override protected void actionPerformed(GuiButton button) {
        int id = button.id;
        if (!recruiting && id < TABS.length) switchTab(id);
        if (id == 10) {
            filter = (filter + 1) % FILTERS.length;
            scrollTo(0);
            refreshControls();
        } else if (id == 11) {
            grouping = !grouping;
            scrollTo(0);
            refreshControls();
        } else if (id == 12) {
            transact(MilitaryNetwork.DEPOSIT_COINS, -1, "");
        } else if (id == 13) {
            transact(MilitaryNetwork.ALL_COINS, 0, "");
        } else if (id == 14) {
            withdraw();
        } else if (id == 15 || id == 16) {
            transact(MilitaryNetwork.DEPOSIT_FOOD, id == 15 ? 1 : -1, "");
        } else if (id == 20 && selected != null && offerReason(selected).isEmpty()) {
            transact(MilitaryNetwork.HIRE, 0, selectedId);
        } else if (id >= 30 && id < 30 + QUICK.length) {
            amount.setText(Integer.toString(QUICK[id - 30]));
            confirmAmount = 0;
            feedback = "";
        }
    }

    private void withdraw() {
        int value = withdrawal();
        if (value <= 0 || value > snapshot.getInteger("MaxWithdrawal")) {
            feedback = "amount";
        } else if (value > snapshot.getLong("Currency")) {
            feedback = "funds";
        } else if (confirmAmount != value) {
            confirmAmount = value;
            feedback = "confirm_withdraw";
        } else {
            confirmAmount = 0;
            transact(MilitaryNetwork.WITHDRAW, value, "");
        }
    }

    private void transact(int action, int value, String id) {
        if (pending != 0 || inventorySlots.windowId == 0) return;
        feedback = "";
        pending = ++transaction;
        MilitaryNetwork.request(inventorySlots.windowId, action, value, pending, id);
    }

    private void request(int action, int value) {
        if (inventorySlots.windowId != 0) MilitaryNetwork.request(inventorySlots.windowId, action, value);
    }

    private void scrollTo(int requested) {
        offset = Math.max(0, Math.min(maxScroll(), requested));
        request(MilitaryNetwork.ROSTER, offset | filter << 10 | (grouping ? 4096 : 0));
    }

    private int maxScroll() {
        return Math.max(0, snapshot.getInteger("Total") - MilitaryContainer.VISIBLE_ROWS);
    }

    private boolean hasList() { return recruiting || tab == PERSONNEL; }

    private NBTTagList rows() { return snapshot.getTagList("Rows", 10); }

    private void reconcileSelection() {
        NBTTagList rows = rows();
        for (int index = 0; index < rows.tagCount(); index++) {
            NBTTagCompound row = rows.getCompoundTagAt(index);
            if (row.getString("Id").equals(selectedId)) {
                selected = row;
                return;
            }
        }
        select(rows.tagCount() == 0 ? null : rows.getCompoundTagAt(0));
    }

    private void select(NBTTagCompound row) {
        selected = row;
        selectedId = row == null ? "" : row.getString("Id");
        feedback = "";
    }

    @Override public void handleMouseInput() {
        super.handleMouseInput();
        int x = Mouse.getEventX() * width / mc.displayWidth - guiLeft;
        int y = height - Mouse.getEventY() * height / mc.displayHeight - 1 - guiTop;
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && hasList() && x >= LIST_X && x < 146 && y >= LIST_Y && y < LIST_Y + ROW_H * MilitaryContainer.VISIBLE_ROWS) {
            scrollTo(offset + (wheel < 0 ? 1 : -1));
        }
    }

    @Override public void updateScreen() {
        super.updateScreen();
        ticks++;
        if (!recruiting && pending == 0 && ticks % 8 == 1 && snapshot.getInteger("View") != tab) request(MilitaryNetwork.TAB, tab);
        amount.updateCursorCounter();
        for (Object entry : buttonList) {
            GuiButton button = (GuiButton)entry;
            if (!(button instanceof DeskButton)) continue;
            switch (button.id) {
                case 12: button.enabled = pending == 0 && depositCount(0) > 0; break;
                case 13: button.enabled = pending == 0 && (snapshot.getLong("CarriedCoins") > 0 || depositCount(0) > 0); break;
                case 14:
                    button.enabled = pending == 0;
                    button.displayString = confirmAmount > 0 ? label("confirm") : label("withdraw");
                    break;
                case 15:
                case 16: button.enabled = pending == 0 && depositCount(1) > 0 && snapshot.getInteger("FoodDeposit") > 0; break;
                case 20:
                    button.enabled = pending == 0 && selected != null && offerReason(selected).isEmpty();
                    button.displayString = selected == null ? label("hire") : label("hire_for", number(selected.getInteger("Price")));
                    break;
                default: break;
            }
        }
    }

    @Override protected void keyTyped(char character, int key) {
        if (!recruiting && key == 15) {
            switchTab((tab + (isShiftKeyDown() ? 3 : 1)) % TABS.length);
            return;
        }
        if (tab == TREASURY && !recruiting && amount.isFocused() && key != 1) {
            if (key == 28 || key == 156) {
                withdraw();
                return;
            }
            if (amount.textboxKeyTyped(character, key)) {
                if (confirmAmount != withdrawal()) confirmAmount = 0;
                if (pending == 0) feedback = "";
                return;
            }
        }
        super.keyTyped(character, key);
    }

    @Override protected void mouseClicked(int x, int y, int button) {
        int relativeX = x - guiLeft, relativeY = y - guiTop;
        if (hasList() && button == 0 && relativeY >= LIST_Y && relativeY < LIST_Y + ROW_H * MilitaryContainer.VISIBLE_ROWS) {
            if (relativeX >= SCROLL_X && relativeX < 147 && maxScroll() > 0) {
                dragging = true;
                drag(relativeY);
                return;
            }
            if (relativeX >= LIST_X && relativeX < LIST_X + LIST_W) {
                int index = (relativeY - LIST_Y) / ROW_H;
                if (index < rows().tagCount()) select(rows().getCompoundTagAt(index));
                return;
            }
        }
        if (!recruiting && tab == TREASURY) amount.mouseClicked(x, y, button);
        super.mouseClicked(x, y, button);
    }

    @Override protected void mouseClickMove(int x, int y, int button, long duration) {
        if (dragging) drag(y - guiTop);
        else super.mouseClickMove(x, y, button, duration);
    }

    @Override protected void mouseMovedOrUp(int x, int y, int button) {
        if (dragging && button == 0) {
            dragging = false;
            drag(y - guiTop);
            return;
        }
        super.mouseMovedOrUp(x, y, button);
    }

    private void drag(int y) {
        int next = (int)Math.round((y - LIST_Y - 7) * (double)maxScroll() / (ROW_H * MilitaryContainer.VISIBLE_ROWS - 15));
        if (next != offset) scrollTo(next);
    }

    @Override public void drawScreen(int x, int y, float partialTicks) {
        hover = Collections.emptyList();
        super.drawScreen(x, y, partialTicks);
        if (!hover.isEmpty()) drawHoveringText(hover, x, y, fontRendererObj);
    }

    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        background(0, 0, WIDTH, HEIGHT, 0xFF161A16);
        background(1, 1, WIDTH - 1, HEIGHT - 1, FRAME);
        background(2, 2, WIDTH - 2, 3, LINE);
        background(3, 3, WIDTH - 3, 26, 0xFF30382F);
        if (hasList()) background(149, LIST_Y, 278, 224, PANEL);
        if (!recruiting && (tab == TREASURY || tab == SUPPLIES)) {
            background(9, 51, 140, 131, PANEL);
            background(149, 51, 279, 131, PANEL);
            slot(17, 84);
            for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) slot(61 + column * 18, 145 + row * 18);
            for (int column = 0; column < 9; column++) slot(61 + column * 18, 203);
            if (tab == TREASURY) amount.drawTextBox();
        }
    }

    private void background(int x, int y, int right, int bottom, int color) {
        drawRect(guiLeft + x, guiTop + y, guiLeft + right, guiTop + bottom, color);
    }

    private void slot(int x, int y) {
        background(x - 1, y - 1, x + 17, y + 17, LINE);
        background(x, y, x + 16, y + 16, INSET);
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        int x = mouseX - guiLeft, y = mouseY - guiTop;
        emblem(recruiting ? PERSONNEL : OVERVIEW, 10, 7);
        title(recruiting ? label("recruitment") : label("title"), 31, 11, 206);
        if (recruiting) roster(x, y);
        else if (tab == OVERVIEW) overview(x, y);
        else if (tab == PERSONNEL) roster(x, y);
        else if (tab == TREASURY) treasury(x, y);
        else supplies(x, y);
        if (!recruiting && y >= 28 && y < 47) {
            for (int i = 0; i < TABS.length; i++) {
                if (x >= TAB_LEFT[i] && x < TAB_LEFT[i] + TAB_WIDTH[i]) {
                    hover = Collections.singletonList(label(TABS[i] + "_tip"));
                    break;
                }
            }
        }
    }

    private void overview(int x, int y) {
        long balance = snapshot.getLong("Currency"), wage = snapshot.getLong("Wage");
        long food = snapshot.getLong("Food"), ration = snapshot.getLong("Ration");
        int active = snapshot.getInteger("Active"), deployed = snapshot.getInteger("Deployed"), warnings = snapshot.getInteger("Warnings");
        card(12, 56, TREASURY, label("account_balance"), number(balance), label("coins"));
        card(148, 56, PERSONNEL, label("army_size"), number(active), label("number_deployed", number(deployed)));
        card(12, 111, OVERVIEW, label("daily_wages"), number(daily(wage)), label("coins_day"));
        card(148, 111, SUPPLIES, label("food_reserve"), number(food), label("food_day", number(daily(ration))));

        drawRect(12, 168, 276, 223, INSET);
        boolean critical = active > 0 && (wage > balance || ration > food);
        double foodDays = days(food, ration);
        double wageDays = days(balance, wage);
        double runway = Math.min(foodDays, wageDays);
        int color = critical ? RED : warnings > 0 || active > 0 && runway < 2 ? YELLOW : GREEN;
        String readiness = active == 0 ? label("no_personnel")
                : critical ? label("critical") : warnings > 0 || runway < 2 ? label("low_reserves") : label("ready");
        emblem(active == 0 ? PERSONNEL : color == GREEN ? 5 : 4, 20, 174);
        title(readiness, 40, 178, 220, active == 0 ? MUTED : color);
        if (active == 0) {
            wrapped(label("visit_trader"), 22, 199, 244, 2, MUTED);
        } else {
            title(label("food_runway", time(foodDays)), 22, 196, 120);
            title(label("wage_runway", time(wageDays)), 146, 196, 122);
            drawRect(22, 211, 264, 216, 0xFF414940);
            int progress = (int)(Math.min(7, runway) / 7 * 242);
            drawRect(22, 211, 22 + progress, 216, 0xFF000000 | color);
        }
        if (inside(x, y, 12, 168, 264, 55) && active > 0) {
            hint(label("warn_count", number(warnings)), label("food_runway", realTime(foodDays)),
                    label("wage_runway", realTime(wageDays)), label("day_tip"));
        }
    }

    private void card(int x, int y, int icon, String heading, String value, String unit) {
        drawRect(x, y, x + 128, y + 48, PANEL);
        drawRect(x, y, x + 128, y + 1, LINE);
        emblem(icon, x + 6, y + 5);
        title(heading, x + 26, y + 9, 96);
        title(value, x + 9, y + 25, 111, GOLD);
        title(unit, x + 9, y + 37, 110, MUTED);
    }

    private void roster(int x, int y) {
        NBTTagList rows = rows();
        if (recruiting) {
            title(I18n.format(snapshot.getString("FactionLabel")), 11, 34, 135, MUTED);
            title(label("account_coins", number(snapshot.getLong("Currency"))), RIGHT, 34, 129, GOLD);
            title(label("reputation", number(snapshot.getInteger("Reputation"))), RIGHT, 47, 125, MUTED);
        }
        if (rows.tagCount() == 0) {
            drawRect(LIST_X, LIST_Y, LIST_X + LIST_W, LIST_Y + 49, INSET);
            wrapped(label(recruiting ? "no_offers" : "empty_roster"), LIST_X + 8, LIST_Y + 8, LIST_W - 16, 3, MUTED);
        }
        for (int i = 0; i < rows.tagCount(); i++) {
            NBTTagCompound row = rows.getCompoundTagAt(i);
            int rowY = LIST_Y + i * ROW_H;
            String identifier = row.getString("Id");
            boolean active = identifier.equals(selectedId);
            boolean pointed = inside(x, y, LIST_X, rowY, LIST_W, ROW_H);
            drawRect(LIST_X, rowY, LIST_X + LIST_W, rowY + ROW_H - 1, active || pointed ? PICKED : INSET);
            if (active) drawRect(LIST_X, rowY, LIST_X + 2, rowY + ROW_H - 1, 0xFFE7CE87);
            emblem(row.getInteger("Capacity") > 0 ? 6 : PERSONNEL, LIST_X + 5, rowY + 3);
            String name = row.getString("Name");
            int count = row.getInteger("Count");
            if (count > 1) name = label("unit_count", name, number(count));
            title(name, LIST_X + 24, rowY + 3, 97, INK);
            String sub = recruiting
                    ? label("offer_row", number(row.getInteger("Tier")), number(row.getInteger("Price")))
                    : label("status_row", number(row.getInteger("Tier")), status(row));
            int tint = recruiting ? offerColor(row) : statusColor(row);
            title(sub, LIST_X + 24, rowY + 13, 97, tint);
            if (pointed) hint(name, sub, recruiting ? offerHint(row) : assignment(row));
        }
        int track = ROW_H * MilitaryContainer.VISIBLE_ROWS;
        drawRect(SCROLL_X, LIST_Y, SCROLL_X + 6, LIST_Y + track, INSET);
        int total = snapshot.getInteger("Total");
        int thumb = Math.min(track, Math.max(15, track * MilitaryContainer.VISIBLE_ROWS / Math.max(1, total)));
        int top = LIST_Y + (maxScroll() == 0 ? 0 : offset * (track - thumb) / maxScroll());
        drawRect(SCROLL_X + 1, top, SCROLL_X + 5, top + thumb, LINE);
        if (selected != null) details(selected, x, y);
        else wrapped(label(recruiting ? "pick_offer" : "pick_unit"), 157, LIST_Y + 9, 111, 3, MUTED);
    }

    private void details(NBTTagCompound row, int x, int y) {
        wrapped(row.getString("Name"), 157, 72, 114, 2, INK);
        title(label("tier", number(row.getInteger("Tier"))), 157, 94, 34, GOLD);
        title(classification(row), 193, 94, 77, MUTED);
        drawRect(156, 106, 197, 155, INSET);
        drawRect(157, 107, 196, 108, LINE);
        previewEntity(row, x, y);
        title(label("health", number(row.hasKey("CurrentHealth") ? row.getFloat("CurrentHealth") : row.getFloat("Health"))), 202, 110, 70, INK);
        title(label("armor", number(row.getInteger("Armor"))), 202, 122, 70, MUTED);
        if (row.getInteger("Capacity") > 0) title(label("command", number(row.getInteger("Capacity"))), 202, 134, 70, MUTED);
        ItemStack weapon = ItemStack.loadItemStackFromNBT(row.getCompoundTag("Weapon"));
        String equipment = weapon == null ? label("no_weapon") : weapon.getDisplayName();
        title(equipment, 157, 158, 113, INK);
        title(label("unit_wage", number(daily(row.getLong("Wage")))), 157, 169, 113, MUTED);
        title(label("unit_ration", number(daily(row.getLong("Ration")))), 157, 180, 113, MUTED);

        if (recruiting) {
            String line = feedback.isEmpty() ? offerHint(row) : result(feedback);
            title(line, 157, 190, 113, feedback.isEmpty() ? offerColor(row) : "hired".equals(feedback) ? GREEN : RED);
        } else {
            title(assignment(row), 157, 203, 113, statusColor(row));
            if (row.hasKey("LeaderName")) title(label("leader", row.getString("LeaderName")), 157, 214, 113, MUTED);
            else if (row.hasKey("Squad") && row.getInteger("Count") == 1) title(label("independent"), 157, 214, 113, MUTED);
        }
        if (inside(x, y, 150, 64, 128, 128)) {
            List<String> lines = new ArrayList<>();
            lines.add(row.getString("Name"));
            lines.add(label("tier_class", number(row.getInteger("Tier")), classification(row)));
            lines.add(equipment);
            if (!row.getString("Description").isEmpty()) lines.addAll(fontRendererObj.listFormattedStringToWidth(row.getString("Description"), 180));
            lines.add(label("range", number(row.getInteger("Range"))));
            lines.add(label("resistance", number(row.getFloat("Resistance") * 100)));
            if (!recruiting) {
                lines.add(assignment(row));
                if (row.hasKey("LeaderName")) lines.add(label("leader", row.getString("LeaderName")));
            }
            hover = lines;
        }
    }

    private void previewEntity(NBTTagCompound row, int mouseX, int mouseY) {
        String key = row.getString("Id") + row.getString("Faction") + row.getCompoundTag("Weapon").toString();
        if (!key.equals(portraitId)) {
            portraitId = key;
            portrait = new EntityMilitaryUnit(mc.theWorld);
            portrait.configurePreview(row.getString("Faction"), ItemStack.loadItemStackFromNBT(row.getCompoundTag("Weapon")));
        }
        if (portrait == null) {
            emblem(PERSONNEL, 168, 122);
            return;
        }
        float previousYaw = RenderManager.instance.playerViewY;
        int depth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_COLOR_BUFFER_BIT
                | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_SCISSOR_BIT);
        try {
            int scale = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight).getScaleFactor();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor((guiLeft + 156) * scale, mc.displayHeight - (guiTop + 155) * scale, 41 * scale, 49 * scale);
            GuiInventory.func_147046_a(177, 151, 20,
                    Math.max(-18, Math.min(18, 175 - mouseX)), Math.max(-10, Math.min(10, 125 - mouseY)), portrait);
        } catch (RuntimeException failedRender) {
            portrait = null;
        } finally {
            RenderManager.instance.playerViewY = previousYaw;
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            while (GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) > depth) GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    private void treasury(int x, int y) {
        long deposit = depositCount(0) * (long)snapshot.getInteger("CoinDeposit");
        title(label("account_balance"), 17, 58, 116, MUTED);
        title(number(snapshot.getLong("Currency")), 17, 71, 116, GOLD);
        title(label("deposit_value", number(deposit)), 42, 101, 96, MUTED);
        title(label("withdraw_coins"), 157, 56, 114);
        updatePreview();
        title(preview.isEmpty() ? label("denomination_hint") : preview, 157, 102, 115, MUTED);
        if (inside(x, y, 157, 101, 115, 11) && !preview.isEmpty()) hint(label("denominations"), preview);
        if (inside(x, y, 17, 57, 116, 22)) hint(label("account_coins", number(snapshot.getLong("Currency"))));
        if (inside(x, y, 17, 108, 116, 16)) hint(label("all_coins_tip", number(snapshot.getLong("CarriedCoins") + deposit)));
        inventoryCaption();
        footer(feedback.isEmpty() ? label("shift_coins") : result(feedback), feedback.isEmpty() ? MUTED : feedbackColor());
    }

    private void supplies(int x, int y) {
        title(label("supply_count", number(snapshot.getLong("Food"))), 17, 58, 117, GOLD);
        title(label("ration_day", number(daily(snapshot.getLong("Ration")))), 17, 71, 117, MUTED);
        long value = depositCount(1) * (long)snapshot.getInteger("FoodDeposit");
        title(label("deposit_value", number(value)), 42, 89, 91, INK);
        title(label("remaining_food"), 157, 58, 115);
        title(time(days(snapshot.getLong("Food"), snapshot.getLong("Ration"))), 157, 73, 115, GOLD);
        if (value > 0 && snapshot.getLong("Ration") > 0) {
            title(label("adds_time", time(days(value, snapshot.getLong("Ration")))), 157, 91, 115, GREEN);
        }
        if (snapshot.getInteger("Warnings") > 0) title(label("supply_warning"), 157, 111, 115, YELLOW);
        if (inside(x, y, 17, 84, 116, 17)) hint(label("nutrients"), label("nutrient_tip"));
        if (inside(x, y, 157, 58, 115, 42)) hint(label("day_tip"),
                label("food_runway", realTime(days(snapshot.getLong("Food"), snapshot.getLong("Ration")))));
        inventoryCaption();
        footer(feedback.isEmpty() ? label("shift_food") : result(feedback), feedback.isEmpty() ? MUTED : feedbackColor());
    }

    private void inventoryCaption() {
        title(I18n.format("container.inventory"), 61, 135, 162, MUTED);
    }

    private void footer(String message, int color) {
        title(message, 35, 223, 241, color);
    }

    private int feedbackColor() {
        return "deposited".equals(feedback) || "supplied".equals(feedback) || "withdrawn".equals(feedback) ? GREEN
                : "confirm_withdraw".equals(feedback) ? YELLOW : RED;
    }

    private int depositCount(int slot) {
        ItemStack stack = container.getSlot(slot).getStack();
        return stack == null ? 0 : stack.stackSize;
    }

    private String assignment(NBTTagCompound row) {
        if (!"active".equals(row.getString("State"))) return status(row);
        String order = row.getString("Assignment");
        return order.isEmpty() || order.equals("unassigned") ? label("unassigned") : I18n.format("gvc.order.mode." + order);
    }

    private String classification(NBTTagCompound row) {
        String type = row.getString("Classification");
        if (type.isEmpty()) return label("infantry");
        String key = "gvc.military.class." + type;
        String translated = I18n.format(key);
        if (!translated.equals(key)) return translated;
        String plain = type.replace('_', ' ').replace('-', ' ');
        return Character.toUpperCase(plain.charAt(0)) + plain.substring(1);
    }

    private String status(NBTTagCompound row) {
        switch (row.getString("State")) {
            case "casualty": return label("fallen");
            case "deserted": return label("deserted");
            default: return label(row.getBoolean("Loaded") ? "deployed" : "out_of_area");
        }
    }

    private int statusColor(NBTTagCompound row) {
        return !row.getString("State").equals("active") ? RED : row.getBoolean("Loaded") ? GREEN : MUTED;
    }

    private String offerReason(NBTTagCompound row) {
        if (snapshot.getInteger("Reputation") < row.getInteger("Required")) return "needs_reputation";
        if (snapshot.getInteger("Active") >= snapshot.getInteger("Limit")) return "limit";
        if (snapshot.getLong("Currency") < row.getInteger("Price")) return "funds";
        return "";
    }

    private String offerHint(NBTTagCompound row) {
        String reason = offerReason(row);
        if (reason.equals("needs_reputation")) return label("needs_reputation", number(row.getInteger("Required")));
        return reason.isEmpty() ? label("available") : result(reason);
    }

    private int offerColor(NBTTagCompound row) {
        String reason = offerReason(row);
        return reason.isEmpty() ? GREEN : reason.equals("funds") ? YELLOW : RED;
    }

    private void updatePreview() {
        int value = withdrawal();
        int[] denominations = snapshot.getIntArray("Denominations");
        if (value == previewAmount && Arrays.equals(denominations, previewDenominations)) return;
        previewAmount = value;
        previewDenominations = denominations;
        int[] change = value <= snapshot.getInteger("MaxWithdrawal") ? MilitarySupplies.previewChange(value, denominations) : null;
        StringBuilder breakdown = new StringBuilder();
        if (change != null) {
            for (int i = change.length - 1; i >= 0; i--) {
                if (change[i] == 0) continue;
                if (breakdown.length() > 0) breakdown.append(" + ");
                breakdown.append(number(change[i])).append('×').append(number(denominations[i]));
            }
        }
        preview = breakdown.toString();
    }

    private int withdrawal() {
        try { return Integer.parseInt(amount.getText()); }
        catch (NumberFormatException invalid) { return 0; }
    }

    private double daily(long amount) { return amount * 24000.0 / Math.max(1, snapshot.getInteger("Interval")); }
    private double days(long amount, long rate) { return rate == 0 ? Double.POSITIVE_INFINITY : amount / daily(rate); }
    private String time(double days) {
        if (Double.isInfinite(days)) return label("no_usage");
        if (days < .05) return label("time_short_seconds", number(Math.round(days * 1200)));
        if (days < 1) return label("time_short_minutes", number(days * 20));
        return label("time_days", number(days));
    }
    private String realTime(double days) {
        if (Double.isInfinite(days)) return label("no_usage");
        return label("time_minutes", number(days * 20));
    }

    private String number(double value) { return numbers.format(value); }
    private String number(long value) { return numbers.format(value); }

    private void hint(String... lines) { hover = Arrays.asList(lines); }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void title(String value, int x, int y, int width) { title(value, x, y, width, INK); }

    private void title(String value, int x, int y, int width, int color) {
        fontRendererObj.drawString(fit(mc, value, width), x, y, color);
    }

    private void wrapped(String value, int x, int y, int width, int maximum, int color) {
        List<String> lines = fontRendererObj.listFormattedStringToWidth(value, width);
        for (int i = 0; i < Math.min(lines.size(), maximum); i++) {
            String line = lines.get(i);
            if (i == maximum - 1 && i < lines.size() - 1) line += "…";
            title(line, x, y + 10 * i, width, color);
        }
    }

    private static String fit(Minecraft client, String text, int width) {
        return client.fontRenderer.getStringWidth(text) <= width ? text : client.fontRenderer.trimStringToWidth(text, width - 6) + "…";
    }

    private static void emblem(int icon, int x, int y) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(ICONS);
        GL11.glColor4f(1, 1, 1, 1);
        Gui.func_146110_a(x, y, icon * 16, 0, 16, 16, 128, 16);
    }

    static String label(String key, Object... args) { return I18n.format("gvc.military.gui." + key, args); }

    private static String result(String key) { return I18n.format("gvc.military.result." + key); }

    private static final class DeskButton extends GuiButton {
        private final int icon;
        private final boolean selected;

        DeskButton(int id, int x, int y, int width, int height, String text, int icon, boolean selected) {
            super(id, x, y, width, height, text);
            this.icon = icon;
            this.selected = selected;
        }

        @Override public void drawButton(Minecraft client, int x, int y) {
            if (!visible) return;
            field_146123_n = inside(x, y, xPosition, yPosition, width, height);
            int face = !enabled ? 0xFF303630 : selected || field_146123_n ? PICKED : PANEL;
            drawRect(xPosition, yPosition, xPosition + width, yPosition + height, LINE);
            drawRect(xPosition + 1, yPosition + 1, xPosition + width - 1, yPosition + height - 1, face);
            if (selected) drawRect(xPosition + 1, yPosition + height - 2, xPosition + width - 1, yPosition + height - 1, 0xFFE7CE87);
            int left = xPosition + (icon < 0 ? 2 : 13);
            if (icon >= 0) {
                GL11.glPushMatrix();
                GL11.glTranslatef(xPosition + 1, yPosition + (height - 10) / 2F, 0);
                GL11.glScalef(.625F, .625F, 1);
                emblem(icon, 0, 0);
                GL11.glPopMatrix();
            }
            String title = fit(client, displayString, xPosition + width - left - 3);
            if (icon < 0) left = xPosition + (width - client.fontRenderer.getStringWidth(title)) / 2;
            client.fontRenderer.drawString(title, left, yPosition + (height - 8) / 2, enabled ? selected ? GOLD : INK : 0x91988B);
        }
    }
}
