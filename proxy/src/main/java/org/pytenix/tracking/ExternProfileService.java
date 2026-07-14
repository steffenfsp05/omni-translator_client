package org.pytenix.tracking;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.omni.config.ConfigurationFile;
import org.omni.packets.PacketMapperRegistry;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.ProfileExternRequestData;
import org.omni.packets.data.ProfileExternUpdateData;
import org.omni.packets.data.ProfileResultData;
import org.omni.profile.AbstractAnalyticsSecret;
import org.omni.profile.AnalyticsKey;
import org.omni.profile.ProfileService;
import org.omni.proto.generated.Protobuf;
import org.pytenix.TranslatorPlugin;
import org.pytenix.backend.OmniConnectionService;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class ExternProfileService extends ProfileService {

    final TranslatorPlugin translatorPlugin;
    final AbstractAnalyticsSecret abstractAnalyticsSecret;
    final PacketMapperRegistry packetMapperRegistry;
    final Provider<OmniConnectionService> omniConnectionService;

    private final ConfigurationFile configurationFile;
    private final ConcurrentHashMap<AnalyticsKey, CompletableFuture<ProfileResultData>> inFlightFetches = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<ProfileResultData>> queue = new ConcurrentHashMap<>();
    private final Cache<UUID, ProfileResultData> cacheProvider = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(3000)
            .build();
    private final Cache<AnalyticsKey, Boolean> deduplicationCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMillis(500))
            .build();

    @Inject
    public ExternProfileService(
            TranslatorPlugin translatorPlugin,
            AbstractAnalyticsSecret abstractAnalyticsSecret,
            ConfigurationFile configurationFile,
            PacketMapperRegistry packetMapperRegistry,
            Provider<OmniConnectionService> omniConnectionService
    ) {
        this.packetMapperRegistry = packetMapperRegistry;
        this.translatorPlugin = translatorPlugin;
        this.configurationFile = configurationFile;
        this.abstractAnalyticsSecret = abstractAnalyticsSecret;
        this.omniConnectionService = omniConnectionService;


    }



    @Override
    public Cache<UUID, ProfileResultData> cacheProvider() {
        return cacheProvider;
    }


    @Override
    public CompletableFuture<ProfileResultData> retrieveProfile(UUID uuid) {

        AnalyticsKey analyticsKey = abstractAnalyticsSecret.getAnalyticsKey(uuid);


        ProfileResultData cachedProfile = cacheProvider.getIfPresent(uuid);
        if (cachedProfile != null) {
            return CompletableFuture.completedFuture(cachedProfile);
        }

        return inFlightFetches.computeIfAbsent(analyticsKey, key -> {
            CompletableFuture<ProfileResultData> future = new CompletableFuture<>();
            UUID requestId = UUID.randomUUID();

            ProfileExternRequestData externProfileRequestData = new ProfileExternRequestData(
                    configurationFile.getLicenseKey(),
                    analyticsKey.bytes(),
                    requestId
            );


            queue.put(requestId, future);

            omniConnectionService.get().sendPacket(
                    PacketRegistry.PROFILE_REQUEST_EXTERN,
                    externProfileRequestData
            );

            return future.orTimeout(5, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        System.err.println("Fehler beim Abrufen des Profils (Asynchron): " + ex.getMessage());

                        inFlightFetches.remove(key);
                        queue.remove(requestId);

                        return new ProfileResultData(
                                "NULL",
                                key.bytes(),
                                requestId,
                                Protobuf.ConsentType.UNKNOWN
                        );
                    });
        });
    }

    @Override
    public void updateProfile(ProfileResultData profileData) {


        final AnalyticsKey analyticsKey = new AnalyticsKey(profileData.analyticId());

        final UUID playerId = abstractAnalyticsSecret.getUuidFromAnalyticsKey(analyticsKey);

        if (deduplicationCache.asMap().putIfAbsent(analyticsKey, Boolean.TRUE) != null) {
            return;
        }

        cacheProvider.put(playerId, profileData);

        omniConnectionService.get().sendPacket(PacketRegistry.PROFILE_UPDATE_EXTERN,
                        new ProfileExternUpdateData(
                                profileData
                        )
                );
    }

    @Override
    public void handleProfileResult(ProfileResultData resultData) {
        final UUID requestId = resultData.requestId();
        final AnalyticsKey analyticsKey = new AnalyticsKey(resultData.analyticId());
        final UUID playerId = abstractAnalyticsSecret.getUuidFromAnalyticsKey(analyticsKey);

        CompletableFuture<ProfileResultData> future = queue.remove(requestId);

        inFlightFetches.remove(analyticsKey);
        cacheProvider.put(playerId, resultData);

        if (future != null) {
            future.complete(resultData);
        }
    }

}
