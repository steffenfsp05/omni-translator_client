package org.pytenix.socket.endpoint;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.GeoRequestData;
import org.omni.packets.data.GeoResultData;
import org.omni.transport.EndpointHandler;
import org.omni.transport.TransportSender;
import org.omni.transport.endpoint.GeoEndpoint;
import org.pytenix.socket.socket.WebSocketService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class GeoSocketEndpoint implements GeoEndpoint {

    final TransportSender transportSender;

    private final ConcurrentHashMap<UUID, CompletableFuture<String>> queue = new ConcurrentHashMap<>();

    @Inject
    public GeoSocketEndpoint(TransportSender transportSender) {
        this.transportSender = transportSender;
    }

    @Override
    public void handleIncoming(GeoResultData inbound) {
        CompletableFuture<String> future = queue.remove(inbound.requestId());
        if (future != null) future.complete(inbound.language());
    }

    @Override
    public CompletableFuture<String> sendRequest(GeoRequestData outbound) {
        CompletableFuture<String> future = new CompletableFuture<>();

        final UUID requestId = outbound.requestId();
        final String ipAddress = outbound.ipAddress();

        if (ipAddress == null || ipAddress.isBlank()) return CompletableFuture.completedFuture("en_en");

        queue.put(requestId, future);

        transportSender.sendPacket(PacketRegistry.GEO_REQUEST, outbound);

        return future.orTimeout(60, TimeUnit.SECONDS).exceptionally(ex -> {
            queue.remove(requestId);
            return "TIMEOUT";
        });
    }
}