package org.pytenix.translation;

import org.pytenix.entity.ServerConfiguration;
import org.pytenix.event.EventService;
import org.pytenix.placeholder.PlaceholderService;
import org.pytenix.proto.generated.NetworkPackets;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface TranslatorService {


    CompletableFuture<String> process(UUID id, String text, String targetLang, String module);


    String preparePayload(UUID batchId, String text);

    CompletableFuture<String> processAndRestore(UUID batchId, String payload, String lang, String module, long started);

    CompletableFuture<String> translate(String text, String lang, String module);


    NetworkPackets.ServerConfiguration convertConfigToProtobuf(ServerConfiguration javaConfig);

    ServerConfiguration convertConfigToNormal(NetworkPackets.ServerConfiguration serverConfiguration);

    String handlePlaceholders(UUID uuid, String result);

    String handleGradient(UUID uuid, String text);


    ServerConfiguration getTranslationConfiguration();

    void setTranslationConfiguration(ServerConfiguration serverConfiguration);


    EventService getEventService();

    PlaceholderService getPlaceholderService();

}
