package com.mobfarmtracker.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Per-entity-type counts within one {@link MobStatsPayload}. {@code avgLifespanSeconds} is the
 * average time from spawn to death, over the same rolling window as the rate counts, for mobs
 * that died of any cause; it's {@code 0} when no tracked death in the window had a known spawn
 * time.
 */
public record MobTypeStats(
        ResourceLocation typeId,
        int currentCount,
        int spawnCount,
        int nonCrammingDeathCount,
        int crammingDeathCount,
        float avgLifespanSeconds
) {
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, MobTypeStats> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, MobTypeStats::typeId,
            ByteBufCodecs.VAR_INT, MobTypeStats::currentCount,
            ByteBufCodecs.VAR_INT, MobTypeStats::spawnCount,
            ByteBufCodecs.VAR_INT, MobTypeStats::nonCrammingDeathCount,
            ByteBufCodecs.VAR_INT, MobTypeStats::crammingDeathCount,
            ByteBufCodecs.FLOAT, MobTypeStats::avgLifespanSeconds,
            MobTypeStats::new
    );
}
