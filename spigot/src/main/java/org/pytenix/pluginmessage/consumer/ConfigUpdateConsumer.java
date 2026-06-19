package org.pytenix.pluginmessage.consumer;

import lombok.AllArgsConstructor;
import org.pytenix.SpigotTranslator;
import org.pytenix.entity.ServerConfiguration;
import org.pytenix.event.register.ConfigUpdateEvent;
import org.pytenix.proto.generated.NetworkPackets;
import org.pytenix.translation.TranslatorService;
import org.transport.service.PacketContext;
import org.transport.service.PacketReceiveConsumer;


@AllArgsConstructor
public class ConfigUpdateConsumer implements PacketReceiveConsumer<String, NetworkPackets.ServerConfiguration> {


    final SpigotTranslator spigotTranslator;
    final TranslatorService translatorService;


    @Override
    public void accept(PacketContext<String> stringPacketContext, NetworkPackets.ServerConfiguration serverConfiguration) {

        ServerConfiguration config = translatorService.convertConfigToNormal(serverConfiguration);

        spigotTranslator.getTranslatorService().setTranslationConfiguration(config);

        spigotTranslator.getSpigotTransport().setHasConfiguration(true);

        translatorService.getEventService().callEvent(new ConfigUpdateEvent(config));
    }
}
