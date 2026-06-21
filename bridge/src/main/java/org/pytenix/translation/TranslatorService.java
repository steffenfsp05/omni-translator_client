package org.pytenix.translation;

import org.pytenix.entity.ServerConfiguration;
import org.pytenix.packets.PacketMapperRegistry;
import org.pytenix.event.EventService;
import org.pytenix.placeholder.PlaceholderService;

import java.util.concurrent.CompletableFuture;

public interface TranslatorService {


    CompletableFuture<String> translate(String text, String lang, String module);


    ServerConfiguration getTranslationConfiguration();
    void setTranslationConfiguration(ServerConfiguration serverConfiguration);


    EventService getEventService();

    PlaceholderService getPlaceholderService();

}
