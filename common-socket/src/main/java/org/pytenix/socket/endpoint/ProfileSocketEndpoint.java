package org.pytenix.socket.endpoint;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.packets.PacketMapperRegistry;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.CacheInvalidationRequest;
import org.omni.packets.data.ProfileExternRequestData;
import org.omni.packets.data.ProfileExternUpdateData;
import org.omni.packets.data.ProfileResultData;
import org.omni.profile.AbstractAnalyticsSecret;
import org.omni.profile.AnalyticsKey;
import org.omni.proto.generated.Protobuf;
import org.omni.transport.TransportSender;
import org.omni.transport.endpoint.ProfileEndpoint;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ProfileSocketEndpoint extends AbstractDeduplicatingEndpoint<AnalyticsKey, UUID, ProfileResultData> implements ProfileEndpoint {

    private final AbstractAnalyticsSecret abstractAnalyticsSecret;
    private final TransportSender transportSender;

    private final Cache<UUID, ProfileResultData> profileCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(3000)
            .build();

    private final Cache<AnalyticsKey, Boolean> deduplicationCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMillis(500))
            .build();

    @Inject
    public ProfileSocketEndpoint(
            AbstractAnalyticsSecret abstractAnalyticsSecret,
            TransportSender transportSender
    ) {
        this.abstractAnalyticsSecret = abstractAnalyticsSecret;
        this.transportSender = transportSender;
    }

    @Override
    public void handleIncoming(ProfileResultData inbound) {
        final UUID requestId = inbound.requestId();
        final AnalyticsKey analyticsKey = new AnalyticsKey(inbound.analyticId());
        final UUID playerId = abstractAnalyticsSecret.getUuidFromAnalyticsKey(analyticsKey);

        this.set(playerId, inbound);

        resolveIncomingByRequestId(requestId, inbound);
    }

    @Override
    public CompletableFuture<ProfileResultData> sendRequest(UUID uuid) {
        AnalyticsKey analyticsKey = abstractAnalyticsSecret.getAnalyticsKey(uuid);
        UUID requestId = UUID.randomUUID();

        ProfileExternRequestData externProfileRequestData = new ProfileExternRequestData(
                requestId,
                analyticsKey.bytes()
        );

        ProfileResultData fallbackValue = new ProfileResultData(
                requestId,
                analyticsKey.bytes(),
                Protobuf.ConsentType.UNKNOWN,
                Protobuf.ConsentType.UNKNOWN
        );

        return executeDeduplicated(
                analyticsKey,
                requestId,
                5,
                () -> {
                    transportSender.sendPacket(
                            PacketRegistry.PROFILE_REQUEST_EXTERN,
                            externProfileRequestData
                    );
                },
                fallbackValue
        );
    }

    @Override
    public void update(ProfileResultData inbound) {
        final AnalyticsKey analyticsKey = new AnalyticsKey(inbound.analyticId());
        final UUID playerId = abstractAnalyticsSecret.getUuidFromAnalyticsKey(analyticsKey);

        if (deduplicationCache.asMap().putIfAbsent(analyticsKey, Boolean.TRUE) != null) {
            return;
        }

        this.set(playerId, inbound);

        transportSender.sendPacket(PacketRegistry.PROFILE_UPDATE_EXTERN,
                new ProfileExternUpdateData(inbound));

        transportSender.sendPacket(PacketRegistry.CACHE_INVALIDATION,
                new CacheInvalidationRequest(UUID.randomUUID(), new CacheInvalidationRequest.Profile(analyticsKey.bytes())));
    }

    @Override
    protected ProfileResultData getFromCache(AnalyticsKey key) {
        UUID playerId = abstractAnalyticsSecret.getUuidFromAnalyticsKey(key);
        return get(playerId);
    }

    @Override
    protected void saveToCache(AnalyticsKey key, ProfileResultData value) {
        UUID playerId = abstractAnalyticsSecret.getUuidFromAnalyticsKey(key);
        set(playerId, value);
    }

    @Override
    public void set(UUID key, ProfileResultData value) {
        profileCache.put(key, value);
    }

    @Override
    public ProfileResultData get(UUID key) {
        return profileCache.getIfPresent(key);
    }

    @Override
    public void invalidate(UUID key) {
        profileCache.invalidate(key);
    }

    @Override
    public boolean exists(UUID key) {
        return profileCache.asMap().containsKey(key);
    }

    @Override
    public void clear() {
        profileCache.invalidateAll();
    }
}