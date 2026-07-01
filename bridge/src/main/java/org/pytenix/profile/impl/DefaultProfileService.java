package org.pytenix.profile.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import org.pytenix.cache.CacheProvider;
import org.pytenix.packets.MappedPacketReceiveConsumer;
import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.packets.impl.ProfileMapper;
import org.pytenix.profile.ProfileService;
import org.pytenix.proto.generated.NetworkPackets;
import org.transport.TransportService;
import org.transport.service.PacketContext;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DefaultProfileService extends ProfileService {

    private final Consumer<NetworkPackets.ProfilePacket> sendEndpoint ;

    private final Supplier<String> licenseKey;

    private final ConcurrentHashMap<UUID, CompletableFuture<ProfileMapper.ProfileData>> inFlightFetches = new ConcurrentHashMap<>();


    private final Cache<UUID, ProfileMapper.ProfileData> cacheProvider = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(3000)
            .build();


    private final Cache<DuplicationKey, Boolean> deduplicationCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMillis(500))
            .build();


    public DefaultProfileService(Supplier<String> licenseKey, Consumer<NetworkPackets.ProfilePacket> sendEndpoint) {
        this.sendEndpoint = sendEndpoint;
        this.licenseKey = licenseKey;


    }



    @Override
    public Cache<UUID, ProfileMapper.ProfileData> cacheProvider() {
        return cacheProvider;
    }

    @Override
    public CompletableFuture<ProfileMapper.ProfileData> retrieveProfile(UUID uuid) {

        ProfileMapper.ProfileData cachedProfile = cacheProvider().getIfPresent(uuid);
        if (cachedProfile != null) {
            return CompletableFuture.completedFuture(cachedProfile);
        }

        return inFlightFetches.computeIfAbsent(uuid, id -> {
            CompletableFuture<ProfileMapper.ProfileData> future = new CompletableFuture<>();
            UUID requestId = UUID.randomUUID();

            ProfileMapper.ProfileData profileData = new ProfileMapper.ProfileData(
                    licenseKey.get(),
                    NetworkPackets.ProfilePacket.Action.FETCH,
                    uuid,       // Player-ID
                    requestId,  // Request-ID
                    NetworkPackets.ProfilePacket.ConsentType.UNKNOWN
            );

            queue.put(requestId, future);
            sendEndpoint.accept(PacketMapperRegistry.toProto(profileData));

            return future.orTimeout(60, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        System.err.println("Fehler beim Abrufen des Profils für " + uuid + ": " + ex.getMessage());
                        return new ProfileMapper.ProfileData(
                                "NULL",
                                NetworkPackets.ProfilePacket.Action.FETCH,
                                uuid, // Original UUID behalten
                                requestId,
                                NetworkPackets.ProfilePacket.ConsentType.UNKNOWN
                        );
                    });
        });
    }

    public void updateProfile(ProfileMapper.ProfileData profileData) {



        DuplicationKey key = new DuplicationKey(profileData.playerId(), NetworkPackets.ProfilePacket.Action.UPDATE);

        if (deduplicationCache.asMap().putIfAbsent(key, Boolean.TRUE) != null) {
            return;
        }

        //NOT WORKING ON SPIGOT SIDE
        cacheProvider().put(profileData.playerId(), profileData);


        sendEndpoint.accept(PacketMapperRegistry.toProto(
                profileData.withAction(NetworkPackets.ProfilePacket.Action.UPDATE)
        ));

    }

    @Override
    public void handleProfileResult(ProfileMapper.ProfileData resultData) {

        final UUID requestId = resultData.requestId();
        final UUID playerId = resultData.playerId();

        System.out.println("AAA INCOMING " + new Gson().toJson(resultData));

        CompletableFuture<ProfileMapper.ProfileData> future = queue.remove(requestId);

        inFlightFetches.remove(playerId);

        cacheProvider().put(playerId, resultData);

        if (future != null) {
            future.complete(resultData);
        }
    }

    public record DuplicationKey(UUID uuid, NetworkPackets.ProfilePacket.Action action) {}
}
