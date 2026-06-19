package org.pytenix.backend;

import com.velocitypowered.api.scheduler.ScheduledTask;
import org.pytenix.TranslatorPlugin;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.event.EventService;
import org.pytenix.event.register.ConfigUpdateEvent;
import org.pytenix.network.ProxyTransport;
import org.pytenix.proto.generated.NetworkPackets;
import org.pytenix.translation.TranslatorService;
import org.pytenix.util.UuidUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RestfulService {

    private static final int MAX_BATCH_SIZE = 25;
    private static final long MAX_WAIT_TIME_MS = 20;
    private final TranslatorPlugin translatorPlugin;
    private final OmniConnectionService connectionManager;
    private final TranslatorService translatorService;
    private final ProxyTransport proxyTransport;
    private final EventService eventService;
    private final ConcurrentHashMap<UUID, CompletableFuture<String>> queue = new ConcurrentHashMap<>();
    private final List<QueuedRequest> pendingRequests = new ArrayList<>();
    private ScheduledTask flushTask = null;


    public RestfulService(TranslatorPlugin translatorPlugin, OmniConnectionService connectionManager) {
        this.translatorPlugin = translatorPlugin;
        this.connectionManager = connectionManager;

        this.translatorService = translatorPlugin.getTranslatorService();
        this.proxyTransport = translatorPlugin.getProxyTransport();
        this.eventService = translatorPlugin.getTranslatorService().getEventService();
    }

    public void handleConfigUpdate(ServerConfiguration config) {
        System.out.println("[OmniTranslator] New Config received!");

        translatorService.setTranslationConfiguration(config);
        eventService.callEvent(new ConfigUpdateEvent(config));

        proxyTransport.broadcastConfigurationUpdate(translatorService.convertConfigToProtobuf(config));
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
                flushTask = translatorPlugin.getProxyServer().getScheduler().buildTask(translatorPlugin, this::flushBatch)
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

    private record QueuedRequest(NetworkPackets.TranslationRequest request, CompletableFuture<String> future,
                                 UUID originalId) {
    }
}