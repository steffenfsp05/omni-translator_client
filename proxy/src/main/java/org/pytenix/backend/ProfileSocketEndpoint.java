package org.pytenix.backend;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.packets.impl.ProfileMapper;
import org.pytenix.proto.generated.NetworkPackets;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


//TODO MERGE WITH SPIGOT SIDE;; MAKE ABSTRACT
public class ProfileSocketEndpoint {
    private final OmniConnectionService connectionManager;

    private final ConcurrentHashMap<UUID, CompletableFuture<ProfileMapper.ProfileData>> requestQueue = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, CompletableFuture<ProfileMapper.ProfileData>> inFlightFetches = new ConcurrentHashMap<>();

    private final Cache<DuplicationKey, Boolean> deduplicationCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMillis(500))
            .build();

    private final Cache<UUID, ProfileMapper.ProfileData> profileCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(3))
            .build();

    public ProfileSocketEndpoint(OmniConnectionService connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void handleProfileResult(ProfileMapper.ProfileData resultData) {
        profileCache.put(resultData.playerId(), resultData);

        CompletableFuture<ProfileMapper.ProfileData> future = requestQueue.remove(resultData.requestId());
        if (future != null) {
            future.complete(resultData);
        }
    }

    public void updateProfile(ProfileMapper.ProfileData profileData) {
        DuplicationKey key = new DuplicationKey(profileData.playerId(), NetworkPackets.ProfilePacket.Action.UPDATE);

        if (deduplicationCache.asMap().putIfAbsent(key, Boolean.TRUE) != null) {
            return;
        }

        profileCache.invalidate(profileData.playerId());

        connectionManager.sendPacket(PacketRegistry.PROFILE,
                PacketMapperRegistry.toProto(
                        profileData.withAction(NetworkPackets.ProfilePacket.Action.UPDATE)
                ));
    }

    public CompletableFuture<ProfileMapper.ProfileData> getProfile(UUID playerId) {
        ProfileMapper.ProfileData cachedProfile = profileCache.getIfPresent(playerId);
        if (cachedProfile != null) {
            return CompletableFuture.completedFuture(cachedProfile);
        }

        return inFlightFetches.computeIfAbsent(playerId, id -> {
            CompletableFuture<ProfileMapper.ProfileData> future = new CompletableFuture<>();
            UUID requestId = UUID.randomUUID();

            requestQueue.put(requestId, future);

            ProfileMapper.ProfileData profileData = new ProfileMapper.ProfileData(
                    connectionManager.getApiKey(),
                    NetworkPackets.ProfilePacket.Action.FETCH,
                    playerId,
                    requestId,
                    NetworkPackets.ProfilePacket.ConsentType.UNKNOWN
            );

            System.out.println("SENDING " + profileData);

            connectionManager.sendPacket(PacketRegistry.PROFILE, PacketMapperRegistry.toProto(profileData));

            return future.orTimeout(60, TimeUnit.SECONDS)
                    .whenComplete((res, ex) -> {
                        requestQueue.remove(requestId);
                        inFlightFetches.remove(playerId);
                    })
                    .exceptionally(ex -> new ProfileMapper.ProfileData("NULL", null, null, null, null));
        });
    }

    public record DuplicationKey(UUID uuid, NetworkPackets.ProfilePacket.Action action) {}
}