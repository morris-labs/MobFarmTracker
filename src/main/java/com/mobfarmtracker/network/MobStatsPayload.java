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
 * Carries the aggregate totals for the HUD overlay plus per-entity-type and per-item-type
 * breakdowns for the detail screen.
 */
public record MobStatsPayload(
        int totalCurrentCount,
        int totalSpawnCount,
        int totalNonCrammingDeathCount,
        int totalCrammingDeathCount,
        int windowSeconds,
        List<MobTypeStats> perType,
        List<ItemDropStats> perItem,
        int totalXpDropped
) implements CustomPacketPayload {
    public static final Type<MobStatsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MobFarmTracker.MODID, "mob_stats"));

    @Override
    public Type<MobStatsPayload> type() {
        return TYPE;
    }

    private static final StreamCodec<FriendlyByteBuf, List<MobTypeStats>> MOB_LIST_CODEC =
            ByteBufCodecs.collection(ArrayList::new, MobTypeStats.STREAM_CODEC);
    private static final StreamCodec<FriendlyByteBuf, List<ItemDropStats>> ITEM_LIST_CODEC =
            ByteBufCodecs.collection(ArrayList::new, ItemDropStats.STREAM_CODEC);

    // StreamCodec.composite supports at most 6 fields, so encode/decode manually.
    public static final StreamCodec<FriendlyByteBuf, MobStatsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.totalCurrentCount());
                buf.writeVarInt(p.totalSpawnCount());
                buf.writeVarInt(p.totalNonCrammingDeathCount());
                buf.writeVarInt(p.totalCrammingDeathCount());
                buf.writeVarInt(p.windowSeconds());
                MOB_LIST_CODEC.encode(buf, p.perType());
                ITEM_LIST_CODEC.encode(buf, p.perItem());
                buf.writeVarInt(p.totalXpDropped());
            },
            buf -> new MobStatsPayload(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    MOB_LIST_CODEC.decode(buf),
                    ITEM_LIST_CODEC.decode(buf),
                    buf.readVarInt()
            )
    );
}
