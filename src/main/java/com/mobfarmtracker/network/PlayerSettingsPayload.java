package com.mobfarmtracker.network;

import com.mobfarmtracker.MobFarmTracker;
import com.mobfarmtracker.MobFilter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client-to-server request to change this player's tracking on/off state, radius, sample window, or mob filter. */
public record PlayerSettingsPayload(boolean enabled, int radiusChunks, int windowSeconds, int filterOrdinal) implements CustomPacketPayload {
    public static final Type<PlayerSettingsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MobFarmTracker.MODID, "player_settings"));

    public PlayerSettingsPayload(boolean enabled, int radiusChunks, int windowSeconds, MobFilter filter) {
        this(enabled, radiusChunks, windowSeconds, filter.ordinal());
    }

    @Override
    public Type<PlayerSettingsPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<FriendlyByteBuf, PlayerSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PlayerSettingsPayload::enabled,
            ByteBufCodecs.VAR_INT, PlayerSettingsPayload::radiusChunks,
            ByteBufCodecs.VAR_INT, PlayerSettingsPayload::windowSeconds,
            ByteBufCodecs.VAR_INT, PlayerSettingsPayload::filterOrdinal,
            PlayerSettingsPayload::new
    );
}
