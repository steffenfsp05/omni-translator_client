package org.pytenix.network.consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.packets.PacketMapperRegistry;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.ConfigurationRequestData;
import org.omni.proto.generated.Protobuf;
import org.omni.translation.TranslatorService;
import org.transport.service.PacketContext;

@Singleton
public class ConfigRequestConsumer extends MappedPacketReceiveConsumer<RegisteredServer, Protobuf.ConfigRequestPacket, ConfigurationRequestData> {

    private final TranslatorService translatorService;
    private final PacketMapperRegistry packetMapperRegistry;

    @Inject
    public ConfigRequestConsumer(TranslatorService translatorService, PacketMapperRegistry packetMapperRegistry) {
        this.translatorService = translatorService;
        this.packetMapperRegistry = packetMapperRegistry;
    }

    @Override
    public void handle(PacketContext<RegisteredServer> context, ConfigurationRequestData requestData) {

        System.out.println("CONFIG REGUEST");
        if (translatorService.getTranslationConfiguration() == null) {
            System.out.println("CONFIG REGUEST ABORTED");
            return;
        }


        System.out.println("REPLYING WITH: " + PacketRegistry.SERVER_CONFIG.id() + " " + context.getConnection().getServerInfo().getName());
        System.out.println("RESULT:: " + context.reply(PacketRegistry.SERVER_CONFIG,
                packetMapperRegistry.toProto(translatorService.getTranslationConfiguration())));
    }


}
