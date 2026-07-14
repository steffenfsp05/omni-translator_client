package org.pytenix.network.consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.packets.PacketRegistry;
import org.omni.packets.data.ConfigurationRequestData;
import org.omni.proto.generated.Protobuf;
import org.omni.translation.TranslatorService;
import org.transport.service.PacketContext;

@Singleton
public class InternConfigRequestConsumer extends MappedPacketReceiveConsumer<RegisteredServer, Protobuf.ConfigRequestPacket, ConfigurationRequestData> {

    private final TranslatorService translatorService;

    @Inject
    public InternConfigRequestConsumer(TranslatorService translatorService) {
        this.translatorService = translatorService;
    }

    @Override
    public void handle(PacketContext<RegisteredServer> context, ConfigurationRequestData requestData) {

        if (translatorService.getTranslationConfiguration() == null) {
            return;
        }

        reply(context, PacketRegistry.SERVER_CONFIG, translatorService.getTranslationConfiguration());
    }


}
