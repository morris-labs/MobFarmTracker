package com.mobfarmtracker.network;

import com.mobfarmtracker.MobFarmTracker;
import com.mobfarmtracker.server.ServerMobTracker;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkHandler {
    private NetworkHandler() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MobFarmTracker.MODID);
        registrar.playToClient(MobStatsPayload.TYPE, MobStatsPayload.STREAM_CODEC,
                com.mobfarmtracker.client.MobStatsClientHandler::handle);
        registrar.playToServer(PlayerSettingsPayload.TYPE, PlayerSettingsPayload.STREAM_CODEC,
                ServerMobTracker::handleSettings);
    }
}
