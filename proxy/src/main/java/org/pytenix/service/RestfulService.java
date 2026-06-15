package org.pytenix.service;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.pytenix.VelocityTranslator;
import org.pytenix.bridge.VelocityBridge;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.proto.generated.NetworkPackets;
import org.pytenix.util.UuidUtil;

import java.util.*;
import java.util.concurrent.*;

public class RestfulService {

    private final VelocityTranslator velocityTranslator;
    private final VelocityBridge velocityBridge;
    private final ProxyServer proxyServer;
    private final OmniConnectionService connectionManager; // Die Leitung zum Server

    private final ConcurrentHashMap<UUID, CompletableFuture<String>> queue = new ConcurrentHashMap<>();
    private record QueuedRequest(NetworkPackets.TranslationRequest request, CompletableFuture<String> future, UUID originalId) {}
    private final List<QueuedRequest> pendingRequests = new ArrayList<>();
    private ScheduledTask flushTask = null;

    private static final int MAX_BATCH_SIZE = 25;
    private static final long MAX_WAIT_TIME_MS = 20;

    public RestfulService(VelocityTranslator velocityTranslator, VelocityBridge velocityBridge, ProxyServer proxyServer, OmniConnectionService connectionManager) {
        this.velocityTranslator = velocityTranslator;
        this.velocityBridge = velocityBridge;
        this.proxyServer = proxyServer;
        this.connectionManager = connectionManager;
    }

    public void handleConfigUpdate(ServerConfiguration config) {
        System.out.println("[OmniTranslator] New Config received!");
        velocityTranslator.getVelocityBridge().setServerConfiguration(config);
        velocityBridge.broadcastConfigUpdate(config);
    }

    public void handleTranslationResult(NetworkPackets.TranslationBatchResult batch) {
        for (NetworkPackets.TranslationResult translationResult : batch.getResultsList()) {
            UUID id = UuidUtil.fromByteString(translationResult.getRequestId());
            CompletableFuture<String> future = queue.remove(id);
            if (future != null) future.complete(translationResult.getResult());
        }
    }

    public CompletableFuture<String> sendTranslationRequest(UUID id, String text, String lang, String module) {
        CompletableFuture<String> future = new CompletableFuture<>();
        if (text == null || text.isBlank()) return CompletableFuture.completedFuture(text);

        NetworkPackets.TranslationRequest request = NetworkPackets.TranslationRequest.newBuilder()
                .setRequestId(UuidUtil.toByteString(id))
                .setText(text)
                .setTargetLang(lang)
                .setModule(module)
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
        NetworkPackets.TranslationBatchRequest.Builder batchBuilder = NetworkPackets.TranslationBatchRequest.newBuilder();
        for (QueuedRequest qr : batch) {
            batchBuilder.addRequests(qr.request());
            queue.put(qr.originalId(), qr.future());
        }

        NetworkPackets.PacketWrapper packetWrapper = NetworkPackets.PacketWrapper.newBuilder()
                .setBatchRequest(batchBuilder.build())
                .build();

        connectionManager.sendPacket(packetWrapper).exceptionally(ex -> {
            System.err.println("BATCH SEND FAILED: " + ex.getMessage());
            for (QueuedRequest qr : batch) {
                qr.future().completeExceptionally(ex);
                queue.remove(qr.originalId());
            }
            return null;
        });
    }
}