package com.mobfarmtracker.client;

import com.mobfarmtracker.network.ItemDropStats;
import com.mobfarmtracker.network.MobStatsPayload;
import com.mobfarmtracker.network.MobTypeStats;

import java.util.List;

/** Latest stats received from the server, read by the HUD overlay and the detail screen. */
public final class ClientMobStats {
    private static volatile MobStatsPayload latest = new MobStatsPayload(0, 0, 0, 0, 10, List.of(), List.of(), 0);

    private ClientMobStats() {
    }

    public static void update(MobStatsPayload payload) {
        latest = payload;
    }

    public static int getCurrentCount() {
        return latest.totalCurrentCount();
    }

    public static double getSpawnRatePerSecond() {
        return perSecond(latest.totalSpawnCount());
    }

    public static double getNonCrammingDeathRatePerSecond() {
        return perSecond(latest.totalNonCrammingDeathCount());
    }

    public static double getCrammingDeathRatePerSecond() {
        return perSecond(latest.totalCrammingDeathCount());
    }

    public static List<MobTypeStats> getPerType() {
        return latest.perType();
    }

    public static List<ItemDropStats> getPerItem() {
        return latest.perItem();
    }

    public static double getTotalItemDropRatePerSecond() {
        return latest.perItem().stream().mapToInt(ItemDropStats::dropCount).sum() / (double) Math.max(1, latest.windowSeconds());
    }

    public static double getTotalXpRatePerSecond() {
        return latest.totalXpDropped() / (double) Math.max(1, latest.windowSeconds());
    }

    public static double getXpRatePerSecond(int xpDropped) {
        return xpDropped / (double) Math.max(1, latest.windowSeconds());
    }

    public static int getWindowSeconds() {
        return latest.windowSeconds();
    }

    public static double perSecond(int countInWindow) {
        int windowSeconds = latest.windowSeconds();
        return windowSeconds <= 0 ? 0 : countInWindow / (double) windowSeconds;
    }
}
