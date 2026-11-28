
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
import java.util.LinkedHashSet;
import java.util.Set;

public final class MilitaryScreen extends GuiContainer {
    private static final int WIDTH = 288, HEIGHT = 234;
    private static final int INK = 0xEEEBDD, MUTED = 0xBABDAC, GOLD = 0xE7CE87;
    private static final int GREEN = 0x9BCD96, YELLOW = 0xEAC36F, RED = 0xF09885;
    private static final int FRAME = 0xFF242824, PANEL = 0xFF343C33, INSET = 0xFF292F29;
    private static final int LINE = 0xFF576354, PICKED = 0xFF4B5947;
    private static final int LIST_X = 11, LIST_Y = 65, LIST_W = 125, ROW_H = 23, SCROLL_X = 138;
    private static final int RIGHT = 150;
    private static final ResourceLocation ICONS = new ResourceLocation("gvcmob:textures/gui/military_icons.png");
    private static final int OVERVIEW = 0, PERSONNEL = 1, TREASURY = 2, SUPPLIES = 3, FACTIONS = 4, JOURNAL = 5;
    private static final String[] TABS = {"overview", "personnel", "treasury", "supplies", "factions", "quests"};
    private static final int[] TAB_LEFT = {9, 55, 103, 150, 196, 242};
    private static final int[] TAB_WIDTH = {45, 47, 46, 45, 45, 37};
    private static final String[] FILTERS = {"everyone", "deployed", "away", "losses"};
    private static final int[] QUICK = {100, 500, 1000};
    private static int lastTab;

    private final MilitaryContainer container;
    private final boolean recruiting;
    private NBTTagCompound snapshot = new NBTTagCompound();
    private NBTTagCompound selected;
    private String selectedId = "";
    private String feedback = "";
    private GuiTextField amount, groupName;
    private NumberFormat numbers;
    private int tab, filter, offset, ticks, transaction, pending, confirmAmount;
    private boolean grouping, dragging;
    private int previewAmount = -1;
    private int[] previewDenominations = {};
    private String preview = "", portraitId = "";
    private EntityMilitaryUnit portrait;
    private List<String> hover = Collections.emptyList();
    private final Set<String> selectedUnits = new LinkedHashSet<>();
    private boolean organization, settlementView, allegianceView;
    private int serviceTab = MilitaryContainer.RECRUIT;
    private final TradePanel commerce = new TradePanel();
    private final QuestPanel journal = new QuestPanel();
    private int groupOffset, factionOffset, nameAction = -1, confirmation = -1;
    private String selectedGroup = "-", selectedFaction = "";

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
        boolean hadAllegiance = !snapshot.getString("Allegiance").isEmpty();
        snapshot = data;
        if (recruiting && !snapshot.getString("Role").equals(priorRole)) {
            priorRole = snapshot.getString("Role");
            if (!snapshot.getBoolean("CanRecruit") && snapshot.getBoolean("CanBuy")) selectService(MilitaryContainer.BUY);
            else if (!snapshot.getBoolean("CanRecruit") && snapshot.getBoolean("CanSell")) selectService(MilitaryContainer.SELL);
            else if (!snapshot.getBoolean("CanRecruit") && snapshot.getBoolean("QuestsEnabled")) selectService(MilitaryContainer.CONTRACTS);
            else refreshControls();
        }
        if (trading()) commerce.reconcile();
        if (questView()) journal.reconcile();
        offset = data.getInteger("Offset");
        if (hasList()) reconcileSelection();
        if (data.getInteger("Ack") >= pending && pending > 0) {
            feedback = data.getString("Result");
            pending = 0;
            refreshControls();
        } else if (tab == FACTIONS && hadAllegiance != !snapshot.getString("Allegiance").isEmpty()) {
            refreshControls();
        }
        if (group(selectedGroup) == null) selectedGroup = "-";
        if (faction(selectedFaction) == null && factions().tagCount() > 0) selectedFaction = factions().getCompoundTagAt(0).getString("Id");
        groupOffset = Math.min(groupOffset, Math.max(0, groups().tagCount() - 3));
    }

    @Override public void initGui() {
        String input = amount == null ? "" : amount.getText();
        String groupInput = groupName == null ? "" : groupName.getText();
        super.initGui();
        Locale locale = Locale.forLanguageTag(mc.getLanguageManager().getCurrentLanguage().getLanguageCode().replace('_', '-'));
        numbers = NumberFormat.getNumberInstance(locale);
        numbers.setMaximumFractionDigits(2);
        amount = new GuiTextField(fontRendererObj, guiLeft + 157, guiTop + 69, 113, 15);
        amount.setMaxStringLength(7);
        amount.setText(input);
        groupName = new GuiTextField(fontRendererObj, guiLeft + 156, guiTop + 127, 115, 16);
        groupName.setMaxStringLength(32);
        groupName.setText(groupInput);
        commerce.init();
        container.positionInventory(tab);
        refreshControls();
    }

    private void refreshControls() {
        buttonList.clear();
        if (recruiting) {
            int left = 10;
            if (snapshot.getBoolean("CanRecruit")) {
                styled(70, left, 28, 51, 17, label("recruit_tab"), -1, serviceTab == MilitaryContainer.RECRUIT);
                left += 53;
            }
            if (snapshot.getBoolean("CanBuy")) {
                styled(71, left, 28, 51, 17, label("buy"), -1, serviceTab == MilitaryContainer.BUY);
                left += 53;
            }
            if (snapshot.getBoolean("CanSell")) {
                styled(72, left, 28, 51, 17, label("sell"), -1, serviceTab == MilitaryContainer.SELL);
                left += 53;
            }
            if (snapshot.getBoolean("QuestsEnabled")) {
                styled(74, left, 28, 49, 17, label("quests"), -1, serviceTab == MilitaryContainer.CONTRACTS);
                left += 51;
            }
            if (snapshot.getBoolean("CanRecruit")) styled(73, left, 28, 58, 17, label("allegiance"), -1, allegianceView);
            if (trading()) {
                commerce.controls();
                return;
            }
            if (questView()) {
                journal.controls();
                return;
            }
            if (allegianceView) control(61, 157, 199, 113, 20, label("pledge"));
            else control(20, 157, 199, 113, 20, label("hire"));
            return;
        }
        for (int i = 0; i < TABS.length; i++) {
            styled(i, TAB_LEFT[i], 28, TAB_WIDTH[i], 19, label(TABS[i]), -1, tab == i);
        }
        if (tab == PERSONNEL) {
            control(10, 11, 50, 68, 13, label(FILTERS[filter]));
            control(11, 81, 50, 64, 13, label(grouping ? "grouped" : "individual"));
            control(40, 151, 50, organization ? 67 : 127, 13, label(organization ? "unit_details" : "manage_groups"));
            if (organization) groupControls();
        } else if (tab == TREASURY) {
            control(12, 42, 84, 91, 16, label("deposit_stack"));
            control(13, 17, 108, 116, 16, label("deposit_all_coins"));
            for (int i = 0; i < QUICK.length; i++) control(30 + i, 157 + i * 38, 87, 36, 13, number(QUICK[i]));
            control(14, 157, 114, 113, 17, label("withdraw"));
        } else if (tab == SUPPLIES) {
            control(15, 17, 108, 56, 16, label("deposit_one"));
            control(16, 77, 108, 56, 16, label("full_stack"));
        } else if (tab == FACTIONS) {
            control(63, 151, 50, 127, 13, label(settlementView ? "faction_details" : "nearby_settlement"));
            if (settlementView) {
                control(64, 155, 171, 117, 17, label("claim_faction"));
                control(65, 155, 190, 117, 17, label("neutralize"));
            } else if (!snapshot.getString("Allegiance").isEmpty()) {
                control(62, 155, 205, 117, 17, label("renounce"));
            }
        } else if (tab == JOURNAL) journal.controls();
        if (tab == OVERVIEW) control(66, 219, 7, 58, 15, label("field_manual"));
    }

    private void groupControls() {
        control(41, 221, 50, 16, 13, "<");
        control(42, 240, 50, 16, 13, ">");
        control(43, 259, 50, 16, 13, "+");
        if (nameAction >= 0) {
            control(44, 155, 151, 56, 16, label("save"));
            control(45, 215, 151, 57, 16, label("cancel"));
            return;
        }
        control(46, 154, 167, 59, 13, label("view_group"));
        control(47, 216, 167, 57, 13, label("assign_group"));
        control(48, 154, 181, 59, 13, label("set_leader"));
        control(49, 216, 181, 57, 13, label("quick_slot"));
        control(50, 154, 195, 59, 13, label("rename"));
        control(51, 216, 195, 57, 13, label("delete_group"));
        control(52, 154, 209, 28, 13, "↑");
        control(53, 185, 209, 28, 13, "↓");
        control(54, 216, 209, 57, 13, label("clear_leader"));
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
        confirmation = -1;
        nameAction = -1;
        amount.setFocused(false);
        container.positionInventory(tab);
        request(MilitaryNetwork.TAB, tab);
        refreshControls();
    }

    @Override protected void actionPerformed(GuiButton button) {
        int id = button.id;
        if (recruiting && id >= 70 && id <= 74) {
            selectService(MilitaryContainer.RECRUIT + id - 70);
            return;
        }
        if (trading() && id >= 80 && id <= 86) {
            commerce.action(id);
            return;
        }
        if (questView() && id >= 90 && id <= 94) {
            journal.action(id);
            return;
        }
        if (!recruiting && id < TABS.length) switchTab(id);
        if (id == 10) {
            filter = (filter + 1) % FILTERS.length;
            scrollTo(0);
            refreshControls();
        } else if (id == 11) {
            grouping = !grouping;
            scrollTo(0);
            refreshControls();
        } else if (id == 40) {
            organization = !organization;
            nameAction = -1;
            if (organization && grouping) {
                grouping = false;
                scrollTo(0);
            }
            refreshControls();
        } else if (id >= 41 && id <= 54) {
            groupAction(id);
        } else if (id >= 60 && id <= 65) {
            factionAction(id);
        } else if (id == 66) {
            MilitaryFieldManual.open(mc);
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

    private void groupAction(int id) {
        if (id == 41 || id == 42) {
            groupOffset = Math.max(0, Math.min(Math.max(0, groups().tagCount() - 3), groupOffset + (id == 41 ? -3 : 3)));
        } else if (id == 43 || id == 50) {
            nameAction = id == 43 ? 0 : 1;
            NBTTagCompound current = group(selectedGroup);
            groupName.setText(id == 43 || current == null ? "" : current.getString("Name"));
            groupName.setFocused(true);
            refreshControls();
        } else if (id == 44) {
            saveGroupName();
        } else if (id == 45) {
            nameAction = -1;
            refreshControls();
        } else if (id == 46) {
            String filter = snapshot.getString("GroupFilter");
            MilitaryNetwork.request(inventorySlots.windowId, MilitaryNetwork.GROUP_FILTER, 0, 0,
                    selectedGroup.equals(filter) ? "all" : selectedGroup);
        } else if (id == 47 && !selectedUnits.isEmpty()) {
            transact(MilitaryNetwork.GROUP_EDIT, 3, selectedGroup + "|" + String.join(",", selectedUnits));
        } else if (id == 48 && selectedUnits.size() == 1) {
            transact(MilitaryNetwork.GROUP_EDIT, 4, selectedGroup + "|" + selectedUnits.iterator().next());
        } else if (id == 49) {
            NBTTagCompound row = group(selectedGroup);
            if (row != null) transact(MilitaryNetwork.GROUP_EDIT, 6, selectedGroup + "|" + (row.getInteger("Slot") + 1) % 9);
        } else if (id == 51) {
            if (confirmation == id) {
                transact(MilitaryNetwork.GROUP_EDIT, 2, selectedGroup);
                confirmation = -1;
            } else {
                confirmation = id;
                feedback = "confirm_delete_group";
            }
        } else if (id == 52 || id == 53) {
            transact(MilitaryNetwork.GROUP_EDIT, 5, selectedGroup + (id == 52 ? "|up" : "|down"));
        } else if (id == 54) {
            transact(MilitaryNetwork.GROUP_EDIT, 4, selectedGroup + "|");
        }
    }

    private void saveGroupName() {
        String name = groupName.getText().trim();
        if (name.isEmpty() || pending != 0) return;
        transact(MilitaryNetwork.GROUP_EDIT, nameAction, nameAction == 0 ? name : selectedGroup + "|" + name);
        nameAction = -1;
        refreshControls();
    }

    private void factionAction(int id) {
        if (id == 60 || id == 63) {
            if (id == 60) allegianceView = !allegianceView;
            else settlementView = !settlementView;
            confirmation = -1;
            feedback = "";
            refreshControls();
        } else if (confirmation != id) {
            confirmation = id;
            feedback = "";
        } else {
            confirmation = -1;
            if (id == 61 || id == 62) {
                transact(MilitaryNetwork.PLEDGE, 0, id == 61 ? snapshot.getString("FactionId") : "");
            } else {
                transact(MilitaryNetwork.CAPTURE, id == 64 ? 1 : 0, snapshot.getCompoundTag("Settlement").getString("Id"));
            }
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

    private boolean hasList() { return recruiting ? serviceTab == MilitaryContainer.RECRUIT : tab == PERSONNEL; }

    private String priorRole = "";

    private boolean trading() {
        return recruiting && (serviceTab == MilitaryContainer.BUY || serviceTab == MilitaryContainer.SELL);
    }

    private boolean questView() {
        return recruiting ? serviceTab == MilitaryContainer.CONTRACTS : tab == JOURNAL;
    }

    private void selectService(int service) {
        serviceTab = service;
        allegianceView = service == MilitaryContainer.ALLEGIANCE;
        confirmation = -1;
        feedback = "";
        request(MilitaryNetwork.TAB, service);
        if (trading()) commerce.query(0);
        if (questView()) journal.reconcile();
        refreshControls();
    }

    private NBTTagList rows() { return snapshot.getTagList("Rows", 10); }
    private NBTTagList groups() { return snapshot.getTagList("Groups", 10); }
    private NBTTagList factions() { return snapshot.getTagList("Factions", 10); }

    private NBTTagCompound group(String id) { return findRow(groups(), id); }
    private NBTTagCompound faction(String id) { return findRow(factions(), id); }

    private static NBTTagCompound findRow(NBTTagList list, String id) {
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound row = list.getCompoundTagAt(i);
            if (id.equals(row.getString("Id"))) return row;
        }
        return null;
    }

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
        if (wheel != 0 && trading() && inside(x, y, 10, 83, 135, 117)) {
            commerce.query(snapshot.getInteger("TradeOffset") + (wheel < 0 ? 6 : -6));
        } else if (wheel != 0 && hasList() && x >= LIST_X && x < 146 && y >= LIST_Y && y < LIST_Y + ROW_H * MilitaryContainer.VISIBLE_ROWS) {
            scrollTo(offset + (wheel < 0 ? 1 : -1));
        } else if (wheel != 0 && !recruiting && tab == PERSONNEL && organization && x >= 150 && y >= 64 && y < 106) {
            groupOffset = Math.max(0, Math.min(Math.max(0, groups().tagCount() - 3), groupOffset + (wheel < 0 ? 1 : -1)));
        } else if (wheel != 0 && !recruiting && tab == FACTIONS && x >= LIST_X && x < 146 && y >= LIST_Y) {
            factionOffset = Math.max(0, Math.min(Math.max(0, factions().tagCount() - 6), factionOffset + (wheel < 0 ? 1 : -1)));
        }
    }

    @Override public void updateScreen() {
        super.updateScreen();
        ticks++;
        if (!recruiting && pending == 0 && ticks % 8 == 1 && snapshot.getInteger("View") != tab) request(MilitaryNetwork.TAB, tab);
        if (recruiting && pending == 0 && ticks % 8 == 1 && snapshot.getInteger("View") != serviceTab) request(MilitaryNetwork.TAB, serviceTab);
        amount.updateCursorCounter();
        groupName.updateCursorCounter();
        NBTTagCompound group = group(selectedGroup), pledge = faction(snapshot.getString("FactionId"));
        NBTTagCompound settlement = snapshot.getCompoundTag("Settlement");
        boolean custom = group != null && !"-".equals(selectedGroup);
        boolean ready = pending == 0;
        if (trading()) commerce.update();
        if (questView()) journal.update();
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
                case 11: button.enabled = !organization; break;
                case 41: button.enabled = groupOffset > 0; break;
                case 42: button.enabled = groupOffset + 3 < groups().tagCount(); break;
                case 43: button.enabled = ready && groups().tagCount() - 1 < snapshot.getInteger("GroupLimit"); break;
                case 46:
                    button.enabled = group != null;
                    button.displayString = label(selectedGroup.equals(snapshot.getString("GroupFilter")) ? "view_all" : "view_group");
                    break;
                case 47: button.enabled = ready && group != null && !selectedUnits.isEmpty(); break;
                case 48: button.enabled = ready && custom && selectedUnits.size() == 1; break;
                case 49:
                    button.enabled = ready && custom;
                    button.displayString = label("slot_number", group == null || group.getInteger("Slot") == 0 ? "–" : group.getInteger("Slot"));
                    break;
                case 50: case 51: case 52: case 53: case 54:
                    button.enabled = ready && custom;
                    if (button.id == 51) button.displayString = label(confirmation == 51 ? "confirm_action" : "delete_group");
                    break;
                case 61:
                    button.enabled = ready && pledge != null && pledge.getBoolean("CanPledge")
                            && !snapshot.getString("Allegiance").equals(pledge.getString("Id")) && snapshot.getLong("AllegianceWait") == 0;
                    button.displayString = label(confirmation == 61 ? "confirm_action" : snapshot.getString("Allegiance").isEmpty() ? "pledge" : "switch_allegiance");
                    break;
                case 62:
                    button.enabled = ready && snapshot.getLong("AllegianceWait") == 0;
                    button.displayString = label(confirmation == 62 ? "confirm_action" : "renounce");
                    break;
                case 64: case 65:
                    button.enabled = ready && settlement.getBoolean(button.id == 64 ? "Claim" : "Neutralize");
                    button.displayString = label(confirmation == button.id ? "confirm_action" : button.id == 64 ? "claim_faction" : "neutralize");
                    break;
                default: break;
            }
        }
    }

    @Override protected void keyTyped(char character, int key) {
        if (trading() && commerce.keyTyped(character, key)) return;
        if (!recruiting && key == 15) {
            switchTab((tab + (isShiftKeyDown() ? TABS.length - 1 : 1)) % TABS.length);
            return;
        }
        if (!recruiting && tab == PERSONNEL && organization && nameAction >= 0 && groupName.isFocused()) {
            if (key == 28 || key == 156) saveGroupName();
            else if (key == 1) {
                nameAction = -1;
                refreshControls();
            } else groupName.textboxKeyTyped(character, key);
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
        if (trading() && commerce.click(x, y, relativeX, relativeY, button)) return;
        if (questView() && button == 0 && journal.click(relativeX, relativeY)) return;
        if (!recruiting && tab == FACTIONS && button == 0 && inside(relativeX, relativeY, LIST_X, LIST_Y, LIST_W, ROW_H * 6)) {
            int index = factionOffset + (relativeY - LIST_Y) / ROW_H;
            if (index < factions().tagCount()) {
                selectedFaction = factions().getCompoundTagAt(index).getString("Id");
                confirmation = -1;
                feedback = "";
            }
            return;
        }
        if (!recruiting && tab == PERSONNEL && organization && button == 0) {
            if (nameAction >= 0) groupName.mouseClicked(x, y, button);
            if (inside(relativeX, relativeY, 153, 65, 122, 39)) {
                int index = groupOffset + (relativeY - 65) / 13;
                if (index < groups().tagCount()) {
                    selectedGroup = groups().getCompoundTagAt(index).getString("Id");
                    nameAction = -1;
                    confirmation = -1;
                    feedback = "";
                    refreshControls();
                }
                return;
            }
        }
        if (hasList() && button == 0 && relativeY >= LIST_Y && relativeY < LIST_Y + ROW_H * MilitaryContainer.VISIBLE_ROWS) {
            if (relativeX >= SCROLL_X && relativeX < 147 && maxScroll() > 0) {
                dragging = true;
                drag(relativeY);
                return;
            }
            if (relativeX >= LIST_X && relativeX < LIST_X + LIST_W) {
                int index = (relativeY - LIST_Y) / ROW_H;
                if (index < rows().tagCount()) {
                    NBTTagCompound row = rows().getCompoundTagAt(index);
                    select(row);
                    if (!recruiting && organization && !grouping && "active".equals(row.getString("State"))) {
                        if (!isCtrlKeyDown()) selectedUnits.clear();
                        if (!selectedUnits.add(selectedId)) selectedUnits.remove(selectedId);
                    }
                }
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
        if (hasList() || !recruiting && tab == FACTIONS || allegianceView) background(149, LIST_Y, 278, 224, PANEL);
        if (trading()) commerce.background();
        if (questView()) {
            background(9, 66, 145, 223, PANEL);
            background(149, 66, 278, 223, PANEL);
        }
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
        title(recruiting ? label("role." + (priorRole.isEmpty() ? "quartermaster" : priorRole)) : label("title"), 31, 11, 151);
        if (recruiting) {
            String coins = label("trade_balance", number(snapshot.getLong("Currency")));
            title(coins, Math.max(174, 279 - fontRendererObj.getStringWidth(coins)), 11, 105, GOLD);
        }
        if (trading()) commerce.draw(x, y);
        else if (questView()) journal.draw(x, y);
        else if (recruiting) roster(x, y);
        else if (tab == OVERVIEW) overview(x, y);
        else if (tab == PERSONNEL) roster(x, y);
        else if (tab == TREASURY) treasury(x, y);
        else if (tab == SUPPLIES) supplies(x, y);
        else factionPanel(x, y);
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
            NBTTagCompound faction = faction(snapshot.getString("FactionId"));
            String factionName = snapshot.getString("FactionLabel");
            if (faction != null) FactionInsignia.fromKey(faction.getString("Insignia")).draw(12, 47, faction.getInteger("Color"));
            title(factionName, 27, 49, 121, MUTED);
            if (faction != null) {
                title(label("faction_score", faction.getString("Standing"), number(faction.getInteger("Score"))), RIGHT, 47, 125, MUTED);
            }
        }
        if (rows.tagCount() == 0) {
            drawRect(LIST_X, LIST_Y, LIST_X + LIST_W, LIST_Y + 49, INSET);
            wrapped(label(recruiting ? "no_offers" : "empty_roster"), LIST_X + 8, LIST_Y + 8, LIST_W - 16, 3, MUTED);
        }
        for (int i = 0; i < rows.tagCount(); i++) {
            NBTTagCompound row = rows.getCompoundTagAt(i);
            int rowY = LIST_Y + i * ROW_H;
            String identifier = row.getString("Id");
            boolean active = !recruiting && organization ? selectedUnits.contains(identifier) : identifier.equals(selectedId);
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
        if (recruiting && allegianceView) allegiancePanel();
        else if (!recruiting && organization) organizationPanel(x, y);
        else if (selected != null) details(selected, x, y);
        else wrapped(label(recruiting ? "pick_offer" : "pick_unit"), 157, LIST_Y + 9, 111, 3, MUTED);
    }

    private final class TradePanel {
        private final String[] categories = {"all", "firearms", "ammunition", "attachments", "equipment", "supplies", "resources"};
        private GuiTextField search;
        private NBTTagCompound item;
        private int category, quantity = 1, viewedSlot = -1;
        private String confirmed = "";

        void init() {
            String text = search == null ? "" : search.getText();
            search = new GuiTextField(fontRendererObj, guiLeft + 11, guiTop + 50, 130, 15);
            search.setMaxStringLength(64);
            search.setText(text);
        }

        NBTTagList offers() { return snapshot.getTagList("TradeRows", 10); }

        void reconcile() {
            NBTTagCompound former = item;
            item = null;
            boolean current = snapshot.getInteger("View") == serviceTab;
            if (!current) return;
            NBTTagList rows = offers();
            for (int i = 0; i < rows.tagCount(); i++) {
                NBTTagCompound row = rows.getCompoundTagAt(i);
                if (former != null && former.getString("Id").equals(row.getString("Id"))
                        && former.getInteger("Slot") == row.getInteger("Slot")) {
                    item = row;
                    if (former.getInteger("Price") != row.getInteger("Price")) confirmed = "";
                    break;
                }
            }
            if (item == null && former != null && serviceTab == MilitaryContainer.SELL && former.getInteger("Slot") >= 0) {
                NBTTagCompound carried = carried(former.getInteger("Slot"));
                if (carried != null && carried.hasKey("Key") && former.getString("Id").equals(carried.getString("Id"))) item = carried;
            }
            if (item == null && viewedSlot < 0 && rows.tagCount() > 0) select(rows.getCompoundTagAt(0));
            quantity = Math.max(1, Math.min(quantity, maximum()));
        }

        void query(int offset) {
            confirmed = "";
            if (search != null && inventorySlots.windowId != 0) MilitaryNetwork.request(inventorySlots.windowId,
                    MilitaryNetwork.TRADE_QUERY, Math.max(0, Math.min(65535, offset)) | category << 16, 0, search.getText());
        }

        void controls() {
            control(80, 150, 49, 127, 17, label("trade_category." + categories[category]));
            control(81, 11, 204, 24, 16, "<");
            control(82, 117, 204, 24, 16, ">");
            control(83, 152, 184, 21, 15, "−");
            control(84, 214, 184, 21, 15, "+");
            control(85, 238, 184, 36, 15, label("trade_max"));
            control(86, 152, 204, 122, 16, label("trade_preview"));
        }

        void action(int id) {
            if (id == 86) {
                if (item == null || maximum() <= 0) return;
                String key = item.getString("Key");
                if (!confirmed.equals(key)) confirmed = key;
                else {
                    confirmed = "";
                    transact(MilitaryNetwork.TRADE, quantity, key);
                }
                return;
            }
            confirmed = "";
            feedback = "";
            if (id == 80) {
                category = (category + 1) % categories.length;
                query(0);
                refreshControls();
            } else if (id == 81 || id == 82) {
                query(snapshot.getInteger("TradeOffset") + (id == 82 ? 6 : -6));
            } else {
                quantity = Math.max(1, Math.min(maximum(), id == 85 ? maximum() : quantity + (id == 84 ? 1 : -1)));
            }
        }

        int maximum() {
            if (item == null) return 0;
            int max = item.getInteger("Max");
            if (serviceTab == MilitaryContainer.BUY) max = (int)Math.min(max,
                    snapshot.getLong("Currency") / Math.max(1, item.getInteger("Price")));
            return max;
        }

        void update() {
            if (search != null) search.updateCursorCounter();
            for (Object entry : buttonList) {
                GuiButton button = (GuiButton)entry;
                int offset = snapshot.getInteger("TradeOffset");
                switch (button.id) {
                    case 81: button.enabled = offset > 0; break;
                    case 82: button.enabled = offset + 6 < snapshot.getInteger("TradeTotal"); break;
                    case 83: button.enabled = quantity > 1; break;
                    case 84: case 85: button.enabled = maximum() > quantity; break;
                    case 86:
                        button.enabled = pending == 0 && maximum() >= quantity;
                        button.displayString = label(confirmed.isEmpty() ? "trade_preview"
                                : serviceTab == MilitaryContainer.BUY ? "trade_confirm_buy" : "trade_confirm_sell");
                        break;
                    default: break;
                }
            }
        }

        boolean keyTyped(char character, int key) {
            if (search != null && search.isFocused() && key != 1 && search.textboxKeyTyped(character, key)) {
                viewedSlot = -1;
                query(0);
                return true;
            }
            return false;
        }

        boolean click(int x, int y, int rx, int ry, int button) {
            if (search != null) search.mouseClicked(x, y, button);
            if (button != 0) return false;
            if (inside(rx, ry, 11, 85, 130, 114)) {
                int row = (ry - 85) / 19;
                if (row < offers().tagCount()) select(offers().getCompoundTagAt(row));
                return true;
            }
            if (inside(rx, ry, 154, 85, 120, 60)) {
                viewedSlot = ((ry - 85) / 15) * 9 + (rx - 154) / 13;
                if (viewedSlot < 0 || viewedSlot >= 36) return true;
                for (int i = 0; i < offers().tagCount(); i++) {
                    NBTTagCompound row = offers().getCompoundTagAt(i);
                    if (row.getInteger("Slot") == viewedSlot) {
                        select(row);
                        return true;
                    }
                }
                if (serviceTab == MilitaryContainer.SELL) {
                    NBTTagCompound stack = carried(viewedSlot);
                    if (stack != null && stack.hasKey("Key")) select(stack);
                    else item = null;
                }
                return true;
            }
            return false;
        }

        NBTTagCompound carried(int slot) {
            NBTTagList carried = snapshot.getTagList("Carried", 10);
            for (int i = 0; i < carried.tagCount(); i++) {
                NBTTagCompound row = carried.getCompoundTagAt(i);
                if (row.getInteger("Slot") == slot) return row;
            }
            return null;
        }

        void select(NBTTagCompound row) {
            item = row;
            viewedSlot = -1;
            quantity = 1;
            feedback = "";
            confirmed = "";
        }

        void background() {
            MilitaryScreen.this.background(9, 68, 144, 223, PANEL);
            MilitaryScreen.this.background(149, 68, 278, 223, PANEL);
            if (search != null) search.drawTextBox();
        }

        void draw(int x, int y) {
            boolean selling = serviceTab == MilitaryContainer.SELL;
            title(label(selling ? "trade_accepted" : "trade_inventory"), 12, 73, 126, MUTED);
            title(label("trade_pack"), 153, 73, 118, MUTED);
            if (search != null && search.getText().isEmpty() && !search.isFocused()) title(label("trade_search"), 15, 54, 120, MUTED);
            NBTTagList rows = offers();
            if (rows.tagCount() == 0) wrapped(label("trade_no_results"), 15, 92, 120, 3, MUTED);
            for (int i = 0; i < rows.tagCount(); i++) {
                NBTTagCompound row = rows.getCompoundTagAt(i);
                int ry = 85 + i * 19;
                drawRect(11, ry, 142, ry + 18, item != null && row.getString("Id").equals(item.getString("Id"))
                        && row.getInteger("Slot") == item.getInteger("Slot") ? PICKED : INSET);
                icon(row, 13, ry + 1, 1);
                title(itemName(row), 33, ry + 2, 106, INK);
                title(label("trade_unit_value", number(row.getInteger("Price"))), 33, ry + 10, 108,
                        row.getInteger("Max") > 0 ? GOLD : MUTED);
                if (inside(x, y, 11, ry, 130, 18)) itemTooltip(row);
            }
            title(label("trade_page", snapshot.getInteger("TradeOffset") / 6 + 1,
                    Math.max(1, (snapshot.getInteger("TradeTotal") + 5) / 6)), 43, 209, 69, MUTED);
            for (int slot = 0; slot < 36; slot++) {
                int sx = 154 + slot % 9 * 13, sy = 85 + slot / 9 * 15;
                boolean selected = item != null && item.getInteger("Slot") == slot || slot == viewedSlot;
                drawRect(sx, sy, sx + 12, sy + 14, selected ? PICKED : INSET);
            }
            NBTTagList carried = snapshot.getTagList("Carried", 10);
            for (int index = 0; index < carried.tagCount(); index++) {
                NBTTagCompound row = carried.getCompoundTagAt(index);
                int slot = row.getInteger("Slot"), sx = 154 + slot % 9 * 13, sy = 85 + slot / 9 * 15;
                icon(row, sx, sy + 1, .75F);
                if (row.getInteger("Count") > 1) {
                    String count = Integer.toString(row.getInteger("Count"));
                    GL11.glPushMatrix();
                    try {
                        GL11.glTranslatef(sx + 13 - fontRendererObj.getStringWidth(count) * .6F, sy + 9, 250);
                        GL11.glScalef(.6F, .6F, 1);
                        fontRendererObj.drawStringWithShadow(count, 0, 0, INK);
                    } finally {
                        GL11.glPopMatrix();
                    }
                }
                if (inside(x, y, sx, sy, 12, 14)) itemTooltip(row);
            }
            if (item == null) {
                title(label(selling && viewedSlot >= 0 ? "trade_not_listed" : "trade_pick_item"), 153, 153, 120, MUTED);
            } else {
                title(itemName(item), 153, 150, 120, INK);
                String detail = item.getString("Classification");
                ItemStack ammo = ItemStack.loadItemStackFromNBT(item.getCompoundTag("Magazine"));
                if (ammo != null) detail = label("trade_ammo", ammo.getDisplayName());
                title(detail, 153, 160, 120, MUTED);
                long total = (long)quantity * item.getInteger("Price");
                String status = item.getInteger("Max") <= 0
                        ? label(snapshot.getInteger("Reputation") < item.getInteger("Reputation") ? "trade_locked" : "trade_unavailable")
                        : label("trade_total", number(total));
                title(status, 153, 171, 120, maximum() >= quantity ? GOLD : RED);
                title(number(quantity), 180, 188, 32, INK);
            }
            String footer = feedback.isEmpty() && !selling && snapshot.getInteger("TradeDiscount") > 0
                    ? label("recruit_discount", snapshot.getInteger("TradeDiscount"))
                    : feedback.isEmpty() ? "" : result(feedback);
            title(footer, 13, 223, 260, feedback.isEmpty() ? GOLD : feedbackColor());
        }

        void icon(NBTTagCompound row, int x, int y, float scale) {
            ItemStack item = ItemStack.loadItemStackFromNBT(row.getCompoundTag("Stack"));
            if (item == null) return;
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(x, y, 0);
                GL11.glScalef(scale, scale, 1);
                net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
                itemRender.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), item, 0, 0);
                net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
            } finally {
                GL11.glPopMatrix();
            }
        }

        String itemName(NBTTagCompound row) {
            if (row.hasKey("CustomName")) return row.getString("CustomName");
            ItemStack stack = ItemStack.loadItemStackFromNBT(row.getCompoundTag("Stack"));
            return stack == null ? "?" : stack.getDisplayName();
        }

        void itemTooltip(NBTTagCompound row) {
            ItemStack stack = ItemStack.loadItemStackFromNBT(row.getCompoundTag("Stack"));
            if (stack == null) return;
            List<String> lines = new ArrayList<>();
            lines.add(itemName(row));
            int count = row.getInteger("Count");
            if (count > 1) lines.add(label("trade_stack", number(count)));
            if (row.hasKey("Price")) lines.add(label("trade_unit_value", number(row.getInteger("Price"))));
            if (!row.getString("Classification").isEmpty()) lines.add(row.getString("Classification"));
            ItemStack ammo = ItemStack.loadItemStackFromNBT(row.getCompoundTag("Magazine"));
            if (ammo != null) lines.add(label("trade_ammo", ammo.getDisplayName()));
            if (row.hasKey("Slot") && !row.hasKey("Price") && serviceTab == MilitaryContainer.SELL) {
                lines.add(label("trade_pack_slot"));
            }
            hover = lines;
        }
    }

    private final class QuestPanel {
        private String selected = "";
        private boolean accepted, confirming;
        private int page;

        NBTTagList rows() {
            return snapshot.getTagList(recruiting && !accepted ? "QuestOffers" : "Contracts", 10);
        }

        NBTTagCompound current() { return findRow(rows(), selected); }

        void reconcile() {
            NBTTagList rows = rows();
            page = Math.max(0, Math.min(page, (rows.tagCount() - 1) / 6));
            if (current() == null) {
                selected = rows.tagCount() == 0 ? "" : rows.getCompoundTagAt(page * 6).getString("Id");
                confirming = false;
            }
        }

        void controls() {
            if (recruiting) {
                styled(90, 11, 48, 62, 15, label("quest_offers"), -1, !accepted);
                styled(91, 76, 48, 66, 15, label("quest_active"), -1, accepted);
            }
            control(92, 152, 204, 122, 18, label(recruiting && !accepted ? "quest_accept" : "quest_abandon"));
            if (recruiting && accepted) control(93, 152, 183, 122, 17, label("quest_report"));
            if (rows().tagCount() > 6) control(94, 11, 204, 130, 16, label("quest_more"));
        }

        void action(int id) {
            if (id == 90 || id == 91 || id == 94) {
                if (id == 94) page = (page + 1) % (Math.max(0, rows().tagCount() - 1) / 6 + 1);
                else {
                    accepted = id == 91;
                    page = 0;
                }
                selected = "";
                feedback = "";
                reconcile();
                refreshControls();
                return;
            }
            if (current() == null) return;
            if (id == 93) transact(MilitaryNetwork.QUEST, 1, selected);
            else if (recruiting && !accepted) transact(MilitaryNetwork.QUEST, 0, selected);
            else if (confirming) {
                confirming = false;
                transact(MilitaryNetwork.QUEST, 2, selected);
            } else confirming = true;
        }

        void update() {
            NBTTagCompound row = current();
            for (Object entry : buttonList) {
                GuiButton button = (GuiButton)entry;
                if (button.id == 92) {
                    button.enabled = pending == 0 && row != null && (!recruiting || accepted || !row.getBoolean("Invalid"));
                    if (confirming) button.displayString = label("confirm_action");
                } else if (button.id == 93) button.enabled = pending == 0 && row != null && row.getBoolean("TurnIn");
            }
        }

        boolean click(int x, int y) {
            if (!inside(x, y, 11, 77, 131, 120)) return false;
            int i = page * 6 + (y - 77) / 20;
            NBTTagList rows = rows();
            if (i < rows.tagCount()) {
                selected = rows.getCompoundTagAt(i).getString("Id");
                confirming = false;
                feedback = "";
            }
            return true;
        }

        void draw(int x, int y) {
            NBTTagList rows = rows();
            boolean offers = recruiting && !accepted;
            title(label(offers ? "quest_available" : "quest_active_count", snapshot.getTagList("Contracts", 10).tagCount(),
                    snapshot.getInteger("QuestLimit")), 11, 69, 129, MUTED);
            if (rows.tagCount() == 0) {
                wrapped(label(!snapshot.getBoolean("QuestsEnabled") ? "quest_disabled" : offers ? "quest_no_offers" : "quest_empty"),
                        15, 88, 121, 4, MUTED);
            }
            for (int i = 0; i < 6 && page * 6 + i < rows.tagCount(); i++) {
                int ry = 77 + i * 20;
                NBTTagCompound row = rows.getCompoundTagAt(page * 6 + i);
                drawRect(11, ry, 142, ry + 19, selected.equals(row.getString("Id")) ? PICKED : INSET);
                title(questName(row), 14, ry + 2, 125, INK);
                title(offers ? label("trade_balance", number(row.getLong("Reward"))) : questStatus(row), 14, ry + 10, 125, MUTED);
            }
            NBTTagCompound row = current();
            if (row != null) {
                title(questName(row), 153, 72, 120, INK);
                title(row.getString("FactionName"), 153, 84, 120, GOLD);
                wrapped(questObjective(row), 153, 96, 119, 3, INK);
                int[] location = row.getIntArray("Location");
                if (location.length == 3) title(label("quest_coordinates", location[0], location[2]), 153, 121, 120, MUTED);
                title(offers ? questProvider(row) : questStatus(row), 153, 132, 120, row.getBoolean("Invalid") ? RED : MUTED);
                title(label("quest_reward", number(row.getLong("Reward")), row.getInteger("ReputationReward")), 153, 143, 120, GOLD);
                NBTTagList rewards = row.getTagList("RewardItems", 10);
                for (int i = 0; i < rewards.tagCount(); i++) {
                    NBTTagCompound reward = rewards.getCompoundTagAt(i);
                    ItemStack stack = ItemStack.loadItemStackFromNBT(reward);
                    if (stack == null) continue;
                    NBTTagCompound icon = new NBTTagCompound();
                    icon.setTag("Stack", reward);
                    int position = 153 + i * 15;
                    commerce.icon(icon, position, 155, .75F);
                    if (inside(x, y, position, 155, 14, 14)) hint(label("quest_reward_item", reward.getInteger("RewardCount"), stack.getDisplayName()));
                }
                if (inside(x, y, 151, 70, 126, 84)) {
                    ArrayList<String> lines = new ArrayList<>();
                    lines.add(questName(row));
                    lines.add(questProvider(row));
                    lines.add(questObjective(row));
                    if (location.length == 3) lines.add(label("quest_location", location[0], location[1], location[2], row.getInteger("Dimension")));
                    if (row.getLong("Remaining") >= 0) lines.add(label("quest_remaining", (row.getLong("Remaining") + 1199) / 1200));
                    hover = lines;
                }
            }
            if (!feedback.isEmpty()) title(result(feedback), 11, 223, 262, feedbackColor());
        }

        String questStatus(NBTTagCompound row) {
            return row.getBoolean("Invalid") ? label("quest_invalid") : row.getBoolean("Complete") ? label("quest_complete")
                    : label("quest_progress", row.getInteger("Progress"), row.getInteger("Amount"));
        }

        String questProvider(NBTTagCompound row) {
            String provider = row.getString("Provider");
            return label("quest_provider", row.getBoolean("ProviderRole") ? label("role." + provider) : provider);
        }

        String questObjective(NBTTagCompound row) {
            int amount = row.getInteger("Amount");
            String type = row.getString("Type");
            ItemStack item = ItemStack.loadItemStackFromNBT(row.getCompoundTag("Required"));
            if (item != null) return label("quest_objective." + type, amount, item.getDisplayName());
            if ("HOSTILES".equals(type)) return label("quest_objective.HOSTILES", amount);
            if ("FACTION".equals(type)) return label("quest_objective.FACTION", amount, row.getString("TargetFactionName"));
            if ("UNIT".equals(type)) return label("quest_objective.UNIT", amount,
                    row.getString("Classification").replace('_', ' '), row.getString("TargetFactionName"));
            return label("quest_objective." + type, label("site." + row.getString("TargetType")));
        }

        String questName(NBTTagCompound row) {
            String id = row.getString("Definition"), text = label("quest_name." + id);
            return text.startsWith("gvc.military.gui.") ? questObjective(row) : text;
        }
    }

    private void organizationPanel(int x, int y) {
        for (int index = 0; index < 3 && index + groupOffset < groups().tagCount(); index++) {
            NBTTagCompound row = groups().getCompoundTagAt(index + groupOffset);
            int top = 65 + index * 13;
            boolean active = selectedGroup.equals(row.getString("Id"));
            drawRect(153, top, 275, top + 12, active ? PICKED : INSET);
            title(groupTitle(row), 157, top + 2, 95, active ? GOLD : INK);
            title(number(row.getInteger("Count")), 252, top + 2, 21, MUTED);
            if (inside(x, y, 153, top, 122, 12)) hint(groupTitle(row), label("unit_count", "", row.getInteger("Count")));
        }
        if (nameAction >= 0) {
            title(label(nameAction == 0 ? "new_group" : "rename"), 156, 113, 117, GOLD);
            GL11.glPushMatrix();
            GL11.glTranslatef(-guiLeft, -guiTop, 0);
            groupName.drawTextBox();
            GL11.glPopMatrix();
            return;
        }
        NBTTagCompound row = group(selectedGroup);
        if (row != null) {
            int count = row.getInteger("Count"), capacity = row.getInteger("Capacity");
            boolean warning = capacity > 0 && count > capacity || !"-".equals(selectedGroup)
                    && snapshot.getBoolean("RequireOfficer") && !row.getBoolean("OfficerDeployed");
            title(label("group_deployed", row.getInteger("Deployed"), count), 157, 106, 114, INK);
            String leader = row.getString("Leader");
            title(leader.isEmpty() ? label("no_officer") : label("group_officer", leader, capacity), 157, 117, 114, warning ? RED : MUTED);
            title(label("group_upkeep", number(daily(row.getLong("Wage"))), number(daily(row.getLong("Ration")))),
                    157, 128, 114, MUTED);
            String orders = row.getString("Orders");
            List<String> translated = new ArrayList<>();
            if (!orders.isEmpty()) for (String order : orders.split(",")) {
                translated.add("unassigned".equals(order) ? label("unassigned") : I18n.format("gvc.order.mode." + order));
            }
            title(translated.isEmpty() ? label("no_orders") : String.join(", ", translated), 157, 139, 114, GOLD);
            title(warning ? label("leadership_warning") : label("selected_units", selectedUnits.size()), 157, 151, 114, warning ? RED : INK);
            if (inside(x, y, 151, 105, 126, 58)) {
                ArrayList<String> lines = new ArrayList<>();
                lines.add(groupTitle(row));
                if (!translated.isEmpty()) lines.addAll(translated);
                if (!leader.isEmpty()) lines.add(label("group_officer", leader, capacity));
                NBTTagList roles = row.getTagList("Composition", 10);
                for (int i = 0; i < roles.tagCount(); i++) {
                    NBTTagCompound role = roles.getCompoundTagAt(i);
                    lines.add(label("unit_count", classification(role), role.getInteger("Count")));
                }
                lines.add(label("multiselect_tip"));
                hover = lines;
            }
        }
        if (!feedback.isEmpty()) title(result(feedback), 11, 218, 134, feedbackColor());
        else title(label("multiselect_short"), 11, 218, 134, MUTED);
    }

    private static String groupTitle(NBTTagCompound row) {
        return "-".equals(row.getString("Id")) ? label("unassigned_pool") : row.getString("Name");
    }

    private void factionPanel(int x, int y) {
        String allegiance = snapshot.getString("Allegiance");
        NBTTagCompound pledged = faction(allegiance);
        title(pledged == null ? label("unaligned") : label("pledged_to", pledged.getString("Name")), 11, 51, 135, GOLD);
        for (int i = 0; i < 6 && i + factionOffset < factions().tagCount(); i++) {
            NBTTagCompound faction = factions().getCompoundTagAt(factionOffset + i);
            int top = LIST_Y + ROW_H * i;
            boolean selected = selectedFaction.equals(faction.getString("Id"));
            drawRect(LIST_X, top, LIST_X + LIST_W, top + ROW_H - 1, selected ? PICKED : INSET);
            drawRect(LIST_X, top, LIST_X + 2, top + ROW_H - 1, 0xFF000000 | faction.getInteger("Color"));
            FactionInsignia.fromKey(faction.getString("Insignia")).draw(LIST_X + 6, top + 5, faction.getInteger("Color"));
            title(faction.getString("Name"), LIST_X + 23, top + 3, 99, INK);
            int tint = faction.getBoolean("Hostile") ? RED : GREEN;
            String score = number(faction.getInteger("Score"));
            int scoreWidth = fontRendererObj.getStringWidth(score);
            title(faction.getString("Standing"), LIST_X + 23, top + 13, 95 - scoreWidth, tint);
            title(score, LIST_X + LIST_W - scoreWidth - 3, top + 13, scoreWidth, tint);
        }
        if (factionOffset > 0) title("↑", SCROLL_X, LIST_Y + 2, 9, GOLD);
        if (factionOffset + 6 < factions().tagCount()) title("↓", SCROLL_X, LIST_Y + ROW_H * 6 - 11, 9, GOLD);
        if (settlementView) {
            capturePanel();
            return;
        }
        NBTTagCompound row = faction(selectedFaction);
        if (row == null) return;
        title(row.getString("Name"), 156, 71, 117, INK);
        title(label("faction_score", row.getString("Standing"), number(row.getInteger("Score"))),
                156, 83, 117, row.getBoolean("Hostile") ? RED : GOLD);
        int score = row.getInteger("Score"), next = row.getInteger("Next"), previous = row.getInteger("Previous");
        if (previous == -1000000 && next > score) previous = Math.min(score, next - 100);
        double progress = next == score ? 1 : (score - previous) / (double) Math.max(1, next - previous);
        drawRect(156, 96, 271, 101, INSET);
        drawRect(156, 96, 156 + (int) (115 * progress), 101, 0xFF000000 | row.getInteger("Color"));
        title(next == score ? label("top_standing") : label("next_standing", number(next - score)), 156, 105, 117, MUTED);
        wrapped(row.getString("Description"), 156, 116, 115, 3, INK);
        title(label("recruit_tier_discount", row.getInteger("Tier"), row.getInteger("Discount")), 156, 149, 117, MUTED);
        title(label(row.getBoolean("Hostile") ? "faction_hostile" : row.getBoolean("Trade") ? "trade_open" : "trade_closed"),
                156, 160, 117, row.getBoolean("Trade") ? GREEN : RED);
        title(row.getBoolean("Pledging") ? label("pledge_required", number(row.getInteger("PledgeRequired"))) : label("pledge_disabled"),
                156, 171, 117, MUTED);
        String allies = row.getString("Allies"), enemies = row.getString("Enemies");
        title(label("faction_allies", allies.isEmpty() ? "–" : allies), 156, 182, 117, GREEN);
        title(label("faction_enemies", enemies.isEmpty() ? "–" : enemies), 156, 193, 117, RED);
        if (inside(x, y, 149, 65, 129, 137)) {
            ArrayList<String> lines = new ArrayList<>();
            lines.add(row.getString("Name"));
            if (!row.getString("Doctrine").isEmpty()) lines.add(label("faction_doctrine", row.getString("Doctrine")));
            if (!row.getString("Architecture").isEmpty()) lines.add(label("faction_architecture", row.getString("Architecture")));
            lines.add(label("pledge_discount", row.getInteger("PledgeDiscount")));
            lines.add(label(row.getBoolean("Capture") ? "privilege_capture" : "privilege_no_capture"));
            lines.add(label(row.getBoolean("Infrastructure") ? "privilege_infrastructure" : "privilege_no_infrastructure"));
            lines.add(label("faction_record", row.getInteger("CompletedQuests"), row.getInteger("Discovered")));
            lines.add(label("faction_defeated", row.getInteger("Defeated")));
            lines.add(label("faction_sites_controlled", row.getInteger("Captured"), row.getInteger("Neutralized")));
            if (!allies.isEmpty()) lines.add(label("faction_allies", allies));
            if (!enemies.isEmpty()) lines.add(label("faction_enemies", enemies));
            if (!row.getString("Neutral").isEmpty()) lines.add(label("faction_neutral", row.getString("Neutral")));
            hover = lines;
        }
        if (confirmation == 62) wrapped(label("leaving_warning", snapshot.getInteger("AllegiancePenalty")), 11, 204, 126, 3, RED);
        else if (!feedback.isEmpty()) title(result(feedback), 11, 218, 134, feedbackColor());
    }


    private void capturePanel() {
        NBTTagCompound post = snapshot.getCompoundTag("Settlement");
        if (post.getString("Id").isEmpty()) {
            wrapped(label("no_nearby_settlement"), 156, 74, 114, 4, MUTED);
            return;
        }
        title(label("site." + post.getString("Type")), 156, 72, 118, INK);
        title(post.getString("Faction").isEmpty() ? label("faction_unclaimed") : post.getString("Faction"), 156, 84, 118, GOLD);
        String state = post.getString("State");
        title(label("beacon." + state), 156, 96, 118, state.equals("CONTESTED") ? RED : MUTED);
        String detail = confirmation == 64 ? "claim_warning" : confirmation == 65 ? "neutralize_warning"
                : post.getBoolean("Claim") ? "claim_available" : state.equals("NEUTRALIZED") ? "neutralized_site" : "capture_site_hint";
        wrapped(label(detail), 156, 113, 115, 5, confirmation >= 0 ? GOLD : MUTED);
        if (confirmation < 0) {
            title(label("site_class." + post.getString("Classification")), 156, 147, 118, MUTED);
            title(label("site_stations", post.getInteger("TraderStations")), 156, 158, 118, MUTED);
        }
        if (!feedback.isEmpty()) title(result(feedback), 156, 210, 115, feedbackColor());
    }

    private void allegiancePanel() {
        NBTTagCompound row = faction(snapshot.getString("FactionId"));
        if (row == null) return;
        String allegiance = snapshot.getString("Allegiance");
        NBTTagCompound previous = faction(allegiance);
        title(row.getString("Name"), 156, 72, 117, GOLD);
        wrapped(previous == null ? label("unaligned") : label("pledged_to", previous.getString("Name")), 156, 85, 115, 2, INK);
        title(row.getBoolean("Pledging") ? label("pledge_required", number(row.getInteger("PledgeRequired"))) : label("pledge_disabled"),
                156, 106, 116, MUTED);
        title(label("pledge_discount", row.getInteger("PledgeDiscount")), 156, 118, 116, MUTED);
        if (confirmation == 61) {
            wrapped(allegiance.isEmpty() ? label("pledge_warning") : label("leaving_warning", snapshot.getInteger("AllegiancePenalty")),
                    156, 135, 116, 5, GOLD);
        } else {
            String message = allegiance.equals(row.getString("Id")) ? label("already_pledged")
                    : snapshot.getLong("AllegianceWait") > 0 ? label("pledge_wait")
                    : row.getBoolean("CanPledge") ? label("pledge_ready") : label("pledge_unavailable");
            wrapped(message, 156, 135, 116, 4, MUTED);
        }
        if (!feedback.isEmpty()) title(result(feedback), 156, 187, 116, feedbackColor());
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
            // GuiContainer can reach its foreground with depth testing disabled when there are no inventory slots.
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
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
        switch (feedback) {
            case "deposited": case "supplied": case "withdrawn": case "hired": case "group_saved":
            case "pledged": case "renounced": case "occupied": case "neutralized": return GREEN;
            case "confirm_withdraw": case "confirm_delete_group": return YELLOW;
            default: return RED;
        }
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
        String lock = row.getString("Lock");
        if (!lock.isEmpty()) return lock;
        if (snapshot.getInteger("Active") >= snapshot.getInteger("Limit")) return "limit";
        if (snapshot.getLong("Currency") < row.getInteger("Price")) return "funds";
        return "";
    }

    private String offerHint(NBTTagCompound row) {
        String reason = offerReason(row);
        if (reason.equals("reputation_locked")) return row.getInteger("Required") > 1000000 ? label("tier_disabled")
                : label("needs_reputation", number(row.getInteger("Required")));
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

    static void emblem(int icon, int x, int y) {
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
            if ((id >= 0 && id <= JOURNAL) || (id >= 70 && id <= 74)) {
                drawTabLabel(client);
                return;
            }
            String title = fit(client, displayString, xPosition + width - left - 3);
            if (icon < 0) left = xPosition + (width - client.fontRenderer.getStringWidth(title)) / 2;
            client.fontRenderer.drawString(title, left, yPosition + (height - 8) / 2, enabled ? selected ? GOLD : INK : 0x91988B);
        }

        private void drawTabLabel(Minecraft client) {
            int textWidth = client.fontRenderer.getStringWidth(displayString);
            float scale = textWidth == 0 ? 1F : Math.max(.82F, Math.min(1F, (width - 5F) / textWidth));
            String text = fit(client, displayString, (int)((width - 4F) / scale));
            float left = xPosition + (width - client.fontRenderer.getStringWidth(text) * scale) / 2F;
            float top = yPosition + (height - 8 * scale) / 2F;
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(left, top, 0);
                GL11.glScalef(scale, scale, 1);
                client.fontRenderer.drawString(text, 0, 0, enabled ? selected ? GOLD : INK : 0x91988B);
            } finally {
                GL11.glPopMatrix();
            }
        }
    }
}
