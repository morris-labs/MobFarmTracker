package com.mobfarmtracker.client;

import com.mobfarmtracker.MobFarmTracker;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class ClientSetup {
    private ClientSetup() {
    }

    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(MobFarmTracker.MODID, "mob_farm_overlay"),
                MobFarmHudOverlay.INSTANCE);
    }
}
