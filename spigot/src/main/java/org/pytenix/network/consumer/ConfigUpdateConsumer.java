package org.pytenix.network.consumer;

import lombok.AllArgsConstructor;
import org.pytenix.TranslatorPlugin;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.event.register.ConfigUpdateEvent;
import org.pytenix.proto.generated.NetworkPackets;
import org.pytenix.translation.TranslatorService;
import org.transport.service.PacketContext;
import org.transport.service.PacketReceiveConsumer;


@AllArgsConstructor
public class ConfigUpdateConsumer implements PacketReceiveConsumer<String, NetworkPackets.ServerConfiguration> {


    final TranslatorPlugin translatorPlugin;
    final TranslatorService translatorService;


    @Override
    public void accept(PacketContext<String> stringPacketContext, NetworkPackets.ServerConfiguration serverConfiguration) {

        ServerConfiguration config = translatorService.convertConfigToNormal(serverConfiguration);

        translatorPlugin.getTranslatorService().setTranslationConfiguration(config);

        translatorPlugin.getSpigotTransport().setHasConfiguration(true);

        translatorService.getEventService().callEvent(new ConfigUpdateEvent(config));
    }
}
