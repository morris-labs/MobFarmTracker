package com.mobfarmtracker.server;

import com.mobfarmtracker.CommonConfig;
import com.mobfarmtracker.MobFilter;
import com.mobfarmtracker.network.MobStatsPayload;
import com.mobfarmtracker.network.MobTypeStats;
import com.mobfarmtracker.network.PlayerSettingsPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks, per player, the mob population within their configured radius, plus rolling
 * spawn and death counts broken down by entity type. Deaths are split into mob-cramming
 * kills and everything else, so a farm's actual kill mechanism (lava, fall damage, etc.)
 * can be told apart from mobs simply crushing each other once too many are packed into
 * one block. Each player can filter to hostile mobs, passive/neutral mobs, or both, and
 * adjust their own radius and sample window from the in-game HUD screen.
 */
public final class ServerMobTracker {
    private static final Map<UUID, PlayerData> DATA = new HashMap<>();

    // Global (not per-player): a mob spawns and dies once regardless of how many players are
    // tracking it, so its spawn tick only needs recording once. Entries are removed on death
    // (after computing the lifespan) or on EntityLeaveLevelEvent otherwise (chunk unload,
    // despawn, dimension change), so this stays bounded by the live mob population rather than
    // growing unbounded over server uptime.
    private static final Map<Integer, Long> SPAWN_TICK_BY_ID = new HashMap<>();

    private ServerMobTracker() {
    }

    private static PlayerData dataFor(UUID playerId) {
        return DATA.computeIfAbsent(playerId, id -> new PlayerData());
    }

    public static void handleSettings(PlayerSettingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            PlayerData data = dataFor(player.getUUID());
            data.enabled = payload.enabled();
            data.radiusChunks = Math.clamp(payload.radiusChunks(), 1, 32);
            data.windowSeconds = Math.clamp(payload.windowSeconds(), 1, 600);
            data.filter = MobFilter.byOrdinalSafe(payload.filterOrdinal());
        });
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        // loadedFromDisk() is true when an entity already in the world reappears because its
        // chunk (re)loaded - not a real spawn. Without this check, walking around re-loads
        // leftover mobs (or mobs just outside render distance) and counts each reload as a
        // "spawn," which is why spawn rate showed activity even on Peaceful with nothing alive.
        if (event.getLevel().isClientSide() || event.loadedFromDisk() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }
        ServerLevel level = (ServerLevel) event.getLevel();
        // Box once: avoids re-boxing the same tick's timestamp on every addLast call below,
        // which matters during spawn bursts (chunk loads, room-clear explosions).
        Long now = level.getGameTime();
        SPAWN_TICK_BY_ID.put(mob.getId(), now);
        for (ServerPlayer player : level.players()) {
            PlayerData data = dataFor(player.getUUID());
            if (!data.enabled) {
                continue;
            }
            if (data.filter.matches(mob) && withinRadius(player, mob, data.radiusChunks)) {
                data.spawnTimes.computeIfAbsent(mob.getType(), t -> new ArrayDeque<>()).addLast(now);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        Long now = level.getGameTime();
        boolean cramming = event.getSource().is(DamageTypes.CRAMMING);
        Long spawnTick = SPAWN_TICK_BY_ID.remove(mob.getId());
        // -1 means we never saw this mob spawn (it existed before the mod loaded, or before the
        // player came in range) - still count the death, just leave it out of the lifespan average.
        long lifespanTicks = spawnTick != null ? now - spawnTick : -1;
        for (ServerPlayer player : level.players()) {
            PlayerData data = dataFor(player.getUUID());
            if (!data.enabled) {
                continue;
            }
            if (data.filter.matches(mob) && withinRadius(player, mob, data.radiusChunks)) {
                Map<EntityType<?>, Deque<Long>> target = cramming ? data.crammingDeathTimes : data.otherDeathTimes;
                target.computeIfAbsent(mob.getType(), t -> new ArrayDeque<>()).addLast(now);
                if (lifespanTicks >= 0) {
                    data.lifespanSamples.computeIfAbsent(mob.getType(), t -> new ArrayDeque<>())
                            .addLast(new LifespanSample(now, lifespanTicks));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Mob) {
            SPAWN_TICK_BY_ID.remove(event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            PlayerData data = dataFor(player.getUUID());

            if (!data.enabled) {
                if (data.notifiedDisabled) {
                    continue;
                }
                data.notifiedDisabled = true;
                data.spawnTimes.clear();
                data.otherDeathTimes.clear();
                data.crammingDeathTimes.clear();
                data.lifespanSamples.clear();
                data.currentCounts = Map.of();
                PacketDistributor.sendToPlayer(player, buildPayload(data));
                continue;
            }
            data.notifiedDisabled = false;

            ServerLevel level = player.serverLevel();
            long now = level.getGameTime();
            long windowTicks = data.windowSeconds * 20L;

            pruneAll(data.spawnTimes, now, windowTicks);
            pruneAll(data.otherDeathTimes, now, windowTicks);
            pruneAll(data.crammingDeathTimes, now, windowTicks);
            pruneLifespans(data.lifespanSamples, now, windowTicks);

            if (now % CommonConfig.POPULATION_SAMPLE_INTERVAL_TICKS.get() == 0) {
                data.currentCounts = countNearbyMobs(level, player, data);
                PacketDistributor.sendToPlayer(player, buildPayload(data));
            }
        }
    }

    private static Map<EntityType<?>, Integer> countNearbyMobs(ServerLevel level, ServerPlayer player, PlayerData data) {
        // Query a box a chunk wider than the radius so the exact chunk-square check below never
        // has to look outside it, then narrow to the real square with withinRadius.
        double queryRadiusBlocks = (data.radiusChunks + 1) * 16.0;
        AABB box = player.getBoundingBox().inflate(queryRadiusBlocks, level.getHeight(), queryRadiusBlocks);
        List<Mob> mobs = level.getEntities(EntityTypeTest.forClass(Mob.class), box,
                mob -> data.filter.matches(mob) && withinRadius(player, mob, data.radiusChunks));

        Map<EntityType<?>, Integer> counts = new HashMap<>();
        for (Mob mob : mobs) {
            counts.merge(mob.getType(), 1, Integer::sum);
        }
        return counts;
    }

    private static MobStatsPayload buildPayload(PlayerData data) {
        java.util.Set<EntityType<?>> types = new java.util.HashSet<>();
        types.addAll(data.currentCounts.keySet());
        types.addAll(data.spawnTimes.keySet());
        types.addAll(data.otherDeathTimes.keySet());
        types.addAll(data.crammingDeathTimes.keySet());
        types.addAll(data.lifespanSamples.keySet());

        List<MobTypeStats> perType = new ArrayList<>();
        int totalCurrent = 0;
        int totalSpawn = 0;
        int totalOtherDeaths = 0;
        int totalCramming = 0;

        for (EntityType<?> type : types) {
            int current = data.currentCounts.getOrDefault(type, 0);
            int spawn = sizeOf(data.spawnTimes.get(type));
            int otherDeaths = sizeOf(data.otherDeathTimes.get(type));
            int cramming = sizeOf(data.crammingDeathTimes.get(type));
            float avgLifespanSeconds = averageLifespanSeconds(data.lifespanSamples.get(type));
            if (current == 0 && spawn == 0 && otherDeaths == 0 && cramming == 0) {
                continue;
            }
            totalCurrent += current;
            totalSpawn += spawn;
            totalOtherDeaths += otherDeaths;
            totalCramming += cramming;

            perType.add(new MobTypeStats(BuiltInRegistries.ENTITY_TYPE.getKey(type), current, spawn, otherDeaths, cramming, avgLifespanSeconds));
        }

        perType.sort(Comparator.comparingInt(MobTypeStats::currentCount).reversed());

        return new MobStatsPayload(totalCurrent, totalSpawn, totalOtherDeaths, totalCramming, data.windowSeconds, perType);
    }

    private static int sizeOf(Deque<Long> deque) {
        return deque == null ? 0 : deque.size();
    }

    private static float averageLifespanSeconds(Deque<LifespanSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return 0f;
        }
        long totalTicks = 0;
        for (LifespanSample sample : samples) {
            totalTicks += sample.lifespanTicks();
        }
        return (totalTicks / (float) samples.size()) / 20f;
    }

    /**
     * True if the mob is within {@code radiusChunks} chunks of the player, using the same
     * square (Chebyshev-distance-in-chunks) region vanilla uses for view/simulation distance -
     * not a circle, which would exclude mobs sitting diagonally from the player even though
     * they're well within "N chunks" by the chunk grid.
     */
    private static boolean withinRadius(Player player, Mob mob, int radiusChunks) {
        int playerChunkX = Mth.floor(player.getX()) >> 4;
        int playerChunkZ = Mth.floor(player.getZ()) >> 4;
        int mobChunkX = Mth.floor(mob.getX()) >> 4;
        int mobChunkZ = Mth.floor(mob.getZ()) >> 4;
        return Math.abs(mobChunkX - playerChunkX) <= radiusChunks && Math.abs(mobChunkZ - playerChunkZ) <= radiusChunks;
    }

    private static void pruneAll(Map<EntityType<?>, Deque<Long>> map, long now, long windowTicks) {
        for (Deque<Long> deque : map.values()) {
            while (!deque.isEmpty() && now - deque.peekFirst() > windowTicks) {
                deque.removeFirst();
            }
        }
        map.values().removeIf(Deque::isEmpty);
    }

    private static void pruneLifespans(Map<EntityType<?>, Deque<LifespanSample>> map, long now, long windowTicks) {
        for (Deque<LifespanSample> deque : map.values()) {
            while (!deque.isEmpty() && now - deque.peekFirst().deathTick() > windowTicks) {
                deque.removeFirst();
            }
        }
        map.values().removeIf(Deque::isEmpty);
    }

    /** One mob's time-to-death, recorded against the tick it died so the sample can be pruned like any other rate. */
    private record LifespanSample(long deathTick, long lifespanTicks) {
    }

    private static final class PlayerData {
        boolean enabled = true;
        boolean notifiedDisabled = false;
        int radiusChunks = CommonConfig.RADIUS_CHUNKS.get();
        int windowSeconds = CommonConfig.RATE_WINDOW_SECONDS.get();
        MobFilter filter = MobFilter.HOSTILE;

        final Map<EntityType<?>, Deque<Long>> spawnTimes = new HashMap<>();
        final Map<EntityType<?>, Deque<Long>> otherDeathTimes = new HashMap<>();
        final Map<EntityType<?>, Deque<Long>> crammingDeathTimes = new HashMap<>();
        final Map<EntityType<?>, Deque<LifespanSample>> lifespanSamples = new HashMap<>();
        Map<EntityType<?>, Integer> currentCounts = new HashMap<>();
    }
}
