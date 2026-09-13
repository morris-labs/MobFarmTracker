package com.mobfarmtracker.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import org.jetbrains.annotations.NotNull;

/** Draws the monster count and per-second spawn/kill/cramming rates in the corner of the screen. */
public final class MobFarmHudOverlay implements LayeredDraw.Layer {
    public static final MobFarmHudOverlay INSTANCE = new MobFarmHudOverlay();

    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int LINE_HEIGHT = 10;

    private MobFarmHudOverlay() {
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        if (!ClientSettings.isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }

        int x = ClientConfig.HUD_X.get();
        int y = ClientConfig.HUD_Y.get();

        String[] lines = {
                "Mobs nearby: %d".formatted(ClientMobStats.getCurrentCount()),
                "Spawn rate: %.2f/sec".formatted(ClientMobStats.getSpawnRatePerSecond()),
                "Kill rate: %.2f/sec".formatted(ClientMobStats.getNonCrammingDeathRatePerSecond()),
                "Cramming rate: %.2f/sec".formatted(ClientMobStats.getCrammingDeathRatePerSecond()),
        };

        for (int i = 0; i < lines.length; i++) {
            guiGraphics.drawString(mc.font, lines[i], x, y + i * LINE_HEIGHT, TEXT_COLOR, true);
        }
    }
}
