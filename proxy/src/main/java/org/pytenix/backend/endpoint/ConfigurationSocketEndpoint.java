package org.pytenix.backend.endpoint;

import com.google.inject.Inject;
import org.omni.entity.ServerConfiguration;
import org.omni.event.EventService;
import org.omni.event.register.ConfigUpdateEvent;
import org.omni.translation.TranslatorService;

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
