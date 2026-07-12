package org.pytenix.backend;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.omni.entity.ServerConfiguration;
import org.omni.event.EventService;
import org.omni.event.register.ConfigUpdateEvent;
import org.omni.packets.PacketMapperRegistry;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.TranslationRequestData;
import org.omni.packets.data.TranslationResultData;
import org.omni.translation.TranslatorService;
import org.pytenix.network.ProxyTransport;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class TranslationSocketEndpoint {

    private final Provider<OmniConnectionService> connectionManagerProvider;
    private final TranslatorService translatorService;
    private final EventService eventService;
    private final Provider<ProxyTransport> proxyTransportProvider;
    private final PacketMapperRegistry packetMapperRegistry;

    private final ConcurrentHashMap<UUID, CompletableFuture<String>> queue = new ConcurrentHashMap<>();

    @Inject
    public TranslationSocketEndpoint(
            Provider<OmniConnectionService> connectionManagerProvider,
            TranslatorService translatorService,
            EventService eventService,
            Provider<ProxyTransport> proxyTransportProvider,
            PacketMapperRegistry packetMapperRegistry) {
        this.connectionManagerProvider = connectionManagerProvider;
        this.translatorService = translatorService;
        this.eventService = eventService;
        this.proxyTransportProvider = proxyTransportProvider;
        this.packetMapperRegistry = packetMapperRegistry;
    }

    public void handleConfigUpdate(ServerConfiguration config) {
        System.out.println("[OmniTranslator] New Config received!");
        translatorService.setTranslationConfiguration(config);
        eventService.callEvent(new ConfigUpdateEvent(config));
        proxyTransportProvider.get().broadcastConfigurationUpdate(packetMapperRegistry.toProto(config));
    }

    public void handleTranslationResult(TranslationResultData resultData) {
        UUID id = resultData.requestId();
        CompletableFuture<String> future = queue.remove(id);
        if (future != null) future.complete(resultData.result());
    }

    public CompletableFuture<String> sendTranslationRequest(UUID id, String text, String lang, String module) {
        CompletableFuture<String> future = new CompletableFuture<>();
        if (text == null || text.isBlank()) return CompletableFuture.completedFuture(text);

        queue.put(id, future);

        connectionManagerProvider.get().sendPacket(PacketRegistry.TRANSLATION_REQUEST,
                packetMapperRegistry.toProto(new TranslationRequestData(
                        id, text, lang, ServerConfiguration.Module.getModule(module)
                )));

        return future.orTimeout(60, TimeUnit.SECONDS).exceptionally(ex -> {
            queue.remove(id);
            return "TIMEOUT";
        });
    }
}