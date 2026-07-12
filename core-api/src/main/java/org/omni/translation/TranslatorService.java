package org.omni.translation;


import org.omni.entity.ServerConfiguration;
import org.omni.entity.TranslationModule;
import org.omni.event.EventService;
import org.omni.placeholder.service.PlaceholderService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface TranslatorService {

    CompletableFuture<Boolean> requiresTranslation(UUID playerUUID);

    CompletableFuture<String> translate(String text, String lang, TranslationModule translationModule);

    ServerConfiguration getTranslationConfiguration();

    void setTranslationConfiguration(ServerConfiguration serverConfiguration);

    EventService getEventService();

    PlaceholderService getPlaceholderService();

}
