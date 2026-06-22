package org.pytenix.backend;

import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.packets.impl.GeoRequestMapper;
import org.pytenix.packets.impl.GeoResultMapper;

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

    public void handleGeoResult(GeoResultMapper.ResultData resultData) {

        CompletableFuture<String> future = queue.remove(resultData.requestId());
        if (future != null) future.complete(resultData.language());
    }

    public CompletableFuture<String> sendGeoRequest(UUID id, String ipAddress) {
        CompletableFuture<String> future = new CompletableFuture<>();
        if (ipAddress == null || ipAddress.isBlank()) return CompletableFuture.completedFuture("en_en");

        queue.put(id, future);

        connectionManager.sendPacket(PacketRegistry.GEO_REQUEST, PacketMapperRegistry.toProto(
                new GeoRequestMapper.RequestData(
                        id,
                        ipAddress
                )
        ));

        return future.orTimeout(60, TimeUnit.SECONDS).exceptionally(ex -> {
            queue.remove(id);
            return "TIMEOUT";
        });
    }
}