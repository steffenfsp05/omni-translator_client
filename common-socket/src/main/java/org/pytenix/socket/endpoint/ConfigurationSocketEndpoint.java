package org.pytenix.socket.endpoint;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.entity.ServerConfiguration;
import org.omni.event.EventService;
import org.omni.event.register.ConfigUpdateEvent;
import org.omni.packets.data.ConfigurationRequestData;
import org.omni.proto.generated.Protobuf;
import org.omni.translation.TranslatorService;
import org.omni.transport.EndpointHandler;
import org.omni.transport.endpoint.ServerConfigurationEndpoint;

import java.util.concurrent.CompletableFuture;

@Singleton
public class ConfigurationSocketEndpoint implements ServerConfigurationEndpoint {


    private final TranslatorService translatorService;
    private final EventService eventService;


    @Inject
    public ConfigurationSocketEndpoint(
            TranslatorService translatorService,
            EventService eventService) {

        this.translatorService = translatorService;
        this.eventService = eventService;
    }


    @Override
    public void handleIncoming(ServerConfiguration inbound) {

        translatorService.setTranslationConfiguration(inbound);
        eventService.callEvent(new ConfigUpdateEvent(inbound));

    }

    @Override
    public CompletableFuture<ServerConfiguration> sendRequest(ConfigurationRequestData outbound) {
        //TODO: DOESNT NEED ACTUALLY
        return null;
    }
}
