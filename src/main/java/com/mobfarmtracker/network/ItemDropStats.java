package com.mobfarmtracker.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** Per-item-type drop count within one {@link MobStatsPayload} window. {@code dropCount} is the total number of items (summing stack sizes), not the number of drop events. */
public record ItemDropStats(ResourceLocation itemId, int dropCount) {
    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ItemDropStats> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, ItemDropStats::itemId,
            ByteBufCodecs.VAR_INT, ItemDropStats::dropCount,
            ItemDropStats::new
    );
}
