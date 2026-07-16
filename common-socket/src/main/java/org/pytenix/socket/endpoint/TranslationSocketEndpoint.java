package org.pytenix.socket.endpoint;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.omni.entity.TranslationModule;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.TranslationRequestData;
import org.omni.packets.data.TranslationResultData;
import org.omni.transport.EndpointHandler;
import org.omni.transport.TransportSender;
import org.omni.transport.endpoint.TranslationEndpoint;
import org.pytenix.socket.socket.WebSocketService;
import org.transport.TransportService;

import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Singleton
public class TranslationSocketEndpoint implements TranslationEndpoint {


    final TransportSender transportSender;



    public final Cache<UUID, List<CompletableFuture<String>>> pendingRequests = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(20))
            .build();
    public final Cache<DeduplicationKey, UUID> deduplicationRequests = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(20))
            .build();

    @Inject
    public TranslationSocketEndpoint(
            TransportSender transportSender
    ) {
        this.transportSender = transportSender;
    }

    @Override
    public void handleIncoming(TranslationResultData inbound) {
        final UUID id = inbound.requestId();
        final String result = inbound.result();

        List<CompletableFuture<String>> futures = pendingRequests.getIfPresent(id);
        if (futures != null) {
            for (CompletableFuture<String> future : futures) {
                future.complete(result);
            }
            pendingRequests.invalidate(id);
        }
    }

    @Override
    public CompletableFuture<String> sendRequest(TranslationRequestData outbound) {

        final UUID id = outbound.requestId();
        final String text = outbound.text();
        final String targetLang = outbound.targetLanguage();
        final TranslationModule translationModule = outbound.module();

        if (text == null || text.isEmpty()) return CompletableFuture.completedFuture("");

        DeduplicationKey key = new DeduplicationKey(text, targetLang, translationModule);
        CompletableFuture<String> future = new CompletableFuture<>();
        future.orTimeout(15, TimeUnit.SECONDS).exceptionally(ex -> text);

        UUID masterId = deduplicationRequests.get(key, k -> id);

        if (pendingRequests.getIfPresent(masterId) == null) {
            deduplicationRequests.invalidate(key);
            masterId = id;
            deduplicationRequests.put(key, masterId);
        }

        pendingRequests.get(masterId, k -> new CopyOnWriteArrayList<>()).add(future);

        if (masterId.equals(id)) {
            transportSender.sendPacket(PacketRegistry.TRANSLATION_REQUEST,
                    new TranslationRequestData(
                            masterId,
                            text,
                            targetLang,
                            translationModule
                    ));
        }

        return future;
    }
    public record DeduplicationKey(String text, String lang, TranslationModule translationModule) {
    }
}