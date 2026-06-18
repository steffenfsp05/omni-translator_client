package org.pytenix.backend;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.pytenix.VelocityTranslator;
import org.pytenix.proto.generated.NetworkPackets;
import org.pytenix.util.UuidUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class GeoService {

    private static final int MAX_BATCH_SIZE = 50;
    private static final long MAX_WAIT_TIME_MS = 10;
    private final ProxyServer proxyServer;
    private final VelocityTranslator velocityTranslator;
    private final OmniConnectionService connectionManager;
    private final ConcurrentHashMap<UUID, CompletableFuture<String>> queue = new ConcurrentHashMap<>();
    private final List<QueuedRequest> pendingRequests = new ArrayList<>();
    private ScheduledTask flushTask = null;
    public GeoService(VelocityTranslator velocityTranslator, ProxyServer proxyServer, OmniConnectionService connectionManager) {
        this.velocityTranslator = velocityTranslator;
        this.proxyServer = proxyServer;
        this.connectionManager = connectionManager;
    }

    public void handleGeoResult(NetworkPackets.GeoBatchResult batch) {
        for (NetworkPackets.GeoResultPacket geoResult : batch.getResultsList()) {
            UUID id = UuidUtil.fromByteString(geoResult.getRequestId());
            CompletableFuture<String> future = queue.remove(id);
            if (future != null) future.complete(geoResult.getLanguage());
        }
    }

    public CompletableFuture<String> sendGeoRequest(UUID id, String ipAddress) {
        CompletableFuture<String> future = new CompletableFuture<>();
        if (ipAddress == null || ipAddress.isBlank()) return CompletableFuture.completedFuture("en_en");

        NetworkPackets.GeoRequestPacket request = NetworkPackets.GeoRequestPacket.newBuilder()
                .setRequestId(UuidUtil.toByteString(id))
                .setIpAddress(ipAddress)
                .build();

        synchronized (pendingRequests) {
            pendingRequests.add(new QueuedRequest(request, future, id));

            if (pendingRequests.size() >= MAX_BATCH_SIZE) {
                flushBatch();
            } else if (flushTask == null) {
                flushTask = proxyServer.getScheduler().buildTask(velocityTranslator, this::flushBatch)
                        .delay(MAX_WAIT_TIME_MS, TimeUnit.MILLISECONDS)
                        .schedule();
            }
        }

        return future.orTimeout(60, TimeUnit.SECONDS).exceptionally(ex -> {
            queue.remove(id);
            return "TIMEOUT";
        });
    }

    private void flushBatch() {
        List<QueuedRequest> batchToProcess;
        synchronized (pendingRequests) {
            if (pendingRequests.isEmpty()) return;
            batchToProcess = new ArrayList<>(pendingRequests);
            pendingRequests.clear();
            if (flushTask != null) {
                flushTask.cancel();
                flushTask = null;
            }
        }
        sendAsProtobufBatch(batchToProcess);
    }

    private void sendAsProtobufBatch(List<QueuedRequest> batch) {
        NetworkPackets.GeoBatchRequest.Builder batchBuilder = NetworkPackets.GeoBatchRequest.newBuilder();
        for (QueuedRequest qr : batch) {
            batchBuilder.addRequests(qr.request());
            queue.put(qr.originalId(), qr.future());
        }

        NetworkPackets.PacketWrapper packetWrapper = NetworkPackets.PacketWrapper.newBuilder()
                .setGeoBatchRequest(batchBuilder.build())
                .build();

        connectionManager.sendPacket(packetWrapper).exceptionally(ex -> {
            System.err.println("GEO BATCH SEND FAILED: " + ex.getMessage());
            for (QueuedRequest qr : batch) {
                qr.future().completeExceptionally(ex);
                queue.remove(qr.originalId());
            }
            return null;
        });
    }

    private record QueuedRequest(NetworkPackets.GeoRequestPacket request, CompletableFuture<String> future,
                                 UUID originalId) {
    }
}