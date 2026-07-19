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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class ProfileSocketEndpoint implements ProfileEndpoint {

    final AbstractAnalyticsSecret abstractAnalyticsSecret;
    final PacketMapperRegistry packetMapperRegistry;
    final TransportSender transportSender;

    private final ConcurrentHashMap<AnalyticsKey, CompletableFuture<ProfileResultData>> inFlightFetches = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<ProfileResultData>> queue = new ConcurrentHashMap<>();

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
            PacketMapperRegistry packetMapperRegistry,
            TransportSender transportSender
    ) {
        this.packetMapperRegistry = packetMapperRegistry;
        this.abstractAnalyticsSecret = abstractAnalyticsSecret;
        this.transportSender = transportSender;


    }


    @Override
    public void handleIncoming(ProfileResultData inbound) {
        final UUID requestId = inbound.requestId();
        final AnalyticsKey analyticsKey = new AnalyticsKey(inbound.analyticId());
        final UUID playerId = abstractAnalyticsSecret.getUuidFromAnalyticsKey(analyticsKey);

        CompletableFuture<ProfileResultData> future = queue.remove(requestId);

        inFlightFetches.remove(analyticsKey);
        this.set(playerId, inbound);

        if (future != null) {
            future.complete(inbound);
        }
    }

    @Override
    public CompletableFuture<ProfileResultData> sendRequest(UUID uuid) {

        AnalyticsKey analyticsKey = abstractAnalyticsSecret.getAnalyticsKey(uuid);


        ProfileResultData cachedProfile = this.get(uuid);
        if (cachedProfile != null) {
            return CompletableFuture.completedFuture(cachedProfile);
        }

        return inFlightFetches.computeIfAbsent(analyticsKey, key -> {
            CompletableFuture<ProfileResultData> future = new CompletableFuture<>();
            UUID requestId = UUID.randomUUID();

            ProfileExternRequestData externProfileRequestData = new ProfileExternRequestData(
                    requestId,
                    analyticsKey.bytes()
            );


            queue.put(requestId, future);

            transportSender.sendPacket(
                    PacketRegistry.PROFILE_REQUEST_EXTERN,
                    externProfileRequestData
            );

            return future.orTimeout(5, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        System.out.println("VALUES: " + uuid);
                        System.out.println("analyticsKey: " + analyticsKey);
                        System.out.println("requestId: " + requestId);
                        System.out.println("externProfileRequestData: " + externProfileRequestData);
                        System.out.println("SALT: " + abstractAnalyticsSecret.getOrCreateSalt());
                        System.err.println("Fehler beim Abrufen des Profils (Asynchron): " + ex.getMessage());

                        inFlightFetches.remove(key);
                        queue.remove(requestId);

                        return new ProfileResultData(
                                requestId,
                                key.bytes(),
                                Protobuf.ConsentType.UNKNOWN
                        );
                    });
        });
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
                new ProfileExternUpdateData(
                        inbound
                )
        );

        transportSender.sendPacket(PacketRegistry.CACHE_INVALIDATION,
                new CacheInvalidationRequest(UUID.randomUUID(), new CacheInvalidationRequest.Profile(analyticsKey.bytes())));
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
