package com.mobfarmtracker.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Per-entity-type counts within one {@link MobStatsPayload}. {@code avgLifespanSeconds} is the
 * average time from spawn to death, over the same rolling window as the rate counts, for mobs
 * that died of any cause; it's {@code 0} when no tracked death in the window had a known spawn
 * time. {@code xpDropped} is the total XP dropped by this mob type within the window.
 */
public record MobTypeStats(
        ResourceLocation typeId,
        int currentCount,
        int spawnCount,
        int nonCrammingDeathCount,
        int crammingDeathCount,
        float avgLifespanSeconds,
        int xpDropped
) {
    // StreamCodec.composite supports at most 6 fields, so encode/decode manually.
    public static final StreamCodec<FriendlyByteBuf, MobTypeStats> STREAM_CODEC = StreamCodec.of(
            (buf, s) -> {
                ResourceLocation.STREAM_CODEC.encode(buf, s.typeId());
                buf.writeVarInt(s.currentCount());
                buf.writeVarInt(s.spawnCount());
                buf.writeVarInt(s.nonCrammingDeathCount());
                buf.writeVarInt(s.crammingDeathCount());
                buf.writeFloat(s.avgLifespanSeconds());
                buf.writeVarInt(s.xpDropped());
            },
            buf -> new MobTypeStats(
                    ResourceLocation.STREAM_CODEC.decode(buf),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readFloat(),
                    buf.readVarInt()
            )
    );
}
