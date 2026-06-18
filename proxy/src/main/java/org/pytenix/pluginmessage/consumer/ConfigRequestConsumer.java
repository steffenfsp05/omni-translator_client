package org.pytenix.pluginmessage.consumer;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.pytenix.TranslatorService;
import org.pytenix.packets.Packets;
import org.pytenix.proto.generated.NetworkPackets;
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
        };

        System.out.println("REPLYING WITH: " + Packets.SERVER_CONFIG.id() + " " + context.getConnection().getServerInfo().getName());
        System.out.println("RESULT:: "+context.reply(Packets.SERVER_CONFIG, translatorService.convertConfigToProtobuf(translatorService.getTranslationConfiguration())));
    }


}
