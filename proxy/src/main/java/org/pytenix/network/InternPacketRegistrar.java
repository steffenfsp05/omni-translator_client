package org.pytenix.network;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.omni.packets.PacketRegistry;
import org.omni.packets.registry.PacketRegistrar;
import org.pytenix.network.consumer.InternConfigRequestConsumer;
import org.pytenix.network.consumer.InternProfileConsumer;
import org.pytenix.network.consumer.InternTranslationRequestConsumer;
import org.transport.TransportService;

@Singleton
public class InternPacketRegistrar implements PacketRegistrar<RegisteredServer> {

    private final InternConfigRequestConsumer configConsumer;
    private final InternTranslationRequestConsumer translationConsumer;
    private final InternProfileConsumer internProfileConsumer;

    @Inject
    public InternPacketRegistrar(
            InternConfigRequestConsumer configConsumer,
            InternTranslationRequestConsumer translationConsumer,
            InternProfileConsumer internProfileConsumer) {

        this.configConsumer = configConsumer;
        this.translationConsumer = translationConsumer;
        this.internProfileConsumer = internProfileConsumer;
    }

    @Override
    public void register(TransportService<RegisteredServer> transport) {
        transport.registerPacket(PacketRegistry.CONFIG_REQUEST, configConsumer);
        transport.registerPacket(PacketRegistry.TRANSLATION_REQUEST, translationConsumer);
        transport.registerPacket(PacketRegistry.PROFILE_REQUEST_INTERN, internProfileConsumer);

        transport.registerPacket(PacketRegistry.TRANSLATION_RESULT, (ctx, result) -> {
        });
        transport.registerPacket(PacketRegistry.CONSENT_REFRESH, (ctx, req) -> {
        });
    }


}
