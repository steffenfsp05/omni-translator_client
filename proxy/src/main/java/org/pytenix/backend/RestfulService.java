package org.pytenix.backend;

import org.pytenix.TranslatorPlugin;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.event.EventService;
import org.pytenix.event.register.ConfigUpdateEvent;
import org.pytenix.network.ProxyTransport;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.proto.generated.NetworkPackets;
import org.pytenix.translation.TranslatorService;
import org.pytenix.util.UuidUtil;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RestfulService {

    private final TranslatorPlugin translatorPlugin;
    private final OmniConnectionService connectionManager;

    private final TranslatorService translatorService;
    private final EventService eventService;
    private final ProxyTransport proxyTransport;

    private final ConcurrentHashMap<UUID, CompletableFuture<String>> queue = new ConcurrentHashMap<>();

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
        proxyTransport.broadcastConfigurationUpdate(translatorService.getServerConfigMapper().to(config));
    }

    public void handleTranslationResult(NetworkPackets.TranslationResult translationResult) {
        UUID id = UuidUtil.fromByteString(translationResult.getRequestId());
        CompletableFuture<String> future = queue.remove(id);
        if (future != null) future.complete(translationResult.getResult());
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

        queue.put(id, future);

        connectionManager.sendPacket(PacketRegistry.TRANSLATION_REQUEST, request);

        return future.orTimeout(60, TimeUnit.SECONDS).exceptionally(ex -> {
            queue.remove(id);
            return "TIMEOUT";
        });
    }
}