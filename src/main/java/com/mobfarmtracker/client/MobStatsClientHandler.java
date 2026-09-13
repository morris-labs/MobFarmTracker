package com.mobfarmtracker.client;

import com.mobfarmtracker.network.MobStatsPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class MobStatsClientHandler {
    private MobStatsClientHandler() {
    }

    public static void handle(MobStatsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientMobStats.update(payload));
    }
}
