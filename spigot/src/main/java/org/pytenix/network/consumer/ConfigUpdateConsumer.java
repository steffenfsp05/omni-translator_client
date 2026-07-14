package org.pytenix.network.consumer;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.omni.entity.ServerConfiguration;
import org.omni.event.EventService;
import org.omni.event.register.ConfigUpdateEvent;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.proto.generated.Protobuf;
import org.omni.translation.TranslatorService;
import org.pytenix.network.SpigotTransport;
import org.transport.service.PacketContext;

@Singleton
public class ConfigUpdateConsumer extends MappedPacketReceiveConsumer<String, Protobuf.ServerConfiguration, ServerConfiguration> {

    private final TranslatorService translatorService;
    private final Provider<SpigotTransport> spigotTransportProvider;
    private final EventService eventService;

    @Inject
    public ConfigUpdateConsumer(TranslatorService translatorService, Provider<SpigotTransport> spigotTransportProvider, EventService eventService) {
        this.translatorService = translatorService;
        this.spigotTransportProvider = spigotTransportProvider;
        this.eventService = eventService;
    }

    @Override
    public void handle(PacketContext<String> context, ServerConfiguration serverConfiguration) {
        translatorService.setTranslationConfiguration(serverConfiguration);
        spigotTransportProvider.get().setHasConfiguration(true);
        eventService.callEvent(new ConfigUpdateEvent(serverConfiguration));
    }
}