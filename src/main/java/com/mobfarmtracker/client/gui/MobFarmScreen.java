package com.mobfarmtracker.client.gui;

import com.mobfarmtracker.MobFilter;
import com.mobfarmtracker.client.ClientMobStats;
import com.mobfarmtracker.client.ClientSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Detail screen: per-mob and per-item drop breakdowns, plus radius, sample window, and mob-type filter controls. */
public final class MobFarmScreen extends Screen {
    private static final int MARGIN = 20;
    private static final int CONTROLS_HEIGHT = 50;
    private static final int HEADER_HEIGHT = CONTROLS_HEIGHT + 60;
    private static final int ROW_HEIGHT = 14;

    private static final int TAB_HEIGHT = 16;
    // TAB_TOP: sits just below the controls, above the title/summary block.
    private static final int TAB_TOP = MARGIN + CONTROLS_HEIGHT + 2;

    // Colors
    private static final int COLOR_CONTENT_BG      = 0xFF1E1E1E;
    private static final int COLOR_TAB_ACTIVE_BG    = COLOR_CONTENT_BG; // merges with content
    private static final int COLOR_TAB_INACTIVE_BG  = 0xFF111111;
    private static final int COLOR_BORDER           = 0xFF888888;
    private static final int COLOR_TAB_ACTIVE_TEXT  = 0xFFFFFF;
    private static final int COLOR_TAB_INACTIVE_TEXT = 0x808080;

    private enum Tab {
        MOBS("Mobs"),
        ITEMS("Items"),
        XP("XP");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private boolean enabled = ClientSettings.isEnabled();
    private int radiusChunks = ClientSettings.getRadiusChunks();
    private int windowSeconds = ClientSettings.getWindowSeconds();
    private MobFilter filter = ClientSettings.getFilter();
    private Tab activeTab = Tab.MOBS;

    private Button radiusButton;
    private Button windowButton;
    private MobTypeList mobTypeList;
    private ItemDropList itemDropList;
    private MobXpList mobXpList;
    private int ticksSinceRefresh;

    public MobFarmScreen() {
        super(Component.translatable("mobfarmtracker.screen.title"));
    }

    @Override
    protected void init() {
        int top = MARGIN;
        int left = MARGIN;

        addRenderableWidget(Button.builder(toggleLabel(), b -> {
            enabled = !enabled;
            b.setMessage(toggleLabel());
            pushSettings();
        }).bounds(left + 450, top, 100, 20).build());

        radiusButton = addRenderableWidget(Button.builder(radiusLabel(), b -> {
            radiusChunks = radiusChunks >= 32 ? 1 : radiusChunks + 1;
            b.setMessage(radiusLabel());
            pushSettings();
        }).bounds(left, top, 140, 20).build());

        windowButton = addRenderableWidget(Button.builder(windowLabel(), b -> {
            windowSeconds = cycleWindow(windowSeconds);
            b.setMessage(windowLabel());
            pushSettings();
        }).bounds(left + 150, top, 140, 20).build());

        addRenderableWidget(CycleButton.builder((MobFilter f) -> f.displayName())
                .withValues(MobFilter.values())
                .withInitialValue(filter)
                .create(left + 300, top, 140, 20, Component.translatable("mobfarmtracker.screen.filter"),
                        (button, value) -> {
                            filter = value;
                            pushSettings();
                        }));

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left, top + 30, 100, 20).build());

        int listTop = top + HEADER_HEIGHT;
        mobTypeList = addRenderableWidget(new MobTypeList(minecraft, width - 2 * MARGIN, height - listTop - MARGIN, listTop, ROW_HEIGHT));
        mobTypeList.setX(MARGIN);
        mobTypeList.refresh();

        itemDropList = addRenderableWidget(new ItemDropList(minecraft, width - 2 * MARGIN, height - listTop - MARGIN, listTop, ROW_HEIGHT));
        itemDropList.setX(MARGIN);
        itemDropList.refresh();

        mobXpList = addRenderableWidget(new MobXpList(minecraft, width - 2 * MARGIN, height - listTop - MARGIN, listTop, ROW_HEIGHT));
        mobXpList.setX(MARGIN);
        mobXpList.refresh();

        applyTabVisibility();
    }

    private void switchTab(Tab tab) {
        activeTab = tab;
        applyTabVisibility();
    }

    private void applyTabVisibility() {
        mobTypeList.visible  = (activeTab == Tab.MOBS);
        itemDropList.visible = (activeTab == Tab.ITEMS);
        mobXpList.visible    = (activeTab == Tab.XP);
    }

    /** Width of each tab, computed dynamically so all tabs together span the full content width. */
    private int tabWidth() {
        return (width - 2 * MARGIN) / Tab.values().length;
    }

    private int tabX(int index) {
        return MARGIN + index * tabWidth();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        // Fill the content box here so list rows render on top of it (not under it).
        int contentTop = TAB_TOP + TAB_HEIGHT;
        guiGraphics.fill(MARGIN, contentTop, width - MARGIN, height - MARGIN, COLOR_CONTENT_BG);
    }

    private void renderTabs(GuiGraphics guiGraphics) {
        Tab[] tabs = Tab.values();
        int tw = tabWidth();
        int contentLeft  = MARGIN;
        int contentRight = width - MARGIN;
        int contentTop   = TAB_TOP + TAB_HEIGHT; // top of the bordered content box
        int contentBottom = height - MARGIN;

        // Background is drawn in renderBackground() so list rows appear on top of it.

        // Content box border: left, right, bottom. Top is handled per-tab below.
        guiGraphics.fill(contentLeft, contentTop, contentLeft + 1, contentBottom, COLOR_BORDER);
        guiGraphics.fill(contentRight - 1, contentTop, contentRight, contentBottom, COLOR_BORDER);
        guiGraphics.fill(contentLeft, contentBottom - 1, contentRight, contentBottom, COLOR_BORDER);

        for (int i = 0; i < tabs.length; i++) {
            boolean active = activeTab == tabs[i];
            int x = tabX(i);
            int bg = active ? COLOR_TAB_ACTIVE_BG : COLOR_TAB_INACTIVE_BG;
            int textColor = active ? COLOR_TAB_ACTIVE_TEXT : COLOR_TAB_INACTIVE_TEXT;

            // Tab background
            guiGraphics.fill(x, TAB_TOP, x + tw, TAB_TOP + TAB_HEIGHT, bg);

            // Top border
            guiGraphics.fill(x, TAB_TOP, x + tw, TAB_TOP + 1, COLOR_BORDER);
            // Left border (only for first tab to avoid a double line between adjacent tabs)
            if (i == 0) {
                guiGraphics.fill(x, TAB_TOP, x + 1, TAB_TOP + TAB_HEIGHT, COLOR_BORDER);
            }
            // Right border (serves as the left border of the next tab)
            guiGraphics.fill(x + tw - 1, TAB_TOP, x + tw, TAB_TOP + TAB_HEIGHT, COLOR_BORDER);

            if (active) {
                // No bottom border: the active tab connects into the content box.
                // Fill over the content box's top border so there's no seam.
                guiGraphics.fill(x + 1, contentTop, x + tw - 1, contentTop + 1, bg);
            } else {
                // Inactive tab has a bottom border, sitting visually above the content box.
                guiGraphics.fill(x, TAB_TOP + TAB_HEIGHT - 1, x + tw, TAB_TOP + TAB_HEIGHT, COLOR_BORDER);
            }

            // Centred label
            int textX = x + (tw - font.width(tabs[i].label)) / 2;
            int textY = TAB_TOP + (TAB_HEIGHT - font.lineHeight) / 2;
            guiGraphics.drawString(font, tabs[i].label, textX, textY, textColor, false);
        }
    }

    private net.minecraft.client.gui.components.AbstractWidget activeList() {
        return switch (activeTab) {
            case MOBS  -> mobTypeList;
            case ITEMS -> itemDropList;
            case XP    -> mobXpList;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseY >= TAB_TOP && mouseY < TAB_TOP + TAB_HEIGHT) {
            Tab[] tabs = Tab.values();
            for (int i = 0; i < tabs.length; i++) {
                int x = tabX(i);
                if (mouseX >= x && mouseX < x + tabWidth()) {
                    switchTab(tabs[i]);
                    return true;
                }
            }
        }
        // Route to the active list only; inactive lists share the same bounds and
        // would otherwise consume the event even though they are not visible.
        if (activeList().mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return activeList().mouseScrolled(mouseX, mouseY, scrollX, scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return activeList().mouseDragged(mouseX, mouseY, button, dragX, dragY)
                || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private Component radiusLabel() {
        return Component.literal("Radius: " + radiusChunks + " chunks");
    }

    private Component windowLabel() {
        return Component.literal("Window: " + windowSeconds + " sec");
    }

    private Component toggleLabel() {
        return Component.literal(enabled ? "Tracking: ON" : "Tracking: OFF");
    }

    private static int cycleWindow(int current) {
        int[] steps = {1, 2, 5, 10, 15, 30, 60, 120, 300, 600};
        for (int step : steps) {
            if (step > current) {
                return step;
            }
        }
        return steps[0];
    }

    private void pushSettings() {
        ClientSettings.apply(enabled, radiusChunks, windowSeconds, filter);
    }

    @Override
    public void tick() {
        super.tick();
        ticksSinceRefresh++;
        if (ticksSinceRefresh >= 20) {
            ticksSinceRefresh = 0;
            if (activeTab == Tab.MOBS) {
                mobTypeList.refresh();
            } else if (activeTab == Tab.ITEMS) {
                itemDropList.refresh();
            } else {
                mobXpList.refresh();
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        renderTabs(guiGraphics);

        int titleY = MARGIN + CONTROLS_HEIGHT + 20;
        guiGraphics.drawString(font, title, MARGIN + 4, titleY, 0xFFFFFF, true);

        int summaryY = titleY + 14;
        if (activeTab == Tab.MOBS) {
            String totals = "Total nearby: %d   Spawn: %.2f/s   Kill: %.2f/s   Cramming: %.2f/s".formatted(
                    ClientMobStats.getCurrentCount(),
                    ClientMobStats.getSpawnRatePerSecond(),
                    ClientMobStats.getNonCrammingDeathRatePerSecond(),
                    ClientMobStats.getCrammingDeathRatePerSecond());
            guiGraphics.drawString(font, totals, MARGIN + 4, summaryY, 0xFFFFFF, false);

            int columnsY = summaryY + 16;
            guiGraphics.drawString(font, "Mob",      MARGIN + 6,   columnsY, 0xA0A0A0, false);
            guiGraphics.drawString(font, "Count",    MARGIN + 150, columnsY, 0xA0A0A0, false);
            guiGraphics.drawString(font, "Spawn/s",  MARGIN + 200, columnsY, 0x7FFF7F, false);
            guiGraphics.drawString(font, "Kill/s",   MARGIN + 260, columnsY, 0xFF7F7F, false);
            guiGraphics.drawString(font, "Cram/s",   MARGIN + 320, columnsY, 0xFFC080, false);
            guiGraphics.drawString(font, "Avg life", MARGIN + 380, columnsY, 0xA0C0FF, false);
        } else if (activeTab == Tab.ITEMS) {
            String totals = "Total drops: %.2f/s".formatted(ClientMobStats.getTotalItemDropRatePerSecond());
            guiGraphics.drawString(font, totals, MARGIN + 4, summaryY, 0xFFFFFF, false);

            int columnsY = summaryY + 16;
            guiGraphics.drawString(font, "Item",   MARGIN + 6,   columnsY, 0xA0A0A0, false);
            guiGraphics.drawString(font, "Drop/s", MARGIN + 250, columnsY, 0xFFD700, false);
        } else {
            String totals = "Total XP: %.2f/s".formatted(ClientMobStats.getTotalXpRatePerSecond());
            guiGraphics.drawString(font, totals, MARGIN + 4, summaryY, 0xFFFFFF, false);

            int columnsY = summaryY + 16;
            guiGraphics.drawString(font, "Mob",   MARGIN + 6,   columnsY, 0xA0A0A0, false);
            guiGraphics.drawString(font, "XP/s",  MARGIN + 250, columnsY, 0x80FF80, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
