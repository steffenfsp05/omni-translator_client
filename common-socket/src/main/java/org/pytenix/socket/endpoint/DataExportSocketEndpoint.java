package org.pytenix.socket.endpoint;

import com.google.inject.Inject;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.*;
import org.omni.profile.AbstractAnalyticsSecret;
import org.omni.profile.AnalyticsKey;
import org.omni.transport.TransportSender;
import org.omni.transport.endpoint.DataExportEndpoint;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DataExportSocketEndpoint implements DataExportEndpoint {

    private final AbstractAnalyticsSecret abstractAnalyticsSecret;
    private final TransportSender transportSender;

    private final ConcurrentHashMap<UUID, CompletableFuture<String>> queue = new ConcurrentHashMap<>();


    @Inject
    public DataExportSocketEndpoint(
            AbstractAnalyticsSecret abstractAnalyticsSecret,
            TransportSender transportSender
    ) {
        this.abstractAnalyticsSecret = abstractAnalyticsSecret;
        this.transportSender = transportSender;
    }
    @Override
    public void handleIncoming(DataExportResultData inbound) {
        CompletableFuture<String> future = queue.remove(inbound.requestId());
        if (future != null) future.complete(inbound.dataLink());
    }

    @Override
    public CompletableFuture<String> sendRequest(UUID outbound) {
        CompletableFuture<String> future = new CompletableFuture<>();

        AnalyticsKey analyticsKey = abstractAnalyticsSecret.getAnalyticsKey(outbound);
        UUID requestId = UUID.randomUUID();

        DataExportRequestData dataExportRequestData = new DataExportRequestData(
                requestId,
                analyticsKey.bytes()
        );

        queue.put(requestId, future);

        transportSender.sendPacket(PacketRegistry.DATA_EXPORT_REQUEST, dataExportRequestData);

        return future.orTimeout(60, TimeUnit.SECONDS).exceptionally(ex -> {
            queue.remove(requestId);
            return "TIMEOUT";
        });
    }
}
