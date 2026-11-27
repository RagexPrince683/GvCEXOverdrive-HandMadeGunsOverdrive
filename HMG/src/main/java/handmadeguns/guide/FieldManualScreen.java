package handmadeguns.guide;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SideOnly(Side.CLIENT)
public final class FieldManualScreen extends GuiScreen {
    private static final int BACK = 1, NEXT = 2, CLOSE = 3;
    private static final int INK = 0x302E25, MUTED = 0x67604F, GOLD = 0xEAC87F;
    private static final int ROW_HEIGHT = 20;
    private final RenderItem icons = new RenderItem();
    private final String title;
    private final int cover;
    private final Page[] pages;
    private final List<Integer> matches = new ArrayList<>();
    private GuiTextField search;
    private int left, top, right, bottom, spine, selected, indexOffset, textOffset;
    private List<String> lines = new ArrayList<>();

    public FieldManualScreen(String title, int cover, Page... pages) {
        this.title = title;
        this.cover = cover;
        this.pages = pages;
    }

    @Override public void initGui() {
        int panel = Math.min(354, width - 10), height = Math.min(220, this.height - 10);
        left = (width - panel) / 2;
        right = left + panel;
        top = (this.height - height) / 2;
        bottom = top + height;
        spine = left + Math.min(panel / 2 - 5, 127);
        String previous = search == null ? "" : search.getText();
        search = new GuiTextField(fontRendererObj, left + 11, top + 34, spine - left - 22, 15);
        search.setMaxStringLength(48);
        search.setText(previous);
        buttonList.add(new GuiButton(BACK, spine + 9, bottom - 23, 22, 15, "<"));
        buttonList.add(new GuiButton(NEXT, spine + 33, bottom - 23, 22, 15, ">"));
        buttonList.add(new GuiButton(CLOSE, right - 49, bottom - 23, 40, 15, label("close")));
        filter();
        wrap();
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    private void filter() {
        matches.clear();
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < pages.length; i++) {
            Page page = pages[i];
            if (query.isEmpty() || (page.title() + " " + page.body()).toLowerCase(Locale.ROOT).contains(query)) matches.add(i);
        }
        indexOffset = 0;
        if (!matches.isEmpty() && !matches.contains(selected)) choose(matches.get(0));
    }

    private void choose(int page) {
        selected = page;
        textOffset = 0;
        wrap();
    }

    private void wrap() {
        lines.clear();
        for (String paragraph : pages[selected].body().split("\n", -1)) {
            if (paragraph.isEmpty()) lines.add("");
            else lines.addAll(fontRendererObj.listFormattedStringToWidth(paragraph, right - spine - 22));
        }
        textOffset = Math.min(textOffset, maxTextOffset());
    }

    @Override public void updateScreen() { search.updateCursorCounter(); }

    @Override protected void keyTyped(char typed, int key) {
        if (key == Keyboard.KEY_ESCAPE) super.keyTyped(typed, key);
        else if (search.textboxKeyTyped(typed, key)) filter();
        else if (key == Keyboard.KEY_LEFT || key == Keyboard.KEY_RIGHT) turn(key == Keyboard.KEY_LEFT ? -1 : 1);
        else if (key == Keyboard.KEY_NEXT || key == Keyboard.KEY_PRIOR) scroll(key == Keyboard.KEY_NEXT ? 4 : -4);
    }

    @Override protected void mouseClicked(int x, int y, int button) {
        search.mouseClicked(x, y, button);
        if (button == 0 && x > left + 6 && x < spine - 4 && y >= top + 56 && y < top + 56 + visibleRows() * ROW_HEIGHT) {
            int index = indexOffset + (y - top - 56) / ROW_HEIGHT;
            if (index < matches.size()) choose(matches.get(index));
            return;
        }
        super.mouseClicked(x, y, button);
    }

    @Override public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int x = Mouse.getEventX() * width / mc.displayWidth;
        int delta = wheel < 0 ? 1 : -1;
        if (x < spine) indexOffset = Math.max(0, Math.min(Math.max(0, matches.size() - visibleRows()), indexOffset + delta));
        else scroll(delta * 3);
    }

    @Override protected void actionPerformed(GuiButton button) {
        if (button.id == CLOSE) mc.displayGuiScreen(null);
        else turn(button.id == BACK ? -1 : 1);
    }

    private void turn(int amount) {
        int index = matches.indexOf(selected) + amount;
        if (index < 0 || index >= matches.size()) return;
        choose(matches.get(index));
        if (index < indexOffset) indexOffset = index;
        if (index >= indexOffset + visibleRows()) indexOffset = index - visibleRows() + 1;
    }

    private void scroll(int amount) { textOffset = Math.max(0, Math.min(maxTextOffset(), textOffset + amount)); }
    private int visibleRows() { return Math.max(1, (bottom - top - 63) / ROW_HEIGHT); }
    private int visibleLines() { return Math.max(1, (bottom - top - 79) / 11); }
    private int maxTextOffset() { return Math.max(0, lines.size() - visibleLines()); }

    @Override public void drawScreen(int x, int y, float partialTicks) {
        drawDefaultBackground();
        drawRect(left - 2, top - 2, right + 2, bottom + 2, 0xFF211B13);
        drawRect(left, top, spine, bottom, cover);
        drawRect(spine, top, right, bottom, 0xFFEAE0C8);
        drawRect(spine, top, spine + 4, bottom, 0xFFC6B695);
        drawRect(left + 7, top + 27, spine - 7, top + 28, 0xFFAD925A);
        fontRendererObj.drawString(fontRendererObj.trimStringToWidth(I18n.format(title), spine - left - 20), left + 10, top + 12, GOLD);
        search.drawTextBox();
        if (!search.isFocused() && search.getText().isEmpty()) fontRendererObj.drawString(label("search"), left + 15, top + 38, 0xA5A499);
        for (int row = 0; row < visibleRows() && row + indexOffset < matches.size(); row++) {
            int index = matches.get(row + indexOffset);
            Page page = pages[index];
            int yRow = top + 56 + row * ROW_HEIGHT;
            boolean hover = x >= left + 5 && x < spine - 4 && y >= yRow && y < yRow + ROW_HEIGHT;
            if (index == selected || hover) drawRect(left + 5, yRow, spine - 4, yRow + 19, 0x494BC6A8);
            icon(page.icon, left + 8, yRow + 1);
            List<String> titleLines = fontRendererObj.listFormattedStringToWidth(page.title(), spine - left - 35);
            int titleY = yRow + (titleLines.size() == 1 ? 6 : 1);
            for (int line = 0; line < Math.min(2, titleLines.size()); line++) {
                String text = titleLines.get(line);
                if (line == 1 && titleLines.size() > 2) text = fontRendererObj.trimStringToWidth(text, spine - left - 41) + "…";
                fontRendererObj.drawString(text, left + 29, titleY + line * 9, index == selected ? GOLD : 0xF2E8D0);
            }
        }
        if (matches.size() > visibleRows()) {
            int track = visibleRows() * ROW_HEIGHT;
            int thumb = Math.max(10, track * visibleRows() / matches.size());
            int yThumb = top + 56 + indexOffset * (track - thumb) / (matches.size() - visibleRows());
            drawRect(spine - 3, yThumb, spine - 1, yThumb + thumb, 0xFFAD925A);
        }
        if (matches.isEmpty()) {
            fontRendererObj.drawString(label("no_results"), spine + 11, top + 15, INK);
        } else {
            Page page = pages[selected];
            int textX = spine + 11, textWidth = right - textX - 10;
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(page.title(), textWidth), textX, top + 15, INK);
            drawRect(textX, top + 31, right - 10, top + 32, 0xFFAD925A);
            for (int row = 0; row < visibleLines() && row + textOffset < lines.size(); row++) {
                fontRendererObj.drawString(lines.get(row + textOffset), textX, top + 41 + row * 11, INK);
            }
            if (maxTextOffset() > 0) {
                int thumb = top + 41 + textOffset * Math.max(1, bottom - top - 85) / maxTextOffset();
                drawRect(right - 7, thumb, right - 5, thumb + 8, 0xFFAD925A);
            }
            fontRendererObj.drawString((matches.indexOf(selected) + 1) + " / " + matches.size(), spine + 62, bottom - 19, MUTED);
        }
        for (Object value : buttonList) {
            GuiButton button = (GuiButton) value;
            if (button.id == BACK) button.enabled = matches.indexOf(selected) > 0;
            if (button.id == NEXT) button.enabled = matches.indexOf(selected) < matches.size() - 1;
        }
        super.drawScreen(x, y, partialTicks);
    }

    private void icon(ItemStack stack, int x, int y) {
        if (stack == null) return;
        RenderHelper.enableGUIStandardItemLighting();
        icons.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GL11.glColor4f(1, 1, 1, 1);
    }

    private static String label(String key) { return I18n.format("hmg.manual.ui." + key); }

    public static final class Page {
        private final String key;
        private final ItemStack icon;
        private final Object[] arguments;

        public Page(String key, ItemStack icon, Object... arguments) {
            this.key = key;
            this.icon = icon;
            this.arguments = arguments;
        }

        private String title() { return I18n.format(key + ".title"); }
        private String body() { return I18n.format(key + ".body", arguments).replace("\\n", "\n"); }
    }
}
