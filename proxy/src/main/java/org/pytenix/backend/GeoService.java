package org.pytenix.backend;

import com.velocitypowered.api.proxy.ProxyServer;
import org.pytenix.TranslatorPlugin;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.proto.generated.NetworkPackets;
import org.pytenix.util.UuidUtil;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class GeoService {

    private final OmniConnectionService connectionManager;
    private final ConcurrentHashMap<UUID, CompletableFuture<String>> queue = new ConcurrentHashMap<>();

    public GeoService(OmniConnectionService connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void handleGeoResult(NetworkPackets.GeoResultPacket geoResult) {
        UUID id = UuidUtil.fromByteString(geoResult.getRequestId());
        CompletableFuture<String> future = queue.remove(id);
        if (future != null) future.complete(geoResult.getLanguage());
    }

    public CompletableFuture<String> sendGeoRequest(UUID id, String ipAddress) {
        CompletableFuture<String> future = new CompletableFuture<>();
        if (ipAddress == null || ipAddress.isBlank()) return CompletableFuture.completedFuture("en_en");

        NetworkPackets.GeoRequestPacket request = NetworkPackets.GeoRequestPacket.newBuilder()
                .setRequestId(UuidUtil.toByteString(id))
                .setIpAddress(ipAddress)
                .build();

        queue.put(id, future);

        connectionManager.sendPacket(PacketRegistry.GEO_REQUEST, request);

        return future.orTimeout(60, TimeUnit.SECONDS).exceptionally(ex -> {
            queue.remove(id);
            return "TIMEOUT";
        });
    }
}