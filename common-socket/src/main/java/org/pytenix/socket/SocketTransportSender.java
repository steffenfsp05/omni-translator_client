package org.pytenix.socket;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.protobuf.MessageLite;
import org.omni.packets.PacketMapperRegistry;
import org.omni.transport.TransportSender;
import org.pytenix.socket.socket.WebSocketService;
import org.transport.TransportService;
import org.transport.service.impl.PacketDefinition;

import java.net.http.WebSocket;

@Singleton
public class SocketTransportSender implements TransportSender {

    final Provider<WebSocketService> webSocketServiceProvider;
    final PacketMapperRegistry packetMapperRegistry;
    final TransportService<WebSocket> transportService;

    @Inject
    public SocketTransportSender(
            Provider<WebSocketService> webSocketServiceProvider,
            PacketMapperRegistry packetMapperRegistry,
            TransportService<WebSocket> transportService
    ) {
        this.webSocketServiceProvider = webSocketServiceProvider;
        this.packetMapperRegistry = packetMapperRegistry;
        this.transportService = transportService;
    }

    @Override
    public <A extends MessageLite> void sendPacket(PacketDefinition<A> packetDefinition, Record record) {
        if (record instanceof MessageLite)
            throw new IllegalArgumentException("Records dürfen keine MessageLite-Implementierungen sein!");


        if (!webSocketServiceProvider.get().getConnectionStatus().get()) return;

        transportService.send(webSocketServiceProvider.get().getWebSocket(), packetDefinition.id(), packetMapperRegistry.toProto(record));
    }
}
