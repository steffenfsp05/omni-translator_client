package org.pytenix.socket.consumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.omni.entity.ServerConfiguration;
import org.omni.packets.MappedPacketReceiveConsumer;
import org.omni.proto.generated.Protobuf;
import org.omni.transport.endpoint.ServerConfigurationEndpoint;
import org.transport.service.PacketContext;

import java.net.http.WebSocket;

@Singleton
public class BackendServerConfigConsumer extends MappedPacketReceiveConsumer<WebSocket, Protobuf.ServerConfiguration, ServerConfiguration> {

    private final ServerConfigurationEndpoint configurationSocketEndpoint;

    @Inject
    public BackendServerConfigConsumer(ServerConfigurationEndpoint configurationSocketEndpoint) {
        this.configurationSocketEndpoint = configurationSocketEndpoint;
    }

    @Override
    public void handle(PacketContext<WebSocket> context, ServerConfiguration javaPacket) {
        configurationSocketEndpoint.handleIncoming(javaPacket);
    }
}
