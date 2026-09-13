package com.mobfarmtracker.client;

import com.mobfarmtracker.client.gui.MobFarmScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class ClientInputHandler {
    private ClientInputHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        while (KeyBindings.OPEN_GUI.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new MobFarmScreen());
            }
        }
        while (KeyBindings.TOGGLE_TRACKING.consumeClick()) {
            ClientSettings.setEnabled(!ClientSettings.isEnabled());
        }
    }
}
