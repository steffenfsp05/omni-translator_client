package org.pytenix.socket.endpoint;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.entity.TranslationModule;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.TranslationRequestData;
import org.omni.packets.data.TranslationResultData;
import org.omni.transport.TransportSender;
import org.omni.transport.endpoint.TranslationEndpoint;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
public class TranslationSocketEndpoint extends AbstractDeduplicatingEndpoint<TranslationEndpoint.DeduplicationKey, UUID, String> implements TranslationEndpoint {

    private final TransportSender transportSender;

    private final Cache<DeduplicationKey, String> translationCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(3000)
            .build();

    @Inject
    public TranslationSocketEndpoint(TransportSender transportSender) {
        this.transportSender = transportSender;
    }

    @Override
    public void handleIncoming(TranslationResultData inbound) {
        final UUID id = inbound.requestId();
        final String result = inbound.result();

        resolveIncomingByRequestId(id, result);
    }

    @Override
    public CompletableFuture<String> sendRequest(TranslationRequestData outbound) {
        final UUID id = outbound.requestId();
        final String text = outbound.text();
        final String targetLang = outbound.targetLanguage();
        final TranslationModule translationModule = outbound.module();

        if (text == null || text.isEmpty()) return CompletableFuture.completedFuture("");

        DeduplicationKey key = new DeduplicationKey(text, targetLang, translationModule);

        return executeDeduplicated(
                key,
                id,
                15,
                () -> {
                    System.out.println("SENDING " + text);
                    transportSender.sendPacket(PacketRegistry.TRANSLATION_REQUEST,
                            new TranslationRequestData(id, text, targetLang, translationModule));
                },
                text
        );
    }

    protected String getFromCache(DeduplicationKey key) {
        return translationCache.getIfPresent(key);
    }

    protected void saveToCache(DeduplicationKey key, String value) {
        if (value == null) return;
        if (value.equalsIgnoreCase("ERROR") || value.equalsIgnoreCase("TIMEOUT") || value.isEmpty()) {
            return;
        }
        translationCache.put(key, value);
    }

    @Override
    public void set(DeduplicationKey key, String value) {
        translationCache.put(key, value);
    }

    @Override
    public String get(DeduplicationKey key) {
        return translationCache.getIfPresent(key);
    }

    @Override
    public void invalidate(DeduplicationKey key) {
        translationCache.invalidate(key);
    }

    @Override
    public boolean exists(DeduplicationKey key) {
        return translationCache.asMap().containsKey(key);
    }

    @Override
    public void clear() {
        translationCache.invalidateAll();
        System.out.println("CLEARED ALL : " + translationCache.estimatedSize());
    }
}