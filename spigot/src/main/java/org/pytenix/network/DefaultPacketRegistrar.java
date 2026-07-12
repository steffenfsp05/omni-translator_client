package org.pytenix.network;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.packets.PacketRegistry;
import org.omni.packets.registry.PacketRegistrar;
import org.pytenix.network.consumer.ConfigUpdateConsumer;
import org.pytenix.network.consumer.ConsentRefreshConsumer;
import org.pytenix.network.consumer.ProfileResultConsumer;
import org.pytenix.network.consumer.TranslationResultConsumer;
import org.transport.TransportService;

@Singleton
public class DefaultPacketRegistrar implements PacketRegistrar<String> {
    private final ConfigUpdateConsumer configUpdateConsumer;
    private final ConsentRefreshConsumer consentRefreshConsumer;
    private final ProfileResultConsumer profileResultConsumer;
    private final TranslationResultConsumer translationResultConsumer;

    @Inject
    public DefaultPacketRegistrar(
            ConfigUpdateConsumer configUpdateConsumer,
            ConsentRefreshConsumer consentRefreshConsumer,
            ProfileResultConsumer profileResultConsumer,
            TranslationResultConsumer translationResultConsumer
    ) {
        this.configUpdateConsumer = configUpdateConsumer;
        this.consentRefreshConsumer = consentRefreshConsumer;
        this.profileResultConsumer = profileResultConsumer;
        this.translationResultConsumer = translationResultConsumer;
    }

    @Override
    public void register(TransportService<String> transport) {
        transport.registerPacket(PacketRegistry.TRANSLATION_RESULT, translationResultConsumer);
        transport.registerPacket(PacketRegistry.SERVER_CONFIG, configUpdateConsumer);
        transport.registerPacket(PacketRegistry.CONSENT_REFRESH, consentRefreshConsumer);
        transport.registerPacket(PacketRegistry.PROFILE, profileResultConsumer);
    }


}
