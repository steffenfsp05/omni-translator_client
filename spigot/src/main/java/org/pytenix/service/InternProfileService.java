package org.pytenix.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.protobuf.MessageLite;
import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.packets.impl.ExternProfileRequestMapper;
import org.pytenix.packets.impl.ExternProfileUpdateMapper;
import org.pytenix.packets.impl.InternProfileRequestMapper;
import org.pytenix.packets.impl.ProfileMapper;
import org.pytenix.profile.AbstractAnalyticsSecret;
import org.pytenix.profile.AnalyticsKey;
import org.pytenix.profile.ProfileService;
import org.pytenix.proto.generated.NetworkPackets;
import org.transport.service.impl.PacketDefinition;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class InternProfileService extends ProfileService {

    private final Supplier<String> licenseKey;

    private final ConcurrentHashMap<UUID, CompletableFuture<ProfileMapper.ProfileData>> inFlightFetches = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<ProfileMapper.ProfileData>> queue = new ConcurrentHashMap<>();


    //REQUESTID;PLAYERID TODO: MEMORY LEAK??
    private final ConcurrentHashMap<UUID,UUID> requestIdPlayerIdMap = new ConcurrentHashMap<>();

    private final Cache<UUID, ProfileMapper.ProfileData> cacheProvider = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(3000)
            .build();
    final BiConsumer<PacketDefinition<? extends MessageLite>, ? extends MessageLite> connectionEndpoint;

    public InternProfileService(
            BiConsumer<PacketDefinition<? extends MessageLite>, ? extends MessageLite> connectionEndpoint,
            Supplier<String> licenseKey
    ) {

        this.connectionEndpoint = connectionEndpoint;
        this.licenseKey = licenseKey;

    }



    @Override
    public CompletableFuture<ProfileMapper.ProfileData> retrieveProfile(UUID uuid) {



        ProfileMapper.ProfileData cachedProfile = cacheProvider.getIfPresent(uuid);
        if (cachedProfile != null) {
            return CompletableFuture.completedFuture(cachedProfile);
        }

        return inFlightFetches.computeIfAbsent(uuid, key -> {
            CompletableFuture<ProfileMapper.ProfileData> future = new CompletableFuture<>();
            UUID requestId = UUID.randomUUID();

            InternProfileRequestMapper.InternProfileData internProfileData = new InternProfileRequestMapper.InternProfileData(
                    licenseKey.get(),
                    uuid,
                    requestId
            );



            queue.put(requestId, future);
            requestIdPlayerIdMap.put(requestId, uuid);

            connectionEndpoint.accept(
                    PacketRegistry.PROFILE_REQUEST_INTERN,
                    PacketMapperRegistry.toProto(internProfileData)
            );


            return future.orTimeout(5, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        System.err.println("Fehler beim Abrufen des Profils (Asynchron): " + ex.getMessage());

                        inFlightFetches.remove(key);
                        queue.remove(requestId);

                        return new ProfileMapper.ProfileData(
                                "NULL",
                                null,
                                requestId,
                                NetworkPackets.ProfilePacket.ConsentType.UNKNOWN
                        );
                    });
        });
    }

    @Override
    public Cache<UUID, ProfileMapper.ProfileData> cacheProvider() {
        return cacheProvider;
    }


    @Override
    public void updateProfile(ProfileMapper.ProfileData profileData) {

        for (int i = 0; i < 50; i++) {
            System.out.println("ERROR --- TRIED TO UPDATE PROFILEDATA WITHIN INTERN!!!!");
        }
        System.out.println(" PROFILE: " + profileData);

/*
        final AnalyticsKey analyticsKey = new AnalyticsKey(profileData.analyticId());


        if (deduplicationCache.asMap().putIfAbsent(analyticsKey, Boolean.TRUE) != null) {
            return;
        }

        cacheProvider.put(analyticsKey, profileData);

        connectionEndpoint.accept(PacketRegistry.PROFILE_UPDATE_EXTERN,
                PacketMapperRegistry.toProto(
                        new ExternProfileUpdateMapper.ExternProfileUpdateData(
                                profileData
                        )
                ));

 */
    }


    @Override
    public void handleProfileResult(ProfileMapper.ProfileData resultData) {
        final UUID requestId = resultData.requestId();
        final UUID playerId = requestIdPlayerIdMap.remove(requestId);

        CompletableFuture<ProfileMapper.ProfileData> future = queue.remove(requestId);

        inFlightFetches.remove(playerId);
        cacheProvider.put(playerId, resultData);

        if (future != null) {
            future.complete(resultData);
        }
    }

}
