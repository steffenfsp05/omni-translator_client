package org.pytenix.backend.endpoint;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.GeoRequestData;
import org.omni.packets.data.GeoResultData;
import org.pytenix.backend.OmniConnectionService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class GeoSocketEndpoint {

    private final Provider<OmniConnectionService> connectionManagerProvider;

    private final ConcurrentHashMap<UUID, CompletableFuture<String>> queue = new ConcurrentHashMap<>();

    @Inject
    public GeoSocketEndpoint(Provider<OmniConnectionService> connectionManagerProvider) {
        this.connectionManagerProvider = connectionManagerProvider;
    }

    public void handleGeoResult(GeoResultData resultData) {
        CompletableFuture<String> future = queue.remove(resultData.requestId());
        if (future != null) future.complete(resultData.language());
    }

    public CompletableFuture<String> sendGeoRequest(UUID id, String ipAddress) {
        CompletableFuture<String> future = new CompletableFuture<>();
        if (ipAddress == null || ipAddress.isBlank()) return CompletableFuture.completedFuture("en_en");

        queue.put(id, future);

        connectionManagerProvider.get().sendPacket(PacketRegistry.GEO_REQUEST, new GeoRequestData(id, ipAddress));

        return future.orTimeout(60, TimeUnit.SECONDS).exceptionally(ex -> {
            queue.remove(id);
            return "TIMEOUT";
        });
    }
}