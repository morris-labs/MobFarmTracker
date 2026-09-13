package com.mobfarmtracker.client;

import com.mobfarmtracker.MobFarmTracker;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** Both unbound by default; bind them under Options > Controls > Key Binds > Mob Farm Tracker. */
public final class KeyBindings {
    public static final KeyMapping OPEN_GUI = new KeyMapping(
            "key." + MobFarmTracker.MODID + ".open_gui",
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories." + MobFarmTracker.MODID);

    public static final KeyMapping TOGGLE_TRACKING = new KeyMapping(
            "key." + MobFarmTracker.MODID + ".toggle_tracking",
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories." + MobFarmTracker.MODID);

    private KeyBindings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GUI);
        event.register(TOGGLE_TRACKING);
    }
}
