package com.mobfarmtracker.client.gui;

import com.mobfarmtracker.MobFilter;
import com.mobfarmtracker.client.ClientMobStats;
import com.mobfarmtracker.client.ClientSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Detail screen: per-mob spawn/kill/cramming breakdown, plus radius, sample window, and mob-type filter controls. */
public final class MobFarmScreen extends Screen {
    private static final int MARGIN = 20;
    // Controls occupy y=[MARGIN, MARGIN+50). The title and summary text sit below that gap
    // rather than under the Done button, which used to overlap them.
    private static final int CONTROLS_HEIGHT = 50;
    private static final int HEADER_HEIGHT = CONTROLS_HEIGHT + 45;
    private static final int ROW_HEIGHT = 14;

    private boolean enabled = ClientSettings.isEnabled();
    private int radiusChunks = ClientSettings.getRadiusChunks();
    private int windowSeconds = ClientSettings.getWindowSeconds();
    private MobFilter filter = ClientSettings.getFilter();

    private Button radiusButton;
    private Button windowButton;
    private MobTypeList mobTypeList;
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
    }

    private String radiusLabelText() {
        return "Radius: " + radiusChunks + " chunks";
    }

    private Component radiusLabel() {
        return Component.literal(radiusLabelText());
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
            mobTypeList.refresh();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int titleY = MARGIN + CONTROLS_HEIGHT;
        guiGraphics.drawString(font, title, MARGIN, titleY, 0xFFFFFF, true);

        String totals = "Total nearby: %d   Spawn: %.2f/s   Kill: %.2f/s   Cramming: %.2f/s".formatted(
                ClientMobStats.getCurrentCount(),
                ClientMobStats.getSpawnRatePerSecond(),
                ClientMobStats.getNonCrammingDeathRatePerSecond(),
                ClientMobStats.getCrammingDeathRatePerSecond());
        int totalsY = titleY + 14;
        guiGraphics.drawString(font, totals, MARGIN, totalsY, 0xFFFFFF, false);

        int columnsY = totalsY + 16;
        guiGraphics.drawString(font, "Mob", MARGIN + 6, columnsY, 0xA0A0A0, false);
        guiGraphics.drawString(font, "Count", MARGIN + 150, columnsY, 0xA0A0A0, false);
        guiGraphics.drawString(font, "Spawn/s", MARGIN + 200, columnsY, 0x7FFF7F, false);
        guiGraphics.drawString(font, "Kill/s", MARGIN + 260, columnsY, 0xFF7F7F, false);
        guiGraphics.drawString(font, "Cram/s", MARGIN + 320, columnsY, 0xFFC080, false);
        guiGraphics.drawString(font, "Avg life", MARGIN + 380, columnsY, 0xA0C0FF, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
