package org.pytenix.pluginmessage.consumer;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.pytenix.packets.PacketRegistry;
import org.pytenix.proto.generated.NetworkPackets;
import org.pytenix.translation.TranslatorService;
import org.transport.service.PacketContext;
import org.transport.service.PacketReceiveConsumer;

public class ConfigRequestConsumer implements PacketReceiveConsumer<RegisteredServer, NetworkPackets.ConfigRequestPacket> {

    final TranslatorService translatorService;

    public ConfigRequestConsumer(TranslatorService translatorService) {
        this.translatorService = translatorService;
    }


    @Override
    public void accept(PacketContext<RegisteredServer> context, NetworkPackets.ConfigRequestPacket configRequestPacket) {
        System.out.println("CONFIG REGUEST");
        if (translatorService.getTranslationConfiguration() == null) {
            System.out.println("CONFIG REGUEST ABORTED");
            return;
        }
        ;

        System.out.println("REPLYING WITH: " + PacketRegistry.SERVER_CONFIG.id() + " " + context.getConnection().getServerInfo().getName());
        System.out.println("RESULT:: " + context.reply(PacketRegistry.SERVER_CONFIG, translatorService.convertConfigToProtobuf(translatorService.getTranslationConfiguration())));
    }


}
