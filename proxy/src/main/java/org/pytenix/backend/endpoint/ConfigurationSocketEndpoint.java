package org.pytenix.backend.endpoint;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.omni.entity.ServerConfiguration;
import org.omni.event.EventService;
import org.omni.event.register.ConfigUpdateEvent;
import org.omni.packets.PacketMapperRegistry;
import org.omni.translation.TranslatorService;
import org.pytenix.backend.OmniConnectionService;
import org.pytenix.network.ProxyTransport;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigurationSocketEndpoint {


    private final TranslatorService translatorService;
    private final EventService eventService;


    @Inject
    public ConfigurationSocketEndpoint(
            TranslatorService translatorService,
            EventService eventService) {

        this.translatorService = translatorService;
        this.eventService = eventService;
    }

    public void handleConfigUpdate(ServerConfiguration config) {

        translatorService.setTranslationConfiguration(config);
        eventService.callEvent(new ConfigUpdateEvent(config));

    }

}
