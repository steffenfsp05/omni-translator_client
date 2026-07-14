package org.pytenix.network.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.omni.entity.TranslationModule;
import org.omni.packets.PacketMapperRegistry;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.TranslationRequestData;
import org.transport.TransportService;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Singleton
public class TranslationRequestService {


    public final Cache<UUID, List<CompletableFuture<String>>> pendingRequests = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(20))
            .build();
    public final Cache<DeduplicationKey, UUID> deduplicationRequests = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(20))
            .build();

    private final String channel;
    private final Provider<TransportService<String>> transportServiceProvider;
    private final PacketMapperRegistry packetMapperRegistry;

    @Inject
    public TranslationRequestService(
            @Named("pluginMessagingChannel") String channel,
            Provider<TransportService<String>> transportServiceProvider,
            PacketMapperRegistry packetMapperRegistry
    ) {
        this.channel = channel;
        this.transportServiceProvider = transportServiceProvider;
        this.packetMapperRegistry = packetMapperRegistry;
    }

    public CompletableFuture<String> translate(UUID id, String text, String targetLang, TranslationModule translationModule) {
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
            transportServiceProvider.get().send(channel, PacketRegistry.TRANSLATION_REQUEST,
                    packetMapperRegistry.toProto(new TranslationRequestData(
                            masterId,
                            text,
                            targetLang,
                            translationModule
                    )));
        }

        return future;
    }

    public void completeRequest(UUID id, String result) {
        List<CompletableFuture<String>> futures = pendingRequests.getIfPresent(id);
        if (futures != null) {
            for (CompletableFuture<String> future : futures) {
                future.complete(result);
            }
            pendingRequests.invalidate(id);
        }
    }

    public record DeduplicationKey(String text, String lang, TranslationModule translationModule) {
    }
}