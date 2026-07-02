package org.pytenix.translation;

import org.pytenix.entity.ServerConfiguration;
import org.pytenix.event.EventService;
import org.pytenix.placeholder.PlaceholderService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface TranslatorService {

    CompletableFuture<String> translate(String text, String lang, String module);
    CompletableFuture<Boolean> requiresTranslation(UUID playerUUID);

    void setTranslationConfiguration(ServerConfiguration serverConfiguration);

    ServerConfiguration getTranslationConfiguration();
    EventService getEventService();
    PlaceholderService getPlaceholderService();

}
