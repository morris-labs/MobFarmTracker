package com.mobfarmtracker.server;

import com.mobfarmtracker.CommonConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Lets a server operator change {@link CommonConfig#POPULATION_SAMPLE_INTERVAL_TICKS} while the
 * server is running, since it's a performance-sensitive setting best tuned by watching live tick
 * time rather than guessed at and applied only on the next restart.
 */
public final class ServerCommands {
    private ServerCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("mobfarmtracker")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("samplerate")
                        .executes(ctx -> {
                            reportSampleRate(ctx.getSource());
                            return 1;
                        })
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 200))
                                .executes(ctx -> {
                                    int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                    CommonConfig.POPULATION_SAMPLE_INTERVAL_TICKS.set(ticks);
                                    reportSampleRate(ctx.getSource());
                                    return 1;
                                }))));
    }

    private static void reportSampleRate(net.minecraft.commands.CommandSourceStack source) {
        int ticks = CommonConfig.POPULATION_SAMPLE_INTERVAL_TICKS.get();
        String perSecond = "%.1f".formatted(20.0 / ticks);
        source.sendSuccess(() -> Component.literal(
                "Mob population sample interval: " + ticks + " ticks (" + perSecond + "/sec)."), true);
    }
}
