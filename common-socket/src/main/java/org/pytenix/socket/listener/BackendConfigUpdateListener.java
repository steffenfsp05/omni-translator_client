package org.pytenix.socket.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.entity.ServerConfiguration;
import org.omni.event.annotation.OmniSubscribe;
import org.omni.event.register.ConfigUpdateEvent;
import org.omni.packets.PacketMapperRegistry;

@Singleton
public class BackendConfigUpdateListener {

    // final ProxyTransport proxyTransport;
    final PacketMapperRegistry packetMapperRegistry;


    @Inject
    public BackendConfigUpdateListener(PacketMapperRegistry packetMapperRegistry) {
        // this.proxyTransport = proxyTransport;
        this.packetMapperRegistry = packetMapperRegistry;
    }

    @OmniSubscribe(priority = 99)
    public void onBackendConfigUpdate(ConfigUpdateEvent event) {
        final ServerConfiguration serverConfiguration = event.translationConfiguration();

        System.out.println("[OmniTranslator] New Config received!");

    }
}
