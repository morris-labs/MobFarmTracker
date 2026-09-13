package com.mobfarmtracker.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only display settings for the HUD overlay. */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue HUD_X;
    public static final ModConfigSpec.IntValue HUD_Y;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("hud");
        HUD_X = builder
                .comment("X position, in pixels, of the HUD overlay's top-left corner.")
                .defineInRange("x", 4, 0, 10000);
        HUD_Y = builder
                .comment("Y position, in pixels, of the HUD overlay's top-left corner.")
                .defineInRange("y", 4, 0, 10000);
        builder.pop();

        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}
