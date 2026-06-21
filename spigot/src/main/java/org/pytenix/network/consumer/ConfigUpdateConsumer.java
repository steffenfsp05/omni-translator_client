package org.pytenix.network.consumer;

import lombok.AllArgsConstructor;
import org.pytenix.TranslatorPlugin;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.event.register.ConfigUpdateEvent;
import org.pytenix.packets.MappedPacketReceiveConsumer;
import org.pytenix.proto.generated.NetworkPackets;
import org.pytenix.translation.TranslatorService;
import org.transport.service.PacketContext;
import org.transport.service.PacketReceiveConsumer;


@AllArgsConstructor
public class ConfigUpdateConsumer implements MappedPacketReceiveConsumer<String, NetworkPackets.ServerConfiguration, ServerConfiguration> {


    final TranslatorPlugin translatorPlugin;
    final TranslatorService translatorService;

    @Override
    public void handle(PacketContext<String> context, ServerConfiguration serverConfiguration) {

        translatorPlugin.getTranslatorService().setTranslationConfiguration(serverConfiguration);

        translatorPlugin.getSpigotTransport().setHasConfiguration(true);

        translatorService.getEventService().callEvent(new ConfigUpdateEvent(serverConfiguration));
    }
}
