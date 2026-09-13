package com.mobfarmtracker.client;

import com.mobfarmtracker.MobFilter;
import com.mobfarmtracker.network.PlayerSettingsPayload;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * This client's desired tracking settings. Mirrors what was last sent to the server so the
 * settings screen can show the current values without a round trip, and so the HUD can hide
 * itself immediately on toggle without waiting for the server to echo the change back.
 */
public final class ClientSettings {
    private static boolean enabled = true;
    private static int radiusChunks = 5;
    private static int windowSeconds = 10;
    private static MobFilter filter = MobFilter.HOSTILE;

    private ClientSettings() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getRadiusChunks() {
        return radiusChunks;
    }

    public static int getWindowSeconds() {
        return windowSeconds;
    }

    public static MobFilter getFilter() {
        return filter;
    }

    public static void apply(boolean newEnabled, int newRadiusChunks, int newWindowSeconds, MobFilter newFilter) {
        enabled = newEnabled;
        radiusChunks = Math.clamp(newRadiusChunks, 1, 32);
        windowSeconds = Math.clamp(newWindowSeconds, 1, 600);
        filter = newFilter;
        PacketDistributor.sendToServer(new PlayerSettingsPayload(enabled, radiusChunks, windowSeconds, filter));
    }

    public static void setEnabled(boolean newEnabled) {
        apply(newEnabled, radiusChunks, windowSeconds, filter);
    }
}
