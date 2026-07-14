package org.pytenix.backend.listener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.omni.entity.ServerConfiguration;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.ConfigUpdateEvent;
import org.omni.packets.PacketMapperRegistry;
import org.pytenix.network.ProxyTransport;

@Singleton
public class BackendConfigUpdateListener {

    final Provider<ProxyTransport> proxyTransportProvider;
    final PacketMapperRegistry packetMapperRegistry;


    @Inject
    public BackendConfigUpdateListener(Provider<ProxyTransport> proxyTransportProvider, PacketMapperRegistry packetMapperRegistry) {
        this.proxyTransportProvider = proxyTransportProvider;
        this.packetMapperRegistry = packetMapperRegistry;
    }

    @OmniSubscribe(priority = 99)
    public void onBackendConfigUpdate(ConfigUpdateEvent event) {
        final ServerConfiguration serverConfiguration = event.translationConfiguration();

        System.out.println("[OmniTranslator] New Config received!");

        proxyTransportProvider.get().broadcastConfigurationUpdate(
                packetMapperRegistry.toProto(serverConfiguration)
        );
    }
}
