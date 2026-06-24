package org.pytenix.network;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.packets.impl.TranslationRequestMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class TranslationRequestService {
    public final com.github.benmanes.caffeine.cache.Cache<UUID, List<CompletableFuture<String>>> pendingRequests = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(20))
            .build();
    public final Cache<DeduplicationKey, UUID> deduplicationRequests = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(20))
            .build();
    private final SpigotTransport transport;
    private final String channel;

    public TranslationRequestService(SpigotTransport transport, String channel) {
        this.transport = transport;
        this.channel = channel;
    }

    public CompletableFuture<String> translate(UUID id, String text, String targetLang, String module) {
        if (text == null || text.isEmpty()) return CompletableFuture.completedFuture("");

        DeduplicationKey key = new DeduplicationKey(text, targetLang, module);
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


            transport.getTransportService().send(channel, PacketRegistry.TRANSLATION_REQUEST,
                    PacketMapperRegistry.toProto(new TranslationRequestMapper.RequestData(
                            masterId,
                            text,
                            targetLang,
                            ServerConfiguration.Module.getModule(module)
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

    public record DeduplicationKey(String text, String lang, String module) {
    }
}
