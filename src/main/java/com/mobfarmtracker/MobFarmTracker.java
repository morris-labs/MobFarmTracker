package com.mobfarmtracker;

import com.mobfarmtracker.client.ClientConfig;
import com.mobfarmtracker.client.ClientInputHandler;
import com.mobfarmtracker.client.ClientSetup;
import com.mobfarmtracker.client.KeyBindings;
import com.mobfarmtracker.network.NetworkHandler;
import com.mobfarmtracker.server.ServerMobTracker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Tracks the mob population, spawn rate, and death rate (split into cramming kills and
 * everything else) near each player, broken down per entity type, and shows the results on
 * a HUD overlay and an in-game detail screen. Tracking runs on the logical server, since
 * only the server knows a mob's real cause of death; the results are synced to each
 * player's client for display. Install this mod on both the client and the server that
 * hosts your farm.
 */
@Mod(MobFarmTracker.MODID)
public final class MobFarmTracker {
    public static final String MODID = "mobfarmtracker";

    public MobFarmTracker(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        modEventBus.addListener(NetworkHandler::register);
        NeoForge.EVENT_BUS.register(ServerMobTracker.class);

        if (dist == Dist.CLIENT) {
            modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
            modEventBus.addListener(ClientSetup::registerOverlays);
            modEventBus.addListener(KeyBindings::register);
            NeoForge.EVENT_BUS.register(ClientInputHandler.class);
        }
    }
}
