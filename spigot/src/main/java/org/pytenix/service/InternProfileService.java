package org.pytenix.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.config.ConfigurationFile;
import org.omni.packets.PacketMapperRegistry;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.ProfileInternRequestData;
import org.omni.packets.data.ProfileResultData;
import org.omni.profile.ProfileService;
import org.omni.proto.generated.Protobuf;
import org.pytenix.TranslatorPlugin;
import org.pytenix.network.SpigotTransport;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class InternProfileService extends ProfileService {


    final TranslatorPlugin translatorPlugin;
    final ConfigurationFile configurationFile;
    final PacketMapperRegistry packetMapperRegistry;
    final SpigotTransport spigotTransport;
    private final ConcurrentHashMap<UUID, CompletableFuture<ProfileResultData>> inFlightFetches = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<ProfileResultData>> queue = new ConcurrentHashMap<>();
    //REQUESTID;PLAYERID TODO: MEMORY LEAK??
    private final ConcurrentHashMap<UUID, UUID> requestIdPlayerIdMap = new ConcurrentHashMap<>();
    private final Cache<UUID, ProfileResultData> cacheProvider = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(3000)
            .build();

    @Inject
    public InternProfileService(
            TranslatorPlugin translatorPlugin,
            ConfigurationFile configurationFile,
            PacketMapperRegistry packetMapperRegistry,
            SpigotTransport spigotTransport
    ) {
        this.packetMapperRegistry = packetMapperRegistry;
        this.translatorPlugin = translatorPlugin;
        this.configurationFile = configurationFile;
        this.spigotTransport = spigotTransport;


    }


    @Override
    public CompletableFuture<ProfileResultData> retrieveProfile(UUID uuid) {


        ProfileResultData cachedProfile = cacheProvider.getIfPresent(uuid);
        if (cachedProfile != null) {
            return CompletableFuture.completedFuture(cachedProfile);
        }

        return inFlightFetches.computeIfAbsent(uuid, key -> {
            CompletableFuture<ProfileResultData> future = new CompletableFuture<>();
            UUID requestId = UUID.randomUUID();

            ProfileInternRequestData internProfileData = new ProfileInternRequestData(
                    uuid,
                    requestId
            );


            queue.put(requestId, future);
            requestIdPlayerIdMap.put(requestId, uuid);

            spigotTransport.getTransportService().send(
                    spigotTransport.pluginMessagingChannel,
                    PacketRegistry.PROFILE_REQUEST_INTERN,
                    packetMapperRegistry.toProto(internProfileData)
            );


            return future.orTimeout(5, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        System.err.println("Fehler beim Abrufen des Profils (Asynchron): " + ex.getMessage());

                        inFlightFetches.remove(key);
                        queue.remove(requestId);

                        return new ProfileResultData(
                                "NULL",
                                null,
                                requestId,
                                Protobuf.ConsentType.UNKNOWN
                        );
                    });
        });
    }

    @Override
    public Cache<UUID, ProfileResultData> cacheProvider() {
        return cacheProvider;
    }


    @Override
    public void updateProfile(ProfileResultData profileData) {

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
    public void handleProfileResult(ProfileResultData resultData) {
        final UUID requestId = resultData.requestId();
        final UUID playerId = requestIdPlayerIdMap.remove(requestId);

        CompletableFuture<ProfileResultData> future = queue.remove(requestId);

        inFlightFetches.remove(playerId);
        cacheProvider.put(playerId, resultData);

        if (future != null) {
            future.complete(resultData);
        }
    }

}
