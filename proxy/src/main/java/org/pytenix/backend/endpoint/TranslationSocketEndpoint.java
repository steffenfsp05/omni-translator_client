package org.pytenix.backend.endpoint;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.omni.entity.ServerConfiguration;
import org.omni.entity.TranslationModule;
import org.omni.event.EventService;
import org.omni.event.register.ConfigUpdateEvent;
import org.omni.packets.PacketMapperRegistry;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.TranslationRequestData;
import org.omni.packets.data.TranslationResultData;
import org.omni.translation.TranslatorService;
import org.pytenix.backend.OmniConnectionService;
import org.pytenix.network.ProxyTransport;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class TranslationSocketEndpoint {

    private final Provider<OmniConnectionService> connectionManagerProvider;

    private final ConcurrentHashMap<UUID, CompletableFuture<String>> queue = new ConcurrentHashMap<>();

    @Inject
    public TranslationSocketEndpoint(
            Provider<OmniConnectionService> connectionManagerProvider
    ) {
        this.connectionManagerProvider = connectionManagerProvider;;
    }



    public void handleTranslationResult(TranslationResultData resultData) {
        UUID id = resultData.requestId();
        CompletableFuture<String> future = queue.remove(id);
        if (future != null) future.complete(resultData.result());
    }

    public CompletableFuture<String> sendTranslationRequest(UUID id, String text, String lang, TranslationModule translationModule) {
        CompletableFuture<String> future = new CompletableFuture<>();
        if (text == null || text.isBlank()) return CompletableFuture.completedFuture(text);

        queue.put(id, future);

        connectionManagerProvider.get().sendPacket(PacketRegistry.TRANSLATION_REQUEST,
                new TranslationRequestData(id, text, lang, translationModule)
        );

        return future.orTimeout(60, TimeUnit.SECONDS).exceptionally(ex -> {
            queue.remove(id);
            return "TIMEOUT";
        });
    }
}