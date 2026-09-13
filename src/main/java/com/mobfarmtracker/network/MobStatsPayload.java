package com.mobfarmtracker.network;

import com.mobfarmtracker.MobFarmTracker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-to-client stats for the mob farm tracker, sent about once a second per player.
 * Carries the aggregate totals for the HUD overlay plus a per-entity-type breakdown for
 * the detail screen.
 */
public record MobStatsPayload(
        int totalCurrentCount,
        int totalSpawnCount,
        int totalNonCrammingDeathCount,
        int totalCrammingDeathCount,
        int windowSeconds,
        List<MobTypeStats> perType
) implements CustomPacketPayload {
    public static final Type<MobStatsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MobFarmTracker.MODID, "mob_stats"));

    @Override
    public Type<MobStatsPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, MobStatsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MobStatsPayload::totalCurrentCount,
            ByteBufCodecs.VAR_INT, MobStatsPayload::totalSpawnCount,
            ByteBufCodecs.VAR_INT, MobStatsPayload::totalNonCrammingDeathCount,
            ByteBufCodecs.VAR_INT, MobStatsPayload::totalCrammingDeathCount,
            ByteBufCodecs.VAR_INT, MobStatsPayload::windowSeconds,
            ByteBufCodecs.collection(ArrayList::new, MobTypeStats.STREAM_CODEC), MobStatsPayload::perType,
            MobStatsPayload::new
    );
}
