package org.pytenix.tracking;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.pytenix.TranslatorPlugin;
import org.pytenix.backend.OmniConnectionService;
import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.packets.impl.ExternProfileRequestMapper;
import org.pytenix.packets.impl.ExternProfileUpdateMapper;
import org.pytenix.packets.impl.ProfileMapper;
import org.pytenix.profile.AbstractAnalyticsSecret;
import org.pytenix.profile.AnalyticsKey;
import org.pytenix.profile.ProfileService;
import org.pytenix.proto.generated.NetworkPackets;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ExternProfileService extends ProfileService {

    private final Supplier<String> licenseKey;

    private final ConcurrentHashMap<AnalyticsKey, CompletableFuture<ProfileMapper.ProfileData>> inFlightFetches = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<ProfileMapper.ProfileData>> queue = new ConcurrentHashMap<>();

    private final Cache<UUID, ProfileMapper.ProfileData> cacheProvider = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(3000)
            .build();

    private final Cache<AnalyticsKey, Boolean> deduplicationCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMillis(500))
            .build();

    final TranslatorPlugin translatorPlugin;
    final AbstractAnalyticsSecret abstractAnalyticsSecret;

    public ExternProfileService(
            TranslatorPlugin translatorPlugin,
            AbstractAnalyticsSecret abstractAnalyticsSecret,
            Supplier<String> licenseKey
    ) {
        this.translatorPlugin = translatorPlugin;
        this.licenseKey = licenseKey;
        this.abstractAnalyticsSecret = abstractAnalyticsSecret;

    }


    public OmniConnectionService getConnectionService() {
        return translatorPlugin.getConnectionService();
    }

    @Override
    public Cache<UUID, ProfileMapper.ProfileData> cacheProvider() {
        return cacheProvider;
    }




    @Override
    public CompletableFuture<ProfileMapper.ProfileData> retrieveProfile(UUID uuid) {

        AnalyticsKey analyticsKey = abstractAnalyticsSecret.getAnalyticsKey(uuid);


        ProfileMapper.ProfileData cachedProfile = cacheProvider.getIfPresent(uuid);
        if (cachedProfile != null) {
            return CompletableFuture.completedFuture(cachedProfile);
        }

        return inFlightFetches.computeIfAbsent(analyticsKey, key -> {
            CompletableFuture<ProfileMapper.ProfileData> future = new CompletableFuture<>();
            UUID requestId = UUID.randomUUID();

            ExternProfileRequestMapper.ExternProfileRequestData externProfileRequestData = new ExternProfileRequestMapper.ExternProfileRequestData(
                    licenseKey.get(),
                    analyticsKey.bytes(),
                    requestId
            );



            queue.put(requestId, future);

            getConnectionService().sendPacket(
                    PacketRegistry.PROFILE_REQUEST_EXTERN,
                    PacketMapperRegistry.toProto(externProfileRequestData)
            );

            return future.orTimeout(5, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        System.err.println("Fehler beim Abrufen des Profils (Asynchron): " + ex.getMessage());

                        inFlightFetches.remove(key);
                        queue.remove(requestId);

                        return new ProfileMapper.ProfileData(
                                "NULL",
                                key.bytes(),
                                requestId,
                                NetworkPackets.ProfilePacket.ConsentType.UNKNOWN
                        );
                    });
        });
    }

    @Override
    public void updateProfile(ProfileMapper.ProfileData profileData) {


        final AnalyticsKey analyticsKey = new AnalyticsKey(profileData.analyticId());

        final UUID playerId = abstractAnalyticsSecret.getUuidFromAnalyticsKey(analyticsKey);

        if (deduplicationCache.asMap().putIfAbsent(analyticsKey, Boolean.TRUE) != null) {
            return;
        }

        cacheProvider.put(playerId, profileData);

        getConnectionService().sendPacket(PacketRegistry.PROFILE_UPDATE_EXTERN,
                PacketMapperRegistry.toProto(
                        new ExternProfileUpdateMapper.ExternProfileUpdateData(
                                profileData
                        )
                ));
    }

    @Override
    public void handleProfileResult(ProfileMapper.ProfileData resultData) {
        final UUID requestId = resultData.requestId();
        final AnalyticsKey analyticsKey = new AnalyticsKey(resultData.analyticId());
        final UUID playerId = abstractAnalyticsSecret.getUuidFromAnalyticsKey(analyticsKey);

        CompletableFuture<ProfileMapper.ProfileData> future = queue.remove(requestId);

        inFlightFetches.remove(analyticsKey);
        cacheProvider.put(playerId, resultData);

        if (future != null) {
            future.complete(resultData);
        }
    }

}
