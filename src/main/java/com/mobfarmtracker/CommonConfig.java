package com.mobfarmtracker;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Default tracking settings, used until a player customizes their own settings from the
 * in-game HUD screen (see {@code MobFarmScreen}). Settings are per-player after that, kept
 * in memory on the server, and reset to these defaults on server restart.
 */
public final class CommonConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue RADIUS_CHUNKS;
    public static final ModConfigSpec.IntValue RATE_WINDOW_SECONDS;
    public static final ModConfigSpec.IntValue POPULATION_SAMPLE_INTERVAL_TICKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("tracking");
        RADIUS_CHUNKS = builder
                .comment("Default radius, in chunks, around each player to count mobs in.")
                .defineInRange("radiusChunks", 5, 1, 32);
        RATE_WINDOW_SECONDS = builder
                .comment("Default length, in seconds, of the rolling sample window used to compute per-second spawn and death rates.")
                .defineInRange("rateWindowSeconds", 10, 1, 600);
        builder.pop();

        builder.push("performance");
        POPULATION_SAMPLE_INTERVAL_TICKS = builder
                .comment(
                        "How often, in ticks, to re-scan the world for the current mob population and push it to",
                        "each tracking player's client (20 ticks = 1/sec). Spawn and death counts are always exact",
                        "and unaffected by this - only the live population count is sampled at this rate.",
                        "Lower values make the population count more responsive but cost more server time per",
                        "scan, since each scan walks every mob in range. With a farm holding hundreds to low",
                        "thousands of mobs, prefer 5 (4/sec) over 1 (20/sec) unless you've confirmed on your own",
                        "hardware that 1 doesn't add noticeable tick time.")
                .defineInRange("populationSampleIntervalTicks", 5, 1, 200);
        builder.pop();

        SPEC = builder.build();
    }

    private CommonConfig() {
    }
}
