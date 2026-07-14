package org.pytenix.backend.consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.entity.ServerConfiguration;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.proto.generated.Protobuf;
import org.pytenix.backend.endpoint.ConfigurationSocketEndpoint;
import org.pytenix.backend.endpoint.TranslationSocketEndpoint;
import org.transport.service.PacketContext;

import java.net.http.WebSocket;

@Singleton
public class BackendServerConfigConsumer extends MappedPacketReceiveConsumer<WebSocket, Protobuf.ServerConfiguration, ServerConfiguration> {

    private final ConfigurationSocketEndpoint configurationSocketEndpoint;

    @Inject
    public BackendServerConfigConsumer(ConfigurationSocketEndpoint configurationSocketEndpoint) {
        this.configurationSocketEndpoint = configurationSocketEndpoint;
    }

    @Override
    public void handle(PacketContext<WebSocket> context, ServerConfiguration javaPacket) {
        configurationSocketEndpoint.handleConfigUpdate(javaPacket);
    }
}
